package ir.coffevista.vista_native.core.di

import android.content.Context
import ir.coffevista.vista_native.BuildConfig
import ir.coffevista.vista_native.core.auth.AuthRepository
import ir.coffevista.vista_native.core.auth.AuthenticationStateOwner
import ir.coffevista.vista_native.core.auth.DefaultAuthRepository
import ir.coffevista.vista_native.core.auth.OkHttpAuthRemoteDataSource
import ir.coffevista.vista_native.core.storage.EncryptedSessionStore
import ir.coffevista.vista_native.core.storage.OnboardingStore
import ir.coffevista.vista_native.core.storage.SessionStore
import ir.coffevista.vista_native.core.storage.SharedPreferencesOnboardingStore
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
