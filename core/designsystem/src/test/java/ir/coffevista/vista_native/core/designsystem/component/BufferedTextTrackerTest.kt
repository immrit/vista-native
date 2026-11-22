package ir.coffevista.vista_native.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class BufferedTextTrackerTest {

    @Test
    fun staleAsyncEchoesDuringFastTypingDoNotDropCharacters() {
        val tracker = BufferedTextTracker("")

        tracker.onLocalInput("س")
        tracker.onLocalInput("سل")
        tracker.onLocalInput("سلا")
        tracker.onLocalInput("سلام")

        // Stale echoes arrive out of phase from an async ViewModel StateFlow
        assertEquals("سلام", tracker.reconcileExternal("س"))
        assertEquals("سلام", tracker.reconcileExternal("سل"))
        assertEquals("سلام", tracker.currentText)

        // Final catch-up echo arrives
        assertEquals("سلام", tracker.reconcileExternal("سلام"))
        assertEquals("سلام", tracker.currentText)
    }

    @Test
    fun genuineExternalMutationUpdatesCurrentTextImmediately() {
        val tracker = BufferedTextTracker("")

        tracker.onLocalInput("متن در حال تایپ")
        assertEquals("متن در حال تایپ", tracker.reconcileExternal("متن در حال تایپ"))

        // External clear (e.g. message/comment submitted or draft discarded)
        assertEquals("", tracker.reconcileExternal(""))
        assertEquals("", tracker.currentText)

        // External hashtag/draft replacement
        tracker.onLocalInput("#ویس")
        assertEquals("#ویستا ", tracker.reconcileExternal("#ویستا "))
        assertEquals("#ویستا ", tracker.currentText)
    }
}
