package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent

/**
 * نوار پیام‌های پین‌شده به سبک تلگرام با قابلیت جابجایی بین چندین پیام پین‌شده.
 */
@Composable
fun PinnedMessagesBar(
    pinnedMessages: List<Message>,
    onMessageClick: (Message) -> Unit,
    onUnpinClick: ((Message) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (pinnedMessages.isEmpty()) return

    var currentIndex by remember(pinnedMessages.size) { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, pinnedMessages.lastIndex)
    val currentMessage = pinnedMessages[safeIndex]

    val title = if (pinnedMessages.size > 1) {
        "پیام سنجاق شده (${safeIndex + 1} از ${pinnedMessages.size})"
    } else {
        "پیام سنجاق شده"
    }

    val previewText = when (val content = currentMessage.content) {
        is MessageContent.Text -> content.value
        is MessageContent.Structured -> content.payload
        MessageContent.Deleted -> "پیام حذف شده"
        MessageContent.EncryptedUnavailable -> "پیام رمزگذاری‌شده"
    }.ifBlank {
        currentMessage.attachment?.fileName ?: "رسانه پیوست"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .clickable(
                role = Role.Button,
                onClick = {
                    onMessageClick(currentMessage)
                    if (pinnedMessages.size > 1) {
                        currentIndex = (safeIndex + 1) % pinnedMessages.size
                    }
                },
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // نوار باریک رنگی سمت راست (در RTL)
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(MaterialTheme.colorScheme.primary),
        )

        Spacer(Modifier.width(10.dp))

        // آیکون پین
        Icon(
            imageVector = Icons.Default.PushPin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )

        Spacer(Modifier.width(8.dp))

        // متن عنوان و خلاصه پیام با انیمیشن تغییر
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )

            AnimatedContent(
                targetState = previewText,
                transitionSpec = {
                    fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
                },
                label = "pinned-message-preview",
            ) { targetPreview ->
                Text(
                    text = targetPreview,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // دکمه لغو سنجاق در صورت مجاز بودن
        if (onUnpinClick != null) {
            IconButton(
                onClick = { onUnpinClick(currentMessage) },
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "برداشتن سنجاق",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
