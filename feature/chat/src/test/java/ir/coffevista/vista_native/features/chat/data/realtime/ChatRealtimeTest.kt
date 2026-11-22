package ir.coffevista.vista_native.features.chat.data.realtime

import ir.coffevista.vista_native.core.network.AssumeOnlineNetworkMonitor
import ir.coffevista.vista_native.features.chat.domain.model.RealtimeConnectionState
import ir.coffevista.vista_native.features.chat.domain.repository.ChatAccount
import ir.coffevista.vista_native.features.chat.domain.repository.ChatSessionProvider
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRealtimeTest {
    @Test
    fun `realtime auth survives proxies that strip authorization on upgrade`() {
        val headers = ChatAccount(
            accountId = "account-1",
            accessToken = "header.payload.signature",
            deviceId = "device-1",
        ).realtimeHeaders()

        assertEquals("Bearer header.payload.signature", headers["Authorization"])
        assertEquals(
            "Authorization, header.payload.signature",
            headers["Sec-WebSocket-Protocol"],
        )
        assertEquals("device-1", headers["X-Device-ID"])
    }

    @Test
    fun `new message envelope parses backend fields`() {
        val event = RealtimeEventParser.parse(
            """{"type":"new_message","event_id":"e1","conversation_id":"c1","occurred_at":"2026-08-06T00:00:00Z","data":{"id":"m1","conversation_id":"c1","sender_id":"u2","message_type":"text","content":"fixture","created_at":"2026-08-06T00:00:00Z","is_sent":true}}""",
        ) as RealtimeEvent.NewMessage
        assertEquals("e1", event.eventId)
        assertEquals("m1", event.message.id)
        assertEquals("c1", event.conversationId)
    }

    @Test
    fun `reaction updated envelope parses backend reaction payload`() {
        val payload = """
            {"type":"reaction_updated","event_id":"rev-1","conversation_id":"c1","occurred_at":"2026-08-06T00:00:00Z","data":{"message_id":"m1","conversation_id":"c1","reactions":[{"user_id":"u2","emoji":"❤️"}]}}
        """.trimIndent()
        val event = RealtimeEventParser.parse(payload) as RealtimeEvent.ReactionUpdated
        assertEquals("rev-1", event.eventId)
        assertEquals("c1", event.conversationId)
        assertEquals("m1", event.update.messageId)
        assertEquals("c1", event.update.conversationId)
        assertEquals(1, event.update.reactions?.size)
        assertEquals("u2", event.update.reactions?.get(0)?.userId)
        assertEquals("❤️", event.update.reactions?.get(0)?.emoji)
    }

    @Test
    fun `reaction updated envelope parses without conversation_id inside data`() {
        val payload = """
            {"type":"reaction_updated","event_id":"rev-2","data":{"message_id":"m2","reactions":[{"user_id":"u3","emoji":"🔥"}]}}
        """.trimIndent()
        val event = RealtimeEventParser.parse(payload) as RealtimeEvent.ReactionUpdated
        assertEquals("rev-2", event.eventId)
        assertNull(event.conversationId)
        assertEquals("m2", event.update.messageId)
        assertEquals("🔥", event.update.reactions?.single()?.emoji)
    }

    @Test
    fun `unknown and malformed events are fail safe`() {
        assertTrue(RealtimeEventParser.parse("""{"type":"future_event","data":{}}""") is RealtimeEvent.Unknown)
        assertNull(RealtimeEventParser.parse("not-json"))
    }

    @Test
    fun `event id deduplicator is bounded and accepts legacy events`() {
        val deduplicator = RealtimeEventDeduplicator(capacity = 2)
        assertTrue(deduplicator.accept(null))
        assertTrue(deduplicator.accept("a"))
        assertFalse(deduplicator.accept("a"))
        assertTrue(deduplicator.accept("b"))
        assertTrue(deduplicator.accept("c"))
        assertTrue(deduplicator.accept("a"))
    }

    @Test
    fun `reconnect delay is capped and deterministic without jitter`() {
        val policy = ReconnectPolicy(maxDelayMillis = 30_000, jitterRatio = 0.0) { 0.5 }
        assertEquals(1_000, policy.delayMillis(0))
        assertEquals(2_000, policy.delayMillis(1))
        assertEquals(30_000, policy.delayMillis(20))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when websocket fails while online and rest sync succeeds state stays connected`() = runTest {
        val sessionProvider = object : ChatSessionProvider {
            override val account: StateFlow<ChatAccount?> = MutableStateFlow(
                ChatAccount("acc-1", "token-1", "dev-1"),
            )
        }
        val failingWs = object : ChatWebSocketFactory {
            override suspend fun connect(url: String, headers: Map<String, String>): ChatSocket {
                throw IOException("ws unreachable")
            }
        }
        var fakeClockMs = 1_000L
        val coordinator = ChatRealtimeCoordinator(
            scope = backgroundScope,
            sessionProvider = sessionProvider,
            socketFactory = failingWs,
            internalBaseUrl = "https://api.example.com",
            reconnectPolicy = ReconnectPolicy(jitterRatio = 0.0) { 0.5 },
            networkMonitor = AssumeOnlineNetworkMonitor,
            clockMs = { fakeClockMs },
        )
        coordinator.setSyncCheckHandler { true }
        coordinator.setForeground(true)
        coordinator.start()
        runCurrent()

        assertEquals(RealtimeConnectionState.CONNECTED, coordinator.state.value)

        // Advance past the 15s fallback retry interval; state must remain CONNECTED while REST sync works.
        fakeClockMs += 15_000L
        advanceTimeBy(15_000L)
        runCurrent()
        assertEquals(RealtimeConnectionState.CONNECTED, coordinator.state.value)

        coordinator.stop()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `state becomes reconnecting after two consecutive poll failures and recovers on notifyRestReachable`() = runTest {
        val sessionProvider = object : ChatSessionProvider {
            override val account: StateFlow<ChatAccount?> = MutableStateFlow(
                ChatAccount("acc-1", "token-1", "dev-1"),
            )
        }
        val failingWs = object : ChatWebSocketFactory {
            override suspend fun connect(url: String, headers: Map<String, String>): ChatSocket {
                throw IOException("ws unreachable")
            }
        }
        var fakeClockMs = 1_000L
        var syncShouldSucceed = true
        val coordinator = ChatRealtimeCoordinator(
            scope = backgroundScope,
            sessionProvider = sessionProvider,
            socketFactory = failingWs,
            internalBaseUrl = "https://api.example.com",
            reconnectPolicy = ReconnectPolicy(jitterRatio = 0.0) { 0.5 },
            networkMonitor = AssumeOnlineNetworkMonitor,
            clockMs = { fakeClockMs },
        )
        coordinator.setSyncCheckHandler { syncShouldSucceed }
        coordinator.setForeground(true)
        coordinator.start()
        runCurrent()
        assertEquals(RealtimeConnectionState.CONNECTED, coordinator.state.value)

        // Single poll failure should NOT flash RECONNECTING yet.
        syncShouldSucceed = false
        fakeClockMs += 3_000L
        advanceTimeBy(3_000L)
        runCurrent()
        assertEquals(RealtimeConnectionState.CONNECTED, coordinator.state.value)

        // Second consecutive poll failure transitions to RECONNECTING.
        fakeClockMs += 3_000L
        advanceTimeBy(3_000L)
        runCurrent()
        assertEquals(RealtimeConnectionState.RECONNECTING, coordinator.state.value)

        // Any successful REST action immediately restores CONNECTED.
        coordinator.notifyRestReachable()
        assertEquals(RealtimeConnectionState.CONNECTED, coordinator.state.value)

        coordinator.stop()
    }
}
