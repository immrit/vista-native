package ir.coffevista.vista_native.features.feed.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CommentTimelineGeometryTest {
    @Test
    fun `timeline constants keep Flutter root and reply rails aligned`() {
        assertEquals(35f, CommentTimelineRailOffsetDp, 0f)
        assertEquals(52f, CommentTimelineLowerSegmentStartDp, 0f)
    }

    @Test
    fun `first reply expansion reserves ten slots before async reload completes`() {
        assertEquals(10, nextVisibleReplyCount(currentCount = 1, loadedReplyCount = 1))
        assertEquals(10, nextVisibleReplyCount(currentCount = 0, loadedReplyCount = 0))
        assertEquals(0, nextVisibleReplyCount(currentCount = 10, loadedReplyCount = 4))
    }
}
