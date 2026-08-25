package ir.coffevista.vista_native.features.chat.data.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRealtimeTest {
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
}
