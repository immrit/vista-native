package ir.coffevista.vista_native.features.search.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.network.ErrorClassifier
import ir.coffevista.vista_native.features.auth.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthenticationStateProvider
import ir.coffevista.vista_native.features.search.data.SearchHistoryType
import ir.coffevista.vista_native.features.search.data.SearchRepository
import ir.coffevista.vista_native.features.search.data.SearchUser
import ir.coffevista.vista_native.features.search.data.canSearchAsTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository,
    private val authStateProvider: AuthenticationStateProvider,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        SearchUiState(
            query = savedStateHandle.get<String>(KEY_QUERY).orEmpty(),
            selectedTab = SearchTab.fromIndex(savedStateHandle.get<Int>(KEY_TAB) ?: 0),
        ),
    )
    val uiState: StateFlow<SearchUiState> = mutableState.asStateFlow()

    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private var appendJob: Job? = null
    private var requestGeneration = 0L
    private var lastExecutedQuery: String? = null
    private var restoredQueryPending = mutableState.value.query.isNotBlank()

    init {
        viewModelScope.launch {
            authStateProvider.state.collectLatest { authState ->
                val accountId = (authState as? AuthenticationState.SignedIn)
                    ?.context
                    ?.userId
                if (accountId == null) {
                    cancelRequests()
                    mutableState.value = SearchUiState()
                    return@collectLatest
                }

                val previousAccount = mutableState.value.accountId
                if (previousAccount != null && previousAccount != accountId) {
                    cancelRequests()
                    savedStateHandle[KEY_QUERY] = ""
                    savedStateHandle[KEY_TAB] = SearchTab.All.index
                    restoredQueryPending = false
                    mutableState.value = SearchUiState(accountId = accountId)
                } else {
                    mutableState.update { it.copy(accountId = accountId) }
                }

                coroutineScope {
                    launch {
                        repository.observeHistory(accountId).collectLatest { history ->
                            mutableState.update { current ->
                                if (current.accountId == accountId) {
                                    current.copy(history = history)
                                } else {
                                    current
                                }
                            }
                        }
                    }
                    launch { loadTrending() }
                    if (restoredQueryPending) {
                        restoredQueryPending = false
                        launch { executeSearch(force = true) }
                    }
                }
            }
        }
    }

    fun onFocusChanged(focused: Boolean) {
        mutableState.update { current ->
            val phase = when {
                current.hasQuery -> current.phase
                focused -> SearchPhase.Focused
                else -> SearchPhase.Idle
            }
            current.copy(isFocused = focused, phase = phase)
        }
    }

    fun onQueryChanged(value: String) {
        savedStateHandle[KEY_QUERY] = value
        val normalized = value.trim()
        searchJob?.cancel()
        suggestionJob?.cancel()
        appendJob?.cancel()
        requestGeneration += 1

        if (normalized.isEmpty()) {
            lastExecutedQuery = null
            savedStateHandle[KEY_TAB] = SearchTab.All.index
            mutableState.update {
                it.copy(
                    query = value,
                    phase = if (it.isFocused) SearchPhase.Focused else SearchPhase.Idle,
                    selectedTab = SearchTab.All,
                    users = emptyList(),
                    posts = emptyList(),
                    suggestions = emptyList(),
                    isLoadingSuggestions = false,
                    isAppendingUsers = false,
                    hasMoreUsers = true,
                    nextUserOffset = 0,
                    errorMessage = null,
                    appendErrorMessage = null,
                    postHasMore = false,
                    postNextCursor = null,
                )
            }
            return
        }

        mutableState.update {
            it.copy(
                query = value,
                phase = SearchPhase.Typing,
                errorMessage = null,
                appendErrorMessage = null,
            )
        }
        searchJob = viewModelScope.launch {
            delay(QUERY_DEBOUNCE_MILLIS)
            executeSearch(force = false)
        }
    }

    fun submit() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { executeSearch(force = false) }
    }

    fun clearQuery() = onQueryChanged("")

    fun retry() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { executeSearch(force = true) }
    }

    fun selectTab(tab: SearchTab) {
        savedStateHandle[KEY_TAB] = tab.index
        mutableState.update { it.copy(selectedTab = tab) }
    }

    fun loadMoreUsers() {
        val current = mutableState.value
        if (
            current.isAppendingUsers ||
            !current.hasMoreUsers ||
            current.query.trim().isEmpty() ||
            current.query.trim().startsWith("#") ||
            current.phase == SearchPhase.Loading
        ) {
            return
        }
        val generation = requestGeneration
        val query = current.query.trim()
        appendJob?.cancel()
        mutableState.update {
            it.copy(isAppendingUsers = true, appendErrorMessage = null)
        }
        appendJob = viewModelScope.launch {
            try {
                val page = repository.searchUsers(query, current.nextUserOffset)
                if (generation != requestGeneration || mutableState.value.query.trim() != query) {
                    return@launch
                }
                mutableState.update { latest ->
                    val existing = latest.users.associateBy(SearchUser::id).toMutableMap()
                    page.users.forEach { existing.putIfAbsent(it.id, it) }
                    latest.copy(
                        users = existing.values.toList(),
                        isAppendingUsers = false,
                        nextUserOffset = latest.nextUserOffset + page.rawCount,
                        hasMoreUsers = page.rawCount >= SearchRepository.USER_LIMIT,
                        appendErrorMessage = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation != requestGeneration) return@launch
                mutableState.update {
                    it.copy(
                        isAppendingUsers = false,
                        appendErrorMessage = friendly(error, "ادامه جستجوی کاربران"),
                    )
                }
            }
        }
    }

    fun selectUser(user: SearchUser) {
        val accountId = mutableState.value.accountId ?: return
        viewModelScope.launch {
            repository.addHistory(
                accountId = accountId,
                query = "@${user.username}",
                type = SearchHistoryType.User,
            )
        }
    }

    fun selectPost() {
        val current = mutableState.value
        val accountId = current.accountId ?: return
        val query = current.query.trim()
        if (!query.startsWith("#")) return
        viewModelScope.launch {
            repository.addHistory(accountId, query, SearchHistoryType.Hashtag)
        }
    }

    fun selectHashtag(tag: String, keepFocus: Boolean = false) {
        val query = "#${tag.trim().trimStart('#')}"
        val accountId = mutableState.value.accountId
        if (accountId != null) {
            viewModelScope.launch {
                repository.addHistory(accountId, query, SearchHistoryType.Hashtag)
            }
        }
        onQueryChanged(query)
        mutableState.update { it.copy(isFocused = keepFocus) }
        submit()
    }

    fun selectHistory(query: String) {
        onQueryChanged(query)
        mutableState.update { it.copy(isFocused = true) }
        submit()
    }

    fun deleteHistory(query: String) {
        val accountId = mutableState.value.accountId ?: return
        viewModelScope.launch { repository.deleteHistory(accountId, query) }
    }

    fun clearHistory() {
        val accountId = mutableState.value.accountId ?: return
        viewModelScope.launch { repository.clearHistory(accountId) }
    }

    fun refreshTrending() {
        viewModelScope.launch { loadTrending() }
    }

    private suspend fun executeSearch(force: Boolean) {
        val normalized = mutableState.value.query.trim()
        if (normalized.isEmpty()) return
        if (!force && normalized == lastExecutedQuery) return

        val generation = ++requestGeneration
        appendJob?.cancel()
        suggestionJob?.cancel()
        lastExecutedQuery = normalized
        val directHashtag = normalized.startsWith("#")
        val tab = if (directHashtag) SearchTab.Tags else SearchTab.All
        savedStateHandle[KEY_TAB] = tab.index
        mutableState.update {
            it.copy(
                phase = SearchPhase.Loading,
                selectedTab = tab,
                users = emptyList(),
                posts = emptyList(),
                suggestions = emptyList(),
                isLoadingSuggestions = false,
                isAppendingUsers = false,
                hasMoreUsers = true,
                nextUserOffset = 0,
                errorMessage = null,
                appendErrorMessage = null,
                postHasMore = false,
                postNextCursor = null,
            )
        }

        if (directHashtag) scheduleSuggestions(normalized, generation)

        try {
            if (directHashtag) {
                val page = repository.searchPostsByHashtag(normalized)
                if (generation != requestGeneration) return
                mutableState.update {
                    it.copy(
                        phase = if (page.posts.isEmpty()) SearchPhase.Empty else SearchPhase.Content,
                        posts = page.posts,
                        postHasMore = page.hasMore,
                        postNextCursor = page.nextCursor,
                    )
                }
                return
            }

            var users = emptyList<SearchUser>()
            var rawUserCount = 0
            var userSearchSucceeded = true
            try {
                val page = repository.searchUsers(normalized, 0)
                users = page.users
                rawUserCount = page.rawCount
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Flutter's ProfileRepository converts user-search failures to
                // an empty page; preserve that visible behavior.
                userSearchSucceeded = false
            }

            var posts = emptyList<ir.coffevista.vista_native.features.search.data.SearchPost>()
            var postHasMore = false
            var nextCursor: String? = null
            if (canSearchAsTag(normalized)) {
                try {
                    val page = repository.searchPostsByHashtag(normalized)
                    posts = page.posts
                    postHasMore = page.hasMore
                    nextCursor = page.nextCursor
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Supplementary tag lookup is best-effort in Flutter.
                }
            }
            if (generation != requestGeneration) return
            mutableState.update {
                it.copy(
                    phase = if (users.isEmpty() && posts.isEmpty()) {
                        SearchPhase.Empty
                    } else {
                        SearchPhase.Content
                    },
                    users = users,
                    posts = posts,
                    nextUserOffset = rawUserCount,
                    hasMoreUsers = userSearchSucceeded &&
                        rawUserCount >= SearchRepository.USER_LIMIT,
                    postHasMore = postHasMore,
                    postNextCursor = nextCursor,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (generation != requestGeneration) return
            mutableState.update {
                it.copy(
                    phase = SearchPhase.Error,
                    errorMessage = friendly(error, "جستجو"),
                )
            }
        }
    }

    private fun scheduleSuggestions(query: String, generation: Long) {
        suggestionJob = viewModelScope.launch {
            delay(SUGGESTION_DEBOUNCE_MILLIS)
            if (generation != requestGeneration) return@launch
            val keyword = query.trim().removePrefix("#")
            if (keyword.isEmpty()) {
                mutableState.update {
                    it.copy(suggestions = it.trending.take(8), isLoadingSuggestions = false)
                }
                return@launch
            }
            mutableState.update { it.copy(isLoadingSuggestions = true) }
            val suggestions = try {
                repository.searchHashtags(keyword)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                val lower = keyword.lowercase()
                mutableState.value.trending
                    .filter { it.tag.lowercase().startsWith(lower) }
                    .take(8)
            }
            if (generation != requestGeneration) return@launch
            mutableState.update {
                it.copy(
                    suggestions = suggestions,
                    isLoadingSuggestions = false,
                )
            }
        }
    }

    private suspend fun loadTrending() {
        val accountId = mutableState.value.accountId ?: return
        mutableState.update { it.copy(isLoadingTrending = true) }
        val trending = runCatching { repository.trendingHashtags() }.getOrDefault(emptyList())
        if (mutableState.value.accountId != accountId) return
        mutableState.update {
            it.copy(trending = trending, isLoadingTrending = false)
        }
    }

    private fun friendly(error: Throwable, context: String): String =
        ErrorClassifier.classify(error, context).messageFa

    private fun cancelRequests() {
        requestGeneration += 1
        searchJob?.cancel()
        suggestionJob?.cancel()
        appendJob?.cancel()
        lastExecutedQuery = null
    }

    companion object {
        const val QUERY_DEBOUNCE_MILLIS = 300L
        const val SUGGESTION_DEBOUNCE_MILLIS = 300L
        private const val KEY_QUERY = "search.query"
        private const val KEY_TAB = "search.tab"
    }
}
