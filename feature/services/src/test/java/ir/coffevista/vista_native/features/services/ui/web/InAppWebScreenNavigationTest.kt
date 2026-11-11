package ir.coffevista.vista_native.features.services.ui.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppWebScreenNavigationTest {
    @Test
    fun `scoped game navigation accepts only its host and path`() {
        assertTrue(isNavigationAllowed("https://coffevista.ir/game/sso?ticket=x", "coffevista.ir", "/game"))
        assertTrue(isNavigationAllowed("https://coffevista.ir/game/room/42", "coffevista.ir", "/game"))
        assertFalse(isNavigationAllowed("https://coffevista.ir/account", "coffevista.ir", "/game"))
        assertFalse(isNavigationAllowed("https://evil.example/game", "coffevista.ir", "/game"))
    }

    @Test
    fun `scoped navigation rejects external schemes but permits webview internals`() {
        assertTrue(isNavigationAllowed("about:blank", "coffevista.ir", "/game"))
        assertTrue(isNavigationAllowed("data:text/plain,ok", "coffevista.ir", "/game"))
        assertFalse(isNavigationAllowed("intent://coffevista.ir/game", "coffevista.ir", "/game"))
        assertFalse(isNavigationAllowed(null, "coffevista.ir", "/game"))
    }

    @Test
    fun `trusted origin helper rejects http and foreign domains`() {
        assertTrue(isTrustedGameOrigin("https://cafevista.ir/game/duel/create"))
        assertTrue(isTrustedGameOrigin("https://www.cafevista.ir/game"))
        assertFalse(isTrustedGameOrigin("http://cafevista.ir/game"))
        assertFalse(isTrustedGameOrigin("https://evil.example/game"))
        assertFalse(isTrustedGameOrigin(null))
    }

    @Test
    fun `bridge request parser extracts valid duel payload and rejects invalid matchId`() {
        val valid = parseDuelShareBridgeRequest(
            """{"requestId":"req-1","matchId":"11111111-1111-4111-8111-111111111111","entryFee":50,"inviterName":"سارا"}""",
        )
        assertNotNull(valid)
        assertEquals("req-1", valid?.requestId)
        assertEquals("11111111-1111-4111-8111-111111111111", valid?.matchId)
        assertEquals(50L, valid?.entryFee)
        assertEquals("سارا", valid?.inviterName)

        assertNull(parseDuelShareBridgeRequest("""{"requestId":"req-2","matchId":"not-a-uuid"}"""))
    }

    @Test
    fun `mini app mode is enabled for game urls and disabled for non-game urls`() {
        assertTrue(shouldUseMiniAppMode("https://cafevista.ir/game"))
        assertTrue(shouldUseMiniAppMode("https://cafevista.ir/game/sso?ticket=abc"))
        assertTrue(shouldUseMiniAppMode("https://cafevista.ir/game/spin"))
        assertFalse(shouldUseMiniAppMode("https://cafevista.ir/post/123"))
        assertFalse(shouldUseMiniAppMode("https://cafevista.ir/games"))
        assertFalse(shouldUseMiniAppMode(null))
    }
}
