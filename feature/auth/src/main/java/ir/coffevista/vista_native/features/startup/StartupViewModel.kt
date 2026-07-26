package ir.coffevista.vista_native.features.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
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

}
