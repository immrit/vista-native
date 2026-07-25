package ir.coffevista.vista_native.core.auth

import ir.coffevista.vista_native.core.common.Outcome

interface AuthRepository {
    suspend fun lookupIdentifier(identifier: String): Outcome<IdentifierLookup>
    suspend fun login(identifier: String, password: String): Outcome<AuthPayload>
    suspend fun sendOtp(phoneNumber: String): Outcome<OtpChallenge>
    suspend fun verifyOtp(phoneNumber: String, code: String): Outcome<OtpVerification>
    suspend fun verifyTwoFactor(token: String, password: String): Outcome<AuthPayload>
    suspend fun setPassword(accessToken: String, password: String): Outcome<Unit>
    suspend fun refresh(refreshToken: String): Outcome<AuthPayload>
    suspend fun maintenanceMode(): Outcome<Boolean>
}
