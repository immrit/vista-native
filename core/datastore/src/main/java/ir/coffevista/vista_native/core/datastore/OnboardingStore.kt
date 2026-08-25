package ir.coffevista.vista_native.core.datastore

import androidx.datastore.core.DataStore
import ir.coffevista.vista_native.core.datastore.proto.AppPreferences
import kotlinx.coroutines.flow.first

interface OnboardingStore {
    suspend fun isCompleted(): Boolean
    suspend fun markCompleted()
}

class ProtoOnboardingStore(
    private val dataStore: DataStore<AppPreferences>,
) : OnboardingStore {
    override suspend fun isCompleted(): Boolean {
        val preferences = dataStore.data.first()
        return isCurrentOnboardingCompletion(
            completed = preferences.onboardingCompleted,
            savedVersion = preferences.onboardingVersion,
        )
    }

    override suspend fun markCompleted() {
        dataStore.updateData { current ->
            current.toBuilder()
                .setSchemaVersion(CURRENT_SCHEMA_VERSION)
                .setOnboardingCompleted(true)
                .setOnboardingVersion(CURRENT_ONBOARDING_VERSION)
                .build()
        }
    }
}

internal const val CURRENT_SCHEMA_VERSION = 5
internal const val CURRENT_ONBOARDING_VERSION = "1.0.0"

internal fun isCurrentOnboardingCompletion(
    completed: Boolean,
    savedVersion: String?,
): Boolean = completed && savedVersion == CURRENT_ONBOARDING_VERSION
