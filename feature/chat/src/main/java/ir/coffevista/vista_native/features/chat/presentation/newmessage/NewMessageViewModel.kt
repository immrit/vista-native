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
    val selectedUsers: List<ChatUser> = emptyList(),
    val isCreatingGroup: Boolean = false,
    val openingUserId: String? = null,
    val error: String? = null,
    val canRetry: Boolean = false,
) {
    val visibleUsers: List<ChatUser>
        get() = searchResults ?: query.trim().takeIf(String::isNotEmpty)?.let { normalized ->
            suggestedUsers.filter { user ->
                user.displayName.contains(normalized, ignoreCase = true) ||
                    user.username.contains(normalized.removePrefix("@"), ignoreCase = true)
            }
        } ?: suggestedUsers
}

@HiltViewModel
class NewMessageViewModel @Inject constructor(
    private val repository: ChatRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NewMessageUiState())
    val state: StateFlow<NewMessageUiState> = mutableState.asStateFlow()
    private var searchJob: Job? = null
    private var cachedSuggestedUsers: List<ChatUser> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeSuggestedUsers().collectLatest { users ->
                cachedSuggestedUsers = users
                mutableState.update { current ->
                    current.copy(suggestedUsers = mergeSuggestedUsers(current.suggestedUsers, users))
                }
            }
        }
        loadSuggestedUsers()
    }

    fun retry() {
        val query = mutableState.value.query
        if (query.isBlank()) loadSuggestedUsers() else queryChanged(query)
    }

    private fun loadSuggestedUsers() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, error = null, canRetry = false) }
            when (val result = repository.refreshSuggestedUsers()) {
                is ChatResult.Success -> mutableState.update {
                    it.copy(
                        suggestedUsers = mergeSuggestedUsers(result.value, cachedSuggestedUsers),
                        isLoading = false,
                        error = null,
                        canRetry = false,
                    )
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(isLoading = false, error = result.message, canRetry = true)
                }
            }
        }
    }

    fun setSecretMode(enabled: Boolean) {
        mutableState.update {
            it.copy(isSecretMode = enabled, isGroupMode = false, error = null, canRetry = false)
        }
    }

    fun setGroupMode(enabled: Boolean) {
        mutableState.update {
            if (enabled) {
                it.copy(isGroupMode = true, isSecretMode = false, error = null, canRetry = false)
            } else {
                it.copy(
                    isGroupMode = false,
                    groupName = "",
                    selectedUserIds = emptySet(),
                    selectedUsers = emptyList(),
                    error = null,
                    canRetry = false,
                )
            }
        }
    }

    fun groupNameChanged(value: String) {
        mutableState.update { it.copy(groupName = value.take(MAX_GROUP_NAME_LENGTH), error = null, canRetry = false) }
    }

    fun toggleGroupUser(user: ChatUser) {
        if (!mutableState.value.isGroupMode || mutableState.value.isCreatingGroup) return
        mutableState.update { state ->
            val selected = state.selectedUserIds
            when {
                user.id in selected -> state.copy(
                    selectedUserIds = selected - user.id,
                    selectedUsers = state.selectedUsers.filterNot { it.id == user.id },
                    error = null,
                    canRetry = false,
                )
                selected.size >= MAX_GROUP_INVITED_MEMBERS -> state.copy(error = "حداکثر ۲۰ عضو مجاز است", canRetry = false)
                else -> state.copy(
                    selectedUserIds = selected + user.id,
                    selectedUsers = state.selectedUsers + user,
                    error = null,
                    canRetry = false,
                )
            }
        }
    }

    fun createGroup(onOpened: (conversationId: String, title: String) -> Unit) {
        val current = mutableState.value
        if (current.isCreatingGroup) return
        val name = current.groupName.trim()
        if (name.isEmpty()) {
            mutableState.update { it.copy(error = "نام گروه الزامی است", canRetry = false) }
            return
        }
        if (current.selectedUserIds.isEmpty()) {
            mutableState.update { it.copy(error = "حداقل یک عضو انتخاب کنید", canRetry = false) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isCreatingGroup = true, error = null, canRetry = false) }
            when (val result = repository.createGroup(name, current.selectedUserIds.toList())) {
                is ChatResult.Success -> {
                    mutableState.update { it.copy(isCreatingGroup = false) }
                    onOpened(result.value.id, result.value.title.ifBlank { name })
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(isCreatingGroup = false, error = result.message, canRetry = false)
                }
            }
        }
    }

    fun queryChanged(value: String) {
        searchJob?.cancel()
        val query = value.trim()
        if (query.isEmpty()) {
            mutableState.update {
                it.copy(query = value, searchResults = null, isSearching = false, error = null, canRetry = false)
            }
            return
        }
        mutableState.update {
            it.copy(
                query = value,
                searchResults = null,
                isSearching = true,
                error = null,
                canRetry = false,
            )
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            when (val result = repository.searchUsers(query)) {
                is ChatResult.Success -> {
                    val existingById = mutableState.value.suggestedUsers.associateBy(ChatUser::id)
                    val merged = result.value.map { remote ->
                        existingById[remote.id]?.let { local ->
                            remote.copy(conversationId = local.conversationId)
                        } ?: remote
                    }
                    mutableState.update {
                        if (it.query.trim() == query) {
                            it.copy(searchResults = merged, isSearching = false, canRetry = false)
                        } else {
                            it
                        }
                    }
                }
                is ChatResult.Failure -> mutableState.update {
                    if (it.query.trim() == query) {
                        it.copy(searchResults = null, isSearching = false, error = result.message, canRetry = true)
                    } else {
                        it
                    }
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
            mutableState.update { it.copy(openingUserId = user.id, error = null, canRetry = false) }
            when (val result = repository.createConversation(user.id, secret)) {
                is ChatResult.Success -> {
                    mutableState.update { it.copy(openingUserId = null) }
                    onOpened(result.value.id, result.value.title.ifBlank { user.displayName })
                }
                is ChatResult.Failure -> mutableState.update {
                    it.copy(openingUserId = null, error = result.message, canRetry = false)
                }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 500L
        const val MAX_GROUP_INVITED_MEMBERS = 19
        const val MAX_GROUP_NAME_LENGTH = 50
    }
}

private fun mergeSuggestedUsers(primary: List<ChatUser>, cached: List<ChatUser>): List<ChatUser> {
    val cachedById = cached.associateBy(ChatUser::id)
    val merged = linkedMapOf<String, ChatUser>()
    primary.forEach { user ->
        val local = cachedById[user.id]
        merged[user.id] = user.copy(
            username = user.username.ifBlank { local?.username.orEmpty() },
            fullName = user.fullName?.takeIf(String::isNotBlank) ?: local?.fullName,
            avatarUrl = user.avatarUrl?.takeIf(String::isNotBlank) ?: local?.avatarUrl,
            conversationId = local?.conversationId?.takeIf(String::isNotBlank) ?: user.conversationId,
        )
    }
    cached.forEach { user -> merged.putIfAbsent(user.id, user) }
    return merged.values.toList()
}
