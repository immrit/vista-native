package ir.coffevista.vista_native.features.chat.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCapturePolicyTest {
    @Test
    fun `stale recorder startup cannot revive a cancelled session`() {
        val starting = VoiceCapturePolicy.begin(VoiceCaptureInteraction())
        val cancelled = VoiceCapturePolicy.finish(starting)

        assertEquals(VoiceCapturePhase.IDLE, VoiceCapturePolicy.started(cancelled, starting.session).phase)
    }

    @Test
    fun `only a holding session can be locked`() {
        val holding = VoiceCapturePolicy.started(VoiceCapturePolicy.begin(VoiceCaptureInteraction()), 1)

        assertEquals(VoiceCapturePhase.LOCKED, VoiceCapturePolicy.lock(holding).phase)
        assertEquals(VoiceCapturePhase.IDLE, VoiceCapturePolicy.lock(VoiceCaptureInteraction()).phase)
    }
}
