package ir.coffevista.vista_native.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object VistaBrandColors {
    // Primary Monochrome Ink
    val InkBlack = Color(0xFF111215)
    val InkWhite = Color(0xFFF4F5F7)

    // Signature Viu Blue (Viu Sky & Deep Cyan)
    val ViuBlue = Color(0xFF1FB6E8)
    val ViuBlueDeep = Color(0xFF0097C7)
    val ViuBlueRich = Color(0xFF007EA8)
    val ViuBlueSurfaceDark = Color(0xFF123442)
    val ViuBlueSurfaceLight = Color(0xFFE3F6FC)

    // Warm Coral (Likes, Stories, Notifications, Emotional Accents)
    val Coral = Color(0xFFF0543B)
    val CoralDeep = Color(0xFFE64E33)
    val CoralSoft = Color(0xFFFF6B4A)
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
    val viuBlue: Color,
    val onViuBlue: Color,
    val viuBlueContainer: Color,
    val coral: Color,
    val onCoral: Color,
    val coralContainer: Color,
    val glassTint: Color,
    val glassHaloTint: Color,
)

object VistaSemanticPalettes {
    val Light = VistaSemanticColors(
        contentPrimary = Color(0xFF111215),
        contentSecondary = Color(0xFF656C76),
        divider = Color(0xFFE5E7EB),
        overlay = Color(0x99090A0C),
        success = Color(0xFF10B981),
        onSuccess = Color(0xFF0F1117),
        warning = Color(0xFFF59E0B),
        onWarning = Color(0xFF0F1117),
        focus = VistaBrandColors.ViuBlueDeep,
        pressedOverlay = Color(0x14000000),
        disabledContent = Color(0xFF9AA1AB),
        disabledContainer = Color(0xFFE5E7EB),
        viuBlue = VistaBrandColors.ViuBlueDeep,
        onViuBlue = Color.White,
        viuBlueContainer = VistaBrandColors.ViuBlueSurfaceLight,
        coral = VistaBrandColors.CoralDeep,
        onCoral = Color.White,
        coralContainer = Color(0xFFFDEBE8),
        glassTint = Color.White,
        glassHaloTint = Color(0xFFF7F8FA),
    )

    val Dark = VistaSemanticColors(
        contentPrimary = Color(0xFFF3F4F6),
        contentSecondary = Color(0xFF8A919C),
        divider = Color(0xFF23272E),
        overlay = Color(0xB3090A0C),
        success = Color(0xFF34D399),
        onSuccess = Color(0xFF022C22),
        warning = Color(0xFFFBBF24),
        onWarning = Color(0xFF451A03),
        focus = VistaBrandColors.ViuBlue,
        pressedOverlay = Color(0x1FFFFFFF),
        disabledContent = Color(0xFF636973),
        disabledContainer = Color(0xFF1F2329),
        viuBlue = VistaBrandColors.ViuBlue,
        onViuBlue = Color(0xFF041820),
        viuBlueContainer = VistaBrandColors.ViuBlueSurfaceDark,
        coral = VistaBrandColors.Coral,
        onCoral = Color.White,
        coralContainer = Color(0xFF381814),
        glassTint = Color(0xFF16191E),
        glassHaloTint = Color(0xFF0E1115),
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
    const val Standard = 250
    const val Emphasized = 400
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
