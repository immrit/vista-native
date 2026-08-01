package ir.coffevista.vista_native.features.search.data

import ir.coffevista.vista_native.core.database.search.SearchHistoryDao
import ir.coffevista.vista_native.core.database.search.SearchHistoryEntity
import ir.coffevista.vista_native.core.network.RemoteFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class DefaultSearchRepositoryTest {
    @Test
    fun queryNormalizationMatchesFlutterEntryRules() {
        assertEquals("sample_user", normalizeUserQuery("  @@sample_user "))
        assertEquals("ویستا", normalizeHashtag("  ##وی ستا "))
        assertEquals("Vista", normalizeHashtagSuggestion(" #Vista "))
    }

    @Test
    fun tagEligibilityAcceptsPersianEnglishAndMixedButRejectsPhrases() {
        assertTrue(canSearchAsTag("ویستا"))
        assertTrue(canSearchAsTag("Vista_ویستا"))
        assertTrue(canSearchAsTag("#anything here"))
        assertFalse(canSearchAsTag("two words"))
        assertFalse(canSearchAsTag(""))
    }

    @Test
    fun emptyUserQueryDoesNotCallNetwork() = runTest {
        val api = FakeSearchApi()
        val page = repository(api).searchUsers(" @ ", 0)
        assertTrue(page.users.isEmpty())
        assertEquals(0, api.profileSearchCalls)
    }

    @Test
    fun exactUsernameIsPrependedAndDeduplicated() = runTest {
        val api = FakeSearchApi().apply {
            usersResponse = SearchProfilesResponseDto(
                listOf(
                    SearchUserDto("same", "SAMPLE_USER", "Search Name", null),
                    SearchUserDto("other", "sample_user_alt", "Other", null),
                ),
            )
            exactResponse = ExactProfileDto("same", "sample_user", "Exact Name", null, true)
        }
        val page = repository(api).searchUsers("@Sample_User", 0)
        assertEquals(listOf("same", "other"), page.users.map(SearchUser::id))
        assertEquals("Exact Name", page.users.first().fullName)
        assertTrue(page.users.first().isVerified)
        assertEquals("sample_user", api.exactUsername)
        assertEquals(2, page.rawCount)
    }

    @Test
    fun laterUserPageSkipsExactLookupAndPreservesOffsetContract() = runTest {
        val api = FakeSearchApi()
        repository(api).searchUsers("sample_user", 20)
        assertEquals(20, api.userOffset)
        assertEquals(20, api.userLimit)
        assertEquals(0, api.exactCalls)
    }

    @Test
    fun nullableAndInvalidUsersAreDroppedSafely() = runTest {
        val api = FakeSearchApi().apply {
            usersResponse = SearchProfilesResponseDto(
                listOf(
                    SearchUserDto(null, "missing-id", "Bad", null),
                    SearchUserDto("ok", null, "Fallback Name", null),
                ),
            )
        }
        val users = repository(api).searchUsers("نام کامل", 0).users
        assertEquals(1, users.size)
        assertEquals("Fallback Name", users.single().username)
    }

    @Test
    fun hashtagRequestUsesBackendLimitAndNormalizedTag() = runTest {
        val api = FakeSearchApi()
        repository(api).searchPostsByHashtag(" #Vi Sta ")
        assertEquals("vista", api.postTag)
        assertEquals(15, api.postLimit)
        assertEquals(0, api.postOffset)
    }

    @Test
    fun postMapperDeduplicatesAndUsesImageArrayFallback() = runTest {
        val api = FakeSearchApi().apply {
            postsResponse = SearchPostsResponseDto(
                posts = listOf(
                    SearchPostDto("one", null, listOf("fallback.jpg"), null),
                    SearchPostDto("one", "duplicate.jpg", emptyList(), null),
                    SearchPostDto(null, "invalid.jpg", emptyList(), null),
                ),
                hasMore = true,
                nextCursor = " cursor ",
            )
        }
        val page = repository(api).searchPostsByHashtag("#vista")
        assertEquals(1, page.posts.size)
        assertEquals("fallback.jpg", page.posts.single().imageUrl)
        assertTrue(page.hasMore)
        assertEquals("cursor", page.nextCursor)
    }

    @Test
    fun hashtagSuggestionsAreCaseInsensitiveDeduplicated() = runTest {
        val api = FakeSearchApi().apply {
            hashtagResponse = HashtagResponseDto(
                listOf(
                    HashtagSuggestionDto("Vista", 2),
                    HashtagSuggestionDto("vista", 3),
                    HashtagSuggestionDto(null, 4),
                ),
            )
        }
        val result = repository(api).searchHashtags("#vis")
        assertEquals(1, result.size)
        assertEquals("vis", api.hashtagKeyword)
        assertEquals(10, api.hashtagLimit)
    }

    @Test
    fun errorEnvelopeBecomesRemoteFailure() = runTest {
        val api = FakeSearchApi().apply {
            profileError = Response.error(
                429,
                """{"code":"rate_limited","message":"slow"}"""
                    .toResponseBody("application/json".toMediaType()),
            )
        }
        val error = runCatching { repository(api).searchUsers("نام کامل", 0) }.exceptionOrNull()
        if (error !is RemoteFailure) throw AssertionError("Unexpected failure", error)
        assertEquals(429, error.statusCode)
        assertEquals("rate_limited", error.code)
    }

    @Test
    fun historyDuplicateMovesToFrontAndKeepsSingleRow() = runTest {
        val dao = FakeHistoryDao()
        var now = 1L
        val repository = DefaultSearchRepository(FakeSearchApi(), dao) { now++ }
        repository.addHistory("a", "#one", SearchHistoryType.Hashtag)
        repository.addHistory("a", "@two", SearchHistoryType.User)
        repository.addHistory("a", "#one", SearchHistoryType.Hashtag)
        val history = repository.observeHistory("a").first()
        assertEquals(listOf("#one", "@two"), history.map(SearchHistoryItem::query))
    }

    @Test
    fun historyIsCappedAtTwentyAndDisplayAtTwelve() = runTest {
        val dao = FakeHistoryDao()
        var now = 0L
        val repository = DefaultSearchRepository(FakeSearchApi(), dao) { ++now }
        repeat(25) { repository.addHistory("a", "#$it", SearchHistoryType.Hashtag) }
        assertEquals(20, dao.rowsFor("a").size)
        assertEquals(12, repository.observeHistory("a").first().size)
    }

    @Test
    fun historyIsIsolatedByAccount() = runTest {
        val dao = FakeHistoryDao()
        val repository = DefaultSearchRepository(FakeSearchApi(), dao) { 1L }
        repository.addHistory("a", "#private-a", SearchHistoryType.Hashtag)
        repository.addHistory("b", "#private-b", SearchHistoryType.Hashtag)
        assertEquals(listOf("#private-a"), repository.observeHistory("a").first().map { it.query })
        assertEquals(listOf("#private-b"), repository.observeHistory("b").first().map { it.query })
    }

    @Test
    fun historyDeleteRemovesOnlyRequestedItem() = runTest {
        val dao = FakeHistoryDao()
        val repository = DefaultSearchRepository(FakeSearchApi(), dao) { 1L }
        repository.addHistory("a", "#one", SearchHistoryType.Hashtag)
        repository.addHistory("a", "#two", SearchHistoryType.Hashtag)
        repository.deleteHistory("a", "#one")
        assertEquals(listOf("#two"), repository.observeHistory("a").first().map { it.query })
    }

    @Test
    fun historyClearAndLogoutClearOnlyRequestedAccount() = runTest {
        val dao = FakeHistoryDao()
        val repository = DefaultSearchRepository(FakeSearchApi(), dao) { 1L }
        repository.addHistory("a", "#one", SearchHistoryType.Hashtag)
        repository.addHistory("b", "#two", SearchHistoryType.Hashtag)
        repository.clearAccount("a")
        assertTrue(repository.observeHistory("a").first().isEmpty())
        assertEquals(1, repository.observeHistory("b").first().size)
    }

    private fun repository(api: FakeSearchApi) =
        DefaultSearchRepository(api, FakeHistoryDao()) { 1L }
}

