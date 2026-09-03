package ir.coffevista.vista_native.features.chat.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedPostDraftTest {
    @Test
    fun `wire payload keeps Flutter shared post field names`() {
        val json = Json.encodeToString(
            SharedPostDraft(
                postId = "post-1",
                authorName = "Vista User",
                authorUsername = "vista",
                content = "سلام",
                mediaUrls = listOf("https://example.test/image.jpg"),
                likesCount = 7,
                commentsCount = 3,
                createdAt = "2026-09-02T00:00:00Z",
            ),
        )

        val payload = Json.parseToJsonElement(json).jsonObject
        assertEquals("post-1", payload["postId"]?.jsonPrimitive?.content)
        assertEquals("vista", payload["authorUsername"]?.jsonPrimitive?.content)
        assertEquals("سلام", payload["content"]?.jsonPrimitive?.content)
    }
}
