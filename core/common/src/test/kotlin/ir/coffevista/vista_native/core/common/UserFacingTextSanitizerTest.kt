package ir.coffevista.vista_native.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserFacingTextSanitizerTest {

    @Test
    fun `looksLikeJsonPayload detects complete, array, and truncated JSON`() {
        assertTrue(looksLikeJsonPayload("""{"story_id":"123","story_owner_id":"456"}"""))
        assertTrue(looksLikeJsonPayload("""{"story_id":"3bb19d76-32d6-4044-a6ff-f3ecd4d08125","story_owner_id":"57fffc28-744..."""))
        assertTrue(looksLikeJsonPayload("""[{"id":"1"}]"""))
        assertTrue(looksLikeJsonPayload("{}"))
        assertTrue(looksLikeJsonPayload("[]"))

        assertFalse(looksLikeJsonPayload("سلام خوبی؟"))
        assertFalse(looksLikeJsonPayload("{سلام}"))
        assertFalse(looksLikeJsonPayload("[موضوع مهم]"))
        assertFalse(looksLikeJsonPayload(""))
        assertFalse(looksLikeJsonPayload(null))
    }

    @Test
    fun `toSafeUserError blocks raw JSON, HTML, stacktraces, and technical transport details`() {
        val fallback = "خطای پیش‌فرض"
        assertEquals(fallback, """{"error":"bad_request"}""".toSafeUserError(fallback))
        assertEquals(fallback, "<!DOCTYPE html><html><body>502</body></html>".toSafeUserError(fallback))
        assertEquals(fallback, "Expected BEGIN_OBJECT at line 1 column 2".toSafeUserError(fallback))
        assertEquals(fallback, "Unable to resolve host \"api.example.com\": No address associated with hostname".toSafeUserError(fallback))
        assertEquals(fallback, "خطا در پردازش\n\tat com.example.Main.run(Main.kt:10)".toSafeUserError(fallback))
        assertEquals("امکان ارسال پیام وجود ندارد", "  امکان ارسال پیام وجود ندارد  ".toSafeUserError(fallback))
    }
}
