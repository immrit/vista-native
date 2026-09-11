package ir.coffevista.vista_native.features.stories.ui.editor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.PostMediaUploadGateway
import ir.coffevista.vista_native.features.stories.data.StoryRepository
import ir.coffevista.vista_native.features.stories.domain.CreateStoryRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryDurationType
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.domain.StoryLink
import ir.coffevista.vista_native.features.stories.domain.StoryLocation
import ir.coffevista.vista_native.features.stories.domain.StoryMention
import ir.coffevista.vista_native.features.stories.domain.StoryPoll
import ir.coffevista.vista_native.features.stories.domain.StoryPollOption
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import ir.coffevista.vista_native.features.stories.domain.StoryUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

internal object StoryVideoUploadPolicy {
    const val MaxBytes: Long = 100L * 1024 * 1024
    const val MaxDurationMs: Long = 60_000L

    fun validateByteCount(byteCount: Long) {
        require(byteCount > 0) { "فایل ویدیو خالی است" }
        require(byteCount <= MaxBytes) { "حجم ویدیو بیش از حد مجاز است (حداکثر ۱۰۰ مگابایت)" }
    }

    fun validateDuration(durationMs: Long?) {
        require(durationMs != null && durationMs > 0) { "مدت ویدیو قابل تشخیص نیست" }
        require(durationMs <= MaxDurationMs) { "مدت ویدیو بیش از حد مجاز است (حداکثر ۶۰ ثانیه)" }
    }
}

data class StoryEditorUiState(
    val mediaUri: Uri? = null,
    val isVideo: Boolean = false,
    val elements: List<StoryElement> = emptyList(),
    val durationType: StoryDurationType = StoryDurationType.Hours24,
    val privacyType: StoryPrivacyType = StoryPrivacyType.Everyone,
    val closeFriendCandidates: List<StoryUser> = emptyList(),
    val selectedCloseFriendIds: Set<String> = emptySet(),
    val isLoadingCloseFriends: Boolean = false,
    val closeFriendsError: String? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadStatus: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
) {
    val canPublish: Boolean get() = mediaUri != null && !isUploading
}

