package ir.coffevista.vista_native.features.feed.ui

import ir.coffevista.vista_native.features.feed.data.FeedPost

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Content(
        val posts: List<FeedPost>,
        val isRefreshing: Boolean = false,
        val isAppending: Boolean = false,
        val isOffline: Boolean = false,
        val isStale: Boolean = false,
        val hasMore: Boolean = true,
        val appendError: String? = null,
        val refreshError: String? = null,
    ) : FeedUiState
    data class Error(val message: String) : FeedUiState
}
