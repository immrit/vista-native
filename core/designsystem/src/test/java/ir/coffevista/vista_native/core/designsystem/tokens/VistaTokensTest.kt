package ir.coffevista.vista_native.core.designsystem.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ir.coffevista.vista_native.core.designsystem.R
import ir.coffevista.vista_native.core.designsystem.theme.VistaTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VistaTokensTest {
    @Test
    fun `brand palette matches approved Vista identity`() {
        assertEquals(Color(0xFF6366F1), VistaBrandColors.Indigo)
        assertEquals(Color(0xFF4F46E5), VistaBrandColors.IndigoDeep)
        assertEquals(Color(0xFF8B5CF6), VistaBrandColors.Violet)
        assertEquals(Color(0xFFEC4899), VistaBrandColors.Pink)
    }

    @Test
    fun `light semantic palette matches Flutter source`() {
        assertEquals(Color(0xFF0F1117), VistaSemanticPalettes.Light.contentPrimary)
        assertEquals(Color(0xFF6B7280), VistaSemanticPalettes.Light.contentSecondary)
        assertEquals(Color(0xFFE5E7EB), VistaSemanticPalettes.Light.divider)
        assertEquals(Color(0xFF10B981), VistaSemanticPalettes.Light.success)
        assertEquals(Color(0xFFF59E0B), VistaSemanticPalettes.Light.warning)
        assertEquals(Color(0xFF6366F1), VistaSemanticPalettes.Light.focus)
    }

    @Test
    fun `dark semantic palette matches Flutter source`() {
        assertEquals(Color(0xFFF0F0FF), VistaSemanticPalettes.Dark.contentPrimary)
        assertEquals(Color(0xFF8B8BAD), VistaSemanticPalettes.Dark.contentSecondary)
        assertEquals(Color(0xFF2A2A45), VistaSemanticPalettes.Dark.divider)
        assertEquals(Color(0xFF34D399), VistaSemanticPalettes.Dark.success)
        assertEquals(Color(0xFFFBBF24), VistaSemanticPalettes.Dark.warning)
        assertEquals(Color(0xFF6366F1), VistaSemanticPalettes.Dark.focus)
        assertEquals(Color(0xFF6E6E92), VistaSemanticPalettes.Dark.disabledContent)
    }

    @Test
    fun `Flutter geometry and motion tokens stay exact`() {
        assertEquals(12f, VistaRadii.Medium.value)
        assertEquals(24f, VistaSpacing.XXLarge.value)
        assertEquals(48f, VistaSpacing.Huge.value)
        assertEquals(51f, VistaComponentSize.Button.value)
        assertEquals(53f, VistaComponentSize.TextField.value)
        assertEquals(150, VistaMotionDuration.Fast)
        assertEquals(600, VistaMotionDuration.Emphasized)
    }

    @Test
    fun `Flutter typography mapping stays exact`() {
        assertEquals(22f, VistaTypography.headlineMedium.fontSize.value)
        assertEquals(27.5f, VistaTypography.headlineMedium.lineHeight.value)
        assertEquals(-0.2f, VistaTypography.headlineMedium.letterSpacing.value)
        assertEquals(16f, VistaTypography.bodyLarge.fontSize.value)
        assertEquals(24.8f, VistaTypography.bodyLarge.lineHeight.value)
        assertEquals(13f, VistaTypography.bodySmall.fontSize.value)
    }

    @Test
    fun `migrated auth logo resources are present`() {
        assertNotEquals(0, R.drawable.vista_auth_logo_light)
        assertNotEquals(0, R.drawable.vista_auth_logo_dark)
        assertNotEquals(0, R.font.vazirmatn_regular)
        assertNotEquals(0, R.font.vazirmatn_bold)
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
