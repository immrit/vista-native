package ir.coffevista.vista_native.features.chat.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatModelsTest {
    @Test
    fun `conversation inbox preview hides a cached base64 envelope`() {
        val conversation = Conversation(
            accountId = "account",
            id = "conversation",
            type = ConversationType.PRIVATE,
            title = "mahshid",
            avatarUrl = null,
            peerId = "peer",
            lastMessage = "8YHCwA4MIJclaktvdNT8hscH0XQ4A+vBEkElz9EC2mBGMH7mQv0X",
            lastMessageAtEpochMillis = null,
            unreadCount = 0,
            isArchived = false,
            isPinned = false,
            isMuted = false,
            requestStatus = null,
        )

        assertEquals("پیام جدید", conversation.inboxPreviewText())
    }

    @Test
    fun `conversation inbox preview preserves readable text`() {
        val conversation = Conversation(
            accountId = "account",
            id = "conversation",
            type = ConversationType.PRIVATE,
            title = "raha.81",
            avatarUrl = null,
            peerId = "peer",
            lastMessage = "سلام، حالت چطوره؟",
            lastMessageAtEpochMillis = null,
            unreadCount = 0,
            isArchived = false,
            isPinned = false,
            isMuted = false,
            requestStatus = null,
        )

        assertEquals("سلام، حالت چطوره؟", conversation.inboxPreviewText())
    }
}
