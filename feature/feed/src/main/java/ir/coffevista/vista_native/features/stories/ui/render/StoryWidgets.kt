package ir.coffevista.vista_native.features.stories.ui.render

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.domain.StoryMusic
import ir.coffevista.vista_native.features.stories.domain.StoryPoll
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Dominant dual-color gradient extracted from the active StoryEntry media
 * (used for Background Layer 0 and Chameleon Widget Theme preset #2).
 */
internal data class StoryDominantColors(
    val topColor: Color = Color(0xFF1A1B2F),
    val bottomColor: Color = Color(0xFF0F101A),
)

internal val LocalStoryDominantColors = compositionLocalOf { StoryDominantColors() }

internal data class WidgetPalette(
    val containerBrush: Brush?,
    val containerColor: Color,
    val content: Color,
    val secondary: Color,
    val accent: Color,
    val badgeBg: Color,
    val topBorder: Color,
    val bottomBorder: Color,
    val textShadow: Boolean = false,
) {
    val field: Color
        get() = if (content.luminance() > 0.5f) {
            Color.White.copy(alpha = 0.14f)
        } else {
            Color(0xFF111318).copy(alpha = 0.065f)
        }
}

internal const val WidgetVariantCount = 4

/**
 * Telegram LocationMarker / LinkView 4-preset flagship theme engine:
 * 0 = Crisp White Porcelain (Pure white capsule, jewel-tinted icon badge, charcoal typography)
 * 1 = Frosted Obsidian Glass (Deep dark glass, top-lit specular border, glowing category badge)
 * 2 = Chameleon Media Aurora (Dynamic diagonal gradient from media dominant colors)
 * 3 = Crystal Translucent Glass (Airy frosted glass with luminous border & subtle text shadow)
 */
internal fun widgetPalette(
    variant: Int,
    dominantColors: StoryDominantColors = StoryDominantColors(),
    categoryAccent: Color = VistaBrandColors.Pink,
): WidgetPalette = when (Math.floorMod(variant, WidgetVariantCount)) {
    0 -> WidgetPalette(
        containerBrush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF6F7FB))),
        containerColor = Color.White,
        content = Color(0xFF111318),
        secondary = Color(0xFF646A7A),
        accent = categoryAccent,
        badgeBg = categoryAccent.copy(alpha = 0.14f),
        topBorder = Color.White,
        bottomBorder = Color(0x14000000),
        textShadow = false,
    )
    1 -> WidgetPalette(
        containerBrush = Brush.verticalGradient(listOf(Color(0xE01C1E28), Color(0xE611131A))),
        containerColor = Color(0xE0151720),
        content = Color.White,
        secondary = Color.White.copy(alpha = 0.68f),
        accent = categoryAccent,
        badgeBg = categoryAccent.copy(alpha = 0.22f),
        topBorder = Color.White.copy(alpha = 0.26f),
        bottomBorder = Color.White.copy(alpha = 0.08f),
        textShadow = false,
    )
    2 -> {
        val cTop = if (dominantColors.topColor != Color(0xFF1A1B2F)) dominantColors.topColor else VistaBrandColors.Indigo
        val cBot = if (dominantColors.bottomColor != Color(0xFF0F101A)) dominantColors.bottomColor else VistaBrandColors.Pink
        val avgLum = (cTop.luminance() + cBot.luminance()) * 0.5f
        val fg = if (avgLum > 0.60f) Color(0xFF111318) else Color.White
        WidgetPalette(
            containerBrush = Brush.linearGradient(
                colors = listOf(cTop, categoryAccent.copy(alpha = 0.92f), cBot),
                start = Offset.Zero,
                end = Offset(320f, 180f),
            ),
            containerColor = cTop,
            content = fg,
            secondary = fg.copy(alpha = 0.80f),
            accent = fg,
            badgeBg = fg.copy(alpha = 0.18f),
            topBorder = Color.White.copy(alpha = 0.38f),
            bottomBorder = Color.White.copy(alpha = 0.14f),
            textShadow = fg == Color.White,
        )
    }
    else -> WidgetPalette(
        containerBrush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.20f)),
        ),
        containerColor = Color(0x47FFFFFF),
        content = Color.White,
        secondary = Color.White.copy(alpha = 0.86f),
        accent = Color.White,
        badgeBg = Color.White.copy(alpha = 0.22f),
        topBorder = Color.White.copy(alpha = 0.65f),
        bottomBorder = Color.White.copy(alpha = 0.28f),
        textShadow = true,
    )
}

