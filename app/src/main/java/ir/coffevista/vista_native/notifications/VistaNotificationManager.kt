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
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.MainActivity
import ir.coffevista.vista_native.R
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
        const val CHANNEL_SOCIAL_NOTIFY = "social_notify"
        const val CHANNEL_POST_UPLOAD = "post_upload_progress"

        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_PEER_ID = "peer_id"
        const val EXTRA_POST_ID = "post_id"
        const val EXTRA_STORY_ID = "story_id"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_COMMENT_ID = "comment_id"

        const val TYPE_CHAT = "chat"
        const val TYPE_POST = "post"
        const val TYPE_STORY = "story"
        const val TYPE_SOCIAL = "social"
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

            // 1. Chat Messages Channel
            val chatChannel = NotificationChannel(
                CHANNEL_CHAT_MESSAGES,
                "پیام‌های چت",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "اعلان دریافت پیام‌های جدید در گفتگوها"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(defaultSoundUri, audioAttributes)
                setShowBadge(true)
            }

            // 2. Social Notifications Channel
            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL_NOTIFY,
                "اعلان‌های شبکه اجتماعی",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "لایک‌ها، نظرات، دنبال‌کننده‌ها و استوری‌ها"
                enableLights(true)
                lightColor = Color.MAGENTA
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Upload Progress Channel
            val uploadChannel = NotificationChannel(
                CHANNEL_POST_UPLOAD,
                "آپلود پست و استوری",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "پیشرفت آپلود فایل‌های مدیا در پس‌زمینه"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(chatChannel, socialChannel, uploadChannel))
        }
    }

    fun showChatNotification(
        notificationId: Int,
        senderName: String,
        messageText: String,
        conversationId: String,
        peerId: String,
        avatarBitmap: Bitmap? = null,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_CHAT)
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
            putExtra(EXTRA_PEER_ID, peerId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CHAT_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (avatarBitmap != null) {
            builder.setLargeIcon(avatarBitmap)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
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
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (postId != null) {
                putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_POST)
                putExtra(EXTRA_POST_ID, postId)
            } else if (storyId != null) {
                putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_STORY)
                putExtra(EXTRA_STORY_ID, storyId)
            } else {
                putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_SOCIAL)
            }
            userId?.let { putExtra(EXTRA_USER_ID, it) }
            commentId?.let { putExtra(EXTRA_COMMENT_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SOCIAL_NOTIFY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }
}
