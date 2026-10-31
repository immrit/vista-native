package ir.coffevista.vista_native.features.stories.data

import android.content.Context
import android.net.Uri
import android.system.Os
import java.io.File
import java.net.URI
import java.util.Locale

object StorySecurityGuard {

    private val UUID_REGEX = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )

    private val FORBIDDEN_SCHEME_PREFIXES = listOf(
        "javascript:",
        "data:",
        "vbscript:",
        "file:",
        "content:",
        "intent:",
        "about:",
        "blob:",
        "jar:",
    )

    fun isValidUuid(value: String?): Boolean {
        val candidate = value?.trim().orEmpty()
        return candidate.length == 36 && UUID_REGEX.matches(candidate)
    }

    internal fun isSafeCanonicalPath(
        canonicalPath: String,
        appDataDir: String,
        allowedAppCacheDirs: List<String>,
    ): Boolean {
        val normTarget = normalizePath(canonicalPath)
        if (normTarget.isBlank()) return false

        val lowerTarget = normTarget.lowercase(Locale.ROOT)
        if (lowerTarget == "/proc" || lowerTarget.startsWith("/proc/") ||
            lowerTarget == "/sys" || lowerTarget.startsWith("/sys/")
        ) {
            return false
        }

        val normDataDir = normalizePath(appDataDir)
        if (normDataDir.isNotBlank() && isSameOrChildPath(normTarget, normDataDir)) {
            return allowedAppCacheDirs.any { allowed ->
                val normAllowed = normalizePath(allowed)
                normAllowed.isNotBlank() && isSameOrChildPath(normTarget, normAllowed)
            }
        }
        return true
    }

    private fun normalizePath(path: String): String {
        val unified = path.trim().replace('\\', '/')
        return if (unified.length > 1) unified.trimEnd('/') else unified
    }

    private fun isSameOrChildPath(target: String, parent: String): Boolean {
        if (target.equals(parent, ignoreCase = true)) return true
        val prefix = if (parent.endsWith("/")) parent else "$parent/"
        return target.startsWith(prefix, ignoreCase = true)
    }

    fun isSafeReadableMediaUri(context: Context, uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme != "content" && scheme != "file") return false

        val appDataDir = runCatching {
            File(context.applicationInfo.dataDir).canonicalPath
        }.getOrDefault(context.applicationInfo.dataDir.orEmpty())

        val allowedDirs = listOfNotNull(
            runCatching { File(context.cacheDir, "camera").canonicalPath }.getOrNull(),
            runCatching { File(context.cacheDir, "incoming_share").canonicalPath }.getOrNull(),
        )

        if (scheme == "file") {
            val rawPath = uri.path ?: return false
            val canonical = runCatching { File(rawPath).canonicalPath }.getOrNull() ?: return false
            return isSafeCanonicalPath(canonical, appDataDir, allowedDirs)
        }

        // For content:// URIs, inspect underlying file descriptor symlink if backed by a real file
        val pfd = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }.getOrNull()
            ?: return false
        return pfd.use { descriptor ->
            val linkTarget = runCatching { Os.readlink("/proc/self/fd/${descriptor.fd}") }.getOrNull()
            if (linkTarget.isNullOrBlank() || !linkTarget.startsWith("/")) {
                // Pipe / virtual stream from ContentProvider (not a local private file)
                true
            } else {
                val canonical = runCatching { File(linkTarget).canonicalPath }.getOrDefault(linkTarget)
                isSafeCanonicalPath(canonical, appDataDir, allowedDirs)
            }
        }
    }

    fun normalizeSafeHttpUrl(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.length > 2048) return null
        if (trimmed.any { it <= ' ' || it.isISOControl() }) return null

        val lower = trimmed.lowercase(Locale.ROOT)
        if (FORBIDDEN_SCHEME_PREFIXES.any { lower.startsWith(it) }) return null

        val candidate = when {
            lower.startsWith("http://") || lower.startsWith("https://") -> trimmed
            "://" !in lower && '.' in trimmed && !trimmed.startsWith(".") && !trimmed.startsWith("/") ->
                "https://$trimmed"
            else -> return null
        }

        val parsed = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = parsed.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (parsed.userInfo != null) return null

        val host = parsed.host?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (host.isBlank() || '.' !in host || host.startsWith(".") || host.endsWith(".")) {
            return null
        }
        return candidate
    }

    fun extractDisplayHost(rawUrl: String?): String {
        val safe = normalizeSafeHttpUrl(rawUrl) ?: return ""
        val host = runCatching { URI(safe).host.orEmpty() }.getOrDefault("")
        return host.removePrefix("www.")
    }

    fun mapReactionEmoji(reaction: String?): String? {
        val clean = reaction?.trim().orEmpty()
        if (clean.isEmpty()) return null
        return when (clean.lowercase(Locale.ROOT)) {
            "unlike", "none", "null" -> null
            "heart", "like", "love" -> "❤️"
            "laugh", "haha", "joy" -> "😂"
            "wow", "surprised" -> "😮"
            "sad", "cry" -> "😢"
            "fire", "lit" -> "🔥"
            "clap" -> "👏"
            "party" -> "🎉"
            "100" -> "💯"
            else -> clean.take(12)
        }
    }
}
