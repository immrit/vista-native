package ir.coffevista.vista_native.features.feed.data

import ir.coffevista.vista_native.core.database.feed.FeedDao
import ir.coffevista.vista_native.core.database.feed.FeedPageStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex

@Singleton
class OfflineFirstFeedRepository @Inject constructor(
    private val feedDao: FeedDao,
    private val feedApi: FeedApi,
) : FeedRepository {

    private val limit = 15
    private val appendMutex = Mutex()

    override fun observeFeed(accountId: String): Flow<FeedSnapshot> {
        return combine(
            feedDao.observeFeed(accountId),
            feedDao.observePageState(accountId),
        ) { entities, pageState ->
            FeedSnapshot(
                posts = entities.map { it.asExternalModel() },
                hasMore = pageState?.hasMore ?: true,
                nextOffset = pageState?.nextOffset ?: entities.size,
            )
        }
    }

    override fun getPostById(accountId: String, postId: String): Flow<FeedPost?> {
        return feedDao.observePostById(accountId, postId).map { it?.asExternalModel() }
    }

    override suspend fun refreshFeed(accountId: String): FeedRefreshResult {
        val response = feedApi.getFeed(limit = limit, offset = 0)
        val entities = response.posts
            .distinctBy(FeedPostDto::id)
            .mapIndexed { index, post ->
                post.asEntity(accountId = accountId, sortOrder = index.toLong())
            }
        feedDao.replaceFirstPage(
            accountId = accountId,
            posts = entities,
            state = FeedPageStateEntity(
                accountId = accountId,
                nextOffset = response.posts.size,
                hasMore = response.hasMore,
                nextCursor = response.nextCursor,
                lastRefreshEpochMillis = System.currentTimeMillis(),
            ),
        )
        return FeedRefreshResult(
            itemCount = entities.size,
            hasMore = response.hasMore,
        )
    }

    override suspend fun loadMoreFeed(accountId: String): FeedAppendResult {
        if (!appendMutex.tryLock()) {
            return FeedAppendResult.IgnoredAlreadyLoading
        }
        try {
            val currentState = feedDao.getPageState(accountId)
            if (currentState?.hasMore == false) {
                return FeedAppendResult.EndReached
            }
            val offset = currentState?.nextOffset ?: feedDao.getPostCount(accountId)
            val response = feedApi.getFeed(limit = limit, offset = offset)
            val distinctPosts = response.posts.distinctBy(FeedPostDto::id)
            val existingIds = if (distinctPosts.isEmpty()) {
                emptySet()
            } else {
                feedDao.getExistingPostIds(
                    accountId = accountId,
                    postIds = distinctPosts.map(FeedPostDto::id),
                ).toSet()
            }
            val firstSortOrder = (feedDao.getMaxSortOrder(accountId) ?: -1L) + 1L
            val newEntities = distinctPosts
                .filterNot { it.id in existingIds }
                .mapIndexed { index, post ->
                    post.asEntity(
                        accountId = accountId,
                        sortOrder = firstSortOrder + index,
                    )
                }
            feedDao.appendPage(
                posts = newEntities,
                state = FeedPageStateEntity(
                    accountId = accountId,
                    nextOffset = offset + response.posts.size,
                    hasMore = response.hasMore,
                    nextCursor = response.nextCursor,
                    lastRefreshEpochMillis = currentState?.lastRefreshEpochMillis
                        ?: System.currentTimeMillis(),
                ),
            )
            return FeedAppendResult.Appended(
                newItemCount = newEntities.size,
                hasMore = response.hasMore,
            )
        } finally {
            appendMutex.unlock()
        }
    }

    override suspend fun clearAccount(accountId: String) {
        feedDao.clearAccount(accountId)
    }
}
