package ir.coffevista.vista_native.notifications

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Formats incoming FCM chat notification payloads into safe, human-readable Persian
 * copy for every supported message type while preventing raw JSON, voice waveforms,
 * or E2EE envelopes from surfacing in the notification tray.
 */
object ChatNotificationContentFormatter {
    private val lenientJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val PREFORMATTED_PREFIXES = listOf(
        "📷 تصویر",
        "🎤 پیام صوتی",
        "🎵 فایل صوتی",
        "🎥 ویدیو",
        "🎬 ویدیو",
        "🎞️ گیف",
        "✨ استیکر",
        "✨ ایموجی",
        "📁 فایل",
        "📎 فایل",
        "📍 موقعیت مکانی",
        "👤 مخاطب",
        "📊 نظرسنجی",
        "🎮 دعوت به دوئل",
        "📮 پست",
        "📖 پاسخ به استوری",
        "📖 استوری",
        "🔒 پیام محرمانه",
        "🔒 پیام رمزگذاری‌شده",
        "↪️ ",
    )

    fun format(
        rawBody: String?,
        messageType: String? = null,
        fileName: String? = null,
        isSecret: Boolean = false,
    ): String {
        if (isSecret) return "🔒 پیام محرمانه"

        val trimmed = rawBody?.trim().orEmpty()
        if (trimmed == "پیام جدید") return "پیام جدید"
        if (PREFORMATTED_PREFIXES.any { trimmed.startsWith(it) }) {
            return clampText(collapseWhitespace(trimmed), 180)
        }
        if (isOpaqueCiphertext(trimmed)) {
            return "🔒 پیام محرمانه"
        }

        val normalizedType = messageType?.trim()?.lowercase().orEmpty()
        val safeFile = fileName?.trim()?.takeIf { it.isNotEmpty() }

        return when (normalizedType) {
            "image", "photo" -> appendDetail("📷 تصویر", sanitizeMediaCaption(trimmed))
            "voice" -> appendDetail("🎤 پیام صوتی", sanitizeMediaCaption(trimmed))
            "audio", "music" -> appendDetail("🎵 فایل صوتی", safeFile ?: sanitizeMediaCaption(trimmed))
            "video" -> appendDetail("🎥 ویدیو", sanitizeMediaCaption(trimmed))
            "gif" -> appendDetail("🎞️ گیف", sanitizeMediaCaption(trimmed))
            "sticker" -> "✨ استیکر"
            "emoji", "animated_emoji" -> {
                val clean = collapseWhitespace(trimmed)
                if (clean.isNotEmpty() && !looksLikeJson(clean) && clean.length <= 16) clean else "✨ ایموجی"
            }
            "file", "document", "pdf", "archive", "attachment", "unknown" ->
                appendDetail("📁 فایل", safeFile ?: sanitizeMediaCaption(trimmed))
            "location" -> parseStructuredJson(trimmed, "location") ?: "📍 موقعیت مکانی"
            "contact" -> parseStructuredJson(trimmed, "contact") ?: "👤 مخاطب"
            "poll" -> parseStructuredJson(trimmed, "poll") ?: "📊 نظرسنجی"
            "gameinvite", "game_invite", "duel", "duel_invite" ->
                parseStructuredJson(trimmed, "gameinvite") ?: "🎮 دعوت به دوئل ویستا کوییز"
            "sharedpost", "shared_post", "post", "share_post", "feed_post", "repost" ->
                parseStructuredJson(trimmed, "sharedpost")
                    ?: appendDetail("📮 پست اشتراک‌گذاری شده", sanitizeMediaCaption(trimmed))
            "storyreply", "story_reply" ->
                parseStructuredJson(trimmed, "storyreply")
                    ?: appendDetail("📖 پاسخ به استوری", sanitizeMediaCaption(trimmed))
            "storyshare", "story_share", "story" ->
                parseStructuredJson(trimmed, "storyshare") ?: "📖 استوری اشتراک‌گذاری شده"
            else -> when {
                trimmed.startsWith("wf:") -> "🎤 پیام صوتی"
                looksLikeJson(trimmed) -> parseStructuredJson(trimmed, normalizedType) ?: "پیام جدید"
                looksLikeDuelUrl(trimmed) -> "🎮 دعوت به دوئل ویستا کوییز"
                else -> collapseWhitespace(trimmed).ifEmpty { "پیام جدید" }.let { clampText(it, 180) }
            }
        }
    }

    fun sanitizeSocialBody(rawBody: String?): String {
        val trimmed = rawBody?.trim().orEmpty()
        if (trimmed.isEmpty()) return "اعلان جدید"
        if (looksLikeJson(trimmed)) {
            return parseStructuredJson(trimmed, "") ?: "اعلان جدید"
        }
        return clampText(collapseWhitespace(trimmed), 180)
    }

