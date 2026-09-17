package ir.coffevista.vista_native.features.services.ui.nearby

import ir.coffevista.vista_native.core.network.RemoteFailure
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCandidate
import ir.coffevista.vista_native.features.services.data.nearby.NearbyLikeResult
import ir.coffevista.vista_native.features.services.data.nearby.NearbyMatch
import ir.coffevista.vista_native.features.services.data.nearby.NearbyPreferences
import ir.coffevista.vista_native.features.services.data.nearby.NearbyReceivedLike
import ir.coffevista.vista_native.features.services.data.nearby.NearbyReceivedLikes
import ir.coffevista.vista_native.features.services.data.nearby.NearbyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class NearbyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeNearbyRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initBootstrap_disabledPreference_setsIsDisabledTrue() = runTest(testDispatcher) {
        fakeRepository.preferences = NearbyPreferences(hasLocation = true, isEnabled = false)
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDisabled)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun setLocationError_updatesState() = runTest(testDispatcher) {
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.setLocationError("service_off")
        assertEquals("service_off", viewModel.uiState.value.locationError)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun onLocationAcquired_updatesLocationAndLoadsCards() = runTest(testDispatcher) {
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.candidates = listOf(
            NearbyCandidate(
                userId = "u1",
                username = "ali",
                fullName = "علی رضایی",
                avatarUrl = "",
                bio = "",
                gender = "male",
                maritalStatus = "single",
                age = 25,
                locationText = "تهران",
                isVerified = false,
                verificationType = "",
                distanceKm = 0.5,
                isOnlineNow = true,
            )
        )

        viewModel.onLocationAcquired(35.6892, 51.3890)
        advanceUntilIdle()

        assertEquals(35.6892, fakeRepository.lastUpdatedLat ?: 0.0, 0.0001)
        assertEquals(51.3890, fakeRepository.lastUpdatedLng ?: 0.0, 0.0001)
        assertEquals(1, viewModel.uiState.value.cards.size)
        assertEquals("u1", viewModel.uiState.value.cards.first().userId)
    }

    @Test
    fun respondToReceivedLike_likeMatches_updatesMatchesAndRemovesFromReceived() = runTest(testDispatcher) {
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        val like = NearbyReceivedLike(
            userId = "peer-1",
            username = "sara",
            fullName = "سارا",
            avatarUrl = "https://example.com/sara.jpg",
            isVerified = false,
            verificationType = "",
            action = "like",
        )
        fakeRepository.receivedLikes = listOf(like)
        viewModel.loadLikesAndMatches()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.receivedLikes.size)

        var matchedResultCallback: Boolean? = null
        var matchIdCallback: String? = null

        val match = NearbyMatch(
            matchId = "match-123",
            userId = "peer-1",
            username = "sara",
            fullName = "سارا",
            avatarUrl = "https://example.com/sara.jpg",
            isVerified = false,
            verificationType = "",
            matchedAt = "2026-09-13",
        )
        fakeRepository.likeResult = NearbyLikeResult(
            matched = true,
            matchId = "match-123",
            match = match,
        )
        fakeRepository.matches = listOf(match)

        viewModel.respondToReceivedLike(
            like = like,
            action = "like",
            onSuccess = { matched, matchId ->
                matchedResultCallback = matched
                matchIdCallback = matchId
            },
        )
        advanceUntilIdle()

        assertTrue(matchedResultCallback == true)
        assertEquals("match-123", matchIdCallback)
        assertTrue(viewModel.uiState.value.receivedLikes.isEmpty())
        assertEquals(1, viewModel.uiState.value.matches.size)
    }

    @Test
    fun respondToReceivedLike_pass_removesFromReceivedWithoutMatching() = runTest(testDispatcher) {
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        val like = NearbyReceivedLike(
            userId = "peer-2",
            username = "reza",
            fullName = "رضا",
            avatarUrl = "",
            isVerified = false,
            verificationType = "",
            action = "like",
        )
        fakeRepository.receivedLikes = listOf(like)
        viewModel.loadLikesAndMatches()
        advanceUntilIdle()

        viewModel.respondToReceivedLike(
            like = like,
            action = "pass",
        )
        advanceUntilIdle()

        assertEquals("pass", fakeRepository.lastAction)
        assertEquals("peer-2", fakeRepository.lastLikedUserId)
        assertTrue(viewModel.uiState.value.receivedLikes.isEmpty())
    }

    @Test
    fun respondToReceivedLike_errorDailyLimit_mapsPersianMessage() = runTest(testDispatcher) {
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        val like = NearbyReceivedLike(
            userId = "peer-3",
            username = "maryam",
            fullName = "مریم",
            avatarUrl = "",
            isVerified = false,
            verificationType = "",
            action = "like",
        )
        fakeRepository.likeError = RemoteFailure(429, "daily_like_limit", "Daily limit")

        var errorMessage: String? = null
        viewModel.respondToReceivedLike(
            like = like,
            action = "like",
            onError = { errorMessage = it }
        )
        advanceUntilIdle()

        assertEquals("سقف لایک روزانه‌ات پر شد", errorMessage)
    }
}

private class FakeNearbyRepository : NearbyRepository {
    var preferences: NearbyPreferences = NearbyPreferences(hasLocation = true, isEnabled = true)
    var candidates: List<NearbyCandidate> = emptyList()
    var receivedLikes: List<NearbyReceivedLike> = emptyList()
    var matches: List<NearbyMatch> = emptyList()
    var likeResult: NearbyLikeResult = NearbyLikeResult(matched = false, matchId = "", match = null)
    var likeError: Throwable? = null

    var lastUpdatedLat: Double? = null
    var lastUpdatedLng: Double? = null
    var lastLikedUserId: String? = null
    var lastAction: String? = null

    override suspend fun updateLocation(lat: Double, lng: Double, cityName: String?, provinceName: String?) {
        lastUpdatedLat = lat
        lastUpdatedLng = lng
    }

    override suspend fun disableLocation() {
        preferences = preferences.copy(isEnabled = false)
    }

    override suspend fun getPreferences(): NearbyPreferences = preferences

    override suspend fun updatePreferences(prefs: NearbyPreferences): NearbyPreferences {
        preferences = prefs
        return prefs
    }

    override suspend fun discover(limit: Int, offset: Int): List<NearbyCandidate> = candidates

    override suspend fun discoverRandomOnline(limit: Int): List<NearbyCandidate> = candidates

    override suspend fun like(targetId: String, action: String): NearbyLikeResult {
        lastLikedUserId = targetId
        lastAction = action
        likeError?.let { throw it }
        return likeResult
    }

    override suspend fun getMatches(): List<NearbyMatch> = matches

    override suspend fun unmatch(matchId: String) {
        matches = matches.filterNot { it.matchId == matchId }
    }

    override suspend fun openChat(matchId: String): String = "conv-$matchId"

    override suspend fun getLikesReceived(limit: Int): NearbyReceivedLikes =
        NearbyReceivedLikes(count = receivedLikes.size, likes = receivedLikes)

    override suspend fun report(targetId: String, reason: String) {}
}
