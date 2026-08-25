package ir.coffevista.vista_native.features.profile.settings.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedPostsUiState(
    val posts: List<SavedPostUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val nextCursor: String? = "0",
    val errorMessage: String? = null,
    val feedbackMessage: String? = null,
)

@HiltViewModel
class SavedPostsViewModel @Inject constructor(
    private val api: SavedPostsApi,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SavedPostsUiState())
    val uiState: StateFlow<SavedPostsUiState> = mutableUiState.asStateFlow()

    init { refresh() }

    fun refresh() = load(reset = true)

    fun loadMore() = load(reset = false)

    fun toggleSave(post: SavedPostUiModel) = viewModelScope.launch {
        val response = runCatching { api.toggleSave(post.id) }.getOrNull()
        if (response?.isSuccessful == true && response.body()?.isSaved == false) {
            mutableUiState.update { it.copy(posts = it.posts.filterNot { saved -> saved.id == post.id }) }
        } else {
            mutableUiState.update { it.copy(feedbackMessage = "تغییر وضعیت ذخیره‌سازی ناموفق بود") }
        }
    }

    private fun load(reset: Boolean) {
        val current = mutableUiState.value
        if (!reset && (!current.hasMore || current.isLoadingMore)) return
        mutableUiState.update {
            if (reset) SavedPostsUiState(isLoading = true) else it.copy(isLoadingMore = true, feedbackMessage = null)
        }
        viewModelScope.launch {
            val offset = if (reset) "0" else current.nextCursor
            val response = runCatching { api.getSavedPosts(PAGE_SIZE, offset) }.getOrNull()
            val body = response?.body()
            if (response?.isSuccessful == true && body != null) {
                val items = body.posts.map(::toUi)
                mutableUiState.update {
                    it.copy(
                        posts = if (reset) items else it.posts + items.filterNot { next -> it.posts.any { previous -> previous.id == next.id } },
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = body.hasMore,
                        nextCursor = body.nextCursor,
                    )
                }
            } else {
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = if (reset) "دریافت پست‌های ذخیره‌شده ناموفق بود" else it.errorMessage,
                        feedbackMessage = if (reset) null else "دریافت پست‌های بیشتر ناموفق بود",
                    )
                }
            }
        }
    }

    private fun toUi(post: SavedPostDto) = SavedPostUiModel(
        id = post.id,
        authorUsername = post.author.username.orEmpty(),
        authorFullName = post.author.fullName.orEmpty(),
        authorAvatarUrl = post.author.avatarUrl,
        authorIsVerified = post.author.isVerified,
        timeAgo = post.createdAt.orEmpty(),
        caption = post.caption.orEmpty(),
        mediaUrl = post.imageUrls.firstOrNull() ?: post.imageUrl ?: post.videoUrl,
        isVideo = !post.videoUrl.isNullOrBlank(),
        likesCount = post.likeCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        commentsCount = post.commentCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        isLiked = post.isLiked,
    )

    private companion object { const val PAGE_SIZE = 20 }
}
