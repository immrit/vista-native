package ir.coffevista.vista_native.features.profile.settings.saved

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SavedPostsApi {
    @GET("v1/me/saved")
    suspend fun getSavedPosts(
        @Query("limit") limit: Int,
        @Query("offset") offset: String?,
    ): Response<SavedPostsResponseDto>

    @POST("v1/posts/save/{postId}")
    suspend fun toggleSave(@Path("postId") postId: String): Response<SavePostResponseDto>

    @POST("v1/posts/like/{postId}")
    suspend fun toggleLike(
        @Path("postId") postId: String,
        @retrofit2.http.Body body: SavedPostLikeRequestDto = SavedPostLikeRequestDto(),
    ): Response<SavedPostLikeResponseDto>
}

@Serializable
data class SavedPostLikeRequestDto(
    @SerialName("owner_id") val ownerId: String? = null,
)

@Serializable
data class SavedPostLikeResponseDto(
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("like_count") val likeCount: Long = 0,
)

@Serializable
data class SavedPostsResponseDto(
    val posts: List<SavedPostDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class SavedPostDto(
    val id: String,
    @SerialName("content") val caption: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("video_thumbnail_url") val videoThumbnailUrl: String? = null,
    @SerialName("like_count") val likeCount: Long = 0,
    @SerialName("comment_count") val commentCount: Long = 0,
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("author") val author: SavedPostAuthorDto = SavedPostAuthorDto(),
    @SerialName("profiles") val profiles: SavedPostAuthorDto? = null,
) {
    val effectiveAuthor: SavedPostAuthorDto
        get() = if (!author.username.isNullOrBlank()) author else (profiles ?: author)
}

@Serializable
data class SavedPostAuthorDto(
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
)

@Serializable
data class SavePostResponseDto(
    @SerialName("is_saved") val isSaved: Boolean = false,
)
