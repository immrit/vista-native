package ir.coffevista.vista_native.features.chat.data.realtime

import android.util.Log
import ir.coffevista.vista_native.features.chat.data.remote.ConversationDto
import ir.coffevista.vista_native.features.chat.data.remote.MessageDto
import ir.coffevista.vista_native.features.chat.data.remote.ReactionUpdateDto
import ir.coffevista.vista_native.features.chat.data.mapper.epochMillisOrNull
import ir.coffevista.vista_native.features.chat.domain.model.ReadReceipt
import ir.coffevista.vista_native.features.chat.domain.model.RealtimeConnectionState
import ir.coffevista.vista_native.features.chat.domain.model.TypingState
import ir.coffevista.vista_native.features.chat.domain.repository.ChatAccount
import ir.coffevista.vista_native.features.chat.domain.repository.ChatSessionProvider
import kotlin.math.min
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import ir.coffevista.vista_native.core.network.PolicyAwareInternalCallFactory

sealed interface RealtimeEvent {
    val eventId: String?
    val conversationId: String?

    data class NewMessage(
        override val eventId: String?,
        override val conversationId: String?,
        val message: MessageDto,
    ) : RealtimeEvent

    data class MessageUpdated(
        override val eventId: String?,
        override val conversationId: String?,
        val message: MessageDto,
    ) : RealtimeEvent

    data class MessageDeleted(
        override val eventId: String?,
        override val conversationId: String?,
        val messageId: String,
    ) : RealtimeEvent

    data class ConversationUpdated(
        override val eventId: String?,
        override val conversationId: String?,
        val conversation: ConversationDto,
    ) : RealtimeEvent

    data class ConversationCleared(
        override val eventId: String?,
        override val conversationId: String?,
    ) : RealtimeEvent

    data class Typing(
        override val eventId: String?,
        override val conversationId: String?,
        val state: TypingState,
    ) : RealtimeEvent

    data class Read(
        override val eventId: String?,
        override val conversationId: String?,
        val receipt: ReadReceipt,
    ) : RealtimeEvent

    data class ReactionUpdated(
        override val eventId: String?,
        override val conversationId: String?,
        val update: ReactionUpdateDto,
    ) : RealtimeEvent

    data class Connected(override val eventId: String? = null) : RealtimeEvent {
        override val conversationId: String? = null
    }

    data class Unknown(
        val type: String,
        override val eventId: String?,
        override val conversationId: String?,
    ) : RealtimeEvent
}

@Serializable
private data class EnvelopeDto(
    val type: String,
    val event_id: String? = null,
    val conversation_id: String? = null,
    val occurred_at: String? = null,
    val data: JsonElement? = null,
)

object RealtimeEventParser {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun parse(payload: String): RealtimeEvent? = runCatching {
        val envelope = json.decodeFromString<EnvelopeDto>(payload)
        val data = envelope.data?.jsonObject ?: JsonObject(emptyMap())
        val conversationId = envelope.conversation_id ?: data.string("conversation_id")
        when (envelope.type) {
            "connected" -> RealtimeEvent.Connected(envelope.event_id)
            "new_message" -> RealtimeEvent.NewMessage(
                envelope.event_id,
                conversationId,
                json.decodeFromJsonElement(data),
            )
            "message_updated" -> RealtimeEvent.MessageUpdated(
                envelope.event_id,
                conversationId,
                json.decodeFromJsonElement(data),
            )
            "message_deleted" -> data.string("message_id")?.let {
                RealtimeEvent.MessageDeleted(envelope.event_id, conversationId, it)
            } ?: RealtimeEvent.Unknown(envelope.type, envelope.event_id, conversationId)
            "conversation_updated" -> RealtimeEvent.ConversationUpdated(
                envelope.event_id,
                conversationId,
                json.decodeFromJsonElement(data),
            )
            "conversation_cleared" -> RealtimeEvent.ConversationCleared(envelope.event_id, conversationId)
            "typing" -> {
                val userId = data.string("user_id") ?: return null
                RealtimeEvent.Typing(
                    envelope.event_id,
                    conversationId,
                    TypingState(
                        conversationId = conversationId.orEmpty(),
                        userId = userId,
                        isTyping = data.string("is_typing")?.toBooleanStrictOrNull() ?: true,
                        occurredAtEpochMillis = envelope.occurred_at.toEpochMillisOrNow(),
                    ),
                )
            }
            "read_receipt" -> {
                val userId = data.string("user_id") ?: return null
                RealtimeEvent.Read(
                    envelope.event_id,
                    conversationId,
                    ReadReceipt(
                        conversationId = conversationId.orEmpty(),
                        userId = userId,
                        readAtEpochMillis = data.string("read_at").toEpochMillisOrNow(),
                    ),
                )
            }
            "reaction_updated" -> RealtimeEvent.ReactionUpdated(
                envelope.event_id,
                conversationId,
                json.decodeFromJsonElement(data),
            )
            else -> RealtimeEvent.Unknown(envelope.type, envelope.event_id, conversationId)
        }
    }.getOrNull()

