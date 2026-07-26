package ir.coffevista.vista_native.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class BackendErrorParserTest {
    @Test
    fun parsesFlatEnvelope() {
        val parsed = BackendErrorParser.parse(
            """{"error":"feature_disabled","message":"غیرفعال است"}""",
        )
        assertEquals("feature_disabled", parsed.code)
        assertEquals("غیرفعال است", parsed.message)
    }

    @Test
    fun parsesNestedEnvelope() {
        val parsed = BackendErrorParser.parse(
            """{"error":{"code":"bad_session","message":"نشست نامعتبر","retry_after_seconds":9}}""",
        )
        assertEquals("bad_session", parsed.code)
        assertEquals("نشست نامعتبر", parsed.message)
        assertEquals(9, parsed.retryAfterSeconds)
    }

    @Test
    fun parsesLegacyErrorsArrayAndHeader() {
        val parsed = BackendErrorParser.parse(
            """{"errors":[{"code":"legacy","detail":"خطای قدیمی"}]}""",
            retryAfterHeader = "12",
        )
        assertEquals("legacy", parsed.code)
        assertEquals("خطای قدیمی", parsed.message)
        assertEquals(12, parsed.retryAfterSeconds)
    }
}
