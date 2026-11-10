package ir.coffevista.vista_native.features.chat.domain.model

import androidx.compose.runtime.Immutable
import ir.coffevista.vista_native.core.model.navigation.VistaGameUrlHelper
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

@Immutable
@Serializable
data class DuelInviteDraft(
    val matchId: String,
    val duelUrl: String,
    val inviterId: String? = null,
    val inviterName: String? = null,
    val inviterAvatar: String? = null,
    val entryFee: Long? = null,
    val captionText: String? = null,
    val isStructured: Boolean = false,
) {
    val targetGamePath: String
        get() = "/game/duel/create?code=$matchId"
}

@Immutable
data class DuelPreviewPlayer(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
)

@Immutable
data class DuelPreview(
    val matchId: String,
    val status: String, // waiting | in_progress | finished | cancelled | expired
    val gameEnabled: Boolean = true,
    val player1: DuelPreviewPlayer,
    val player2: DuelPreviewPlayer? = null,
    val entryFee: Long = 0L,
    val baseEntryFee: Long = 0L,
    val winCoinBonus: Long = 0L,
    val canJoin: Boolean = false,
    val isParticipant: Boolean = false,
    val viewerAtLimit: Boolean = false,
    val cancelReason: String? = null,
)

/**
 * Parses both structured `gameInvite` JSON payloads and plain/shared Vista Quiz
 * duel or match URLs inside chat messages into a canonical [DuelInviteDraft].
 */
object DuelInviteParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val URL_SCAN_REGEX = Regex(
        """(?:https?://(?:www\.)?(?:cafevista\.ir|coffevista\.ir|vista\.me)|vista://)\S+""",
        RegexOption.IGNORE_CASE,
    )

    fun serializePayload(
        matchId: String,
        inviterId: String? = null,
        inviterName: String? = null,
        inviterAvatar: String? = null,
        entryFee: Long? = null,
    ): String {
        val cleanMatchId = matchId.trim().lowercase()
        val obj = buildJsonObject {
            put("type", "gameInvite")
            put("gameId", "vista-quiz")
            put("matchId", cleanMatchId)
            put("duelUrl", VistaGameUrlHelper.buildDuelWebUrl(cleanMatchId))
            inviterId?.trim()?.takeIf { it.isNotEmpty() }?.let { put("inviterId", it) }
            inviterName?.trim()?.takeIf { it.isNotEmpty() }?.let { put("inviterName", it.take(80)) }
            inviterAvatar?.trim()?.takeIf { it.isNotEmpty() }?.let { put("inviterAvatar", it.take(500)) }
            if (entryFee != null && entryFee >= 0L) put("entryFee", entryFee)
        }
        return json.encodeToString(JsonObject.serializer(), obj)
    }

    fun tryParse(raw: String?, messageType: String? = null): DuelInviteDraft? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        val isGameMsgType = messageType.equals("gameInvite", ignoreCase = true) ||
            messageType.equals("game_invite", ignoreCase = true)

        if (trimmed.startsWith("{") || isGameMsgType) {
            parseStructuredJson(trimmed, requireType = !isGameMsgType)?.let { return it }
        }

        val match = URL_SCAN_REGEX.findAll(trimmed).firstNotNullOfOrNull { result ->
            val rawUrl = result.value.trimEnd('.', ',', ';', ')', ']', '"', '\'')
            val matchId = VistaGameUrlHelper.extractDuelMatchId(rawUrl)
            if (matchId != null) rawUrl to matchId else null
        } ?: return null

        val (foundUrl, matchId) = match
        val remaining = trimmed.replace(foundUrl, "").trim().takeIf { it.isNotEmpty() }
        return DuelInviteDraft(
            matchId = matchId,
            duelUrl = VistaGameUrlHelper.buildDuelWebUrl(matchId),
            captionText = remaining,
            isStructured = false,
        )
    }

    fun previewSummary(draft: DuelInviteDraft): String {
        val inviter = draft.inviterName?.trim()?.takeIf { it.isNotEmpty() }
        return if (inviter != null) {
            "🎮 دعوت به دوئل ویستا کوییز از طرف $inviter"
        } else {
            "🎮 دعوت به دوئل ویستا کوییز"
        }
    }

    fun formatPreviewIfDuel(raw: String?, messageType: String? = null): String? =
        tryParse(raw, messageType)?.let(::previewSummary)

    private fun parseStructuredJson(raw: String, requireType: Boolean): DuelInviteDraft? {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        val obj = (root["gameInviteData"] as? JsonObject) ?: root
        val type = obj.str("type")?.lowercase()
        val isExplicitGameInvite = type == "gameinvite" || type == "game_invite"
        if (requireType && !isExplicitGameInvite) return null

        val rawMatchId = obj.str("matchId") ?: obj.str("match_id")
        val rawDuelUrl = obj.str("duelUrl") ?: obj.str("duel_url") ?: obj.str("url")
        val resolvedMatchId = rawMatchId?.takeIf(VistaGameUrlHelper::isValidUuid)?.lowercase()
            ?: VistaGameUrlHelper.extractDuelMatchId(rawDuelUrl)
            ?: return null

        return DuelInviteDraft(
            matchId = resolvedMatchId,
            duelUrl = rawDuelUrl?.takeIf { it.isNotBlank() }
                ?: VistaGameUrlHelper.buildDuelWebUrl(resolvedMatchId),
            inviterId = obj.str("inviterId") ?: obj.str("inviter_id"),
            inviterName = obj.str("inviterName") ?: obj.str("inviter_name"),
            inviterAvatar = obj.str("inviterAvatar") ?: obj.str("inviter_avatar"),
            entryFee = obj.long("entryFee") ?: obj.long("entry_fee"),
            isStructured = true,
        )
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.long(key: String): Long? =
        (this[key] as? JsonPrimitive)?.longOrNull
}
