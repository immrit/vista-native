package ir.coffevista.vista_native.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpTlsFailureReporter @Inject constructor(
    @BootstrapApi private val bootstrapClient: OkHttpClient,
    @InternalEnvironment environment: AppEnvironment,
) : TlsFailureReporter {
    private val reportUrl = environment.internalBaseUrl.trimEnd('/') +
        "/api/v1/system/tls-report"
    private val inFlight = AtomicBoolean(false)
    private val lastReportMillis = AtomicLong(0)

    override fun report(failure: TlsFailureReport) {
        val now = System.currentTimeMillis()
        if (now - lastReportMillis.get() < REPORT_INTERVAL_MILLIS) return
        if (!inFlight.compareAndSet(false, true)) return
        lastReportMillis.set(now)

        val payload = buildJsonObject {
            put("host", failure.host)
            put("seen_fingerprint", failure.seenFingerprint)
            put("reason", failure.reason.name.lowercase())
        }.toString()
        val request = Request.Builder()
            .url(reportUrl)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        bootstrapClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, exception: IOException) {
                inFlight.set(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                inFlight.set(false)
            }
        })
    }

    private companion object {
        const val REPORT_INTERVAL_MILLIS = 60_000L
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
