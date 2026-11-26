package ir.coffevista.vista_native.core.network

import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BootstrapStatusClientSecurityTest {

    @Test
    fun `valid ed25519 signed enforce tls policy is accepted`() {
        val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val x509Encoded = kp.public.encoded
        val rawPubKey = x509Encoded.copyOfRange(x509Encoded.size - 32, x509Encoded.size)
        val pubB64 = Base64.getEncoder().encodeToString(rawPubKey)

        val pinHex = "a".repeat(64)
        val expiresAt = "2030-01-01T00:00:00Z"
        val canonical = "v1|2|enforce|$pinHex|$expiresAt"
        val sig = Signature.getInstance("Ed25519").run {
            initSign(kp.private)
            update(canonical.toByteArray(Charsets.UTF_8))
            sign()
        }
        val sigB64 = Base64.getEncoder().encodeToString(sig)

        val payload = Json.parseToJsonElement(
            """
            {
              "revision": 2,
              "contract_version": 1,
              "mode": "enforce",
              "fingerprints": ["$pinHex"],
              "expires_at": "$expiresAt",
              "signature": "$sigB64"
            }
            """.trimIndent(),
        ).jsonObject

        val store = TlsPolicyStore()
        val result = verifyAndApplyTlsPolicy(payload, store, pubB64)
        assertTrue(result is TlsPolicyUpdate.Accepted)
        assertEquals(TlsMode.ENFORCE, store.current().mode)
        assertEquals(pinHex, store.current().currentPinSha256)
    }

    @Test
    fun `unsigned spoofed vista-backend tls policy is rejected`() {
        val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val x509Encoded = kp.public.encoded
        val pubB64 = Base64.getEncoder().encodeToString(x509Encoded.copyOfRange(x509Encoded.size - 32, x509Encoded.size))

        val payload = Json.parseToJsonElement(
            """
            {
              "source": "vista-backend",
              "revision": 5,
              "mode": "enforce",
              "fingerprints": ["${"b".repeat(64)}"],
              "expires_at": "2030-01-01T00:00:00Z"
            }
            """.trimIndent(),
        ).jsonObject

        val store = TlsPolicyStore()
        val result = verifyAndApplyTlsPolicy(payload, store, pubB64)
        assertTrue(result is TlsPolicyUpdate.Rejected)
        assertEquals(TlsPolicyRejection.UNSIGNED, (result as TlsPolicyUpdate.Rejected).reason)
        assertEquals(TlsMode.OFF, store.current().mode)
    }

    @Test
    fun `tampered fingerprint with valid signature is rejected`() {
        val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        val x509Encoded = kp.public.encoded
        val pubB64 = Base64.getEncoder().encodeToString(x509Encoded.copyOfRange(x509Encoded.size - 32, x509Encoded.size))

        val canonical = "v1|2|enforce|${"c".repeat(64)}|2030-01-01T00:00:00Z"
        val sig = Signature.getInstance("Ed25519").run {
            initSign(kp.private)
            update(canonical.toByteArray(Charsets.UTF_8))
            sign()
        }
        val sigB64 = Base64.getEncoder().encodeToString(sig)

        val tamperedPayload = Json.parseToJsonElement(
            """
            {
              "revision": 2,
              "mode": "enforce",
              "fingerprints": ["${"d".repeat(64)}"],
              "expires_at": "2030-01-01T00:00:00Z",
              "signature": "$sigB64"
            }
            """.trimIndent(),
        ).jsonObject

        val store = TlsPolicyStore()
        val result = verifyAndApplyTlsPolicy(tamperedPayload, store, pubB64)
        assertTrue(result is TlsPolicyUpdate.Rejected)
        assertEquals(TlsPolicyRejection.UNSIGNED, (result as TlsPolicyUpdate.Rejected).reason)
    }
}
