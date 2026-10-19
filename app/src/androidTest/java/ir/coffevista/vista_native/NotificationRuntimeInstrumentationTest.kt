package ir.coffevista.vista_native

import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.coffevista.vista_native.notifications.VistaNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationRuntimeInstrumentationTest {

    private lateinit var context: Context
    private lateinit var systemNotificationManager: NotificationManager
    private lateinit var vistaNotificationManager: VistaNotificationManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        systemNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        vistaNotificationManager = VistaNotificationManager(context)
    }

    @Test
    fun notificationChannelsAreCreatedWithProperImportanceAndProperties() {
        vistaNotificationManager.createNotificationChannels()

        val chatChannel = systemNotificationManager.getNotificationChannel(VistaNotificationManager.CHANNEL_CHAT_MESSAGES)
        assertNotNull("Chat channel must exist", chatChannel)
        assertEquals("پیام‌های چت", chatChannel.name)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, chatChannel.importance)
        assertTrue(chatChannel.canShowBadge())

        val socialChannel = systemNotificationManager.getNotificationChannel(VistaNotificationManager.CHANNEL_SOCIAL_NOTIFY)
        assertNotNull("Social channel must exist", socialChannel)
        assertEquals("اعلان‌های شبکه اجتماعی", socialChannel.name)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, socialChannel.importance)
        assertTrue(socialChannel.canShowBadge())

        val uploadChannel = systemNotificationManager.getNotificationChannel(VistaNotificationManager.CHANNEL_POST_UPLOAD)
        assertNotNull("Upload channel must exist", uploadChannel)
        assertEquals("آپلود پست و استوری", uploadChannel.name)
        assertEquals(NotificationManager.IMPORTANCE_LOW, uploadChannel.importance)
    }

    @Test
    fun showChatNotificationPostsActiveNotificationToSystemTray() {
        vistaNotificationManager.createNotificationChannels()
        val notifId = 8801

        vistaNotificationManager.showChatNotification(
            notificationId = notifId,
            senderName = "علی رضایی",
            messageText = "سلام! کجایی؟",
            conversationId = "conv-runtime-test-1",
            peerId = "peer-runtime-test-1",
        )

        val active = (0..20).firstNotNullOfOrNull {
            Thread.sleep(50)
            systemNotificationManager.activeNotifications.firstOrNull { it.id == notifId }
        }
        assertNotNull("Active chat notification must be present in system tray", active)
        assertEquals(VistaNotificationManager.CHANNEL_CHAT_MESSAGES, active!!.notification.channelId)
        val extras = active.notification.extras
        assertEquals("علی رضایی", extras.getString("android.title"))
        assertEquals("سلام! کجایی؟", extras.getCharSequence("android.text")?.toString())

        vistaNotificationManager.cancelNotification(notifId)
        val cancelled = (0..20).any {
            Thread.sleep(50)
            systemNotificationManager.activeNotifications.none { it.id == notifId }
        }
        assertTrue("Notification should be cancelled", cancelled)
    }

    @Test
    fun showSocialNotificationPostsActiveNotificationToSystemTray() {
        vistaNotificationManager.createNotificationChannels()
        val notifId = 8802

        vistaNotificationManager.showSocialNotification(
            notificationId = notifId,
            title = "اعلان جدید",
            body = "پست شما توسط مریم پسندیده شد",
            postId = "post-runtime-test-2",
        )

        val active = (0..20).firstNotNullOfOrNull {
            Thread.sleep(50)
            systemNotificationManager.activeNotifications.firstOrNull { it.id == notifId }
        }
        assertNotNull("Active social notification must be present in system tray", active)
        assertEquals(VistaNotificationManager.CHANNEL_SOCIAL_NOTIFY, active!!.notification.channelId)
        val extras = active.notification.extras
        assertEquals("اعلان جدید", extras.getString("android.title"))
        assertEquals("پست شما توسط مریم پسندیده شد", extras.getCharSequence("android.text")?.toString())

        vistaNotificationManager.cancelAll()
        val cleared = (0..20).any {
            Thread.sleep(50)
            systemNotificationManager.activeNotifications.none { it.id == notifId }
        }
        assertTrue("Notification should be cleared by cancelAll", cleared)
    }
}
