package ir.coffevista.vista_native.features.profile.settings.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.network.BackendErrorParser
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response

enum class TwoFactorWizardStep {
    LOADING,
    INTRO_AND_PREREQS,
    ENTER_CURRENT_PASSWORD,
    ENTER_NEW_PASSWORD,
    CONFIRM_NEW_PASSWORD,
    ENTER_HINT,
    ENTER_RECOVERY_EMAIL,
    VERIFY_RECOVERY_CODE,
    ACTIVE_MENU,
    CHANGE_HINT,
    CHANGE_EMAIL,
}

data class TwoFactorUiState(
    val step: TwoFactorWizardStep = TwoFactorWizardStep.LOADING,
    val status: TwoFAStatusDto = TwoFAStatusDto(),
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    // Setup / change inputs
    val currentPasswordInput: String = "",
    val newPasswordInput: String = "",
    val confirmPasswordInput: String = "",
    val hintInput: String = "",
    val recoveryEmailInput: String = "",
    val confirmCodeInput: String = "",
    val confirmSessionId: String? = null,
    val pendingEmailPattern: String? = null,
    val resendSeconds: Int = 0,
    val isUpdatingExisting: Boolean = false,
    // Inline email verification before starting 2FA setup
    val accountEmailInput: String = "",
    val accountEmailCodeInput: String = "",
    val accountEmailSessionId: String? = null,
    val isSendingAccountEmail: Boolean = false,
    val isVerifyingAccountEmail: Boolean = false,
)

