package ir.coffevista.vista_native.features.feed.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.datastore.MediaDataPolicy
import ir.coffevista.vista_native.core.datastore.SettingsPreferenceStore
import ir.coffevista.vista_native.core.network.ExternalMedia
import ir.coffevista.vista_native.core.network.forLargeMediaUpload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.internal.http2.StreamResetException
import okio.BufferedSink
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

data class UploadedMediaResult(
    val url: String,
    val objectKey: String,
    val isVideo: Boolean = false,
    val thumbnailUrl: String? = null,
    val wasTrimmed: Boolean = false,
)

/**
 * Boundary between post/story presentation code and the Android-backed media
 * uploader. Keeping this small makes the publishing state machine testable on
 * the JVM without mocking Android framework objects or a final implementation.
 */
interface PostMediaUploadGateway {
    suspend fun uploadImage(
        userId: String,
        uri: Uri,
        maxBytes: Long = 15L * 1024 * 1024,
        onProgress: (Float) -> Unit = {},
    ): UploadedMediaResult

    /**
     * Uploads a transparent PNG sticker (e.g., Post/Profile story card) preserving alpha channel.
     */
    suspend fun uploadStickerPng(
        userId: String,
        uri: Uri,
        maxBytes: Long = 15L * 1024 * 1024,
        onProgress: (Float) -> Unit = {},
    ): UploadedMediaResult = uploadImage(userId, uri, maxBytes, onProgress)

    suspend fun uploadVideo(
        userId: String,
        videoFile: File,
        maxBytes: Long = 15L * 1024 * 1024,
        onProgress: (Float) -> Unit = {},
    ): UploadedMediaResult

    suspend fun uploadAudio(
        userId: String,
        uri: Uri,
        maxBytes: Long = 15L * 1024 * 1024,
        startMs: Int = 0,
        endMs: Int = 0,
        onProgress: (Float) -> Unit = {},
    ): UploadedMediaResult

    suspend fun deleteUpload(objectKey: String): Boolean
}

