package ir.coffevista.vista_native.core.security

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.AuthSession
import ir.coffevista.vista_native.core.model.auth.AuthUser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreSessionStoreTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() = reset()

    @After
    fun tearDown() = reset()

    @Test
    fun keystoreRoundTripStoresNoCredentialPlaintext() {
        val store = EncryptedSessionStore(context)

        store.save(payload())

        assertEquals(USER_ID, store.read()?.userId)
        val encoded = context.getSharedPreferences(SESSION_FILE_NAME, Context.MODE_PRIVATE)
            .getString(SESSION_PAYLOAD_KEY, null)
            .orEmpty()
        assertFalse(encoded.contains(ACCESS_TOKEN))
        assertFalse(encoded.contains(REFRESH_TOKEN))
        assertFalse(encoded.contains(USER_ID))
    }

    @Test
    fun missingKeystoreKeyFailsClosedAndClearsCiphertext() {
        val store = EncryptedSessionStore(context)
        store.save(payload())
        keyStore().deleteEntry(SESSION_KEY_ALIAS)

        assertNull(store.read())
        assertFalse(
            context.getSharedPreferences(SESSION_FILE_NAME, Context.MODE_PRIVATE)
                .contains(SESSION_PAYLOAD_KEY),
        )
    }

    @Test
    fun corruptCiphertextFailsClosedWithoutCrash() {
        context.getSharedPreferences(SESSION_FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SESSION_PAYLOAD_KEY, "corrupt")
            .commit()

        assertNull(EncryptedSessionStore(context).read())
        assertFalse(
            context.getSharedPreferences(SESSION_FILE_NAME, Context.MODE_PRIVATE)
                .contains(SESSION_PAYLOAD_KEY),
        )
    }

    private fun reset() {
        context.getSharedPreferences(SESSION_FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val keyStore = keyStore()
        if (keyStore.containsAlias(SESSION_KEY_ALIAS)) {
            keyStore.deleteEntry(SESSION_KEY_ALIAS)
        }
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
}

private fun payload() = AuthPayload(
    user = AuthUser(
        id = USER_ID,
        phoneNumber = null,
        profileCompleted = true,
        hasPassword = true,
        passwordRequired = false,
        accountStatus = "active",
        username = "vista-user",
        fullName = "کاربر تست",
    ),
    session = AuthSession(
        accessToken = ACCESS_TOKEN,
        refreshToken = REFRESH_TOKEN,
        expiresAtEpochSeconds = 1_900_000_000L,
    ),
    isNewUser = false,
)

private const val ACCESS_TOKEN = "header.payload.signature"
private const val REFRESH_TOKEN = "refresh-secret-value"
private const val USER_ID = "account-42"
