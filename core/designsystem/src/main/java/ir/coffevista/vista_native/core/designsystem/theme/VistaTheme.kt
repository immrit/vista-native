package ir.coffevista.vista_native.core.designsystem.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import ir.coffevista.vista_native.core.designsystem.R
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaMotionDuration
import ir.coffevista.vista_native.core.designsystem.tokens.VistaRadii
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSemanticColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSemanticPalettes

private val VistaLightColorScheme = lightColorScheme(
    primary = VistaBrandColors.IndigoDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = VistaBrandColors.VioletDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = VistaBrandColors.PinkDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF831843),
    background = Color(0xFFF8F9FF),
    onBackground = VistaSemanticPalettes.Light.contentPrimary,
    surface = Color.White,
    onSurface = VistaSemanticPalettes.Light.contentPrimary,
    surfaceVariant = Color(0xFFF3F4FF),
    onSurfaceVariant = VistaSemanticPalettes.Light.contentSecondary,
    outline = Color(0xFFA1A1AA),
    outlineVariant = VistaSemanticPalettes.Light.divider,
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val VistaDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFC4B5FD),
    onSecondary = Color(0xFF2E1065),
    secondaryContainer = Color(0xFF5B21B6),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFFF9A8D4),
    onTertiary = Color(0xFF500724),
    tertiaryContainer = Color(0xFF9D174D),
    onTertiaryContainer = Color(0xFFFCE7F3),
    background = Color(0xFF09090F),
    onBackground = VistaSemanticPalettes.Dark.contentPrimary,
    surface = Color(0xFF13131E),
    onSurface = VistaSemanticPalettes.Dark.contentPrimary,
    surfaceVariant = Color(0xFF1C1C2E),
    onSurfaceVariant = VistaSemanticPalettes.Dark.contentSecondary,
    outline = Color(0xFF71717A),
    outlineVariant = VistaSemanticPalettes.Dark.divider,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

private val VistaShapes = Shapes(
    extraSmall = RoundedCornerShape(VistaRadii.Small),
    small = RoundedCornerShape(VistaRadii.Medium),
    medium = RoundedCornerShape(VistaRadii.Large),
    large = RoundedCornerShape(VistaRadii.XXLarge),
    extraLarge = RoundedCornerShape(VistaRadii.Hero),
)

val VistaFontFamily = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
    Font(R.font.vazirmatn_black, FontWeight.Black),
)

private fun Typography.withVistaFont(): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = VistaFontFamily),
    displayMedium = displayMedium.copy(fontFamily = VistaFontFamily),
    displaySmall = displaySmall.copy(fontFamily = VistaFontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = VistaFontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = VistaFontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = VistaFontFamily),
    titleLarge = titleLarge.copy(fontFamily = VistaFontFamily),
    titleMedium = titleMedium.copy(fontFamily = VistaFontFamily),
    titleSmall = titleSmall.copy(fontFamily = VistaFontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = VistaFontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = VistaFontFamily),
    bodySmall = bodySmall.copy(fontFamily = VistaFontFamily),
    labelLarge = labelLarge.copy(fontFamily = VistaFontFamily),
    labelMedium = labelMedium.copy(fontFamily = VistaFontFamily),
    labelSmall = labelSmall.copy(fontFamily = VistaFontFamily),
)

private val VistaTypography = Typography().withVistaFont()

@Immutable
data class VistaMotion(
    val fast: Int,
    val standard: Int,
    val emphasized: Int,
)

private val LocalVistaSemanticColors = staticCompositionLocalOf { VistaSemanticPalettes.Light }
private val LocalVistaMotion = staticCompositionLocalOf {
    VistaMotion(
        fast = VistaMotionDuration.Fast,
        standard = VistaMotionDuration.Standard,
        emphasized = VistaMotionDuration.Emphasized,
    )
}

val MaterialTheme.vistaColors: VistaSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalVistaSemanticColors.current

val MaterialTheme.vistaMotion: VistaMotion
    @Composable
    @ReadOnlyComposable
    get() = LocalVistaMotion.current

@Composable
fun VistaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val animationsEnabled = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }.getOrDefault(true)
    val motion = if (animationsEnabled) {
        VistaMotion(
            fast = VistaMotionDuration.Fast,
            standard = VistaMotionDuration.Standard,
            emphasized = VistaMotionDuration.Emphasized,
        )
    } else {
        VistaMotion(
            fast = VistaMotionDuration.Instant,
            standard = VistaMotionDuration.Instant,
            emphasized = VistaMotionDuration.Instant,
        )
    }

    VistaSystemBars(darkTheme)
    CompositionLocalProvider(
        LocalVistaSemanticColors provides if (darkTheme) {
            VistaSemanticPalettes.Dark
        } else {
            VistaSemanticPalettes.Light
        },
        LocalVistaMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) VistaDarkColorScheme else VistaLightColorScheme,
            typography = VistaTypography,
            shapes = VistaShapes,
            content = content,
        )
    }
}

@Composable
private fun VistaSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}
