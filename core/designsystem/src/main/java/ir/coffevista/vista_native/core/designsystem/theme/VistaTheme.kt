package ir.coffevista.vista_native.core.designsystem.theme

import android.app.Activity
import android.os.Build
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import ir.coffevista.vista_native.core.designsystem.R
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaMotionDuration
import ir.coffevista.vista_native.core.designsystem.tokens.VistaRadii
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSemanticColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSemanticPalettes

private val VistaLightColorScheme = lightColorScheme(
    primary = VistaBrandColors.Indigo,
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
    outline = Color(0xFFE5E7EB),
    outlineVariant = VistaSemanticPalettes.Light.divider,
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val VistaDarkColorScheme = darkColorScheme(
    primary = VistaBrandColors.Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E2E6E),
    onPrimaryContainer = Color(0xFFF0F0FF),
    secondary = VistaBrandColors.Violet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E2E6E),
    onSecondaryContainer = Color(0xFFF0F0FF),
    tertiary = VistaBrandColors.Pink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF252540),
    onTertiaryContainer = Color(0xFFF0F0FF),
    background = Color(0xFF09090F),
    onBackground = VistaSemanticPalettes.Dark.contentPrimary,
    surface = Color(0xFF13131E),
    onSurface = VistaSemanticPalettes.Dark.contentPrimary,
    surfaceVariant = Color(0xFF1C1C2E),
    onSurfaceVariant = VistaSemanticPalettes.Dark.contentSecondary,
    outline = Color(0xFF2A2A45),
    outlineVariant = VistaSemanticPalettes.Dark.divider,
    error = Color(0xFFF87171),
    onError = Color(0xFF09090F),
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

private fun vistaTextStyle(
    size: Int,
    weight: FontWeight,
    lineHeight: Float,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = VistaFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = letterSpacing.sp,
)

internal val VistaTypography = Typography(
    displayLarge = vistaTextStyle(32, FontWeight.ExtraBold, 1.15f, -0.4f),
    displayMedium = vistaTextStyle(32, FontWeight.ExtraBold, 1.15f, -0.4f),
    displaySmall = vistaTextStyle(26, FontWeight.ExtraBold, 1.2f, -0.4f),
    headlineLarge = vistaTextStyle(26, FontWeight.ExtraBold, 1.2f, -0.4f),
    headlineMedium = vistaTextStyle(22, FontWeight.Bold, 1.25f, -0.2f),
    headlineSmall = vistaTextStyle(18, FontWeight.SemiBold, 1.3f, -0.2f),
    titleLarge = vistaTextStyle(18, FontWeight.Bold, 1.3f),
    titleMedium = vistaTextStyle(16, FontWeight.SemiBold, 1.35f),
    titleSmall = vistaTextStyle(14, FontWeight.SemiBold, 1.4f),
    bodyLarge = vistaTextStyle(16, FontWeight.Normal, 1.55f),
    bodyMedium = vistaTextStyle(14, FontWeight.Normal, 1.5f),
    bodySmall = vistaTextStyle(13, FontWeight.Normal, 1.45f),
    labelLarge = vistaTextStyle(14, FontWeight.SemiBold, 1.4f),
    labelMedium = vistaTextStyle(12, FontWeight.Medium, 1.4f, 0.2f),
    labelSmall = vistaTextStyle(11, FontWeight.Medium, 1.4f, 0.2f),
)

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

/** Persisted Flutter-compatible mode for newly presented chat messages. */
val LocalChatEntryMode = staticCompositionLocalOf { "adaptive" }

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
    reduceMotion: Boolean = false,
    chatEntryMode: String = "adaptive",
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
    val motion = if (animationsEnabled && !reduceMotion) {
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
        LocalChatEntryMode provides chatEntryMode,
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
    val statusBarColor = if (darkTheme) Color(0xFF13131E) else Color.White
    val navigationBarColor = if (darkTheme) Color(0xFF09090F) else Color.White
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            run {
                window.statusBarColor = statusBarColor.toArgb()
                window.navigationBarColor = navigationBarColor.toArgb()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.navigationBarDividerColor = Color.Transparent.toArgb()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                }
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}
