package ir.coffevista.vista_native.features.chat.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end encryption service for Vista chat messages.
 * Matches Flutter E2EEncryptionService:
 *   1. X25519 ECDH for key exchange and shared secret computation.
 *   2. Per-message AES-256 key derivation via HKDF-SHA256 (salt: 16-byte random, info: "vista-e2e-msg-v1").
 *   3. AES-256-GCM authenticated encryption (envelope prefixes: VE2E1:, VE2E2:, e2ee:v1:).
 */
class ChatE2EEService(
    private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("vista_e2e_keys", Context.MODE_PRIVATE)
    }
    private val secureRandom = SecureRandom()

    /**
     * Retrieves the stored X25519 keypair for the given [userId], or generates and saves a new one.
     */
    @Synchronized
    fun getOrGenerateKeyPair(userId: String): KeyPair {
        val privB64 = prefs.getString("e2e_priv_$userId", null)
        val pubB64 = prefs.getString("e2e_pub_$userId", null)
        if (!privB64.isNullOrBlank() && !pubB64.isNullOrBlank()) {
            return try {
                KeyPair(
                    privateKey = Base64.decode(privB64, Base64.NO_WRAP),
                    publicKey = Base64.decode(pubB64, Base64.NO_WRAP),
                )
            } catch (_: Exception) {
                generateAndSaveKeyPair(userId)
            }
        }
        return generateAndSaveKeyPair(userId)
    }

    @Synchronized
    fun generateAndSaveKeyPair(userId: String): KeyPair {
        val privateKey = ByteArray(32).also { secureRandom.nextBytes(it) }
        // Clamp private key according to RFC 7748
        privateKey[0] = (privateKey[0].toInt() and 248).toByte()
        privateKey[31] = (privateKey[31].toInt() and 127).toByte()
        privateKey[31] = (privateKey[31].toInt() or 64).toByte()

        val publicKey = X25519.scalarMultBase(privateKey)
        val privB64 = Base64.encodeToString(privateKey, Base64.NO_WRAP)
        val pubB64 = Base64.encodeToString(publicKey, Base64.NO_WRAP)

        prefs.edit()
            .putString("e2e_priv_$userId", privB64)
            .putString("e2e_pub_$userId", pubB64)
            .apply()

        return KeyPair(privateKey, publicKey)
    }

    /**
     * Gets the stored peer public key for [conversationId].
     */
    fun getPeerPublicKey(conversationId: String): ByteArray? {
        val b64 = prefs.getString("e2e_peer_pub_$conversationId", null) ?: return null
        return try {
            Base64.decode(b64.trim(), Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Saves the peer public key for [conversationId].
     */
    fun savePeerPublicKey(conversationId: String, peerPublicKeyB64: String) {
        val trimmed = peerPublicKeyB64.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString("e2e_peer_pub_$conversationId", trimmed).apply()
        }
    }

    fun savePeerPublicKey(conversationId: String, peerPublicKeyBytes: ByteArray) {
        val b64 = Base64.encodeToString(peerPublicKeyBytes, Base64.NO_WRAP)
        savePeerPublicKey(conversationId, b64)
    }

    /**
     * Computes the raw 32-byte X25519 shared secret between own private key and peer's public key.
     */
    fun computeSharedSecret(myPrivateKey: ByteArray, peerPublicKey: ByteArray): ByteArray {
        require(myPrivateKey.size == 32) { "Invalid private key size: ${myPrivateKey.size}" }
        require(peerPublicKey.size == 32) { "Invalid peer public key size: ${peerPublicKey.size}" }
        return X25519.scalarMult(myPrivateKey, peerPublicKey)
    }

    /**
     * Derives a 32-byte AES key from the root shared secret using HKDF-SHA256.
     */
    fun deriveMessageKey(rootSecret: ByteArray, salt: ByteArray): ByteArray {
        val hkdfInfo = HKDF_INFO.toByteArray(Charsets.UTF_8)
        return hkdf(rootSecret, salt, hkdfInfo, 32)
    }

    /**
     * Encrypts plaintext message matching Flutter VE2E1: envelope format.
     */
    fun encryptMessage(
        plainText: String,
        sharedSecret: ByteArray,
        binding: String? = null,
    ): String {
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val messageKey = deriveMessageKey(sharedSecret, salt)

        val nonce = ByteArray(12).also { secureRandom.nextBytes(it) }
        val plainBytes = plainText.toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(messageKey, "AES"), spec)
        if (!binding.isNullOrBlank()) {
            cipher.updateAAD(binding.toByteArray(Charsets.UTF_8))
        }
        val encryptedWithTag = cipher.doFinal(plainBytes)
        val tagLength = 16
        val cipherTextLength = encryptedWithTag.size - tagLength
        val cipherText = ByteArray(cipherTextLength)
        val mac = ByteArray(tagLength)
        System.arraycopy(encryptedWithTag, 0, cipherText, 0, cipherTextLength)
        System.arraycopy(encryptedWithTag, cipherTextLength, mac, 0, tagLength)

        val totalSize = salt.size + 1 + nonce.size + 1 + mac.size + cipherText.size
        val buffer = ByteBuffer.allocate(totalSize)
        buffer.put(salt)
        buffer.put(nonce.size.toByte())
        buffer.put(nonce)
        buffer.put(mac.size.toByte())
        buffer.put(mac)
        buffer.put(cipherText)

        val prefix = if (binding == null) PREFIX_V1 else PREFIX_V2
        return prefix + Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
    }

    /**
     * Decrypts a message payload (VE2E1:, VE2E2:, e2ee:v1:, or legacy AES-GCM).
     */
    fun decryptMessage(
        encrypted: String,
        sharedSecret: ByteArray,
        binding: String? = null,
    ): String {
        val trimmed = encrypted.trim()
        if (trimmed.isEmpty()) return encrypted

        if (trimmed.startsWith(PREFIX_V2)) {
            val b64 = trimmed.removePrefix(PREFIX_V2)
            val aad = binding?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
            return runCatching { decryptEnvelope(b64, sharedSecret, aad) }
                .getOrElse {
                    runCatching { decryptEnvelope(b64, sharedSecret, ByteArray(0)) }
                        .getOrDefault(encrypted)
                }
        }

        if (trimmed.startsWith(PREFIX_V1)) {
            val b64 = trimmed.removePrefix(PREFIX_V1)
            return runCatching { decryptEnvelope(b64, sharedSecret, ByteArray(0)) }
                .getOrDefault(encrypted)
        }

        if (trimmed.startsWith(PREFIX_LEGACY_V1)) {
            val b64 = trimmed.removePrefix(PREFIX_LEGACY_V1)
            return runCatching { decryptLegacyV1(b64, sharedSecret) }
                .getOrDefault(encrypted)
        }

        return runCatching { decryptLegacy(trimmed, sharedSecret) }
            .getOrDefault(encrypted)
    }

    private fun decryptEnvelope(
        b64: String,
        sharedSecret: ByteArray,
        aad: ByteArray,
    ): String {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        var offset = 0
        require(bytes.size > SALT_LENGTH + 2 + 12 + 16) { "Envelope payload too short" }

        val salt = ByteArray(SALT_LENGTH)
        System.arraycopy(bytes, offset, salt, 0, SALT_LENGTH)
        offset += SALT_LENGTH

        val nonceLength = bytes[offset++].toInt() and 0xFF
        val nonce = ByteArray(nonceLength)
        System.arraycopy(bytes, offset, nonce, 0, nonceLength)
        offset += nonceLength

        val macLength = bytes[offset++].toInt() and 0xFF
        val mac = ByteArray(macLength)
        System.arraycopy(bytes, offset, mac, 0, macLength)
        offset += macLength

        val cipherTextLength = bytes.size - offset
        val cipherText = ByteArray(cipherTextLength)
        System.arraycopy(bytes, offset, cipherText, 0, cipherTextLength)

        val cipherInput = ByteArray(cipherText.size + mac.size)
        System.arraycopy(cipherText, 0, cipherInput, 0, cipherText.size)
        System.arraycopy(mac, 0, cipherInput, cipherText.size, mac.size)

        val messageKey = deriveMessageKey(sharedSecret, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(mac.size * 8, nonce)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(messageKey, "AES"), spec)
        if (aad.isNotEmpty()) {
            cipher.updateAAD(aad)
        }
        val decrypted = cipher.doFinal(cipherInput)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun decryptLegacyV1(b64: String, sharedSecret: ByteArray): String {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        val nonceLength = 12
        val macLength = 16
        require(bytes.size > nonceLength + macLength) { "Legacy v1 payload too short" }

        val nonce = ByteArray(nonceLength)
        System.arraycopy(bytes, 0, nonce, 0, nonceLength)

        val cipherTextLength = bytes.size - nonceLength - macLength
        val cipherText = ByteArray(cipherTextLength)
        System.arraycopy(bytes, nonceLength, cipherText, 0, cipherTextLength)

        val mac = ByteArray(macLength)
        System.arraycopy(bytes, bytes.size - macLength, mac, 0, macLength)

        val cipherInput = ByteArray(cipherText.size + mac.size)
        System.arraycopy(cipherText, 0, cipherInput, 0, cipherText.size)
        System.arraycopy(mac, 0, cipherInput, cipherText.size, mac.size)

        val cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(sharedSecret, "ChaCha20"))
        val decrypted = cipher.doFinal(cipherInput)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun decryptLegacy(b64: String, sharedSecret: ByteArray): String {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        var offset = 0
        require(bytes.size > 2 + 12 + 16) { "Legacy payload too short" }

        val nonceLength = bytes[offset++].toInt() and 0xFF
        require(nonceLength in 12..16) { "Invalid legacy nonce length" }
        val nonce = ByteArray(nonceLength)
        System.arraycopy(bytes, offset, nonce, 0, nonceLength)
        offset += nonceLength

        val macLength = bytes[offset++].toInt() and 0xFF
        require(macLength == 16) { "Invalid legacy mac length" }
        val mac = ByteArray(macLength)
        System.arraycopy(bytes, offset, mac, 0, macLength)
        offset += macLength

        val cipherTextLength = bytes.size - offset
        val cipherText = ByteArray(cipherTextLength)
        System.arraycopy(bytes, offset, cipherText, 0, cipherTextLength)

        val cipherInput = ByteArray(cipherText.size + mac.size)
        System.arraycopy(cipherText, 0, cipherInput, 0, cipherText.size)
        System.arraycopy(mac, 0, cipherInput, cipherText.size, mac.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(sharedSecret, "AES"), spec)
        val decrypted = cipher.doFinal(cipherInput)
        return String(decrypted, Charsets.UTF_8)
    }

    fun isEncryptedEnvelope(content: String): Boolean {
        val trimmed = content.trim()
        return trimmed.startsWith(PREFIX_V1) ||
            trimmed.startsWith(PREFIX_V2) ||
            trimmed.startsWith(PREFIX_LEGACY_V1)
    }

    fun computeSafetyNumber(myPublicKey: ByteArray, peerPublicKey: ByteArray): String {
        val (first, second) = if (compareBytes(myPublicKey, peerPublicKey) <= 0) {
            myPublicKey to peerPublicKey
        } else {
            peerPublicKey to myPublicKey
        }
        val combined = ByteArray(first.size + second.size)
        System.arraycopy(first, 0, combined, 0, first.size)
        System.arraycopy(second, 0, combined, first.size, second.size)

        val digest = MessageDigest.getInstance("SHA-256").digest(combined)
        return formatSafetyNumber(digest)
    }

    private fun compareBytes(a: ByteArray, b: ByteArray): Int {
        val minLen = minOf(a.size, b.size)
        for (i in 0 until minLen) {
            val diff = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (diff != 0) return diff
        }
        return a.size - b.size
    }

    private fun formatSafetyNumber(digest: ByteArray): String {
        val chunks = mutableListOf<String>()
        for (i in 0 until 6) {
            val offset = i * 4
            val value = ((digest[offset].toInt() and 0xFF) shl 24) or
                ((digest[offset + 1].toInt() and 0xFF) shl 16) or
                ((digest[offset + 2].toInt() and 0xFF) shl 8) or
                (digest[offset + 3].toInt() and 0xFF)
            val num = (value and 0x7FFFFFFF) % 100000
            chunks.add(String.format("%05d", num))
        }
        return chunks.chunked(3).joinToString("\n") { it.joinToString(" ") }
    }

    data class KeyPair(
        val privateKey: ByteArray,
        val publicKey: ByteArray,
    ) {
        val publicKeyB64: String
            get() = Base64.encodeToString(publicKey, Base64.NO_WRAP)
    }

    companion object {
        const val PREFIX_V1 = "VE2E1:"
        const val PREFIX_V2 = "VE2E2:"
        const val PREFIX_LEGACY_V1 = "e2ee:v1:"
        const val HKDF_INFO = "vista-e2e-msg-v1"
        const val SALT_LENGTH = 16

        fun messageBinding(
            conversationId: String,
            senderId: String,
            messageId: String,
            field: String = "content",
        ): String = "vista-e2ee-v2|$conversationId|$senderId|$messageId|$field"

        private fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
            val hmac = Mac.getInstance("HmacSHA256")
            val saltKey = if (salt.isEmpty()) ByteArray(32) else salt
            hmac.init(SecretKeySpec(saltKey, "HmacSHA256"))
            val prk = hmac.doFinal(ikm)

            hmac.init(SecretKeySpec(prk, "HmacSHA256"))
            val okm = ByteArray(length)
            var t = ByteArray(0)
            var offset = 0
            var counter = 1

            while (offset < length) {
                hmac.reset()
                hmac.update(t)
                hmac.update(info)
                hmac.update(counter.toByte())
                t = hmac.doFinal()

                val toCopy = minOf(t.size, length - offset)
                System.arraycopy(t, 0, okm, offset, toCopy)
                offset += toCopy
                counter++
            }
            return okm
        }
    }
}

