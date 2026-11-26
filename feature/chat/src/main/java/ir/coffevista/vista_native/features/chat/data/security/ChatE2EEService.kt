package ir.coffevista.vista_native.features.chat.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import ir.coffevista.vista_native.core.security.FlutterSecureStorageReader
import ir.coffevista.vista_native.core.security.FlutterSecureValue
import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
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
    private val flutterSecureStorage = FlutterSecureStorageReader(context)

    /**
     * Retrieves the stored X25519 keypair for the given [userId], or generates and saves a new one.
     */
    @Synchronized
    fun getOrGenerateKeyPair(userId: String): KeyPair {
        val pubB64 = prefs.getString("e2e_pub_$userId", null)
        val privateKey = runCatching {
            readPrivateKey(userId) ?: importFlutterPrivateKey(userId)
        }.getOrNull()
        if (privateKey != null) {
            val derivedPublicKey = X25519.scalarMultBase(privateKey)
            val storedPublicKey = runCatching {
                Base64.decode(pubB64.orEmpty(), Base64.NO_WRAP)
            }.getOrNull()
            if (storedPublicKey == null || !storedPublicKey.contentEquals(derivedPublicKey)) {
                // The private key is the durable identity. Repair a missing or
                // stale public half instead of silently rotating that identity.
                runCatching {
                    prefs.edit()
                        .putString("e2e_pub_$userId", Base64.encodeToString(derivedPublicKey, Base64.NO_WRAP))
                        .commit()
                }
            }
            return KeyPair(privateKey, derivedPublicKey)
        }
        return generateAndSaveKeyPair(userId)
    }

    private fun importFlutterPrivateKey(userId: String): ByteArray? = runCatching {
        fun readPair(privateName: String, publicName: String): ByteArray? {
            val privateValue = runCatching { flutterSecureStorage.read(privateName) }.getOrNull()
            val publicValue = runCatching { flutterSecureStorage.read(publicName) }.getOrNull()
            if (privateValue !is FlutterSecureValue.Present) return null
            val encoded = privateValue.value
            val privateKey = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
                ?.takeIf { it.size == X25519_KEY_LENGTH } ?: return null
            if (publicValue is FlutterSecureValue.Present) {
                val publicKey = runCatching { Base64.decode(publicValue.value, Base64.DEFAULT) }.getOrNull()
                if (publicKey != null && !publicKey.contentEquals(X25519.scalarMultBase(privateKey))) {
                    return null
                }
            }
            return privateKey
        }

        val imported = readPair("e2e_priv_$userId", "e2e_pub_$userId")
            ?: readPair("e2ee_private_key", "e2ee_public_key")
            ?: return null
        runCatching { writePrivateKey(userId, imported) }
        imported
    }.getOrNull()

    @Synchronized
    fun generateAndSaveKeyPair(userId: String): KeyPair {
        val privateKey = ByteArray(32).also { secureRandom.nextBytes(it) }
        // Clamp private key according to RFC 7748
        privateKey[0] = (privateKey[0].toInt() and 248).toByte()
        privateKey[31] = (privateKey[31].toInt() and 127).toByte()
        privateKey[31] = (privateKey[31].toInt() or 64).toByte()

        val publicKey = X25519.scalarMultBase(privateKey)
        val pubB64 = Base64.encodeToString(publicKey, Base64.NO_WRAP)

        writePrivateKey(userId, privateKey)
        prefs.edit().putString("e2e_pub_$userId", pubB64).apply()

        return KeyPair(privateKey, publicKey)
    }

    /**
     * Gets the stored peer public key scoped strictly to [conversationId] (without peerId bleed).
     */
    fun getConversationPeerPublicKey(conversationId: String): ByteArray? {
        val b64 = prefs.getString("e2e_peer_pub_$conversationId", null) ?: return null
        return try {
            Base64.decode(b64.trim(), Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    fun getPeerPublicKeyTimestamp(conversationId: String): Long =
        prefs.getLong("e2e_peer_pub_ts_$conversationId", 0L)

    /**
     * Records a one-time deterministic clientId for a key-change divider in [conversationId].
     * Returns null if a notice for this exact key fingerprint was already recorded.
     */
    fun recordSecurityNoticeFingerprint(conversationId: String, keyBytes: ByteArray): String? {
        val keyHash = MessageDigest.getInstance("SHA-256")
            .digest(keyBytes)
            .take(8)
            .joinToString("") { "%02x".format(it) }
        val prefKey = "e2e_notice_${conversationId}_$keyHash"
        if (prefs.getBoolean(prefKey, false)) {
            return null
        }
        prefs.edit().putBoolean(prefKey, true).apply()
        return "e2e_key_change_${conversationId}_$keyHash"
    }

    /**
     * Gets the stored peer public key for [conversationId].
     */
    fun getPeerPublicKey(id: String): ByteArray? {
        val b64 = prefs.getString("e2e_peer_pub_$id", null)
            ?: prefs.getString("e2e_pub_$id", null)
            ?: return null
        return try {
            Base64.decode(b64.trim(), Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Saves the peer public key for [conversationId] and optional [peerId].
     */
    fun savePeerPublicKey(
        conversationId: String,
        peerPublicKeyB64: String,
        peerId: String? = null,
        frameTimestampMillis: Long = 0L,
    ) {
        val trimmed = peerPublicKeyB64.trim()
        if (trimmed.isNotBlank()) {
            val editor = prefs.edit().putString("e2e_peer_pub_$conversationId", trimmed)
            if (frameTimestampMillis > 0L) {
                val existingTs = prefs.getLong("e2e_peer_pub_ts_$conversationId", 0L)
                if (frameTimestampMillis >= existingTs) {
                    editor.putLong("e2e_peer_pub_ts_$conversationId", frameTimestampMillis)
                }
            }
            if (!peerId.isNullOrBlank()) {
                editor.putString("e2e_peer_pub_$peerId", trimmed)
                editor.putString("e2e_pub_$peerId", trimmed)
            }
            editor.apply()
        }
    }

    fun savePeerPublicKey(
        conversationId: String,
        peerPublicKeyBytes: ByteArray,
        peerId: String? = null,
        frameTimestampMillis: Long = 0L,
    ) {
        val b64 = Base64.encodeToString(peerPublicKeyBytes, Base64.NO_WRAP)
        savePeerPublicKey(conversationId, b64, peerId, frameTimestampMillis)
    }

    fun getSecretAutoDeleteSeconds(conversationId: String): Int {
        return prefs.getInt("secret_auto_delete_seconds_$conversationId", 0)
    }

    fun setSecretAutoDeleteSeconds(conversationId: String, seconds: Int) {
        if (seconds > 0) {
            prefs.edit()
                .putInt("secret_auto_delete_seconds_$conversationId", seconds)
                .putLong("secret_auto_delete_enabled_at_ms_$conversationId", System.currentTimeMillis())
                .apply()
        } else {
            prefs.edit()
                .remove("secret_auto_delete_seconds_$conversationId")
                .remove("secret_auto_delete_enabled_at_ms_$conversationId")
                .apply()
        }
    }

    /**
     * Generates a random 32-byte AES Sender Key for group end-to-end encryption.
     */
    fun generateSenderKey(): String {
        val bytes = ByteArray(32).also { secureRandom.nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun getGroupSenderKey(groupId: String, senderId: String): String? {
        if (groupId.isBlank() || senderId.isBlank()) return null
        val encKeyName = "group_sender_key_enc_${groupId}_$senderId"
        prefs.getString(encKeyName, null)?.let { encrypted ->
            val decrypted = runCatching { decryptGroupSenderKeyFromStorage(groupId, senderId, encrypted) }.getOrNull()
            if (!decrypted.isNullOrBlank()) {
                return decrypted
            }
            runCatching { prefs.edit().remove(encKeyName).apply() }
        }

        val legacyKeyName = "group_sender_key_${groupId}_$senderId"
        val legacy = prefs.getString(legacyKeyName, null)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        runCatching { saveGroupSenderKey(groupId, senderId, legacy) }
        return legacy
    }

    fun saveGroupSenderKey(groupId: String, senderId: String, senderKeyB64: String) {
        val trimmed = senderKeyB64.trim()
        if (groupId.isBlank() || senderId.isBlank() || trimmed.isBlank()) return
        val encKeyName = "group_sender_key_enc_${groupId}_$senderId"
        val legacyKeyName = "group_sender_key_${groupId}_$senderId"
        val encrypted = runCatching { encryptGroupSenderKeyForStorage(groupId, senderId, trimmed) }.getOrNull()
        if (encrypted != null) {
            prefs.edit()
                .putString(encKeyName, encrypted)
                .remove(legacyKeyName)
                .apply()
        } else {
            prefs.edit().putString(legacyKeyName, trimmed).apply()
        }
    }

    private fun groupSenderKeyAad(groupId: String, senderId: String): ByteArray =
        "vista-e2ee-group-sender-key-v1|$groupId|$senderId".toByteArray(Charsets.UTF_8)

    private fun encryptGroupSenderKeyForStorage(groupId: String, senderId: String, senderKeyB64: String): String {
        val cipher = Cipher.getInstance(KEYSTORE_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, privateKeyEncryptionKey())
        cipher.updateAAD(groupSenderKeyAad(groupId, senderId))
        val encrypted = cipher.doFinal(senderKeyB64.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(
            ByteBuffer.allocate(2 + cipher.iv.size + encrypted.size)
                .put(PRIVATE_KEY_ENVELOPE_VERSION)
                .put(cipher.iv.size.toByte())
                .put(cipher.iv)
                .put(encrypted)
                .array(),
            Base64.NO_WRAP,
        )
    }

    private fun decryptGroupSenderKeyFromStorage(groupId: String, senderId: String, encoded: String): String {
        val buffer = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP))
        require(buffer.remaining() > 2) { "Malformed encrypted sender key" }
        require(buffer.get() == PRIVATE_KEY_ENVELOPE_VERSION) { "Unsupported encrypted sender key version" }
        val ivSize = buffer.get().toInt() and 0xFF
        require(ivSize in 12..16 && buffer.remaining() > ivSize) { "Malformed encrypted sender key IV" }
        val iv = ByteArray(ivSize).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(KEYSTORE_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKeyEncryptionKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(groupSenderKeyAad(groupId, senderId))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun getOrGenerateMySenderKey(groupId: String, myUserId: String): String {
        val existing = getGroupSenderKey(groupId, myUserId)
        if (!existing.isNullOrBlank()) return existing
        val generated = generateSenderKey()
        saveGroupSenderKey(groupId, myUserId, generated)
        return generated
    }

    fun rotateMyGroupSenderKey(groupId: String, myUserId: String): String {
        val rotated = generateSenderKey()
        saveGroupSenderKey(groupId, myUserId, rotated)
        return rotated
    }

    fun clearGroupSenderKey(groupId: String, senderId: String) {
        if (groupId.isBlank() || senderId.isBlank()) return
        runCatching {
            prefs.edit()
                .remove("group_sender_key_enc_${groupId}_$senderId")
                .remove("group_sender_key_${groupId}_$senderId")
                .apply()
        }
    }

    /**
     * Encrypts a Sender Key for distribution to a specific group participant using X25519 channel.
     */
    fun encryptSenderKeyForParticipant(
        senderKeyB64: String,
        myPrivateKey: ByteArray,
        peerPublicKey: ByteArray,
    ): String {
        val sharedSecret = computeSharedSecret(myPrivateKey, peerPublicKey)
        return encryptMessage(senderKeyB64, sharedSecret)
    }

    /**
     * Decrypts a received Sender Key from a specific group participant using X25519 channel.
     */
    fun decryptSenderKeyFromParticipant(
        encryptedSenderKey: String,
        myPrivateKey: ByteArray,
        peerPublicKey: ByteArray,
    ): String {
        val sharedSecret = computeSharedSecret(myPrivateKey, peerPublicKey)
        return decryptMessage(encryptedSenderKey, sharedSecret)
    }

    /**
     * Encrypts a group message using Sender Key matching Flutter VE2E_GROUP_V1 envelope format:
     * "VE2E_GROUP_V1:" + base64( salt(16) | nonceLen(1) | nonce | macLen(1) | mac | cipherText )
     */
    fun encryptGroupMessage(plainText: String, senderKeyB64: String): String {
        val senderKeyBytes = Base64.decode(senderKeyB64, Base64.DEFAULT)
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val messageKey = deriveGroupMessageKey(senderKeyBytes, salt)

        val nonce = ByteArray(12).also { secureRandom.nextBytes(it) }
        val plainBytes = plainText.toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(messageKey, "AES"), spec)

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

        return PREFIX_GROUP_V1 + Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
    }

    /**
     * Decrypts a group message using Sender Key matching Flutter VE2E_GROUP_V1 envelope format.
     */
    @Throws(GroupE2ETamperException::class)
    fun decryptGroupMessage(cipherText: String, senderKeyB64: String): String {
        if (!cipherText.startsWith(PREFIX_GROUP_V1)) {
            return cipherText
        }
        return try {
            val b64 = cipherText.removePrefix(PREFIX_GROUP_V1)
            val raw = Base64.decode(b64, Base64.DEFAULT)
            require(raw.size > SALT_LENGTH + 2 + 12 + 16) { "Group payload too short" }

            var offset = 0
            val salt = ByteArray(SALT_LENGTH)
            System.arraycopy(raw, offset, salt, 0, SALT_LENGTH)
            offset += SALT_LENGTH

            val nonceLength = raw[offset++].toInt() and 0xFF
            val nonce = ByteArray(nonceLength)
            System.arraycopy(raw, offset, nonce, 0, nonceLength)
            offset += nonceLength

            val macLength = raw[offset++].toInt() and 0xFF
            val mac = ByteArray(macLength)
            System.arraycopy(raw, offset, mac, 0, macLength)
            offset += macLength

            val cipherTextLength = raw.size - offset
            val cipherTextBytes = ByteArray(cipherTextLength)
            System.arraycopy(raw, offset, cipherTextBytes, 0, cipherTextLength)

            val cipherInput = ByteArray(cipherTextBytes.size + mac.size)
            System.arraycopy(cipherTextBytes, 0, cipherInput, 0, cipherTextBytes.size)
            System.arraycopy(mac, 0, cipherInput, cipherTextBytes.size, mac.size)

            val senderKeyBytes = Base64.decode(senderKeyB64, Base64.DEFAULT)
            val messageKey = deriveGroupMessageKey(senderKeyBytes, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(mac.size * 8, nonce)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(messageKey, "AES"), spec)
            val decrypted = cipher.doFinal(cipherInput)
            String(decrypted, Charsets.UTF_8)
        } catch (tamper: GroupE2ETamperException) {
            throw tamper
        } catch (e: Exception) {
            throw GroupE2ETamperException("Group payload decryption failed: ${e.message}")
        }
    }

    private fun deriveGroupMessageKey(senderKey: ByteArray, salt: ByteArray): ByteArray {
        return hkdf(senderKey, salt, GROUP_HKDF_INFO.toByteArray(Charsets.UTF_8), 32)
    }

    /**
     * Private X25519 material must never be persisted as a plain SharedPreferences value.
     * Public keys remain readable by design. Existing plaintext installs are migrated only
     * after their encrypted replacement has been durably written.
     */
    private fun readPrivateKey(userId: String): ByteArray? {
        prefs.getString("e2e_priv_enc_$userId", null)?.let { encrypted ->
            val decrypted = runCatching { decryptPrivateKey(userId, encrypted) }.getOrNull()
            if (decrypted != null && decrypted.size == X25519_KEY_LENGTH) {
                return decrypted
            }
            // Corrupted or invalidated key in keystore - remove to allow regeneration
            runCatching { prefs.edit().remove("e2e_priv_enc_$userId").apply() }
        }

        val legacy = prefs.getString("e2e_priv_$userId", null) ?: return null
        val legacyKey = runCatching { Base64.decode(legacy, Base64.NO_WRAP) }.getOrNull()
            ?.takeIf { it.size == X25519_KEY_LENGTH }
        if (legacyKey != null) {
            runCatching { writePrivateKey(userId, legacyKey) }
            runCatching { prefs.edit().remove("e2e_priv_$userId").apply() }
            return legacyKey
        }
        runCatching { prefs.edit().remove("e2e_priv_$userId").apply() }
        return null
    }

    private fun writePrivateKey(userId: String, privateKey: ByteArray) {
        if (privateKey.size != X25519_KEY_LENGTH) return
        runCatching {
            val encrypted = encryptPrivateKey(userId, privateKey)
            prefs.edit()
                .putString("e2e_priv_enc_$userId", encrypted)
                .remove("e2e_priv_$userId")
                .apply()
        }
    }

    private fun encryptPrivateKey(userId: String, privateKey: ByteArray): String {
        val cipher = Cipher.getInstance(KEYSTORE_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, privateKeyEncryptionKey())
        cipher.updateAAD(privateKeyAad(userId))
        val encrypted = cipher.doFinal(privateKey)
        return Base64.encodeToString(
            ByteBuffer.allocate(2 + cipher.iv.size + encrypted.size)
                .put(PRIVATE_KEY_ENVELOPE_VERSION)
                .put(cipher.iv.size.toByte())
                .put(cipher.iv)
                .put(encrypted)
                .array(),
            Base64.NO_WRAP,
        )
    }

    private fun decryptPrivateKey(userId: String, encoded: String): ByteArray {
        val buffer = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP))
        require(buffer.remaining() > 2) { "Malformed encrypted private key" }
        require(buffer.get() == PRIVATE_KEY_ENVELOPE_VERSION) { "Unsupported encrypted private key" }
        val ivSize = buffer.get().toInt() and 0xFF
        require(ivSize in 12..16 && buffer.remaining() > ivSize) { "Malformed encrypted private key IV" }
        val iv = ByteArray(ivSize).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(KEYSTORE_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKeyEncryptionKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(privateKeyAad(userId))
        return cipher.doFinal(encrypted).also {
            require(it.size == X25519_KEY_LENGTH) { "Invalid decrypted X25519 private key" }
        }
    }

    @Synchronized
    private fun privateKeyEncryptionKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(PRIVATE_KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    PRIVATE_KEYSTORE_ALIAS,
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

    private fun privateKeyAad(userId: String): ByteArray =
        "vista-e2ee-private-key-v1|$userId".toByteArray(Charsets.UTF_8)

    /**
     * Computes the raw 32-byte X25519 shared secret between own private key and peer's public key.
     */
    fun computeSharedSecret(myPrivateKey: ByteArray, peerPublicKey: ByteArray): ByteArray {
        require(myPrivateKey.size == 32) { "Invalid private key size: ${myPrivateKey.size}" }
        require(peerPublicKey.size == 32) { "Invalid peer public key size: ${peerPublicKey.size}" }
        val secret = X25519.scalarMult(myPrivateKey, peerPublicKey)
        if (MessageDigest.isEqual(secret, ByteArray(32))) {
            throw SecurityException("Rejected low-order X25519 public key (all-zero shared secret)")
        }
        return secret
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
     * Forward-secret per-message ephemeral X25519 + static DH hybrid encryption ("VE2E3:").
     */
    fun encryptMessageV3(
        plainText: String,
        myPrivateKey: ByteArray,
        peerPublicKey: ByteArray,
        conversationId: String,
        senderId: String,
        messageId: String,
        field: String = "content",
    ): String = encryptV3Payload(
        plainText = plainText,
        myPrivateKey = myPrivateKey,
        peerPublicKey = peerPublicKey,
        conversationId = conversationId,
        senderId = senderId,
        messageId = messageId,
        field = field,
        secureRandom = secureRandom,
    )

    /**
     * Decrypts a forward-secret "VE2E3:" payload. Returns [encrypted] on failure.
     */
    fun decryptMessageV3(
        encrypted: String,
        myPrivateKey: ByteArray,
        peerPublicKey: ByteArray,
        conversationId: String,
        senderId: String,
        messageId: String,
        field: String = "content",
    ): String = runCatching {
        decryptV3Payload(
            encrypted = encrypted,
            myPrivateKey = myPrivateKey,
            peerPublicKey = peerPublicKey,
            conversationId = conversationId,
            senderId = senderId,
            messageId = messageId,
            field = field,
        )
    }.getOrDefault(encrypted)

    /**
     * Encrypts a forward-secret hybrid payload ("VE2E4:") with per-message ephemeral X25519
     * AND dual-wrapped content key so BOTH recipient and sender can decrypt across history syncs.
     */
    fun encryptMessageV4(
        plainText: String,
        myPrivateKey: ByteArray,
        peerPublicKey: ByteArray,
        conversationId: String,
        senderId: String,
        messageId: String,
        field: String = "content",
    ): String = encryptV4Payload(
        plainText = plainText,
        myPrivateKey = myPrivateKey,
        peerPublicKey = peerPublicKey,
        conversationId = conversationId,
        senderId = senderId,
        messageId = messageId,
        field = field,
        secureRandom = secureRandom,
    )

    fun decryptMessageV4(
        encrypted: String,
        myPrivateKey: ByteArray,
        peerPublicKey: ByteArray,
        conversationId: String,
        senderId: String,
        messageId: String,
        isMine: Boolean,
        field: String = "content",
    ): String = runCatching {
        decryptV4Payload(
            encrypted = encrypted,
            myPrivateKey = myPrivateKey,
            peerPublicKey = peerPublicKey,
            conversationId = conversationId,
            senderId = senderId,
            messageId = messageId,
            isMine = isMine,
            field = field,
        )
    }.getOrDefault(encrypted)

    data class GeneratedPreKeyBundle(
        val identityKey: String,
        val signingKey: String,
        val signedPreKeyId: Long,
        val signedPreKey: String,
        val signedPreKeySig: String,
        val oneTimePreKeys: List<Pair<Long, String>>,
    )

    @Synchronized
    fun getOrGenerateSigningKeyPair(userId: String): Ed25519Crypto.SigningKeyPair {
        val pubB64 = prefs.getString("e2e_sig_pub_$userId", null)
        val encSeed = prefs.getString("e2e_sig_priv_enc_$userId", null)
        if (!encSeed.isNullOrBlank()) {
            val seed = runCatching { decryptScopedSecret("sig|$userId", encSeed) }
                .getOrNull()
                ?.takeIf { it.size == 32 }
            if (seed != null) {
                val derivedPub = Ed25519Crypto.derivePublicKey(seed)
                val storedPub = runCatching { Base64.decode(pubB64.orEmpty(), Base64.NO_WRAP) }.getOrNull()
                if (storedPub == null || !storedPub.contentEquals(derivedPub)) {
                    prefs.edit().putString("e2e_sig_pub_$userId", Base64.encodeToString(derivedPub, Base64.NO_WRAP)).apply()
                }
                return Ed25519Crypto.SigningKeyPair(seed = seed, publicKey = derivedPub)
            }
        }
        val kp = Ed25519Crypto.generateKeyPair(secureRandom)
        val encrypted = runCatching { encryptScopedSecret("sig|$userId", kp.seed) }.getOrNull()
        if (encrypted != null) {
            prefs.edit()
                .putString("e2e_sig_priv_enc_$userId", encrypted)
                .putString("e2e_sig_pub_$userId", Base64.encodeToString(kp.publicKey, Base64.NO_WRAP))
                .apply()
        }
        return kp
    }

    fun getSignedPreKeyPrivate(userId: String, spkId: Long): ByteArray? {
        val enc = prefs.getString("e2e_spk_priv_enc_${userId}_$spkId", null) ?: return null
        return runCatching { decryptScopedSecret("spk|$userId|$spkId", enc) }
            .getOrNull()
            ?.takeIf { it.size == X25519_KEY_LENGTH }
    }

    fun consumeOneTimePreKeyPrivate(userId: String, keyId: Long): ByteArray? {
        val prefKey = "e2e_otpk_priv_enc_${userId}_$keyId"
        val enc = prefs.getString(prefKey, null) ?: return null
        val decrypted = runCatching { decryptScopedSecret("otpk|$userId|$keyId", enc) }
            .getOrNull()
            ?.takeIf { it.size == X25519_KEY_LENGTH }
        prefs.edit().remove(prefKey).apply()
        return decrypted
    }

    fun verifyAndPinPeerPreKeyBundle(
        accountId: String,
        peerUserId: String,
        identityKeyB64: String,
        signingKeyB64: String,
        signedPreKeyB64: String,
        signedPreKeySigB64: String,
    ): Boolean {
        val cleanSigKey = signingKeyB64.trim()
        if (cleanSigKey.isEmpty()) return true // Legacy peer without Ed25519 signing key yet
        val idBytes = runCatching { Base64.decode(identityKeyB64.trim(), Base64.DEFAULT) }.getOrNull()
            ?.takeIf { it.size == X25519_KEY_LENGTH } ?: return false
        val spkBytes = runCatching { Base64.decode(signedPreKeyB64.trim(), Base64.DEFAULT) }.getOrNull()
            ?.takeIf { it.size == X25519_KEY_LENGTH } ?: return false
        val pubBytes = runCatching { Base64.decode(cleanSigKey, Base64.DEFAULT) }.getOrNull()
            ?.takeIf { it.size == 32 } ?: return false
        val sigBytes = runCatching { Base64.decode(signedPreKeySigB64.trim(), Base64.DEFAULT) }.getOrNull()
            ?.takeIf { it.size == 64 } ?: return false

        val signedMsg = ByteArray("VISTA-E2EE-SPK-V1:".length + idBytes.size + spkBytes.size)
        val prefix = "VISTA-E2EE-SPK-V1:".toByteArray(Charsets.UTF_8)
        System.arraycopy(prefix, 0, signedMsg, 0, prefix.size)
        System.arraycopy(idBytes, 0, signedMsg, prefix.size, idBytes.size)
        System.arraycopy(spkBytes, 0, signedMsg, prefix.size + idBytes.size, spkBytes.size)
        if (!Ed25519Crypto.verify(pubBytes, signedMsg, sigBytes)) {
            return false
        }
        val tofuKey = "e2e_peer_sig_tofu_${accountId}_$peerUserId"
        val canonicalPin = Base64.encodeToString(pubBytes, Base64.NO_WRAP)
        prefs.edit().putString(tofuKey, canonicalPin).apply()
        return true
    }

    fun clearAccountKeys(accountId: String) {
        if (accountId.isBlank()) return
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.contains(accountId)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    private fun encryptScopedSecret(scope: String, secret: ByteArray): String {
        val cipher = Cipher.getInstance(KEYSTORE_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, privateKeyEncryptionKey())
        cipher.updateAAD("vista-e2ee-scoped-v1|$scope".toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(secret)
        return Base64.encodeToString(
            ByteBuffer.allocate(2 + cipher.iv.size + encrypted.size)
                .put(PRIVATE_KEY_ENVELOPE_VERSION)
                .put(cipher.iv.size.toByte())
                .put(cipher.iv)
                .put(encrypted)
                .array(),
            Base64.NO_WRAP,
        )
    }

    private fun decryptScopedSecret(scope: String, encoded: String): ByteArray {
        val buffer = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP))
        require(buffer.remaining() > 2) { "Malformed encrypted secret" }
        require(buffer.get() == PRIVATE_KEY_ENVELOPE_VERSION) { "Unsupported envelope version" }
        val ivSize = buffer.get().toInt() and 0xFF
        require(ivSize in 12..16 && buffer.remaining() > ivSize) { "Malformed IV" }
        val iv = ByteArray(ivSize).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(KEYSTORE_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKeyEncryptionKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD("vista-e2ee-scoped-v1|$scope".toByteArray(Charsets.UTF_8))
        return cipher.doFinal(encrypted)
    }

    fun generatePreKeyBundle(userId: String, oneTimeCount: Int = 20): GeneratedPreKeyBundle {
        val identity = getOrGenerateKeyPair(userId)
        val signingKeyPair = getOrGenerateSigningKeyPair(userId)
        val spkId = System.currentTimeMillis() / 1000L
        val spkPriv = generateRandomClampedScalar(secureRandom)
        val spkPub = X25519.scalarMultBase(spkPriv)
        runCatching {
            encryptScopedSecret("spk|$userId|$spkId", spkPriv)
        }.getOrNull()?.let { encSpk ->
            prefs.edit()
                .putString("e2e_spk_priv_enc_${userId}_$spkId", encSpk)
                .putLong("e2e_active_spk_id_$userId", spkId)
                .apply()
        }
        spkPriv.fill(0)

        val prefix = "VISTA-E2EE-SPK-V1:".toByteArray(Charsets.UTF_8)
        val signedPayload = ByteArray(prefix.size + identity.publicKey.size + spkPub.size)
        System.arraycopy(prefix, 0, signedPayload, 0, prefix.size)
        System.arraycopy(identity.publicKey, 0, signedPayload, prefix.size, identity.publicKey.size)
        System.arraycopy(spkPub, 0, signedPayload, prefix.size + identity.publicKey.size, spkPub.size)
        val edSig = Ed25519Crypto.sign(signingKeyPair.seed, signedPayload)

        val baseOtId = spkId * 100L
        val otEditor = prefs.edit()
        val otKeys = (1..oneTimeCount).map { idx ->
            val keyId = baseOtId + idx
            val otPriv = generateRandomClampedScalar(secureRandom)
            val otPub = X25519.scalarMultBase(otPriv)
            runCatching { encryptScopedSecret("otpk|$userId|$keyId", otPriv) }.getOrNull()?.let { encOt ->
                otEditor.putString("e2e_otpk_priv_enc_${userId}_$keyId", encOt)
            }
            otPriv.fill(0)
            keyId to Base64.encodeToString(otPub, Base64.NO_WRAP)
        }
        otEditor.apply()
        return GeneratedPreKeyBundle(
            identityKey = identity.publicKeyB64,
            signingKey = Base64.encodeToString(signingKeyPair.publicKey, Base64.NO_WRAP),
            signedPreKeyId = spkId,
            signedPreKey = Base64.encodeToString(spkPub, Base64.NO_WRAP),
            signedPreKeySig = Base64.encodeToString(edSig, Base64.NO_WRAP),
            oneTimePreKeys = otKeys,
        )
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
                .getOrDefault(encrypted)
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
        return trimmed.startsWith(PREFIX_V4) ||
            trimmed.startsWith(PREFIX_V3) ||
            trimmed.startsWith(PREFIX_V1) ||
            trimmed.startsWith(PREFIX_V2) ||
            trimmed.startsWith(PREFIX_LEGACY_V1) ||
            trimmed.startsWith(PREFIX_GROUP_V1)
    }

    /**
     * Historical Vista clients sometimes sent the legacy AES-GCM payload
     * without an envelope prefix. Only accept the exact legacy binary shape;
     * a generic Base64-looking user message must not be treated as ciphertext.
     */
    fun isEncryptedPayload(content: String): Boolean {
        if (isEncryptedEnvelope(content)) return true
        val decoded = runCatching {
            Base64.decode(content.trim(), Base64.DEFAULT)
        }.getOrNull() ?: return false
        return hasLegacyEncryptedPayloadShape(decoded)
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

    /**
     * Encrypts a file using AES-256-GCM authenticated encryption.
     * Output format: 4-byte magic "VE2F" + salt (16 bytes) + nonce (12 bytes) + ciphertext + GCM tag (16 bytes).
     */
    fun encryptFile(
        inputFile: File,
        outputFile: File,
        sharedSecret: ByteArray,
        binding: String? = null,
    ) {
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val fileKey = deriveMessageKey(sharedSecret, salt)
        val nonce = ByteArray(12).also { secureRandom.nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(fileKey, "AES"), spec)
        if (!binding.isNullOrBlank()) {
            cipher.updateAAD(binding.toByteArray(Charsets.UTF_8))
        }

        outputFile.outputStream().buffered().use { output ->
            output.write(FILE_ENVELOPE_MAGIC)
            output.write(salt)
            output.write(nonce)
            val buffer = ByteArray(64 * 1024)
            inputFile.inputStream().buffered().use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    val chunk = cipher.update(buffer, 0, read)
                    if (chunk != null && chunk.isNotEmpty()) {
                        output.write(chunk)
                    }
                }
            }
            val finalChunk = cipher.doFinal()
            if (finalChunk != null && finalChunk.isNotEmpty()) {
                output.write(finalChunk)
            }
            output.flush()
        }
    }

    /**
     * Decrypts an encrypted file using AES-256-GCM.
     * Returns true if file was encrypted with VE2F magic and decrypted successfully,
     * or false if file was not encrypted.
     */
    fun decryptFile(
        inputFile: File,
        outputFile: File,
        sharedSecret: ByteArray,
        binding: String? = null,
    ): Boolean {
        if (!isEncryptedFile(inputFile)) return false
        inputFile.inputStream().buffered().use { input ->
            val magic = ByteArray(FILE_ENVELOPE_MAGIC.size)
            if (input.read(magic) != FILE_ENVELOPE_MAGIC.size || !magic.contentEquals(FILE_ENVELOPE_MAGIC)) {
                return false
            }
            val salt = ByteArray(SALT_LENGTH)
            if (input.read(salt) != SALT_LENGTH) return false
            val nonce = ByteArray(12)
            if (input.read(nonce) != 12) return false

            val fileKey = deriveMessageKey(sharedSecret, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, nonce)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(fileKey, "AES"), spec)
            if (!binding.isNullOrBlank()) {
                cipher.updateAAD(binding.toByteArray(Charsets.UTF_8))
            }

            val tempOutputFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
            try {
                tempOutputFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        val chunk = cipher.update(buffer, 0, read)
                        if (chunk != null && chunk.isNotEmpty()) {
                            output.write(chunk)
                        }
                    }
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null && finalChunk.isNotEmpty()) {
                        output.write(finalChunk)
                    }
                    output.flush()
                }
                if (outputFile.exists()) outputFile.delete()
                if (!tempOutputFile.renameTo(outputFile)) {
                    tempOutputFile.copyTo(outputFile, overwrite = true)
                    tempOutputFile.delete()
                }
                return true
            } catch (e: Exception) {
                runCatching { tempOutputFile.delete() }
                runCatching { outputFile.delete() }
                throw e
            }
        }
    }

    fun isEncryptedFile(file: File): Boolean {
        if (!file.exists() || file.length() < FILE_ENVELOPE_MAGIC.size + SALT_LENGTH + 12 + 16) return false
        return runCatching {
            file.inputStream().use { input ->
                val header = ByteArray(FILE_ENVELOPE_MAGIC.size)
                input.read(header) == FILE_ENVELOPE_MAGIC.size && header.contentEquals(FILE_ENVELOPE_MAGIC)
            }
        }.getOrDefault(false)
    }

    data class KeyPair(
        val privateKey: ByteArray,
        val publicKey: ByteArray,
    ) {
        val publicKeyB64: String
            get() = Base64.encodeToString(publicKey, Base64.NO_WRAP)
    }

    companion object {
        val FILE_ENVELOPE_MAGIC = byteArrayOf(0x56, 0x45, 0x32, 0x46) // "VE2F"
        const val PREFIX_V1 = "VE2E1:"
        const val PREFIX_V2 = "VE2E2:"
        const val PREFIX_V3 = "VE2E3:"
        const val PREFIX_V4 = "VE2E4:"
        const val PREFIX_LEGACY_V1 = "e2ee:v1:"
        const val PREFIX_GROUP_V1 = "VE2E_GROUP_V1:"
        const val HKDF_INFO = "vista-e2e-msg-v1"
        const val HKDF_INFO_V3 = "vista-e2e-msg-v3"
        const val HKDF_INFO_V4_PEER = "vista-e2e-msg-v4-peer"
        const val HKDF_INFO_V4_SELF = "vista-e2e-msg-v4-self"
        const val GROUP_HKDF_INFO = "vista-e2e-group-v1"
        const val SALT_LENGTH = 16
        private const val X25519_KEY_LENGTH = 32
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val PRIVATE_KEYSTORE_ALIAS = "vista_native_e2ee_private_key_v1"
        private const val KEYSTORE_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val PRIVATE_KEY_ENVELOPE_VERSION: Byte = 1

        fun messageBinding(
            conversationId: String,
            senderId: String,
            messageId: String,
            field: String = "content",
        ): String = "vista-e2ee-v2|$conversationId|$senderId|$messageId|$field"

        fun messageBindingV3(
            conversationId: String,
            senderId: String,
            messageId: String,
            ephemeralPubB64: String,
            field: String = "content",
        ): String = "vista-e2ee-v3|$conversationId|$senderId|$messageId|$field|$ephemeralPubB64"

        internal fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
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

internal fun hasLegacyEncryptedPayloadShape(bytes: ByteArray): Boolean {
    if (bytes.size <= 1 + 12 + 1 + 16) return false
    val nonceLength = bytes[0].toInt() and 0xFF
    if (nonceLength !in 12..16) return false
    val macLengthIndex = 1 + nonceLength
    if (macLengthIndex >= bytes.lastIndex) return false
    val macLength = bytes[macLengthIndex].toInt() and 0xFF
    if (macLength != 16) return false
    val cipherTextOffset = macLengthIndex + 1 + macLength
    return cipherTextOffset < bytes.size
}

internal fun generateRandomClampedScalar(secureRandom: SecureRandom = SecureRandom()): ByteArray {
    val scalar = ByteArray(32).also { secureRandom.nextBytes(it) }
    scalar[0] = (scalar[0].toInt() and 248).toByte()
    scalar[31] = (scalar[31].toInt() and 127 or 64).toByte()
    return scalar
}

internal fun encryptV3Payload(
    plainText: String,
    myPrivateKey: ByteArray,
    peerPublicKey: ByteArray,
    conversationId: String,
    senderId: String,
    messageId: String,
    field: String = "content",
    secureRandom: SecureRandom = SecureRandom(),
): String {
    require(myPrivateKey.size == 32 && peerPublicKey.size == 32) { "Invalid X25519 key size" }
    val ephPriv = generateRandomClampedScalar(secureRandom)
    val ephPub = X25519.scalarMultBase(ephPriv)
    val ephDh = X25519.scalarMult(ephPriv, peerPublicKey)
    ephPriv.fill(0)
    if (MessageDigest.isEqual(ephDh, ByteArray(32))) {
        throw SecurityException("Rejected low-order X25519 peer key")
    }
    val staticDh = X25519.scalarMult(myPrivateKey, peerPublicKey)
    if (MessageDigest.isEqual(staticDh, ByteArray(32))) {
        throw SecurityException("Rejected low-order X25519 static shared secret")
    }

    val ikm = ByteArray(64)
    System.arraycopy(ephDh, 0, ikm, 0, 32)
    System.arraycopy(staticDh, 0, ikm, 32, 32)
    ephDh.fill(0)
    staticDh.fill(0)

    val salt = ByteArray(ChatE2EEService.SALT_LENGTH).also { secureRandom.nextBytes(it) }
    val nonce = ByteArray(12).also { secureRandom.nextBytes(it) }
    val msgKey = ChatE2EEService.hkdf(
        ikm = ikm,
        salt = salt,
        info = ChatE2EEService.HKDF_INFO_V3.toByteArray(Charsets.UTF_8),
        length = 32,
    )
    ikm.fill(0)

    val ephPubB64 = java.util.Base64.getEncoder().encodeToString(ephPub)
    val aad = ChatE2EEService.messageBindingV3(conversationId, senderId, messageId, ephPubB64, field)
        .toByteArray(Charsets.UTF_8)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(msgKey, "AES"), GCMParameterSpec(128, nonce))
    cipher.updateAAD(aad)
    val encryptedWithTag = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    msgKey.fill(0)

    val out = ByteBuffer.allocate(1 + 32 + ChatE2EEService.SALT_LENGTH + 12 + encryptedWithTag.size)
        .put(0x03.toByte())
        .put(ephPub)
        .put(salt)
        .put(nonce)
        .put(encryptedWithTag)
        .array()

    return ChatE2EEService.PREFIX_V3 + java.util.Base64.getEncoder().encodeToString(out)
}

internal fun decryptV3Payload(
    encrypted: String,
    myPrivateKey: ByteArray,
    peerPublicKey: ByteArray,
    conversationId: String,
    senderId: String,
    messageId: String,
    field: String = "content",
): String {
    val trimmed = encrypted.trim()
    require(trimmed.startsWith(ChatE2EEService.PREFIX_V3)) { "Not a VE2E3 payload" }
    require(myPrivateKey.size == 32 && peerPublicKey.size == 32) { "Invalid X25519 key size" }

    val raw = java.util.Base64.getDecoder().decode(trimmed.removePrefix(ChatE2EEService.PREFIX_V3))
    require(raw.size > 1 + 32 + ChatE2EEService.SALT_LENGTH + 12 + 16 && raw[0] == 0x03.toByte()) {
        "Malformed VE2E3 envelope"
    }

    var offset = 1
    val ephPub = raw.copyOfRange(offset, offset + 32)
    offset += 32
    val salt = raw.copyOfRange(offset, offset + ChatE2EEService.SALT_LENGTH)
    offset += ChatE2EEService.SALT_LENGTH
    val nonce = raw.copyOfRange(offset, offset + 12)
    offset += 12
    val cipherAndTag = raw.copyOfRange(offset, raw.size)

    val ephDh = X25519.scalarMult(myPrivateKey, ephPub)
    val staticDh = X25519.scalarMult(myPrivateKey, peerPublicKey)
    if (MessageDigest.isEqual(ephDh, ByteArray(32)) || MessageDigest.isEqual(staticDh, ByteArray(32))) {
        ephDh.fill(0)
        staticDh.fill(0)
        throw SecurityException("Rejected low-order X25519 public key")
    }
    val ikm = ByteArray(64)
    System.arraycopy(ephDh, 0, ikm, 0, 32)
    System.arraycopy(staticDh, 0, ikm, 32, 32)
    ephDh.fill(0)
    staticDh.fill(0)

    val msgKey = ChatE2EEService.hkdf(
        ikm = ikm,
        salt = salt,
        info = ChatE2EEService.HKDF_INFO_V3.toByteArray(Charsets.UTF_8),
        length = 32,
    )
    ikm.fill(0)

    val ephPubB64 = java.util.Base64.getEncoder().encodeToString(ephPub)
    val aad = ChatE2EEService.messageBindingV3(conversationId, senderId, messageId, ephPubB64, field)
        .toByteArray(Charsets.UTF_8)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(msgKey, "AES"), GCMParameterSpec(128, nonce))
    cipher.updateAAD(aad)
    val clear = cipher.doFinal(cipherAndTag)
    msgKey.fill(0)
    return String(clear, Charsets.UTF_8)
}

internal fun encryptV4Payload(
    plainText: String,
    myPrivateKey: ByteArray,
    peerPublicKey: ByteArray,
    conversationId: String,
    senderId: String,
    messageId: String,
    field: String = "content",
    secureRandom: SecureRandom = SecureRandom(),
): String {
    require(myPrivateKey.size == 32 && peerPublicKey.size == 32) { "Invalid X25519 key size" }
    val myPublicKey = X25519.scalarMultBase(myPrivateKey)
    val ephPriv = generateRandomClampedScalar(secureRandom)
    val ephPub = X25519.scalarMultBase(ephPriv)
    val ephDh = X25519.scalarMult(ephPriv, peerPublicKey)
    ephPriv.fill(0)
    val staticDh = X25519.scalarMult(myPrivateKey, peerPublicKey)
    if (MessageDigest.isEqual(ephDh, ByteArray(32)) || MessageDigest.isEqual(staticDh, ByteArray(32))) {
        ephDh.fill(0)
        staticDh.fill(0)
        throw SecurityException("Rejected low-order X25519 peer public key")
    }
    val peerIkm = ByteArray(64)
    System.arraycopy(ephDh, 0, peerIkm, 0, 32)
    System.arraycopy(staticDh, 0, peerIkm, 32, 32)
    ephDh.fill(0)
    staticDh.fill(0)

    val selfDh = X25519.scalarMult(myPrivateKey, myPublicKey)
    val contentKey = ByteArray(32).also { secureRandom.nextBytes(it) }
    val salt = ByteArray(ChatE2EEService.SALT_LENGTH).also { secureRandom.nextBytes(it) }
    val msgNonce = ByteArray(12).also { secureRandom.nextBytes(it) }
    val peerWrapNonce = ByteArray(12).also { secureRandom.nextBytes(it) }
    val selfWrapNonce = ByteArray(12).also { secureRandom.nextBytes(it) }

    val peerWrapKey = ChatE2EEService.hkdf(peerIkm, salt, ChatE2EEService.HKDF_INFO_V4_PEER.toByteArray(Charsets.UTF_8), 32)
    peerIkm.fill(0)
    val selfWrapKey = ChatE2EEService.hkdf(selfDh, salt, ChatE2EEService.HKDF_INFO_V4_SELF.toByteArray(Charsets.UTF_8), 32)
    selfDh.fill(0)

    val ephPubB64 = java.util.Base64.getEncoder().encodeToString(ephPub)
    val aad = ChatE2EEService.messageBindingV3(conversationId, senderId, messageId, ephPubB64, field)
        .toByteArray(Charsets.UTF_8)

    val peerWrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
    peerWrapCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(peerWrapKey, "AES"), GCMParameterSpec(128, peerWrapNonce))
    peerWrapCipher.updateAAD(aad)
    val peerWrappedKey = peerWrapCipher.doFinal(contentKey)
    peerWrapKey.fill(0)

    val selfWrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
    selfWrapCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(selfWrapKey, "AES"), GCMParameterSpec(128, selfWrapNonce))
    selfWrapCipher.updateAAD(aad)
    val selfWrappedKey = selfWrapCipher.doFinal(contentKey)
    selfWrapKey.fill(0)

    val bodyCipher = Cipher.getInstance("AES/GCM/NoPadding")
    bodyCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(contentKey, "AES"), GCMParameterSpec(128, msgNonce))
    bodyCipher.updateAAD(aad)
    val bodyCipherAndTag = bodyCipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    contentKey.fill(0)

    // Wire format: ver(1=0x04) | ephPub(32) | salt(16) | peerWrapNonce(12) | peerWrapped(48) | selfWrapNonce(12) | selfWrapped(48) | msgNonce(12) | bodyCipherAndTag
    val out = ByteBuffer.allocate(1 + 32 + ChatE2EEService.SALT_LENGTH + 12 + peerWrappedKey.size + 12 + selfWrappedKey.size + 12 + bodyCipherAndTag.size)
        .put(0x04.toByte())
        .put(ephPub)
        .put(salt)
        .put(peerWrapNonce)
        .put(peerWrappedKey)
        .put(selfWrapNonce)
        .put(selfWrappedKey)
        .put(msgNonce)
        .put(bodyCipherAndTag)
        .array()
    return ChatE2EEService.PREFIX_V4 + java.util.Base64.getEncoder().encodeToString(out)
}

internal fun decryptV4Payload(
    encrypted: String,
    myPrivateKey: ByteArray,
    peerPublicKey: ByteArray,
    conversationId: String,
    senderId: String,
    messageId: String,
    isMine: Boolean,
    field: String = "content",
): String {
    val trimmed = encrypted.trim()
    require(trimmed.startsWith(ChatE2EEService.PREFIX_V4)) { "Not a VE2E4 payload" }
    require(myPrivateKey.size == 32 && peerPublicKey.size == 32) { "Invalid X25519 key size" }
    val raw = java.util.Base64.getDecoder().decode(trimmed.removePrefix(ChatE2EEService.PREFIX_V4))
    val wrappedSize = 32 + 16 // AES-256 key + GCM tag
    val minSize = 1 + 32 + ChatE2EEService.SALT_LENGTH + 12 + wrappedSize + 12 + wrappedSize + 12 + 16
    require(raw.size > minSize && raw[0] == 0x04.toByte()) { "Malformed VE2E4 envelope" }

    var offset = 1
    val ephPub = raw.copyOfRange(offset, offset + 32); offset += 32
    val salt = raw.copyOfRange(offset, offset + ChatE2EEService.SALT_LENGTH); offset += ChatE2EEService.SALT_LENGTH
    val peerWrapNonce = raw.copyOfRange(offset, offset + 12); offset += 12
    val peerWrappedKey = raw.copyOfRange(offset, offset + wrappedSize); offset += wrappedSize
    val selfWrapNonce = raw.copyOfRange(offset, offset + 12); offset += 12
    val selfWrappedKey = raw.copyOfRange(offset, offset + wrappedSize); offset += wrappedSize
    val msgNonce = raw.copyOfRange(offset, offset + 12); offset += 12
    val bodyCipherAndTag = raw.copyOfRange(offset, raw.size)

    val ephPubB64 = java.util.Base64.getEncoder().encodeToString(ephPub)
    val aad = ChatE2EEService.messageBindingV3(conversationId, senderId, messageId, ephPubB64, field)
        .toByteArray(Charsets.UTF_8)

    val contentKey: ByteArray = if (isMine) {
        val myPub = X25519.scalarMultBase(myPrivateKey)
        val selfDh = X25519.scalarMult(myPrivateKey, myPub)
        val selfWrapKey = ChatE2EEService.hkdf(selfDh, salt, ChatE2EEService.HKDF_INFO_V4_SELF.toByteArray(Charsets.UTF_8), 32)
        selfDh.fill(0)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(selfWrapKey, "AES"), GCMParameterSpec(128, selfWrapNonce))
        cipher.updateAAD(aad)
        val unwrapped = cipher.doFinal(selfWrappedKey)
        selfWrapKey.fill(0)
        unwrapped
    } else {
        val ephDh = X25519.scalarMult(myPrivateKey, ephPub)
        val staticDh = X25519.scalarMult(myPrivateKey, peerPublicKey)
        if (MessageDigest.isEqual(ephDh, ByteArray(32)) || MessageDigest.isEqual(staticDh, ByteArray(32))) {
            ephDh.fill(0)
            staticDh.fill(0)
            throw SecurityException("Rejected low-order X25519 public key")
        }
        val peerIkm = ByteArray(64)
        System.arraycopy(ephDh, 0, peerIkm, 0, 32)
        System.arraycopy(staticDh, 0, peerIkm, 32, 32)
        ephDh.fill(0)
        staticDh.fill(0)
        val peerWrapKey = ChatE2EEService.hkdf(peerIkm, salt, ChatE2EEService.HKDF_INFO_V4_PEER.toByteArray(Charsets.UTF_8), 32)
        peerIkm.fill(0)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(peerWrapKey, "AES"), GCMParameterSpec(128, peerWrapNonce))
        cipher.updateAAD(aad)
        val unwrapped = cipher.doFinal(peerWrappedKey)
        peerWrapKey.fill(0)
        unwrapped
    }

    val bodyCipher = Cipher.getInstance("AES/GCM/NoPadding")
    bodyCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(contentKey, "AES"), GCMParameterSpec(128, msgNonce))
    bodyCipher.updateAAD(aad)
    val clear = bodyCipher.doFinal(bodyCipherAndTag)
    contentKey.fill(0)
    return String(clear, Charsets.UTF_8)
}

