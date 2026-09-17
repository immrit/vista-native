package ir.coffevista.vista_native.features.chat.domain.repository

import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import ir.coffevista.vista_native.features.chat.domain.model.BlockStatus
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser
import ir.coffevista.vista_native.features.chat.domain.model.ChatPartnerProfile
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.ModerationReason
import ir.coffevista.vista_native.features.chat.domain.model.Page
import ir.coffevista.vista_native.features.chat.domain.model.PaginationCursor
import ir.coffevista.vista_native.features.chat.domain.model.RealtimeConnectionState
import ir.coffevista.vista_native.features.chat.domain.model.ProfileNote
import ir.coffevista.vista_native.features.chat.domain.model.SharedPostDraft
import ir.coffevista.vista_native.features.chat.domain.model.PresenceState
import ir.coffevista.vista_native.features.chat.domain.model.GroupInfo
import ir.coffevista.vista_native.features.chat.domain.model.GroupMember
import ir.coffevista.vista_native.features.chat.domain.model.DownloadTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class ChatAccount(
    val accountId: String,
    val accessToken: String,
    val deviceId: String,
)

interface ChatSessionProvider {
    val account: StateFlow<ChatAccount?>

    /** Refreshes the in-memory projection after login or token rotation. */
    fun synchronize() = Unit

    /** Drops the account projection immediately on logout. */
    fun clear() = Unit

    /** Refreshes an expired server session and updates [account] on success. */
    suspend fun refresh(): Boolean = false
}

fun interface ChatSessionRefresher {
    suspend fun refresh(): Boolean
}

interface ChatContentCipher {
    suspend fun encrypt(
        accountId: String,
        conversationId: String,
        recordId: String,
        plaintext: String,
    ): ByteArray

    suspend fun decrypt(
        accountId: String,
        conversationId: String,
        recordId: String,
        ciphertext: ByteArray,
    ): String
}

sealed interface ChatResult<out T> {
    data class Success<T>(val value: T) : ChatResult<T>
    data class Failure(val message: String, val retryable: Boolean) : ChatResult<Nothing>
}

interface ChatRepository {
    val realtimeState: StateFlow<RealtimeConnectionState>

    fun observeConversations(includeArchived: Boolean = false): Flow<List<Conversation>>
    suspend fun refreshConversations(reset: Boolean = true): ChatResult<Page<Conversation>>
    suspend fun loadMoreConversations(): ChatResult<Page<Conversation>>
    fun observeSuggestedUsers(): Flow<List<ChatUser>>
    suspend fun refreshSuggestedUsers(): ChatResult<List<ChatUser>> = ChatResult.Success(emptyList())
    suspend fun searchUsers(query: String): ChatResult<List<ChatUser>>
    suspend fun createConversation(peerId: String, isSecret: Boolean = false): ChatResult<Conversation>
    fun observeProfileNotes(): Flow<List<ProfileNote>>
    suspend fun refreshProfileNotes(): ChatResult<Unit>
    suspend fun upsertOwnProfileNote(content: String): ChatResult<ProfileNote>
    suspend fun deleteOwnProfileNote(): ChatResult<Unit>
    suspend fun replyToProfileNote(
        conversationId: String,
        note: ProfileNote,
        senderName: String,
        text: String,
    ): ChatResult<Message> = sendText(conversationId, text)

    suspend fun createGroup(name: String, memberIds: List<String>, imageUrl: String? = null): ChatResult<Conversation> =
        ChatResult.Failure("ساخت گروه در دسترس نیست", false)
    suspend fun groupInfo(conversationId: String): ChatResult<GroupInfo> = ChatResult.Failure("اطلاعات گروه در دسترس نیست", false)
    suspend fun groupMembers(conversationId: String): ChatResult<List<GroupMember>> = ChatResult.Failure("اعضای گروه در دسترس نیست", false)
    suspend fun updateGroup(conversationId: String, name: String? = null, imageUrl: String? = null): ChatResult<GroupInfo> =
        ChatResult.Failure("ویرایش گروه در دسترس نیست", false)
    suspend fun addGroupMembers(conversationId: String, memberIds: List<String>): ChatResult<Int> = ChatResult.Failure("افزودن عضو در دسترس نیست", false)
    suspend fun removeGroupMember(conversationId: String, memberId: String): ChatResult<Unit> = ChatResult.Failure("حذف عضو در دسترس نیست", false)
    suspend fun setGroupAdmin(conversationId: String, memberId: String, makeAdmin: Boolean): ChatResult<Unit> = ChatResult.Failure("تغییر مدیر در دسترس نیست", false)
    suspend fun groupInvite(conversationId: String): ChatResult<String?> = ChatResult.Failure("دعوت گروه در دسترس نیست", false)
    suspend fun setGroupInviteEnabled(conversationId: String, enabled: Boolean): ChatResult<String?> = ChatResult.Failure("دعوت گروه در دسترس نیست", false)
    suspend fun regenerateGroupInvite(conversationId: String): ChatResult<String?> = ChatResult.Failure("دعوت گروه در دسترس نیست", false)
    suspend fun joinGroup(inviteCode: String): ChatResult<Conversation> = ChatResult.Failure("عضویت گروه در دسترس نیست", false)
    suspend fun leaveGroup(conversationId: String): ChatResult<Unit> = ChatResult.Failure("ترک گروه در دسترس نیست", false)
    suspend fun deleteGroup(conversationId: String): ChatResult<Unit> = ChatResult.Failure("حذف گروه در دسترس نیست", false)

