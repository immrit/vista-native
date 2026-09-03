package ir.coffevista.vista_native.features.chat.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatLinkPreviewCardTest {

    @Test
    fun extractFirstUrl_returnsCorrectHttpUrl() {
        val message = "سلام به سایت http://coffevista.ir خوش آمدید"
        val extracted = extractFirstUrl(message)
        assertEquals("http://coffevista.ir", extracted)
    }

    @Test
    fun extractFirstUrl_returnsCorrectHttpsUrlWithQuery() {
        val message = "Check this link: https://vista.io/post/123?ref=native&tab=feed"
        val extracted = extractFirstUrl(message)
        assertEquals("https://vista.io/post/123?ref=native&tab=feed", extracted)
    }

    @Test
    fun extractFirstUrl_returnsNullWhenNoUrlPresent() {
        val message = "یک پیام عادی بدون هیچ لینکی"
        val extracted = extractFirstUrl(message)
        assertNull(extracted)
    }
}