/**
 * Pure Kotlin implementation of RFC 7748 X25519 (Curve25519 Montgomery form).
 */
object X25519 {
    private val P = BigInteger.valueOf(2).pow(255).subtract(BigInteger.valueOf(19))
    private val A24 = BigInteger.valueOf(121665)
    private val BASE_POINT = BigInteger.valueOf(9)

    fun scalarMultBase(scalar: ByteArray): ByteArray = scalarMult(scalar, BASE_POINT)

    fun scalarMult(scalar: ByteArray, uCoords: ByteArray): ByteArray {
        val u = decodeLittleEndian(uCoords)
        return scalarMult(scalar, u)
    }

    fun scalarMult(scalar: ByteArray, u: BigInteger): ByteArray {
        val k = clampScalar(scalar)
        var x1 = u
        var x2 = BigInteger.ONE
        var z2 = BigInteger.ZERO
        var x3 = u
        var z3 = BigInteger.ONE
        var swap = 0

        for (t in 254 downTo 0) {
            val kt = (k.shiftRight(t).toInt()) and 1
            swap = swap xor kt
            if (swap == 1) {
                val tempX = x2; x2 = x3; x3 = tempX
                val tempZ = z2; z2 = z3; z3 = tempZ
            }
            swap = kt

            val a = x2.add(z2).mod(P)
            val aa = a.multiply(a).mod(P)
            val b = x2.subtract(z2).mod(P)
            val bb = b.multiply(b).mod(P)
            val e = aa.subtract(bb).mod(P)
            val c = x3.add(z3).mod(P)
            val d = x3.subtract(z3).mod(P)
            val da = d.multiply(a).mod(P)
            val cb = c.multiply(b).mod(P)

            x3 = da.add(cb).mod(P).pow(2).mod(P)
            z3 = x1.multiply(da.subtract(cb).mod(P).pow(2).mod(P)).mod(P)
            x2 = aa.multiply(bb).mod(P)
            z2 = e.multiply(aa.add(A24.multiply(e).mod(P)).mod(P)).mod(P)
        }

        if (swap == 1) {
            val tempX = x2; x2 = x3; x3 = tempX
            val tempZ = z2; z2 = z3; z3 = tempZ
        }

        val result = x2.multiply(z2.modInverse(P)).mod(P)
        return encodeLittleEndian(result)
    }

    private fun clampScalar(k: ByteArray): BigInteger {
        val copy = k.copyOf(32)
        copy[0] = (copy[0].toInt() and 248).toByte()
        copy[31] = (copy[31].toInt() and 127).toByte()
        copy[31] = (copy[31].toInt() or 64).toByte()
        return decodeLittleEndian(copy)
    }

    private fun decodeLittleEndian(bytes: ByteArray): BigInteger {
        val reversed = ByteArray(bytes.size + 1)
        for (i in bytes.indices) {
            reversed[bytes.size - 1 - i + 1] = bytes[i]
        }
        return BigInteger(reversed)
    }

    private fun encodeLittleEndian(n: BigInteger): ByteArray {
        val raw = n.toByteArray()
        val result = ByteArray(32)
        var rawIdx = raw.size - 1
        var resIdx = 0
        while (rawIdx >= 0 && resIdx < 32) {
            result[resIdx++] = raw[rawIdx--]
        }
        return result
    }
}