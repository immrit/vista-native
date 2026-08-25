package ir.coffevista.vista_native.features.profile.settings.datastorage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageCacheUiState(
    val sizeBytes: Long? = null,
    val isClearing: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class StorageCacheViewModel @Inject constructor(
    private val cacheManager: AppCacheManager,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(StorageCacheUiState())
    val uiState: StateFlow<StorageCacheUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        runCatching { cacheManager.sizeBytes() }
            .onSuccess { size -> mutableUiState.update { it.copy(sizeBytes = size) } }
            .onFailure { mutableUiState.update { it.copy(message = "محاسبهٔ حجم کش ناموفق بود") } }
    }

    fun clear() = viewModelScope.launch {
        mutableUiState.update { it.copy(isClearing = true, message = null) }
        runCatching { cacheManager.clear() }
            .onSuccess {
                mutableUiState.update {
                    it.copy(sizeBytes = 0L, isClearing = false, message = "حافظهٔ موقت با موفقیت پاکسازی شد")
                }
            }
            .onFailure {
                mutableUiState.update {
                    it.copy(isClearing = false, message = "پاکسازی حافظهٔ موقت ناموفق بود")
                }
            }
    }
}
