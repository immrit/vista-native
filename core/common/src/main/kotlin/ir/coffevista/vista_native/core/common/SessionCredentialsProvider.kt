package ir.coffevista.vista_native.core.common

/**
 * Process-wide provider for the current active session credentials stored in
 * SharedPreferences (`vista_active_session_prefs`).
 */
interface SessionCredentialsProvider {
    fun currentSessionId(): String?
    fun currentSessionToken(): String?
    fun currentUserId(): String?
    fun saveSessionCredentials(sessionId: String, sessionToken: String, userId: String? = null)
    fun clear()

    companion object {
        const val PREFS_NAME = "vista_active_session_prefs"
        const val KEY_SESSION_ID = "current_session_id"
        const val KEY_SESSION_TOKEN = "current_session_token"
        const val KEY_SESSION_USER_ID = "current_session_user_id"

        @Volatile
        var current: SessionCredentialsProvider? = null
    }
}
