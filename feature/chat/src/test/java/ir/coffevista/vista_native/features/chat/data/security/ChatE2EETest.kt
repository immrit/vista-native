package ir.coffevista.vista_native.features.chat.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class ChatE2EETest {

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
}