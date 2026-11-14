package ir.coffevista.vista_native.features.chat.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.SecureRandom

class ChatE2EETest {

    @Test
    fun `legacy encrypted payload shape requires nonce mac and ciphertext`() {
        val valid = ByteArray(34)
        valid[0] = 12
        valid[13] = 16

        assertTrue(hasLegacyEncryptedPayloadShape(valid))
        assertFalse(hasLegacyEncryptedPayloadShape(ByteArray(30).also {
            it[0] = 12
            it[13] = 16
        }))
        assertFalse(hasLegacyEncryptedPayloadShape(valid.copyOf().also { it[0] = 8 }))
        assertFalse(hasLegacyEncryptedPayloadShape(valid.copyOf().also { it[13] = 12 }))
        assertFalse(hasLegacyEncryptedPayloadShape("ordinary Base64 text".toByteArray()))
    }

    @Test
    fun `ecdh shared secret matches on both sides`() {
        val alicePriv = ByteArray(32) { (it + 1).toByte() }
        alicePriv[0] = (alicePriv[0].toInt() and 248).toByte()
        alicePriv[31] = (alicePriv[31].toInt() and 127 or 64).toByte()
        val alicePub = X25519.scalarMultBase(alicePriv)

        val bobPriv = ByteArray(32) { (it + 50).toByte() }
        bobPriv[0] = (bobPriv[0].toInt() and 248).toByte()
        bobPriv[31] = (bobPriv[31].toInt() and 127 or 64).toByte()
        val bobPub = X25519.scalarMultBase(bobPriv)

        val aliceShared = X25519.scalarMult(alicePriv, bobPub)
        val bobShared = X25519.scalarMult(bobPriv, alicePub)

        assertEquals(32, aliceShared.size)
        assertEquals(32, bobShared.size)
        assertTrue(aliceShared.contentEquals(bobShared))
    }

    @Test
    fun `v3 forward secret envelope round trips produces distinct ciphertexts and rejects tampered aad`() {
        val rng = SecureRandom()
        val alicePriv = generateRandomClampedScalar(rng)
        val alicePub = X25519.scalarMultBase(alicePriv)
        val bobPriv = generateRandomClampedScalar(rng)
        val bobPub = X25519.scalarMultBase(bobPriv)

        val ct1 = encryptV3Payload(
            plainText = "secret message v3",
            myPrivateKey = alicePriv,
            peerPublicKey = bobPub,
            conversationId = "conv-1",
            senderId = "alice",
            messageId = "msg-1",
            secureRandom = rng,
        )
        val ct2 = encryptV3Payload(
            plainText = "secret message v3",
            myPrivateKey = alicePriv,
            peerPublicKey = bobPub,
            conversationId = "conv-1",
            senderId = "alice",
            messageId = "msg-1",
            secureRandom = rng,
        )

        assertTrue(ct1.startsWith(ChatE2EEService.PREFIX_V3))
        assertNotEquals(ct1, ct2)

        val clear = decryptV3Payload(
            encrypted = ct1,
            myPrivateKey = bobPriv,
            peerPublicKey = alicePub,
            conversationId = "conv-1",
            senderId = "alice",
            messageId = "msg-1",
        )
        assertEquals("secret message v3", clear)

        val wrongMsg = runCatching {
            decryptV3Payload(
                encrypted = ct1,
                myPrivateKey = bobPriv,
                peerPublicKey = alicePub,
                conversationId = "conv-1",
                senderId = "alice",
                messageId = "msg-2",
            )
        }
        assertTrue(wrongMsg.isFailure)

        val wrongConv = runCatching {
            decryptV3Payload(
                encrypted = ct1,
                myPrivateKey = bobPriv,
                peerPublicKey = alicePub,
                conversationId = "conv-2",
                senderId = "alice",
                messageId = "msg-1",
            )
        }
        assertTrue(wrongConv.isFailure)
    }

    @Test
    fun `wipeDecryptedMediaCache scrubs transient decrypted media files and keeps unrelated files`() {
        val dir = Files.createTempDirectory("vista-wipe-test").toFile()
        try {
            val decFile = File(dir, "dec_secret_photo.jpg").apply { writeText("sensitive photo bytes") }
            val tempDecFile = File(dir, "temp_dec_voice.m4a").apply { writeText("sensitive voice bytes") }
            val nestedDir = File(dir, "sub").apply { mkdirs() }
            val encChatFile = File(nestedDir, "enc_chat_clip.bin").apply { writeText("cipher bytes") }
            val safeFile = File(dir, "avatar_public.jpg").apply { writeText("keep me") }

            wipeDecryptedMediaCache(dir)

            assertFalse(decFile.exists())
            assertFalse(tempDecFile.exists())
            assertFalse(encChatFile.exists())
            assertTrue(safeFile.exists())
            assertEquals("keep me", safeFile.readText())
        } finally {
            dir.deleteRecursively()
        }
    }
}
