package ir.coffevista.vista_native.features.profile.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PublicProfileApi {
    @GET("v1/profiles/{userId}")
    suspend fun fetchPublicProfile(
        @Path("userId") userId: String,
    ): Response<PublicProfileDto>

    @POST("v1/me/follow")
    suspend fun follow(
        @Body request: FollowActionRequestDto,
    ): Response<FollowActionResponseDto>

    @POST("v1/me/unfollow")
    suspend fun unfollow(
        @Body request: FollowActionRequestDto,
    ): Response<UnfollowResponseDto>

    @GET("v1/users/{userId}/followers")
    suspend fun getFollowers(
        @Path("userId") userId: String,
        @retrofit2.http.Query("limit") limit: Int = 30,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): Response<FollowListResponseDto>

    @GET("v1/users/{userId}/following")
    suspend fun getFollowing(
        @Path("userId") userId: String,
        @retrofit2.http.Query("limit") limit: Int = 30,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): Response<FollowListResponseDto>
}
