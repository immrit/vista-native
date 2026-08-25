package ir.coffevista.vista_native.core.network

import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.security.KeyStore
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class PolicyAwareInternalCallFactory(
    @InternalEnvironment environment: AppEnvironment,
    private val policyStore: TlsPolicyStore,
    private val evaluator: TlsPolicyEvaluator,
    private val reporter: TlsFailureReporter,
    private val authorizationInterceptor: InternalAuthorizationInterceptor,
) : Call.Factory {
    private val host = environment.internalBaseUrl.toHttpUrl().host
    private val lock = Any()

    @Volatile
    private var holder = buildHolder(policyStore.current().revision, generation = 1)

    override fun newCall(request: Request): Call = currentClient().newCall(request)

    /** Keeps realtime traffic on the same rotating TLS-policy client as REST. */
    fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket =
        currentClient().newWebSocket(request, listener)

    internal fun currentGeneration(): Long = currentClient().let { holder.generation }

    internal fun currentPingIntervalMillis(): Long = currentClient().pingIntervalMillis.toLong()

    internal fun currentProxy(): Proxy? = currentClient().proxy

    private fun currentClient(): OkHttpClient {
        val revision = policyStore.current().revision
        val snapshot = holder
        if (snapshot.policyRevision == revision) return snapshot.client
        return synchronized(lock) {
            val current = holder
            if (current.policyRevision == revision) {
                current.client
            } else {
                val replacement = buildHolder(revision, current.generation + 1)
                holder = replacement
                current.client.dispatcher.cancelAll()
                current.client.connectionPool.evictAll()
                current.client.dispatcher.executorService.shutdown()
                replacement.client
            }
        }
    }

    private fun buildHolder(policyRevision: Long, generation: Long): Holder {
        val trustManager = platformTrustManager()
        val policyTrustManager = PolicyTrustManager(
            delegate = trustManager,
            host = host,
            policyStore = policyStore,
            evaluator = evaluator,
            reporter = reporter,
        )
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(policyTrustManager), null)
        val client = OkHttpClient.Builder()
            // Internal REST/realtime must not inherit a stale debugging proxy.
            // TLS policy is evaluated against the canonical Vista host directly.
            .proxy(Proxy.NO_PROXY)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor(BoundedRetryInterceptor())
            .addInterceptor(authorizationInterceptor)
            .sslSocketFactory(sslContext.socketFactory, policyTrustManager)
            .build()
        return Holder(policyRevision, generation, client)
    }

    private fun platformTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm(),
        )
        factory.init(null as KeyStore?)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().single()
    }

    private data class Holder(
        val policyRevision: Long,
        val generation: Long,
        val client: OkHttpClient,
    )
}
