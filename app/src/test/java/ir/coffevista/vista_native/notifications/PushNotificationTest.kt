package ir.coffevista.vista_native.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PushNotificationTest {

    @Test
    fun `channel constants match required specifications`() {
        assertEquals("chat_messages", VistaNotificationManager.CHANNEL_CHAT_MESSAGES)
        assertEquals("chat_messages_vibrate", VistaNotificationManager.CHANNEL_CHAT_VIBRATE_ONLY)
        assertEquals("chat_messages_silent", VistaNotificationManager.CHANNEL_CHAT_SILENT)
        assertEquals("social_notify", VistaNotificationManager.CHANNEL_SOCIAL_NOTIFY)
        assertEquals("social_notify_silent", VistaNotificationManager.CHANNEL_SOCIAL_SILENT)
        assertEquals("post_upload_progress", VistaNotificationManager.CHANNEL_POST_UPLOAD)
    }

    @Test
    fun `stableNotificationId avoids negative hashCode collision onto ID 1`() {
        // Choose UUID-like strings whose JVM String.hashCode() is negative.
        val negativeHashKeys = listOf(
            "aaaaaaaa-1111-4111-8111-111111111111",
            "bbbbbbbb-2222-4222-8222-222222222222",
            "cccccccc-3333-4333-8333-333333333333",
        ).filter { it.hashCode() < 0 }
        org.junit.Assert.assertTrue(negativeHashKeys.isNotEmpty())

        val ids = negativeHashKeys.map { VistaNotificationManager.stableNotificationId("chat", it) }
        org.junit.Assert.assertTrue(ids.all { it >= 10_000 })
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(
            VistaNotificationManager.stableNotificationId("chat", negativeHashKeys.first()),
            ids.first(),
        )
    }

    @Test
    fun `notification types match deep link intents`() {
        assertEquals("chat", VistaNotificationManager.TYPE_CHAT)
        assertEquals("post", VistaNotificationManager.TYPE_POST)
        assertEquals("story", VistaNotificationManager.TYPE_STORY)
        assertEquals("social", VistaNotificationManager.TYPE_SOCIAL)
    }

    @Test
    fun `fcm registration dto serialization matches server contract`() {
        val dto = FcmTokenRegistrationDto(
            token = "sample_fcm_token_12345",
            platform = "android",
            deviceType = "mobile",
        )
        assertEquals("sample_fcm_token_12345", dto.token)
        assertEquals("android", dto.platform)
        assertEquals("mobile", dto.deviceType)
    }
}