@Singleton
class PostMediaUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: FeedApi,
    @ExternalMedia private val externalClient: OkHttpClient,
    private val preferenceStore: SettingsPreferenceStore,
) : PostMediaUploadGateway {
    private val resolver: ContentResolver = context.contentResolver
    private val cacheDir = context.cacheDir
    private val uploadClient: OkHttpClient = externalClient.forLargeMediaUpload()

    override suspend fun uploadImage(
        userId: String,
        uri: Uri,
        maxBytes: Long,
        onProgress: (Float) -> Unit,
    ): UploadedMediaResult = withContext(Dispatchers.IO) {
        require(userId.isNotBlank()) { "شناسه کاربر نامعتبر است" }
        val qualityKey = runCatching { preferenceStore.preferences.first().mediaQuality }.getOrDefault("high")
        val preset = MediaDataPolicy.uploadPreset(qualityKey)
        val temporaryJpeg = compressImageToJpeg(
            uri = uri,
            maxBytes = maxBytes,
            maxDimension = preset.maxDimension,
            jpegQuality = preset.jpegQuality,
        )
        try {
            val objectKey = "posts/${userId.safeSegment()}/${System.currentTimeMillis()}_image.jpg"
            val presign = MediaUploadExecutor.uploadWithRetry(
                client = uploadClient,
                file = temporaryJpeg,
                contentType = JPEG_MIME,
                errorLabel = "آپلود تصویر",
                presignProvider = {
                    api.presignUpload(
                        PostPresignRequestDto(
                            objectKey = objectKey,
                            contentType = JPEG_MIME,
                            fileSize = temporaryJpeg.length(),
                        ),
                    )
                },
                onProgress = onProgress,
            )
            onProgress(1f)
            UploadedMediaResult(
                url = presign.objectUrl,
                objectKey = presign.objectKey,
                isVideo = false,
            )
        } finally {
            temporaryJpeg.delete()
        }
    }

    override suspend fun uploadStickerPng(
        userId: String,
        uri: Uri,
        maxBytes: Long,
        onProgress: (Float) -> Unit,
    ): UploadedMediaResult = withContext(Dispatchers.IO) {
        require(userId.isNotBlank()) { "شناسه کاربر نامعتبر است" }
        val temporaryPng = copyOrCompressPng(uri, maxBytes)
        try {
            val objectKey = "posts/${userId.safeSegment()}/${System.currentTimeMillis()}_sticker.png"
            val presign = MediaUploadExecutor.uploadWithRetry(
                client = uploadClient,
                file = temporaryPng,
                contentType = "image/png",
                errorLabel = "آپلود استیکر",
                maxRetries = 1,
                presignProvider = {
                    api.presignUpload(
                        PostPresignRequestDto(
                            objectKey = objectKey,
                            contentType = "image/png",
                            fileSize = temporaryPng.length(),
                        ),
                    )
                },
                onProgress = onProgress,
            )
            onProgress(1f)
            UploadedMediaResult(
                url = presign.objectUrl,
                objectKey = presign.objectKey,
                isVideo = false,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            uploadImage(userId, uri, maxBytes, onProgress)
        } finally {
            temporaryPng.delete()
        }
    }

    private fun copyOrCompressPng(uri: Uri, maxBytes: Long): File {
        val decoded = (openInputStream(uri) ?: throw IOException("فایل استیکر قابل خواندن نیست")).use {
            BitmapFactory.decodeStream(it)
        } ?: throw IOException("دیکود استیکر ناموفق بود")

        val scaled = if (decoded.width > MAX_IMAGE_DIMENSION || decoded.height > MAX_IMAGE_DIMENSION) {
            val ratio = decoded.width.toFloat() / decoded.height.toFloat()
            val targetW: Int
            val targetH: Int
            if (ratio > 1f) {
                targetW = MAX_IMAGE_DIMENSION
                targetH = (MAX_IMAGE_DIMENSION / ratio).toInt().coerceAtLeast(1)
            } else {
                targetH = MAX_IMAGE_DIMENSION
                targetW = (MAX_IMAGE_DIMENSION * ratio).toInt().coerceAtLeast(1)
            }
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                if (it != decoded) decoded.recycle()
            }
        } else decoded

        val file = File.createTempFile("story-sticker-", ".png", cacheDir)
        return try {
            file.outputStream().use { output ->
                if (!scaled.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw IOException("ذخیره استیکر ناموفق بود")
                }
            }
            if (file.length() > maxBytes) {
                throw IOException("حجم استیکر بیش از حد مجاز است")
            }
            file
        } catch (e: Throwable) {
            file.delete()
            throw e
        } finally {
            scaled.recycle()
        }
    }

    override suspend fun uploadVideo(
        userId: String,
        videoFile: File,
        maxBytes: Long,
        onProgress: (Float) -> Unit,
    ): UploadedMediaResult = withContext(Dispatchers.IO) {
        require(userId.isNotBlank()) { "شناسه کاربر نامعتبر است" }
        if (!videoFile.exists() || videoFile.length() <= 0) {
            throw IOException("فایل ویدیو وجود ندارد یا خالی است")
        }
        if (videoFile.length() > maxBytes) {
            val maxMb = maxBytes / (1024 * 1024)
            throw IOException("حجم ویدیو بیش از حد مجاز است (حداکثر $maxMb مگابایت)")
        }

        // 1. Generate micro thumbnail
        val thumbnailFile = generateVideoThumbnail(videoFile)
        var uploadedThumbnailUrl: String? = null
        if (thumbnailFile != null && thumbnailFile.exists()) {
            try {
                val thumbKey = "posts/${userId.safeSegment()}/${System.currentTimeMillis()}_thumb.jpg"
                val thumbPresign = MediaUploadExecutor.uploadWithRetry(
                    client = uploadClient,
                    file = thumbnailFile,
                    contentType = JPEG_MIME,
                    errorLabel = "آپلود تصویر پیش‌نمایش",
                    maxRetries = 1,
                    presignProvider = {
                        api.presignUpload(
                            PostPresignRequestDto(
                                objectKey = thumbKey,
                                contentType = JPEG_MIME,
                                fileSize = thumbnailFile.length(),
                            ),
                        )
                    },
                    onProgress = {},
                )
                uploadedThumbnailUrl = thumbPresign.objectUrl
            } catch (_: Exception) {
                // Non-fatal if thumbnail upload fails
            } finally {
                thumbnailFile.delete()
            }
        }

        // 2. Upload video file
        val videoKey = "posts/${userId.safeSegment()}/${System.currentTimeMillis()}_video.mp4"
        val presign = MediaUploadExecutor.uploadWithRetry(
            client = uploadClient,
            file = videoFile,
            contentType = MP4_MIME,
            errorLabel = "آپلود ویدیو",
            presignProvider = {
                api.presignUpload(
                    PostPresignRequestDto(
                        objectKey = videoKey,
                        contentType = MP4_MIME,
                        fileSize = videoFile.length(),
                    ),
                )
            },
            onProgress = onProgress,
        )
        onProgress(1f)
        UploadedMediaResult(
            url = presign.objectUrl,
            objectKey = presign.objectKey,
            isVideo = true,
            thumbnailUrl = uploadedThumbnailUrl,
        )
    }

    override suspend fun uploadAudio(
        userId: String,
        uri: Uri,
        maxBytes: Long,
        startMs: Int,
        endMs: Int,
        onProgress: (Float) -> Unit,
    ): UploadedMediaResult = withContext(Dispatchers.IO) {
        require(userId.isNotBlank()) { "شناسه کاربر نامعتبر است" }
        val trimmedAudio = if (endMs > startMs && endMs > 0) {
            ir.coffevista.vista_native.features.feed.music.AudioTrimEngine.trimAudio(
                context = context,
                audioUri = uri,
                startMs = startMs.toLong(),
                endMs = endMs.toLong(),
            )
        } else null

        val (audioFile, extension, mimeType) = if (trimmedAudio != null) {
            Triple(trimmedAudio, "m4a", "audio/mp4")
        } else {
            val rawMime = resolver.getType(uri)
            val mime = if (rawMime?.startsWith("audio/") == true) {
                rawMime
            } else {
                val name = uri.lastPathSegment?.lowercase().orEmpty()
                when {
                    name.endsWith(".m4a") -> "audio/mp4"
                    name.endsWith(".ogg") -> "audio/ogg"
                    name.endsWith(".wav") -> "audio/wav"
                    name.endsWith(".aac") -> "audio/aac"
                    name.endsWith(".flac") -> "audio/flac"
                    else -> "audio/mpeg"
                }
            }
            val ext = when (mime) {
                "audio/mp4" -> "m4a"
                "audio/ogg" -> "ogg"
                "audio/wav", "audio/x-wav" -> "wav"
                "audio/aac" -> "aac"
                "audio/flac" -> "flac"
                else -> "mp3"
            }
            val temp = File.createTempFile("post-audio-", ".$ext", cacheDir)
            val stream = openInputStream(uri)
            stream?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("فایل صوتی قابل خواندن نیست")
            Triple(temp, ext, mime)
        }

        try {
            require(audioFile.length() > 0L && audioFile.length() <= maxBytes) {
                "حجم فایل صوتی بیش از حد مجاز است"
            }
            val objectKey = "music/${userId.safeSegment()}/${System.currentTimeMillis()}_audio.$extension"
            val presign = MediaUploadExecutor.uploadWithRetry(
                client = uploadClient,
                file = audioFile,
                contentType = mimeType,
                errorLabel = "آپلود موسیقی",
                presignProvider = {
                    api.presignUpload(PostPresignRequestDto(objectKey, mimeType, audioFile.length()))
                },
                onProgress = onProgress,
            )
            onProgress(1f)
            UploadedMediaResult(
                url = presign.objectUrl,
                objectKey = presign.objectKey,
                wasTrimmed = trimmedAudio != null,
            )
        } finally {
            audioFile.delete()
        }
    }

    override suspend fun deleteUpload(objectKey: String): Boolean = withContext(Dispatchers.IO) {
        if (objectKey.isBlank()) return@withContext false
        runCatching {
            api.deleteUpload(DeleteUploadRequestDto(objectKey)).success
        }.getOrDefault(false)
    }



    private fun openInputStream(uri: Uri): java.io.InputStream? {
        return if (uri.scheme.equals("file", ignoreCase = true)) {
            val path = uri.path ?: uri.toString().removePrefix("file://")
            val f = File(path)
            if (f.exists()) f.inputStream() else resolver.openInputStream(uri)
        } else {
            resolver.openInputStream(uri)
        }
    }

    private fun compressImageToJpeg(
        uri: Uri,
        maxBytes: Long,
        maxDimension: Int = MAX_IMAGE_DIMENSION,
        jpegQuality: Int = JPEG_QUALITY,
    ): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (openInputStream(uri) ?: throw IOException("فایل تصویر قابل خواندن نیست")).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("فایل انتخاب‌شده تصویر معتبری نیست")
        }

        var sampleSize = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w > maxDimension || h > maxDimension) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = (openInputStream(uri) ?: throw IOException("فایل تصویر قابل خواندن نیست")).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IOException("دیکود تصویر ناموفق بود")

        val matrix = android.graphics.Matrix()
        runCatching {
            openInputStream(uri)?.use { stream ->
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
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap) rawBitmap.recycle()
            rotated
        } else rawBitmap

        val finalBitmap = if (oriented.width > maxDimension || oriented.height > maxDimension) {
            val ratio = oriented.width.toFloat() / oriented.height.toFloat()
            val targetW: Int
            val targetH: Int
            if (ratio > 1f) {
                targetW = maxDimension
                targetH = (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                targetH = maxDimension
                targetW = (maxDimension * ratio).toInt().coerceAtLeast(1)
            }
            val scaled = Bitmap.createScaledBitmap(oriented, targetW, targetH, true)
            if (scaled != oriented) oriented.recycle()
            scaled
        } else oriented

        val file = File.createTempFile("post-media-", ".jpg", cacheDir)
        return try {
            file.outputStream().use { output ->
                if (!finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)) {
                    throw IOException("فشرده‌سازی تصویر ناموفق بود")
                }
            }
            if (file.length() > maxBytes) {
                val maxMb = maxBytes / (1024 * 1024)
                throw IOException("حجم تصویر پس از فشرده‌سازی بیش از حد مجاز است ($maxMb مگابایت)")
            }
            file
        } catch (e: Throwable) {
            file.delete()
            throw e
        } finally {
            finalBitmap.recycle()
        }
    }

    private fun generateVideoThumbnail(videoFile: File): File? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (bitmap != null) {
                val file = File.createTempFile("video-thumb-", ".jpg", cacheDir)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                }
                bitmap.recycle()
                file
            } else null
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun String.safeSegment(): String =
        filter { it.isLetterOrDigit() || it == '_' || it == '-' }.ifBlank { "user" }

    companion object {
        const val DEFAULT_MAX_BYTES = 10L * 1024 * 1024 // 10 MB for normal users
        const val GOLD_BLACK_MAX_BYTES = 50L * 1024 * 1024 // 50 MB for gold/black tick users
        const val PREMIUM_MAX_BYTES = 100L * 1024 * 1024 // 100 MB for blue tick users
        const val MAX_IMAGE_DIMENSION = 1440
        const val JPEG_QUALITY = 85
        const val JPEG_MIME = "image/jpeg"
        const val MP4_MIME = "video/mp4"
    }
}

