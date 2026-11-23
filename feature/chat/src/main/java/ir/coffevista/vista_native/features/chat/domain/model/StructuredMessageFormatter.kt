package ir.coffevista.vista_native.features.chat.domain.model

import androidx.compose.runtime.Immutable
import ir.coffevista.vista_native.core.common.looksLikeJsonPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

@Immutable
data class StoryReplyPayload(
    val storyId: String = "",
    val storyOwnerId: String? = null,
    val ownerName: String? = null,
    val replyText: String = "",
    val mediaUrl: String? = null,
    val isShare: Boolean = false,
)

/**
 * Parses story reply / story share JSON payloads (including backend payloads
 * that only carry `story_id` and `story_owner_id`, or truncated JSON previews).
 */
object StoryReplyParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val STORY_TYPES = setOf(
        "storyreply",
        "story_reply",
        "storyshare",
        "story_share",
        "story",
    )

    private val TRUNCATED_STORY_KEY_REGEX = Regex(
        """"(?:story_id|storyId|story_owner_id|storyOwnerId|story_reply_data|storyReplyData|story_share_data|storyShareData)"\s*:""",
    )

    fun tryParse(raw: String?, transportType: String? = null): StoryReplyPayload? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) return null

        val normalizedType = transportType?.trim()?.lowercase().orEmpty()
        val isStoryTransport = normalizedType in STORY_TYPES

        val root = runCatching { json.parseToJsonElement(trimmed).jsonObject }.getOrNull()
        if (root != null) {
            val target = unwrapNested(
                root,
                "story_reply_data",
                "storyReplyData",
                "story_share_data",
                "storyShareData",
                "story",
                "data",
            )
            val declaredType = (target.str("type", "messageType", "message_type")
                ?: root.str("type", "messageType", "message_type"))
                ?.lowercase()
            val isExplicitStoryType = declaredType in STORY_TYPES || isStoryTransport
            val hasStoryIdentity = target.hasAny("storyId", "story_id", "storyOwnerId", "story_owner_id") ||
                root.hasAny("storyId", "story_id", "storyOwnerId", "story_owner_id")

            if (!isExplicitStoryType && !hasStoryIdentity) return null

            val isShare = declaredType in setOf("storyshare", "story_share") ||
                normalizedType in setOf("storyshare", "story_share", "story")
            val replyText = extractSafeText(
                target.str("replyText", "reply_text", "reply", "text", "message", "content", "caption", "story_caption", "storyCaption")
                    ?: root.str("replyText", "reply_text", "reply", "text", "message", "content", "caption", "story_caption", "storyCaption"),
            )
            val ownerName = extractSafeText(
                target.str("ownerName", "owner_name", "storyOwnerUsername", "story_owner_username", "authorName", "author_name", "username")
                    ?: root.str("ownerName", "owner_name", "storyOwnerUsername", "story_owner_username", "authorName", "author_name", "username"),
            ).ifBlank { null }

            return StoryReplyPayload(
                storyId = target.str("storyId", "story_id") ?: root.str("storyId", "story_id").orEmpty(),
                storyOwnerId = target.str("storyOwnerId", "story_owner_id", "ownerId", "owner_id")
                    ?: root.str("storyOwnerId", "story_owner_id", "ownerId", "owner_id"),
                ownerName = ownerName,
                replyText = replyText,
                mediaUrl = target.str("storyMediaUrl", "story_media_url", "mediaUrl", "media_url", "imageUrl", "image_url", "thumbnailUrl", "thumbnail_url"),
                isShare = isShare,
            )
        }

        // Truncated JSON object (e.g., cut at 500 chars by reply preview limit or server)
        if (isStoryTransport || TRUNCATED_STORY_KEY_REGEX.containsMatchIn(trimmed)) {
            val isShare = normalizedType in setOf("storyshare", "story_share", "story")
            val storyId = extractPartialJsonField(trimmed, "story_id", "storyId").orEmpty()
            val ownerId = extractPartialJsonField(trimmed, "story_owner_id", "storyOwnerId", "owner_id", "ownerId")
            val ownerName = extractSafeText(
                extractPartialJsonField(trimmed, "owner_name", "ownerName", "story_owner_username", "storyOwnerUsername", "author_name", "authorName"),
            ).ifBlank { null }
            val replyText = extractSafeText(
                extractPartialJsonField(trimmed, "reply_text", "replyText", "reply", "caption", "story_caption", "storyCaption", "text", "message", "content"),
            )
            return StoryReplyPayload(
                storyId = storyId,
                storyOwnerId = ownerId,
                ownerName = ownerName,
                replyText = replyText,
                isShare = isShare,
            )
        }

        return null
    }

    fun formatSummary(payload: StoryReplyPayload, includePrefix: Boolean = true): String {
        val cleanReply = extractSafeText(payload.replyText)
        if (!includePrefix) {
            return cleanReply.ifBlank {
                if (payload.isShare) "استوری اشتراک‌گذاری‌شده" else "استوری"
            }
        }
        val base = if (payload.isShare) "📖 استوری اشتراک‌گذاری شده" else "📖 پاسخ به استوری"
        return when {
            cleanReply.isNotEmpty() -> "$base: $cleanReply"
            !payload.ownerName.isNullOrBlank() -> "$base (${payload.ownerName})"
            else -> base
        }
    }

    private fun extractSafeText(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() ||
            trimmed.startsWith("wf:") ||
            looksLikeJsonPayload(trimmed) ||
            trimmed.startsWith("VE2E1:") ||
            trimmed.startsWith("VE2E2:") ||
            trimmed.startsWith("VE2E3:") ||
            trimmed.startsWith("e2ee:")
        ) {
            return ""
        }
        val lower = trimmed.lowercase()
        if ((lower.startsWith("http://") || lower.startsWith("https://")) && !trimmed.any(Char::isWhitespace)) {
            return ""
        }
        return trimmed.replace(Regex("\\s+"), " ")
    }
}

