package ir.coffevista.vista_native.core.designsystem.component

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Minimalist interactive audio waveform component.
 *
 * Renders audio waveform bars with a smooth, continuous fluid fill that sweeps across each bar
 * from the side (via canvas clipping), completely avoiding abrupt bar-by-bar discrete jumps.
 *
 * Features:
 * - Dynamic equalizer bounce animation when [isPlaying] is true for an alive, musical feel.
 * - Stable, natural acoustic bars when paused.
 * - Interactive scrubbing (tap or drag) with haptic feedback.
 */
@Composable
fun VistaSoundCloudWaveform(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    seed: Long = 0L,
    barCount: Int = 36,
    waveformHeight: Dp = 20.dp,
    activeBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary),
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
    showTimeLabels: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val currentOnSeek = rememberUpdatedState(onSeek)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Dynamic wave animation phase for active playback bounce
    val infiniteTransition = rememberInfiniteTransition(label = "waveform-bounce")
    val bouncePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bouncePhase",
    )

    // Generate deterministic pseudo-waveform heights based on audio seed.
    val barAmplitudes = remember(seed, barCount) {
        val safeSeed = if (seed == 0L) 1234567L else seed
        List(barCount) { index ->
            val norm = index.toFloat() / (barCount - 1f).coerceAtLeast(1f)
            val envelope = (sin(norm * Math.PI).toFloat()).coerceAtLeast(0.35f)
            val variation = sin(index * 0.82f + (safeSeed % 100)) * kotlin.math.cos(index * 0.42f + 1.1f)
            val secondary = sin(index * 1.5f + (safeSeed % 37)) * 0.18f
            val raw = 0.22f + 0.78f * envelope * (0.50f + 0.38f * abs(variation).toFloat() + abs(secondary).toFloat())
            raw.coerceIn(0.20f, 1.0f)
        }
    }

    // Force strictly Left-to-Right layout direction so audio waveform always flows 0:00 -> End
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(waveformHeight)
                    .pointerInput(barCount) {
                        detectTapGestures { offset ->
                            val width = size.width.toFloat()
                            if (width > 0) {
                                val targetFrac = (offset.x / width).coerceIn(0f, 1f)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSeek.value(targetFrac)
                            }
                        }
                    }
                    .pointerInput(barCount) {
                        var lastDispatchedFrac = progress
                        detectDragGestures(
                            onDragStart = { offset ->
                                val width = size.width.toFloat()
                                if (width > 0) {
                                    val targetFrac = (offset.x / width).coerceIn(0f, 1f)
                                    lastDispatchedFrac = targetFrac
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentOnSeek.value(targetFrac)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val width = size.width.toFloat()
                                if (width > 0) {
                                    val targetFrac = (change.position.x / width).coerceIn(0f, 1f)
                                    if (abs(targetFrac - lastDispatchedFrac) >= 0.02f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        lastDispatchedFrac = targetFrac
                                    }
                                    currentOnSeek.value(targetFrac)
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(waveformHeight),
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    if (canvasWidth <= 0 || canvasHeight <= 0) return@Canvas

                    val totalSpacing = (barCount - 1) * 2.dp.toPx()
                    val barWidth = ((canvasWidth - totalSpacing) / barCount.toFloat()).coerceAtLeast(1.8.dp.toPx())
                    val barSpacing = if (barCount > 1) {
                        (canvasWidth - (barWidth * barCount)) / (barCount - 1).toFloat()
                    } else 0f
                    val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                    // 1. Draw base inactive waveform (all bars in unplayed color)
                    for (i in 0 until barCount) {
                        val dynamicBounce = if (isPlaying) {
                            1f + 0.20f * sin(bouncePhase + i * 0.48f)
                        } else {
                            1f
                        }
                        val barH = (canvasHeight * barAmplitudes[i] * dynamicBounce).coerceIn(barWidth, canvasHeight)
                        val topY = (canvasHeight - barH) / 2f
                        val leftX = i * (barWidth + barSpacing)

                        drawRoundRect(
                            color = inactiveColor,
                            topLeft = Offset(leftX, topY),
                            size = Size(barWidth, barH),
                            cornerRadius = cornerRadius,
                        )
                    }

                    // 2. Continuous, fluid horizontal fill via clipRect
                    val fillRight = (progress * canvasWidth).coerceIn(0f, canvasWidth)
                    if (fillRight > 0f) {
                        clipRect(left = 0f, top = 0f, right = fillRight, bottom = canvasHeight) {
                            for (i in 0 until barCount) {
                                val dynamicBounce = if (isPlaying) {
                                    1f + 0.20f * sin(bouncePhase + i * 0.48f)
                                } else {
                                    1f
                                }
                                val barH = (canvasHeight * barAmplitudes[i] * dynamicBounce).coerceIn(barWidth, canvasHeight)
                                val topY = (canvasHeight - barH) / 2f
                                val leftX = i * (barWidth + barSpacing)

                                drawRoundRect(
                                    brush = activeBrush,
                                    topLeft = Offset(leftX, topY),
                                    size = Size(barWidth, barH),
                                    cornerRadius = cornerRadius,
                                    alpha = 1.0f,
                                )
                            }
                        }
                    }
                }
            }

            if (showTimeLabels) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val labelColor = if (isDark) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                    } else {
                        Color(0xFFA5A7AE)
                    }
                    Text(
                        text = formatAudioTime(currentPositionMs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor,
                    )
                    Text(
                        text = formatAudioTime(durationMs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

/**
 * Unified compact, minimal, user-friendly music player card component.
 *
 * Designed with a streamlined 2-row horizontal layout:
 * - Start: 36dp circular Play/Pause button with gradient & crisp icon
 * - Center:
 *     Top row: Track title (ellipsis) + Compact time readout (current / total)
 *     Bottom row: 36-bar acoustic waveform (20dp height) with fluid fill & dynamic bounce
 * - End (optional): Trailing action buttons (e.g. trim/delete in composer)
 */
@Composable
fun VistaMusicPlayerCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "موسیقی پست",
    isPlaying: Boolean = false,
    isBuffering: Boolean = false,
    progress: Float = 0f,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    seed: Long = 0L,
    onPlayPauseClick: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    trailingAction: (@Composable () -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBackground = if (isDark) {
        Color(0xFF131622)
    } else {
        Color(0xFFF7F8FC)
    }
    val cardBorder = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFFE5E7F0)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                border = BorderStroke(0.8.dp, cardBorder),
                shape = RoundedCornerShape(14.dp),
            )
            .then(
                if (onCardClick != null) Modifier.clickable(onClick = onCardClick) else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        color = cardBackground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sleek Circular Play/Pause Button (36dp)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                            ),
                        ),
                    )
                    .clickable(onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center,
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else if (isPlaying) {
                    Icon(
                        painter = painterResource(DesignSystemR.drawable.vista_ic_pause),
                        contentDescription = "توقف موسیقی",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(17.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "پخش موسیقی",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Center Zone: Title + Time on top, Waveform below
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title.ifBlank { "موسیقی" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    val timeString = if (durationMs > 0L) {
                        "${formatAudioTime(currentPositionMs)} / ${formatAudioTime(durationMs)}"
                    } else {
                        formatAudioTime(currentPositionMs)
                    }
                    Text(
                        text = timeString,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        } else {
                            Color(0xFF8B8E9D)
                        },
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.height(5.dp))

                VistaSoundCloudWaveform(
                    progress = progress,
                    onSeek = onSeek,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    seed = seed,
                    barCount = 36,
                    waveformHeight = 20.dp,
                    activeBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    inactiveColor = if (isDark) Color.White.copy(alpha = 0.16f) else Color(0xFFD6D8E2),
                    showTimeLabels = false,
                )
            }

            if (trailingAction != null) {
                Spacer(Modifier.width(6.dp))
                trailingAction()
            }
        }
    }
}

/** Formats milliseconds into standard mm:ss string */
fun formatAudioTime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Resolves a clean human-readable music title from title metadata or URL path.
 */
fun resolveMusicTitle(title: String?, url: String?): String {
    val direct = title?.trim().orEmpty()
    if (direct.isNotEmpty()) return direct

    val cleanUrl = url?.trim().orEmpty()
    if (cleanUrl.isEmpty()) return "موزیک"

    val lastSegment = runCatching {
        Uri.parse(cleanUrl).pathSegments?.lastOrNull()
    }.getOrNull().takeUnless { it.isNullOrBlank() } ?: cleanUrl.substringAfterLast('/')
    val withoutExtension = lastSegment.replace(Regex("""\.[^.]+$"""), "")
    val normalized = withoutExtension
        .replace(Regex("""^[^_]+_[0-9]+_"""), "")
        .replace('_', ' ')
        .trim()
    return normalized.ifEmpty { "موزیک" }
}

/**
 * Determines whether a post's music is intended to play as background music.
 * Background mode is active only when the user explicitly enabled the background music toggle
 * (encoded as #bg=1 or ?bg=1 in the musicUrl).
 */
fun isBackgroundMusicPost(musicUrl: String?): Boolean {
    if (musicUrl.isNullOrBlank()) return false
    val fragment = musicUrl.substringAfter('#', "")
    if (fragment.contains("bg=1") || fragment.contains("background=true")) return true
    val query = musicUrl.substringBefore('#').substringAfter('?', "")
    return query.contains("bg=1") || query.contains("background=true")
}

/**
 * Resolves optional start/end ms from musicUrl fragment (#t=start,end) or query params.
 */
fun resolveMusicRange(musicUrl: String): Pair<Long, Long?> {
    val fragment = musicUrl.substringAfter('#', "").trim()
    if (fragment.startsWith("t=")) {
        val timing = fragment.removePrefix("t=").substringBefore('&')
        val parts = timing.split(',')
        val start = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val end = parts.getOrNull(1)?.toLongOrNull()
        return Pair(start, end)
    }
    val query = musicUrl.substringBefore('#').substringAfter('?', "")
    if (query.isNotEmpty()) {
        val params = query.split('&').associate {
            val idx = it.indexOf('=')
            if (idx != -1) it.substring(0, idx) to it.substring(idx + 1) else it to ""
        }
        val start = params["startMs"]?.toLongOrNull()
            ?: params["t"]?.substringBefore(',')?.toLongOrNull()
            ?: 0L
        val end = params["endMs"]?.toLongOrNull()
            ?: params["t"]?.substringAfter(',', "")?.toLongOrNull()
        return Pair(start, end)
    }
    return Pair(0L, null)
}

/**
 * Alternates between relative post time and a sleek music badge.
 *
 * Guarantees that the music title is completely and thoroughly displayed
 * from the very first character to the very last character before transitioning
 * back to the post publication date.
 */
@Composable
fun PostHeaderTimeOrMusicBadge(
    timeLabel: String,
    musicTitle: String?,
    modifier: Modifier = Modifier,
    musicUrl: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    timeColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val cleanTitle = remember(musicTitle, musicUrl) {
        val direct = musicTitle?.trim().orEmpty()
        if (direct.isNotEmpty()) direct
        else if (!musicUrl.isNullOrBlank()) resolveMusicTitle(direct, musicUrl)
        else null
    }

    if (cleanTitle == null) {
        Text(
            text = timeLabel,
            color = timeColor,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = modifier,
        )
        return
    }

    var showMusic by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(cleanTitle) {
        while (isActive) {
            // 1. Show publication time
            showMusic = false
            delay(3200L)

            // 2. Switch to music badge and reset scroll to beginning
            scrollState.scrollTo(0)
            showMusic = true

            // Brief delay to allow Compose layout to measure exact scroll bounds
            delay(300L)
            val maxScroll = scrollState.maxValue

            if (maxScroll > 0) {
                // Pause at start so user can read the opening text clearly
                delay(1800L)

                // Calculate smooth, readable scroll duration at ~30 dp/sec
                val maxScrollDp = with(density) { maxScroll.toDp() }
                val scrollDurationMs = ((maxScrollDp.value / 30f) * 1000f).toInt().coerceIn(1600, 16000)

                // Scroll smoothly all the way to the very last character
                scrollState.animateScrollTo(
                    value = maxScroll,
                    animationSpec = tween(
                        durationMillis = scrollDurationMs,
                        easing = LinearEasing,
                    ),
                )

                // Hold at the end for 2.2 seconds so user finishes reading completely
                delay(2200L)
            } else {
                // Short title: fits completely without scrolling, show for 4.2 seconds
                delay(4200L)
            }
        }
    }

    AnimatedContent(
        targetState = showMusic,
        contentAlignment = Alignment.CenterStart,
        transitionSpec = {
            (slideInVertically { height -> height } + fadeIn())
                .togetherWith(slideOutVertically { height -> -height } + fadeOut())
        },
        label = "post-time-music-slide",
        modifier = modifier,
    ) { isMusic ->
        if (isMusic) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Icon(
                    painter = painterResource(DesignSystemR.drawable.vista_ic_music),
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .horizontalScroll(scrollState, enabled = false),
                ) {
                    Text(
                        text = cleanTitle,
                        color = badgeColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        } else {
            Text(
                text = timeLabel,
                color = timeColor,
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }
}

