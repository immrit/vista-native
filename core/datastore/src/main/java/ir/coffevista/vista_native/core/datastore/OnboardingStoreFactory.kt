package ir.coffevista.vista_native.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.migrations.SharedPreferencesMigration
import ir.coffevista.vista_native.core.common.DispatcherProvider
import ir.coffevista.vista_native.core.datastore.proto.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

/**
 * Platform-backed construction stays in the datastore owner module while the
 * application composition root controls the singleton lifecycle.
 */
object OnboardingStoreFactory {
    fun create(
        context: Context,
        applicationScope: CoroutineScope,
        dispatchers: DispatcherProvider,
    ): OnboardingStore {
        val dataStore: DataStore<AppPreferences> = DataStoreFactory.create(
            serializer = AppPreferencesSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler {
                defaultAppPreferences()
            },
            migrations = listOf(
                SharedPreferencesMigration(
                    context = context,
                    sharedPreferencesName = LEGACY_SHARED_PREFERENCES,
                    migrate = ::migrateLegacyOnboarding,
                ),
                AppPreferencesSchemaMigration(),
            ),
            scope = applicationScope + dispatchers.io,
            produceFile = { context.dataStoreFile(DATASTORE_FILE_NAME) },
        )
        return ProtoOnboardingStore(dataStore)
    }

    private const val DATASTORE_FILE_NAME = "vista_app_preferences.pb"
}
