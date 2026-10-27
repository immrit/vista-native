package ir.coffevista.vista_native.features.auth

import ir.coffevista.vista_native.core.common.TokenRefresher
import ir.coffevista.vista_native.core.security.SessionStore
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTokenRefresher @Inject constructor(
    private val sessionStore: SessionStore,
    private val refreshCoordinator: SessionRefreshCoordinator,
    private val authenticationStateOwner: AuthenticationStateOwner,
) : TokenRefresher {
    constructor(
        sessionStore: SessionStore,
        refreshCoordinator: SessionRefreshCoordinator,
    ) : this(sessionStore, refreshCoordinator, AuthenticationStateOwner())

    override fun refreshToken(): String? {
        val current = sessionStore.read() ?: return null
        val token = current.refreshToken
        if (token.isBlank()) return null
        return when (val res = runBlocking { refreshCoordinator.refresh(token) }) {
            is RefreshResolution.Refreshed -> {
                authenticationStateOwner?.refreshSession(res.payload)
                res.payload.session.accessToken
            }
            RefreshResolution.TerminalSession -> {
                authenticationStateOwner?.signOut()
                null
            }
            else -> null
        }
    }
}
