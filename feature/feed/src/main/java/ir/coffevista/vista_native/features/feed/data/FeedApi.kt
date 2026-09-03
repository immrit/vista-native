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

    @GET("v1/posts/hashtag/{tag}")
    suspend fun getHashtagPosts(
        @Path("tag") tag: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): FeedResponseDto

    @GET("v1/hashtags/trending")
    suspend fun getTrendingHashtags(
        @Query("limit") limit: Int = 20,
        @Query("days") days: Int = 30,
    ): HashtagSuggestionsResponseDto

    @GET("v1/hashtags/search")
    suspend fun searchHashtags(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
    ): HashtagSuggestionsResponseDto

    @retrofit2.http.POST("v1/posts/like/{postId}")
    suspend fun toggleLike(
        @Path("postId") postId: String,
        @retrofit2.http.Body body: LikeRequestDto,
    ): LikeResponseDto

    @retrofit2.http.POST("v1/posts/save/{postId}")
    suspend fun toggleSave(
        @Path("postId") postId: String,
    ): SaveResponseDto

    @retrofit2.http.PATCH("v1/posts/{postId}")
    suspend fun updatePost(
        @Path("postId") postId: String,
        @retrofit2.http.Body body: UpdatePostRequestDto,
    ): FeedPostDto

    @retrofit2.http.DELETE("v1/posts/{postId}")
    suspend fun deletePost(@Path("postId") postId: String)

    @retrofit2.http.POST("v1/posts/report")
    suspend fun reportPost(@retrofit2.http.Body body: ReportPostRequestDto)

    @retrofit2.http.POST("v1/posts/appeal")
    suspend fun submitAppeal(@retrofit2.http.Body body: SubmitAppealRequestDto)

    @retrofit2.http.POST("v1/feed/event")
    suspend fun trackFeedEvent(@retrofit2.http.Body body: FeedEventRequestDto)

    @retrofit2.http.POST("v1/uploads/presign")
    suspend fun presignUpload(@retrofit2.http.Body request: PostPresignRequestDto): PostPresignResponseDto

    @retrofit2.http.POST("v1/posts")
    suspend fun createPost(@retrofit2.http.Body request: CreatePostRequestDto): CreatePostResponseDto
}
