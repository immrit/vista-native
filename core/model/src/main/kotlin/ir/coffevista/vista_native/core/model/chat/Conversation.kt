package ir.coffevista.vista_native.core.model.chat

import kotlinx.datetime.Instant

data class Conversation(
    val id: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessage: String?,
    val lastMessageTime: Instant?,
    val unreadCount: Int,
    val hasUnreadMessages: Boolean,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val isArchived: Boolean,
    val type: String,
    val participants: List<ConversationParticipant>,
    val otherUserName: String?,
    val otherUserAvatar: String?,
    val otherUserId: String?,
    val isGroup: Boolean,
    val isSecret: Boolean
)

data class ConversationParticipant(
    val userId: String,
    val conversationId: String,
    val joinedAt: Instant?,
    val role: String,
    val unreadCount: Int,
    val user: ParticipantUser?
)

data class ParticipantUser(
    val id: String,
    val username: String?,
    val fullName: String?,
    val avatarUrl: String?,
    val isVerified: Boolean
)