/**
 * Overwrites and deletes transient plaintext decrypted media caches (`dec_*`, `temp_dec_*`,
 * `raw_chat_*`, `enc_chat_*`) so decrypted secret attachments do not linger on disk.
 */
internal fun wipeDecryptedMediaCache(rootDir: File?) {
    if (rootDir == null || !rootDir.exists()) return
    rootDir.walkTopDown().forEach { file ->
        if (!file.isFile) return@forEach
        val name = file.name
        if (
            name.startsWith("dec_") ||
            name.startsWith("temp_dec_") ||
            name.startsWith("raw_chat_") ||
            name.startsWith("enc_chat_")
        ) {
            runCatching {
                val len = file.length()
                if (len in 1L..(32L * 1024L * 1024L)) {
                    java.io.RandomAccessFile(file, "rw").use { raf ->
                        val zeros = ByteArray(minOf(len.toInt(), 8192))
                        var remaining = len
                        while (remaining > 0) {
                            val chunk = minOf(remaining.toInt(), zeros.size)
                            raf.write(zeros, 0, chunk)
                            remaining -= chunk
                        }
                        raf.fd.sync()
                    }
                }
            }
            runCatching { file.delete() }
        }
    }
}

/**
 * Constant-time pure Kotlin RFC 7748 X25519 (Curve25519 Montgomery ladder)
 * over 16-limb signed radix-2^16 field elements with branchless cswap.
 */
