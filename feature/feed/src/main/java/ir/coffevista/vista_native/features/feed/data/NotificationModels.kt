package ir.coffevista.vista_native.features.feed.data

import ir.coffevista.vista_native.core.common.looksLikeJsonPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
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
data class NotificationSenderDto(
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("user_is_verified") val userIsVerified: Boolean = false,
    @SerialName("verification_type") val verificationType: String? = null,
    val role: String? = null,
)

@Serializable
data class NotificationDto(
    val id: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("recipient_id") val recipientId: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val type: String = "",
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
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
    val metadata: Map<String, JsonElement>? = null,
    val sender: NotificationSenderDto? = null,
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

fun NotificationDto.toDomain(): VistaNotification {
    val resolvedUsername = sender?.username?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }
        ?: sender?.fullName?.takeIf { it.isNotBlank() }
        ?: fullName?.takeIf { it.isNotBlank() }
        ?: "کاربر"

    val resolvedFullName = sender?.fullName?.takeIf { it.isNotBlank() }
        ?: fullName?.takeIf { it.isNotBlank() }
        ?: resolvedUsername

    val resolvedAvatar = sender?.avatarUrl?.takeIf { it.isNotBlank() }
        ?: avatarUrl?.takeIf { it.isNotBlank() }

    val resolvedVerified = (sender?.isVerified == true) || (sender?.userIsVerified == true) || userIsVerified || isVerified
    val resolvedVerificationType = sender?.verificationType?.takeIf { it.isNotBlank() } ?: verificationType

    val canonicalType = canonicalNotificationType(type)
    val resolvedContent = normalizeNotificationContent(content, canonicalType, resolvedUsername)

    val meta = metadata?.mapValues { (_, value) -> value.asLooseString() } ?: emptyMap()
    val resolvedPostId = postId?.takeIf { it.isNotBlank() }
        ?: meta["post_id"]?.takeIf { it.isNotBlank() }
        ?: meta["postId"]?.takeIf { it.isNotBlank() }

    val resolvedCommentId = commentId?.takeIf { it.isNotBlank() }
        ?: meta["comment_id"]?.takeIf { it.isNotBlank() }
        ?: meta["commentId"]?.takeIf { it.isNotBlank() }

    val resolvedConversationId = conversationId?.takeIf { it.isNotBlank() }
        ?: meta["conversation_id"]?.takeIf { it.isNotBlank() }
        ?: meta["conversationId"]?.takeIf { it.isNotBlank() }

    val resolvedFollowerId = followerId?.takeIf { it.isNotBlank() }
        ?: meta["follower_id"]?.takeIf { it.isNotBlank() }
        ?: meta["followerId"]?.takeIf { it.isNotBlank() }

    return VistaNotification(
        id = id,
        senderId = senderId,
        recipientId = recipientId,
        content = resolvedContent,
        createdAt = createdAt,
        type = canonicalType,
        username = resolvedUsername,
        fullName = resolvedFullName,
        userIsVerified = resolvedVerified,
        avatarUrl = resolvedAvatar,
        postId = resolvedPostId,
        commentId = resolvedCommentId,
        parentCommentId = parentCommentId?.takeIf { it.isNotBlank() },
        isRead = isRead,
        verificationType = resolvedVerificationType,
        openScreen = openScreen,
        conversationId = resolvedConversationId,
        followerId = resolvedFollowerId,
        deeplink = deeplink,
        metadata = meta,
    )
}

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
        "post_admin_removed" -> "post_removed"
        "post_admin_edit" -> "post_edited"
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

private val notificationLenientJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

internal fun normalizeNotificationContent(raw: String, type: String, username: String = "کاربر"): String {
    val trimmed = raw.trim()
    val canonicalType = canonicalNotificationType(type)
    if (trimmed.isNotEmpty()) {
        if (looksLikeJsonPayload(trimmed)) {
            extractNotificationTextFromJson(trimmed, canonicalType)?.let { return it }
            return defaultNotificationFallback(canonicalType)
        }
        val cleanName = username.trim()
        val stripped = if (cleanName.isNotBlank() && trimmed.startsWith(cleanName)) {
            trimmed.removePrefix(cleanName).trimStart()
        } else trimmed
        if (stripped.isNotEmpty()) {
            if (looksLikeJsonPayload(stripped)) {
                extractNotificationTextFromJson(stripped, canonicalType)?.let { return it }
                return defaultNotificationFallback(canonicalType)
            }
            if (stripped.contains("mentioned you in a comment", ignoreCase = true)) return "شما را در یک نظر تگ کرد"
            if (stripped.contains("mentioned you in a post", ignoreCase = true)) return "شما را در یک پست تگ کرد"
            if (stripped.contains("liked your post", ignoreCase = true)) return "پست شما را لایک کرد"
            if (stripped.contains("commented on your post", ignoreCase = true)) return "روی پست شما نظر داد"
            if (stripped.contains("replied to your comment", ignoreCase = true)) return "به نظر شما پاسخ داد"
            if (stripped.contains("started following you", ignoreCase = true)) return "شما را دنبال کرد"
            return stripped
        }
    }
    return defaultNotificationFallback(canonicalType)
}

