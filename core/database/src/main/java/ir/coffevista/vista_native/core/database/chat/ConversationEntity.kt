package ir.coffevista.vista_native.core.database.chat

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ir.coffevista.vista_native.core.model.chat.Conversation
import kotlinx.datetime.Instant

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    @ColumnInfo(name = "last_message")
    val lastMessage: String?,
    @ColumnInfo(name = "last_message_time")
    val lastMessageTime: Instant?,
    @ColumnInfo(name = "unread_count")
    val unreadCount: Int,
    @ColumnInfo(name = "has_unread_messages")
    val hasUnreadMessages: Boolean,
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean,
    @ColumnInfo(name = "is_muted")
    val isMuted: Boolean,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
    val type: String,
    
    // Flattened other user details for simple list display
    @ColumnInfo(name = "other_user_name")
    val otherUserName: String?,
    @ColumnInfo(name = "other_user_avatar")
    val otherUserAvatar: String?,
    @ColumnInfo(name = "other_user_id")
    val otherUserId: String?
)

fun ConversationEntity.asExternalModel() = Conversation(
    id = id,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastMessage = lastMessage,
    lastMessageTime = lastMessageTime,
    unreadCount = unreadCount,
    hasUnreadMessages = hasUnreadMessages,
    isPinned = isPinned,
    isMuted = isMuted,
    isArchived = isArchived,
    type = type,
    participants = emptyList(), // Participants not stored in this flat entity for now
    otherUserName = otherUserName,
    otherUserAvatar = otherUserAvatar,
    otherUserId = otherUserId,
    isGroup = type == "group",
    isSecret = type == "secret"
)
