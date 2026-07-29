package ir.coffevista.vista_native.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.OtpVerification
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.features.auth.domain.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthStep {
    IDENTIFIER,
    PASSWORD,
    OTP,
    SET_PASSWORD,
}

data class AuthUiState(
    val step: AuthStep = AuthStep.IDENTIFIER,
    val identifier: String = "",
    val normalizedPhone: String? = null,
    val password: String = "",
    val otp: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isRegistering: Boolean = false,
    val isTwoFactor: Boolean = false,
    val resendSeconds: Int = 0,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val completedContext: AuthenticatedContext? = null,
)

sealed interface AuthAction {
    data class IdentifierChanged(val value: String) : AuthAction
    data class PasswordChanged(val value: String) : AuthAction
    data class OtpChanged(val value: String) : AuthAction
    data object TogglePasswordVisibility : AuthAction
    data object Submit : AuthAction
    data object ResendOtp : AuthAction
    data object Back : AuthAction
    data object ClearMessage : AuthAction
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore,
    private val authStateOwner: AuthenticationStateOwner,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    private var pendingTwoFactorToken: String? = null
    private var pendingAuthPayload: AuthPayload? = null
    private var countdownJob: Job? = null

    fun requirePasswordSetup() {
        val current = mutableState.value
        if (current.step != AuthStep.IDENTIFIER || current.isLoading) return
        mutableState.value = current.copy(
            step = AuthStep.SET_PASSWORD,
            infoMessage = "برای ادامه، یک رمز عبور امن برای حساب خود تعیین کنید.",
        )
    }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.IdentifierChanged -> updateInput(identifier = action.value)
            is AuthAction.PasswordChanged -> updateInput(password = action.value)
            is AuthAction.OtpChanged -> {
                val digits = normalizeDigits(action.value).filter(Char::isDigit).take(5)
                updateInput(otp = digits)
            }
            AuthAction.TogglePasswordVisibility -> {
                mutableState.value = mutableState.value.copy(
                    passwordVisible = !mutableState.value.passwordVisible,
                )
            }
            AuthAction.Submit -> submit()
            AuthAction.ResendOtp -> resendOtp()
            AuthAction.Back -> goBack()
            AuthAction.ClearMessage -> {
                mutableState.value = mutableState.value.copy(
                    errorMessage = null,
                    infoMessage = null,
                )
            }
        }
    }

    private fun updateInput(
        identifier: String = mutableState.value.identifier,
        password: String = mutableState.value.password,
        otp: String = mutableState.value.otp,
    ) {
        mutableState.value = mutableState.value.copy(
            identifier = identifier,
            password = password,
            otp = otp,
            errorMessage = null,
        )
    }

    private fun submit() {
        if (mutableState.value.isLoading) return
        when (mutableState.value.step) {
            AuthStep.IDENTIFIER -> submitIdentifier()
            AuthStep.PASSWORD -> submitPassword()
            AuthStep.OTP -> submitOtp()
            AuthStep.SET_PASSWORD -> submitPasswordSetup()
        }
    }

    private fun submitIdentifier() {
        val input = normalizeDigits(mutableState.value.identifier).trim()
        if (input.isBlank()) {
            showError("لطفاً ورودی را کامل کنید")
            return
        }
        val phone = normalizeIranPhone(input)
        launchLoading {
            when (val result = repository.lookupIdentifier(phone ?: input.lowercase())) {
                is Outcome.Failure -> showError(result.error.messageFa)
                is Outcome.Success -> {
                    val lookup = result.value
                    if (lookup.accountStatus.equals("banned", true) ||
                        lookup.accountStatus.equals("suspended", true)
                    ) {
                        showError("حساب کاربری شما غیرفعال شده است")
                        return@launchLoading
                    }
                    if (phone != null) {
                        val normalized = lookup.normalizedIdentifier ?: phone
                        mutableState.value = mutableState.value.copy(
                            identifier = normalized,
                            normalizedPhone = normalized,
                            isRegistering = !lookup.exists,
                            isLoading = false,
                            errorMessage = null,
                        )
                        when {
                            lookup.exists && lookup.authFlow == "password" -> {
                                mutableState.value = mutableState.value.copy(step = AuthStep.PASSWORD)
                            }
                            lookup.exists -> sendOtp()
                            else -> {
                                mutableState.value = mutableState.value.copy(
                                    step = AuthStep.PASSWORD,
                                    infoMessage = "برای ساخت حساب، ابتدا یک رمز عبور امن انتخاب کنید.",
                                )
                            }
                        }
                    } else if (lookup.exists) {
                        mutableState.value = mutableState.value.copy(
                            identifier = input.lowercase(),
                            normalizedPhone = null,
                            isRegistering = false,
                            step = AuthStep.PASSWORD,
                            isLoading = false,
                        )
                    } else {
                        showError("برای ثبت‌نام جدید لطفاً از شماره موبایل استفاده کنید")
                    }
                }
            }
        }
    }

    private fun submitPassword() {
        val password = mutableState.value.password
        if (password.isBlank()) {
            showError("لطفاً رمز عبور را وارد کنید")
            return
        }
        if (mutableState.value.isRegistering) {
            val validation = validatePassword(password, mutableState.value.normalizedPhone)
            if (validation != null) {
                showError(validation)
                return
            }
            sendOtp()
            return
        }

        val twoFactorToken = pendingTwoFactorToken
        launchLoading {
            val result = if (twoFactorToken != null) {
                repository.verifyTwoFactor(twoFactorToken, password)
            } else {
                repository.login(
                    identifier = normalizedIdentifier(),
                    password = password,
                )
            }
            when (result) {
                is Outcome.Failure -> showError(result.error.messageFa)
                is Outcome.Success -> completeAuthentication(result.value)
            }
        }
    }

    private fun sendOtp() {
        val phone = mutableState.value.normalizedPhone
            ?: normalizeIranPhone(mutableState.value.identifier)
        if (phone == null) {
            showError("شماره موبایل نامعتبر است")
            return
        }
        launchLoading {
            when (val result = repository.sendOtp(phone)) {
                is Outcome.Failure -> {
                    if (result.error.code.equals("AUTH_HAS_PASSWORD", true)) {
                        mutableState.value = mutableState.value.copy(
                            step = AuthStep.PASSWORD,
                            isRegistering = false,
                            isLoading = false,
                            password = "",
                            errorMessage = null,
                        )
                    } else {
                        showError(result.error.messageFa)
                    }
                }
                is Outcome.Success -> {
                    mutableState.value = mutableState.value.copy(
                        step = AuthStep.OTP,
                        normalizedPhone = phone,
                        otp = "",
                        resendSeconds = result.value.expiresInSeconds.coerceAtMost(60),
                        isLoading = false,
                        errorMessage = null,
                        infoMessage = "کد تایید برای $phone ارسال شد.",
                    )
                    startCountdown()
                }
            }
        }
    }

    private fun submitOtp() {
        val phone = mutableState.value.normalizedPhone
        if (phone == null) {
            showError("شماره موبایل نامعتبر است")
            return
        }
        if (mutableState.value.otp.length != 5) {
            showError("کد تایید ۵ رقمی را کامل وارد کنید")
            return
        }
        launchLoading {
            when (val result = repository.verifyOtp(phone, mutableState.value.otp)) {
                is Outcome.Failure -> showError(result.error.messageFa)
                is Outcome.Success -> when (val verification = result.value) {
                    is OtpVerification.PasswordChallenge -> {
                        pendingTwoFactorToken = verification.twoFactorToken
                        countdownJob?.cancel()
                        mutableState.value = mutableState.value.copy(
                            step = AuthStep.PASSWORD,
                            password = "",
                            isTwoFactor = true,
                            isRegistering = false,
                            isLoading = false,
                            errorMessage = null,
                            infoMessage = "حساب شما تایید دومرحله‌ای دارد؛ رمز عبور را وارد کنید.",
                        )
                    }
                    is OtpVerification.Authenticated -> {
                        pendingAuthPayload = verification.payload
                        sessionStore.save(verification.payload)
                        if (verification.payload.user.passwordRequired ||
                            mutableState.value.isRegistering
                        ) {
                            val selectedPassword = mutableState.value.password
                            if (selectedPassword.isNotBlank()) {
                                configurePassword(selectedPassword)
                            } else {
                                mutableState.value = mutableState.value.copy(
                                    step = AuthStep.SET_PASSWORD,
                                    password = "",
                                    isLoading = false,
                                    errorMessage = null,
                                    infoMessage = "برای ادامه، یک رمز عبور امن تعیین کنید.",
                                )
                            }
                        } else {
                            completeAuthentication(verification.payload)
                        }
                    }
                }
            }
        }
    }

    private fun submitPasswordSetup() {
        val password = mutableState.value.password
        val phone = pendingAuthPayload?.user?.phoneNumber
            ?: mutableState.value.normalizedPhone
        val validation = validatePassword(password, phone)
        if (validation != null) {
            showError(validation)
            return
        }
        launchLoading { configurePassword(password) }
    }

    private suspend fun configurePassword(password: String) {
        val accessToken = pendingAuthPayload?.session?.accessToken
            ?: sessionStore.read()?.accessToken
        if (accessToken.isNullOrBlank()) {
            showError("نشست معتبر پیدا نشد؛ لطفاً دوباره وارد شوید")
            return
        }
        when (val result = repository.setPassword(accessToken, password)) {
            is Outcome.Failure -> {
                mutableState.value = mutableState.value.copy(
                    step = AuthStep.SET_PASSWORD,
                    isLoading = false,
                    errorMessage = result.error.messageFa,
                    infoMessage = "نشست شما حفظ شده است؛ تعیین رمز را دوباره امتحان کنید.",
                )
            }
            is Outcome.Success -> {
                val payload = pendingAuthPayload
                if (payload != null) {
                    val updated = payload.copy(
                        user = payload.user.copy(
                            hasPassword = true,
                            passwordRequired = false,
                        ),
                    )
                    pendingAuthPayload = updated
                    sessionStore.save(updated)
                    completeAuthentication(updated)
                } else {
                    sessionStore.markPasswordConfigured()
                    val stored = sessionStore.read()
                    if (stored == null) {
                        showError("ذخیره نشست ممکن نشد؛ لطفاً دوباره وارد شوید")
                        return
                    }
                    val context = AuthenticatedContext(
                        userId = stored.userId,
                        profileCompleted = stored.profileCompleted,
                        passwordRequired = false,
                        offline = false,
                        displayName = stored.displayName,
                    )
                    authStateOwner.accept(context)
                    mutableState.value = mutableState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        completedContext = context,
                    )
                }
            }
        }
    }

    private fun completeAuthentication(payload: AuthPayload) {
        sessionStore.save(payload)
        val context = AuthenticatedContext(
            userId = payload.user.id,
            profileCompleted = payload.user.profileCompleted,
            passwordRequired = payload.user.passwordRequired,
            offline = false,
            displayName = payload.user.welcomeName,
        )
        authStateOwner.accept(context)
        mutableState.value = mutableState.value.copy(
            isLoading = false,
            errorMessage = null,
            completedContext = context,
        )
    }

    private fun resendOtp() {
        if (mutableState.value.step != AuthStep.OTP ||
            mutableState.value.resendSeconds > 0 ||
            mutableState.value.isLoading
        ) {
            return
        }
        sendOtp()
    }

    private fun goBack() {
        if (mutableState.value.isLoading) return
        countdownJob?.cancel()
        pendingTwoFactorToken = null
        mutableState.value = when (mutableState.value.step) {
            AuthStep.IDENTIFIER -> mutableState.value
            AuthStep.PASSWORD -> mutableState.value.copy(
                step = AuthStep.IDENTIFIER,
                password = "",
                isTwoFactor = false,
                isRegistering = false,
                errorMessage = null,
                infoMessage = null,
            )
            AuthStep.OTP -> mutableState.value.copy(
                step = if (mutableState.value.isRegistering) AuthStep.PASSWORD else AuthStep.IDENTIFIER,
                otp = "",
                resendSeconds = 0,
                errorMessage = null,
                infoMessage = null,
            )
            AuthStep.SET_PASSWORD -> mutableState.value
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (mutableState.value.resendSeconds > 0) {
                delay(1_000)
                mutableState.value = mutableState.value.copy(
                    resendSeconds = (mutableState.value.resendSeconds - 1).coerceAtLeast(0),
                )
            }
        }
    }

    private fun normalizedIdentifier(): String {
        return mutableState.value.normalizedPhone
            ?: normalizeDigits(mutableState.value.identifier).trim().lowercase()
    }

    private fun launchLoading(block: suspend () -> Unit) {
        mutableState.value = mutableState.value.copy(
            isLoading = true,
            errorMessage = null,
        )
        viewModelScope.launch {
            try {
                block()
            } catch (_: Throwable) {
                showError("ذخیره یا بازیابی امن نشست ممکن نشد. لطفاً دوباره تلاش کنید")
            }
        }
    }

    private fun showError(message: String) {
        mutableState.value = mutableState.value.copy(
            isLoading = false,
            errorMessage = message,
        )
    }

}
