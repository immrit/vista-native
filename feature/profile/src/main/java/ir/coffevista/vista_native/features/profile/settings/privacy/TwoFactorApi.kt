package ir.coffevista.vista_native.features.profile.settings.privacy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TwoFactorApi {
    @GET("v1/auth/2fa/status")
    suspend fun getStatus(): Response<TwoFAStatusDto>

    @POST("v1/auth/2fa/setup")
    suspend fun setupOrUpdate(@Body body: TwoFASetupRequestDto): Response<TwoFASetupResponseDto>

    @POST("v1/auth/2fa/confirm-email")
    suspend fun confirmEmail(@Body body: TwoFAConfirmEmailRequestDto): Response<TwoFASetupResponseDto>

    @POST("v1/auth/2fa/resend-email")
    suspend fun resendEmail(): Response<TwoFASetupResponseDto>

    @POST("v1/auth/2fa/abort-setup")
    suspend fun abortSetup(): Response<TwoFAStatusDto>

    @POST("v1/auth/2fa/hint")
    suspend fun updateHint(@Body body: TwoFAUpdateHintRequestDto): Response<TwoFAStatusDto>

    @POST("v1/auth/2fa/recovery-email")
    suspend fun changeRecoveryEmail(@Body body: TwoFAChangeEmailRequestDto): Response<TwoFASetupResponseDto>

    @POST("v1/auth/2fa/remove")
    suspend fun remove(@Body body: Remove2FADto): Response<TwoFAMessageDto>

    @POST("v1/auth/2fa/reset/cancel")
    suspend fun cancelPendingReset(): Response<TwoFAStatusDto>
}

@Serializable
data class TwoFAStatusDto(
    @SerialName("has_password") val hasPassword: Boolean = false,
    @SerialName("hint") val hint: String = "",
    @SerialName("has_recovery") val hasRecovery: Boolean = false,
    @SerialName("recovery_email_pattern") val recoveryEmailPattern: String? = null,
    @SerialName("email_unconfirmed_pattern") val emailUnconfirmedPattern: String? = null,
    @SerialName("pending_reset_date") val pendingResetDate: Long = 0L,
    @SerialName("phone_verified") val phoneVerified: Boolean = false,
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("verified_phone") val verifiedPhone: String? = null,
    @SerialName("verified_email") val verifiedEmail: String? = null,
    @SerialName("can_enable") val canEnable: Boolean = false,
)

@Serializable
data class TwoFASetupRequestDto(
    @SerialName("current_password") val currentPassword: String = "",
    @SerialName("new_password") val newPassword: String,
    @SerialName("hint") val hint: String = "",
    @SerialName("recovery_email") val recoveryEmail: String = "",
    @SerialName("keep_recovery_email") val keepRecoveryEmail: Boolean = false,
)

@Serializable
data class TwoFASetupResponseDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String? = null,
    @SerialName("requires_email_confirm") val requiresEmailConfirm: Boolean = false,
    @SerialName("email_pattern") val emailPattern: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("expires_in_seconds") val expiresInSeconds: Long = 600L,
    @SerialName("status") val status: TwoFAStatusDto? = null,
)

@Serializable
data class TwoFAConfirmEmailRequestDto(
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("code") val code: String,
)

@Serializable
data class TwoFAUpdateHintRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("hint") val hint: String,
)

@Serializable
data class TwoFAChangeEmailRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("recovery_email") val recoveryEmail: String,
)

@Serializable
data class TwoFAMessageDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("message") val message: String? = null,
)
