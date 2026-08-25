package ir.coffevista.vista_native.core.datastore

import ir.coffevista.vista_native.core.datastore.proto.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPreferenceStoreTest {
    @Test
    fun currentSchemaMapsEveryPersistedSetting() {
        val stored = AppPreferences.newBuilder()
            .setSchemaVersion(CURRENT_SCHEMA_VERSION)
            .setAppLocale("en")
            .setThemeMode("dark")
            .setReduceMotion(true)
            .setChatEntryMode("minimal")
            .setEmojiStyle("system")
            .setAutoplayMedia(false)
            .setMediaQuality("data_saver")
            .setVideoDataSaver(true)
            .build()

        val actual = stored.toSettingsPreferences()

        assertEquals("en", actual.locale)
        assertEquals("dark", actual.themeMode)
        assertTrue(actual.reduceMotion)
        assertEquals("minimal", actual.chatEntryMode)
        assertEquals("system", actual.emojiStyle)
        assertFalse(actual.autoplayMedia)
        assertEquals("data_saver", actual.mediaQuality)
        assertTrue(actual.videoDataSaver)
    }

    @Test
    fun preMediaSchemaGetsSafeDefaultsInsteadOfUndefinedPlayback() {
        val old = AppPreferences.newBuilder()
            .setSchemaVersion(1)
            .setAppLocale("")
            .setThemeMode("")
            .setAutoplayMedia(false)
            .setMediaQuality("")
            .build()

        val actual = old.toSettingsPreferences()

        assertEquals("fa", actual.locale)
        assertEquals("system", actual.themeMode)
        assertEquals("adaptive", actual.chatEntryMode)
        assertEquals("custom", actual.emojiStyle)
        assertTrue(actual.autoplayMedia)
        assertEquals("high", actual.mediaQuality)
        assertFalse(actual.videoDataSaver)
    }

    @Test
    fun unknownAppearanceModesFailClosedToFlutterDefaults() {
        val stored = AppPreferences.newBuilder()
            .setSchemaVersion(CURRENT_SCHEMA_VERSION)
            .setChatEntryMode("fast")
            .setEmojiStyle("native")
            .build()

        val actual = stored.toSettingsPreferences()

        assertEquals("adaptive", actual.chatEntryMode)
        assertEquals("custom", actual.emojiStyle)
    }
}
