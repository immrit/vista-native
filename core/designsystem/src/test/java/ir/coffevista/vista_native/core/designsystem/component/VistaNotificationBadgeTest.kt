package ir.coffevista.vista_native.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class VistaNotificationBadgeTest {

    @Test
    fun formatBadgeCount_singleDigitPersian() {
        assertEquals("۱", formatBadgeCount(1, usePersianDigits = true))
        assertEquals("۸", formatBadgeCount(8, usePersianDigits = true))
        assertEquals("۹", formatBadgeCount(9, usePersianDigits = true))
    }

    @Test
    fun formatBadgeCount_singleDigitEnglish() {
        assertEquals("1", formatBadgeCount(1, usePersianDigits = false))
        assertEquals("8", formatBadgeCount(8, usePersianDigits = false))
        assertEquals("9", formatBadgeCount(9, usePersianDigits = false))
    }

    @Test
    fun formatBadgeCount_multiDigit() {
        assertEquals("۱۲", formatBadgeCount(12, usePersianDigits = true))
        assertEquals("12", formatBadgeCount(12, usePersianDigits = false))
        assertEquals("۹۹", formatBadgeCount(99, usePersianDigits = true))
        assertEquals("99", formatBadgeCount(99, usePersianDigits = false))
    }

    @Test
    fun formatBadgeCount_exceedingMaxCount() {
        assertEquals("۹۹+", formatBadgeCount(100, maxCount = 99, usePersianDigits = true))
        assertEquals("99+", formatBadgeCount(100, maxCount = 99, usePersianDigits = false))
        assertEquals("۹۹+", formatBadgeCount(500, maxCount = 99, usePersianDigits = true))
    }
}
