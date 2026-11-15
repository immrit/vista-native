package ir.coffevista.vista_native.features.services.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.network.RemoteFailure
import ir.coffevista.vista_native.features.services.data.nearby.NearbyBazaarPendingVerification
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCandidate
import ir.coffevista.vista_native.features.services.data.nearby.NearbyChatRequest
import ir.coffevista.vista_native.features.services.data.nearby.NearbyChatRequestPack
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCoinPackage
import ir.coffevista.vista_native.features.services.data.nearby.NearbyLikeResult
import ir.coffevista.vista_native.features.services.data.nearby.NearbyMatch
import ir.coffevista.vista_native.features.services.data.nearby.NearbyPreferences
import ir.coffevista.vista_native.features.services.data.nearby.NearbyReceivedLike
import ir.coffevista.vista_native.features.services.data.nearby.NearbyRepository
import ir.coffevista.vista_native.features.services.data.nearby.NearbyWallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NearbyViewMode {
    GRID,
    DECK,
}

enum class NearbyZoneFilter(val queryParam: String?, val label: String) {
    ALL(null, "همه نزدیک‌ها"),
    SAME_CITY("same_city", "هم‌شهری"),
    SAME_PROVINCE("same_province", "هم‌استانی"),
    ONLINE("online", "آنلاین‌ها"),
    RANDOM_ONLINE(null, "تصادفی آنلاین"),
}

