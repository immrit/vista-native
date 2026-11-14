package ir.coffevista.vista_native.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class VistaVerificationBadgeTest {

    @Test
    fun parseVerificationBadgeType_handlesCommonStrings() {
        assertEquals(ResolvedVerificationBadgeType.BLUE_TICK, parseVerificationBadgeType("blue"))
        assertEquals(ResolvedVerificationBadgeType.BLUE_TICK, parseVerificationBadgeType("blueTick"))
        assertEquals(ResolvedVerificationBadgeType.BLUE_TICK, parseVerificationBadgeType("official"))
        assertEquals(ResolvedVerificationBadgeType.GOLD_TICK, parseVerificationBadgeType("gold"))
        assertEquals(ResolvedVerificationBadgeType.GOLD_TICK, parseVerificationBadgeType("goldTick"))
        assertEquals(ResolvedVerificationBadgeType.BLACK_TICK, parseVerificationBadgeType("black"))
        assertEquals(ResolvedVerificationBadgeType.BLACK_TICK, parseVerificationBadgeType("blackTick"))
        assertEquals(ResolvedVerificationBadgeType.BLACK_TICK, parseVerificationBadgeType("VerificationType.blackTick"))
        assertEquals(ResolvedVerificationBadgeType.NONE, parseVerificationBadgeType("none"))
        assertEquals(ResolvedVerificationBadgeType.NONE, parseVerificationBadgeType(null))
        assertEquals(ResolvedVerificationBadgeType.NONE, parseVerificationBadgeType(""))
    }

    @Test
    fun resolveVerificationBadgeType_whenNotVerified_returnsNone() {
        assertEquals(ResolvedVerificationBadgeType.NONE, resolveVerificationBadgeType(false, "blueTick"))
        assertEquals(ResolvedVerificationBadgeType.NONE, resolveVerificationBadgeType(false, "goldTick"))
        assertEquals(ResolvedVerificationBadgeType.NONE, resolveVerificationBadgeType(false, "blackTick"))
    }

    @Test
    fun resolveVerificationBadgeType_whenVerified_resolvesCorrectType() {
        assertEquals(ResolvedVerificationBadgeType.BLACK_TICK, resolveVerificationBadgeType(true, "blackTick"))
        assertEquals(ResolvedVerificationBadgeType.GOLD_TICK, resolveVerificationBadgeType(true, "goldTick"))
        assertEquals(ResolvedVerificationBadgeType.BLUE_TICK, resolveVerificationBadgeType(true, "blueTick"))
        assertEquals(ResolvedVerificationBadgeType.BLUE_TICK, resolveVerificationBadgeType(true, null))
    }

    @Test
    fun verificationBadgeColorArgb_returnsExpectedTierColors() {
        assertEquals(
            0xFF0095F6.toInt(),
            verificationBadgeColorArgb(ResolvedVerificationBadgeType.BLUE_TICK, isDarkSurface = false),
        )
        assertEquals(
            0xFF0095F6.toInt(),
            verificationBadgeColorArgb(ResolvedVerificationBadgeType.BLUE_TICK, isDarkSurface = true),
        )
        assertEquals(
            0xFFFFB300.toInt(),
            verificationBadgeColorArgb(ResolvedVerificationBadgeType.GOLD_TICK, isDarkSurface = false),
        )
        assertEquals(
            0xFF1A1A1A.toInt(),
            verificationBadgeColorArgb(ResolvedVerificationBadgeType.BLACK_TICK, isDarkSurface = false),
        )
        assertEquals(
            0xFFFFFFFF.toInt(),
            verificationBadgeColorArgb(ResolvedVerificationBadgeType.BLACK_TICK, isDarkSurface = true),
        )
    }
}
