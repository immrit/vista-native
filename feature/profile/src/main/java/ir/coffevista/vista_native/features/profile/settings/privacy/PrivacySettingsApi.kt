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
}

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
