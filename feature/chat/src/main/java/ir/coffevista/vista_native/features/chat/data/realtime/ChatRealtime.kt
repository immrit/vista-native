package ir.coffevista.vista_native.features.chat.data.realtime

import android.util.Log
import ir.coffevista.vista_native.features.chat.data.remote.ConversationDto
import ir.coffevista.vista_native.features.chat.data.remote.MessageDto
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
import kotlinx.coroutines.Job
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
        val opened = CompletableDeferred<Unit>()
        val channel = Channel<String>(capacity = Channel.BUFFERED)
        lateinit var socket: WebSocket
        val request = Request.Builder().url(url).apply {
            headers.forEach { (name, value) -> header(name, value) }
        }.build()
        socket = callFactory.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { opened.complete(Unit) }
            override fun onMessage(webSocket: WebSocket, text: String) { channel.trySend(text) }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { channel.close() }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(
                    "VistaChatRealtime",
                    "WebSocket failure type=${t.javaClass.simpleName} cause=${t.cause?.javaClass?.simpleName} http=${response?.code}",
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

    val state: StateFlow<RealtimeConnectionState> = mutableState.asStateFlow()
    val events = mutableEvents.asSharedFlow()

    fun start() {
        if (runner?.isActive == true) return
        runner = scope.launch {
            combine(sessionProvider.account, foreground) { account, active -> account to active }
                .collectLatest { (account, active) ->
                    when {
                        account == null -> mutableState.value = RealtimeConnectionState.DISCONNECTED
                        !active -> mutableState.value = RealtimeConnectionState.PAUSED
                        else -> connectionLoop(account)
                    }
                }
        }
    }

    suspend fun setForeground(active: Boolean) { foreground.emit(active) }

    suspend fun stop() {
        runner?.cancel()
        runner = null
        activeSocket?.close()
        activeSocket = null
        mutableState.value = RealtimeConnectionState.DISCONNECTED
    }

    private suspend fun connectionLoop(account: ChatAccount) {
        val deduplicator = RealtimeEventDeduplicator()
        var attempt = 0
        while (true) {
            var refreshedSession = false
            try {
                val currentAccount = sessionProvider.account.value ?: return
                mutableState.value = if (attempt == 0) {
                    RealtimeConnectionState.CONNECTING
                } else {
                    RealtimeConnectionState.RECONNECTING
                }
                mutableState.value = RealtimeConnectionState.AUTHENTICATING
                val socket = socketFactory.connect(
                    url = internalBaseUrl.toChatWebSocketUrl(),
                    headers = mapOf(
                        "Authorization" to "Bearer ${currentAccount.accessToken}",
                    ),
                )
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
                    "Reconnect type=${failure.javaClass.simpleName} cause=${failure.cause?.javaClass?.simpleName}",
                )
                if (failure is ChatSocketOpenException && failure.statusCode == 401) {
                    refreshedSession = sessionProvider.refresh()
                }
                mutableState.value = if (sessionProvider.account.value == null) {
                    RealtimeConnectionState.FAILED
                } else {
                    RealtimeConnectionState.RECONNECTING
                }
            } finally {
                activeSocket?.close()
                activeSocket = null
            }
            if (refreshedSession) {
                attempt = 0
                continue
            }
            delay(reconnectPolicy.delayMillis(attempt++))
        }
    }

    private fun String.toChatWebSocketUrl(): String {
        val normalized = trimEnd('/')
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")
        return "$normalized/v1/chat/ws"
    }
}
