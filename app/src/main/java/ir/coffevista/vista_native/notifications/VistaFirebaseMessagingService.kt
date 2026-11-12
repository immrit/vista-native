package ir.coffevista.vista_native.notifications

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import ir.coffevista.vista_native.core.common.NotificationRuntimeCoordinator
import ir.coffevista.vista_native.core.network.ExternalMedia
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

@AndroidEntryPoint
class VistaFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationManager: VistaNotificationManager

    @Inject
    lateinit var pushTokenRegistrar: PushTokenRegistrar

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    @ExternalMedia
    lateinit var externalMediaClient: OkHttpClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            pushTokenRegistrar.registerToken(token = token, force = true)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val notification = remoteMessage.notification
        val rawType = (data["type"] ?: data["notification_type"] ?: "social").trim().lowercase()

        // Silent control push: dismiss tray notification when conversation was read elsewhere.
        if (rawType == "chat_notification_clear") {
            val conversationId = data["conversation_id"] ?: data["chat_id"].orEmpty()
            if (conversationId.isNotBlank()) {
                notificationManager.cancelChatNotification(conversationId)
            }
            return
        }

        val title = notification?.title?.takeIf(String::isNotBlank)
            ?: data["title"]?.takeIf(String::isNotBlank)
            ?: "ویستا"
        val rawBody = listOf(notification?.body, data["body"], data["content"], data["message"])
            .firstOrNull { !it.isNullOrBlank() }
        val body = ChatNotificationContentFormatter.sanitizeSocialBody(rawBody)
        val soundEnabled = data["sound_enabled"]?.toBooleanStrictOrNull() ?: true
        val vibrationEnabled = data["vibration_enabled"]?.toBooleanStrictOrNull() ?: true
        val deeplink = data["deeplink"]?.trim()?.takeIf(String::isNotEmpty)

        when (rawType) {
            "chat", "message", "chat_message" -> {
                val conversationId = (data["conversation_id"] ?: data["chat_id"]).orEmpty().trim()
                if (NotificationRuntimeCoordinator.isConversationActivelyViewed(conversationId)) {
                    return
                }

                // Refresh conversation previews/unread counts safely without marking messages read.
                serviceScope.launch {
                    runCatching { chatRepository.refreshConversations(reset = true) }
                }

                val peerId = (data["sender_id"] ?: data["peer_id"]).orEmpty()
                val senderName = data["sender_name"]?.takeIf(String::isNotBlank) ?: title
                val isGroup = data["conversation_type"].equals("group", ignoreCase = true)
                val isSecret = data["is_secret"]?.toBooleanStrictOrNull() == true ||
                    data["conversation_type"].equals("secret", ignoreCase = true)
                val groupName = data["group_name"]?.takeIf(String::isNotBlank)
                val avatarUrl = if (isGroup && !data["group_avatar"].isNullOrBlank()) {
                    data["group_avatar"]
                } else {
                    data["sender_avatar"]
                }
                val chatMessageText = ChatNotificationContentFormatter.format(
                    rawBody = rawBody,
                    messageType = data["chat_message_type"] ?: data["attachment_type"],
                    fileName = data["file_name"] ?: data["attachment_file_name"],
                    isSecret = isSecret,
                )
                val notificationId = VistaNotificationManager.stableNotificationId(
                    namespace = "chat",
                    rawKey = conversationId.ifBlank { peerId.ifBlank { chatMessageText } },
                )
                val timestampMillis = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                val avatarBitmap = fetchCircularBitmapBounded(avatarUrl)

                notificationManager.showChatNotification(
                    notificationId = notificationId,
                    senderName = senderName,
                    messageText = chatMessageText,
                    conversationId = conversationId,
                    peerId = peerId,
                    avatarBitmap = avatarBitmap,
                    messageId = data["message_id"],
                    isGroup = isGroup,
                    groupName = groupName,
                    timestampMillis = timestampMillis,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                )
            }

            "reaction", "message_reaction" -> {
                val conversationId = (data["conversation_id"] ?: data["chat_id"]).orEmpty().trim()
                if (NotificationRuntimeCoordinator.isConversationActivelyViewed(conversationId)) {
                    return
                }
                val peerId = (data["sender_id"] ?: data["peer_id"]).orEmpty()
                val senderName = data["sender_name"]?.takeIf(String::isNotBlank) ?: title
                val isGroup = data["conversation_type"].equals("group", ignoreCase = true)
                val groupName = data["group_name"]?.takeIf(String::isNotBlank)
                val reactionTitle = if (isGroup && !groupName.isNullOrBlank()) {
                    "$senderName در $groupName"
                } else {
                    senderName
                }
                val notificationId = VistaNotificationManager.stableNotificationId(
                    namespace = "chat_reaction",
                    rawKey = "$conversationId:${data["message_id"].orEmpty()}:$peerId",
                )
                val avatarBitmap = fetchCircularBitmapBounded(data["sender_avatar"])
                notificationManager.showSocialNotification(
                    notificationId = notificationId,
                    title = reactionTitle,
                    body = body,
                    userId = peerId,
                    conversationId = conversationId,
                    messageId = data["message_id"],
                    notificationType = "message_reaction",
                    deeplink = deeplink ?: conversationId.takeIf(String::isNotBlank)?.let { "vista://chat/$it" },
                    avatarBitmap = avatarBitmap,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                )
            }

            "post", "comment", "comment_reply", "like", "mention",
            "suggest_post", "post_admin_edit",
            -> {
                NotificationRuntimeCoordinator.notifySocialPushArrived(rawType)
                val postId = data["post_id"]
                val userId = data["user_id"] ?: data["sender_id"]
                val commentId = data["comment_id"]
                val key = data["notification_id"] ?: data["event_id"] ?: "$rawType:${postId.orEmpty()}:$body"
                val notificationId = VistaNotificationManager.stableNotificationId("social", key)
                val avatarBitmap = fetchCircularBitmapBounded(data["sender_avatar"])
                val postThumb = fetchBitmapBounded(data["image_url"], circular = false)

                notificationManager.showSocialNotification(
                    notificationId = notificationId,
                    title = title,
                    body = body,
                    postId = postId,
                    userId = userId,
                    commentId = commentId,
                    notificationType = rawType,
                    deeplink = deeplink,
                    avatarBitmap = avatarBitmap,
                    largeImageBitmap = postThumb,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                )
            }

            "story", "story_reply", "story_reaction" -> {
                NotificationRuntimeCoordinator.notifySocialPushArrived(rawType)
                val storyId = data["story_id"]
                val userId = data["user_id"] ?: data["sender_id"]
                val key = data["notification_id"] ?: data["event_id"] ?: "$rawType:${storyId.orEmpty()}:$body"
                val notificationId = VistaNotificationManager.stableNotificationId("social", key)
                val avatarBitmap = fetchCircularBitmapBounded(data["sender_avatar"])

                notificationManager.showSocialNotification(
                    notificationId = notificationId,
                    title = title,
                    body = body,
                    storyId = storyId,
                    userId = userId,
                    notificationType = rawType,
                    deeplink = deeplink,
                    avatarBitmap = avatarBitmap,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                )
            }

            "follow", "follow_request", "follow_request_accepted", "suggest_follow" -> {
                NotificationRuntimeCoordinator.notifySocialPushArrived(rawType)
                val followerId = data["follower_id"] ?: data["user_id"] ?: data["sender_id"]
                val key = data["notification_id"] ?: data["event_id"] ?: "$rawType:${followerId.orEmpty()}:$body"
                val notificationId = VistaNotificationManager.stableNotificationId("social", key)
                val avatarBitmap = fetchCircularBitmapBounded(data["sender_avatar"])

                notificationManager.showSocialNotification(
                    notificationId = notificationId,
                    title = title,
                    body = body,
                    userId = followerId,
                    followerId = followerId,
                    notificationType = rawType,
                    deeplink = deeplink,
                    avatarBitmap = avatarBitmap,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                )
            }

            else -> {
                NotificationRuntimeCoordinator.notifySocialPushArrived(rawType)
                val key = data["notification_id"] ?: data["event_id"] ?: "$rawType:$title:$body"
                val notificationId = VistaNotificationManager.stableNotificationId("social", key)
                val avatarBitmap = fetchCircularBitmapBounded(data["sender_avatar"])
                val largeThumb = fetchBitmapBounded(data["image_url"], circular = false)

                notificationManager.showSocialNotification(
                    notificationId = notificationId,
                    title = title,
                    body = body,
                    postId = data["post_id"],
                    storyId = data["story_id"],
                    userId = data["user_id"] ?: data["sender_id"],
                    followerId = data["follower_id"],
                    commentId = data["comment_id"],
                    conversationId = data["conversation_id"],
                    messageId = data["message_id"],
                    notificationType = rawType,
                    deeplink = deeplink,
                    avatarBitmap = avatarBitmap,
                    largeImageBitmap = largeThumb,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                )
            }
        }
    }

    private fun fetchCircularBitmapBounded(rawUrl: String?): Bitmap? =
        fetchBitmapBounded(rawUrl, circular = true)

    private fun fetchBitmapBounded(rawUrl: String?, circular: Boolean): Bitmap? {
        val url = rawUrl?.trim()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: return null
        val cacheKey = "${if (circular) "c" else "r"}:$url"
        avatarCache.get(cacheKey)?.let { return it }

        val fetched = runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(AVATAR_FETCH_TIMEOUT_MS) {
                runCatching {
                    val request = Request.Builder().url(url).get().build()
                    externalMediaClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        val bytes = response.body?.bytes() ?: return@use null
                        if (bytes.isEmpty() || bytes.size > MAX_IMAGE_BYTES) return@use null
                        val decoded = decodeSampledBitmap(bytes, targetPx = if (circular) 192 else 512)
                            ?: return@use null
                        if (circular) cropCircleBitmap(decoded) else decoded
                    }
                }.getOrNull()
            }
        }
        if (fetched != null) {
            avatarCache.put(cacheKey, fetched)
        }
        return fetched
    }

    private fun decodeSampledBitmap(bytes: ByteArray, targetPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > targetPx * 2 || bounds.outHeight / sampleSize > targetPx * 2) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun cropCircleBitmap(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height).coerceAtLeast(1)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val left = (source.width - size) / 2
        val top = (source.height - size) / 2
        val srcRect = Rect(left, top, left + size, top + size)
        val dstRect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(dstRect, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, srcRect, dstRect, paint)
        if (output !== source) source.recycle()
        return output
    }

    private companion object {
        const val AVATAR_FETCH_TIMEOUT_MS = 1_200L
        const val MAX_IMAGE_BYTES = 2 * 1024 * 1024
        val avatarCache = LruCache<String, Bitmap>(32)
    }
}
