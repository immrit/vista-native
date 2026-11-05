package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.sin

/**
 * Minimalist interactive audio waveform component.
 *
 * Renders audio waveform bars with a smooth, continuous fluid fill that sweeps across each bar
 * from the side (via canvas clipping), completely avoiding abrupt bar-by-bar discrete jumps.
 * The bars maintain natural, steady heights (no dancing or pulsing) for a clean, minimal aesthetic.
 *
 * Supports dragging finger across the waveform to scrub (seek) backward / forward smoothly with haptic feedback,
 * as well as tapping anywhere along the waveform to immediately jump to that position.
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
    barCount: Int = 42,
    waveformHeight: Dp = 32.dp,
    activeBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary),
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
    showTimeLabels: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val currentOnSeek = rememberUpdatedState(onSeek)

    // Generate deterministic pseudo-waveform heights based on audio seed.
    // Heights are completely stable and static (no dancing animation).
    val barAmplitudes = remember(seed, barCount) {
        val safeSeed = if (seed == 0L) 1234567L else seed
        List(barCount) { index ->
            val norm = index.toFloat() / (barCount - 1f).coerceAtLeast(1f)
            val envelope = (sin(norm * Math.PI).toFloat()).coerceAtLeast(0.35f)
            val variation = sin(index * 0.82f + (safeSeed % 100)) * kotlin.math.cos(index * 0.42f + 1.1f)
            val secondary = sin(index * 1.5f + (safeSeed % 37)) * 0.18f
            val raw = 0.22f + 0.78f * envelope * (0.50f + 0.38f * abs(variation).toFloat() + abs(secondary).toFloat())
            raw.coerceIn(0.18f, 1.0f)
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
                    modifier = Modifier.fillMaxWidth().height(waveformHeight),
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    if (canvasWidth <= 0 || canvasHeight <= 0) return@Canvas

                    val totalSpacing = (barCount - 1) * 2.dp.toPx()
                    val barWidth = ((canvasWidth - totalSpacing) / barCount.toFloat()).coerceAtLeast(1.5.dp.toPx())
                    val barSpacing = if (barCount > 1) {
                        (canvasWidth - (barWidth * barCount)) / (barCount - 1).toFloat()
                    } else 0f
                    val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                    // 1. Draw base inactive waveform (all bars in unplayed color)
                    for (i in 0 until barCount) {
                        val barH = (canvasHeight * barAmplitudes[i]).coerceAtLeast(barWidth)
                        val topY = (canvasHeight - barH) / 2f
                        val leftX = i * (barWidth + barSpacing)

                        drawRoundRect(
                            color = inactiveColor,
                            topLeft = Offset(leftX, topY),
                            size = Size(barWidth, barH),
                            cornerRadius = cornerRadius,
                        )
                    }

                    // 2. Continuous, fluid horizontal fill:
                    // Using clipRect to slice horizontally across the bars as progress advances.
                    // This creates the exact effect of each bar smoothly filling from the side.
                    val fillRight = (progress * canvasWidth).coerceIn(0f, canvasWidth)
                    if (fillRight > 0f) {
                        clipRect(left = 0f, top = 0f, right = fillRight, bottom = canvasHeight) {
                            for (i in 0 until barCount) {
                                val barH = (canvasHeight * barAmplitudes[i]).coerceAtLeast(barWidth)
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
                        .padding(top = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatAudioTime(currentPositionMs),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                    Text(
                        text = formatAudioTime(durationMs),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                }
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
