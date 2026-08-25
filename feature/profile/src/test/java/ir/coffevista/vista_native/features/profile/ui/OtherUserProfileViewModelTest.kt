package ir.coffevista.vista_native.features.profile.ui

import androidx.lifecycle.SavedStateHandle
import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.profile.data.FollowMutationResult
import ir.coffevista.vista_native.features.profile.data.FollowState
import ir.coffevista.vista_native.features.profile.data.ProfileRefreshResult
import ir.coffevista.vista_native.features.profile.data.PublicProfile
import ir.coffevista.vista_native.features.profile.data.UserProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OtherUserProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeUserProfileRepository
    private lateinit var auth: FakeOtherProfileAuthState

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeUserProfileRepository()
        auth = FakeOtherProfileAuthState(signedIn("viewer-a"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadingTransitionsToContent() = runTest(dispatcher) {
        repository.onRefresh = { viewer, profile ->
            repository.emit(viewer, profile, publicProfile())
            ProfileRefreshResult.Success
        }

        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.isInitialLoading)
        advanceUntilIdle()

        assertEquals("user-b", viewModel.uiState.value.profile?.userId)
        assertFalse(viewModel.uiState.value.isInitialLoading)
    }

    @Test
    fun initialLoadingTransitionsToError() = runTest(dispatcher) {
        repository.onRefresh = { _, _ ->
            ProfileRefreshResult.Failure(error(), hadCache = false)
        }

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("offline", viewModel.uiState.value.initialError?.messageFa)
        assertNull(viewModel.uiState.value.profile)
    }

    @Test
    fun refreshReplacesContentAndClearsStale() = runTest(dispatcher) {
        repository.emit("viewer-a", "user-b", publicProfile(fullName = "Cached"))
        repository.onRefresh = { _, _ ->
            ProfileRefreshResult.Failure(error(), hadCache = true)
        }
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isStale)
        val refreshRelease = CompletableDeferred<Unit>()
        repository.onRefresh = { viewer, profile ->
            repository.emit(viewer, profile, publicProfile(fullName = "Fresh"))
            refreshRelease.await()
            ProfileRefreshResult.Success
        }

        viewModel.onAction(OtherUserProfileAction.Refresh)
        runCurrent()
        assertTrue(viewModel.uiState.value.isRefreshing)
        refreshRelease.complete(Unit)
        advanceUntilIdle()

        assertEquals("Fresh", viewModel.uiState.value.profile?.fullName)
        assertFalse(viewModel.uiState.value.isStale)
    }

    @Test
    fun offlineFailureWithCacheKeepsContentAndShowsStale() = runTest(dispatcher) {
        repository.emit("viewer-a", "user-b", publicProfile(fullName = "Cached"))
        repository.onRefresh = { _, _ ->
            ProfileRefreshResult.Failure(error(), hadCache = true)
        }

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("Cached", viewModel.uiState.value.profile?.fullName)
        assertTrue(viewModel.uiState.value.isStale)
        assertNull(viewModel.uiState.value.initialError)
    }

    @Test
    fun followPendingTransitionsToSuccess() = runTest(dispatcher) {
        repository.emit("viewer-a", "user-b", publicProfile())
        val release = CompletableDeferred<Unit>()
        repository.onFollow = { viewer, profile ->
            repository.emit(
                viewer,
                profile,
                publicProfile(followState = FollowState.Following, followers = 5),
            )
            release.await()
            FollowMutationResult.Success(FollowState.Following)
        }
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(OtherUserProfileAction.FollowClicked)
        runCurrent()
        assertEquals(FollowMutationPending.Follow, viewModel.uiState.value.followMutationPending)
        assertEquals(FollowState.Following, viewModel.uiState.value.profile?.followState)
        release.complete(Unit)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.followMutationPending)
        assertEquals(5L, viewModel.uiState.value.profile?.followersCount)
    }

    @Test
    fun followFailureRollsBackAndExposesError() = runTest(dispatcher) {
        repository.emit("viewer-a", "user-b", publicProfile())
        repository.onFollow = { viewer, profile ->
            repository.emit(
                viewer,
                profile,
                publicProfile(followState = FollowState.Following, followers = 5),
            )
            repository.emit(viewer, profile, publicProfile())
            FollowMutationResult.Failure(error())
        }
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(OtherUserProfileAction.FollowClicked)
        advanceUntilIdle()

        assertEquals(FollowState.NotFollowing, viewModel.uiState.value.profile?.followState)
        assertEquals(4L, viewModel.uiState.value.profile?.followersCount)
        assertEquals("offline", viewModel.uiState.value.followMutationError?.messageFa)
    }

    @Test
    fun unfollowPendingTransitionsToSuccess() = runTest(dispatcher) {
        repository.emit(
            "viewer-a",
            "user-b",
            publicProfile(followState = FollowState.Following, followers = 5),
        )
        repository.onUnfollow = { viewer, profile ->
            repository.emit(viewer, profile, publicProfile())
            FollowMutationResult.Success(FollowState.NotFollowing)
        }
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(OtherUserProfileAction.UnfollowClicked)
        advanceUntilIdle()

        assertEquals(FollowState.NotFollowing, viewModel.uiState.value.profile?.followState)
        assertNull(viewModel.uiState.value.followMutationPending)
    }

    @Test
    fun duplicateClickIsIgnoredWhileMutationPending() = runTest(dispatcher) {
        repository.emit("viewer-a", "user-b", publicProfile())
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        repository.onFollow = { _, _ ->
            entered.complete(Unit)
            release.await()
            FollowMutationResult.Success(FollowState.Following)
        }
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(OtherUserProfileAction.FollowClicked)
        viewModel.onAction(OtherUserProfileAction.FollowClicked)
        runCurrent()
        entered.await()
        assertEquals(1, repository.followCalls)
        release.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun selfProfileEmitsRedirectWithoutRepositoryRefresh() = runTest(dispatcher) {
        auth = FakeOtherProfileAuthState(signedIn("user-b"))
        val viewModel = viewModel()
        val effect = async { viewModel.effects.first() }

        advanceUntilIdle()

        assertEquals(OtherUserProfileEffect.RedirectToOwnProfile, effect.await())
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun profileNotFoundUsesDedicatedState() = runTest(dispatcher) {
        repository.onRefresh = { _, _ ->
            ProfileRefreshResult.Failure(
                error = AppError(
                    kind = ErrorKind.VALIDATION,
                    messageFa = "پروفایل یافت نشد",
                    code = "PROFILE_NOT_FOUND",
                ),
                hadCache = false,
            )
        }

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.userNotFound)
        assertNull(viewModel.uiState.value.initialError)
    }

    private fun viewModel() = OtherUserProfileViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(OtherUserProfileViewModel.PROFILE_USER_ID_KEY to "user-b"),
        ),
        repository = repository,
        authenticationStateProvider = auth,
    )

    private fun publicProfile(
        fullName: String = "Vista User",
        followState: FollowState = FollowState.NotFollowing,
        followers: Long = 4,
    ) = PublicProfile(
        viewerAccountId = "viewer-a",
        userId = "user-b",
        username = "vista",
        fullName = fullName,
        bio = null,
        avatarUrl = null,
        isVerified = false,
        verificationType = null,
        isPrivate = false,
        isBlocked = false,
        isPremium = false,
        postsCount = 1,
        followersCount = followers,
        followingCount = 2,
        followState = followState,
        updatedAt = "2026-07-28T09:30:00Z",
        lastSyncedEpochMillis = 1,
    )

    private fun error() = AppError(ErrorKind.NETWORK, "offline")

    private fun signedIn(userId: String) = AuthenticationState.SignedIn(
        AuthenticatedContext(
            userId = userId,
            profileCompleted = true,
            passwordRequired = false,
            offline = false,
            displayName = "Viewer",
        ),
    )
}

