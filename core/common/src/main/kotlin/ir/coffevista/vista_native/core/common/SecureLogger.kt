package ir.coffevista.vista_native.core.common

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

data class LogRecord(
    val level: LogLevel,
    val event: String,
    val fields: Map<String, String>,
)

fun interface LogSink {
    fun write(record: LogRecord)
}

interface SecureLogger {
    fun log(
        level: LogLevel,
        event: String,
        fields: Map<String, Any?> = emptyMap(),
    )
}

class RedactingLogger(
    private val enabled: Boolean,
    private val sink: LogSink,
) : SecureLogger {
    override fun log(level: LogLevel, event: String, fields: Map<String, Any?>) {
        if (!enabled) return
        val safeFields = fields.mapValues { (key, value) ->
            if (SENSITIVE_KEY.containsMatchIn(key)) {
                REDACTED
            } else {
                redactValue(value?.toString().orEmpty())
            }
        }
        sink.write(LogRecord(level, redactValue(event), safeFields))
    }

    private fun redactValue(value: String): String = value
        .replace(BEARER, "Bearer $REDACTED")
        .replace(JWT, REDACTED)
        .replace(IRAN_PHONE, REDACTED)

    private companion object {
        const val REDACTED = "[REDACTED]"
        val SENSITIVE_KEY = Regex(
            "(?i)(token|authorization|cookie|phone|message|payload|key|secret|password|credential)",
        )
        val BEARER = Regex("(?i)Bearer\\s+[^\\s,;]+")
        val JWT = Regex("[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}")
        val IRAN_PHONE = Regex("(?<!\\d)(?:\\+98|0098|0)?9\\d{9}(?!\\d)")
    }
}
