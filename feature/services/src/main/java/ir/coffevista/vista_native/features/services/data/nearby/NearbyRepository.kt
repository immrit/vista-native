package ir.coffevista.vista_native.features.services.data.nearby

import ir.coffevista.vista_native.core.network.BackendErrorParser
import ir.coffevista.vista_native.core.network.RemoteFailure
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

interface NearbyRepository {
    suspend fun updateLocation(lat: Double, lng: Double, cityName: String? = null, provinceName: String? = null)
    suspend fun disableLocation()
    suspend fun getPreferences(): NearbyPreferences
    suspend fun updatePreferences(prefs: NearbyPreferences): NearbyPreferences
    suspend fun discover(limit: Int = 20, offset: Int = 0): List<NearbyCandidate>
    suspend fun discoverFiltered(limit: Int = 20, offset: Int = 0, zone: String? = null): List<NearbyCandidate> =
        discover(limit = limit, offset = offset)
    suspend fun discoverRandomOnline(limit: Int = 20): List<NearbyCandidate>
    suspend fun like(targetId: String, action: String = "like"): NearbyLikeResult
    suspend fun getMatches(): List<NearbyMatch>
    suspend fun unmatch(matchId: String)
    suspend fun openChat(matchId: String): String
    suspend fun getLikesReceived(limit: Int = 50): NearbyReceivedLikes
    suspend fun report(targetId: String, reason: String)
    suspend fun undo(targetId: String) {}

    suspend fun getWallet(): NearbyWallet = NearbyWallet()
    suspend fun getShopCatalog(): NearbyShopCatalog = NearbyShopCatalog()
    suspend fun buyChatRequestPack(packId: String): NearbyBuyRequestsResult =
        throw UnsupportedOperationException("buyChatRequestPack not implemented")
    suspend fun getChatRequests(direction: String = "incoming", limit: Int = 50): List<NearbyChatRequest> = emptyList()
    suspend fun sendChatRequest(targetId: String, introMessage: String = ""): NearbySendChatRequestResult =
        throw UnsupportedOperationException("sendChatRequest not implemented")
    suspend fun respondChatRequest(requestId: String, action: String): NearbyRespondChatRequestResult =
        throw UnsupportedOperationException("respondChatRequest not implemented")
    suspend fun getCoinPackages(): List<NearbyCoinPackage> = emptyList()
    suspend fun verifyBazaarPurchase(
        purchaseToken: String,
        productId: String,
        packageName: String,
    ): NearbyBazaarVerifyResult =
        throw UnsupportedOperationException("verifyBazaarPurchase not implemented")
}

