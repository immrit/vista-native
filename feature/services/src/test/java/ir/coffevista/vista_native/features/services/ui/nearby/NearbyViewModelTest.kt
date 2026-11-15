package ir.coffevista.vista_native.features.services.ui.nearby

import ir.coffevista.vista_native.core.network.RemoteFailure
import ir.coffevista.vista_native.features.services.data.nearby.NearbyBazaarVerifyResult
import ir.coffevista.vista_native.features.services.data.nearby.NearbyBuyRequestsResult
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCandidate
import ir.coffevista.vista_native.features.services.data.nearby.NearbyChatRequest
import ir.coffevista.vista_native.features.services.data.nearby.NearbyChatRequestPack
import ir.coffevista.vista_native.features.services.data.nearby.NearbyLikeResult
import ir.coffevista.vista_native.features.services.data.nearby.NearbyMatch
import ir.coffevista.vista_native.features.services.data.nearby.NearbyPreferences
import ir.coffevista.vista_native.features.services.data.nearby.NearbyReceivedLike
import ir.coffevista.vista_native.features.services.data.nearby.NearbyReceivedLikes
import ir.coffevista.vista_native.features.services.data.nearby.NearbyRepository
import ir.coffevista.vista_native.features.services.data.nearby.NearbySendChatRequestResult
import ir.coffevista.vista_native.features.services.data.nearby.NearbyShopCatalog
import ir.coffevista.vista_native.features.services.data.nearby.NearbyWallet
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

        assertEquals("سقف لایک روزانه شما پر شده است", errorMessage)
    }

    @Test
    fun selectZoneFilter_updatesZoneAndLoadsFilteredDiscover() = runTest(testDispatcher) {
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.selectZoneFilter(NearbyZoneFilter.SAME_CITY)
        advanceUntilIdle()

        assertEquals(NearbyZoneFilter.SAME_CITY, viewModel.uiState.value.zoneFilter)
        assertEquals("same_city", fakeRepository.lastZoneParam)
    }

    @Test
    fun buyChatRequestPack_1000CoinsFor50Requests_updatesWallet() = runTest(testDispatcher) {
        fakeRepository.wallet = NearbyWallet(coins = 1200L, chatRequestCredits = 0)
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.openShop()
        advanceUntilIdle()
        viewModel.buyChatRequestPack("req_50")
        advanceUntilIdle()

        assertEquals(200L, viewModel.uiState.value.wallet.coins)
        assertEquals(50, viewModel.uiState.value.wallet.chatRequestCredits)
        assertTrue(viewModel.uiState.value.shopMessage?.contains("50") == true)
    }

    @Test
    fun sendChatRequest_updatesWalletAndCandidatePendingState() = runTest(testDispatcher) {
        fakeRepository.wallet = NearbyWallet(coins = 100L, chatRequestCredits = 2)
        fakeRepository.candidates = listOf(
            NearbyCandidate(
                userId = "peer-10",
                username = "sara",
                fullName = "سارا",
                bio = "سلام",
                avatarUrl = "",
                isVerified = false,
                verificationType = "",
                age = 24,
                gender = "female",
                maritalStatus = "single",
                distanceKm = 2.5,
                zone = "same_city",
                cityName = "تهران",
                provinceName = "تهران",
                locationText = "تهران",
                compatibilityScore = 90,
                isOnlineNow = true,
            )
        )
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()
        viewModel.loadCards(reset = true)
        advanceUntilIdle()

        viewModel.sendChatRequest("peer-10", "سلام، آشنا بشیم؟")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.wallet.chatRequestCredits)
        assertTrue(viewModel.uiState.value.cards.first().hasPendingChatRequest)
    }

    @Test
    fun sendChatRequest_insufficientCoins_opensShop() = runTest(testDispatcher) {
        fakeRepository.sendRequestError = RemoteFailure(402, "insufficient_coins", "Insufficient coins")
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.sendChatRequest("peer-11", "سلام")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isShopVisible)
        assertEquals("موجودی سکه کافی نیست — از فروشگاه سکه تهیه کنید", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun openMatchChat_resolvesConversationId() = runTest(testDispatcher) {
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        var openedConversationId = ""
        viewModel.openMatchChat("match-77", "peer-77") { convId ->
            openedConversationId = convId
        }
        advanceUntilIdle()

        assertEquals("conv-match-77", openedConversationId)
    }

    @Test
    fun candidateZoneType_zeroDistanceWithoutZone_neverClaimsSameCity() {
        val unknownCandidate = NearbyCandidate(
            userId = "u-x",
            username = "ali",
            fullName = "علی",
            bio = "",
            avatarUrl = "",
            isVerified = false,
            verificationType = "",
            age = 25,
            gender = "male",
            maritalStatus = "single",
            distanceKm = 0.0,
            zone = "",
            cityName = "",
            provinceName = "",
            locationText = "",
        )
        assertEquals("", unknownCandidate.zoneType)

        val sameCityCandidate = unknownCandidate.copy(zone = "same_city", cityName = "تهران")
        assertEquals("same_city", sameCityCandidate.zoneType)
    }

    @Test
    fun verifyBazaarCoinPurchase_updatesWalletAndSetsConsumeToken() = runTest(testDispatcher) {
        fakeRepository.wallet = NearbyWallet(coins = 200L, chatRequestCredits = 0, chatRequestCoinCost = 20)
        fakeRepository.bazaarVerifyResult = NearbyBazaarVerifyResult(
            success = true,
            message = "بسته ۱٬۰۰۰ سکه اضافه شد",
            type = "coins",
            coinsAdded = 1000L,
            newBalance = 1200L,
            alreadyProcessed = false,
        )
        val viewModel = NearbyViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.verifyBazaarCoinPurchase(
            productId = "coins_1000",
            purchaseToken = "tok-coins-1",
            packageName = "ir.coffevista.vista",
        )
        advanceUntilIdle()

        assertEquals("tok-coins-1", viewModel.uiState.value.pendingConsumeToken)
        assertEquals(null, viewModel.uiState.value.pendingBazaarVerification)
        assertEquals(1200L, viewModel.uiState.value.wallet.coins)
        assertTrue(viewModel.uiState.value.shopMessage?.contains("۱٬۰۰۰") == true)

        viewModel.onBazaarPurchaseConsumed()
        assertEquals(null, viewModel.uiState.value.pendingConsumeToken)
    }
}

