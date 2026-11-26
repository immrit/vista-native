package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.glass.VistaWindowBlurEffect
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassBlurSupported
import ir.coffevista.vista_native.core.designsystem.tokens.VistaRadii

data class VistaActionItem(
    val label: String,
    val icon: ImageVector? = null,
    val iconRes: Int? = null,
    val iconTint: Color? = null,
    val textColor: Color? = null,
    val isDestructive: Boolean = false,
    val trailing: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit,
)

/**
 * Telegram-inspired sleek action bottom sheet replacing raw Material DropdownMenus
 * on content actions, room actions, and profile items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaActionMenuSheet(
    visible: Boolean,
    items: List<VistaActionItem>,
    onDismiss: () -> Unit,
    title: String? = null,
) {
    if (!visible || items.isEmpty()) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val blurSupported = rememberVistaGlassBlurSupported()
    val haptics = rememberVistaHaptics()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = VistaRadii.XXLarge, topEnd = VistaRadii.XXLarge),
        containerColor = MaterialTheme.colorScheme.surface.copy(
            alpha = if (blurSupported) 0.90f else 0.99f,
        ),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)),
            )
        },
    ) {
        VistaWindowBlurEffect()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items.forEach { item ->
                VistaActionSheetRow(
                    item = item,
                    onSelect = {
                        haptics.tap()
                        onDismiss()
                        item.onClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun VistaActionSheetRow(
    item: VistaActionItem,
    onSelect: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 560f),
        label = "vistaActionRowScale",
    )
    val resolvedIconTint = when {
        item.iconTint != null -> item.iconTint
        item.isDestructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val resolvedTextColor = when {
        item.textColor != null -> item.textColor
        item.isDestructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(VistaRadii.Medium))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onSelect,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.icon != null || item.iconRes != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(resolvedIconTint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    item.icon != null -> Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = resolvedIconTint,
                        modifier = Modifier.size(20.dp),
                    )
                    item.iconRes != null -> Image(
                        painter = painterResource(item.iconRes),
                        contentDescription = null,
                        colorFilter = if (item.iconTint != null || item.isDestructive) {
                            ColorFilter.tint(resolvedIconTint)
                        } else null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = resolvedTextColor,
            ),
            modifier = Modifier.weight(1f),
        )
        item.trailing?.invoke()
    }
}

/**
 * Custom-styled anchored popover replacing raw unstyled DropdownMenu for compact controls
 * such as playback speed, quick overflow anchors, or inside modal bottom sheets.
 */
@Composable
fun VistaPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<VistaActionItem>,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberVistaHaptics()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(VistaRadii.Large),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        items.forEach { item ->
            val resolvedIconTint = when {
                item.iconTint != null -> item.iconTint
                item.isDestructive -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            val resolvedTextColor = when {
                item.textColor != null -> item.textColor
                item.isDestructive -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minWidth = 168.dp, minHeight = 46.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        onClick = {
                            haptics.tap()
                            onDismissRequest()
                            item.onClick()
                        },
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.icon != null || item.iconRes != null) {
                    when {
                        item.icon != null -> Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = resolvedIconTint,
                            modifier = Modifier.size(18.dp),
                        )
                        item.iconRes != null -> Image(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            colorFilter = if (item.iconTint != null || item.isDestructive) {
                                ColorFilter.tint(resolvedIconTint)
                            } else null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = resolvedTextColor,
                    ),
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.trailing != null) {
                    Spacer(Modifier.width(12.dp))
                    item.trailing.invoke()
                }
            }
        }
    }
}
