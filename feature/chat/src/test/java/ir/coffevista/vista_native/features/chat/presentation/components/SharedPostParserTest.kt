package ir.coffevista.vista_native.features.chat.presentation.components

import ir.coffevista.vista_native.features.chat.domain.model.SharedPostDraft
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SharedPostParserTest {

    @Test
    fun `parses standard SharedPostDraft json`() {
        val draft = SharedPostDraft(
            postId = "post-100",
            authorName = "علی رضایی",
            authorUsername = "ali_rezaei",
            content = "این یک پست تست در ویستا است",
            mediaUrls = listOf("https://example.com/img1.jpg"),
            likesCount = 42,
            commentsCount = 7,
            createdAt = "2026-10-15T12:00:00Z",
            isVerified = true,
        )
        val json = Json.encodeToString(draft)
        val parsed = SharedPostParser.tryParse(json)

        assertNotNull(parsed)
        assertEquals("post-100", parsed?.postId)
        assertEquals("علی رضایی", parsed?.authorName)
        assertEquals("ali_rezaei", parsed?.authorUsername)
        assertEquals("این یک پست تست در ویستا است", parsed?.content)
        assertEquals(listOf("https://example.com/img1.jpg"), parsed?.mediaUrls)
        assertEquals(42L, parsed?.likesCount)
        assertEquals(7L, parsed?.commentsCount)
        assertEquals(true, parsed?.isVerified)
    }

    @Test
    fun `parses snake_case legacy Flutter payload`() {
        val legacyJson = """
            {
              "post_id": "flutter-post-200",
              "post_author_name": "سارا محمدی",
              "post_author_username": "sara_m",
              "post_content": "پست به اشتراک گذاشته شده از فلاتر",
              "media_urls": ["https://example.com/photo.jpg"],
              "like_count": 88,
              "comment_count": 12,
              "is_verified": true
            }
        """.trimIndent()

        val parsed = SharedPostParser.tryParse(legacyJson)

        assertNotNull(parsed)
        assertEquals("flutter-post-200", parsed?.postId)
        assertEquals("سارا محمدی", parsed?.authorName)
        assertEquals("sara_m", parsed?.authorUsername)
        assertEquals("پست به اشتراک گذاشته شده از فلاتر", parsed?.content)
        assertEquals(listOf("https://example.com/photo.jpg"), parsed?.mediaUrls)
        assertEquals(88L, parsed?.likesCount)
        assertEquals(12L, parsed?.commentsCount)
        assertEquals(true, parsed?.isVerified)
    }

    @Test
    fun `parses wrapped shared_post_data envelope`() {
        val envelopeJson = """
            {
              "shared_post_data": {
                "postId": "env-300",
                "authorName": "نویسنده",
                "content": "متن درون پاکت",
                "postImageUrl": "https://example.com/single.jpg"
              }
            }
        """.trimIndent()

        val parsed = SharedPostParser.tryParse(envelopeJson)

        assertNotNull(parsed)
        assertEquals("env-300", parsed?.postId)
        assertEquals("نویسنده", parsed?.authorName)
        assertEquals("متن درون پاکت", parsed?.content)
        assertEquals(listOf("https://example.com/single.jpg"), parsed?.mediaUrls)
    }

    @Test
    fun `returns null on plain text or malformed input`() {
        assertNull(SharedPostParser.tryParse("سلام چطوری؟"))
        assertNull(SharedPostParser.tryParse(""))
        assertNull(SharedPostParser.tryParse(null))
        assertNull(SharedPostParser.tryParse("{not a valid json}"))
        assertNull(SharedPostParser.tryParse("""{"someOtherField": 123}"""))
    }
}
