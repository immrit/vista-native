package ir.coffevista.vista_native.features.profile.data

import ir.coffevista.vista_native.core.database.profile.PublicProfileDao
import ir.coffevista.vista_native.core.database.profile.PublicProfileEntity
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class OfflineFirstUserProfileRepositoryTest {
    private lateinit var dao: FakePublicProfileDao
    private lateinit var api: FakePublicProfileApi
    private lateinit var repository: OfflineFirstUserProfileRepository

    @Before
    fun setUp() {
        dao = FakePublicProfileDao()
        api = FakePublicProfileApi()
        repository = OfflineFirstUserProfileRepository(api, dao) { 123L }
    }

    @Test
    fun cacheFirstEmissionDoesNotCallNetwork() = runTest {
        dao.upsert(entity("viewer-a", "user-b"))

        val profile = repository.observeProfile("viewer-a", "user-b").first()

        assertEquals("user-b", profile?.userId)
        assertEquals(0, api.profileCalls)
    }

    @Test
    fun refreshSuccessUpdatesCache() = runTest {
        api.profileResponse = Response.success(dto(followers = 9))

        val result = repository.refreshProfile("viewer-a", "user-b")

        assertEquals(ProfileRefreshResult.Success, result)
        assertEquals(9L, dao.get("viewer-a", "user-b")?.followerCount)
        assertEquals(123L, dao.get("viewer-a", "user-b")?.lastSyncedEpochMillis)
    }

    @Test
    fun refreshFailureWithCachePreservesDataAndMarksHadCache() = runTest {
        val cached = entity("viewer-a", "user-b")
        dao.upsert(cached)
        api.profileFailure = IOException("offline")

        val result = repository.refreshProfile("viewer-a", "user-b")

        assertTrue((result as ProfileRefreshResult.Failure).hadCache)
        assertEquals(cached, dao.get("viewer-a", "user-b"))
    }

    @Test
    fun refreshFailureWithoutCacheReportsNoCache() = runTest {
        api.profileFailure = IOException("offline")

        val result = repository.refreshProfile("viewer-a", "user-b")

        assertFalse((result as ProfileRefreshResult.Failure).hadCache)
        assertNull(dao.get("viewer-a", "user-b"))
    }

    @Test
    fun accountIsolationAndClearOnlyRemoveRequestedViewer() = runTest {
        dao.upsert(entity("viewer-a", "user-b"))
        dao.upsert(entity("viewer-c", "user-b"))

        repository.clearAccount("viewer-a")

        assertNull(dao.get("viewer-a", "user-b"))
        assertEquals("user-b", dao.get("viewer-c", "user-b")?.profileUserId)
    }

    @Test
    fun selfProfileReturnsRedirectResultWithoutNetwork() = runTest {
        val result = repository.refreshProfile("viewer-a", "viewer-a")

        assertEquals(ProfileRefreshResult.SelfProfile, result)
        assertEquals(0, api.profileCalls)
    }

    @Test
    fun followSuccessOptimisticallyUpdatesThenReconciles() = runTest {
        dao.upsert(entity("viewer-a", "user-b", followers = 4))
        api.followResponse = Response.success(
            FollowActionResponseDto("following", "ok"),
        )

        val result = repository.follow("viewer-a", "user-b")

        assertEquals(FollowMutationResult.Success(FollowState.Following), result)
        assertEquals("following", dao.get("viewer-a", "user-b")?.followStatus)
        assertEquals(5L, dao.get("viewer-a", "user-b")?.followerCount)
    }

    @Test
    fun privateFollowReconcilesRequestedWithoutIncrementingCount() = runTest {
        dao.upsert(entity("viewer-a", "user-b", followers = 4))
        api.followResponse = Response.success(
            FollowActionResponseDto("requested", "pending"),
        )

        val result = repository.follow("viewer-a", "user-b")

        assertEquals(FollowMutationResult.Success(FollowState.Requested), result)
        assertEquals("requested", dao.get("viewer-a", "user-b")?.followStatus)
        assertEquals(4L, dao.get("viewer-a", "user-b")?.followerCount)
    }

    @Test
    fun unfollowSuccessDecrementsCount() = runTest {
        dao.upsert(
            entity(
                "viewer-a",
                "user-b",
                followers = 4,
                followStatus = "following",
            ),
        )
        api.unfollowResponse = Response.success(UnfollowResponseDto("unfollowed"))

        val result = repository.unfollow("viewer-a", "user-b")

        assertEquals(FollowMutationResult.Success(FollowState.NotFollowing), result)
        assertEquals(3L, dao.get("viewer-a", "user-b")?.followerCount)
        assertEquals("none", dao.get("viewer-a", "user-b")?.followStatus)
    }

    @Test
    fun followFailureRollsBackStateAndCount() = runTest {
        val original = entity("viewer-a", "user-b", followers = 4)
        dao.upsert(original)
        api.followFailure = IOException("offline")

        val result = repository.follow("viewer-a", "user-b")

        assertTrue(result is FollowMutationResult.Failure)
        assertEquals(original, dao.get("viewer-a", "user-b"))
    }

    @Test
    fun unfollowFailureRollsBackStateAndCount() = runTest {
        val original = entity(
            "viewer-a",
            "user-b",
            followers = 4,
            followStatus = "following",
        )
        dao.upsert(original)
        api.unfollowResponse = Response.error(
            429,
            """{"error":{"code":"error","message":"rate limit exceeded"}}"""
                .toResponseBody("application/json".toMediaType()),
        )

        val result = repository.unfollow("viewer-a", "user-b")

        assertTrue(result is FollowMutationResult.Failure)
        assertEquals(original, dao.get("viewer-a", "user-b"))
    }

    @Test
    fun concurrentMutationIsIgnored() = runTest {
        dao.upsert(entity("viewer-a", "user-b"))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        api.followHandler = {
            entered.complete(Unit)
            release.await()
            Response.success(FollowActionResponseDto("following", "ok"))
        }

        val first = async { repository.follow("viewer-a", "user-b") }
        entered.await()
        val second = repository.follow("viewer-a", "user-b")
        release.complete(Unit)

        assertEquals(FollowMutationResult.IgnoredConcurrent, second)
        assertTrue(first.await() is FollowMutationResult.Success)
        assertEquals(1, api.followCalls)
    }

    @Test
    fun canonicalRefreshOverwritesOptimisticRelationship() = runTest {
        dao.upsert(entity("viewer-a", "user-b", followers = 4))
        api.followResponse = Response.success(FollowActionResponseDto("following", "ok"))
        repository.follow("viewer-a", "user-b")
        api.profileResponse = Response.success(dto(followers = 8, followStatus = "none"))

        repository.refreshProfile("viewer-a", "user-b")

        assertEquals("none", dao.get("viewer-a", "user-b")?.followStatus)
        assertEquals(8L, dao.get("viewer-a", "user-b")?.followerCount)
    }

    private fun dto(
        followers: Long = 4,
        followStatus: String = "none",
    ) = PublicProfileDto(
        userId = "user-b",
        username = "vista",
        fullName = "Vista User",
        bio = null,
        avatarUrl = null,
        isVerified = false,
        verificationType = null,
        isPrivate = false,
        isBlocked = false,
        subscriptionPlan = null,
        premiumDaysRemaining = null,
        postCount = 1,
        followerCount = followers,
        followingCount = 2,
        followStatus = followStatus,
        updatedAt = "2026-07-28T09:30:00Z",
    )

    private fun entity(
        viewer: String,
        profile: String,
        followers: Long = 4,
        followStatus: String = "none",
    ) = PublicProfileEntity(
        viewerAccountId = viewer,
        profileUserId = profile,
        username = "vista",
        fullName = "Vista User",
        bio = null,
        avatarUrl = null,
        isVerified = false,
        verificationType = null,
        isPrivate = false,
        isBlocked = false,
        subscriptionPlan = null,
        premiumDaysRemaining = null,
        postCount = 1,
        followerCount = followers,
        followingCount = 2,
        followStatus = followStatus,
        updatedAt = "2026-07-28T09:30:00Z",
        lastSyncedEpochMillis = 1,
    )
}

