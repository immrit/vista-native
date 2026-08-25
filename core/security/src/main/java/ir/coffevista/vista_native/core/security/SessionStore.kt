package ir.coffevista.vista_native.core.security

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import javax.inject.Inject

data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val expiresAtEpochSeconds: Long,
    val profileCompleted: Boolean,
    val passwordRequired: Boolean,
    val displayName: String = "کاربر ویستا",
    val biometricEnabled: Boolean = false,
)

interface SessionStore {
    fun read(): StoredSession?
    fun save(payload: AuthPayload)
    fun markPasswordConfigured()
    fun isBiometricEnabled(): Boolean
    fun setBiometricEnabled(enabled: Boolean)
    fun clear()
}

@SuppressLint("ApplySharedPref")
class EncryptedSessionStore internal constructor(
    private val storage: SessionPayloadStorage,
    private val keyProvider: SessionSecretKeyProvider,
) : SessionStore {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        storage = SharedPreferencesSessionPayloadStorage(context),
        keyProvider = AndroidKeystoreSessionKeyProvider(),
    )

    override fun read(): StoredSession? {
        val encoded = runCatching(storage::read).getOrElse {
            failClosed()
            return null
        }?.takeIf(String::isNotBlank) ?: return null

        return runCatching {
            SessionEnvelopeCodec.decode(
                encoded = encoded,
                key = keyProvider.getOrCreate(),
            )
        }.getOrElse {
            failClosed()
            null
        }
    }

    override fun save(payload: AuthPayload) {
        val preservedBiometricPreference = read()
            ?.takeIf { current -> current.userId == payload.user.id }
            ?.biometricEnabled
            ?: false
        saveStoredSession(
            StoredSession(
                accessToken = payload.session.accessToken,
                refreshToken = payload.session.refreshToken,
                userId = payload.user.id,
                expiresAtEpochSeconds = payload.session.expiresAtEpochSeconds,
                profileCompleted = payload.user.profileCompleted,
                passwordRequired = payload.user.passwordRequired,
                displayName = payload.user.welcomeName,
                biometricEnabled = preservedBiometricPreference,
            ),
        )
    }

    override fun markPasswordConfigured() {
        val current = read() ?: return
        saveStoredSession(current.copy(passwordRequired = false))
    }

    override fun isBiometricEnabled(): Boolean = read()?.biometricEnabled == true

    override fun setBiometricEnabled(enabled: Boolean) {
        val current = read() ?: return
        saveStoredSession(current.copy(biometricEnabled = enabled))
    }

    override fun clear() {
        check(storage.clear()) { "Encrypted session clear failed" }
    }

    private fun saveStoredSession(session: StoredSession) {
        require(session.accessToken.isNotBlank()) { "Access token must not be blank" }
        require(session.refreshToken.isNotBlank()) { "Refresh token must not be blank" }
        require(session.userId.isNotBlank()) { "User id must not be blank" }
        require(session.expiresAtEpochSeconds > 0L) { "Session expiry must be positive" }

        val encoded = SessionEnvelopeCodec.encode(
            session = session,
            key = keyProvider.getOrCreate(),
        )
        check(storage.write(encoded)) { "Encrypted session persistence failed" }
    }

    private fun failClosed() {
        runCatching(storage::clear)
        runCatching(keyProvider::reset)
    }
}

internal interface SessionPayloadStorage {
    fun read(): String?
    fun write(encoded: String): Boolean
    fun clear(): Boolean
}

@SuppressLint("ApplySharedPref")
private class SharedPreferencesSessionPayloadStorage(
    context: Context,
) : SessionPayloadStorage {
    private val preferences = context.getSharedPreferences(
        SESSION_FILE_NAME,
        Context.MODE_PRIVATE,
    )

    override fun read(): String? = preferences.getString(SESSION_PAYLOAD_KEY, null)

    override fun write(encoded: String): Boolean = preferences.edit()
        .putString(SESSION_PAYLOAD_KEY, encoded)
        .commit()

    override fun clear(): Boolean = preferences.edit().clear().commit()
}

internal interface SessionSecretKeyProvider {
    fun getOrCreate(): SecretKey
    fun reset()
}

