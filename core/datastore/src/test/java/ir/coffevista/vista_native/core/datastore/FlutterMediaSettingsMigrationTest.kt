package ir.coffevista.vista_native.core.datastore

import ir.coffevista.vista_native.core.datastore.proto.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlutterMediaSettingsMigrationTest {
    @Test
    fun importsExplicitFlutterChoicesAndMarksMigration() {
        val current = AppPreferences.newBuilder()
            .setSchemaVersion(CURRENT_SCHEMA_VERSION)
            .setMediaQuality("high")
            .setAutoplayMedia(true)
            .build()

        val migrated = migrateFlutterMediaSettings(
            current,
            mapOf(
                "flutter.onboarding_completed" to true,
                "flutter.onboarding_version" to "1.0.0",
                "flutter.data_upload_quality" to "data_saver",
                "flutter.video_auto_play" to false,
                "flutter.video_data_saver" to true,
            ),
        )

        assertTrue(migrated.onboardingCompleted)
        assertEquals("1.0.0", migrated.onboardingVersion)
        assertEquals("data_saver", migrated.mediaQuality)
        assertFalse(migrated.autoplayMedia)
        assertTrue(migrated.videoDataSaver)
        assertTrue(migrated.flutterMediaSettingsMigrated)
        assertFalse(shouldImportFlutterMediaSettings(false, migrated))
    }

    @Test
    fun invalidAndAbsentValuesLeaveNativeDefaultsUntouched() {
        val current = AppPreferences.newBuilder()
            .setMediaQuality("standard")
            .setAutoplayMedia(true)
            .setVideoDataSaver(false)
            .build()

        val migrated = migrateFlutterMediaSettings(
            current,
            mapOf(
                "flutter.data_upload_quality" to "ultra",
                "flutter.video_auto_play" to "false",
            ),
        )

        assertEquals("standard", migrated.mediaQuality)
        assertTrue(migrated.autoplayMedia)
        assertFalse(migrated.videoDataSaver)
        assertTrue(migrated.flutterMediaSettingsMigrated)
    }

    @Test
    fun existingNativeDataStoreTakesPrecedenceOverFlutterSnapshot() {
        val current = AppPreferences.newBuilder().build()

        assertFalse(shouldImportFlutterMediaSettings(true, current))
        assertTrue(shouldImportFlutterMediaSettings(false, current))
    }
}
