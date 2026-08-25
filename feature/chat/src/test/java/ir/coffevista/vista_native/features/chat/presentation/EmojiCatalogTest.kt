package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.presentation.components.EmojiCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiCatalogTest {

    @Test
    fun `emoji catalog contains standard categories`() {
        val categories = EmojiCatalog.categories
        assertTrue("Categories should not be empty", categories.isNotEmpty())

        val categoryIds = categories.map { it.id }
        assertTrue(categoryIds.contains("smileys"))
        assertTrue(categoryIds.contains("people"))
        assertTrue(categoryIds.contains("animals"))
        assertTrue(categoryIds.contains("food"))
        assertTrue(categoryIds.contains("travel"))
        assertTrue(categoryIds.contains("activities"))
        assertTrue(categoryIds.contains("objects"))
        assertTrue(categoryIds.contains("symbols"))
        assertTrue(categoryIds.contains("flags"))
    }

    @Test
    fun `categories have valid emoji lists and non-empty icons`() {
        EmojiCatalog.categories.forEach { category ->
            assertTrue(category.id.isNotBlank())
            assertTrue(category.icon.isNotBlank())
            assertTrue(category.title.isNotBlank())
            assertTrue("Category ${category.id} should have emojis", category.emojis.isNotEmpty())
        }
    }

    @Test
    fun `search returns relevant emojis for persian and english queries`() {
        val laughResults = EmojiCatalog.search("خنده")
        assertTrue(laughResults.contains("😂"))

        val loveResults = EmojiCatalog.search("love")
        assertTrue(loveResults.contains("❤️") || loveResults.contains("😍"))

        val heartResults = EmojiCatalog.search("قلب")
        assertTrue(heartResults.contains("❤️"))

        val fireResults = EmojiCatalog.search("fire")
        assertTrue(fireResults.contains("🔥"))

        val emptyResults = EmojiCatalog.search("")
        assertTrue(emptyResults.isEmpty())
    }
}
