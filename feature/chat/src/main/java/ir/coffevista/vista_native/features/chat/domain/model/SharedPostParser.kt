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
            val root = json.parseToJsonElement(trimmed).jsonObject
            val target = listOf("shared_post_data", "sharedPostData", "post", "data")
                .firstNotNullOfOrNull { key -> root[key] as? kotlinx.serialization.json.JsonObject }
                ?: root

            val postId = target.string("postId", "post_id", "id") ?: return@runCatching null

            val authorName = target.string("authorName", "post_author_name", "postAuthorName", "author_name") ?: "کاربر"
            val authorUsername = target.string("authorUsername", "post_author_username", "postAuthorUsername", "author_username") ?: ""
            val authorAvatar = target.string("authorAvatar", "post_author_avatar", "postAuthorAvatar", "author_avatar")
            val content = target.string("content", "post_content", "postContent") ?: ""

            val mediaUrls = mutableListOf<String>()
            val rawMedia = target["mediaUrls"] ?: target["media_urls"]
            if (rawMedia is JsonArray) {
                mediaUrls.addAll(rawMedia.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf(String::isNotEmpty) })
            }
            val singleImg = target.string("post_image_url", "postImageUrl", "imageUrl", "image_url", "thumbnailUrl", "thumbnail_url")
            if (singleImg != null && singleImg !in mediaUrls) {
                mediaUrls.add(0, singleImg)
            }

            val hashtags = mutableListOf<String>()
            val rawTags = target["hashtags"] ?: target["tags"]
            if (rawTags is JsonArray) {
                hashtags.addAll(rawTags.mapNotNull { it.jsonPrimitive.contentOrNull?.trim()?.takeIf(String::isNotEmpty) })
            }

            val likes = target["likesCount"]?.jsonPrimitive?.longOrNull
                ?: target["like_count"]?.jsonPrimitive?.longOrNull
                ?: target["likeCount"]?.jsonPrimitive?.longOrNull
                ?: 0L

            val comments = target["commentsCount"]?.jsonPrimitive?.longOrNull
                ?: target["comment_count"]?.jsonPrimitive?.longOrNull
                ?: target["commentCount"]?.jsonPrimitive?.longOrNull
                ?: 0L

            val createdAt = target.string("createdAt", "post_created_at", "postCreatedAt", "created_at") ?: ""

            val isVerified = target["isVerified"]?.jsonPrimitive?.booleanOrNull
                ?: target["is_verified"]?.jsonPrimitive?.booleanOrNull
                ?: false

            val verificationType = target.string("verificationType", "verification_type") ?: "none"

            val authorId = target.string("authorId", "author_id", "userId", "user_id")

            val hasStory = target["hasStory"]?.jsonPrimitive?.booleanOrNull
                ?: target["has_story"]?.jsonPrimitive?.booleanOrNull
                ?: target["hasActiveStory"]?.jsonPrimitive?.booleanOrNull
                ?: target["has_active_story"]?.jsonPrimitive?.booleanOrNull
                ?: false

            val postVideoUrl = target.string("postVideoUrl", "post_video_url", "videoUrl", "video_url")
            val musicUrl = target.string("musicUrl", "music_url", "postMusicUrl", "post_music_url", "audioUrl", "audio_url")
            val musicTitle = target.string("musicTitle", "music_title", "postMusicTitle", "post_music_title", "audioTitle", "audio_title")

            SharedPostDraft(
                postId = postId,
                authorName = authorName,
                authorUsername = authorUsername,
                authorAvatar = authorAvatar,
                content = content,
                mediaUrls = mediaUrls,
                postVideoUrl = postVideoUrl,
                musicUrl = musicUrl,
                musicTitle = musicTitle,
                likesCount = likes,
                commentsCount = comments,
                createdAt = createdAt,
                isVerified = isVerified,
                verificationType = verificationType,
                role = target.string("role"),
                hashtags = hashtags,
                authorId = authorId,
                hasStory = hasStory,
            )
        }.getOrNull()
    }

    private fun kotlinx.serialization.json.JsonObject.string(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            val primitive = this[key] as? kotlinx.serialization.json.JsonPrimitive
            primitive?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
        }
}
