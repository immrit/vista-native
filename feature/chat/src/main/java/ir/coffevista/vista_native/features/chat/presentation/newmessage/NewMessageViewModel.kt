package ir.coffevista.vista_native.features.chat.presentation.newmessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewMessageUiState(
    val suggestedUsers: List<ChatUser> = emptyList(),
    val searchResults: List<ChatUser>? = null,
    val query: String = "",
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isSecretMode: Boolean = false,
    val isGroupMode: Boolean = false,
    val groupName: String = "",
    val selectedUserIds: Set<String> = emptySet(),
    val isCreatingGroup: Boolean = false,
    val openingUserId: String? = null,
    val error: String? = null,
) {
    val visibleUsers: List<ChatUser> get() = searchResults ?: suggestedUsers
}

@HiltViewModel
class NewMessageViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NewMessageUiState())
    val state: StateFlow<NewMessageUiState> = mutableState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeSuggestedUsers().collectLatest { users ->
                mutableState.update { current ->
                    current.copy(suggestedUsers = users, isLoading = false)
                }
            }
        }
    }

    fun setSecretMode(enabled: Boolean) {
        mutableState.update { it.copy(isSecretMode = enabled, isGroupMode = false) }
    }

    fun setGroupMode(enabled: Boolean) {
        mutableState.update { it.copy(isGroupMode = enabled, isSecretMode = false, error = null) }
    }

    fun groupNameChanged(value: String) {
        mutableState.update { it.copy(groupName = value.take(MAX_GROUP_NAME_LENGTH), error = null) }
    }

    fun toggleGroupUser(user: ChatUser) {
        if (!mutableState.value.isGroupMode || mutableState.value.isCreatingGroup) return
        mutableState.update { state ->
            val selected = state.selectedUserIds
            when {
                user.id in selected -> state.copy(selectedUserIds = selected - user.id, error = null)
                selected.size >= MAX_GROUP_INVITED_MEMBERS -> state.copy(error = "حداکثر ۲۰ عضو مجاز است")
                else -> state.copy(selectedUserIds = selected + user.id, error = null)
            }
        }
    }

    fun createGroup(onOpened: (conversationId: String, title: String) -> Unit) {
        val current = mutableState.value
        if (current.isCreatingGroup) return
        val name = current.groupName.trim()
        if (name.isEmpty()) {
            mutableState.update { it.copy(error = "نام گروه الزامی است") }
            return
        }
        if (current.selectedUserIds.isEmpty()) {
            mutableState.update { it.copy(error = "حداقل یک عضو انتخاب کنید") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isCreatingGroup = true, error = null) }
            when (val result = repository.createGroup(name, current.selectedUserIds.toList())) {
                is ChatResult.Success -> {
                    mutableState.update { it.copy(isCreatingGroup = false) }
                    onOpened(result.value.id, result.value.title.ifBlank { name })
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(isCreatingGroup = false, error = result.message)
                }
            }
        }
    }

    fun queryChanged(value: String) {
        mutableState.update { it.copy(query = value, error = null) }
        searchJob?.cancel()
        val query = value.trim()
        if (query.isEmpty()) {
            mutableState.update { it.copy(searchResults = null, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            mutableState.update { it.copy(isSearching = true) }
            when (val result = repository.searchUsers(query)) {
                is ChatResult.Success -> {
                    val existingById = mutableState.value.suggestedUsers.associateBy(ChatUser::id)
                    val merged = result.value.map { remote ->
                        existingById[remote.id]?.let { local ->
                            remote.copy(conversationId = local.conversationId)
                        } ?: remote
                    }
                    mutableState.update { it.copy(searchResults = merged, isSearching = false) }
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(searchResults = emptyList(), isSearching = false, error = result.message)
                }
            }
        }
    }

    fun openUser(user: ChatUser, onOpened: (conversationId: String, title: String) -> Unit) {
        if (mutableState.value.openingUserId != null) return
        val secret = mutableState.value.isSecretMode
        val existing = user.conversationId?.takeIf(String::isNotBlank)
        if (!secret && existing != null) {
            onOpened(existing, user.displayName)
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(openingUserId = user.id, error = null) }
            when (val result = repository.createConversation(user.id, secret)) {
                is ChatResult.Success -> {
                    mutableState.update { it.copy(openingUserId = null) }
                    onOpened(result.value.id, result.value.title.ifBlank { user.displayName })
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(openingUserId = null, error = result.message)
                }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 500L
        const val MAX_GROUP_INVITED_MEMBERS = 19
        const val MAX_GROUP_NAME_LENGTH = 100
    }
}
