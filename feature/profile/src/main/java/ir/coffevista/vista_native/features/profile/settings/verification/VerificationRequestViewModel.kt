package ir.coffevista.vista_native.features.profile.settings.verification

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.profile.data.OwnProfileRepository
import ir.coffevista.vista_native.features.profile.data.ProfileAvatarUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerificationRequestUiState(
    val category: String = "",
    val notes: String = "",
    val selectedDocumentUri: Uri? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class VerificationRequestViewModel @Inject constructor(
    private val ownProfileRepository: OwnProfileRepository,
    private val uploader: ProfileAvatarUploader,
    private val authenticationStateProvider: AuthenticationStateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationRequestUiState())
    val uiState: StateFlow<VerificationRequestUiState> = _uiState.asStateFlow()

    fun onCategoryChanged(value: String) {
        _uiState.update { it.copy(category = value, errorMessage = null) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { it.copy(notes = value, errorMessage = null) }
    }

    fun onDocumentSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedDocumentUri = uri, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        val category = state.category.trim()
        if (category.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "لطفاً دسته‌بندی فعالیت را وارد کنید") }
            return
        }

        val docUri = state.selectedDocumentUri
        if (docUri == null) {
            _uiState.update { it.copy(errorMessage = "لطفاً تصویر مدرک هویتی را انتخاب کنید") }
            return
        }

        val userId = (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "نشست کاربری نامعتبر است. لطفاً دوباره وارد شوید") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, isUploading = true, errorMessage = null) }

            val uploadResult = runCatching {
                uploader.uploadDocument(
                    userId = userId,
                    uri = docUri,
                    onProgress = { progress ->
                        _uiState.update { it.copy(uploadProgress = progress) }
                    },
                )
            }

            val uploaded = uploadResult.getOrElse { throwable ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isUploading = false,
                        errorMessage = throwable.message ?: "خطا در بارگذاری مدرک هویتی",
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isUploading = false) }

            when (val outcome = ownProfileRepository.requestVerification(
                category = category,
                documentUrl = uploaded.url,
                notes = state.notes.trim().ifEmpty { null },
            )) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
                }
                is Outcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = outcome.error.messageFa,
                        )
                    }
                }
            }
        }
    }
}
