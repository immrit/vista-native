package ir.coffevista.vista_native.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import dagger.hilt.android.AndroidEntryPoint
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class ChatNotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var vistaNotificationManager: VistaNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId = intent.getStringExtra(VistaNotificationManager.EXTRA_CONVERSATION_ID)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: return
        val notificationId = intent.getIntExtra(
            EXTRA_NOTIFICATION_ID,
            VistaNotificationManager.stableNotificationId("chat", conversationId),
        )

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_REPLY -> {
                        val replyText = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(KEY_TEXT_REPLY)
                            ?.toString()
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                        if (replyText != null) {
                            val result = withTimeoutOrNull(8_000L) {
                                val sent = chatRepository.sendText(conversationId, replyText)
                                if (sent is ChatResult.Success) {
                                    chatRepository.markRead(conversationId)
                                }
                                sent
                            }
                            if (result is ChatResult.Success) {
                                vistaNotificationManager.cancelNotification(notificationId)
                            } else {
                                // Clear the spinner on the notification if reply failed so user knows.
                                vistaNotificationManager.showChatNotification(
                                    notificationId = notificationId,
                                    senderName = "خطا در ارسال پاسخ",
                                    messageText = replyText,
                                    conversationId = conversationId,
                                    peerId = "",
                                    soundEnabled = false,
                                    vibrationEnabled = false,
                                )
                            }
                        }
                    }

                    ACTION_MARK_READ -> {
                        vistaNotificationManager.cancelNotification(notificationId)
                        withTimeoutOrNull(6_000L) {
                            chatRepository.markRead(conversationId)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REPLY = "ir.coffevista.vista_native.action.NOTIFICATION_REPLY"
        const val ACTION_MARK_READ = "ir.coffevista.vista_native.action.NOTIFICATION_MARK_READ"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
