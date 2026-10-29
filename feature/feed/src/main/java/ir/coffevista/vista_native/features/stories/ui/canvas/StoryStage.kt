package ir.coffevista.vista_native.features.stories.ui.canvas

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.min

/** Size (px) of the 9:16 stage the current composition is rendering into. */
internal val LocalStageSize = staticCompositionLocalOf {
    StageSize(StoryStageSpec.RefWidth, StoryStageSpec.RefHeight)
}

/**
 * The single source of truth for story geometry: the largest 9:16 rectangle that fits the
 * available space, centred.
 */
@Composable
internal fun StoryStage(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    content: @Composable BoxScope.(StageSize) -> Unit,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val widthPx = floor(min(maxW, maxH * StoryStageSpec.Aspect)).coerceAtLeast(1f)
        val heightPx = floor(widthPx / StoryStageSpec.Aspect).coerceAtLeast(1f)
        val stage = StageSize(widthPx, heightPx)
        val outerDensity = LocalDensity.current
        val widthDp = with(outerDensity) { widthPx.toDp() }
        val heightDp = with(outerDensity) { heightPx.toDp() }
        val corner = if (cornerRadius.value > 0f) {
            Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius))
        } else {
            Modifier
        }
        Box(modifier = Modifier.size(widthDp, heightDp).then(corner)) {
            CompositionLocalProvider(
                LocalStageSize provides stage,
                LocalDensity provides Density(density = stage.unit, fontScale = 1f),
            ) {
                content(stage)
            }
        }
    }
}

/** Thin wrapper over [View.performHapticFeedback] with API-level fallbacks. */
internal class StoryHaptics(private val view: View) {
    fun snap() = perform(HapticFeedbackConstants.CLOCK_TICK)

    fun boundary() = perform(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK,
    )

    fun trashHover() = perform(
        if (Build.VERSION.SDK_INT >= 23) HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.LONG_PRESS,
    )

    fun deleted() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
    )

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}
