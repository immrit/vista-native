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
    suspend fun discoverRandomOnline(limit: Int = 20): List<NearbyCandidate>
    suspend fun like(targetId: String, action: String = "like"): NearbyLikeResult
    suspend fun getMatches(): List<NearbyMatch>
    suspend fun unmatch(matchId: String)
    suspend fun openChat(matchId: String): String
    suspend fun getLikesReceived(limit: Int = 50): NearbyReceivedLikes
    suspend fun report(targetId: String, reason: String)
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
            )
        ).bodyOrThrow("update_preferences_failed")
        return resp.toDomain()
    }

    override suspend fun discover(limit: Int, offset: Int): List<NearbyCandidate> {
        val resp = api.discover(limit = limit, offset = offset).bodyOrThrow("discover_empty")
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
