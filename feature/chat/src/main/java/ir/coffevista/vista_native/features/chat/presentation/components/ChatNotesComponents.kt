package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.model.note.ProfileNotePrompts
import ir.coffevista.vista_native.core.designsystem.R
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.ProfileNote
import ir.coffevista.vista_native.features.chat.presentation.ChatAvatarImage

@Composable
internal fun ProfileNotesTray(
    notes: List<ProfileNote>,
    conversations: List<Conversation>,
    isLoading: Boolean,
    onOpenOwnNote: () -> Unit,
    onOpenPeerNote: (ProfileNote, Conversation) -> Unit,
) {
    val own = remember(notes) { notes.firstOrNull { it.isMine && !it.isExpired() } }
    val peerNotes = remember(notes, conversations) {
        val privateConvsByPeer = HashMap<String, Conversation>(conversations.size)
        conversations.forEach { conv ->
            if (conv.type == ConversationType.PRIVATE) {
                conv.peerId?.trim()?.takeIf(String::isNotEmpty)?.let { pid ->
                    val lower = pid.lowercase()
                    privateConvsByPeer.putIfAbsent(lower, conv)
                    privateConvsByPeer.putIfAbsent(lower.replace("-", ""), conv)
                }
            }
        }
        notes.asSequence()
            .filter { !it.isMine && !it.isExpired() && it.userId.isNotBlank() }
            .distinctBy { it.userId }
            .mapNotNull { note ->
                val cleanNoteUserId = note.userId.trim().lowercase()
                val conv = privateConvsByPeer[cleanNoteUserId]
                    ?: privateConvsByPeer[cleanNoteUserId.replace("-", "")]
                conv?.let { note to it }
            }
            .toList()
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item(key = "own-note") {
            ProfileNoteItem(
                text = own?.content,
                label = if (own == null) "یادداشت" else "یادداشت شما",
                avatarUrl = null,
                loading = isLoading,
                onClick = onOpenOwnNote,
            )
        }
        items(peerNotes, key = { "peer-note-${it.first.userId}" }) { (note, conversation) ->
            ProfileNoteItem(
                text = note.content,
                label = conversation.title,
                avatarUrl = conversation.avatarUrl,
                loading = false,
                onClick = { onOpenPeerNote(note, conversation) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeerNoteReplySheet(
    note: ProfileNote,
    conversation: Conversation,
    sending: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var reply by remember(note.id) { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                Modifier.fillMaxWidth().imePadding().padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fallback = painterResource(R.drawable.vista_default_avatar)
                AsyncImage(
                    model = conversation.avatarUrl,
                    contentDescription = null,
                    placeholder = fallback,
                    error = fallback,
                    fallback = fallback,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                )
                Spacer(Modifier.height(8.dp))
                Text(conversation.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    note.content,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it.take(16_000) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !sending,
                    singleLine = false,
                    maxLines = 5,
                    label = { Text("پاسخ شما به این یادداشت...") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onSend(reply.trim()) },
                    enabled = reply.isNotBlank() && !sending,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("ارسال پاسخ")
                }
                TextButton(onClick = onDismiss, enabled = !sending) { Text("انصراف") }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
internal fun ProfileNoteItem(
    text: String?,
    label: String,
    avatarUrl: String?,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier.width(80.dp).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(86.dp).fillMaxWidth()) {
            ChatAvatarImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(64.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        shape = CircleShape,
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 48.dp, max = 80.dp)
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xF21C2230) else Color(0xF8FFFFFF))
                        .border(
                            width = 0.5.dp,
                            color = if (text.isNullOrBlank()) {
                                Color(0xFF2196F3).copy(alpha = 0.6f)
                            } else {
                                if (isDark) Color(0x38FFFFFF) else Color(0x22000000)
                            },
                            shape = RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        loading -> CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
                        text.isNullOrBlank() -> Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFF2196F3),
                        )
                        else -> {
                            val cleanText = text.trim()
                            val (fontSize, lineHeight, maxLines) = when {
                                cleanText.length <= 12 -> Triple(10.sp, 13.sp, 2)
                                cleanText.length <= 26 -> Triple(9.sp, 11.5.sp, 2)
                                else -> Triple(8.sp, 10.5.sp, 3)
                            }
                            Text(
                                cleanText,
                                maxLines = maxLines,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.White else Color(0xFF1E293B),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(1.5.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xF21C2230) else Color(0xF8FFFFFF))
                        .border(0.5.dp, if (isDark) Color(0x30FFFFFF) else Color(0x18000000), CircleShape),
                )
                Spacer(Modifier.height(1.dp))
                Box(
                    modifier = Modifier
                        .size(2.5.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xF21C2230) else Color(0xF8FFFFFF))
                        .border(0.5.dp, if (isDark) Color(0x30FFFFFF) else Color(0x18000000), CircleShape),
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OwnNoteEditorSheet(
    current: ProfileNote?,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var content by remember(current?.id) { mutableStateOf(current?.content.orEmpty()) }
    var activePrompt by remember { mutableStateOf(ProfileNotePrompts.randomPrompt()) }
    val isDark = isSystemInDarkTheme()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Live Thought Bubble Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0xFF141923) else Color(0xFFF1F5F9))
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val previewText = content.trim().ifEmpty { activePrompt }
                        val (fontSize, lineHeight, maxLines) = when {
                            previewText.length <= 14 -> Triple(12.sp, 16.sp, 2)
                            previewText.length <= 34 -> Triple(10.5.sp, 14.sp, 2)
                            else -> Triple(9.5.sp, 12.5.sp, 3)
                        }
                        Box(
                            modifier = Modifier
                                .widthIn(min = 60.dp, max = 220.dp)
                                .shadow(5.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isDark) Color(0xF21C2230) else Color(0xF8FFFFFF))
                                .border(
                                    1.dp,
                                    if (content.isNotBlank()) {
                                        if (isDark) Color(0x4DFFFFFF) else Color(0x22000000)
                                    } else {
                                        Color(0xFF2196F3).copy(alpha = 0.6f)
                                    },
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = previewText,
                                style = TextStyle(
                                    fontSize = fontSize,
                                    lineHeight = lineHeight,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (content.isBlank()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    } else {
                                        if (isDark) Color.White else Color(0xFF0F172A)
                                    },
                                    textAlign = TextAlign.Center,
                                ),
                                maxLines = maxLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xF21C2230) else Color(0xF8FFFFFF))
                                .border(0.5.dp, if (isDark) Color(0x38FFFFFF) else Color(0x20000000), CircleShape),
                        )
                        Spacer(modifier = Modifier.height(1.5.dp))
                        Box(
                            modifier = Modifier
                                .size(3.5.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xF21C2230) else Color(0xF8FFFFFF))
                                .border(0.5.dp, if (isDark) Color(0x38FFFFFF) else Color(0x20000000), CircleShape),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("یادداشت جدید", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "به مدت ۲۴ ساعت برای مخاطبین شما نمایش داده می‌شود.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { activePrompt = ProfileNotePrompts.randomPrompt() },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "ایده دیگر",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Quick suggestion chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ProfileNotePrompts.quickChips) { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (content == chip) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { content = chip }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = chip,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (content == chip) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                val charColor = when {
                    content.length <= 44 -> MaterialTheme.colorScheme.onSurfaceVariant
                    content.length <= 54 -> Color(0xFFFFA000)
                    else -> Color(0xFFE53935)
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 60) content = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(activePrompt) },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !saving,
                    shape = RoundedCornerShape(14.dp),
                    supportingText = {
                        Text(
                            "${content.length}/60",
                            modifier = Modifier.fillMaxWidth(),
                            color = charColor,
                            fontWeight = if (content.length > 54) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.End,
                        )
                    },
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onSave(content.trim()) },
                        enabled = !saving && content.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("اشتراک‌گذاری یادداشت", fontWeight = FontWeight.SemiBold)
                    }
                    if (current != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            enabled = !saving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text("حذف")
                        }
                    }
                    TextButton(onClick = onDismiss, enabled = !saving, shape = RoundedCornerShape(12.dp)) {
                        Text("انصراف")
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
