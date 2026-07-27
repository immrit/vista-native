package ir.coffevista.vista_native.core.datastore

import androidx.datastore.core.CorruptionException
import ir.coffevista.vista_native.core.datastore.proto.AppPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class AppPreferencesSerializerTest {
    @Test
    fun defaultIsVersionedAndSignedOutSafe() {
        val default = AppPreferencesSerializer.defaultValue

        assertEquals(CURRENT_SCHEMA_VERSION, default.schemaVersion)
        assertFalse(default.onboardingCompleted)
        assertEquals("", default.onboardingVersion)
    }

    @Test
    fun serializerRoundTripsVersionedCompletion() = runTest {
        val expected = AppPreferences.newBuilder()
            .setSchemaVersion(CURRENT_SCHEMA_VERSION)
            .setOnboardingCompleted(true)
            .setOnboardingVersion(CURRENT_ONBOARDING_VERSION)
            .build()
        val output = ByteArrayOutputStream()

        AppPreferencesSerializer.writeTo(expected, output)
        val actual = AppPreferencesSerializer.readFrom(
            ByteArrayInputStream(output.toByteArray()),
        )

        assertEquals(expected, actual)
    }

    @Test
    fun malformedProtoRaisesCorruptionInsteadOfReturningPartialState() = runTest {
        var thrown: Throwable? = null
        try {
            AppPreferencesSerializer.readFrom(
                ByteArrayInputStream(byteArrayOf(0x7F, 0x7F, 0x7F)),
            )
        } catch (failure: Throwable) {
            thrown = failure
        }

        assertTrue(thrown is CorruptionException)
    }

    @Test
    fun schemaMigrationInvalidatesOldOnboardingVersion() = runTest {
        val old = AppPreferences.newBuilder()
            .setSchemaVersion(0)
            .setOnboardingCompleted(true)
            .setOnboardingVersion("0.9.0")
            .build()
        val migration = AppPreferencesSchemaMigration()

        assertTrue(migration.shouldMigrate(old))
        val migrated = migration.migrate(old)

        assertEquals(CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
        assertFalse(migrated.onboardingCompleted)
    }
}
