package ir.coffevista.vista_native.features.profile.settings.privacy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BlockedUsersApi {
    @GET("v1/me/blocked-users")
    suspend fun getBlockedUsers(): Response<BlockedUsersResponseDto>

    @POST("v1/me/unblock")
    suspend fun unblock(@Body request: UnblockUserRequestDto): Response<Unit>
}

@Serializable
data class BlockedUsersResponseDto(
    val profiles: List<BlockedUserDto> = emptyList(),
)

@Serializable
data class BlockedUserDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val username: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class UnblockUserRequestDto(
    @SerialName("user_id") val userId: String,
)
