package ir.coffevista.vista_native.features.auth

import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.AuthSession
import ir.coffevista.vista_native.core.model.auth.AuthUser
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification
import ir.coffevista.vista_native.core.network.RemoteFailure
import ir.coffevista.vista_native.features.auth.data.AuthRemoteDataSource
import ir.coffevista.vista_native.features.auth.data.DefaultAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureFoundationTest {
    @Test
    fun normalizesPersianAndInternationalIranPhoneNumbers() {
        assertEquals("09123456789", normalizeIranPhone("۰۹۱۲ ۳۴۵ ۶۷۸۹"))
        assertEquals("09123456789", normalizeIranPhone("+98 912 345 6789"))
        assertEquals("123", normalizeDigits("۱۲۳"))
        assertNull(normalizeIranPhone("02112345678"))
    }

    @Test
    fun passwordValidationMatchesTheCurrentAuthenticationRequirement() {
        assertEquals(
            "رمز عبور باید حداقل ۸ کاراکتر باشد",
            validatePassword("a1"),
        )
        assertNull(validatePassword("vista1405", phone = "09123456789"))
    }

    @Test
    fun repositoryKeepsTechnicalAndPersianErrorClassificationSeparate() = runBlocking {
        val repository = DefaultAuthRepository(
            object : FakeRemote() {
                override suspend fun login(identifier: String, password: String): AuthPayload {
                    throw RemoteFailure(
                        statusCode = 429,
                        code = "AUTH_RATE_LIMITED",
                        message = "تلاش‌های زیادی انجام شده است",
                        retryAfterSeconds = 42,
                    )
                }
            },
        )

        val result = repository.login("vista", "secret")

        assertTrue(result is Outcome.Failure)
        val error = (result as Outcome.Failure).error
        assertEquals(ErrorKind.RATE_LIMITED, error.kind)
        assertEquals("تلاش‌های زیادی انجام شده است", error.messageFa)
        assertEquals(42, error.retryAfterSeconds)
    }
}

private open class FakeRemote : AuthRemoteDataSource {
    override suspend fun lookupIdentifier(identifier: String) =
        IdentifierLookup(false, false, null, null, null)

    override suspend fun login(identifier: String, password: String) = payload()

    override suspend fun sendOtp(phoneNumber: String) = OtpChallenge(60, null)

    override suspend fun verifyOtp(phoneNumber: String, code: String) =
        OtpVerification.Authenticated(payload())

    override suspend fun verifyTwoFactor(twoFactorToken: String, password: String) = payload()

    override suspend fun setPassword(accessToken: String, password: String) = Unit

    override suspend fun refresh(refreshToken: String) = payload()

    override suspend fun isMaintenanceMode() = false

    private fun payload() = AuthPayload(
        user = AuthUser("user", "09123456789", true, true, false, "active"),
        session = AuthSession("access", "refresh", 1_900_000_000),
        isNewUser = false,
    )
}
