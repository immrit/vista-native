package ir.coffevista.vista_native.core.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.migrations.SharedPreferencesView
import ir.coffevista.vista_native.core.datastore.proto.AppPreferences

class AppPreferencesSchemaMigration : DataMigration<AppPreferences> {
    override suspend fun shouldMigrate(currentData: AppPreferences): Boolean =
        currentData.schemaVersion < CURRENT_SCHEMA_VERSION

    override suspend fun migrate(currentData: AppPreferences): AppPreferences =
        currentData.toBuilder()
            .setSchemaVersion(CURRENT_SCHEMA_VERSION)
            .setOnboardingCompleted(
                isCurrentOnboardingCompletion(
                    currentData.onboardingCompleted,
                    currentData.onboardingVersion,
                ),
            )
            .build()

    override suspend fun cleanUp() = Unit
}

internal fun migrateLegacyOnboarding(
    legacy: SharedPreferencesView,
    current: AppPreferences,
): AppPreferences {
    val completed = legacy.getBoolean(LEGACY_KEY_COMPLETED, false)
    val version = legacy.getString(LEGACY_KEY_VERSION, null).orEmpty()
    return current.toBuilder()
        .setSchemaVersion(CURRENT_SCHEMA_VERSION)
        .setOnboardingCompleted(isCurrentOnboardingCompletion(completed, version))
        .setOnboardingVersion(version)
        .build()
}

internal const val LEGACY_SHARED_PREFERENCES = "vista_onboarding"
internal const val LEGACY_KEY_COMPLETED = "onboarding_completed"
internal const val LEGACY_KEY_VERSION = "onboarding_version"
