package ir.coffevista.vista_native.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

class FlutterSecureStorageCodecTest {
    @Test
    fun readsCurrentFlutterRsaOaepAndAesGcmFormat() {
        val fixture = fixture()

        assertArrayEquals(
            fixture.clear,
            FlutterSecureStorageCodec.decrypt(
                fixture.wrappedKey,
                fixture.encryptedValue,
                fixture.privateKey,
            ),
        )
    }

    @Test
    fun rejectsTamperedFlutterCiphertext() {
        val fixture = fixture()
        fixture.encryptedValue[fixture.encryptedValue.lastIndex] =
            (fixture.encryptedValue.last().toInt() xor 1).toByte()

        assertTrue(
            runCatching {
                FlutterSecureStorageCodec.decrypt(
                    fixture.wrappedKey,
                    fixture.encryptedValue,
                    fixture.privateKey,
                )
            }.isFailure,
        )
    }

    private fun fixture(): CipherFixture {
        val rsa = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val aesBytes = ByteArray(16).also(SecureRandom()::nextBytes)
        val aesKey = SecretKeySpec(aesBytes, "AES")
        val oaep = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA1,
            PSource.PSpecified.DEFAULT,
        )
        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        rsaCipher.init(Cipher.WRAP_MODE, rsa.public, oaep)
        val wrapped = rsaCipher.wrap(aesKey)

        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        val clear = "synthetic-migration-fixture".toByteArray(Charsets.UTF_8)
        val encrypted = iv + aesCipher.doFinal(clear)
        return CipherFixture(wrapped, encrypted, rsa.private, clear)
    }

    private data class CipherFixture(
        val wrappedKey: ByteArray,
        val encryptedValue: ByteArray,
        val privateKey: java.security.PrivateKey,
        val clear: ByteArray,
    )
}
