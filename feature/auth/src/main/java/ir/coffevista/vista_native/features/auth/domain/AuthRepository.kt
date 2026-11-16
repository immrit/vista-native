package ir.coffevista.vista_native.features.auth.domain

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.LoginOutcome
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification
import ir.coffevista.vista_native.core.model.auth.TwoFactorRecoveryChallenge
import ir.coffevista.vista_native.core.model.auth.TwoFactorResetResult

interface AuthRepository {
    suspend fun lookupIdentifier(identifier: String): Outcome<IdentifierLookup>
    suspend fun login(identifier: String, password: String): Outcome<AuthPayload>
    suspend fun loginWithChallenge(identifier: String, password: String): Outcome<LoginOutcome> =
        when (val res = login(identifier, password)) {
            is Outcome.Success -> Outcome.Success(LoginOutcome.Authenticated(res.value))
            is Outcome.Failure -> Outcome.Failure(res.error)
        }
    suspend fun sendOtp(phoneNumber: String): Outcome<OtpChallenge>
    suspend fun verifyOtp(phoneNumber: String, code: String): Outcome<OtpVerification>
    suspend fun verifyTwoFactor(token: String, password: String): Outcome<AuthPayload>
    suspend fun sendTwoFactorRecoveryCode(twoFactorToken: String): Outcome<TwoFactorRecoveryChallenge> =
        Outcome.Failure(AppError(ErrorKind.UNKNOWN, messageFa = "بازیابی ایمیل در دسترس نیست"))
    suspend fun verifyTwoFactorRecoveryCode(
        twoFactorToken: String,
        sessionId: String?,
        code: String,
    ): Outcome<AuthPayload> =
        Outcome.Failure(AppError(ErrorKind.UNKNOWN, messageFa = "بازیابی ایمیل در دسترس نیست"))
    suspend fun requestTwoFactorReset(twoFactorToken: String): Outcome<TwoFactorResetResult> =
        Outcome.Failure(AppError(ErrorKind.UNKNOWN, messageFa = "بازنشانی دومرحله‌ای در دسترس نیست"))
    suspend fun setPassword(accessToken: String, password: String): Outcome<Unit>
    suspend fun recoveryOptions(identifier: String): Outcome<List<ir.coffevista.vista_native.features.auth.data.RecoveryOption>>
    suspend fun sendRecoveryCode(optionId: String): Outcome<Unit>
    suspend fun verifyRecoveryCode(optionId: String, code: String): Outcome<String>
    suspend fun completeRecovery(token: String, newPassword: String): Outcome<Unit>
    suspend fun refresh(refreshToken: String): Outcome<AuthPayload>
    suspend fun maintenanceMode(): Outcome<Boolean>
    suspend fun logout(refreshToken: String? = null): Outcome<Unit> = Outcome.Success(Unit)
}
