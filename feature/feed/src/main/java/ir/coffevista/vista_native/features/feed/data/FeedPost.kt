package ir.coffevista.vista_native.features.feed.data

import ir.coffevista.vista_native.core.database.feed.FeedPostEntity

data class FeedPost(
    val id: String,
    val userId: String,
    val content: String?,
    val imageUrl: String?,
    val imageUrls: List<String>,
    val videoUrl: String?,
    val musicUrl: String?,
    val aspectRatio: String?,
    val musicTitle: String?,
    val tags: List<String>,
    val likeCount: Long,
    val commentCount: Long,
    val isLiked: Boolean,
    val isSaved: Boolean,
    val hideLikeCount: Boolean,
    val hideCommentCount: Boolean,
    val authorUsername: String?,
    val authorFullName: String,
    val authorAvatarUrl: String?,
    val authorIsVerified: Boolean,
    val authorVerificationType: String?,
    val createdAt: String,
) {
    val primaryImageUrl: String?
        get() = imageUrls.firstOrNull() ?: imageUrl

    val videoThumbnailUrl: String?
        get() = if (videoUrl != null) primaryImageUrl else null
}

fun FeedPostEntity.asExternalModel() = FeedPost(
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
    authorUsername = authorUsername,
    authorFullName = authorFullName,
    authorAvatarUrl = authorAvatarUrl,
    authorIsVerified = authorIsVerified,
    authorVerificationType = authorVerificationType,
    createdAt = createdAt,
)
