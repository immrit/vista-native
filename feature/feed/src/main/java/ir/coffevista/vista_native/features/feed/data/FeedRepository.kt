package ir.coffevista.vista_native.features.feed.data

import kotlinx.coroutines.flow.Flow

enum class FeedKind {
    Explore,
    Following,
}

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
    fun observeFeed(
        accountId: String,
        kind: FeedKind = FeedKind.Explore,
    ): Flow<FeedSnapshot>
    fun getPostById(accountId: String, postId: String): Flow<FeedPost?>
    fun observeUserPosts(accountId: String, userId: String): Flow<FeedSnapshot>
    suspend fun refreshFeed(
        accountId: String,
        kind: FeedKind = FeedKind.Explore,
    ): FeedRefreshResult
    suspend fun loadMoreFeed(
        accountId: String,
        kind: FeedKind = FeedKind.Explore,
    ): FeedAppendResult
    suspend fun refreshPost(accountId: String, postId: String): FeedPost
    suspend fun refreshUserPosts(accountId: String, userId: String): FeedRefreshResult
    suspend fun loadMoreUserPosts(accountId: String, userId: String): FeedAppendResult
    suspend fun clearAccount(accountId: String)
}
