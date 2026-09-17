package ir.coffevista.vista_native.features.chat.data.mapper

import ir.coffevista.vista_native.features.chat.data.local.ConversationEntity
import ir.coffevista.vista_native.features.chat.data.local.MessageEntity
import ir.coffevista.vista_native.features.chat.data.remote.ConversationDto
import ir.coffevista.vista_native.features.chat.data.remote.ConversationParticipantDto
import ir.coffevista.vista_native.features.chat.data.remote.MessageDto
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.Attachment
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import ir.coffevista.vista_native.features.chat.domain.model.TransferState
import ir.coffevista.vista_native.features.chat.domain.model.chatMessageTypePreview
import ir.coffevista.vista_native.features.chat.domain.repository.ChatContentCipher
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun String?.epochMillisOrNull(): Long? {
    val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val normalized = value.replace(Regex("(\\.\\d{3})\\d+(?=Z|[+-]\\d{2}:?\\d{2}$)"), "$1")
    val patterns = if (normalized.contains('.')) {
        listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    } else {
        listOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ssX")
    }
    return patterns.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(normalized)?.time
        }.getOrNull()
    }
}

/** Mirrors Flutter AvatarAssetUtils.resolveUrl for backend storage keys. */
internal fun String?.resolvedAvatarUrl(): String? {
    val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (value.startsWith("http://") || value.startsWith("https://")) return value
    if (value.startsWith("//")) return "https:$value"
    // Flutter intentionally preserves bundled avatar sources so system peers
    // (for example vista_service) render their shipped identity asset.
    if (value.startsWith("asset://") || value.startsWith("lib/") || value.startsWith("assets/")) return value
    return "$AVATAR_CDN_BASE/${value.removePrefix("/")}"
}

internal suspend fun ConversationDto.toEntity(
    accountId: String,
    cipher: ChatContentCipher,
    now: Long,
): ConversationEntity {
    val participantIdentity = participantPeerIdentity(accountId)
    val effectivePeerId = peerId?.trim()?.takeIf(String::isNotEmpty) ?: participantIdentity?.userId
    val effectiveUnreadCount = allParticipants()
        .firstOrNull { it.resolvedUserId == accountId }
        ?.unreadCount
        ?: unreadCount
    return ConversationEntity(
    accountId = accountId,
    id = id,
    type = resolvedConversationType.name,
    title = listOf(
        name,
        peerUsername,
        otherUserUsername,
        username,
        peerFullName,
        otherUserFullName,
        participantIdentity?.username,
        participantIdentity?.fullName,
    )
        .firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotEmpty) }
        ?: "کاربر",
    avatarUrl = listOf(image, peerAvatarUrl, otherUserAvatar, avatarUrl, participantIdentity?.avatarUrl)
        .firstNotNullOfOrNull { it.resolvedAvatarUrl() },
    peerId = effectivePeerId,
    lastMessageCiphertext = conversationPreview()?.let { preview ->
        cipher.encrypt(accountId, id, "conversation-preview", preview)
    },
    lastMessageAtEpochMillis = (lastMessageAt ?: lastMessageTime).epochMillisOrNull(),
    unreadCount = effectiveUnreadCount.coerceAtLeast(0),
    isArchived = isArchived,
    isPinned = isPinned,
    isMuted = isMuted,
    requestStatus = status ?: messageRequestStatus ?: requestStatus,
    lastSyncedAtEpochMillis = now,
    isMessageRequest = isMessageRequest || messageRequest,
    lastMessageType = lastMessageType,
    lastMessageIsMine = isLastMessageFromMe ?: (lastMessageSenderId?.trim() == accountId),
    lastMessageStatus = resolvedLastMessageStatus.name,
)
}

private val ConversationDto.resolvedLastMessageStatus: MessageStatus
    get() = when (lastMessageDeliveryStatus?.trim()?.lowercase()) {
        "failed" -> MessageStatus.FAILED
        "read", "seen" -> MessageStatus.READ
        "delivered" -> MessageStatus.DELIVERED
        "sent" -> MessageStatus.SENT
        "pending" -> MessageStatus.PENDING
        else -> when {
            lastMessageIsRead || lastMessageIsSeen -> MessageStatus.READ
            lastMessageIsDelivered -> MessageStatus.DELIVERED
            lastMessageIsSent -> MessageStatus.SENT
            else -> MessageStatus.PENDING
        }
    }

