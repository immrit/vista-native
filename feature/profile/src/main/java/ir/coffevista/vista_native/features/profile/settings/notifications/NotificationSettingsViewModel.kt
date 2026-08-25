package ir.coffevista.vista_native.features.profile.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val settings: NotificationSettingsDto = NotificationSettingsDto(),
    val isLoading: Boolean = true,
    val hasLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val api: NotificationSettingsApi,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching { api.getSettings() }
            .onSuccess { response ->
                if (response.isSuccessful && response.body() != null) {
                    mutableUiState.update {
                        it.copy(settings = requireNotNull(response.body()), isLoading = false, hasLoaded = true)
                    }
                } else {
                    mutableUiState.update {
                        it.copy(isLoading = false, errorMessage = "دریافت تنظیمات اعلان‌ها ناموفق بود")
                    }
                }
            }
            .onFailure {
                mutableUiState.update {
                    it.copy(isLoading = false, errorMessage = "اتصال برای دریافت تنظیمات اعلان‌ها برقرار نشد")
                }
            }
    }

    fun update(transform: (NotificationSettingsDto) -> NotificationSettingsDto) {
        val previous = mutableUiState.value.settings
        val updated = transform(previous)
        mutableUiState.update { it.copy(settings = updated, isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { api.updateSettings(updated) }
                .onSuccess { response ->
                    if (response.isSuccessful) {
                        mutableUiState.update { it.copy(isSaving = false) }
                    } else {
                        mutableUiState.update {
                            it.copy(settings = previous, isSaving = false, errorMessage = "ذخیرهٔ تنظیمات اعلان‌ها ناموفق بود")
                        }
                    }
                }
                .onFailure {
                    mutableUiState.update {
                        it.copy(settings = previous, isSaving = false, errorMessage = "اتصال برای ذخیرهٔ تنظیمات اعلان‌ها برقرار نشد")
                    }
                }
        }
    }
}