    private fun JsonObject.string(key: String): String? = get(key)?.toString()?.trim('"')
    private fun String?.toEpochMillisOrNow(): Long =
        epochMillisOrNull() ?: System.currentTimeMillis()
}

class RealtimeEventDeduplicator(private val capacity: Int = 1024) {
    private val order = ArrayDeque<String>()
    private val seen = HashSet<String>()

    @Synchronized
    fun accept(eventId: String?): Boolean {
        if (eventId.isNullOrBlank()) return true
        if (!seen.add(eventId)) return false
        order.addLast(eventId)
        while (order.size > capacity) seen.remove(order.removeFirst())
        return true
    }
}

class ReconnectPolicy(
    private val maxDelayMillis: Long = 30_000,
    private val jitterRatio: Double = 0.2,
    private val random: () -> Double = { Random.nextDouble() },
) {
    fun delayMillis(attempt: Int): Long {
        val exponent = 1L shl min(attempt.coerceAtLeast(0), 5)
        val base = min(maxDelayMillis, exponent * 1_000L)
        val multiplier = 1.0 + ((random() * 2.0 - 1.0) * jitterRatio)
        return (base * multiplier).toLong().coerceIn(1_000L, maxDelayMillis)
    }
}

interface ChatSocket {
    val incoming: Flow<String>
    suspend fun close()
}

interface ChatWebSocketFactory {
    suspend fun connect(url: String, headers: Map<String, String>): ChatSocket
}

private class ChatSocketOpenException(
    val statusCode: Int?,
    cause: Throwable,
) : IOException("Chat WebSocket handshake failed", cause)

