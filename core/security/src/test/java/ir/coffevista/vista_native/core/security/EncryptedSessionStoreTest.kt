package ir.coffevista.vista_native.core.security

import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.AuthSession
import ir.coffevista.vista_native.core.model.auth.AuthUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class EncryptedSessionStoreTest {
    @Test
    fun roundTripUsesVersionedCiphertextWithoutPlaintextFallback() {
        val storage = MemorySessionPayloadStorage()
        val store = EncryptedSessionStore(storage, MutableKeyProvider())

        store.save(payload())

        assertEquals(stored(), store.read())
        val encoded = requireNotNull(storage.encoded)
        assertTrue(encoded.startsWith("v1."))
        assertFalse(encoded.contains(ACCESS_TOKEN))
        assertFalse(encoded.contains(REFRESH_TOKEN))
        assertFalse(encoded.contains(USER_ID))
    }

    @Test
    fun malformedCiphertextFailsClosedAndResetsKey() {
        val storage = MemorySessionPayloadStorage(encoded = "not-an-envelope")
        val keys = MutableKeyProvider()
        val store = EncryptedSessionStore(storage, keys)

        assertNull(store.read())

        assertNull(storage.encoded)
        assertEquals(1, storage.clearCalls)
        assertEquals(1, keys.resetCalls)
    }

    @Test
    fun missingKeyCannotDecryptExistingAccountAndFailsClosed() {
        val storage = MemorySessionPayloadStorage()
        val keys = MutableKeyProvider()
        val store = EncryptedSessionStore(storage, keys)
        store.save(payload())
        keys.key = newAesKey()

        assertNull(store.read())
        assertNull(storage.encoded)
        assertEquals(1, keys.resetCalls)
    }

    @Test
    fun invalidatedKeyFailureNeverReturnsOrRewritesPlaintext() {
        val storage = MemorySessionPayloadStorage(encoded = "v1.binding.iv.cipher")
        val keys = MutableKeyProvider(failReads = true)
        val store = EncryptedSessionStore(storage, keys)

        assertNull(store.read())

        assertNull(storage.encoded)
        assertEquals(1, keys.resetCalls)
    }

    @Test
    fun accountBindingTamperFailsClosed() {
        val storage = MemorySessionPayloadStorage()
        val keys = MutableKeyProvider()
        val store = EncryptedSessionStore(storage, keys)
        store.save(payload())
        val parts = requireNotNull(storage.encoded).split('.').toMutableList()
        parts[1] = "wrong-account-binding"
        storage.encoded = parts.joinToString(".")

        assertNull(store.read())
        assertNull(storage.encoded)
    }

    @Test
    fun expiredSessionRemainsDecryptableForRefreshDecision() {
        val storage = MemorySessionPayloadStorage()
        val store = EncryptedSessionStore(storage, MutableKeyProvider())
        store.save(payload(expiresAt = 1L))

        assertEquals(1L, store.read()?.expiresAtEpochSeconds)
    }

    @Test
    fun passwordTransitionReencryptsBoundPayload() {
        val storage = MemorySessionPayloadStorage()
        val store = EncryptedSessionStore(storage, MutableKeyProvider())
        store.save(payload(passwordRequired = true))
        val before = storage.encoded

        store.markPasswordConfigured()

        assertFalse(store.read()?.passwordRequired ?: true)
        assertTrue(before != storage.encoded)
    }

    @Test
    fun explicitLogoutClearsOnlyAccountOwnedSessionPayload() {
        val storage = MemorySessionPayloadStorage()
        val keys = MutableKeyProvider()
        val store = EncryptedSessionStore(storage, keys)
        store.save(payload())

        store.clear()

        assertNull(storage.encoded)
        assertEquals(0, keys.resetCalls)
    }
}

private class MemorySessionPayloadStorage(
    var encoded: String? = null,
) : SessionPayloadStorage {
    var clearCalls = 0

    override fun read(): String? = encoded

    override fun write(encoded: String): Boolean {
        this.encoded = encoded
        return true
    }

    override fun clear(): Boolean {
        clearCalls += 1
        encoded = null
        return true
    }
}

private class MutableKeyProvider(
    var key: SecretKey = newAesKey(),
    private val failReads: Boolean = false,
) : SessionSecretKeyProvider {
    var resetCalls = 0

    override fun getOrCreate(): SecretKey {
        if (failReads) error("Key permanently invalidated")
        return key
    }

    override fun reset() {
        resetCalls += 1
    }
}

private fun newAesKey(): SecretKey =
    KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

private fun payload(
    expiresAt: Long = 1_900_000_000L,
    passwordRequired: Boolean = false,
) = AuthPayload(
    user = AuthUser(
        id = USER_ID,
        phoneNumber = null,
        profileCompleted = true,
        hasPassword = true,
        passwordRequired = passwordRequired,
        accountStatus = "active",
        username = "vista-user",
        fullName = "کاربر تست",
    ),
    session = AuthSession(
        accessToken = ACCESS_TOKEN,
        refreshToken = REFRESH_TOKEN,
        expiresAtEpochSeconds = expiresAt,
    ),
    isNewUser = false,
)

private fun stored(
    expiresAt: Long = 1_900_000_000L,
) = StoredSession(
    accessToken = ACCESS_TOKEN,
    refreshToken = REFRESH_TOKEN,
    userId = USER_ID,
    expiresAtEpochSeconds = expiresAt,
    profileCompleted = true,
    passwordRequired = false,
    displayName = "کاربر تست",
)

private const val ACCESS_TOKEN = "header.payload.signature"
private const val REFRESH_TOKEN = "refresh-secret-value"
private const val USER_ID = "account-42"
