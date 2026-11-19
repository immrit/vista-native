package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
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
import ir.coffevista.vista_native.features.chat.presentation.MessageGroupPosition
import ir.coffevista.vista_native.features.chat.presentation.messageBubbleCornerRounding
import ir.coffevista.vista_native.features.chat.presentation.telegramXOuterCornerRadii
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Exact easing curves & timing modeled after Telegram Android (`DrKLO/Telegram`):
 * `ChatListItemAnimator.DEFAULT_INTERPOLATOR`, `CubicBezierInterpolator.EASE_OUT_QUINT`,
 * `CubicBezierInterpolator.EASE_OUT`, and `TextMessageEnterTransition.java`.
 */
internal val TelegramSendBezierEasing = CubicBezierEasing(
    0.199194729f,
    0.010661144f,
    0.27920937f,
    0.91025195f,
)

internal val TelegramEaseOutQuint = CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)
internal val TelegramEaseOut = CubicBezierEasing(0.0f, 0.0f, 0.58f, 1.0f)

internal const val TELEGRAM_SEND_DURATION_MS = 250
internal const val TELEGRAM_MINIMAL_SEND_DURATION_MS = 140
internal const val TELEGRAM_COMPOSER_TO_BUBBLE_TEXT_SCALE = 17.5f / 16f

internal data class PendingSendFlight(
    val flightId: Long,
    val text: String,
    val replyPreview: String?,
    val startBoundsLocal: Rect,
    val createdAtMs: Long,
    val replySender: String? = null,
    val replyStartBoundsLocal: Rect? = null,
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
    val replySender: String? = null,
    val replyStartBoundsLocal: Rect? = null,
    val formattedTime: String = "",
    val topClipPx: Float = 0f,
)