private data class ParticipantPeerIdentity(
    val userId: String,
    val username: String?,
    val fullName: String?,
    val avatarUrl: String?,
)

private val ConversationParticipantDto.resolvedUserId: String?
    get() = listOf(userId, id).firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotEmpty) }

private fun ConversationDto.allParticipants(): List<ConversationParticipantDto> =
    (conversationParticipants + participants).distinctBy { it.resolvedUserId }

private fun ConversationDto.participantPeerIdentity(accountId: String): ParticipantPeerIdentity? {
    val participant = allParticipants().firstOrNull { it.resolvedUserId != null && it.resolvedUserId != accountId }
        ?: return null
    val profile = participant.profileObject()
    return ParticipantPeerIdentity(
        userId = participant.resolvedUserId ?: return null,
        username = profile.stringValue("username", "user_name"),
        fullName = profile.stringValue("full_name", "name", "display_name"),
        avatarUrl = profile.stringValue("avatar_url", "avatar", "image"),
    )
}

private fun ConversationParticipantDto.profileObject(): JsonObject? {
    val value = profiles ?: profile ?: return null
    return when (value) {
        is JsonObject -> value
        is JsonArray -> value.firstOrNull() as? JsonObject
        else -> null
    }
}

private fun JsonObject?.stringValue(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    this?.get(key)?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
}

private val ConversationDto.isSecretConversation: Boolean
    get() = isSecret ||
        type.equals("secret", ignoreCase = true) ||
        conversationType.equals("secret", ignoreCase = true)

private fun ConversationDto.conversationPreview(): String? {
    val raw = (lastMessageText ?: lastMessage)?.takeIf(String::isNotBlank)
    if (isSecretConversation) {
        raw ?: return null
        return chatMessageTypePreview(lastMessageType, secret = true) ?: "🔒 پیام محرمانه"
    }
    if (raw != null && raw.looksLikeCiphertextPreview()) {
        return "پیام جدید"
    }
    return chatMessageTypePreview(lastMessageType, secret = false) ?: raw
}

private val ConversationDto.resolvedConversationType: ConversationType
    get() {
        if (isSecretConversation) return ConversationType.SECRET
        val raw = (type ?: conversationType).trim().lowercase()
        if (raw == "group" || raw == "group_chat" || raw == "groupchat" || raw.contains("group")) {
            return ConversationType.GROUP
        }
        if (raw in setOf("direct", "private", "dm")) return ConversationType.PRIVATE
        val normalizedPeer = peerId?.trim().orEmpty()
        return if (normalizedPeer.isNotEmpty() && normalizedPeer != NIL_UUID) {
            ConversationType.PRIVATE
        } else {
            ConversationType.GROUP
        }
    }

private const val NIL_UUID = "00000000-0000-0000-0000-000000000000"

internal suspend fun ConversationEntity.toDomain(cipher: ChatContentCipher): Conversation = Conversation(
    accountId = accountId,
    id = id,
    type = runCatching { ConversationType.valueOf(type) }.getOrDefault(ConversationType.PRIVATE),
    title = title,
    avatarUrl = avatarUrl,
    peerId = peerId,
    lastMessage = lastMessageCiphertext?.let {
        val decrypted = runCatching { cipher.decrypt(accountId, id, "conversation-preview", it) }.getOrNull()
        if (decrypted != null && decrypted.looksLikeCiphertextPreview()) {
            "پیام جدید"
        } else {
            decrypted
        }
    },
    lastMessageAtEpochMillis = lastMessageAtEpochMillis,
    unreadCount = unreadCount,
    isArchived = isArchived,
    isPinned = isPinned,
    isMuted = isMuted,
    requestStatus = requestStatus,
    isMessageRequest = isMessageRequest,
    lastMessageType = lastMessageType,
    isLastMessageFromMe = lastMessageIsMine,
    lastMessageStatus = runCatching { MessageStatus.valueOf(lastMessageStatus) }
        .getOrDefault(MessageStatus.SENT),
)

