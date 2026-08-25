package ir.coffevista.vista_native.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.coffevista.vista_native.BuildConfig
import ir.coffevista.vista_native.core.common.ApplicationScope
import ir.coffevista.vista_native.core.common.DefaultDispatcherProvider
import ir.coffevista.vista_native.core.common.DispatcherProvider
import ir.coffevista.vista_native.core.common.EpochClock
import ir.coffevista.vista_native.core.common.FoundationTelemetry
import ir.coffevista.vista_native.core.common.NoOpFoundationTelemetry
import ir.coffevista.vista_native.core.common.RedactingLogger
import ir.coffevista.vista_native.core.common.SecureLogger
import ir.coffevista.vista_native.core.database.VerifiedTlsPolicyDao
import ir.coffevista.vista_native.core.database.VerifiedTlsPolicyEntity
import ir.coffevista.vista_native.core.datastore.AppPreferenceStores
import ir.coffevista.vista_native.core.datastore.OnboardingStore
import ir.coffevista.vista_native.core.datastore.OnboardingStoreFactory
import ir.coffevista.vista_native.core.datastore.SettingsPreferenceStore
import ir.coffevista.vista_native.core.datastore.PrivacySettingsCache
import ir.coffevista.vista_native.core.logging.AndroidLogSink
import ir.coffevista.vista_native.core.network.AppEnvironment
import ir.coffevista.vista_native.core.network.EnvironmentName
import ir.coffevista.vista_native.core.network.InternalEnvironment
import ir.coffevista.vista_native.core.network.TlsMode
import ir.coffevista.vista_native.core.network.TlsPolicy
import ir.coffevista.vista_native.core.network.TlsPolicyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FoundationModule {
    @Provides
    @InternalEnvironment
    fun provideInternalEnvironment(): AppEnvironment = AppEnvironment(
        name = if (BuildConfig.APP_ENVIRONMENT == "production") {
            EnvironmentName.PRODUCTION
        } else {
            EnvironmentName.BETA
        },
        internalBaseUrl = BuildConfig.API_BASE_URL,
        debugBuild = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun provideEpochClock(): EpochClock =
        EpochClock { System.currentTimeMillis() / 1_000L }

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        dispatchers: DispatcherProvider,
    ): CoroutineScope = CoroutineScope(SupervisorJob()) + dispatchers.default

    @Provides
    @Singleton
    fun provideAppPreferenceStores(
        @ApplicationContext context: Context,
        @ApplicationScope applicationScope: CoroutineScope,
        dispatchers: DispatcherProvider,
    ): AppPreferenceStores = OnboardingStoreFactory.createAppPreferenceStores(
        context = context,
        applicationScope = applicationScope,
        dispatchers = dispatchers,
    )

    @Provides
    @Singleton
    fun provideOnboardingStore(stores: AppPreferenceStores): OnboardingStore =
        stores.onboardingStore

    @Provides
    @Singleton
    fun provideSettingsPreferenceStore(stores: AppPreferenceStores): SettingsPreferenceStore =
        stores.settingsPreferenceStore

    @Provides
    @Singleton
    fun providePrivacySettingsCache(stores: AppPreferenceStores): PrivacySettingsCache =
        stores.privacySettingsCache

    @Provides
    @Singleton
    fun provideTlsPolicyStore(
        dao: VerifiedTlsPolicyDao,
        dispatchers: DispatcherProvider,
    ): TlsPolicyStore {
        val initial = runCatching {
            runBlocking(dispatchers.io) { dao.read() }
        }.fold(
            onSuccess = { entity -> entity?.toPolicy() ?: TlsPolicyStore.DEFAULT_POLICY },
            onFailure = { TlsPolicyStore.FAIL_CLOSED_POLICY },
        )
        return TlsPolicyStore(initialPolicy = initial) { policy ->
            runCatching {
                runBlocking(dispatchers.io) {
                    dao.replaceAtomically(policy.toEntity())
                }
            }.isSuccess
        }
    }

    @Provides
    @Singleton
    fun provideSecureLogger(): SecureLogger =
        RedactingLogger(enabled = BuildConfig.DEBUG, sink = AndroidLogSink())

    @Provides
    @Singleton
    fun provideFoundationTelemetry(): FoundationTelemetry = NoOpFoundationTelemetry
}

private fun VerifiedTlsPolicyEntity.toPolicy(): TlsPolicy = TlsPolicy(
    contractVersion = contractVersion,
    revision = revision,
    mode = when (mode) {
        TlsMode.OFF.name -> TlsMode.OFF
        TlsMode.MONITOR.name -> TlsMode.MONITOR
        TlsMode.ENFORCE.name -> TlsMode.ENFORCE
        else -> TlsMode.ENFORCE
    },
    currentPinSha256 = currentPinSha256,
    nextPinSha256 = nextPinSha256,
    expiresAtEpochSeconds = expiresAtEpochSeconds,
)

private fun TlsPolicy.toEntity(): VerifiedTlsPolicyEntity = VerifiedTlsPolicyEntity(
    contractVersion = contractVersion,
    revision = revision,
    mode = mode.name,
    currentPinSha256 = currentPinSha256,
    nextPinSha256 = nextPinSha256,
    expiresAtEpochSeconds = expiresAtEpochSeconds,
)
