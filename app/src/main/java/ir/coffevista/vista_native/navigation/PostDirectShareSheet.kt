package ir.coffevista.vista_native.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.SharedPostDraft
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.feed.data.FeedPost
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostDirectShareSheet(
    post: FeedPost,
    chatRepository: ChatRepository,
    onDismiss: () -> Unit,
    onDelivered: () -> Unit,
) {
    val conversations by chatRepository.observeConversations().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var selectedIds by remember(post.id) { mutableStateOf(emptySet<String>()) }
    var isSending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { chatRepository.refreshConversations(reset = true) }

    ModalBottomSheet(
        onDismissRequest = { if (!isSending) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("ارسال پست", style = MaterialTheme.typography.titleLarge)
            Text(
                "یک یا چند گفت‌وگو را انتخاب کنید",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(conversations, key = Conversation::id) { conversation ->
                    RecipientRow(
                        conversation = conversation,
                        selected = conversation.id in selectedIds,
                        onToggle = {
                            selectedIds = if (conversation.id in selectedIds) {
                                selectedIds - conversation.id
                            } else {
                                selectedIds + conversation.id
                            }
                        },
                    )
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Button(
                enabled = selectedIds.isNotEmpty() && !isSending,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                onClick = {
                    isSending = true
                    error = null
                    scope.launch {
                        val payload = post.toSharedPostDraft()
                        val delivered = selectedIds.count { conversationId ->
                            val result = chatRepository.sendSharedPost(conversationId, payload)
                            result is ir.coffevista.vista_native.features.chat.domain.repository.ChatResult.Success
                        }
                        isSending = false
                        if (delivered > 0) {
                            onDelivered()
                            onDismiss()
                        } else {
                            error = "ارسال پست انجام نشد. دوباره تلاش کنید."
                        }
                    }
                },
            ) {
                Text(if (isSending) "در حال ارسال…" else "ارسال به ${selectedIds.size} مقصد")
            }
        }
    }
}

@Composable
private fun RecipientRow(conversation: Conversation, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Column(Modifier.weight(1f)) {
            Text(conversation.title, style = MaterialTheme.typography.bodyLarge)
            conversation.lastMessage?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        Spacer(Modifier.width(8.dp))
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}

private fun FeedPost.toSharedPostDraft() = SharedPostDraft(
    postId = id,
    authorName = authorFullName,
    authorUsername = authorUsername.orEmpty(),
    authorAvatar = authorAvatarUrl,
    content = content.orEmpty(),
    mediaUrls = imageUrls.ifEmpty { listOfNotNull(imageUrl) },
    postVideoUrl = videoUrl,
    likesCount = likeCount,
    commentsCount = commentCount,
    createdAt = createdAt,
    isVerified = authorIsVerified,
    verificationType = authorVerificationType.orEmpty().ifBlank { "none" },
    hashtags = tags,
)
