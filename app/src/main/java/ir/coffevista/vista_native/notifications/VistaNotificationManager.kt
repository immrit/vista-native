package ir.coffevista.vista_native.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista.MainActivity
import ir.coffevista.vista_native.R
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VistaNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_CHAT_MESSAGES = "chat_messages"
        const val CHANNEL_CHAT_VIBRATE_ONLY = "chat_messages_vibrate"
        const val CHANNEL_CHAT_SILENT = "chat_messages_silent"
        const val CHANNEL_SOCIAL_NOTIFY = "social_notify"
        const val CHANNEL_SOCIAL_SILENT = "social_notify_silent"
        const val CHANNEL_POST_UPLOAD = "post_upload_progress"

        const val GROUP_KEY_CHAT = "ir.coffevista.vista.GROUP_CHAT"
        const val GROUP_KEY_SOCIAL = "ir.coffevista.vista.GROUP_SOCIAL"
        const val SUMMARY_ID_CHAT = 9001
        const val SUMMARY_ID_SOCIAL = 9002

        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_PEER_ID = "peer_id"
        const val EXTRA_POST_ID = "post_id"
        const val EXTRA_STORY_ID = "story_id"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_FOLLOWER_ID = "follower_id"
        const val EXTRA_COMMENT_ID = "comment_id"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_DEEPLINK = "deeplink"

        const val TYPE_CHAT = "chat"
        const val TYPE_POST = "post"
        const val TYPE_STORY = "story"
        const val TYPE_SOCIAL = "social"

        private const val MAX_CONVERSATION_LINES = 8
        private const val SUMMARY_REQUEST_CODE_CHAT = 9901
        private const val SUMMARY_REQUEST_CODE_SOCIAL = 9902

        /**
         * Derives a deterministic, strictly positive notification ID outside
         * reserved worker/summary ID ranges (1..9999) so negative String.hashCode()
         * values never collapse onto ID 1 and overwrite unrelated notifications.
         */
        fun stableNotificationId(namespace: String, rawKey: String): Int {
            val crc = CRC32()
            crc.update("$namespace:$rawKey".toByteArray(Charsets.UTF_8))
            return ((crc.value and 0x7FFFFFFFL) % 1_900_000_000L).toInt() + 10_000
        }
    }

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()
            val socialAudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val chatChannel = NotificationChannel(
                CHANNEL_CHAT_MESSAGES,
                "پیام‌های چت",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "اعلان دریافت پیام‌های جدید در گفتگوها"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 220, 120, 220)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
            }

            val chatVibrateOnlyChannel = NotificationChannel(
                CHANNEL_CHAT_VIBRATE_ONLY,
                "پیام‌های چت (فقط لرزش)",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "اعلان پیام‌های جدید بدون صدا و همراه با لرزش"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 220, 120, 220)
                setSound(null, null)
                setShowBadge(true)
            }

            val chatSilentChannel = NotificationChannel(
                CHANNEL_CHAT_SILENT,
                "پیام‌های چت (بی‌صدا)",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "اعلان پیام‌های جدید بدون صدا و بدون لرزش"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(true)
            }

            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL_NOTIFY,
                "اعلان‌های شبکه اجتماعی",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "لایک‌ها، نظرات، دنبال‌کننده‌ها و استوری‌ها"
                enableLights(true)
                lightColor = Color.MAGENTA
                enableVibration(true)
                setSound(defaultSoundUri, socialAudioAttributes)
                setShowBadge(true)
            }

            val socialSilentChannel = NotificationChannel(
                CHANNEL_SOCIAL_SILENT,
                "اعلان‌های شبکه اجتماعی (بی‌صدا)",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "اعلان‌های شبکه اجتماعی بدون صدا"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(true)
            }

            val uploadChannel = NotificationChannel(
                CHANNEL_POST_UPLOAD,
                "آپلود پست و استوری",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "پیشرفت آپلود فایل‌های مدیا در پس‌زمینه"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(
                    chatChannel,
                    chatVibrateOnlyChannel,
                    chatSilentChannel,
                    socialChannel,
                    socialSilentChannel,
                    uploadChannel,
                ),
            )
        }
    }

    fun showChatNotification(
        notificationId: Int,
        senderName: String,
        messageText: String,
        conversationId: String,
        peerId: String,
        avatarBitmap: Bitmap? = null,
        messageId: String? = null,
        isGroup: Boolean = false,
        groupName: String? = null,
        timestampMillis: Long = System.currentTimeMillis(),
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_CHAT)
            putExtra("type", TYPE_CHAT)
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
            putExtra("conversation_id", conversationId)
            putExtra(EXTRA_PEER_ID, peerId)
            putExtra("peer_id", peerId)
            messageId?.takeIf(String::isNotBlank)?.let {
                putExtra(EXTRA_MESSAGE_ID, it)
                putExtra("message_id", it)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = resolveChatChannel(soundEnabled, vibrationEnabled)
        val priority = if (soundEnabled || vibrationEnabled) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_LOW
        }
        val accentColor = ContextCompat.getColor(context, R.color.vista_notification_accent)

        val senderPersonBuilder = Person.Builder()
            .setName(senderName.ifBlank { "کاربر" })
            .setKey(peerId.ifBlank { senderName })
        if (avatarBitmap != null) {
            senderPersonBuilder.setIcon(IconCompat.createWithBitmap(avatarBitmap))
        }
        val senderPerson = senderPersonBuilder.build()
        val mePerson = Person.Builder().setName("شما").setKey("me").build()

        // Recover existing unread messages in this conversation's active tray
        // notification so consecutive messages stack cleanly even across process kills.
        val existingStyle = findExistingMessagingStyle(notificationId)
        val messagingStyle: NotificationCompat.MessagingStyle = NotificationCompat.MessagingStyle(mePerson)
        if (isGroup && !groupName.isNullOrBlank()) {
            messagingStyle.setGroupConversation(true)
            messagingStyle.setConversationTitle(groupName)
        } else {
            messagingStyle.setGroupConversation(false)
        }
        val priorMessages = existingStyle?.messages.orEmpty()
            .takeLast(MAX_CONVERSATION_LINES - 1)
        for (msg in priorMessages) {
            messagingStyle.addMessage(msg)
        }
        messagingStyle.addMessage(messageText, timestampMillis, senderPerson)

        val displayTitle = if (isGroup && !groupName.isNullOrBlank()) {
            groupName
        } else {
            senderName.ifBlank { "کاربر" }
        }
        val displaySummary = if (isGroup && !groupName.isNullOrBlank() && senderName.isNotBlank()) {
            "$senderName: $messageText"
        } else {
            messageText
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(DesignSystemR.drawable.ic_notification)
            .setColor(accentColor)
            .setContentTitle(displayTitle)
            .setContentText(displaySummary)
            .setStyle(messagingStyle)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_KEY_CHAT)
            .setWhen(timestampMillis)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (!soundEnabled && !vibrationEnabled) {
            builder.setSilent(true)
        }
        if (avatarBitmap != null) {
            builder.setLargeIcon(avatarBitmap)
        }

        if (conversationId.isNotBlank()) {
            val replyRemoteInput = RemoteInput.Builder(ChatNotificationActionReceiver.KEY_TEXT_REPLY)
                .setLabel("پاسخ سریع...")
                .build()
            val replyIntent = Intent(context, ChatNotificationActionReceiver::class.java).apply {
                action = ChatNotificationActionReceiver.ACTION_REPLY
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(ChatNotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val replyPendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId xor 0x1001,
                replyIntent,
                replyPendingFlags,
            )
            val replyAction = NotificationCompat.Action.Builder(
                DesignSystemR.drawable.ic_notification,
                "پاسخ",
                replyPendingIntent,
            )
                .addRemoteInput(replyRemoteInput)
                .setAllowGeneratedReplies(true)
                .build()

            val markReadIntent = Intent(context, ChatNotificationActionReceiver::class.java).apply {
                action = ChatNotificationActionReceiver.ACTION_MARK_READ
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(ChatNotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val markReadPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId xor 0x2002,
                markReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val markReadAction = NotificationCompat.Action.Builder(
                DesignSystemR.drawable.ic_notification,
                "خوانده شد",
                markReadPendingIntent,
            ).build()

            builder.addAction(replyAction)
            builder.addAction(markReadAction)
        }

        try {
            val compat = NotificationManagerCompat.from(context)
            compat.notify(notificationId, builder.build())
            updateGroupSummaryIfNeeded(
                groupKey = GROUP_KEY_CHAT,
                summaryId = SUMMARY_ID_CHAT,
                channelId = channelId,
                title = "پیام‌های جدید ویستا",
                requestCode = SUMMARY_REQUEST_CODE_CHAT,
            )
        } catch (_: SecurityException) {
            // Permission not granted on Android 13+
        }
    }

    fun showSocialNotification(
        notificationId: Int,
        title: String,
        body: String,
        postId: String? = null,
        storyId: String? = null,
        userId: String? = null,
        commentId: String? = null,
        followerId: String? = null,
        conversationId: String? = null,
        messageId: String? = null,
        notificationType: String? = null,
        deeplink: String? = null,
        avatarBitmap: Bitmap? = null,
        largeImageBitmap: Bitmap? = null,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
    ) {
        val resolvedType = notificationType?.takeIf(String::isNotBlank) ?: when {
            postId != null -> TYPE_POST
            storyId != null -> TYPE_STORY
            else -> TYPE_SOCIAL
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, resolvedType)
            putExtra("type", resolvedType)
            postId?.takeIf(String::isNotBlank)?.let {
                putExtra(EXTRA_POST_ID, it)
                putExtra("post_id", it)
            }
            storyId?.takeIf(String::isNotBlank)?.let {
                putExtra(EXTRA_STORY_ID, it)
                putExtra("story_id", it)
            }
            userId?.takeIf(String::isNotBlank)?.let {
                putExtra(EXTRA_USER_ID, it)
                putExtra("user_id", it)
            }
            followerId?.takeIf(String::isNotBlank)?.let {
                putExtra(EXTRA_FOLLOWER_ID, it)
                putExtra("follower_id", it)
            }
            commentId?.takeIf(String::isNotBlank)?.let {
                putExtra(EXTRA_COMMENT_ID, it)
                putExtra("comment_id", it)
            }
            conversationId?.takeIf(String::isNotBlank)?.let {
                putExtra(EXTRA_CONVERSATION_ID, it)
                putExtra("conversation_id", it)
            }
            messageId?.takeIf(String::isNotBlank)?.let {
                putExtra(EXTRA_MESSAGE_ID, it)
                putExtra("message_id", it)
            }
            deeplink?.takeIf(String::isNotBlank)?.let {
                putExtra(EXTRA_DEEPLINK, it)
                putExtra("deeplink", it)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = if (soundEnabled) CHANNEL_SOCIAL_NOTIFY else CHANNEL_SOCIAL_SILENT
        val accentColor = ContextCompat.getColor(context, R.color.vista_notification_accent)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(DesignSystemR.drawable.ic_notification)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW)
            .setGroup(GROUP_KEY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (!soundEnabled && !vibrationEnabled) {
            builder.setSilent(true)
        }
        if (avatarBitmap != null) {
            builder.setLargeIcon(avatarBitmap)
        }
        if (largeImageBitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(largeImageBitmap)
                    .bigLargeIcon(avatarBitmap),
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            updateGroupSummaryIfNeeded(
                groupKey = GROUP_KEY_SOCIAL,
                summaryId = SUMMARY_ID_SOCIAL,
                channelId = channelId,
                title = "اعلان‌های ویستا",
                requestCode = SUMMARY_REQUEST_CODE_SOCIAL,
            )
        } catch (_: SecurityException) {
        }
    }

    fun cancelChatNotification(conversationId: String) {
        if (conversationId.isBlank()) return
        val id = stableNotificationId("chat", conversationId)
        cancelNotification(id)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        reconcileGroupSummaries()
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }

    private fun resolveChatChannel(soundEnabled: Boolean, vibrationEnabled: Boolean): String = when {
        soundEnabled -> CHANNEL_CHAT_MESSAGES
        vibrationEnabled -> CHANNEL_CHAT_VIBRATE_ONLY
        else -> CHANNEL_CHAT_SILENT
    }

    private fun findExistingMessagingStyle(notificationId: Int): NotificationCompat.MessagingStyle? {
        val active = runCatching {
            notificationManager.activeNotifications.firstOrNull { it.id == notificationId }
        }.getOrNull() ?: return null
        return NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(active.notification)
    }

    private fun updateGroupSummaryIfNeeded(
        groupKey: String,
        summaryId: Int,
        channelId: String,
        title: String,
        requestCode: Int,
    ) {
        val activeChildren = runCatching {
            notificationManager.activeNotifications.count {
                it.notification.group == groupKey && it.id != summaryId
            }
        }.getOrDefault(0)
        if (activeChildren < 2) {
            notificationManager.cancel(summaryId)
            return
        }
        val summaryIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (groupKey == GROUP_KEY_SOCIAL) {
                putExtra(EXTRA_DEEPLINK, "vista://notifications")
                putExtra("deeplink", "vista://notifications")
            }
        }
        val pending = PendingIntent.getActivity(
            context,
            requestCode,
            summaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val summary = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(DesignSystemR.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.vista_notification_accent))
            .setContentTitle(title)
            .setContentText("$activeChildren اعلان خوانده‌نشده")
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setSilent(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(summaryId, summary)
        } catch (_: SecurityException) {
        }
    }

    private fun reconcileGroupSummaries() {
        val active = runCatching { notificationManager.activeNotifications }.getOrNull() ?: return
        val chatCount = active.count { it.notification.group == GROUP_KEY_CHAT && it.id != SUMMARY_ID_CHAT }
        if (chatCount < 2) notificationManager.cancel(SUMMARY_ID_CHAT)
        val socialCount = active.count { it.notification.group == GROUP_KEY_SOCIAL && it.id != SUMMARY_ID_SOCIAL }
        if (socialCount < 2) notificationManager.cancel(SUMMARY_ID_SOCIAL)
    }
}
