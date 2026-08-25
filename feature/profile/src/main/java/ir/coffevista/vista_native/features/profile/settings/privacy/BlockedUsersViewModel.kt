package ir.coffevista.vista_native.features.profile.settings.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedUsersUiState(
    val users: List<BlockedUserUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val feedbackMessage: String? = null,
)

@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val api: BlockedUsersApi,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = mutableUiState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { api.getBlockedUsers() }
            .onSuccess { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    mutableUiState.update {
                        it.copy(
                            users = body.profiles.mapNotNull { profile ->
                                val id = profile.id ?: profile.userId
                                id?.takeIf(String::isNotBlank)?.let {
                                    BlockedUserUiModel(it, profile.username, profile.fullName, profile.avatarUrl)
                                }
                            },
                            isLoading = false,
                        )
                    }
                } else {
                    mutableUiState.update { it.copy(isLoading = false, errorMessage = "دریافت کاربران مسدودشده ناموفق بود") }
                }
            }
            .onFailure {
                mutableUiState.update { it.copy(isLoading = false, errorMessage = "اتصال برای دریافت کاربران مسدودشده برقرار نشد") }
            }
    }

    fun unblock(user: BlockedUserUiModel) = viewModelScope.launch {
        mutableUiState.update { it.copy(isMutating = true, feedbackMessage = null) }
        val success = runCatching { api.unblock(UnblockUserRequestDto(user.id)).isSuccessful }.getOrDefault(false)
        if (success) {
            mutableUiState.update { it.copy(users = it.users.filterNot { blocked -> blocked.id == user.id }, isMutating = false) }
        } else {
            mutableUiState.update { it.copy(isMutating = false, feedbackMessage = "رفع مسدودیت ناموفق بود. دوباره تلاش کنید") }
        }
    }
}
