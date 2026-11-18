package ir.coffevista.vista_native.core.network

import android.os.Build
import ir.coffevista.vista_native.core.common.AccessTokenProvider
import ir.coffevista.vista_native.core.common.DeviceIdProvider
import ir.coffevista.vista_native.core.common.DeviceMetadataProvider
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

        val meta = DeviceMetadataProvider.current?.metadata()
        val deviceName = sanitizeHeaderValue(
            meta?.deviceName ?: runCatching {
                DeviceMetadataProvider.formatAndroidDeviceName(Build.MANUFACTURER, Build.MODEL, Build.BRAND)
            }.getOrNull().orEmpty(),
        )
        val deviceModel = sanitizeHeaderValue(
            meta?.deviceModel ?: runCatching { Build.MODEL?.trim() }.getOrNull().orEmpty(),
        )
        val osVersion = sanitizeHeaderValue(
            meta?.osVersion ?: runCatching {
                Build.VERSION.RELEASE?.trim()?.takeIf(String::isNotBlank)?.let { "Android $it" }
            }.getOrNull().orEmpty(),
        )
        val appVersion = sanitizeHeaderValue(meta?.appVersion.orEmpty())
        val userAgent = sanitizeHeaderValue(
            meta?.userAgent ?: if (deviceName.isNotBlank()) {
                "VistaAndroid (${osVersion.ifBlank { "Android" }}; $deviceName)"
            } else {
                ""
            },
        )

        if (request.header(HEADER_VISTA_CLIENT) == null) {
            builder.header(HEADER_VISTA_CLIENT, "vista-android")
        }
        if (request.header(HEADER_PLATFORM) == null) {
            builder.header(HEADER_PLATFORM, "android")
        }
        if (deviceName.isNotBlank() && request.header(HEADER_DEVICE_NAME) == null) {
            builder.header(HEADER_DEVICE_NAME, deviceName)
        }
        if (deviceModel.isNotBlank() && request.header(HEADER_DEVICE_MODEL) == null) {
            builder.header(HEADER_DEVICE_MODEL, deviceModel)
        }
        if (osVersion.isNotBlank() && request.header(HEADER_OS_VERSION) == null) {
            builder.header(HEADER_OS_VERSION, osVersion)
        }
        if (appVersion.isNotBlank() && request.header(HEADER_APP_VERSION) == null) {
            builder.header(HEADER_APP_VERSION, appVersion)
        }
        if (userAgent.isNotBlank() && request.header(HEADER_USER_AGENT) == null) {
            builder.header(HEADER_USER_AGENT, userAgent)
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

    private fun sanitizeHeaderValue(value: String): String =
        buildString(value.length) {
            for (ch in value) {
                if (ch in '\u0020'..'\u007E') {
                    append(ch)
                }
            }
        }.trim()

    companion object {
        const val AUTHORIZATION = "Authorization"
        const val HEADER_DEVICE_ID = "X-Device-ID"
        const val HEADER_SESSION_ID = "X-Session-ID"
        const val HEADER_VISTA_CLIENT = "X-Vista-Client"
        const val HEADER_PLATFORM = "X-Platform"
        const val HEADER_DEVICE_NAME = "X-Device-Name"
        const val HEADER_DEVICE_MODEL = "X-Device-Model"
        const val HEADER_OS_VERSION = "X-OS-Version"
        const val HEADER_APP_VERSION = "X-App-Version"
        const val HEADER_USER_AGENT = "User-Agent"

        @Volatile
        private var cachedFallbackDeviceId: String? = null

        private val fallbackDeviceId: String
            get() = cachedFallbackDeviceId ?: synchronized(this) {
                cachedFallbackDeviceId ?: UUID.randomUUID().toString().also { cachedFallbackDeviceId = it }
            }
    }
}
