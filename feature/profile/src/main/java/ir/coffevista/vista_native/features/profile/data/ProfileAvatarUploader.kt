package ir.coffevista.vista_native.features.profile.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.network.ExternalMedia
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink

data class UploadedProfileAvatar(val url: String, val objectKey: String)

/** Uploads profile avatars through the same presigned-storage contract as Flutter. */
@Singleton
class ProfileAvatarUploader @Inject constructor(
    @ApplicationContext context: Context,
    private val api: ProfileApi,
    @ExternalMedia private val externalClient: OkHttpClient,
) {
    private val resolver: ContentResolver = context.contentResolver
    private val cacheDirectory = context.cacheDir

    suspend fun upload(
        userId: String,
        uri: Uri,
        onProgress: (Float) -> Unit = {},
    ): UploadedProfileAvatar = withContext(Dispatchers.IO) {
        require(userId.isNotBlank()) { "شناسه کاربر نامعتبر است" }
        require(resolver.getType(uri)?.startsWith("image/") != false) { "فقط تصویر قابل انتخاب است" }
        val sourceSize = resolver.openAssetFileDescriptor(uri, "r")?.length
            ?.takeIf { it >= 0 }
            ?: sourceLengthAtMost(uri)
        if (sourceSize > MAX_INPUT_BYTES) throw IOException("حجم تصویر باید حداکثر ۵ مگابایت باشد")

        val temporaryJpeg = compressToJpeg(uri)
        try {
            if (temporaryJpeg.length() !in 1..MAX_INPUT_BYTES) {
                throw IOException("حجم تصویر فشرده‌شده باید حداکثر ۵ مگابایت باشد")
            }
            val objectKey = "avatars/${userId.safeSegment()}/${System.currentTimeMillis()}_avatar.jpg"
            val presign = api.presignUpload(
                ProfileMediaPresignRequestDto(
                    objectKey = objectKey,
                    contentType = JPEG_MIME,
                    fileSize = temporaryJpeg.length(),
                ),
            )
            val request = Request.Builder().url(presign.url)
                .put(ProfileAvatarRequestBody(temporaryJpeg, JPEG_MIME, onProgress))
            presign.headers.forEach { (name, value) ->
                if (!name.equals("host", ignoreCase = true) && !name.equals("content-length", ignoreCase = true)) {
                    request.header(name, value)
                }
            }
            externalClient.newCall(request.build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("آپلود تصویر با خطای ${response.code} متوقف شد")
            }
            onProgress(1f)
            UploadedProfileAvatar(presign.objectUrl, presign.objectKey)
        } finally {
            temporaryJpeg.delete()
        }
    }

    suspend fun uploadDocument(
        userId: String,
        uri: Uri,
        onProgress: (Float) -> Unit = {},
    ): UploadedProfileAvatar = withContext(Dispatchers.IO) {
        require(userId.isNotBlank()) { "شناسه کاربر نامعتبر است" }
        val sourceSize = resolver.openAssetFileDescriptor(uri, "r")?.length
            ?.takeIf { it >= 0 }
            ?: sourceLengthAtMost(uri)
        if (sourceSize > MAX_INPUT_BYTES) throw IOException("حجم مدرک باید حداکثر ۵ مگابایت باشد")

        val temporaryJpeg = compressToJpeg(uri)
        try {
            if (temporaryJpeg.length() !in 1..MAX_INPUT_BYTES) {
                throw IOException("حجم تصویر مدرک فشرده‌شده باید حداکثر ۵ مگابایت باشد")
            }
            val objectKey = "documents/${userId.safeSegment()}/${System.currentTimeMillis()}_verification.jpg"
            val presign = api.presignUpload(
                ProfileMediaPresignRequestDto(
                    objectKey = objectKey,
                    contentType = JPEG_MIME,
                    fileSize = temporaryJpeg.length(),
                ),
            )
            val request = Request.Builder().url(presign.url)
                .put(ProfileAvatarRequestBody(temporaryJpeg, JPEG_MIME, onProgress))
            presign.headers.forEach { (name, value) ->
                if (!name.equals("host", ignoreCase = true) && !name.equals("content-length", ignoreCase = true)) {
                    request.header(name, value)
                }
            }
            externalClient.newCall(request.build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("آپلود تصویر مدرک با خطای ${response.code} متوقف شد")
            }
            onProgress(1f)
            UploadedProfileAvatar(presign.objectUrl, presign.objectKey)
        } finally {
            temporaryJpeg.delete()
        }
    }

    suspend fun deleteByUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        val objectKey = objectKeyFromUrl(url) ?: return@withContext false
        api.deleteUpload(ProfileMediaDeleteRequestDto(objectKey)).success
    }

    private fun compressToJpeg(uri: Uri): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (resolver.openInputStream(uri) ?: throw IOException("فایل انتخاب‌شده قابل خواندن نیست")).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("فایل انتخاب‌شده تصویر معتبر نیست")
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
        val rawBitmap = (resolver.openInputStream(uri) ?: throw IOException("فایل انتخاب‌شده قابل خواندن نیست")).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IOException("فایل انتخاب‌شده قابل خواندن نیست")

        val matrix = android.graphics.Matrix()
        runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = android.media.ExifInterface(stream)
                when (exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)) {
                    android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    android.media.ExifInterface.ORIENTATION_TRANSPOSE -> {
                        matrix.postRotate(90f)
                        matrix.postScale(-1f, 1f)
                    }
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    android.media.ExifInterface.ORIENTATION_TRANSVERSE -> {
                        matrix.postRotate(270f)
                        matrix.postScale(-1f, 1f)
                    }
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
            }
        }
        val oriented = if (!matrix.isIdentity) {
            val transformed = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (transformed != rawBitmap) rawBitmap.recycle()
            transformed
        } else rawBitmap

        val finalBitmap = if (oriented.width > MAX_DIMENSION || oriented.height > MAX_DIMENSION) {
            val ratio = oriented.width.toFloat() / oriented.height.toFloat()
            val targetW: Int
            val targetH: Int
            if (ratio > 1f) {
                targetW = MAX_DIMENSION
                targetH = (MAX_DIMENSION / ratio).toInt().coerceAtLeast(1)
            } else {
                targetH = MAX_DIMENSION
                targetW = (MAX_DIMENSION * ratio).toInt().coerceAtLeast(1)
            }
            val scaled = Bitmap.createScaledBitmap(oriented, targetW, targetH, true)
            if (scaled != oriented) oriented.recycle()
            scaled
        } else oriented

        val file = File.createTempFile("profile-avatar-", ".jpg", cacheDirectory)
        return try {
            file.outputStream().use { output ->
                if (!finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    throw IOException("فشرده‌سازی تصویر ناموفق بود")
                }
            }
            file
        } catch (error: Throwable) {
            file.delete()
            throw error
        } finally {
            finalBitmap.recycle()
        }
    }

    private fun sourceLengthAtMost(uri: Uri): Long {
        var total = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        resolver.openInputStream(uri)?.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_INPUT_BYTES) return total
            }
        } ?: throw IOException("فایل انتخاب‌شده قابل خواندن نیست")
        return total
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        var sampledWidth = width
        var sampledHeight = height
        while (sampledWidth > MAX_DIMENSION || sampledHeight > MAX_DIMENSION) {
            sample *= 2
            sampledWidth /= 2
            sampledHeight /= 2
        }
        return sample
    }

    private fun objectKeyFromUrl(url: String): String? {
        val segments = Uri.parse(url).pathSegments
        val bucketIndex = segments.indexOf("vista-bucket")
        if (bucketIndex >= 0 && bucketIndex < segments.lastIndex) return segments.drop(bucketIndex + 1).joinToString("/")
        val avatarIndex = segments.indexOf("avatars")
        return avatarIndex.takeIf { it >= 0 }?.let { segments.drop(it).joinToString("/") }
    }

    private fun String.safeSegment(): String = filter { it.isLetterOrDigit() || it == '_' || it == '-' }.ifBlank { "user" }

    private companion object {
        const val MAX_INPUT_BYTES = 5L * 1024 * 1024
        const val MAX_DIMENSION = 1080
        const val JPEG_QUALITY = 85
        const val JPEG_MIME = "image/jpeg"
    }
}

private class ProfileAvatarRequestBody(
    private val file: File,
    private val contentType: String,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {
    override fun contentType() = contentType.toMediaType()
    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var sent = 0L
        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                sink.write(buffer, 0, read)
                sent += read
                onProgress((sent.toFloat() / total.toFloat()).coerceIn(0f, 1f))
            }
        }
    }
}
