package ir.coffevista.vista_native.features.feed.ui.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.CreatePostRequestDto
import ir.coffevista.vista_native.features.feed.data.FeedApi
import ir.coffevista.vista_native.features.feed.data.PostMediaUploadGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class PostAspectRatio(val label: String, val ratioValue: String, val floatValue: Float) {
    Square("۱:۱", "1:1", 1f),
    Portrait("۴:۵", "4:5", 4f / 5f),
    Landscape("۱۶:۹", "16:9", 16f / 9f),
    Auto("اصلی", "auto", 1f),
}

data class AddPostUiState(
    val content: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val selectedVideo: File? = null,
    val selectedVideoUri: Uri? = null,
    val isVideo: Boolean = false,
    val aspectRatio: PostAspectRatio = PostAspectRatio.Square,
    val hideLikeCount: Boolean = false,
    val hideCommentCount: Boolean = false,
    val commentsDisabled: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadStatusMessage: String? = null,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isPremium: Boolean = false,
) {
    val maxCharLength: Int get() = if (isPremium) 1000 else 500
    val maxGalleryImages: Int get() = if (isPremium) 10 else 3
    val maxUploadBytes: Long get() = if (isPremium) 100L * 1024 * 1024 else 15L * 1024 * 1024
    val maxVideoDurationMs: Long get() = if (isPremium) 120_000L else 60_000L
    val canSubmit: Boolean get() = !isUploading && (content.isNotBlank() || selectedImages.isNotEmpty() || selectedVideo != null)
}

@HiltViewModel
class AddPostViewModel @Inject constructor(
    private val api: FeedApi,
    private val uploader: PostMediaUploadGateway,
    private val authStateProvider: AuthenticationStateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPostUiState())
    val uiState: StateFlow<AddPostUiState> = _uiState.asStateFlow()

    private val currentUserId: String
        get() = (authStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId.orEmpty()

    fun onContentChanged(newContent: String) {
        if (newContent.length <= _uiState.value.maxCharLength) {
            _uiState.update { it.copy(content = newContent, errorMessage = null) }
        }
    }

    fun onImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val currentMax = _uiState.value.maxGalleryImages
        val limited = uris.take(currentMax)
        _uiState.update {
            it.copy(
                selectedImages = limited,
                selectedVideo = null,
                selectedVideoUri = null,
                isVideo = false,
                errorMessage = null,
            )
        }
    }

    fun removeImage(uri: Uri) {
        _uiState.update {
            val updated = it.selectedImages.filterNot { item -> item == uri }
            it.copy(selectedImages = updated)
        }
    }

    fun onVideoSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedVideoUri = uri,
                selectedImages = emptyList(),
                isVideo = true,
                errorMessage = null,
            )
        }
    }

    fun onVideoTrimmed(trimmedFile: File) {
        _uiState.update {
            it.copy(
                selectedVideo = trimmedFile,
                selectedImages = emptyList(),
                isVideo = true,
                errorMessage = null,
            )
        }
    }

    fun removeVideo() {
        _uiState.update {
            it.copy(
                selectedVideo = null,
                selectedVideoUri = null,
                isVideo = false,
            )
        }
    }

    fun onAspectRatioChanged(ratio: PostAspectRatio) {
        _uiState.update { it.copy(aspectRatio = ratio) }
    }

    fun toggleHideLikeCount(hide: Boolean) {
        _uiState.update { it.copy(hideLikeCount = hide) }
    }

    fun toggleHideCommentCount(hide: Boolean) {
        _uiState.update { it.copy(hideCommentCount = hide) }
    }

    fun toggleCommentsDisabled(disabled: Boolean) {
        _uiState.update { it.copy(commentsDisabled = disabled) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun submitPost() {
        val state = _uiState.value
        val userId = currentUserId
        if (userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "کاربر احراز هویت نشده است") }
            return
        }
        if (!state.canSubmit) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadProgress = 0.05f,
                    uploadStatusMessage = "در حال آماده‌سازی فایل‌ها…",
                    errorMessage = null,
                )
            }

            try {
                var finalVideoUrl: String? = null
                var finalThumbUrl: String? = null
                val finalImageUrls = mutableListOf<String>()

                // 1. Upload Video if present
                if (state.isVideo && state.selectedVideo != null) {
                    _uiState.update { it.copy(uploadStatusMessage = "در حال آپلود ویدیو…") }
                    val result = uploader.uploadVideo(
                        userId = userId,
                        videoFile = state.selectedVideo,
                        maxBytes = state.maxUploadBytes,
                        onProgress = { progress ->
                            _uiState.update { it.copy(uploadProgress = 0.1f + progress * 0.7f) }
                        },
                    )
                    finalVideoUrl = result.url
                    finalThumbUrl = result.thumbnailUrl
                }
                // 2. Upload Images if present
                else if (state.selectedImages.isNotEmpty()) {
                    val totalImages = state.selectedImages.size
                    state.selectedImages.forEachIndexed { index, imageUri ->
                        _uiState.update {
                            it.copy(uploadStatusMessage = "در حال آپلود تصویر (${index + 1} از $totalImages)…")
                        }
                        val result = uploader.uploadImage(
                            userId = userId,
                            uri = imageUri,
                            maxBytes = state.maxUploadBytes,
                            onProgress = { p ->
                                val baseProgress = 0.1f + (index.toFloat() / totalImages) * 0.7f
                                val currentItemProgress = (p / totalImages) * 0.7f
                                _uiState.update { it.copy(uploadProgress = baseProgress + currentItemProgress) }
                            },
                        )
                        finalImageUrls.add(result.url)
                    }
                }

                // 3. Extract Hashtags from content
                val hashtags = extractHashtags(state.content)

                // 4. Create Post on Server
                _uiState.update {
                    it.copy(
                        uploadProgress = 0.9f,
                        uploadStatusMessage = "در حال انتشار پست…",
                    )
                }

                val createReq = CreatePostRequestDto(
                    content = state.content.trim().ifBlank { null },
                    imageUrl = finalImageUrls.firstOrNull() ?: finalThumbUrl,
                    imageUrls = finalImageUrls,
                    videoUrl = finalVideoUrl,
                    aspectRatio = state.aspectRatio.ratioValue,
                    tags = hashtags,
                    hideLikeCount = state.hideLikeCount,
                    hideCommentCount = state.hideCommentCount,
                    commentsDisabled = state.commentsDisabled,
                )

                val response = api.createPost(createReq)
                if (response.success || response.post != null) {
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
                            errorMessage = response.message ?: "انتشار پست با خطا مواجه شد",
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = e.message ?: "خطا در آپلود یا ثبت پست",
                    )
                }
            }
        }
    }

    private fun extractHashtags(text: String): List<String> {
        val regex = Regex("#[\\p{L}\\p{N}_]+")
        return regex.findAll(text).map { it.value.removePrefix("#") }.toList()
    }
}
