package ir.coffevista.vista_native.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.component.VistaVerificationBadge
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.DuelInviteParser
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import ir.coffevista.vista_native.features.services.ui.web.DuelShareBridgeRequest
import ir.coffevista.vista_native.features.services.ui.web.DuelShareBridgeResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles direct targeted duel sends (when started from inside a DM/group)
 * without forcing an extra picker step. Falls back to error result if blocked
 * or unreachable.
 */
internal suspend fun dispatchDirectDuelShare(
    request: DuelShareBridgeRequest,
    currentUserId: String,
    chatRepository: ChatRepository,
    context: Context,
): DuelShareBridgeResult {
    val payloadJson = DuelInviteParser.serializePayload(
        matchId = request.matchId,
        inviterId = currentUserId,
        inviterName = request.inviterName,
        inviterAvatar = request.inviterAvatar,
        entryFee = request.entryFee,
    )

    val convId = request.conversationId?.trim()?.takeIf { it.isNotEmpty() }
    val targetPeerId = request.targetUserId?.trim()?.takeIf { it.isNotEmpty() }

    if (targetPeerId != null) {
        when (val blockResult = chatRepository.getBlockStatus(targetPeerId, forceRefresh = true)) {
            is ChatResult.Success -> if (blockResult.value.hasAnyBlock) {
                return DuelShareBridgeResult(
                    requestId = request.requestId,
                    status = "error",
                    message = "امکان ارسال دعوت به این کاربر وجود ندارد",
                )
            }
            else -> Unit
        }
    }

    val resolvedConversation = when {
        convId != null -> convId
        targetPeerId != null -> {
            val existing = chatRepository.findConversationByPeer(targetPeerId, isSecret = false)
            if (existing != null) {
                existing.id
            } else {
                when (val created = chatRepository.createConversation(targetPeerId, isSecret = false)) {
                    is ChatResult.Success -> created.value.id
                    is ChatResult.Failure -> {
                        return DuelShareBridgeResult(
                            requestId = request.requestId,
                            status = "error",
                            message = created.message,
                        )
                    }
                }
            }
        }
        else -> return DuelShareBridgeResult(
            requestId = request.requestId,
            status = "error",
            message = "مقصد گفتگو مشخص نیست",
        )
    }

    return when (val sendRes = chatRepository.sendDuelInvite(resolvedConversation, payloadJson)) {
        is ChatResult.Success -> {
            Toast.makeText(context, "دعوت دوئل در چت ارسال شد", Toast.LENGTH_SHORT).show()
            DuelShareBridgeResult(
                requestId = request.requestId,
                status = "sent",
                conversationId = resolvedConversation,
            )
        }
        is ChatResult.Failure -> DuelShareBridgeResult(
            requestId = request.requestId,
            status = "error",
            message = sendRes.message,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GameInviteShareModal(
    request: DuelShareBridgeRequest,
    currentUserId: String,
    chatRepository: ChatRepository,
    context: Context,
    onDismiss: (DuelShareBridgeResult) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val conversations by chatRepository.observeConversations()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val suggestedUsers by chatRepository.observeSuggestedUsers()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ChatUser>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { chatRepository.refreshConversations(reset = true) }
    }

    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.length < 2) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(250)
        when (val res = chatRepository.searchUsers(trimmed)) {
            is ChatResult.Success -> {
                searchResults = res.value.filterNot { it.id == currentUserId }
            }
            is ChatResult.Failure -> searchResults = emptyList()
        }
        isSearching = false
    }

    val payloadJson = remember(request, currentUserId) {
        DuelInviteParser.serializePayload(
            matchId = request.matchId,
            inviterId = currentUserId,
            inviterName = request.inviterName,
            inviterAvatar = request.inviterAvatar,
            entryFee = request.entryFee,
        )
    }

    val nonSecretConversations = remember(conversations, searchQuery) {
        val q = searchQuery.trim().lowercase()
        conversations
            .filter { it.type != ConversationType.SECRET }
            .filter { conv -> q.isEmpty() || conv.title.lowercase().contains(q) }
    }

    val visibleUsers = remember(searchQuery, searchResults, suggestedUsers, conversations, currentUserId) {
        val existingPeerIds = conversations.mapNotNull { it.peerId?.lowercase() }.toSet()
        val raw = if (searchQuery.trim().length >= 2) searchResults else suggestedUsers
        raw.filterNot { it.id == currentUserId || it.id.lowercase() in existingPeerIds }
    }

    fun sendToUser(user: ChatUser) {
        if (isSending) return
        scope.launch {
            isSending = true
            errorMessage = null
            when (val block = chatRepository.getBlockStatus(user.id, forceRefresh = true)) {
                is ChatResult.Success -> if (block.value.hasAnyBlock) {
                    errorMessage = "امکان ارسال دعوت به ${user.displayName} وجود ندارد"
                    isSending = false
                    return@launch
                }
                else -> Unit
            }
            val convId = chatRepository.findConversationByPeer(user.id, isSecret = false)?.id
                ?: when (val created = chatRepository.createConversation(user.id, isSecret = false)) {
                    is ChatResult.Success -> created.value.id
                    is ChatResult.Failure -> {
                        errorMessage = created.message
                        isSending = false
                        return@launch
                    }
                }
            when (val res = chatRepository.sendDuelInvite(convId, payloadJson)) {
                is ChatResult.Success -> {
                    Toast.makeText(
                        context,
                        "دعوت دوئل برای ${user.displayName} ارسال شد",
                        Toast.LENGTH_SHORT,
                    ).show()
                    isSending = false
                    onDismiss(
                        DuelShareBridgeResult(
                            requestId = request.requestId,
                            status = "sent",
                            conversationId = convId,
                            recipientName = user.displayName,
                        ),
                    )
                }
                is ChatResult.Failure -> {
                    errorMessage = res.message
                    isSending = false
                }
            }
        }
    }

    fun sendToSelectedConversations() {
        if (selectedIds.isEmpty() || isSending) return
        scope.launch {
            isSending = true
            errorMessage = null
            val targets = nonSecretConversations.filter { it.id in selectedIds }
            var sentCount = 0
            var lastConvId: String? = null
            var lastError: String? = null
            for (conv in targets) {
                if (conv.type == ConversationType.PRIVATE && !conv.peerId.isNullOrBlank()) {
                    val blocked = (chatRepository.getBlockStatus(conv.peerId!!) as? ChatResult.Success)
                        ?.value?.hasAnyBlock == true
                    if (blocked) continue
                }
                when (val res = chatRepository.sendDuelInvite(conv.id, payloadJson)) {
                    is ChatResult.Success -> {
                        sentCount++
                        lastConvId = conv.id
                    }
                    is ChatResult.Failure -> lastError = res.message
                }
            }
            isSending = false
            if (sentCount > 0) {
                Toast.makeText(context, "دعوت دوئل ارسال شد", Toast.LENGTH_SHORT).show()
                onDismiss(
                    DuelShareBridgeResult(
                        requestId = request.requestId,
                        status = "sent",
                        conversationId = lastConvId,
                        recipientName = targets.firstOrNull()?.title,
                    ),
                )
            } else {
                errorMessage = lastError ?: "ارسال دعوت ناموفق بود"
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isSending) {
                    onDismiss(DuelShareBridgeResult(requestId = request.requestId, status = "cancelled"))
                }
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF7C3AED)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ارسال دعوت دوئل به چت ویستا",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = "دوست یا گفتگوی موردنظر را انتخاب کنید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(
                        enabled = !isSending,
                        onClick = {
                            onDismiss(DuelShareBridgeResult(requestId = request.requestId, status = "cancelled"))
                        },
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "بستن")
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = { Text("جستجوی گفتگو یا نام کاربری...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                errorMessage?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 360.dp),
                ) {
                    if (nonSecretConversations.isNotEmpty()) {
                        item(key = "header-convs") {
                            Text(
                                text = "گفتگوهای اخیر",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                        items(nonSecretConversations, key = { "conv-${it.id}" }) { conv ->
                            val isSelected = conv.id in selectedIds
                            ConversationPickRow(
                                conversation = conv,
                                selected = isSelected,
                                onToggle = {
                                    selectedIds = if (isSelected) selectedIds - conv.id else selectedIds + conv.id
                                },
                            )
                        }
                    }

                    if (isSearching) {
                        item(key = "searching-indicator") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            }
                        }
                    } else if (visibleUsers.isNotEmpty()) {
                        item(key = "header-users") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Text(
                                text = if (searchQuery.trim().length >= 2) "نتایج جستجو" else "پیشنهاد کاربران",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        items(visibleUsers, key = { "user-${it.id}" }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ShareAvatar(avatarUrl = user.avatarUrl, name = user.displayName)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.displayName,
                                            modifier = Modifier.weight(1f, fill = false),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (user.isVerified) {
                                            Spacer(Modifier.width(4.dp))
                                            VistaVerificationBadge(
                                                isVerified = true,
                                                verificationType = user.verificationType,
                                                role = user.role,
                                                size = 14.dp,
                                            )
                                        }
                                    }
                                    if (user.username.isNotBlank()) {
                                        Text(
                                            text = "@${user.username}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                TextButton(
                                    enabled = !isSending,
                                    onClick = { sendToUser(user) },
                                ) {
                                    Text("ارسال", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (nonSecretConversations.isEmpty()) {
                        item(key = "empty-pickers") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(28.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "گفتگویی یافت نشد؛ نام کاربر را جستجو کنید.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = { sendToSelectedConversations() },
                    enabled = selectedIds.isNotEmpty() && !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            text = if (selectedIds.isEmpty()) {
                                "گفتگو را انتخاب کنید"
                            } else {
                                "ارسال دعوت دوئل (${selectedIds.size})"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ConversationPickRow(
    conversation: Conversation,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShareAvatar(avatarUrl = conversation.avatarUrl, name = conversation.title)
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = conversation.title.ifBlank { "گفتگو" },
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            if (conversation.type != ConversationType.GROUP && conversation.isVerified) {
                Spacer(Modifier.width(4.dp))
                VistaVerificationBadge(
                    isVerified = true,
                    verificationType = conversation.verificationType,
                    role = conversation.role,
                    size = 14.dp,
                )
            }
        }
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun ShareAvatar(avatarUrl: String?, name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(CircleShape),
            )
        } else {
            Text(
                text = name.trim().take(1).ifEmpty { "؟" },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
