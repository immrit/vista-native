package ir.coffevista.vista_native.core.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lightweight, process-wide coordinator bridging notification runtime state
 * between `:app` (FCM service, MainActivity) and feature modules (`:feature:chat`,
 * `:feature:feed`, `:feature:profile`) without introducing circular module dependencies.
 */
object NotificationRuntimeCoordinator {
    @Volatile
    var isAppForeground: Boolean = false
        private set

    @Volatile
    var activeConversationId: String? = null
        private set

    @Volatile
    var inAppChatSoundsEnabled: Boolean = true
        private set

    @Volatile
    var pushSoundEnabled: Boolean = true
        private set

    @Volatile
    var pushVibrationEnabled: Boolean = true
        private set

    private val mutableDismissedConversations = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val dismissedConversations: SharedFlow<String> = mutableDismissedConversations.asSharedFlow()

    private val mutableSocialPushArrivals = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val socialPushArrivals: SharedFlow<String> = mutableSocialPushArrivals.asSharedFlow()

    private val mutableInAppChatChimes = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val inAppChatChimes: SharedFlow<String> = mutableInAppChatChimes.asSharedFlow()

    private val mutableFcmResyncRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val fcmResyncRequests: SharedFlow<Unit> = mutableFcmResyncRequests.asSharedFlow()

    fun setAppForeground(foreground: Boolean) {
        isAppForeground = foreground
    }

    fun setActiveConversation(conversationId: String?, active: Boolean) {
        val normalized = conversationId?.trim()?.takeIf(String::isNotEmpty)
        if (active && normalized != null) {
            activeConversationId = normalized
            mutableDismissedConversations.tryEmit(normalized)
        } else if (normalized == null || activeConversationId == normalized) {
            activeConversationId = null
        }
    }

    fun isConversationActivelyViewed(conversationId: String?): Boolean {
        val target = conversationId?.trim()?.takeIf(String::isNotEmpty) ?: return false
        return isAppForeground && activeConversationId == target
    }

    fun notifyConversationRead(conversationId: String?) {
        val normalized = conversationId?.trim()?.takeIf(String::isNotEmpty) ?: return
        mutableDismissedConversations.tryEmit(normalized)
    }

    fun notifySocialPushArrived(type: String) {
        mutableSocialPushArrivals.tryEmit(type)
    }

    fun updateNotificationPreferences(
        inAppChatSounds: Boolean,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
    ) {
        inAppChatSoundsEnabled = inAppChatSounds
        pushSoundEnabled = soundEnabled
        pushVibrationEnabled = vibrationEnabled
    }

    fun requestInAppChatChime(conversationId: String) {
        if (!isAppForeground || !inAppChatSoundsEnabled) return
        mutableInAppChatChimes.tryEmit(conversationId)
    }

    fun requestFcmTokenResync() {
        mutableFcmResyncRequests.tryEmit(Unit)
    }
}
