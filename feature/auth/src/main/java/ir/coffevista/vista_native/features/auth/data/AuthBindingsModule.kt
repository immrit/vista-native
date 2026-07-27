package ir.coffevista.vista_native.features.auth.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.features.auth.domain.AuthRepository
import ir.coffevista.vista_native.features.auth.SessionRefreshCoordinator
import ir.coffevista.vista_native.features.auth.SingleFlightSessionRefreshCoordinator
import ir.coffevista.vista_native.features.auth.AuthenticationStateOwner
import ir.coffevista.vista_native.features.auth.AuthenticationStateProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingsModule {
    @Binds
    @Singleton
    abstract fun bindRemoteDataSource(
        implementation: OkHttpAuthRemoteDataSource,
    ): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        implementation: DefaultAuthRepository,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionRefreshCoordinator(
        implementation: SingleFlightSessionRefreshCoordinator,
    ): SessionRefreshCoordinator

    @Binds
    @Singleton
    abstract fun bindAuthenticationStateProvider(
        implementation: AuthenticationStateOwner,
    ): AuthenticationStateProvider
}
