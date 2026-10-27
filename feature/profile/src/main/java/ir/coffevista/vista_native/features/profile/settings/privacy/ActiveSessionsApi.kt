package ir.coffevista.vista_native.features.profile.settings.privacy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ActiveSessionsApi {
    @GET("v1/sessions/active")
    suspend fun getActiveSessions(
        @Query("current_session_id") currentSessionId: String? = null,
    ): Response<ActiveSessionsResponseDto>

    @POST("v1/sessions/register")
    suspend fun register(
        @Body request: RegisterSessionRequestDto,
    ): Response<ActiveSessionDto>

    @POST("v1/sessions/validate")
    suspend fun validate(
        @Body request: ValidateSessionRequestDto,
    ): Response<ValidateSessionResponseDto>

    @POST("v1/sessions/touch")
    suspend fun touch(
        @Body request: TouchSessionRequestDto,
    ): Response<TouchSessionResponseDto>

    @POST("v1/sessions/terminate")
    suspend fun terminate(@Body request: TerminateSessionRequestDto): Response<SessionMutationResponseDto>

    @POST("v1/sessions/terminate-others")
    suspend fun terminateOthers(
        @Body request: TerminateOthersRequestDto,
    ): Response<TerminateOthersResponseDto>
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
data class RegisterSessionRequestDto(
    @SerialName("session_id") val sessionId: String,
    @SerialName("session_token") val sessionToken: String,
    @SerialName("device_info") val deviceInfo: Map<String, String> = emptyMap(),
    @SerialName("app_version") val appVersion: String = "1.0.0",
    val platform: String = "android",
    @SerialName("fcm_token") val fcmToken: String? = null,
)

@Serializable
data class ValidateSessionRequestDto(
    @SerialName("session_id") val sessionId: String,
    @SerialName("session_token") val sessionToken: String,
)

@Serializable
data class ValidateSessionResponseDto(
    val valid: Boolean = false,
    val reason: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class TouchSessionRequestDto(
    @SerialName("session_id") val sessionId: String,
    @SerialName("session_token") val sessionToken: String,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("fcm_sync_epoch") val fcmSyncEpoch: Long = 0L,
)

@Serializable
data class TouchSessionResponseDto(
    val valid: Boolean = false,
    @SerialName("fcm_resync_epoch") val fcmResyncEpoch: Long = 0L,
    @SerialName("require_fcm_sync") val requireFcmSync: Boolean = false,
)

@Serializable
data class TerminateSessionRequestDto(
    @SerialName("target_session_id") val targetSessionId: String,
    @SerialName("current_session_id") val currentSessionId: String? = null,
)

@Serializable
data class TerminateOthersRequestDto(
    @SerialName("current_session_id") val currentSessionId: String,
)

@Serializable
data class TerminateOthersResponseDto(
    val count: Long = 0L,
)

@Serializable
data class SessionMutationResponseDto(
    val success: Boolean = false,
)
