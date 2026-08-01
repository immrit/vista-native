package ir.coffevista.vista_native.features.search.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchApi {
    @GET("v1/profiles/search")
    suspend fun searchProfiles(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): Response<SearchProfilesResponseDto>

    @GET("v1/profiles/by-username/{username}")
    suspend fun profileByUsername(
        @Path("username") username: String,
    ): Response<ExactProfileDto>

    @GET("v1/posts/hashtag/{tag}")
    suspend fun searchPostsByHashtag(
        @Path("tag") tag: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): Response<SearchPostsResponseDto>

    @GET("v1/hashtags/search")
    suspend fun searchHashtags(
        @Query("q") keyword: String,
        @Query("limit") limit: Int,
    ): Response<HashtagResponseDto>

    @GET("v1/hashtags/trending")
    suspend fun trendingHashtags(
        @Query("limit") limit: Int,
        @Query("days") days: Int,
    ): Response<HashtagResponseDto>
}
