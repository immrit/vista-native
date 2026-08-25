package ir.coffevista.vista_native.features.chat.presentation.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
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
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ConversationsUiState())
    val state: StateFlow<ConversationsUiState> = mutableState.asStateFlow()
    private var observer: Job? = null
    private var lastRealtimeState = repository.realtimeState.value

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
                        isOffline = connection == RealtimeConnectionState.DISCONNECTED ||
                            connection == RealtimeConnectionState.FAILED,
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
                mutableState.update {
                    it.copy(
                        conversations = conversations,
                        profileNotes = notes.filterNot { it.isExpired() },
                        isNotesLoading = false,
                        isInitialLoading = false,
                        error = if (conversations.isNotEmpty()) null else it.error,
                    )
                }
            }
        }
    }

    fun showArchived(show: Boolean) = bind(includeArchived = show)

    fun toggleFlag(conversationId: String, flag: String) {
        viewModelScope.launch {
            when (val result = repository.toggleConversationFlag(conversationId, flag)) {
                is ChatResult.Success -> Unit
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
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

    fun acceptRequest(conversationId: String) {
        viewModelScope.launch {
            when (val result = repository.acceptMessageRequest(conversationId)) {
                is ChatResult.Success -> refresh()
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun rejectRequest(conversationId: String) {
        viewModelScope.launch {
            when (val result = repository.rejectMessageRequest(conversationId)) {
                is ChatResult.Success -> refresh()
                is ChatResult.Failure -> mutableState.update { it.copy(error = result.message) }
            }
        }
    }

    fun refresh() {
        if (mutableState.value.isRefreshing) return
        viewModelScope.launch {
            mutableState.update { it.copy(isRefreshing = true, error = null) }
            when (val result = repository.refreshConversations()) {
                is ChatResult.Success -> mutableState.update {
                    it.copy(isRefreshing = false, isInitialLoading = false, hasMore = result.value.hasMore)
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(isRefreshing = false, isInitialLoading = false, error = result.message)
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
                is ChatResult.Success -> mutableState.update {
                    it.copy(isAppending = false, hasMore = result.value.hasMore)
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(isAppending = false, error = result.message)
                }
            }
        }
    }

    fun setForeground(active: Boolean) {
        viewModelScope.launch { repository.setForeground(active) }
    }
}
