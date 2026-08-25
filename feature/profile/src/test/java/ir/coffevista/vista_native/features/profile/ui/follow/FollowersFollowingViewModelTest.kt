package ir.coffevista.vista_native.features.profile.ui.follow

import ir.coffevista.vista_native.features.profile.data.FollowActionRequestDto
import ir.coffevista.vista_native.features.profile.data.FollowActionResponseDto
import ir.coffevista.vista_native.features.profile.data.FollowListResponseDto
import ir.coffevista.vista_native.features.profile.data.FollowUserDto
import ir.coffevista.vista_native.features.profile.data.PublicProfileApi
import ir.coffevista.vista_native.features.profile.data.PublicProfileDto
import ir.coffevista.vista_native.features.profile.data.UnfollowResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class FollowersFollowingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakePublicProfileApi : PublicProfileApi {
        val sampleFollower = FollowUserDto(
            userId = "u1",
            username = "ali_vista",
            fullName = "علی ویستا",
            isFollowing = false,
            isFollower = true,
        )

        val sampleFollowing = FollowUserDto(
            userId = "u2",
            username = "sara_design",
            fullName = "سارا",
            isFollowing = true,
            isFollower = false,
        )

        var lastFollowTarget: String? = null
        var lastUnfollowTarget: String? = null

        override suspend fun fetchPublicProfile(userId: String): Response<PublicProfileDto> =
            error("unused")

        override suspend fun follow(request: FollowActionRequestDto): Response<FollowActionResponseDto> {
            lastFollowTarget = request.targetUserId
            return Response.success(FollowActionResponseDto(status = "following"))
        }

        override suspend fun unfollow(request: FollowActionRequestDto): Response<UnfollowResponseDto> {
            lastUnfollowTarget = request.targetUserId
            return Response.success(UnfollowResponseDto(status = "unfollowed"))
        }

        override suspend fun getFollowers(userId: String, limit: Int, offset: Int): Response<FollowListResponseDto> {
            return Response.success(FollowListResponseDto(users = listOf(sampleFollower), total = 1))
        }

        override suspend fun getFollowing(userId: String, limit: Int, offset: Int): Response<FollowListResponseDto> {
            return Response.success(FollowListResponseDto(users = listOf(sampleFollowing), total = 1))
        }
    }

    private lateinit var fakeApi: FakePublicProfileApi
    private lateinit var viewModel: FollowersFollowingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeApi = FakePublicProfileApi()
        viewModel = FollowersFollowingViewModel(fakeApi)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `bind loads followers and following correctly`() = runTest(dispatcher) {
        viewModel.bind("target_user", initialTab = 0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.selectedTab)
        assertEquals(1, state.followers.size)
        assertEquals(1, state.following.size)
        assertEquals("ali_vista", state.followers.first().username)
        assertEquals("sara_design", state.following.first().username)
        assertFalse(state.isLoading)
    }

    @Test
    fun `search query filters list by username or full name`() = runTest(dispatcher) {
        viewModel.bind("target_user", initialTab = 0)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("sara")
        assertEquals(0, viewModel.uiState.value.filteredFollowers.size)
        assertEquals(1, viewModel.uiState.value.filteredFollowing.size)

        viewModel.onSearchQueryChanged("ali")
        assertEquals(1, viewModel.uiState.value.filteredFollowers.size)
        assertEquals(0, viewModel.uiState.value.filteredFollowing.size)
    }

    @Test
    fun `toggleFollow optimistic updates state and triggers api`() = runTest(dispatcher) {
        viewModel.bind("target_user", initialTab = 0)
        advanceUntilIdle()

        val follower = viewModel.uiState.value.followers.first()
        assertFalse(follower.isFollowing)

        viewModel.toggleFollow(follower)
        advanceUntilIdle()

        assertEquals("u1", fakeApi.lastFollowTarget)
        assertTrue(viewModel.uiState.value.followers.first().isFollowing)
    }
}
