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
    fun `short single-word text stacks footer vertically below content in RTL`() {
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

        assertFalse(result.isInline)
        assertEquals(IntSize(100, 66), result.size) // 40 + 2 + 24 = 66
        assertEquals(IntOffset(x = 0, y = 0), result.textOffset)
        assertEquals(IntOffset(x = 40, y = 42), result.footerOffset) // 100 - 60 = 40 (فوتر همواره در گوشه راست)
    }

    @Test
    fun `short single-word text stacks footer vertically below content in LTR`() {
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

        assertFalse(result.isInline)
        assertEquals(IntSize(100, 66), result.size)
        assertEquals(IntOffset(x = 0, y = 0), result.textOffset)
        assertEquals(IntOffset(x = 40, y = 42), result.footerOffset) // 100 - 60 = 40
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
        assertEquals(IntOffset(x = 370, y = 124), result.footerOffset) // 450 - 80 = 370 (فوتر همواره در گوشه راست)
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
        assertEquals(IntOffset(x = 370, y = 124), result.footerOffset)
    }

    @Test
    fun `tiny text smaller than footer expands width to footerWidth and stacks vertically in RTL`() {
        val result = calculateChatTextBubblePlacement(
            textWidth = 40,
            textHeight = 30,
            footerWidth = 90,
            footerHeight = 20,
            maxWidthPx = 300,
            spacingPx = 10,
            verticalGapPx = 2,
            layoutDirection = LayoutDirection.Rtl,
        )

        assertFalse(result.isInline)
        assertEquals(IntSize(90, 52), result.size) // 30 + 2 + 20 = 52
        assertEquals(IntOffset(x = 50, y = 0), result.textOffset) // 90 - 40 = 50 in RTL
        assertEquals(IntOffset(x = 0, y = 32), result.footerOffset) // 90 - 90 = 0
    }

    @Test
    fun `tiny text smaller than footer expands width to footerWidth and stacks vertically in LTR`() {
        val result = calculateChatTextBubblePlacement(
            textWidth = 40,
            textHeight = 30,
            footerWidth = 90,
            footerHeight = 20,
            maxWidthPx = 300,
            spacingPx = 10,
            verticalGapPx = 2,
            layoutDirection = LayoutDirection.Ltr,
        )

        assertFalse(result.isInline)
        assertEquals(IntSize(90, 52), result.size)
        assertEquals(IntOffset(x = 0, y = 0), result.textOffset) // starts at 0 in LTR
        assertEquals(IntOffset(x = 0, y = 32), result.footerOffset) // 90 - 90 = 0
    }

    @Test
    fun `text exceeding boundary wraps vertically`() {
        val result = calculateChatTextBubblePlacement(
            textWidth = 201,
            textHeight = 30,
            footerWidth = 90,
            footerHeight = 20,
            maxWidthPx = 300,
            spacingPx = 10,
            verticalGapPx = 3,
            layoutDirection = LayoutDirection.Rtl,
        )

        assertFalse(result.isInline)
        assertEquals(IntSize(201, 53), result.size) // 30 + 3 + 20 = 53
    }
}
