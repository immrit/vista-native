package ir.coffevista.vista_native.features.shell

import ir.coffevista.vista_native.core.model.navigation.VistaGameUrlHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VistaGameUrlHelperTest {
    private val matchUuid = "11111111-1111-4111-8111-111111111111"

    @Test
    fun `parses valid cafevista https game and duel links cleanly`() {
        assertEquals("/game", VistaGameUrlHelper.parseGameTargetPath("https://cafevista.ir/game"))
        assertEquals(
            "/game/duel/create?code=$matchUuid",
            VistaGameUrlHelper.parseGameTargetPath("https://cafevista.ir/game/duel/create?code=$matchUuid"),
        )
        assertEquals(
            "/game/duel/create?code=$matchUuid",
            VistaGameUrlHelper.parseGameTargetPath("https://www.cafevista.ir/game/duel/create?code=$matchUuid"),
        )
        assertEquals(
            "/game/match/$matchUuid",
            VistaGameUrlHelper.parseGameTargetPath("https://cafevista.ir/game/match/$matchUuid"),
        )
        assertEquals(
            "/game/play/$matchUuid",
            VistaGameUrlHelper.parseGameTargetPath("https://cafevista.ir/game/play/$matchUuid"),
        )
    }

    @Test
    fun `parses custom vista game and duel scheme uris`() {
        assertEquals("/game", VistaGameUrlHelper.parseGameTargetPath("vista://game"))
        assertEquals(
            "/game/duel/create?code=$matchUuid",
            VistaGameUrlHelper.parseGameTargetPath("vista://game/duel/create?code=$matchUuid"),
        )
        assertEquals(
            "/game/duel/create?code=$matchUuid",
            VistaGameUrlHelper.parseGameTargetPath("vista://duel/$matchUuid"),
        )
        assertEquals(
            "/game/match/$matchUuid",
            VistaGameUrlHelper.parseGameTargetPath("vista://game/match/$matchUuid"),
        )
    }

    @Test
    fun `rejects unsafe paths, sso redirect loops, and foreign hosts`() {
        assertNull(VistaGameUrlHelper.parseGameTargetPath("https://evil.example/game/duel/create?code=$matchUuid"))
        assertNull(VistaGameUrlHelper.parseGameTargetPath("https://cafevista.ir/game/sso?ticket=abc"))
        assertNull(VistaGameUrlHelper.parseGameTargetPath("vista://duel/not-a-uuid"))
        assertNull(VistaGameUrlHelper.parseGameTargetPath("https://cafevista.ir/game/duel/create?code=not-a-uuid"))
        assertNull(VistaGameUrlHelper.sanitizeGameTargetPath("//evil.example/game"))
        assertNull(VistaGameUrlHelper.sanitizeGameTargetPath("/game/../../etc/passwd"))
        assertNull(VistaGameUrlHelper.sanitizeGameTargetPath("/feed"))
    }
}
