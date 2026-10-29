package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily

object VistaBadgeDefaults {
    /** Modern, vibrant notification scarlet used by Threads, Instagram, and iOS. */
    val NotificationColor = Color(0xFFFF334B)
    val ChatBadgeColor = Color(0xFFFF334B)
    val BadgeHeight = 18.dp
    val BadgeMinSize = 18.dp
    val BorderWidth = 1.5.dp
}

/**
 * A delicate, modern badge for unread notification and message counts.
 * Inspired by design systems of X, Threads, and iOS 18.
 *
 * - Single digits (e.g. 1 to 9) form a perfect 18dp circle.
 * - Multi-digits (e.g. 10 to 99, 99+) form a sleek horizontal pill with fixed 18dp height.
 * - Supports clean cutout border matching the background for layered depth.
 * - Prevents vertical stretching with precision line-height styling and disabled font padding.
 */
@Composable
fun VistaNotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = VistaBadgeDefaults.NotificationColor,
    contentColor: Color = Color.White,
    borderColor: Color? = null,
    borderWidth: Dp = VistaBadgeDefaults.BorderWidth,
    maxCount: Int = 99,
    usePersianDigits: Boolean = true,
) {
    val visible = count > 0
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = Spring.StiffnessMediumLow,
            )
        ) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier,
    ) {
        val label = remember(count, maxCount, usePersianDigits) {
            formatBadgeCount(count, maxCount, usePersianDigits)
        }

        val isSingleChar = label.length == 1
        val horizontalPadding = if (isSingleChar) 0.dp else 5.dp

        val baseModifier = Modifier
            .height(VistaBadgeDefaults.BadgeHeight)
            .widthIn(min = VistaBadgeDefaults.BadgeMinSize)
            .clip(CircleShape)
            .then(
                if (borderColor != null) {
                    Modifier.border(borderWidth, borderColor, CircleShape)
                } else Modifier
            )
            .background(containerColor, CircleShape)
            .padding(horizontal = horizontalPadding)

        Box(
            modifier = baseModifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = contentColor,
                style = TextStyle(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both,
                    ),
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * A subtle dot badge for unread activity without a numeric count.
 */
@Composable
fun VistaBadgeDot(
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    color: Color = VistaBadgeDefaults.NotificationColor,
    borderColor: Color? = null,
    borderWidth: Dp = 1.5.dp,
) {
    Box(
        modifier = modifier
            .height(size)
            .widthIn(min = size)
            .clip(CircleShape)
            .then(
                if (borderColor != null) {
                    Modifier.border(borderWidth, borderColor, CircleShape)
                } else Modifier
            )
            .background(color, CircleShape)
    )
}

/**
 * Anchors a badge to the top-end corner of an icon or content.
 * Automatically mirrors correctly for RTL (Persian) and LTR.
 */
@Composable
fun VistaBadgedBox(
    badge: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = {
            Box(propagateMinConstraints = false) { content() }
            Box(propagateMinConstraints = false) { badge() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val contentPlaceable = measurables[0].measure(constraints)
        val badgePlaceable = measurables[1].measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )

        val layoutWidth = contentPlaceable.width
        val layoutHeight = contentPlaceable.height

        layout(layoutWidth, layoutHeight) {
            contentPlaceable.placeRelative(0, 0)
            if (badgePlaceable.width > 0 && badgePlaceable.height > 0) {
                // In placeRelative, x is relative to Start (right in RTL, left in LTR).
                // layoutWidth - (badgePlaceable.width / 2) places badge center on the End edge.
                // We add an overlap offset so the badge sits gracefully over the icon's top-end corner.
                val overlapOffset = 3.dp.roundToPx()
                val badgeX = layoutWidth - (badgePlaceable.width / 2) - overlapOffset
                val badgeY = -(badgePlaceable.height / 2) + overlapOffset
                badgePlaceable.placeRelative(badgeX, badgeY)
            }
        }
    }
}

fun formatBadgeCount(
    count: Int,
    maxCount: Int = 99,
    usePersianDigits: Boolean = true,
): String {
    return if (count > maxCount) {
        if (usePersianDigits) "۹۹+" else "99+"
    } else {
        if (usePersianDigits) count.toPersianDigitsInternal() else count.toString()
    }
}

internal fun Int.toPersianDigitsInternal(): String = toString().map { char ->
    when (char) {
        '0' -> '۰'
        '1' -> '۱'
        '2' -> '۲'
        '3' -> '۳'
        '4' -> '۴'
        '5' -> '۵'
        '6' -> '۶'
        '7' -> '۷'
        '8' -> '۸'
        '9' -> '۹'
        else -> char
    }
}.joinToString("")
