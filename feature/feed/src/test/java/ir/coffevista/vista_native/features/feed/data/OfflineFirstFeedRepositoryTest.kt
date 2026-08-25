package ir.coffevista.vista_native.features.feed.data

import ir.coffevista.vista_native.core.database.feed.FeedDao
import ir.coffevista.vista_native.core.database.feed.FeedPageStateEntity
import ir.coffevista.vista_native.core.database.feed.FeedPostEntity
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineFirstFeedRepositoryTest {
    private lateinit var dao: FakeFeedDao
    private lateinit var api: FakeFeedApi
    private lateinit var repository: OfflineFirstFeedRepository

    @Before
    fun setUp() {
        dao = FakeFeedDao()
        api = FakeFeedApi()
        repository = OfflineFirstFeedRepository(dao, api)
    }

    @Test
    fun cacheFirstEmissionReturnsStoredPostsBeforeNetworkWork() = runTest {
        dao.insertPosts(listOf(entity(accountId = "account-a", id = "cached")))

        val snapshot = repository.observeFeed("account-a").first()

        assertEquals(listOf("cached"), snapshot.posts.map(FeedPost::id))
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun refreshSuccessAtomicallyReplacesFirstPageAndMetadata() = runTest {
        dao.insertPosts(listOf(entity(accountId = "account-a", id = "old")))
        api.response = response("new-1", "new-2", hasMore = true)

        val result = repository.refreshFeed("account-a")

        assertEquals(2, result.itemCount)
        assertEquals(listOf("new-1", "new-2"), dao.posts("account-a").map { it.id })
        assertEquals(2, dao.getPageState("account-a")?.nextOffset)
        assertTrue(dao.getPageState("account-a")?.hasMore == true)
        assertEquals(listOf(15 to 0), api.calls)
    }

    @Test
    fun refreshFailurePreservesExistingCache() = runTest {
        dao.insertPosts(listOf(entity(accountId = "account-a", id = "cached")))
        api.failure = IOException("offline")

        assertTrue(
            runCatching { repository.refreshFeed("account-a") }.exceptionOrNull() is IOException,
        )

        assertEquals(listOf("cached"), dao.posts("account-a").map { it.id })
    }

    @Test
    fun refreshFailureWithoutCacheLeavesAccountEmpty() = runTest {
        api.failure = IOException("offline")

        assertTrue(
            runCatching { repository.refreshFeed("account-a") }.exceptionOrNull() is IOException,
        )

        assertTrue(dao.posts("account-a").isEmpty())
        assertEquals(null, dao.getPageState("account-a"))
    }

    @Test
    fun appendSuccessUsesPersistedOffsetAndKeepsExistingList() = runTest {
        dao.insertPosts(listOf(entity(accountId = "account-a", id = "cached", sortOrder = 0)))
        dao.upsertPageState(pageState("account-a", nextOffset = 7, hasMore = true))
        api.response = response("next", hasMore = false)

        val result = repository.loadMoreFeed("account-a") as FeedAppendResult.Appended

        assertEquals(listOf(15 to 7), api.calls)
        assertEquals(listOf("cached", "next"), dao.posts("account-a").map { it.id })
        assertEquals(8, dao.getPageState("account-a")?.nextOffset)
        assertFalse(result.hasMore)
    }

    @Test
    fun appendFailureDoesNotMutatePostsOrMetadata() = runTest {
        dao.insertPosts(listOf(entity(accountId = "account-a", id = "cached")))
        val state = pageState("account-a", nextOffset = 1, hasMore = true)
        dao.upsertPageState(state)
        api.failure = IOException("append failed")

        assertTrue(
            runCatching { repository.loadMoreFeed("account-a") }.exceptionOrNull() is IOException,
        )

        assertEquals(listOf("cached"), dao.posts("account-a").map { it.id })
        assertEquals(state, dao.getPageState("account-a"))
    }

    @Test
    fun appendDeduplicatesByPostIdButAdvancesByServerItemCount() = runTest {
        dao.insertPosts(listOf(entity(accountId = "account-a", id = "same")))
        dao.upsertPageState(pageState("account-a", nextOffset = 1, hasMore = true))
        api.response = response("same", "new", hasMore = true)

        val result = repository.loadMoreFeed("account-a") as FeedAppendResult.Appended

        assertEquals(1, result.newItemCount)
        assertEquals(listOf("same", "new"), dao.posts("account-a").map { it.id })
        assertEquals(3, dao.getPageState("account-a")?.nextOffset)
    }

    @Test
    fun endReachedStopsWithoutCallingApi() = runTest {
        dao.upsertPageState(pageState("account-a", nextOffset = 15, hasMore = false))

        val result = repository.loadMoreFeed("account-a")

        assertEquals(FeedAppendResult.EndReached, result)
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun concurrentAppendIsIgnoredInsteadOfQueued() = runTest {
        dao.upsertPageState(pageState("account-a", nextOffset = 1, hasMore = true))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        api.handler = { _, _ ->
            entered.complete(Unit)
            release.await()
            response("new", hasMore = false)
        }

        val first = async { repository.loadMoreFeed("account-a") }
        entered.await()
        val second = repository.loadMoreFeed("account-a")
        release.complete(Unit)

        assertEquals(FeedAppendResult.IgnoredAlreadyLoading, second)
        assertTrue(first.await() is FeedAppendResult.Appended)
        assertEquals(1, api.calls.size)
    }

    @Test
    fun accountIsolationAndClearAffectOnlyRequestedAccount() = runTest {
        dao.insertPosts(
            listOf(
                entity(accountId = "account-a", id = "a"),
                entity(accountId = "account-b", id = "b"),
            ),
        )
        dao.upsertPageState(pageState("account-a", nextOffset = 1, hasMore = true))
        dao.upsertPageState(pageState("account-b", nextOffset = 1, hasMore = true))

        repository.clearAccount("account-a")

        assertTrue(repository.observeFeed("account-a").first().posts.isEmpty())
        assertEquals(
            listOf("b"),
            repository.observeFeed("account-b").first().posts.map(FeedPost::id),
        )
        assertEquals(null, dao.getPageState("account-a"))
        assertEquals(1, dao.getPageState("account-b")?.nextOffset)
    }

    @Test
    fun exploreAndFollowingCachesRemainIndependent() = runTest {
        api.response = response("explore", hasMore = false)
        repository.refreshFeed("account-a", FeedKind.Explore)
        api.response = response("following", hasMore = false)
        repository.refreshFeed("account-a", FeedKind.Following)

        assertEquals(
            listOf("explore"),
            repository.observeFeed("account-a", FeedKind.Explore)
                .first()
                .posts
                .map(FeedPost::id),
        )
        assertEquals(
            listOf("following"),
            repository.observeFeed("account-a", FeedKind.Following)
                .first()
                .posts
                .map(FeedPost::id),
        )
    }

    @Test
    fun followingAppendUsesServerCursor() = runTest {
        api.response = response("first", hasMore = true)
        repository.refreshFeed("account-a", FeedKind.Following)
        api.response = response("second", hasMore = false)

        repository.loadMoreFeed("account-a", FeedKind.Following)

        assertEquals(
            listOf(null, "2026-07-27T17:21:5Z"),
            api.followingCursors,
        )
    }

    @Test
    fun postDetailRefreshPersistsWithoutPollutingExploreFeed() = runTest {
        api.response = response("detail", hasMore = false)

        val refreshed = repository.refreshPost("account-a", "detail")

        assertEquals("detail", refreshed.id)
        assertEquals(
            "detail",
            repository.getPostById("account-a", "detail").first()?.id,
        )
        assertTrue(repository.observeFeed("account-a").first().posts.isEmpty())
    }

    @Test
    fun profilePostsAreCachedPerViewerAndTargetAndClearedOnLogout() = runTest {
        api.response = response("profile-post", hasMore = false)
        repository.refreshUserPosts("account-a", "target")

        assertEquals(
            listOf("profile-post"),
            repository.observeUserPosts("account-a", "target")
                .first()
                .posts
                .map(FeedPost::id),
        )
        assertTrue(
            repository.observeUserPosts("account-b", "target")
                .first()
                .posts
                .isEmpty(),
        )

        repository.clearAccount("account-a")

        assertTrue(
            repository.observeUserPosts("account-a", "target")
                .first()
                .posts
                .isEmpty(),
        )
    }

    private fun response(
        vararg ids: String,
        hasMore: Boolean,
    ) = FeedResponseDto(
        posts = ids.map(::dto),
        hasMore = hasMore,
        nextCursor = ids.lastOrNull()?.let { "2026-07-27T17:21:${it.length}Z" },
    )

    private fun dto(id: String) = FeedPostDto(
        id = id,
        userId = "author",
        content = "caption-$id",
        imageUrl = null,
        imageUrls = emptyList(),
        videoUrl = null,
        musicUrl = null,
        aspectRatio = null,
        musicTitle = null,
        tags = emptyList(),
        likeCount = 0,
        commentCount = 0,
        isLiked = false,
        isSaved = false,
        hideLikeCount = false,
        hideCommentCount = false,
        author = AuthorInfoDto(
            userId = "author",
            username = "author",
            fullName = "Author",
            avatarUrl = null,
            isVerified = false,
            verificationType = null,
        ),
        createdAt = "2026-07-27T17:21:49Z",
        updatedAt = "2026-07-27T17:21:49Z",
    )

    private fun entity(
        accountId: String,
        id: String,
        sortOrder: Long = 0,
    ) = dto(id).asEntity(accountId = accountId, sortOrder = sortOrder)

    private fun pageState(
        accountId: String,
        nextOffset: Int,
        hasMore: Boolean,
    ) = FeedPageStateEntity(
        accountId = accountId,
        nextOffset = nextOffset,
        hasMore = hasMore,
        nextCursor = null,
        lastRefreshEpochMillis = 123,
    )
}

private class FakeFeedApi : FeedApi {
    var response = FeedResponseDto(emptyList(), hasMore = false)
    var failure: IOException? = null
    var handler: (suspend (Int, Int) -> FeedResponseDto)? = null
    val calls = mutableListOf<Pair<Int, Int>>()
    val followingCursors = mutableListOf<String?>()

    override suspend fun getExploreFeed(limit: Int, offset: Int): FeedResponseDto {
        calls += limit to offset
        failure?.let { throw it }
        return handler?.invoke(limit, offset) ?: response
    }

    override suspend fun getFollowingFeed(
        limit: Int,
        cursor: String?,
    ): FeedResponseDto {
        followingCursors += cursor
        failure?.let { throw it }
        return response
    }

    override suspend fun getPost(postId: String): FeedPostDto =
        response.posts.firstOrNull { it.id == postId } ?: throw IOException("missing")

    override suspend fun getUserPosts(
        userId: String,
        limit: Int,
        offset: Int,
    ): FeedResponseDto {
        calls += limit to offset
        failure?.let { throw it }
        return response
    }

    override suspend fun toggleLike(postId: String, body: LikeRequestDto): LikeResponseDto {
        failure?.let { throw it }
        return LikeResponseDto(isLiked = true, likeCount = 1)
    }

    override suspend fun toggleSave(postId: String): SaveResponseDto {
        failure?.let { throw it }
        return SaveResponseDto(isSaved = true)
    }

    override suspend fun updatePost(postId: String, body: UpdatePostRequestDto): FeedPostDto = error("unused")
    override suspend fun deletePost(postId: String) = Unit
    override suspend fun reportPost(body: ReportPostRequestDto) = Unit
    override suspend fun trackFeedEvent(body: FeedEventRequestDto) = Unit
    override suspend fun presignUpload(request: PostPresignRequestDto): PostPresignResponseDto = error("unused")
    override suspend fun createPost(request: CreatePostRequestDto): CreatePostResponseDto = error("unused")
}

private class FakeFeedDao : FeedDao {
    private val postsByAccount = linkedMapOf<String, LinkedHashMap<String, FeedPostEntity>>()
    private val postFlows = mutableMapOf<String, MutableStateFlow<List<FeedPostEntity>>>()
    private val states = mutableMapOf<String, FeedPageStateEntity>()
    private val stateFlows = mutableMapOf<String, MutableStateFlow<FeedPageStateEntity?>>()

    override fun observeFeed(accountId: String): Flow<List<FeedPostEntity>> =
        postFlows.getOrPut(accountId) { MutableStateFlow(posts(accountId)) }

    override fun observePageState(accountId: String): Flow<FeedPageStateEntity?> =
        stateFlows.getOrPut(accountId) { MutableStateFlow(states[accountId]) }

    override suspend fun getPageState(accountId: String): FeedPageStateEntity? =
        states[accountId]

    override suspend fun insertPosts(posts: List<FeedPostEntity>) {
        posts.forEach { post ->
            postsByAccount.getOrPut(post.accountId) { linkedMapOf() }[post.id] = post
        }
        posts.map(FeedPostEntity::accountId).distinct().forEach(::emitPosts)
    }

    override suspend fun upsertPageState(state: FeedPageStateEntity) {
        states[state.accountId] = state
        stateFlows.getOrPut(state.accountId) { MutableStateFlow(null) }.value = state
    }

    override suspend fun deletePosts(accountId: String) {
        postsByAccount.remove(accountId)
        emitPosts(accountId)
    }

    override suspend fun deletePageState(accountId: String) {
        states.remove(accountId)
        stateFlows.getOrPut(accountId) { MutableStateFlow(null) }.value = null
    }

    override suspend fun deletePostsAndNamespaces(
        accountId: String,
        namespacePrefix: String,
    ) {
        postsByAccount.keys
            .filter { it == accountId || it.startsWith(namespacePrefix) }
            .toList()
            .forEach {
                postsByAccount.remove(it)
                emitPosts(it)
            }
    }

    override suspend fun deletePageStatesAndNamespaces(
        accountId: String,
        namespacePrefix: String,
    ) {
        states.keys
            .filter { it == accountId || it.startsWith(namespacePrefix) }
            .toList()
            .forEach {
                states.remove(it)
                stateFlows.getOrPut(it) { MutableStateFlow(null) }.value = null
            }
    }

    override suspend fun getMaxSortOrder(accountId: String): Long? =
        posts(accountId).maxOfOrNull(FeedPostEntity::sortOrder)

    override suspend fun getPostCount(accountId: String): Int = posts(accountId).size

    override suspend fun getExistingPostIds(
        accountId: String,
        postIds: List<String>,
    ): List<String> = postsByAccount[accountId]
        .orEmpty()
        .keys
        .filter { it in postIds }

    override fun observePostById(
        accountId: String,
        postId: String,
    ): Flow<FeedPostEntity?> = flowOf(postsByAccount[accountId]?.get(postId))

    override suspend fun updateLikeState(postId: String, isLiked: Boolean, likeCount: Long) {
        postsByAccount.values.forEach { map ->
            map[postId]?.let { post ->
                map[postId] = post.copy(isLiked = isLiked, likeCount = likeCount)
            }
        }
        postsByAccount.keys.forEach(::emitPosts)
    }

    override suspend fun updateSaveState(postId: String, isSaved: Boolean) {
        postsByAccount.values.forEach { map ->
            map[postId]?.let { post ->
                map[postId] = post.copy(isSaved = isSaved)
            }
        }
        postsByAccount.keys.forEach(::emitPosts)
    }

    override suspend fun updateCommentCount(postId: String, commentCount: Long) {
        postsByAccount.values.forEach { map ->
            map[postId]?.let { post ->
                map[postId] = post.copy(commentCount = commentCount)
            }
        }
        postsByAccount.keys.forEach(::emitPosts)
    }

    override suspend fun updatePostFields(postId: String, content: String?, hideLikeCount: Boolean?, hideCommentCount: Boolean?) = Unit
    override suspend fun deletePostEverywhere(postId: String) {
        postsByAccount.values.forEach { it.remove(postId) }
        postsByAccount.keys.forEach(::emitPosts)
    }

    fun posts(accountId: String): List<FeedPostEntity> = postsByAccount[accountId]
        .orEmpty()
        .values
        .sortedBy(FeedPostEntity::sortOrder)

    private fun emitPosts(accountId: String) {
        postFlows.getOrPut(accountId) { MutableStateFlow(emptyList()) }.value = posts(accountId)
    }
}