@Singleton
class DefaultNearbyRepository @Inject constructor(
    private val api: NearbyApi,
) : NearbyRepository {

    override suspend fun updateLocation(
        lat: Double,
        lng: Double,
        cityName: String?,
        provinceName: String?,
    ) {
        api.updateLocation(
            LocationUpdateRequestDto(
                lat = lat,
                lng = lng,
                cityName = cityName,
                provinceName = provinceName,
            )
        ).bodyOrThrow("update_location_failed")
    }

    override suspend fun disableLocation() {
        api.disableLocation().bodyOrThrow("disable_location_failed")
    }

    override suspend fun getPreferences(): NearbyPreferences {
        val resp = api.getPreferences().bodyOrThrow("preferences_empty")
        return resp.toDomain()
    }

    override suspend fun updatePreferences(prefs: NearbyPreferences): NearbyPreferences {
        val resp = api.updatePreferences(
            NearbyPreferencesUpdateRequestDto(
                interestedIn = prefs.interestedIn,
                minAge = prefs.minAge,
                maxAge = prefs.maxAge,
                maxDistanceKm = prefs.maxDistanceKm,
                maritalPref = prefs.maritalPref,
                statusText = prefs.statusText,
                hideDistance = prefs.hideDistance,
            )
        ).bodyOrThrow("update_preferences_failed")
        return resp.toDomain()
    }

    override suspend fun discover(limit: Int, offset: Int): List<NearbyCandidate> =
        discoverFiltered(limit = limit, offset = offset, zone = null)

    override suspend fun discoverFiltered(limit: Int, offset: Int, zone: String?): List<NearbyCandidate> {
        val resp = api.discover(limit = limit, offset = offset, zone = zone).bodyOrThrow("discover_empty")
        return resp.candidates.map { it.toDomain() }
    }

    override suspend fun discoverRandomOnline(limit: Int): List<NearbyCandidate> {
        val resp = api.discoverRandomOnline(limit = limit).bodyOrThrow("random_online_empty")
        return resp.candidates.map { it.toDomain() }
    }

    override suspend fun like(targetId: String, action: String): NearbyLikeResult {
        val resp = api.like(NearbyLikeRequestDto(targetId = targetId, action = action)).bodyOrThrow("like_failed")
        return resp.toDomain()
    }

    override suspend fun getMatches(): List<NearbyMatch> {
        val resp = api.getMatches().bodyOrThrow("matches_empty")
        return resp.matches.map { it.toDomain() }
    }

    override suspend fun unmatch(matchId: String) {
        api.unmatch(matchId).bodyOrThrow("unmatch_failed")
    }

    override suspend fun openChat(matchId: String): String {
        val resp = api.openChat(matchId).bodyOrThrow("open_chat_failed")
        return resp.conversationId
    }

    override suspend fun getLikesReceived(limit: Int): NearbyReceivedLikes {
        val resp = api.getLikesReceived(limit = limit).bodyOrThrow("likes_received_empty")
        return resp.toDomain()
    }

    override suspend fun report(targetId: String, reason: String) {
        api.report(NearbyReportRequestDto(targetId = targetId, reason = reason)).bodyOrThrow("report_failed")
    }

    override suspend fun undo(targetId: String) {
        api.undo(NearbyUndoRequestDto(targetId = targetId)).bodyOrThrow("undo_failed")
    }

    override suspend fun getWallet(): NearbyWallet =
        api.getWallet().bodyOrThrow("wallet_failed").toDomain()

    override suspend fun getShopCatalog(): NearbyShopCatalog =
        api.getShopCatalog().bodyOrThrow("shop_catalog_failed").toDomain()

    override suspend fun buyChatRequestPack(packId: String): NearbyBuyRequestsResult =
        api.buyChatRequestPack(NearbyBuyRequestsRequestDto(packId = packId))
            .bodyOrThrow("buy_requests_failed")
            .toDomain()

    override suspend fun getChatRequests(direction: String, limit: Int): List<NearbyChatRequest> =
        api.getChatRequests(direction = direction, limit = limit)
            .bodyOrThrow("chat_requests_failed")
            .requests
            .map { it.toDomain() }

    override suspend fun sendChatRequest(targetId: String, introMessage: String): NearbySendChatRequestResult =
        api.sendChatRequest(NearbySendChatRequestDto(targetId = targetId, introMessage = introMessage))
            .bodyOrThrow("send_chat_request_failed")
            .toDomain()

    override suspend fun respondChatRequest(requestId: String, action: String): NearbyRespondChatRequestResult =
        api.respondChatRequest(NearbyRespondChatRequestDto(requestId = requestId, action = action))
            .bodyOrThrow("respond_chat_request_failed")
            .toDomain()

    override suspend fun getCoinPackages(): List<NearbyCoinPackage> =
        api.getCoinPackages()
            .bodyOrThrow("coin_packages_failed")
            .packages
            .map { it.toDomain() }

    override suspend fun verifyBazaarPurchase(
        purchaseToken: String,
        productId: String,
        packageName: String,
    ): NearbyBazaarVerifyResult =
        api.verifyBazaarPurchase(
            NearbyBazaarVerifyRequestDto(
                purchaseToken = purchaseToken,
                productId = productId,
                packageName = packageName,
            ),
        )
            .bodyOrThrow("bazaar_verify_failed")
            .toDomain()

    private fun <T> Response<T>.bodyOrThrow(emptyCode: String): T {
        if (isSuccessful) {
            val b = body()
            if (b != null || this.raw().code == 204 || this.raw().code == 200) {
                @Suppress("UNCHECKED_CAST")
                return (b ?: Unit) as T
            }
            throw RemoteFailure(
                statusCode = code(),
                code = emptyCode,
                message = "پاسخ سرور خالی بود",
            )
        }
        val parsed = BackendErrorParser.parse(
            rawBody = errorBody()?.string().orEmpty(),
            retryAfterHeader = headers()["Retry-After"],
        )
        throw RemoteFailure(
            statusCode = code(),
            code = parsed.code ?: emptyCode,
            message = parsed.message ?: "خطای ناشناخته در ارتباط با سرور",
        )
    }
}
