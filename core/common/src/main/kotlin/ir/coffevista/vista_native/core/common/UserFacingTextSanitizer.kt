package ir.coffevista.vista_native.core.common

private val JSON_OBJECT_PREFIX_REGEX = Regex("""^\{\s*"[^"\r\n]+"\s*:""")
private val JSON_ARRAY_OF_OBJECTS_PREFIX_REGEX = Regex("""^\[\s*\{\s*"[^"\r\n]+"\s*:""")

/**
 * Returns true when [raw] looks like a JSON object or JSON array-of-objects
 * (including truncated payloads that lost their closing brace/bracket).
 */
fun looksLikeJsonPayload(raw: String?): Boolean {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return false
    if (trimmed == "{}" || trimmed == "[]") return true
    if (JSON_OBJECT_PREFIX_REGEX.containsMatchIn(trimmed)) return true
    if (JSON_ARRAY_OF_OBJECTS_PREFIX_REGEX.containsMatchIn(trimmed)) return true
    if (trimmed.startsWith("{\"") && (trimmed.endsWith("...") || trimmed.endsWith("…"))) {
        return true
    }
    return false
}

private val TECHNICAL_ERROR_PATTERNS = listOf(
    "exception:",
    "error:",
    "\n\tat ",
    "expected begin_",
    "unexpected json",
    "jsondecodingexception",
    "unable to resolve host",
    "failed to connect",
    "connection reset",
    "software caused connection abort",
    "read timed out",
    "connect timed out",
    "cleartext communication",
    "javax.net.",
    "java.io.",
    "java.net.",
    "kotlinx.serialization.",
    "org.json.",
)

/**
 * Ensures an exception or server-supplied error string never surfaces raw JSON,
 * HTML error pages, stack traces, or technical transport/serialization details to users.
 */
fun String?.toSafeUserError(fallbackFa: String): String {
    val trimmed = this?.trim().orEmpty()
    if (trimmed.isEmpty()) return fallbackFa
    if (looksLikeJsonPayload(trimmed)) return fallbackFa
    val lower = trimmed.lowercase()
    if (lower.startsWith("<!doctype") || lower.startsWith("<html") || lower.contains("<body")) {
        return fallbackFa
    }
    if (TECHNICAL_ERROR_PATTERNS.any { pattern -> lower.contains(pattern) }) {
        return fallbackFa
    }
    return trimmed
}

fun Throwable.toSafeUserError(fallbackFa: String): String =
    message.toSafeUserError(fallbackFa)
