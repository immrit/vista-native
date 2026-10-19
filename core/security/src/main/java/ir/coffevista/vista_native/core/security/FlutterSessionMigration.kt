package ir.coffevista.vista_native.core.security

import android.content.Context

/** The Flutter store stays intact for rollback; a logout tombstone prevents re-import. */
internal interface FlutterSessionMigration {
    fun importOrNull(): StoredSession?
    fun preventFutureImport(): Boolean
}

internal object NoOpFlutterSessionMigration : FlutterSessionMigration {
    override fun importOrNull(): StoredSession? = null
    override fun preventFutureImport(): Boolean = true
}

internal class AndroidFlutterSessionMigration(context: Context) : FlutterSessionMigration {
    private val reader = FlutterSecureStorageReader(context)
    private val gate = context.getSharedPreferences(MIGRATION_STATE_PREFS, Context.MODE_PRIVATE)

    override fun importOrNull(): StoredSession? {
        if (gate.getBoolean(IMPORT_DISABLED, false)) return null
        return runCatching { flutterSessionFromValues(reader::read) }.getOrNull()
    }

    override fun preventFutureImport(): Boolean = gate.edit()
        .putBoolean(IMPORT_DISABLED, true)
        .commit()

    private companion object {
        const val MIGRATION_STATE_PREFS = "vista_flutter_session_migration"
        const val IMPORT_DISABLED = "legacy_import_disabled"
    }
}

internal fun flutterSessionFromValues(
    readValue: (String) -> FlutterSecureValue,
): StoredSession? {
    fun read(key: String): String? = when (val value = readValue(key)) {
        FlutterSecureValue.Missing -> null
        is FlutterSecureValue.Present -> value.value
        FlutterSecureValue.Unavailable -> error("Flutter secure session is unavailable")
    }

    val accessToken = read("vista_access_token")?.takeIf(String::isNotBlank) ?: return null
    val refreshToken = read("vista_refresh_token")?.takeIf(String::isNotBlank)
        ?: error("Flutter secure session is incomplete")
    val storedUserId = read("vista_user_id")?.takeIf(String::isNotBlank)
    val tokenUserId = userIdFromJwt(accessToken)
    if (storedUserId != null && tokenUserId != null) {
        require(storedUserId == tokenUserId) { "Flutter session account binding mismatch" }
    }
    val userId = storedUserId ?: tokenUserId
        ?: error("Flutter secure session has no account binding")
    val passwordRequired = read("vista_password_required") == "true"

    return StoredSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        // Force a server refresh before accepting any Flutter-only profile state.
        expiresAtEpochSeconds = 1L,
        profileCompleted = false,
        passwordRequired = passwordRequired,
        requiresOnlineValidation = true,
    )
}
