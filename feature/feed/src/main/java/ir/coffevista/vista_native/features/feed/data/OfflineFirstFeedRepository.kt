package ir.coffevista.vista_native.features.feed.data

import ir.coffevista.vista_native.core.database.feed.FeedDao
import ir.coffevista.vista_native.core.database.feed.FeedPageStateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstFeedRepository @Inject constructor(
    private val feedDao: FeedDao,
    private val feedApi: FeedApi,
) : FeedRepository {

    private val exploreLimit = 15
    private val followingLimit = 20
    private val profileLimit = 30
    private val appendMutexes = mutableMapOf<String, Mutex>()

    override fun observeFeed(accountId: String, kind: FeedKind): Flow<FeedSnapshot> =
        observePage(storageKey(accountId, kind))

    override fun observeUserPosts(accountId: String, userId: String): Flow<FeedSnapshot> =
        observePage(userPostsStorageKey(accountId, userId))

    private fun observePage(storageAccountId: String): Flow<FeedSnapshot> = combine(
        feedDao.observeFeed(storageAccountId),
        feedDao.observePageState(storageAccountId),
    ) { entities, pageState ->
        FeedSnapshot(
            posts = entities.map { it.asExternalModel() },
            hasMore = pageState?.hasMore ?: true,
            nextOffset = pageState?.nextOffset ?: entities.size,
        )
    }

    override fun getPostById(accountId: String, postId: String): Flow<FeedPost?> = combine(
        feedDao.observePostById(accountId, postId),
        feedDao.observePostById(storageKey(accountId, FeedKind.Following), postId),
        feedDao.observePostById(detailStorageKey(accountId), postId),
    ) { explore, following, detail ->
        (detail ?: explore ?: following)?.asExternalModel()
    }

    override suspend fun refreshFeed(
        accountId: String,
        kind: FeedKind,
    ): FeedRefreshResult {
        val storageAccountId = storageKey(accountId, kind)
        val response = when (kind) {
            FeedKind.Explore -> feedApi.getExploreFeed(limit = exploreLimit, offset = 0)
            FeedKind.Following -> feedApi.getFollowingFeed(
                limit = followingLimit,
                cursor = null,
            )
        }
        return replaceFirstPage(storageAccountId, response)
    }

    override suspend fun loadMoreFeed(
        accountId: String,
        kind: FeedKind,
    ): FeedAppendResult {
        val storageAccountId = storageKey(accountId, kind)
        val limit = if (kind == FeedKind.Explore) exploreLimit else followingLimit
        return appendPage(storageAccountId) { state, offset ->
            when (kind) {
                FeedKind.Explore -> feedApi.getExploreFeed(limit = limit, offset = offset)
                FeedKind.Following -> feedApi.getFollowingFeed(
                    limit = limit,
                    cursor = state?.nextCursor,
                )
            }
        }
    }

    override suspend fun refreshPost(accountId: String, postId: String): FeedPost {
        val post = feedApi.getPost(postId)
        val storageAccountId = detailStorageKey(accountId)
        feedDao.insertPosts(
            listOf(post.asEntity(accountId = storageAccountId, sortOrder = 0)),
        )
        return post.asEntity(storageAccountId, 0).asExternalModel()
    }

    override suspend fun refreshUserPosts(
        accountId: String,
        userId: String,
    ): FeedRefreshResult {
        val response = feedApi.getUserPosts(userId, profileLimit, 0)
        return replaceFirstPage(userPostsStorageKey(accountId, userId), response)
    }

    override suspend fun loadMoreUserPosts(
        accountId: String,
        userId: String,
    ): FeedAppendResult {
        val storageAccountId = userPostsStorageKey(accountId, userId)
        return appendPage(storageAccountId) { _, offset ->
            feedApi.getUserPosts(userId, profileLimit, offset)
        }
    }

    private suspend fun replaceFirstPage(
        storageAccountId: String,
        response: FeedResponseDto,
    ): FeedRefreshResult {
        val entities = response.posts
            .distinctBy(FeedPostDto::id)
            .mapIndexed { index, post ->
                post.asEntity(
                    accountId = storageAccountId,
                    sortOrder = index.toLong(),
                )
            }
        feedDao.replaceFirstPage(
            accountId = storageAccountId,
            posts = entities,
            state = FeedPageStateEntity(
                accountId = storageAccountId,
                nextOffset = response.posts.size,
                hasMore = response.hasMore,
                nextCursor = response.nextCursor,
                lastRefreshEpochMillis = System.currentTimeMillis(),
            ),
        )
        return FeedRefreshResult(entities.size, response.hasMore)
    }

    private suspend fun appendPage(
        storageAccountId: String,
        request: suspend (FeedPageStateEntity?, Int) -> FeedResponseDto,
    ): FeedAppendResult {
        val mutex = synchronized(appendMutexes) {
            appendMutexes.getOrPut(storageAccountId) { Mutex() }
        }
        if (!mutex.tryLock()) return FeedAppendResult.IgnoredAlreadyLoading
        try {
            val currentState = feedDao.getPageState(storageAccountId)
            if (currentState?.hasMore == false) return FeedAppendResult.EndReached
            val offset = currentState?.nextOffset ?: feedDao.getPostCount(storageAccountId)
            val response = request(currentState, offset)
            val distinctPosts = response.posts.distinctBy(FeedPostDto::id)
            val existingIds = if (distinctPosts.isEmpty()) {
                emptySet()
            } else {
                feedDao.getExistingPostIds(
                    accountId = storageAccountId,
                    postIds = distinctPosts.map(FeedPostDto::id),
                ).toSet()
            }
            val firstSortOrder =
                (feedDao.getMaxSortOrder(storageAccountId) ?: -1L) + 1L
            val newEntities = distinctPosts
                .filterNot { it.id in existingIds }
                .mapIndexed { index, post ->
                    post.asEntity(
                        accountId = storageAccountId,
                        sortOrder = firstSortOrder + index,
                    )
                }
            feedDao.appendPage(
                posts = newEntities,
                state = FeedPageStateEntity(
                    accountId = storageAccountId,
                    nextOffset = offset + response.posts.size,
                    hasMore = response.hasMore,
                    nextCursor = response.nextCursor,
                    lastRefreshEpochMillis = currentState?.lastRefreshEpochMillis
                        ?: System.currentTimeMillis(),
                ),
            )
            return FeedAppendResult.Appended(newEntities.size, response.hasMore)
        } finally {
            mutex.unlock()
        }
    }

    override suspend fun clearAccount(accountId: String) {
        feedDao.clearAccountNamespaces(accountId, "$accountId$NAMESPACE_SEPARATOR")
        synchronized(appendMutexes) {
            appendMutexes.keys.removeAll { key ->
                key == accountId || key.startsWith("$accountId$NAMESPACE_SEPARATOR")
            }
        }
    }

    private fun storageKey(accountId: String, kind: FeedKind): String = when (kind) {
        FeedKind.Explore -> accountId
        FeedKind.Following -> "$accountId${NAMESPACE_SEPARATOR}following"
    }

    private fun detailStorageKey(accountId: String): String =
        "$accountId${NAMESPACE_SEPARATOR}post-detail"

    private fun userPostsStorageKey(accountId: String, userId: String): String =
        "$accountId${NAMESPACE_SEPARATOR}profile-posts$NAMESPACE_SEPARATOR$userId"

    private companion object {
        const val NAMESPACE_SEPARATOR = "\u001F"
    }
}
