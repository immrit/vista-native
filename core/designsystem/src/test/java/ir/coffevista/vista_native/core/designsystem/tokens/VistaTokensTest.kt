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
        assertEquals(Color(0xFF111215), VistaBrandColors.InkBlack)
        assertEquals(Color(0xFFF4F5F7), VistaBrandColors.InkWhite)
        assertEquals(Color(0xFF1FB6E8), VistaBrandColors.ViuBlue)
        assertEquals(Color(0xFF0097C7), VistaBrandColors.ViuBlueDeep)
        assertEquals(Color(0xFFF0543B), VistaBrandColors.Coral)
    }

    @Test
    fun `light semantic palette matches approved identity`() {
        assertEquals(Color(0xFF111215), VistaSemanticPalettes.Light.contentPrimary)
        assertEquals(Color(0xFF656C76), VistaSemanticPalettes.Light.contentSecondary)
        assertEquals(Color(0xFFE5E7EB), VistaSemanticPalettes.Light.divider)
        assertEquals(Color(0xFF10B981), VistaSemanticPalettes.Light.success)
        assertEquals(Color(0xFFF59E0B), VistaSemanticPalettes.Light.warning)
        assertEquals(VistaBrandColors.ViuBlueDeep, VistaSemanticPalettes.Light.focus)
    }

    @Test
    fun `dark semantic palette matches approved identity`() {
        assertEquals(Color(0xFFF3F4F6), VistaSemanticPalettes.Dark.contentPrimary)
        assertEquals(Color(0xFF8A919C), VistaSemanticPalettes.Dark.contentSecondary)
        assertEquals(Color(0xFF23272E), VistaSemanticPalettes.Dark.divider)
        assertEquals(Color(0xFF34D399), VistaSemanticPalettes.Dark.success)
        assertEquals(Color(0xFFFBBF24), VistaSemanticPalettes.Dark.warning)
        assertEquals(VistaBrandColors.ViuBlue, VistaSemanticPalettes.Dark.focus)
        assertEquals(Color(0xFF636973), VistaSemanticPalettes.Dark.disabledContent)
    }

    @Test
    fun `Flutter geometry and motion tokens stay exact`() {
        assertEquals(12f, VistaRadii.Medium.value)
        assertEquals(24f, VistaSpacing.XXLarge.value)
        assertEquals(48f, VistaSpacing.Huge.value)
        assertEquals(51f, VistaComponentSize.Button.value)
        assertEquals(53f, VistaComponentSize.TextField.value)
        assertEquals(150, VistaMotionDuration.Fast)
        assertEquals(250, VistaMotionDuration.Standard)
        assertEquals(400, VistaMotionDuration.Emphasized)
    }

    @Test
    fun `Flutter typography mapping stays exact`() {
        assertEquals(22f, VistaTypography.headlineMedium.fontSize.value)
        assertEquals(27.5f, VistaTypography.headlineMedium.lineHeight.value)
        assertEquals(-0.2f, VistaTypography.headlineMedium.letterSpacing.value)
        assertEquals(16f, VistaTypography.bodyLarge.fontSize.value)
        assertEquals(24.8f, VistaTypography.bodyLarge.lineHeight.value)
        assertEquals(13f, VistaTypography.bodySmall.fontSize.value)
        assertEquals(14f, VistaTypography.labelLarge.fontSize.value)
        assertEquals(19.6f, VistaTypography.labelLarge.lineHeight.value)
        assertEquals(0.2f, VistaTypography.labelMedium.letterSpacing.value)
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
        assertTrue(contrast(VistaSemanticPalettes.Dark.contentPrimary, Color(0xFF121417)) >= 4.5)
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