internal object MediaUploadExecutor {
    const val DEFAULT_MAX_RETRIES = 2
    val DEFAULT_RETRY_BACKOFF_MS = longArrayOf(2_000L, 5_000L)

    private class RetryableUploadHttpException(
        val code: Int,
        message: String,
    ) : IOException(message)

    suspend fun uploadWithRetry(
        client: OkHttpClient,
        file: File,
        contentType: String,
        errorLabel: String,
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        backoffMs: LongArray = DEFAULT_RETRY_BACKOFF_MS,
        presignProvider: suspend () -> PostPresignResponseDto,
        onProgress: (Float) -> Unit,
    ): PostPresignResponseDto {
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        for (attempt in 0..maxRetries) {
            currentCoroutineContext().ensureActive()
            if (attempt > 0) {
                onProgress(0f)
            }
            try {
                val presign = presignProvider()
                val request = Request.Builder()
                    .url(presign.url)
                    .put(ProgressRequestBody(file, contentType, onProgress))

                presign.headers.forEach { (name, value) ->
                    if (!name.equals("host", ignoreCase = true) &&
                        !name.equals("content-length", ignoreCase = true)
                    ) {
                        request.header(name, value)
                    }
                }

                val call = client.newCall(request.build())
                val cancelHandle = currentCoroutineContext().job.invokeOnCompletion { cause ->
                    if (cause != null) {
                        call.cancel()
                    }
                }
                try {
                    call.execute().use { response ->
                        if (response.isSuccessful) {
                            return presign
                        }
                        val message = "$errorLabel با خطای ${response.code} متوقف شد"
                        if (isRetryableHttpCode(response.code)) {
                            throw RetryableUploadHttpException(response.code, message)
                        } else {
                            throw IOException(message)
                        }
                    }
                } finally {
                    cancelHandle.dispose()
                }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                if (attempt < maxRetries && isRetryableFailure(e)) {
                    val waitMs = backoffMs.getOrElse(attempt) { backoffMs.lastOrNull() ?: 2_000L }
                    if (waitMs > 0L) {
                        delay(waitMs)
                    }
                } else if (e is RetryableUploadHttpException) {
                    throw IOException(e.message, e)
                } else {
                    throw e
                }
            }
        }
        throw IOException("$errorLabel متوقف شد")
    }

