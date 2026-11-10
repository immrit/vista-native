package ir.coffevista.vista_native.features.chat.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuelInviteParserTest {
    private val sampleId = "11111111-1111-4111-8111-111111111111"

    @Test
    fun `parses serialized gameInvite JSON cleanly`() {
        val raw = DuelInviteParser.serializePayload(
            matchId = sampleId,
            inviterId = "u-1",
            inviterName = "سارا",
            entryFee = 50L,
        )
        val parsed = DuelInviteParser.tryParse(raw, messageType = "gameInvite")
        assertNotNull(parsed)
        assertTrue(parsed?.isStructured == true)
        assertEquals(sampleId, parsed?.matchId)
        assertEquals("سارا", parsed?.inviterName)
        assertEquals(50L, parsed?.entryFee)
        assertEquals("/game/duel/create?code=$sampleId", parsed?.targetGamePath)
    }

    @Test
    fun `parses plain duel link inside chat message and preserves caption`() {
        val parsed = DuelInviteParser.tryParse(
            "بیا دوئل بزنیم https://cafevista.ir/game/duel/create?code=$sampleId",
            messageType = "text",
        )
        assertNotNull(parsed)
        assertFalse(parsed?.isStructured ?: true)
        assertEquals(sampleId, parsed?.matchId)
        assertEquals("بیا دوئل بزنیم", parsed?.captionText)
    }

    @Test
    fun `parses match url and vista duel uri`() {
        val fromMatchUrl = DuelInviteParser.tryParse("https://www.cafevista.ir/game/match/$sampleId")
        assertNotNull(fromMatchUrl)
        assertEquals(sampleId, fromMatchUrl?.matchId)

        val fromCustomScheme = DuelInviteParser.tryParse("vista://duel/$sampleId")
        assertNotNull(fromCustomScheme)
        assertEquals(sampleId, fromCustomScheme?.matchId)
    }

    @Test
    fun `ignores non-game urls and invalid payloads`() {
        assertNull(DuelInviteParser.tryParse("سلام خوبی؟ https://cafevista.ir/post/123"))
        assertNull(DuelInviteParser.tryParse("https://evil.example/game/duel/create?code=$sampleId"))
        assertNull(DuelInviteParser.tryParse("""{"type":"gameInvite","matchId":"invalid"}""", "gameInvite"))
    }
}
