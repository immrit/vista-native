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
    @SerialName("is_enabled") val isEnabled: Boolean = false,
    @SerialName("has_location") val hasLocation: Boolean = false,
) {
    fun toDomain(): NearbyPreferences = NearbyPreferences(
        interestedIn = interestedIn,
        minAge = minAge,
        maxAge = maxAge,
        maxDistanceKm = maxDistanceKm,
        maritalPref = maritalPref,
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
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("verification_type") val verificationType: String = "",
    @SerialName("distance_km") val distanceKm: Double = 0.0,
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
        isVerified = isVerified,
        verificationType = verificationType,
        distanceKm = distanceKm,
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
) {
    fun toDomain(): NearbyLikeResult = NearbyLikeResult(
        matched = matched,
        matchId = matchId,
        match = match?.toDomain(),
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
    @SerialName("action") val action: String = "like",
) {
    fun toDomain(): NearbyReceivedLike = NearbyReceivedLike(
        userId = userId,
        username = username,
        fullName = fullName,
        avatarUrl = avatarUrl,
        isVerified = isVerified,
        verificationType = verificationType,
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