private class FakeNearbyRepository : NearbyRepository {
    var preferences: NearbyPreferences = NearbyPreferences(hasLocation = true, isEnabled = true)
    var candidates: List<NearbyCandidate> = emptyList()
    var receivedLikes: List<NearbyReceivedLike> = emptyList()
    var matches: List<NearbyMatch> = emptyList()
    var likeResult: NearbyLikeResult = NearbyLikeResult(matched = false, matchId = "", match = null)
    var likeError: Throwable? = null
    var wallet: NearbyWallet = NearbyWallet(coins = 1000L, chatRequestCredits = 0, chatRequestCoinCost = 20)
    var sendRequestError: Throwable? = null
    var bazaarVerifyResult: NearbyBazaarVerifyResult = NearbyBazaarVerifyResult(
        success = true,
        message = "ok",
        type = "coins",
        coinsAdded = 1000L,
        newBalance = 2000L,
        alreadyProcessed = false,
    )

    var lastUpdatedLat: Double? = null
    var lastUpdatedLng: Double? = null
    var lastLikedUserId: String? = null
    var lastAction: String? = null
    var lastZoneParam: String? = null

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

    override suspend fun discoverFiltered(limit: Int, offset: Int, zone: String?): List<NearbyCandidate> {
        lastZoneParam = zone
        return candidates
    }

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

    override suspend fun getWallet(): NearbyWallet = wallet

    override suspend fun getShopCatalog(): NearbyShopCatalog = NearbyShopCatalog(
        wallet = wallet,
        packs = listOf(
            NearbyChatRequestPack(id = "req_50", title = "بسته ۵۰ درخواست چت", requestsCount = 50, coinCost = 1000)
        ),
        coinsPerRequest = 20,
        sampleRateLabel = "هر ۱,۰۰۰ سکه = ۵۰ درخواست چت",
    )

    override suspend fun buyChatRequestPack(packId: String): NearbyBuyRequestsResult {
        val pack = NearbyChatRequestPack(id = packId, title = "بسته ۵۰ درخواست چت", requestsCount = 50, coinCost = 1000)
        wallet = wallet.copy(
            coins = wallet.coins - pack.coinCost,
            chatRequestCredits = wallet.chatRequestCredits + pack.requestsCount,
        )
        return NearbyBuyRequestsResult(pack = pack, wallet = wallet)
    }

    override suspend fun sendChatRequest(targetId: String, introMessage: String): NearbySendChatRequestResult {
        sendRequestError?.let { throw it }
        wallet = wallet.copy(chatRequestCredits = (wallet.chatRequestCredits - 1).coerceAtLeast(0))
        return NearbySendChatRequestResult(
            request = NearbyChatRequest(
                id = "req-1",
                senderId = "me",
                recipientId = targetId,
                peerId = targetId,
                direction = "outgoing",
                username = "sara",
                fullName = "سارا",
                avatarUrl = "",
                isVerified = false,
                isOnlineNow = true,
                age = 24,
                bio = "",
                statusText = "",
                cityName = "تهران",
                provinceName = "تهران",
                distanceKm = 2.0,
                hideDistance = false,
                introMessage = introMessage,
                status = "pending",
                conversationId = "",
                createdAt = "2026-11-15T10:00:00Z",
            ),
            wallet = wallet,
            spentKind = "credit",
            coinsSpent = 0,
            creditsSpent = 1,
        )
    }

    override suspend fun verifyBazaarPurchase(
        purchaseToken: String,
        productId: String,
        packageName: String,
    ): NearbyBazaarVerifyResult {
        val updatedCoins = bazaarVerifyResult.newBalance ?: (wallet.coins + bazaarVerifyResult.coinsAdded)
        wallet = wallet.copy(coins = updatedCoins)
        return bazaarVerifyResult
    }
}
