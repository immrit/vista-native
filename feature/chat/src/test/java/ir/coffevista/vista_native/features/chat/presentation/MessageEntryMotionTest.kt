package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageEntryMotionTest {
    private val now = 1_000_000L

    @Test
    fun `initial history and old messages stay static`() {
        assertFalse(shouldAnimateMessageEntry(message(isMine = false), emptySet(), now))
        assertFalse(
            shouldAnimateMessageEntry(
                message(isMine = false, createdAt = now - 60_000L),
                setOf("already-presented"),
                now,
            ),
        )
    }

    @Test
    fun `fresh unseen peer message animates once`() {
        val message = message(isMine = false)
        assertTrue(shouldAnimateMessageEntry(message, setOf("older"), now))
        assertFalse(shouldAnimateMessageEntry(message, setOf("older", message.stableKey), now))
    }

    @Test
    fun `only optimistic fresh own message animates`() {
        assertTrue(
            shouldAnimateMessageEntry(
                message(isMine = true, status = MessageStatus.PENDING),
                setOf("older"),
                now,
            ),
        )
        assertFalse(
            shouldAnimateMessageEntry(
                message(isMine = true, status = MessageStatus.SENT),
                setOf("older"),
                now,
            ),
        )
    }

    @Test
    fun `audio player duration is stable and clamps invalid values`() {
        assertEquals("00:00", formatPlayerDuration(-1))
        assertEquals("00:00", formatPlayerDuration(999))
        assertEquals("01:01", formatPlayerDuration(61_000))
    }

    private fun message(
        isMine: Boolean,
        status: MessageStatus = MessageStatus.READ,
        createdAt: Long = now - 1_000L,
    ) = Message(
        accountId = "account",
        conversationId = "conversation",
        clientId = "client-${isMine}-${status.name}-$createdAt",
        serverId = null,
        senderId = if (isMine) "account" else "peer",
        content = MessageContent.Text("motion fixture"),
        createdAtEpochMillis = createdAt,
        status = status,
        isMine = isMine,
    )
}
