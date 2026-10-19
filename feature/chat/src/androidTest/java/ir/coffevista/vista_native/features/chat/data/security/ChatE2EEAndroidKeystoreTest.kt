package ir.coffevista.vista_native.features.chat.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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

/** Exercises the real AndroidKeyStore rather than only the pure X25519 implementation. */
@RunWith(AndroidJUnit4::class)
class ChatE2EEAndroidKeystoreTest {
    @Test
    fun flutterCurrentFormatIdentityImportsWithoutChangingLegacyCiphertext() {
        // Instrumentation owns these synthetic preferences and Keystore entries.
        val context = InstrumentationRegistry.getInstrumentation().context
        val userId = "flutter-e2ee-fixture-${System.nanoTime()}"
        val privateKey = ByteArray(32) { (it + 1).toByte() }.also {
            it[0] = (it[0].toInt() and 248).toByte()
            it[31] = (it[31].toInt() and 127 or 64).toByte()
        }
        val publicKey = X25519.scalarMultBase(privateKey)
        val privateName = "e2e_priv_$userId"
        val publicName = "e2e_pub_$userId"
        val dataPrefs = context.getSharedPreferences("FlutterSecureStorage", Context.MODE_PRIVATE)
        val keyPrefs = context.getSharedPreferences("FlutterSecureKeyStorage", Context.MODE_PRIVATE)
        val configPrefs = context.getSharedPreferences(
            "FlutterSecureStorageConfiguration:FlutterSecureStorage", Context.MODE_PRIVATE,
        )
        val nativePrefs = context.getSharedPreferences("vista_e2e_keys", Context.MODE_PRIVATE)
        val flutterAlias = "${context.packageName}.FlutterSecureStoragePluginKeyOAEP"
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        try {
            val fixtureCiphertexts = writeFlutterKeyFixture(
                context, flutterAlias,
                mapOf(
                    privateName to Base64.encodeToString(privateKey, Base64.NO_WRAP),
                    publicName to Base64.encodeToString(publicKey, Base64.NO_WRAP),
                ),
            )
            val imported = ChatE2EEService(context).getOrGenerateKeyPair(userId)

            assertTrue(privateKey.contentEquals(imported.privateKey))
            assertTrue(publicKey.contentEquals(imported.publicKey))
            assertTrue(nativePrefs.getString("e2e_priv_enc_$userId", null)?.isNotBlank() == true)
            assertEquals(null, nativePrefs.getString(privateName, null))
            assertTrue(privateKey.contentEquals(ChatE2EEService(context).getOrGenerateKeyPair(userId).privateKey))
            fixtureCiphertexts.forEach { (name, ciphertext) ->
                assertEquals(ciphertext, dataPrefs.getString("${FLUTTER_KEY_PREFIX}_$name", null))
            }
        } finally {
            nativePrefs.edit().remove(privateName).remove("e2e_priv_enc_$userId")
                .remove(publicName).commit()
            dataPrefs.edit().remove("${FLUTTER_KEY_PREFIX}_$privateName")
                .remove("${FLUTTER_KEY_PREFIX}_$publicName").commit()
            keyPrefs.edit().remove(FLUTTER_WRAPPED_AES_KEY).commit()
            configPrefs.edit().remove("FlutterSecureSAlgorithmKey")
                .remove("FlutterSecureSAlgorithmStorage").commit()
            if (keyStore.containsAlias(flutterAlias)) keyStore.deleteEntry(flutterAlias)
        }
    }

    @Test
    fun corruptLegacyPrivateKeyIsPreservedAndDoesNotRotateIdentity() {
        // Use the instrumentation package, not the installed production app's data.
        val context = InstrumentationRegistry.getInstrumentation().context
        val userId = "e2ee-corrupt-legacy-${System.nanoTime()}"
        val preferences = context.getSharedPreferences("vista_e2e_keys", Context.MODE_PRIVATE)
        val legacyValue = "not-a-valid-x25519-private-key"

        try {
            assertTrue(preferences.edit().putString("e2e_priv_$userId", legacyValue).commit())

            val failure = runCatching {
                ChatE2EEService(context).getOrGenerateKeyPair(userId)
            }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
            assertEquals(legacyValue, preferences.getString("e2e_priv_$userId", null))
            assertEquals(null, preferences.getString("e2e_priv_enc_$userId", null))
        } finally {
            preferences.edit()
                .remove("e2e_priv_$userId")
                .remove("e2e_priv_enc_$userId")
                .remove("e2e_pub_$userId")
                .commit()
        }
    }

    @Test
    fun stalePublicKeyIsRepairedWithoutRotatingThePrivateIdentity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val service = ChatE2EEService(context)
        val userId = "e2ee-repair-test-${System.nanoTime()}"
        val preferences = context.getSharedPreferences("vista_e2e_keys", Context.MODE_PRIVATE)

