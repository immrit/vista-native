package ir.coffevista.vista_native.features.services.data.nearby

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationUpdateRequestDto(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("city_name") val cityName: String? = null,
    @SerialName("province_name") val provinceName: String? = null,
)

@Serializable
data class NearbyPreferencesDto(
    @SerialName("interested_in") val interestedIn: String = "all",
    @SerialName("min_age") val minAge: Int = 18,
    @SerialName("max_age") val maxAge: Int = 60,
    @SerialName("max_distance_km") val maxDistanceKm: Int = 50,
    @SerialName("marital_pref") val maritalPref: String = "all",
    @SerialName("status_text") val statusText: String = "",
    @SerialName("hide_distance") val hideDistance: Boolean = false,
    @SerialName("is_enabled") val isEnabled: Boolean = false,
    @SerialName("has_location") val hasLocation: Boolean = false,
) {
    fun toDomain(): NearbyPreferences = NearbyPreferences(
        interestedIn = interestedIn,
        minAge = minAge,
        maxAge = maxAge,
        maxDistanceKm = maxDistanceKm,
        maritalPref = maritalPref,
        statusText = statusText,
        hideDistance = hideDistance,
        isEnabled = isEnabled,
        hasLocation = hasLocation,
    )
}

@Serializable
data class NearbyPreferencesUpdateRequestDto(
    @SerialName("interested_in") val interestedIn: String,
    @SerialName("min_age") val minAge: Int,
    @SerialName("max_age") val maxAge: Int,
    @SerialName("max_distance_km") val maxDistanceKm: Int,
    @SerialName("marital_pref") val maritalPref: String,
    @SerialName("status_text") val statusText: String = "",
    @SerialName("hide_distance") val hideDistance: Boolean = false,
)

@Serializable
data class NearbyCandidateDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("bio") val bio: String = "",
    @SerialName("gender") val gender: String = "",
    @SerialName("marital_status") val maritalStatus: String = "",
    @SerialName("age") val age: Int = 0,
    @SerialName("location_text") val locationText: String = "",
    @SerialName("status_text") val statusText: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("verification_type") val verificationType: String = "",
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("hide_distance") val hideDistance: Boolean = false,
    @SerialName("has_pending_chat_request") val hasPendingChatRequest: Boolean = false,
    @SerialName("compatibility_score") val compatibilityScore: Int = 0,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("zone") val zone: String? = null,
    @SerialName("city_name") val cityName: String? = null,
    @SerialName("province_name") val provinceName: String? = null,
    @SerialName("is_online_now") val isOnlineNow: Boolean = false,
) {
    fun toDomain(): NearbyCandidate = NearbyCandidate(
        userId = userId,
        username = username,
        fullName = fullName,
        avatarUrl = avatarUrl,
        bio = bio,
        gender = gender,
        maritalStatus = maritalStatus,
        age = age,
        locationText = locationText,
        statusText = statusText,
        isVerified = isVerified,
        verificationType = verificationType,
        distanceKm = distanceKm,
        hideDistance = hideDistance,
        hasPendingChatRequest = hasPendingChatRequest,
        compatibilityScore = compatibilityScore,
        lastSeenAt = lastSeenAt,
        zone = zone,
        cityName = cityName,
        provinceName = provinceName,
        isOnlineNow = isOnlineNow,
    )
}

@Serializable
data class NearbyDiscoverResponseDto(
    @SerialName("candidates") val candidates: List<NearbyCandidateDto> = emptyList(),
)

@Serializable
data class NearbyLikeRequestDto(
    @SerialName("target_id") val targetId: String,
    @SerialName("action") val action: String = "like", // like | pass | superlike
)

@Serializable
data class NearbyMatchDto(
    @SerialName("match_id") val matchId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("verification_type") val verificationType: String = "",
    @SerialName("is_online_now") val isOnlineNow: Boolean = false,
    @SerialName("city_name") val cityName: String = "",
    @SerialName("province_name") val provinceName: String = "",
    @SerialName("hide_distance") val hideDistance: Boolean = false,
    @SerialName("matched_at") val matchedAt: String = "",
) {
    fun toDomain(): NearbyMatch = NearbyMatch(
        matchId = matchId,
        userId = userId,
        username = username,
        fullName = fullName,
        avatarUrl = avatarUrl,
        isVerified = isVerified,
        verificationType = verificationType,
        isOnlineNow = isOnlineNow,
        cityName = cityName,
        provinceName = provinceName,
        hideDistance = hideDistance,
        matchedAt = matchedAt,
    )
}

@Serializable
data class NearbyMatchesResponseDto(
    @SerialName("matches") val matches: List<NearbyMatchDto> = emptyList(),
)

