package ir.coffevista.vista_native.core.datastore

import ir.coffevista.vista_native.core.common.ConnectionTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaDataPolicyTest {
    @Test
    fun `uploadPreset scales dimensions and quality downward from high to data_saver`() {
        val high = MediaDataPolicy.uploadPreset("high")
        val standard = MediaDataPolicy.uploadPreset("standard")
        val saver = MediaDataPolicy.uploadPreset("data_saver")

        assertTrue(high.maxDimension > standard.maxDimension)
        assertTrue(standard.maxDimension > saver.maxDimension)
        assertTrue(high.jpegQuality > standard.jpegQuality)
        assertTrue(standard.jpegQuality > saver.jpegQuality)
        assertEquals(800, saver.maxDimension)
        assertEquals(60, saver.jpegQuality)
    }

    @Test
    fun `keepMediaDurationMillis matches period keys and returns null for forever`() {
        assertEquals(3L * 24 * 60 * 60 * 1000, MediaDataPolicy.keepMediaDurationMillis("3_days"))
        assertEquals(7L * 24 * 60 * 60 * 1000, MediaDataPolicy.keepMediaDurationMillis("1_week"))
        assertEquals(30L * 24 * 60 * 60 * 1000, MediaDataPolicy.keepMediaDurationMillis("1_month"))
        assertNull(MediaDataPolicy.keepMediaDurationMillis("forever"))
    }

    @Test
    fun `shouldAutoDownloadAttachment respects wifi, cellular, roaming, and offline rules`() {
        val prefs = SettingsPreferences(
            autoDownloadWifi = true,
            autoDownloadCellular = true,
            autoDownloadRoaming = false,
        )

        assertFalse(MediaDataPolicy.shouldAutoDownloadAttachment(ConnectionTransport.OFFLINE, true, 1024, prefs))
        assertTrue(MediaDataPolicy.shouldAutoDownloadAttachment(ConnectionTransport.WIFI, false, 50L * 1024 * 1024, prefs))
        assertTrue(MediaDataPolicy.shouldAutoDownloadAttachment(ConnectionTransport.CELLULAR, true, 5L * 1024 * 1024, prefs))
        assertFalse(MediaDataPolicy.shouldAutoDownloadAttachment(ConnectionTransport.CELLULAR, false, 10L * 1024 * 1024, prefs))
        assertTrue(MediaDataPolicy.shouldAutoDownloadAttachment(ConnectionTransport.CELLULAR, false, 1L * 1024 * 1024, prefs))
        assertFalse(MediaDataPolicy.shouldAutoDownloadAttachment(ConnectionTransport.ROAMING, true, 1024, prefs))

        val roamingEnabled = prefs.copy(autoDownloadRoaming = true)
        assertTrue(MediaDataPolicy.shouldAutoDownloadAttachment(ConnectionTransport.ROAMING, true, 1024, roamingEnabled))
        assertFalse(MediaDataPolicy.shouldAutoDownloadAttachment(ConnectionTransport.ROAMING, false, 1024, roamingEnabled))

        val wifiDisabled = prefs.copy(autoDownloadWifi = false)
        assertFalse(MediaDataPolicy.shouldAutoDownloadAttachment(ConnectionTransport.WIFI, true, 1024, wifiDisabled))
    }
}
