package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import ir.coffevista.vista_native.features.chat.domain.model.Message
import kotlin.math.roundToInt

/**
 * منوی بازشوی کانتکست و واکنش‌ها (Reactions) با تم اصیل Telegram و زبان فارسی:
 * - پیام در همان موقعیت فیزیکی خود در لیست چت بدون جابجایی باقی می‌ماند
 * - فضای پس‌زمینه با لایه تیره مات می‌شود
 * - نوار واکنش‌ها و کارت گزینه‌ها به موقعیت حباب پیام متصل و مهار می‌شوند
 */
@Composable
internal fun MessageContextMenu(
    message: Message,
    bubbleBounds: Rect? = null,
    capabilities: MessageContextCapabilities = MessageContextCapabilities(),
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onSelect: () -> Unit = {},
    onInfo: () -> Unit = {},
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val quickReactions = remember { listOf("❤️", "👍", "🔥", "👎", "😂", "🎉", "🙏") }
    val policy = remember(message, capabilities) { message.contextActionPolicy(capabilities) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scrimProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "contextScrim",
    )
    val menuProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 650f),
        label = "contextMenu",
    )
    val reactionProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 560f),
        label = "contextReactions",
    )
    val menuTransformOrigin = if (message.isMine) {
        TransformOrigin(1f, 0f)
    } else {
        TransformOrigin(0f, 0f)
    }

    val colors = messageContextMenuColors()
    val focusedBubbleCornerRadiusPx = with(density) { 18.dp.toPx() }
    var menuSizePx by remember { mutableStateOf(IntSize.Zero) }
    var reactionsSizePx by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        // Compose Dialog applies its own dim-behind layer. Keep that layer transparent so
        // the custom scrim below can punch through around the selected message bubble.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setDimAmount(0f) }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    // Render the scrim offscreen so BlendMode.Clear can reveal the
                    // original anchored bubble beneath it. The selected message
                    // remains the visual reference while the chat is subdued.
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .background(colors.scrim.copy(alpha = colors.scrim.alpha * scrimProgress))
                    .drawWithContent {
                        drawContent()
                        bubbleBounds?.let { bounds ->
                            drawRoundRect(
                                color = Color.Transparent,
                                topLeft = Offset(bounds.left, bounds.top),
                                size = Size(bounds.width, bounds.height),
                                cornerRadius = CornerRadius(
                                    focusedBubbleCornerRadiusPx,
                                    focusedBubbleCornerRadiusPx,
                                ),
                                blendMode = BlendMode.Clear,
                            )
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            ) {
                if (bubbleBounds != null) {
                    val insetPadding = WindowInsets.statusBars
                        .union(WindowInsets.navigationBars)
                        .union(WindowInsets.ime)
                        .asPaddingValues()
                    val placement = remember(
                        bubbleBounds,
                        constraints.maxWidth,
                        constraints.maxHeight,
                        policy.visibleActionCount,
                        policy.canReact,
                        message.isMine,
                        insetPadding,
                        layoutDirection,
                    ) {
                        with(density) {
                            calculateMessageContextMenuPlacement(
                                anchor = bubbleBounds,
                                viewport = androidx.compose.ui.unit.IntSize(
                                    maxWidth.toPx().roundToInt(),
                                    maxHeight.toPx().roundToInt(),
                                ),
                                safeInsets = MessageContextMenuSafeInsets(
                                    left = insetPadding.calculateLeftPadding(layoutDirection).toPx(),
                                    top = insetPadding.calculateTopPadding().toPx(),
                                    right = insetPadding.calculateRightPadding(layoutDirection).toPx(),
                                    bottom = insetPadding.calculateBottomPadding().toPx(),
                                ),
                                isMine = message.isMine,
                                menuWidth = 195.dp.toPx(),
                                // Font scale, optional divider, and icon metrics make
                                // a fixed dp estimate unreliable. Reposition after the
                                // first measurement so the two real surfaces never meet.
                                menuHeight = menuSizePx.height.takeIf { it > 0 }?.toFloat()
                                    ?: (policy.visibleActionCount * 40 + 16).dp.toPx(),
                                reactionsWidth = reactionsSizePx.width.takeIf { it > 0 }?.toFloat()
                                    ?: 300.dp.toPx(),
                                reactionsHeight = reactionsSizePx.height.takeIf { it > 0 }?.toFloat()
                                    ?: 44.dp.toPx(),
                                showReactions = policy.canReact,
                                edgeGap = 12.dp.toPx(),
                                elementGap = 8.dp.toPx(),
                            )
                        }
                    }

                    if (policy.canReact) Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.surface,
                        shadowElevation = 8.dp,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    placement.reactions!!.x.roundToInt(),
                                    placement.reactions.y.roundToInt(),
                                )
                            }
                            .graphicsLayer {
                                alpha = reactionProgress
                                scaleX = 0.86f + reactionProgress * 0.14f
                                scaleY = 0.86f + reactionProgress * 0.14f
                                transformOrigin = menuTransformOrigin
                            }
                            // The reactions capsule is intentionally above the
                            // command card even when constrained placement puts
                            // the two surfaces close together.
                            .zIndex(1f)
                            .onSizeChanged { reactionsSizePx = it }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            quickReactions.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(33.dp)
                                        .clip(CircleShape)
                                        .semantics { contentDescription = "واکنش $emoji" }
                                        .clickable(role = Role.Button) { onReact(emoji) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TelegramEmoji(
                                        emoji = emoji,
                                        size = 21.dp,
                                    )
                                }
                            }
                        }
                    }

                    // ۲. کارت دستورات پیام (Action Menu Card)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surface,
                        shadowElevation = 10.dp,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    placement.menu.x.roundToInt(),
                                    placement.menu.y.roundToInt(),
                                )
                            }
                            .graphicsLayer {
                                alpha = menuProgress
                                scaleX = 0.92f + menuProgress * 0.08f
                                scaleY = 0.92f + menuProgress * 0.08f
                                transformOrigin = menuTransformOrigin
                            }
                            .zIndex(0f)
                            .onSizeChanged { menuSizePx = it }
                            .width(195.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            MessageContextMenuActions(
                                message = message,
                                policy = policy,
                                iconPrimaryColor = colors.icon,
                                textPrimaryColor = colors.text,
                                dividerColor = colors.divider,
                                deleteColor = colors.delete,
                                onReply = onReply,
                                onCopy = onCopy,
                                onForward = onForward,
                                onSelect = onSelect,
                                onInfo = onInfo,
                                onTogglePinned = onTogglePinned,
                                onEdit = onEdit,
                                onDelete = onDelete,
                            )
                        }
                    }
                } else {
                    // Fallback اگر موقعیت در دسترس نبود
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surface,
                            shadowElevation = 10.dp,
                            modifier = Modifier
                                .width(195.dp)
                                .graphicsLayer {
                                    alpha = menuProgress
                                    scaleX = 0.92f + menuProgress * 0.08f
                                    scaleY = 0.92f + menuProgress * 0.08f
                                    transformOrigin = TransformOrigin.Center
                                }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                MessageContextMenuActions(
                                    message = message,
                                    policy = policy,
                                    iconPrimaryColor = colors.icon,
                                    textPrimaryColor = colors.text,
                                    dividerColor = colors.divider,
                                    deleteColor = colors.delete,
                                    onReply = onReply,
                                    onCopy = onCopy,
                                    onForward = onForward,
                                    onSelect = onSelect,
                                    onInfo = onInfo,
                                    onTogglePinned = onTogglePinned,
                                    onEdit = onEdit,
                                    onDelete = onDelete,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class MessageContextMenuColors(
    val surface: Color,
    val text: Color,
    val icon: Color,
    val divider: Color,
    val delete: Color,
    val scrim: Color,
)

@Composable
private fun messageContextMenuColors(): MessageContextMenuColors {
    val scheme = MaterialTheme.colorScheme
    return MessageContextMenuColors(
        surface = scheme.surfaceContainerHigh,
        text = scheme.onSurface,
        icon = scheme.onSurfaceVariant,
        divider = scheme.outlineVariant,
        delete = scheme.error,
        scrim = scheme.scrim.copy(alpha = 0.45f),
    )
}

@Composable
private fun MessageContextMenuActions(
    message: Message,
    policy: MessageContextActionPolicy,
    iconPrimaryColor: Color,
    textPrimaryColor: Color,
    dividerColor: Color,
    deleteColor: Color,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onSelect: () -> Unit,
    onInfo: () -> Unit,
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        if (policy.canReply) {
            TelegramContextMenuItem(
                icon = Icons.AutoMirrored.Filled.Reply,
                label = "پاسخ",
                iconColor = iconPrimaryColor,
                textColor = textPrimaryColor,
                onClick = onReply,
            )
        }
        if (policy.canCopy) {
            TelegramContextMenuItem(
                icon = Icons.Default.ContentCopy,
                label = "کپی",
                iconColor = iconPrimaryColor,
                textColor = textPrimaryColor,
                onClick = onCopy,
            )
        }
        if (policy.canForward) {
            TelegramContextMenuItem(
                icon = Icons.AutoMirrored.Filled.Forward,
                label = "فوروارد",
                iconColor = iconPrimaryColor,
                textColor = textPrimaryColor,
                onClick = onForward,
            )
        }
        if (policy.canTogglePinned) {
            TelegramContextMenuItem(
                icon = Icons.Default.PushPin,
                label = if (message.isPinned) "برداشتن سنجاق" else "سنجاق کردن",
                iconColor = iconPrimaryColor,
                textColor = textPrimaryColor,
                onClick = onTogglePinned,
            )
        }
        if (policy.canEdit) {
            TelegramContextMenuItem(
                icon = Icons.Default.Edit,
                label = "ویرایش",
                iconColor = iconPrimaryColor,
                textColor = textPrimaryColor,
                onClick = onEdit,
            )
        }
        if (policy.canSelect) {
            TelegramContextMenuItem(
                icon = Icons.Default.CheckCircleOutline,
                label = "انتخاب",
                iconColor = iconPrimaryColor,
                textColor = textPrimaryColor,
                onClick = onSelect,
            )
        }
        if (policy.canViewInfo) {
            TelegramContextMenuItem(
                icon = Icons.Default.Info,
                label = "جزئیات",
                iconColor = iconPrimaryColor,
                textColor = textPrimaryColor,
                onClick = onInfo,
            )
        }
        if (policy.canDelete && policy.visibleActionCount > 1) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 3.dp, horizontal = 10.dp),
                thickness = 0.5.dp,
                color = dividerColor,
            )
        }
        if (policy.canDelete) {
            TelegramContextMenuItem(
                icon = Icons.Default.Delete,
                label = "حذف",
                iconColor = deleteColor,
                textColor = deleteColor,
                onClick = onDelete,
                isDestructive = true,
            )
        }
    }
}

@Composable
private fun TelegramContextMenuItem(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isDestructive) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
