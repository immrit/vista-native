package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiText
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Exact easing curve & timing modeled after Telegram Android's
 * CubicBezierInterpolator + ChatListItemAnimator / message enter transition.
 */
internal val TelegramSendBezierEasing = CubicBezierEasing(
    0.199194729f,
    0.010661144f,
    0.27920937f,
    0.91025195f,
)

internal const val TELEGRAM_SEND_DURATION_MS = 250
internal const val TELEGRAM_MINIMAL_SEND_DURATION_MS = 140

internal data class PendingSendFlight(
    val flightId: Long,
    val text: String,
    val replyPreview: String?,
    val startBoundsLocal: Rect,
    val createdAtMs: Long,
)

internal data class ActiveSendFlight(
    val flightId: Long,
    val messageKey: String,
    val text: String,
    val replyPreview: String?,
    val startBoundsLocal: Rect,
    val fallbackTargetBoundsLocal: Rect,
    val targetAlignRight: Boolean = true,
    val targetIsFirstInGroup: Boolean = true,
    val targetIsLastInGroup: Boolean = true,
)

internal fun isEligibleSendFlightCandidate(
    pending: PendingSendFlight?,
    candidateStableKey: String,
    candidateText: String?,
    candidateIsMine: Boolean,
    candidateIsPending: Boolean,
    flownMessageKeys: Set<String>,
): Boolean {
    if (pending == null) return false
    if (!candidateIsMine || !candidateIsPending) return false
    if (candidateStableKey.isBlank() || candidateStableKey in flownMessageKeys) return false
    val cleanCandidate = candidateText?.trim().orEmpty()
    return cleanCandidate.isNotEmpty() && cleanCandidate == pending.text
}

/**
 * Computes the physical start rectangle in composer coordinates.
 * Short messages stay anchored to the text origin side (right for RTL, left for LTR)
 * instead of detaching the full empty composer width.
 */
internal fun resolveSendFlightStartBounds(
    inputBoundsLocal: Rect,
    estimatedBubbleWidthPx: Float,
    isRtlText: Boolean,
): Rect {
    val safeWidth = inputBoundsLocal.width.coerceAtLeast(1f)
    val clippedWidth = estimatedBubbleWidthPx.coerceIn(safeWidth.coerceAtMost(120f), safeWidth)
    return if (isRtlText) {
        Rect(
            left = (inputBoundsLocal.right - clippedWidth).coerceAtLeast(inputBoundsLocal.left),
            top = inputBoundsLocal.top,
            right = inputBoundsLocal.right,
            bottom = inputBoundsLocal.bottom,
        )
    } else {
        Rect(
            left = inputBoundsLocal.left,
            top = inputBoundsLocal.top,
            right = (inputBoundsLocal.left + clippedWidth).coerceAtMost(inputBoundsLocal.right),
            bottom = inputBoundsLocal.bottom,
        )
    }
}

/**
 * Computes the physical fallback landing bounds before the real bubble reports
 * `boundsInRoot()`. Outgoing messages (`alignToRight = true`) dock against the
 * physical right edge (`rootWidthPx - horizontalMarginPx`); peer messages dock left.
 */
internal fun resolveSendFlightTargetFallbackBounds(
    rootWidthPx: Float,
    rootHeightPx: Float,
    composerHeightPx: Float,
    estimatedBubbleWidthPx: Float,
    estimatedBubbleHeightPx: Float,
    alignToRight: Boolean,
    horizontalMarginPx: Float,
    bottomMarginPx: Float,
): Rect {
    val maxWidth = (rootWidthPx - horizontalMarginPx * 2f).coerceAtLeast(1f)
    val clampedWidth = estimatedBubbleWidthPx.coerceIn(1f, maxWidth)
    val clampedHeight = estimatedBubbleHeightPx.coerceAtLeast(1f)
    val bottom = (rootHeightPx - composerHeightPx - bottomMarginPx).coerceAtLeast(clampedHeight)
    val top = (bottom - clampedHeight).coerceAtLeast(0f)
    return if (alignToRight) {
        val right = (rootWidthPx - horizontalMarginPx).coerceAtLeast(clampedWidth)
        Rect(
            left = (right - clampedWidth).coerceAtLeast(0f),
            top = top,
            right = right,
            bottom = bottom,
        )
    } else {
        val left = horizontalMarginPx.coerceAtMost((rootWidthPx - clampedWidth).coerceAtLeast(0f))
        Rect(
            left = left,
            top = top,
            right = (left + clampedWidth).coerceAtMost(rootWidthPx),
            bottom = bottom,
        )
    }
}

