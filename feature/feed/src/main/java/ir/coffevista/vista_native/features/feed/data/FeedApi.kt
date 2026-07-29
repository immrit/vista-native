package ir.coffevista.vista_native.features.feed.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FeedApi {
    @GET("v1/explore")
    suspend fun getExploreFeed(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): FeedResponseDto

    @GET("v1/feed/following")
    suspend fun getFollowingFeed(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
    ): FeedResponseDto

    @GET("v1/posts/{postId}")
    suspend fun getPost(
        @Path("postId") postId: String,
    ): FeedPostDto

    @GET("v1/users/{userId}/posts")
    suspend fun getUserPosts(
        @Path("userId") userId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): FeedResponseDto
}
