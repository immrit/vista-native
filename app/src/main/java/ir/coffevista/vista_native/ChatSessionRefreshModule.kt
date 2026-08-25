package ir.coffevista.vista_native

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.features.auth.AuthenticationStateOwner
import ir.coffevista.vista_native.features.auth.RefreshResolution
import ir.coffevista.vista_native.features.auth.SessionRefreshCoordinator
import ir.coffevista.vista_native.features.chat.domain.repository.ChatSessionRefresher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatSessionRefreshModule {
    @Provides
    @Singleton
    fun provideChatSessionRefresher(
        sessionStore: SessionStore,
        coordinator: SessionRefreshCoordinator,
        authenticationStateOwner: AuthenticationStateOwner,
    ): ChatSessionRefresher = ChatSessionRefresher {
        val refreshToken = sessionStore.read()?.refreshToken
            ?.takeIf(String::isNotBlank)
            ?: return@ChatSessionRefresher false
        when (coordinator.refresh(refreshToken)) {
            is RefreshResolution.Refreshed -> true
            RefreshResolution.TerminalSession -> {
                authenticationStateOwner.signOut()
                false
            }
            RefreshResolution.PersistenceFailure,
            RefreshResolution.TransientFailure,
            -> false
        }
    }
}
