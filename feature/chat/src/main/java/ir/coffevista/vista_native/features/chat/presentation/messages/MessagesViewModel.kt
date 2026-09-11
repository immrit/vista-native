package ir.coffevista.vista_native.features.chat.presentation.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import ir.coffevista.vista_native.features.chat.domain.model.ChatPartnerProfile
import ir.coffevista.vista_native.features.chat.domain.model.BlockStatus
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.GroupInfo
import ir.coffevista.vista_native.features.chat.domain.model.GroupMember
import ir.coffevista.vista_native.features.chat.domain.model.GifItem
import ir.coffevista.vista_native.features.chat.domain.model.DownloadTask
import ir.coffevista.vista_native.features.chat.domain.model.ModerationReason
import ir.coffevista.vista_native.features.chat.domain.model.RealtimeConnectionState
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import ir.coffevista.vista_native.features.chat.domain.repository.GifCatalog
import kotlinx.coroutines.Job
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
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val gifCatalog: GifCatalog = EmptyGifCatalog,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MessagesUiState())
    val state: StateFlow<MessagesUiState> = mutableState.asStateFlow()
    private var observer: Job? = null
    private var conversationObserver: Job? = null
    private var forwardTargetsObserver: Job? = null
    private var searchJob: Job? = null
    private var gifSearchJob: Job? = null
    private var sharedMediaObserver: Job? = null
    private var downloadsObserver: Job? = null
    private var lastTypingAt = 0L
    private var lastRealtimeState = repository.realtimeState.value
    private var loadedGroupId: String? = null
    private var loadedBlockPeerId: String? = null
    private var loadedPartnerProfilePeerId: String? = null

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
        sharedMediaObserver?.cancel()
        downloadsObserver?.cancel()
        loadedBlockPeerId = null
        loadedPartnerProfilePeerId = null
        mutableState.value = MessagesUiState(
            conversationId = conversationId,
            connectionState = repository.realtimeState.value,
        )
        observer = viewModelScope.launch {
            repository.observeMessages(conversationId).collectLatest { messages ->
                mutableState.update { current ->
                    current.copy(
                        messages = messages,
                        pinnedMessages = messages.filter(Message::isPinned),
                        isInitialLoading = false,
                    )
                }
            }
        }
        conversationObserver = viewModelScope.launch {
            repository.observeConversation(conversationId).collectLatest { conversation ->
                mutableState.update { it.copy(conversation = conversation) }
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
            repository.setConversationActive(conversationId, true)
            repository.markRead(conversationId)
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
        val peerId = current.conversation?.peerId?.trim().orEmpty()
        if (peerId.isEmpty() || current.conversation?.type != ConversationType.PRIVATE) return
        viewModelScope.launch {
            when (val result = repository.createConversation(peerId, isSecret = true)) {
                is ChatResult.Success -> onOpened(result.value)
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
                    it.copy(isRefreshing = false, isInitialLoading = false, error = result.message)
                }
            }
        }
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
                is ChatResult.Failure -> mutableState.update { it.copy(isAppending = false, error = result.message) }
            }
        }
    }

    fun send(text: String, onAccepted: () -> Unit) {
        val conversationId = mutableState.value.conversationId ?: return
        if (text.isBlank()) return
        onAccepted()
        viewModelScope.launch {
            if (repository.sendText(conversationId, text) is ChatResult.Failure) {
                // The failed optimistic row is the actionable error surface.
            }
        }
    }

    fun sendReply(text: String, replyTo: Message, onAccepted: () -> Unit) {
        val conversationId = mutableState.value.conversationId ?: return
        if (text.isBlank()) return
        onAccepted()
        viewModelScope.launch {
            when (val result = repository.sendReply(conversationId, text, replyTo)) {
                is ChatResult.Success -> Unit
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
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
            when (val result = repository.sendAttachment(conversationId, draft)) {
                is ChatResult.Success -> Unit
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
            }
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
                        it.copy(isSearching = false, error = result.message)
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
        viewModelScope.launch {
            val result = repository.deleteMessage(message, forEveryone)
            if (result is ChatResult.Failure) {
                mutableState.update { it.copy(error = result.message) }
            }
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

    fun addGroupMember(userId: String) = groupMutation {
        val conversationId = mutableState.value.conversationId ?: return@groupMutation ChatResult.Failure("گروه یافت نشد", false)
        repository.addGroupMembers(conversationId, listOf(userId))
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

    override fun onCleared() {
        mutableState.value.conversationId?.let { conversationId ->
            viewModelScope.launch { repository.setConversationActive(conversationId, false) }
        }
        super.onCleared()
    }

    private companion object {
        const val TYPING_THROTTLE_MILLIS = 2_000L
    }
}

private object EmptyGifCatalog : GifCatalog {
    override suspend fun trending(limit: Int): List<GifItem> = emptyList()
    override suspend fun search(query: String, limit: Int): List<GifItem> = emptyList()
}
