package ir.coffevista.vista_native.features.stories.data

import android.content.Context
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.features.feed.music.MusicTrack
import ir.coffevista.vista_native.features.stories.domain.StoryDurationType
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryTransform
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PendingStoryUpload(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val mediaUri: Uri? = null,
    val mediaFile: File? = null,
    val isVideo: Boolean = false,
    val mediaTransform: StoryTransform = StoryTransform(cx = 0.5f, cy = 0.5f, scale = 1f, rotation = 0f),
    val elements: List<StoryElement> = emptyList(),
    val musicTrack: MusicTrack? = null,
    val musicStartMs: Int = 0,
    val musicEndMs: Int = 0,
    val saveMusicToLibrary: Boolean = false,
    val durationType: StoryDurationType = StoryDurationType.Hours24,
    val privacyType: StoryPrivacyType = StoryPrivacyType.Everyone,
    val selectedCloseFriendIds: List<String> = emptyList(),
)

data class StoryUploadState(
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val pendingUpload: PendingStoryUpload? = null,
)

@Singleton
class StoryUploadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StoryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workManager by lazy { WorkManager.getInstance(context) }
    private var observerJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _uploadState = MutableStateFlow(StoryUploadState())
    val uploadState: StateFlow<StoryUploadState> = _uploadState.asStateFlow()

    init {
        observeCurrentWork()
    }

    private fun observeCurrentWork() {
        observerJob?.cancel()
        observerJob = scope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(StoryUploadWorker.WORK_NAME).collect { workInfos ->
                val info = workInfos.firstOrNull() ?: return@collect
                when (info.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = info.progress.getFloat(StoryUploadWorker.KEY_PROGRESS, 0.1f)
                        val status = info.progress.getString(StoryUploadWorker.KEY_STATUS) ?: "در حال انتشار استوری…"
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
                                statusMessage = "استوری با موفقیت منتشر شد",
                                isSuccess = true,
                                errorMessage = null,
                            )
                        }
                        repository.refreshActiveStories()
                        delay(3000L)
                        _uploadState.update { current ->
                            if (current.isSuccess) current.copy(isSuccess = false, pendingUpload = null)
                            else current
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val error = info.outputData.getString(StoryUploadWorker.KEY_ERROR) ?: "خطا در انتشار استوری"
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
                                errorMessage = "انتشار استوری لغو شد",
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun startUpload(upload: PendingStoryUpload) {
        _uploadState.update {
            StoryUploadState(
                isUploading = true,
                progress = 0.05f,
                statusMessage = "در حال آماده‌سازی استوری…",
                pendingUpload = upload,
            )
        }

        val elementsJson = json.encodeToString(upload.elements)
        val elementsFilePath = if (elementsJson.toByteArray().size > 3000) {
            val file = File(context.cacheDir, "story_elements_${upload.id}.json")
            runCatching {
                file.writeText(elementsJson)
                file.absolutePath
            }.getOrNull()
        } else null

        val inputData = workDataOf(
            StoryUploadWorker.KEY_USER_ID to upload.userId,
            StoryUploadWorker.KEY_MEDIA_PATH to upload.mediaFile?.absolutePath,
            StoryUploadWorker.KEY_MEDIA_URI to upload.mediaUri.toString(),
            StoryUploadWorker.KEY_IS_VIDEO to upload.isVideo,
            StoryUploadWorker.KEY_DURATION_TYPE to if (upload.durationType == StoryDurationType.Hours48) "hours48" else "hours24",
            StoryUploadWorker.KEY_PRIVACY_TYPE to when (upload.privacyType) {
                StoryPrivacyType.CloseFriends -> "close_friends"
                StoryPrivacyType.Followers -> "followers"
                StoryPrivacyType.Custom -> "custom"
                else -> "everyone"
            },
            StoryUploadWorker.KEY_ALLOWED_USER_IDS to upload.selectedCloseFriendIds.toTypedArray(),
            StoryUploadWorker.KEY_ELEMENTS_JSON to (if (elementsFilePath == null) elementsJson else null),
            StoryUploadWorker.KEY_ELEMENTS_FILE_PATH to elementsFilePath,
            StoryUploadWorker.KEY_MUSIC_URL to (upload.musicTrack?.musicUrl),
            StoryUploadWorker.KEY_MUSIC_TITLE to (upload.musicTrack?.title),
            StoryUploadWorker.KEY_MUSIC_ARTIST to (upload.musicTrack?.artist),
            StoryUploadWorker.KEY_MUSIC_COVER to (upload.musicTrack?.coverUrl),
            StoryUploadWorker.KEY_MUSIC_START_MS to upload.musicStartMs,
            StoryUploadWorker.KEY_MUSIC_END_MS to upload.musicEndMs,
            StoryUploadWorker.KEY_SAVE_MUSIC_TO_LIB to upload.saveMusicToLibrary,
            StoryUploadWorker.KEY_TRANSFORM_CX to upload.mediaTransform.cx,
            StoryUploadWorker.KEY_TRANSFORM_CY to upload.mediaTransform.cy,
            StoryUploadWorker.KEY_TRANSFORM_SCALE to upload.mediaTransform.scale,
            StoryUploadWorker.KEY_TRANSFORM_ROTATION to upload.mediaTransform.rotation,
        )

        runCatching {
            val request = OneTimeWorkRequestBuilder<StoryUploadWorker>()
                .setInputData(inputData)
                .addTag(StoryUploadWorker.WORK_TAG)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            workManager.enqueueUniqueWork(
                StoryUploadWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }.onFailure { err ->
            _uploadState.update {
                it.copy(
                    isUploading = false,
                    errorMessage = err.message ?: "خطا در شروع ارسال استوری",
                )
            }
        }
    }

    fun retry() {
        val pending = _uploadState.value.pendingUpload ?: return
        startUpload(pending)
    }

    fun cancel() {
        cleanupPendingFiles(_uploadState.value.pendingUpload)
        workManager.cancelUniqueWork(StoryUploadWorker.WORK_NAME)
        _uploadState.update { StoryUploadState() }
    }

    fun dismiss() {
        val current = _uploadState.value
        if (!current.isUploading) {
            cleanupPendingFiles(current.pendingUpload)
        }
        _uploadState.update { it.copy(errorMessage = null, isSuccess = false, pendingUpload = null) }
    }

    private fun cleanupPendingFiles(pending: PendingStoryUpload?) {
        if (pending == null) return
        runCatching { pending.mediaFile?.delete() }
        runCatching {
            val mUrl = pending.musicTrack?.musicUrl
            if (mUrl != null && mUrl.startsWith("file://", ignoreCase = true)) {
                val path = Uri.parse(mUrl).path
                if (path != null && path.contains("story_uploads")) {
                    File(path).delete()
                }
            }
        }
    }
}
