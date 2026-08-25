package ir.coffevista.vista_native.features.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.FeedAppendResult
import ir.coffevista.vista_native.features.feed.data.FeedKind
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import ir.coffevista.vista_native.features.feed.data.FeedSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val storyRepository: ir.coffevista.vista_native.features.stories.data.StoryRepository,
    private val authStateProvider: AuthenticationStateProvider,
) : ViewModel() {

    val activeStoryUsers = storyRepository.activeStoryUsers

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _selectedKind = MutableStateFlow(FeedKind.Explore)
    val selectedKind: StateFlow<FeedKind> = _selectedKind.asStateFlow()

    private val _viewerUserId = MutableStateFlow<String?>(null)
    val viewerUserId: StateFlow<String?> = _viewerUserId.asStateFlow()

    private var currentUserId: String? = null
    private var initialRefreshPending = false
    private var pageJob: Job? = null

    init {
        viewModelScope.launch {
            storyRepository.refreshActiveStories()
            authStateProvider.state.collectLatest { authState ->
                if (authState is AuthenticationState.SignedIn) {
                    currentUserId = authState.context.userId
                    _viewerUserId.value = authState.context.userId
                    startPage(authState.context.userId, _selectedKind.value)
                } else {
                    pageJob?.cancel()
                    currentUserId = null
                    _viewerUserId.value = null
                    initialRefreshPending = false
                    _uiState.value = FeedUiState.Content(
                        posts = emptyList(),
                        hasMore = true,
                    )
                }
            }
        }
    }

    fun selectKind(kind: FeedKind) {
        if (_selectedKind.value == kind) return
        _selectedKind.value = kind
        currentUserId?.let { startPage(it, kind) }
    }

    private fun startPage(userId: String, kind: FeedKind) {
        pageJob?.cancel()
        initialRefreshPending = true
        _uiState.value = FeedUiState.Loading
        pageJob = viewModelScope.launch {
            coroutineScope {
                launch { observeFeed(userId, kind) }
                launch {
                    yield()
                    refreshFeed(userId, kind)
                }
            }
        }
    }

    private suspend fun observeFeed(userId: String, kind: FeedKind) {
        feedRepository.observeFeed(userId, kind)
            .catch { error ->
                if (isCurrent(userId, kind)) {
                    _uiState.value = FeedUiState.Error(
                        error.message ?: "خطا در خواندن فید",
                    )
                }
            }
            .collectLatest { snapshot ->
                if (isCurrent(userId, kind)) applySnapshot(snapshot)
            }
    }

    private fun applySnapshot(snapshot: FeedSnapshot) {
        when (val current = _uiState.value) {
            FeedUiState.Loading -> {
                if (snapshot.posts.isNotEmpty() || !initialRefreshPending) {
                    _uiState.value = FeedUiState.Content(
                        posts = snapshot.posts,
                        hasMore = snapshot.hasMore,
                    )
                }
            }
            is FeedUiState.Content -> {
                _uiState.value = current.copy(
                    posts = snapshot.posts,
                    hasMore = snapshot.hasMore,
                )
            }
            is FeedUiState.Error -> {
                if (snapshot.posts.isNotEmpty()) {
                    _uiState.value = FeedUiState.Content(
                        posts = snapshot.posts,
                        isOffline = true,
                        isStale = true,
                        hasMore = snapshot.hasMore,
                        refreshError = current.message,
                    )
                }
            }
        }
    }

    fun refresh() {
        val userId = currentUserId ?: return
        val kind = _selectedKind.value
        val currentState = _uiState.value
        if (currentState is FeedUiState.Content) {
            if (currentState.isRefreshing) return
            _uiState.value = currentState.copy(
                isRefreshing = true,
                refreshError = null,
            )
        } else {
            _uiState.value = FeedUiState.Loading
        }
        viewModelScope.launch { refreshFeed(userId, kind) }
    }

    private suspend fun refreshFeed(userId: String, kind: FeedKind) {
        try {
            val result = feedRepository.refreshFeed(userId, kind)
            if (!isCurrent(userId, kind)) return
            initialRefreshPending = false
            when (val current = _uiState.value) {
                FeedUiState.Loading -> {
                    _uiState.value = FeedUiState.Content(
                        posts = emptyList(),
                        hasMore = result.hasMore,
                    )
                }
                is FeedUiState.Content -> {
                    _uiState.value = current.copy(
                        isRefreshing = false,
                        isOffline = false,
                        isStale = false,
                        hasMore = result.hasMore,
                        refreshError = null,
                    )
                }
                is FeedUiState.Error -> Unit
            }
        } catch (error: Exception) {
            if (!isCurrent(userId, kind)) return
            initialRefreshPending = false
            val message = localizedFeedError(error)
            val current = _uiState.value
            if (current is FeedUiState.Content && current.posts.isNotEmpty()) {
                _uiState.value = current.copy(
                    isRefreshing = false,
                    isOffline = true,
                    isStale = true,
                    refreshError = message,
                )
            } else {
                _uiState.value = FeedUiState.Error(message)
            }
        }
    }

    private fun localizedFeedError(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("Unable to resolve host", ignoreCase = true) ||
                raw.contains("No address associated with hostname", ignoreCase = true) ->
                "اتصال اینترنت برقرار نیست؛ نسخه ذخیره‌شده نمایش داده می‌شود."
            raw.contains("timeout", ignoreCase = true) ->
                "زمان دریافت فید به پایان رسید. دوباره تلاش کنید."
            else -> "به‌روزرسانی فید ناموفق بود"
        }
    }

    fun loadMore() {
        val userId = currentUserId ?: return
        val kind = _selectedKind.value
        val currentState = _uiState.value
        if (
            currentState !is FeedUiState.Content ||
            currentState.isRefreshing ||
            currentState.isAppending ||
            !currentState.hasMore
        ) return

        _uiState.value = currentState.copy(
            isAppending = true,
            appendError = null,
        )
        viewModelScope.launch {
            try {
                val result = feedRepository.loadMoreFeed(userId, kind)
                if (!isCurrent(userId, kind)) return@launch
                _uiState.update {
                    val content = it as? FeedUiState.Content ?: return@update it
                    when (result) {
                        is FeedAppendResult.Appended -> content.copy(
                            isAppending = false,
                            hasMore = result.hasMore,
                            appendError = null,
                        )
                        FeedAppendResult.EndReached -> content.copy(
                            isAppending = false,
                            hasMore = false,
                            appendError = null,
                        )
                        FeedAppendResult.IgnoredAlreadyLoading ->
                            content.copy(isAppending = false)
                    }
                }
            } catch (error: Exception) {
                if (!isCurrent(userId, kind)) return@launch
                _uiState.update {
                    val content = it as? FeedUiState.Content ?: return@update it
                    content.copy(
                        isAppending = false,
                        appendError = error.message ?: "بارگذاری ادامه فید ناموفق بود",
                    )
                }
            }
        }
    }

    private fun isCurrent(userId: String, kind: FeedKind): Boolean =
        currentUserId == userId && _selectedKind.value == kind

    fun toggleLike(postId: String, isLiked: Boolean, currentLikeCount: Long) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                val newCount = if (isLiked) currentLikeCount - 1 else currentLikeCount + 1
                val ownerId = (_uiState.value as? FeedUiState.Content)
                    ?.posts?.firstOrNull { it.id == postId }?.userId ?: return@launch
                feedRepository.toggleLike(userId, postId, ownerId, !isLiked, maxOf(0L, newCount))
            } catch (e: Exception) {
                e.printStackTrace()
                // Ignored, repository handles rollback
            }
        }
    }

    fun toggleSave(postId: String, isSaved: Boolean) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                feedRepository.toggleSave(userId, postId, !isSaved)
            } catch (e: Exception) {
                // Ignored, repository handles rollback
            }
        }
    }

    fun deletePost(postId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            runCatching { feedRepository.deletePost(userId, postId) }
        }
    }

    fun reportPost(post: FeedPost, reason: String) {
        viewModelScope.launch {
            runCatching {
                feedRepository.reportPost(
                    postId = post.id,
                    reportedUserId = post.userId,
                    reason = reason,
                )
            }
        }
    }

    fun markNotInterested(postId: String) {
        trackEvent(postId, "not_interested")
        _uiState.update { state ->
            val content = state as? FeedUiState.Content ?: return@update state
            content.copy(posts = content.posts.filterNot { it.id == postId })
        }
    }

    fun updateEngagementVisibility(
        post: FeedPost,
        hideLikeCount: Boolean?,
        hideCommentCount: Boolean?,
    ) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            runCatching {
                feedRepository.updatePost(
                    accountId = userId,
                    postId = post.id,
                    hideLikeCount = hideLikeCount,
                    hideCommentCount = hideCommentCount,
                )
            }
        }
    }

    fun trackEvent(postId: String, eventType: String) {
        viewModelScope.launch {
            runCatching { feedRepository.trackFeedEvent(postId, eventType) }
        }
    }
}
