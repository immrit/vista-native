package ir.coffevista.vista_native.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class SystemAlert(
    val id: String,
    val severity: String,
    val scope: String,
    val feature: String? = null,
    val message: String,
)

object SystemGateMonitor {
    private val _maintenanceActive = MutableStateFlow(false)
    val maintenanceActive: StateFlow<Boolean> = _maintenanceActive.asStateFlow()

    private val _bannedReasonFa = MutableStateFlow<String?>(null)
    val bannedReasonFa: StateFlow<String?> = _bannedReasonFa.asStateFlow()

    private val _isBanned = MutableStateFlow(false)
    val isBanned: StateFlow<Boolean> = _isBanned.asStateFlow()

    private val _sessionTerminated = MutableStateFlow(false)
    val sessionTerminated: StateFlow<Boolean> = _sessionTerminated.asStateFlow()

    fun reportMaintenance(active: Boolean) {
        _maintenanceActive.value = active
    }

    fun reportSessionTerminated(terminated: Boolean = true) {
        _sessionTerminated.value = terminated
    }

    fun reportBanned(reasonFa: String? = null) {
        _bannedReasonFa.value = reasonFa ?: "دستگاه یا حساب کاربری شما به دلیل نقض قوانین مسدود شده است."
        _isBanned.value = true
    }

    fun clearBanned() {
        _bannedReasonFa.value = null
        _isBanned.value = false
    }
}

data class BootstrapStatus(
    val maintenance: Boolean,
    val tlsUpdate: TlsPolicyUpdate?,
    val featureFlags: Map<String, Boolean> = emptyMap(),
    val killSwitches: Set<String> = emptySet(),
    val alerts: List<SystemAlert> = emptyList(),
    val fcmResyncEpoch: Long = 0L,
) {
    fun isFeatureEnabled(featureKey: String): Boolean {
        val normalized = featureKey.trim().lowercase()
        if (normalized in killSwitches) return false
        return featureFlags[normalized]
            ?: featureFlags["${normalized}_enabled"]
            ?: true
    }
}

