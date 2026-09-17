package ir.coffevista.vista_native.features.chat.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
data class Conversation(
    val accountId: String,
    val id: String,
    val type: ConversationType,
    val title: String,
    val avatarUrl: String?,
    val peerId: String?,
    val lastMessage: String?,
    val lastMessageAtEpochMillis: Long?,
    val unreadCount: Int,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val requestStatus: String?,
    val typingUserIds: Set<String> = emptySet(),
    val isMessageRequest: Boolean = false,
    val lastMessageType: String? = null,
    val isLastMessageFromMe: Boolean = false,
    val lastMessageStatus: MessageStatus = MessageStatus.SENT,
)

enum class ConversationType { PRIVATE, GROUP, SECRET }

@Immutable
data class BlockStatus(
    val isBlocked: Boolean,
    val isBlockedBy: Boolean,
    val blockedAtEpochMillis: Long? = null,
    val blockedByAtEpochMillis: Long? = null,
) {
    val hasAnyBlock: Boolean get() = isBlocked || isBlockedBy
    val canSendMessage: Boolean get() = !hasAnyBlock
}

enum class ModerationReason(val wireName: String) {
    INAPPROPRIATE_CONTENT("inappropriateContent"),
    HARASSMENT("harassment"),
    SPAM("spam"),
    IMPERSONATION("impersonation"),
    SCAM("scam"),
    HATE_SPEECH("hateSpeech"),
    VIOLENCE("violence"),
    OTHER("other"),
}

@Immutable
data class ChatUser(
    val id: String,
    val username: String,
    val fullName: String?,
    val avatarUrl: String?,
    val conversationId: String? = null,
) {
    val displayName: String get() = fullName?.trim()?.takeIf(String::isNotEmpty)
        ?: username.trim().takeIf(String::isNotEmpty)
        ?: "کاربر"
}

/** Wire-compatible payload rendered as a shared-post card by Flutter and Native chat. */
@Immutable
@Serializable
data class SharedPostDraft(
    val postId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String? = null,
    val content: String = "",
    val mediaUrls: List<String> = emptyList(),
    val postVideoUrl: String? = null,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val createdAt: String,
    val isVerified: Boolean = false,
    val verificationType: String = "none",
    val role: String? = null,
    val hashtags: List<String> = emptyList(),
)

/** Ephemeral peer metadata for the in-chat detail screen. Never persisted with messages. */
@Immutable
data class ChatPartnerProfile(
    val userId: String,
    val username: String? = null,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
)

@Immutable
data class ProfileNote(
    val id: String,
    val userId: String,
    val content: String,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    val isMine: Boolean,
) {
    fun isExpired(nowEpochMillis: Long = System.currentTimeMillis()): Boolean =
        expiresAtEpochMillis <= nowEpochMillis
}

@Immutable
data class GroupInfo(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val memberCount: Int,
    val maxMembers: Int,
    val inviteCode: String?,
    val inviteEnabled: Boolean,
    val isAdmin: Boolean,
    val createdByUserId: String? = null,
    val currentUserId: String? = null,
)

@Immutable
data class GroupMember(
    val userId: String,
    val username: String,
    val fullName: String?,
    val avatarUrl: String?,
    val isAdmin: Boolean,
    val joinedAtEpochMillis: Long?,
) {
    val displayName: String get() = fullName?.trim()?.takeIf(String::isNotEmpty)
        ?: username.trim().takeIf(String::isNotEmpty)
        ?: "کاربر"
}

@Immutable
data class ConversationParticipant(
    val accountId: String,
    val conversationId: String,
    val userId: String,
    val displayName: String?,
    val avatarUrl: String?,
)

@Immutable
data class Message(
    val accountId: String,
    val conversationId: String,
    val clientId: String,
    val serverId: String?,
    val senderId: String,
    val content: MessageContent,
    val createdAtEpochMillis: Long,
    val editedAtEpochMillis: Long? = null,
    val deletedAtEpochMillis: Long? = null,
    val status: MessageStatus,
    val replyToMessageId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null,
    val replyToKind: String? = null,
    val attachment: Attachment? = null,
    val reactions: Map<String, Set<String>> = emptyMap(),
    val isForwarded: Boolean = false,
    val forwardedFromSenderName: String? = null,
    val originalSenderId: String? = null,
    val originalMessageId: String? = null,
    val isPinned: Boolean = false,
    val isMine: Boolean,
) {
    val stableKey: String get() = serverId ?: "local:$clientId"
}

sealed interface MessageContent {
    @Immutable
    data class Text(val value: String) : MessageContent

    @Immutable
    data class Structured(val kind: String, val payload: String) : MessageContent

    /** CHAT-01 never renders an E2EE payload as plaintext. */
    data object EncryptedUnavailable : MessageContent

    data object Deleted : MessageContent
}

enum class MessageStatus {
    PENDING,
    FAILED,
    SENT,
    DELIVERED,
    READ,
}

