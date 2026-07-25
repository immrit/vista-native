package ir.coffevista.vista_native.core.auth

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
