package ir.coffevista.vista_native.features.chat.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import ir.coffevista.vista_native.features.chat.domain.repository.ChatContentCipher
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encryption at rest for the Chat Room cache. This is not the E2EE protocol. */
class AndroidKeystoreChatContentCipher : ChatContentCipher {
    override suspend fun encrypt(
        accountId: String,
        conversationId: String,
        recordId: String,
        plaintext: String,
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(aad(accountId, conversationId, recordId))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return ByteBuffer.allocate(2 + cipher.iv.size + encrypted.size)
            .put(ENVELOPE_VERSION)
            .put(cipher.iv.size.toByte())
            .put(cipher.iv)
            .put(encrypted)
            .array()
    }

    override suspend fun decrypt(
        accountId: String,
        conversationId: String,
        recordId: String,
        ciphertext: ByteArray,
    ): String {
        require(ciphertext.size > 2) { "Malformed Chat cache envelope" }
        val buffer = ByteBuffer.wrap(ciphertext)
        require(buffer.get() == ENVELOPE_VERSION) { "Unsupported Chat cache envelope" }
        val ivSize = buffer.get().toInt() and 0xFF
        require(ivSize in 12..16 && buffer.remaining() > ivSize) { "Malformed Chat cache IV" }
        val iv = ByteArray(ivSize).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad(accountId, conversationId, recordId))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    @Synchronized
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
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

    private fun aad(accountId: String, conversationId: String, recordId: String): ByteArray =
        "$accountId\u001f$conversationId\u001f$recordId".toByteArray(Charsets.UTF_8)

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "vista_native_chat_cache_aes_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val ENVELOPE_VERSION: Byte = 1
    }
}
