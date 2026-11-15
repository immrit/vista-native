package ir.coffevista.vista_native.features.chat.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Immutable
@Serializable
data class SessionAlertPayload(
    val type: String = "",
    val version: Int = 1,
    @SerialName("session_id") val sessionId: String = "",
    val status: String = "pending", // pending | approved | rejected
    @SerialName("device_name") val deviceName: String = "",
    val platform: String = "",
    @SerialName("os_version") val osVersion: String = "",
    @SerialName("app_version") val appVersion: String = "",
    @SerialName("ip_address") val ipAddress: String = "",
    val location: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val summary: String = "",
)

object SessionAlertParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun tryParse(raw: String?, transportType: String? = null): SessionAlertPayload? {
        val trimmed = raw?.trim().orEmpty()
        val isAlertType = transportType.equals("session_alert", ignoreCase = true) ||
            transportType.equals("sessionalert", ignoreCase = true)
        if (!trimmed.startsWith("{")) {
            return null
        }
        if (!isAlertType && !trimmed.contains("\"vista_session_alert\"")) {
            return null
        }
        val parsed = runCatching { json.decodeFromString<SessionAlertPayload>(trimmed) }.getOrNull()
            ?: return null
        if (parsed.type != "vista_session_alert" || parsed.sessionId.isBlank()) {
            return null
        }
        return parsed
    }

    fun formatPreview(raw: String?, transportType: String? = null): String? {
        val alert = tryParse(raw, transportType) ?: return null
        val device = alert.deviceName.ifBlank { alert.platform.ifBlank { "دستگاه ناشناس" } }
        return when (alert.status.lowercase()) {
            "approved" -> "✅ ورود تایید شد: $device"
            "rejected" -> "🛑 نشست مسدود و خارج شد: $device"
            else -> "🔐 هشدار ورود جدید: $device"
        }
    }
}
