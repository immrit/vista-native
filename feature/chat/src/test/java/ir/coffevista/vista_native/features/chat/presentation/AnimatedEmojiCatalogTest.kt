package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.presentation.components.AnimatedEmojiCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedEmojiCatalogTest {
    @Test
    fun `heart resolves with and without variation selector`() {
        assertEquals(listOf("2764_fe0f", "2764"), AnimatedEmojiCatalog.candidateKeys("\u2764\uFE0F"))
    }

    @Test
    fun `supplementary plane emoji resolves to single codepoint`() {
        assertEquals(listOf("1f602"), AnimatedEmojiCatalog.candidateKeys("\uD83D\uDE02"))
    }
}
