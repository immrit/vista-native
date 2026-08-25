package ir.coffevista.vista_native.features.services.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.services.data.ContactVistaUser
import ir.coffevista.vista_native.features.services.data.ServicesHubData
import ir.coffevista.vista_native.features.services.data.ServicesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ServicesHubUiState {
    data object Loading : ServicesHubUiState
    data class Success(val data: ServicesHubData) : ServicesHubUiState
    data class Error(val message: String) : ServicesHubUiState
}

sealed interface ContactsRailUiState {
    data object Loading : ContactsRailUiState
    data object PermissionRequired : ContactsRailUiState
    data object Empty : ContactsRailUiState
    data class Success(val users: List<ContactVistaUser>) : ContactsRailUiState
    data class Error(val message: String) : ContactsRailUiState
}

data class ServicesScreenState(
    val hubState: ServicesHubUiState = ServicesHubUiState.Loading,
    val contactsState: ContactsRailUiState = ContactsRailUiState.PermissionRequired,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val repository: ServicesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServicesScreenState())
    val uiState: StateFlow<ServicesScreenState> = _uiState.asStateFlow()

    init {
        loadHub()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadHub(isPullToRefresh = true)
    }

    fun loadHub(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isPullToRefresh) {
                _uiState.update { it.copy(hubState = ServicesHubUiState.Loading) }
            }
            runCatching {
                repository.getHub()
            }.onSuccess { data ->
                _uiState.update {
                    it.copy(
                        hubState = ServicesHubUiState.Success(data),
                        isRefreshing = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        hubState = ServicesHubUiState.Error(error.message ?: "خطا در بارگذاری اطلاعات"),
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    fun syncContacts(phoneNumbers: List<String>) {
        if (phoneNumbers.isEmpty()) {
            _uiState.update { it.copy(contactsState = ContactsRailUiState.Empty) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(contactsState = ContactsRailUiState.Loading) }
            runCatching {
                repository.findContacts(phoneNumbers)
            }.onSuccess { users ->
                _uiState.update {
                    it.copy(
                        contactsState = if (users.isEmpty()) {
                            ContactsRailUiState.Empty
                        } else {
                            ContactsRailUiState.Success(users)
                        },
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        contactsState = ContactsRailUiState.Error(error.message ?: "خطا در همگام‌سازی مخاطبین"),
                    )
                }
            }
        }
    }

    fun onContactsPermissionDenied() {
        _uiState.update { it.copy(contactsState = ContactsRailUiState.PermissionRequired) }
    }
}
