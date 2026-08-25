package ir.coffevista.vista_native.features.chat.data.repository

import ir.coffevista.vista_native.features.chat.domain.model.MergeSource
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageMergeReducerTest {
    @Test
    fun `status never regresses for every pair`() {
        val statuses = MessageStatus.entries
        statuses.forEach { old ->
            statuses.forEach { incoming ->
                val merged = MessageMergeReducer.monotonicStatus(old, incoming)
                if (old in listOf(MessageStatus.SENT, MessageStatus.DELIVERED, MessageStatus.READ)) {
                    assertTrue("$old -> $incoming became $merged", merged in listOf(old, MessageStatus.DELIVERED, MessageStatus.READ))
                }
            }
        }
    }

    @Test
    fun `optimistic and server acknowledgement keep one row`() {
        val optimistic = message(clientId = "client-1", serverId = null, status = MessageStatus.PENDING)
        val acknowledged = message(clientId = "client-1", serverId = "server-1", status = MessageStatus.SENT)
        val result = MessageMergeReducer.reduce(listOf(optimistic), listOf(acknowledged), MergeSource.REST)
        assertEquals(1, result.size)
        assertEquals("server-1", result.single().serverId)
        assertEquals(MessageStatus.SENT, result.single().status)
    }

    @Test
    fun `replayed realtime event is deduplicated by server identity`() {
        val event = message(clientId = "server-1", serverId = "server-1", status = MessageStatus.DELIVERED)
        val result = MessageMergeReducer.reduce(listOf(event), listOf(event, event), MergeSource.REALTIME)
        assertEquals(1, result.size)
    }

    @Test
    fun `timestamp ties use stable descending identity`() {
        val a = message(clientId = "a", serverId = "a", status = MessageStatus.SENT)
        val b = message(clientId = "b", serverId = "b", status = MessageStatus.SENT)
        assertEquals(listOf("b", "a"), MessageMergeReducer.reduce(emptyList(), listOf(a, b), MergeSource.REST).map { it.serverId })
    }

    @Test
    fun `delete tombstone wins over later stale content`() {
        val original = message(clientId = "a", serverId = "a", status = MessageStatus.SENT)
            .copy(content = MessageContent.Deleted, deletedAtEpochMillis = 20)
        val stale = message(clientId = "a", serverId = "a", status = MessageStatus.SENT)
        val result = MessageMergeReducer.merge(original, stale, MergeSource.REST)
        assertEquals(MessageContent.Deleted, result.content)
        assertEquals(20L, result.deletedAtEpochMillis)
    }

    @Test
    fun `explicit retry moves failed row to pending without changing identity`() {
        val failed = message(clientId = "client-1", serverId = null, status = MessageStatus.FAILED)
        val retrying = failed.copy(status = MessageStatus.PENDING)
        val result = MessageMergeReducer.merge(failed, retrying, MergeSource.OPTIMISTIC)
        assertEquals("client-1", result.clientId)
        assertEquals(MessageStatus.PENDING, result.status)
    }

    @Test
    fun `server merge carries reactions forward metadata and pinned state`() {
        val existing = message(clientId = "a", serverId = "a", status = MessageStatus.SENT)
        val incoming = existing.copy(
            reactions = mapOf("🔥" to setOf("peer")),
            isForwarded = true,
            forwardedFromSenderName = "فرستنده",
            originalSenderId = "origin",
            originalMessageId = "origin-message",
            replyToSenderName = "پاسخ‌دهنده",
            replyToKind = "text",
            isPinned = true,
        )

        val result = MessageMergeReducer.merge(existing, incoming, MergeSource.REST)

        assertEquals(setOf("peer"), result.reactions["🔥"])
        assertTrue(result.isForwarded)
        assertEquals("فرستنده", result.forwardedFromSenderName)
        assertEquals("origin", result.originalSenderId)
        assertEquals("origin-message", result.originalMessageId)
        assertEquals("پاسخ‌دهنده", result.replyToSenderName)
        assertEquals("text", result.replyToKind)
        assertTrue(result.isPinned)
    }

    @Test
    fun `generic history refresh cannot erase pinned endpoint state`() {
        val pinned = message(clientId = "a", serverId = "a", status = MessageStatus.SENT)
            .copy(isPinned = true)
        val historyEnvelopeWithoutPinField = pinned.copy(isPinned = false)

        val result = MessageMergeReducer.merge(pinned, historyEnvelopeWithoutPinField, MergeSource.REST)

        assertTrue(result.isPinned)
    }

    private fun message(clientId: String, serverId: String?, status: MessageStatus) = Message(
        accountId = "account-a",
        conversationId = "conversation-a",
        clientId = clientId,
        serverId = serverId,
        senderId = "account-a",
        content = MessageContent.Text("fixture"),
        createdAtEpochMillis = 10,
        status = status,
        isMine = true,
    )
}
