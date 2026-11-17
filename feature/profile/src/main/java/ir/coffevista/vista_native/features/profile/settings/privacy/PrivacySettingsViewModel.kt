package ir.coffevista.vista_native.features.profile.settings.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.datastore.PrivacySettingsCache
import ir.coffevista.vista_native.core.datastore.PrivacySettingsSnapshot
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.core.network.BackendErrorParser
import ir.coffevista.vista_native.core.security.BiometricAuthenticator
import ir.coffevista.vista_native.core.security.BiometricAvailability
import ir.coffevista.vista_native.core.security.SessionStore
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacySettingsUiState(
    val settings: PrivacySettingsDto = PrivacySettingsDto(),
    val storyReplyPermission: String = "everyone",
    val isLoading: Boolean = true,
    val hasLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricLoading: Boolean = true,
    val errorMessage: String? = null,
    val verifiedEmail: String? = null,
    val isEmailVerified: Boolean = false,
    val emailVerificationSessionId: String? = null,
    val emailVerificationTargetEmail: String = "",
    val emailVerificationResendSeconds: Int = 0,
    val emailVerificationCompleted: Boolean = false,
    val isSendingEmailVerification: Boolean = false,
    val isVerifyingEmail: Boolean = false,
    val emailVerificationSuccessMessage: String? = null,
    val emailVerificationErrorMessage: String? = null,
)

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val api: PrivacySettingsApi,
    private val twoFactorApi: TwoFactorApi,
    private val cache: PrivacySettingsCache,
    private val authenticationStateProvider: AuthenticationStateProvider,
    private val sessionStore: SessionStore,
    private val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PrivacySettingsUiState())
    val uiState: StateFlow<PrivacySettingsUiState> = mutableUiState.asStateFlow()
    private var emailResendCountdownJob: Job? = null

    init {
        refreshBiometricPolicy()
        refresh()
    }

    private fun refreshBiometricPolicy() {
        mutableUiState.update {
            it.copy(
                biometricEnabled = sessionStore.isBiometricEnabled(),
                biometricAvailable = biometricAuthenticator.availability() == BiometricAvailability.AVAILABLE,
                biometricLoading = false,
            )
        }
    }

    fun setBiometricEnabled(activity: FragmentActivity, enabled: Boolean) {
        if (!enabled) {
            runCatching { sessionStore.setBiometricEnabled(false) }
                .onSuccess { mutableUiState.update { state -> state.copy(biometricEnabled = false) } }
                .onFailure { mutableUiState.update { state -> state.copy(errorMessage = "غیرفعال‌سازی ورود بیومتریک ناموفق بود") } }
            return
        }
        if (biometricAuthenticator.availability() != BiometricAvailability.AVAILABLE) {
            mutableUiState.update { it.copy(errorMessage = "بیومتریک یا قفل دستگاه در دسترس نیست") }
            return
        }
        mutableUiState.update { it.copy(biometricLoading = true, errorMessage = null) }
        biometricAuthenticator.authenticate(activity) { authenticated ->
            if (authenticated) {
                runCatching { sessionStore.setBiometricEnabled(true) }
                    .onSuccess { mutableUiState.update { state -> state.copy(biometricEnabled = true, biometricLoading = false) } }
                    .onFailure { mutableUiState.update { state -> state.copy(biometricLoading = false, errorMessage = "ذخیرهٔ امن ورود بیومتریک ناموفق بود") } }
            } else {
                mutableUiState.update { state -> state.copy(biometricLoading = false, errorMessage = "تایید هویت بیومتریک ناموفق بود") }
            }
        }
    }

    fun refresh() = viewModelScope.launch {
        mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
        val twoFaStatus = runCatching { twoFactorApi.getStatus() }.getOrNull()?.takeIf { it.isSuccessful }?.body()
        if (twoFaStatus != null) {
            mutableUiState.update {
                it.copy(
                    isEmailVerified = twoFaStatus.emailVerified,
                    verifiedEmail = twoFaStatus.verifiedEmail ?: it.verifiedEmail,
                    emailVerificationTargetEmail = it.emailVerificationTargetEmail.ifBlank {
                        twoFaStatus.verifiedEmail.orEmpty()
                    },
                )
            }
        }
        val storyResp = runCatching { api.getStorySettings() }.getOrNull()
        val storyPerm = storyResp?.body()?.reply_permission ?: mutableUiState.value.storyReplyPermission
        val response = runCatching { api.getSettings() }.getOrNull()
        val settings = response?.body()
        if (response?.isSuccessful == true && settings != null) {
            currentOwnerId()?.let { ownerId ->
                runCatching { cache.write(ownerId, settings.toSnapshot()) }
            }
            mutableUiState.update { it.copy(settings = settings, storyReplyPermission = storyPerm, isLoading = false, hasLoaded = true) }
            return@launch
        }

        val cached = currentOwnerId()?.let { ownerId ->
            runCatching { cache.read(ownerId) }.getOrNull()?.toDto()
        }
        if (cached != null) {
            mutableUiState.update {
                it.copy(
                    settings = cached,
                    storyReplyPermission = storyPerm,
                    isLoading = false,
                    hasLoaded = true,
                    errorMessage = "تنظیمات ذخیره‌شده نمایش داده می‌شوند؛ اتصال برقرار نیست",
                )
            }
        } else {
            mutableUiState.update {
                it.copy(
                    storyReplyPermission = storyPerm,
                    isLoading = false,
                    errorMessage = if (response == null) "اتصال برای دریافت تنظیمات حریم خصوصی برقرار نشد" else "دریافت تنظیمات حریم خصوصی ناموفق بود",
                )
            }
        }
    }

    fun setStoryReplyPermission(permission: String) {
        val previous = mutableUiState.value.storyReplyPermission
        mutableUiState.update { it.copy(storyReplyPermission = permission) }
        viewModelScope.launch {
            val res = runCatching { api.updateStorySettings(StorySettingsDto(permission)) }.getOrNull()
            if (res?.isSuccessful != true) {
                mutableUiState.update {
                    it.copy(storyReplyPermission = previous, errorMessage = "ذخیره تنظیمات پاسخ استوری ناموفق بود")
                }
            }
        }
    }

    fun requestAccountDeletionOtp(
        reason: String,
        onSent: (channel: String?, expiresInSeconds: Long, hasPassword: Boolean) -> Unit = { _, _, _ -> },
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = runCatching {
                api.requestAccountDeletionOtp(RequestAccountDeletionOtpDto(reason))
            }
            mutableUiState.update { it.copy(isSaving = false) }
            val res = result.getOrNull()
            val body = res?.body()
            if (res?.isSuccessful == true && body?.success != false) {
                onSent(body?.channel, body?.expiresInSeconds ?: 120L, body?.hasPassword ?: true)
            } else {
                val parsed = res?.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let {
                    BackendErrorParser.parse(it, res.headers()["Retry-After"])
                }
                val msg = parsed?.message
                    ?: body?.message
                    ?: "ارسال کد تایید حذف حساب ناموفق بود"
                mutableUiState.update { it.copy(errorMessage = msg) }
                onError(msg)
            }
        }
    }

    fun confirmAccountDeletion(
        otpCode: String,
        password: String,
        reason: String,
        onSuccess: (gracePeriodDays: Int) -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = runCatching {
                api.confirmAccountDeletion(
                    ConfirmAccountDeletionDto(
                        otpCode = otpCode,
                        password = password,
                        reason = reason,
                    ),
                )
            }
            mutableUiState.update { it.copy(isSaving = false) }
            val res = result.getOrNull()
            val body = res?.body()
            if (res?.isSuccessful == true && body?.success != false) {
                val grace = body?.gracePeriodDays ?: 14
                onSuccess(grace)
            } else {
                val parsed = res?.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let {
                    BackendErrorParser.parse(it, res.headers()["Retry-After"])
                }
                val msg = parsed?.message
                    ?: body?.message
                    ?: "کد تایید یا رمز عبور نامعتبر است"
                mutableUiState.update { it.copy(errorMessage = msg) }
                onError(msg)
            }
        }
    }

    fun remove2FA(
        currentPassword: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
            val res = runCatching {
                api.remove2FA(Remove2FADto(currentPassword))
            }.getOrNull()
            mutableUiState.update { it.copy(isSaving = false) }
            if (res?.isSuccessful == true) {
                onSuccess()
            } else {
                val msg = "رمز عبور فعلی نادرست است یا خطایی رخ داد"
                mutableUiState.update { it.copy(errorMessage = msg) }
                onError(msg)
            }
        }
    }

    fun update(transform: (PrivacySettingsDto) -> PrivacySettingsDto) = save(transform(mutableUiState.value.settings))

    fun setPrivate(value: Boolean) = save(
        updated = mutableUiState.value.settings.copy(is_private = value),
        updatePrivateAccount = true,
    )

    private fun save(updated: PrivacySettingsDto, updatePrivateAccount: Boolean = false) {
        val previous = mutableUiState.value.settings
        mutableUiState.update { it.copy(settings = updated, isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val success = runCatching {
                api.updateSettings(updated).isSuccessful &&
                    (!updatePrivateAccount || api.updatePrivateAccount(PrivateAccountRequestDto(updated.is_private)).isSuccessful)
            }.getOrDefault(false)
            if (success) {
                currentOwnerId()?.let { ownerId ->
                    runCatching { cache.write(ownerId, updated.toSnapshot()) }
                }
                mutableUiState.update { it.copy(isSaving = false) }
            } else {
                mutableUiState.update {
                    it.copy(
                        settings = previous,
                        isSaving = false,
                        errorMessage = if (updatePrivateAccount) {
                            "تغییر حالت حساب خصوصی ثبت نشد. دوباره تلاش کنید"
                        } else {
                            "ذخیرهٔ تنظیمات حریم خصوصی ناموفق بود"
                        },
                    )
                }
            }
        }
    }

    fun prepareEmailVerificationSheet() {
        mutableUiState.update {
            it.copy(
                emailVerificationErrorMessage = null,
                emailVerificationSuccessMessage = null,
                emailVerificationCompleted = false,
            )
        }
    }

    fun resetEmailVerificationToEmailStep() {
        emailResendCountdownJob?.cancel()
        mutableUiState.update {
            it.copy(
                emailVerificationSessionId = null,
                emailVerificationResendSeconds = 0,
                emailVerificationCompleted = false,
                emailVerificationErrorMessage = null,
                emailVerificationSuccessMessage = null,
            )
        }
    }

    fun sendEmailVerification(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            mutableUiState.update { it.copy(emailVerificationErrorMessage = "لطفاً یک آدرس ایمیل معتبر وارد کنید") }
            return
        }
        mutableUiState.update {
            it.copy(
                isSendingEmailVerification = true,
                emailVerificationTargetEmail = trimmedEmail,
                emailVerificationCompleted = false,
                emailVerificationErrorMessage = null,
                emailVerificationSuccessMessage = null,
            )
        }
        viewModelScope.launch {
            val res = runCatching { api.sendEmailVerification(SendEmailVerificationDto(email = trimmedEmail)) }.getOrNull()
            val body = res?.body()
            if (res != null && res.isSuccessful && body?.success == true) {
                val sessionId = body.sessionId?.trim()?.takeIf { it.isNotEmpty() }
                if (sessionId == null) {
                    emailResendCountdownJob?.cancel()
                    mutableUiState.update {
                        it.copy(
                            isSendingEmailVerification = false,
                            isEmailVerified = true,
                            verifiedEmail = trimmedEmail,
                            emailVerificationSessionId = null,
                            emailVerificationResendSeconds = 0,
                            emailVerificationCompleted = true,
                            emailVerificationSuccessMessage = body.message ?: "این ایمیل قبلاً تأیید شده است",
                        )
                    }
                } else {
                    startEmailResendCountdown(60)
                    mutableUiState.update {
                        it.copy(
                            isSendingEmailVerification = false,
                            emailVerificationSessionId = sessionId,
                            emailVerificationSuccessMessage = body.message ?: "کد ۶ رقمی به ایمیل شما ارسال شد",
                        )
                    }
                }
            } else {
                val errorText = parseApiError(
                    response = res,
                    networkFallback = "خطا در برقراری ارتباط با سرور؛ اتصال اینترنت را بررسی کنید",
                    httpFallback = body?.message ?: "ارسال کد تأیید ناموفق بود. لطفاً ایمیل را بررسی کنید",
                )
                mutableUiState.update {
                    it.copy(
                        isSendingEmailVerification = false,
                        emailVerificationErrorMessage = errorText,
                    )
                }
            }
        }
    }

    fun resendEmailVerification() {
        val state = mutableUiState.value
        if (state.isSendingEmailVerification || state.emailVerificationResendSeconds > 0) return
        val email = state.emailVerificationTargetEmail.trim()
        if (email.isBlank()) {
            resetEmailVerificationToEmailStep()
            return
        }
        sendEmailVerification(email)
    }

    fun verifyEmail(code: String) {
        val sessionId = mutableUiState.value.emailVerificationSessionId?.trim()
        if (sessionId.isNullOrBlank()) {
            mutableUiState.update { it.copy(emailVerificationErrorMessage = "ابتدا درخواست کد تأیید دهید") }
            return
        }
        val cleanCode = normalizeOtpDigits(code)
        if (cleanCode.length < 6) {
            mutableUiState.update { it.copy(emailVerificationErrorMessage = "لطفاً کد ۶ رقمی ارسال‌شده را کامل وارد کنید") }
            return
        }
        mutableUiState.update {
            it.copy(
                isVerifyingEmail = true,
                emailVerificationErrorMessage = null,
                emailVerificationSuccessMessage = null,
            )
        }
        viewModelScope.launch {
            val res = runCatching {
                api.verifyEmail(VerifyEmailDto(sessionId = sessionId, code = cleanCode))
            }.getOrNull()
            val body = res?.body()
            if (res != null && res.isSuccessful && body?.success == true) {
                emailResendCountdownJob?.cancel()
                val verifiedAddr = body.email?.takeIf { it.isNotBlank() }
                    ?: mutableUiState.value.emailVerificationTargetEmail.takeIf { it.isNotBlank() }
                    ?: mutableUiState.value.verifiedEmail
                mutableUiState.update {
                    it.copy(
                        isVerifyingEmail = false,
                        isEmailVerified = true,
                        verifiedEmail = verifiedAddr,
                        emailVerificationCompleted = true,
                        emailVerificationSuccessMessage = "ایمیل حساب کاربری شما با موفقیت تأیید شد",
                        emailVerificationSessionId = null,
                        emailVerificationResendSeconds = 0,
                    )
                }
                // همگام‌سازی وضعیت نهایی از سرور
                runCatching { twoFactorApi.getStatus() }.getOrNull()
                    ?.takeIf { it.isSuccessful }
                    ?.body()
                    ?.let { status ->
                        mutableUiState.update {
                            it.copy(
                                isEmailVerified = status.emailVerified || it.isEmailVerified,
                                verifiedEmail = status.verifiedEmail ?: it.verifiedEmail,
                            )
                        }
                    }
            } else {
                val errorText = parseApiError(
                    response = res,
                    networkFallback = "خطا در برقراری ارتباط با سرور؛ لطفاً دوباره تلاش کنید",
                    httpFallback = body?.message ?: "کد تأیید اشتباه یا منقضی شده است",
                )
                mutableUiState.update {
                    it.copy(
                        isVerifyingEmail = false,
                        emailVerificationErrorMessage = errorText,
                    )
                }
            }
        }
    }

    private fun startEmailResendCountdown(seconds: Int) {
        emailResendCountdownJob?.cancel()
        mutableUiState.update { it.copy(emailVerificationResendSeconds = seconds.coerceAtLeast(0)) }
        emailResendCountdownJob = viewModelScope.launch {
            var remaining = seconds.coerceAtLeast(0)
            while (remaining > 0) {
                delay(1_000L)
                remaining -= 1
                mutableUiState.update { it.copy(emailVerificationResendSeconds = remaining) }
            }
        }
    }

    private fun parseApiError(
        response: retrofit2.Response<*>?,
        networkFallback: String,
        httpFallback: String,
    ): String {
        if (response == null) return networkFallback
        val raw = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
        return BackendErrorParser.parse(raw).message ?: httpFallback
    }

    override fun onCleared() {
        emailResendCountdownJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun normalizeOtpDigits(raw: String): String =
            raw.map { ch ->
                when (ch) {
                    in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
                    in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                    else -> ch
                }
            }.filter { it in '0'..'9' }.take(6).joinToString("")
    }

    private fun currentOwnerId(): String? =
        (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)
            ?.context
            ?.userId
            ?.takeIf(String::isNotBlank)
}

private fun PrivacySettingsDto.toSnapshot() = PrivacySettingsSnapshot(
    isPrivate = is_private,
    lastSeenVisibility = last_seen_visibility,
    messagePrivacy = message_privacy,
    groupAddPrivacy = group_add_privacy,
    readReceipts = read_receipts,
    allowProfileZoom = allow_profile_zoom,
    inactiveAccountTtl = inactive_account_ttl.ifBlank { "6_months" },
)

private fun PrivacySettingsSnapshot.toDto() = PrivacySettingsDto(
    is_private = isPrivate,
    last_seen_visibility = lastSeenVisibility,
    message_privacy = messagePrivacy,
    group_add_privacy = groupAddPrivacy,
    read_receipts = readReceipts,
    allow_profile_zoom = allowProfileZoom,
    inactive_account_ttl = inactiveAccountTtl.ifBlank { "6_months" },
)
