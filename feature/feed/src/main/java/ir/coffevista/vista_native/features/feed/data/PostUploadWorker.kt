package ir.coffevista.vista_native.features.feed.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.coffevista.vista_native.core.database.feed.FeedDao
import kotlinx.coroutines.CancellationException
import java.io.File

@HiltWorker
class PostUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val api: FeedApi,
    private val uploader: PostMediaUploadGateway,
    private val feedDao: FeedDao,
    private val postMentions: PostMentions,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uploadedObjectKeys = mutableListOf<String>()

        return try {
            val userId = inputData.getString(KEY_USER_ID)
                ?: return Result.failure(workDataOf(KEY_ERROR to "کاربر مشخص نشده است"))
            val content = inputData.getString(KEY_CONTENT).orEmpty()
            val imageUrisRaw = inputData.getStringArray(KEY_IMAGE_URIS)?.toList().orEmpty()
            val videoPath = inputData.getString(KEY_VIDEO_PATH)
            val videoUriRaw = inputData.getString(KEY_VIDEO_URI)
            val isVideo = inputData.getBoolean(KEY_IS_VIDEO, false)
            val aspectRatio = inputData.getString(KEY_ASPECT_RATIO) ?: "1:1"
            val location = inputData.getString(KEY_LOCATION)
            val musicUriRaw = inputData.getString(KEY_MUSIC_URI)
            val musicUrl = inputData.getString(KEY_MUSIC_URL)
            val musicTitle = inputData.getString(KEY_MUSIC_TITLE)
            val musicStartMs = inputData.getInt(KEY_MUSIC_START_MS, 0)
            val musicEndMs = inputData.getInt(KEY_MUSIC_END_MS, 0)
            val hideLikeCount = inputData.getBoolean(KEY_HIDE_LIKE_COUNT, false)
            val hideCommentCount = inputData.getBoolean(KEY_HIDE_COMMENT_COUNT, false)
            val commentsDisabled = inputData.getBoolean(KEY_COMMENTS_DISABLED, false)
            val maxUploadBytes = inputData.getLong(KEY_MAX_BYTES, 15L * 1024 * 1024)

            updateProgressState(0.05f, "آماده‌سازی فایل‌ها…")

            var finalVideoUrl: String? = null
            var finalThumbUrl: String? = null
            val finalImageUrls = mutableListOf<String>()

            // 1. Audio / Music upload
            val finalMusicUrl = if (!musicUriRaw.isNullOrBlank()) {
                val audioUri = Uri.parse(musicUriRaw)
                updateProgressState(0.08f, "در حال آپلود موسیقی…")
                val result = uploader.uploadAudio(
                    userId = userId,
                    uri = audioUri,
                    maxBytes = maxUploadBytes,
                    startMs = musicStartMs,
                    endMs = musicEndMs,
                    onProgress = { p ->
                        kotlinx.coroutines.runBlocking {
                            updateProgressState(0.05f + p * 0.2f, "در حال آپلود موسیقی…")
                        }
                    },
                )
                uploadedObjectKeys.add(result.objectKey)
                result.url
            } else {
                musicUrl
            }

            // 2. Video upload
            if (isVideo && (!videoPath.isNullOrBlank() || !videoUriRaw.isNullOrBlank())) {
                updateProgressState(0.25f, "در حال آپلود ویدیو…")
                val videoFile = if (!videoPath.isNullOrBlank() && File(videoPath).exists()) {
                    File(videoPath)
                } else {
                    val uri = Uri.parse(videoUriRaw!!)
                    val temp = File(context.cacheDir, "work_upload_video_${System.currentTimeMillis()}.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw java.io.IOException("امکان خواندن فایل ویدیو وجود ندارد")
                    temp
                }

                val result = uploader.uploadVideo(
                    userId = userId,
                    videoFile = videoFile,
                    maxBytes = maxUploadBytes,
                    onProgress = { p ->
                        kotlinx.coroutines.runBlocking {
                            updateProgressState(0.25f + p * 0.6f, "در حال آپلود ویدیو…")
                        }
                    },
                )
                uploadedObjectKeys.add(result.objectKey)
                finalVideoUrl = result.url
                finalThumbUrl = result.thumbnailUrl
            }
            // 3. Images upload
            else if (imageUrisRaw.isNotEmpty()) {
                val total = imageUrisRaw.size
                imageUrisRaw.forEachIndexed { index, uriStr ->
                    updateProgressState(
                        0.25f + (index.toFloat() / total) * 0.6f,
                        "در حال آپلود تصویر (${index + 1} از $total)…",
                    )
                    val uri = Uri.parse(uriStr)
                    val result = uploader.uploadImage(
                        userId = userId,
                        uri = uri,
                        maxBytes = maxUploadBytes,
                        onProgress = { p ->
                            val baseProgress = 0.25f + (index.toFloat() / total) * 0.6f
                            val currentItemProgress = (p / total) * 0.6f
                            kotlinx.coroutines.runBlocking {
                                updateProgressState(
                                    baseProgress + currentItemProgress,
                                    "در حال آپلود تصویر (${index + 1} از $total)…",
                                )
                            }
                        },
                    )
                    uploadedObjectKeys.add(result.objectKey)
                    finalImageUrls.add(result.url)
                }
            }

            // 4. Extract mentions & hashtags
            val hashtags = extractHashtags(content)
            val mentionUsernames = extractMentions(content)
            val mentionIds = postMentions.resolve(mentionUsernames)

            updateProgressState(0.9f, "در حال انتشار پست…")

            val createReq = CreatePostRequestDto(
                content = content.trim().ifBlank { null },
                imageUrl = finalImageUrls.firstOrNull() ?: finalThumbUrl,
                imageUrls = finalImageUrls,
                videoUrl = finalVideoUrl,
                musicUrl = finalMusicUrl,
                musicTitle = musicTitle,
                musicStartMs = musicStartMs.takeIf { finalMusicUrl != null },
                musicEndMs = musicEndMs.takeIf { finalMusicUrl != null },
                aspectRatio = aspectRatio,
                tags = hashtags,
                location = location,
                hideLikeCount = hideLikeCount,
                hideCommentCount = hideCommentCount,
                commentsDisabled = commentsDisabled,
            )

            val postDto = api.createPost(createReq)
            postMentions.attach(postDto.id, mentionIds)

            // Cache locally for instant appearance
            if (userId.isNotBlank()) {
                runCatching {
                    val exploreEntity = postDto.asEntity(accountId = userId, sortOrder = -1L)
                    val followingEntity = postDto.asEntity(accountId = "$userId\u001Ffollowing", sortOrder = -1L)
                    feedDao.insertPosts(listOf(exploreEntity, followingEntity))
                }
            }

            updateProgressState(1f, "پست با موفقیت منتشر شد")
            Result.success(workDataOf(KEY_POST_ID to postDto.id))
        } catch (cancelled: CancellationException) {
            cleanupOrphanFiles(uploadedObjectKeys)
            Result.failure(workDataOf(KEY_ERROR to "ارسال پست لغو شد"))
        } catch (e: Throwable) {
            cleanupOrphanFiles(uploadedObjectKeys)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "خطا در ارسال پست")))
        }
    }

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

    private fun createForegroundInfo(progress: Float, message: String): ForegroundInfo {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ارسال پست",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "نمایش پیشرفت آپلود پست‌های ارسالی"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("ارسال پست در ویستا")
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

    private fun extractHashtags(text: String): List<String> {
        val regex = Regex("(?<![^\\s\\n])#([\\p{L}\\p{N}_]+)")
        return regex.findAll(text).map { it.groupValues[1] }.distinct().toList()
    }

    private fun extractMentions(text: String): List<String> {
        val regex = Regex("(?<![^\\s\\n])@([\\p{L}\\p{N}_]+)")
        return regex.findAll(text).map { it.groupValues[1] }.distinct().toList()
    }

    companion object {
        const val WORK_NAME = "ir.coffevista.vista_native.POST_UPLOAD"
        const val WORK_TAG = "post_upload_worker"
        const val CHANNEL_ID = "post_uploads"
        const val NOTIFICATION_ID = 4001

        const val KEY_USER_ID = "user_id"
        const val KEY_CONTENT = "content"
        const val KEY_IMAGE_URIS = "image_uris"
        const val KEY_VIDEO_PATH = "video_path"
        const val KEY_VIDEO_URI = "video_uri"
        const val KEY_IS_VIDEO = "is_video"
        const val KEY_ASPECT_RATIO = "aspect_ratio"
        const val KEY_LOCATION = "location"
        const val KEY_MUSIC_URI = "music_uri"
        const val KEY_MUSIC_URL = "music_url"
        const val KEY_MUSIC_TITLE = "music_title"
        const val KEY_MUSIC_START_MS = "music_start_ms"
        const val KEY_MUSIC_END_MS = "music_end_ms"
        const val KEY_HIDE_LIKE_COUNT = "hide_like_count"
        const val KEY_HIDE_COMMENT_COUNT = "hide_comment_count"
        const val KEY_COMMENTS_DISABLED = "comments_disabled"
        const val KEY_MAX_BYTES = "max_bytes"

        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        const val KEY_ERROR = "error"
        const val KEY_POST_ID = "post_id"
    }
}
