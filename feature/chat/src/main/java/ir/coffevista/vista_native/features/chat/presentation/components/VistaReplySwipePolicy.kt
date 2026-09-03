package ir.coffevista.vista_native.features.chat.presentation.components

import kotlin.math.abs

/**
 * Keeps reply-swipe arbitration deterministic and independent from Compose
 * pointer-detector implementation details.
 */
internal data class ReplySwipePolicy(
    val thresholdPx: Float,
    val touchSlopPx: Float,
    val bubbleOnRight: Boolean,
) {
    init {
        require(thresholdPx > 0f)
        require(touchSlopPx >= 0f)
    }

    private val direction: Float
        get() = if (bubbleOnRight) -1f else 1f

    fun accepts(horizontalDeltaPx: Float, verticalDeltaPx: Float): Boolean =
        abs(horizontalDeltaPx) > touchSlopPx &&
            abs(horizontalDeltaPx) > abs(verticalDeltaPx) &&
            horizontalDeltaPx * direction > 0f

    /**
     * [detectHorizontalDragGestures] has already won pointer arbitration and
     * crossed Compose's touch slop before it invokes its drag callback. Do not
     * apply [touchSlopPx] to every subsequent, frequently 1–3px, drag delta:
     * that makes a legitimate reply gesture appear inert on physical devices.
     */
    fun shouldConsumeDrag(currentOffsetPx: Float, horizontalDeltaPx: Float): Boolean =
        currentOffsetPx > 0f || horizontalDeltaPx * direction > 0f

    fun nextOffset(currentOffsetPx: Float, horizontalDeltaPx: Float): Float =
        if (currentOffsetPx > 0f || horizontalDeltaPx * direction > 0f) {
            (currentOffsetPx + horizontalDeltaPx * direction * 0.6f)
                .coerceIn(0f, thresholdPx * 1.7f)
        } else {
            currentOffsetPx
        }

    fun shouldTrigger(offsetPx: Float): Boolean = offsetPx >= thresholdPx
}
