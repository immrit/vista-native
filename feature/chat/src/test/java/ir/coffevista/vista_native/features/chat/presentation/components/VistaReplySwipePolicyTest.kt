package ir.coffevista.vista_native.features.chat.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VistaReplySwipePolicyTest {
    private val incoming = ReplySwipePolicy(
        thresholdPx = 72f,
        touchSlopPx = 8f,
        bubbleOnRight = false,
    )
    private val outgoing = incoming.copy(bubbleOnRight = true)

    @Test
    fun `vertical movement never captures reply gesture`() {
        assertFalse(incoming.accepts(horizontalDeltaPx = 18f, verticalDeltaPx = 24f))
        assertFalse(incoming.accepts(horizontalDeltaPx = 7f, verticalDeltaPx = 0f))
    }

    @Test
    fun `incoming bubble accepts only leftward horizontal movement`() {
        assertTrue(incoming.accepts(horizontalDeltaPx = -20f, verticalDeltaPx = 2f))
        assertFalse(incoming.accepts(horizontalDeltaPx = 20f, verticalDeltaPx = 2f))
    }

    @Test
    fun `outgoing bubble accepts only rightward horizontal movement`() {
        assertTrue(outgoing.accepts(horizontalDeltaPx = 20f, verticalDeltaPx = 2f))
        assertFalse(outgoing.accepts(horizontalDeltaPx = -20f, verticalDeltaPx = 2f))
    }

    @Test
    fun `offset is clamped and trigger is threshold based`() {
        assertEquals(43.2f, incoming.nextOffset(0f, -72f), 0.01f)
        assertEquals(122.4f, incoming.nextOffset(100f, -100f), 0.01f)
        assertTrue(incoming.shouldTrigger(72f))
        assertFalse(incoming.shouldTrigger(71.99f))
    }

    @Test
    fun `opposite movement cannot pull an idle bubble into reply`() {
        assertEquals(0f, outgoing.nextOffset(0f, -32f), 0.01f)
    }

    @Test
    fun `post arbitration micro deltas begin a reply swipe in the correct direction`() {
        // Compose has already crossed touch slop when onHorizontalDrag is
        // called, so individual callback deltas must not need to cross it too.
        assertTrue(incoming.shouldConsumeDrag(currentOffsetPx = 0f, horizontalDeltaPx = -1f))
        assertTrue(outgoing.shouldConsumeDrag(currentOffsetPx = 0f, horizontalDeltaPx = 1f))
        assertFalse(incoming.shouldConsumeDrag(currentOffsetPx = 0f, horizontalDeltaPx = 1f))
        assertFalse(outgoing.shouldConsumeDrag(currentOffsetPx = 0f, horizontalDeltaPx = -1f))
    }

    @Test
    fun `a captured reply swipe continues to consume its return motion`() {
        assertTrue(incoming.shouldConsumeDrag(currentOffsetPx = 12f, horizontalDeltaPx = 2f))
    }
}
