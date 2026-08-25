package ir.coffevista.vista_native.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VistaFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationManager: VistaNotificationManager

    @Inject
    lateinit var pushTokenRegistrar: PushTokenRegistrar

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            pushTokenRegistrar.registerToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = data["type"] ?: data["notification_type"] ?: "social"
        val title = notification?.title ?: data["title"] ?: "ویستا"
        val body = notification?.body ?: data["body"] ?: data["content"] ?: "اعلان جدید"

        when (type.lowercase()) {
            "chat", "message" -> {
                val conversationId = data["conversation_id"] ?: data["chat_id"].orEmpty()
                val peerId = data["sender_id"] ?: data["peer_id"].orEmpty()
                val senderName = data["sender_name"] ?: title
                val notificationId = conversationId.hashCode().coerceAtLeast(1)

                notificationManager.showChatNotification(
                    notificationId = notificationId,
                    senderName = senderName,
                    messageText = body,
                    conversationId = conversationId,
                    peerId = peerId,
                )
            }

            "post", "comment", "like" -> {
                val postId = data["post_id"]
                val notificationId = (postId ?: body).hashCode().coerceAtLeast(1)

                notificationManager.showSocialNotification(
                    notificationId = notificationId,
                    title = title,
                    body = body,
                    postId = postId,
                )
            }

            "story" -> {
                val storyId = data["story_id"]
                val notificationId = (storyId ?: body).hashCode().coerceAtLeast(1)

                notificationManager.showSocialNotification(
                    notificationId = notificationId,
                    title = title,
                    body = body,
                    storyId = storyId,
                )
            }

            else -> {
                val notificationId = System.currentTimeMillis().toInt()
                notificationManager.showSocialNotification(
                    notificationId = notificationId,
                    title = title,
                    body = body,
                )
            }
        }
    }
}
