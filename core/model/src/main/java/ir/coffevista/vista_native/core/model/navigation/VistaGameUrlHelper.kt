package ir.coffevista.vista_native.core.model.navigation

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Canonical parser, sanitizer, and builder for Vista Quiz game and duel URLs.
 * Shared across deep links, WebView bridge, internal navigation, and chat cards
 * to guarantee consistent routing without host/path divergence.
 */
object VistaGameUrlHelper {
    val VISTA_WEB_HOSTS: Set<String> = setOf(
        "cafevista.ir",
        "www.cafevista.ir",
        "coffevista.ir",
        "www.coffevista.ir",
        "vista.me",
    )

    private val UUID_REGEX = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )

    private val SAFE_ID_REGEX = Regex("^[A-Za-z0-9._-]{1,128}$")

    fun isVistaWebHost(host: String?): Boolean =
        host?.trim()?.lowercase() in VISTA_WEB_HOSTS

    fun isValidUuid(value: String?): Boolean =
        !value.isNullOrBlank() && UUID_REGEX.matches(value.trim())

    fun buildDuelWebUrl(matchId: String): String =
        "https://cafevista.ir/game/duel/create?code=${URLEncoder.encode(matchId.trim().lowercase(), "UTF-8")}"

    /**
     * Validates and normalizes an internal game target path (e.g. `/game`,
     * `/game/duel/create?code=...`, `/game/match/...`, `/game/profile/...`).
     * Rejects authority tricks (`//`), backslashes, schemes (`:`), control chars,
     * traversal (`..`), and `/game/sso` loops.
     */
    fun sanitizeGameTargetPath(rawPath: String?): String? {
        val candidate = rawPath?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!candidate.startsWith("/") || candidate.startsWith("//")) return null
        if (candidate.contains('\\') || candidate.contains(':') || candidate.contains("..")) return null
        if (candidate.any { it.code in 0x00..0x1f || it.code == 0x7f }) return null

        val uri = runCatching { URI("https://cafevista.ir$candidate") }.getOrNull() ?: return null
        val normalizedPath = uri.path.orEmpty().ifBlank { return null }
        if (normalizedPath == "/game/sso" || normalizedPath.startsWith("/game/sso/")) return null
        if (normalizedPath != "/game" && !normalizedPath.startsWith("/game/")) return null

        val segments = normalizedPath.split('/').filter(String::isNotBlank)
        if (segments.isEmpty() || segments.first().lowercase() != "game") return null

        return when (segments.size) {
            1 -> "/game"
            2 -> when (val section = segments[1].lowercase()) {
                "lobby", "store", "profile", "play", "match", "missions", "spin", "leaderboard" ->
                    "/game/$section"
                else -> null
            }
            3 -> when (val section = segments[1].lowercase()) {
                "duel" -> when (segments[2].lowercase()) {
                    "create" -> buildSanitizedDuelCreatePath(uri.rawQuery)
                    else -> null
                }
                "match", "play", "profile" -> {
                    val id = segments[2].trim()
                    if (!SAFE_ID_REGEX.matches(id)) null else "/game/$section/$id"
                }
                else -> null
            }
            else -> null
        }
    }

    /**
     * Parses any HTTPS Vista URL or `vista://game` / `vista://duel` URI into a
     * sanitized `/game...` relative path ready for WebView SSO (`next=`).
     */
    fun parseGameTargetPath(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()

        if (uri.userInfo != null || uri.port != -1) return null

        return when (scheme) {
            "https" -> {
                if (!isVistaWebHost(uri.host)) return null
                val pathAndQuery = buildString {
                    append(uri.path.orEmpty())
                    if (!uri.rawQuery.isNullOrBlank()) {
                        append('?')
                        append(uri.rawQuery)
                    }
                }
                sanitizeGameTargetPath(pathAndQuery)
            }
            "vista" -> {
                val host = uri.host?.lowercase() ?: return null
                val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
                val query = uri.rawQuery?.takeIf { it.isNotBlank() }
                when (host) {
                    "game" -> {
                        val candidatePath = buildString {
                            append("/game")
                            if (segments.isNotEmpty()) {
                                append('/')
                                append(segments.joinToString("/"))
                            }
                            if (query != null) {
                                append('?')
                                append(query)
                            }
                        }
                        if (segments == listOf("duel") && query != null) {
                            sanitizeGameTargetPath("/game/duel/create?$query")
                        } else {
                            sanitizeGameTargetPath(candidatePath)
                        }
                    }
                    "duel" -> {
                        if (segments.size > 1) return null
                        if (segments.size == 1) {
                            val code = segments.single().takeIf(::isValidUuid)?.lowercase() ?: return null
                            "/game/duel/create?code=$code"
                        } else {
                            buildSanitizedDuelCreatePath(query)
                        }
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    /**
     * Extracts a duel/match UUID from an invite/match URL when present.
     */
    fun extractDuelMatchId(rawUrl: String?): String? {
        val targetPath = parseGameTargetPath(rawUrl) ?: return null
        val uri = runCatching { URI("https://cafevista.ir$targetPath") }.getOrNull() ?: return null
        val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
        if (segments.size >= 3 && segments[0] == "game" && segments[1] == "duel" && segments[2] == "create") {
            return extractQueryParams(uri.rawQuery)["code"]?.takeIf(::isValidUuid)?.lowercase()
        }
        if (segments.size == 3 && segments[0] == "game" && (segments[1] == "match" || segments[1] == "play")) {
            return segments[2].takeIf(::isValidUuid)?.lowercase()
        }
        return null
    }

    private fun buildSanitizedDuelCreatePath(rawQuery: String?): String? {
        val params = extractQueryParams(rawQuery)
        val rawCode = params["code"]?.trim()
        if (rawCode != null && !isValidUuid(rawCode)) return null
        val kept = buildList {
            rawCode?.let {
                add("code=${URLEncoder.encode(it.lowercase(), "UTF-8")}")
            }
            params["opponent"]?.trim()?.takeIf(SAFE_ID_REGEX::matches)?.let {
                add("opponent=${URLEncoder.encode(it, "UTF-8")}")
            }
            params["chatConvId"]?.trim()?.takeIf(SAFE_ID_REGEX::matches)?.let {
                add("chatConvId=${URLEncoder.encode(it, "UTF-8")}")
            }
        }
        return if (kept.isEmpty()) "/game/duel/create"
        else "/game/duel/create?${kept.joinToString("&")}"
    }

    private fun extractQueryParams(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        val result = linkedMapOf<String, String>()
        for (pair in rawQuery.split('&')) {
            if (pair.isBlank()) continue
            val idx = pair.indexOf('=')
            val key = runCatching {
                URLDecoder.decode(if (idx >= 0) pair.substring(0, idx) else pair, "UTF-8")
            }.getOrNull() ?: continue
            val value = if (idx >= 0) {
                runCatching { URLDecoder.decode(pair.substring(idx + 1), "UTF-8") }.getOrNull().orEmpty()
            } else ""
            if (key.isNotBlank() && value.isNotBlank()) {
                result[key] = value
            }
        }
        return result
    }
}