private class FakePublicProfileApi : PublicProfileApi {
    var profileResponse: Response<PublicProfileDto> = Response.success(
        PublicProfileDto(
            userId = "user-b",
            username = null,
            fullName = "Vista User",
            bio = null,
            avatarUrl = null,
            isVerified = false,
            verificationType = null,
            isPrivate = false,
            isBlocked = false,
            subscriptionPlan = null,
            premiumDaysRemaining = null,
            postCount = 0,
            followerCount = 0,
            followingCount = 0,
            followStatus = "none",
            updatedAt = "2026-07-28T09:30:00Z",
        ),
    )
    var followResponse: Response<FollowActionResponseDto> =
        Response.success(FollowActionResponseDto("following", "ok"))
    var unfollowResponse: Response<UnfollowResponseDto> =
        Response.success(UnfollowResponseDto("unfollowed"))
    var profileFailure: IOException? = null
    var followFailure: IOException? = null
    var followHandler: (suspend () -> Response<FollowActionResponseDto>)? = null
    var profileCalls = 0
    var followCalls = 0

    override suspend fun fetchPublicProfile(userId: String): Response<PublicProfileDto> {
        profileCalls += 1
        profileFailure?.let { throw it }
        return profileResponse
    }

    override suspend fun follow(
        request: FollowActionRequestDto,
    ): Response<FollowActionResponseDto> {
        followCalls += 1
        followFailure?.let { throw it }
        return followHandler?.invoke() ?: followResponse
    }

    override suspend fun unfollow(
        request: FollowActionRequestDto,
    ): Response<UnfollowResponseDto> = unfollowResponse
}

private class FakePublicProfileDao : PublicProfileDao {
    private val values = linkedMapOf<Pair<String, String>, PublicProfileEntity>()
    private val flows = mutableMapOf<Pair<String, String>, MutableStateFlow<PublicProfileEntity?>>()

    override fun observe(
        viewerAccountId: String,
        profileUserId: String,
    ): Flow<PublicProfileEntity?> = flows.getOrPut(viewerAccountId to profileUserId) {
        MutableStateFlow(values[viewerAccountId to profileUserId])
    }

    override suspend fun get(
        viewerAccountId: String,
        profileUserId: String,
    ): PublicProfileEntity? = values[viewerAccountId to profileUserId]

    override suspend fun upsert(profile: PublicProfileEntity) {
        val key = profile.viewerAccountId to profile.profileUserId
        values[key] = profile
        flows.getOrPut(key) { MutableStateFlow(null) }.value = profile
    }

    override suspend fun clearViewer(viewerAccountId: String) {
        values.keys.filter { it.first == viewerAccountId }.toList().forEach { key ->
            values.remove(key)
            flows.getOrPut(key) { MutableStateFlow(null) }.value = null
        }
    }

    override suspend fun clearAll() {
        values.clear()
        flows.values.forEach { it.value = null }
    }
}
