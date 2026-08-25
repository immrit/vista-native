package ir.coffevista.vista_native.features.profile.ui

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.profile.data.OwnProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class OwnProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeRepository = FakeOwnProfileRepository()
    private val authStateProvider = FakeAuthenticationStateProvider()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoading() = runTest {
        val viewModel = OwnProfileViewModel(fakeRepository, authStateProvider)
        assertEquals(OwnProfileUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun onSignedIn_fetchesProfileAndObservesCache() = runTest {
        val viewModel = OwnProfileViewModel(fakeRepository, authStateProvider)
        
        // Setup initial cache
        val entity = OwnProfileEntity(
            userId = "test_user_id",
            username = "test",
            fullName = "Test User",
            bio = null,
            avatarUrl = null,
            isVerified = false,
            accountType = null,
            postCount = 0,
            followerCount = 0,
            followingCount = 0,
            updatedAt = null
        )
        fakeRepository.setCachedProfile(entity)
        
        // Act: Sign in
        authStateProvider.setSignedIn("test_user_id")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert network was fetched
        assertEquals(1, fakeRepository.fetchCount)
        
        // Assert UI State updated from cache
        val state = viewModel.uiState.value
        assertTrue(state is OwnProfileUiState.Content)
        assertEquals("test_user_id", (state as OwnProfileUiState.Content).profile.userId)
    }
    
    @Test
    fun onSignedOut_clearsProfile() = runTest {
        val viewModel = OwnProfileViewModel(fakeRepository, authStateProvider)
        
        authStateProvider.setSignedIn("test_user_id")
        testDispatcher.scheduler.advanceUntilIdle()
        
        authStateProvider.setSignedOut()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(true, fakeRepository.cleared)
    }

    @Test
    fun fetchFailureWithoutCache_remainsErrorAfterEmptyRoomEmission() = runTest {
        val viewModel = OwnProfileViewModel(fakeRepository, authStateProvider)
        fakeRepository.fetchResult = Outcome.Failure(
            AppError(
                kind = ErrorKind.NETWORK,
                messageFa = "خطای شبکه",
            ),
        )

        authStateProvider.setSignedIn("test_user_id")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is OwnProfileUiState.Error)
        assertEquals("خطای شبکه", (state as OwnProfileUiState.Error).error.messageFa)
    }
}

class FakeAuthenticationStateProvider : AuthenticationStateProvider {
    private val _state = MutableStateFlow<AuthenticationState>(AuthenticationState.Unknown)
    override val state: StateFlow<AuthenticationState> = _state
    
    fun setSignedIn(userId: String) {
        _state.value = AuthenticationState.SignedIn(
            AuthenticatedContext(userId, false, false, false, "Test User")
        )
    }
    
    fun setSignedOut() {
        _state.value = AuthenticationState.SignedOut
    }
}

class FakeOwnProfileRepository : OwnProfileRepository {
    private val _profileFlow = MutableStateFlow<OwnProfileEntity?>(null)
    var fetchCount = 0
    var cleared = false
    var fetchResult: Outcome<Unit> = Outcome.Success(Unit)
    
    fun setCachedProfile(entity: OwnProfileEntity?) {
        _profileFlow.value = entity
    }

    override fun getOwnProfileFlow(userId: String) = _profileFlow

    override suspend fun fetchAndCacheOwnProfile(userId: String): Outcome<Unit> {
        fetchCount++
        return fetchResult
    }
    
    override suspend fun clearProfileData() {
        cleared = true
        _profileFlow.value = null
    }
}
