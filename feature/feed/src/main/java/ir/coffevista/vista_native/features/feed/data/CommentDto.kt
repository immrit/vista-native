package ir.coffevista.vista_native.features.feed.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentListResponseDto(
    @SerialName("comments")
    val comments: List<CommentResponseDto> = emptyList(),
    @SerialName("has_more")
    val hasMore: Boolean = false,
)

@Serializable
data class CreateCommentRequestDto(
    @SerialName("post_id")
    val postId: String,
    @SerialName("content")
    val content: String,
    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,
)

@Serializable
data class UpdateCommentRequestDto(
    @SerialName("content")
    val content: String,
)

@Serializable
data class ReportCommentRequestDto(
    @SerialName("reason")
    val reason: String,
    @SerialName("additional_details")
    val additionalDetails: String? = null,
)

@Serializable
data class CommentMentionsRequestDto(
    @SerialName("user_ids")
    val userIds: List<String>,
)

@Serializable
data class CommentResponseDto(
    @SerialName("id")
    val id: String,
    @SerialName("post_id")
    val postId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("content")
    val content: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,
    @SerialName("owner_id")
    val ownerId: String? = null,
    @SerialName("post_owner_id")
    val postOwnerId: String? = null,
    @SerialName("profiles")
    val profiles: CommentAuthorProfileDto = CommentAuthorProfileDto(),
)

@Serializable
data class CommentAuthorProfileDto(
    @SerialName("username")
    val username: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("role")
    val role: String? = null,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    @SerialName("verification_type")
    val verificationType: String? = null,
)
