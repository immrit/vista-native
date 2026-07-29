package ir.coffevista.vista_native.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object VistaBrandColors {
    val Indigo = Color(0xFF6366F1)
    val IndigoDeep = Color(0xFF4F46E5)
    val Violet = Color(0xFF8B5CF6)
    val VioletDeep = Color(0xFF7C3AED)
    val Pink = Color(0xFFEC4899)
    val PinkDeep = Color(0xFFDB2777)
}

@Immutable
data class VistaSemanticColors(
    val contentPrimary: Color,
    val contentSecondary: Color,
    val divider: Color,
    val overlay: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val focus: Color,
    val pressedOverlay: Color,
    val disabledContent: Color,
    val disabledContainer: Color,
)

object VistaSemanticPalettes {
    val Light = VistaSemanticColors(
        contentPrimary = Color(0xFF0F1117),
        contentSecondary = Color(0xFF6B7280),
        divider = Color(0xFFE5E7EB),
        overlay = Color(0x9909090F),
        success = Color(0xFF10B981),
        onSuccess = Color(0xFF0F1117),
        warning = Color(0xFFF59E0B),
        onWarning = Color(0xFF0F1117),
        focus = VistaBrandColors.Indigo,
        pressedOverlay = Color(0x14000000),
        disabledContent = Color(0xFF9CA3AF),
        disabledContainer = Color(0xFFE5E7EB),
    )

    val Dark = VistaSemanticColors(
        contentPrimary = Color(0xFFF0F0FF),
        contentSecondary = Color(0xFF8B8BAD),
        divider = Color(0xFF2A2A45),
        overlay = Color(0xB309090F),
        success = Color(0xFF34D399),
        onSuccess = Color(0xFF022C22),
        warning = Color(0xFFFBBF24),
        onWarning = Color(0xFF451A03),
        focus = VistaBrandColors.Indigo,
        pressedOverlay = Color(0x1FFFFFFF),
        disabledContent = Color(0xFF6E6E92),
        disabledContainer = Color(0xFF252540),
    )
}

object VistaSpacing {
    val None = 0.dp
    val Micro = 2.dp
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 20.dp
    val XXLarge = 24.dp
    val XXXLarge = 32.dp
    val Huge = 48.dp
    val Giant = 64.dp
}

object VistaRadii {
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 20.dp
    val XXLarge = 24.dp
    val Hero = 28.dp
    val Pill = 999.dp
}

object VistaElevation {
    val None = 0.dp
    val Low = 2.dp
    val Medium = 6.dp
    val High = 12.dp
}

object VistaBorder {
    val Hairline = 1.dp
    val Strong = 2.dp
}

object VistaIconSize {
    val Small = 18.dp
    val Medium = 24.dp
    val Large = 32.dp
}

object VistaMotionDuration {
    const val Instant = 0
    const val Fast = 150
    const val Standard = 300
    const val Emphasized = 600
}

object VistaAlpha {
    const val Disabled = 0.38f
    const val Secondary = 0.66f
    const val Subtle = 0.12f
}

object VistaComponentSize {
    val Button = 51.dp
    val TextField = 53.dp
    val NavigationBar = 72.dp
    val TouchTarget = 48.dp
    val Avatar = 44.dp
}

object VistaLayout {
    val ScreenHorizontal = 24.dp
    val ScreenVertical = 20.dp
    val ContentMaxWidth = 640.dp
}
