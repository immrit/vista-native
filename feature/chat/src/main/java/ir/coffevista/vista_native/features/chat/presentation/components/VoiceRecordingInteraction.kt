package ir.coffevista.vista_native.features.chat.presentation.components

/**
 * Pure interaction policy for the voice dock. Keeping this separate from the
 * recorder makes pointer events deterministic even while recorder startup is
 * asynchronous or the composable is being removed from composition.
 */
internal enum class VoiceCapturePhase { IDLE, STARTING, HOLDING, LOCKED }

internal data class VoiceCaptureInteraction(
    val phase: VoiceCapturePhase = VoiceCapturePhase.IDLE,
    val session: Long = 0L,
) {
    val isActive: Boolean get() = phase != VoiceCapturePhase.IDLE
}

internal object VoiceCapturePolicy {
    fun begin(current: VoiceCaptureInteraction): VoiceCaptureInteraction =
        VoiceCaptureInteraction(phase = VoiceCapturePhase.STARTING, session = current.session + 1)

    fun started(current: VoiceCaptureInteraction, session: Long): VoiceCaptureInteraction =
        if (current.session == session && current.phase == VoiceCapturePhase.STARTING) {
            current.copy(phase = VoiceCapturePhase.HOLDING)
        } else {
            current
        }

    fun lock(current: VoiceCaptureInteraction): VoiceCaptureInteraction =
        if (current.phase == VoiceCapturePhase.HOLDING) current.copy(phase = VoiceCapturePhase.LOCKED) else current

    fun finish(current: VoiceCaptureInteraction): VoiceCaptureInteraction =
        VoiceCaptureInteraction(session = current.session + 1)
}
