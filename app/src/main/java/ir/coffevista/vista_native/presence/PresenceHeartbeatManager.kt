package ir.coffevista.vista_native.presence

import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.network.InternalApi
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.features.auth.AuthenticationStateOwner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class PresenceUpdateRequest(
    val status: String,
    val section: String? = null,
)

interface PresenceHeartbeatApi {
    @POST("v1/presence/update")
    suspend fun updatePresence(@Body body: PresenceUpdateRequest): Response<ResponseBody>
}

@Singleton
class PresenceHeartbeatManager @Inject constructor(
    @InternalApi retrofit: Retrofit,
    private val authenticationStateOwner: AuthenticationStateOwner,
    private val sessionStore: SessionStore,
) {
    private val api: PresenceHeartbeatApi = retrofit.create(PresenceHeartbeatApi::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var lastOnlineSentAtMs: Long = 0L

    private fun hasActiveSignedInSession(): Boolean {
        val state = authenticationStateOwner.state.value
        val signedInOnline = state is AuthenticationState.SignedIn && !state.context.offline
        val tokenReady = !sessionStore.read()?.accessToken.isNullOrBlank()
        return signedInOnline && tokenReady
    }

    suspend fun sendHeartbeatIfEligible(force: Boolean = false, section: String? = null) {
        if (!hasActiveSignedInSession()) return
        val now = System.currentTimeMillis()
        if (!force && now - lastOnlineSentAtMs < MIN_HEARTBEAT_INTERVAL_MS) return
        lastOnlineSentAtMs = now
        runCatching {
            api.updatePresence(PresenceUpdateRequest(status = "online", section = section))
        }
    }

    fun sendOfflineAsync(isChangingConfigurations: Boolean = false) {
        if (isChangingConfigurations) return
        if (!hasActiveSignedInSession()) return
        lastOnlineSentAtMs = 0L
        scope.launch {
            runCatching {
                api.updatePresence(PresenceUpdateRequest(status = "offline"))
            }
        }
    }

    suspend fun sendOfflineImmediately() {
        if (!hasActiveSignedInSession()) return
        lastOnlineSentAtMs = 0L
        runCatching {
            api.updatePresence(PresenceUpdateRequest(status = "offline"))
        }
    }

    private companion object {
        const val MIN_HEARTBEAT_INTERVAL_MS = 45_000L
    }
}
