package ir.coffevista.vista_native.features.chat.presentation.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.BlockStatus
import ir.coffevista.vista_native.features.chat.domain.model.RealtimeConnectionState
import ir.coffevista.vista_native.features.chat.domain.model.ProfileNote
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val hasMore: Boolean = true,
    val isOffline: Boolean = false,
    val connectionState: RealtimeConnectionState = RealtimeConnectionState.DISCONNECTED,
    val includeArchived: Boolean = false,
    val error: String? = null,
    val profileNotes: List<ProfileNote> = emptyList(),
    val isNotesLoading: Boolean = true,
    val isNoteSaving: Boolean = false,
    val isNoteReplySending: Boolean = false,
    val noteError: String? = null,
    val requestActionConversationIds: Set<String> = emptySet(),
    val actionFeedback: String? = null,
    val blockStatusByUserId: Map<String, BlockStatus> = emptyMap(),
    val blockStatusLoadingUserIds: Set<String> = emptySet(),
    val blockActionUserIds: Set<String> = emptySet(),
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConversationsUiState())
    val state: StateFlow<ConversationsUiState> = mutableState.asStateFlow()
    private var observer: Job? = null
    private var lastRealtimeState = repository.realtimeState.value
    private val requestActionsInFlight = mutableSetOf<String>()
    private val blockActionsInFlight = mutableSetOf<String>()
    private val blockStatusRequestsInFlight = mutableSetOf<String>()
    private var lastConversationSyncFailedRetryably = false

    init {
        bind()
        viewModelScope.launch {
            repository.realtimeState.collectLatest { connection ->
                val reconnected = lastRealtimeState != RealtimeConnectionState.CONNECTED &&
                    connection == RealtimeConnectionState.CONNECTED
                lastRealtimeState = connection
                mutableState.update {
                    it.copy(
                        connectionState = connection,
                    )
                }
                if (reconnected) refresh()
            }
        }
        refresh()
    }

    fun bind(includeArchived: Boolean = false) {
        observer?.cancel()
        mutableState.update { it.copy(includeArchived = includeArchived) }
        observer = viewModelScope.launch {
            combine(
                repository.observeConversations(includeArchived),
                repository.observeProfileNotes(),
            ) { conversations, notes -> conversations to notes }.collectLatest { (conversations, notes) ->
                // The repository flag means "include archived" and therefore
                // returns both folders when true. This screen flag means
                // "show the archived folder", so enforce the exact folder at
                // the presentation boundary instead of leaking main chats into it.
                val folderConversations = conversations
                    .filter { conversation -> conversation.isArchived == includeArchived }
                mutableState.update {
                    it.copy(
                        conversations = folderConversations,
                        profileNotes = notes.filterNot { it.isExpired() },
                        isNotesLoading = false,
                        isInitialLoading = false,
                        isOffline = lastConversationSyncFailedRetryably && folderConversations.isNotEmpty(),
                        error = if (folderConversations.isNotEmpty()) null else it.error,
                    )
                }
            }
        }
    }

    fun showArchived(show: Boolean) = bind(includeArchived = show)

    fun toggleFlag(conversationId: String, flag: String) {
        viewModelScope.launch {
            when (val result = repository.toggleConversationFlag(conversationId, flag)) {
                is ChatResult.Success -> mutableState.update {
                    it.copy(
                        actionFeedback = when (flag) {
                            "archive" -> if (result.value.isArchived) {
                                "گفتگو بایگانی شد"
                            } else {
                                "گفتگو از بایگانی خارج شد"
                            }
                            "pin" -> if (result.value.isPinned) "گفتگو سنجاق شد" else "سنجاق گفتگو برداشته شد"
                            "mute" -> if (result.value.isMuted) "گفتگو بی‌صدا شد" else "صدای گفتگو فعال شد"
                            else -> null
                        },
                    )
                }
                is ChatResult.Failure -> mutableState.update { it.copy(actionFeedback = result.message) }
            }
        }
    }

    fun deleteConversation(conversationId: String, complete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = repository.deleteConversation(conversationId)) {
                is ChatResult.Success -> complete(true)
                is ChatResult.Failure -> {
                    mutableState.update { it.copy(error = result.message) }
                    complete(false)
                }
            }
        }
    }

    fun acceptRequest(conversationId: String) = respondToRequest(conversationId, accept = true)

    fun rejectRequest(conversationId: String) = respondToRequest(conversationId, accept = false)

    private fun respondToRequest(conversationId: String, accept: Boolean) {
        if (!requestActionsInFlight.add(conversationId)) return
        viewModelScope.launch {
            try {
                mutableState.update {
                    it.copy(
                        requestActionConversationIds = it.requestActionConversationIds + conversationId,
                    actionFeedback = null,
                    )
                }
                val result = if (accept) {
                    repository.acceptMessageRequest(conversationId)
                } else {
                    repository.rejectMessageRequest(conversationId)
                }
                when (result) {
                    is ChatResult.Success -> {
                        mutableState.update {
                            it.copy(
                                conversations = if (accept) {
                                    it.conversations.map { conversation ->
                                        if (conversation.id == conversationId) {
                                            conversation.copy(isMessageRequest = false, requestStatus = "accepted")
                                        } else {
                                            conversation
                                        }
                                    }
                                } else {
                                    it.conversations.filterNot { conversation -> conversation.id == conversationId }
                                },
                                actionFeedback = if (accept) "درخواست پیام پذیرفته شد" else "درخواست پیام رد شد",
                            )
                        }
                        refresh()
                    }
                    is ChatResult.Failure -> mutableState.update {
                        it.copy(actionFeedback = result.message)
                    }
                }
            } finally {
                requestActionsInFlight -= conversationId
                mutableState.update {
                    it.copy(requestActionConversationIds = it.requestActionConversationIds - conversationId)
                }
            }
        }
    }

    fun clearActionFeedback() {
        mutableState.update { it.copy(actionFeedback = null) }
    }

    fun prepareBlockStatus(userId: String) {
        val normalizedId = userId.trim()
        if (normalizedId.isEmpty() || !blockStatusRequestsInFlight.add(normalizedId)) return
        viewModelScope.launch {
            try {
                mutableState.update {
                    it.copy(blockStatusLoadingUserIds = it.blockStatusLoadingUserIds + normalizedId)
                }
                when (val result = repository.getBlockStatus(normalizedId, forceRefresh = true)) {
                    is ChatResult.Success -> mutableState.update {
                        it.copy(blockStatusByUserId = it.blockStatusByUserId + (normalizedId to result.value))
                    }
                    is ChatResult.Failure -> mutableState.update { it.copy(actionFeedback = result.message) }
                }
            } finally {
                blockStatusRequestsInFlight -= normalizedId
                mutableState.update {
                    it.copy(blockStatusLoadingUserIds = it.blockStatusLoadingUserIds - normalizedId)
                }
            }
        }
    }

    fun toggleBlock(userId: String, complete: (Boolean) -> Unit = {}) {
        val normalizedId = userId.trim()
        val currentStatus = mutableState.value.blockStatusByUserId[normalizedId] ?: return
        if (!blockActionsInFlight.add(normalizedId)) return
        viewModelScope.launch {
            try {
                mutableState.update { it.copy(blockActionUserIds = it.blockActionUserIds + normalizedId) }
                val result = if (currentStatus.isBlocked) {
                    repository.unblockUser(normalizedId)
                } else {
                    repository.blockUser(normalizedId)
                }
                when (result) {
                    is ChatResult.Success -> {
                        val nextStatus = currentStatus.copy(isBlocked = !currentStatus.isBlocked)
                        mutableState.update {
                            it.copy(
                                blockStatusByUserId = it.blockStatusByUserId + (normalizedId to nextStatus),
                                actionFeedback = if (nextStatus.isBlocked) "کاربر مسدود شد" else "مسدودیت کاربر برداشته شد",
                            )
                        }
                        refresh()
                        complete(true)
                    }
                    is ChatResult.Failure -> {
                        mutableState.update { it.copy(actionFeedback = result.message) }
                        complete(false)
                    }
                }
            } finally {
                blockActionsInFlight -= normalizedId
                mutableState.update { it.copy(blockActionUserIds = it.blockActionUserIds - normalizedId) }
            }
        }
    }

    fun refresh() {
        if (mutableState.value.isRefreshing) return
        viewModelScope.launch {
            mutableState.update { it.copy(isRefreshing = true, error = null) }
            when (val result = repository.refreshConversations()) {
                is ChatResult.Success -> {
                    lastConversationSyncFailedRetryably = false
                    mutableState.update {
                        it.copy(
                            isRefreshing = false,
                            isInitialLoading = false,
                            hasMore = result.value.hasMore,
                            isOffline = false,
                        )
                    }
                }
                is ChatResult.Failure -> {
                    lastConversationSyncFailedRetryably = result.retryable
                    mutableState.update {
                        it.copy(
                            isRefreshing = false,
                            isInitialLoading = false,
                            isOffline = result.retryable && it.conversations.isNotEmpty(),
                            error = result.message,
                        )
                    }
                }
            }
            when (val notes = repository.refreshProfileNotes()) {
                is ChatResult.Success -> mutableState.update { it.copy(isNotesLoading = false, noteError = null) }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(isNotesLoading = false, noteError = notes.message)
                }
            }
        }
    }

    fun saveOwnNote(content: String, onComplete: (Boolean) -> Unit) {
        if (mutableState.value.isNoteSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(isNoteSaving = true, noteError = null) }
            when (val result = repository.upsertOwnProfileNote(content)) {
                is ChatResult.Success -> {
                    mutableState.update { it.copy(isNoteSaving = false, noteError = null) }
                    onComplete(true)
                }
                is ChatResult.Failure -> {
                    mutableState.update { it.copy(isNoteSaving = false, noteError = result.message) }
                    onComplete(false)
                }
            }
        }
    }

    fun deleteOwnNote(onComplete: (Boolean) -> Unit) {
        if (mutableState.value.isNoteSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(isNoteSaving = true, noteError = null) }
            when (val result = repository.deleteOwnProfileNote()) {
                is ChatResult.Success -> {
                    mutableState.update { it.copy(isNoteSaving = false, noteError = null) }
                    onComplete(true)
                }
                is ChatResult.Failure -> {
                    mutableState.update { it.copy(isNoteSaving = false, noteError = result.message) }
                    onComplete(false)
                }
            }
        }
    }

    fun replyToNote(
        note: ProfileNote,
        conversation: Conversation,
        text: String,
        onComplete: (Boolean) -> Unit,
    ) {
        if (mutableState.value.isNoteReplySending || text.isBlank()) return
        viewModelScope.launch {
            mutableState.update { it.copy(isNoteReplySending = true, noteError = null) }
            when (
                val result = repository.replyToProfileNote(
                    conversationId = conversation.id,
                    note = note,
                    senderName = conversation.title,
                    text = text,
                )
            ) {
                is ChatResult.Success -> {
                    mutableState.update { it.copy(isNoteReplySending = false, noteError = null) }
                    onComplete(true)
                }
                is ChatResult.Failure -> {
                    mutableState.update { it.copy(isNoteReplySending = false, noteError = result.message) }
                    onComplete(false)
                }
            }
        }
    }

    fun loadMore() {
        val current = mutableState.value
        if (current.isAppending || !current.hasMore) return
        viewModelScope.launch {
            mutableState.update { it.copy(isAppending = true) }
            when (val result = repository.loadMoreConversations()) {
                is ChatResult.Success -> {
                    lastConversationSyncFailedRetryably = false
                    mutableState.update {
                        it.copy(isAppending = false, hasMore = result.value.hasMore, isOffline = false)
                    }
                }
                is ChatResult.Failure -> {
                    lastConversationSyncFailedRetryably = result.retryable
                    mutableState.update {
                        it.copy(
                            isAppending = false,
                            isOffline = result.retryable && it.conversations.isNotEmpty(),
                            error = result.message,
                        )
                    }
                }
            }
        }
    }

    fun setForeground(active: Boolean) {
        viewModelScope.launch { repository.setForeground(active) }
    }
}
