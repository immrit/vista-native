package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
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
    val currentLeft = lerp(start.left, target.left, p)
    val currentTop = lerp(start.top, target.top, p)
    val currentWidthPx = lerp(start.width, target.width, p).coerceAtLeast(1f)
    val currentHeightPx = lerp(start.height, target.height, p).coerceAtLeast(1f)

    val cornerTopLeft = with(density) { lerp(22f, 18f, p).dp }
    val cornerTopRight = with(density) { lerp(22f, 18f, p).dp }
    val cornerBottomLeft = with(density) { lerp(22f, 18f, p).dp }
    val cornerBottomRight = with(density) { lerp(22f, 6f, p).dp }
    val shape = AbsoluteRoundedCornerShape(
        topLeft = cornerTopLeft,
        topRight = cornerTopRight,
        bottomLeft = cornerBottomLeft,
        bottomRight = cornerBottomRight,
    )

    val bubbleGradientFraction = (p / 0.45f).coerceIn(0f, 1f)
    val textColor = lerp(
        MaterialTheme.colorScheme.onSurface,
        Color.White,
        bubbleGradientFraction,
    )
    val startSurfaceTint = MaterialTheme.colorScheme.surface
    val arcLiftElevation = (sin(p * PI).toFloat() * 4f).coerceAtLeast(0f).dp
    val metaAlpha = ((p - 0.62f) / 0.38f).coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize()) {
        val widthDp = with(density) { currentWidthPx.toDp() }
        val heightDp = with(density) { currentHeightPx.toDp() }
        Box(
            modifier = Modifier
                .offset { IntOffset(currentLeft.roundToInt(), currentTop.roundToInt()) }
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier.align(AbsoluteAlignment.CenterRight),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = flight.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDirection = textDirection,
                    ),
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
                            modifier = Modifier.size(12.dp),
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
