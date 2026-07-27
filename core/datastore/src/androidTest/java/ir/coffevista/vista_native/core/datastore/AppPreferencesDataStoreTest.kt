package ir.coffevista.vista_native.core.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppPreferencesDataStoreTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun cleanUp() {
        scopes.forEach(CoroutineScope::cancel)
        context.getSharedPreferences(TEST_LEGACY_PREFS, 0).edit().clear().commit()
        context.filesDir.resolve("datastore/$MIGRATION_FILE").delete()
        context.filesDir.resolve("datastore/$CORRUPTION_FILE").delete()
    }

    @Test
    fun legacySharedPreferencesMigratesExactlyOnceToCurrentProto() = runBlocking {
        context.getSharedPreferences(TEST_LEGACY_PREFS, 0)
            .edit()
            .putBoolean(LEGACY_KEY_COMPLETED, true)
            .putString(LEGACY_KEY_VERSION, CURRENT_ONBOARDING_VERSION)
            .commit()
        val dataStore = createDataStore(
            fileName = MIGRATION_FILE,
            includeLegacyMigration = true,
        )

        val migrated = dataStore.data.first()

        assertEquals(CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
        assertTrue(migrated.onboardingCompleted)
        assertEquals(CURRENT_ONBOARDING_VERSION, migrated.onboardingVersion)
        assertFalse(
            context.getSharedPreferences(TEST_LEGACY_PREFS, 0)
                .contains(LEGACY_KEY_COMPLETED),
        )
    }

    @Test
    fun corruptNonSensitivePreferencesRecoverToDeterministicDefault() = runBlocking {
        val file = context.filesDir.resolve("datastore/$CORRUPTION_FILE")
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(0x7F, 0x7F, 0x7F))
        val dataStore = createDataStore(
            fileName = CORRUPTION_FILE,
            includeLegacyMigration = false,
        )

        val recovered = dataStore.data.first()

        assertEquals(CURRENT_SCHEMA_VERSION, recovered.schemaVersion)
        assertFalse(recovered.onboardingCompleted)
    }

    private fun createDataStore(
        fileName: String,
        includeLegacyMigration: Boolean,
    ) = DataStoreFactory.create(
        serializer = AppPreferencesSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler {
            defaultAppPreferences()
        },
        migrations = buildList {
            if (includeLegacyMigration) {
                add(
                    SharedPreferencesMigration(
                        context = context,
                        sharedPreferencesName = TEST_LEGACY_PREFS,
                        migrate = ::migrateLegacyOnboarding,
                    ),
                )
            }
            add(AppPreferencesSchemaMigration())
        },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also(scopes::add),
        produceFile = { context.filesDir.resolve("datastore/$fileName") },
    )

    private companion object {
        const val TEST_LEGACY_PREFS = "fnd_datastore_legacy_test"
        const val MIGRATION_FILE = "fnd_app_preferences_migration.pb"
        const val CORRUPTION_FILE = "fnd_app_preferences_corrupt.pb"
    }
}
