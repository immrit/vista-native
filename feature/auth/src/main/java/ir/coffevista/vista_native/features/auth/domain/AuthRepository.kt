package ir.coffevista.vista_native.features.auth.domain

import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification

interface AuthRepository {
    suspend fun lookupIdentifier(identifier: String): Outcome<IdentifierLookup>
    suspend fun login(identifier: String, password: String): Outcome<AuthPayload>
    suspend fun sendOtp(phoneNumber: String): Outcome<OtpChallenge>
    suspend fun verifyOtp(phoneNumber: String, code: String): Outcome<OtpVerification>
    suspend fun verifyTwoFactor(token: String, password: String): Outcome<AuthPayload>
    suspend fun setPassword(accessToken: String, password: String): Outcome<Unit>
    suspend fun recoveryOptions(identifier: String): Outcome<List<ir.coffevista.vista_native.features.auth.data.RecoveryOption>>
    suspend fun sendRecoveryCode(optionId: String): Outcome<Unit>
    suspend fun verifyRecoveryCode(optionId: String, code: String): Outcome<String>
    suspend fun completeRecovery(token: String, newPassword: String): Outcome<Unit>
    suspend fun refresh(refreshToken: String): Outcome<AuthPayload>
    suspend fun maintenanceMode(): Outcome<Boolean>
}
