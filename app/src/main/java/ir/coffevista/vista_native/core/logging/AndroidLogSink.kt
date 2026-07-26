package ir.coffevista.vista_native.core.logging

import android.util.Log
import ir.coffevista.vista_native.core.common.LogLevel
import ir.coffevista.vista_native.core.common.LogRecord
import ir.coffevista.vista_native.core.common.LogSink

class AndroidLogSink : LogSink {
    override fun write(record: LogRecord) {
        val message = buildString {
            append(record.event)
            record.fields.toSortedMap().forEach { (key, value) ->
                append(" ")
                append(key)
                append("=")
                append(value)
            }
        }
        when (record.level) {
            LogLevel.DEBUG -> Log.d(TAG, message)
            LogLevel.INFO -> Log.i(TAG, message)
            LogLevel.WARN -> Log.w(TAG, message)
            LogLevel.ERROR -> Log.e(TAG, message)
        }
    }

    private companion object {
        const val TAG = "VistaFoundation"
    }
}
