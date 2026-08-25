package ir.coffevista.vista_native.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PushNotificationTest {

    @Test
    fun `channel constants match required specifications`() {
        assertEquals("chat_messages", VistaNotificationManager.CHANNEL_CHAT_MESSAGES)
        assertEquals("social_notify", VistaNotificationManager.CHANNEL_SOCIAL_NOTIFY)
        assertEquals("post_upload_progress", VistaNotificationManager.CHANNEL_POST_UPLOAD)
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