@HiltViewModel
class StoryEditorViewModel @Inject constructor(
    private val repository: StoryRepository,
    private val uploader: PostMediaUploadGateway,
    private val authStateProvider: AuthenticationStateProvider,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryEditorUiState())
    val uiState: StateFlow<StoryEditorUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = (authStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId.orEmpty()

    fun onMediaSelected(uri: Uri, isVideo: Boolean = false) {
        _uiState.update {
            it.copy(
                mediaUri = uri,
                isVideo = isVideo,
                errorMessage = null,
            )
        }
    }

    fun addTextElement(text: String, colorHex: String = "#FFFFFF", bgHex: String? = null) {
        if (text.isBlank()) return
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "text",
            content = text,
            color = colorHex,
            backgroundColor = bgHex,
            y = 0f,
        )
        _uiState.update { it.copy(elements = it.elements + element) }
    }

    fun addPollElement(question: String, option1: String, option2: String) {
        if (question.isBlank() || option1.isBlank() || option2.isBlank()) return
        val poll = StoryPoll(
            id = UUID.randomUUID().toString(),
            question = question,
            options = listOf(
                StoryPollOption(id = "opt_1", text = option1),
                StoryPollOption(id = "opt_2", text = option2),
            ),
        )
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "poll",
            poll = poll,
        )
        _uiState.update { it.copy(elements = it.elements + element) }
    }

    fun addLocationElement(name: String) {
        if (name.isBlank()) return
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "location",
            location = StoryLocation(name = name),
            y = -0.3f,
        )
        _uiState.update { it.copy(elements = it.elements + element) }
    }

    fun addLinkElement(url: String, title: String?) {
        if (url.isBlank()) return
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "link",
            link = StoryLink(url = url, title = title),
            y = 0.3f,
        )
        _uiState.update { it.copy(elements = it.elements + element) }
    }

    fun addMentionElement(username: String) {
        if (username.isBlank()) return
        val clean = username.removePrefix("@")
        val element = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "mention",
            mention = StoryMention(userId = clean, username = clean),
            y = -0.2f,
        )
        _uiState.update { it.copy(elements = it.elements + element) }
    }

    fun removeElement(id: String) {
        _uiState.update { it.copy(elements = it.elements.filterNot { el -> el.id == id }) }
    }

    fun setDuration(duration: StoryDurationType) {
        _uiState.update { it.copy(durationType = duration) }
    }

    fun setPrivacy(privacy: StoryPrivacyType) {
        _uiState.update { it.copy(privacyType = privacy) }
    }

    fun loadCloseFriends() {
        val userId = currentUserId
        if (userId.isBlank() || _uiState.value.isLoadingCloseFriends) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCloseFriends = true, closeFriendsError = null) }
            val users = repository.getFollowingUsers(userId)
            val selected = repository.getCloseFriendIds()
            _uiState.update { current ->
                current.copy(
                    closeFriendCandidates = users.getOrDefault(emptyList()),
                    selectedCloseFriendIds = selected.getOrDefault(current.selectedCloseFriendIds.toList()).toSet(),
                    isLoadingCloseFriends = false,
                    closeFriendsError = users.exceptionOrNull()?.message ?: selected.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun toggleCloseFriend(userId: String) {
        _uiState.update { current ->
            val selected = current.selectedCloseFriendIds.toMutableSet()
            if (!selected.add(userId)) selected.remove(userId)
            current.copy(selectedCloseFriendIds = selected, closeFriendsError = null)
        }
    }

    fun saveCloseFriends() {
        if (_uiState.value.isLoadingCloseFriends) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCloseFriends = true, closeFriendsError = null) }
            repository.updateCloseFriends(_uiState.value.selectedCloseFriendIds.toList())
                .onSuccess { _uiState.update { it.copy(isLoadingCloseFriends = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingCloseFriends = false, closeFriendsError = error.message ?: "ذخیره دوستان نزدیک ناموفق بود") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun publishStory() {
        val state = _uiState.value
        if (state.isUploading || state.isSuccess) return
        val uri = state.mediaUri ?: return
        val userId = currentUserId
        if (userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "کاربر وارد نشده است") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadProgress = 0.1f,
                    uploadStatus = "در حال آماده‌سازی رسانه…",
                    errorMessage = null,
                )
            }

            try {
                // 1. Upload Media
                val uploadResult = if (state.isVideo) {
                    _uiState.update { it.copy(uploadStatus = "در حال آپلود ویدیو…") }
                    uploadStoryVideo(userId, uri) { p ->
                        _uiState.update { it.copy(uploadProgress = 0.1f + p * 0.7f) }
                    }
                } else {
                    _uiState.update { it.copy(uploadStatus = "در حال آپلود تصویر…") }
                    uploader.uploadImage(userId, uri) { p ->
                        _uiState.update { it.copy(uploadProgress = 0.1f + p * 0.7f) }
                    }
                }

                // 2. Create Story via API
                _uiState.update {
                    it.copy(
                        uploadProgress = 0.9f,
                        uploadStatus = "در حال انتشار استوری…",
                    )
                }

                val createReq = CreateStoryRequestDto(
                    mediaUrl = uploadResult.url,
                    mediaType = if (state.isVideo) "video" else "image",
                    thumbnailUrl = uploadResult.thumbnailUrl,
                    durationType = if (state.durationType == StoryDurationType.Hours48) "hours48" else "hours24",
                    privacyType = when (state.privacyType) {
                        StoryPrivacyType.CloseFriends -> "close_friends"
                        StoryPrivacyType.Followers -> "followers"
                        StoryPrivacyType.Custom -> "custom"
                        else -> "everyone"
                    },
                    allowedUserIds = state.selectedCloseFriendIds.toList(),
                    interactiveElements = state.elements,
                )

                val result = repository.createStory(createReq)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadProgress = 1f,
                            isSuccess = true,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "خطا در ایجاد استوری",
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = e.message ?: "خطا در آپلود استوری",
                    )
                }
            }
        }
    }

    private suspend fun uploadStoryVideo(
        userId: String,
        uri: Uri,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val maxBytes = StoryVideoUploadPolicy.MaxBytes
        val temporaryVideo = File.createTempFile("story-video-", ".mp4", context.cacheDir)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw java.io.IOException("فایل ویدیو قابل خواندن نیست")
            input.use { source ->
                temporaryVideo.outputStream().use { target ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        StoryVideoUploadPolicy.validateByteCount(total)
                        target.write(buffer, 0, count)
                    }
                    StoryVideoUploadPolicy.validateByteCount(total)
                }
            }
            val retriever = MediaMetadataRetriever()
            val durationMs = try {
                retriever.setDataSource(temporaryVideo.absolutePath)
                require(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes") {
                    "فایل انتخاب‌شده ویدیوی معتبری نیست"
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
            StoryVideoUploadPolicy.validateDuration(durationMs)
            currentCoroutineContext().ensureActive()
            uploader.uploadVideo(userId, temporaryVideo, maxBytes, onProgress)
        } finally {
            temporaryVideo.delete()
        }
    }
}
