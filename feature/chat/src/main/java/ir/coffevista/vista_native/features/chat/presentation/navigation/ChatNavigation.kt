package ir.coffevista.vista_native.features.chat.presentation.navigation

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ChatDestination(
    val conversationId: String,
    val messageId: String? = null,
)

object ChatNavigationContract {
    private val json = Json { ignoreUnknownKeys = true }

    /** Flutter accepts vista://chat/{conversationId} and chat URL/query variants. */
    fun fromDeepLink(rawUri: String): ChatDestination? {
        val uri = runCatching { URI(rawUri) }.getOrNull() ?: return null
        val pathSegments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
        val query = uri.rawQuery.orEmpty().split('&').mapNotNull { pair ->
            val parts = pair.split('=', limit = 2)
            if (parts.isEmpty() || parts[0].isBlank()) null else {
                URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name()) to
                    URLDecoder.decode(parts.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
            }
        }.toMap()
        val isVistaChat = uri.scheme.equals("vista", true) && uri.host.equals("chat", true)
        val isHttpChat = uri.scheme in setOf("http", "https") &&
            pathSegments.any { it.equals("chat", true) }
        if (!isVistaChat && !isHttpChat) return null
        val idFromPath = if (isVistaChat) {
            pathSegments.firstOrNull()
        } else {
            pathSegments.dropWhile { !it.equals("chat", true) }.drop(1).firstOrNull()
        }
        val conversationId = (
            idFromPath ?: query["conversationId"] ?: query["conversation_id"]
            )?.trim().orEmpty()
        if (conversationId.isEmpty()) return null
        return ChatDestination(
            conversationId = conversationId,
            messageId = query["messageId"] ?: query["message_id"],
        )
    }

    fun fromNotification(data: Map<String, String>): ChatDestination? {
        val normalized = normalizeNotificationData(data)
        val type = normalized["type"]?.trim()?.lowercase()
        if (type != null && type !in setOf("chat_message", "message")) return null
        val conversationId = (
            normalized["conversation_id"] ?: normalized["conversationId"]
            )?.trim().orEmpty()
        if (conversationId.isEmpty()) return null
        return ChatDestination(
            conversationId = conversationId,
            messageId = normalized["message_id"] ?: normalized["messageId"],
        )
    }

    /** Mirrors Flutter's flat and nested-JSON notification compatibility. */
    fun normalizeNotificationData(data: Map<String, String>): Map<String, String> {
        val nested = data["data"]
            ?.let { raw -> runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull() }
            ?.mapNotNull { (key, value) ->
                runCatching { key to value.jsonPrimitive.content }.getOrNull()
            }
            ?.toMap()
            .orEmpty()
        return nested + data.filterKeys { it != "data" }
    }
}
