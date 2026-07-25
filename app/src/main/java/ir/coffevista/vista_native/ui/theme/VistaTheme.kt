package ir.coffevista.vista_native.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.R

object VistaColors {
    val Ink = Color(0xFF09243A)
    val Night = Color(0xFF071018)
    val SurfaceLight = Color(0xFFF7FBFD)
    val SurfaceDark = Color(0xFF0B1A25)
    val Cyan = Color(0xFF00A8D8)
    val CyanDeep = Color(0xFF0077B6)
    val Coral = Color(0xFFFF7168)
    val Navy = Color(0xFF274C9B)
    val MutedLight = Color(0xFF526A79)
    val MutedDark = Color(0xFFB8C7D1)
}

private val LightColors = lightColorScheme(
    primary = VistaColors.CyanDeep,
    onPrimary = Color.White,
    secondary = VistaColors.Coral,
    background = VistaColors.SurfaceLight,
    onBackground = VistaColors.Ink,
    surface = Color.White,
    onSurface = VistaColors.Ink,
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF64D7F2),
    onPrimary = Color(0xFF003544),
    secondary = Color(0xFFFFB4AE),
    background = VistaColors.Night,
    onBackground = Color(0xFFF0F7FA),
    surface = VistaColors.SurfaceDark,
    onSurface = Color(0xFFF0F7FA),
    error = Color(0xFFFFB4AB),
)

private val VistaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
)

private val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
    Font(R.font.vazirmatn_black, FontWeight.Black),
)

private val BaseTypography = Typography()
private val VistaTypography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = VazirmatnFontFamily),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = VazirmatnFontFamily),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = VazirmatnFontFamily),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = VazirmatnFontFamily),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = VazirmatnFontFamily),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = VazirmatnFontFamily),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = VazirmatnFontFamily),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = VazirmatnFontFamily),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = VazirmatnFontFamily),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = VazirmatnFontFamily),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = VazirmatnFontFamily),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = VazirmatnFontFamily),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = VazirmatnFontFamily),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = VazirmatnFontFamily),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = VazirmatnFontFamily),
)

@Composable
fun VistaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = VistaTypography,
        shapes = VistaShapes,
        content = content,
    )
}
