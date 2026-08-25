package ir.coffevista.vista_native.features.auth

import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationStateOwner @Inject constructor() : AuthenticationStateProvider {
    private val mutableState = MutableStateFlow<AuthenticationState>(AuthenticationState.Unknown)
    override val state: StateFlow<AuthenticationState> = mutableState.asStateFlow()

    fun accept(context: AuthenticatedContext) {
        mutableState.value = AuthenticationState.SignedIn(context)
    }

    fun signOut() {
        mutableState.value = AuthenticationState.SignedOut
    }
}
