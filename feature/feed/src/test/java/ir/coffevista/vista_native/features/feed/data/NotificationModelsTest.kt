package ir.coffevista.vista_native.features.feed.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationModelsTest {

    @Test
    fun canonicalNotificationType_mapsAdminModerationTypes() {
        assertEquals("post_removed", canonicalNotificationType("post_admin_removed"))
        assertEquals("post_edited", canonicalNotificationType("post_admin_edit"))
        assertEquals("like", canonicalNotificationType("post_like"))
        assertEquals("comment", canonicalNotificationType("new_comment"))
        assertEquals("mention", canonicalNotificationType("comment_mention"))
    }

    @Test
    fun normalizeNotificationContent_translatesEnglishMentionsAndModeration() {
        assertEquals("شما را در یک نظر تگ کرد", normalizeNotificationContent("mentioned you in a comment", "mention"))
        assertEquals("شما را در یک پست تگ کرد", normalizeNotificationContent("mentioned you in a post", "mention"))
        assertEquals("پست شما را لایک کرد", normalizeNotificationContent("liked your post", "like"))
        assertEquals("پست شما به دلیل نقض قوانین حذف شد", normalizeNotificationContent("", "post_admin_removed"))
        assertEquals("پست شما توسط مدیر ویرایش شد", normalizeNotificationContent("", "post_admin_edit"))
    }

    @Test
    fun normalizeNotificationContent_blocksRawAndTruncatedJson() {
        assertEquals(
            "به استوری شما پاسخ داد",
            normalizeNotificationContent(
                """{"story_id":"3bb19d76-32d6-4044-a6ff-f3ecd4d08125","story_owner_id":"57fffc28-744...""",
                "story_reply",
            ),
        )
        assertEquals(
            "به استوری شما پاسخ داد: خیلی عالی بود",
            normalizeNotificationContent(
                """{"story_id":"1","reply_text":"خیلی عالی بود"}""",
                "story_reply",
            ),
        )
        assertEquals(
            "اعلان جدید",
            normalizeNotificationContent(
                """{"unknown_payload":true,"nested":{"x":1}}""",
                "unknown_type",
            ),
        )
    }
}