        try {
            val original = service.getOrGenerateKeyPair(userId)
            preferences.edit()
                .putString("e2e_pub_$userId", Base64.encodeToString(ByteArray(32) { 1 }, Base64.NO_WRAP))
                .commit()

            val repaired = ChatE2EEService(context).getOrGenerateKeyPair(userId)

            assertTrue(original.privateKey.contentEquals(repaired.privateKey))
            assertTrue(original.publicKey.contentEquals(repaired.publicKey))
            assertEquals(
                Base64.encodeToString(original.publicKey, Base64.NO_WRAP),
                preferences.getString("e2e_pub_$userId", null),
            )
        } finally {
            preferences.edit()
                .remove("e2e_priv_$userId")
                .remove("e2e_priv_enc_$userId")
                .remove("e2e_pub_$userId")
                .apply()
        }
    }

    @Test
    fun privateKeyIsKeystoreEncrypted_andBoundMessagesRejectTampering() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val service = ChatE2EEService(context)
        val userId = "e2ee-android-test-${System.nanoTime()}"
        val preferences = context.getSharedPreferences("vista_e2e_keys", Context.MODE_PRIVATE)

        try {
            val alice = service.getOrGenerateKeyPair(userId)
            val encodedPrivate = Base64.encodeToString(alice.privateKey, Base64.NO_WRAP)
            val encryptedPrivate = preferences.getString("e2e_priv_enc_$userId", null)

            assertTrue(encryptedPrivate?.isNotBlank() == true)
            assertEquals(null, preferences.getString("e2e_priv_$userId", null))
            assertNotEquals(encodedPrivate, encryptedPrivate)
            assertTrue(alice.privateKey.contentEquals(service.getOrGenerateKeyPair(userId).privateKey))

            val peerPrivate = ByteArray(32) { (it + 37).toByte() }.also {
                it[0] = (it[0].toInt() and 248).toByte()
                it[31] = (it[31].toInt() and 127 or 64).toByte()
            }
            val sharedSecret = service.computeSharedSecret(alice.privateKey, X25519.scalarMultBase(peerPrivate))
            val binding = ChatE2EEService.messageBinding("fixture-secret", userId, "message-1")
            val encrypted = service.encryptMessage("متن محرمانه تست", sharedSecret, binding)

            assertTrue(encrypted.startsWith(ChatE2EEService.PREFIX_V2))
            assertEquals("متن محرمانه تست", service.decryptMessage(encrypted, sharedSecret, binding))
            assertFalse(service.decryptMessage(encrypted, sharedSecret, "$binding-wrong") == "متن محرمانه تست")
            val tampered = encrypted.dropLast(1) + if (encrypted.last() == 'A') "B" else "A"
            assertFalse(service.decryptMessage(tampered, sharedSecret, binding) == "متن محرمانه تست")
        } finally {
            preferences.edit()
                .remove("e2e_priv_$userId")
                .remove("e2e_priv_enc_$userId")
                .remove("e2e_pub_$userId")
                .apply()
        }
    }

    private fun writeFlutterKeyFixture(
        context: Context,
        alias: String,
        values: Map<String, String>,
    ): Map<String, String> {
        val keyPair = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(
                    alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
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
        val dataEditor = context.getSharedPreferences("FlutterSecureStorage", Context.MODE_PRIVATE)
            .edit()
        encrypted.forEach { (name, ciphertext) ->
            dataEditor.putString("${FLUTTER_KEY_PREFIX}_$name", ciphertext)
        }
        assertTrue(dataEditor.commit())
        assertTrue(
            context.getSharedPreferences("FlutterSecureKeyStorage", Context.MODE_PRIVATE)
                .edit().putString(FLUTTER_WRAPPED_AES_KEY, wrapped).commit(),
        )
        assertTrue(
            context.getSharedPreferences(
                "FlutterSecureStorageConfiguration:FlutterSecureStorage", Context.MODE_PRIVATE,
            ).edit()
                .putString("FlutterSecureSAlgorithmKey", "RSA_ECB_OAEPwithSHA_256andMGF1Padding")
                .putString("FlutterSecureSAlgorithmStorage", "AES_GCM_NoPadding")
                .commit(),
        )
        return encrypted
    }

    private companion object {
        const val FLUTTER_KEY_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIHNlY3VyZSBzdG9yYWdlCg"
        const val FLUTTER_WRAPPED_AES_KEY =
            "AESVGhpcyBpcyB0aGUga2V5IGZvciBhIHNlY3VyZSBzdG9yYWdlIEFFUyBLZXkK"
    }
}
