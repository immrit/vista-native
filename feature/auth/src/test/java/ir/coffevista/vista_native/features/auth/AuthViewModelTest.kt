package ir.coffevista.vista_native.features.auth

import ir.coffevista.vista_native.core.model.session.AuthenticationState

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.AuthSession
import ir.coffevista.vista_native.core.model.auth.AuthUser
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.core.security.StoredSession
import ir.coffevista.vista_native.core.testing.MainDispatcherRule
import ir.coffevista.vista_native.features.auth.domain.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dispatcher
        get() = mainDispatcherRule.dispatcher

    @Test
    fun blankIdentifierUsesCanonicalFlutterValidationCopy() = runTest(dispatcher) {
        val viewModel = viewModel(RecordingAuthRepository())

        viewModel.onAction(AuthAction.Submit)

        assertEquals("لطفاً ورودی را کامل کنید", viewModel.state.value.errorMessage)
    }

    @Test
    fun existingPasswordAccountMovesFromIdentifierToPassword() = runTest(dispatcher) {
        val repository = RecordingAuthRepository(
            lookupResult = Outcome.Success(
                IdentifierLookup(
                    exists = true,
                    isPhone = true,
                    normalizedIdentifier = "09123456789",
                    authFlow = "password",
                    accountStatus = "active",
                ),
            ),
        )
        val viewModel = viewModel(repository)

        viewModel.onAction(AuthAction.IdentifierChanged("۰۹۱۲ ۳۴۵ ۶۷۸۹"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()

        assertEquals(AuthStep.PASSWORD, viewModel.state.value.step)
        assertEquals("09123456789", viewModel.state.value.identifier)
        assertFalse(viewModel.state.value.isRegistering)
    }

    @Test
    fun passwordLoginPersistsSessionAndCompletes() = runTest(dispatcher) {
        val repository = RecordingAuthRepository(
            lookupResult = Outcome.Success(
                IdentifierLookup(true, false, null, "password", "active"),
            ),
            loginResult = Outcome.Success(authPayload()),
        )
        val store = AuthTestSessionStore()
        val owner = AuthenticationStateOwner()
        val viewModel = viewModel(repository, store, owner)

        viewModel.onAction(AuthAction.IdentifierChanged("vista_user"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()
        viewModel.onAction(AuthAction.PasswordChanged("Vista1405"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()

        assertEquals(1, store.saveCalls)
        assertNotNull(viewModel.state.value.completedContext)
        assertTrue(owner.state.value is AuthenticationState.SignedIn)
    }

    @Test
    fun newPhoneCompletesOtpThenConfiguresPassword() = runTest(dispatcher) {
        val repository = RecordingAuthRepository(
            lookupResult = Outcome.Success(
                IdentifierLookup(false, true, "09123456789", "otp", null),
            ),
            verifyOtpResult = Outcome.Success(
                OtpVerification.Authenticated(
                    authPayload(passwordRequired = true, isNewUser = true),
                ),
            ),
        )
        val store = AuthTestSessionStore()
        val viewModel = viewModel(repository, store)

        viewModel.onAction(AuthAction.IdentifierChanged("09123456789"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()
        assertEquals(AuthStep.PASSWORD, viewModel.state.value.step)
        assertTrue(viewModel.state.value.isRegistering)

        viewModel.onAction(AuthAction.PasswordChanged("Vista1405"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()
        assertEquals(AuthStep.OTP, viewModel.state.value.step)

        viewModel.onAction(AuthAction.OtpChanged("۱۲۳۴۵"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()

        assertEquals(1, repository.setPasswordCalls)
        assertFalse(viewModel.state.value.completedContext!!.passwordRequired)
        assertTrue(store.saveCalls >= 2)
    }

    @Test
    fun loginFailureSurfacesPersianMessageWithoutCompleting() = runTest(dispatcher) {
        val repository = RecordingAuthRepository(
            lookupResult = Outcome.Success(
                IdentifierLookup(true, false, null, "password", "active"),
            ),
            loginResult = Outcome.Failure(
                AppError(
                    ErrorKind.UNAUTHORIZED,
                    "نام کاربری یا رمز عبور اشتباه است",
                ),
            ),
        )
        val viewModel = viewModel(repository)

        viewModel.onAction(AuthAction.IdentifierChanged("vista"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()
        viewModel.onAction(AuthAction.PasswordChanged("wrong"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()

        assertEquals("نام کاربری یا رمز عبور اشتباه است", viewModel.state.value.errorMessage)
        assertEquals(null, viewModel.state.value.completedContext)
    }

    @Test
    fun restoredPasswordRequiredSessionCanFinishSetup() = runTest(dispatcher) {
        val repository = RecordingAuthRepository()
        val store = AuthTestSessionStore(
            initial = StoredSession(
                accessToken = "access",
                refreshToken = "refresh",
                userId = "user-id",
                expiresAtEpochSeconds = 1_900_000_000,
                profileCompleted = false,
                passwordRequired = true,
            ),
        )
        val owner = AuthenticationStateOwner()
        val viewModel = viewModel(
            repository = repository,
            store = store,
            owner = owner,
            startInPasswordSetup = true,
        )

        assertEquals(AuthStep.SET_PASSWORD, viewModel.state.value.step)
        viewModel.onAction(AuthAction.PasswordChanged("Vista1405"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()

        assertEquals(1, store.markPasswordCalls)
        assertFalse(viewModel.state.value.completedContext!!.passwordRequired)
        assertTrue(owner.state.value is AuthenticationState.SignedIn)
    }

    @Test
    fun otpPasswordChallengeCompletesTwoFactorLogin() = runTest(dispatcher) {
        val repository = RecordingAuthRepository(
            lookupResult = Outcome.Success(
                IdentifierLookup(true, true, "09123456789", "otp", "active"),
            ),
            verifyOtpResult = Outcome.Success(
                OtpVerification.PasswordChallenge("two-factor-token"),
            ),
            loginResult = Outcome.Success(authPayload()),
        )
        val owner = AuthenticationStateOwner()
        val viewModel = viewModel(repository = repository, owner = owner)

        viewModel.onAction(AuthAction.IdentifierChanged("09123456789"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()
        assertEquals(AuthStep.OTP, viewModel.state.value.step)

        viewModel.onAction(AuthAction.OtpChanged("۱۲۳۴۵"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()
        assertEquals(AuthStep.PASSWORD, viewModel.state.value.step)
        assertTrue(viewModel.state.value.isTwoFactor)

        viewModel.onAction(AuthAction.PasswordChanged("Vista1405"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()

        assertEquals(1, repository.verifyTwoFactorCalls)
        assertTrue(owner.state.value is AuthenticationState.SignedIn)
    }

    @Test
    fun forgotPasswordTriggersOtpAndSetsRecoveryFlow() = runTest(dispatcher) {
        val repository = RecordingAuthRepository(
            lookupResult = Outcome.Success(
                IdentifierLookup(true, true, "09123456789", "password", "active"),
            ),
            verifyOtpResult = Outcome.Success(
                OtpVerification.Authenticated(authPayload()),
            ),
        )
        val store = AuthTestSessionStore()
        val viewModel = viewModel(repository, store)

        viewModel.onAction(AuthAction.IdentifierChanged("09123456789"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()
        assertEquals(AuthStep.PASSWORD, viewModel.state.value.step)

        viewModel.onAction(AuthAction.ForgotPassword)
        runCurrent()
        assertEquals(AuthStep.OTP, viewModel.state.value.step)
        assertTrue(viewModel.state.value.isPasswordRecovery)

        viewModel.onAction(AuthAction.OtpChanged("۱۲۳۴۵"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()
        assertEquals(AuthStep.SET_PASSWORD, viewModel.state.value.step)

        viewModel.onAction(AuthAction.PasswordChanged("NewPassword123"))
        viewModel.onAction(AuthAction.Submit)
        runCurrent()
        assertEquals(1, repository.completeRecoveryCalls)
        assertEquals("token", repository.completedRecoveryToken)
    }

    private fun viewModel(
        repository: RecordingAuthRepository,
        store: AuthTestSessionStore = AuthTestSessionStore(),
        owner: AuthenticationStateOwner = AuthenticationStateOwner(),
        startInPasswordSetup: Boolean = false,
    ): AuthViewModel = AuthViewModel(
        repository = repository,
        sessionStore = store,
        authStateOwner = owner,
    ).also {
        if (startInPasswordSetup) {
            it.requirePasswordSetup()
        }
    }
}

private class RecordingAuthRepository(
    var lookupResult: Outcome<IdentifierLookup> = Outcome.Success(
        IdentifierLookup(true, true, "09123456789", "password", "active"),
    ),
    var loginResult: Outcome<AuthPayload> = Outcome.Success(authPayload()),
    var verifyOtpResult: Outcome<OtpVerification> = Outcome.Success(
        OtpVerification.Authenticated(authPayload()),
    ),
) : AuthRepository {
    var setPasswordCalls = 0
    var verifyTwoFactorCalls = 0
    var completeRecoveryCalls = 0
    var completedRecoveryToken: String? = null

    override suspend fun lookupIdentifier(identifier: String) = lookupResult

    override suspend fun login(identifier: String, password: String) = loginResult

    override suspend fun sendOtp(phoneNumber: String): Outcome<OtpChallenge> =
        Outcome.Success(OtpChallenge(60, null))

    override suspend fun verifyOtp(phoneNumber: String, code: String) = verifyOtpResult

    override suspend fun verifyTwoFactor(
        token: String,
        password: String,
    ): Outcome<AuthPayload> {
        verifyTwoFactorCalls += 1
        return loginResult
    }

    override suspend fun setPassword(
        accessToken: String,
        password: String,
    ): Outcome<Unit> {
        setPasswordCalls += 1
        return Outcome.Success(Unit)
    }

    override suspend fun recoveryOptions(identifier: String) =
        Outcome.Success(
            listOf(
                ir.coffevista.vista_native.features.auth.data.RecoveryOption(
                    id = "sms-option",
                    method = "sms",
                    masked = "0912****6789",
                ),
            ),
        )

    override suspend fun sendRecoveryCode(optionId: String) = Outcome.Success(Unit)

    override suspend fun verifyRecoveryCode(optionId: String, code: String) =
        Outcome.Success("token")

    override suspend fun completeRecovery(token: String, newPassword: String): Outcome<Unit> {
        completeRecoveryCalls += 1
        completedRecoveryToken = token
        return Outcome.Success(Unit)
    }

    override suspend fun refresh(refreshToken: String): Outcome<AuthPayload> = loginResult

    override suspend fun maintenanceMode(): Outcome<Boolean> = Outcome.Success(false)
}

private class AuthTestSessionStore(
    private var initial: StoredSession? = null,
) : SessionStore {
    var saveCalls = 0
    var markPasswordCalls = 0

    override fun read() = initial

    override fun save(payload: AuthPayload) {
        saveCalls += 1
        initial = StoredSession(
            accessToken = payload.session.accessToken,
            refreshToken = payload.session.refreshToken,
            userId = payload.user.id,
            expiresAtEpochSeconds = payload.session.expiresAtEpochSeconds,
            profileCompleted = payload.user.profileCompleted,
            passwordRequired = payload.user.passwordRequired,
        )
    }

    override fun markPasswordConfigured() {
        markPasswordCalls += 1
        initial = initial?.copy(passwordRequired = false)
    }

    override fun markProfileCompleted() {
        initial = initial?.copy(profileCompleted = true)
    }

    override fun isBiometricEnabled(): Boolean = initial?.biometricEnabled == true

    override fun setBiometricEnabled(enabled: Boolean) {
        initial = initial?.copy(biometricEnabled = enabled)
    }

    override fun clear() {
        initial = null
    }
}

private fun authPayload(
    passwordRequired: Boolean = false,
    isNewUser: Boolean = false,
) = AuthPayload(
    user = AuthUser(
        id = "user-id",
        phoneNumber = "09123456789",
        profileCompleted = !isNewUser,
        hasPassword = !passwordRequired,
        passwordRequired = passwordRequired,
        accountStatus = "active",
    ),
    session = AuthSession(
        accessToken = "access",
        refreshToken = "refresh",
        expiresAtEpochSeconds = 1_900_000_000,
    ),
    isNewUser = isNewUser,
)