/**
 * Spring scale pulse (1.0 -> 0.92 -> 1.0) triggered whenever a widget's
 * variant/shape mode is cycled via single-tap (Telegram LocationMarker / PhotoPaintView parity).
 */
@Composable
internal fun Modifier.tapCycleBounce(variant: Int): Modifier {
    var previousVariant by remember { mutableIntStateOf(variant) }
    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(variant) {
        if (variant != previousVariant) {
            previousVariant = variant
            scaleAnim.snapTo(0.91f)
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }
    return this.graphicsLayer {
        scaleX = scaleAnim.value
        scaleY = scaleAnim.value
    }
}

/**
 * Viewer-side gleaming border sweep animation (Telegram StoryMediaAreasView parity).
 */
@Composable
internal fun Modifier.gleamingMediaAreaBorder(
    enabled: Boolean,
    cornerRadiusDp: Float = 24f,
): Modifier {
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "media_area_gleam")
    val sweepProgress by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gleam_sweep_progress",
    )
    return this.drawWithContent {
        drawContent()
        val w = size.width
        val h = size.height
        val r = cornerRadiusDp.dp.toPx().coerceAtMost(min(w, h) * 0.5f)
        val startX = w * sweepProgress
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.78f),
                Color.Transparent,
            ),
            start = Offset(startX - w * 0.32f, 0f),
            end = Offset(startX + w * 0.32f, h),
        )
        drawRoundRect(
            brush = brush,
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = 1.6.dp.toPx()),
        )
    }
}

private fun Modifier.telegramStickerSurface(
    palette: WidgetPalette,
    shape: Shape,
    elevation: Dp = 10.dp,
): Modifier {
    val borderBrush = Brush.verticalGradient(listOf(palette.topBorder, palette.bottomBorder))
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color(0x55000000),
            spotColor = Color(0x66000000),
        )
        .clip(shape)
        .then(
            if (palette.containerBrush != null) {
                Modifier.background(palette.containerBrush, shape)
            } else {
                Modifier.background(palette.containerColor, shape)
            },
        )
        .border(width = 1.dp, brush = borderBrush, shape = shape)
}

private fun baseText(
    size: Float,
    weight: FontWeight,
    color: Color,
    withShadow: Boolean = false,
) = TextStyle(
    fontFamily = VistaFontFamily,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    textDirection = TextDirection.Content,
    shadow = if (withShadow) {
        Shadow(
            color = Color.Black.copy(alpha = 0.38f),
            offset = Offset(0f, 1.5f),
            blurRadius = 4f,
        )
    } else {
        null
    },
)