private class FakeOtherProfileAuthState(
    initial: AuthenticationState,
) : AuthenticationStateProvider {
    private val mutableState = MutableStateFlow(initial)
    override val state: StateFlow<AuthenticationState> = mutableState
}

private class FakeUserProfileRepository : UserProfileRepository {
    private val profiles = mutableMapOf<Pair<String, String>, MutableStateFlow<PublicProfile?>>()
    var onRefresh: suspend (String, String) -> ProfileRefreshResult = { _, _ ->
        ProfileRefreshResult.Success
    }
    var onFollow: suspend (String, String) -> FollowMutationResult = { _, _ ->
        FollowMutationResult.Success(FollowState.Following)
    }
    var onUnfollow: suspend (String, String) -> FollowMutationResult = { _, _ ->
        FollowMutationResult.Success(FollowState.NotFollowing)
    }
    var refreshCalls = 0
    var followCalls = 0

    override fun observeProfile(
        viewerAccountId: String,
        profileUserId: String,
    ): Flow<PublicProfile?> = profiles.getOrPut(viewerAccountId to profileUserId) {
        MutableStateFlow(null)
    }

    override suspend fun refreshProfile(
        viewerAccountId: String,
        profileUserId: String,
    ): ProfileRefreshResult {
        refreshCalls += 1
        return onRefresh(viewerAccountId, profileUserId)
    }

    override suspend fun follow(
        viewerAccountId: String,
        profileUserId: String,
    ): FollowMutationResult {
        followCalls += 1
        return onFollow(viewerAccountId, profileUserId)
    }

    override suspend fun unfollow(
        viewerAccountId: String,
        profileUserId: String,
    ): FollowMutationResult = onUnfollow(viewerAccountId, profileUserId)

    override suspend fun clearAccount(viewerAccountId: String) {
        profiles.keys.filter { it.first == viewerAccountId }.forEach {
            profiles[it]?.value = null
        }
    }

    fun emit(viewer: String, profileId: String, profile: PublicProfile) {
        profiles.getOrPut(viewer to profileId) { MutableStateFlow(null) }.value = profile
    }
}
