package ir.coffevista.vista_native.features.profile.settings.privacy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ActiveSessionsApi {
    @GET("v1/sessions/active")
    suspend fun getActiveSessions(): Response<ActiveSessionsResponseDto>

    @POST("v1/sessions/terminate")
    suspend fun terminate(@Body request: TerminateSessionRequestDto): Response<SessionMutationResponseDto>
}

@Serializable
data class ActiveSessionsResponseDto(
    val sessions: List<ActiveSessionDto> = emptyList(),
)

@Serializable
data class ActiveSessionDto(
    val id: String,
    @SerialName("device_name") val deviceName: String? = null,
    val platform: String? = null,
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("location_city") val locationCity: String? = null,
    @SerialName("location_country") val locationCountry: String? = null,
    @SerialName("last_activity") val lastActivity: String? = null,
    @SerialName("is_current_session") val isCurrentSession: Boolean = false,
)

@Serializable
data class TerminateSessionRequestDto(
    @SerialName("target_session_id") val targetSessionId: String,
)

@Serializable
data class SessionMutationResponseDto(
    val success: Boolean = false,
)
