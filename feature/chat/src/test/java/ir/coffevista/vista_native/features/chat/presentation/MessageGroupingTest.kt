package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.Attachment
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import org.junit.Assert.assertEquals
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

    @Test
    fun `unread divider follows the first read or own row after contiguous peer messages`() {
        val window = unreadMessageWindow(
            listOf(
                message("newest", "peer", 4L, MessageStatus.DELIVERED),
                message("newer", "peer", 3L, MessageStatus.SENT),
                message("own-boundary", "account", 2L),
                message("oldest", "peer", 1L),
            ),
        )

        assertEquals(2, window.count)
        assertEquals(2, window.dividerIndex)
    }

    @Test
    fun `all loaded unread messages anchor divider on oldest row`() {
        val window = unreadMessageWindow(
            listOf(
                message("newest", "peer", 2L, MessageStatus.DELIVERED),
                message("oldest", "peer", 1L, MessageStatus.SENT),
            ),
        )

        assertEquals(2, window.count)
        assertEquals(1, window.dividerIndex)
    }

    @Test
    fun explicitMediaGroupBecomesOneStableAlbumRow() {
        val messages = listOf(
            image("newest", "alice", 3_000L, "album"),
            image("older", "alice", 2_000L, "album"),
            message("text", "alice", 1_000L),
        )

        val rows = buildMessageRenderItems(messages)

        assertEquals(2, rows.size)
        assertTrue(rows.first().isAlbum)
        assertEquals(listOf("newest", "older"), rows.first().messages.map { it.clientId })
        assertEquals(0, renderItemIndexForMessage(rows, "older"))
        assertEquals(1, renderItemIndexForMessage(rows, "text"))
    }

    @Test
    fun albumGroupingRespectsSenderTimeWindowsAndTenItemCap() {
        val capped = (0 until 12).map { index ->
            image("image-" + index, "alice", 60_000L - index, "same")
        }
        val cappedRows = buildMessageRenderItems(capped)
        assertEquals(10, cappedRows.first().messages.size)
        assertEquals(2, cappedRows[1].messages.size)

        val implicit = buildMessageRenderItems(
            listOf(
                image("one", "alice", 60_000L),
                image("two", "alice", 40_000L),
                image("too-late", "alice", 10_000L),
                image("other-sender", "bob", 9_000L),
            ),
        )
        assertEquals(2, implicit.first().messages.size)
        assertFalse(implicit[1].isAlbum)
        assertFalse(implicit[2].isAlbum)
    }

    @Test
    fun albumSpanUsesOldestAndNewestEdgesForVisualGrouping() {
        val messages = listOf(
            message("below", "alice", 4_000L),
            image("album-new", "alice", 3_000L, "album"),
            image("album-old", "alice", 2_000L, "album"),
            message("above", "alice", 1_000L),
        )

        val position = messageGroupPosition(messages, index = 1, spanLength = 2)

        assertFalse(position.isFirstInGroup)
        assertFalse(position.isLastInGroup)
    }

    @Test
    fun floatingDateUsesTheActualVisibleRenderRow() {
        val rows = buildMessageRenderItems(
            listOf(
                message("newest", "alice", 3_000L),
                image("album-new", "alice", 2_000L, "album"),
                image("album-old", "alice", 1_900L, "album"),
                message("oldest", "alice", 1_000L),
            ),
        )

        assertEquals(3_000L, visibleMessageDateEpochMillis(rows, 0))
        assertEquals(2_000L, visibleMessageDateEpochMillis(rows, 1))
        assertEquals(1_000L, visibleMessageDateEpochMillis(rows, 2))
        assertEquals(null, visibleMessageDateEpochMillis(rows, 3))
    }

    @Test
    fun adaptiveAlbumRowsNeverLeaveASingletonTail() {
        assertEquals(listOf(listOf(0, 1, 2), listOf(3, 4)), albumRowIndices(5))
        assertEquals(listOf(listOf(0, 1, 2), listOf(3, 4), listOf(5, 6)), albumRowIndices(7))
        assertEquals(listOf(listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7)), albumRowIndices(8))
        assertEquals(
            listOf(listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7), listOf(8, 9)),
            albumRowIndices(10),
        )
    }

    private fun image(
        clientId: String,
        senderId: String,
        createdAt: Long,
        mediaGroupId: String? = null,
    ) = message(clientId, senderId, createdAt).copy(
        attachment = Attachment(
            kind = AttachmentKind.IMAGE,
            remoteUrl = "https://example.test/" + clientId + ".jpg",
            fileName = clientId + ".jpg",
            mimeType = "image/jpeg",
            sizeBytes = 1L,
            mediaGroupId = mediaGroupId,
        ),
    )

    private fun message(
        clientId: String,
        senderId: String,
        createdAt: Long,
        status: MessageStatus = MessageStatus.READ,
    ) = Message(
        accountId = "account",
        conversationId = "conversation",
        clientId = clientId,
        serverId = clientId,
        senderId = senderId,
        content = MessageContent.Text(clientId),
        createdAtEpochMillis = createdAt,
        status = status,
        isMine = senderId == "account",
    )
}
