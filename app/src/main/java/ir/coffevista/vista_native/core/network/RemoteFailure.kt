package ir.coffevista.vista_native.core.network

import java.io.IOException

class RemoteFailure(
    val statusCode: Int?,
    val code: String?,
    override val message: String?,
    val retryAfterSeconds: Int? = null,
    cause: Throwable? = null,
) : IOException(message, cause)
