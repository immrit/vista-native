package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent

/**
 * منوی بازشوی کانتکست و واکنش‌ها (Reactions) با تم اصیل Telegram و زبان فارسی:
 * - پیام در همان موقعیت فیزیکی خود در لیست چت بدون جابجایی باقی می‌ماند
 * - فضای پس‌زمینه با لایه تیره مات می‌شود
 * - نوار واکنش‌ها و کارت گزینه‌ها به موقعیت حباب پیام متصل و مهار می‌شوند
 */
@Composable
fun TelegramMessageContextMenu(
    message: Message,
    bubbleBounds: Rect? = null,
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
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp
    val screenWidth = config.screenWidthDp.dp

    val cardBackgroundColor = Color(0xFF24303E)
    val textPrimaryColor = Color(0xFFFFFFFF)
    val iconPrimaryColor = Color(0xFF8E9DAE)
    val dividerColor = Color(0xFF2E3C4D)
    val deleteColor = Color(0xFFFF595A)

    val isTextOnly = message.content is MessageContent.Text && message.attachment == null
    val isEditable = message.isMine && isTextOnly && message.deletedAtEpochMillis == null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            ) {
                if (bubbleBounds != null) {
                    val bubbleTopDp = with(density) { bubbleBounds.top.toDp() }
                    val bubbleBottomDp = with(density) { bubbleBounds.bottom.toDp() }
                    val bubbleLeftDp = with(density) { bubbleBounds.left.toDp() }
                    val bubbleRightDp = with(density) { bubbleBounds.right.toDp() }

                    val reactionsHeight = 44.dp
                    val reactionsWidth = 300.dp
                    val menuWidth = 195.dp

                    val totalItems = 3 + // Reply, Forward, Select
                        (if (isTextOnly) 1 else 0) + // Copy
                        (if (message.serverId != null) 1 else 0) + // Pin
                        (if (isEditable) 1 else 0) + // Edit
                        1 + // Info
                        1   // Delete
                    val menuHeight = (totalItems * 40 + 16).dp

                    val hasTopSpaceForReactions = bubbleTopDp >= (reactionsHeight + 14.dp)
                    val hasBottomSpaceForMenu = (bubbleBottomDp + menuHeight + 16.dp) <= screenHeight

                    val reactionsTop = if (hasTopSpaceForReactions) {
                        bubbleTopDp - reactionsHeight - 8.dp
                    } else {
                        bubbleBottomDp + 8.dp
                    }

                    val menuTop = if (hasBottomSpaceForMenu) {
                        if (hasTopSpaceForReactions) {
                            bubbleBottomDp + 8.dp
                        } else {
                            bubbleBottomDp + reactionsHeight + 16.dp
                        }
                    } else {
                        if (hasTopSpaceForReactions) {
                            (bubbleTopDp - reactionsHeight - menuHeight - 12.dp).coerceAtLeast(16.dp)
                        } else {
                            (bubbleTopDp - menuHeight - 12.dp).coerceAtLeast(16.dp)
                        }
                    }

                    val maxReactionsLeft = (screenWidth - reactionsWidth - 12.dp).coerceAtLeast(12.dp)
                    val reactionsLeft = if (message.isMine) {
                        (bubbleRightDp - reactionsWidth).coerceIn(12.dp, maxReactionsLeft)
                    } else {
                        bubbleLeftDp.coerceIn(12.dp, maxReactionsLeft)
                    }

                    val maxMenuLeft = (screenWidth - menuWidth - 12.dp).coerceAtLeast(12.dp)
                    val menuLeft = if (message.isMine) {
                        (bubbleRightDp - menuWidth).coerceIn(12.dp, maxMenuLeft)
                    } else {
                        bubbleLeftDp.coerceIn(12.dp, maxMenuLeft)
                    }

                    // ۱. نوار واکنش‌های سریع (Reactions Capsule)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = cardBackgroundColor,
                        shadowElevation = 8.dp,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .offset(x = reactionsLeft, y = reactionsTop)
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
                                        .clickable { onReact(emoji) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TelegramEmoji(
                                        emoji = emoji,
                                        size = 21.dp,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "سایر واکنش‌ها",
                                    tint = iconPrimaryColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    // ۲. کارت دستورات پیام (Action Menu Card)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cardBackgroundColor,
                        shadowElevation = 10.dp,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .offset(x = menuLeft, y = menuTop)
                            .width(menuWidth)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                TelegramContextMenuItem(
                                    icon = Icons.AutoMirrored.Filled.Reply,
                                    label = "پاسخ",
                                    iconColor = iconPrimaryColor,
                                    textColor = textPrimaryColor,
                                    onClick = onReply,
                                )
                                if (isTextOnly) {
                                    TelegramContextMenuItem(
                                        icon = Icons.Default.ContentCopy,
                                        label = "کپی متن",
                                        iconColor = iconPrimaryColor,
                                        textColor = textPrimaryColor,
                                        onClick = onCopy,
                                    )
                                }
                                TelegramContextMenuItem(
                                    icon = Icons.AutoMirrored.Filled.Forward,
                                    label = "هدایت پیام",
                                    iconColor = iconPrimaryColor,
                                    textColor = textPrimaryColor,
                                    onClick = onForward,
                                )
                                if (message.serverId != null) {
                                    TelegramContextMenuItem(
                                        icon = Icons.Default.PushPin,
                                        label = if (message.isPinned) "برداشتن سنجاق" else "سنجاق کردن",
                                        iconColor = iconPrimaryColor,
                                        textColor = textPrimaryColor,
                                        onClick = onTogglePinned,
                                    )
                                }
                                if (isEditable) {
                                    TelegramContextMenuItem(
                                        icon = Icons.Default.Edit,
                                        label = "ویرایش",
                                        iconColor = iconPrimaryColor,
                                        textColor = textPrimaryColor,
                                        onClick = onEdit,
                                    )
                                }
                                TelegramContextMenuItem(
                                    icon = Icons.Default.CheckCircleOutline,
                                    label = "انتخاب پیام",
                                    iconColor = iconPrimaryColor,
                                    textColor = textPrimaryColor,
                                    onClick = onSelect,
                                )
                                TelegramContextMenuItem(
                                    icon = Icons.Default.Info,
                                    label = "جزئیات پیام",
                                    iconColor = iconPrimaryColor,
                                    textColor = textPrimaryColor,
                                    onClick = onInfo,
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 3.dp, horizontal = 10.dp),
                                    thickness = 0.5.dp,
                                    color = dividerColor,
                                )
                                TelegramContextMenuItem(
                                    icon = Icons.Default.Delete,
                                    label = "حذف پیام",
                                    iconColor = deleteColor,
                                    textColor = deleteColor,
                                    onClick = onDelete,
                                    isDestructive = true,
                                )
                            }
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
                            color = cardBackgroundColor,
                            shadowElevation = 10.dp,
                            modifier = Modifier
                                .width(195.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                    TelegramContextMenuItem(
                                        icon = Icons.AutoMirrored.Filled.Reply,
                                        label = "پاسخ",
                                        iconColor = iconPrimaryColor,
                                        textColor = textPrimaryColor,
                                        onClick = onReply,
                                    )
                                    if (isTextOnly) {
                                        TelegramContextMenuItem(
                                            icon = Icons.Default.ContentCopy,
                                            label = "کپی متن",
                                            iconColor = iconPrimaryColor,
                                            textColor = textPrimaryColor,
                                            onClick = onCopy,
                                        )
                                    }
                                    TelegramContextMenuItem(
                                        icon = Icons.AutoMirrored.Filled.Forward,
                                        label = "هدایت پیام",
                                        iconColor = iconPrimaryColor,
                                        textColor = textPrimaryColor,
                                        onClick = onForward,
                                    )
                                    if (message.serverId != null) {
                                        TelegramContextMenuItem(
                                            icon = Icons.Default.PushPin,
                                            label = if (message.isPinned) "برداشتن سنجاق" else "سنجاق کردن",
                                            iconColor = iconPrimaryColor,
                                            textColor = textPrimaryColor,
                                            onClick = onTogglePinned,
                                        )
                                    }
                                    if (isEditable) {
                                        TelegramContextMenuItem(
                                            icon = Icons.Default.Edit,
                                            label = "ویرایش",
                                            iconColor = iconPrimaryColor,
                                            textColor = textPrimaryColor,
                                            onClick = onEdit,
                                        )
                                    }
                                    TelegramContextMenuItem(
                                        icon = Icons.Default.CheckCircleOutline,
                                        label = "انتخاب پیام",
                                        iconColor = iconPrimaryColor,
                                        textColor = textPrimaryColor,
                                        onClick = onSelect,
                                    )
                                    TelegramContextMenuItem(
                                        icon = Icons.Default.Info,
                                        label = "جزئیات پیام",
                                        iconColor = iconPrimaryColor,
                                        textColor = textPrimaryColor,
                                        onClick = onInfo,
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 3.dp, horizontal = 10.dp),
                                        thickness = 0.5.dp,
                                        color = dividerColor,
                                    )
                                    TelegramContextMenuItem(
                                        icon = Icons.Default.Delete,
                                        label = "حذف پیام",
                                        iconColor = deleteColor,
                                        textColor = deleteColor,
                                        onClick = onDelete,
                                        isDestructive = true,
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
            .clickable(onClick = onClick)
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