@Serializable
data class NearbyLikeResponseDto(
    @SerialName("matched") val matched: Boolean = false,
    @SerialName("match_id") val matchId: String = "",
    @SerialName("match") val match: NearbyMatchDto? = null,
    @SerialName("daily_likes_remaining") val dailyLikesRemaining: Int = 0,
    @SerialName("coins_balance") val coinsBalance: Long = 0,
) {
    fun toDomain(): NearbyLikeResult = NearbyLikeResult(
        matched = matched,
        matchId = matchId,
        match = match?.toDomain(),
        dailyLikesRemaining = dailyLikesRemaining,
        coinsBalance = coinsBalance,
    )
}

@Serializable
data class NearbyReceivedLikeDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("verification_type") val verificationType: String = "",
    @SerialName("is_online_now") val isOnlineNow: Boolean = false,
    @SerialName("city_name") val cityName: String = "",
    @SerialName("hide_distance") val hideDistance: Boolean = false,
    @SerialName("action") val action: String = "like",
) {
    fun toDomain(): NearbyReceivedLike = NearbyReceivedLike(
        userId = userId,
        username = username,
        fullName = fullName,
        avatarUrl = avatarUrl,
        isVerified = isVerified,
        verificationType = verificationType,
        isOnlineNow = isOnlineNow,
        cityName = cityName,
        hideDistance = hideDistance,
        action = action,
    )
}

@Serializable
data class NearbyReceivedLikesResponseDto(
    @SerialName("count") val count: Int = 0,
    @SerialName("likes") val likes: List<NearbyReceivedLikeDto> = emptyList(),
) {
    fun toDomain(): NearbyReceivedLikes = NearbyReceivedLikes(
        count = count,
        likes = likes.map { it.toDomain() },
    )
}

@Serializable
data class NearbyOpenChatResponseDto(
    @SerialName("conversation_id") val conversationId: String = "",
)

@Serializable
data class NearbyReportRequestDto(
    @SerialName("target_id") val targetId: String,
    @SerialName("reason") val reason: String,
)

@Serializable
data class NearbyUndoRequestDto(
    @SerialName("target_id") val targetId: String,
)

// ── Wallet, Shop & Direct Chat Requests ──────────────────────────────────────

@Serializable
data class NearbyWalletDto(
    @SerialName("coins") val coins: Long = 0,
    @SerialName("chat_request_credits") val chatRequestCredits: Int = 0,
    @SerialName("free_daily_requests_remaining") val freeDailyRequestsRemaining: Int = 0,
    @SerialName("daily_likes_remaining") val dailyLikesRemaining: Int = 0,
    @SerialName("chat_request_coin_cost") val chatRequestCoinCost: Int = 20,
    @SerialName("superlike_coin_cost") val superlikeCoinCost: Int = 10,
) {
    fun toDomain(): NearbyWallet = NearbyWallet(
        coins = coins,
        chatRequestCredits = chatRequestCredits,
        freeDailyRequestsRemaining = freeDailyRequestsRemaining,
        dailyLikesRemaining = dailyLikesRemaining,
        chatRequestCoinCost = chatRequestCoinCost,
        superlikeCoinCost = superlikeCoinCost,
    )
}

@Serializable
data class NearbyChatRequestPackDto(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("requests_count") val requestsCount: Int = 0,
    @SerialName("coin_cost") val coinCost: Int = 0,
    @SerialName("bonus_label") val bonusLabel: String = "",
    @SerialName("badge") val badge: String = "",
    @SerialName("is_popular") val isPopular: Boolean = false,
) {
    fun toDomain(): NearbyChatRequestPack = NearbyChatRequestPack(
        id = id,
        title = title,
        requestsCount = requestsCount,
        coinCost = coinCost,
        bonusLabel = bonusLabel,
        badge = badge,
        isPopular = isPopular,
    )
}

@Serializable
data class NearbyShopCatalogDto(
    @SerialName("wallet") val wallet: NearbyWalletDto = NearbyWalletDto(),
    @SerialName("packs") val packs: List<NearbyChatRequestPackDto> = emptyList(),
    @SerialName("coins_per_request") val coinsPerRequest: Int = 20,
    @SerialName("sample_rate_label") val sampleRateLabel: String = "",
) {
    fun toDomain(): NearbyShopCatalog = NearbyShopCatalog(
        wallet = wallet.toDomain(),
        packs = packs.map { it.toDomain() },
        coinsPerRequest = coinsPerRequest,
        sampleRateLabel = sampleRateLabel,
    )
}

@Serializable
data class NearbyBuyRequestsRequestDto(
    @SerialName("pack_id") val packId: String,
)

@Serializable
data class NearbyBuyRequestsResponseDto(
    @SerialName("pack") val pack: NearbyChatRequestPackDto = NearbyChatRequestPackDto(),
    @SerialName("wallet") val wallet: NearbyWalletDto = NearbyWalletDto(),
) {
    fun toDomain(): NearbyBuyRequestsResult = NearbyBuyRequestsResult(
        pack = pack.toDomain(),
        wallet = wallet.toDomain(),
    )
}