@Singleton
class BootstrapStatusClient @Inject constructor(
    @BootstrapApi private val client: OkHttpClient,
    @InternalEnvironment environment: AppEnvironment,
    private val tlsPolicyStore: TlsPolicyStore,
) {
    private val statusUrl = environment.internalBaseUrl.trimEnd('/') + "/api/v1/system/status"
    private val json = Json { ignoreUnknownKeys = true }
    private val _statusState = MutableStateFlow<BootstrapStatus?>(null)
    val statusState: StateFlow<BootstrapStatus?> = _statusState.asStateFlow()

    suspend fun fetch(): BootstrapStatus {
        val request = Request.Builder().url(statusUrl).get().build()
        val response = client.newCall(request).await()
        response.use {
            if (!it.isSuccessful) {
                throw RemoteFailure(it.code, "bootstrap_status_failed", null)
            }
            val root = json.parseToJsonElement(it.body?.string().orEmpty()).jsonObject
            val data = root["data"] as? JsonObject ?: root
            val maintenance = data["maintenance"]?.jsonPrimitive?.booleanOrNull
                ?: data["maintenance_mode"]?.jsonPrimitive?.booleanOrNull
                ?: false
            val controlPlane = data["control_plane"] as? JsonObject
            val isVerifiedControlPlane =
                (controlPlane?.get("source") as? JsonPrimitive)?.contentOrNull == "vista-backend" ||
                    data["schema_version"] != null

            val featureFlags = parseFeatureFlags(data)
            val killSwitches = parseKillSwitches(data, featureFlags)
            val alerts = parseAlerts(data)
            val fcmResyncEpoch = (data["fcm_resync_epoch"] as? JsonPrimitive)?.longOrNull ?: 0L

            val tls = data["tls_pinning"] as? JsonObject
            val update = tls?.let { raw -> observeBackendPolicy(raw, isVerifiedControlPlane) }
            val status = BootstrapStatus(
                maintenance = maintenance,
                tlsUpdate = update,
                featureFlags = featureFlags,
                killSwitches = killSwitches,
                alerts = alerts,
                fcmResyncEpoch = fcmResyncEpoch,
            )
            _statusState.value = status
            SystemGateMonitor.reportMaintenance(maintenance)
            return status
        }
    }

    private fun observeBackendPolicy(
        raw: JsonObject,
        controlPlaneVerified: Boolean,
    ): TlsPolicyUpdate {
        val rawMode = (raw["mode"] as? JsonPrimitive)?.contentOrNull?.lowercase()
        val mode = when (rawMode) {
            "monitor" -> TlsMode.MONITOR
            "enforce" -> TlsMode.ENFORCE
            else -> TlsMode.OFF
        }
        val fingerprints = (raw["fingerprints"] as? JsonArray)
            ?.mapNotNull { element -> (element as? JsonPrimitive)?.contentOrNull.normalizedPin() }
            .orEmpty()
        val expiresAtEpochSeconds = parseIsoEpochSeconds(
            (raw["expires_at"] as? JsonPrimitive)?.contentOrNull,
        ) ?: if (mode != TlsMode.OFF) {
            (System.currentTimeMillis() / 1_000L) + DEFAULT_PIN_TTL_SECONDS
        } else {
            null
        }
        val updatedAtEpochSeconds = parseIsoEpochSeconds(
            (raw["updated_at"] as? JsonPrimitive)?.contentOrNull,
        )
        val current = tlsPolicyStore.current()
        val explicitRevision = (raw["revision"] as? JsonPrimitive)?.longOrNull
            ?.takeIf { it > 0L }
        val payloadChanged = mode != current.mode ||
            fingerprints.getOrNull(0) != current.currentPinSha256 ||
            fingerprints.getOrNull(1) != current.nextPinSha256 ||
            expiresAtEpochSeconds != current.expiresAtEpochSeconds
        val resolvedRevision = when {
            explicitRevision != null -> explicitRevision
            updatedAtEpochSeconds != null && updatedAtEpochSeconds > current.revision -> updatedAtEpochSeconds
            payloadChanged -> current.revision + 1L
            current.revision > 0L -> current.revision
            else -> 1L
        }
        val contractVersion = (raw["contract_version"] as? JsonPrimitive)?.longOrNull?.toInt()
            ?.takeIf { it > 0 }
            ?: TlsPolicyStore.SUPPORTED_CONTRACT_VERSION

        val candidate = TlsPolicy(
            contractVersion = contractVersion,
            revision = resolvedRevision,
            mode = mode,
            currentPinSha256 = fingerprints.getOrNull(0),
            nextPinSha256 = fingerprints.getOrNull(1),
            expiresAtEpochSeconds = expiresAtEpochSeconds,
        )
        val signatureVerified = controlPlaneVerified || rawMode in setOf("off", "monitor", "enforce")
        return tlsPolicyStore.update(candidate, signatureVerified = signatureVerified)
    }

    private fun parseFeatureFlags(data: JsonObject): Map<String, Boolean> {
        val result = linkedMapOf<String, Boolean>()
        val sources = listOfNotNull(
            data["feature_flags"] as? JsonObject,
            data["features"] as? JsonObject,
        )
        for (source in sources) {
            for ((key, value) in source) {
                val boolVal = (value as? JsonPrimitive)?.booleanOrNull ?: continue
                result[key.trim().lowercase()] = boolVal
            }
        }
        return result
    }

    private fun parseKillSwitches(
        data: JsonObject,
        featureFlags: Map<String, Boolean>,
    ): Set<String> {
        val disabled = linkedSetOf<String>()
        (data["disabled"] as? JsonArray)?.forEach { elem ->
            val key = (elem as? JsonPrimitive)?.contentOrNull?.trim()?.lowercase()
            if (!key.isNullOrEmpty()) disabled += key
        }
        (data["kill_switches"] as? JsonObject)?.forEach { (key, value) ->
            if ((value as? JsonPrimitive)?.booleanOrNull == true) {
                disabled += key.trim().lowercase()
            }
        }
        featureFlags.forEach { (key, enabled) ->
            if (!enabled) disabled += key
        }
        return disabled
    }

    private fun parseAlerts(data: JsonObject): List<SystemAlert> {
        val arr = data["alerts"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull { elem ->
            val obj = elem as? JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val message = (obj["message"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            SystemAlert(
                id = id,
                severity = (obj["severity"] as? JsonPrimitive)?.contentOrNull ?: "info",
                scope = (obj["scope"] as? JsonPrimitive)?.contentOrNull ?: "global",
                feature = (obj["feature"] as? JsonPrimitive)?.contentOrNull,
                message = message,
            )
        }
    }

    private fun parseIsoEpochSeconds(raw: String?): Long? {
        val trimmed = raw?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return runCatching { Instant.parse(trimmed).epochSecond }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(trimmed).toEpochSecond() }.getOrNull()
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, exception: IOException) {
                if (continuation.isActive) continuation.resumeWithException(exception)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        })
    }

    private companion object {
        const val DEFAULT_PIN_TTL_SECONDS = 30L * 86_400L
    }
}
