package ir.coffevista.vista_native.features.chat.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCachePathTest {
    @Test
    fun `absolute recorder path is retained for cleanup`() {
        assertEquals(
            "/data/user/0/ir.coffevista.vista_native.production/cache/chat-voice/voice.m4a",
            "/data/user/0/ir.coffevista.vista_native.production/cache/chat-voice/voice.m4a"
                .toLocalVoiceCachePath(),
        )
    }

    @Test
    fun `file uri recorder path is retained for cleanup`() {
        assertEquals(
            "/data/user/0/ir.coffevista.vista_native.production/cache/chat-voice/voice.m4a",
            "file:///data/user/0/ir.coffevista.vista_native.production/cache/chat-voice/voice.m4a"
                .toLocalVoiceCachePath(),
        )
    }

    @Test
    fun `remote media url is never treated as a local cache file`() {
        assertNull("https://cdn.example.test/chat/voice.m4a".toLocalVoiceCachePath())
    }
}
