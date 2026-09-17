package ir.coffevista.vista_native.features.chat.presentation

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageDateLabelTest {
    @Test
    fun `date label mirrors Flutter relative weekday and Jalali policy`() {
        val now = epoch(2026, Calendar.SEPTEMBER, 13)

        assertEquals("امروز", localDayLabel(now, now))
        assertEquals("دیروز", localDayLabel(epoch(2026, Calendar.SEPTEMBER, 12), now))
        assertEquals("پنج‌شنبه", localDayLabel(epoch(2026, Calendar.SEPTEMBER, 10), now))
        assertEquals("۱ فروردین", localDayLabel(epoch(2026, Calendar.MARCH, 21), now))
        assertEquals("۱ فروردین ۱۴۰۴", localDayLabel(epoch(2025, Calendar.MARCH, 21), now))
    }

    private fun epoch(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.timeInMillis
}
