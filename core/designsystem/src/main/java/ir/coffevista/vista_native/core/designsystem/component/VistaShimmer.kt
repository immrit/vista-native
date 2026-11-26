package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Provides an RTL-aware, subtle shimmer brush with calm 6-8% contrast delta
 * so skeletons feel crafted and unobtrusive instead of harsh flashing boxes.
 */
@Composable
fun rememberVistaShimmerBrush(): Brush {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = lerp(
        base,
        MaterialTheme.colorScheme.onSurface,
        if (isDark) 0.08f else 0.06f,
    )
    val transition = rememberInfiniteTransition(label = "vistaShimmerTransition")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vistaShimmerProgress",
    )
    val startX = if (isRtl) 1200f - progress * 1600f else -400f + progress * 1600f
    val endX = startX + 380f
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x = startX, y = 0f),
        end = Offset(x = endX, y = 180f),
    )
}
