package ir.coffevista.vista_native.core.di

import android.content.Context
import ir.coffevista.vista_native.BuildConfig
import ir.coffevista.vista_native.core.datastore.OnboardingStore
import ir.coffevista.vista_native.core.datastore.SharedPreferencesOnboardingStore
import ir.coffevista.vista_native.core.security.EncryptedSessionStore
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.features.auth.AuthenticationStateOwner
import ir.coffevista.vista_native.features.auth.data.DefaultAuthRepository
import ir.coffevista.vista_native.features.auth.data.OkHttpAuthRemoteDataSource
import ir.coffevista.vista_native.features.auth.domain.AuthRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val onboardingStore: OnboardingStore =
        SharedPreferencesOnboardingStore(context.applicationContext)

    val sessionStore: SessionStore =
        EncryptedSessionStore(context.applicationContext)

    val authRepository: AuthRepository = DefaultAuthRepository(
        OkHttpAuthRemoteDataSource(httpClient, BuildConfig.API_BASE_URL),
    )

    val authenticationStateOwner = AuthenticationStateOwner()
}