object X25519 {
    private val BASE_U = ByteArray(32).apply { this[0] = 9 }

    fun scalarMultBase(scalar: ByteArray): ByteArray = scalarMult(scalar, BASE_U)

    fun scalarMult(scalar: ByteArray, u: BigInteger): ByteArray =
        scalarMult(scalar, encodeBigInt32(u))

    fun scalarMult(scalar: ByteArray, uCoords: ByteArray): ByteArray {
        val k = clampScalarBytes(scalar)
        val x1 = unpack25519(uCoords)
        val x = LongArray(16)
        val a = LongArray(16)
        val b = LongArray(16)
        val c = LongArray(16)
        val d = LongArray(16)
        val e = LongArray(16)
        val f = LongArray(16)

        for (i in 0 until 16) {
            b[i] = x1[i]
            a[i] = 0L
            c[i] = 0L
            d[i] = 0L
        }
        a[0] = 1L
        d[0] = 1L

        for (pos in 254 downTo 0) {
            val r = (k[pos ushr 3].toInt() ushr (pos and 7)) and 1
            sel25519(a, b, r)
            sel25519(c, d, r)
            add(e, a, c)
            sub(a, a, c)
            add(c, b, d)
            sub(b, b, d)
            mult(d, e, e)
            mult(f, a, a)
            mult(a, c, a)
            mult(c, b, e)
            add(e, a, c)
            sub(a, a, c)
            mult(b, a, a)
            sub(c, d, f)
            mult(a, c, C121665)
            add(a, a, d)
            mult(c, c, a)
            mult(a, d, f)
            mult(d, b, x1)
            mult(b, e, e)
            sel25519(a, b, r)
            sel25519(c, d, r)
        }

        inv25519(c, c)
        mult(x, a, c)
        val out = pack25519(x)
        k.fill(0)
        return out
    }

