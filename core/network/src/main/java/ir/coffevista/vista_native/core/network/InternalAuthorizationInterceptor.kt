package ir.coffevista.vista_native.core.network

import ir.coffevista.vista_native.core.common.AccessTokenProvider
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Inject

class InternalAuthorizationInterceptor @Inject constructor(
    @InternalEnvironment environment: AppEnvironment,
    private val accessTokenProvider: AccessTokenProvider,
) : Interceptor {
    private val allowedHost = environment.internalBaseUrl.toHttpUrl().host

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != allowedHost || request.header(AUTHORIZATION) != null) {
            return chain.proceed(request)
        }
        val token = runCatching(accessTokenProvider::accessToken).getOrNull()
            ?.takeIf(String::isNotBlank)
            ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .header(AUTHORIZATION, "Bearer $token")
                .build(),
        )
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
