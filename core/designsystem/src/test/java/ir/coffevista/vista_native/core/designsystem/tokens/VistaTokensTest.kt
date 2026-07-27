package ir.coffevista.vista_native.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VistaTokensTest {
    @Test
    fun `brand palette matches approved Vista identity`() {
        assertEquals(Color(0xFF6366F1), VistaBrandColors.Indigo)
        assertEquals(Color(0xFF8B5CF6), VistaBrandColors.Violet)
        assertEquals(Color(0xFFEC4899), VistaBrandColors.Pink)
    }

    @Test
    fun `spacing scale remains monotonic`() {
        val values = listOf(
            VistaSpacing.None,
            VistaSpacing.Micro,
            VistaSpacing.XSmall,
            VistaSpacing.Small,
            VistaSpacing.Medium,
            VistaSpacing.Large,
            VistaSpacing.XLarge,
            VistaSpacing.XXLarge,
            VistaSpacing.XXXLarge,
            VistaSpacing.Huge,
            VistaSpacing.Giant,
        )
        assertEquals(values, values.sorted())
    }

    @Test
    fun `semantic content contrast meets normal text threshold`() {
        assertTrue(contrast(VistaSemanticPalettes.Light.contentPrimary, Color.White) >= 4.5)
        assertTrue(contrast(VistaSemanticPalettes.Dark.contentPrimary, Color(0xFF13131E)) >= 4.5)
        assertTrue(contrast(VistaSemanticPalettes.Light.onSuccess, VistaSemanticPalettes.Light.success) >= 4.5)
        assertTrue(contrast(VistaSemanticPalettes.Dark.onWarning, VistaSemanticPalettes.Dark.warning) >= 4.5)
    }

    @Test
    fun `touch target is at least forty eight dp`() {
        assertTrue(VistaComponentSize.TouchTarget.value >= 48f)
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val lighter = maxOf(foreground.luminance(), background.luminance()).toDouble()
        val darker = minOf(foreground.luminance(), background.luminance()).toDouble()
        return (lighter + 0.05) / (darker + 0.05)
    }
}
