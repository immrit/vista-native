package ir.coffevista.vista_native.features.feed.ui

import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.FeedAppendResult
import ir.coffevista.vista_native.features.feed.data.FeedKind
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.FeedRefreshResult
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import ir.coffevista.vista_native.features.feed.data.FeedSnapshot
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeFeedRepository
    private lateinit var storyRepository: ir.coffevista.vista_native.features.stories.data.StoryRepository
    private lateinit var auth: FakeAuthenticationStateProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeFeedRepository()
        storyRepository = ir.coffevista.vista_native.features.stories.data.StoryRepository(
            object : ir.coffevista.vista_native.features.stories.data.StoryApi {
                override suspend fun getActiveStories() = ir.coffevista.vista_native.features.stories.domain.ActiveStoriesResponseDto()
                override suspend fun getUserStories(userId: String) = ir.coffevista.vista_native.features.stories.domain.UserStoriesResponseDto()
                override suspend fun getStoryById(storyId: String) = error("unused")
                override suspend fun createStory(request: ir.coffevista.vista_native.features.stories.domain.CreateStoryRequestDto) = error("unused")
                override suspend fun deleteStory(storyId: String) = Unit
                override suspend fun trackView(storyId: String) = Unit
                override suspend fun getStoryViews(storyId: String, limit: Int, offset: Int) = ir.coffevista.vista_native.features.stories.domain.StoryViewsResponseDto()
                override suspend fun reactToStory(storyId: String, request: ir.coffevista.vista_native.features.stories.domain.StoryReactRequestDto) = Unit
                override suspend fun replyToStory(storyId: String, request: ir.coffevista.vista_native.features.stories.domain.StoryReplyRequestDto) = Unit
                override suspend fun votePoll(storyId: String, request: ir.coffevista.vista_native.features.stories.domain.StoryVoteRequestDto) = Unit
            },
            dispatcher,
        )
        auth = FakeAuthenticationStateProvider(signedIn())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadingTransitionsToContent() = runTest(dispatcher) {
        repository.onRefresh = { accountId ->
            repository.emit(accountId, listOf(post("network")), hasMore = true)
            FeedRefreshResult(itemCount = 1, hasMore = true)
        }

        val viewModel = FeedViewModel(repository, storyRepository, auth)
        assertEquals(FeedUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val content = viewModel.uiState.value as FeedUiState.Content
        assertEquals(listOf("network"), content.posts.map(FeedPost::id))
        assertFalse(content.isOffline)
    }

    @Test
    fun initialLoadingTransitionsToErrorWhenNoCache() = runTest(dispatcher) {
        repository.onRefresh = { throw IOException("offline") }

        val viewModel = FeedViewModel(repository, storyRepository, auth)
        advanceUntilIdle()

        assertEquals(FeedUiState.Error("به‌روزرسانی فید ناموفق بود"), viewModel.uiState.value)
    }

    @Test
    fun refreshUpdatesContentAndClearsStaleFlags() = runTest(dispatcher) {
        repository.emit("account-a", listOf(post("cached")), hasMore = true)
        val viewModel = FeedViewModel(repository, storyRepository, auth)
        advanceUntilIdle()
        repository.onRefresh = { accountId ->
            repository.emit(accountId, listOf(post("fresh")), hasMore = false)
            FeedRefreshResult(itemCount = 1, hasMore = false)
        }

        viewModel.refresh()
        assertTrue((viewModel.uiState.value as FeedUiState.Content).isRefreshing)
        advanceUntilIdle()

        val content = viewModel.uiState.value as FeedUiState.Content
        assertEquals(listOf("fresh"), content.posts.map(FeedPost::id))
        assertFalse(content.isRefreshing)
        assertFalse(content.hasMore)
    }

    @Test
    fun appendSuccessKeepsOldPostsAndAddsNextPage() = runTest(dispatcher) {
        repository.emit("account-a", listOf(post("one")), hasMore = true)
        val viewModel = FeedViewModel(repository, storyRepository, auth)
        advanceUntilIdle()
        repository.onAppend = { accountId ->
            repository.emit(accountId, listOf(post("one"), post("two")), hasMore = true)
            FeedAppendResult.Appended(newItemCount = 1, hasMore = true)
        }

        viewModel.loadMore()
        assertTrue((viewModel.uiState.value as FeedUiState.Content).isAppending)
        advanceUntilIdle()

        val content = viewModel.uiState.value as FeedUiState.Content
        assertEquals(listOf("one", "two"), content.posts.map(FeedPost::id))
        assertFalse(content.isAppending)
    }

    @Test
    fun appendErrorPreservesListAndExposesRetryState() = runTest(dispatcher) {
        repository.emit("account-a", listOf(post("one")), hasMore = true)
        val viewModel = FeedViewModel(repository, storyRepository, auth)
        advanceUntilIdle()
        repository.onAppend = { throw IOException("append offline") }

        viewModel.loadMore()
        advanceUntilIdle()

        val content = viewModel.uiState.value as FeedUiState.Content
        assertEquals(listOf("one"), content.posts.map(FeedPost::id))
        assertEquals("append offline", content.appendError)
        assertFalse(content.isAppending)
    }

    @Test
    fun cachedRefreshFailureBecomesOfflineAndStale() = runTest(dispatcher) {
        repository.emit("account-a", listOf(post("cached")), hasMore = true)
        repository.onRefresh = { throw IOException("offline") }

        val viewModel = FeedViewModel(repository, storyRepository, auth)
        advanceUntilIdle()

        val content = viewModel.uiState.value as FeedUiState.Content
        assertEquals(listOf("cached"), content.posts.map(FeedPost::id))
        assertTrue(content.isOffline)
        assertTrue(content.isStale)
        assertEquals("به‌روزرسانی فید ناموفق بود", content.refreshError)
    }

    @Test
    fun endReachedPreventsPaginationTrigger() = runTest(dispatcher) {
        repository.emit("account-a", listOf(post("last")), hasMore = false)
        repository.onRefresh = { FeedRefreshResult(itemCount = 1, hasMore = false) }
        val viewModel = FeedViewModel(repository, storyRepository, auth)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(0, repository.appendCalls)
        assertFalse((viewModel.uiState.value as FeedUiState.Content).hasMore)
    }

    @Test
    fun duplicatePaginationTriggerIsIgnoredWhileAppendRuns() = runTest(dispatcher) {
        repository.emit("account-a", listOf(post("one")), hasMore = true)
        val viewModel = FeedViewModel(repository, storyRepository, auth)
        advanceUntilIdle()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        repository.onAppend = {
            entered.complete(Unit)
            release.await()
            FeedAppendResult.Appended(newItemCount = 0, hasMore = true)
        }

        viewModel.loadMore()
        viewModel.loadMore()
        dispatcher.scheduler.runCurrent()
        entered.await()
        assertEquals(1, repository.appendCalls)
        release.complete(Unit)
        advanceUntilIdle()

        assertFalse((viewModel.uiState.value as FeedUiState.Content).isAppending)
    }

    private fun signedIn() = AuthenticationState.SignedIn(
        AuthenticatedContext(
            userId = "account-a",
            profileCompleted = true,
            passwordRequired = false,
            offline = false,
            displayName = "Vista User",
        ),
    )

    private fun post(id: String) = FeedPost(
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
        likeCount = 1,
        commentCount = 2,
        isLiked = false,
        isSaved = false,
        hideLikeCount = false,
        hideCommentCount = false,
        authorUsername = "author",
        authorFullName = "Author",
        authorAvatarUrl = null,
        authorIsVerified = false,
        authorVerificationType = null,
        createdAt = "2026-07-27T17:21:49Z",
    )
}

