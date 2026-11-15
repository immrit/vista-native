package ir.coffevista.vista_native.core.network

import ir.coffevista.vista_native.core.common.AccessTokenProvider
import ir.coffevista.vista_native.core.common.DeviceIdProvider
import ir.coffevista.vista_native.core.common.SessionCredentialsProvider
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.UUID
import javax.inject.Inject

class InternalAuthorizationInterceptor @Inject constructor(
    @InternalEnvironment environment: AppEnvironment,
    private val accessTokenProvider: AccessTokenProvider,
    private val deviceIdProvider: DeviceIdProvider,
) : Interceptor {
    constructor(
        environment: AppEnvironment,
        accessTokenProvider: AccessTokenProvider,
    ) : this(
        environment = environment,
        accessTokenProvider = accessTokenProvider,
        deviceIdProvider = DeviceIdProvider { DeviceIdProvider.current?.deviceId() },
    )

    private val allowedHost = environment.internalBaseUrl.toHttpUrl().host

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != allowedHost) {
            return chain.proceed(request)
        }

        val builder = request.newBuilder()

        if (request.header(HEADER_DEVICE_ID) == null) {
            val deviceId = deviceIdProvider?.deviceId()
                ?: DeviceIdProvider.current?.deviceId()
                ?: fallbackDeviceId
            if (!deviceId.isNullOrBlank()) {
                builder.header(HEADER_DEVICE_ID, deviceId)
            }
        }

        if (request.header(HEADER_SESSION_ID) == null) {
            val sessionId = SessionCredentialsProvider.current?.currentSessionId()
                ?.takeIf(String::isNotBlank)
            if (sessionId != null) {
                builder.header(HEADER_SESSION_ID, sessionId)
            }
        }

        if (request.header(AUTHORIZATION) == null) {
            val token = runCatching(accessTokenProvider::accessToken).getOrNull()
                ?.takeIf(String::isNotBlank)
            if (token != null) {
                builder.header(AUTHORIZATION, "Bearer $token")
            }
        }

        return chain.proceed(builder.build())
    }

    companion object {
        const val AUTHORIZATION = "Authorization"
        const val HEADER_DEVICE_ID = "X-Device-ID"
        const val HEADER_SESSION_ID = "X-Session-ID"

        @Volatile
        private var cachedFallbackDeviceId: String? = null

        private val fallbackDeviceId: String
            get() = cachedFallbackDeviceId ?: synchronized(this) {
                cachedFallbackDeviceId ?: UUID.randomUUID().toString().also { cachedFallbackDeviceId = it }
            }
    }
}
