package ir.coffevista.vista_native.features.profile.settings.privacy

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.common.DeviceIdProvider
import ir.coffevista.vista_native.core.common.DeviceMetadataProvider
import ir.coffevista.vista_native.core.common.NotificationRuntimeCoordinator
import ir.coffevista.vista_native.core.common.SessionCredentialsProvider
import ir.coffevista.vista_native.core.network.SystemGateMonitor
import ir.coffevista.vista_native.core.security.SessionStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates automatic session registration, validation, and lightweight activity
 * heartbeats so the current device is always tracked cleanly without re-minting
 * sessions on opening Active Sessions or randomly kicking the user out on transient errors.
 */
@Singleton
class ActiveSessionLifecycleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ActiveSessionsApi,
    private val sessionStore: SessionStore,
) {
    private val mutex = Mutex()

    suspend fun ensureCurrentSessionRegistered(expectedUserId: String? = null): String? = mutex.withLock {
        val activeUserId = expectedUserId?.takeIf(String::isNotBlank)
            ?: sessionStore.read()?.userId?.takeIf(String::isNotBlank)
            ?: return@withLock null

        val prefs = context.getSharedPreferences(SessionCredentialsProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val savedUserId = SessionCredentialsProvider.current?.currentUserId()
            ?: prefs.getString(SessionCredentialsProvider.KEY_SESSION_USER_ID, null)?.takeIf(String::isNotBlank)

        var sessionId = SessionCredentialsProvider.current?.currentSessionId()
            ?: prefs.getString(SessionCredentialsProvider.KEY_SESSION_ID, null)?.takeIf(String::isNotBlank)
        var sessionToken = SessionCredentialsProvider.current?.currentSessionToken()
            ?: SessionCredentialsProvider.current?.currentSessionToken()?.takeIf(String::isNotBlank)

        if (savedUserId != null && savedUserId != activeUserId) {
            sessionId = null
            sessionToken = null
            persistCredentials(prefs, null, null, null)
        }

        val fcmPrefs = context.getSharedPreferences("fcm_token_prefs", Context.MODE_PRIVATE)
        val cachedFcmToken = fcmPrefs.getString("last_synced_fcm_token", null)
        val fcmSyncEpoch = fcmPrefs.getLong("last_fcm_resync_epoch", 0L)

        if (sessionId != null && sessionToken != null) {
            val validateRes = runCatching {
                api.validate(ValidateSessionRequestDto(sessionId = sessionId, sessionToken = sessionToken))
            }.getOrNull()

            if (validateRes != null && validateRes.isSuccessful) {
                val body = validateRes.body()
                if (body?.valid == true) {
                    persistCredentials(prefs, sessionId, sessionToken, activeUserId)
                    touchInternal(sessionId, sessionToken, cachedFcmToken, fcmSyncEpoch)
                    return@withLock sessionId
                }
                when (body?.reason) {
                    "inactive", "rejected", "expired" -> {
                        val latestSid = SessionCredentialsProvider.current?.currentSessionId()
                        if (!latestSid.isNullOrBlank() && latestSid != sessionId) {
                            return@withLock latestSid
                        }
                        // Session was explicitly terminated or rejected; force immediate logout.
                        persistCredentials(prefs, null, null, null)
                        SystemGateMonitor.reportSessionTerminated(true)
                        return@withLock null
                    }
                    "not_found" -> {
                        // Legacy upgrade or unrecorded session: proceed below to register once cleanly.
                    }
                    else -> return@withLock sessionId
                }
            } else {
                // Network/5xx or 401 handled by authenticator; preserve existing session safely.
                return@withLock sessionId
            }
        }

        val newSessionId = UUID.randomUUID().toString()
        val newSessionToken = UUID.randomUUID().toString()
        val deviceId = DeviceIdProvider.current?.deviceId().orEmpty()
        val manufacturer = runCatching { Build.MANUFACTURER }.getOrNull().orEmpty().trim()
        val model = runCatching { Build.MODEL }.getOrNull()?.trim().orEmpty().ifBlank { "Android" }
        val brand = runCatching { Build.BRAND }.getOrNull().orEmpty().trim()
        val deviceName = DeviceMetadataProvider.formatAndroidDeviceName(manufacturer, model, brand)
        val appVersion = resolveAppVersion()
        val appVersionCode = resolveAppVersionCode()

        val registered = runCatching {
            api.register(
                RegisterSessionRequestDto(
                    sessionId = newSessionId,
                    sessionToken = newSessionToken,
                    deviceInfo = buildMap {
                        if (deviceId.isNotBlank()) put("device_id", deviceId)
                        put("device_name", deviceName)
                        put("model", model)
                        if (manufacturer.isNotBlank()) put("manufacturer", manufacturer)
                        put("os_version", runCatching { Build.VERSION.RELEASE }.getOrNull() ?: "14")
                        put("platform", "android")
                    },
                    appVersion = appVersion,
                    appVersionCode = appVersionCode.takeIf { it > 0 },
                    platform = "android",
                    fcmToken = cachedFcmToken,
                ),
            )
        }.getOrNull()

        if (registered != null && registered.isSuccessful) {
            persistCredentials(prefs, newSessionId, newSessionToken, activeUserId)
            return@withLock newSessionId
        }
        return@withLock sessionId
    }

    suspend fun touchCurrentSession() {
        val prefs = context.getSharedPreferences(SessionCredentialsProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val sessionId = SessionCredentialsProvider.current?.currentSessionId()
            ?: prefs.getString(SessionCredentialsProvider.KEY_SESSION_ID, null)?.takeIf(String::isNotBlank)
            ?: return
        val sessionToken = SessionCredentialsProvider.current?.currentSessionToken()
            ?: SessionCredentialsProvider.current?.currentSessionToken()?.takeIf(String::isNotBlank)
            ?: return
        val fcmPrefs = context.getSharedPreferences("fcm_token_prefs", Context.MODE_PRIVATE)
        val cachedFcmToken = fcmPrefs.getString("last_synced_fcm_token", null)
        val fcmSyncEpoch = fcmPrefs.getLong("last_fcm_resync_epoch", 0L)
        touchInternal(sessionId, sessionToken, cachedFcmToken, fcmSyncEpoch)
    }

    fun clearLocalSession() {
        val prefs = context.getSharedPreferences(SessionCredentialsProvider.PREFS_NAME, Context.MODE_PRIVATE)
        persistCredentials(prefs, null, null, null)
    }

    private suspend fun touchInternal(
        sessionId: String,
        sessionToken: String,
        cachedFcmToken: String?,
        fcmSyncEpoch: Long,
    ) {
        val touchResponse = runCatching {
            api.touch(
                TouchSessionRequestDto(
                    sessionId = sessionId,
                    sessionToken = sessionToken,
                    fcmToken = cachedFcmToken,
                    fcmSyncEpoch = fcmSyncEpoch,
                    appVersion = resolveAppVersion(),
                    appVersionCode = resolveAppVersionCode().takeIf { it > 0 },
                ),
            )
        }.getOrNull()
        val body = touchResponse?.body()
        if (touchResponse != null && touchResponse.isSuccessful && body != null) {
            if (!body.valid) {
                val validateRes = runCatching {
                    api.validate(ValidateSessionRequestDto(sessionId = sessionId, sessionToken = sessionToken))
                }.getOrNull()
                val reason = validateRes?.body()?.reason
                if (validateRes != null && validateRes.isSuccessful && reason in setOf("inactive", "rejected", "expired")) {
                    val latestSid = SessionCredentialsProvider.current?.currentSessionId()
                    if (latestSid.isNullOrBlank() || latestSid == sessionId) {
                        val prefs = context.getSharedPreferences(SessionCredentialsProvider.PREFS_NAME, Context.MODE_PRIVATE)
                        persistCredentials(prefs, null, null, null)
                        SystemGateMonitor.reportSessionTerminated(true)
                    }
                }
                return
            }
            if (body.requireFcmSync) {
                NotificationRuntimeCoordinator.requestFcmTokenResync()
            }
        }
    }

    private fun persistCredentials(
        prefs: android.content.SharedPreferences,
        sessionId: String?,
        sessionToken: String?,
        userId: String?,
    ) {
        val provider = SessionCredentialsProvider.current
        if (sessionId.isNullOrBlank() || sessionToken.isNullOrBlank()) {
            provider?.clear()
            prefs.edit()
                .remove(SessionCredentialsProvider.KEY_SESSION_ID)
                .remove(SessionCredentialsProvider.KEY_SESSION_TOKEN)
                .remove(SessionCredentialsProvider.KEY_SESSION_USER_ID)
                .apply()
        } else {
            provider?.saveSessionCredentials(sessionId, sessionToken, userId)
        }
    }

    private fun resolveAppVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()?.takeIf(String::isNotBlank) ?: "1.0.0"

    private fun resolveAppVersionCode(): Int = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info).toInt()
    }.getOrDefault(0)
}