private class FakeAuthenticationStateProvider(
    initial: AuthenticationState,
) : AuthenticationStateProvider {
    private val mutableState = MutableStateFlow(initial)
    override val state: StateFlow<AuthenticationState> = mutableState
}

private class FakeFeedRepository : FeedRepository {
    private val snapshots = mutableMapOf<String, MutableStateFlow<FeedSnapshot>>()
    var onRefresh: suspend (String) -> FeedRefreshResult = {
        val snapshot = snapshots[it]?.value ?: FeedSnapshot(emptyList(), true, 0)
        FeedRefreshResult(snapshot.posts.size, snapshot.hasMore)
    }
    var onAppend: suspend (String) -> FeedAppendResult = {
        FeedAppendResult.Appended(newItemCount = 0, hasMore = true)
    }
    var appendCalls = 0

    override fun observeFeed(accountId: String, kind: FeedKind): Flow<FeedSnapshot> =
        snapshots.getOrPut(accountId) {
            MutableStateFlow(FeedSnapshot(emptyList(), hasMore = true, nextOffset = 0))
        }

    override fun getPostById(accountId: String, postId: String): Flow<FeedPost?> =
        flowOf(snapshots[accountId]?.value?.posts?.firstOrNull { it.id == postId })

    override fun observeUserPosts(accountId: String, userId: String): Flow<FeedSnapshot> =
        observeFeed(accountId, FeedKind.Explore)

    override suspend fun refreshFeed(
        accountId: String,
        kind: FeedKind,
    ): FeedRefreshResult = onRefresh(accountId)

    override suspend fun loadMoreFeed(
        accountId: String,
        kind: FeedKind,
    ): FeedAppendResult {
        appendCalls += 1
        return onAppend(accountId)
    }

    override suspend fun refreshPost(accountId: String, postId: String): FeedPost =
        snapshots[accountId]?.value?.posts?.first { it.id == postId }
            ?: error("missing")

    override suspend fun refreshUserPosts(
        accountId: String,
        userId: String,
    ): FeedRefreshResult = onRefresh(accountId)

    override suspend fun loadMoreUserPosts(
        accountId: String,
        userId: String,
    ): FeedAppendResult = onAppend(accountId)

    override suspend fun getHashtagPosts(accountId: String, hashtag: String, offset: Int): FeedSnapshot =
        FeedSnapshot(emptyList(), hasMore = false, nextOffset = offset)

    override suspend fun clearAccount(accountId: String) {
        snapshots.remove(accountId)
    }

    override suspend fun toggleLike(accountId: String, postId: String, ownerId: String, isLiked: Boolean, newLikeCount: Long) {}
    override suspend fun toggleSave(accountId: String, postId: String, isSaved: Boolean) {}
    override suspend fun updatePost(accountId: String, postId: String, content: String?, hideLikeCount: Boolean?, hideCommentCount: Boolean?) = error("unused")
    override suspend fun deletePost(accountId: String, postId: String) = Unit
    override suspend fun reportPost(postId: String, reportedUserId: String, reason: String, additionalDetails: String?) = Unit
    override suspend fun submitAppeal(postId: String, reason: String) = Unit
    override suspend fun trackFeedEvent(postId: String, eventType: String) = Unit

    fun emit(accountId: String, posts: List<FeedPost>, hasMore: Boolean) {
        snapshots.getOrPut(accountId) {
            MutableStateFlow(FeedSnapshot(emptyList(), true, 0))
        }.value = FeedSnapshot(
            posts = posts,
            hasMore = hasMore,
            nextOffset = posts.size,
        )
    }
}
