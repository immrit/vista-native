package ir.coffevista.vista_native.features.feed.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data class Content(
        val post: FeedPost,
        val viewerUserId: String? = null,
        val isRefreshing: Boolean = false,
        val isStale: Boolean = false,
        val refreshError: String? = null,
    ) : PostDetailUiState
    data class Error(val message: String) : PostDetailUiState
}

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val repository: FeedRepository,
    private val authStateProvider: AuthenticationStateProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val postId: String = savedStateHandle.get<String>("reference") ?: ""
    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    init {
        if (postId.isBlank()) {
            _uiState.value = PostDetailUiState.Error("شناسه پست موجود نیست")
        } else {
            viewModelScope.launch {
                authStateProvider.state.collectLatest { authState ->
                    when (authState) {
                        is AuthenticationState.SignedIn ->
                            load(authState.context.userId)
                        is AuthenticationState.SignedOut ->
                            _uiState.value = PostDetailUiState.Error("نشست فعال نیست")
                        else -> Unit
                    }
                }
            }
        }
    }

    private suspend fun load(accountId: String) = coroutineScope {
        launch {
            repository.getPostById(accountId, postId)
                .catch { error ->
                    if (_uiState.value !is PostDetailUiState.Content) {
                        _uiState.value = PostDetailUiState.Error(
                            error.message ?: "خواندن پست ناموفق بود",
                        )
                    }
                }
                .collectLatest { post ->
                    if (post != null) {
                        val current = _uiState.value as? PostDetailUiState.Content
                        _uiState.value = PostDetailUiState.Content(
                            post = post,
                            viewerUserId = accountId,
                            isRefreshing = current?.isRefreshing ?: true,
                            isStale = current?.isStale ?: false,
                            refreshError = current?.refreshError,
                        )
                    }
                }
        }
        launch {
            yield()
            refresh(accountId)
        }
    }

    private suspend fun refresh(accountId: String) {
        val cached = (_uiState.value as? PostDetailUiState.Content)?.post
        if (cached != null) {
            _uiState.value = PostDetailUiState.Content(cached, viewerUserId = accountId, isRefreshing = true)
        }
        try {
            val refreshed = repository.refreshPost(accountId, postId)
            _uiState.value = PostDetailUiState.Content(refreshed, viewerUserId = accountId)
        } catch (error: Exception) {
            val current = (_uiState.value as? PostDetailUiState.Content)?.post ?: cached
            if (current != null) {
                _uiState.value = PostDetailUiState.Content(
                    post = current,
                    viewerUserId = accountId,
                    isStale = true,
                    refreshError = error.message ?: "به‌روزرسانی پست ناموفق بود",
                )
            } else {
                _uiState.value = PostDetailUiState.Error(
                    error.message ?: "پست در دسترس نیست",
                )
            }
        }
    }

    fun toggleLike(isLiked: Boolean, currentLikeCount: Long) {
        viewModelScope.launch {
            val authState = authStateProvider.state.value
            val accountId = (authState as? AuthenticationState.SignedIn)?.context?.userId ?: return@launch
            try {
                val newCount = if (isLiked) currentLikeCount - 1 else currentLikeCount + 1
                val ownerId = (_uiState.value as? PostDetailUiState.Content)?.post?.userId ?: return@launch
                repository.toggleLike(accountId, postId, ownerId, !isLiked, maxOf(0L, newCount))
            } catch (e: Exception) {
                // Ignored, repository handles rollback
            }
        }
    }

    fun toggleSave(isSaved: Boolean) {
        viewModelScope.launch {
            val authState = authStateProvider.state.value
            val accountId = (authState as? AuthenticationState.SignedIn)?.context?.userId ?: return@launch
            try {
                repository.toggleSave(accountId, postId, !isSaved)
            } catch (e: Exception) {
                // Ignored, repository handles rollback
            }
        }
    }

    fun deletePost() {
        viewModelScope.launch {
            val accountId = signedInAccountId() ?: return@launch
            runCatching { repository.deletePost(accountId, postId) }
                .onSuccess { _uiState.value = PostDetailUiState.Error("این پست حذف شد") }
        }
    }

    fun reportPost(reason: String) {
        viewModelScope.launch {
            val post = (_uiState.value as? PostDetailUiState.Content)?.post ?: return@launch
            runCatching { repository.reportPost(post.id, post.userId, reason) }
        }
    }

    fun markNotInterested() {
        viewModelScope.launch {
            runCatching { repository.trackFeedEvent(postId, "not_interested") }
        }
    }

    fun updateEngagementVisibility(hideLikeCount: Boolean?, hideCommentCount: Boolean?) {
        viewModelScope.launch {
            val accountId = signedInAccountId() ?: return@launch
            runCatching {
                repository.updatePost(
                    accountId = accountId,
                    postId = postId,
                    hideLikeCount = hideLikeCount,
                    hideCommentCount = hideCommentCount,
                )
            }
        }
    }

    fun trackShare() {
        viewModelScope.launch { runCatching { repository.trackFeedEvent(postId, "share") } }
    }

    private fun signedInAccountId(): String? =
        (authStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId
}
