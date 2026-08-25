package ir.coffevista.vista_native.features.feed.data

data class Comment(
    val id: String,
    val postId: String,
    val content: String,
    val createdAt: String,
    val authorUserId: String,
    val authorUsername: String?,
    val authorFullName: String,
    val authorAvatarUrl: String?,
    val authorIsVerified: Boolean,
    val authorVerificationType: String?,
    val authorRole: String? = null,
    val postOwnerId: String = "",
    val parentCommentId: String?,
    val replies: List<Comment> = emptyList(),
)

fun CommentResponseDto.asExternalModel(replies: List<Comment> = emptyList()) = Comment(
    id = id,
    postId = postId,
    content = content,
    createdAt = createdAt,
    authorUserId = userId,
    authorUsername = profiles.username,
    authorFullName = profiles.fullName ?: profiles.username ?: "کاربر",
    authorAvatarUrl = profiles.avatarUrl,
    authorIsVerified = profiles.isVerified,
    authorVerificationType = profiles.verificationType,
    authorRole = profiles.role,
    postOwnerId = postOwnerId ?: ownerId.orEmpty(),
    parentCommentId = parentCommentId,
    replies = replies,
)