internal fun MessageDto.toDomain(accountId: String): Message {
    val deleted = deletedAt != null
    val contentModel = when {
        deleted -> MessageContent.Deleted
        isSecret -> MessageContent.EncryptedUnavailable
        messageType.equals("sharedPost", true) || messageType.equals("storyReply", true) ->
            MessageContent.Structured(messageType, content)
        else -> MessageContent.Text(content)
    }
    val resolvedUrl = listOf(mediaUrl, attachmentUrl, audioUrl)
        .firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotEmpty) }
    val attachment = resolvedUrl?.let {
        Attachment(
            kind = messageType.toAttachmentKind(),
            remoteUrl = it,
            fileName = attachmentFileName,
            mimeType = attachmentMimeType,
            sizeBytes = attachmentSizeBytes,
            durationSeconds = duration,
            audioTitle = audioTitle,
            audioArtist = audioArtist,
            audioAlbum = audioAlbum,
            mediaGroupId = mediaGroupId,
        )
    }
    return Message(
        accountId = accountId,
        conversationId = conversationId,
        clientId = id,
        serverId = id,
        senderId = senderId,
        content = contentModel,
        createdAtEpochMillis = createdAt.epochMillisOrNull() ?: 0L,
        editedAtEpochMillis = editedAt.epochMillisOrNull(),
        deletedAtEpochMillis = deletedAt.epochMillisOrNull(),
        status = when {
            isRead || isSeen -> MessageStatus.READ
            isDelivered -> MessageStatus.DELIVERED
            isSent -> MessageStatus.SENT
            else -> MessageStatus.PENDING
        },
        replyToMessageId = replyToMessageId,
        replyToContent = if (isSecret) null else replyToContent,
        replyToSenderName = replyToSenderName,
        replyToKind = replyToKind,
        attachment = attachment,
        reactions = reactions.groupBy({ it.emoji }, { it.userId }).mapValues { it.value.toSet() },
        isForwarded = isForwarded,
        forwardedFromSenderName = forwardedFromSenderName,
        originalSenderId = originalSenderId,
        originalMessageId = originalMessageId,
        isPinned = isPinned,
        isMine = false,
    )
}

/**
 * Conversation summaries must never render an opaque encrypted envelope. Some
 * historical server payloads omitted the E2EE prefix and exposed raw Base64.
 * Prefer a neutral preview to risking ciphertext disclosure in the inbox.
 */
private fun String.looksLikeCiphertextPreview(): Boolean {
    val value = trim()
    if (value.startsWith("VE2E1:") || value.startsWith("VE2E2:") || value.startsWith("e2ee:v1:")) {
        return true
    }
    return value.length >= 48 && value.all { char ->
        char.isLetterOrDigit() || char == '+' || char == '/' || char == '_' || char == '-' || char == '='
    }
}

internal suspend fun Message.toEntity(cipher: ChatContentCipher, now: Long): MessageEntity {
    val (kind, plaintext) = when (val value = content) {
        is MessageContent.Text -> "text" to value.value
        is MessageContent.Structured -> "structured:${value.kind}" to value.payload
        MessageContent.Deleted -> "deleted" to ""
        MessageContent.EncryptedUnavailable -> "encrypted_unavailable" to ""
    }
    return MessageEntity(
        accountId = accountId,
        conversationId = conversationId,
        clientId = clientId,
        serverId = serverId,
        senderId = senderId,
        contentCiphertext = cipher.encrypt(accountId, conversationId, clientId, plaintext),
        contentKind = kind,
        createdAtEpochMillis = createdAtEpochMillis,
        editedAtEpochMillis = editedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        status = status.name,
        replyToMessageId = replyToMessageId,
        replyToContentCiphertext = replyToContent?.let {
            cipher.encrypt(accountId, conversationId, "$clientId:reply", it)
        },
        isMine = isMine,
        lastMutationAtEpochMillis = now,
        messageType = when (val value = content) {
            is MessageContent.Structured -> value.kind
            else -> attachment?.kind?.wireName ?: "text"
        },
        mediaUrl = attachment?.remoteUrl,
        localUri = attachment?.localUri,
        attachmentFileName = attachment?.fileName,
        attachmentMimeType = attachment?.mimeType,
        attachmentSizeBytes = attachment?.sizeBytes,
        durationSeconds = attachment?.durationSeconds,
        audioTitle = attachment?.audioTitle,
        audioArtist = attachment?.audioArtist,
        audioAlbum = attachment?.audioAlbum,
        mediaGroupId = attachment?.mediaGroupId,
        transferState = attachment?.transferState?.name ?: TransferState.COMPLETE.name,
        transferProgress = attachment?.progress ?: 1f,
        replyToSenderName = replyToSenderName,
        replyToKind = replyToKind,
        reactionsJson = reactions.toStorageString(),
        isForwarded = isForwarded,
        forwardedFromSenderName = forwardedFromSenderName,
        originalSenderId = originalSenderId,
        originalMessageId = originalMessageId,
        isPinned = isPinned,
    )
}

