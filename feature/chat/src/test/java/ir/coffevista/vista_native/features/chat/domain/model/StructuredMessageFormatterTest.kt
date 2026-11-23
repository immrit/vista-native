package ir.coffevista.vista_native.features.chat.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StructuredMessageFormatterTest {

    private fun baseMessage(
        content: MessageContent,
        transportType: String? = "text",
    ) = Message(
        accountId = "acc-1",
        conversationId = "conv-1",
        clientId = "client-1",
        serverId = "srv-1",
        senderId = "user-2",
        content = content,
        transportType = transportType,
        createdAtEpochMillis = 1_700_000_000_000L,
        status = MessageStatus.SENT,
        isMine = false,
    )

    @Test
    fun `story reply raw JSON in reply quote never surfaces to UI`() {
        val truncatedStoryJson = """{"story_id":"3bb19d76-32d6-4044-a6ff-f3ecd4d08125","story_owner_id":"57fffc28-744..."""
        val fullStoryJson = """{"story_id":"3bb19d76-32d6-4044-a6ff-f3ecd4d08125","story_owner_id":"57fffc28-7440"}"""

        assertEquals(
            "استوری",
            StructuredMessageFormatter.formatReplyQuotePreview(
                rawReplyContent = truncatedStoryJson,
                replyToKind = "text",
                isStoryReply = true,
            ),
        )
        assertEquals(
            "استوری",
            StructuredMessageFormatter.formatReplyQuotePreview(
                rawReplyContent = fullStoryJson,
                replyToKind = "storyReply",
                isStoryReply = true,
            ),
        )
        assertEquals(
            "کپشن استوری",
            StructuredMessageFormatter.formatReplyQuotePreview(
                rawReplyContent = """{"story_id":"1","story_owner_id":"2","caption":"کپشن استوری"}""",
                replyToKind = "story_reply",
                isStoryReply = true,
            ),
        )
    }

    @Test
    fun `structured storyReply message body never renders raw JSON`() {
        val payload = """{"story_id":"3bb19d76-32d6-4044-a6ff-f3ecd4d08125","story_owner_id":"57fffc28-7440"}"""
        val message = baseMessage(
            content = MessageContent.Structured("storyReply", payload),
            transportType = "storyReply",
        )

        assertEquals("📖 پاسخ به استوری", StructuredMessageFormatter.formatBubbleText(message))
        assertEquals("📖 پاسخ به استوری", StructuredMessageFormatter.formatMessagePreview(message))
        assertEquals("📖 پاسخ به استوری", message.inboxPreviewText())
    }

    @Test
    fun `unknown JSON payloads are masked everywhere`() {
        val unknownJson = """{"unexpected_key":"value","nested":{"a":1}}"""
        val truncatedJson = """{"unexpected_key":"very_long_value_cut_off"""
        val arrayJson = """[{"id":"1","status":"active"}]"""

        for (raw in listOf(unknownJson, truncatedJson, arrayJson)) {
            val msg = baseMessage(MessageContent.Text(raw), transportType = "text")
            val bubble = StructuredMessageFormatter.formatBubbleText(msg)
            val preview = StructuredMessageFormatter.formatMessagePreview(msg)
            val reply = StructuredMessageFormatter.formatReplyQuotePreview(raw)
            val inbox = msg.inboxPreviewText()

            assertFalse("Bubble leaked JSON: $bubble", bubble.contains("{") || bubble.contains("unexpected_key"))
            assertFalse("Preview leaked JSON: $preview", preview.contains("{") || preview.contains("unexpected_key"))
            assertFalse("Reply leaked JSON: $reply", reply.contains("{") || reply.contains("unexpected_key"))
            assertFalse("Inbox leaked JSON: $inbox", inbox.contains("{") || inbox.contains("unexpected_key"))
        }
    }

    @Test
    fun `location, contact, and poll JSONs format cleanly`() {
        assertEquals(
            "📍 موقعیت مکانی: کافه ویستا",
            StructuredMessageFormatter.formatStructuredOrJsonPreview(
                """{"type":"location","lat":35.7,"lng":51.4,"title":"کافه ویستا"}""",
            ),
        )
        assertEquals(
            "👤 مخاطب: علی رضایی",
            StructuredMessageFormatter.formatStructuredOrJsonPreview(
                """{"type":"contact","name":"علی رضایی","phone":"09120000000"}""",
            ),
        )
        assertEquals(
            "📊 نظرسنجی: نظرتان چیست؟",
            StructuredMessageFormatter.formatStructuredOrJsonPreview(
                """{"type":"poll","question":"نظرتان چیست؟"}""",
            ),
        )
    }

    @Test
    fun `normal user text including braces without JSON shape is preserved`() {
        val msg = baseMessage(MessageContent.Text("سلام {دوست} خوبی؟"))
        assertEquals("سلام {دوست} خوبی؟", StructuredMessageFormatter.formatBubbleText(msg))
        assertEquals("سلام {دوست} خوبی؟", StructuredMessageFormatter.formatMessagePreview(msg))
        assertEquals("سلام {دوست} خوبی؟", msg.inboxPreviewText())
    }
}
