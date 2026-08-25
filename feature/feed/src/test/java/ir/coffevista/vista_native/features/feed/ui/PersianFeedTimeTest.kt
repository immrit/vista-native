package ir.coffevista.vista_native.features.feed.ui

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PersianFeedTimeTest {
    private val now = Instant.parse("2026-08-24T12:00:00Z")

    @Test
    fun `full feed dates use Jalali calendar and Persian digits`() {
        assertEquals(
            "۱۴۰۵/۰۵/۲۶",
            feedRelativeTime("2026-08-17T12:00:00Z", now),
        )
    }

    @Test
    fun `recent feed dates use Persian relative labels`() {
        assertEquals(
            "۵ دقیقه پیش",
            feedRelativeTime("2026-08-24T11:55:00Z", now),
        )
    }
}
