package ir.coffevista.vista_native.core.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore AES-256-GCM persistence for verified TLS pinning policy.
 * Survives Room `fallbackToDestructiveMigration()` so an app schema upgrade cannot reset
 * ENFORCE mode or `lastAcceptedRevision` back to zero.
 */
object KeystoreTlsPolicyPersistence {
    private const val PREFS_NAME = "vista_verified_tls_policy_enc"
    private const val KEY_ENVELOPE = "policy_gcm_v1"
    private const val KEYSTORE_ALIAS = "vista_verified_tls_policy_aes_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val AAD = "vista-verified-tls-policy-v1".toByteArray(Charsets.UTF_8)

    fun save(context: Context, policy: TlsPolicy): Boolean = runCatching {
        val payloadJson = buildString {
            append('{')
            append("\"contract_version\":").append(policy.contractVersion).append(',')
            append("\"revision\":").append(policy.revision).append(',')
            append("\"mode\":\"").append(policy.mode.name).append('"')
            policy.currentPinSha256?.let { append(",\"current_pin\":\"").append(it).append('"') }
            policy.nextPinSha256?.let { append(",\"next_pin\":\"").append(it).append('"') }
            policy.expiresAtEpochSeconds?.let { append(",\"expires_at\":").append(it) }
            append('}')
        }.toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(AAD)
        val ciphertext = cipher.doFinal(payloadJson)
        val packed = ByteBuffer.allocate(1 + cipher.iv.size + ciphertext.size)
            .put(cipher.iv.size.toByte())
            .put(cipher.iv)
            .put(ciphertext)
            .array()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENVELOPE, Base64.encodeToString(packed, Base64.NO_WRAP))
            .commit()
    }.getOrDefault(false)

    fun read(context: Context): TlsPolicy? = runCatching {
        val encoded = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ENVELOPE, null)
            ?.takeIf(String::isNotBlank)
            ?: return null
        val buf = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP))
        val ivLen = buf.get().toInt() and 0xFF
        require(ivLen in 12..16 && buf.remaining() > ivLen)
        val iv = ByteArray(ivLen).also(buf::get)
        val ciphertext = ByteArray(buf.remaining()).also(buf::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        cipher.updateAAD(AAD)
        val jsonText = String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        val obj = Json.parseToJsonElement(jsonText).jsonObject
        val modeName = obj["mode"]?.jsonPrimitive?.contentOrNull.orEmpty()
        TlsPolicy(
            contractVersion = obj["contract_version"]?.jsonPrimitive?.intOrNull ?: 1,
            revision = obj["revision"]?.jsonPrimitive?.longOrNull ?: return null,
            mode = TlsMode.entries.firstOrNull { it.name == modeName } ?: TlsMode.ENFORCE,
            currentPinSha256 = obj["current_pin"]?.jsonPrimitive?.contentOrNull,
            nextPinSha256 = obj["next_pin"]?.jsonPrimitive?.contentOrNull,
            expiresAtEpochSeconds = obj["expires_at"]?.jsonPrimitive?.longOrNull,
        )
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }
}