@HiltViewModel
class TwoFactorSettingsViewModel @Inject constructor(
    private val twoFactorApi: TwoFactorApi,
    private val privacyApi: PrivacySettingsApi,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(TwoFactorUiState())
    val uiState: StateFlow<TwoFactorUiState> = mutableUiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadStatus()
    }

    fun loadStatus() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(step = TwoFactorWizardStep.LOADING, isBusy = true, errorMessage = null) }
            val res = runCatching { twoFactorApi.getStatus() }.getOrNull()
            if (res == null || !res.isSuccessful || res.body() == null) {
                val msg = res?.let { extractError(it, "خطا در دریافت وضعیت تایید دو مرحله‌ای") }
                    ?: "خطا در ارتباط با سرور"
                mutableUiState.update {
                    it.copy(
                        step = TwoFactorWizardStep.INTRO_AND_PREREQS,
                        isBusy = false,
                        errorMessage = msg,
                    )
                }
                return@launch
            }
            val status = res.body()!!
            val targetStep = when {
                !status.emailUnconfirmedPattern.isNullOrBlank() -> TwoFactorWizardStep.VERIFY_RECOVERY_CODE
                status.hasPassword -> TwoFactorWizardStep.ACTIVE_MENU
                else -> TwoFactorWizardStep.INTRO_AND_PREREQS
            }
            mutableUiState.update {
                it.copy(
                    step = targetStep,
                    status = status,
                    isBusy = false,
                    errorMessage = null,
                    pendingEmailPattern = status.emailUnconfirmedPattern,
                    recoveryEmailInput = status.verifiedEmail.orEmpty().ifBlank { it.recoveryEmailInput },
                    accountEmailInput = status.verifiedEmail.orEmpty().ifBlank { it.accountEmailInput },
                )
            }
        }
    }

    fun updateAccountEmailInput(value: String) {
        mutableUiState.update { it.copy(accountEmailInput = value, errorMessage = null) }
    }

    fun updateAccountEmailCodeInput(value: String) {
        val digits = value.map { ch ->
            when (ch) {
                in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
                in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                else -> ch
            }
        }.filter { it in '0'..'9' }.take(6).joinToString("")
        mutableUiState.update { it.copy(accountEmailCodeInput = digits, errorMessage = null) }
    }

    fun sendAccountEmailVerification() {
        val email = mutableUiState.value.accountEmailInput.trim()
        if (email.isBlank() || !email.contains("@")) {
            mutableUiState.update { it.copy(errorMessage = "لطفاً یک ایمیل معتبر وارد کنید") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSendingAccountEmail = true, errorMessage = null, infoMessage = null) }
            val res = runCatching {
                privacyApi.sendEmailVerification(SendEmailVerificationDto(email = email))
            }.getOrNull()
            if (res != null && res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                if (body.sessionId.isNullOrBlank()) {
                    // Already verified -> reload 2FA status
                    mutableUiState.update { it.copy(isSendingAccountEmail = false, infoMessage = body.message) }
                    loadStatus()
                } else {
                    mutableUiState.update {
                        it.copy(
                            isSendingAccountEmail = false,
                            accountEmailSessionId = body.sessionId,
                            accountEmailCodeInput = "",
                            infoMessage = body.message ?: "کد ۶ رقمی تایید به ایمیل شما ارسال شد",
                        )
                    }
                }
            } else {
                val msg = res?.let { extractError(it, "ارسال کد تایید ایمیل ناموفق بود") }
                    ?: "خطا در ارتباط با سرور"
                mutableUiState.update { it.copy(isSendingAccountEmail = false, errorMessage = msg) }
            }
        }
    }

    fun verifyAccountEmailCode() {
        val state = mutableUiState.value
        val sessionId = state.accountEmailSessionId
        val code = state.accountEmailCodeInput.trim()
        if (sessionId.isNullOrBlank()) {
            mutableUiState.update { it.copy(errorMessage = "ابتدا کد تایید ایمیل را ارسال کنید") }
            return
        }
        if (code.length < 4) {
            mutableUiState.update { it.copy(errorMessage = "کد تایید ایمیل را کامل وارد کنید") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isVerifyingAccountEmail = true, errorMessage = null) }
            val res = runCatching {
                privacyApi.verifyEmail(VerifyEmailDto(sessionId = sessionId, code = code))
            }.getOrNull()
            if (res != null && res.isSuccessful) {
                mutableUiState.update {
                    it.copy(
                        isVerifyingAccountEmail = false,
                        accountEmailSessionId = null,
                        accountEmailCodeInput = "",
                        infoMessage = "ایمیل حساب کاربری شما با موفقیت تایید شد",
                    )
                }
                loadStatus()
            } else {
                val msg = res?.let { extractError(it, "کد تایید نامعتبر یا منقضی است") }
                    ?: "خطا در ارتباط با سرور"
                mutableUiState.update { it.copy(isVerifyingAccountEmail = false, errorMessage = msg) }
            }
        }
    }

    fun beginEnableWizard() {
        val status = mutableUiState.value.status
        if (!status.canEnable) {
            mutableUiState.update {
                it.copy(
                    errorMessage = "برای فعال‌سازی تایید دو مرحله‌ای، ابتدا باید شماره موبایل یا ایمیل خود را تایید کنید.",
                )
            }
            return
        }
        mutableUiState.update {
            it.copy(
                step = TwoFactorWizardStep.ENTER_NEW_PASSWORD,
                isUpdatingExisting = false,
                currentPasswordInput = "",
                newPasswordInput = "",
                confirmPasswordInput = "",
                hintInput = "",
                recoveryEmailInput = status.verifiedEmail.orEmpty(),
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun beginChangePasswordWizard() {
        mutableUiState.update {
            it.copy(
                step = TwoFactorWizardStep.ENTER_CURRENT_PASSWORD,
                isUpdatingExisting = true,
                currentPasswordInput = "",
                newPasswordInput = "",
                confirmPasswordInput = "",
                hintInput = it.status.hint,
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun beginChangeHint() {
        mutableUiState.update {
            it.copy(
                step = TwoFactorWizardStep.CHANGE_HINT,
                currentPasswordInput = "",
                hintInput = it.status.hint,
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun beginChangeRecoveryEmail() {
        mutableUiState.update {
            it.copy(
                step = TwoFactorWizardStep.CHANGE_EMAIL,
                currentPasswordInput = "",
                recoveryEmailInput = it.status.verifiedEmail.orEmpty(),
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun backToMenuOrIntro() {
        val status = mutableUiState.value.status
        mutableUiState.update {
            it.copy(
                step = if (status.hasPassword) TwoFactorWizardStep.ACTIVE_MENU else TwoFactorWizardStep.INTRO_AND_PREREQS,
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun updateCurrentPassword(value: String) {
        mutableUiState.update { it.copy(currentPasswordInput = value, errorMessage = null) }
    }

    fun updateNewPassword(value: String) {
        mutableUiState.update { it.copy(newPasswordInput = value, errorMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        mutableUiState.update { it.copy(confirmPasswordInput = value, errorMessage = null) }
    }

    fun updateHintInput(value: String) {
        mutableUiState.update { it.copy(hintInput = value.take(64), errorMessage = null) }
    }

    fun updateRecoveryEmailInput(value: String) {
        mutableUiState.update { it.copy(recoveryEmailInput = value, errorMessage = null) }
    }

    fun updateConfirmCodeInput(value: String) {
        val digits = value.filter(Char::isDigit).take(6)
        mutableUiState.update { it.copy(confirmCodeInput = digits, errorMessage = null) }
    }

    fun submitCurrentPasswordForChange() {
        if (mutableUiState.value.currentPasswordInput.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "رمز عبور دومرحله‌ای فعلی را وارد کنید") }
            return
        }
        mutableUiState.update { it.copy(step = TwoFactorWizardStep.ENTER_NEW_PASSWORD, errorMessage = null) }
    }

    fun submitNewPassword() {
        val pass = mutableUiState.value.newPasswordInput
        if (pass.length < 6) {
            mutableUiState.update { it.copy(errorMessage = "رمز عبور دومرحله‌ای باید حداقل ۶ کاراکتر باشد") }
            return
        }
        mutableUiState.update { it.copy(step = TwoFactorWizardStep.CONFIRM_NEW_PASSWORD, errorMessage = null) }
    }

    fun submitConfirmPassword() {
        val state = mutableUiState.value
        if (state.newPasswordInput != state.confirmPasswordInput) {
            mutableUiState.update { it.copy(errorMessage = "رمز عبور و تکرار آن یکسان نیستند") }
            return
        }
        mutableUiState.update { it.copy(step = TwoFactorWizardStep.ENTER_HINT, errorMessage = null) }
    }

    fun submitHintAndProceed(skipHint: Boolean = false) {
        val state = mutableUiState.value
        val hint = if (skipHint) "" else state.hintInput.trim()
        if (hint.isNotEmpty() && hint == state.newPasswordInput) {
            mutableUiState.update { it.copy(errorMessage = "راهنمای رمز نباید با خود رمز عبور یکسان باشد") }
            return
        }
        mutableUiState.update { it.copy(hintInput = hint, errorMessage = null) }
        if (state.isUpdatingExisting && state.status.hasRecovery) {
            submitSetupRequest(keepRecoveryEmail = true)
        } else {
            mutableUiState.update { it.copy(step = TwoFactorWizardStep.ENTER_RECOVERY_EMAIL) }
        }
    }

    fun submitSetupRequest(keepRecoveryEmail: Boolean = false) {
        val state = mutableUiState.value
        val email = state.recoveryEmailInput.trim()
        if (!keepRecoveryEmail && (email.isBlank() || !email.contains("@"))) {
            mutableUiState.update { it.copy(errorMessage = "لطفاً یک ایمیل بازیابی معتبر وارد کنید") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isBusy = true, errorMessage = null, infoMessage = null) }
            val res = runCatching {
                twoFactorApi.setupOrUpdate(
                    TwoFASetupRequestDto(
                        currentPassword = if (state.isUpdatingExisting) state.currentPasswordInput else "",
                        newPassword = state.newPasswordInput,
                        hint = state.hintInput.trim(),
                        recoveryEmail = if (keepRecoveryEmail) "" else email,
                        keepRecoveryEmail = keepRecoveryEmail,
                    ),
                )
            }.getOrNull()
            if (res == null || !res.isSuccessful || res.body() == null) {
                val msg = res?.let { extractError(it, "ثبت تنظیمات تایید دو مرحله‌ای ناموفق بود") }
                    ?: "خطا در ارتباط با سرور"
                mutableUiState.update { it.copy(isBusy = false, errorMessage = msg) }
                return@launch
            }
            val body = res.body()!!
            val updatedStatus = body.status ?: state.status
            if (body.requiresEmailConfirm) {
                mutableUiState.update {
                    it.copy(
                        step = TwoFactorWizardStep.VERIFY_RECOVERY_CODE,
                        status = updatedStatus,
                        confirmSessionId = body.sessionId,
                        pendingEmailPattern = body.emailPattern ?: updatedStatus.emailUnconfirmedPattern,
                        confirmCodeInput = "",
                        resendSeconds = 60,
                        isBusy = false,
                        infoMessage = body.message,
                    )
                }
                startCountdown()
            } else {
                mutableUiState.update {
                    it.copy(
                        step = TwoFactorWizardStep.ACTIVE_MENU,
                        status = updatedStatus,
                        isBusy = false,
                        infoMessage = body.message ?: "رمز دومرحله‌ای با موفقیت ذخیره شد",
                    )
                }
            }
        }
    }

    fun confirmRecoveryEmailCode() {
        val state = mutableUiState.value
        val code = state.confirmCodeInput.trim()
        if (code.length < 6) {
            mutableUiState.update { it.copy(errorMessage = "کد تایید ۶ رقمی ارسال‌شده به ایمیل را وارد کنید") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isBusy = true, errorMessage = null) }
            val res = runCatching {
                twoFactorApi.confirmEmail(
                    TwoFAConfirmEmailRequestDto(
                        sessionId = state.confirmSessionId,
                        code = code,
                    ),
                )
            }.getOrNull()
            if (res == null || !res.isSuccessful || res.body() == null) {
                val msg = res?.let { extractError(it, "کد تایید ایمیل نامعتبر یا منقضی است") }
                    ?: "خطا در ارتباط با سرور"
                mutableUiState.update { it.copy(isBusy = false, errorMessage = msg) }
                return@launch
            }
            val body = res.body()!!
            countdownJob?.cancel()
            mutableUiState.update {
                it.copy(
                    step = TwoFactorWizardStep.ACTIVE_MENU,
                    status = body.status ?: state.status.copy(hasPassword = true),
                    confirmCodeInput = "",
                    confirmSessionId = null,
                    pendingEmailPattern = null,
                    resendSeconds = 0,
                    isBusy = false,
                    infoMessage = body.message ?: "تایید دو مرحله‌ای با موفقیت فعال شد",
                )
            }
        }
    }

    fun resendRecoveryConfirmCode() {
        if (mutableUiState.value.resendSeconds > 0 || mutableUiState.value.isBusy) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isBusy = true, errorMessage = null) }
            val res = runCatching { twoFactorApi.resendEmail() }.getOrNull()
            if (res == null || !res.isSuccessful || res.body() == null) {
                val msg = res?.let { extractError(it, "ارسال مجدد کد ناموفق بود") }
                    ?: "خطا در ارتباط با سرور"
                mutableUiState.update { it.copy(isBusy = false, errorMessage = msg) }
                return@launch
            }
            val body = res.body()!!
            mutableUiState.update {
                it.copy(
                    confirmSessionId = body.sessionId ?: it.confirmSessionId,
                    resendSeconds = 60,
                    isBusy = false,
                    infoMessage = body.message ?: "کد تایید مجدداً ارسال شد",
                )
            }
            startCountdown()
        }
    }

    fun abortPendingSetup() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isBusy = true, errorMessage = null) }
            val res = runCatching { twoFactorApi.abortSetup() }.getOrNull()
            countdownJob?.cancel()
            val status = res?.body() ?: mutableUiState.value.status.copy(emailUnconfirmedPattern = null)
            mutableUiState.update {
                it.copy(
                    step = if (status.hasPassword) TwoFactorWizardStep.ACTIVE_MENU else TwoFactorWizardStep.INTRO_AND_PREREQS,
                    status = status,
                    pendingEmailPattern = null,
                    confirmSessionId = null,
                    confirmCodeInput = "",
                    isBusy = false,
                )
            }
        }
    }

    fun submitChangeHint() {
        val state = mutableUiState.value
        if (state.currentPasswordInput.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "رمز عبور دومرحله‌ای فعلی را وارد کنید") }
            return
        }
        val hint = state.hintInput.trim()
        if (hint.isNotEmpty() && hint == state.currentPasswordInput) {
            mutableUiState.update { it.copy(errorMessage = "راهنمای رمز نباید با خود رمز عبور یکسان باشد") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isBusy = true, errorMessage = null) }
            val res = runCatching {
                twoFactorApi.updateHint(
                    TwoFAUpdateHintRequestDto(
                        currentPassword = state.currentPasswordInput,
                        hint = hint,
                    ),
                )
            }.getOrNull()
            if (res == null || !res.isSuccessful || res.body() == null) {
                val msg = res?.let { extractError(it, "تغییر راهنمای رمز ناموفق بود") }
                    ?: "خطا در ارتباط با سرور"
                mutableUiState.update { it.copy(isBusy = false, errorMessage = msg) }
                return@launch
            }
            mutableUiState.update {
                it.copy(
                    step = TwoFactorWizardStep.ACTIVE_MENU,
                    status = res.body()!!,
                    currentPasswordInput = "",
                    isBusy = false,
                    infoMessage = "راهنمای رمز با موفقیت به‌روزرسانی شد",
                )
            }
        }
    }

    fun submitChangeRecoveryEmail() {
        val state = mutableUiState.value
        if (state.currentPasswordInput.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "رمز عبور دومرحله‌ای فعلی را وارد کنید") }
            return
        }
        val email = state.recoveryEmailInput.trim()
        if (email.isBlank() || !email.contains("@")) {
            mutableUiState.update { it.copy(errorMessage = "ایمیل بازیابی معتبر وارد کنید") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isBusy = true, errorMessage = null) }
            val res = runCatching {
                twoFactorApi.changeRecoveryEmail(
                    TwoFAChangeEmailRequestDto(
                        currentPassword = state.currentPasswordInput,
                        recoveryEmail = email,
                    ),
                )
            }.getOrNull()
            if (res == null || !res.isSuccessful || res.body() == null) {
                val msg = res?.let { extractError(it, "تغییر ایمیل بازیابی ناموفق بود") }
                    ?: "خطا در ارتباط با سرور"
                mutableUiState.update { it.copy(isBusy = false, errorMessage = msg) }
                return@launch
            }
            val body = res.body()!!
            mutableUiState.update {
                it.copy(
                    step = TwoFactorWizardStep.VERIFY_RECOVERY_CODE,
                    status = body.status ?: state.status,
                    confirmSessionId = body.sessionId,
                    pendingEmailPattern = body.emailPattern,
                    confirmCodeInput = "",
                    resendSeconds = 60,
                    isBusy = false,
                    infoMessage = body.message,
                )
            }
            startCountdown()
        }
    }

    fun disableTwoFactor(currentPassword: String, onDone: () -> Unit) {
        if (currentPassword.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "لطفاً رمز عبور دومرحله‌ای فعلی را وارد کنید") }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(isBusy = true, errorMessage = null) }
            val res = runCatching { twoFactorApi.remove(Remove2FADto(currentPassword)) }.getOrNull()
            if (res == null || !res.isSuccessful) {
                val msg = res?.let { extractError(it, "غیرفعال‌سازی تایید دو مرحله‌ای ناموفق بود") }
                    ?: "خطا در ارتباط با سرور"
                mutableUiState.update { it.copy(isBusy = false, errorMessage = msg) }
                return@launch
            }
            mutableUiState.update {
                it.copy(
                    isBusy = false,
                    infoMessage = "تایید دو مرحله‌ای با موفقیت غیرفعال شد",
                )
            }
            onDone()
            loadStatus()
        }
    }

    fun cancelPendingReset() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isBusy = true, errorMessage = null) }
            val res = runCatching { twoFactorApi.cancelPendingReset() }.getOrNull()
            if (res == null || !res.isSuccessful || res.body() == null) {
                val msg = res?.let { extractError(it, "لغو درخواست بازنشانی ناموفق بود") }
                    ?: "خطا در ارتباط با سرور"
                mutableUiState.update { it.copy(isBusy = false, errorMessage = msg) }
                return@launch
            }
            mutableUiState.update {
                it.copy(
                    status = res.body()!!,
                    isBusy = false,
                    infoMessage = "درخواست بازنشانی ۷ روزه با موفقیت لغو شد",
                )
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (mutableUiState.value.resendSeconds > 0) {
                delay(1_000)
                mutableUiState.update {
                    it.copy(resendSeconds = (it.resendSeconds - 1).coerceAtLeast(0))
                }
            }
        }
    }

    private fun extractError(res: Response<*>, fallback: String): String {
        val raw = runCatching { res.errorBody()?.string() }.getOrNull().orEmpty()
        if (raw.isBlank()) return fallback
        return BackendErrorParser.parse(raw, res.headers()["Retry-After"]).message?.ifBlank { fallback } ?: fallback
    }
}
