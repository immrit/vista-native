package ir.coffevista.vista_native.features.stories.ui.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.PI
import kotlin.math.abs

internal interface StoryGestureCallbacks {
    val selectedId: String?
    fun hitTest(x: Float, y: Float): String?
    fun onGestureStart(id: String)
    fun onGestureUpdate(
        prevCx: Float,
        prevCy: Float,
        cx: Float,
        cy: Float,
        zoom: Float,
        rotationDeg: Float,
        pointerX: Float,
        pointerY: Float,
    )
    fun onGestureEnd(id: String, cancelled: Boolean)
    fun onTap(x: Float, y: Float, hitId: String?)
}

internal suspend fun PointerInputScope.detectStoryGestures(callbacks: () -> StoryGestureCallbacks) {
    val touchSlop = viewConfiguration.touchSlop
    val tapTimeout = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val cb = callbacks()
        val hitId = cb.hitTest(down.position.x, down.position.y)
        var activeId: String? = hitId
        var pastSlop = false
        var started = false
        var cancelled = false
        var zoomAcc = 1f
        var rotationAcc = 0f
        var panAcc = Offset.Zero
        var maxPointers = 1
        var lastUptime = down.uptimeMillis

        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed > maxPointers) maxPointers = pressed
            lastUptime = event.changes.maxOf { it.uptimeMillis }
            if (event.changes.any { it.isConsumed }) {
                cancelled = true
                break
            }

            val zoomChange = event.calculateZoom()
            val rotationChange = event.calculateRotation()
            val panChange = event.calculatePan()

            if (activeId == null && pressed >= 2) activeId = cb.selectedId

            if (!pastSlop) {
                zoomAcc *= zoomChange
                rotationAcc += rotationChange
                panAcc += panChange
                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1f - zoomAcc) * centroidSize
                val rotationMotion = abs(rotationAcc * PI.toFloat() / 180f * centroidSize)
                val panMotion = panAcc.getDistance()
                if (zoomMotion > touchSlop || rotationMotion > touchSlop || panMotion > touchSlop) {
                    pastSlop = true
                    val id = activeId
                    if (id != null) {
                        started = true
                        cb.onGestureStart(id)
                        val current = event.calculateCentroid(useCurrent = true)
                        if (current != Offset.Unspecified) {
                            val pointer = primaryPointer(event.changes, current)
                            cb.onGestureUpdate(
                                prevCx = current.x - panAcc.x,
                                prevCy = current.y - panAcc.y,
                                cx = current.x,
                                cy = current.y,
                                zoom = zoomAcc,
                                rotationDeg = rotationAcc,
                                pointerX = pointer.x,
                                pointerY = pointer.y,
                            )
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                }
            } else if (started) {
                val previous = event.calculateCentroid(useCurrent = false)
                val current = event.calculateCentroid(useCurrent = true)
                if (previous != Offset.Unspecified && current != Offset.Unspecified &&
                    (zoomChange != 1f || rotationChange != 0f || panChange != Offset.Zero)
                ) {
                    val pointer = primaryPointer(event.changes, current)
                    cb.onGestureUpdate(
                        prevCx = previous.x,
                        prevCy = previous.y,
                        cx = current.x,
                        cy = current.y,
                        zoom = zoomChange,
                        rotationDeg = rotationChange,
                        pointerX = pointer.x,
                        pointerY = pointer.y,
                    )
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (!cancelled && event.changes.any { it.pressed })

        val id = activeId
        if (started && id != null) {
            cb.onGestureEnd(id, cancelled)
        } else if (!pastSlop && !cancelled && maxPointers == 1 && lastUptime - down.uptimeMillis < tapTimeout) {
            cb.onTap(down.position.x, down.position.y, hitId)
        }
    }
}

private fun primaryPointer(
    changes: List<androidx.compose.ui.input.pointer.PointerInputChange>,
    fallback: Offset,
): Offset = changes.firstOrNull { it.pressed }?.position ?: fallback
