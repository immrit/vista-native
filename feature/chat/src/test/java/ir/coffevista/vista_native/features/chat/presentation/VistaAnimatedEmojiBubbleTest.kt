package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.presentation.components.SingleEmojiDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VistaAnimatedEmojiBubbleTest {

    @Test
    fun `single emoji is detected correctly`() {
        val singleHeart = SingleEmojiDetector.extractSingleEmoji(
            text = "❤️",
            hasAttachment = false,
            isForwarded = false,
            hasReply = false,
            hasSharedPost = false,
            isDeleted = false,
        )
        assertEquals("❤️", singleHeart)

        val singleLaugh = SingleEmojiDetector.extractSingleEmoji(
            text = "😂",
            hasAttachment = false,
            isForwarded = false,
            hasReply = false,
            hasSharedPost = false,
            isDeleted = false,
        )
        assertEquals("😂", singleLaugh)

        val singleFlameWithSpaces = SingleEmojiDetector.extractSingleEmoji(
            text = "   🔥  ",
            hasAttachment = false,
            isForwarded = false,
            hasReply = false,
            hasSharedPost = false,
            isDeleted = false,
        )
        assertEquals("🔥", singleFlameWithSpaces)
    }

    @Test
    fun `text with words or multiple emojis is rejected`() {
        assertNull(
            SingleEmojiDetector.extractSingleEmoji(
                text = "سلام ❤️",
                hasAttachment = false,
                isForwarded = false,
                hasReply = false,
                hasSharedPost = false,
                isDeleted = false,
            ),
        )

        assertNull(
            SingleEmojiDetector.extractSingleEmoji(
                text = "😂😂",
                hasAttachment = false,
                isForwarded = false,
                hasReply = false,
                hasSharedPost = false,
                isDeleted = false,
            ),
        )

        assertNull(
            SingleEmojiDetector.extractSingleEmoji(
                text = "hello",
                hasAttachment = false,
                isForwarded = false,
                hasReply = false,
                hasSharedPost = false,
                isDeleted = false,
            ),
        )
    }

    @Test
    fun `messages with attachments forwards replies or deleted are rejected`() {
        assertNull(
            SingleEmojiDetector.extractSingleEmoji(
                text = "❤️",
                hasAttachment = true,
                isForwarded = false,
                hasReply = false,
                hasSharedPost = false,
                isDeleted = false,
            ),
        )

        assertNull(
            SingleEmojiDetector.extractSingleEmoji(
                text = "❤️",
                hasAttachment = false,
                isForwarded = true,
                hasReply = false,
                hasSharedPost = false,
                isDeleted = false,
            ),
        )

        assertNull(
            SingleEmojiDetector.extractSingleEmoji(
                text = "❤️",
                hasAttachment = false,
                isForwarded = false,
                hasReply = true,
                hasSharedPost = false,
                isDeleted = false,
            ),
        )

        assertNull(
            SingleEmojiDetector.extractSingleEmoji(
                text = "❤️",
                hasAttachment = false,
                isForwarded = false,
                hasReply = false,
                hasSharedPost = false,
                isDeleted = true,
            ),
        )
    }
}