    fun observeMessages(conversationId: String): Flow<List<Message>>
    fun observeConversation(conversationId: String): Flow<Conversation?>
    suspend fun refreshMessages(conversationId: String): ChatResult<Page<Message>>
    suspend fun loadOlderMessages(conversationId: String): ChatResult<Page<Message>>
    suspend fun searchMessages(conversationId: String, query: String): ChatResult<List<Message>>
    suspend fun presence(userId: String): ChatResult<PresenceState> =
        ChatResult.Failure("وضعیت حضور در دسترس نیست", false)
    suspend fun partnerProfile(userId: String): ChatResult<ChatPartnerProfile> =
        ChatResult.Failure("اطلاعات کاربر در دسترس نیست", false)

    suspend fun sendText(conversationId: String, text: String): ChatResult<Message>
    suspend fun sendSharedPost(conversationId: String, post: SharedPostDraft): ChatResult<Message> =
        ChatResult.Failure("ارسال پست در دسترس نیست", false)
    suspend fun sendReply(conversationId: String, text: String, replyTo: Message): ChatResult<Message> =
        sendText(conversationId, text)
    suspend fun sendAttachment(conversationId: String, draft: ChatAttachmentDraft): ChatResult<Message>
    suspend fun sendRemoteGif(conversationId: String, url: String): ChatResult<Message>
    suspend fun retryMessage(conversationId: String, clientId: String): ChatResult<Message>
    suspend fun cancelTransfer(conversationId: String, clientId: String): ChatResult<Unit>
    fun observeDownloads(conversationId: String): Flow<List<DownloadTask>> = kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun startDownload(message: Message): ChatResult<DownloadTask> =
        ChatResult.Failure("دانلود فایل در دسترس نیست", false)
    suspend fun pauseDownload(messageId: String): ChatResult<Unit> =
        ChatResult.Failure("توقف دانلود در دسترس نیست", false)
    suspend fun resumeDownload(messageId: String): ChatResult<Unit> =
        ChatResult.Failure("ادامه دانلود در دسترس نیست", false)
    suspend fun cancelDownload(messageId: String): ChatResult<Unit> =
        ChatResult.Failure("لغو دانلود در دسترس نیست", false)
    suspend fun markRead(conversationId: String): ChatResult<Unit>
    suspend fun sendTyping(conversationId: String): ChatResult<Unit>
    suspend fun setConversationActive(conversationId: String, active: Boolean): ChatResult<Unit>
    suspend fun toggleConversationFlag(conversationId: String, flag: String): ChatResult<Conversation>
    suspend fun deleteConversation(conversationId: String): ChatResult<Unit> =
        ChatResult.Failure("حذف گفتگو در دسترس نیست", false)
    suspend fun acceptMessageRequest(conversationId: String): ChatResult<Unit>
    suspend fun rejectMessageRequest(conversationId: String): ChatResult<Unit>
    suspend fun editMessage(message: Message, content: String): ChatResult<Unit>
    suspend fun deleteMessage(message: Message, forEveryone: Boolean): ChatResult<Unit>
    suspend fun forwardMessage(messageId: String, targetConversationId: String): ChatResult<Message>
    suspend fun toggleReaction(message: Message, emoji: String): ChatResult<Unit>

    suspend fun getPinnedMessages(conversationId: String): ChatResult<List<Message>>
    fun observeSharedMedia(conversationId: String): Flow<List<Message>>
    suspend fun pinMessage(conversationId: String, messageId: String): ChatResult<Unit>
    suspend fun unpinMessage(conversationId: String, messageId: String): ChatResult<Unit>
    suspend fun blockUser(userId: String): ChatResult<Unit>
    suspend fun unblockUser(userId: String): ChatResult<Unit>
    suspend fun getBlockStatus(userId: String, forceRefresh: Boolean = false): ChatResult<BlockStatus>
    suspend fun reportUser(userId: String, reason: ModerationReason, additionalDetails: String?): ChatResult<Unit>

    suspend fun setForeground(foreground: Boolean)
    suspend fun reconcileNotification(payload: Map<String, String>): ChatResult<Unit>
    suspend fun clearAccount(accountId: String)
}
