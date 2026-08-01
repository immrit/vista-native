package ir.coffevista.vista_native.features.search.ui

import ir.coffevista.vista_native.features.search.data.HashtagSuggestion
import ir.coffevista.vista_native.features.search.data.SearchHistoryItem
import ir.coffevista.vista_native.features.search.data.SearchPost
import ir.coffevista.vista_native.features.search.data.SearchUser

enum class SearchPhase {
    Idle,
    Focused,
    Typing,
    Loading,
    Content,
    Empty,
    Error,
}

enum class SearchTab(val index: Int) {
    All(0),
    People(1),
    Tags(2);

    companion object {
        fun fromIndex(index: Int): SearchTab =
            entries.firstOrNull { it.index == index } ?: All
    }
}

data class SearchUiState(
    val accountId: String? = null,
    val query: String = "",
    val phase: SearchPhase = SearchPhase.Idle,
    val selectedTab: SearchTab = SearchTab.All,
    val users: List<SearchUser> = emptyList(),
    val posts: List<SearchPost> = emptyList(),
    val trending: List<HashtagSuggestion> = emptyList(),
    val suggestions: List<HashtagSuggestion> = emptyList(),
    val history: List<SearchHistoryItem> = emptyList(),
    val isFocused: Boolean = false,
    val isLoadingTrending: Boolean = false,
    val isLoadingSuggestions: Boolean = false,
    val isAppendingUsers: Boolean = false,
    val hasMoreUsers: Boolean = true,
    val nextUserOffset: Int = 0,
    val errorMessage: String? = null,
    val appendErrorMessage: String? = null,
    val postHasMore: Boolean = false,
    val postNextCursor: String? = null,
) {
    val hasQuery: Boolean
        get() = query.trim().isNotEmpty()
}

internal object SearchTestTags {
    const val Launcher = "search-launcher"
    const val LauncherField = "search-launcher-field"
    const val Workspace = "search-workspace"
    const val Field = "search-field"
    const val ClearQuery = "search-clear-query"
    const val Tabs = "search-tabs"
    const val Loading = "search-loading"
    const val Empty = "search-empty"
    const val Error = "search-error"
    const val Retry = "search-retry"
    const val History = "search-history"
    const val ClearHistory = "search-clear-history"
    const val Suggestions = "search-suggestions"
    const val UserList = "search-user-list"
    const val PostGrid = "search-post-grid"
    const val AppendLoading = "search-append-loading"
    const val AppendError = "search-append-error"

    fun user(id: String) = "search-user-$id"
    fun post(id: String) = "search-post-$id"
    fun history(query: String) = "search-history-${query.hashCode()}"
}
