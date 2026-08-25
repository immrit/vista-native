package ir.coffevista.vista_native.features.chat.data.remote

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class TenorGifCatalogTest {
    private val catalog = TenorGifCatalog(OkHttpClient())

    @Test
    fun `flutter tenor v1 payload selects gif and nanogif variants`() {
        val parsed = catalog.parse(
            """
            {
              "results": [
                {
                  "id": "gif-1",
                  "media": [{
                    "gif": {"url": "https://media.tenor.com/full.gif", "dims": [320, 180]},
                    "nanogif": {"url": "https://media.tenor.com/preview.gif", "dims": [160, 90]}
                  }]
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, parsed.size)
        assertEquals("gif-1", parsed.single().id)
        assertEquals("https://media.tenor.com/full.gif", parsed.single().url)
        assertEquals("https://media.tenor.com/preview.gif", parsed.single().previewUrl)
        assertEquals(320, parsed.single().width)
        assertEquals(180, parsed.single().height)
    }

    @Test
    fun `invalid and url-less tenor rows are ignored safely`() {
        assertEquals(emptyList<Any>(), catalog.parse("not-json"))
        assertEquals(emptyList<Any>(), catalog.parse("{\"results\":[{\"id\":\"x\",\"media\":[]}]}") )
    }
}
