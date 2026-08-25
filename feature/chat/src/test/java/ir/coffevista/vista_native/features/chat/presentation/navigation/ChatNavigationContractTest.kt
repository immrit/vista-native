package ir.coffevista.vista_native.features.chat.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatNavigationContractTest {
    @Test
    fun `vista chat deep link opens exact conversation`() {
        assertEquals("conversation-1", ChatNavigationContract.fromDeepLink("vista://chat/conversation-1")?.conversationId)
    }

    @Test
    fun `vista chat deep link retains exact message anchor`() {
        assertEquals(
            ChatDestination("conversation-1", "message-1"),
            ChatNavigationContract.fromDeepLink(
                "vista://chat/conversation-1?messageId=message-1",
            ),
        )
    }

    @Test
    fun `notification accepts canonical backend identifiers`() {
        val destination = ChatNavigationContract.fromNotification(
            mapOf("type" to "chat_message", "conversation_id" to "conversation-1", "message_id" to "message-1"),
        )
        assertEquals(ChatDestination("conversation-1", "message-1"), destination)
    }

    @Test
    fun `unrelated notification is rejected`() {
        assertNull(ChatNavigationContract.fromNotification(mapOf("type" to "post", "conversation_id" to "conversation-1")))
    }

    @Test
    fun `notification accepts Flutter compatible nested JSON payload`() {
        val destination = ChatNavigationContract.fromNotification(
            mapOf(
                "type" to "chat_message",
                "data" to """{"conversation_id":"conversation-2","message_id":"message-2","sender_id":"user-2"}""",
            ),
        )

        assertEquals("conversation-2", destination?.conversationId)
        assertEquals("message-2", destination?.messageId)
    }

    @Test
    fun `flat notification values override nested compatibility payload`() {
        val normalized = ChatNavigationContract.normalizeNotificationData(
            mapOf(
                "conversation_id" to "current-conversation",
                "data" to """{"conversation_id":"legacy-conversation","message_id":"message-3"}""",
            ),
        )

        assertEquals("current-conversation", normalized["conversation_id"])
        assertEquals("message-3", normalized["message_id"])
    }
}
