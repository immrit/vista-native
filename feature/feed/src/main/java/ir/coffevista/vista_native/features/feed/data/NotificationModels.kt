package ir.coffevista.vista_native.features.feed.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class NotificationFilter(val type: String) {
    All("all"),
    FollowRequest("follow_request"),
    Follow("follow"),
    Like("like"),
    Comment("comment"),
    CommentReply("comment_reply"),
    Mention("mention"),
    Suggestions("daily_suggestion_digest"),
}

enum class FollowRequestActionState {
    Success,
    AlreadyHandled,
    Failed,
}

data class FollowRequestActionResult(
    val state: FollowRequestActionState,
    val message: String,
)

@Serializable
data class NotificationsResponseDto(
    val notifications: List<NotificationDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class NotificationDto(
    val id: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("recipient_id") val recipientId: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val type: String = "",
    val username: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("user_is_verified") val userIsVerified: Boolean = false,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("comment_id") val commentId: String? = null,
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("verification_type") val verificationType: String? = null,
    @SerialName("open_screen") val openScreen: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("follower_id") val followerId: String? = null,
    val deeplink: String? = null,
    val metadata: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class FollowRequestRespondDto(
    @SerialName("requester_id") val requesterId: String,
    val accept: Boolean,
)

@Serializable
data class FollowRequestRespondResponseDto(
    val message: String? = null,
)

data class VistaNotification(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val content: String,
    val createdAt: String,
    val type: String,
    val username: String,
    val fullName: String,
    val userIsVerified: Boolean,
    val avatarUrl: String?,
    val postId: String?,
    val commentId: String?,
    val parentCommentId: String?,
    val isRead: Boolean,
    val verificationType: String?,
    val openScreen: String?,
    val conversationId: String?,
    val followerId: String?,
    val deeplink: String?,
    val metadata: Map<String, String>,
) {
    val canonicalType: String get() = canonicalNotificationType(type)
}

fun NotificationDto.toDomain(): VistaNotification = VistaNotification(
    id = id,
    senderId = senderId,
    recipientId = recipientId,
    content = normalizeNotificationContent(content, type),
    createdAt = createdAt,
    type = canonicalNotificationType(type),
    username = username.ifBlank { fullName.ifBlank { "کاربر" } },
    fullName = fullName.ifBlank { username.ifBlank { "کاربر" } },
    userIsVerified = userIsVerified || isVerified,
    avatarUrl = avatarUrl,
    postId = postId?.takeIf { it.isNotBlank() },
    commentId = commentId?.takeIf { it.isNotBlank() },
    parentCommentId = parentCommentId?.takeIf { it.isNotBlank() },
    isRead = isRead,
    verificationType = verificationType,
    openScreen = openScreen,
    conversationId = conversationId?.takeIf { it.isNotBlank() },
    followerId = followerId?.takeIf { it.isNotBlank() },
    deeplink = deeplink,
    metadata = metadata.mapValues { (_, value) -> value.asLooseString() },
)

fun canonicalNotificationType(rawType: String?): String {
    return when (rawType?.trim()?.lowercase().orEmpty()) {
        "post_like" -> "like"
        "new_comment", "post_comment" -> "comment"
        "reply_comment" -> "comment_reply"
        "comment_mention", "post_mention", "mention" -> "mention"
        "new_message", "chat_message" -> "message"
        "message_reaction" -> "reaction"
        "suggested_follow" -> "suggest_follow"
        "suggested_post" -> "suggest_post"
        else -> rawType?.trim()?.lowercase().orEmpty()
    }
}

fun notificationTypeMatchesFilter(notificationType: String, filterType: String?): Boolean {
    val filter = canonicalNotificationType(filterType)
    if (filter.isBlank() || filter == NotificationFilter.All.type) return true
    val type = canonicalNotificationType(notificationType)
    return when (filter) {
        NotificationFilter.Follow.type -> type == "follow" ||
            type == "follow_request" ||
            type == "follow_request_accepted"
        NotificationFilter.Comment.type -> type == "comment"
        NotificationFilter.CommentReply.type -> type == "comment_reply"
        NotificationFilter.FollowRequest.type -> type == "follow_request"
        NotificationFilter.Suggestions.type -> type == "daily_suggestion_digest" ||
            type == "suggest_follow" ||
            type == "suggest_post"
        else -> type == filter
    }
}

private fun normalizeNotificationContent(raw: String, type: String): String {
    val trimmed = raw.trim()
    if (trimmed.isNotEmpty()) return trimmed
    return when (canonicalNotificationType(type)) {
        "message" -> "پیام جدید"
        "follow" -> "دنبال‌کننده جدید"
        "follow_request" -> "درخواست دنبال کردن"
        "follow_request_accepted" -> "درخواست دنبال‌کردن پذیرفته شد"
        "like" -> "لایک جدید"
        "comment" -> "نظر جدید"
        "comment_reply" -> "پاسخ جدید"
        "mention" -> "شما را تگ کرد"
        else -> "اعلان جدید"
    }
}

private fun JsonElement.asLooseString(): String {
    val primitive = this as? JsonPrimitive ?: return toString()
    return primitive.contentOrNull
        ?: primitive.booleanOrNull?.toString()
        ?: primitive.toString()
}
