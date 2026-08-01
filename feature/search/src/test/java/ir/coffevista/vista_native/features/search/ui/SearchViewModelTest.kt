package ir.coffevista.vista_native.features.search.ui

import androidx.lifecycle.SavedStateHandle
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.features.auth.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthenticationStateProvider
import ir.coffevista.vista_native.features.search.data.HashtagSuggestion
import ir.coffevista.vista_native.features.search.data.PostSearchPage
import ir.coffevista.vista_native.features.search.data.SearchHistoryItem
import ir.coffevista.vista_native.features.search.data.SearchHistoryType
import ir.coffevista.vista_native.features.search.data.SearchPost
import ir.coffevista.vista_native.features.search.data.SearchRepository
import ir.coffevista.vista_native.features.search.data.SearchUser
import ir.coffevista.vista_native.features.search.data.UserSearchPage
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeSearchRepository
    private lateinit var auth: FakeSearchAuth

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeSearchRepository()
        auth = FakeSearchAuth(signedIn("account-a"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blankQueryMovesBetweenIdleAndFocusedWithoutRequest() = runTest(dispatcher) {
        val viewModel = create()
        runCurrent()
        viewModel.onFocusChanged(true)
        assertEquals(SearchPhase.Focused, viewModel.uiState.value.phase)
        viewModel.onFocusChanged(false)
        assertEquals(SearchPhase.Idle, viewModel.uiState.value.phase)
        assertEquals(0, repository.userCalls)
    }

    @Test
    fun debounceWaitsExactlyThreeHundredMilliseconds() = runTest(dispatcher) {
        val viewModel = create()
        runCurrent()
        viewModel.onQueryChanged("vista")
        advanceTimeBy(299)
        runCurrent()
        assertEquals(0, repository.userCalls)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, repository.userCalls)
    }

    @Test
    fun submitBypassesDebounceAndDuplicateSubmitIsSuppressed() = runTest(dispatcher) {
        val viewModel = create()
        runCurrent()
        viewModel.onQueryChanged("vista")
        viewModel.submit()
        advanceUntilIdle()
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(1, repository.userCalls)
    }

    @Test
    fun latestQueryWinsAndPreviousRequestIsCancelled() = runTest(dispatcher) {
        val firstGate = CompletableDeferred<UserSearchPage>()
        repository.onUsers = { query, _ ->
            if (query == "first") firstGate.await() else UserSearchPage(listOf(user("second")), 1)
        }
        val viewModel = create()
        runCurrent()
        viewModel.onQueryChanged("first")
        advanceTimeBy(300)
        runCurrent()
        viewModel.onQueryChanged("second")
        advanceTimeBy(300)
        advanceUntilIdle()
        firstGate.complete(UserSearchPage(listOf(user("first")), 1))
        advanceUntilIdle()
        assertEquals(listOf("second"), viewModel.uiState.value.users.map(SearchUser::id))
    }

    @Test
    fun successfulUserSearchProducesContent() = runTest(dispatcher) {
        repository.onUsers = { _, _ -> UserSearchPage(listOf(user("one")), 1) }
        val viewModel = create()
        runCurrent()
        viewModel.onQueryChanged("نام کامل")
        advanceUntilIdle()
        assertEquals(SearchPhase.Content, viewModel.uiState.value.phase)
        assertEquals("one", viewModel.uiState.value.users.single().id)
    }

    @Test
    fun noResultsProducesEmptyState() = runTest(dispatcher) {
        val viewModel = create()
        runCurrent()
        viewModel.onQueryChanged("two words")
        advanceUntilIdle()
        assertEquals(SearchPhase.Empty, viewModel.uiState.value.phase)
    }

    @Test
    fun directHashtagFailureProducesErrorAndRetryRecovers() = runTest(dispatcher) {
        repository.onPosts = { throw IOException("offline") }
        val viewModel = create()
        runCurrent()
        viewModel.onQueryChanged("#vista")
        advanceUntilIdle()
        assertEquals(SearchPhase.Error, viewModel.uiState.value.phase)
        repository.onPosts = { PostSearchPage(listOf(post("p1")), false, null) }
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(SearchPhase.Content, viewModel.uiState.value.phase)
    }

    @Test
    fun hashtagQuerySelectsTagsAndLoadsPosts() = runTest(dispatcher) {
        repository.onPosts = { PostSearchPage(listOf(post("p1")), true, "cursor") }
        val viewModel = create()
        runCurrent()
        viewModel.onQueryChanged("#vista")
        advanceUntilIdle()
        assertEquals(SearchTab.Tags, viewModel.uiState.value.selectedTab)
        assertEquals("p1", viewModel.uiState.value.posts.single().id)
        assertTrue(viewModel.uiState.value.postHasMore)
    }

    @Test
    fun paginationDeduplicatesAndAdvancesByRawServerCount() = runTest(dispatcher) {
        repository.onUsers = { _, offset ->
            if (offset == 0) {
                UserSearchPage(List(20) { user("u$it") }, 20)
            } else {
                UserSearchPage(listOf(user("u19"), user("u20")), 2)
            }
        }
        val viewModel = create()
        runCurrent()
        viewModel.onQueryChanged("people query")
        advanceUntilIdle()
        viewModel.loadMoreUsers()
        advanceUntilIdle()
        assertEquals(21, viewModel.uiState.value.users.size)
        assertEquals(22, viewModel.uiState.value.nextUserOffset)
        assertFalse(viewModel.uiState.value.hasMoreUsers)
    }

    @Test
    fun appendFailureKeepsExistingContentAndExposesRetryMessage() = runTest(dispatcher) {
        repository.onUsers = { _, offset ->
            if (offset == 0) UserSearchPage(List(20) { user("u$it") }, 20)
            else throw IOException("append offline")
        }
        val viewModel = create()
        runCurrent()
        viewModel.onQueryChanged("people query")
        advanceUntilIdle()
        viewModel.loadMoreUsers()
        advanceUntilIdle()
        assertEquals(20, viewModel.uiState.value.users.size)
        assertTrue(viewModel.uiState.value.appendErrorMessage != null)
    }

    @Test
    fun historyActionsAreScopedToAuthenticatedAccount() = runTest(dispatcher) {
        val viewModel = create()
        runCurrent()
        viewModel.selectHashtag("vista")
        advanceUntilIdle()
        viewModel.deleteHistory("#vista")
        viewModel.clearHistory()
        advanceUntilIdle()
        assertEquals(
            listOf(
                "add:account-a:#vista:Hashtag",
                "delete:account-a:#vista",
                "clear:account-a",
            ),
            repository.historyActions,
        )
    }

    @Test
    fun savedQueryAndTabAreRestoredAfterRecreation() = runTest(dispatcher) {
        repository.onUsers = { _, _ -> UserSearchPage(listOf(user("restored")), 1) }
        val handle = SavedStateHandle(
            mapOf("search.query" to "restored query", "search.tab" to SearchTab.People.index),
        )
        val viewModel = SearchViewModel(repository, auth, handle)
        advanceUntilIdle()
        assertEquals("restored query", viewModel.uiState.value.query)
        assertEquals("restored", viewModel.uiState.value.users.single().id)
    }

    @Test
    fun accountChangeCancelsAndResetsQueryWithoutClearingOtherAccountHistory() = runTest(dispatcher) {
        val viewModel = create()
        runCurrent()
        viewModel.onQueryChanged("private query")
        auth.emit(signedIn("account-b"))
        advanceUntilIdle()
        assertEquals("account-b", viewModel.uiState.value.accountId)
        assertEquals("", viewModel.uiState.value.query)
        assertTrue(repository.historyActions.isEmpty())
    }

    private fun create() = SearchViewModel(repository, auth, SavedStateHandle())

    private fun user(id: String) = SearchUser(
        id = id,
        username = id,
        fullName = "User $id",
        avatarUrl = null,
        isVerified = false,
        verificationType = null,
        role = null,
    )

    private fun post(id: String) = SearchPost(id, null, null)

    private fun signedIn(id: String) = AuthenticationState.SignedIn(
        AuthenticatedContext(
            userId = id,
            profileCompleted = true,
            passwordRequired = false,
            offline = false,
            displayName = "Vista User",
        ),
    )
}

