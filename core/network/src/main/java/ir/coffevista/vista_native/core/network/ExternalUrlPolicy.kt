package ir.coffevista.vista_native.core.network

import java.net.URI

enum class UrlDisposition {
    EXTERNAL_BROWSER,
    REJECT,
}

data class ExternalUrlDecision(
    val disposition: UrlDisposition,
    val sanitizedUrl: String? = null,
)

class ExternalUrlPolicy(
    private val allowedHosts: Set<String>,
    private val allowedPathPrefixes: Set<String> = setOf("/"),
) {
    fun evaluate(rawUrl: String): ExternalUrlDecision {
        val uri = runCatching { URI(rawUrl) }.getOrNull()
            ?: return ExternalUrlDecision(UrlDisposition.REJECT)
        val host = uri.host?.lowercase()
            ?: return ExternalUrlDecision(UrlDisposition.REJECT)
        if (uri.scheme?.lowercase() != "https") return ExternalUrlDecision(UrlDisposition.REJECT)
        if (uri.userInfo != null || uri.fragment != null) {
            return ExternalUrlDecision(UrlDisposition.REJECT)
        }
        if (host !in allowedHosts.map(String::lowercase)) {
            return ExternalUrlDecision(UrlDisposition.REJECT)
        }
        val path = uri.rawPath.ifEmpty { "/" }
        if (allowedPathPrefixes.none { prefix -> path == prefix || path.startsWith(prefix) }) {
            return ExternalUrlDecision(UrlDisposition.REJECT)
        }
        val sanitized = URI("https", null, host, uri.port, path, uri.rawQuery, null).toString()
        return ExternalUrlDecision(UrlDisposition.EXTERNAL_BROWSER, sanitized)
    }

    fun safeHeaders(source: Map<String, String>): Map<String, String> =
        source.filterKeys { key ->
            key.lowercase() !in SENSITIVE_HEADERS
        }

    private companion object {
        val SENSITIVE_HEADERS = setOf(
            "authorization",
            "cookie",
            "proxy-authorization",
            "x-api-key",
        )
    }
}
