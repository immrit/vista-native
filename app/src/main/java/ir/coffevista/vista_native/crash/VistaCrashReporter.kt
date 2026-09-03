package ir.coffevista.vista_native.crash

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.format.DateTimeFormatter
import org.json.JSONObject

object VistaCrashReporter {
    private const val TAG = "VistaCrashReporter"
    private const val MAX_LOG_SIZE_BYTES = 512 * 1024L // 512 KB cap
    private const val CRASH_LOG_FILE = "crash_reports.log"

    private var initialized = false
    private var logFile: File? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    // Redaction patterns matching SecureLogger
    private val BEARER = Regex("(?i)Bearer\\s+[^\\s,;]+")
    private val JWT = Regex("[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}")
    private val IRAN_PHONE = Regex("(?<!\\d)(?:\\+98|0098|0)?9\\d{9}(?!\\d)")

    fun install(context: Context) {
        if (initialized) return
        initialized = true

        val filesDir = runCatching { context.filesDir }.getOrNull()
        if (filesDir != null) {
            logFile = File(filesDir, CRASH_LOG_FILE)
        }

        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordException(throwable, fatal = true, source = "uncaught_${thread.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record fatal crash", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
        Log.i(TAG, "Global crash reporter installed successfully")
    }

    fun recordNonFatal(throwable: Throwable, source: String = "non_fatal") {
        try {
            recordException(throwable, fatal = false, source = source)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record non-fatal exception", e)
        }
    }

    private fun recordException(throwable: Throwable, fatal: Boolean, source: String) {
        val stackTraceString = StringWriter().use { sw ->
            PrintWriter(sw).use { pw ->
                throwable.printStackTrace(pw)
            }
            sw.toString()
        }

        val redactedError = redact(throwable.message ?: throwable.javaClass.simpleName)
        val redactedTrace = redact(stackTraceString)

        val json = JSONObject().apply {
            put("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            put("fatal", fatal)
            put("source", source)
            put("exception_class", throwable.javaClass.name)
            put("error", redactedError)
            put("stackTrace", redactedTrace)
        }

        writeToFile(json.toString())
    }

    @Synchronized
    private fun writeToFile(entry: String) {
        val file = logFile ?: return
        try {
            if (file.exists() && file.length() > MAX_LOG_SIZE_BYTES) {
                val text = file.readText()
                val lines = text.lines()
                val pruned = lines.takeLast(lines.size / 2).joinToString("\n")
                file.writeText(pruned)
            }
            FileWriter(file, true).use { writer ->
                writer.appendLine(entry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash entry to disk", e)
        }
    }

    private fun redact(value: String): String = value
        .replace(BEARER, "Bearer [REDACTED]")
        .replace(JWT, "[REDACTED]")
        .replace(IRAN_PHONE, "[REDACTED]")
}