    private val C121665 = longArrayOf(0xDB41, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    private fun clampScalarBytes(k: ByteArray): ByteArray {
        val z = k.copyOf(32)
        z[0] = (z[0].toInt() and 248).toByte()
        z[31] = (z[31].toInt() and 127).toByte()
        z[31] = (z[31].toInt() or 64).toByte()
        return z
    }

    private fun unpack25519(n: ByteArray): LongArray {
        val o = LongArray(16)
        val masked = n.copyOf(32)
        // RFC 7748 Section 5: always mask MSB of final byte.
        masked[31] = (masked[31].toInt() and 0x7F).toByte()
        for (i in 0 until 16) {
            o[i] = ((masked[2 * i].toInt() and 0xFF) + ((masked[2 * i + 1].toInt() and 0xFF) shl 8)).toLong()
        }
        return o
    }

    private fun sel25519(p: LongArray, q: LongArray, b: Int) {
        val c = -(b and 1).toLong()
        for (i in 0 until 16) {
            val t = c and (p[i] xor q[i])
            p[i] = p[i] xor t
            q[i] = q[i] xor t
        }
    }

    private fun car25519(o: LongArray) {
        var c: Long
        for (i in 0 until 16) {
            o[i] += 65536L
            c = o[i] shr 16
            o[if (i < 15) i + 1 else 0] += c - 1L + if (i == 15) 37L * (c - 1L) else 0L
            o[i] -= c shl 16
        }
    }

    private fun add(o: LongArray, a: LongArray, b: LongArray) {
        for (i in 0 until 16) o[i] = a[i] + b[i]
    }

    private fun sub(o: LongArray, a: LongArray, b: LongArray) {
        for (i in 0 until 16) o[i] = a[i] - b[i]
    }

    private fun mult(o: LongArray, a: LongArray, b: LongArray) {
        val t = LongArray(31)
        for (i in 0 until 16) {
            for (j in 0 until 16) {
                t[i + j] += a[i] * b[j]
            }
        }
        for (i in 0 until 15) {
            t[i] += 38L * t[i + 16]
        }
        for (i in 0 until 16) o[i] = t[i]
        car25519(o)
        car25519(o)
    }

    private fun inv25519(o: LongArray, i: LongArray) {
        val c = LongArray(16)
        for (a in 0 until 16) c[a] = i[a]
        for (a in 253 downTo 0) {
            mult(c, c, c)
            if (a != 2 && a != 4) {
                mult(c, c, i)
            }
        }
        for (a in 0 until 16) o[a] = c[a]
    }

    private fun pack25519(n: LongArray): ByteArray {
        val m = LongArray(16)
        val t = LongArray(16)
        for (i in 0 until 16) t[i] = n[i]
        car25519(t)
        car25519(t)
        car25519(t)
        for (j in 0 until 2) {
            m[0] = t[0] - 0xffedL
            for (i in 1 until 15) {
                m[i] = t[i] - 0xffffL - ((m[i - 1] shr 16) and 1L)
                m[i - 1] = m[i - 1] and 0xffffL
            }
            m[15] = t[15] - 0x7fffL - ((m[14] shr 16) and 1L)
            val b = ((m[15] shr 16) and 1L).toInt()
            m[14] = m[14] and 0xffffL
            sel25519(t, m, 1 - b)
        }
        val o = ByteArray(32)
        for (i in 0 until 16) {
            o[2 * i] = (t[i] and 0xffL).toByte()
            o[2 * i + 1] = ((t[i] shr 8) and 0xffL).toByte()
        }
        return o
    }

    private fun encodeBigInt32(n: BigInteger): ByteArray {
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

/**
 * Thrown when a v1 group envelope fails authentication (tamper/wrong key).
 */
class GroupE2ETamperException(message: String = "Group E2EE verification failed") : Exception(message)

