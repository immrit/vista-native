package ir.coffevista.vista_native.core.auth

data class AuthUser(
    val id: String,
    val phoneNumber: String?,
    val profileCompleted: Boolean,
    val hasPassword: Boolean,
    val passwordRequired: Boolean,
    val accountStatus: String,
    val username: String? = null,
    val fullName: String? = null,
) {
    val welcomeName: String
        get() = fullName?.trim()?.takeIf(String::isNotEmpty)
            ?: username?.trim()?.takeIf(String::isNotEmpty)
            ?: phoneNumber?.trim()?.takeIf(String::isNotEmpty)
            ?: "کاربر ویستا"
}

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
)

data class AuthPayload(
    val user: AuthUser,
    val session: AuthSession,
    val isNewUser: Boolean,
)

data class IdentifierLookup(
    val exists: Boolean,
    val isPhone: Boolean,
    val normalizedIdentifier: String?,
    val authFlow: String?,
    val accountStatus: String?,
)

data class OtpChallenge(
    val expiresInSeconds: Int,
    val debugCode: String?,
)

sealed interface OtpVerification {
    data class Authenticated(val payload: AuthPayload) : OtpVerification
    data class PasswordChallenge(val twoFactorToken: String) : OtpVerification
}
