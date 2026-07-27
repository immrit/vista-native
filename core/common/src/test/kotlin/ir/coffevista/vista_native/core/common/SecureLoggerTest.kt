package ir.coffevista.vista_native.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureLoggerTest {
    @Test
    fun sensitiveKeysAndValuesAreRedacted() {
        val records = mutableListOf<LogRecord>()
        val logger = RedactingLogger(enabled = true, sink = records::add)
        val jwt = "abcdefgh.ijklmnop.qrstuvwx"

        logger.log(
            level = LogLevel.ERROR,
            event = "auth failed for 09123456789",
            fields = mapOf(
                "authorization" to "Bearer $jwt",
                "message" to "متن خصوصی",
                "safe_code" to "bad_session",
                "detail" to "Bearer $jwt phone=09123456789",
            ),
        )

        val rendered = records.single().toString()
        assertTrue(rendered.contains("[REDACTED]"))
        assertTrue(rendered.contains("bad_session"))
        assertFalse(rendered.contains(jwt))
        assertFalse(rendered.contains("09123456789"))
        assertFalse(rendered.contains("متن خصوصی"))
    }

    @Test
    fun disabledLoggerWritesNothing() {
        val records = mutableListOf<LogRecord>()
        val logger = RedactingLogger(enabled = false, sink = records::add)

        logger.log(LogLevel.DEBUG, "event")

        assertEquals(emptyList<LogRecord>(), records)
    }
}
