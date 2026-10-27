package ir.coffevista.vista_native.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import java.net.Proxy
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InternalNetworkModule {
    @Provides
    @Singleton
    @BootstrapApi
    fun provideBootstrapClient(): OkHttpClient = baseClientBuilder().build()

    @Provides
    @Singleton
    @ExternalMedia
    fun provideExternalMediaClient(): OkHttpClient = baseClientBuilder()
        .proxy(Proxy.NO_PROXY)
        .build()

    @Provides
    @Singleton
    @InternalApi
    fun provideInternalApiCallFactory(
        @InternalEnvironment environment: AppEnvironment,
        authorizationInterceptor: InternalAuthorizationInterceptor,
        policyStore: TlsPolicyStore,
        evaluator: TlsPolicyEvaluator,
        reporter: HttpTlsFailureReporter,
        authenticator: InternalAuthenticator,
    ): okhttp3.Call.Factory = PolicyAwareInternalCallFactory(
        environment = environment,
        policyStore = policyStore,
        evaluator = evaluator,
        reporter = reporter,
        authorizationInterceptor = authorizationInterceptor,
        authenticator = authenticator,
    )

    @Provides
    @Singleton
    @InternalApi
    fun provideInternalRetrofit(
        @InternalApi callFactory: okhttp3.Call.Factory,
        @InternalEnvironment environment: AppEnvironment,
    ): Retrofit {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
        return Retrofit.Builder()
            .baseUrl(environment.internalBaseUrl.ensureTrailingSlash())
            .callFactory(callFactory)
            .addConverterFactory(
                json.asConverterFactory("application/json; charset=utf-8".toMediaType()),
            )
            .build()
    }

    private fun baseClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(BoundedRetryInterceptor())

    private fun String.ensureTrailingSlash(): String =
        if (endsWith('/')) this else "$this/"
}