private class FakeSearchApi : SearchApi {
    var usersResponse = SearchProfilesResponseDto()
    var exactResponse = ExactProfileDto()
    var postsResponse = SearchPostsResponseDto()
    var hashtagResponse = HashtagResponseDto()
    var trendingResponse = HashtagResponseDto()
    var profileError: Response<SearchProfilesResponseDto>? = null
    var profileSearchCalls = 0
    var exactCalls = 0
    var userOffset = -1
    var userLimit = -1
    var exactUsername: String? = null
    var postTag: String? = null
    var postLimit = -1
    var postOffset = -1
    var hashtagKeyword: String? = null
    var hashtagLimit = -1

    override suspend fun searchProfiles(
        query: String,
        limit: Int,
        offset: Int,
    ): Response<SearchProfilesResponseDto> {
        profileSearchCalls++
        userOffset = offset
        userLimit = limit
        return profileError ?: Response.success(usersResponse)
    }

    override suspend fun profileByUsername(username: String): Response<ExactProfileDto> {
        exactCalls++
        exactUsername = username
        return Response.success(exactResponse)
    }

    override suspend fun searchPostsByHashtag(
        tag: String,
        limit: Int,
        offset: Int,
    ): Response<SearchPostsResponseDto> {
        postTag = tag
        postLimit = limit
        postOffset = offset
        return Response.success(postsResponse)
    }

    override suspend fun searchHashtags(
        keyword: String,
        limit: Int,
    ): Response<HashtagResponseDto> {
        hashtagKeyword = keyword
        hashtagLimit = limit
        return Response.success(hashtagResponse)
    }

    override suspend fun trendingHashtags(
        limit: Int,
        days: Int,
    ): Response<HashtagResponseDto> = Response.success(trendingResponse)
}

private class FakeHistoryDao : SearchHistoryDao {
    private val rows = mutableMapOf<String, MutableStateFlow<List<SearchHistoryEntity>>>()

    override fun observeRecent(
        accountId: String,
        limit: Int,
    ): Flow<List<SearchHistoryEntity>> =
        flow(accountId).map { items -> items.take(limit) }

    override suspend fun upsert(entity: SearchHistoryEntity) {
        val current = flow(entity.accountId).value
        flow(entity.accountId).value = (current.filterNot { it.query == entity.query } + entity)
            .sortedByDescending(SearchHistoryEntity::timestampEpochMillis)
    }

    override suspend fun trimToSize(accountId: String, maxSize: Int) {
        flow(accountId).value = flow(accountId).value.take(maxSize)
    }

    override suspend fun delete(accountId: String, query: String) {
        flow(accountId).value = flow(accountId).value.filterNot { it.query == query }
    }

    override suspend fun clearAccount(accountId: String) {
        flow(accountId).value = emptyList()
    }

    fun rowsFor(accountId: String): List<SearchHistoryEntity> = flow(accountId).value

    private fun flow(accountId: String) =
        rows.getOrPut(accountId) { MutableStateFlow(emptyList()) }
}
