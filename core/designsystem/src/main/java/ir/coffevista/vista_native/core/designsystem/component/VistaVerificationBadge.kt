package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.R

/**
 * انواع نشان‌های رسمی تأیید هویت در پلتفرم ویستا (هماهنگ با نسخه فلاتر و بک‌اند).
 */
enum class ResolvedVerificationBadgeType {
    NONE,
    BLUE_TICK,
    GOLD_TICK,
    BLACK_TICK,
}

object VerificationBadgeDefaults {
    // Official Instagram Verified Blue for standard blue-tick badges.
    val Blue = Color(0xFF0095F6)
    val Gold = Color(0xFFFFB300)
    val BlackLight = Color(0xFF1A1A1A)
    val BlackDark = Color(0xFFFFFFFF)
}

/**
 * تحلیل مقدار خام نوع تأیید هویت دریافتی از سرور.
 */
fun parseVerificationBadgeType(raw: String?, role: String? = null): ResolvedVerificationBadgeType {
    val normalizedRole = role?.trim()?.lowercase()
    if (normalizedRole == "admin" || normalizedRole == "moderator") {
        return ResolvedVerificationBadgeType.BLUE_TICK
    }
    if (raw.isNullOrBlank()) {
        return if (normalizedRole == "premium") ResolvedVerificationBadgeType.GOLD_TICK else ResolvedVerificationBadgeType.NONE
    }
    val value = if (raw.contains('.')) raw.substringAfterLast('.') else raw
    val normalized = value.lowercase().replace(Regex("[^a-z]"), "")
    return when (normalized) {
        "blue", "bluetick", "official", "identity" -> ResolvedVerificationBadgeType.BLUE_TICK
        "gold", "goldtick", "premium", "vip" -> ResolvedVerificationBadgeType.GOLD_TICK
        "black", "blacktick", "dark", "creator", "founder" -> ResolvedVerificationBadgeType.BLACK_TICK
        else -> if (normalizedRole == "premium") ResolvedVerificationBadgeType.GOLD_TICK else ResolvedVerificationBadgeType.NONE
    }
}

/**
 * تصمیم‌گیری درباره نوع نشان تأیید هویت بر اساس وضعیت کاربر.
 */
fun resolveVerificationBadgeType(
    isVerified: Boolean,
    verificationType: String? = null,
    role: String? = null,
): ResolvedVerificationBadgeType {
    if (!isVerified) return ResolvedVerificationBadgeType.NONE
    val parsed = parseVerificationBadgeType(verificationType)
    if (parsed == ResolvedVerificationBadgeType.NONE) {
        return when (role?.lowercase()?.trim()) {
            "creator", "artist", "founder" -> ResolvedVerificationBadgeType.BLACK_TICK
            "business", "brand", "vip", "gold" -> ResolvedVerificationBadgeType.GOLD_TICK
            else -> ResolvedVerificationBadgeType.BLUE_TICK
        }
    }
    return parsed
}

/**
 * تعیین رنگ نشان با در نظر گرفتن زمینه روشن یا تیره.
 */
/**
 * رنگ غیرCompose (ARGB) متناسب با نوع تیک رسمی برای استفاده در Canvas / Bitmap.
 */
fun verificationBadgeColorArgb(
    type: ResolvedVerificationBadgeType,
    isDarkSurface: Boolean = false,
): Int {
    return when (type) {
        ResolvedVerificationBadgeType.BLUE_TICK -> VerificationBadgeDefaults.Blue.toArgb()
        ResolvedVerificationBadgeType.GOLD_TICK -> VerificationBadgeDefaults.Gold.toArgb()
        ResolvedVerificationBadgeType.BLACK_TICK -> if (isDarkSurface) {
            VerificationBadgeDefaults.BlackDark.toArgb()
        } else {
            VerificationBadgeDefaults.BlackLight.toArgb()
        }
        ResolvedVerificationBadgeType.NONE -> android.graphics.Color.TRANSPARENT
    }
}

@Composable
fun verificationBadgeColor(
    type: ResolvedVerificationBadgeType,
    isDarkSurface: Boolean = isSystemInDarkTheme() || MaterialTheme.colorScheme.surface.luminance() < 0.5f,
): Color {
    return when (type) {
        ResolvedVerificationBadgeType.BLUE_TICK -> VerificationBadgeDefaults.Blue
        ResolvedVerificationBadgeType.GOLD_TICK -> VerificationBadgeDefaults.Gold
        ResolvedVerificationBadgeType.BLACK_TICK -> if (isDarkSurface) {
            VerificationBadgeDefaults.BlackDark
        } else {
            VerificationBadgeDefaults.BlackLight
        }
        ResolvedVerificationBadgeType.NONE -> Color.Transparent
    }
}

/**
 * کامپوننت جامع و واحد نمایش نشان تیک تأیید هویت در تمام بخش‌های برنامه.
 */
@Composable
fun VistaVerificationBadge(
    isVerified: Boolean,
    verificationType: String? = null,
    role: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 15.dp,
    isDarkSurface: Boolean = isSystemInDarkTheme() || MaterialTheme.colorScheme.surface.luminance() < 0.5f,
    overrideColor: Color? = null,
    contentDescription: String = "حساب تایید شده",
) {
    val resolvedType = resolveVerificationBadgeType(
        isVerified = isVerified,
        verificationType = verificationType,
        role = role,
    )
    if (resolvedType == ResolvedVerificationBadgeType.NONE) return

    val badgeColor = overrideColor ?: verificationBadgeColor(
        type = resolvedType,
        isDarkSurface = isDarkSurface,
    )

    Icon(
        painter = painterResource(R.drawable.ic_verified_badge),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = badgeColor,
    )
}
