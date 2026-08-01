package ir.coffevista.vista_native.features.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.features.auth.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthenticationStateProvider
import ir.coffevista.vista_native.features.profile.data.OwnProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwnProfileViewModel @Inject constructor(
    private val repository: OwnProfileRepository,
    private val authStateProvider: AuthenticationStateProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<OwnProfileUiState>(OwnProfileUiState.Loading)
    val uiState: StateFlow<OwnProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            authStateProvider.state.collectLatest { authState ->
                if (authState is AuthenticationState.SignedIn) {
                    val userId = authState.context.userId
                    if (userId != currentUserId) {
                        currentUserId = userId
                        observeProfile(userId)
                    }
                } else if (authState is AuthenticationState.SignedOut) {
                    currentUserId = null
                    repository.clearProfileData()
                }
            }
        }
    }

    private fun observeProfile(userId: String) {
        viewModelScope.launch {
            // Initiate network fetch without awaiting, flow collection handles the rest
            launch { fetchProfile(userId) }
            
            repository.getOwnProfileFlow(userId).collectLatest { entity ->
                if (entity != null) {
                    _uiState.update { currentState ->
                        val isRefreshing = (currentState as? OwnProfileUiState.Content)?.isRefreshing ?: false
                        OwnProfileUiState.Content(profile = entity, isRefreshing = isRefreshing)
                    }
                }
            }
        }
    }

    private suspend fun fetchProfile(userId: String, isRefresh: Boolean = false) {
        if (isRefresh) {
            _uiState.update { state ->
                if (state is OwnProfileUiState.Content) state.copy(isRefreshing = true) else state
            }
        }
        
        when (val outcome = repository.fetchAndCacheOwnProfile(userId)) {
            is Outcome.Failure -> {
                // Only show error screen if we don't have cached content
                if (_uiState.value !is OwnProfileUiState.Content) {
                    _uiState.value = OwnProfileUiState.Error(outcome.error)
                } else if (isRefresh) {
                    _uiState.update { state ->
                        (state as? OwnProfileUiState.Content)?.copy(isRefreshing = false) ?: state
                    }
                }
            }
            is Outcome.Success -> {
                if (isRefresh) {
                    _uiState.update { state ->
                        (state as? OwnProfileUiState.Content)?.copy(isRefreshing = false) ?: state
                    }
                }
            }
        }
    }

    fun handleAction(action: OwnProfileAction) {
        val userId = currentUserId ?: return
        when (action) {
            OwnProfileAction.Refresh -> {
                viewModelScope.launch {
                    fetchProfile(userId, isRefresh = true)
                }
            }
            OwnProfileAction.Retry -> {
                _uiState.value = OwnProfileUiState.Loading
                viewModelScope.launch {
                    fetchProfile(userId)
                }
            }
        }
    }
}