private class AndroidKeystoreSessionKeyProvider : SessionSecretKeyProvider {
    @Synchronized
    override fun getOrCreate(): SecretKey {
        val keyStore = keyStore()
        val existing = runCatching {
            keyStore.getKey(SESSION_KEY_ALIAS, null) as? SecretKey
        }.getOrElse {
            runCatching { keyStore.deleteEntry(SESSION_KEY_ALIAS) }
            null
        }
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                SESSION_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    @Synchronized
    override fun reset() {
        val keyStore = keyStore()
        if (keyStore.containsAlias(SESSION_KEY_ALIAS)) {
            keyStore.deleteEntry(SESSION_KEY_ALIAS)
        }
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
}

internal object SessionEnvelopeCodec {
    fun encode(
        session: StoredSession,
        key: SecretKey,
    ): String {
        val accountBinding = accountBinding(session.userId)
        val clearJson = JSONObject()
            .put(KEY_SCHEMA_VERSION, SESSION_SCHEMA_VERSION)
            .put(KEY_ACCOUNT_BINDING, accountBinding)
            .put(KEY_ACCESS_TOKEN, session.accessToken)
            .put(KEY_REFRESH_TOKEN, session.refreshToken)
            .put(KEY_USER_ID, session.userId)
            .put(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            .put(KEY_PROFILE_COMPLETED, session.profileCompleted)
            .put(KEY_PASSWORD_REQUIRED, session.passwordRequired)
            .put(KEY_DISPLAY_NAME, session.displayName)
            .put(KEY_BIOMETRIC_ENABLED, session.biometricEnabled)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(additionalAuthenticatedData(accountBinding))
        val encrypted = cipher.doFinal(clearJson)

        return listOf(
            ENVELOPE_VERSION,
            accountBinding,
            Base64UrlCodec.encode(cipher.iv),
            Base64UrlCodec.encode(encrypted),
        ).joinToString(PAYLOAD_SEPARATOR)
    }

    fun decode(
        encoded: String,
        key: SecretKey,
    ): StoredSession {
        val parts = encoded.split(PAYLOAD_SEPARATOR)
        require(parts.size == ENVELOPE_PART_COUNT) { "Encrypted session payload is malformed" }
        require(parts[0] == ENVELOPE_VERSION) { "Unsupported session envelope version" }
        val accountBinding = parts[1].takeIf(String::isNotBlank)
            ?: error("Missing account binding")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(
                GCM_TAG_LENGTH_BITS,
                Base64UrlCodec.decode(parts[2]),
            ),
        )
        cipher.updateAAD(additionalAuthenticatedData(accountBinding))
        val clearBytes = cipher.doFinal(Base64UrlCodec.decode(parts[3]))
        val json = JSONObject(clearBytes.toString(Charsets.UTF_8))
        require(json.optInt(KEY_SCHEMA_VERSION) == SESSION_SCHEMA_VERSION) {
            "Unsupported session schema version"
        }
        val userId = json.optString(KEY_USER_ID).takeIf(String::isNotBlank)
            ?: error("Missing session user")
        require(accountBinding == accountBinding(userId)) { "Session account binding mismatch" }
        require(json.optString(KEY_ACCOUNT_BINDING) == accountBinding) {
            "Session payload binding mismatch"
        }
        val accessToken = json.optString(KEY_ACCESS_TOKEN).takeIf(String::isNotBlank)
            ?: error("Missing access token")
        val refreshToken = json.optString(KEY_REFRESH_TOKEN).takeIf(String::isNotBlank)
            ?: error("Missing refresh token")
        val expiresAt = json.optLong(KEY_EXPIRES_AT).takeIf { it > 0L }
            ?: error("Invalid session expiry")

        return StoredSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            expiresAtEpochSeconds = expiresAt,
            profileCompleted = json.optBoolean(KEY_PROFILE_COMPLETED, true),
            passwordRequired = json.optBoolean(KEY_PASSWORD_REQUIRED, false),
            displayName = json.optString(KEY_DISPLAY_NAME)
                .trim()
                .takeIf(String::isNotEmpty)
                ?: displayNameFromJwt(accessToken)
                ?: "کاربر ویستا",
            biometricEnabled = json.optBoolean(KEY_BIOMETRIC_ENABLED, false),
        )
    }

    private fun accountBinding(userId: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(userId.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xFF) }

    private fun additionalAuthenticatedData(accountBinding: String): ByteArray =
        "$ENVELOPE_VERSION:$SESSION_SCHEMA_VERSION:$accountBinding"
            .toByteArray(Charsets.UTF_8)
}

internal const val SESSION_FILE_NAME = "vista_secure_session"
internal const val SESSION_PAYLOAD_KEY = "encrypted_payload"
internal const val SESSION_KEY_ALIAS = "vista_native_session_aes_v2"

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private const val PAYLOAD_SEPARATOR = "."
private const val ENVELOPE_VERSION = "v1"
private const val ENVELOPE_PART_COUNT = 4
private const val SESSION_SCHEMA_VERSION = 1

private const val KEY_SCHEMA_VERSION = "schema_version"
private const val KEY_ACCOUNT_BINDING = "account_binding"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER_ID = "user_id"
private const val KEY_EXPIRES_AT = "expires_at"
private const val KEY_PROFILE_COMPLETED = "profile_completed"
private const val KEY_PASSWORD_REQUIRED = "password_required"
private const val KEY_DISPLAY_NAME = "display_name"
private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

fun expiryFromJwt(token: String): Long? = jwtPayload(token)
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
    val decoded = Base64UrlCodec.decode(parts[1])
    JSONObject(decoded.toString(Charsets.UTF_8))
}.getOrNull()

@OptIn(ExperimentalEncodingApi::class)
private object Base64UrlCodec {
    private val codec = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    fun encode(bytes: ByteArray): String = codec.encode(bytes)

    fun decode(encoded: String): ByteArray = codec.decode(encoded)
}