private fun extractNotificationTextFromJson(rawJson: String, canonicalType: String): String? {
    val root = runCatching { notificationLenientJson.parseToJsonElement(rawJson).jsonObject }.getOrNull()
    if (root != null) {
        val target = listOf(
            "shared_post_data", "sharedPostData",
            "story_reply_data", "storyReplyData",
            "gameInviteData", "game_invite_data",
            "post", "story", "data",
        ).firstNotNullOfOrNull { key -> root[key] as? JsonObject } ?: root

        fun str(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            (target[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf {
                it.isNotEmpty() && !looksLikeJsonPayload(it) && !it.startsWith("wf:")
            }
        } ?: keys.firstNotNullOfOrNull { key ->
            (root[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf {
                it.isNotEmpty() && !looksLikeJsonPayload(it) && !it.startsWith("wf:")
            }
        }

        val declaredType = str("type", "messageType", "message_type")?.lowercase() ?: canonicalType
        val text = str("replyText", "reply_text", "reply", "content", "post_content", "postContent", "caption", "text", "message", "body")
        if (declaredType in setOf("story_reply", "storyreply") || target["story_id"] != null || target["storyId"] != null) {
            return if (!text.isNullOrBlank()) "به استوری شما پاسخ داد: $text" else "به استوری شما پاسخ داد"
        }
        if (declaredType in setOf("gameinvite", "game_invite", "duel", "duel_invite") || target["matchId"] != null || target["match_id"] != null) {
            val inviter = str("inviterName", "inviter_name", "senderName", "sender_name")
            return if (!inviter.isNullOrBlank()) "دعوت به دوئل ویستا کوییز از طرف $inviter" else "دعوت به دوئل ویستا کوییز"
        }
        if (!text.isNullOrBlank()) return text
        val author = str("authorName", "post_author_name", "postAuthorName", "author_name")
        if (!author.isNullOrBlank()) return "پست اشتراک‌گذاری‌شده از $author"
        if (target["postId"] != null || target["post_id"] != null || declaredType in setOf("post", "shared_post", "sharedpost")) {
            return "یک پست به اشتراک گذاشته شد"
        }
        return null
    }
    // Truncated JSON fallback
    val partialText = Regex(""""(?:reply_text|replyText|reply|content|post_content|caption|text|message)"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""")
        .find(rawJson)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !looksLikeJsonPayload(it) }
    if (!partialText.isNullOrBlank()) return partialText
    val partialAuthor = Regex(""""(?:authorName|post_author_name)"\s*:\s*"([^"]+)"""")
        .find(rawJson)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
    if (!partialAuthor.isNullOrBlank()) return "پست اشتراک‌گذاری‌شده از $partialAuthor"
    return null
}

private fun defaultNotificationFallback(canonicalType: String): String = when (canonicalType) {
    "message" -> "پیام جدید"
    "follow" -> "شما را دنبال کرد"
    "follow_request" -> "درخواست دنبال‌کردن شما را ارسال کرد"
    "follow_request_accepted" -> "درخواست دنبال‌کردن شما را پذیرفت"
    "like" -> "پست شما را لایک کرد"
    "comment" -> "روی پست شما نظر داد"
    "comment_reply" -> "به نظر شما پاسخ داد"
    "mention" -> "شما را در یک نظر تگ کرد"
    "reaction" -> "به پیام شما واکنش نشان داد"
    "story_reply" -> "به استوری شما پاسخ داد"
    "story_reaction" -> "به استوری شما واکنش نشان داد"
    "story" -> "استوری جدید"
    "suggest_follow" -> "پیشنهاد دنبال کردن"
    "suggest_post" -> "پیشنهاد پست جدید"
    "daily_suggestion_digest" -> "خلاصه پیشنهادهای امروز"
    "post_admin_removed", "post_removed" -> "پست شما به دلیل نقض قوانین حذف شد"
    "post_admin_edit", "post_edited" -> "پست شما توسط مدیر ویرایش شد"
    "moderation" -> "پیام نظارت و بررسی محتوا"
    else -> "اعلان جدید"
}

private fun JsonElement.asLooseString(): String {
    val primitive = this as? JsonPrimitive ?: return toString()
    return primitive.contentOrNull
        ?: primitive.booleanOrNull?.toString()
        ?: primitive.toString()
}
