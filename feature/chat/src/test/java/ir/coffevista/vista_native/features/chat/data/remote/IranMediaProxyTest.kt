package ir.coffevista.vista_native.features.chat.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IranMediaProxyTest {

    @Test
    fun `blocked hosts are detected accurately`() {
        assertTrue(IranMediaProxy.isHostBlocked("media.tenor.com"))
        assertTrue(IranMediaProxy.isHostBlocked("c.tenor.com"))
        assertTrue(IranMediaProxy.isHostBlocked("media.giphy.com"))
        assertTrue(IranMediaProxy.isHostBlocked("i.imgur.com"))
        assertFalse(IranMediaProxy.isHostBlocked("api.coffevista.ir"))
        assertFalse(IranMediaProxy.isHostBlocked("wsrv.nl"))
        assertFalse(IranMediaProxy.isHostBlocked(null))
    }

    @Test
    fun `tenor gif url is wrapped with edge proxy`() {
        val raw = "https://media.tenor.com/abc123/reaction.gif"
        val proxied = IranMediaProxy.toIranAccessibleUrl(raw, preview = false)
        assertTrue(proxied.startsWith("https://wsrv.nl/?url="))
        assertTrue(proxied.contains("output=gif"))
        assertTrue(proxied.contains("n=-1"))
    }

    @Test
    fun `preview generates fast webp thumbnail`() {
        val raw = "https://media.tenor.com/abc123/reaction.gif"
        val preview = IranMediaProxy.toIranAccessibleUrl(raw, preview = true)
        assertTrue(preview.startsWith("https://wsrv.nl/?url="))
        assertTrue(preview.contains("output=webp"))
        assertTrue(preview.contains("w=220"))
    }

    @Test
    fun `local uris and already proxied urls are untouched`() {
        assertEquals("content://media/123", IranMediaProxy.toIranAccessibleUrl("content://media/123"))
        assertEquals("file:///storage/emulated/0/cat.gif", IranMediaProxy.toIranAccessibleUrl("file:///storage/emulated/0/cat.gif"))
        val alreadyProxied = "https://wsrv.nl/?url=https%3A%2F%2Ffoo.com%2Fa.gif&output=gif&n=-1"
        assertEquals(alreadyProxied, IranMediaProxy.toIranAccessibleUrl(alreadyProxied))
    }

    @Test
    fun `iran gif bank returns populated trending and search results`() {
        val trending = IranGifBank.trending()
        assertTrue(trending.isNotEmpty())
        assertTrue(trending.all { it.url.isNotBlank() && it.previewUrl.isNotBlank() })

        val laughResults = IranGifBank.search("خنده")
        assertTrue(laughResults.isNotEmpty())

        val loveResults = IranGifBank.search("love")
        assertTrue(loveResults.isNotEmpty())

        val danceResults = IranGifBank.search("رقص")
        assertTrue(danceResults.isNotEmpty())
    }
}
