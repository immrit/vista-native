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

    @GET("v1/profiles/by-username/{username}")
    suspend fun fetchProfileByUsername(
        @Path("username") username: String,
    ): Response<PublicProfileDto> = fetchPublicProfile(username)

    @POST("v1/me/follow")
    suspend fun follow(
        @Body request: FollowActionRequestDto,
    ): Response<FollowActionResponseDto>

    @POST("v1/me/unfollow")
    suspend fun unfollow(
        @Body request: FollowActionRequestDto,
    ): Response<UnfollowResponseDto>

    @GET("v1/profiles/followers/{userId}")
    suspend fun getFollowers(
        @Path("userId") userId: String,
        @retrofit2.http.Query("limit") limit: Int = 30,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): Response<FollowListResponseDto> = Response.success(FollowListResponseDto())

    @GET("v1/profiles/following/{userId}")
    suspend fun getFollowing(
        @Path("userId") userId: String,
        @retrofit2.http.Query("limit") limit: Int = 30,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): Response<FollowListResponseDto> = Response.success(FollowListResponseDto())
}
