package ir.coffevista.vista_native.features.profile.settings.password

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ChangePasswordApi {
    @POST("v1/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): Response<Unit>
}

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)