@Composable
private fun WidgetText(
    text: String,
    palette: WidgetPalette,
    size: Float = 16f,
    weight: FontWeight = FontWeight.ExtraBold,
    secondary: Boolean = false,
    maxLines: Int = 1,
    align: TextAlign = TextAlign.Center,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = text,
        style = baseText(
            size = size,
            weight = weight,
            color = if (secondary) palette.secondary else palette.content,
            withShadow = palette.textShadow,
        ).copy(textAlign = align),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
internal fun StoryPillWidget(
    text: String,
    variant: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingEmoji: String? = null,
    trailing: String? = null,
    categoryAccent: Color = VistaBrandColors.Pink,
    gleaming: Boolean = false,
) {
    val dominant = LocalStoryDominantColors.current
    val palette = widgetPalette(variant, dominant, categoryAccent)
    val resolvedIcon = if (!leadingEmoji.isNullOrBlank()) null else (icon ?: when {
        text.startsWith("@") -> Icons.Default.AlternateEmail
        text.startsWith("#") -> Icons.Default.Tag
        else -> null
    })
    val hasLeadingBadge = !leadingEmoji.isNullOrBlank() || resolvedIcon != null
    val displayText = when {
        icon == null && leadingEmoji.isNullOrBlank() && text.startsWith("@") -> text.removePrefix("@")
        icon == null && leadingEmoji.isNullOrBlank() && text.startsWith("#") -> text.removePrefix("#")
        else -> text
    }
    val pillShape = RoundedCornerShape(50)

    Row(
        modifier = modifier
            .tapCycleBounce(variant)
            .telegramStickerSurface(palette, pillShape, elevation = 10.dp)
            .gleamingMediaAreaBorder(enabled = gleaming, cornerRadiusDp = 28f)
            .padding(
                start = if (hasLeadingBadge) 7.dp else 16.dp,
                end = 16.dp,
                top = 7.dp,
                bottom = 7.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (!leadingEmoji.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(palette.badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = leadingEmoji,
                    style = TextStyle(fontSize = 15.sp, textAlign = TextAlign.Center),
                )
            }
        } else if (resolvedIcon != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(palette.badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = resolvedIcon,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        WidgetText(
            text = displayText,
            palette = palette,
            size = 15.5f,
            weight = FontWeight.ExtraBold,
        )
        if (!trailing.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.field)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                WidgetText(
                    text = trailing,
                    palette = palette,
                    size = 12f,
                    weight = FontWeight.Bold,
                    secondary = true,
                )
            }
        }
    }
}

internal val LocationIcon: ImageVector get() = Icons.Default.LocationOn
internal val LinkIcon: ImageVector get() = Icons.Default.Link
internal val DateIcon: ImageVector get() = Icons.Default.CalendarMonth
internal val WeatherIcon: ImageVector get() = Icons.Default.WbSunny

@Composable
private fun rememberTickingNow(): State<Long> = produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        delay(1_000L)
        value = System.currentTimeMillis()
    }
}

@Composable
internal fun StoryCountdownWidget(
    title: String,
    targetMs: Long,
    variant: Int,
    modifier: Modifier = Modifier,
) {
    val dominant = LocalStoryDominantColors.current
    val palette = widgetPalette(variant, dominant, Color(0xFFFF6D3F))
    val now by rememberTickingNow()
    val remaining = (targetMs - now).coerceAtLeast(0L)
    val days = remaining / 86_400_000L
    val hours = remaining / 3_600_000L % 24
    val minutes = remaining / 60_000L % 60
    val seconds = remaining / 1_000L % 60
    val cardShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .tapCycleBounce(variant)
            .width(256.dp)
            .telegramStickerSurface(palette, cardShape, elevation = 12.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(palette.badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(14.dp),
                )
            }
            WidgetText(
                text = title,
                palette = palette,
                size = 14.5f,
                weight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Spacer(Modifier.height(12.dp))
        if (remaining == 0L) {
            WidgetText("رویداد آغاز شد ✨", palette, size = 16f)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (days > 0) CountdownCell(days, "روز", palette)
                CountdownCell(hours, "ساعت", palette)
                CountdownCell(minutes, "دقیقه", palette)
                CountdownCell(seconds, "ثانیه", palette)
            }
        }
    }
}

@Composable
private fun CountdownCell(value: Long, label: String, palette: WidgetPalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(palette.field)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetText("%02d".format(value).toPersianDigits(), palette, size = 19f, weight = FontWeight.Black)
        }
        Spacer(Modifier.height(4.dp))
        WidgetText(label, palette, size = 10f, weight = FontWeight.SemiBold, secondary = true)
    }
}

