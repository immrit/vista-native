package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatImageEditorHelperTest {

    @Test
    fun `default ImageEditState is not modified`() {
        val state = ImageEditState()
        assertFalse(state.isModified)
    }

    @Test
    fun `rotation marks ImageEditState as modified`() {
        val state90 = ImageEditState(rotationDegrees = 90f)
        assertTrue(state90.isModified)

        val state360 = ImageEditState(rotationDegrees = 360f)
        assertFalse(state360.isModified)
    }

    @Test
    fun `crop marks ImageEditState as modified`() {
        val cropped = ImageEditState(cropRectNorm = NormalizedCropRect(0.1f, 0.1f, 0.9f, 0.9f))
        assertTrue(cropped.isModified)
    }

    @Test
    fun `drawing stroke marks ImageEditState as modified`() {
        val stroke = ChatDrawingStroke(
            colorArgb = 0xFFFF0000.toInt(),
            strokeWidthNorm = 0.01f,
            points = listOf(Offset(0.2f, 0.2f), Offset(0.3f, 0.4f)),
        )
        val stateWithStrokes = ImageEditState(strokes = listOf(stroke))
        assertTrue(stateWithStrokes.isModified)
    }
}
