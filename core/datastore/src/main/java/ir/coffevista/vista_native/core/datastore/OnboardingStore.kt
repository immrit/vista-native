package ir.coffevista.vista_native.core.datastore

import android.annotation.SuppressLint
import android.content.Context

interface OnboardingStore {
    fun isCompleted(): Boolean
    fun markCompleted()
}

@SuppressLint("ApplySharedPref")
class SharedPreferencesOnboardingStore(context: Context) : OnboardingStore {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun isCompleted(): Boolean {
        val completed = preferences.getBoolean(KEY_COMPLETED, false)
        val savedVersion = preferences.getString(KEY_VERSION, null)
        if (completed && !isCurrentOnboardingCompletion(completed, savedVersion)) {
            preferences.edit().putBoolean(KEY_COMPLETED, false).commit()
            return false
        }
        return completed
    }

    override fun markCompleted() {
        preferences.edit()
            .putBoolean(KEY_COMPLETED, true)
            .putString(KEY_VERSION, CURRENT_VERSION)
            .commit()
    }

    companion object {
        const val CURRENT_VERSION = "1.0.0"
        private const val FILE_NAME = "vista_onboarding"
        private const val KEY_COMPLETED = "onboarding_completed"
        private const val KEY_VERSION = "onboarding_version"
    }
}

internal fun isCurrentOnboardingCompletion(
    completed: Boolean,
    savedVersion: String?,
): Boolean = completed && savedVersion == SharedPreferencesOnboardingStore.CURRENT_VERSION
