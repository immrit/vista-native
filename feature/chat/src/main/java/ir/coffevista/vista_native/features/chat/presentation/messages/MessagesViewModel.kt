package ir.coffevista.vista_native.features.chat.presentation.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import ir.coffevista.vista_native.features.chat.domain.model.ChatPartnerProfile
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser
import ir.coffevista.vista_native.features.chat.domain.model.BlockStatus
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.GroupInfo
import ir.coffevista.vista_native.features.chat.domain.model.GroupMember
import ir.coffevista.vista_native.features.chat.domain.model.GifItem
import ir.coffevista.vista_native.features.chat.domain.model.DownloadTask
import ir.coffevista.vista_native.features.chat.domain.model.ModerationReason
import ir.coffevista.vista_native.features.chat.domain.model.RealtimeConnectionState
import ir.coffevista.vista_native.features.chat.domain.model.SharedPostParser
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import ir.coffevista.vista_native.features.chat.domain.repository.GifCatalog
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUiState(
    val conversationId: String? = null,
    val conversation: Conversation? = null,
    val forwardTargets: List<Conversation> = emptyList(),
    val messages: List<Message> = emptyList(),
    val firstUnreadMessageId: String? = null,
    val searchResults: List<Message> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val hasMore: Boolean = true,
    val connectionState: RealtimeConnectionState = RealtimeConnectionState.DISCONNECTED,
    val error: String? = null,
    val groupInfo: GroupInfo? = null,
    val groupMembers: List<GroupMember> = emptyList(),
    val groupUserQuery: String = "",
    val groupUserResults: List<ChatUser>? = null,
    val isGroupUserSearching: Boolean = false,
    val groupUserSearchError: String? = null,
    val isGroupLoading: Boolean = false,
    val groupError: String? = null,
    val pinnedMessages: List<Message> = emptyList(),
    val sharedMedia: List<Message> = emptyList(),
    val blockStatus: BlockStatus? = null,
    val isModerationLoading: Boolean = false,
    val moderationError: String? = null,
    val downloads: Map<String, DownloadTask> = emptyMap(),
    val presence: ir.coffevista.vista_native.features.chat.domain.model.PresenceState? = null,
    val partnerProfile: ChatPartnerProfile? = null,
    val isPartnerProfileLoading: Boolean = false,
    val gifs: List<GifItem> = emptyList(),
    val gifQuery: String = "",
    val isLoadingGifs: Boolean = false,
    val gifError: String? = null,
    val secretAutoDeleteSeconds: Int = 0,
    val secretSafetyNumber: String? = null,
    val isSafetyNumberLoading: Boolean = false,
) {
    val isSecret: Boolean get() = conversation?.type == ConversationType.SECRET
}

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val sessionStore: SessionStore? = null,
    private val gifCatalog: GifCatalog = EmptyGifCatalog,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MessagesUiState())
    val state: StateFlow<MessagesUiState> = mutableState.asStateFlow()
    private var observer: Job? = null
    private var conversationObserver: Job? = null
    private var forwardTargetsObserver: Job? = null
    private var searchJob: Job? = null
    private var groupUserSearchJob: Job? = null
    private var gifSearchJob: Job? = null
    private var sharedMediaObserver: Job? = null
    private var downloadsObserver: Job? = null
    private var readAcknowledgement: Job? = null
    private var isConversationVisible = false
    private var lastReadAttemptedMessageId: String? = null
    private var lastTypingAt = 0L
    private var lastRealtimeState = repository.realtimeState.value
    private var loadedGroupId: String? = null
    private var loadedBlockPeerId: String? = null
    private var loadedPartnerProfilePeerId: String? = null
    private val autoDeleteJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    private var autoDeleteEnabledAtEpochMillis: Long = 0L
    private val messageReadOrShownTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val optimisticPendingMessages = java.util.concurrent.ConcurrentHashMap<String, Message>()

    init {
        viewModelScope.launch {
            repository.realtimeState.collectLatest { connection ->
                val reconnected = lastRealtimeState != RealtimeConnectionState.CONNECTED &&
                    connection == RealtimeConnectionState.CONNECTED
                lastRealtimeState = connection
                mutableState.update { it.copy(connectionState = connection) }
                if (reconnected && mutableState.value.conversationId != null) refresh()
            }
        }
    }

    fun bind(conversationId: String) {
        if (mutableState.value.conversationId == conversationId && observer?.isActive == true) return
        observer?.cancel()
        conversationObserver?.cancel()
        forwardTargetsObserver?.cancel()
        groupUserSearchJob?.cancel()
        sharedMediaObserver?.cancel()
        downloadsObserver?.cancel()
        readAcknowledgement?.cancel()
        lastReadAttemptedMessageId = null
        autoDeleteJobs.values.forEach { it.cancel() }
        autoDeleteJobs.clear()
        autoDeleteEnabledAtEpochMillis = 0L
        messageReadOrShownTimestamps.clear()
        loadedBlockPeerId = null
        loadedPartnerProfilePeerId = null
        mutableState.value = MessagesUiState(
            conversationId = conversationId,
            connectionState = repository.realtimeState.value,
        )
        observer = viewModelScope.launch {
            repository.observeMessages(conversationId).collectLatest { messages ->
                val persistedClientIds = messages.map { it.clientId }.toSet()
                persistedClientIds.forEach { optimisticPendingMessages.remove(it) }
                val remainingPending = optimisticPendingMessages.values.toList()
                val mergedMessages = remainingPending + messages.filterNot { m -> remainingPending.any { it.clientId == m.clientId } }
                val initialUnreadId = mutableState.value.firstUnreadMessageId ?: run {
                    mergedMessages.filter { !it.isMine && it.status != MessageStatus.READ }
                        .lastOrNull()?.stableKey
                }
                mutableState.update { current ->
                    current.copy(
                        messages = mergedMessages,
                        pinnedMessages = mergedMessages.filter(Message::isPinned),
                        firstUnreadMessageId = current.firstUnreadMessageId ?: initialUnreadId,
                        isInitialLoading = false,
                    )
                }
                acknowledgeVisibleUnread()
                val autoDelete = mutableState.value.secretAutoDeleteSeconds
                if (autoDelete > 0) {
                    scheduleSecretAutoDelete(mergedMessages, autoDelete)
                }
            }
        }
        conversationObserver = viewModelScope.launch {
            repository.observeConversation(conversationId).collectLatest { conversation ->
                mutableState.update { it.copy(conversation = conversation) }
                if (conversation?.type == ConversationType.SECRET) {
                    runCatching { repository.ensureSecretChatHandshake(conversationId) }
                    val autoDelete = runCatching { repository.getSecretAutoDeleteSeconds(conversationId) }.getOrDefault(0)
                    mutableState.update { it.copy(secretAutoDeleteSeconds = autoDelete) }
                    if (autoDelete > 0) {
                        scheduleSecretAutoDelete(mutableState.value.messages, autoDelete)
                    }
                }
                if (conversation?.type == ConversationType.GROUP && loadedGroupId != conversationId) {
                    loadedGroupId = conversationId
                    loadGroupDetails()
                }
                val peerId = conversation?.peerId
                if (conversation?.type != ConversationType.GROUP && !peerId.isNullOrBlank() && loadedBlockPeerId != peerId) {
                    loadedBlockPeerId = peerId
                    loadBlockStatus(peerId)
                    loadPresence(peerId)
                }
            }
        }
        forwardTargetsObserver = viewModelScope.launch {
            repository.observeConversations(includeArchived = false).collectLatest { conversations ->
                mutableState.update {
                    it.copy(forwardTargets = conversations.filterNot { candidate -> candidate.id == conversationId })
                }
            }
        }
        sharedMediaObserver = repository.observeSharedMedia(conversationId)
            .onEach { media -> mutableState.update { it.copy(sharedMedia = media) } }
            .launchIn(viewModelScope)
        downloadsObserver = repository.observeDownloads(conversationId)
            .onEach { tasks -> mutableState.update { it.copy(downloads = tasks.associateBy(DownloadTask::messageId)) } }
            .launchIn(viewModelScope)
        refresh()
        loadPinnedMessages()
        viewModelScope.launch {
            if (isConversationVisible) repository.setConversationActive(conversationId, true)
            acknowledgeVisibleUnread()
            val canonical = repository.findConversationByPeer(conversationId, isSecret = false)
            if (canonical != null && canonical.id != conversationId) {
                bind(canonical.id)
            }
        }
    }

    fun setConversationVisible(conversationId: String, visible: Boolean) {
        if (mutableState.value.conversationId != conversationId) {
            if (visible) isConversationVisible = true
            return
        }
        if (isConversationVisible == visible) return
        isConversationVisible = visible
        if (!visible) lastReadAttemptedMessageId = null
        viewModelScope.launch { repository.setConversationActive(conversationId, visible) }
        if (visible) acknowledgeVisibleUnread()
    }

    private fun acknowledgeVisibleUnread() {
        val current = mutableState.value
        val conversationId = current.conversationId ?: return
        if (!isConversationVisible || readAcknowledgement?.isActive == true) return
        val newestUnread = current.messages.firstOrNull { !it.isMine && it.status != MessageStatus.READ } ?: return
        if (newestUnread.clientId == lastReadAttemptedMessageId) return
        lastReadAttemptedMessageId = newestUnread.clientId
        readAcknowledgement = viewModelScope.launch {
            repository.markRead(conversationId)
        }.also { job ->
            job.invokeOnCompletion {
                viewModelScope.launch { acknowledgeVisibleUnread() }
            }
        }
    }

    private fun loadPresence(peerId: String) {
        viewModelScope.launch {
            val value = (repository.presence(peerId) as? ChatResult.Success)?.value ?: return@launch
            mutableState.update { it.copy(presence = value) }
        }
    }

    fun loadPartnerProfile(peerId: String) {
        val current = mutableState.value
        if (current.isPartnerProfileLoading || (loadedPartnerProfilePeerId == peerId && current.partnerProfile != null)) return
        loadedPartnerProfilePeerId = peerId
        viewModelScope.launch {
            mutableState.update { it.copy(isPartnerProfileLoading = true) }
            when (val result = repository.partnerProfile(peerId)) {
                is ChatResult.Success -> mutableState.update {
                    it.copy(partnerProfile = result.value, isPartnerProfileLoading = false)
                }
                is ChatResult.Failure -> {
                    loadedPartnerProfilePeerId = null
                    mutableState.update { it.copy(isPartnerProfileLoading = false) }
                }
            }
        }
    }

    fun startSecretChat(onOpened: (Conversation) -> Unit = {}) {
        val current = mutableState.value
        val peerId = current.conversation?.peerId?.trim()?.takeIf(String::isNotEmpty)
            ?: current.partnerProfile?.userId?.trim()?.takeIf(String::isNotEmpty)
            ?: ""
        if (peerId.isEmpty() || current.conversation?.type != ConversationType.PRIVATE) return
        viewModelScope.launch {
            val existing = repository.findConversationByPeer(peerId, isSecret = true)
            if (existing != null) {
                withContext(Dispatchers.Main.immediate) {
                    onOpened(existing)
                }
                return@launch
            }
            when (val result = repository.createConversation(peerId, isSecret = true)) {
                is ChatResult.Success -> withContext(Dispatchers.Main.immediate) {
                    onOpened(result.value)
                }
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun refresh() {
        val conversationId = mutableState.value.conversationId ?: return
        if (mutableState.value.isRefreshing) return
        viewModelScope.launch {
            mutableState.update { it.copy(isRefreshing = true, error = null) }
            when (val result = repository.refreshMessages(conversationId)) {
                is ChatResult.Success -> mutableState.update {
                    it.copy(isRefreshing = false, isInitialLoading = false, hasMore = result.value.hasMore)
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(isRefreshing = false, isInitialLoading = false)
                }
            }
        }
    }

    fun clearError() {
        mutableState.update { it.copy(error = null) }
    }

    fun loadPinnedMessages() {
        val conversationId = mutableState.value.conversationId ?: return
        viewModelScope.launch {
            val result = repository.getPinnedMessages(conversationId)
            if (result is ChatResult.Success) {
                mutableState.update { it.copy(pinnedMessages = result.value) }
            }
        }
    }

    fun loadOlder() {
        val current = mutableState.value
        val conversationId = current.conversationId ?: return
        if (current.isAppending || !current.hasMore) return
        viewModelScope.launch {
            mutableState.update { it.copy(isAppending = true) }
            when (val result = repository.loadOlderMessages(conversationId)) {
                is ChatResult.Success -> mutableState.update {
                    it.copy(isAppending = false, hasMore = result.value.hasMore)
                }
                is ChatResult.Failure -> mutableState.update { it.copy(isAppending = false) }
            }
        }
    }

    fun send(text: String, onAccepted: () -> Unit) {
        val conversationId = mutableState.value.conversationId ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        onAccepted()
        val clientId = UUID.randomUUID().toString()
        val myUserId = sessionStore?.read()?.userId
            ?: mutableState.value.messages.firstOrNull { it.isMine }?.senderId.orEmpty()
        val optimistic = Message(
            accountId = myUserId,
            conversationId = conversationId,
            clientId = clientId,
            serverId = null,
            senderId = myUserId,
            content = MessageContent.Text(trimmed),
            createdAtEpochMillis = System.currentTimeMillis(),
            status = MessageStatus.PENDING,
            isMine = true,
        )
        optimisticPendingMessages[clientId] = optimistic
        mutableState.update { current ->
            current.copy(
                messages = listOf(optimistic) + current.messages.filterNot { it.clientId == clientId },
                error = null,
            )
        }
        viewModelScope.launch {
            when (val result = repository.sendText(conversationId, trimmed, clientId)) {
                is ChatResult.Failure -> {
                    optimisticPendingMessages.remove(clientId)
                    mutableState.update { current ->
                        current.copy(
                            messages = current.messages.map {
                                if (it.clientId == clientId) it.copy(status = MessageStatus.FAILED) else it
                            },
                        )
                    }
                }
                is ChatResult.Success -> Unit
            }
        }
    }

    fun sendReply(text: String, replyTo: Message, onAccepted: () -> Unit) {
        val conversationId = mutableState.value.conversationId ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        onAccepted()
        val clientId = UUID.randomUUID().toString()
        val myUserId = sessionStore?.read()?.userId
            ?: mutableState.value.messages.firstOrNull { it.isMine }?.senderId.orEmpty()
        val rawReplyContent = when (val c = replyTo.content) {
            is MessageContent.Text -> c.value
            is MessageContent.Structured -> c.payload
            MessageContent.Deleted -> "این پیام حذف شده است"
            MessageContent.EncryptedUnavailable -> "پیام محرمانه"
        }
        val replyContent = SharedPostParser.tryParse(rawReplyContent)?.let { post ->
            "پست ${post.authorName}: ${post.content.ifBlank { "رسانه پیوست" }}"
        } ?: rawReplyContent
        val optimistic = Message(
            accountId = myUserId,
            conversationId = conversationId,
            clientId = clientId,
            serverId = null,
            senderId = myUserId,
            content = MessageContent.Text(trimmed),
            createdAtEpochMillis = System.currentTimeMillis(),
            status = MessageStatus.PENDING,
            replyToMessageId = replyTo.serverId ?: replyTo.clientId,
            replyToContent = replyContent,
            replyToSenderName = if (replyTo.isMine) "شما" else mutableState.value.conversation?.title.orEmpty().ifBlank { "کاربر" },
            replyToKind = replyTo.attachment?.kind?.name?.lowercase() ?: "text",
            isMine = true,
        )
        optimisticPendingMessages[clientId] = optimistic
        mutableState.update { current ->
            current.copy(
                messages = listOf(optimistic) + current.messages.filterNot { it.clientId == clientId },
                error = null,
            )
        }
        viewModelScope.launch {
            when (val result = repository.sendReply(conversationId, trimmed, replyTo, clientId)) {
                is ChatResult.Failure -> {
                    optimisticPendingMessages.remove(clientId)
                    mutableState.update { current ->
                        current.copy(
                            messages = current.messages.map {
                                if (it.clientId == clientId) it.copy(status = MessageStatus.FAILED) else it
                            },
                        )
                    }
                }
                is ChatResult.Success -> Unit
            }
        }
    }

    fun retry(message: Message) {
        viewModelScope.launch {
            when (val result = repository.retryMessage(message.conversationId, message.clientId)) {
                is ChatResult.Success -> Unit
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun sendAttachment(draft: ChatAttachmentDraft) {
        val conversationId = mutableState.value.conversationId ?: return
        viewModelScope.launch {
            repository.sendAttachment(conversationId, draft)
        }
    }

    fun loadTrendingGifs(force: Boolean = false) {
        if (!force && (mutableState.value.gifs.isNotEmpty() || mutableState.value.isLoadingGifs)) return
        gifSearchJob?.cancel()
        gifSearchJob = viewModelScope.launch {
            mutableState.update { it.copy(gifQuery = "", isLoadingGifs = true, gifError = null) }
            runCatching { gifCatalog.trending() }
                .onSuccess { gifs -> mutableState.update { it.copy(gifs = gifs, isLoadingGifs = false) } }
                .onFailure { mutableState.update { it.copy(gifs = emptyList(), isLoadingGifs = false, gifError = "خطا در بارگذاری گیف‌ها") } }
        }
    }

    fun searchGifs(query: String) {
        gifSearchJob?.cancel()
        mutableState.update { it.copy(gifQuery = query, gifError = null) }
        if (query.trim().isEmpty()) {
            loadTrendingGifs(force = true)
            return
        }
        gifSearchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            mutableState.update { it.copy(isLoadingGifs = true, gifs = emptyList()) }
            runCatching { gifCatalog.search(query) }
                .onSuccess { gifs ->
                    mutableState.update { current ->
                        if (current.gifQuery == query) current.copy(gifs = gifs, isLoadingGifs = false) else current
                    }
                }
                .onFailure {
                    mutableState.update { current ->
                        if (current.gifQuery == query) current.copy(isLoadingGifs = false, gifError = "خطا در بارگذاری گیف‌ها") else current
                    }
                }
        }
    }

    fun sendGif(url: String) {
        val conversationId = mutableState.value.conversationId ?: return
        viewModelScope.launch {
            when (val result = repository.sendRemoteGif(conversationId, url)) {
                is ChatResult.Success -> Unit
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun startDownload(message: Message) = downloadMutation { repository.startDownload(message) }

    fun pauseDownload(messageId: String) = downloadMutation { repository.pauseDownload(messageId) }

    fun resumeDownload(messageId: String) = downloadMutation { repository.resumeDownload(messageId) }

    fun cancelDownload(messageId: String) = downloadMutation { repository.cancelDownload(messageId) }

    private fun downloadMutation(block: suspend () -> ChatResult<*>) {
        viewModelScope.launch {
            val result = block()
            if (result is ChatResult.Failure) {
                mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun composerChanged(value: String) {
        if (value.isBlank()) return
        val conversationId = mutableState.value.conversationId ?: return
        val current = System.currentTimeMillis()
        if (current - lastTypingAt < TYPING_THROTTLE_MILLIS) return
        lastTypingAt = current
        viewModelScope.launch { repository.sendTyping(conversationId) }
    }

    fun search(query: String) {
        val conversationId = mutableState.value.conversationId ?: return
        val normalized = query.trim()
        searchJob?.cancel()
        mutableState.update { it.copy(searchQuery = query, error = null) }
        if (normalized.length < 2) {
            mutableState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            mutableState.update { it.copy(isSearching = true) }
            kotlinx.coroutines.delay(300)
            when (val result = repository.searchMessages(conversationId, normalized)) {
                is ChatResult.Success -> mutableState.update {
                    if (it.searchQuery.trim() == normalized) {
                        it.copy(searchResults = result.value, isSearching = false)
                    } else it
                }
                is ChatResult.Failure -> mutableState.update {
                    if (it.searchQuery.trim() == normalized) {
                        it.copy(isSearching = false)
                    } else it
                }
            }
        }
    }

    fun edit(message: Message, content: String) {
        viewModelScope.launch {
            val result = repository.editMessage(message, content)
            if (result is ChatResult.Failure) {
                mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun delete(message: Message, forEveryone: Boolean) {
        val targetId = message.serverId ?: message.clientId
        optimisticPendingMessages.remove(message.clientId)
        mutableState.update { current ->
            current.copy(
                messages = current.messages.filterNot { (it.serverId ?: it.clientId) == targetId },
                pinnedMessages = current.pinnedMessages.filterNot { (it.serverId ?: it.clientId) == targetId },
            )
        }
        viewModelScope.launch {
            val result = repository.deleteMessage(message, forEveryone)
            if (result is ChatResult.Failure) {
                mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun deleteConversation(conversationId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteConversation(conversationId)
            onComplete(result is ChatResult.Success)
        }
    }

    fun react(message: Message, emoji: String) {
        viewModelScope.launch {
            val result = repository.toggleReaction(message, emoji)
            if (result is ChatResult.Failure) {
                mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun forward(message: Message, targetConversationId: String) {
        val messageId = message.serverId ?: return
        viewModelScope.launch {
            val result = repository.forwardMessage(messageId, targetConversationId)
            if (result is ChatResult.Failure) {
                mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun loadGroupDetails() {
        val conversationId = mutableState.value.conversationId ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(isGroupLoading = true, groupError = null) }
            val info = repository.groupInfo(conversationId)
            val members = repository.groupMembers(conversationId)
            mutableState.update { current ->
                current.copy(
                    groupInfo = (info as? ChatResult.Success)?.value ?: current.groupInfo,
                    groupMembers = (members as? ChatResult.Success)?.value ?: current.groupMembers,
                    isGroupLoading = false,
                    groupError = (info as? ChatResult.Failure)?.message
                        ?: (members as? ChatResult.Failure)?.message,
                )
            }
        }
    }

    fun updateGroupName(name: String) = groupMutation {
        val conversationId = mutableState.value.conversationId ?: return@groupMutation ChatResult.Failure("گروه یافت نشد", false)
        repository.updateGroup(conversationId, name = name)
    }

    fun addGroupMembers(userIds: List<String>) = groupMutation {
        val conversationId = mutableState.value.conversationId ?: return@groupMutation ChatResult.Failure("گروه یافت نشد", false)
        repository.addGroupMembers(conversationId, userIds)
    }

    fun searchGroupUsers(value: String) {
        groupUserSearchJob?.cancel()
        val query = value.trim()
        mutableState.update {
            it.copy(
                groupUserQuery = value,
                groupUserResults = null,
                isGroupUserSearching = query.isNotEmpty(),
                groupUserSearchError = null,
            )
        }
        if (query.isEmpty()) return

        groupUserSearchJob = viewModelScope.launch {
            delay(GROUP_USER_SEARCH_DEBOUNCE_MILLIS)
            when (val result = repository.searchUsers(query)) {
                is ChatResult.Success -> mutableState.update {
                    if (it.groupUserQuery.trim() == query) {
                        it.copy(
                            groupUserResults = result.value,
                            isGroupUserSearching = false,
                            groupUserSearchError = null,
                        )
                    } else {
                        it
                    }
                }
                is ChatResult.Failure -> mutableState.update {
                    if (it.groupUserQuery.trim() == query) {
                        it.copy(
                            isGroupUserSearching = false,
                            groupUserSearchError = result.message,
                        )
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun removeGroupMember(userId: String) = groupMutation {
        val conversationId = mutableState.value.conversationId ?: return@groupMutation ChatResult.Failure("گروه یافت نشد", false)
        repository.removeGroupMember(conversationId, userId)
    }

    fun setGroupAdmin(userId: String, makeAdmin: Boolean) = groupMutation {
        val conversationId = mutableState.value.conversationId ?: return@groupMutation ChatResult.Failure("گروه یافت نشد", false)
        repository.setGroupAdmin(conversationId, userId, makeAdmin)
    }

    fun setGroupInviteEnabled(enabled: Boolean) = groupMutation {
        val conversationId = mutableState.value.conversationId ?: return@groupMutation ChatResult.Failure("گروه یافت نشد", false)
        repository.setGroupInviteEnabled(conversationId, enabled)
    }

    fun regenerateGroupInvite() = groupMutation {
        val conversationId = mutableState.value.conversationId ?: return@groupMutation ChatResult.Failure("گروه یافت نشد", false)
        repository.regenerateGroupInvite(conversationId)
    }

    fun leaveGroup(onComplete: (Boolean) -> Unit) = terminalGroupMutation(onComplete) { conversationId ->
        repository.leaveGroup(conversationId)
    }

    fun deleteGroup(onComplete: (Boolean) -> Unit) = terminalGroupMutation(onComplete) { conversationId ->
        repository.deleteGroup(conversationId)
    }

    private fun groupMutation(block: suspend () -> ChatResult<*>) {
        if (mutableState.value.isGroupLoading) return
        viewModelScope.launch {
            mutableState.update { it.copy(isGroupLoading = true, groupError = null) }
            when (val result = block()) {
                is ChatResult.Success -> {
                    mutableState.update { it.copy(isGroupLoading = false) }
                    loadedGroupId = null
                    loadGroupDetails()
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(isGroupLoading = false, groupError = result.message)
                }
            }
        }
    }

    private fun terminalGroupMutation(
        onComplete: (Boolean) -> Unit,
        block: suspend (String) -> ChatResult<Unit>,
    ) {
        val conversationId = mutableState.value.conversationId ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(isGroupLoading = true, groupError = null) }
            when (val result = block(conversationId)) {
                is ChatResult.Success -> onComplete(true)
                is ChatResult.Failure -> {
                    mutableState.update { it.copy(isGroupLoading = false, groupError = result.message) }
                    onComplete(false)
                }
            }
        }
    }

    fun cancelTransfer(message: Message) {
        viewModelScope.launch {
            when (val result = repository.cancelTransfer(message.conversationId, message.clientId)) {
                is ChatResult.Success -> Unit
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun setForeground(active: Boolean) {
        viewModelScope.launch {
            repository.setForeground(active)
            mutableState.value.conversationId?.let { repository.setConversationActive(it, active) }
        }
    }

    fun loadBlockStatus(userId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            when (val result = repository.getBlockStatus(userId, forceRefresh)) {
                is ChatResult.Success -> mutableState.update {
                    it.copy(blockStatus = result.value, isModerationLoading = false, moderationError = null)
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(isModerationLoading = false, moderationError = result.message)
                }
            }
        }
    }

    fun toggleBlock(userId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            mutableState.update { it.copy(isModerationLoading = true, moderationError = null) }
            val result = if (mutableState.value.blockStatus?.isBlocked == true) {
                repository.unblockUser(userId)
            } else {
                repository.blockUser(userId)
            }
            when (result) {
                is ChatResult.Success -> {
                    loadBlockStatus(userId, forceRefresh = true)
                    onComplete(true)
                }
                is ChatResult.Failure -> {
                    mutableState.update { it.copy(isModerationLoading = false, moderationError = result.message) }
                    onComplete(false)
                }
            }
        }
    }

    fun reportUser(
        userId: String,
        reason: ModerationReason,
        additionalDetails: String?,
        onComplete: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            mutableState.update { it.copy(isModerationLoading = true, moderationError = null) }
            when (val result = repository.reportUser(userId, reason, additionalDetails)) {
                is ChatResult.Success -> {
                    mutableState.update { it.copy(isModerationLoading = false) }
                    onComplete(true)
                }
                is ChatResult.Failure -> {
                    mutableState.update { it.copy(isModerationLoading = false, moderationError = result.message) }
                    onComplete(false)
                }
            }
        }
    }

    fun toggleMute() {
        val conversationId = mutableState.value.conversationId ?: return
        viewModelScope.launch {
            when (val result = repository.toggleConversationFlag(conversationId, "mute")) {
                is ChatResult.Success -> mutableState.update { it.copy(conversation = result.value) }
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun togglePinned(message: Message) {
        val conversationId = mutableState.value.conversationId ?: return
        val messageId = message.serverId ?: return
        viewModelScope.launch {
            val result = if (message.isPinned) {
                repository.unpinMessage(conversationId, messageId)
            } else {
                repository.pinMessage(conversationId, messageId)
            }
            when (result) {
                is ChatResult.Success -> loadPinnedMessages()
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun setSecretAutoDelete(seconds: Int) {
        val conversationId = mutableState.value.conversationId ?: return
        viewModelScope.launch {
            repository.setSecretAutoDeleteSeconds(conversationId, seconds)
            autoDeleteEnabledAtEpochMillis = if (seconds > 0) System.currentTimeMillis() else 0L
            mutableState.update { it.copy(secretAutoDeleteSeconds = seconds) }
            scheduleSecretAutoDelete(mutableState.value.messages, seconds)
        }
    }

    fun computeSafetyNumber(onResult: (String?) -> Unit = {}) {
        val conversationId = mutableState.value.conversationId ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(isSafetyNumberLoading = true) }
            val number = repository.computeSecretSafetyNumber(conversationId)
            mutableState.update { it.copy(isSafetyNumberLoading = false, secretSafetyNumber = number) }
            onResult(number)
        }
    }

    private fun scheduleSecretAutoDelete(messages: List<Message>, seconds: Int) {
        if (seconds <= 0) {
            autoDeleteJobs.values.forEach { it.cancel() }
            autoDeleteJobs.clear()
            return
        }
        val now = System.currentTimeMillis()
        val currentIds = messages.mapTo(mutableSetOf()) { it.serverId ?: it.clientId }
        val iterator = autoDeleteJobs.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!currentIds.contains(entry.key)) {
                entry.value.cancel()
                iterator.remove()
            }
        }

        for (message in messages) {
            val targetId = message.serverId ?: message.clientId
            // Only apply auto-delete to messages created after the timer was enabled
            if (autoDeleteEnabledAtEpochMillis > 0L && message.createdAtEpochMillis < autoDeleteEnabledAtEpochMillis) continue
            if (message.status == MessageStatus.PENDING || message.status == MessageStatus.FAILED || autoDeleteJobs.containsKey(targetId)) continue

            // Auto-delete countdown starts when the message is read (MessageStatus.READ) or displayed
            val isReadOrSeen = message.status == MessageStatus.READ || !message.isMine
            if (!isReadOrSeen) continue

            val baseTime = messageReadOrShownTimestamps.getOrPut(targetId) { now }
            val expiresAt = baseTime + seconds * 1000L
            val delayMs = expiresAt - now
            if (delayMs <= 0L) {
                viewModelScope.launch {
                    runCatching { repository.deleteMessage(message, forEveryone = true) }
                }
            } else {
                autoDeleteJobs[targetId] = viewModelScope.launch {
                    try {
                        delay(delayMs)
                        autoDeleteJobs.remove(targetId)
                        runCatching { repository.deleteMessage(message, forEveryone = true) }
                    } catch (_: kotlinx.coroutines.CancellationException) {
                        // ignore cancellation
                    } catch (_: Exception) {
                        // ignore failures
                    }
                }
            }
        }
    }

    override fun onCleared() {
        autoDeleteJobs.values.forEach { it.cancel() }
        autoDeleteJobs.clear()
        autoDeleteEnabledAtEpochMillis = 0L
        messageReadOrShownTimestamps.clear()
        mutableState.value.conversationId?.let { conversationId ->
            viewModelScope.launch { repository.setConversationActive(conversationId, false) }
        }
        super.onCleared()
    }

    private companion object {
        const val TYPING_THROTTLE_MILLIS = 2_000L
        const val GROUP_USER_SEARCH_DEBOUNCE_MILLIS = 500L
    }
}

private object EmptyGifCatalog : GifCatalog {
    override suspend fun trending(limit: Int): List<GifItem> = emptyList()
    override suspend fun search(query: String, limit: Int): List<GifItem> = emptyList()
}
