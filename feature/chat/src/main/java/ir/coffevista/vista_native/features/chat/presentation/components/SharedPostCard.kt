package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.component.ResolvedVerificationBadgeType
import ir.coffevista.vista_native.core.designsystem.component.VistaVerificationBadge
import ir.coffevista.vista_native.core.designsystem.component.resolveVerificationBadgeType
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.chat.domain.model.SharedPostDraft
import ir.coffevista.vista_native.features.chat.domain.model.SharedPostParser
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
    val isDarkTheme = isSystemInDarkTheme()
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
            // آواتار با حاشیه گرادیانت استوری (فقط در صورتی که کاربر استوری فعال داشته باشد)
            val hasStory = post.hasStory
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (hasStory) {
                            Modifier
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
                                .background(if (isMine) VistaBrandColors.VioletDeep else MaterialTheme.colorScheme.surface)
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!post.authorAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = post.authorAvatar,
                        contentDescription = post.authorName,
                        modifier = Modifier
                            .size(if (hasStory) 33.dp else 36.dp)
                            .clip(CircleShape),
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.authorName.ifBlank { "کاربر" },
                        color = primaryTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (post.isVerified) {
                        Spacer(Modifier.width(3.dp))
                        VistaVerificationBadge(
                            isVerified = post.isVerified,
                            verificationType = post.verificationType,
                            role = post.role,
                            size = 13.dp,
                            isDarkSurface = isMine || isDarkTheme,
                            overrideColor = if (isMine) {
                                when (resolveVerificationBadgeType(post.isVerified, post.verificationType, post.role)) {
                                    ResolvedVerificationBadgeType.BLUE_TICK -> Color(0xFF90CAF9)
                                    ResolvedVerificationBadgeType.GOLD_TICK -> Color(0xFFFFD54F)
                                    ResolvedVerificationBadgeType.BLACK_TICK -> Color.White
                                    ResolvedVerificationBadgeType.NONE -> Color.Transparent
                                }
                            } else null,
                        )
                    }
                }
                if (post.authorUsername.isNotBlank()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = "@${post.authorUsername.removePrefix("@")}",
                            color = secondaryTextColor,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button) { onViewPost(post.postId) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "مشاهده پست ‹",
                    color = if (isMine) Color.White.copy(alpha = 0.9f) else VistaBrandColors.Indigo,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

val SharedPostParser = ir.coffevista.vista_native.features.chat.domain.model.SharedPostParser
