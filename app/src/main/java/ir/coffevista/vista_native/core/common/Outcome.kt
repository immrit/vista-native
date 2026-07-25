package ir.coffevista.vista_native.core.common

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

enum class ErrorKind {
    VALIDATION,
    NETWORK,
    UNAUTHORIZED,
    ACCOUNT_DISABLED,
    RATE_LIMITED,
    SERVER,
    MALFORMED_RESPONSE,
    UNKNOWN,
}

data class AppError(
    val kind: ErrorKind,
    val messageFa: String,
    val code: String? = null,
    val retryAfterSeconds: Int? = null,
)