@Serializable
data class NearbyChatRequestDto(
    @SerialName("id") val id: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("recipient_id") val recipientId: String = "",
    @SerialName("peer_id") val peerId: String = "",
    @SerialName("direction") val direction: String = "incoming",
    @SerialName("username") val username: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_online_now") val isOnlineNow: Boolean = false,
    @SerialName("age") val age: Int = 0,
    @SerialName("bio") val bio: String = "",
    @SerialName("status_text") val statusText: String = "",
    @SerialName("city_name") val cityName: String = "",
    @SerialName("province_name") val provinceName: String = "",
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("hide_distance") val hideDistance: Boolean = false,
    @SerialName("intro_message") val introMessage: String = "",
    @SerialName("status") val status: String = "pending",
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("responded_at") val respondedAt: String? = null,
) {
    fun toDomain(): NearbyChatRequest = NearbyChatRequest(
        id = id,
        senderId = senderId,
        recipientId = recipientId,
        peerId = peerId,
        direction = direction,
        username = username,
        fullName = fullName,
        avatarUrl = avatarUrl,
        isVerified = isVerified,
        isOnlineNow = isOnlineNow,
        age = age,
        bio = bio,
        statusText = statusText,
        cityName = cityName,
        provinceName = provinceName,
        distanceKm = distanceKm,
        hideDistance = hideDistance,
        introMessage = introMessage,
        status = status,
        conversationId = conversationId,
        createdAt = createdAt,
        respondedAt = respondedAt,
    )
}

@Serializable
data class NearbyChatRequestsListResponseDto(
    @SerialName("requests") val requests: List<NearbyChatRequestDto> = emptyList(),
)

@Serializable
data class NearbySendChatRequestDto(
    @SerialName("target_id") val targetId: String,
    @SerialName("intro_message") val introMessage: String = "",
)

@Serializable
data class NearbySendChatRequestResponseDto(
    @SerialName("request") val request: NearbyChatRequestDto = NearbyChatRequestDto(),
    @SerialName("wallet") val wallet: NearbyWalletDto = NearbyWalletDto(),
    @SerialName("spent_kind") val spentKind: String = "",
    @SerialName("coins_spent") val coinsSpent: Int = 0,
    @SerialName("credits_spent") val creditsSpent: Int = 0,
) {
    fun toDomain(): NearbySendChatRequestResult = NearbySendChatRequestResult(
        request = request.toDomain(),
        wallet = wallet.toDomain(),
        spentKind = spentKind,
        coinsSpent = coinsSpent,
        creditsSpent = creditsSpent,
    )
}

@Serializable
data class NearbyRespondChatRequestDto(
    @SerialName("request_id") val requestId: String,
    @SerialName("action") val action: String, // accept | reject | cancel
)

@Serializable
data class NearbyRespondChatRequestResponseDto(
    @SerialName("status") val status: String = "",
    @SerialName("conversation_id") val conversationId: String = "",
) {
    fun toDomain(): NearbyRespondChatRequestResult = NearbyRespondChatRequestResult(
        status = status,
        conversationId = conversationId,
    )
}

@Serializable
data class NearbyCoinPackageDto(
    @SerialName("id") val id: String = "",
    @SerialName("coins") val coins: Long = 0,
    @SerialName("bonus") val bonus: Long = 0,
    @SerialName("priceToman") val priceToman: Int = 0,
    @SerialName("amountRial") val amountRial: Int = 0,
) {
    fun toDomain(): NearbyCoinPackage = NearbyCoinPackage(
        id = id,
        coins = coins,
        bonus = bonus,
        priceToman = priceToman,
        amountRial = amountRial,
    )
}

@Serializable
data class NearbyCoinPackagesResponseDto(
    @SerialName("packages") val packages: List<NearbyCoinPackageDto> = emptyList(),
)

@Serializable
data class NearbyBazaarVerifyRequestDto(
    @SerialName("purchase_token") val purchaseToken: String,
    @SerialName("product_id") val productId: String,
    @SerialName("package_name") val packageName: String,
)

@Serializable
data class NearbyBazaarVerifyResponseDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("coins_added") val coinsAdded: Long = 0,
    @SerialName("new_balance") val newBalance: Long? = null,
    @SerialName("already_processed") val alreadyProcessed: Boolean = false,
) {
    fun toDomain(): NearbyBazaarVerifyResult = NearbyBazaarVerifyResult(
        success = success,
        message = message,
        type = type,
        coinsAdded = coinsAdded,
        newBalance = newBalance,
        alreadyProcessed = alreadyProcessed,
    )
}
