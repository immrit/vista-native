package ir.coffevista.vista_native.features.chat.data.remote

import java.net.URI
import java.net.URLEncoder

/**
 * Provides URL rewrites and proxy endpoints optimized for loading media in Iran,
 * bypassing ISP-level domain filtering (e.g. Tenor/Giphy CDNs) and converting
 * previews to lightweight WebP for rapid loading on mobile networks.
 */
object IranMediaProxy {
    private const val PROXY_BASE = "https://wsrv.nl/"

    private val BLOCKED_HOST_PATTERNS = listOf(
        Regex(".*\\.tenor\\.com$", RegexOption.IGNORE_CASE),
        Regex(".*\\.giphy\\.com$", RegexOption.IGNORE_CASE),
        Regex(".*\\.gfycat\\.com$", RegexOption.IGNORE_CASE),
        Regex(".*\\.imgur\\.com$", RegexOption.IGNORE_CASE),
        Regex(".*\\.giphy-preview\\.com$", RegexOption.IGNORE_CASE),
    )

    fun isHostBlocked(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val cleanHost = host.lowercase().trim()
        return BLOCKED_HOST_PATTERNS.any { it.matches(cleanHost) }
    }

    /**
     * Converts a media URL to an Iran-accessible URL.
     *
     * @param url The raw media URL.
     * @param preview If true, generates an optimized WebP thumbnail (~2-8 KB) for fast grid display.
     */
    fun toIranAccessibleUrl(url: String, preview: Boolean = false): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.startsWith("file://") ||
            trimmed.startsWith("content://") ||
            trimmed.startsWith("android.resource://") ||
            trimmed.startsWith("data:")
        ) {
            return trimmed
        }

        // If already proxied, do not wrap twice
        if (trimmed.contains("wsrv.nl") || trimmed.contains("images.weserv.nl")) {
            return trimmed
        }

        val host = runCatching { URI(trimmed).host }.getOrNull()
        val needsProxy = isHostBlocked(host) || trimmed.endsWith(".gif", ignoreCase = true)

        return if (needsProxy) {
            val encoded = runCatching { URLEncoder.encode(trimmed, "UTF-8") }.getOrDefault(trimmed)
            if (preview) {
                "${PROXY_BASE}?url=$encoded&w=220&output=webp"
            } else {
                "${PROXY_BASE}?url=$encoded&output=gif&n=-1"
            }
        } else {
            trimmed
        }
    }
}
