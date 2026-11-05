package ir.coffevista.vista_native.features.stories.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.coffevista.vista_native.features.feed.data.PostMediaUploadGateway
import ir.coffevista.vista_native.features.feed.music.MusicRepository
import ir.coffevista.vista_native.features.stories.domain.CreateStoryRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.ui.editor.StoryVideoUploadPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@HiltWorker
class StoryUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: StoryRepository,
    private val uploader: PostMediaUploadGateway,
    private val musicRepository: MusicRepository,
) : CoroutineWorker(context, params) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun doWork(): Result {
        val uploadedObjectKeys = mutableListOf<String>()
        var temporaryVideoToDelete: File? = null

        return try {
            val userId = inputData.getString(KEY_USER_ID)
                ?: return Result.failure(workDataOf(KEY_ERROR to "کاربر مشخص نشده است"))
            val mediaPath = inputData.getString(KEY_MEDIA_PATH)
            val mediaUriRaw = inputData.getString(KEY_MEDIA_URI)
                ?: return Result.failure(workDataOf(KEY_ERROR to "فایل رسانه مشخص نشده است"))
            val isVideo = inputData.getBoolean(KEY_IS_VIDEO, false)
            val durationType = inputData.getString(KEY_DURATION_TYPE) ?: "hours24"
            val privacyType = inputData.getString(KEY_PRIVACY_TYPE) ?: "everyone"
            val allowedUserIds = inputData.getStringArray(KEY_ALLOWED_USER_IDS)?.toList() ?: emptyList()
            val rawElementsJson = inputData.getString(KEY_ELEMENTS_JSON)
            val elementsFilePath = inputData.getString(KEY_ELEMENTS_FILE_PATH)
            val rawMusicUrl = inputData.getString(KEY_MUSIC_URL)
            val rawMusicTitle = inputData.getString(KEY_MUSIC_TITLE) ?: "موزیک"
            val rawMusicArtist = inputData.getString(KEY_MUSIC_ARTIST) ?: "موزیک انتخابی"
            val rawMusicCover = inputData.getString(KEY_MUSIC_COVER)
            var musicStartMs = inputData.getInt(KEY_MUSIC_START_MS, 0)
            var musicEndMs = inputData.getInt(KEY_MUSIC_END_MS, 0)
            val saveMusicToLibrary = inputData.getBoolean(KEY_SAVE_MUSIC_TO_LIB, false)
            val transformCx = inputData.getFloat(KEY_TRANSFORM_CX, 0.5f)
            val transformCy = inputData.getFloat(KEY_TRANSFORM_CY, 0.5f)
            val transformScale = inputData.getFloat(KEY_TRANSFORM_SCALE, 1f)
            val transformRotation = inputData.getFloat(KEY_TRANSFORM_ROTATION, 0f)

            updateProgressState(0.05f, "آماده‌سازی استوری…")

            val elements: List<StoryElement> = runCatching {
                val rawJson = if (!elementsFilePath.isNullOrBlank() && File(elementsFilePath).exists()) {
                    val content = File(elementsFilePath).readText()
                    runCatching { File(elementsFilePath).delete() }
                    content
                } else {
                    rawElementsJson
                }
                if (!rawJson.isNullOrBlank()) {
                    json.decodeFromString<List<StoryElement>>(rawJson)
                } else {
                    emptyList()
                }
            }.getOrDefault(emptyList())

            // 1. Upload local music track if provided
            var uploadedMusicRemoteUrl: String? = null
            var uploadedMusicCoverUrl: String? = null

            val activeMusicElem = elements.firstOrNull { it.type == "music" }?.music
            val effectiveMusicUrl = rawMusicUrl ?: activeMusicElem?.musicUrl
            val effectiveMusicTitle = rawMusicTitle.ifBlank { activeMusicElem?.title ?: "موزیک" }
            val effectiveMusicArtist = rawMusicArtist.ifBlank { activeMusicElem?.artist ?: "موزیک انتخابی" }
            val effectiveMusicCover = rawMusicCover ?: activeMusicElem?.coverUrl

            if (!effectiveMusicUrl.isNullOrBlank() &&
                (effectiveMusicUrl.startsWith("content://", ignoreCase = true) ||
                    effectiveMusicUrl.startsWith("file://", ignoreCase = true))
            ) {
                updateProgressState(0.12f, "در حال پردازش و آپلود موزیک انتخابی…")
                if (!effectiveMusicCover.isNullOrBlank() &&
                    (effectiveMusicCover.startsWith("content://", ignoreCase = true) ||
                        effectiveMusicCover.startsWith("file://", ignoreCase = true))
                ) {
                    uploadedMusicCoverUrl = runCatching {
                        val coverRes = uploader.uploadImage(userId, Uri.parse(effectiveMusicCover))
                        uploadedObjectKeys.add(coverRes.objectKey)
                        coverRes.url
                    }.getOrNull()
                }

                val audioUploaded = uploader.uploadAudio(
                    userId = userId,
                    uri = Uri.parse(effectiveMusicUrl),
                    maxBytes = 30L * 1024 * 1024,
                    startMs = musicStartMs,
                    endMs = musicEndMs,
                ) { p ->
                    throttleProgress(0.12f + p * 0.25f, "در حال آپلود موزیک انتخابی…")
                }
                uploadedObjectKeys.add(audioUploaded.objectKey)
                uploadedMusicRemoteUrl = audioUploaded.url
                if (audioUploaded.wasTrimmed) {
                    musicStartMs = 0
                    musicEndMs = 0
                }
                if (saveMusicToLibrary) {
                    runCatching {
                        musicRepository.publishMusic(
                            title = effectiveMusicTitle,
                            artist = effectiveMusicArtist,
                            musicUrl = audioUploaded.url,
                            coverUrl = uploadedMusicCoverUrl,
                        )
                    }
                }
            }

            // 2. Upload photo cutout elements if local URI
            val uploadedElements = elements.map { element ->
                when (element.type) {
                    "photo" -> {
                        val rawUriStr = element.content.orEmpty()
                        if (rawUriStr.startsWith("content://", ignoreCase = true) ||
                            rawUriStr.startsWith("file://", ignoreCase = true)
                        ) {
                            updateProgressState(0.40f, "در حال آپلود برش تصویر…")
                            val cutoutRes = uploader.uploadImage(userId, Uri.parse(rawUriStr))
                            uploadedObjectKeys.add(cutoutRes.objectKey)
                            element.copy(content = cutoutRes.url)
                        } else {
                            element
                        }
                    }
                    "music" -> {
                        val currentMusic = element.music
                        if (currentMusic != null && uploadedMusicRemoteUrl != null) {
                            element.copy(
                                music = currentMusic.copy(
                                    musicUrl = uploadedMusicRemoteUrl,
                                    coverUrl = uploadedMusicCoverUrl ?: currentMusic.coverUrl?.takeIf {
                                        it.startsWith("http://", ignoreCase = true) ||
                                            it.startsWith("https://", ignoreCase = true)
                                    },
                                    startMs = musicStartMs,
                                    endMs = musicEndMs,
                                ),
                            )
                        } else {
                            element
                        }
                    }
                    else -> element
                }
            }

            // 3. Upload Primary Media
            val baseProgressOffset = if (uploadedMusicRemoteUrl != null) 0.40f else 0.15f
            val baseProgressSpan = if (uploadedMusicRemoteUrl != null) 0.48f else 0.65f

            val uploadResult = if (isVideo) {
                updateProgressState(baseProgressOffset, "در حال آپلود ویدیو…")
                val videoFile = if (!mediaPath.isNullOrBlank() && File(mediaPath).exists()) {
                    File(mediaPath)
                } else {
                    val uri = Uri.parse(mediaUriRaw)
                    val temp = File.createTempFile("story_video_work_", ".mp4", context.cacheDir)
                    temporaryVideoToDelete = temp
                    val inStream = if (uri.scheme.equals("file", ignoreCase = true)) {
                        val f = File(uri.path ?: uri.toString().removePrefix("file://"))
                        if (f.exists()) f.inputStream() else context.contentResolver.openInputStream(uri)
                    } else {
                        context.contentResolver.openInputStream(uri)
                    }
                    inStream?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw java.io.IOException("امکان خواندن فایل ویدیو وجود ندارد")
                    temp
                }
                StoryVideoUploadPolicy.validateByteCount(videoFile.length())
                val res = uploader.uploadVideo(
                    userId = userId,
                    videoFile = videoFile,
                    maxBytes = StoryVideoUploadPolicy.MaxBytes,
                    onProgress = { p ->
                        throttleProgress(baseProgressOffset + p * baseProgressSpan, "در حال آپلود ویدیو…")
                    },
                )
                uploadedObjectKeys.add(res.objectKey)
                res
            } else {
                updateProgressState(baseProgressOffset, "در حال پردازش و آپلود تصویر…")
                val effectiveUri = if (!mediaPath.isNullOrBlank() && File(mediaPath).exists()) {
                    val file = File(mediaPath)
                    runCatching {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.files",
                            file,
                        )
                    }.getOrElse {
                        Uri.fromFile(file)
                    }
                } else {
                    Uri.parse(mediaUriRaw)
                }

                val res = uploader.uploadImage(
                    userId = userId,
                    uri = effectiveUri,
                    onProgress = { p ->
                        throttleProgress(baseProgressOffset + p * baseProgressSpan, "در حال پردازش و آپلود تصویر…")
                    },
                )
                uploadedObjectKeys.add(res.objectKey)
                res
            }

            // 4. Create Story API call
            updateProgressState(0.92f, "در حال انتشار استوری…")

            val resolvedMusicUrl = uploadedMusicRemoteUrl
                ?: uploadedElements.firstOrNull { it.type == "music" }?.music?.musicUrl
                ?: rawMusicUrl?.takeIf {
                    it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
                }
            val resolvedMusicTitle = if (!effectiveMusicTitle.isNullOrBlank()) {
                "$effectiveMusicTitle — $effectiveMusicArtist"
            } else null

            val finalInteractiveElements = if (isVideo) {
                val hasVideoTransform = kotlin.math.abs(transformCx - 0.5f) > 0.004f ||
                    kotlin.math.abs(transformCy - 0.5f) > 0.004f ||
                    kotlin.math.abs(transformScale - 1f) > 0.008f ||
                    kotlin.math.abs(transformRotation) > 0.15f
                val base = uploadedElements.filterNot { it.type == "media_transform" }
                if (hasVideoTransform) {
                    base + StoryElement(
                        id = "__media_transform__",
                        type = "media_transform",
                        schema = 2,
                        xNorm = transformCx,
                        yNorm = transformCy,
                        scale = transformScale,
                        rotation = transformRotation,
                    )
                } else {
                    base
                }
            } else {
                uploadedElements.filterNot { it.type == "media_transform" }
            }

            val createReq = CreateStoryRequestDto(
                mediaUrl = uploadResult.url,
                mediaType = if (isVideo) "video" else "image",
                thumbnailUrl = uploadResult.thumbnailUrl,
                durationType = durationType,
                privacyType = privacyType,
                allowedUserIds = allowedUserIds,
                interactiveElements = finalInteractiveElements,
                musicUrl = resolvedMusicUrl,
                musicTitle = resolvedMusicTitle,
            )

            val createRes = repository.createStory(createReq)
            if (createRes.isSuccess) {
                val createdStory = createRes.getOrNull()
                temporaryVideoToDelete?.delete()
                if (!mediaPath.isNullOrBlank()) {
                    runCatching { File(mediaPath).delete() }
                }
                if (!effectiveMusicUrl.isNullOrBlank() && effectiveMusicUrl.startsWith("file://", ignoreCase = true)) {
                    val p = Uri.parse(effectiveMusicUrl).path
                    if (!p.isNullOrBlank() && p.contains("story_uploads")) {
                        runCatching { File(p).delete() }
                    }
                }
                updateProgressState(1f, "استوری با موفقیت منتشر شد")
                Result.success(
                    workDataOf(
                        KEY_PROGRESS to 1f,
                        KEY_STATUS to "استوری با موفقیت منتشر شد",
                        KEY_STORY_ID to (createdStory?.id ?: ""),
                    )
                )
            } else {
                val err = createRes.exceptionOrNull()?.message ?: "خطا در ایجاد استوری"
                cleanupOrphanFiles(uploadedObjectKeys)
                // Preserve local media and music cache files so StoryUploadManager.retry() can re-read them
                Result.failure(workDataOf(KEY_ERROR to err))
            }
        } catch (e: CancellationException) {
            cleanupOrphanFiles(uploadedObjectKeys)
            temporaryVideoToDelete?.delete()
            throw e
        } catch (e: Exception) {
            cleanupOrphanFiles(uploadedObjectKeys)
            // Preserve local files on error for retry
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "خطا در انتشار استوری")))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0.05f, "در حال آماده‌سازی استوری…")
    }

    private var lastProgressUpdateTime = 0L
    private var lastProgressValue = 0f

    private suspend fun updateProgressState(progress: Float, message: String) {
        runCatching {
            setProgress(
                workDataOf(
                    KEY_PROGRESS to progress,
                    KEY_STATUS to message,
                )
            )
        }
        runCatching {
            setForeground(createForegroundInfo(progress, message))
        }
    }

    private fun throttleProgress(progress: Float, message: String) {
        val now = System.currentTimeMillis()
        if (progress >= 1f || kotlin.math.abs(progress - lastProgressValue) >= 0.04f || (now - lastProgressUpdateTime) >= 300L) {
            lastProgressUpdateTime = now
            lastProgressValue = progress
            runCatching {
                kotlinx.coroutines.runBlocking {
                    updateProgressState(progress, message)
                }
            }
        }
    }

    private fun createForegroundInfo(progress: Float, message: String): ForegroundInfo {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "انتشار استوری",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "نمایش وضعیت آپلود و انتشار استوری‌های جدید"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("انتشار استوری در ویستا")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(progress < 1f)
            .setProgress(100, (progress * 100).toInt(), false)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun cleanupOrphanFiles(keys: List<String>) {
        keys.forEach { key ->
            runCatching { uploader.deleteUpload(key) }
        }
    }

    companion object {
        const val WORK_NAME = "ir.coffevista.vista_native.STORY_UPLOAD"
        const val WORK_TAG = "story_upload_worker"
        const val CHANNEL_ID = "story_uploads"
        const val NOTIFICATION_ID = 4002

        const val KEY_USER_ID = "user_id"
        const val KEY_MEDIA_PATH = "media_path"
        const val KEY_MEDIA_URI = "media_uri"
        const val KEY_IS_VIDEO = "is_video"
        const val KEY_DURATION_TYPE = "duration_type"
        const val KEY_PRIVACY_TYPE = "privacy_type"
        const val KEY_ALLOWED_USER_IDS = "allowed_user_ids"
        const val KEY_ELEMENTS_JSON = "elements_json"
        const val KEY_ELEMENTS_FILE_PATH = "elements_file_path"
        const val KEY_MUSIC_URL = "music_url"
        const val KEY_MUSIC_TITLE = "music_title"
        const val KEY_MUSIC_ARTIST = "music_artist"
        const val KEY_MUSIC_COVER = "music_cover"
        const val KEY_MUSIC_START_MS = "music_start_ms"
        const val KEY_MUSIC_END_MS = "music_end_ms"
        const val KEY_SAVE_MUSIC_TO_LIB = "save_music_to_lib"
        const val KEY_TRANSFORM_CX = "transform_cx"
        const val KEY_TRANSFORM_CY = "transform_cy"
        const val KEY_TRANSFORM_SCALE = "transform_scale"
        const val KEY_TRANSFORM_ROTATION = "transform_rotation"

        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        const val KEY_ERROR = "error"
        const val KEY_STORY_ID = "story_id"
    }
}
