package ir.coffevista.vista_native.core.security

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.core.common.AccessTokenProvider
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityBindingsModule {
    @Binds
    @Singleton
    abstract fun bindSessionStore(
        implementation: EncryptedSessionStore,
    ): SessionStore

    @Binds
    @Singleton
    abstract fun bindAccessTokenProvider(
        implementation: SessionAccessTokenProvider,
    ): AccessTokenProvider
}

class SessionAccessTokenProvider @Inject constructor(
    private val sessionStore: SessionStore,
) : AccessTokenProvider {
    override fun accessToken(): String? = sessionStore.read()?.accessToken
}
