package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import ir.coffevista.vista_native.features.chat.domain.model.RealtimeConnectionState
import ir.coffevista.vista_native.features.chat.presentation.conversations.ConversationsUiState
import ir.coffevista.vista_native.features.chat.presentation.messages.MessagesUiState

/** Synthetic deterministic data only. It must never contain account or backend identifiers. */
internal object ChatVisualFixtures {
    private const val accountId = "fixture-account"
    private const val conversationId = "fixture-conversation"
    private const val peerId = "fixture-peer"
    private const val fixedTime = 1_722_959_400_000L

    val conversations = listOf(
        conversation(
            id = "conversation-unread",
            title = "نگار نمونه",
            preview = "سلام، برای جلسه فردا آماده‌ای؟",
            unread = 4,
            pinned = true,
            timeOffsetMinutes = 0,
        ),
        conversation(
            id = "conversation-typing",
            title = "آرمان آزمایشی",
            preview = "پیام قبلی مصنوعی",
            typing = true,
            timeOffsetMinutes = 7,
        ),
        conversation(
            id = "conversation-muted",
            title = "گفتگوی بی‌صدای نمونه",
            preview = "این متن فقط fixture است",
            muted = true,
            timeOffsetMinutes = 16,
        ),
        conversation(
            id = "conversation-long",
            title = "عنوان بسیار طولانی برای بررسی برش صحیح متن در یک سطر",
            preview = "پیش‌نمایش بسیار طولانی برای کنترل ellipsis و فاصله‌های ردیف گفتگو در حالت راست‌به‌چپ",
            timeOffsetMinutes = 42,
        ),
        conversation(
            id = "conversation-mixed",
            title = "Vista QA Team",
            preview = "نسخه build-42 آماده review است ✅",
            timeOffsetMinutes = 75,
        ),
    )

    val conversationContent = ConversationsUiState(
        conversations = conversations,
        isInitialLoading = false,
        hasMore = false,
        connectionState = RealtimeConnectionState.CONNECTED,
    )

    val conversationOffline = conversationContent.copy(
        isOffline = true,
        connectionState = RealtimeConnectionState.DISCONNECTED,
    )

    val conversationPagination = conversationContent.copy(isAppending = true, hasMore = true)

    fun conversationState(vararg ids: String) = conversationContent.copy(
        conversations = conversations.filter { it.id in ids },
    )

    val messages = listOf(
        message("message-read", "پیام خوانده‌شدهٔ مصنوعی", true, MessageStatus.READ, 0),
        message("message-delivered", "پیام تحویل‌شده", true, MessageStatus.DELIVERED, 1),
        message("message-sending", "در حال ارسال...", true, MessageStatus.PENDING, 2),
        message("message-failed", "نمونهٔ ناموفق برای تلاش مجدد", true, MessageStatus.FAILED, 3),
        message(
            id = "message-reply",
            text = "این پاسخ فقط برای بررسی چیدمان است",
            mine = false,
            status = MessageStatus.SENT,
            offsetMinutes = 4,
            reply = "متن مرجع مصنوعی",
        ),
        message(
            id = "message-long",
            text = "این یک پیام چندخطی طولانی و کاملاً مصنوعی است تا بیشینهٔ عرض حباب، فاصلهٔ خطوط و رفتار راست‌به‌چپ در کنار English words و https://example.test بررسی شود.",
            mine = false,
            status = MessageStatus.SENT,
            offsetMinutes = 5,
        ),
        message("message-emoji", "سلام Vista 👋🏽✨ تست emoji", true, MessageStatus.SENT, 6, edited = true),
        message("message-short", "سلام", false, MessageStatus.SENT, 7),
    )

    val messageHistory = MessagesUiState(
        conversationId = conversationId,
        messages = messages,
        isInitialLoading = false,
        hasMore = false,
        connectionState = RealtimeConnectionState.CONNECTED,
    )

    val messageReconnect = messageHistory.copy(connectionState = RealtimeConnectionState.RECONNECTING)

    fun messageState(vararg ids: String) = messageHistory.copy(
        messages = messages.filter { it.serverId in ids },
    )

    private fun conversation(
        id: String,
        title: String,
        preview: String,
        unread: Int = 0,
        pinned: Boolean = false,
        muted: Boolean = false,
        typing: Boolean = false,
        timeOffsetMinutes: Int,
    ) = Conversation(
        accountId = accountId,
        id = id,
        type = ConversationType.PRIVATE,
        title = title,
        avatarUrl = null,
        peerId = peerId,
        lastMessage = preview,
        lastMessageAtEpochMillis = fixedTime - timeOffsetMinutes * 60_000L,
        unreadCount = unread,
        isArchived = false,
        isPinned = pinned,
        isMuted = muted,
        requestStatus = null,
        typingUserIds = if (typing) setOf(peerId) else emptySet(),
    )

    private fun message(
        id: String,
        text: String,
        mine: Boolean,
        status: MessageStatus,
        offsetMinutes: Int,
        reply: String? = null,
        edited: Boolean = false,
    ) = Message(
        accountId = accountId,
        conversationId = conversationId,
        clientId = "client-$id",
        serverId = id,
        senderId = if (mine) accountId else peerId,
        content = MessageContent.Text(text),
        createdAtEpochMillis = fixedTime - offsetMinutes * 60_000L,
        editedAtEpochMillis = if (edited) fixedTime else null,
        status = status,
        replyToMessageId = if (reply == null) null else "fixture-reply-target",
        replyToContent = reply,
        isMine = mine,
    )
}
