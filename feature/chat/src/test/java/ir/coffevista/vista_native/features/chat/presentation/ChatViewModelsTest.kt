package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.core.testing.MainDispatcherRule
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser
import ir.coffevista.vista_native.features.chat.domain.model.BlockStatus
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import ir.coffevista.vista_native.features.chat.domain.model.GifItem
import ir.coffevista.vista_native.features.chat.domain.model.ModerationReason
import ir.coffevista.vista_native.features.chat.domain.model.Page
import ir.coffevista.vista_native.features.chat.domain.model.ProfileNote
import ir.coffevista.vista_native.features.chat.domain.model.PaginationCursor
import ir.coffevista.vista_native.features.chat.domain.model.RealtimeConnectionState
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import ir.coffevista.vista_native.features.chat.domain.repository.GifCatalog
import ir.coffevista.vista_native.features.chat.presentation.conversations.ConversationsViewModel
import ir.coffevista.vista_native.features.chat.presentation.messages.MessagesViewModel
import ir.coffevista.vista_native.features.chat.presentation.newmessage.NewMessageViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelsTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `conversation cache is visible and realtime failure becomes offline`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository().apply { conversations.value = listOf(conversation()) }
        val viewModel = ConversationsViewModel(repository)
        advanceUntilIdle()

        assertEquals("fixture-conversation", viewModel.state.value.conversations.single().id)
        assertFalse(viewModel.state.value.isInitialLoading)

        repository.mutableRealtime.value = RealtimeConnectionState.FAILED
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isOffline)
        assertEquals(RealtimeConnectionState.FAILED, viewModel.state.value.connectionState)
    }

    @Test
    fun `pagination failure resets loading and exposes retryable state`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository().apply {
            conversations.value = listOf(conversation())
            refreshConversationsResult = ChatResult.Success(page(conversations.value, hasMore = true))
            loadMoreConversationsResult = ChatResult.Failure("fixture-error", retryable = true)
        }
        val viewModel = ConversationsViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isAppending)
        assertEquals("fixture-error", viewModel.state.value.error)
    }

    @Test
    fun `message send accepts nonblank once and retry preserves identity`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository().apply { messages.value = listOf(message()) }
        val viewModel = MessagesViewModel(repository)
        viewModel.bind("fixture-conversation")
        advanceUntilIdle()

        var accepted = 0
        viewModel.send("fixture text") { accepted += 1 }
        advanceUntilIdle()
        viewModel.retry(message())
        advanceUntilIdle()

        assertEquals(1, accepted)
        assertEquals(listOf("fixture-conversation" to "fixture text"), repository.sent)
        assertEquals(listOf("fixture-conversation" to "fixture-client"), repository.retried)
    }

    @Test
    fun `transfer retry and cancel failures are visible to the user`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository().apply {
            messages.value = listOf(message())
            retryResult = ChatResult.Failure("retry failed", retryable = false)
            cancelResult = ChatResult.Failure("cancel failed", retryable = true)
        }
        val viewModel = MessagesViewModel(repository)
        viewModel.bind("fixture-conversation")
        advanceUntilIdle()

        viewModel.retry(message())
        advanceUntilIdle()
        assertEquals("retry failed", viewModel.state.value.error)

        viewModel.cancelTransfer(message())
        advanceUntilIdle()
        assertEquals("cancel failed", viewModel.state.value.error)
    }

    @Test
    fun `reply send preserves the selected message identity`() = runTest(mainDispatcherRule.dispatcher) {
        val replyTarget = message()
        val repository = FakeChatRepository()
        val viewModel = MessagesViewModel(repository)
        viewModel.bind("fixture-conversation")
        advanceUntilIdle()

        var accepted = 0
        viewModel.sendReply("reply body", replyTarget) { accepted += 1 }
        advanceUntilIdle()

        assertEquals(1, accepted)
        assertEquals(
            listOf(Triple("fixture-conversation", "reply body", "fixture-client")),
            repository.sentReplies,
        )
    }

    @Test
    fun `message search is debounced and ignores a replaced query`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository().apply { messages.value = listOf(message()) }
        val viewModel = MessagesViewModel(repository)
        viewModel.bind("fixture-conversation")
        advanceUntilIdle()

        viewModel.search("old")
        advanceTimeBy(150)
        viewModel.search("new")
        advanceTimeBy(299)
        assertTrue(repository.messageSearches.isEmpty())
        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(listOf("fixture-conversation" to "new"), repository.messageSearches)
        assertEquals("new", viewModel.state.value.searchQuery)
        assertEquals(1, viewModel.state.value.searchResults.size)
    }

    @Test
    fun `gif catalog mirrors trending search debounce and remote gif send`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository()
        val catalog = FakeGifCatalog()
        val viewModel = MessagesViewModel(repository, catalog)
        viewModel.bind("fixture-conversation")
        advanceUntilIdle()

        viewModel.loadTrendingGifs()
        advanceUntilIdle()
        assertEquals(listOf("trending"), viewModel.state.value.gifs.map(GifItem::id))

        viewModel.searchGifs("old")
        advanceTimeBy(250)
        viewModel.searchGifs("new")
        advanceTimeBy(499)
        assertTrue(catalog.searches.isEmpty())
        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(listOf("new"), catalog.searches)
        assertEquals(listOf("new"), viewModel.state.value.gifs.map(GifItem::id))

        viewModel.sendGif("https://media.tenor.com/fixture.gif")
        advanceUntilIdle()
        assertEquals(listOf("fixture-conversation" to "https://media.tenor.com/fixture.gif"), repository.sentGifs)
    }

    @Test
    fun `profile note reply preserves note identity and conversation target`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository()
        val viewModel = ConversationsViewModel(repository)
        val note = ProfileNote("note-id", "fixture-peer", "یادداشت نمونه", 1, Long.MAX_VALUE, false)
        advanceUntilIdle()

        var completed = false
        viewModel.replyToNote(note, conversation(), "پاسخ") { completed = it }
        advanceUntilIdle()

        assertTrue(completed)
        assertEquals(
            listOf(listOf("fixture-conversation", "fixture-peer", "نمونه", "پاسخ")),
            repository.noteReplies,
        )
    }

    @Test
    fun `binding a conversation preserves the current realtime state`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository().apply {
            mutableRealtime.value = RealtimeConnectionState.CONNECTED
        }
        val viewModel = MessagesViewModel(repository)

        viewModel.bind("fixture-conversation")

        assertEquals(RealtimeConnectionState.CONNECTED, viewModel.state.value.connectionState)
    }

    @Test
    fun `message timeline catches up through REST after realtime reconnects`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository()
        val viewModel = MessagesViewModel(repository)
        viewModel.bind("fixture-conversation")
        advanceUntilIdle()
        assertEquals(1, repository.refreshMessagesCalls)

        repository.mutableRealtime.value = RealtimeConnectionState.PAUSED
        advanceUntilIdle()
        repository.mutableRealtime.value = RealtimeConnectionState.CONNECTED
        advanceUntilIdle()

        assertEquals(2, repository.refreshMessagesCalls)
    }

    @Test
    fun `inbox catches up through REST after realtime reconnects`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository()
        ConversationsViewModel(repository)
        advanceUntilIdle()
        assertEquals(1, repository.refreshConversationsCalls)

        repository.mutableRealtime.value = RealtimeConnectionState.PAUSED
        advanceUntilIdle()
        repository.mutableRealtime.value = RealtimeConnectionState.CONNECTED
        advanceUntilIdle()

        assertEquals(2, repository.refreshConversationsCalls)
    }

    @Test
    fun `new message reuses a cached direct conversation without creating a duplicate`() = runTest(mainDispatcherRule.dispatcher) {
        val user = ChatUser("peer", "peer", "Peer", null, "existing-conversation")
        val repository = FakeChatRepository().apply { suggestedUsers.value = listOf(user) }
        val viewModel = NewMessageViewModel(repository)
        advanceUntilIdle()
        var opened: Pair<String, String>? = null

        viewModel.openUser(user) { id, title -> opened = id to title }

        assertEquals("existing-conversation" to "Peer", opened)
        assertTrue(repository.createdConversations.isEmpty())
    }

    @Test
    fun `new message search is debounced and preserves an existing conversation id`() = runTest(mainDispatcherRule.dispatcher) {
        val cached = ChatUser("peer", "cached", "Cached", null, "existing-conversation")
        val remote = ChatUser("peer", "remote", "Remote", null)
        val repository = FakeChatRepository().apply {
            suggestedUsers.value = listOf(cached)
            searchedUsers = listOf(remote)
        }
        val viewModel = NewMessageViewModel(repository)
        advanceUntilIdle()

        viewModel.queryChanged("rem")
        advanceTimeBy(499)
        assertEquals(0, repository.searchUsersCalls)
        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(1, repository.searchUsersCalls)
        assertEquals("existing-conversation", viewModel.state.value.visibleUsers.single().conversationId)
    }

    @Test
    fun `new group requires a name and selected member then opens server conversation`() = runTest(mainDispatcherRule.dispatcher) {
        val user = ChatUser("peer", "peer", "Peer", null)
        val repository = FakeChatRepository().apply { suggestedUsers.value = listOf(user) }
        val viewModel = NewMessageViewModel(repository)
        advanceUntilIdle()
        viewModel.setGroupMode(true)
        viewModel.groupNameChanged("گروه نمونه")
        viewModel.toggleGroupUser(user)
        var opened: Pair<String, String>? = null

        viewModel.createGroup { id, title -> opened = id to title }
        advanceUntilIdle()

        assertEquals(listOf("گروه نمونه" to listOf("peer")), repository.createdGroups)
        assertEquals("fixture-group" to "گروه نمونه", opened)
    }

    @Test
    fun `message moderation loads status and reports with flutter wire reason`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeChatRepository().apply {
            conversations.value = listOf(conversation())
            blockStatus = BlockStatus(isBlocked = false, isBlockedBy = false)
        }
        val viewModel = MessagesViewModel(repository)

        viewModel.bind("fixture-conversation")
        advanceUntilIdle()
        assertEquals(false, viewModel.state.value.blockStatus?.isBlocked)
        assertEquals(listOf("fixture-peer" to false), repository.blockStatusCalls)

        var reported = false
        viewModel.reportUser("fixture-peer", ModerationReason.SPAM, "fixture detail") { reported = it }
        advanceUntilIdle()

        assertTrue(reported)
        assertEquals("spam", ModerationReason.SPAM.wireName)
        assertEquals(listOf(Triple("fixture-peer", ModerationReason.SPAM, "fixture detail")), repository.reports)
    }

    @Test
    fun `pin failure is exposed and successful pin refreshes pinned messages`() = runTest(mainDispatcherRule.dispatcher) {
        val serverMessage = message().copy(serverId = "fixture-server", status = MessageStatus.SENT)
        val repository = FakeChatRepository().apply {
            messages.value = listOf(serverMessage)
            pinResult = ChatResult.Failure("pin failed", retryable = true)
        }
        val viewModel = MessagesViewModel(repository)
        viewModel.bind("fixture-conversation")
        advanceUntilIdle()
        repository.getPinnedMessagesCalls = 0

        viewModel.togglePinned(serverMessage)
        advanceUntilIdle()
        assertEquals("pin failed", viewModel.state.value.error)
        assertEquals(0, repository.getPinnedMessagesCalls)

        repository.pinResult = ChatResult.Success(Unit)
        viewModel.togglePinned(serverMessage)
        advanceUntilIdle()
        assertEquals(1, repository.getPinnedMessagesCalls)
    }

    private class FakeChatRepository : ChatRepository {
        val mutableRealtime = MutableStateFlow(RealtimeConnectionState.CONNECTED)
        override val realtimeState: StateFlow<RealtimeConnectionState> = mutableRealtime
        val conversations = MutableStateFlow<List<Conversation>>(emptyList())
        val messages = MutableStateFlow<List<Message>>(emptyList())
        val suggestedUsers = MutableStateFlow<List<ChatUser>>(emptyList())
        val profileNotes = MutableStateFlow<List<ProfileNote>>(emptyList())
        val sent = mutableListOf<Pair<String, String>>()
        val sentReplies = mutableListOf<Triple<String, String, String>>()
        val sentGifs = mutableListOf<Pair<String, String>>()
        val noteReplies = mutableListOf<List<String>>()
        val messageSearches = mutableListOf<Pair<String, String>>()
        val retried = mutableListOf<Pair<String, String>>()
        val createdConversations = mutableListOf<Pair<String, Boolean>>()
        val createdGroups = mutableListOf<Pair<String, List<String>>>()
        val blockStatusCalls = mutableListOf<Pair<String, Boolean>>()
        val reports = mutableListOf<Triple<String, ModerationReason, String?>>()
        var blockStatus = BlockStatus(isBlocked = false, isBlockedBy = false)
        var searchedUsers = emptyList<ChatUser>()
        var searchUsersCalls = 0
        var refreshConversationsCalls = 0
        var refreshMessagesCalls = 0
        var getPinnedMessagesCalls = 0
        var pinResult: ChatResult<Unit> = ChatResult.Success(Unit)
        var retryResult: ChatResult<Message> = ChatResult.Success(message())
        var cancelResult: ChatResult<Unit> = ChatResult.Success(Unit)
        var refreshConversationsResult: ChatResult<Page<Conversation>> =
            ChatResult.Success(page(emptyList()))
        var loadMoreConversationsResult: ChatResult<Page<Conversation>> = ChatResult.Success(page(emptyList()))

        override fun observeConversations(includeArchived: Boolean): Flow<List<Conversation>> = conversations
        override suspend fun refreshConversations(reset: Boolean): ChatResult<Page<Conversation>> {
            refreshConversationsCalls += 1
            return refreshConversationsResult
        }
        override suspend fun loadMoreConversations() = loadMoreConversationsResult
        override fun observeSuggestedUsers(): Flow<List<ChatUser>> = suggestedUsers
        override suspend fun searchUsers(query: String): ChatResult<List<ChatUser>> {
            searchUsersCalls += 1
            return ChatResult.Success(searchedUsers)
        }
        override suspend fun createConversation(peerId: String, isSecret: Boolean): ChatResult<Conversation> {
            createdConversations += peerId to isSecret
            return ChatResult.Success(conversation())
        }
        override suspend fun createGroup(name: String, memberIds: List<String>, imageUrl: String?): ChatResult<Conversation> {
            createdGroups += name to memberIds
            return ChatResult.Success(
                conversation().copy(id = "fixture-group", type = ConversationType.GROUP, title = name),
            )
        }
        override fun observeProfileNotes(): Flow<List<ProfileNote>> = profileNotes
        override suspend fun refreshProfileNotes() = ChatResult.Success(Unit)
        override suspend fun upsertOwnProfileNote(content: String) = ChatResult.Success(
            ProfileNote("note", "fixture-account", content, 1, Long.MAX_VALUE, true),
        )
        override suspend fun deleteOwnProfileNote() = ChatResult.Success(Unit)
        override fun observeMessages(conversationId: String): Flow<List<Message>> = messages
        override fun observeConversation(conversationId: String): Flow<Conversation?> = conversations
            .let { source ->
                MutableStateFlow(source.value.firstOrNull { it.id == conversationId })
            }
        override suspend fun refreshMessages(conversationId: String): ChatResult<Page<Message>> {
            refreshMessagesCalls += 1
            return ChatResult.Success(page(messages.value))
        }
        override suspend fun loadOlderMessages(conversationId: String) = ChatResult.Success(page(emptyList<Message>()))
        override suspend fun searchMessages(conversationId: String, query: String): ChatResult<List<Message>> {
            messageSearches += conversationId to query
            return ChatResult.Success(messages.value)
        }
        override suspend fun sendText(conversationId: String, text: String): ChatResult<Message> {
            sent += conversationId to text
            return ChatResult.Success(message())
        }
        override suspend fun sendReply(conversationId: String, text: String, replyTo: Message): ChatResult<Message> {
            sentReplies += Triple(conversationId, text, replyTo.serverId ?: replyTo.clientId)
            return ChatResult.Success(message())
        }
        override suspend fun replyToProfileNote(
            conversationId: String,
            note: ProfileNote,
            senderName: String,
            text: String,
        ): ChatResult<Message> {
            noteReplies += listOf(conversationId, note.userId, senderName, text)
            return ChatResult.Success(message())
        }
        override suspend fun sendAttachment(conversationId: String, draft: ChatAttachmentDraft) =
            ChatResult.Success(message())
        override suspend fun sendRemoteGif(conversationId: String, url: String): ChatResult<Message> {
            sentGifs += conversationId to url
            return ChatResult.Success(message())
        }
        override suspend fun retryMessage(conversationId: String, clientId: String): ChatResult<Message> {
            retried += conversationId to clientId
            return retryResult
        }
        override suspend fun cancelTransfer(conversationId: String, clientId: String) = cancelResult
        override suspend fun markRead(conversationId: String) = ChatResult.Success(Unit)
        override suspend fun sendTyping(conversationId: String) = ChatResult.Success(Unit)
        override suspend fun setConversationActive(conversationId: String, active: Boolean) = ChatResult.Success(Unit)
        override suspend fun toggleConversationFlag(conversationId: String, flag: String) =
            ChatResult.Success(conversation())
        override suspend fun acceptMessageRequest(conversationId: String) = ChatResult.Success(Unit)
        override suspend fun rejectMessageRequest(conversationId: String) = ChatResult.Success(Unit)
        override suspend fun editMessage(message: Message, content: String) = ChatResult.Success(Unit)
        override suspend fun deleteMessage(message: Message, forEveryone: Boolean) = ChatResult.Success(Unit)
        override suspend fun forwardMessage(messageId: String, targetConversationId: String): ChatResult<Message> {
            return ChatResult.Failure("Not implemented", false)
        }
        
        override suspend fun toggleReaction(message: Message, emoji: String): ChatResult<Unit> {
            return ChatResult.Success(Unit)
        }

        override suspend fun getPinnedMessages(conversationId: String): ChatResult<List<Message>> {
            getPinnedMessagesCalls += 1
            return ChatResult.Success(emptyList())
        }

        override fun observeSharedMedia(conversationId: String): Flow<List<Message>> = messages

        override suspend fun pinMessage(conversationId: String, messageId: String): ChatResult<Unit> {
            return pinResult
        }

        override suspend fun unpinMessage(conversationId: String, messageId: String): ChatResult<Unit> {
            return ChatResult.Success(Unit)
        }

        override suspend fun blockUser(userId: String): ChatResult<Unit> = ChatResult.Success(Unit)

        override suspend fun unblockUser(userId: String): ChatResult<Unit> = ChatResult.Success(Unit)

        override suspend fun getBlockStatus(userId: String, forceRefresh: Boolean): ChatResult<BlockStatus> =
            ChatResult.Success(blockStatus).also { blockStatusCalls += userId to forceRefresh }

        override suspend fun reportUser(
            userId: String,
            reason: ModerationReason,
            additionalDetails: String?,
        ): ChatResult<Unit> = ChatResult.Success(Unit).also {
            reports += Triple(userId, reason, additionalDetails)
        }

        override suspend fun setForeground(foreground: Boolean) = Unit
        override suspend fun reconcileNotification(payload: Map<String, String>) = ChatResult.Success(Unit)
        override suspend fun clearAccount(accountId: String) = Unit
    }

    private class FakeGifCatalog : GifCatalog {
        val searches = mutableListOf<String>()
        override suspend fun trending(limit: Int) = listOf(gif("trending"))
        override suspend fun search(query: String, limit: Int): List<GifItem> {
            searches += query
            return listOf(gif(query))
        }

        private fun gif(id: String) = GifItem(
            id = id,
            url = "https://media.tenor.com/$id.gif",
            previewUrl = "https://media.tenor.com/$id-preview.gif",
            width = 200,
            height = 100,
        )
    }

    companion object {
        private fun conversation() = Conversation(
            accountId = "fixture-account",
            id = "fixture-conversation",
            type = ConversationType.PRIVATE,
            title = "نمونه",
            avatarUrl = null,
            peerId = "fixture-peer",
            lastMessage = "fixture",
            lastMessageAtEpochMillis = 1,
            unreadCount = 0,
            isArchived = false,
            isPinned = false,
            isMuted = false,
            requestStatus = null,
        )

        private fun message() = Message(
            accountId = "fixture-account",
            conversationId = "fixture-conversation",
            clientId = "fixture-client",
            serverId = null,
            senderId = "fixture-account",
            content = MessageContent.Text("fixture"),
            createdAtEpochMillis = 1,
            status = MessageStatus.PENDING,
            isMine = true,
        )

        private fun <T> page(items: List<T>, hasMore: Boolean = false) =
            Page(items, PaginationCursor(null), hasMore = hasMore)
    }
}
