package ir.coffevista.vista_native.features.auth.data

import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.LoginOutcome
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification
import ir.coffevista.vista_native.core.model.auth.TwoFactorRecoveryChallenge
import ir.coffevista.vista_native.core.model.auth.TwoFactorResetResult

interface AuthRemoteDataSource {
    suspend fun lookupIdentifier(identifier: String): IdentifierLookup
    suspend fun login(identifier: String, password: String): AuthPayload
    suspend fun loginWithChallenge(identifier: String, password: String): LoginOutcome =
        LoginOutcome.Authenticated(login(identifier, password))
    suspend fun sendOtp(phoneNumber: String): OtpChallenge
    suspend fun verifyOtp(phoneNumber: String, code: String): OtpVerification
    suspend fun verifyTwoFactor(twoFactorToken: String, password: String): AuthPayload
    suspend fun sendTwoFactorRecoveryCode(twoFactorToken: String): TwoFactorRecoveryChallenge {
        throw UnsupportedOperationException("2FA recovery not configured")
    }
    suspend fun verifyTwoFactorRecoveryCode(
        twoFactorToken: String,
        sessionId: String?,
        code: String,
    ): AuthPayload {
        throw UnsupportedOperationException("2FA recovery verify not configured")
    }
    suspend fun requestTwoFactorReset(twoFactorToken: String): TwoFactorResetResult {
        throw UnsupportedOperationException("2FA reset not configured")
    }
    suspend fun setPassword(accessToken: String, password: String)
    suspend fun recoveryOptions(identifier: String): List<RecoveryOption>
    suspend fun sendRecoveryCode(optionId: String)
    suspend fun verifyRecoveryCode(optionId: String, code: String): String
    suspend fun completeRecovery(token: String, newPassword: String)
    suspend fun refresh(refreshToken: String): AuthPayload
    suspend fun isMaintenanceMode(): Boolean
    suspend fun logout(refreshToken: String? = null) {}
}

data class RecoveryOption(
    val id: String,
    val method: String,
    val masked: String,
)
