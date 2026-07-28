package ir.coffevista.vista_native.features.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.auth.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.FeedAppendResult
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import ir.coffevista.vista_native.features.feed.data.FeedSnapshot
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
    private val authStateProvider: AuthenticationStateProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var initialRefreshPending = false

    init {
        viewModelScope.launch {
            authStateProvider.state.collectLatest { authState ->
                if (authState is AuthenticationState.SignedIn) {
                    val userId = authState.context.userId
                    currentUserId = userId
                    initialRefreshPending = true
                    _uiState.value = FeedUiState.Loading
                    coroutineScope {
                        launch { observeFeed(userId) }
                        launch {
                            yield()
                            refreshFeed(userId)
                        }
                    }
                } else {
                    currentUserId = null
                    initialRefreshPending = false
                    _uiState.value = FeedUiState.Content(
                        posts = emptyList(),
                        hasMore = true,
                    )
                }
            }
        }
    }

    private suspend fun observeFeed(userId: String) {
        feedRepository.observeFeed(userId)
            .catch { error ->
                if (currentUserId == userId) {
                    _uiState.value = FeedUiState.Error(
                        error.message ?: "Unknown error observing feed",
                    )
                }
            }
            .collectLatest { snapshot ->
                if (currentUserId == userId) {
                    applySnapshot(snapshot)
                }
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

        viewModelScope.launch { refreshFeed(userId) }
    }

    private suspend fun refreshFeed(userId: String) {
        try {
            val result = feedRepository.refreshFeed(userId)
            if (currentUserId != userId) return
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
            if (currentUserId != userId) return
            initialRefreshPending = false
            val message = error.message ?: "Failed to refresh feed"
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

    fun loadMore() {
        val userId = currentUserId ?: return
        val currentState = _uiState.value
        if (
            currentState !is FeedUiState.Content ||
            currentState.isRefreshing ||
            currentState.isAppending ||
            !currentState.hasMore
        ) {
            return
        }

        _uiState.value = currentState.copy(
            isAppending = true,
            appendError = null,
        )

        viewModelScope.launch {
            try {
                val result = feedRepository.loadMoreFeed(userId)
                if (currentUserId != userId) return@launch
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
                        FeedAppendResult.IgnoredAlreadyLoading -> content.copy(
                            isAppending = false,
                        )
                    }
                }
            } catch (error: Exception) {
                if (currentUserId != userId) return@launch
                _uiState.update {
                    val content = it as? FeedUiState.Content ?: return@update it
                    content.copy(
                        isAppending = false,
                        appendError = error.message ?: "Failed to load more",
                    )
                }
            }
        }
    }
}