data class NearbyUiState(
    val cards: List<NearbyCandidate> = emptyList(),
    val currentIndex: Int = 0,
    val viewMode: NearbyViewMode = NearbyViewMode.GRID,
    val zoneFilter: NearbyZoneFilter = NearbyZoneFilter.ALL,
    val isLoading: Boolean = false,
    val isLocating: Boolean = false,
    val locationError: String? = null,
    val isDisabled: Boolean = false,
    val isRandomOnline: Boolean = false,
    val receivedLikesCount: Int = 0,
    val pendingChatRequestsCount: Int = 0,
    val preferences: NearbyPreferences = NearbyPreferences(),
    val isPreferencesSheetVisible: Boolean = false,
    val matchedResult: NearbyLikeResult? = null,
    val matchedCandidate: NearbyCandidate? = null,
    val zoneTransition: String? = null,
    val isLiking: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val receivedLikes: List<NearbyReceivedLike> = emptyList(),
    val matches: List<NearbyMatch> = emptyList(),
    val incomingChatRequests: List<NearbyChatRequest> = emptyList(),
    val outgoingChatRequests: List<NearbyChatRequest> = emptyList(),
    val isLoadingLikesMatches: Boolean = false,
    val locationCity: String? = null,
    // Wallet & Shop
    val wallet: NearbyWallet = NearbyWallet(),
    val shopPacks: List<NearbyChatRequestPack> = emptyList(),
    val coinPackages: List<NearbyCoinPackage> = emptyList(),
    val sampleRateLabel: String = "هر ۱,۰۰۰ سکه = ۵۰ درخواست چت",
    val isShopVisible: Boolean = false,
    val isShopLoading: Boolean = false,
    val shopPurchasingId: String? = null,
    val shopMessage: String? = null,
    val pendingBazaarVerification: NearbyBazaarPendingVerification? = null,
    val pendingConsumeToken: String? = null,
    // Quick Preview & Chat Request dialog
    val previewCandidate: NearbyCandidate? = null,
    val chatRequestTarget: NearbyCandidate? = null,
    val isSendingChatRequest: Boolean = false,
) {
    val totalInboxBadge: Int
        get() = receivedLikesCount + pendingChatRequestsCount
}

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val repository: NearbyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()

    private var currentZone: String? = null
    private var lastSwipedCandidate: NearbyCandidate? = null

    init {
        initBootstrap()
    }

    fun initBootstrap() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, locationError = null, isDisabled = false) }
            runCatching {
                val prefs = repository.getPreferences()
                _uiState.update { it.copy(preferences = prefs) }
                if (prefs.hasLocation && !prefs.isEnabled) {
                    _uiState.update { it.copy(isLocating = false, isDisabled = true) }
                    return@launch
                }
            }
            loadCards(reset = true)
            loadReceivedLikesBadge()
            refreshWallet()
            loadIncomingRequestsCount()
        }
    }

    fun setViewMode(mode: NearbyViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun selectZoneFilter(filter: NearbyZoneFilter) {
        if (_uiState.value.zoneFilter == filter) return
        val isRandom = filter == NearbyZoneFilter.RANDOM_ONLINE
        _uiState.update { it.copy(zoneFilter = filter, isRandomOnline = isRandom) }
        loadCards(reset = true, setRandomOnline = isRandom)
    }

    fun setLocationError(error: String?) {
        _uiState.update { it.copy(locationError = error, isLocating = false) }
    }

    fun onLocationAcquired(
        lat: Double,
        lng: Double,
        cityName: String? = null,
        provinceName: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLocating = true,
                    locationError = null,
                    isDisabled = false,
                    locationCity = cityName ?: it.locationCity,
                )
            }
            runCatching {
                repository.updateLocation(
                    lat = lat,
                    lng = lng,
                    cityName = cityName,
                    provinceName = provinceName,
                )
            }.onFailure { err ->
                val msg = mapNearbyError(err, fallback = "امکان ثبت موقعیت مکانی وجود ندارد")
                _uiState.update { it.copy(errorMessage = msg) }
            }
            loadCards(reset = true)
            loadReceivedLikesBadge()
            refreshWallet()
        }
    }

    fun loadCards(reset: Boolean = false, setRandomOnline: Boolean? = null) {
        viewModelScope.launch {
            val isRandom = setRandomOnline ?: _uiState.value.isRandomOnline
            val filter = if (isRandom) {
                NearbyZoneFilter.RANDOM_ONLINE
            } else if (_uiState.value.zoneFilter == NearbyZoneFilter.RANDOM_ONLINE) {
                NearbyZoneFilter.ALL
            } else {
                _uiState.value.zoneFilter
            }

            _uiState.update {
                it.copy(
                    isLoading = reset || it.cards.isEmpty(),
                    isRandomOnline = isRandom,
                    zoneFilter = filter,
                    isLocating = false,
                    locationError = null,
                )
            }

            runCatching {
                if (isRandom) {
                    repository.discoverRandomOnline(limit = 24)
                } else {
                    val offset = if (reset) 0 else _uiState.value.cards.size
                    repository.discoverFiltered(limit = 24, offset = offset, zone = filter.queryParam)
                }
            }.onSuccess { candidates ->
                val existingIds = if (reset) emptySet() else _uiState.value.cards.map { it.userId }.toSet()
                val appended = candidates.filterNot { it.userId in existingIds }
                val newCards = if (reset) candidates else _uiState.value.cards + appended
                _uiState.update {
                    it.copy(
                        cards = newCards,
                        currentIndex = if (reset) 0 else it.currentIndex.coerceAtMost((newCards.size - 1).coerceAtLeast(0)),
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                val code = (error as? RemoteFailure)?.code
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        locationError = if (code == "location_required") "location_required" else it.locationError,
                        errorMessage = if (reset && it.cards.isEmpty() && code != "location_required") {
                            mapNearbyError(error, fallback = "خطا در دریافت افراد نزدیک")
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    fun loadReceivedLikesBadge() {
        viewModelScope.launch {
            runCatching { repository.getLikesReceived(limit = 1) }
                .onSuccess { result ->
                    _uiState.update { it.copy(receivedLikesCount = result.count) }
                }
        }
    }

    private fun loadIncomingRequestsCount() {
        viewModelScope.launch {
            runCatching { repository.getChatRequests(direction = "incoming", limit = 50) }
                .onSuccess { list ->
                    val pending = list.count { it.status == "pending" }
                    _uiState.update {
                        it.copy(
                            incomingChatRequests = list,
                            pendingChatRequestsCount = pending,
                        )
                    }
                }
        }
    }

    fun refreshWallet() {
        viewModelScope.launch {
            runCatching { repository.getWallet() }
                .onSuccess { wallet ->
                    _uiState.update { it.copy(wallet = wallet) }
                }
        }
    }

    fun openShop() {
        _uiState.update { it.copy(isShopVisible = true, isShopLoading = true, shopMessage = null) }
        viewModelScope.launch {
            val catalogResult = runCatching { repository.getShopCatalog() }
            val coinsResult = runCatching { repository.getCoinPackages() }

            catalogResult.onSuccess { catalog ->
                _uiState.update {
                    it.copy(
                        wallet = catalog.wallet,
                        shopPacks = catalog.packs,
                        sampleRateLabel = catalog.sampleRateLabel.ifBlank { it.sampleRateLabel },
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(shopMessage = mapNearbyError(err, "خطا در دریافت فروشگاه")) }
            }

            coinsResult.onSuccess { packages ->
                _uiState.update { it.copy(coinPackages = packages) }
            }

            _uiState.update { it.copy(isShopLoading = false) }
        }
    }

    fun closeShop() {
        _uiState.update { it.copy(isShopVisible = false, shopMessage = null) }
    }

    fun clearShopMessage() {
        _uiState.update { it.copy(shopMessage = null) }
    }

    fun buyChatRequestPack(packId: String) {
        if (_uiState.value.shopPurchasingId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(shopPurchasingId = packId, shopMessage = null) }
            runCatching { repository.buyChatRequestPack(packId) }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            wallet = result.wallet,
                            shopPurchasingId = null,
                            shopMessage = "${result.pack.requestsCount} درخواست چت به کیف‌پول شما اضافه شد",
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            shopPurchasingId = null,
                            shopMessage = mapNearbyError(err, "خرید بسته انجام نشد"),
                        )
                    }
                }
        }
    }

    fun onBazaarCoinPurchaseStarted(packageId: String) {
        _uiState.update { it.copy(shopPurchasingId = packageId, shopMessage = null) }
    }

    fun onBazaarCoinPurchaseCanceled() {
        _uiState.update {
            it.copy(
                shopPurchasingId = null,
                shopMessage = "پرداخت لغو شد.",
            )
        }
    }

    fun onBazaarCoinPurchaseFailed(reason: String?) {
        val message = reason?.takeIf { it.isNotBlank() }
            ?: "پرداخت از طریق درگاه کافه‌بازار ناموفق بود. دوباره تلاش کنید."
        _uiState.update {
            it.copy(
                shopPurchasingId = null,
                shopMessage = message,
            )
        }
    }

    fun verifyBazaarCoinPurchase(
        productId: String,
        purchaseToken: String,
        packageName: String,
    ) {
        if (purchaseToken.isBlank() || productId.isBlank()) {
            onBazaarCoinPurchaseFailed("اطلاعات تراکنش کافه‌بازار ناقص است.")
            return
        }
        val pending = NearbyBazaarPendingVerification(
            productId = productId,
            purchaseToken = purchaseToken,
            packageName = packageName,
        )
        verifyBazaarPending(pending)
    }

    fun retryPendingBazaarVerification() {
        val pending = _uiState.value.pendingBazaarVerification ?: return
        if (_uiState.value.isShopLoading) return
        verifyBazaarPending(pending)
    }

    fun onBazaarPurchaseConsumed() {
        _uiState.update { it.copy(pendingConsumeToken = null) }
    }

    private fun verifyBazaarPending(pending: NearbyBazaarPendingVerification) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isShopLoading = true,
                    shopPurchasingId = pending.productId,
                    pendingBazaarVerification = pending,
                    shopMessage = null,
                )
            }
            runCatching {
                repository.verifyBazaarPurchase(
                    purchaseToken = pending.purchaseToken,
                    productId = pending.productId,
                    packageName = pending.packageName,
                )
            }
                .onSuccess { res ->
                    if (!res.success) {
                        _uiState.update {
                            it.copy(
                                isShopLoading = false,
                                shopPurchasingId = null,
                                shopMessage = res.message.ifBlank {
                                    "پرداخت هنوز تایید نشده است؛ در صورت کسر وجه دوباره بررسی کنید"
                                },
                            )
                        }
                        return@onSuccess
                    }
                    refreshWallet()
                    val msg = res.message.ifBlank {
                        if (res.alreadyProcessed) {
                            "این پرداخت قبلاً تایید شده و سکه‌ها به حساب شما افزوده شده است"
                        } else if (res.coinsAdded > 0) {
                            "${res.coinsAdded} سکه به موجودی شما اضافه شد 🎉"
                        } else {
                            "بسته سکه با موفقیت به حساب شما اضافه شد"
                        }
                    }
                    _uiState.update { state ->
                        val updatedWallet = res.newBalance?.let { balance ->
                            state.wallet.copy(coins = balance)
                        } ?: state.wallet
                        state.copy(
                            wallet = updatedWallet,
                            isShopLoading = false,
                            shopPurchasingId = null,
                            pendingBazaarVerification = null,
                            pendingConsumeToken = pending.purchaseToken,
                            shopMessage = msg,
                        )
                    }
                }
                .onFailure { err ->
                    refreshWallet()
                    _uiState.update {
                        it.copy(
                            isShopLoading = false,
                            shopPurchasingId = null,
                            shopMessage = mapNearbyError(err, "پرداخت هنوز تایید نشده است؛ در صورت کسر وجه دوباره بررسی کنید"),
                        )
                    }
                }
        }
    }

    fun openPreview(candidate: NearbyCandidate) {
        _uiState.update { it.copy(previewCandidate = candidate) }
    }

    fun dismissPreview() {
        _uiState.update { it.copy(previewCandidate = null) }
    }

    fun openChatRequestComposer(candidate: NearbyCandidate) {
        _uiState.update { it.copy(chatRequestTarget = candidate, previewCandidate = null) }
    }

    fun dismissChatRequestComposer() {
        _uiState.update { it.copy(chatRequestTarget = null) }
    }

    fun sendChatRequest(targetId: String, introMessage: String) {
        if (_uiState.value.isSendingChatRequest) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingChatRequest = true, errorMessage = null) }
            runCatching { repository.sendChatRequest(targetId = targetId, introMessage = introMessage) }
                .onSuccess { res ->
                    _uiState.update { current ->
                        val updatedCards = current.cards.map { c ->
                            if (c.userId == targetId) c.copy(hasPendingChatRequest = true) else c
                        }
                        val spentNote = when (res.spentKind) {
                            "free" -> "درخواست چت با سهمیه رایگان امروز ارسال شد"
                            "credit" -> "درخواست چت با اعتبار فروشگاه ارسال شد"
                            else -> "درخواست چت با کسر ${res.coinsSpent} سکه ارسال شد"
                        }
                        current.copy(
                            cards = updatedCards,
                            wallet = res.wallet,
                            outgoingChatRequests = listOf(res.request) + current.outgoingChatRequests,
                            chatRequestTarget = null,
                            isSendingChatRequest = false,
                            infoMessage = spentNote,
                        )
                    }
                }
                .onFailure { err ->
                    val code = (err as? RemoteFailure)?.code
                    _uiState.update {
                        it.copy(
                            isSendingChatRequest = false,
                            errorMessage = mapNearbyError(err, "ارسال درخواست چت انجام نشد"),
                        )
                    }
                    if (code == "insufficient_coins" || code == "insufficient_chat_request_credits") {
                        _uiState.update { it.copy(chatRequestTarget = null) }
                        openShop()
                    }
                }
        }
    }

    fun loadLikesAndMatches() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLikesMatches = true) }
            val likesResult = runCatching { repository.getLikesReceived(limit = 50) }
            val matchesResult = runCatching { repository.getMatches() }
            val incomingResult = runCatching { repository.getChatRequests(direction = "incoming", limit = 50) }
            val outgoingResult = runCatching { repository.getChatRequests(direction = "outgoing", limit = 50) }

            val likes = likesResult.getOrNull()?.likes.orEmpty()
            val count = likesResult.getOrNull()?.count ?: 0
            val matches = matchesResult.getOrNull().orEmpty()
            val incoming = incomingResult.getOrNull().orEmpty()
            val outgoing = outgoingResult.getOrNull().orEmpty()

            _uiState.update {
                it.copy(
                    receivedLikes = likes,
                    matches = matches,
                    receivedLikesCount = count,
                    incomingChatRequests = incoming,
                    outgoingChatRequests = outgoing,
                    pendingChatRequestsCount = incoming.count { r -> r.status == "pending" },
                    isLoadingLikesMatches = false,
                )
            }
        }
    }

    fun respondToReceivedLike(
        like: NearbyReceivedLike,
        action: String,
        onSuccess: (matched: Boolean, matchId: String?) -> Unit = { _, _ -> },
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                repository.like(like.userId, action = action)
            }.onSuccess { result ->
                _uiState.update { current ->
                    val updatedLikes = current.receivedLikes.filterNot { it.userId == like.userId }
                    current.copy(
                        receivedLikes = updatedLikes,
                        receivedLikesCount = (current.receivedLikesCount - 1).coerceAtLeast(0),
                    )
                }
                if (result.matched) {
                    val updatedMatches = runCatching { repository.getMatches() }.getOrNull() ?: _uiState.value.matches
                    _uiState.update { it.copy(matches = updatedMatches) }
                    onSuccess(true, result.matchId)
                } else {
                    onSuccess(false, null)
                }
            }.onFailure { error ->
                onError(mapNearbyError(error, "خطا، دوباره تلاش کن"))
            }
        }
    }

    fun respondToChatRequest(
        request: NearbyChatRequest,
        action: String,
        onAcceptedChat: (conversationId: String, peerId: String, name: String, avatar: String) -> Unit = { _, _, _, _ -> },
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                repository.respondChatRequest(requestId = request.id, action = action)
            }.onSuccess { res ->
                loadLikesAndMatches()
                if (action == "accept" && res.conversationId.isNotBlank()) {
                    onAcceptedChat(res.conversationId, request.peerId, request.fullName, request.avatarUrl)
                }
            }.onFailure { err ->
                onError(mapNearbyError(err, "انجام عملیات ممکن نشد"))
            }
        }
    }

    fun openMatchChat(
        matchId: String,
        fallbackOtherUserId: String,
        onReady: (conversationId: String) -> Unit,
    ) {
        viewModelScope.launch {
            val resolved = runCatching { repository.openChat(matchId) }.getOrNull().orEmpty()
            onReady(resolved.ifBlank { fallbackOtherUserId })
        }
    }

    fun likeCurrent() {
        val state = _uiState.value
        if (state.isLiking || state.currentIndex < 0 || state.currentIndex >= state.cards.size) return
        likeCandidate(state.cards[state.currentIndex], action = "like", fromDeck = true)
    }

    fun superlikeCurrent() {
        val state = _uiState.value
        if (state.isLiking || state.currentIndex < 0 || state.currentIndex >= state.cards.size) return
        likeCandidate(state.cards[state.currentIndex], action = "superlike", fromDeck = true)
    }

    fun likeCandidate(candidate: NearbyCandidate, action: String = "like", fromDeck: Boolean = false) {
        if (_uiState.value.isLiking) return
        if (fromDeck) {
            lastSwipedCandidate = candidate
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLiking = true, previewCandidate = null) }
            runCatching { repository.like(candidate.userId, action = action) }
                .onSuccess { result ->
                    _uiState.update { current ->
                        val updatedWallet = if (result.coinsBalance > 0) {
                            current.wallet.copy(coins = result.coinsBalance)
                        } else {
                            current.wallet
                        }
                        val nextCards = if (!fromDeck) {
                            current.cards.filterNot { it.userId == candidate.userId }
                        } else {
                            current.cards
                        }
                        current.copy(
                            isLiking = false,
                            cards = nextCards,
                            wallet = updatedWallet,
                            matchedResult = if (result.matched) result else current.matchedResult,
                            matchedCandidate = if (result.matched) candidate else current.matchedCandidate,
                            infoMessage = if (!result.matched && !fromDeck) {
                                if (action == "superlike") "سوپرلایک ویژه برای ${candidate.fullName} ارسال شد ⭐"
                                else "لایک برای ${candidate.fullName} ثبت شد 💜"
                            } else {
                                current.infoMessage
                            },
                        )
                    }
                    if (!result.matched && fromDeck) {
                        advance(forward = true)
                    }
                }
                .onFailure { err ->
                    val code = (err as? RemoteFailure)?.code
                    _uiState.update {
                        it.copy(
                            isLiking = false,
                            errorMessage = mapNearbyError(err, "ثبت نشد، دوباره تلاش کن"),
                        )
                    }
                    if (code == "insufficient_coins") {
                        openShop()
                    }
                }
        }
    }

    fun passCurrent() {
        val state = _uiState.value
        if (state.currentIndex < 0 || state.currentIndex >= state.cards.size) return
        val card = state.cards[state.currentIndex]
        lastSwipedCandidate = card

        viewModelScope.launch {
            runCatching { repository.like(card.userId, action = "pass") }
            advance(forward = true)
        }
    }

    fun undoLastSwipe() {
        val last = lastSwipedCandidate
        if (last != null) {
            viewModelScope.launch {
                runCatching { repository.undo(last.userId) }
                lastSwipedCandidate = null
                advance(forward = false)
            }
        } else {
            advance(forward = false)
        }
    }

    fun advance(forward: Boolean, wrap: Boolean = false) {
        val state = _uiState.value
        val cards = state.cards
        if (cards.isEmpty()) return

        if (forward) {
            if (state.currentIndex < cards.size - 1) {
                val newIndex = state.currentIndex + 1
                val transition = detectZoneChange(cards[newIndex])
                _uiState.update {
                    it.copy(
                        currentIndex = newIndex,
                        zoneTransition = transition,
                    )
                }
                if (newIndex >= cards.size - 5) {
                    loadCards(reset = false)
                }
            } else if (wrap && cards.size > 1) {
                val transition = detectZoneChange(cards[0])
                _uiState.update {
                    it.copy(
                        currentIndex = 0,
                        zoneTransition = transition,
                    )
                }
            }
        } else {
            if (state.currentIndex > 0) {
                val newIndex = state.currentIndex - 1
                val transition = detectZoneChange(cards[newIndex])
                _uiState.update {
                    it.copy(
                        currentIndex = newIndex,
                        zoneTransition = transition,
                    )
                }
            }
        }
    }

    private fun detectZoneChange(card: NearbyCandidate): String? {
        val key = if (card.cityLabel.isNotEmpty()) card.cityLabel else card.zoneType
        if (currentZone == null) {
            currentZone = key
            return null
        }
        if (currentZone == key) return null
        currentZone = key
        return if (card.cityLabel.isNotEmpty()) "افراد ${card.cityLabel}" else "افراد دورتر"
    }

    fun clearMatchDialog() {
        val wasDeck = _uiState.value.viewMode == NearbyViewMode.DECK
        _uiState.update { it.copy(matchedResult = null, matchedCandidate = null) }
        if (wasDeck) {
            advance(forward = true)
        }
    }

    fun clearZoneTransition() {
        _uiState.update { it.copy(zoneTransition = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun setPreferencesSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isPreferencesSheetVisible = visible) }
    }

    fun updatePreferences(prefs: NearbyPreferences) {
        viewModelScope.launch {
            _uiState.update { it.copy(preferences = prefs, isPreferencesSheetVisible = false) }
            runCatching { repository.updatePreferences(prefs) }
            loadCards(reset = true)
        }
    }

    fun disableDiscovery() {
        viewModelScope.launch {
            runCatching { repository.disableLocation() }
            _uiState.update { it.copy(isDisabled = true, isPreferencesSheetVisible = false) }
        }
    }

    fun reportUser(targetId: String, reason: String) {
        viewModelScope.launch {
            runCatching { repository.report(targetId, reason) }
            _uiState.update { current ->
                current.copy(
                    cards = current.cards.filterNot { it.userId == targetId },
                    previewCandidate = null,
                    infoMessage = "گزارش با موفقیت ثبت شد و این کاربر مخفی گردید",
                )
            }
        }
    }

    fun unmatch(matchId: String) {
        viewModelScope.launch {
            runCatching { repository.unmatch(matchId) }
            loadLikesAndMatches()
        }
    }

    private fun mapNearbyError(err: Throwable, fallback: String): String {
        return when ((err as? RemoteFailure)?.code) {
            "insufficient_coins" -> "موجودی سکه کافی نیست — از فروشگاه سکه تهیه کنید"
            "insufficient_chat_request_credits" -> "اعتبار درخواست چت کافی نیست — از فروشگاه بسته تهیه کنید"
            "daily_chat_request_limit_reached" -> "سقف ارسال درخواست چت روزانه شما پر شده است"
            "daily_like_limit" -> "سقف لایک روزانه شما پر شده است"
            "chat_request_already_pending" -> "قبلاً برای این کاربر درخواست چت فعال ارسال کرده‌اید"
            "already_matched_open_chat_directly" -> "شما با هم مَچ هستید و می‌توانید رایگان چت کنید!"
            "inappropriate_intro_message" -> "متن پیام دارای کلمات نامجاز است"
            "inappropriate_status_text" -> "متن وضعیت دارای کلمات نامجاز است"
            "suspicious_location_jump" -> "تغییر ناگهانی موقعیت مجاز نیست؛ لطفاً کمی بعد تلاش کنید"
            "rate_limit_exceeded" -> "تعداد درخواست‌ها زیاد است؛ کمی صبر کنید"
            "feature_disabled" -> "سرویس اطراف من موقتاً توسط مدیریت غیرفعال است"
            "banned_from_nearby" -> "دسترسی شما به بخش اطراف من محدود شده است"
            "user_blocked", "user_reported" -> "امکان ارتباط با این کاربر وجود ندارد"
            else -> (err as? RemoteFailure)?.message?.takeIf { it.isNotBlank() } ?: fallback
        }
    }
}
