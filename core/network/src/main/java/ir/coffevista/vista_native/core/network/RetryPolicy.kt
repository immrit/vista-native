package ir.coffevista.vista_native.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class BoundedRetryPolicy(
    private val maxRetries: Int = 1,
) {
    init {
        require(maxRetries in 0..2)
    }

    fun shouldRetry(method: String, completedRetries: Int, failure: IOException): Boolean {
        if (completedRetries >= maxRetries) return false
        if (method.uppercase() !in IDEMPOTENT_METHODS) return false
        return failure !is javax.net.ssl.SSLException
    }

    private companion object {
        val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "OPTIONS")
    }
}

class BoundedRetryInterceptor(
    private val policy: BoundedRetryPolicy = BoundedRetryPolicy(),
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var completedRetries = 0
        while (true) {
            try {
                return chain.proceed(request)
            } catch (failure: IOException) {
                if (!policy.shouldRetry(request.method, completedRetries, failure)) throw failure
                completedRetries += 1
            }
        }
    }
}
