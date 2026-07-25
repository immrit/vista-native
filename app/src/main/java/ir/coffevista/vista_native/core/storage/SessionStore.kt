package ir.coffevista.vista_native.core.storage

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import ir.coffevista.vista_native.core.auth.AuthPayload
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val expiresAtEpochSeconds: Long,
    val profileCompleted: Boolean,
    val passwordRequired: Boolean,
    val displayName: String = "کاربر ویستا",
)

interface SessionStore {
    fun read(): StoredSession?
    fun save(payload: AuthPayload)
    fun markPasswordConfigured()
    fun clear()
}

@SuppressLint("ApplySharedPref")
class EncryptedSessionStore(context: Context) : SessionStore {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun read(): StoredSession? {
        val json = readJson() ?: return null
        val accessToken = json.optString(KEY_ACCESS_TOKEN)
            .takeIf(String::isNotBlank) ?: return null
        val userId = json.optString(KEY_USER_ID)
            .takeIf(String::isNotBlank) ?: userIdFromJwt(accessToken) ?: return null
        val expiresAt = json.optLong(KEY_EXPIRES_AT)
            .takeIf { it > 0L } ?: expiryFromJwt(accessToken) ?: return null

        return StoredSession(
            accessToken = accessToken,
            refreshToken = json.optString(KEY_REFRESH_TOKEN),
            userId = userId,
            expiresAtEpochSeconds = expiresAt,
            profileCompleted = json.optBoolean(KEY_PROFILE_COMPLETED, true),
            passwordRequired = json.optBoolean(KEY_PASSWORD_REQUIRED, false),
            displayName = json.optString(KEY_DISPLAY_NAME)
                .trim()
                .takeIf(String::isNotEmpty)
                ?: displayNameFromJwt(accessToken)
                ?: "کاربر ویستا",
        )
    }

    override fun save(payload: AuthPayload) {
        writeJson(
            JSONObject()
                .put(KEY_ACCESS_TOKEN, payload.session.accessToken)
                .put(KEY_REFRESH_TOKEN, payload.session.refreshToken)
                .put(KEY_USER_ID, payload.user.id)
                .put(KEY_EXPIRES_AT, payload.session.expiresAtEpochSeconds)
                .put(KEY_PROFILE_COMPLETED, payload.user.profileCompleted)
                .put(KEY_PASSWORD_REQUIRED, payload.user.passwordRequired)
                .put(KEY_DISPLAY_NAME, payload.user.welcomeName),
        )
    }

    override fun markPasswordConfigured() {
        val json = readJson() ?: return
        json.put(KEY_PASSWORD_REQUIRED, false)
        writeJson(json)
    }

    override fun clear() {
        preferences.edit().clear().commit()
    }

    private fun readJson(): JSONObject? {
        val encoded = preferences.getString(KEY_ENCRYPTED_PAYLOAD, null)
            ?.takeIf(String::isNotBlank) ?: return null
        val parts = encoded.split(PAYLOAD_SEPARATOR, limit = 2)
        require(parts.size == 2) { "Encrypted session payload is malformed" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(parts[0], Base64.NO_WRAP)),
        )
        val clearBytes = cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP))
        return JSONObject(clearBytes.toString(Charsets.UTF_8))
    }

    private fun writeJson(json: JSONObject) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        val payload = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) +
            PAYLOAD_SEPARATOR +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)

        check(
            preferences.edit()
                .putString(KEY_ENCRYPTED_PAYLOAD, payload)
                .commit(),
        ) { "Encrypted session persistence failed" }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val FILE_NAME = "vista_secure_session"
        private const val KEY_ENCRYPTED_PAYLOAD = "encrypted_payload"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "vista_native_session_aes_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PAYLOAD_SEPARATOR = "."

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_PROFILE_COMPLETED = "profile_completed"
        private const val KEY_PASSWORD_REQUIRED = "password_required"
        private const val KEY_DISPLAY_NAME = "display_name"
    }
}

internal fun expiryFromJwt(token: String): Long? = jwtPayload(token)
    ?.optLong("exp")
    ?.takeIf { it > 0L }

private fun userIdFromJwt(token: String): String? = jwtPayload(token)
    ?.optString("sub")
    ?.takeIf(String::isNotBlank)

private fun displayNameFromJwt(token: String): String? = jwtPayload(token)
    ?.let { payload ->
        payload.optString("username").trim().takeIf(String::isNotEmpty)
            ?: payload.optString("phone_number").trim().takeIf(String::isNotEmpty)
    }

private fun jwtPayload(token: String): JSONObject? = runCatching {
    val parts = token.split('.')
    if (parts.size != 3) return null
    val decoded = Base64.decode(
        parts[1],
        Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
    )
    JSONObject(decoded.toString(Charsets.UTF_8))
}.getOrNull()
