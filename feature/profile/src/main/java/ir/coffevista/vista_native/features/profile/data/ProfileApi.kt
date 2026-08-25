package ir.coffevista.vista_native.features.profile.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

interface ProfileApi {
    @GET("v1/me/profile")
suspend fun fetchOwnProfile(): Response<ProfileDto>

@POST("v1/me/profile/update")
    suspend fun updateOwnProfile(
        @Body request: ProfileUpdateRequestDto,
    ): Response<ProfileDto>

    @POST("v1/me/profile/update")
    suspend fun updateAvatar(@Body request: ProfileAvatarUpdateRequestDto): Response<ProfileDto>

    @POST("v1/uploads/presign")
    suspend fun presignUpload(@Body request: ProfileMediaPresignRequestDto): ProfileMediaPresignResponseDto

    @POST("v1/uploads/delete")
    suspend fun deleteUpload(@Body request: ProfileMediaDeleteRequestDto): ProfileMediaDeleteResponseDto
}
