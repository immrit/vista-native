package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Press-hold-and-drag multi-selection for a message list.
 *
 * The gesture is observed in the Initial pass so it never competes with bubble
 * click handlers: if the user keeps the finger down past the long-press timeout
 * while a selection is already active (started by a bubble or by the empty space
 * beside it), every following move is consumed and the rows under the finger are
 * selected as a contiguous range, with automatic scrolling near the list edges.
 *
 * @param isSelecting whether a selection session is currently active.
 * @param hitTest maps a position in the list's local coordinates to a list item index.
 * @param onDragStart called once with the anchor row index.
 * @param onRangeChanged called with (anchor, current) every time the row under the finger changes.
 * @param scrollBy scrolls the list by the given pixels; positive moves toward older messages.
 */
fun Modifier.dragSelectMessages(
    isSelecting: () -> Boolean,
    hitTest: (position: Offset) -> Int?,
    onDragStart: (anchor: Int) -> Unit,
    onRangeChanged: (anchor: Int, current: Int) -> Unit,
    scrollBy: suspend (Float) -> Unit,
    topEdgeInsetPx: Float,
    edgeZonePx: Float,
): Modifier = pointerInput(Unit) {
    coroutineScope {
    val scope: CoroutineScope = this
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        // Slightly longer than the bubble/row long-press so the selection session
        // is already active when we check it.
        val holdMillis = viewConfiguration.longPressTimeoutMillis + 80L

        var cancelled = false
        withTimeoutOrNull(holdMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed ||
                    (change.position - down.position).getDistance() > slop
                ) {
                    cancelled = true
                    break
                }
            }
        }
        if (cancelled || !isSelecting()) return@awaitEachGesture

        val anchor = hitTest(down.position) ?: return@awaitEachGesture
        onDragStart(anchor)

        var pointer = down.position
        var current = anchor
        val height = size.height.toFloat()

        fun refresh() {
            val index = hitTest(pointer) ?: return
            if (index != current) {
                current = index
                onRangeChanged(anchor, index)
            }
        }

        val autoScroll: Job = scope.launch {
            while (isActive) {
                delay(16)
                val direction = when {
                    pointer.y < topEdgeInsetPx + edgeZonePx -> 1f
                    pointer.y > height - edgeZonePx -> -1f
                    else -> 0f
                }
                if (direction != 0f) {
                    val depth = if (direction > 0f) {
                        (topEdgeInsetPx + edgeZonePx - pointer.y) / edgeZonePx
                    } else {
                        (pointer.y - (height - edgeZonePx)) / edgeZonePx
                    }
                    scrollBy(direction * (6f + 22f * depth.coerceIn(0f, 1f)))
                    refresh()
                }
            }
        }

        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                event.changes.forEach { it.consume() }
                if (!change.pressed) break
                pointer = change.position
                refresh()
            }
        } finally {
            autoScroll.cancel()
        }
    }
}
}
