package ir.coffevista.vista_native.features.chat.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Resolves dark theme from the active MaterialTheme color scheme so Vista's
 * in-app light/dark toggle governs chat surfaces consistently.
 */
@Composable
@ReadOnlyComposable
internal fun isChatDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * Threads-inspired monochrome chat surface & bubble tokens harmonized with
 * Vista's warm sandstone light and deep charcoal dark doodle wallpapers.
 */
internal object VistaChatPalette {
    fun backdropFallback(isDark: Boolean): Color =
        if (isDark) Color(0xFF0D0E10) else Color(0xFFF5F2EE)

    fun wallpaperScrim(isDark: Boolean): Color =
        if (isDark) Color(0xFF0A0B0D).copy(alpha = 0.36f) else Color(0xFFFAF9F7).copy(alpha = 0.42f)

    fun outgoingBubbleColors(isDark: Boolean): List<Color> =
        if (isDark) {
            listOf(Color(0xFF3B3E44), Color(0xFF33353A))
        } else {
            listOf(Color(0xFF191B1F), Color(0xFF121316))
        }

    fun outgoingBubbleBrush(isDark: Boolean): Brush =
        Brush.verticalGradient(outgoingBubbleColors(isDark))

    fun outgoingBubbleSolid(isDark: Boolean): Color =
        if (isDark) Color(0xFF36383D) else Color(0xFF141518)

    fun outgoingBubbleBorder(isDark: Boolean): Color =
        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Transparent

    fun incomingBubbleBg(isDark: Boolean): Color =
        if (isDark) Color(0xFF1C1E22) else Color(0xFFFFFFFF)

    fun incomingBubbleBorder(isDark: Boolean): Color =
        if (isDark) Color.White.copy(alpha = 0.07f) else Color(0x14000000)

    fun outgoingNestedCardBg(isDark: Boolean): Color =
        if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.11f)

    fun incomingNestedCardBg(isDark: Boolean): Color =
        if (isDark) Color(0xFF23262B) else Color(0xFFF3F4F6)

    fun incomingAccent(isDark: Boolean): Color =
        if (isDark) Color(0xFF36383D) else Color(0xFF141518)

    fun incomingActiveWave(isDark: Boolean): Color =
        if (isDark) Color(0xFFE5E7EB) else Color(0xFF141518)

    fun composerCapsuleTint(isDark: Boolean): Color =
        if (isDark) Color(0xFF1A1C20) else Color(0xFFFFFFFF)

    fun composerIconTint(isDark: Boolean): Color =
        if (isDark) Color(0xFF8A919C) else Color(0xFF656C76)

    fun topBarTint(isDark: Boolean): Color =
        if (isDark) Color(0xFF16181C) else Color(0xFFFFFFFF)
}
