package ir.coffevista.vista_native.core.network

import org.json.JSONArray
import org.json.JSONObject

data class ParsedBackendError(
    val code: String?,
    val message: String?,
    val retryAfterSeconds: Int?,
)

object BackendErrorParser {
    fun parse(rawBody: String, retryAfterHeader: String? = null): ParsedBackendError {
        val root = runCatching { JSONObject(rawBody) }.getOrNull()
        val nested = root?.optJSONObject("error")
        val flatError = root?.opt("error") as? String
        val legacy = root?.optJSONArray("errors").firstObjectOrNull()

        return ParsedBackendError(
            code = root?.optionalString("code")
                ?: nested?.optionalString("code")
                ?: legacy?.optionalString("code")
                ?: flatError?.trim()?.takeIf(String::isNotEmpty),
            message = root?.optionalString("message")
                ?: nested?.optionalString("message")
                ?: legacy?.optionalString("message")
                ?: legacy?.optionalString("detail"),
            retryAfterSeconds = root?.positiveInt("retry_after_seconds")
                ?: nested?.positiveInt("retry_after_seconds")
                ?: legacy?.positiveInt("retry_after_seconds")
                ?: retryAfterHeader?.toIntOrNull()?.takeIf { it > 0 },
        )
    }

    private fun JSONArray?.firstObjectOrNull(): JSONObject? =
        this?.optJSONObject(0)

    private fun JSONObject.optionalString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeIf(String::isNotEmpty)
    }

    private fun JSONObject.positiveInt(key: String): Int? =
        optInt(key).takeIf { it > 0 }
}
