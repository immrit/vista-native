package ir.coffevista.vista_native.features.profile.settings.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successNonce: Long = 0L,
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val api: ChangePasswordApi,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = mutableUiState.asStateFlow()

    fun submit(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "لطفاً تمامی فیلدها را کامل کنید") }
            return
        }
        if (newPassword.length < 6) {
            mutableUiState.update { it.copy(errorMessage = "رمز عبور باید حداقل ۶ کاراکتر باشد") }
            return
        }
        mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val success = runCatching {
                api.changePassword(ChangePasswordRequestDto(currentPassword, newPassword)).isSuccessful
            }.getOrDefault(false)
            mutableUiState.update {
                if (success) it.copy(isSubmitting = false, successNonce = it.successNonce + 1)
                else it.copy(isSubmitting = false, errorMessage = "تغییر رمز عبور ناموفق بود. دوباره تلاش کنید")
            }
        }
    }
}
