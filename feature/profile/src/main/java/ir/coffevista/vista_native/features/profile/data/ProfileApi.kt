package ir.coffevista.vista_native.features.profile.data

import retrofit2.Response
import retrofit2.http.GET

interface ProfileApi {
    @GET("v1/me/profile")
    suspend fun fetchOwnProfile(): Response<ProfileDto>
}
