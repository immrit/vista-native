package ir.coffevista.vista_native.features.feed.data

import android.content.Context
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.database.feed.FeedDao
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
    private val workManager by lazy { WorkManager.getInstance(context) }
    private var observerJob: Job? = null

    private val _uploadState = MutableStateFlow(PostUploadState())
    val uploadState: StateFlow<PostUploadState> = _uploadState.asStateFlow()

    init {
        observeCurrentWork()
    }

    private fun observeCurrentWork() {
        observerJob?.cancel()
        observerJob = scope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(PostUploadWorker.WORK_NAME).collect { workInfos ->
                val info = workInfos.firstOrNull() ?: return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = info.progress.getFloat(PostUploadWorker.KEY_PROGRESS, 0.1f)
                        val status = info.progress.getString(PostUploadWorker.KEY_STATUS) ?: "در حال ارسال پست…"
                        _uploadState.update { current ->
                            current.copy(
                                isUploading = true,
                                progress = progress,
                                statusMessage = status,
                                errorMessage = null,
                            )
                        }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        _uploadState.update { current ->
                            current.copy(
                                isUploading = false,
                                progress = 1f,
                                statusMessage = "پست با موفقیت منتشر شد",
                                isSuccess = true,
                                errorMessage = null,
                            )
                        }
                        delay(3000L)
                        _uploadState.update { current ->
                            if (current.isSuccess) current.copy(isSuccess = false, pendingUpload = null)
                            else current
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val error = info.outputData.getString(PostUploadWorker.KEY_ERROR) ?: "خطا در ارسال پست"
                        _uploadState.update { current ->
                            current.copy(
                                isUploading = false,
                                errorMessage = error,
                            )
                        }
                    }
                    WorkInfo.State.CANCELLED -> {
                        _uploadState.update { current ->
                            current.copy(
                                isUploading = false,
                                errorMessage = "ارسال پست لغو شد",
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun startUpload(upload: PendingPostUpload) {
        _uploadState.update {
            PostUploadState(
                isUploading = true,
                progress = 0.05f,
                statusMessage = "آماده‌سازی فایل‌ها…",
                pendingUpload = upload,
            )
        }

        val inputData = workDataOf(
            PostUploadWorker.KEY_USER_ID to upload.userId,
            PostUploadWorker.KEY_CONTENT to upload.content,
            PostUploadWorker.KEY_IMAGE_URIS to upload.images.map { it.toString() }.toTypedArray(),
            PostUploadWorker.KEY_VIDEO_PATH to upload.videoFile?.absolutePath,
            PostUploadWorker.KEY_VIDEO_URI to upload.videoUri?.toString(),
            PostUploadWorker.KEY_IS_VIDEO to upload.isVideo,
            PostUploadWorker.KEY_ASPECT_RATIO to upload.aspectRatio,
            PostUploadWorker.KEY_LOCATION to upload.location,
            PostUploadWorker.KEY_MUSIC_URI to upload.musicUri?.toString(),
            PostUploadWorker.KEY_MUSIC_URL to upload.musicUrl,
            PostUploadWorker.KEY_MUSIC_TITLE to upload.musicTitle,
            PostUploadWorker.KEY_MUSIC_START_MS to upload.musicStartMs,
            PostUploadWorker.KEY_MUSIC_END_MS to upload.musicEndMs,
            PostUploadWorker.KEY_HIDE_LIKE_COUNT to upload.hideLikeCount,
            PostUploadWorker.KEY_HIDE_COMMENT_COUNT to upload.hideCommentCount,
            PostUploadWorker.KEY_COMMENTS_DISABLED to upload.commentsDisabled,
            PostUploadWorker.KEY_MAX_BYTES to upload.maxUploadBytes,
        )

        val request = OneTimeWorkRequestBuilder<PostUploadWorker>()
            .setInputData(inputData)
            .addTag(PostUploadWorker.WORK_TAG)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            PostUploadWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun retry() {
        val pending = _uploadState.value.pendingUpload ?: return
        startUpload(pending)
    }

    fun cancel() {
        workManager.cancelUniqueWork(PostUploadWorker.WORK_NAME)
        _uploadState.update { PostUploadState() }
    }

    fun dismiss() {
        _uploadState.update { it.copy(errorMessage = null, isSuccess = false) }
    }
}
