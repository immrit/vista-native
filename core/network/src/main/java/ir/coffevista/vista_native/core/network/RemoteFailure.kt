package ir.coffevista.vista_native.core.network

import retrofit2.HttpException
import java.io.IOException

class RemoteFailure(
    val statusCode: Int?,
    val code: String?,
    override val message: String?,
    val retryAfterSeconds: Int? = null,
    cause: Throwable? = null,
    val feature: String? = null,
) : IOException(message, cause) {
    companion object {
        fun fromHttpException(exception: HttpException): RemoteFailure {
            val response = exception.response()
            val retryAfterHeader = response?.headers()?.get("Retry-After")
            val rawBody = runCatching { response?.errorBody()?.string() }.getOrNull().orEmpty()
            val parsed = BackendErrorParser.parse(rawBody, retryAfterHeader)
            return RemoteFailure(
                statusCode = exception.code(),
                code = parsed.code,
                message = parsed.message,
                retryAfterSeconds = parsed.retryAfterSeconds,
                cause = exception,
                feature = parsed.feature,
            )
        }
    }
}
