package ir.coffevista.vista_native.features.auth.data

import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification

interface AuthRemoteDataSource {
    suspend fun lookupIdentifier(identifier: String): IdentifierLookup
    suspend fun login(identifier: String, password: String): AuthPayload
    suspend fun sendOtp(phoneNumber: String): OtpChallenge
    suspend fun verifyOtp(phoneNumber: String, code: String): OtpVerification
    suspend fun verifyTwoFactor(twoFactorToken: String, password: String): AuthPayload
    suspend fun setPassword(accessToken: String, password: String)
    suspend fun refresh(refreshToken: String): AuthPayload
    suspend fun isMaintenanceMode(): Boolean
}
