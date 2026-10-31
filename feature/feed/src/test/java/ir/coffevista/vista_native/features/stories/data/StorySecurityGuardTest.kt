package ir.coffevista.vista_native.features.stories.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StorySecurityGuardTest {

    @Test
    fun `isValidUuid recognizes valid UUID v4 and rejects arbitrary strings`() {
        assertTrue(StorySecurityGuard.isValidUuid("c81d4e2e-bcf2-11e6-869b-7df92533d2db"))
        assertTrue(StorySecurityGuard.isValidUuid("00000000-0000-0000-0000-000000000000"))
        assertFalse(StorySecurityGuard.isValidUuid("not-a-uuid"))
        assertFalse(StorySecurityGuard.isValidUuid("@john_doe"))
        assertFalse(StorySecurityGuard.isValidUuid(""))
        assertFalse(StorySecurityGuard.isValidUuid(null))
    }

    @Test
    fun `normalizeSafeHttpUrl validates and normalizes safe URLs`() {
        assertEquals("https://coffevista.ir", StorySecurityGuard.normalizeSafeHttpUrl("coffevista.ir"))
        assertEquals("https://coffevista.ir/path?q=1", StorySecurityGuard.normalizeSafeHttpUrl("https://coffevista.ir/path?q=1"))
        assertEquals("http://example.com/test", StorySecurityGuard.normalizeSafeHttpUrl("http://example.com/test"))
        
        // Schemes that must be blocked
        assertNull(StorySecurityGuard.normalizeSafeHttpUrl("javascript:alert(1)"))
        assertNull(StorySecurityGuard.normalizeSafeHttpUrl("data:text/html,<script>"))
        assertNull(StorySecurityGuard.normalizeSafeHttpUrl("file:///etc/passwd"))
        assertNull(StorySecurityGuard.normalizeSafeHttpUrl("intent:#Intent;scheme=bad;end"))
        assertNull(StorySecurityGuard.normalizeSafeHttpUrl("content://media/external/images"))
        assertNull(StorySecurityGuard.normalizeSafeHttpUrl("blob:https://example.com"))

        // Malicious CRLF / whitespace injection
        assertNull(StorySecurityGuard.normalizeSafeHttpUrl("https://example.com\r\nSet-Cookie:bad"))
        assertNull(StorySecurityGuard.normalizeSafeHttpUrl("   "))
        assertNull(StorySecurityGuard.normalizeSafeHttpUrl(null))
    }

    @Test
    fun `extractDisplayHost correctly strips www and returns clean host`() {
        assertEquals("coffevista.ir", StorySecurityGuard.extractDisplayHost("https://www.coffevista.ir/post/123"))
        assertEquals("t.me", StorySecurityGuard.extractDisplayHost("https://t.me/channel"))
        assertEquals("github.com", StorySecurityGuard.extractDisplayHost("github.com/DrKLO/Telegram"))
        assertEquals("", StorySecurityGuard.extractDisplayHost("javascript:void(0)"))
    }

    @Test
    fun `mapReactionEmoji converts string keywords to authentic unicode emojis`() {
        assertEquals("❤️", StorySecurityGuard.mapReactionEmoji("heart"))
        assertEquals("❤️", StorySecurityGuard.mapReactionEmoji("like"))
        assertEquals("😂", StorySecurityGuard.mapReactionEmoji("laugh"))
        assertEquals("😮", StorySecurityGuard.mapReactionEmoji("wow"))
        assertEquals("😢", StorySecurityGuard.mapReactionEmoji("sad"))
        assertEquals("🔥", StorySecurityGuard.mapReactionEmoji("fire"))
        assertEquals("💯", StorySecurityGuard.mapReactionEmoji("100"))
        assertEquals("🎉", StorySecurityGuard.mapReactionEmoji("🎉"))

        assertNull(StorySecurityGuard.mapReactionEmoji("unlike"))
        assertNull(StorySecurityGuard.mapReactionEmoji("none"))
        assertNull(StorySecurityGuard.mapReactionEmoji(""))
        assertNull(StorySecurityGuard.mapReactionEmoji(null))
    }

    @Test
    fun `isSafeCanonicalPath blocks path traversal into private app data dir while allowing cache`() {
        val appData = "/data/user/0/ir.coffevista.vista_native"
        val cacheAllowed = listOf(
            "/data/user/0/ir.coffevista.vista_native/cache/camera",
            "/data/user/0/ir.coffevista.vista_native/cache/incoming_share",
        )

        // Attempt to access databases or shared_prefs
        assertFalse(
            StorySecurityGuard.isSafeCanonicalPath(
                "/data/user/0/ir.coffevista.vista_native/databases/app.db",
                appData,
                cacheAllowed,
            ),
        )
        assertFalse(
            StorySecurityGuard.isSafeCanonicalPath(
                "/data/user/0/ir.coffevista.vista_native/shared_prefs/session.xml",
                appData,
                cacheAllowed,
            ),
        )
        assertFalse(
            StorySecurityGuard.isSafeCanonicalPath(
                "/proc/self/cmdline",
                appData,
                cacheAllowed,
            ),
        )

        // Allowed camera export or incoming share cache files
        assertTrue(
            StorySecurityGuard.isSafeCanonicalPath(
                "/data/user/0/ir.coffevista.vista_native/cache/camera/story_export_123.jpg",
                appData,
                cacheAllowed,
            ),
        )
        assertTrue(
            StorySecurityGuard.isSafeCanonicalPath(
                "/data/user/0/ir.coffevista.vista_native/cache/incoming_share/shared_video.mp4",
                appData,
                cacheAllowed,
            ),
        )

        // External storage public paths
        assertTrue(
            StorySecurityGuard.isSafeCanonicalPath(
                "/storage/emulated/0/DCIM/Camera/VID_2026.mp4",
                appData,
                cacheAllowed,
            ),
        )
    }
}
