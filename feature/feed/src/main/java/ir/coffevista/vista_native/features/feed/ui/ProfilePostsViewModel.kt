package ir.coffevista.vista_native.features.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.auth.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.FeedAppendResult
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

data class ProfilePostsUiState(
    val posts: List<FeedPost> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val isOffline: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val appendError: String? = null,
)

@HiltViewModel
class ProfilePostsViewModel @Inject constructor(
    private val repository: FeedRepository,
    private val authStateProvider: AuthenticationStateProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfilePostsUiState())
    val uiState: StateFlow<ProfilePostsUiState> = _uiState.asStateFlow()

    private var targetUserId: String? = null
    private var accountId: String? = null
    private var bindJob: Job? = null

    fun bind(userId: String) {
        if (targetUserId == userId && bindJob?.isActive == true) return
        targetUserId = userId
        bindJob?.cancel()
        _uiState.value = ProfilePostsUiState()
        bindJob = viewModelScope.launch {
            authStateProvider.state.collectLatest { authState ->
                if (authState is AuthenticationState.SignedIn) {
                    accountId = authState.context.userId
                    load(authState.context.userId, userId)
                }
            }
        }
    }

    private suspend fun load(accountId: String, userId: String) = coroutineScope {
        launch {
            repository.observeUserPosts(accountId, userId).collectLatest { snapshot ->
                _uiState.update {
                    it.copy(
                        posts = snapshot.posts,
                        hasMore = snapshot.hasMore,
                        isInitialLoading = it.isInitialLoading && snapshot.posts.isEmpty(),
                    )
                }
            }
        }
        launch {
            yield()
            refresh()
        }
    }

    fun refresh() {
        val account = accountId ?: return
        val target = targetUserId ?: return
        _uiState.update {
            it.copy(
                isRefreshing = it.posts.isNotEmpty(),
                isInitialLoading = it.posts.isEmpty(),
                error = null,
            )
        }
        viewModelScope.launch {
            try {
                repository.refreshUserPosts(account, target)
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        isOffline = false,
                        error = null,
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        isOffline = it.posts.isNotEmpty(),
                        error = error.message ?: "دریافت پست‌های نمایه ناموفق بود",
                    )
                }
            }
        }
    }

    fun loadMore() {
        val account = accountId ?: return
        val target = targetUserId ?: return
        val current = _uiState.value
        if (
            current.isInitialLoading ||
            current.isRefreshing ||
            current.isAppending ||
            !current.hasMore
        ) return
        _uiState.update { it.copy(isAppending = true, appendError = null) }
        viewModelScope.launch {
            try {
                val result = repository.loadMoreUserPosts(account, target)
                _uiState.update {
                    when (result) {
                        is FeedAppendResult.Appended -> it.copy(
                            isAppending = false,
                            hasMore = result.hasMore,
                        )
                        FeedAppendResult.EndReached -> it.copy(
                            isAppending = false,
                            hasMore = false,
                        )
                        FeedAppendResult.IgnoredAlreadyLoading ->
                            it.copy(isAppending = false)
                    }
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isAppending = false,
                        appendError = error.message ?: "بارگذاری پست‌های بیشتر ناموفق بود",
                    )
                }
            }
        }
    }
}
