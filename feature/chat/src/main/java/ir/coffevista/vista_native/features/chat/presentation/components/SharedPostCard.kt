package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.chat.domain.model.SharedPostDraft
import ir.coffevista.vista_native.features.chat.presentation.toPersianDigits
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * کارت پست اشتراک‌گذاری‌شده در چت هماهنگ با SocialStylePostCard در نسخه فلاتر.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedPostCard(
    post: SharedPostDraft,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onViewPost: (String) -> Unit = {},
) {
    val primaryTextColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isMine) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val cardBackground = if (isMine) {
        Color.White.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val cardBorder = if (isMine) {
        Color.White.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Column(
        modifier = modifier
            .widthIn(min = 230.dp, max = 290.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBackground)
            .border(0.75.dp, cardBorder, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = { onViewPost(post.postId) })
            .semantics { contentDescription = "پست اشتراک‌گذاری‌شده از ${post.authorName}" },
    ) {
        // --- هدر نویسنده پست ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // آواتار با حاشیه گرادیانت ویستا
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFAB47BC),
                                Color(0xFFEC407A),
                                Color(0xFFFFA726),
                            ),
                        ),
                    )
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(if (isMine) VistaBrandColors.VioletDeep else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (!post.authorAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = post.authorAvatar,
                        contentDescription = post.authorName,
                        modifier = Modifier.size(33.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = post.authorName.take(1).uppercase(),
                        color = primaryTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.authorName.ifBlank { "کاربر" },
                        color = primaryTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (post.isVerified) {
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "حساب تایید شده",
                            tint = if (isMine) Color(0xFF90CAF9) else Color(0xFF1E88E5),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                if (post.authorUsername.isNotBlank()) {
                    Text(
                        text = "@${post.authorUsername.removePrefix("@")}",
                        color = secondaryTextColor,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // --- محتوای متنی پست ---
        if (post.content.isNotBlank()) {
            Text(
                text = post.content,
                color = primaryTextColor,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(textDirection = TextDirection.Rtl),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }

        // --- هشتگ‌ها ---
        if (post.hashtags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                post.hashtags.take(4).forEach { tag ->
                    val cleanTag = if (tag.startsWith("#")) tag else "#$tag"
                    Text(
                        text = cleanTag,
                        color = if (isMine) Color(0xFFBBDEFB) else VistaBrandColors.Indigo,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // --- مدیا و تصویر بندانگشتی ---
        val mediaUrl = post.mediaUrls.firstOrNull()
        if (!mediaUrl.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .heightIn(max = 160.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = mediaUrl,
                    contentDescription = "تصویر پست",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 160.dp),
                    contentScale = ContentScale.Crop,
                )
                if (!post.postVideoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "پخش ویدیو",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(
            color = if (isMine) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 0.5.dp,
        )

        // --- نوار آمار (لایک و کامنت) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // لایک
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (post.likesCount > 0) Color(0xFFEF5350) else secondaryTextColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = post.likesCount.toString().toPersianDigits(),
                        color = secondaryTextColor,
                        fontSize = 11.sp,
                    )
                }

                // کامنت
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = null,
                        tint = secondaryTextColor,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = post.commentsCount.toString().toPersianDigits(),
                        color = secondaryTextColor,
                        fontSize = 11.sp,
                    )
                }
            }

            Text(
                text = "مشاهده پست ‹",
                color = if (isMine) Color.White.copy(alpha = 0.9f) else VistaBrandColors.Indigo,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            )
        }
    }
}

/**
 * تحلیل‌گر هوشمند برای خواندن SharedPostDraft از ساختارهای مختلف JSON و داده‌های قدیمی فلاتر.
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
            )
        }.getOrNull()
    }
}
