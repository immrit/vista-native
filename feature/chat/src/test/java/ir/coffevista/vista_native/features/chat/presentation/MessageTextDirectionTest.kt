package ir.coffevista.vista_native.features.chat.presentation

import androidx.compose.ui.text.style.TextDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTextDirectionTest {
    @Test
    fun `persian first strong character resolves rtl`() {
        assertEquals(TextDirection.Rtl, resolveMessageTextDirection("این پسره کیه 😡"))
    }

    @Test
    fun `latin first strong character resolves ltr`() {
        assertEquals(TextDirection.Ltr, resolveMessageTextDirection("VISTA_F2N_0813_1309 پیام"))
    }

    @Test
    fun `emoji-only content preserves source order in ltr`() {
        assertEquals(TextDirection.Ltr, resolveMessageTextDirection("😒😂"))
    }

    @Test
    fun `leading neutral characters do not override later rtl text`() {
        assertEquals(TextDirection.Rtl, resolveMessageTextDirection("123 - پیام"))
    }
}
