package ir.coffevista.vista_native.core.database.tier

import ir.coffevista.vista_native.core.model.tier.UserTier
import ir.coffevista.vista_native.core.model.tier.UserTierPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTierPolicyTest {
    @Test
    fun `normal user gets exact restricted limits`() {
        val caps = UserTierPolicy.capabilities(role = "normal", verificationType = null, isVerified = false)
        assertEquals(UserTier.NORMAL, caps.tier)
        assertEquals(300, caps.maxPostChars)
        assertFalse(caps.canEditPost)
        assertFalse(caps.canEditComment)
        assertEquals(24, caps.maxStoryHours)
        assertEquals(10, caps.maxUploadMb)
        assertEquals(10L * 1024 * 1024, caps.maxUploadBytes)
        assertFalse(caps.canPostMusic)
        assertEquals(60_000L, caps.maxVideoDurationMs)
        assertFalse(caps.canHideEngagementCounts)
        assertEquals(4, caps.searchPriority)
        assertFalse(caps.adsFree)
    }

    @Test
    fun `black tick user gets celebrity tier limits`() {
        val caps = UserTierPolicy.capabilities(role = "normal", verificationType = "blackTick", isVerified = true)
        assertEquals(UserTier.BLACK_TICK, caps.tier)
        assertEquals(500, caps.maxPostChars)
        assertTrue(caps.canEditPost)
        assertTrue(caps.canEditComment)
        assertEquals(48, caps.maxStoryHours)
        assertEquals(50, caps.maxUploadMb)
        assertTrue(caps.canPostMusic)
        assertEquals(120_000L, caps.maxVideoDurationMs)
        assertTrue(caps.canHideEngagementCounts)
        assertEquals(2, caps.searchPriority)
        assertTrue(caps.adsFree)
    }

    @Test
    fun `gold tick user gets premium tier limits and expires cleanly`() {
        val caps = UserTierPolicy.capabilities(
            role = "premium",
            verificationType = "goldTick",
            isVerified = true,
            subscriptionPlan = "monthly",
            premiumDaysRemaining = 20,
        )
        assertEquals(UserTier.GOLD_TICK, caps.tier)
        assertEquals(1000, caps.maxPostChars)
        assertEquals(50, caps.maxUploadMb)
        assertEquals(120_000L, caps.maxVideoDurationMs)
        assertEquals(3, caps.searchPriority)
        assertTrue(caps.adsFree)

        val expired = UserTierPolicy.capabilities(
            role = "premium",
            verificationType = "goldTick",
            isVerified = true,
            subscriptionPlan = "monthly",
            premiumDaysRemaining = 0,
        )
        assertEquals(UserTier.NORMAL, expired.tier)
    }

    @Test
    fun `blue tick user gets highest tier limits`() {
        val caps = UserTierPolicy.capabilities(role = "moderator", verificationType = "blueTick", isVerified = true)
        assertEquals(UserTier.BLUE_TICK, caps.tier)
        assertEquals(1500, caps.maxPostChars)
        assertTrue(caps.canEditPost)
        assertTrue(caps.canEditComment)
        assertEquals(48, caps.maxStoryHours)
        assertEquals(100, caps.maxUploadMb)
        assertTrue(caps.canPostMusic)
        assertEquals(300_000L, caps.maxVideoDurationMs)
        assertTrue(caps.canHideEngagementCounts)
        assertEquals(1, caps.searchPriority)
        assertTrue(caps.adsFree)
    }
}