private const val AVATAR_CDN_BASE = "https://s3.coffevista.ir"

internal suspend fun MessageEntity.toDomain(cipher: ChatContentCipher): Message {
    val plaintext = runCatching {
        cipher.decrypt(accountId, conversationId, clientId, contentCiphertext)
    }.getOrNull()
    val content = when (contentKind) {
        "deleted" -> MessageContent.Deleted
        "encrypted_unavailable" -> MessageContent.EncryptedUnavailable
        else -> if (contentKind.startsWith("structured:")) {
            MessageContent.Structured(contentKind.removePrefix("structured:"), plaintext.orEmpty())
        } else {
            plaintext?.let(MessageContent::Text) ?: MessageContent.EncryptedUnavailable
        }
    }
    val attachment = (mediaUrl ?: localUri)?.let {
        Attachment(
            kind = messageType.toAttachmentKind(),
            remoteUrl = mediaUrl,
            localUri = localUri,
            fileName = attachmentFileName,
            mimeType = attachmentMimeType,
            sizeBytes = attachmentSizeBytes,
            durationSeconds = durationSeconds,
            audioTitle = audioTitle,
            audioArtist = audioArtist,
            audioAlbum = audioAlbum,
            mediaGroupId = mediaGroupId,
            progress = transferProgress.coerceIn(0f, 1f),
            transferState = runCatching { TransferState.valueOf(transferState) }.getOrDefault(TransferState.COMPLETE),
        )
    }
    return Message(
        accountId = accountId,
        conversationId = conversationId,
        clientId = clientId,
        serverId = serverId,
        senderId = senderId,
        content = content,
        createdAtEpochMillis = createdAtEpochMillis,
        editedAtEpochMillis = editedAtEpochMillis,
        deletedAtEpochMillis = deletedAtEpochMillis,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.PENDING),
        replyToMessageId = replyToMessageId,
        replyToContent = replyToContentCiphertext?.let {
            runCatching { cipher.decrypt(accountId, conversationId, "$clientId:reply", it) }.getOrNull()
        },
        replyToSenderName = replyToSenderName,
        replyToKind = replyToKind,
        attachment = attachment,
        reactions = reactionsJson.fromStorageString(),
        isForwarded = isForwarded,
        forwardedFromSenderName = forwardedFromSenderName,
        originalSenderId = originalSenderId,
        originalMessageId = originalMessageId,
        isPinned = isPinned,
        isMine = isMine,
    )
}

internal val AttachmentKind.wireName: String
    get() = when (this) {
        AttachmentKind.IMAGE -> "image"
        AttachmentKind.VIDEO -> "video"
        AttachmentKind.GIF -> "gif"
        AttachmentKind.VOICE -> "voice"
        AttachmentKind.AUDIO -> "audio"
        AttachmentKind.DOCUMENT -> "file"
        AttachmentKind.UNKNOWN -> "file"
    }

internal fun String.toAttachmentKind(): AttachmentKind = when (trim().lowercase()) {
    "image" -> AttachmentKind.IMAGE
    "video" -> AttachmentKind.VIDEO
    "gif" -> AttachmentKind.GIF
    "voice" -> AttachmentKind.VOICE
    "audio" -> AttachmentKind.AUDIO
    "file", "document", "pdf" -> AttachmentKind.DOCUMENT
    else -> AttachmentKind.UNKNOWN
}

private fun Map<String, Set<String>>.toStorageString(): String = entries.joinToString("\u001e") { (emoji, users) ->
    "$emoji\u001f${users.joinToString("\u001d")}" 
}

private fun String.fromStorageString(): Map<String, Set<String>> =
    takeIf(String::isNotBlank)?.split('\u001e')?.mapNotNull { entry ->
        val parts = entry.split('\u001f', limit = 2)
        parts.firstOrNull()?.takeIf(String::isNotBlank)?.let { emoji ->
            emoji to parts.getOrElse(1) { "" }.split('\u001d').filter(String::isNotBlank).toSet()
        }
    }?.toMap().orEmpty()
