package ir.coffevista.vista_native.features.feed.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.network.ExternalMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class UploadedMediaResult(
    val url: String,
    val objectKey: String,
    val isVideo: Boolean = false,
    val thumbnailUrl: String? = null,
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
) : PostMediaUploadGateway {
    private val resolver: ContentResolver = context.contentResolver
    private val cacheDir = context.cacheDir

    override suspend fun uploadImage(
        userId: String,
        uri: Uri,
        maxBytes: Long,
        onProgress: (Float) -> Unit,
    ): UploadedMediaResult = withContext(Dispatchers.IO) {
        require(userId.isNotBlank()) { "شناسه کاربر نامعتبر است" }
        val temporaryJpeg = compressImageToJpeg(uri, maxBytes)
        try {
            val objectKey = "posts/${userId.safeSegment()}/${System.currentTimeMillis()}_image.jpg"
            val presign = api.presignUpload(
                PostPresignRequestDto(
                    objectKey = objectKey,
                    contentType = JPEG_MIME,
                    fileSize = temporaryJpeg.length(),
                ),
            )
            val request = Request.Builder()
                .url(presign.url)
                .put(ProgressRequestBody(temporaryJpeg, JPEG_MIME, onProgress))

            presign.headers.forEach { (name, value) ->
                if (!name.equals("host", ignoreCase = true) && !name.equals("content-length", ignoreCase = true)) {
                    request.header(name, value)
                }
            }

            externalClient.newCall(request.build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("آپلود تصویر با خطای ${response.code} متوقف شد")
            }
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
                val thumbPresign = api.presignUpload(
                    PostPresignRequestDto(
                        objectKey = thumbKey,
                        contentType = JPEG_MIME,
                        fileSize = thumbnailFile.length(),
                    ),
                )
                val thumbReq = Request.Builder()
                    .url(thumbPresign.url)
                    .put(ProgressRequestBody(thumbnailFile, JPEG_MIME) {})

                thumbPresign.headers.forEach { (name, value) ->
                    if (!name.equals("host", ignoreCase = true) && !name.equals("content-length", ignoreCase = true)) {
                        thumbReq.header(name, value)
                    }
                }
                externalClient.newCall(thumbReq.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        uploadedThumbnailUrl = thumbPresign.objectUrl
                    }
                }
            } catch (_: Exception) {
                // Non-fatal if thumbnail upload fails
            } finally {
                thumbnailFile.delete()
            }
        }

        // 2. Upload video file
        val videoKey = "posts/${userId.safeSegment()}/${System.currentTimeMillis()}_video.mp4"
        val presign = api.presignUpload(
            PostPresignRequestDto(
                objectKey = videoKey,
                contentType = MP4_MIME,
                fileSize = videoFile.length(),
            ),
        )
        val request = Request.Builder()
            .url(presign.url)
            .put(ProgressRequestBody(videoFile, MP4_MIME, onProgress))

        presign.headers.forEach { (name, value) ->
            if (!name.equals("host", ignoreCase = true) && !name.equals("content-length", ignoreCase = true)) {
                request.header(name, value)
            }
        }

        externalClient.newCall(request.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("آپلود ویدیو با خطای ${response.code} متوقف شد")
        }
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
            trimAudioFast(uri, startMs.toLong(), endMs.toLong())
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
            resolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("فایل صوتی قابل خواندن نیست")
            Triple(temp, ext, mime)
        }

        try {
            require(audioFile.length() > 0L && audioFile.length() <= maxBytes) {
                "حجم فایل صوتی بیش از حد مجاز است"
            }
            val objectKey = "music/${userId.safeSegment()}/${System.currentTimeMillis()}_audio.$extension"
            val presign = api.presignUpload(PostPresignRequestDto(objectKey, mimeType, audioFile.length()))
            val request = Request.Builder().url(presign.url)
                .put(ProgressRequestBody(audioFile, mimeType, onProgress))
            presign.headers.forEach { (name, value) ->
                if (!name.equals("host", true) && !name.equals("content-length", true)) request.header(name, value)
            }
            externalClient.newCall(request.build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("آپلود موسیقی با خطای ${response.code} متوقف شد")
            }
            onProgress(1f)
            UploadedMediaResult(url = presign.objectUrl, objectKey = presign.objectKey)
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

    private fun trimAudioFast(
        audioUri: Uri,
        startMs: Long,
        endMs: Long,
    ): File? {
        return runCatching {
            val extractor = android.media.MediaExtractor()
            val fd = resolver.openFileDescriptor(audioUri, "r") ?: return null
            extractor.setDataSource(fd.fileDescriptor)
            val trackCount = extractor.trackCount
            var audioTrackIndex = -1
            var audioFormat: android.media.MediaFormat? = null

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) {
                extractor.release()
                fd.close()
                return null
            }

            val outputFile = File.createTempFile("trimmed-audio-", ".m4a", cacheDir)
            val muxer = android.media.MediaMuxer(outputFile.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            var bufferSize = 256 * 1024
            if (audioFormat.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)) {
                val sz = audioFormat.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)
                if (sz > bufferSize) bufferSize = sz
            }
            val buffer = java.nio.ByteBuffer.allocate(bufferSize)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            try {
                extractor.seekTo(startUs, android.media.MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                var basePtsUs = startUs
                var isFirstSample = true

                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break

                    val sampleTrack = extractor.sampleTrackIndex
                    val sampleTime = extractor.sampleTime

                    if (sampleTrack == audioTrackIndex) {
                        if (sampleTime > endUs) break
                        if (sampleTime >= startUs) {
                            if (isFirstSample) {
                                basePtsUs = sampleTime
                                isFirstSample = false
                            }
                            bufferInfo.presentationTimeUs = (sampleTime - basePtsUs).coerceAtLeast(0L)
                            bufferInfo.flags = extractor.sampleFlags
                            muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                        }
                    }
                    extractor.advance()
                }
            } finally {
                runCatching { muxer.stop() }
                runCatching { muxer.release() }
                extractor.release()
                fd.close()
            }

            if (outputFile.exists() && outputFile.length() > 0L) outputFile else null
        }.getOrNull()
    }

    private fun compressImageToJpeg(uri: Uri, maxBytes: Long): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (resolver.openInputStream(uri) ?: throw IOException("فایل تصویر قابل خواندن نیست")).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("فایل انتخاب‌شده تصویر معتبری نیست")
        }

        var sampleSize = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w > MAX_IMAGE_DIMENSION || h > MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = (resolver.openInputStream(uri) ?: throw IOException("فایل تصویر قابل خواندن نیست")).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IOException("دیکود تصویر ناموفق بود")

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
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap) rawBitmap.recycle()
            rotated
        } else rawBitmap

        val finalBitmap = if (oriented.width > MAX_IMAGE_DIMENSION || oriented.height > MAX_IMAGE_DIMENSION) {
            val ratio = oriented.width.toFloat() / oriented.height.toFloat()
            val targetW: Int
            val targetH: Int
            if (ratio > 1f) {
                targetW = MAX_IMAGE_DIMENSION
                targetH = (MAX_IMAGE_DIMENSION / ratio).toInt().coerceAtLeast(1)
            } else {
                targetH = MAX_IMAGE_DIMENSION
                targetW = (MAX_IMAGE_DIMENSION * ratio).toInt().coerceAtLeast(1)
            }
            val scaled = Bitmap.createScaledBitmap(oriented, targetW, targetH, true)
            if (scaled != oriented) oriented.recycle()
            scaled
        } else oriented

        val file = File.createTempFile("post-media-", ".jpg", cacheDir)
        return try {
            file.outputStream().use { output ->
                if (!finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
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
            val bitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
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
        const val DEFAULT_MAX_BYTES = 15L * 1024 * 1024 // 15 MB for standard users
        const val PREMIUM_MAX_BYTES = 100L * 1024 * 1024 // 100 MB for premium users
        const val MAX_IMAGE_DIMENSION = 1440
        const val JPEG_QUALITY = 85
        const val JPEG_MIME = "image/jpeg"
        const val MP4_MIME = "video/mp4"
    }
}

private class ProgressRequestBody(
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
                if (total > 0) {
                    onProgress((sent.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                }
            }
        }
    }
}