/**
 * Single source of truth for converting any chat message, reply quote, pinned
 * preview, inbox summary, or raw structured payload into safe human-readable Persian
 * text while guaranteeing raw JSON never leaks to the user.
 */
object StructuredMessageFormatter {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val STRUCTURED_TYPES = setOf(
        "sharedpost", "shared_post", "post", "share_post", "feed_post", "repost",
        "storyreply", "story_reply",
        "storyshare", "story_share", "story",
        "gameinvite", "game_invite", "duel", "duel_invite",
        "session_alert", "sessionalert",
        "location", "contact", "poll", "sticker",
    )

    fun isStructuredMessageType(messageType: String?): Boolean =
        messageType?.trim()?.lowercase() in STRUCTURED_TYPES

    fun isStructuredOrJsonPayload(raw: String?, transportType: String? = null): Boolean {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return false
        if (trimmed.startsWith("wf:")) return true
        if (DuelInviteParser.tryParse(trimmed, transportType) != null) return true
        if (SessionAlertParser.tryParse(trimmed, transportType) != null) return true
        if (SharedPostParser.tryParse(trimmed) != null) return true
        if (StoryReplyParser.tryParse(trimmed, transportType) != null) return true
        return looksLikeJsonPayload(trimmed)
    }

    fun formatStructuredOrJsonPreview(
        raw: String?,
        transportType: String? = null,
        sharedPostWithColon: Boolean = false,
        isReplyQuote: Boolean = false,
        isStoryReplyContext: Boolean = false,
    ): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return if (isReplyQuote && isStoryReplyContext) "استوری"
            else chatMessageTypePreview(transportType, secret = false)
        }
        if (trimmed.startsWith("wf:")) return "🎤 پیام صوتی"

        DuelInviteParser.formatPreviewIfDuel(trimmed, transportType)?.let { return it }
        SessionAlertParser.formatPreview(trimmed, transportType)?.let { return it }
        SharedPostParser.tryParse(trimmed)?.let { post ->
            return if (sharedPostWithColon) {
                "پست از ${post.authorName}: ${post.content.ifBlank { "رسانه" }}"
            } else {
                "📮 پست از ${post.authorName}"
            }
        }
        StoryReplyParser.tryParse(trimmed, transportType)?.let { story ->
            return StoryReplyParser.formatSummary(
                story,
                includePrefix = !isReplyQuote && !isStoryReplyContext,
            )
        }

        if (!looksLikeJsonPayload(trimmed)) {
            return null
        }

        val root = runCatching { json.parseToJsonElement(trimmed).jsonObject }.getOrNull()
        if (root != null) {
            val target = unwrapNested(root, "data", "payload", "metadata")
            val declaredType = (target.str("type", "messageType", "message_type")
                ?: root.str("type", "messageType", "message_type"))
                ?.lowercase()
            val resolvedType = declaredType ?: transportType?.trim()?.lowercase().orEmpty()

            if (resolvedType == "location" ||
                (target.hasAny("lat", "latitude") && target.hasAny("lng", "lon", "longitude"))
            ) {
                val place = target.str("title", "name", "address", "placeName", "place_name", "label")
                return if (!place.isNullOrBlank()) "📍 موقعیت مکانی: $place" else "📍 موقعیت مکانی"
            }

            if (resolvedType == "contact" || target.hasAny("phone", "phoneNumber", "phone_number", "vcard")) {
                val name = target.str("name", "fullName", "full_name", "displayName", "display_name")
                return if (!name.isNullOrBlank()) "👤 مخاطب: $name" else "👤 مخاطب"
            }

            if (resolvedType == "poll" || target.hasAny("question")) {
                val question = target.str("question", "title")
                return if (!question.isNullOrBlank()) "📊 نظرسنجی: $question" else "📊 نظرسنجی"
            }

            val typeLabel = chatMessageTypePreview(resolvedType, secret = false)
            if (isReplyQuote && isStoryReplyContext) {
                val replyText = extractSafeJsonCaption(target, root)
                return replyText ?: "استوری"
            }
            if (typeLabel != null) {
                val caption = extractSafeJsonCaption(target, root)
                return if (caption != null) "$typeLabel: $caption" else typeLabel
            }

            extractSafeJsonCaption(target, root)?.let { return it }
        }

        if (isReplyQuote && isStoryReplyContext) {
            return extractPartialJsonField(
                trimmed,
                "reply_text",
                "replyText",
                "reply",
                "caption",
                "text",
                "message",
                "content",
            )?.takeUnless { looksLikeJsonPayload(it) || it.startsWith("wf:") } ?: "استوری"
        }

        return chatMessageTypePreview(transportType, secret = false)
            ?: if (isReplyQuote) "پیام" else "پیام جدید"
    }

    fun formatMessagePreview(message: Message, sharedPostWithColon: Boolean = true): String =
        when (val value = message.content) {
            MessageContent.Deleted -> "این پیام حذف شده است"
            MessageContent.EncryptedUnavailable -> "🔒 پیام رمزگذاری‌شده"
            is MessageContent.Text -> {
                formatStructuredOrJsonPreview(
                    raw = value.value,
                    transportType = message.transportType,
                    sharedPostWithColon = sharedPostWithColon,
                ) ?: when {
                    message.attachment?.kind == AttachmentKind.VOICE -> "🎤 پیام صوتی"
                    value.value.isNotBlank() -> value.value
                    else -> chatMessageTypePreview(message.attachment?.kind?.name, secret = false)
                        ?: message.attachment?.fileName
                        ?: "پیوست"
                }
            }
            is MessageContent.Structured -> {
                formatStructuredOrJsonPreview(
                    raw = value.payload,
                    transportType = message.transportType ?: value.kind,
                    sharedPostWithColon = sharedPostWithColon,
                ) ?: chatMessageTypePreview(value.kind, secret = false)
                    ?: chatMessageTypePreview(message.transportType, secret = false)
                    ?: "پیام جدید"
            }
        }

    fun formatBubbleText(message: Message): String =
        when (val content = message.content) {
            MessageContent.Deleted -> "این پیام حذف شده است"
            MessageContent.EncryptedUnavailable -> "🔒 پیام رمزگذاری‌شده"
            is MessageContent.Text -> {
                if (content.value.trimStart().startsWith("wf:")) {
                    ""
                } else if (looksLikeJsonPayload(content.value)) {
                    formatStructuredOrJsonPreview(
                        raw = content.value,
                        transportType = message.transportType,
                        sharedPostWithColon = true,
                    ) ?: "پیام جدید"
                } else {
                    content.value
                }
            }
            is MessageContent.Structured -> {
                if (content.payload.trimStart().startsWith("wf:")) {
                    ""
                } else {
                    formatStructuredOrJsonPreview(
                        raw = content.payload,
                        transportType = message.transportType ?: content.kind,
                        sharedPostWithColon = true,
                    ) ?: chatMessageTypePreview(content.kind, secret = false)
                        ?: if (looksLikeJsonPayload(content.payload)) "پیام جدید" else content.payload
                }
            }
        }

    fun formatReplyQuotePreview(
        rawReplyContent: String?,
        replyToKind: String? = null,
        isStoryReply: Boolean = false,
        isNoteReply: Boolean = false,
    ): String {
        val raw = rawReplyContent?.trim().orEmpty()
        if (raw.isEmpty()) {
            return when {
                isStoryReply -> "استوری"
                isNoteReply -> "یادداشت"
                else -> chatMessageTypePreview(replyToKind, secret = false) ?: ""
            }
        }
        return formatStructuredOrJsonPreview(
            raw = raw,
            transportType = replyToKind,
            sharedPostWithColon = true,
            isReplyQuote = true,
            isStoryReplyContext = isStoryReply,
        ) ?: raw
    }

    fun formatConversationPreview(
        raw: String?,
        messageType: String? = null,
        isSecret: Boolean = false,
    ): String? {
        val trimmed = raw?.trim()?.takeIf(String::isNotEmpty) ?: return if (isSecret) null else {
            chatMessageTypePreview(messageType, secret = false)
        }
        if (isSecret) {
            return chatMessageTypePreview(messageType, secret = true) ?: "🔒 پیام محرمانه"
        }
        if (trimmed.startsWith("wf:")) return "🎤 پیام صوتی"
        if (trimmed.looksLikeOpaquePayload()) return "پیام جدید"
        formatStructuredOrJsonPreview(
            raw = trimmed,
            transportType = messageType,
            sharedPostWithColon = false,
        )?.let { return it }
        return chatMessageTypePreview(messageType, secret = false) ?: trimmed
    }

    private fun extractSafeJsonCaption(target: JsonObject, root: JsonObject): String? {
        val candidate = (target.str("replyText", "reply_text", "reply", "caption", "summary", "text", "message", "body", "content", "title")
            ?: root.str("replyText", "reply_text", "reply", "caption", "summary", "text", "message", "body", "content", "title"))
            ?.trim()
            .orEmpty()
        if (candidate.isEmpty() ||
            candidate.startsWith("wf:") ||
            looksLikeJsonPayload(candidate) ||
            candidate.looksLikeOpaquePayload()
        ) {
            return null
        }
        val lower = candidate.lowercase()
        if ((lower.startsWith("http://") || lower.startsWith("https://")) && !candidate.any(Char::isWhitespace)) {
            return null
        }
        return candidate
    }
}

private fun String.looksLikeOpaquePayload(): Boolean {
    if (startsWith("VE2E1:") || startsWith("VE2E2:") || startsWith("VE2E3:") || startsWith("e2ee:v1:")) {
        return true
    }
    return length >= 32 && all { char ->
        char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' ||
            char == '+' || char == '/' || char == '_' || char == '-' || char == '='
    }
}

private fun unwrapNested(root: JsonObject, vararg keys: String): JsonObject {
    for (key in keys) {
        val child = root[key] as? JsonObject
        if (child != null && child.isNotEmpty()) return child
    }
    return root
}

private fun JsonObject.str(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
}

private fun JsonObject.hasAny(vararg keys: String): Boolean = keys.any { key ->
    val elem = this[key] ?: return@any false
    if (elem is JsonPrimitive) !elem.contentOrNull.isNullOrBlank() else true
}

private fun extractPartialJsonField(raw: String, vararg keys: String): String? {
    for (key in keys) {
        val regex = Regex(""""${Regex.escape(key)}"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""")
        val match = regex.find(raw) ?: continue
        val value = match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\n", " ")
            .replace("\\\\", "\\")
            .trim()
        if (value.isNotEmpty()) return value
    }
    return null
}
