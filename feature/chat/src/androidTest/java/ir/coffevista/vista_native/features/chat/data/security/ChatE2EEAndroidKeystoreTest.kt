package ir.coffevista.vista_native.features.chat.data.security

import android.content.Context
import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real AndroidKeyStore rather than only the pure X25519 implementation. */
@RunWith(AndroidJUnit4::class)
class ChatE2EEAndroidKeystoreTest {
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
}
