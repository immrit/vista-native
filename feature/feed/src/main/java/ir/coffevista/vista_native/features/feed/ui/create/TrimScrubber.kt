package ir.coffevista.vista_native.features.feed.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * One-finger trimmer used by both the music and the video editor.
 *
 * A highlighted window of [clipMs] stays put while the timeline (the [content] slot, [durationMs]
 * long) scrolls underneath it, so any part of a track of any length is reachable with a plain
 * swipe or fling. The window start is reported live through [onStartChanged].
 *
 * @param scaleReferenceMs the clip length that exactly fills the window area; it fixes the
 *   horizontal scale so changing [clipMs] resizes the window instead of zooming the timeline.
 * @param snapStart optional hook run when scrolling settles; the timeline glides to the value it
 *   returns (used by the video trimmer to snap to keyframes).
 */
@Composable
internal fun TrimScrubber(
    durationMs: Long,
    clipMs: Long,
    initialStartMs: Long,
    scaleReferenceMs: Long,
    onStartChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 76.dp,
    snapStart: (Long) -> Long = { it },
    content: @Composable (contentWidth: Dp, contentHeight: Dp) -> Unit,
) {
    if (durationMs <= 0L || clipMs <= 0L) return

    // Time always flows left → right regardless of the (RTL) screen direction.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val density = LocalDensity.current
        val haptics = LocalHapticFeedback.current
        val primary = MaterialTheme.colorScheme.primary
        val scrollState = rememberScrollState()
        val currentOnStartChanged by rememberUpdatedState(onStartChanged)
        val currentSnapStart by rememberUpdatedState(snapStart)

        BoxWithConstraints(modifier = modifier.fillMaxWidth().height(height)) {
            val viewportPx = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else with(density) { 360.dp.toPx() }
            val marginPx = with(density) { 24.dp.toPx() }
            val windowAreaPx = (viewportPx - 2f * marginPx).coerceAtLeast(1f)
            val referenceMs = minOf(scaleReferenceMs.coerceAtLeast(1L), durationMs).coerceAtLeast(1L)
            val pxPerMs = windowAreaPx / referenceMs
            if (pxPerMs <= 0f || pxPerMs.isNaN() || pxPerMs.isInfinite()) return@BoxWithConstraints
            val windowPx = clipMs * pxPerMs
            val contentPx = durationMs * pxPerMs

            // Jump to the remembered start once (and again if the scale changes, e.g. rotation).
            LaunchedEffect(pxPerMs) {
                scrollState.scrollTo(TrimMath.scrollFromStart(initialStartMs, pxPerMs, durationMs, clipMs))
            }

            // Live start while dragging / flinging.
            LaunchedEffect(pxPerMs, durationMs, clipMs) {
                snapshotFlow { scrollState.value }
                    .distinctUntilChanged()
                    .collect { px ->
                        currentOnStartChanged(TrimMath.startFromScroll(px, pxPerMs, durationMs, clipMs))
                    }
            }

            // Once the finger lifts and the fling ends, let the caller snap (keyframes) and buzz.
            LaunchedEffect(pxPerMs, durationMs, clipMs) {
                snapshotFlow { scrollState.isScrollInProgress }
                    .distinctUntilChanged()
                    .drop(1)
                    .collect { scrolling ->
                        if (scrolling) return@collect
                        val current = TrimMath.startFromScroll(scrollState.value, pxPerMs, durationMs, clipMs)
                        val snapped = TrimMath.clampStartMs(currentSnapStart(current), durationMs, clipMs)
                        if (snapped != current) {
                            scrollState.animateScrollTo(
                                TrimMath.scrollFromStart(snapped, pxPerMs, durationMs, clipMs),
                            )
                        }
                        runCatching { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    }
            }

            val marginDp = with(density) { marginPx.toDp() }
            val contentWidthDp = with(density) { contentPx.toDp() }
            val trailingDp = with(density) { (viewportPx - marginPx - windowPx).coerceAtLeast(0f).toDp() }
            val windowDp = with(density) { windowPx.toDp() }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState),
            ) {
                Spacer(Modifier.width(marginDp))
                Box(
                    modifier = Modifier
                        .width(contentWidthDp)
                        .height(height)
                        .clip(RoundedCornerShape(10.dp)),
                ) {
                    content(contentWidthDp, height)
                }
                Spacer(Modifier.width(trailingDp))
            }

            // Fixed selection window + scrim over everything outside it. Pure drawing, so touches
            // fall straight through to the scrollable timeline underneath.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        val scrim = Color.Black.copy(alpha = 0.55f)
                        drawRect(scrim, Offset.Zero, Size(marginPx, size.height))
                        val windowEnd = marginPx + windowPx
                        drawRect(scrim, Offset(windowEnd, 0f), Size((size.width - windowEnd).coerceAtLeast(0f), size.height))
                    },
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = marginDp)
                        .width(windowDp)
                        .fillMaxSize()
                        .background(primary.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                        .border(2.5.dp, primary, RoundedCornerShape(10.dp)),
                )
            }
        }
    }
}
