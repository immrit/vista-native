package ir.coffevista.vista_native.features.chat.presentation.components

import kotlin.math.abs

/**
 * Keeps reply-swipe arbitration deterministic and independent from Compose
 * pointer-detector implementation details.
 * Telegram gesture: swiping to the left triggers reply for all messages.
 */
internal data class ReplySwipePolicy(
    val thresholdPx: Float,
    val touchSlopPx: Float,
    val bubbleOnRight: Boolean = false,
) {
    init {
        require(thresholdPx > 0f)
        require(touchSlopPx >= 0f)
    }

    fun accepts(horizontalDeltaPx: Float, verticalDeltaPx: Float): Boolean =
        abs(horizontalDeltaPx) > touchSlopPx &&
            abs(horizontalDeltaPx) > abs(verticalDeltaPx) &&
            horizontalDeltaPx < 0f

    /**
     * [detectHorizontalDragGestures] has already won pointer arbitration.
     * Consumes leftward drag (horizontalDeltaPx < 0) or ongoing drag recovery.
     */
    fun shouldConsumeDrag(currentOffsetPx: Float, horizontalDeltaPx: Float): Boolean =
        currentOffsetPx > 0f || horizontalDeltaPx < 0f

    fun nextOffset(currentOffsetPx: Float, horizontalDeltaPx: Float): Float =
        if (currentOffsetPx > 0f || horizontalDeltaPx < 0f) {
            (currentOffsetPx - horizontalDeltaPx * 0.6f)
                .coerceIn(0f, thresholdPx * 1.7f)
        } else {
            currentOffsetPx
        }

    fun shouldTrigger(offsetPx: Float): Boolean = offsetPx >= thresholdPx
}
