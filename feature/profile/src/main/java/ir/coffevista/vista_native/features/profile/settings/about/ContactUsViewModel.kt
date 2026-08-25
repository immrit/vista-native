package ir.coffevista.vista_native.features.profile.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactUsUiState(
    val isSubmitting: Boolean = false,
    val isOpeningSupportChat: Boolean = false,
    val supportConversationId: String? = null,
    val supportConversationTitle: String = "پشتیبانی ویستا",
    val errorMessage: String? = null,
    val successNonce: Long = 0L,
)

@HiltViewModel
class ContactUsViewModel @Inject constructor(
    private val api: ContactRequestsApi,
    private val supportConversationApi: SupportConversationApi,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ContactUsUiState())
    val uiState: StateFlow<ContactUsUiState> = mutableUiState.asStateFlow()

    fun submit(fullName: String, email: String, subject: String, message: String) {
        if (fullName.isBlank() || email.isBlank() || subject.isBlank() || message.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "لطفاً همهٔ فیلدهای ضروری را کامل کنید") }
            return
        }
        mutableUiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val success = runCatching {
                api.submit(ContactRequestDto(fullName.trim(), email.trim(), subject.trim(), message.trim())).isSuccessful
            }.getOrDefault(false)
            mutableUiState.update {
                if (success) it.copy(isSubmitting = false, successNonce = it.successNonce + 1)
                else it.copy(isSubmitting = false, errorMessage = "ارسال پیام با مشکل مواجه شد. لطفاً دوباره تلاش کنید")
            }
        }
    }

    fun openSupportChat() {
        if (mutableUiState.value.isOpeningSupportChat) return
        mutableUiState.update { it.copy(isOpeningSupportChat = true, errorMessage = null) }
        viewModelScope.launch {
            val conversation = runCatching { supportConversationApi.open() }
                .getOrNull()
                ?.takeIf { it.isSuccessful }
                ?.body()
                ?.takeIf { it.id.isNotBlank() }
            mutableUiState.update {
                if (conversation == null) {
                    it.copy(
                        isOpeningSupportChat = false,
                        errorMessage = "باز کردن گفت‌وگوی پشتیبانی ناموفق بود. دوباره تلاش کنید",
                    )
                } else {
                    it.copy(
                        isOpeningSupportChat = false,
                        supportConversationId = conversation.id,
                        supportConversationTitle = conversation.name.ifBlank { "پشتیبانی ویستا" },
                    )
                }
            }
        }
    }
}
