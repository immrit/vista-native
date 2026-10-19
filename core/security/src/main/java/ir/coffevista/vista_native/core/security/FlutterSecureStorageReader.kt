package ir.coffevista.vista_native.core.security

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import java.security.PrivateKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/** Reads the current Flutter secure-storage format without creating keys or changing Flutter data. */
class FlutterSecureStorageReader(context: Context) {
    private val appContext = context.applicationContext

    fun read(logicalKey: String): FlutterSecureValue = runCatching {
        require(logicalKey.isNotBlank() && logicalKey.length <= 256)
        val data = appContext.getSharedPreferences(DATA_PREFS, Context.MODE_PRIVATE)
        val encrypted = data.getString("${DATA_KEY_PREFIX}_$logicalKey", null)
        if (encrypted == null) {
            return@runCatching if (data.contains(ESP_KEY_KEYSET) || data.contains(ESP_VALUE_KEYSET)) {
                FlutterSecureValue.Unavailable
            } else {
                FlutterSecureValue.Missing
            }
        }

        val config = appContext.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
        val oldConfig = appContext.getSharedPreferences(LEGACY_CONFIG_PREFS, Context.MODE_PRIVATE)
        val keyAlgorithm = config.getString(KEY_ALGORITHM_MARKER, null)
            ?: oldConfig.getString(KEY_ALGORITHM_MARKER, null)
        val storageAlgorithm = config.getString(STORAGE_ALGORITHM_MARKER, null)
            ?: oldConfig.getString(STORAGE_ALGORITHM_MARKER, null)
        if (keyAlgorithm != CURRENT_KEY_ALGORITHM || storageAlgorithm != CURRENT_STORAGE_ALGORITHM) {
            return@runCatching FlutterSecureValue.Unavailable
        }

        val wrapped = appContext.getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE)
            .getString(WRAPPED_AES_KEY, null)
            ?: return@runCatching FlutterSecureValue.Unavailable
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = keyStore.getKey(
            "${appContext.packageName}.FlutterSecureStoragePluginKeyOAEP",
            null,
        ) as? PrivateKey ?: return@runCatching FlutterSecureValue.Unavailable
        val clear = FlutterSecureStorageCodec.decrypt(
            wrappedKey = Base64.decode(wrapped, Base64.DEFAULT),
            encryptedValue = Base64.decode(encrypted, Base64.DEFAULT),
            privateKey = privateKey,
            rsaProvider = "AndroidKeyStoreBCWorkaround",
        )
        FlutterSecureValue.Present(clear.toString(Charsets.UTF_8))
    }.getOrDefault(FlutterSecureValue.Unavailable)

    private companion object {
        const val DATA_PREFS = "FlutterSecureStorage"
        const val KEY_PREFS = "FlutterSecureKeyStorage"
        const val CONFIG_PREFS = "FlutterSecureStorageConfiguration:FlutterSecureStorage"
        const val LEGACY_CONFIG_PREFS = "FlutterSecureStorageConfiguration"
        const val DATA_KEY_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIHNlY3VyZSBzdG9yYWdlCg"
        const val WRAPPED_AES_KEY = "AESVGhpcyBpcyB0aGUga2V5IGZvciBhIHNlY3VyZSBzdG9yYWdlIEFFUyBLZXkK"
        const val KEY_ALGORITHM_MARKER = "FlutterSecureSAlgorithmKey"
        const val STORAGE_ALGORITHM_MARKER = "FlutterSecureSAlgorithmStorage"
        const val CURRENT_KEY_ALGORITHM = "RSA_ECB_OAEPwithSHA_256andMGF1Padding"
        const val CURRENT_STORAGE_ALGORITHM = "AES_GCM_NoPadding"
        const val ESP_KEY_KEYSET = "__androidx_security_crypto_encrypted_prefs_key_keyset__"
        const val ESP_VALUE_KEYSET = "__androidx_security_crypto_encrypted_prefs_value_keyset__"
    }
}

sealed interface FlutterSecureValue {
    data object Missing : FlutterSecureValue
    data class Present(val value: String) : FlutterSecureValue
    data object Unavailable : FlutterSecureValue
}

/** Byte-level codec kept separate from Android preferences/Keystore for deterministic tests. */
internal object FlutterSecureStorageCodec {
    fun decrypt(
        wrappedKey: ByteArray,
        encryptedValue: ByteArray,
        privateKey: PrivateKey,
        rsaProvider: String? = null,
    ): ByteArray {
        require(encryptedValue.size >= IV_SIZE + TAG_SIZE_BYTES)
        val rsa = if (rsaProvider == null) {
            Cipher.getInstance("RSA/ECB/OAEPPadding")
        } else {
            Cipher.getInstance("RSA/ECB/OAEPPadding", rsaProvider)
        }
        rsa.init(
            Cipher.UNWRAP_MODE,
            privateKey,
            OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT),
        )
        val aesKey = rsa.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY)
        val aes = Cipher.getInstance("AES/GCM/NoPadding")
        aes.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, encryptedValue, 0, IV_SIZE))
        return aes.doFinal(encryptedValue, IV_SIZE, encryptedValue.size - IV_SIZE)
    }

    private const val IV_SIZE = 12
    private const val TAG_SIZE_BYTES = 16
}
