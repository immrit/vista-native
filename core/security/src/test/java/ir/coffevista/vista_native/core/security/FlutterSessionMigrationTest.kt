package ir.coffevista.vista_native.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlutterSessionMigrationTest {
    @Test
    fun importsOnlyAccountBoundTokensAndRequiresOnlineRefresh() {
        val values = mapOf(
            "vista_access_token" to "synthetic.header.signature",
            "vista_refresh_token" to "synthetic-refresh",
            "vista_user_id" to "account-42",
            "vista_password_required" to "true",
        )

        val imported = requireNotNull(flutterSessionFromValues { key ->
            values[key]?.let(FlutterSecureValue::Present) ?: FlutterSecureValue.Missing
        })

        assertEquals("account-42", imported.userId)
        assertEquals(1L, imported.expiresAtEpochSeconds)
        assertTrue(imported.requiresOnlineValidation)
        assertTrue(imported.passwordRequired)
        assertFalse(imported.profileCompleted)
    }

    @Test
    fun missingFlutterSessionDoesNotCreateNativeCredentials() {
        assertNull(flutterSessionFromValues { FlutterSecureValue.Missing })
    }

    @Test
    fun unreadableOrIncompleteFlutterSessionFailsWithoutPartialImport() {
        assertTrue(runCatching {
            flutterSessionFromValues { FlutterSecureValue.Unavailable }
        }.isFailure)
        assertTrue(runCatching {
            flutterSessionFromValues { key ->
                if (key == "vista_access_token") {
                    FlutterSecureValue.Present("synthetic.header.signature")
                } else {
                    FlutterSecureValue.Missing
                }
            }
        }.isFailure)
    }
}