@Composable
internal fun StoryQuestionWidget(
    prompt: String,
    variant: Int,
    modifier: Modifier = Modifier,
    isAuthor: Boolean = false,
    onAnswerClick: (() -> Unit)? = null,
    onViewAnswers: (() -> Unit)? = null,
) {
    val dominant = LocalStoryDominantColors.current
    val palette = widgetPalette(variant, dominant, VistaBrandColors.Violet)
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .tapCycleBounce(variant)
            .width(268.dp)
            .telegramStickerSurface(palette, shape, elevation = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when (Math.floorMod(variant, WidgetVariantCount)) {
                        3 -> Brush.linearGradient(listOf(Color.White.copy(0.18f), Color.White.copy(0.08f)))
                        1 -> Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))
                        else -> Brush.linearGradient(listOf(VistaBrandColors.Indigo, VistaBrandColors.Violet, VistaBrandColors.Pink))
                    },
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetText(
                text = prompt,
                palette = WidgetPalette(
                    containerBrush = null,
                    containerColor = Color.Unspecified,
                    content = Color.White,
                    secondary = Color.White.copy(alpha = 0.8f),
                    accent = Color.White,
                    badgeBg = Color.Transparent,
                    topBorder = Color.Transparent,
                    bottomBorder = Color.Transparent,
                ),
                size = 16f,
                maxLines = 3,
            )
        }
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            if (isAuthor) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.field)
                        .then(if (onViewAnswers != null) Modifier.clickable(onClick = onViewAnswers) else Modifier)
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = palette.accent, modifier = Modifier.size(17.dp))
                    WidgetText("مشاهده پاسخ‌ها", palette, size = 13.5f, weight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.field)
                        .then(if (onAnswerClick != null) Modifier.clickable(onClick = onAnswerClick) else Modifier)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    WidgetText(
                        text = "پاسخ خود را بنویسید…",
                        palette = palette,
                        size = 13.5f,
                        weight = FontWeight.Medium,
                        secondary = true,
                        align = TextAlign.Start,
                    )
                }
            }
        }
    }
}

