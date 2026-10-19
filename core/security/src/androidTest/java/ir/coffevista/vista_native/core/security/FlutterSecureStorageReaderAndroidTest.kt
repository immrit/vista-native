package ir.coffevista.vista_native.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

/** Synthetic fixture in the instrumentation package; no production account data is accessed. */
@RunWith(AndroidJUnit4::class)
class FlutterSecureStorageReaderAndroidTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().context
    private val alias: String
        get() = "${context.packageName}.FlutterSecureStoragePluginKeyOAEP"

    @Before fun setUp() = reset()
    @After fun tearDown() = reset()

    @Test
    fun currentFlutterFormatReadsWithoutChangingWrappedKeyOrCiphertext() {
        val stored = writeFixture("fixture_key", "synthetic-value")

        assertEquals(
            FlutterSecureValue.Present("synthetic-value"),
            FlutterSecureStorageReader(context).read("fixture_key"),
        )
        assertEquals(stored.first, prefs(DATA_PREFS).getString("${DATA_KEY_PREFIX}_fixture_key", null))
        assertEquals(stored.second, prefs(KEY_PREFS).getString(WRAPPED_AES_KEY, null))
    }

    @Test
    fun missingKeystoreAliasDoesNotGenerateReplacementForExistingFlutterData() {
        writeFixture("fixture_key", "synthetic-value")
        keyStore().deleteEntry(alias)

        assertEquals(FlutterSecureValue.Unavailable, FlutterSecureStorageReader(context).read("fixture_key"))
        assertFalse(keyStore().containsAlias(alias))
    }

    @Test
    fun syntheticFlutterSessionImportsToNativeCiphertextAndLogoutDoesNotReimport() {
        val values = mapOf(
            "vista_access_token" to "synthetic.header.signature",
            "vista_refresh_token" to "synthetic-refresh",
            "vista_user_id" to "fixture-account",
        )
        writeFixtureValues(values)
        val store = EncryptedSessionStore(context)

        val imported = requireNotNull(store.read())
        assertEquals("fixture-account", imported.userId)
        assertTrue(imported.requiresOnlineValidation)
        val nativeCiphertext = prefs(SESSION_FILE_NAME).getString(SESSION_PAYLOAD_KEY, null).orEmpty()
        assertFalse(nativeCiphertext.contains("synthetic-refresh"))
        assertTrue(prefs(DATA_PREFS).contains("${DATA_KEY_PREFIX}_vista_access_token"))

        store.clear()

        assertEquals(null, store.read())
        assertTrue(prefs(DATA_PREFS).contains("${DATA_KEY_PREFIX}_vista_access_token"))
    }

    private fun writeFixture(logicalKey: String, value: String): Pair<String, String> {
        val stored = writeFixtureValues(mapOf(logicalKey to value))
        return stored.first.getValue(logicalKey) to stored.second
    }

    private fun writeFixtureValues(values: Map<String, String>): Pair<Map<String, String>, String> {
        val keyPair = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .build(),
            )
        }.generateKeyPair()
        val aes = SecretKeySpec(ByteArray(16).also(SecureRandom()::nextBytes), "AES")
        val oaep = OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT,
        )
        val rsa = Cipher.getInstance("RSA/ECB/OAEPPadding", "AndroidKeyStoreBCWorkaround")
        rsa.init(Cipher.WRAP_MODE, keyPair.public, oaep)
        val wrapped = Base64.encodeToString(rsa.wrap(aes), Base64.DEFAULT)

        val encrypted = values.mapValues { (_, value) ->
            val iv = ByteArray(12).also(SecureRandom()::nextBytes)
            val gcm = Cipher.getInstance("AES/GCM/NoPadding")
            gcm.init(Cipher.ENCRYPT_MODE, aes, GCMParameterSpec(128, iv))
            Base64.encodeToString(iv + gcm.doFinal(value.toByteArray()), Base64.DEFAULT)
        }
        val dataEditor = prefs(DATA_PREFS).edit()
        encrypted.forEach { (key, ciphertext) ->
            dataEditor.putString("${DATA_KEY_PREFIX}_$key", ciphertext)
        }
        assertTrue(dataEditor.commit())
        assertTrue(prefs(KEY_PREFS).edit().putString(WRAPPED_AES_KEY, wrapped).commit())
        assertTrue(
            prefs(CONFIG_PREFS).edit()
                .putString("FlutterSecureSAlgorithmKey", "RSA_ECB_OAEPwithSHA_256andMGF1Padding")
                .putString("FlutterSecureSAlgorithmStorage", "AES_GCM_NoPadding")
                .commit(),
        )
        return encrypted to wrapped
    }

    private fun reset() {
        listOf(
            DATA_PREFS,
            KEY_PREFS,
            CONFIG_PREFS,
            SESSION_FILE_NAME,
            "vista_flutter_session_migration",
        ).forEach { name ->
            prefs(name).edit().clear().commit()
        }
        keyStore().let {
            if (it.containsAlias(alias)) it.deleteEntry(alias)
            if (it.containsAlias(SESSION_KEY_ALIAS)) it.deleteEntry(SESSION_KEY_ALIAS)
        }
    }

    private fun prefs(name: String) = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private companion object {
        const val DATA_PREFS = "FlutterSecureStorage"
        const val KEY_PREFS = "FlutterSecureKeyStorage"
        const val CONFIG_PREFS = "FlutterSecureStorageConfiguration:FlutterSecureStorage"
        const val DATA_KEY_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIHNlY3VyZSBzdG9yYWdlCg"
        const val WRAPPED_AES_KEY = "AESVGhpcyBpcyB0aGUga2V5IGZvciBhIHNlY3VyZSBzdG9yYWdlIEFFUyBLZXkK"
    }
}