internal data class SendFlightFrameMetrics(
    val rect: Rect,
    val progressY: Float,
    val progressX: Float,
    val alphaProgress: Float,
    val textScale: Float,
    val shadowAlpha: Float,
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

/**
 * Exact dual-curve flight math from Telegram Android's `TextMessageEnterTransition.java`:
 * - `progressY` follows `TelegramSendBezierEasing`
 * - `progressX` follows `EASE_OUT(EASE_OUT_QUINT(t))` (horizontal arc ahead of vertical travel)
 * - `alphaProgress` completes within the first 40% (`t / 0.4f`)
 * - subtle trailing edge overshoot tapers out along `(1 - progressX)`
 * - text scales smoothly from composer sp size down to bubble sp size along `progressX`
 */
internal fun evaluateSendFlightFrame(
    rawProgress: Float,
    start: Rect,
    target: Rect,
    alignToRight: Boolean,
    rightOvershootPx: Float,
    composerTextScale: Float = TELEGRAM_COMPOSER_TO_BUBBLE_TEXT_SCALE,
): SendFlightFrameMetrics {
    val t = rawProgress.coerceIn(0f, 1f)
    val progressY = TelegramSendBezierEasing.transform(t)
    val progressX = TelegramEaseOut.transform(TelegramEaseOutQuint.transform(t))
    val alphaProgress = (t / 0.4f).coerceIn(0f, 1f)
    val overshoot = if (alignToRight) {
        rightOvershootPx * (1f - progressX)
    } else {
        -rightOvershootPx * (1f - progressX)
    }
    val left = lerp(start.left, target.left, progressX) + if (!alignToRight) overshoot else 0f
    val right = lerp(start.right, target.right, progressX) + if (alignToRight) overshoot else 0f
    val top = lerp(start.top, target.top, progressY)
    val bottom = lerp(start.bottom, target.bottom, progressY)
    val textScale = progressX + composerTextScale * (1f - progressX)
    return SendFlightFrameMetrics(
        rect = Rect(
            left = left,
            top = top,
            right = right.coerceAtLeast(left + 1f),
            bottom = bottom.coerceAtLeast(top + 1f),
        ),
        progressY = progressY,
        progressX = progressX,
        alphaProgress = alphaProgress,
        textScale = textScale,
        shadowAlpha = progressX,
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
    val rawProgress = remember(flight.flightId) { Animatable(0f) }

    LaunchedEffect(flight.flightId) {
        try {
            rawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (minimalMotion) {
                        TELEGRAM_MINIMAL_SEND_DURATION_MS
                    } else {
                        TELEGRAM_SEND_DURATION_MS
                    },
                    easing = LinearEasing,
                ),
            )
        } finally {
            latestOnFinished(flight.messageKey)
        }
    }

    val start = flight.startBoundsLocal
    val target = latestTargetBounds
    val overshootPx = with(density) { 4.dp.toPx() }
    val frame = evaluateSendFlightFrame(
        rawProgress = rawProgress.value,
        start = start,
        target = target,
        alignToRight = flight.targetAlignRight,
        rightOvershootPx = overshootPx,
    )
    val currentRect = frame.rect
    val pAlpha = frame.alphaProgress
    val pX = frame.progressX

    // Morph corner radii from composer pill (23.dp) directly into the exact Telegram X bubble shape.
    val targetRadii = remember(flight.targetAlignRight, flight.targetIsFirstInGroup, flight.targetIsLastInGroup) {
        telegramXOuterCornerRadii(
            messageBubbleCornerRounding(
                isMine = flight.targetAlignRight,
                groupPosition = MessageGroupPosition(
                    isFirstInGroup = flight.targetIsFirstInGroup,
                    isLastInGroup = flight.targetIsLastInGroup,
                ),
            ),
        )
    }
    val shape = AbsoluteRoundedCornerShape(
        topLeft = lerp(23f, targetRadii.topLeft, pX).dp,
        topRight = lerp(23f, targetRadii.topRight, pX).dp,
        bottomLeft = lerp(23f, targetRadii.bottomLeft, pX).dp,
        bottomRight = lerp(23f, targetRadii.bottomRight, pX).dp,
    )

    val startTextColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E2732)
    val endTextColor = if (flight.targetAlignRight) Color.White else MaterialTheme.colorScheme.onSurface
    val textColor = lerp(startTextColor, endTextColor, pAlpha)
    val startSurfaceTint = if (isDark) Color(0xFF1A222C) else Color.White
    val endIncomingTint = MaterialTheme.colorScheme.surfaceVariant
    val arcLiftElevation = (sin(frame.progressY * PI).toFloat() * 3.dp.value).coerceAtLeast(0f).dp
    val shadowOpacity = frame.shadowAlpha
    val isRtlMessage = textDirection == TextDirection.Rtl || textDirection == TextDirection.ContentOrRtl

    // Force physical top-left coordinate space so `currentRect` in root-local pixels
    // never gets horizontally mirrored when the surrounding screen is RTL.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .drawWithContent {
                    val safeTop = flight.topClipPx.coerceIn(0f, size.height)
                    clipRect(top = safeTop) {
                        this@drawWithContent.drawContent()
                    }
                },
            contentAlignment = AbsoluteAlignment.TopLeft,
        ) {
            val widthDp = with(density) { currentRect.width.toDp() }
            val heightDp = with(density) { currentRect.height.toDp() }
            val bubbleBgModifier = if (flight.targetAlignRight) {
                Modifier
                    .background(startSurfaceTint.copy(alpha = 1f - pAlpha))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                VistaBrandColors.Indigo.copy(alpha = pAlpha),
                                VistaBrandColors.VioletDeep.copy(alpha = pAlpha),
                            ),
                        ),
                    )
            } else {
                Modifier.background(lerp(startSurfaceTint, endIncomingTint, pAlpha))
            }

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
                        ambientColor = Color.Black.copy(alpha = 0.06f * shadowOpacity),
                        spotColor = Color.Black.copy(alpha = 0.08f * shadowOpacity),
                    )
                    .clip(shape)
                    .then(bubbleBgModifier)
                    .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 6.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val replyPreview = flight.replyPreview?.trim().orEmpty()
                    if (replyPreview.isNotEmpty()) {
                        val barColor = lerp(
                            MaterialTheme.colorScheme.primary,
                            if (flight.targetAlignRight) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary,
                            pAlpha,
                        )
                        val replyCardBg = if (flight.targetAlignRight) {
                            Color.White.copy(alpha = 0.15f * pAlpha)
                        } else {
                            Color.Black.copy(alpha = 0.05f * pAlpha)
                        }
                        val replyTitleColor = lerp(
                            MaterialTheme.colorScheme.primary,
                            if (flight.targetAlignRight) Color.White else MaterialTheme.colorScheme.primary,
                            pAlpha,
                        )
                        val replyBodyColor = lerp(
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            if (flight.targetAlignRight) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            pAlpha,
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(replyCardBg)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .width(3.dp)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(barColor),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val senderTitle = flight.replySender?.trim().orEmpty()
                                    if (senderTitle.isNotEmpty()) {
                                        Text(
                                            text = senderTitle,
                                            fontWeight = FontWeight.SemiBold,
                                            color = replyTitleColor,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Text(
                                        text = replyPreview,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = replyBodyColor,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }

                    ChatTextBubbleLayout(
                        text = {
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
                                modifier = Modifier.graphicsLayer {
                                    scaleX = frame.textScale
                                    scaleY = frame.textScale
                                    transformOrigin = TransformOrigin(if (isRtlMessage) 1f else 0f, 0.5f)
                                },
                            )
                        },
                        footer = {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.graphicsLayer { alpha = pAlpha },
                                ) {
                                    if (flight.formattedTime.isNotBlank()) {
                                        Text(
                                            text = flight.formattedTime,
                                            color = if (flight.targetAlignRight) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                        )
                                        Spacer(Modifier.width(3.dp))
                                    }
                                    if (flight.targetAlignRight) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.72f),
                                            modifier = Modifier.size(11.dp),
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        layoutDirection = if (isRtlMessage) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    )
                }
            }
        }
    }
}
