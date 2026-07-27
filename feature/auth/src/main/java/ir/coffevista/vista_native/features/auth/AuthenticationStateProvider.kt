package ir.coffevista.vista_native.features.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthenticationStateProvider {
    val state: StateFlow<AuthenticationState>
}