    private fun parseStructuredJson(raw: String, hintType: String): String? {
        if (!looksLikeJson(raw)) return null
        val root = runCatching { lenientJson.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        if (root.isEmpty()) return null

        val target = unwrapNested(
            root,
            "shared_post_data", "sharedPostData",
            "gameInviteData", "game_invite_data",
            "story_reply_data", "storyReplyData",
            "post", "story", "data",
        )
        val declaredType = target.str("type", "messageType", "message_type")?.lowercase()
            ?: root.str("type", "messageType", "message_type")?.lowercase()
        val resolvedType = declaredType ?: hintType

        if (resolvedType in setOf("sharedpost", "shared_post", "post", "share_post") ||
            target.hasAny("postId", "post_id")
        ) {
            val author = target.str(
                "authorName", "author_name", "post_author_name",
                "postAuthorName", "authorUsername", "author_username",
            )
            val snippet = sanitizeMediaCaption(
                target.str("content", "post_content", "postContent", "caption", "text").orEmpty(),
            )
            return if (!author.isNullOrBlank()) {
                appendDetail("📮 پست از ${clampText(author, 32)}", snippet)
            } else {
                appendDetail("📮 پست اشتراک‌گذاری شده", snippet)
            }
        }

        if (resolvedType in setOf("gameinvite", "game_invite", "duel", "duel_invite") ||
            target.hasAny("matchId", "match_id", "duelUrl", "duel_url")
        ) {
            val inviter = target.str("inviterName", "inviter_name", "senderName", "sender_name")
            return if (!inviter.isNullOrBlank()) {
                "🎮 دعوت به دوئل ویستا کوییز از طرف ${clampText(inviter, 32)}"
            } else {
                "🎮 دعوت به دوئل ویستا کوییز"
            }
        }

        if (resolvedType in setOf("storyreply", "story_reply", "storyshare", "story_share") ||
            target.hasAny("storyId", "story_id")
        ) {
            val reply = sanitizeMediaCaption(
                target.str("replyText", "reply_text", "reply", "text", "message", "content").orEmpty(),
            )
            return if (resolvedType == "storyshare" || resolvedType == "story_share") {
                appendDetail("📖 استوری اشتراک‌گذاری شده", reply)
            } else {
                appendDetail("📖 پاسخ به استوری", reply)
            }
        }

        if (resolvedType == "location" ||
            (target.hasAny("lat", "latitude") && target.hasAny("lng", "lon", "longitude"))
        ) {
            val place = target.str("title", "name", "address", "placeName", "place_name", "label")
            return appendDetail("📍 موقعیت مکانی", place)
        }

        if (resolvedType == "contact" || target.hasAny("phone", "phoneNumber", "phone_number", "vcard")) {
            val name = target.str("name", "fullName", "full_name", "displayName", "display_name")
            return appendDetail("👤 مخاطب", name)
        }

        if (resolvedType == "poll" || target.hasAny("question")) {
            val question = target.str("question", "title")
            return appendDetail("📊 نظرسنجی", question)
        }

        val fallbackText = sanitizeMediaCaption(
            target.str("text", "caption", "message", "body", "content", "title").orEmpty(),
        )
        return fallbackText.takeIf { it.isNotEmpty() }?.let { clampText(it, 120) } ?: "پیام جدید"
    }

    private fun unwrapNested(root: JsonObject, vararg keys: String): JsonObject {
        for (key in keys) {
            val child = root[key] as? JsonObject
            if (child != null && child.isNotEmpty()) return child
        }
        return root
    }

    private fun JsonObject.str(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        (this[key] as? JsonPrimitive)?.contentOrNull?.let(::collapseWhitespace)?.takeIf { it.isNotEmpty() }
    }

    private fun JsonObject.hasAny(vararg keys: String): Boolean = keys.any { key ->
        val elem = this[key] ?: return@any false
        if (elem is JsonPrimitive) !elem.contentOrNull.isNullOrBlank() else true
    }

    private fun appendDetail(base: String, detail: String?): String {
        val clean = detail?.let(::collapseWhitespace)?.takeIf { it.isNotEmpty() } ?: return base
        return "$base: ${clampText(clean, 90)}"
    }

    private fun sanitizeMediaCaption(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("wf:") || isOpaqueCiphertext(trimmed) || looksLikeJson(trimmed)) {
            return ""
        }
        val lower = trimmed.lowercase()
        if ((lower.startsWith("http://") || lower.startsWith("https://")) && !trimmed.any(Char::isWhitespace)) {
            return ""
        }
        return collapseWhitespace(trimmed)
    }

    private fun looksLikeJson(raw: String): Boolean {
        val t = raw.trim()
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))
    }

    private fun isOpaqueCiphertext(raw: String): Boolean =
        raw.startsWith("VE2E1:") || raw.startsWith("VE2E2:") || raw.startsWith("e2ee:v1:")

    private fun looksLikeDuelUrl(raw: String): Boolean {
        val lower = raw.trim().lowercase()
        if (lower.isEmpty() || lower.any(Char::isWhitespace)) return false
        return (lower.contains("coffevista.ir") || lower.contains("cafevista.ir") || lower.startsWith("vista://")) &&
            lower.contains("/game/duel")
    }

    private fun collapseWhitespace(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ")

    private fun clampText(text: String, maxChars: Int): String =
        if (text.length <= maxChars) text else text.take(maxChars - 1).trimEnd() + "…"
}
