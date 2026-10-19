package ir.coffevista.vista_native.core.network

import dagger.Lazy
import ir.coffevista.vista_native.core.common.AccessTokenProvider
import ir.coffevista.vista_native.core.common.TokenRefresher
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InternalAuthenticator @Inject constructor(
    @InternalEnvironment private val environment: AppEnvironment,
    private val accessTokenProvider: AccessTokenProvider,
    private val tokenRefresher: Lazy<TokenRefresher>,
) : Authenticator {
    private val host = environment.internalBaseUrl.toHttpUrl().host
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.host != host) return null
        val path = response.request.url.encodedPath
        if (path.contains("/v1/auth/") || path.contains("/auth/")) return null
        if (responseCount(response) >= 3) return null

        val currentAuth = response.request.header("Authorization")
        val currentBearer = currentAuth?.removePrefix("Bearer ")?.trim()

        synchronized(lock) {
            val latestToken = accessTokenProvider.accessToken()
            // If another thread already refreshed the token, use the latest token immediately
            if (!latestToken.isNullOrBlank() && latestToken != currentBearer) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }

            // Otherwise, trigger refresh
            val refreshedToken = runCatching { tokenRefresher.get().refreshToken() }.getOrNull()
            if (!refreshedToken.isNullOrBlank() && refreshedToken != currentBearer) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $refreshedToken")
                    .build()
            }
        }
        return null
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