/** The caller must provide the same TLS-policy-aware client used for internal APIs. */
class OkHttpChatWebSocketFactory(
    private val callFactory: PolicyAwareInternalCallFactory,
) : ChatWebSocketFactory {
    override suspend fun connect(url: String, headers: Map<String, String>): ChatSocket {
        // Strip Sec-WebSocket-Protocol from raw websocket headers if present because
        // sending full JWTs as protocol tokens violates token syntax and triggers 502 on Nginx.
        val cleanHeaders = headers.filterKeys { !it.equals("Sec-WebSocket-Protocol", ignoreCase = true) }

        // 1. Try primary websocket endpoint: /v1/chat/ws
        val firstError = try {
            return connectWebSocket(url, cleanHeaders)
        } catch (t: Throwable) {
            t
        }

        // 2. Try alternative websocket endpoint: /ws
        val altWsUrl = url.replace("/v1/chat/ws", "/ws")
        if (altWsUrl != url) {
            try {
                return connectWebSocket(altWsUrl, cleanHeaders)
            } catch (_: Throwable) {
                // Ignore and proceed to SSE
            }
        }

        // 3. Fall back to SSE stream: /v1/chat/stream
        Log.w(
            "VistaChatRealtime",
            "WebSocket connection failed (${firstError.message}), falling back to SSE stream...",
        )
        return connectSse(url, headers)
    }

    private suspend fun connectWebSocket(url: String, headers: Map<String, String>): ChatSocket {
        val opened = CompletableDeferred<Unit>()
        val channel = Channel<String>(capacity = Channel.BUFFERED)
        lateinit var socket: WebSocket
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
        }.build()
        socket = callFactory.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("VistaChatRealtime", "WebSocket opened successfully proto=${response.protocol} code=${response.code} url=$url")
                opened.complete(Unit)
            }
            override fun onMessage(webSocket: WebSocket, text: String) { channel.trySend(text) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("VistaChatRealtime", "WebSocket closed code=$code reason=$reason")
                channel.close()
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val bodySnippet = runCatching { response?.peekBody(1024)?.string() }.getOrNull()
                Log.w(
                    "VistaChatRealtime",
                    "WebSocket failure url=$url type=${t.javaClass.simpleName} msg=${t.message} cause=${t.cause?.javaClass?.simpleName} http=${response?.code} proto=${response?.protocol} body=$bodySnippet",
                )
                if (!opened.isCompleted) {
                    opened.completeExceptionally(ChatSocketOpenException(response?.code, t))
                }
                channel.close(t)
            }
        })
        opened.await()
        return object : ChatSocket {
            override val incoming: Flow<String> = channel.receiveAsFlow()
            override suspend fun close() {
                socket.close(1000, null)
                channel.close()
            }
        }
    }

    private suspend fun connectSse(url: String, headers: Map<String, String>): ChatSocket {
        val token = headers["Authorization"]?.removePrefix("Bearer ")?.trim().orEmpty()
        var sseUrl = url
            .replace("/v1/chat/ws", "/v1/chat/stream")
            .replace("/ws", "/v1/chat/stream")
            .replaceFirst("wss://", "https://")
            .replaceFirst("ws://", "http://")
        if (token.isNotEmpty() && !sseUrl.contains("token=")) {
            sseUrl += (if (sseUrl.contains("?")) "&" else "?") + "token=$token"
        }
        val opened = CompletableDeferred<Unit>()
        val channel = Channel<String>(capacity = Channel.BUFFERED)
        val request = Request.Builder()
            .url(sseUrl)
            .apply {
                headers.forEach { (name, value) ->
                    // Exclude Sec-WebSocket-* headers for SSE
                    if (!name.startsWith("Sec-WebSocket", ignoreCase = true)) {
                        header(name, value)
                    }
                }
                header("Accept", "text/event-stream")
                header("Cache-Control", "no-cache")
            }
            .build()

        val call = callFactory.newStreamCall(request)
        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    val code = response.code
                    val body = runCatching { response.peekBody(512).string() }.getOrNull()
                    Log.w("VistaChatRealtime", "SSE stream failed http=$code body=$body")
                    if (!opened.isCompleted) {
                        opened.completeExceptionally(ChatSocketOpenException(code, IOException("SSE HTTP $code: $body")))
                    }
                    response.close()
                    channel.close(IOException("SSE HTTP $code"))
                    return@launch
                }
                Log.i("VistaChatRealtime", "SSE stream connected successfully! code=${response.code}")
                opened.complete(Unit)
                response.body?.source()?.use { source ->
                    while (!source.exhausted() && isActive) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()
                        if (trimmed.startsWith("data:")) {
                            val data = trimmed.removePrefix("data:").trim()
                            if (data.isNotBlank()) {
                                channel.trySend(data)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                if (!opened.isCompleted) {
                    opened.completeExceptionally(ChatSocketOpenException(null, t))
                }
                channel.close(t)
            } finally {
                call.cancel()
                channel.close()
            }
        }
        opened.await()
        return object : ChatSocket {
            override val incoming: Flow<String> = channel.receiveAsFlow()
            override suspend fun close() {
                job.cancel()
                call.cancel()
                channel.close()
            }
        }
    }
}

