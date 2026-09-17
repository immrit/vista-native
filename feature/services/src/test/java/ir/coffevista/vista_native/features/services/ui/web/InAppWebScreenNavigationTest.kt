package ir.coffevista.vista_native.features.services.ui.web

import org.junit.Assert.assertFalse
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
}
