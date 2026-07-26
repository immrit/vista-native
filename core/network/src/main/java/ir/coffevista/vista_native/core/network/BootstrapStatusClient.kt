package ir.coffevista.vista_native.core.network

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class BootstrapStatus(
    val maintenance: Boolean,
    val tlsUpdate: TlsPolicyUpdate?,
)

class BootstrapStatusClient @Inject constructor(
    @BootstrapApi private val client: OkHttpClient,
    @InternalEnvironment environment: AppEnvironment,
    private val tlsPolicyStore: TlsPolicyStore,
) {
    private val statusUrl = environment.internalBaseUrl.trimEnd('/') + "/api/v1/system/status"
    private val json = Json { ignoreUnknownKeys = true }

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
            val tls = data["tls_pinning"] as? JsonObject
            val update = tls?.let(::observeBackendPolicy)
            return BootstrapStatus(maintenance, update)
        }
    }

    private fun observeBackendPolicy(raw: JsonObject): TlsPolicyUpdate {
        val mode = when (raw["mode"]?.jsonPrimitive?.content?.lowercase()) {
            "monitor" -> TlsMode.MONITOR
            "enforce" -> TlsMode.ENFORCE
            else -> TlsMode.OFF
        }
        val fingerprints = raw["fingerprints"]
            ?.let { element -> element as? kotlinx.serialization.json.JsonArray }
            ?.mapNotNull { element -> element.jsonPrimitive.content.normalizedPin() }
            .orEmpty()
        val candidate = TlsPolicy(
            contractVersion = raw["contract_version"]?.jsonPrimitive?.longOrNull?.toInt() ?: 0,
            revision = raw["revision"]?.jsonPrimitive?.longOrNull ?: 0,
            mode = mode,
            currentPinSha256 = fingerprints.getOrNull(0),
            nextPinSha256 = fingerprints.getOrNull(1),
            expiresAtEpochSeconds = null,
        )
        // Backend فعلی signature ندارد. این مشاهده عمداً policy را فعال نمی‌کند.
        return tlsPolicyStore.update(candidate, signatureVerified = false)
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
}
