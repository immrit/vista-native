package ir.coffevista.vista_native.features.chat.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * تحلیل‌گر هوشمند برای خواندن SharedPostDraft از ساختارهای مختلف JSON و داده‌های ارسالی چت.
 */
object SharedPostParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun tryParse(raw: String?): SharedPostDraft? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

        return runCatching {
            json.decodeFromString(SharedPostDraft.serializer(), trimmed)
        }.recoverCatching {
            val root = Json.parseToJsonElement(trimmed).jsonObject
            val target = if (root.containsKey("shared_post_data") && root["shared_post_data"] is kotlinx.serialization.json.JsonObject) {
                root["shared_post_data"]!!.jsonObject
            } else if (root.containsKey("sharedPostData") && root["sharedPostData"] is kotlinx.serialization.json.JsonObject) {
                root["sharedPostData"]!!.jsonObject
            } else {
                root
            }

            val postId = target["postId"]?.jsonPrimitive?.contentOrNull
                ?: target["post_id"]?.jsonPrimitive?.contentOrNull
                ?: target["id"]?.jsonPrimitive?.contentOrNull
                ?: return@recoverCatching null

            val authorName = target["authorName"]?.jsonPrimitive?.contentOrNull
                ?: target["post_author_name"]?.jsonPrimitive?.contentOrNull
                ?: target["postAuthorName"]?.jsonPrimitive?.contentOrNull
                ?: "کاربر"

            val authorUsername = target["authorUsername"]?.jsonPrimitive?.contentOrNull
                ?: target["post_author_username"]?.jsonPrimitive?.contentOrNull
                ?: target["postAuthorUsername"]?.jsonPrimitive?.contentOrNull
                ?: ""

            val authorAvatar = target["authorAvatar"]?.jsonPrimitive?.contentOrNull
                ?: target["post_author_avatar"]?.jsonPrimitive?.contentOrNull
                ?: target["postAuthorAvatar"]?.jsonPrimitive?.contentOrNull

            val content = target["content"]?.jsonPrimitive?.contentOrNull
                ?: target["post_content"]?.jsonPrimitive?.contentOrNull
                ?: target["postContent"]?.jsonPrimitive?.contentOrNull
                ?: ""

            val mediaUrls = mutableListOf<String>()
            val rawMedia = target["mediaUrls"] ?: target["media_urls"]
            if (rawMedia is JsonArray) {
                mediaUrls.addAll(rawMedia.mapNotNull { it.jsonPrimitive.contentOrNull })
            }
            val singleImg = target["post_image_url"]?.jsonPrimitive?.contentOrNull
                ?: target["postImageUrl"]?.jsonPrimitive?.contentOrNull
                ?: target["imageUrl"]?.jsonPrimitive?.contentOrNull
                ?: target["thumbnailUrl"]?.jsonPrimitive?.contentOrNull
            if (singleImg != null && singleImg !in mediaUrls) {
                mediaUrls.add(0, singleImg)
            }

            val likes = target["likesCount"]?.jsonPrimitive?.longOrNull
                ?: target["like_count"]?.jsonPrimitive?.longOrNull
                ?: target["likeCount"]?.jsonPrimitive?.longOrNull
                ?: 0L

            val comments = target["commentsCount"]?.jsonPrimitive?.longOrNull
                ?: target["comment_count"]?.jsonPrimitive?.longOrNull
                ?: target["commentCount"]?.jsonPrimitive?.longOrNull
                ?: 0L

            val createdAt = target["createdAt"]?.jsonPrimitive?.contentOrNull
                ?: target["post_created_at"]?.jsonPrimitive?.contentOrNull
                ?: target["postCreatedAt"]?.jsonPrimitive?.contentOrNull
                ?: ""

            val isVerified = target["isVerified"]?.jsonPrimitive?.booleanOrNull
                ?: target["is_verified"]?.jsonPrimitive?.booleanOrNull
                ?: false

            val verificationType = target["verificationType"]?.jsonPrimitive?.contentOrNull
                ?: target["verification_type"]?.jsonPrimitive?.contentOrNull
                ?: "none"

            val authorId = target["authorId"]?.jsonPrimitive?.contentOrNull
                ?: target["author_id"]?.jsonPrimitive?.contentOrNull
                ?: target["userId"]?.jsonPrimitive?.contentOrNull
                ?: target["user_id"]?.jsonPrimitive?.contentOrNull

            val hasStory = target["hasStory"]?.jsonPrimitive?.booleanOrNull
                ?: target["has_story"]?.jsonPrimitive?.booleanOrNull
                ?: target["hasActiveStory"]?.jsonPrimitive?.booleanOrNull
                ?: target["has_active_story"]?.jsonPrimitive?.booleanOrNull
                ?: false

            SharedPostDraft(
                postId = postId,
                authorName = authorName,
                authorUsername = authorUsername,
                authorAvatar = authorAvatar,
                content = content,
                mediaUrls = mediaUrls,
                postVideoUrl = target["postVideoUrl"]?.jsonPrimitive?.contentOrNull ?: target["post_video_url"]?.jsonPrimitive?.contentOrNull,
                likesCount = likes,
                commentsCount = comments,
                createdAt = createdAt,
                isVerified = isVerified,
                verificationType = verificationType,
                role = target["role"]?.jsonPrimitive?.contentOrNull,
                authorId = authorId,
                hasStory = hasStory,
            )
        }.getOrNull()
    }
}