@Composable
internal fun StoryPollWidget(
    poll: StoryPoll,
    variant: Int,
    modifier: Modifier = Modifier,
    isAuthor: Boolean = false,
    onVote: ((optionIndex: Int) -> Unit)? = null,
) {
    val dominant = LocalStoryDominantColors.current
    val palette = widgetPalette(variant, dominant, VistaBrandColors.Indigo)
    val shape = RoundedCornerShape(24.dp)
    val hasVoted = poll.userVotedOptionId != null || poll.options.any { it.isVoted }
    val showResults = hasVoted || isAuthor
    val total = maxOf(poll.options.sumOf { it.votesCount }, poll.totalVotes, 1)

    Column(
        modifier = modifier
            .tapCycleBounce(variant)
            .width(268.dp)
            .telegramStickerSurface(palette, shape, elevation = 12.dp)
            .padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WidgetText(
            text = poll.question,
            palette = palette,
            size = 16.5f,
            weight = FontWeight.ExtraBold,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            poll.options.forEachIndexed { index, option ->
                val targetFraction = if (showResults) (option.votesCount.toFloat() / total).coerceIn(0f, 1f) else 0f
                val animatedFraction by animateFloatAsState(
                    targetValue = targetFraction,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                    label = "poll_bar_$index",
                )
                val mine = option.isVoted || option.id == poll.userVotedOptionId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.field)
                        .border(
                            width = if (mine) 1.5.dp else 0.8.dp,
                            color = if (mine) palette.accent else palette.content.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .then(if (!showResults && onVote != null) Modifier.clickable { onVote(index) } else Modifier),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (showResults && animatedFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction.coerceAtLeast(0.04f))
                                .background(
                                    if (mine) palette.accent.copy(alpha = 0.32f)
                                    else palette.content.copy(alpha = 0.12f),
                                ),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (mine) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            WidgetText(
                                text = option.text,
                                palette = palette,
                                size = 14.5f,
                                weight = FontWeight.Bold,
                                maxLines = 1,
                                align = if (showResults) TextAlign.Start else TextAlign.Center,
                                modifier = if (showResults) Modifier else Modifier.fillMaxWidth(),
                            )
                        }
                        if (showResults) {
                            WidgetText(
                                text = "${(targetFraction * 100).roundToInt().toPersianDigits()}٪",
                                palette = palette,
                                size = 13f,
                                weight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoryMusicStickerWidget(
    music: StoryMusic,
    modifier: Modifier = Modifier,
    variant: Int = 1,
) {
    val dominant = LocalStoryDominantColors.current
    val palette = widgetPalette(variant, dominant, VistaBrandColors.Pink)
    val isCompactPill = Math.floorMod(variant, WidgetVariantCount) == 3
    val shape = if (isCompactPill) RoundedCornerShape(50) else RoundedCornerShape(20.dp)

    Row(
        modifier = modifier
            .tapCycleBounce(variant)
            .then(if (isCompactPill) Modifier else Modifier.width(256.dp))
            .telegramStickerSurface(palette, shape, elevation = 12.dp)
            .padding(
                horizontal = if (isCompactPill) 10.dp else 10.dp,
                vertical = if (isCompactPill) 7.dp else 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val coverSize = if (isCompactPill) 32.dp else 46.dp
        val coverShape = if (isCompactPill) CircleShape else RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(coverSize)
                .clip(coverShape)
                .background(palette.badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            if (!music.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = music.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(if (isCompactPill) 16.dp else 22.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        if (isCompactPill) {
            WidgetText(
                text = music.title,
                palette = palette,
                size = 13.5f,
                weight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.width(8.dp))
            AnimatedEqualizerBars(color = palette.accent)
            Spacer(Modifier.width(4.dp))
        } else {
            Column(modifier = Modifier.weight(1f)) {
                WidgetText(
                    text = music.title,
                    palette = palette,
                    size = 14f,
                    weight = FontWeight.ExtraBold,
                    align = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(2.dp))
                WidgetText(
                    text = music.artist,
                    palette = palette,
                    size = 12f,
                    weight = FontWeight.Medium,
                    secondary = true,
                    align = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(palette.badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedEqualizerBars(color = palette.accent)
            }
        }
    }
}

@Composable
private fun AnimatedEqualizerBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "music_eq")
    val b1 by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1",
    )
    val b2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(340, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2",
    )
    val b3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(510, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3",
    )
    Canvas(modifier = Modifier.size(width = 14.dp, height = 14.dp)) {
        val barW = 2.6.dp.toPx()
        val gap = (size.width - barW * 3f) / 2f
        val heights = floatArrayOf(b1, b2, b3)
        for (i in 0..2) {
            val x = i * (barW + gap) + barW * 0.5f
            val barH = size.height * heights[i]
            val topY = (size.height - barH) * 0.5f
            drawLine(
                color = color,
                start = Offset(x, topY),
                end = Offset(x, topY + barH),
                strokeWidth = barW,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun StoryGifWidget(url: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = "استیکر گیف",
        modifier = modifier.size(150.dp),
        contentScale = ContentScale.Fit,
    )
}

/**
 * Telegram Reaction Widget (ReactionWidgetEntityView parity):
 * Floating asymmetric speech-bubble pill with tap-to-cycle theme and pop animation.
 */
@Composable
internal fun StoryReactionWidget(
    emoji: String,
    variant: Int,
    modifier: Modifier = Modifier,
    gleaming: Boolean = false,
) {
    val dominant = LocalStoryDominantColors.current
    val palette = widgetPalette(variant, dominant, Color(0xFFFF2D55))
    val popScale = remember { Animatable(1f) }
    LaunchedEffect(emoji) {
        popScale.snapTo(1.26f)
        popScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }
    val bubbleShape = RoundedCornerShape(
        topStart = 30.dp,
        topEnd = 30.dp,
        bottomEnd = 30.dp,
        bottomStart = 8.dp,
    )
    Box(
        modifier = modifier
            .tapCycleBounce(variant)
            .telegramStickerSurface(palette, bubbleShape, elevation = 12.dp)
            .gleamingMediaAreaBorder(enabled = gleaming, cornerRadiusDp = 28f)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = emoji.ifBlank { "❤️" },
            style = TextStyle(fontSize = 34.sp, textAlign = TextAlign.Center),
            modifier = Modifier.graphicsLayer {
                scaleX = popScale.value
                scaleY = popScale.value
            },
        )
    }
}

/**
 * Telegram Photo-on-Photo Cutout Overlay Widget (PhotoPaintView parity):
 * Single-tapping cycles shape mask (variant 0..3):
 * 0 = Squircle, 1 = Circle, 2 = Smooth 5-Point Star, 3 = Smooth Heart
 */
@Composable
internal fun StoryPhotoCutoutWidget(
    imageUri: String,
    variant: Int,
    modifier: Modifier = Modifier,
) {
    val shapeMode = Math.floorMod(variant, 4)
    val clipShape = remember(shapeMode) { PhotoCutoutShape(shapeMode) }
    Box(
        modifier = modifier
            .tapCycleBounce(variant)
            .size(168.dp)
            .shadow(14.dp, clipShape, ambientColor = Color(0x66000000), spotColor = Color(0x77000000))
            .clip(clipShape)
            .background(Color.Black.copy(alpha = 0.25f))
            .border(2.5.dp, Color.White.copy(alpha = 0.90f), clipShape),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = "برچسب تصویر",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

internal class PhotoCutoutShape(private val shapeMode: Int) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        val minDim = min(w, h)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val path = Path()
        when (Math.floorMod(shapeMode, 4)) {
            0 -> { // Squircle / Rounded Rect
                val r = minDim * 0.24f
                path.addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(r, r)))
            }
            1 -> { // Circle
                val r = minDim * 0.48f
                path.addOval(Rect(cx - r, cy - r, cx + r, cy + r))
            }
            2 -> { // Smooth Rounded 5-Point Star (Telegram PhotoPaintView parity)
                val outerR = minDim * 0.48f
                val innerR = minDim * 0.24f
                val pts = Array(10) { i ->
                    val rad = if (i % 2 == 0) outerR else innerR
                    val angle = Math.toRadians((i * 36.0) - 90.0)
                    Offset(
                        cx + (rad * cos(angle)).toFloat(),
                        cy + (rad * sin(angle)).toFloat(),
                    )
                }
                val smoothing = 0.22f
                for (i in 0 until 10) {
                    val prev = pts[(i + 9) % 10]
                    val curr = pts[i]
                    val next = pts[(i + 1) % 10]
                    val pStart = Offset(
                        curr.x + (prev.x - curr.x) * smoothing,
                        curr.y + (prev.y - curr.y) * smoothing,
                    )
                    val pEnd = Offset(
                        curr.x + (next.x - curr.x) * smoothing,
                        curr.y + (next.y - curr.y) * smoothing,
                    )
                    if (i == 0) path.moveTo(pStart.x, pStart.y) else path.lineTo(pStart.x, pStart.y)
                    path.quadraticTo(curr.x, curr.y, pEnd.x, pEnd.y)
                }
                path.close()
            }
            else -> { // Symmetric Cubic Bezier Heart
                val s = minDim * 0.45f
                path.moveTo(cx, cy + s * 0.85f)
                path.cubicTo(
                    cx - s * 1.15f, cy + s * 0.25f,
                    cx - s * 0.95f, cy - s * 0.85f,
                    cx, cy - s * 0.35f,
                )
                path.cubicTo(
                    cx + s * 0.95f, cy - s * 0.85f,
                    cx + s * 1.15f, cy + s * 0.25f,
                    cx, cy + s * 0.85f,
                )
                path.close()
            }
        }
        return Outline.Generic(path)
    }
}

@Composable
internal fun StoryEmojiStickerWidget(
    emoji: String,
    variant: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .tapCycleBounce(variant)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = emoji.ifBlank { "🔥" },
            style = TextStyle(
                fontSize = 58.sp,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.28f),
                    offset = Offset(0f, 4f),
                    blurRadius = 10f,
                ),
            ),
        )
    }
}
