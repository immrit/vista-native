package ir.coffevista.vista_native.notifications

import android.content.Context
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.network.AppEnvironment
import ir.coffevista.vista_native.core.network.InternalApi
import ir.coffevista.vista_native.core.network.InternalEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Call
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class FcmTokenRegistrationDto(
    val token: String,
    val platform: String = "android",
    @SerialName("device_type") val deviceType: String = "mobile",
    @SerialName("device_model") val deviceModel: String = runCatching { Build.MODEL }.getOrNull() ?: "Android",
    @SerialName("os_version") val osVersion: String = runCatching { Build.VERSION.RELEASE }.getOrNull() ?: "14",
)

@Singleton
class PushTokenRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    @InternalApi private val client: Call.Factory,
    @InternalEnvironment private val environment: AppEnvironment,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = context.getSharedPreferences("fcm_token_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_SYNCED_TOKEN = "last_synced_fcm_token"
        private const val KEY_LAST_SYNCED_USER_ID = "last_synced_fcm_user_id"
        private const val KEY_LAST_RESYNC_EPOCH = "last_fcm_resync_epoch"
    }

    fun onControlPlaneEpoch(epoch: Long) {
        if (epoch <= 0L) return
        val lastEpoch = prefs.getLong(KEY_LAST_RESYNC_EPOCH, 0L)
        if (epoch > lastEpoch) {
            prefs.edit()
                .remove(KEY_LAST_SYNCED_TOKEN)
                .putLong(KEY_LAST_RESYNC_EPOCH, epoch)
                .apply()
            syncTokenAsync()
        }
    }

    fun syncTokenAsync(userId: String? = null) {
        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                if (!token.isNullOrBlank()) {
                    registerToken(token = token, userId = userId)
                }
            } catch (_: Exception) {
                // Non-fatal if FCM token lookup fails (e.g. offline)
            }
        }
    }

    fun unregisterTokenAsync() {
        scope.launch {
            val token = prefs.getString(KEY_LAST_SYNCED_TOKEN, null)
                ?: runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
            prefs.edit()
                .remove(KEY_LAST_SYNCED_TOKEN)
                .remove(KEY_LAST_SYNCED_USER_ID)
                .apply()
            if (!token.isNullOrBlank()) {
                try {
                    val payload = Json.encodeToString(mapOf("token" to token))
                    val request = Request.Builder()
                        .url("${environment.internalBaseUrl.trimEnd('/')}/v1/fcm/token")
                        .delete(payload.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute().close()
                } catch (_: Exception) {
                    // Best-effort unregister on logout
                }
            }
        }
    }

    suspend fun registerToken(token: String, userId: String? = null): Boolean = withContext(Dispatchers.IO) {
        val lastToken = prefs.getString(KEY_LAST_SYNCED_TOKEN, null)
        val lastUserId = prefs.getString(KEY_LAST_SYNCED_USER_ID, null)
        if (token == lastToken && (userId == null || userId == lastUserId)) {
            return@withContext true
        }

        try {
            val payload = Json.encodeToString(
                FcmTokenRegistrationDto(token = token),
            )
            val request = Request.Builder()
                .url("${environment.internalBaseUrl.trimEnd('/')}/v1/fcm/token")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    prefs.edit()
                        .putString(KEY_LAST_SYNCED_TOKEN, token)
                        .apply {
                            if (!userId.isNullOrBlank()) {
                                putString(KEY_LAST_SYNCED_USER_ID, userId)
                            }
                        }
                        .apply()
                    true
                } else {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }
}
