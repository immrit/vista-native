package ir.coffevista.vista_native.features.chat.data.security

import java.security.SecureRandom
import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatE2EEServiceSecurityTest {

    @Test
    fun `x25519 matches rfc 7748 test vector and masks msb bit`() {
        val aliceScalar = hexToBytes("a546e36bf0527c9d3b16154b82465edd62144c0ac1fc5a18506a2244ba449ac4")
        val bobU = hexToBytes("e6db6867583030db3594c1a424b15f7c726624ec26b3353b10a903a6d0ab1c4c")
        val expected = hexToBytes("c3da55379de9c6908e94ea4df28d084f32eccf03491c71f754b4075577a28552")

        val shared = X25519.scalarMult(aliceScalar, bobU)
        assertArrayEquals(expected, shared)

        // RFC 7748 Section 5: most significant bit of u-coordinate must be masked out.
        val bobUWithMsbSet = bobU.copyOf().also { it[31] = (it[31].toInt() or 0x80).toByte() }
        val sharedMsb = X25519.scalarMult(aliceScalar, bobUWithMsbSet)
        assertArrayEquals(expected, sharedMsb)
    }

    @Test(expected = SecurityException::class)
    fun `v4 encryption rejects low order all zero peer public key`() {
        val alicePriv = generateRandomClampedScalar(SecureRandom())
        val zeroPeerPub = ByteArray(32)
        encryptV4Payload(
            plainText = "should fail",
            myPrivateKey = alicePriv,
            peerPublicKey = zeroPeerPub,
            conversationId = "conv-1",
            senderId = "user-a",
            messageId = "msg-1",
        )
    }

    @Test
    fun `ed25519 prekey signature binds signed prekey and rejects tampering`() {
        val kp = Ed25519Crypto.generateKeyPair(SecureRandom())
        val spkPub = Base64.getEncoder().encodeToString(ByteArray(32) { 7 })
        val msg = "VISTA-E2EE-SPK-V1:$spkPub".toByteArray(Charsets.UTF_8)
        val sig = Ed25519Crypto.sign(kp.seed, msg)

        assertTrue(Ed25519Crypto.verify(kp.publicKey, msg, sig))
        val tamperedMsg = "VISTA-E2EE-SPK-V1:${Base64.getEncoder().encodeToString(ByteArray(32) { 8 })}".toByteArray(Charsets.UTF_8)
        assertFalse(Ed25519Crypto.verify(kp.publicKey, tamperedMsg, sig))
    }

    @Test
    fun `v4 envelope supports peer decryption and sender self decryption and rejects field mismatch`() {
        val rng = SecureRandom()
        val alicePriv = generateRandomClampedScalar(rng)
        val alicePub = X25519.scalarMultBase(alicePriv)
        val bobPriv = generateRandomClampedScalar(rng)
        val bobPub = X25519.scalarMultBase(bobPriv)

        val ciphertext = encryptV4Payload(
            plainText = "hello secret world",
            myPrivateKey = alicePriv,
            peerPublicKey = bobPub,
            conversationId = "conv-secret-1",
            senderId = "alice",
            messageId = "msg-42",
            field = "reply",
            secureRandom = rng,
        )
        assertTrue(ciphertext.startsWith(ChatE2EEService.PREFIX_V4))

        val decryptedByPeer = decryptV4Payload(
            encrypted = ciphertext,
            myPrivateKey = bobPriv,
            peerPublicKey = alicePub,
            conversationId = "conv-secret-1",
            senderId = "alice",
            messageId = "msg-42",
            isMine = false,
            field = "reply",
        )
        assertEquals("hello secret world", decryptedByPeer)

        val decryptedBySender = decryptV4Payload(
            encrypted = ciphertext,
            myPrivateKey = alicePriv,
            peerPublicKey = bobPub,
            conversationId = "conv-secret-1",
            senderId = "alice",
            messageId = "msg-42",
            isMine = true,
            field = "reply",
        )
        assertEquals("hello secret world", decryptedBySender)

        val wrongFieldResult = runCatching {
            decryptV4Payload(
                encrypted = ciphertext,
                myPrivateKey = bobPriv,
                peerPublicKey = alicePub,
                conversationId = "conv-secret-1",
                senderId = "alice",
                messageId = "msg-42",
                isMine = false,
                field = "content",
            )
        }
        assertTrue(wrongFieldResult.isFailure)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
