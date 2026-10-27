package ir.coffevista.vista_native.features.feed.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.database.feed.FeedDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PendingPostUpload(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val content: String = "",
    val images: List<Uri> = emptyList(),
    val videoFile: File? = null,
    val videoUri: Uri? = null,
    val isVideo: Boolean = false,
    val aspectRatio: String = "1:1",
    val location: String? = null,
    val musicUri: Uri? = null,
    val musicUrl: String? = null,
    val musicTitle: String? = null,
    val musicStartMs: Int = 0,
    val musicEndMs: Int = 0,
    val hideLikeCount: Boolean = false,
    val hideCommentCount: Boolean = false,
    val commentsDisabled: Boolean = false,
    val maxUploadBytes: Long = 15L * 1024 * 1024,
)

data class PostUploadState(
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val pendingUpload: PendingPostUpload? = null,
)

@Singleton
class PostUploadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: FeedApi,
    private val uploader: PostMediaUploadGateway,
    private val feedDao: FeedDao,
    private val postMentions: PostMentions,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var uploadJob: Job? = null

    private val _uploadState = MutableStateFlow(PostUploadState())
    val uploadState: StateFlow<PostUploadState> = _uploadState.asStateFlow()

    fun startUpload(upload: PendingPostUpload) {
        uploadJob?.cancel()
        _uploadState.update {
            PostUploadState(
                isUploading = true,
                progress = 0.05f,
                statusMessage = "آماده‌سازی فایل‌ها…",
                pendingUpload = upload,
            )
        }

        uploadJob = scope.launch {
            val uploadedObjectKeys = mutableListOf<String>()
            try {
                var finalVideoUrl: String? = null
                var finalThumbUrl: String? = null
                val finalImageUrls = mutableListOf<String>()

                // 1. Music upload if selected
                val finalMusicUrl = upload.musicUri?.let { audioUri ->
                    _uploadState.update { it.copy(statusMessage = "در حال آپلود موسیقی…") }
                    val result = uploader.uploadAudio(
                        userId = upload.userId,
                        uri = audioUri,
                        maxBytes = upload.maxUploadBytes,
                        startMs = upload.musicStartMs,
                        endMs = upload.musicEndMs,
                        onProgress = { p ->
                            _uploadState.update { it.copy(progress = 0.05f + p * 0.2f) }
                        },
                    )
                    uploadedObjectKeys.add(result.objectKey)
                    result.url
                } ?: upload.musicUrl

                // 2. Video upload if present
                if (upload.isVideo && (upload.videoFile != null || upload.videoUri != null)) {
                    _uploadState.update { it.copy(statusMessage = "در حال آپلود ویدیو…") }
                    val videoFile = upload.videoFile ?: run {
                        val uri = upload.videoUri ?: throw java.io.IOException("فایل ویدیو یافت نشد")
                        val temp = File(context.cacheDir, "bg_upload_video_${System.currentTimeMillis()}.mp4")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            temp.outputStream().use { output -> input.copyTo(output) }
                        } ?: throw java.io.IOException("امکان خواندن فایل ویدیو وجود ندارد")
                        temp
                    }
                    val result = uploader.uploadVideo(
                        userId = upload.userId,
                        videoFile = videoFile,
                        maxBytes = upload.maxUploadBytes,
                        onProgress = { p ->
                            _uploadState.update { it.copy(progress = 0.1f + p * 0.7f) }
                        },
                    )
                    uploadedObjectKeys.add(result.objectKey)
                    finalVideoUrl = result.url
                    finalThumbUrl = result.thumbnailUrl
                }
                // 3. Images upload if present
                else if (upload.images.isNotEmpty()) {
                    val total = upload.images.size
                    upload.images.forEachIndexed { index, uri ->
                        _uploadState.update {
                            it.copy(statusMessage = "در حال آپلود تصویر (${index + 1} از $total)…")
                        }
                        val result = uploader.uploadImage(
                            userId = upload.userId,
                            uri = uri,
                            maxBytes = upload.maxUploadBytes,
                            onProgress = { p ->
                                val baseProgress = 0.1f + (index.toFloat() / total) * 0.7f
                                val currentItemProgress = (p / total) * 0.7f
                                _uploadState.update { it.copy(progress = baseProgress + currentItemProgress) }
                            },
                        )
                        uploadedObjectKeys.add(result.objectKey)
                        finalImageUrls.add(result.url)
                    }
                }

                // 4. Extract mentions & hashtags
                val hashtags = extractHashtags(upload.content)
                val mentionUsernames = extractMentions(upload.content)
                val mentionIds = postMentions.resolve(mentionUsernames)

                _uploadState.update {
                    it.copy(
                        progress = 0.9f,
                        statusMessage = "در حال انتشار پست…",
                    )
                }

                val createReq = CreatePostRequestDto(
                    content = upload.content.trim().ifBlank { null },
                    imageUrl = finalImageUrls.firstOrNull() ?: finalThumbUrl,
                    imageUrls = finalImageUrls,
                    videoUrl = finalVideoUrl,
                    musicUrl = finalMusicUrl,
                    musicTitle = upload.musicTitle,
                    musicStartMs = upload.musicStartMs.takeIf { finalMusicUrl != null },
                    musicEndMs = upload.musicEndMs.takeIf { finalMusicUrl != null },
                    aspectRatio = upload.aspectRatio,
                    tags = hashtags,
                    location = upload.location,
                    hideLikeCount = upload.hideLikeCount,
                    hideCommentCount = upload.hideCommentCount,
                    commentsDisabled = upload.commentsDisabled,
                )

                val postDto = api.createPost(createReq)
                postMentions.attach(postDto.id, mentionIds)

                // Insert into local cache for immediate visibility
                val userId = upload.userId
                if (userId.isNotBlank()) {
                    runCatching {
                        val exploreEntity = postDto.asEntity(accountId = userId, sortOrder = -1L)
                        val followingEntity = postDto.asEntity(accountId = "$userId\u001Ffollowing", sortOrder = -1L)
                        feedDao.insertPosts(listOf(exploreEntity, followingEntity))
                    }
                }

                _uploadState.update {
                    it.copy(
                        isUploading = false,
                        progress = 1f,
                        statusMessage = "پست با موفقیت منتشر شد",
                        isSuccess = true,
                        errorMessage = null,
                    )
                }

                // Dismiss success banner after 3 seconds
                delay(3000L)
                _uploadState.update { current ->
                    if (current.isSuccess) current.copy(isSuccess = false, pendingUpload = null)
                    else current
                }
            } catch (cancelled: CancellationException) {
                cleanupOrphanFiles(uploadedObjectKeys)
                _uploadState.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = "ارسال پست لغو شد",
                    )
                }
                throw cancelled
            } catch (e: Throwable) {
                cleanupOrphanFiles(uploadedObjectKeys)
                _uploadState.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = e.message ?: "خطا در ارسال پست",
                    )
                }
            }
        }
    }

    private suspend fun cleanupOrphanFiles(keys: List<String>) {
        keys.forEach { key ->
            runCatching { uploader.deleteUpload(key) }
        }
    }

    fun retry() {
        val pending = _uploadState.value.pendingUpload ?: return
        startUpload(pending)
    }

    fun cancel() {
        uploadJob?.cancel()
        _uploadState.update { PostUploadState() }
    }

    fun dismiss() {
        _uploadState.update { it.copy(errorMessage = null, isSuccess = false) }
    }

    private fun extractHashtags(text: String): List<String> {
        val regex = Regex("(?<![^\\s\\n])#([\\p{L}\\p{N}_]+)")
        return regex.findAll(text).map { it.groupValues[1] }.distinct().toList()
    }

    private fun extractMentions(text: String): List<String> {
        val regex = Regex("(?<![^\\s\\n])@([\\p{L}\\p{N}_]+)")
        return regex.findAll(text).map { it.groupValues[1] }.distinct().toList()
    }
}
