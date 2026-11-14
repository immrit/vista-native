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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import ir.coffevista.vista_native.core.designsystem.component.VistaSoundCloudWaveform
import ir.coffevista.vista_native.core.designsystem.component.VistaVerificationBadge
import ir.coffevista.vista_native.core.designsystem.component.formatAudioTime
import ir.coffevista.vista_native.core.designsystem.component.resolveMusicTitle
import ir.coffevista.vista_native.core.designsystem.component.resolveVerificationBadgeType
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.chat.domain.model.SharedPostDraft
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioKind
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioPlayer
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioTrack
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
    onHashtagClick: (String) -> Unit = {},
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
                                    ResolvedVerificationBadgeType.BLUE_TICK -> Color(0xFFC7D2FE)
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
            val hasEntities = remember(post.content) { post.content.contains('#') || post.content.contains('@') }
            if (hasEntities) {
                val entityStyle = SpanStyle(
                    color = if (isMine) Color(0xFFBBDEFB) else VistaBrandColors.Indigo,
                    fontWeight = FontWeight.Bold,
                )
                val annotated = remember(post.content) {
                    buildAnnotatedString {
                        val pattern = Regex("""(?<![^\s\n])@([\u0600-\u06FF\w_]+)|#([\u0600-\u06FF\w_]+)""")
                        var cursor = 0
                        pattern.findAll(post.content).forEach { match ->
                            if (match.range.first > cursor) {
                                append(post.content.substring(cursor, match.range.first))
                            }
                            val start = length
                            append(match.value)
                            addStyle(entityStyle, start, length)
                            if (match.value.startsWith("#")) {
                                addStringAnnotation("HASHTAG", match.value.removePrefix("#"), start, length)
                            } else if (match.value.startsWith("@")) {
                                addStringAnnotation("MENTION", match.value.removePrefix("@"), start, length)
                            }
                            cursor = match.range.last + 1
                        }
                        if (cursor < post.content.length) {
                            append(post.content.substring(cursor))
                        }
                    }
                }
                @Suppress("DEPRECATION")
                ClickableText(
                    text = annotated,
                    style = TextStyle(
                        color = primaryTextColor,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textDirection = TextDirection.Rtl,
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    onClick = { offset ->
                        val hashtag = annotated.getStringAnnotations("HASHTAG", offset, offset).firstOrNull()
                        if (hashtag != null) {
                            onHashtagClick(hashtag.item)
                            return@ClickableText
                        }
                        onViewPost(post.postId)
                    },
                )
            } else {
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
                        modifier = Modifier.clickable { onHashtagClick(tag.removePrefix("#")) },
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

        // --- موزیک پلیر درون پست ---
        val musicUrl = post.musicUrl?.trim()?.takeIf(String::isNotEmpty)
        if (musicUrl != null) {
            Spacer(Modifier.height(6.dp))
            SharedPostMusicPlayer(
                post = post,
                musicUrl = musicUrl,
                isMine = isMine,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
            )
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

@Composable
private fun SharedPostMusicPlayer(
    post: SharedPostDraft,
    musicUrl: String,
    isMine: Boolean,
    primaryTextColor: Color,
    secondaryTextColor: Color,
) {
    val context = LocalContext.current
    LaunchedEffect(context) { ChatAudioPlayer.attach(context) }

    val trackId = remember(post.postId, musicUrl) { "shared_post:${post.postId}" }
    val resolvedTitle = remember(post.musicTitle, musicUrl) {
        resolveMusicTitle(post.musicTitle, musicUrl)
    }
    val track = remember(trackId, post.postId, musicUrl, resolvedTitle, post.authorName, post.mediaUrls.firstOrNull()) {
        ChatAudioTrack(
            id = trackId,
            url = musicUrl,
            kind = ChatAudioKind.MUSIC,
            title = resolvedTitle,
            artist = post.authorName.ifBlank { null },
            durationMs = 0L,
            senderName = post.authorName.ifBlank { "ویستا" },
            conversationId = "",
            createdAtEpochMillis = 0L,
            postId = post.postId.takeIf(String::isNotBlank),
            artworkUrl = post.mediaUrls.firstOrNull()?.takeIf { it.isNotBlank() },
        )
    }

    val playerState by ChatAudioPlayer.state.collectAsState()
    val positionMs by ChatAudioPlayer.position.collectAsState()
    val isActive = playerState.isActive(trackId)
    val isPlaying = isActive && playerState.isPlaying
    val isBuffering = isActive && playerState.isBuffering
    val durationMs = if (isActive && playerState.durationMs > 0L) playerState.durationMs else 0L
    val activePosMs = if (isActive) positionMs else ChatAudioPlayer.savedPositionMs(trackId)
    val progress = if (durationMs > 0L) (activePosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val containerBg = if (isMine) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val containerBorder = if (isMine) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val accentColor = if (isMine) Color.White else MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(0.6.dp, containerBorder, RoundedCornerShape(12.dp))
            .clickable { ChatAudioPlayer.toggle(track) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isMine) {
                        SolidColor(Color.White.copy(alpha = 0.22f))
                    } else {
                        Brush.linearGradient(listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep))
                    },
                )
                .clickable(role = Role.Button) { ChatAudioPlayer.toggle(track) },
            contentAlignment = Alignment.Center,
        ) {
            when {
                isBuffering -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                isPlaying -> Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "توقف موسیقی پست",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                else -> Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "پخش موسیقی پست",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = resolvedTitle,
                        color = primaryTextColor,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (durationMs > 0L || activePosMs > 0L) {
                    Spacer(Modifier.width(6.dp))
                    val timeText = if (durationMs > 0L) {
                        "${formatAudioTime(activePosMs)} / ${formatAudioTime(durationMs)}"
                    } else {
                        formatAudioTime(activePosMs)
                    }
                    Text(
                        text = timeText.toPersianDigits(),
                        color = secondaryTextColor,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(5.dp))

            VistaSoundCloudWaveform(
                progress = progress,
                onSeek = { frac ->
                    if (isActive) {
                        ChatAudioPlayer.seekToFraction(frac)
                    } else {
                        ChatAudioPlayer.toggle(track)
                    }
                },
                isPlaying = isPlaying,
                currentPositionMs = activePosMs,
                durationMs = durationMs,
                seed = post.postId.hashCode().toLong(),
                barCount = 26,
                waveformHeight = 16.dp,
                activeBrush = SolidColor(accentColor),
                inactiveColor = if (isMine) Color.White.copy(alpha = 0.28f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                showTimeLabels = false,
            )
        }
    }
}

val SharedPostParser = ir.coffevista.vista_native.features.chat.domain.model.SharedPostParser

