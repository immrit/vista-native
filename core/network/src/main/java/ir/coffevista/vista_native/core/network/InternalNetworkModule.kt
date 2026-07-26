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
    fun provideExternalMediaClient(): OkHttpClient = baseClientBuilder().build()

    @Provides
    @Singleton
    @InternalApi
    fun provideInternalApiCallFactory(
        @InternalEnvironment environment: AppEnvironment,
        authorizationInterceptor: InternalAuthorizationInterceptor,
        policyStore: TlsPolicyStore,
        evaluator: TlsPolicyEvaluator,
        reporter: HttpTlsFailureReporter,
    ): okhttp3.Call.Factory = PolicyAwareInternalCallFactory(
        environment = environment,
        policyStore = policyStore,
        evaluator = evaluator,
        reporter = reporter,
        authorizationInterceptor = authorizationInterceptor,
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
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addInterceptor(BoundedRetryInterceptor())

    private fun String.ensureTrailingSlash(): String =
        if (endsWith('/')) this else "$this/"
}
