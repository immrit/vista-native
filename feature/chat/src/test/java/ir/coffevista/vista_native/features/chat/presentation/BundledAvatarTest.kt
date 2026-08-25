package ir.coffevista.vista_native.features.chat.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BundledAvatarTest {
    @Test
    fun `Flutter Vista service asset maps to existing Native mark`() {
        assertEquals(
            "vista_logo_mark",
            bundledAvatarDrawableName("lib/utils/images/vistalogo-new.png"),
        )
    }

    @Test
    fun `network avatar is not treated as bundled resource`() {
        assertNull(bundledAvatarDrawableName("https://s3.coffevista.ir/avatar.png"))
    }
}
