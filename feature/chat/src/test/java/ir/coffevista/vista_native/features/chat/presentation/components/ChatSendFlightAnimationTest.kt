package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSendFlightAnimationTest {

    @Test
    fun `outgoing fallback target docks against physical right edge`() {
        val target = resolveSendFlightTargetFallbackBounds(
            rootWidthPx = 1080f,
            rootHeightPx = 2200f,
            composerHeightPx = 180f,
            estimatedBubbleWidthPx = 300f,
            estimatedBubbleHeightPx = 110f,
            alignToRight = true,
            horizontalMarginPx = 36f,
            bottomMarginPx = 24f,
        )

        assertEquals(1044f, target.right, 0.01f)
        assertEquals(744f, target.left, 0.01f)
        assertTrue("Outgoing bubble must land in right half of screen", target.left > 540f)
        assertEquals(1996f, target.bottom, 0.01f)
    }

    @Test
    fun `left-aligned fallback target docks against physical left edge`() {
        val target = resolveSendFlightTargetFallbackBounds(
            rootWidthPx = 1080f,
            rootHeightPx = 2200f,
            composerHeightPx = 180f,
            estimatedBubbleWidthPx = 300f,
            estimatedBubbleHeightPx = 110f,
            alignToRight = false,
            horizontalMarginPx = 36f,
            bottomMarginPx = 24f,
        )

        assertEquals(36f, target.left, 0.01f)
        assertEquals(336f, target.right, 0.01f)
        assertTrue("Left-aligned bubble must land in left half of screen", target.right < 540f)
    }

    @Test
    fun `start bounds anchor short text on RTL right or LTR left inside composer`() {
        val composerInput = Rect(left = 150f, top = 2040f, right = 920f, bottom = 2150f)

        val rtlStart = resolveSendFlightStartBounds(
            inputBoundsLocal = composerInput,
            estimatedBubbleWidthPx = 240f,
            isRtlText = true,
        )
        assertEquals(920f, rtlStart.right, 0.01f)
        assertEquals(680f, rtlStart.left, 0.01f)

        val ltrStart = resolveSendFlightStartBounds(
            inputBoundsLocal = composerInput,
            estimatedBubbleWidthPx = 240f,
            isRtlText = false,
        )
        assertEquals(150f, ltrStart.left, 0.01f)
        assertEquals(390f, ltrStart.right, 0.01f)
    }

    @Test
    fun `flight interpolation moves continuously toward right-aligned outgoing target`() {
        val start = Rect(left = 680f, top = 2040f, right = 920f, bottom = 2150f)
        val target = Rect(left = 744f, top = 1880f, right = 1044f, bottom = 1990f)

        val mid = interpolateSendFlightRect(start, target, 0.5f)
        val end = interpolateSendFlightRect(start, target, 1f)

        assertTrue(mid.right > start.right && mid.right < target.right)
        assertTrue(mid.top < start.top && mid.top > target.top)
        assertEquals(target.left, end.left, 0.01f)
        assertEquals(target.right, end.right, 0.01f)
        assertEquals(target.top, end.top, 0.01f)
        assertEquals(target.bottom, end.bottom, 0.01f)
    }
}
