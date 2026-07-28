package ir.coffevista.vista_native.features.feed.data

import retrofit2.http.GET
import retrofit2.http.Query

interface FeedApi {
    @GET("v1/feed")
    suspend fun getFeed(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): FeedResponseDto
}
