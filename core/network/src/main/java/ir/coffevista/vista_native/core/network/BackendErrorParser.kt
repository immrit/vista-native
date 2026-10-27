package ir.coffevista.vista_native.core.network

import org.json.JSONArray
import org.json.JSONObject

data class ParsedBackendError(
    val code: String?,
    val message: String?,
    val retryAfterSeconds: Int?,
    val feature: String? = null,
)

object BackendErrorParser {
    private val KNOWN_MACHINE_CODES = setOf(
        "banned",
        "account_banned",
        "account_suspended",
        "account_disabled",
        "maintenance_mode",
        "feature_disabled",
    )

    fun parse(rawBody: String, retryAfterHeader: String? = null): ParsedBackendError {
        val root = runCatching { JSONObject(rawBody) }.getOrNull()
        val nested = root?.optJSONObject("error")
        val flatError = (root?.opt("error") as? String)?.trim()?.takeIf(String::isNotEmpty)
        val legacy = root?.optJSONArray("errors").firstObjectOrNull()

        val rawCode = root?.optionalString("code")
            ?: nested?.optionalString("code")
            ?: legacy?.optionalString("code")
            ?: flatError

        val explicitMessage = root?.optionalString("message")
            ?: nested?.optionalString("message")
            ?: legacy?.optionalString("message")
            ?: legacy?.optionalString("detail")

        val promotedCode = if (
            rawCode != null &&
            rawCode.equals("error", ignoreCase = true) &&
            explicitMessage != null &&
            explicitMessage.lowercase() in KNOWN_MACHINE_CODES
        ) {
            explicitMessage.lowercase()
        } else {
            rawCode
        }

        val resolvedMessage = explicitMessage
            ?: flatError?.takeIf { candidate ->
                candidate.any { ch -> ch in '\u0600'..'\u06FF' } || candidate.contains(' ')
            }

        val feature = root?.optionalString("feature")
            ?: nested?.optionalString("feature")
            ?: legacy?.optionalString("feature")

        return ParsedBackendError(
            code = promotedCode,
            message = resolvedMessage,
            retryAfterSeconds = root?.positiveInt("retry_after_seconds")
                ?: nested?.positiveInt("retry_after_seconds")
                ?: legacy?.positiveInt("retry_after_seconds")
                ?: retryAfterHeader?.toIntOrNull()?.takeIf { it > 0 },
            feature = feature,
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