class ChatRealtimeCoordinator(
    private val scope: CoroutineScope,
    private val sessionProvider: ChatSessionProvider,
    private val socketFactory: ChatWebSocketFactory,
    private val internalBaseUrl: String,
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy(),
) {
    private val foreground = MutableStateFlow(false)
    private val mutableState = MutableStateFlow(RealtimeConnectionState.DISCONNECTED)
    private val mutableEvents = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    private var runner: Job? = null
    private var activeSocket: ChatSocket? = null
    private var syncHandler: (suspend () -> Unit)? = null
    private var pollingJob: Job? = null

    val state: StateFlow<RealtimeConnectionState> = mutableState.asStateFlow()
    val events = mutableEvents.asSharedFlow()

    fun setSyncHandler(handler: suspend () -> Unit) {
        syncHandler = handler
    }

    fun start() {
        if (runner?.isActive == true) return
        runner = scope.launch {
            combine(sessionProvider.account, foreground) { account, active -> account to active }
                .collectLatest { (account, active) ->
                    when {
                        account == null -> {
                            stopPolling()
                            mutableState.value = RealtimeConnectionState.DISCONNECTED
                        }
                        !active -> {
                            mutableState.value = RealtimeConnectionState.PAUSED
                        }
                        else -> {
                            connectionLoop(account)
                        }
                    }
                }
        }
    }

    suspend fun setForeground(active: Boolean) {
        foreground.emit(active)
        if (active && pollingJob != null) {
            scope.launch {
                runCatching { syncHandler?.invoke() }
            }
        }
    }

    suspend fun stop() {
        runner?.cancel()
        runner = null
        stopPolling()
        activeSocket?.close()
        activeSocket = null
        mutableState.value = RealtimeConnectionState.DISCONNECTED
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            // Immediate sync when entering polling fallback mode
            runCatching { syncHandler?.invoke() }
            while (isActive) {
                delay(8_000L) // Poll every 8 seconds while in foreground
                if (foreground.value && sessionProvider.account.value != null) {
                    runCatching { syncHandler?.invoke() }
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun connectionLoop(account: ChatAccount) {
        val deduplicator = RealtimeEventDeduplicator()
        var attempt = 0
        var isFallbackPolling = false

        while (true) {
            var refreshedSession = false
            try {
                val currentAccount = sessionProvider.account.value ?: return
                if (!isFallbackPolling) {
                    mutableState.value = if (attempt == 0) {
                        RealtimeConnectionState.CONNECTING
                    } else {
                        RealtimeConnectionState.RECONNECTING
                    }
                    mutableState.value = RealtimeConnectionState.AUTHENTICATING
                }
                val socket = socketFactory.connect(
                    url = internalBaseUrl.toChatWebSocketUrl(),
                    headers = currentAccount.realtimeHeaders(),
                )
                stopPolling()
                isFallbackPolling = false
                activeSocket = socket
                mutableState.value = RealtimeConnectionState.CONNECTED
                attempt = 0
                socket.incoming.collect { payload ->
                    val event = RealtimeEventParser.parse(payload) ?: return@collect
                    if (deduplicator.accept(event.eventId)) mutableEvents.emit(event)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                Log.w(
                    "VistaChatRealtime",
                    "Realtime stream failure (${failure.javaClass.simpleName}: ${failure.message}), checking fallback...",
                )
                if (failure is ChatSocketOpenException && failure.statusCode == 401) {
                    refreshedSession = sessionProvider.refresh()
                }
                if (sessionProvider.account.value == null) {
                    stopPolling()
                    isFallbackPolling = false
                    mutableState.value = RealtimeConnectionState.FAILED
                } else {
                    // Gateway error (502, 503, 504) or failed handshake:
                    // Server streaming is currently unavailable, switch to Fallback Polling.
                    // Keep mutableState as CONNECTED so the UI is NOT stuck in "در حال اتصال..."
                    isFallbackPolling = true
                    mutableState.value = RealtimeConnectionState.CONNECTED
                    startPolling()
                }
            } finally {
                activeSocket?.close()
                activeSocket = null
            }
            if (refreshedSession) {
                attempt = 0
                continue
            }
            val delayMs = if (isFallbackPolling) {
                // In fallback polling mode, check the websocket in the background every 45s
                45_000L
            } else {
                reconnectPolicy.delayMillis(attempt++)
            }
            delay(delayMs)
        }
    }

    private fun String.toChatWebSocketUrl(): String {
        val normalized = trimEnd('/')
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        return "$normalized/v1/chat/ws"
    }
}

internal fun ChatAccount.realtimeHeaders(): Map<String, String> = mapOf(
    "Authorization" to "Bearer $accessToken",
    // Some reverse proxies drop Authorization during an HTTP Upgrade. The
    // backend deliberately accepts the same token as the second negotiated
    // WebSocket subprotocol, without exposing it in the URL.
    "Sec-WebSocket-Protocol" to "Authorization, $accessToken",
    "X-Device-ID" to deviceId,
)
