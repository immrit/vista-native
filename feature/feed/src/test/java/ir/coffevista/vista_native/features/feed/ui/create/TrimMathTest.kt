package ir.coffevista.vista_native.features.feed.ui.create

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrimMathTest {

    @Test
    fun clampClipMs_neverExceedsSourceOrCap() {
        assertEquals(15_000L, TrimMath.clampClipMs(durationMs = 240_000, requestedMs = 60_000, maxClipMs = 15_000))
        assertEquals(8_000L, TrimMath.clampClipMs(durationMs = 8_000, requestedMs = 15_000, maxClipMs = 15_000))
    }

    @Test
    fun clampClipMs_respectsMinimumUnlessSourceIsShorter() {
        assertEquals(3_000L, TrimMath.clampClipMs(durationMs = 240_000, requestedMs = 500, maxClipMs = 15_000))
        assertEquals(2_000L, TrimMath.clampClipMs(durationMs = 2_000, requestedMs = 500, maxClipMs = 15_000))
        assertEquals(0L, TrimMath.clampClipMs(durationMs = 0, requestedMs = 5_000, maxClipMs = 15_000))
    }

    @Test
    fun clampStartMs_keepsWindowInsideTrack() {
        assertEquals(0L, TrimMath.clampStartMs(-500, durationMs = 60_000, clipMs = 15_000))
        assertEquals(45_000L, TrimMath.clampStartMs(59_000, durationMs = 60_000, clipMs = 15_000))
        assertEquals(20_000L, TrimMath.clampStartMs(20_000, durationMs = 60_000, clipMs = 15_000))
        // Window as long as the whole track: nothing to scroll.
        assertEquals(0L, TrimMath.clampStartMs(5_000, durationMs = 10_000, clipMs = 10_000))
    }

    @Test
    fun scrollAndStartAreInverse() {
        val pxPerMs = 0.02f
        val duration = 300_000L
        val clip = 15_000L
        listOf(0L, 1_000L, 123_000L, 285_000L).forEach { start ->
            val scroll = TrimMath.scrollFromStart(start, pxPerMs, duration, clip)
            assertEquals(start, TrimMath.startFromScroll(scroll, pxPerMs, duration, clip))
        }
    }

    @Test
    fun startFromScroll_clampsOverscrollAndBadScale() {
        assertEquals(285_000L, TrimMath.startFromScroll(Int.MAX_VALUE / 2, 0.02f, 300_000, 15_000))
        assertEquals(0L, TrimMath.startFromScroll(-40, 0.02f, 300_000, 15_000))
        assertEquals(0L, TrimMath.startFromScroll(100, 0f, 300_000, 15_000))
    }

    @Test
    fun clipLengthOptions_forLongAndShortSources() {
        assertEquals(listOf(5_000L, 10_000L, 15_000L), TrimMath.clipLengthOptions(240_000, 15_000))
        assertEquals(listOf(20_000L, 40_000L, 60_000L), TrimMath.clipLengthOptions(240_000, 60_000))
        assertEquals(listOf(2_000L), TrimMath.clipLengthOptions(2_000, 15_000))
        assertTrue(TrimMath.clipLengthOptions(0, 15_000).isEmpty())
        TrimMath.clipLengthOptions(8_000, 15_000).forEach { assertTrue(it in 3_000L..8_000L) }
    }

    @Test
    fun snapToKeyframe_picksNearestKeyframeThatFits() {
        val keyframes = longArrayOf(0, 2_000, 4_100, 6_000, 58_000)
        assertEquals(4_100L, TrimMath.snapToKeyframe(keyframes, startMs = 4_700, maxStartMs = 50_000))
        assertEquals(6_000L, TrimMath.snapToKeyframe(keyframes, startMs = 5_500, maxStartMs = 50_000))
        // 58s keyframe would leave no room for a full window, so it is skipped.
        assertEquals(6_000L, TrimMath.snapToKeyframe(keyframes, startMs = 57_000, maxStartMs = 50_000))
    }

    @Test
    fun snapToKeyframe_withoutUsableKeyframeKeepsClampedStart() {
        assertEquals(30_000L, TrimMath.snapToKeyframe(longArrayOf(), startMs = 30_000, maxStartMs = 40_000))
        assertEquals(40_000L, TrimMath.snapToKeyframe(longArrayOf(90_000), startMs = 50_000, maxStartMs = 40_000))
    }

    @Test
    fun formatTime_usesLatinDigitsAndHours() {
        assertEquals("00:00", TrimMath.formatTime(0))
        assertEquals("01:05", TrimMath.formatTime(65_900))
        assertEquals("1:01:01", TrimMath.formatTime(3_661_000))
        assertEquals("00:00", TrimMath.formatTime(-5))
    }

    @Test
    fun clipLengthOptions_areSortedAndDistinct() {
        val options = TrimMath.clipLengthOptions(100_000, 3_000)
        assertArrayEquals(options.sorted().distinct().toLongArray(), options.toLongArray())
    }
}
