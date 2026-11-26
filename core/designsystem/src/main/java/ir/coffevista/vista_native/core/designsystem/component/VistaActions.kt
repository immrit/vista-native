package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import ir.coffevista.vista_native.core.designsystem.glass.LocalVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
import ir.coffevista.vista_native.core.designsystem.tokens.VistaComponentSize
import ir.coffevista.vista_native.core.designsystem.tokens.VistaRadii

enum class VistaButtonVariant { Primary, Secondary, Outline, Text, Destructive }

@Composable
fun VistaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: VistaButtonVariant = VistaButtonVariant.Primary,
    containerColor: Color? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val haptics = rememberVistaHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !loading) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 560f,
        ),
        label = "vista_btn_press_scale",
    )
    val hapticClick: () -> Unit = {
        haptics.tap()
        onClick()
    }
    val resolvedModifier = modifier
        .defaultMinSize(
            minWidth = VistaComponentSize.TouchTarget,
            minHeight = VistaComponentSize.Button,
        )
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .semantics {
            if (loading) stateDescription = "در حال انجام"
        }
    val buttonShape = RoundedCornerShape(VistaRadii.Medium)
    val renderedContent: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = when (variant) {
                    VistaButtonVariant.Outline, VistaButtonVariant.Text -> MaterialTheme.colorScheme.primary
                    VistaButtonVariant.Secondary -> MaterialTheme.colorScheme.onSecondary
                    VistaButtonVariant.Destructive -> MaterialTheme.colorScheme.onError
                    VistaButtonVariant.Primary -> MaterialTheme.colorScheme.onPrimary
                },
            )
        } else {
            content()
        }
    }
    when (variant) {
        VistaButtonVariant.Outline -> OutlinedButton(
            onClick = hapticClick,
            modifier = resolvedModifier,
            enabled = enabled && !loading,
            shape = buttonShape,
            interactionSource = interactionSource,
            content = renderedContent,
        )
        VistaButtonVariant.Text -> TextButton(
            onClick = hapticClick,
            modifier = resolvedModifier,
            enabled = enabled && !loading,
            shape = buttonShape,
            interactionSource = interactionSource,
            content = renderedContent,
        )
        else -> {
            val defaultColor = when (variant) {
                VistaButtonVariant.Primary -> MaterialTheme.colorScheme.primary
                VistaButtonVariant.Secondary -> MaterialTheme.colorScheme.secondary
                VistaButtonVariant.Destructive -> MaterialTheme.colorScheme.error
                else -> error("Handled above")
            }
            val defaultContentColor = when (variant) {
                VistaButtonVariant.Primary -> MaterialTheme.colorScheme.onPrimary
                VistaButtonVariant.Secondary -> MaterialTheme.colorScheme.onSecondary
                VistaButtonVariant.Destructive -> MaterialTheme.colorScheme.onError
                else -> error("Handled above")
            }
            Button(
                onClick = hapticClick,
                modifier = resolvedModifier,
                enabled = enabled && !loading,
                shape = buttonShape,
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor ?: defaultColor,
                    contentColor = defaultContentColor,
                ),
                content = renderedContent,
            )
        }
    }
}

object VistaFabDefaults {
    val RightMargin = 16.dp
    val BottomPaddingAboveNavBar = 94.dp
}

/**
 * Standard Floating Action Button (FAB) across the Vista application.
 * Positioned identically on the physical right above the floating bottom navigation bar and system/IME insets.
 */
@Composable
fun VistaFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val bottomInset = with(density) {
        maxOf(
            WindowInsets.navigationBars.getBottom(density),
            WindowInsets.ime.getBottom(density),
        ).toDp()
    }
    val glassState = LocalVistaGlassState.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val haptics = rememberVistaHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "fab_press_scale",
    )
    Box(
        modifier = modifier
            .absolutePadding(
                right = VistaFabDefaults.RightMargin,
                bottom = VistaFabDefaults.BottomPaddingAboveNavBar + bottomInset,
            )
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .size(56.dp)
            .then(
                if (glassState != null) {
                    Modifier
                        .vistaGlass(
                            state = glassState,
                            shape = CircleShape,
                            tint = MaterialTheme.colorScheme.primary,
                            isDark = isDark,
                            blurRadius = VistaGlassTokens.PillBlur,
                            tintAlphaOverride = 0.88f,
                            drawHighlight = true,
                            rimWidth = 0.65.dp,
                            shadowElevation = 6.dp,
                        )
                        .clip(CircleShape)
                } else {
                    Modifier
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.onPrimary),
                role = Role.Button,
                onClick = {
                    haptics.tap()
                    onClick()
                },
            )
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
            content()
        }
    }
}

