package ir.coffevista.vista_native.features.chat.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionGenerationGuardTest {
    @Test
    fun `logout invalidates callbacks from the previous session`() {
        val guard = SessionGenerationGuard()
        val beforeLogout = guard.snapshot()

        guard.invalidate()

        assertFalse(guard.isCurrent(beforeLogout))
        assertTrue(guard.isCurrent(guard.snapshot()))
    }

    @Test
    fun `repeated logout invalidates every earlier generation`() {
        val guard = SessionGenerationGuard()
        val first = guard.snapshot()
        guard.invalidate()
        val second = guard.snapshot()
        guard.invalidate()

        assertFalse(guard.isCurrent(first))
        assertFalse(guard.isCurrent(second))
        assertTrue(guard.isCurrent(guard.snapshot()))
    }
}
