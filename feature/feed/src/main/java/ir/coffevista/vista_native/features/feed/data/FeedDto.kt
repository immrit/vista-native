package ir.coffevista.vista_native.features.feed.data

import ir.coffevista.vista_native.core.database.feed.FeedPostEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedResponseDto(
    @SerialName("posts")
    val posts: List<FeedPostDto>,
    @SerialName("has_more")
    val hasMore: Boolean,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
)

@Serializable
data class HashtagSuggestionsResponseDto(
    @SerialName("hashtags") val hashtags: List<HashtagSuggestionDto> = emptyList(),
)

@Serializable
data class HashtagSuggestionDto(
    @SerialName("tag") val tag: String = "",
    @SerialName("usage_count") val usageCount: Long = 0,
)

@Serializable
data class FeedPostDto(
    @SerialName("id")
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("content")
    val content: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("video_url")
    val videoUrl: String? = null,
    @SerialName("music_url")
    val musicUrl: String? = null,
    @SerialName("aspect_ratio")
    val aspectRatio: String? = null,
    @SerialName("music_title")
    val musicTitle: String? = null,
    @SerialName("tags")
    val tags: List<String> = emptyList(),
    @SerialName("like_count")
    val likeCount: Long = 0,
    @SerialName("comment_count")
    val commentCount: Long = 0,
    @SerialName("is_liked")
    val isLiked: Boolean = false,
    @SerialName("is_saved")
    val isSaved: Boolean = false,
    @SerialName("feed_source")
    val feedSource: String? = null,
    @SerialName("feed_score")
    val feedScore: Double? = null,
    @SerialName("author_follow_status")
    val authorFollowStatus: String? = null,
    @SerialName("moderator_id")
    val moderatorId: String? = null,
    @SerialName("moderator_username")
    val moderatorUsername: String? = null,
    @SerialName("moderated_at")
    val moderatedAt: String? = null,
    @SerialName("moderation_reason")
    val moderationReason: String? = null,
    @SerialName("edited_by_vista")
    val editedByVista: Boolean = false,
    @SerialName("comments_disabled")
    val commentsDisabled: Boolean = false,
    @SerialName("hide_like_count")
    val hideLikeCount: Boolean = false,
    @SerialName("hide_comment_count")
    val hideCommentCount: Boolean = false,
    @SerialName("author")
    val author: AuthorInfoDto,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class AuthorInfoDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("username")
    val username: String? = null,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    @SerialName("verification_type")
    val verificationType: String? = null,
)

internal fun FeedPostDto.asEntity(
    accountId: String,
    sortOrder: Long,
) = FeedPostEntity(
    accountId = accountId,
    id = id,
    userId = userId,
    content = content,
    imageUrl = imageUrl,
    imageUrls = imageUrls,
    videoUrl = videoUrl,
    musicUrl = musicUrl,
    aspectRatio = aspectRatio,
    musicTitle = musicTitle,
    tags = tags,
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = isLiked,
    isSaved = isSaved,
    hideLikeCount = hideLikeCount,
    hideCommentCount = hideCommentCount,
    authorId = author.userId,
    authorUsername = author.username,
    authorFullName = author.fullName,
    authorAvatarUrl = author.avatarUrl,
    authorIsVerified = author.isVerified,
    authorVerificationType = author.verificationType,
    authorFollowStatus = authorFollowStatus,
    feedSource = feedSource,
    editedByVista = editedByVista,
    moderationReason = moderationReason,
    commentsDisabled = commentsDisabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sortOrder = sortOrder,
)

@Serializable
data class LikeRequestDto(
    @SerialName("owner_id")
    val ownerId: String,
)

@Serializable
data class LikeResponseDto(
    @SerialName("is_liked")
    val isLiked: Boolean,
    @SerialName("like_count")
    val likeCount: Long,
)

@Serializable
data class SaveResponseDto(
    @SerialName("is_saved")
    val isSaved: Boolean,
)

@Serializable
data class UpdatePostRequestDto(
    @SerialName("content") val content: String? = null,
    @SerialName("hide_like_count") val hideLikeCount: Boolean? = null,
    @SerialName("hide_comment_count") val hideCommentCount: Boolean? = null,
)

@Serializable
data class ReportPostRequestDto(
    @SerialName("post_id") val postId: String,
    @SerialName("reported_user_id") val reportedUserId: String,
    @SerialName("reason") val reason: String,
    @SerialName("additional_details") val additionalDetails: String? = null,
)

@Serializable
data class FeedEventRequestDto(
    @SerialName("post_id") val postId: String,
    @SerialName("event_type") val eventType: String,
)

@Serializable
data class PostPresignRequestDto(
    @SerialName("object_key") val objectKey: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("file_size") val fileSize: Long,
)

@Serializable
data class PostPresignResponseDto(
    val url: String,
    val method: String = "PUT",
    val headers: Map<String, String> = emptyMap(),
    @SerialName("object_key") val objectKey: String,
    @SerialName("object_url") val objectUrl: String,
)

@Serializable
data class CreatePostRequestDto(
    @SerialName("content") val content: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("music_url") val musicUrl: String? = null,
    @SerialName("music_title") val musicTitle: String? = null,
    @SerialName("music_start_ms") val musicStartMs: Int? = null,
    @SerialName("music_end_ms") val musicEndMs: Int? = null,
    @SerialName("aspect_ratio") val aspectRatio: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("hide_like_count") val hideLikeCount: Boolean = false,
    @SerialName("hide_comment_count") val hideCommentCount: Boolean = false,
    @SerialName("comments_disabled") val commentsDisabled: Boolean = false,
)

@Serializable
data class CreatePostResponseDto(
    @SerialName("post") val post: FeedPostDto? = null,
    @SerialName("success") val success: Boolean = true,
    @SerialName("message") val message: String? = null,
)
@Serializable
data class SubmitAppealRequestDto(
    @SerialName("post_id") val postId: String,
    val reason: String,
)
