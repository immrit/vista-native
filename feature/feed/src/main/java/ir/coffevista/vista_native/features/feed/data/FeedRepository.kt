package ir.coffevista.vista_native.features.feed.data

import kotlinx.coroutines.flow.Flow

data class FeedSnapshot(
    val posts: List<FeedPost>,
    val hasMore: Boolean,
    val nextOffset: Int,
)

data class FeedRefreshResult(
    val itemCount: Int,
    val hasMore: Boolean,
)

sealed interface FeedAppendResult {
    data class Appended(
        val newItemCount: Int,
        val hasMore: Boolean,
    ) : FeedAppendResult

    data object EndReached : FeedAppendResult
    data object IgnoredAlreadyLoading : FeedAppendResult
}

interface FeedRepository {
    fun observeFeed(accountId: String): Flow<FeedSnapshot>
    fun getPostById(accountId: String, postId: String): Flow<FeedPost?>
    suspend fun refreshFeed(accountId: String): FeedRefreshResult
    suspend fun loadMoreFeed(accountId: String): FeedAppendResult
    suspend fun clearAccount(accountId: String)
}
