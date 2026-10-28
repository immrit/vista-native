package ir.coffevista.vista_native.features.profile.settings.privacy

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Flutter's canonical settings blob plus the profile-level private-account gate. */
interface PrivacySettingsApi {
    @GET("v1/me/privacy")
    suspend fun getSettings(): Response<PrivacySettingsDto>

    @POST("v1/me/privacy")
    suspend fun updateSettings(@Body settings: PrivacySettingsDto): Response<Unit>

    @POST("v1/me/profile/update")
    suspend fun updatePrivateAccount(@Body request: PrivateAccountRequestDto): Response<Unit>

    @GET("v1/me/story-settings")
    suspend fun getStorySettings(): Response<StorySettingsDto>

    @retrofit2.http.PATCH("v1/me/story-settings")
    suspend fun updateStorySettings(@Body settings: StorySettingsDto): Response<Unit>

    @POST("v1/auth/account-deletion/request-otp")
    suspend fun requestAccountDeletionOtp(@Body request: RequestAccountDeletionOtpDto): Response<AccountDeletionOtpResponseDto>

    @POST("v1/auth/account-deletion/confirm")
    suspend fun confirmAccountDeletion(@Body request: ConfirmAccountDeletionDto): Response<ConfirmAccountDeletionResponseDto>

    @POST("v1/auth/2fa/remove")
    suspend fun remove2FA(@Body request: Remove2FADto): Response<Map<String, Boolean>>

    @POST("v1/auth/send-email-verification")
    suspend fun sendEmailVerification(@Body request: SendEmailVerificationDto): Response<SendEmailVerificationResponseDto>

    @POST("v1/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailDto): Response<VerifyEmailResponseDto>
}

@Serializable
data class SendEmailVerificationDto(
    val email: String,
)

@Serializable
data class SendEmailVerificationResponseDto(
    val success: Boolean = true,
    val message: String? = null,
    @kotlinx.serialization.SerialName("session_id") val sessionId: String? = null,
    @kotlinx.serialization.SerialName("expires_in_seconds") val expiresInSeconds: Long = 120,
)

@Serializable
data class VerifyEmailDto(
    @kotlinx.serialization.SerialName("session_id") val sessionId: String,
    val code: String,
)

@Serializable
data class VerifyEmailResponseDto(
    val success: Boolean = true,
    val message: String? = null,
)

@Serializable
data class RequestAccountDeletionOtpDto(
    val reason: String,
)

@Serializable
data class AccountDeletionOtpResponseDto(
    val success: Boolean = true,
    val message: String? = null,
    @kotlinx.serialization.SerialName("expires_in_seconds") val expiresInSeconds: Long = 120,
)

@Serializable
data class ConfirmAccountDeletionDto(
    @kotlinx.serialization.SerialName("otp_code") val otpCode: String,
    val password: String = "",
    val reason: String,
)

@Serializable
data class ConfirmAccountDeletionResponseDto(
    val success: Boolean = true,
    val message: String? = null,
    @kotlinx.serialization.SerialName("grace_period_days") val gracePeriodDays: Int = 30,
)

@Serializable
data class Remove2FADto(
    @kotlinx.serialization.SerialName("current_password") val currentPassword: String,
)

@Serializable
data class StorySettingsDto(
    @kotlinx.serialization.SerialName("reply_permission") val reply_permission: String = "everyone",
)

@Serializable
data class PrivacySettingsDto(
    val is_private: Boolean = false,
    val last_seen_visibility: String = "everyone",
    val message_privacy: String = "everyone",
    val group_add_privacy: String = "everyone",
    val read_receipts: Boolean = true,
    val allow_profile_zoom: Boolean = true,
)

@Serializable
data class PrivateAccountRequestDto(
    val is_private: Boolean,
)