@Immutable
data class Attachment(
    val kind: AttachmentKind,
    val remoteUrl: String? = null,
    val localUri: String? = null,
    val fileName: String?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val durationSeconds: Int? = null,
    val audioTitle: String? = null,
    val audioArtist: String? = null,
    val audioAlbum: String? = null,
    val mediaGroupId: String? = null,
    val progress: Float = 1f,
    val transferState: TransferState = TransferState.COMPLETE,
)

enum class AttachmentKind { IMAGE, VIDEO, GIF, VOICE, AUDIO, DOCUMENT, UNKNOWN }

fun chatMessageTypePreview(type: String?, secret: Boolean = false): String? {
    val normalized = type?.trim()?.lowercase()?.takeIf { it.isNotEmpty() && it != "text" } ?: return null
    if (secret) return when (normalized) {
        "voice", "audio" -> "🔒 پیام صوتی"
        "image", "photo" -> "🔒 تصویر"
        "video" -> "🔒 ویدیو"
        "file", "document" -> "🔒 فایل"
        else -> "🔒 پیام محرمانه"
    }
    return when (normalized) {
        "voice", "audio" -> "🎤 پیام صوتی"
        "image", "photo" -> "📷 تصویر"
        "video" -> "🎬 ویدیو"
        "file", "document", "unknown" -> "📎 فایل"
        "gif" -> "🎞️ گیف"
        "post", "shared_post" -> "📮 پست اشتراک‌گذاری شده"
        "sticker" -> "😀 استیکر"
        "location" -> "📍 موقعیت مکانی"
        "contact" -> "👤 مخاطب"
        "poll" -> "📊 نظرسنجی"
        else -> null
    }
}

fun Message.inboxPreviewText(): String = attachment?.let { value ->
    chatMessageTypePreview(value.kind.name, secret = false)
} ?: when (val value = content) {
    is MessageContent.Text -> value.value
    is MessageContent.Structured -> value.payload
    MessageContent.Deleted -> "این پیام حذف شده است"
    MessageContent.EncryptedUnavailable -> "🔒 پیام رمزگذاری‌شده"
}

/**
 * Inbox rows are a trust boundary: a cached or server-supplied opaque envelope
 * must never be rendered as though it were a human-readable message.
 */
fun Conversation.inboxPreviewText(): String? = lastMessage
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?.let { preview ->
        if (preview.looksLikeOpaqueConversationPreview()) "پیام جدید" else preview
    }

private fun String.looksLikeOpaqueConversationPreview(): Boolean {
    if (startsWith("VE2E1:") || startsWith("VE2E2:") || startsWith("e2ee:v1:")) return true
    return length >= 32 && all { char ->
        char.isLetterOrDigit() || char == '+' || char == '/' || char == '_' || char == '-' || char == '='
    }
}

enum class TransferState { QUEUED, UPLOADING, COMPLETE, FAILED, CANCELLED }

enum class DownloadState { QUEUED, DOWNLOADING, PAUSED, COMPLETE, FAILED, CANCELLED }

@Immutable
data class DownloadTask(
    val accountId: String,
    val conversationId: String,
    val messageId: String,
    val localUri: String?,
    val mimeType: String?,
    val fileName: String,
    val receivedBytes: Long,
    val totalBytes: Long,
    val state: DownloadState,
    val attempts: Int,
) {
    val progress: Float
        get() = if (totalBytes > 0L) {
            (receivedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

@Immutable
data class ChatAttachmentDraft(
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val kind: AttachmentKind,
    val durationSeconds: Int? = null,
    val caption: String = "",
    val mediaGroupId: String? = null,
)

@Immutable
data class Reaction(
    val messageId: String,
    val userId: String,
    val emoji: String,
)

@Immutable
data class TransferTask(
    val accountId: String,
    val conversationId: String,
    val clientId: String,
    val localUri: String,
    val objectKey: String,
    val progress: Float,
    val state: TransferState,
    val attempts: Int,
)

@Immutable
data class TypingState(
    val conversationId: String,
    val userId: String,
    val isTyping: Boolean,
    val occurredAtEpochMillis: Long,
)

@Immutable
data class PresenceState(
    val userId: String,
    val isOnline: Boolean,
    val lastOnlineAtEpochMillis: Long? = null,
    val canViewLastSeen: Boolean = false,
)

@Immutable
data class ReadReceipt(
    val conversationId: String,
    val userId: String,
    val readAtEpochMillis: Long,
)

@Immutable
data class DeliveryReceipt(
    val conversationId: String,
    val messageId: String,
    val deliveredAtEpochMillis: Long,
)

@Immutable
data class PaginationCursor(val value: String?)

@Immutable
data class Page<T>(
    val items: List<T>,
    val nextCursor: PaginationCursor,
    val hasMore: Boolean,
)

enum class PendingOperationType { SEND_TEXT, SEND_ATTACHMENT, MARK_READ }

@Immutable
data class PendingOperation(
    val accountId: String,
    val conversationId: String,
    val clientId: String,
    val type: PendingOperationType,
    val attempts: Int,
    val createdAtEpochMillis: Long,
)

enum class RealtimeConnectionState {
    DISCONNECTED,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    RECONNECTING,
    PAUSED,
    FAILED,
}

enum class MergeSource { DATABASE, OPTIMISTIC, REST, REALTIME, NOTIFICATION }
