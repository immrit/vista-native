package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Signature Switch used across Vista design system.
 * Features a pill track with custom animated hollow/accent thumb matching the reference design.
 */
@Composable
fun VistaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    checkedThumbBorderColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedThumbBorderColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    thumbInnerColor: Color = MaterialTheme.colorScheme.surface,
    trackWidth: Dp = 40.dp,
    trackHeight: Dp = 20.dp,
    thumbSize: Dp = 20.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberVistaHaptics()

    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> (if (checked) checkedTrackColor else uncheckedTrackColor).copy(alpha = 0.38f)
            checked -> checkedTrackColor
            else -> uncheckedTrackColor
        },
        label = "VistaSwitchTrackColor",
    )

    val thumbBorderColor by animateColorAsState(
        targetValue = when {
            !enabled -> (if (checked) checkedThumbBorderColor else uncheckedThumbBorderColor).copy(alpha = 0.38f)
            checked -> checkedThumbBorderColor
            else -> uncheckedThumbBorderColor
        },
        label = "VistaSwitchThumbBorderColor",
    )

    val thumbInnerAnimatedColor by animateColorAsState(
        targetValue = when {
            !enabled -> thumbInnerColor.copy(alpha = 0.38f)
            checked -> thumbInnerColor
            else -> Color(0xFF586675)
        },
        label = "VistaSwitchThumbInnerColor",
    )

    val maxOffset = trackWidth - thumbSize
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "VistaSwitchThumbOffset",
    )

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 22.dp),
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
            ) {
                haptics.tap()
                onCheckedChange?.invoke(!checked)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(trackColor),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(thumbInnerAnimatedColor)
                    .border(width = 2.5.dp, color = thumbBorderColor, shape = CircleShape),
            )
        }
    }
}
