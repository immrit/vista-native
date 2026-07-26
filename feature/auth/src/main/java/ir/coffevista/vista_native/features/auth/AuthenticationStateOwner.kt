package ir.coffevista.vista_native.features.auth

import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthenticationState {
    data object Unknown : AuthenticationState
    data object SignedOut : AuthenticationState
    data class SignedIn(val context: AuthenticatedContext) : AuthenticationState
}

class AuthenticationStateOwner {
    private val mutableState = MutableStateFlow<AuthenticationState>(AuthenticationState.Unknown)
    val state: StateFlow<AuthenticationState> = mutableState.asStateFlow()

    fun accept(context: AuthenticatedContext) {
        mutableState.value = AuthenticationState.SignedIn(context)
    }

    fun signOut() {
        mutableState.value = AuthenticationState.SignedOut
    }
}
