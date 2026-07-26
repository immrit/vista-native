package ir.coffevista.vista_native.features.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StartupViewModel(
    private val resolver: StartupResolver,
) : ViewModel() {
    private val mutableState = MutableStateFlow(StartupUiState())
    val state: StateFlow<StartupUiState> = mutableState.asStateFlow()

    init {
        resolve()
    }

    fun retry() {
        resolve()
    }

    private fun resolve() {
        mutableState.value = StartupUiState(StartupDestination.Loading)
        viewModelScope.launch {
            mutableState.value = StartupUiState(resolver.resolve())
        }
    }

    class Factory(
        private val resolver: StartupResolver,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(StartupViewModel::class.java))
            return StartupViewModel(resolver) as T
        }
    }
}