    internal fun isRetryableHttpCode(code: Int): Boolean =
        code == 408 || code == 429 || code in 500..599

    internal fun isRetryableFailure(error: Throwable): Boolean {
        val causes = generateSequence(error) { it.cause }.take(16).toList()
        if (causes.any { it is SSLException }) return false
        return causes.any {
            it is RetryableUploadHttpException ||
                it is SocketTimeoutException ||
                it is InterruptedIOException ||
                it is ConnectException ||
                it is UnknownHostException ||
                it is EOFException ||
                it is ProtocolException ||
                it is SocketException ||
                it is StreamResetException
        }
    }
}

internal fun Throwable.toUserFriendlyUploadError(defaultMessage: String): String {
    val causes = generateSequence(this) { it.cause }.take(16).toList()
    if (causes.any {
            it is SocketTimeoutException ||
                it is InterruptedIOException ||
                it.message?.contains("timeout", ignoreCase = true) == true ||
                it.message?.contains("timed out", ignoreCase = true) == true
        }
    ) {
        return "زمان آپلود به پایان رسید؛ لطفاً اتصال اینترنت را بررسی کرده و دوباره تلاش کنید"
    }
    if (causes.any { it is UnknownHostException || it is ConnectException }) {
        return "اتصال اینترنت برقرار نیست؛ لطفاً شبکه را بررسی کرده و دوباره تلاش کنید"
    }
    if (causes.any { it is EOFException || it is SocketException || it is StreamResetException }) {
        return "ارتباط هنگام آپلود قطع شد؛ لطفاً دوباره تلاش کنید"
    }
    val rawMessage = message?.trim().orEmpty()
    return if (rawMessage.isNotEmpty() && rawMessage.any { it in '\u0600'..'\u06FF' }) {
        rawMessage
    } else {
        defaultMessage
    }
}

private const val UPLOAD_BUFFER_BYTES = 64 * 1024

private class ProgressRequestBody(
    private val file: File,
    private val contentType: String,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {
    override fun contentType() = contentType.toMediaType()
    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val total = contentLength()
        val buffer = ByteArray(UPLOAD_BUFFER_BYTES)
        var sent = 0L
        file.inputStream().use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                sink.write(buffer, 0, read)
                sent += read
                if (total > 0) {
                    onProgress((sent.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                }
            }
        }
    }
}
