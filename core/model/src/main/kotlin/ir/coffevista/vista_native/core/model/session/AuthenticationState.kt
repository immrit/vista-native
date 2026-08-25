package ir.coffevista.vista_native.core.model.session

import kotlinx.coroutines.flow.StateFlow

sealed interface AuthenticationState {
    data object Unknown : AuthenticationState
    data object SignedOut : AuthenticationState
    data class SignedIn(val context: AuthenticatedContext) : AuthenticationState
}

interface AuthenticationStateProvider {
    val state: StateFlow<AuthenticationState>
}