internal fun interpolateSendFlightRect(
    start: Rect,
    target: Rect,
    progress: Float,
): Rect {
    val p = progress.coerceIn(0f, 1f)
    val left = lerp(start.left, target.left, p)
    val top = lerp(start.top, target.top, p)
    val right = lerp(start.right, target.right, p)
    val bottom = lerp(start.bottom, target.bottom, p)
    return Rect(
        left = left,
        top = top,
        right = right.coerceAtLeast(left + 1f),
        bottom = bottom.coerceAtLeast(top + 1f),
    )
}

@Composable
internal fun TelegramSendFlightOverlay(
    flight: ActiveSendFlight,
    targetBoundsLocal: Rect?,
    textDirection: TextDirection,
    minimalMotion: Boolean,
    onFlightFinished: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val latestTargetBounds by rememberUpdatedState(targetBoundsLocal ?: flight.fallbackTargetBoundsLocal)
    val latestOnFinished by rememberUpdatedState(onFlightFinished)
    val progress = remember(flight.flightId) { Animatable(0f) }

    LaunchedEffect(flight.flightId) {
        try {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (minimalMotion) {
                        TELEGRAM_MINIMAL_SEND_DURATION_MS
                    } else {
                        TELEGRAM_SEND_DURATION_MS
                    },
                    easing = TelegramSendBezierEasing,
                ),
            )
        } finally {
            latestOnFinished(flight.messageKey)
        }
    }

    val p = progress.value.coerceIn(0f, 1f)
    val start = flight.startBoundsLocal
    val target = latestTargetBounds
    val currentRect = interpolateSendFlightRect(start = start, target = target, progress = p)

    val topLeftTargetCorner = if (flight.targetAlignRight || flight.targetIsFirstInGroup) 18f else 6f
    val topRightTargetCorner = if (!flight.targetAlignRight || flight.targetIsFirstInGroup) 18f else 6f
    val bottomLeftTargetCorner = if (flight.targetAlignRight || flight.targetIsLastInGroup) 18f else 6f
    val bottomRightTargetCorner = if (!flight.targetAlignRight || flight.targetIsLastInGroup) 18f else 6f

    val shape = AbsoluteRoundedCornerShape(
        topLeft = lerp(22f, topLeftTargetCorner, p).dp,
        topRight = lerp(22f, topRightTargetCorner, p).dp,
        bottomLeft = lerp(22f, bottomLeftTargetCorner, p).dp,
        bottomRight = lerp(22f, bottomRightTargetCorner, p).dp,
    )

    val bubbleGradientFraction = (p / 0.45f).coerceIn(0f, 1f)
    val textColor = lerp(
        MaterialTheme.colorScheme.onSurface,
        Color.White,
        bubbleGradientFraction,
    )
    val startSurfaceTint = if (isDark) Color(0xFF1A222C) else Color.White
    val arcLiftElevation = (sin(p * PI).toFloat() * 4f).coerceAtLeast(0f).dp
    val metaAlpha = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val isRtlMessage = textDirection == TextDirection.Rtl || textDirection == TextDirection.ContentOrRtl

    // Force physical top-left coordinate space so `currentRect` in root-local pixels
    // never gets horizontally mirrored when the surrounding screen is RTL.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = AbsoluteAlignment.TopLeft,
        ) {
            val widthDp = with(density) { currentRect.width.toDp() }
            val heightDp = with(density) { currentRect.height.toDp() }
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(currentRect.left.roundToInt(), currentRect.top.roundToInt())
                    }
                    .width(widthDp)
                    .height(heightDp)
                    .shadow(
                        elevation = 1.dp + arcLiftElevation,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.12f),
                    )
                    .clip(shape)
                    .background(startSurfaceTint.copy(alpha = 1f - bubbleGradientFraction))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                VistaBrandColors.Indigo.copy(alpha = bubbleGradientFraction),
                                VistaBrandColors.VioletDeep.copy(alpha = bubbleGradientFraction),
                            ),
                        ),
                    )
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 6.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    VistaEmojiText(
                        text = flight.text,
                        style = TextStyle(
                            color = textColor,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            textDirection = textDirection,
                            textAlign = if (isRtlMessage) TextAlign.Right else TextAlign.Left,
                        ),
                        emojiSize = 20.dp,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (metaAlpha > 0.01f) {
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.78f * metaAlpha),
                                modifier = Modifier.size(11.dp),
                            )
                            Text(
                                text = "در حال ارسال",
                                color = Color.White.copy(alpha = 0.78f * metaAlpha),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}
