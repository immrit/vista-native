package ir.coffevista.vista_native.features.chat.presentation

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationTimeFormatterTest {
    private val now = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 13, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `same 24 hour window uses clock time`() {
        val timestamp = now - 23 * 60 * 60 * 1000L
        val expected = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
            .format(java.util.Date(timestamp))

        assertEquals(expected, formatConversationTime(timestamp, now))
    }

    @Test
    fun `one elapsed day uses yesterday`() {
        assertEquals("دیروز", formatConversationTime(now - 25 * 60 * 60 * 1000L, now))
    }

    @Test
    fun `two through six elapsed days use relative label`() {
        assertEquals("4 روز", formatConversationTime(now - 4 * 24 * 60 * 60 * 1000L, now))
    }

    @Test
    fun `older timestamps use day and month`() {
        val timestamp = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 30, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals("30/7", formatConversationTime(timestamp, now))
    }
}
