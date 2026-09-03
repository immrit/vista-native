package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageGroupingTest {

    @Test
    fun `reverse list groups same sender messages less than fifteen minutes apart`() {
        val messages = listOf(
            message("newest", "alice", 10_000L),
            message("oldest", "alice", 1_000L),
        )

        val newest = messageGroupPosition(messages, 0)
        val oldest = messageGroupPosition(messages, 1)

        assertFalse(newest.isFirstInGroup)
        assertTrue(newest.isLastInGroup)
        assertTrue(oldest.isFirstInGroup)
        assertFalse(oldest.isLastInGroup)
    }

    @Test
    fun `sender change or fifteen minute boundary splits a visual group`() {
        val senderChanged = listOf(
            message("newest", "alice", 900_000L),
            message("older", "bob", 1L),
        )
        val boundary = listOf(
            message("newest", "alice", 900_000L),
            message("older", "alice", 0L),
        )

        assertTrue(messageGroupPosition(senderChanged, 0).isLastInGroup)
        assertTrue(messageGroupPosition(boundary, 0).isLastInGroup)
    }

    @Test
    fun `own group squares only the right joining edge`() {
        val oldest = MessageGroupPosition(isFirstInGroup = true, isLastInGroup = false)
        val newest = MessageGroupPosition(isFirstInGroup = false, isLastInGroup = true)

        assertTrue(messageBubbleCornerRounding(isMine = true, groupPosition = oldest).topRight)
        assertFalse(messageBubbleCornerRounding(isMine = true, groupPosition = oldest).bottomRight)
        assertFalse(messageBubbleCornerRounding(isMine = true, groupPosition = newest).topRight)
        assertTrue(messageBubbleCornerRounding(isMine = true, groupPosition = newest).bottomRight)
    }

    @Test
    fun `peer group mirrors its joining edge on the left`() {
        val oldest = MessageGroupPosition(isFirstInGroup = true, isLastInGroup = false)
        val newest = MessageGroupPosition(isFirstInGroup = false, isLastInGroup = true)

        assertTrue(messageBubbleCornerRounding(isMine = false, groupPosition = oldest).topLeft)
        assertFalse(messageBubbleCornerRounding(isMine = false, groupPosition = oldest).bottomLeft)
        assertFalse(messageBubbleCornerRounding(isMine = false, groupPosition = newest).topLeft)
        assertTrue(messageBubbleCornerRounding(isMine = false, groupPosition = newest).bottomLeft)
    }

    private fun message(clientId: String, senderId: String, createdAt: Long) = Message(
        accountId = "account",
        conversationId = "conversation",
        clientId = clientId,
        serverId = clientId,
        senderId = senderId,
        content = MessageContent.Text(clientId),
        createdAtEpochMillis = createdAt,
        status = MessageStatus.READ,
        isMine = senderId == "account",
    )
}
