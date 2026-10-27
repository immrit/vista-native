package ir.coffevista.vista_native.features.chat.presentation.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Telegram-style singleton coordinator for voice message playback:
 * 1. Single-active playback: playing one voice automatically pauses any other.
 * 2. Playback speed memory: remembers 1.0x / 1.5x / 2.0x globally across messages.
 * 3. Sequential autoplay: automatically triggers playback of the next voice in conversation.
 */
object VoicePlaybackCoordinator {
    private val _activeMessageId = MutableStateFlow<String?>(null)
    val activeMessageId: StateFlow<String?> = _activeMessageId.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val conversationVoiceQueue = mutableListOf<String>()

    fun startPlaying(messageId: String) {
        _activeMessageId.value = messageId
    }

    fun stopPlaying(messageId: String) {
        if (_activeMessageId.value == messageId) {
            _activeMessageId.value = null
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun cycleSpeed(): Float {
        val next = when (_playbackSpeed.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _playbackSpeed.value = next
        return next
    }

    fun updateVoiceQueue(voiceMessageIds: List<String>) {
        conversationVoiceQueue.clear()
        conversationVoiceQueue.addAll(voiceMessageIds)
    }

    fun nextVoiceId(currentMessageId: String): String? {
        val index = conversationVoiceQueue.indexOf(currentMessageId)
        return if (index != -1 && index + 1 < conversationVoiceQueue.size) {
            conversationVoiceQueue[index + 1]
        } else null
    }
}
