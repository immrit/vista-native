package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTextBubbleLayoutTest {

    @Test
    fun `short text fits inline with footer in RTL`() {
        val result = calculateChatTextBubblePlacement(
            textWidth = 100,
            textHeight = 40,
            footerWidth = 60,
            footerHeight = 24,
            maxWidthPx = 500,
            spacingPx = 10,
            verticalGapPx = 2,
            layoutDirection = LayoutDirection.Rtl,
        )

        assertTrue(result.isInline)
        assertEquals(IntSize(170, 40), result.size)
        // In RTL inline mode: text is shifted to the right by footerWidth + spacing
        assertEquals(IntOffset(x = 70, y = 0), result.textOffset)
        // Footer is at left edge x = 0, aligned to bottom y = 40 - 24 = 16
        assertEquals(IntOffset(x = 0, y = 16), result.footerOffset)
    }

    @Test
    fun `short text fits inline with footer in LTR`() {
        val result = calculateChatTextBubblePlacement(
            textWidth = 100,
            textHeight = 40,
            footerWidth = 60,
            footerHeight = 24,
            maxWidthPx = 500,
            spacingPx = 10,
            verticalGapPx = 2,
            layoutDirection = LayoutDirection.Ltr,
        )

        assertTrue(result.isInline)
        assertEquals(IntSize(170, 40), result.size)
        // In LTR inline mode: text is on the left x = 0
        assertEquals(IntOffset(x = 0, y = 0), result.textOffset)
        // Footer is to the right of text x = 100 + 10 = 110, y = 40 - 24 = 16
        assertEquals(IntOffset(x = 110, y = 16), result.footerOffset)
    }

    @Test
    fun `wide text wraps footer vertically in RTL`() {
        val result = calculateChatTextBubblePlacement(
            textWidth = 450,
            textHeight = 120,
            footerWidth = 80,
            footerHeight = 25,
            maxWidthPx = 500,
            spacingPx = 10,
            verticalGapPx = 4,
            layoutDirection = LayoutDirection.Rtl,
        )

        assertFalse(result.isInline)
        assertEquals(IntSize(450, 149), result.size) // 120 + 4 + 25 = 149
        assertEquals(IntOffset(x = 0, y = 0), result.textOffset)
        // In RTL stacked mode: footer is pinned to the left edge x = 0
        assertEquals(IntOffset(x = 0, y = 124), result.footerOffset)
    }

    @Test
    fun `wide text wraps footer vertically in LTR`() {
        val result = calculateChatTextBubblePlacement(
            textWidth = 450,
            textHeight = 120,
            footerWidth = 80,
            footerHeight = 25,
            maxWidthPx = 500,
            spacingPx = 10,
            verticalGapPx = 4,
            layoutDirection = LayoutDirection.Ltr,
        )

        assertFalse(result.isInline)
        assertEquals(IntSize(450, 149), result.size)
        assertEquals(IntOffset(x = 0, y = 0), result.textOffset)
        // In LTR stacked mode: footer is pinned to the right edge x = 450 - 80 = 370
        assertEquals(IntOffset(x = 370, y = 124), result.footerOffset)
    }

    @Test
    fun `exact boundary fit allows inline placement`() {
        val result = calculateChatTextBubblePlacement(
            textWidth = 200,
            textHeight = 30,
            footerWidth = 90,
            footerHeight = 20,
            maxWidthPx = 300,
            spacingPx = 10, // 200 + 10 + 90 = 300 == maxWidth
            verticalGapPx = 2,
            layoutDirection = LayoutDirection.Rtl,
        )

        assertTrue(result.isInline)
        assertEquals(IntSize(300, 30), result.size)
    }

    @Test
    fun `text just exceeding boundary wraps vertically`() {
        val result = calculateChatTextBubblePlacement(
            textWidth = 201,
            textHeight = 30,
            footerWidth = 90,
            footerHeight = 20,
            maxWidthPx = 300,
            spacingPx = 10, // 201 + 10 + 90 = 301 > 300
            verticalGapPx = 3,
            layoutDirection = LayoutDirection.Rtl,
        )

        assertFalse(result.isInline)
        assertEquals(IntSize(201, 53), result.size) // 30 + 3 + 20 = 53
    }
}