private class FakeSearchAuth(initial: AuthenticationState) : AuthenticationStateProvider {
    private val mutable = MutableStateFlow(initial)
    override val state: StateFlow<AuthenticationState> = mutable
    fun emit(value: AuthenticationState) {
        mutable.value = value
    }
}

private class FakeSearchRepository : SearchRepository {
    var userCalls = 0
    var onUsers: suspend (String, Int) -> UserSearchPage = { _, _ ->
        UserSearchPage(emptyList(), 0)
    }
    var onPosts: suspend (String) -> PostSearchPage = {
        PostSearchPage(emptyList(), false, null)
    }
    val historyActions = mutableListOf<String>()
    private val histories = mutableMapOf<String, MutableStateFlow<List<SearchHistoryItem>>>()

    override suspend fun searchUsers(query: String, offset: Int): UserSearchPage {
        userCalls++
        return onUsers(query, offset)
    }

    override suspend fun searchPostsByHashtag(query: String): PostSearchPage = onPosts(query)
    override suspend fun searchHashtags(query: String): List<HashtagSuggestion> = emptyList()
    override suspend fun trendingHashtags(): List<HashtagSuggestion> = emptyList()

    override fun observeHistory(accountId: String): Flow<List<SearchHistoryItem>> =
        histories.getOrPut(accountId) { MutableStateFlow(emptyList()) }

    override suspend fun addHistory(
        accountId: String,
        query: String,
        type: SearchHistoryType,
    ) {
        historyActions += "add:$accountId:$query:$type"
    }

    override suspend fun deleteHistory(accountId: String, query: String) {
        historyActions += "delete:$accountId:$query"
    }

    override suspend fun clearHistory(accountId: String) {
        historyActions += "clear:$accountId"
    }

    override suspend fun clearAccount(accountId: String) {
        historyActions += "clear:$accountId"
    }
}
