package ir.coffevista.vista_native.features.chat.data.repository

import android.net.Uri
import ir.coffevista.vista_native.features.chat.data.local.ChatDao
import ir.coffevista.vista_native.features.chat.data.local.ConversationEntity
import ir.coffevista.vista_native.features.chat.data.local.MessageEntity
import ir.coffevista.vista_native.features.chat.data.local.PendingOperationEntity
import ir.coffevista.vista_native.features.chat.data.local.ProfileNoteEntity
import ir.coffevista.vista_native.features.chat.data.local.RemoteKeyEntity
import ir.coffevista.vista_native.features.chat.data.local.TombstoneEntity
import ir.coffevista.vista_native.features.chat.data.local.TransferTaskEntity
import ir.coffevista.vista_native.features.chat.data.mapper.toDomain
import ir.coffevista.vista_native.features.chat.data.mapper.toEntity
import ir.coffevista.vista_native.features.chat.data.mapper.epochMillisOrNull
import ir.coffevista.vista_native.features.chat.data.mapper.resolvedAvatarUrl
import ir.coffevista.vista_native.features.chat.data.mapper.wireName
import ir.coffevista.vista_native.features.chat.data.realtime.ChatRealtimeCoordinator
import ir.coffevista.vista_native.features.chat.data.realtime.RealtimeEvent
import ir.coffevista.vista_native.features.chat.data.remote.ActiveConversationRequest
import ir.coffevista.vista_native.features.chat.data.remote.BlockStatusDto
import ir.coffevista.vista_native.features.chat.data.remote.ChatApi
import ir.coffevista.vista_native.features.chat.data.remote.ChatMediaUploader
import ir.coffevista.vista_native.features.chat.data.remote.ChatMediaDownloader
import ir.coffevista.vista_native.features.chat.data.remote.CreateConversationRequest
import ir.coffevista.vista_native.features.chat.data.remote.CreateGroupRequest
import ir.coffevista.vista_native.features.chat.data.remote.EditMessageRequest
import ir.coffevista.vista_native.features.chat.data.remote.ForwardMessageRequest
import ir.coffevista.vista_native.features.chat.data.remote.GroupInviteRequest
import ir.coffevista.vista_native.features.chat.data.remote.GroupInviteDto
import ir.coffevista.vista_native.features.chat.data.remote.GroupInfoDto
import ir.coffevista.vista_native.features.chat.data.remote.GroupMembersRequest
import ir.coffevista.vista_native.features.chat.data.remote.hasMoreForPagination
import ir.coffevista.vista_native.features.chat.data.remote.isAdminFor
import ir.coffevista.vista_native.features.chat.data.remote.ReactionRequest
import ir.coffevista.vista_native.features.chat.data.remote.ReportUserRequest
import ir.coffevista.vista_native.features.chat.data.remote.ProfileBatchRequest
import ir.coffevista.vista_native.features.chat.data.remote.ProfileDto
import ir.coffevista.vista_native.features.chat.data.remote.ProfileNoteDto
import ir.coffevista.vista_native.features.chat.data.remote.ReactionUpdateDto
import ir.coffevista.vista_native.features.chat.data.remote.SendMessageRequest
import ir.coffevista.vista_native.features.chat.data.remote.SetGroupAdminRequest
import ir.coffevista.vista_native.features.chat.data.remote.TargetUserRequest
import ir.coffevista.vista_native.features.chat.data.remote.UnpinMessageRequest
import ir.coffevista.vista_native.features.chat.data.remote.UpdateGroupRequest
import ir.coffevista.vista_native.features.chat.data.remote.UpsertProfileNoteRequest
import ir.coffevista.vista_native.features.chat.domain.model.Attachment
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.BlockStatus
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser
import ir.coffevista.vista_native.features.chat.domain.model.ChatPartnerProfile
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.DownloadState
import ir.coffevista.vista_native.features.chat.domain.model.DownloadTask
import ir.coffevista.vista_native.features.chat.domain.model.MergeSource
import ir.coffevista.vista_native.features.chat.domain.model.GroupInfo
import ir.coffevista.vista_native.features.chat.domain.model.GroupMember
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import ir.coffevista.vista_native.features.chat.domain.model.ModerationReason
import ir.coffevista.vista_native.features.chat.domain.model.Page
import ir.coffevista.vista_native.features.chat.domain.model.ProfileNote
import ir.coffevista.vista_native.features.chat.domain.model.PresenceState
import ir.coffevista.vista_native.features.chat.domain.model.PaginationCursor
import ir.coffevista.vista_native.features.chat.domain.model.RealtimeConnectionState
import ir.coffevista.vista_native.features.chat.domain.model.TransferState
import ir.coffevista.vista_native.features.chat.domain.model.inboxPreviewText
import ir.coffevista.vista_native.features.chat.domain.repository.ChatContentCipher
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import ir.coffevista.vista_native.features.chat.domain.repository.ChatSessionProvider
import java.util.UUID
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class OfflineFirstChatRepository(
    private val api: ChatApi,
    private val dao: ChatDao,
    private val cipher: ChatContentCipher,
    private val sessionProvider: ChatSessionProvider,
    private val realtime: ChatRealtimeCoordinator,
    private val mediaUploader: ChatMediaUploader,
    private val mediaDownloader: ChatMediaDownloader,
    private val e2eeService: ir.coffevista.vista_native.features.chat.data.security.ChatE2EEService,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ChatRepository {
    private val typing = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val typingExpiry = mutableMapOf<String, Job>()
    private val pendingRetryMutex = Mutex()
    // Refresh and prepend pagination share the same cursor. Serializing them
    // per account/conversation prevents a refresh from overwriting the cursor
    // chosen by an in-flight older-page request.
    private val messageFetchMutexes = ConcurrentHashMap<String, Mutex>()
    private val cancelledTransfers = ConcurrentHashMap.newKeySet<String>()
    private val blockStatusCache = ConcurrentHashMap<String, CachedBlockStatus>()
    private val sessionGeneration = SessionGenerationGuard()
    private val pendingJson = Json { ignoreUnknownKeys = true }

    override val realtimeState: StateFlow<RealtimeConnectionState> = realtime.state

    init {
        sessionProvider.synchronize()
        sessionProvider.account.value?.accountId?.let(mediaDownloader::recover)
        realtime.start()
        scope.launch { realtime.events.collect(::applyRealtimeEvent) }
    }

    override fun observeConversations(includeArchived: Boolean): Flow<List<Conversation>> {
        sessionProvider.synchronize()
        val accountId = requireAccount().accountId
        return combine(dao.observeConversations(accountId, includeArchived), typing) { rows, typingByConversation ->
            rows.map { row ->
                row.toDomain(cipher).copy(typingUserIds = typingByConversation[row.id].orEmpty())
            }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun refreshConversations(reset: Boolean): ChatResult<Page<Conversation>> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val generation = sessionGeneration.snapshot()
        return remoteCall {
            var cursor = if (reset) null else dao.remoteKey(account.accountId, CONVERSATIONS)?.nextCursor
            val seenCursors = mutableSetOf<String>()
            val rowsById = linkedMapOf<String, ConversationEntity>()
            val timestamp = now()
            var nextCursor: String? = cursor
            var hasMore = true
            var pages = 0
            val pageLimit = if (reset) MAX_CONVERSATION_PAGES_PER_REFRESH else 1

            while (pages < pageLimit && hasMore) {
                val response = api.conversations(PAGE_SIZE_CONVERSATIONS, cursor)
                val mapped = response.conversations.map { it.toEntity(account.accountId, cipher, timestamp) }
                enrichMissingProfiles(mapped).forEach { rowsById[it.id] = it }
                pages += 1
                nextCursor = response.nextCursor?.trim()?.takeIf(String::isNotEmpty)
                hasMore = response.hasMore && nextCursor != null
                if (!hasMore) {
                    break
                }
                if (!seenCursors.add(nextCursor!!)) {
                    hasMore = false
                    break
                }
                cursor = nextCursor
            }

            val entities = rowsById.values.toList()
            val key = RemoteKeyEntity(
                account.accountId,
                CONVERSATIONS,
                nextCursor,
                hasMore,
                timestamp,
            )
            check(sessionGeneration.isCurrent(generation)) { "نشست کاربر تغییر کرده است" }
            // Flutter treats the conversation list as an upsert-only cache and
            // removes rows only after an explicit delete/clear event. Absence
            // from a cursor snapshot is not deletion evidence (requests and
            // secret conversations may be omitted by backend filters), so a
            // refresh must never erase previously visible conversations.
            dao.upsertConversations(entities)
            // Some backend deployments keep the deleted message as the
            // conversation preview for a short period. A local delete
            // tombstone is stronger evidence than that stale projection;
            // rebuild the preview from the newest non-tombstoned message.
            entities.forEach { entity ->
                val tombstoneAt = dao.latestTombstoneAt(account.accountId, entity.id)
                if (tombstoneAt != null &&
                    (entity.lastMessageAtEpochMillis == null || entity.lastMessageAtEpochMillis <= tombstoneAt)
                ) {
                    refreshConversationPreview(account.accountId, entity.id)
                }
            }
            recoverOrphanedConversations(account.accountId, timestamp)
            dao.upsertRemoteKey(key)
            Page(
                items = entities.map { it.toDomain(cipher) },
                nextCursor = PaginationCursor(nextCursor),
                hasMore = hasMore,
            )
        }
    }

    override suspend fun loadMoreConversations(): ChatResult<Page<Conversation>> {
        val account = sessionProvider.account.value ?: return loggedOut()
        val key = dao.remoteKey(account.accountId, CONVERSATIONS)
        if (key?.hasMore == false) return ChatResult.Success(Page(emptyList(), PaginationCursor(null), false))
        return refreshConversations(reset = false)
    }

    override fun observeSuggestedUsers(): Flow<List<ChatUser>> {
        sessionProvider.synchronize()
        val accountId = requireAccount().accountId
        return dao.observeConversations(accountId, includeArchived = true).map { rows ->
            rows.toSuggestedUsers(accountId)
        }
    }

    override suspend fun refreshSuggestedUsers(): ChatResult<List<ChatUser>> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val cached = dao.conversations(account.accountId).toSuggestedUsers(account.accountId)
        val conversationCandidates = remoteCall {
            val timestamp = now()
            val rows = api.conversations(SUGGESTED_USERS_CONVERSATION_LIMIT, null).conversations
                .map { it.toEntity(account.accountId, cipher, timestamp) }
                .let { enrichMissingProfiles(it) }
            dao.upsertConversations(rows)
            rows.toSuggestedUsers(account.accountId)
        }
        if (conversationCandidates is ChatResult.Success && conversationCandidates.value.isNotEmpty()) {
            return conversationCandidates
        }
        if (conversationCandidates is ChatResult.Failure && cached.isNotEmpty()) {
            return ChatResult.Success(cached)
        }

        val following = remoteCall { api.followingProfiles(account.accountId).resolvedProfiles }
        val followers = remoteCall { api.followerProfiles(account.accountId).resolvedProfiles }
        val merged = linkedMapOf<String, ChatUser>()
        var fallbackSucceeded = false
        var fallbackFailure: ChatResult.Failure? = null
        listOf(following, followers).forEach { result ->
            when (result) {
                is ChatResult.Success -> {
                    fallbackSucceeded = true
                    result.value.asSequence()
                        .mapNotNull { it.toChatUserOrNull(account.accountId) }
                        .forEach { user -> merged[user.id] = user }
                }
                is ChatResult.Failure -> if (fallbackFailure == null) fallbackFailure = result
            }
        }
        if (fallbackSucceeded) return ChatResult.Success(merged.values.toList())
        return (conversationCandidates as? ChatResult.Failure)
            ?: fallbackFailure
            ?: ChatResult.Failure("بارگذاری کاربران پیشنهادی انجام نشد", retryable = true)
    }

    override suspend fun searchUsers(query: String): ChatResult<List<ChatUser>> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val normalized = query.trim()
        if (normalized.isEmpty()) return ChatResult.Success(emptyList())
        return remoteCall {
            api.searchProfiles(normalized).resolvedProfiles
                .asSequence()
                .mapNotNull { it.toChatUserOrNull(account.accountId) }
                .toList()
        }
    }

    override suspend fun createConversation(peerId: String, isSecret: Boolean): ChatResult<Conversation> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val normalizedPeerId = peerId.trim()
        if (normalizedPeerId.isEmpty() || normalizedPeerId == account.accountId) {
            return ChatResult.Failure("کاربر انتخاب‌شده معتبر نیست", retryable = false)
        }
        return remoteCall {
            val base = api.createConversation(CreateConversationRequest(normalizedPeerId, isSecret))
                .toEntity(account.accountId, cipher, now())
            val entity = enrichMissingProfiles(listOf(base)).single()
            dao.upsertConversations(listOf(entity))
            entity.toDomain(cipher)
        }
    }

    override fun observeProfileNotes(): Flow<List<ProfileNote>> {
        sessionProvider.synchronize()
        val accountId = requireAccount().accountId
        return dao.observeProfileNotes(accountId).map { rows ->
            rows.mapNotNull { row -> runCatching { row.toDomainNote() }.getOrNull() }
        }
    }

    override suspend fun refreshProfileNotes(): ChatResult<Unit> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val generation = sessionGeneration.snapshot()
        return remoteCall {
            val own = try {
                api.ownProfileNote()
            } catch (error: HttpException) {
                if (error.code() == 404) null else throw error
            }
            val peerIds = dao.conversations(account.accountId).asSequence()
                .filter { it.type != ConversationType.GROUP.name }
                .mapNotNull { it.peerId?.trim()?.takeIf(String::isNotEmpty) }
                .filter { it != account.accountId }
                .distinct()
                .take(PROFILE_NOTES_BATCH_SIZE)
                .toList()
            val others = if (peerIds.isEmpty()) {
                emptyList()
            } else {
                api.profileNotesBatch(ProfileBatchRequest(peerIds)).values.toList()
            }
            val timestamp = now()
            val notes = (listOfNotNull(own) + others)
                .distinctBy { it.userId }
                .map { it.toNoteEntity(account.accountId, timestamp, it.userId == account.accountId) }
                .filter { it.expiresAtEpochMillis > timestamp }
            check(sessionGeneration.isCurrent(generation)) { "نشست کاربر تغییر کرده است" }
            dao.deleteProfileNotes(account.accountId)
            if (notes.isNotEmpty()) dao.upsertProfileNotes(notes)
        }
    }

    override suspend fun upsertOwnProfileNote(content: String): ChatResult<ProfileNote> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val normalized = content.trim()
        if (normalized.isEmpty() || normalized.length > PROFILE_NOTE_MAX_LENGTH) {
            return ChatResult.Failure("یادداشت باید بین ۱ تا ۶۰ کاراکتر باشد", retryable = false)
        }
        return remoteCall {
            val entity = api.upsertOwnProfileNote(UpsertProfileNoteRequest(normalized))
                .toNoteEntity(account.accountId, now(), isMine = true)
            dao.upsertProfileNotes(listOf(entity))
            entity.toDomainNote()
        }
    }

    override suspend fun deleteOwnProfileNote(): ChatResult<Unit> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        return remoteCall {
            api.deleteOwnProfileNote()
            dao.deleteProfileNote(account.accountId, account.accountId)
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> {
        sessionProvider.synchronize()
        val accountId = requireAccount().accountId
        mediaDownloader.recover(accountId)
        return dao.observeMessages(accountId, conversationId)
            .map { rows -> rows.map { it.toDomain(cipher) } }
            .flowOn(Dispatchers.Default)
    }

    override fun observeDownloads(conversationId: String): Flow<List<DownloadTask>> {
        sessionProvider.synchronize()
        val accountId = requireAccount().accountId
        mediaDownloader.recover(accountId)
        return dao.observeDownloads(accountId, conversationId).map { rows ->
            rows.map { row -> row.toDownloadTask() }
        }
    }

    override suspend fun startDownload(message: Message): ChatResult<DownloadTask> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val attachment = message.attachment ?: return ChatResult.Failure("این پیام فایل ندارد", false)
        val remoteUrl = attachment.remoteUrl?.trim()?.takeIf(String::isNotEmpty)
            ?: return ChatResult.Failure("نشانی دانلود فایل موجود نیست", false)
        return try {
            val row = mediaDownloader.start(
                accountId = account.accountId,
                conversationId = message.conversationId,
                messageId = message.serverId ?: message.clientId,
                remoteUrl = remoteUrl,
                fileName = attachment.fileName ?: "file",
                mimeType = attachment.mimeType,
            )
            ChatResult.Success(row.toDownloadTask(attachment.fileName))
        } catch (error: Throwable) {
            ChatResult.Failure(error.message ?: "شروع دانلود ممکن نیست", true)
        }
    }

    override suspend fun pauseDownload(messageId: String): ChatResult<Unit> = downloadMutation {
        mediaDownloader.pause(it, messageId)
    }

    override suspend fun resumeDownload(messageId: String): ChatResult<Unit> = downloadMutation {
        mediaDownloader.resume(it, messageId)
    }

    override suspend fun cancelDownload(messageId: String): ChatResult<Unit> = downloadMutation {
        mediaDownloader.cancel(it, messageId)
    }

    override fun observeConversation(conversationId: String): Flow<Conversation?> {
        sessionProvider.synchronize()
        val accountId = requireAccount().accountId
        return dao.observeConversation(accountId, conversationId)
            .map { it?.toDomain(cipher) }
            .flowOn(Dispatchers.Default)
    }

    override suspend fun refreshMessages(conversationId: String): ChatResult<Page<Message>> =
        fetchMessages(conversationId, reset = true)

    override suspend fun loadOlderMessages(conversationId: String): ChatResult<Page<Message>> =
        fetchMessages(conversationId, reset = false)

    override suspend fun searchMessages(conversationId: String, query: String): ChatResult<List<Message>> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val normalized = query.trim()
        if (normalized.length < 2) return ChatResult.Success(emptyList())
        return remoteCall {
            api.searchMessages(conversationId, normalized).messages.map { dto ->
                decryptAndMapMessageDto(dto, account.accountId)
            }
        }
    }

    override suspend fun presence(userId: String): ChatResult<PresenceState> = remoteCall {
        val dto = api.presence(userId)
        PresenceState(
            userId = dto.userId,
            isOnline = dto.status.equals("online", ignoreCase = true),
            lastOnlineAtEpochMillis = dto.lastOnlineAt.epochMillisOrNull(),
            canViewLastSeen = dto.canViewLastSeen,
        )
    }

    override suspend fun partnerProfile(userId: String): ChatResult<ChatPartnerProfile> = remoteCall {
        val profile = api.profile(userId)
        ChatPartnerProfile(
            userId = profile.resolvedUserId.ifBlank { userId },
            username = profile.username?.trim()?.takeIf(String::isNotEmpty),
            fullName = profile.fullName?.trim()?.takeIf(String::isNotEmpty),
            avatarUrl = profile.avatarUrl.resolvedAvatarUrl(),
            bio = profile.bio?.trim()?.takeIf(String::isNotEmpty),
        )
    }

    private suspend fun fetchMessages(conversationId: String, reset: Boolean): ChatResult<Page<Message>> {
        val account = sessionProvider.account.value ?: return loggedOut()
        val fetchKey = "${account.accountId}:$conversationId"
        val mutex = messageFetchMutexes.getOrPut(fetchKey) { Mutex() }
        return mutex.withLock {
        val generation = sessionGeneration.snapshot()
        remoteCall {
            val localConv = dao.conversation(account.accountId, conversationId)
            val resolvedConversationId = localConv?.id ?: conversationId
            val scope = messageScope(resolvedConversationId)
            val key = dao.remoteKey(account.accountId, scope)
            if (!reset && key?.hasMore == false) return@remoteCall Page(emptyList(), PaginationCursor(null), false)
            val response = try {
                api.messages(resolvedConversationId, PAGE_SIZE_MESSAGES, if (reset) null else key?.nextCursor)
            } catch (httpEx: retrofit2.HttpException) {
                if (httpEx.code() == 404) {
                    val peerTarget = localConv?.peerId ?: conversationId
                    val created = createConversation(peerTarget)
                    if (created is ChatResult.Success) {
                        api.messages(created.value.id, PAGE_SIZE_MESSAGES, if (reset) null else key?.nextCursor)
                    } else throw httpEx
                } else throw httpEx
            }
            val messages = response.messages.map { dto ->
                decryptAndMapMessageDto(dto, account.accountId)
            }
            check(sessionGeneration.isCurrent(generation)) { "نشست کاربر تغییر کرده است" }
            messages.forEach { mergeAndPersist(it, MergeSource.REST) }
            val hasMore = response.hasMoreForPagination()
            dao.upsertRemoteKey(
                RemoteKeyEntity(account.accountId, scope, response.nextCursor, hasMore, now()),
            )
            Page(messages, PaginationCursor(response.nextCursor), hasMore)
        }
        }
    }

    private suspend fun decryptAndMapMessageDto(
        dto: ir.coffevista.vista_native.features.chat.data.remote.MessageDto,
        accountId: String,
    ): Message {
        val conversationId = dto.conversationId
        // 1. Process key exchange messages
        if (dto.messageType == "exchange_key" || dto.messageType == "exchange_key_reply") {
            if (dto.senderId != accountId && dto.content.isNotBlank()) {
                e2eeService.savePeerPublicKey(conversationId, dto.content)
                if (dto.messageType == "exchange_key") {
                    scope.launch {
                        val myKeyPair = e2eeService.getOrGenerateKeyPair(accountId)
                        runCatching {
                            api.sendMessage(
                                conversationId,
                                SendMessageRequest(
                                    id = newId(),
                                    content = myKeyPair.publicKeyB64,
                                    messageType = "exchange_key_reply",
                                ),
                            )
                        }
                    }
                }
            }
        }

        var contentText = dto.content
        var replyText = dto.replyToContent

        // 2. Decrypt if ciphertext
        val peerPub = e2eeService.getPeerPublicKey(conversationId)
        if (peerPub != null) {
            val myKeyPair = e2eeService.getOrGenerateKeyPair(accountId)
            val sharedSecret = runCatching { e2eeService.computeSharedSecret(myKeyPair.privateKey, peerPub) }.getOrNull()
            if (sharedSecret != null) {
                val binding = ir.coffevista.vista_native.features.chat.data.security.ChatE2EEService.messageBinding(conversationId, dto.senderId, dto.id)
                if (e2eeService.isEncryptedEnvelope(contentText)) {
                    contentText = e2eeService.decryptMessage(contentText, sharedSecret, binding)
                }
                if (replyText != null && e2eeService.isEncryptedEnvelope(replyText)) {
                    replyText = e2eeService.decryptMessage(replyText, sharedSecret, binding)
                }
            }
        }

        return dto.toDomain(accountId).copy(
            isMine = dto.senderId == accountId,
            content = when {
                dto.deletedAt != null -> MessageContent.Deleted
                dto.messageType.equals("sharedPost", true) || dto.messageType.equals("storyReply", true) ->
                    MessageContent.Structured(dto.messageType, contentText)
                dto.messageType == "exchange_key" || dto.messageType == "exchange_key_reply" ->
                    MessageContent.Text("🔒 ارتباط رمزنگاری‌شده (E2EE) برقرار شد.")
                else -> MessageContent.Text(contentText)
            },
            replyToContent = replyText,
        )
    }

    override suspend fun sendText(conversationId: String, text: String): ChatResult<Message> =
        sendTextInternal(conversationId, text, replyTo = null, replyOverride = null)

    override suspend fun sendSharedPost(
        conversationId: String,
        post: ir.coffevista.vista_native.features.chat.domain.model.SharedPostDraft,
    ): ChatResult<Message> = sendTextInternal(
        conversationId = conversationId,
        text = pendingJson.encodeToString(
            ir.coffevista.vista_native.features.chat.domain.model.SharedPostDraft.serializer(),
            post,
        ),
        replyTo = null,
        replyOverride = null,
        messageType = "sharedPost",
    )

    override suspend fun sendReply(
        conversationId: String,
        text: String,
        replyTo: Message,
    ): ChatResult<Message> = sendTextInternal(conversationId, text, replyTo, replyOverride = null)

    override suspend fun replyToProfileNote(
        conversationId: String,
        note: ProfileNote,
        senderName: String,
        text: String,
    ): ChatResult<Message> = sendTextInternal(
        conversationId = conversationId,
        text = text,
        replyTo = null,
        replyOverride = PendingReplyPayload(
            messageId = "note:${note.userId}",
            content = note.content.take(MAX_REPLY_PREVIEW_LENGTH),
            senderName = "یادداشت ${senderName.trim().ifBlank { "کاربر" }}",
            kind = "note",
        ),
    )

    override suspend fun createGroup(
        name: String,
        memberIds: List<String>,
        imageUrl: String?,
    ): ChatResult<Conversation> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val normalizedName = name.trim()
        val normalizedMembers = memberIds.map(String::trim).filter(String::isNotEmpty).distinct()
        if (normalizedName.isEmpty()) return ChatResult.Failure("نام گروه الزامی است", false)
        if (normalizedMembers.isEmpty()) return ChatResult.Failure("حداقل یک عضو انتخاب کنید", false)
        if (normalizedMembers.size > MAX_GROUP_INVITED_MEMBERS) {
            return ChatResult.Failure("حداکثر ۲۰ عضو مجاز است", false)
        }
        return remoteCall {
            val created = api.createGroup(CreateGroupRequest(normalizedName, normalizedMembers, imageUrl))
            // POST success is authoritative. A transient enrichment failure must not
            // surface as "create failed" and invite a duplicate retry.
            val info = fetchCreatedGroupInfoBestEffort { api.group(created.id) }
            val entity = ir.coffevista.vista_native.features.chat.data.remote.ConversationDto(
                id = created.id,
                conversationType = "group",
                name = info?.name?.takeIf(String::isNotBlank) ?: normalizedName,
                image = info?.image?.takeIf(String::isNotBlank) ?: imageUrl,
            ).toEntity(account.accountId, cipher, now())
            dao.upsertConversations(listOf(entity))
            entity.toDomain(cipher)
        }
    }

    override suspend fun groupInfo(conversationId: String): ChatResult<GroupInfo> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        return remoteCall {
            val info = api.group(conversationId)
            val members = api.groupMembers(conversationId).members
            val invite = fetchGroupInviteBestEffort { api.groupInvite(conversationId) }
            info.toDomainGroupInfo(
                isAdminOverride = info.isAdminFor(account.accountId, members),
                currentUserId = account.accountId,
                memberCountOverride = info.memberCount.takeIf { it > 0 } ?: members.size,
                inviteOverride = invite,
            )
        }
    }

    override suspend fun groupMembers(conversationId: String): ChatResult<List<GroupMember>> = remoteCall {
        val members = api.groupMembers(conversationId).members
        val profiles = members.map { it.userId }.filter(String::isNotBlank).chunked(PROFILE_BATCH_SIZE)
            .flatMap { api.profilesBatch(ProfileBatchRequest(it)).resolvedProfiles }
            .associateBy { it.resolvedUserId }
        members.map { member ->
            val profile = profiles[member.userId]
            GroupMember(
                userId = member.userId,
                username = profile?.username.orEmpty(),
                fullName = profile?.fullName,
                avatarUrl = profile?.avatarUrl.resolvedAvatarUrl(),
                isAdmin = member.isAdmin,
                joinedAtEpochMillis = member.joinedAt.epochMillisOrNull(),
            )
        }
    }

    override suspend fun updateGroup(
        conversationId: String,
        name: String?,
        imageUrl: String?,
    ): ChatResult<GroupInfo> {
        val account = sessionProvider.account.value ?: return loggedOut()
        val normalizedName = name?.trim()?.takeIf(String::isNotEmpty)
        if (normalizedName == null && imageUrl == null) return ChatResult.Failure("تغییری وارد نشده است", false)
        return remoteCall {
            api.updateGroup(conversationId, UpdateGroupRequest(normalizedName, imageUrl))
            val info = api.group(conversationId)
            val existing = dao.conversation(account.accountId, conversationId)
            if (existing != null) dao.upsertConversations(
                listOf(existing.copy(title = info.name, avatarUrl = info.image.resolvedAvatarUrl(), lastSyncedAtEpochMillis = now())),
            )
            info.toDomainGroupInfo(
                isAdminOverride = info.isAdminFor(account.accountId, emptyList()),
                currentUserId = account.accountId,
            )
        }
    }

    override suspend fun addGroupMembers(conversationId: String, memberIds: List<String>): ChatResult<Int> {
        val normalized = memberIds.map(String::trim).filter(String::isNotEmpty).distinct()
        if (normalized.isEmpty()) return ChatResult.Failure("عضوی انتخاب نشده است", false)
        return remoteCall { api.addGroupMembers(conversationId, GroupMembersRequest(normalized)).added }
    }

    override suspend fun removeGroupMember(conversationId: String, memberId: String): ChatResult<Unit> = remoteCall {
        api.removeGroupMember(conversationId, memberId)
    }

    override suspend fun setGroupAdmin(
        conversationId: String,
        memberId: String,
        makeAdmin: Boolean,
    ): ChatResult<Unit> = remoteCall {
        api.setGroupAdmin(conversationId, memberId, SetGroupAdminRequest(makeAdmin))
    }

    override suspend fun groupInvite(conversationId: String): ChatResult<String?> = remoteCall {
        api.groupInvite(conversationId).inviteCode
    }

    override suspend fun setGroupInviteEnabled(conversationId: String, enabled: Boolean): ChatResult<String?> = remoteCall {
        api.updateGroupInvite(conversationId, GroupInviteRequest(enabled)).inviteCode
    }

    override suspend fun regenerateGroupInvite(conversationId: String): ChatResult<String?> = remoteCall {
        api.updateGroupInvite(conversationId, GroupInviteRequest()).inviteCode
    }

    override suspend fun joinGroup(inviteCode: String): ChatResult<Conversation> {
        val account = sessionProvider.account.value ?: return loggedOut()
        val code = inviteCode.trim().substringAfterLast('/').trim()
        if (code.isEmpty()) return ChatResult.Failure("کد دعوت معتبر نیست", false)
        return remoteCall {
            val joined = api.joinGroup(code)
            val info = api.group(joined.id)
            val entity = ir.coffevista.vista_native.features.chat.data.remote.ConversationDto(
                id = joined.id,
                conversationType = "group",
                name = info.name,
                image = info.image,
            ).toEntity(account.accountId, cipher, now())
            dao.upsertConversations(listOf(entity))
            entity.toDomain(cipher)
        }
    }

    override suspend fun leaveGroup(conversationId: String): ChatResult<Unit> {
        val account = sessionProvider.account.value ?: return loggedOut()
        return remoteCall {
            api.leaveGroup(conversationId)
            dao.deleteConversationMessages(account.accountId, conversationId)
            dao.deleteConversation(account.accountId, conversationId)
        }
    }

    override suspend fun deleteGroup(conversationId: String): ChatResult<Unit> {
        val account = sessionProvider.account.value ?: return loggedOut()
        return remoteCall {
            api.deleteGroup(conversationId)
            dao.deleteConversationMessages(account.accountId, conversationId)
            dao.deleteConversation(account.accountId, conversationId)
        }
    }

    private suspend fun sendTextInternal(
        conversationId: String,
        text: String,
        replyTo: Message?,
        replyOverride: PendingReplyPayload?,
        messageType: String = "text",
    ): ChatResult<Message> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val normalized = text.trim()
        if (normalized.isEmpty()) return ChatResult.Failure("پیام خالی است", retryable = false)
        if (normalized.length > MAX_TEXT_LENGTH) {
            return ChatResult.Failure("متن پیام بیش از حد مجاز است", retryable = false)
        }
        val conversation = dao.conversation(account.accountId, conversationId)
        val clientId = newId()
        val replyPayload = replyOverride ?: replyTo?.let { source ->
            val replyContent = when (val content = source.content) {
                is MessageContent.Text -> content.value
                is MessageContent.Structured -> content.payload
                MessageContent.Deleted -> "این پیام حذف شده است"
                MessageContent.EncryptedUnavailable -> return ChatResult.Failure(
                    "پاسخ به پیام محافظت‌شده در دسترس نیست",
                    retryable = false,
                )
            }.take(MAX_REPLY_PREVIEW_LENGTH)
            PendingReplyPayload(
                messageId = source.serverId ?: source.clientId,
                content = replyContent,
                senderName = if (source.isMine) "شما" else conversation?.title.orEmpty().ifBlank { "کاربر" },
                kind = source.attachment?.kind?.wireName
                    ?: (source.content as? MessageContent.Structured)?.kind
                    ?: "text",
            )
        }
        val pendingPayload = PendingTextPayload(
            text = normalized,
            reply = replyPayload,
            messageType = messageType,
        )
        val optimistic = Message(
            accountId = account.accountId,
            conversationId = conversationId,
            clientId = clientId,
            serverId = null,
            senderId = account.accountId,
            content = if (messageType == "text") MessageContent.Text(normalized)
            else MessageContent.Structured(messageType, normalized),
            createdAtEpochMillis = now(),
            status = MessageStatus.PENDING,
            replyToMessageId = replyPayload?.messageId,
            replyToContent = replyPayload?.content,
            replyToSenderName = replyPayload?.senderName,
            replyToKind = replyPayload?.kind,
            isMine = true,
        )
        mergeAndPersist(optimistic, MergeSource.OPTIMISTIC)
        dao.upsertPending(
            PendingOperationEntity(
                accountId = account.accountId,
                conversationId = conversationId,
                clientId = clientId,
                type = "SEND_TEXT",
                payloadCiphertext = cipher.encrypt(
                    account.accountId,
                    conversationId,
                    "$clientId:pending",
                    pendingJson.encodeToString(PendingTextPayload.serializer(), pendingPayload),
                ),
                attempts = 0,
                createdAtEpochMillis = optimistic.createdAtEpochMillis,
            ),
        )
        return sendPending(optimistic, pendingPayload)
    }

    override suspend fun sendAttachment(
        conversationId: String,
        draft: ChatAttachmentDraft,
    ): ChatResult<Message> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val conversation = dao.conversation(account.accountId, conversationId)
        if (conversation?.type == ConversationType.SECRET.name) {
            return ChatResult.Failure(
                "ارسال رسانه در گفتگوی محرمانه تا آماده‌شدن رمزنگاری فایل در دسترس نیست",
                retryable = false,
            )
        }
        if (draft.uri.isBlank() || draft.fileName.isBlank() || draft.mimeType.isBlank() || draft.sizeBytes <= 0L) {
            return ChatResult.Failure("فایل انتخاب‌شده معتبر نیست", retryable = false)
        }
        val caption = draft.caption.trim().take(MAX_TEXT_LENGTH)
        val prepared = try {
            mediaUploader.prepare(
                accountId = account.accountId,
                conversationId = conversationId,
                draft = draft.copy(caption = caption),
            )
        } catch (error: Throwable) {
            return ChatResult.Failure(
                error.message?.takeIf(String::isNotBlank) ?: "فایل انتخاب‌شده قابل ارسال نیست",
                retryable = false,
            )
        }
        val validatedDraft = prepared.draft
        val clientId = newId()
        val createdAt = now()
        val optimistic = Message(
            accountId = account.accountId,
            conversationId = conversationId,
            clientId = clientId,
            serverId = null,
            senderId = account.accountId,
            content = MessageContent.Text(caption),
            createdAtEpochMillis = createdAt,
            status = MessageStatus.PENDING,
            attachment = Attachment(
                kind = validatedDraft.kind,
                localUri = validatedDraft.uri,
                fileName = validatedDraft.fileName,
                mimeType = validatedDraft.mimeType,
                sizeBytes = validatedDraft.sizeBytes,
                durationSeconds = validatedDraft.durationSeconds,
                progress = 0f,
                mediaGroupId = validatedDraft.mediaGroupId,
                transferState = TransferState.QUEUED,
            ),
            isMine = true,
        )
        mergeAndPersist(optimistic, MergeSource.OPTIMISTIC)
        dao.upsertTransfer(
            TransferTaskEntity(
                accountId = account.accountId,
                conversationId = conversationId,
                clientId = clientId,
                localUri = validatedDraft.uri,
                objectKey = prepared.objectKey,
                mimeType = validatedDraft.mimeType,
                fileName = validatedDraft.fileName,
                sizeBytes = validatedDraft.sizeBytes,
                kind = validatedDraft.kind.name,
                durationSeconds = validatedDraft.durationSeconds,
                mediaGroupId = validatedDraft.mediaGroupId,
                captionCiphertext = cipher.encrypt(
                    account.accountId,
                    conversationId,
                    "$clientId:transfer-caption",
                    caption,
                ),
                progress = 0f,
                state = TransferState.QUEUED.name,
                attempts = 0,
                createdAtEpochMillis = createdAt,
            ),
        )
        return uploadAndSendAttachment(optimistic, prepared)
    }

    override suspend fun sendRemoteGif(conversationId: String, url: String): ChatResult<Message> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val conversation = dao.conversation(account.accountId, conversationId)
        if (conversation?.type == ConversationType.SECRET.name) {
            return ChatResult.Failure("ارسال گیف در این گفتگوی محافظت‌شده در دسترس نیست", retryable = false)
        }
        val normalizedUrl = url.trim()
        val parsed = runCatching { Uri.parse(normalizedUrl) }.getOrNull()
        if (parsed?.scheme != "https" || parsed.host.isNullOrBlank()) {
            return ChatResult.Failure("آدرس گیف معتبر نیست", retryable = false)
        }
        val clientId = newId()
        val optimistic = Message(
            accountId = account.accountId,
            conversationId = conversationId,
            clientId = clientId,
            serverId = null,
            senderId = account.accountId,
            content = MessageContent.Text(""),
            createdAtEpochMillis = now(),
            status = MessageStatus.PENDING,
            attachment = Attachment(
                kind = AttachmentKind.GIF,
                remoteUrl = normalizedUrl,
                fileName = null,
                mimeType = "image/gif",
                sizeBytes = null,
                progress = 1f,
                transferState = TransferState.COMPLETE,
            ),
            isMine = true,
        )
        val payload = PendingGifPayload(normalizedUrl)
        mergeAndPersist(optimistic, MergeSource.OPTIMISTIC)
        dao.upsertPending(
            PendingOperationEntity(
                accountId = account.accountId,
                conversationId = conversationId,
                clientId = clientId,
                type = "SEND_GIF",
                payloadCiphertext = cipher.encrypt(
                    account.accountId,
                    conversationId,
                    "$clientId:pending",
                    pendingJson.encodeToString(PendingGifPayload.serializer(), payload),
                ),
                attempts = 0,
                createdAtEpochMillis = optimistic.createdAtEpochMillis,
            ),
        )
        return sendPendingGif(optimistic, payload)
    }

    override suspend fun retryMessage(conversationId: String, clientId: String): ChatResult<Message> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val transfer = dao.transfer(account.accountId, clientId)
        if (transfer != null) {
            val caption = cipher.decrypt(
                account.accountId,
                conversationId,
                "$clientId:transfer-caption",
                transfer.captionCiphertext,
            )
            val existing = dao.messageByIdentity(account.accountId, conversationId, clientId, null)
                ?.toDomain(cipher)
                ?: return ChatResult.Failure("فایل محلی یافت نشد", retryable = false)
            dao.upsertTransfer(transfer.copy(attempts = transfer.attempts + 1, state = TransferState.QUEUED.name))
            val retrying = existing.copy(
                status = MessageStatus.PENDING,
                attachment = existing.attachment?.copy(progress = 0f, transferState = TransferState.QUEUED),
            )
            mergeAndPersist(retrying, MergeSource.OPTIMISTIC)
            val prepared = try {
                mediaUploader.prepare(
                    accountId = account.accountId,
                    conversationId = conversationId,
                    existingObjectKey = transfer.objectKey,
                    draft = ChatAttachmentDraft(
                    uri = transfer.localUri,
                    fileName = transfer.fileName,
                    mimeType = transfer.mimeType,
                    sizeBytes = transfer.sizeBytes,
                    kind = runCatching { AttachmentKind.valueOf(transfer.kind) }
                        .getOrDefault(AttachmentKind.UNKNOWN),
                    durationSeconds = transfer.durationSeconds,
                    caption = caption,
                    mediaGroupId = transfer.mediaGroupId,
                ),
                )
            } catch (error: Throwable) {
                val failed = retrying.copy(
                    status = MessageStatus.FAILED,
                    attachment = retrying.attachment?.copy(transferState = TransferState.FAILED),
                )
                mergeAndPersist(failed, MergeSource.OPTIMISTIC)
                dao.updateTransfer(account.accountId, clientId, transfer.progress, TransferState.FAILED.name)
                return ChatResult.Failure(
                    error.message?.takeIf(String::isNotBlank) ?: "فایل محلی قابل ارسال نیست",
                    retryable = false,
                )
            }
            if (transfer.objectKey != prepared.objectKey) {
                dao.upsertTransfer(transfer.copy(objectKey = prepared.objectKey, state = TransferState.QUEUED.name))
            }
            return uploadAndSendAttachment(retrying, prepared)
        }
        val pending = dao.pending(account.accountId, clientId)
            ?: return ChatResult.Failure("پیام قابل تلاش مجدد نیست", retryable = false)
        val decrypted = cipher.decrypt(account.accountId, conversationId, "$clientId:pending", pending.payloadCiphertext)
        val existing = dao.messageByIdentity(account.accountId, conversationId, clientId, null)
            ?.toDomain(cipher)
            ?: return ChatResult.Failure("پیام محلی یافت نشد", retryable = false)
        dao.upsertPending(pending.copy(attempts = pending.attempts + 1))
        val retrying = existing.copy(status = MessageStatus.PENDING)
        mergeAndPersist(retrying, MergeSource.OPTIMISTIC)
        return when (pending.type) {
            "SEND_TEXT" -> sendPending(retrying, decodePendingTextPayload(decrypted))
            "SEND_GIF" -> sendPendingGif(retrying, pendingJson.decodeFromString(PendingGifPayload.serializer(), decrypted))
            else -> ChatResult.Failure("نوع پیام قابل تلاش مجدد نیست", retryable = false)
        }
    }

    override suspend fun cancelTransfer(conversationId: String, clientId: String): ChatResult<Unit> {
        val account = sessionProvider.account.value ?: return loggedOut()
        val transfer = dao.transfer(account.accountId, clientId)
            ?: return ChatResult.Failure("انتقال فعالی یافت نشد", retryable = false)
        cancelledTransfers += clientId
        mediaUploader.cancel(clientId)
        dao.updateTransfer(account.accountId, clientId, transfer.progress, TransferState.CANCELLED.name)
        dao.updateMessageTransfer(account.accountId, clientId, transfer.progress, TransferState.CANCELLED.name)
        return ChatResult.Success(Unit)
    }

    private suspend fun sendPending(local: Message, payload: PendingTextPayload): ChatResult<Message> {
        val generation = sessionGeneration.snapshot()
        val conversation = dao.conversation(local.accountId, local.conversationId)
        val isSecretTarget = conversation?.type == ConversationType.SECRET.name
        val peerPub = e2eeService.getPeerPublicKey(local.conversationId)
        if (isSecretTarget && peerPub == null) {
            val localKey = e2eeService.getOrGenerateKeyPair(local.accountId)
            val exchange = remoteCall {
                api.sendMessage(
                    local.conversationId,
                    SendMessageRequest(
                        id = newId(),
                        content = localKey.publicKeyB64,
                        messageType = "exchange_key",
                    ),
                )
            }
            if (sessionGeneration.isCurrent(generation)) {
                mergeAndPersist(local.copy(status = MessageStatus.FAILED), MergeSource.OPTIMISTIC)
            }
            return ChatResult.Failure(
                if (exchange is ChatResult.Success) {
                    "کلید رمزنگاری در حال تبادل است؛ پس از تأیید دوباره تلاش کنید"
                } else {
                    "تبادل کلید رمزنگاری انجام نشد؛ دوباره تلاش کنید"
                },
                retryable = true,
            )
        }

        val secretSharedKey = if (isSecretTarget) {
            val localKey = e2eeService.getOrGenerateKeyPair(local.accountId)
            runCatching { e2eeService.computeSharedSecret(localKey.privateKey, requireNotNull(peerPub)) }.getOrNull()
                ?: run {
                    if (sessionGeneration.isCurrent(generation)) {
                        mergeAndPersist(local.copy(status = MessageStatus.FAILED), MergeSource.OPTIMISTIC)
                    }
                    return ChatResult.Failure("کلید رمزنگاری گفتگو معتبر نیست", retryable = true)
                }
        } else {
            null
        }

        return remoteCall(
            onFailure = {
                if (sessionGeneration.isCurrent(generation)) {
                    mergeAndPersist(local.copy(status = MessageStatus.FAILED), MergeSource.OPTIMISTIC)
                }
            },
        ) {
            var contentToSend = payload.text
            var replyContentToSend = payload.reply?.content

            if (secretSharedKey != null) {
                val binding = ir.coffevista.vista_native.features.chat.data.security.ChatE2EEService.messageBinding(local.conversationId, local.accountId, local.clientId)
                contentToSend = e2eeService.encryptMessage(payload.text, secretSharedKey, binding)
                if (replyContentToSend != null) {
                    replyContentToSend = e2eeService.encryptMessage(replyContentToSend, secretSharedKey, binding)
                }
            }

            val dto = api.sendMessage(
                local.conversationId,
            SendMessageRequest(
                id = local.clientId,
                content = contentToSend,
                messageType = payload.messageType,
                    replyToMessageId = payload.reply?.messageId,
                    replyToContent = replyContentToSend,
                    replyToSenderName = payload.reply?.senderName,
                    replyToKind = payload.reply?.kind,
                ),
            )
            check(sessionGeneration.isCurrent(generation)) { "نشست کاربر تغییر کرده است" }
            val server = decryptAndMapMessageDto(dto, local.accountId).copy(isMine = true)
            val merged = MessageMergeReducer.merge(local, server, MergeSource.REST)
            mergeAndPersist(merged, MergeSource.REST)
            dao.deletePending(local.accountId, local.clientId)
            merged
        }
    }

    private suspend fun sendPendingGif(local: Message, payload: PendingGifPayload): ChatResult<Message> {
        val generation = sessionGeneration.snapshot()
        return remoteCall(
            onFailure = {
                if (sessionGeneration.isCurrent(generation)) {
                    mergeAndPersist(local.copy(status = MessageStatus.FAILED), MergeSource.OPTIMISTIC)
                }
            },
        ) {
            val dto = api.sendMessage(
                local.conversationId,
                SendMessageRequest(
                    id = local.clientId,
                    content = "",
                    messageType = AttachmentKind.GIF.wireName,
                    mediaUrl = payload.url,
                    attachmentMimeType = "image/gif",
                ),
            )
            check(sessionGeneration.isCurrent(generation)) { "نشست کاربر تغییر کرده است" }
            val server = dto.toDomain(local.accountId).copy(isMine = true)
            val merged = MessageMergeReducer.merge(local, server, MergeSource.REST)
            mergeAndPersist(merged, MergeSource.REST)
            dao.deletePending(local.accountId, local.clientId)
            merged
        }
    }

    private suspend fun uploadAndSendAttachment(
        local: Message,
        prepared: ir.coffevista.vista_native.features.chat.data.remote.PreparedChatUpload,
    ): ChatResult<Message> {
        val generation = sessionGeneration.snapshot()
        var uploadedObjectKey: String? = null
        return remoteCall(
        onFailure = {
            uploadedObjectKey?.let { key ->
                runCatching { mediaUploader.deleteUploadedObject(key) }
            }
            if (!sessionGeneration.isCurrent(generation)) return@remoteCall
            val latestProgress = dao.transfer(local.accountId, local.clientId)?.progress
                ?: local.attachment?.progress
                ?: 0f
            if (cancelledTransfers.remove(local.clientId)) {
                val cancelled = local.copy(
                    status = MessageStatus.FAILED,
                    attachment = local.attachment?.copy(
                        progress = latestProgress,
                        transferState = TransferState.CANCELLED,
                    ),
                )
                mergeAndPersist(cancelled, MergeSource.OPTIMISTIC)
                dao.updateTransfer(
                    local.accountId,
                    local.clientId,
                    latestProgress,
                    TransferState.CANCELLED.name,
                )
            } else {
                val failed = local.copy(
                    status = MessageStatus.FAILED,
                    attachment = local.attachment?.copy(
                        progress = latestProgress,
                        transferState = TransferState.FAILED,
                    ),
                )
                mergeAndPersist(failed, MergeSource.OPTIMISTIC)
                dao.updateTransfer(
                    local.accountId,
                    local.clientId,
                    latestProgress,
                    TransferState.FAILED.name,
                )
            }
        },
    ) {
        val draft = prepared.draft
        cancelledTransfers.remove(local.clientId)
        dao.updateTransfer(local.accountId, local.clientId, 0f, TransferState.UPLOADING.name)
        dao.updateMessageTransfer(local.accountId, local.clientId, 0f, TransferState.UPLOADING.name)
        val uploaded = mediaUploader.upload(
            accountId = local.accountId,
            clientId = local.clientId,
            prepared = prepared,
            onProgress = { progress ->
                check(sessionGeneration.isCurrent(generation)) { "نشست کاربر تغییر کرده است" }
                check(local.clientId !in cancelledTransfers) { "انتقال لغو شد" }
                scope.launch {
                    if (sessionGeneration.isCurrent(generation)) {
                        dao.advanceTransferProgress(local.accountId, local.clientId, progress)
                        dao.advanceMessageTransferProgress(local.accountId, local.clientId, progress)
                    }
                }
            },
        )
        uploadedObjectKey = uploaded.objectKey
        check(sessionGeneration.isCurrent(generation)) { "نشست کاربر تغییر کرده است" }
        if (local.clientId in cancelledTransfers) {
            runCatching { mediaUploader.deleteUploadedObject(uploaded.objectKey) }
            throw java.io.IOException("انتقال لغو شد")
        }
        val dto = api.sendMessage(
            local.conversationId,
            SendMessageRequest(
                id = local.clientId,
                content = draft.caption,
                messageType = draft.kind.wireName,
                mediaUrl = uploaded.objectUrl,
                attachmentFileName = draft.fileName,
                attachmentMimeType = draft.mimeType,
                attachmentSizeBytes = draft.sizeBytes,
                duration = draft.durationSeconds,
                mediaGroupId = draft.mediaGroupId,
            ),
        )
        check(sessionGeneration.isCurrent(generation)) { "نشست کاربر تغییر کرده است" }
        val server = dto.toDomain(local.accountId).copy(isMine = true)
        val voiceSourcePath = draft.uri.toLocalVoiceCachePath()
        val ephemeralVoiceFile = draft.kind == AttachmentKind.VOICE &&
            voiceSourcePath?.replace('\\', '/')?.contains("/chat-voice/") == true
        val merged = MessageMergeReducer.merge(local, server, MergeSource.REST).copy(
            attachment = server.attachment?.copy(
                localUri = if (ephemeralVoiceFile) null else draft.uri,
                progress = 1f,
                transferState = TransferState.COMPLETE,
            ),
        )
        mergeAndPersist(merged, MergeSource.REST)
        cancelledTransfers.remove(local.clientId)
        dao.deleteTransfer(local.accountId, local.clientId)
        if (ephemeralVoiceFile) {
            voiceSourcePath?.let { path -> runCatching { File(path).delete() } }
        }
        merged
    }
    }

    override suspend fun markRead(conversationId: String): ChatResult<Unit> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        return remoteCall {
            api.markRead(conversationId)
            dao.resetUnread(account.accountId, conversationId)
        }
    }

    override suspend fun sendTyping(conversationId: String): ChatResult<Unit> = remoteCall {
        api.typing(conversationId)
    }

    override suspend fun setConversationActive(
        conversationId: String,
        active: Boolean,
    ): ChatResult<Unit> = remoteCall {
        api.setActive(conversationId, ActiveConversationRequest(active))
    }

    override suspend fun toggleConversationFlag(
        conversationId: String,
        flag: String,
    ): ChatResult<Conversation> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val allowed = setOf("archive", "mute", "pin")
        if (flag !in allowed) return ChatResult.Failure("عملیات گفتگو معتبر نیست", retryable = false)
        return remoteCall {
            val entity = api.toggleConversationFlag(conversationId, flag).toEntity(account.accountId, cipher, now())
            dao.upsertConversations(listOf(entity))
            entity.toDomain(cipher)
        }
    }

    override suspend fun deleteConversation(conversationId: String): ChatResult<Unit> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val normalizedId = conversationId.trim()
        if (normalizedId.isEmpty()) return ChatResult.Failure("گفتگو معتبر نیست", retryable = false)
        dao.deleteConversationMessages(account.accountId, normalizedId)
        dao.deleteConversation(account.accountId, normalizedId)
        return when (val result = remoteCall { api.deleteConversation(normalizedId) }) {
            is ChatResult.Success -> result
            is ChatResult.Failure -> {
                refreshConversations(reset = true)
                result
            }
        }
    }

    override suspend fun acceptMessageRequest(conversationId: String): ChatResult<Unit> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val normalizedId = conversationId.trim()
        if (normalizedId.isEmpty()) return ChatResult.Failure("گفتگو معتبر نیست", retryable = false)
        return remoteCall {
            api.acceptMessageRequest(normalizedId)
            dao.resolveMessageRequest(account.accountId, normalizedId, "accepted")
        }
    }

    override suspend fun rejectMessageRequest(conversationId: String): ChatResult<Unit> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val normalizedId = conversationId.trim()
        if (normalizedId.isEmpty()) return ChatResult.Failure("گفتگو معتبر نیست", retryable = false)
        return remoteCall {
            api.rejectMessageRequest(normalizedId)
            dao.deleteConversationWithMessages(account.accountId, normalizedId)
        }
    }

    override suspend fun editMessage(message: Message, content: String): ChatResult<Unit> {
        if (message.content is MessageContent.EncryptedUnavailable) {
            return ChatResult.Failure("ویرایش پیام محافظت‌شده در دسترس نیست", retryable = false)
        }
        val normalized = content.trim()
        if (normalized.isEmpty() || normalized.length > MAX_TEXT_LENGTH) {
            return ChatResult.Failure("متن ویرایش معتبر نیست", retryable = false)
        }
        val messageId = message.serverId ?: return ChatResult.Failure("پیام هنوز ارسال نشده است", retryable = false)
        return remoteCall {
            api.editMessage(messageId, EditMessageRequest(normalized))
            mergeAndPersist(
                message.copy(content = MessageContent.Text(normalized), editedAtEpochMillis = now()),
                MergeSource.REST,
            )
        }
    }

    override suspend fun deleteMessage(message: Message, forEveryone: Boolean): ChatResult<Unit> {
        val messageId = message.serverId ?: message.clientId
        return remoteCall {
            if (message.serverId != null) api.deleteMessage(messageId, forEveryone)
            dao.upsertTombstone(
                TombstoneEntity(message.accountId, message.conversationId, messageId, now()),
            )
            dao.deleteMessageByIdentity(message.accountId, message.conversationId, messageId)
            refreshConversationPreview(message.accountId, message.conversationId)
        }
    }

    override suspend fun forwardMessage(
        messageId: String,
        targetConversationId: String,
    ): ChatResult<Message> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        return remoteCall {
            val dto = api.forwardMessage(messageId, ForwardMessageRequest(targetConversationId))
            val message = dto.toDomain(account.accountId).copy(isMine = true)
            mergeAndPersist(message, MergeSource.REST)
            message
        }
    }

    override suspend fun toggleReaction(message: Message, emoji: String): ChatResult<Unit> {
        val messageId = message.serverId ?: return ChatResult.Failure("پیام هنوز ارسال نشده است", retryable = false)
        val normalized = emoji.trim()
        if (normalized.isEmpty()) return ChatResult.Failure("واکنش معتبر نیست", retryable = false)
        return remoteCall {
            val update = api.toggleReaction(messageId, ReactionRequest(normalized))
            val reactions = update.reactions.groupBy({ it.emoji }, { it.userId }).mapValues { it.value.toSet() }
            mergeAndPersist(message.copy(reactions = reactions), MergeSource.REST)
        }
    }

    override suspend fun setForeground(foreground: Boolean) { realtime.setForeground(foreground) }

    override suspend fun reconcileNotification(payload: Map<String, String>): ChatResult<Unit> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val conversationId = payload["conversation_id"]?.trim().orEmpty()
        val messageId = payload["message_id"]?.trim().orEmpty()
        val senderId = payload["sender_id"]?.trim().orEmpty()
        if (conversationId.isEmpty() || messageId.isEmpty() || senderId.isEmpty()) {
            return ChatResult.Failure("اعلان گفتگو نامعتبر است", retryable = false)
        }
        val raw = payload["content"].orEmpty()
        val content = if (raw.startsWith("VE2E1:") || raw.startsWith("e2ee:v1:")) {
            MessageContent.EncryptedUnavailable
        } else {
            MessageContent.Text(raw)
        }
        val message = Message(
            accountId = account.accountId,
            conversationId = conversationId,
            clientId = messageId,
            serverId = messageId,
            senderId = senderId,
            content = content,
            createdAtEpochMillis = payload["created_at"].epochMillisOrNull() ?: now(),
            status = MessageStatus.DELIVERED,
            isMine = senderId == account.accountId,
        )
        mergeAndPersist(message, MergeSource.NOTIFICATION)
        return ChatResult.Success(Unit)
    }

    override suspend fun clearAccount(accountId: String) {
        sessionGeneration.invalidate()
        sessionProvider.clear()
        typingExpiry.values.forEach(Job::cancel)
        typingExpiry.clear()
        typing.value = emptyMap()
        cancelledTransfers.clear()
        blockStatusCache.keys.removeIf { it.startsWith("$accountId:") }
        realtime.stop()
        mediaUploader.cancelAccount(accountId)
        mediaDownloader.clearAccount(accountId)
        dao.clearAccount(accountId)
    }

    private suspend fun applyRealtimeEvent(event: RealtimeEvent) {
        val account = sessionProvider.account.value ?: return
        when (event) {
            is RealtimeEvent.NewMessage -> mergeAndPersist(
                incoming = decryptAndMapMessageDto(event.message, account.accountId),
                source = MergeSource.REALTIME,
            )
            is RealtimeEvent.MessageUpdated -> mergeAndPersist(
                incoming = decryptAndMapMessageDto(event.message, account.accountId),
                source = MergeSource.REALTIME,
            )
            is RealtimeEvent.MessageDeleted -> {
                val conversationId = event.conversationId ?: return
                dao.upsertTombstone(
                    TombstoneEntity(account.accountId, conversationId, event.messageId, now()),
                )
                dao.deleteMessageByIdentity(account.accountId, conversationId, event.messageId)
                refreshConversationPreview(account.accountId, conversationId)
            }
            is RealtimeEvent.ConversationUpdated -> dao.upsertConversations(
                listOf(event.conversation.toEntity(account.accountId, cipher, now())),
            )
            is RealtimeEvent.ConversationCleared -> event.conversationId?.let {
                dao.deleteConversationMessages(account.accountId, it)
            }
            is RealtimeEvent.Typing -> applyTyping(event.state.conversationId, event.state.userId)
            is RealtimeEvent.Read -> {
                val messages = dao.messages(account.accountId, event.receipt.conversationId)
                    .map { it.toDomain(cipher) }
                    .filter { it.isMine && it.createdAtEpochMillis <= event.receipt.readAtEpochMillis }
                    .map { it.copy(status = MessageStatus.READ).toEntity(cipher, now()) }
                dao.upsertMessages(messages)
            }
            is RealtimeEvent.Connected -> retryPendingOperations(account.accountId)
            is RealtimeEvent.Unknown -> Unit
        }
    }

    private suspend fun retryPendingOperations(accountId: String) = pendingRetryMutex.withLock {
        dao.pendingTransfers(accountId).forEach { transfer ->
            if (transfer.state == TransferState.CANCELLED.name) return@forEach
            val entity = dao.messageByIdentity(
                accountId,
                transfer.conversationId,
                transfer.clientId,
                null,
            ) ?: return@forEach
            val caption = cipher.decrypt(
                accountId,
                transfer.conversationId,
                "${transfer.clientId}:transfer-caption",
                transfer.captionCiphertext,
            )
            val draft = ChatAttachmentDraft(
                    uri = transfer.localUri,
                    fileName = transfer.fileName,
                    mimeType = transfer.mimeType,
                    sizeBytes = transfer.sizeBytes,
                    kind = runCatching { AttachmentKind.valueOf(transfer.kind) }.getOrDefault(AttachmentKind.UNKNOWN),
                    durationSeconds = transfer.durationSeconds,
                    caption = caption,
                    mediaGroupId = transfer.mediaGroupId,
                )
            val prepared = runCatching {
                mediaUploader.prepare(
                    accountId = accountId,
                    conversationId = transfer.conversationId,
                    draft = draft,
                    existingObjectKey = transfer.objectKey,
                )
            }.getOrNull() ?: return@forEach
            if (transfer.objectKey != prepared.objectKey) {
                dao.upsertTransfer(transfer.copy(objectKey = prepared.objectKey, state = TransferState.QUEUED.name))
            }
            uploadAndSendAttachment(
                entity.toDomain(cipher).copy(status = MessageStatus.PENDING),
                prepared,
            )
        }
        dao.pendingForAccount(accountId).forEach { pending ->
            if (pending.type !in setOf("SEND_TEXT", "SEND_GIF")) return@forEach
            val entity = dao.messageByIdentity(
                accountId,
                pending.conversationId,
                pending.clientId,
                null,
            ) ?: return@forEach
            val message = entity.toDomain(cipher)
            val decrypted = cipher.decrypt(
                accountId,
                pending.conversationId,
                "${pending.clientId}:pending",
                pending.payloadCiphertext,
            )
            dao.upsertPending(pending.copy(attempts = pending.attempts + 1))
            when (pending.type) {
                "SEND_TEXT" -> sendPending(message.copy(status = MessageStatus.PENDING), decodePendingTextPayload(decrypted))
                "SEND_GIF" -> sendPendingGif(
                    message.copy(status = MessageStatus.PENDING),
                    pendingJson.decodeFromString(PendingGifPayload.serializer(), decrypted),
                )
            }
        }
    }

    private fun applyTyping(conversationId: String, userId: String) {
        val key = "$conversationId:$userId"
        typing.value = typing.value + (conversationId to (typing.value[conversationId].orEmpty() + userId))
        typingExpiry.remove(key)?.cancel()
        typingExpiry[key] = scope.launch {
            delay(TYPING_TTL_MILLIS)
            val remaining = typing.value[conversationId].orEmpty() - userId
            typing.value = if (remaining.isEmpty()) typing.value - conversationId else typing.value + (conversationId to remaining)
            typingExpiry.remove(key)
        }
    }

    private suspend fun mergeAndPersist(incoming: Message, source: MergeSource) {
        val identities = listOfNotNull(incoming.serverId, incoming.clientId).distinct()
        if (identities.any { identity ->
                dao.isTombstoned(incoming.accountId, incoming.conversationId, identity)
            }
        ) return
        val existingEntity = dao.messageByIdentity(
            incoming.accountId,
            incoming.conversationId,
            incoming.clientId,
            incoming.serverId,
        )
        val existing = existingEntity?.toDomain(cipher)
        val merged = MessageMergeReducer.merge(existing, incoming, source)
        val preview = merged.inboxPreviewText()
        val mutationAt = now()
        val fallback = ConversationEntity(
            accountId = merged.accountId,
            id = merged.conversationId,
            type = ConversationType.PRIVATE.name,
            title = "کاربر",
            avatarUrl = null,
            peerId = if (merged.isMine) null else merged.senderId,
            lastMessageCiphertext = null,
            lastMessageAtEpochMillis = null,
            unreadCount = 0,
            isArchived = false,
            isPinned = false,
            isMuted = false,
            requestStatus = null,
            lastSyncedAtEpochMillis = mutationAt,
        )
        dao.mergeMessageAndConversation(
            message = merged.toEntity(cipher, mutationAt),
            replacedClientId = existingEntity?.clientId,
            fallbackConversation = fallback,
            previewCiphertext = cipher.encrypt(
                merged.accountId,
                merged.conversationId,
                "conversation-preview",
                preview,
            ),
            incrementUnread = existingEntity == null && !merged.isMine &&
                (source == MergeSource.REALTIME || source == MergeSource.NOTIFICATION),
            mutationAtEpochMillis = mutationAt,
        )
        val conversationAfterMerge = dao.conversation(merged.accountId, merged.conversationId)
        if (conversationAfterMerge?.title == "کاربر" || conversationAfterMerge?.avatarUrl.isNullOrBlank()) {
            val refreshed = runCatching {
                api.conversation(merged.conversationId).toEntity(merged.accountId, cipher, now())
            }.getOrNull()
            refreshed?.let { entity ->
                dao.upsertConversations(enrichMissingProfiles(listOf(entity)))
            }
        }
    }

    private suspend fun enrichMissingProfiles(
        conversations: List<ConversationEntity>,
    ): List<ConversationEntity> {
        // Flutter refreshes peer identity from the profile source even when a
        // conversation already carries an avatar: that URL can be stale or
        // revoked and must not permanently pin the inbox to a broken image.
        val missingIds = conversations.asSequence()
              .filter { it.type != ConversationType.GROUP.name }
            .mapNotNull { it.peerId?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
            .take(PROFILE_BATCH_SIZE)
            .toList()
        if (missingIds.isEmpty()) return conversations
        val batchProfiles = runCatching { api.profilesBatch(ProfileBatchRequest(missingIds)).resolvedProfiles }
              .getOrDefault(emptyList())
              .associateBy { it.resolvedUserId }
            .toMutableMap()
        // Flutter falls back to GET /profiles/{userId} when the lightweight
        // conversation/batch payload does not contain an avatar. Keep the
        // same bounded fallback so valid peers do not render as anonymous,
        // while avoiding an unbounded N+1 request on a large inbox.
        missingIds.asSequence()
            .filter { batchProfiles[it]?.avatarUrl.isNullOrBlank() }
            .take(PROFILE_INDIVIDUAL_FALLBACK_LIMIT)
            .forEach { userId ->
                val profile = runCatching { api.profile(userId) }.getOrNull()
                if (profile != null && profile.resolvedUserId.isNotBlank()) {
                    batchProfiles[userId] = profile
                }
            }
        conversations.asSequence()
            .filter {
                it.type != ConversationType.GROUP.name &&
                    it.title != "کاربر" &&
                    !it.peerId.isNullOrBlank() &&
                    batchProfiles[it.peerId?.trim()]?.avatarUrl.isNullOrBlank()
            }
            .distinctBy { it.peerId }
            .take(PROFILE_USERNAME_FALLBACK_LIMIT)
            .forEach { conversation ->
                val profile = runCatching { api.profileByUsername(conversation.title) }.getOrNull()
                if (profile != null && !profile.avatarUrl.isNullOrBlank()) {
                    batchProfiles[conversation.peerId.orEmpty().trim()] = profile
                }
            }
        return conversations.map { conversation ->
            val profile = conversation.peerId?.trim()?.let(batchProfiles::get) ?: return@map conversation
            val name = if (conversation.title != "کاربر") {
                conversation.title
            } else {
                listOf(profile.username, profile.fullName)
                    .firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotEmpty) }
                    ?: conversation.title
            }
            conversation.copy(
                title = name,
                  avatarUrl = profile.avatarUrl.resolvedAvatarUrl() ?: conversation.avatarUrl,
                lastSyncedAtEpochMillis = now(),
            )
        }
    }

    /**
     * Repairs rows removed by older destructive first-page refreshes without
     * weakening the account/encryption boundary. The existing conversation
     * endpoint remains authoritative; cached messages are only a fail-safe for
     * conversations omitted from the current list snapshot.
     */
    private suspend fun recoverOrphanedConversations(accountId: String, timestamp: Long) {
        val recovered = dao.orphanConversationIds(accountId).mapNotNull { conversationId ->
            runCatching {
                api.conversation(conversationId).toEntity(accountId, cipher, timestamp)
            }.getOrNull() ?: run {
                val messages = dao.messages(accountId, conversationId)
                val latest = messages.firstOrNull() ?: return@mapNotNull null
                val latestDomain = latest.toDomain(cipher)
                val preview = latestDomain.inboxPreviewText()
                ConversationEntity(
                    accountId = accountId,
                    id = conversationId,
                    type = if (messages.any { it.contentKind == "encrypted_unavailable" }) {
                        ConversationType.SECRET.name
                    } else {
                        ConversationType.PRIVATE.name
                    },
                    title = "کاربر",
                    avatarUrl = null,
                    peerId = messages.asSequence()
                        .map { it.senderId }
                        .firstOrNull { it.isNotBlank() && it != accountId },
                    lastMessageCiphertext = cipher.encrypt(
                        accountId,
                        conversationId,
                        "conversation-preview",
                        preview,
                    ),
                    lastMessageAtEpochMillis = latest.createdAtEpochMillis,
                    unreadCount = 0,
                    isArchived = false,
                    isPinned = false,
                    isMuted = false,
                    requestStatus = null,
                    lastSyncedAtEpochMillis = timestamp,
                )
            }
        }
        if (recovered.isNotEmpty()) {
            dao.upsertConversations(enrichMissingProfiles(recovered))
        }
    }

    private fun requireAccount() = checkNotNull(sessionProvider.account.value) { "Chat requires an authenticated account" }
    private fun loggedOut() = ChatResult.Failure("کاربر وارد نشده است", retryable = false)

    private suspend fun ir.coffevista.vista_native.features.chat.data.local.DownloadTaskEntity.toDownloadTask(
        fallbackName: String? = null,
    ): DownloadTask {
        val cachedFile = localPath?.let(::File)?.takeIf(File::isFile)
        val effectiveState = runCatching { DownloadState.valueOf(state) }.getOrDefault(DownloadState.FAILED)
            .let { parsed -> if (parsed == DownloadState.COMPLETE && cachedFile == null) DownloadState.FAILED else parsed }
        return DownloadTask(
        accountId = accountId,
        conversationId = conversationId,
        messageId = messageId,
        localUri = cachedFile?.let { Uri.fromFile(it).toString() },
        mimeType = mimeType,
        fileName = fallbackName ?: runCatching {
            cipher.decrypt(accountId, conversationId, "$messageId:download-name", fileNameCiphertext)
        }.getOrDefault("فایل"),
        receivedBytes = receivedBytes,
        totalBytes = totalBytes,
        state = effectiveState,
        attempts = attempts,
    )
    }

    private suspend fun downloadMutation(block: suspend (String) -> Unit): ChatResult<Unit> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        return try {
            block(account.accountId)
            ChatResult.Success(Unit)
        } catch (error: Throwable) {
            ChatResult.Failure(error.message ?: "عملیات دانلود انجام نشد", true)
        }
    }

    private fun decodePendingTextPayload(value: String): PendingTextPayload =
        runCatching { pendingJson.decodeFromString(PendingTextPayload.serializer(), value) }
            // Native builds before reply support stored the plaintext directly.
            .getOrElse { PendingTextPayload(text = value) }

    private suspend fun <T> remoteCall(
        onFailure: suspend () -> Unit = {},
        block: suspend () -> T,
    ): ChatResult<T> = try {
        ChatResult.Success(withContext(Dispatchers.IO) { block() })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        onFailure()
        ChatResult.Failure(
            message = when (error) {
                is HttpException -> when (error.code()) {
                    401 -> "لطفاً دوباره وارد شوید"
                    403 -> "اجازه انجام این عملیات را ندارید"
                    404 -> "گفتگو یا پیام یافت نشد"
                    429 -> "تعداد درخواست‌ها زیاد است؛ کمی بعد تلاش کنید"
                    else -> "ارتباط با سرویس گفتگو ناموفق بود"
                }
                else -> "اتصال برقرار نشد؛ دوباره تلاش کنید"
            },
            retryable = error !is HttpException || error.code() >= 500 || error.code() == 429,
        )
    }

    private suspend fun refreshConversationPreview(accountId: String, conversationId: String) {
        val conversation = dao.conversation(accountId, conversationId) ?: return
        val latest = dao.messages(accountId, conversationId).firstOrNull()
        val latestDomain = latest?.toDomain(cipher)
        dao.upsertConversations(
            listOf(
                conversation.copy(
                    lastMessageCiphertext = latestDomain?.inboxPreviewText()?.let { preview ->
                        cipher.encrypt(accountId, conversationId, "conversation-preview", preview)
                    },
                    lastMessageAtEpochMillis = latest?.createdAtEpochMillis,
                    lastSyncedAtEpochMillis = now(),
                ),
            ),
        )
    }

      private fun messageScope(conversationId: String) = "messages:$conversationId"

      private suspend fun ProfileNoteDto.toNoteEntity(
          accountId: String,
          fallbackTimestamp: Long,
          isMine: Boolean,
      ): ProfileNoteEntity {
          val created = createdAt.epochMillisOrNull() ?: fallbackTimestamp
          val expires = expiresAt.epochMillisOrNull() ?: (created + PROFILE_NOTE_TTL_MILLIS)
          return ProfileNoteEntity(
              accountId = accountId,
              userId = userId,
              id = id?.trim()?.takeIf(String::isNotEmpty) ?: userId,
              contentCiphertext = cipher.encrypt(accountId, PROFILE_NOTE_SCOPE, userId, content),
              createdAtEpochMillis = created,
              expiresAtEpochMillis = expires,
              isMine = isMine,
          )
      }

      private suspend fun ProfileNoteEntity.toDomainNote(): ProfileNote = ProfileNote(
          id = id,
          userId = userId,
          content = cipher.decrypt(accountId, PROFILE_NOTE_SCOPE, userId, contentCiphertext),
          createdAtEpochMillis = createdAtEpochMillis,
          expiresAtEpochMillis = expiresAtEpochMillis,
          isMine = isMine,
      )

    override fun observeSharedMedia(conversationId: String): Flow<List<Message>> {
        sessionProvider.synchronize()
        val accountId = sessionProvider.account.value?.accountId ?: return flowOf(emptyList())
        return dao.observeSharedMedia(accountId, conversationId).map { entities ->
            entities.map { it.toDomain(cipher) }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun getPinnedMessages(conversationId: String): ChatResult<List<Message>> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        return remoteCall {
            val response = api.getPinnedMessages(conversationId)
            val messages = response.messages.map { dto ->
                dto.toDomain(account.accountId).copy(
                    isMine = dto.senderId == account.accountId,
                    isPinned = true,
                )
            }
            dao.clearPinnedMessages(account.accountId, conversationId)
            messages.forEach { mergeAndPersist(it, MergeSource.REST) }
            messages
        }
    }

    override suspend fun pinMessage(conversationId: String, messageId: String): ChatResult<Unit> {
        val account = sessionProvider.account.value ?: return loggedOut()
        return remoteCall {
            api.pinMessage(messageId)
            val entity = dao.messageByIdentity(account.accountId, conversationId, messageId, messageId)
            if (entity != null) {
                dao.upsertMessage(entity.copy(isPinned = true, lastMutationAtEpochMillis = now()))
            }
        }
    }

    override suspend fun unpinMessage(conversationId: String, messageId: String): ChatResult<Unit> {
        val account = sessionProvider.account.value ?: return loggedOut()
        return remoteCall {
            api.unpinMessage(messageId)
            val entity = dao.messageByIdentity(account.accountId, conversationId, messageId, messageId)
            if (entity != null) {
                dao.upsertMessage(entity.copy(isPinned = false, lastMutationAtEpochMillis = now()))
            }
        }
    }

    override suspend fun blockUser(userId: String): ChatResult<Unit> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val result = remoteCall {
            api.blockUser(TargetUserRequest(userId))
        }
        if (result is ChatResult.Success) blockStatusCache.remove("${account.accountId}:$userId")
        return result
    }

    override suspend fun unblockUser(userId: String): ChatResult<Unit> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val result = remoteCall {
            api.unblockUser(TargetUserRequest(userId))
        }
        if (result is ChatResult.Success) blockStatusCache.remove("${account.accountId}:$userId")
        return result
    }

    override suspend fun getBlockStatus(userId: String, forceRefresh: Boolean): ChatResult<BlockStatus> {
        sessionProvider.synchronize()
        val account = sessionProvider.account.value ?: return loggedOut()
        val key = "${account.accountId}:$userId"
        val cached = blockStatusCache[key]
        if (!forceRefresh && cached != null && now() - cached.cachedAtEpochMillis < BLOCK_STATUS_CACHE_MILLIS) {
            return ChatResult.Success(cached.status)
        }
        val result = remoteCall { api.blockStatus(userId).toDomainBlockStatus() }
        if (result is ChatResult.Success) {
            blockStatusCache[key] = CachedBlockStatus(result.value, now())
            return result
        }
        return cached?.let { ChatResult.Success(it.status) } ?: result
    }

    override suspend fun reportUser(
        userId: String,
        reason: ModerationReason,
        additionalDetails: String?,
    ): ChatResult<Unit> {
        sessionProvider.synchronize()
        if (sessionProvider.account.value == null) return loggedOut()
        return remoteCall {
            api.reportUser(
                ReportUserRequest(
                    userId = userId,
                    reason = reason.wireName,
                    additionalDetails = additionalDetails?.trim()?.takeIf(String::isNotEmpty),
                ),
            )
        }
    }

    private fun BlockStatusDto.toDomainBlockStatus() = BlockStatus(
        isBlocked = isBlocked,
        isBlockedBy = isBlockedBy,
        blockedAtEpochMillis = blockedAt.epochMillisOrNull(),
        blockedByAtEpochMillis = blockedByAt.epochMillisOrNull(),
    )

      private fun GroupInfoDto.toDomainGroupInfo(
          isAdminOverride: Boolean = isAdmin,
          currentUserId: String? = null,
          memberCountOverride: Int = memberCount,
          inviteOverride: GroupInviteDto? = null,
      ) = GroupInfo(
          id = id,
          name = name,
          imageUrl = image.resolvedAvatarUrl(),
          memberCount = memberCountOverride,
          maxMembers = maxMembers.takeIf { it > 0 } ?: 20,
          inviteCode = inviteOverride?.inviteCode ?: inviteCode,
          inviteEnabled = inviteOverride?.inviteEnabled ?: inviteOverride?.enabled ?: inviteEnabled,
          isAdmin = isAdminOverride,
          createdByUserId = createdBy?.trim()?.takeIf(String::isNotEmpty),
          currentUserId = currentUserId?.trim()?.takeIf(String::isNotEmpty),
      )

      private companion object {
        const val CONVERSATIONS = "conversations"
        const val PAGE_SIZE_CONVERSATIONS = 50
        const val SUGGESTED_USERS_CONVERSATION_LIMIT = 100
        const val MAX_CONVERSATION_PAGES_PER_REFRESH = 20
        const val PAGE_SIZE_MESSAGES = 50
        const val MAX_TEXT_LENGTH = 16_000
        const val MAX_REPLY_PREVIEW_LENGTH = 500
        const val MAX_GROUP_INVITED_MEMBERS = 19
          const val PROFILE_BATCH_SIZE = 15
          const val PROFILE_INDIVIDUAL_FALLBACK_LIMIT = 6
          const val PROFILE_USERNAME_FALLBACK_LIMIT = 4
          const val PROFILE_NOTES_BATCH_SIZE = 20
          const val PROFILE_NOTE_MAX_LENGTH = 60
          const val PROFILE_NOTE_TTL_MILLIS = 24 * 60 * 60 * 1_000L
          const val PROFILE_NOTE_SCOPE = "profile-note"
        const val TYPING_TTL_MILLIS = 8_000L
        const val BLOCK_STATUS_CACHE_MILLIS = 5 * 60 * 1_000L
    }
}
internal suspend fun fetchCreatedGroupInfoBestEffort(
    fetch: suspend () -> GroupInfoDto,
): GroupInfoDto? = try {
    fetch()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

internal suspend fun fetchGroupInviteBestEffort(
    fetch: suspend () -> GroupInviteDto,
): GroupInviteDto? = try {
    fetch()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}


private fun List<ConversationEntity>.toSuggestedUsers(accountId: String): List<ChatUser> =
    asSequence()
        .filter { row ->
            row.type != ConversationType.GROUP.name &&
                !row.peerId.isNullOrBlank() &&
                row.peerId != accountId &&
                row.peerId != NIL_USER_ID
        }
        .distinctBy(ConversationEntity::peerId)
        .map { row ->
            ChatUser(
                id = row.peerId.orEmpty(),
                username = row.title,
                fullName = row.title,
                avatarUrl = row.avatarUrl,
                conversationId = row.id,
            )
        }
        .toList()

private fun ProfileDto.toChatUserOrNull(accountId: String): ChatUser? {
    val userId = resolvedUserId
    if (userId.isBlank() || userId == accountId || userId == NIL_USER_ID) return null
    return ChatUser(
        id = userId,
        username = username.orEmpty(),
        fullName = fullName,
        avatarUrl = avatarUrl.resolvedAvatarUrl(),
    )
}

private const val NIL_USER_ID = "00000000-0000-0000-0000-000000000000"

internal class SessionGenerationGuard {
    private val generation = AtomicLong(0L)

    fun snapshot(): Long = generation.get()

    fun isCurrent(snapshot: Long): Boolean = generation.get() == snapshot

    fun invalidate() {
        generation.incrementAndGet()
    }
}

internal fun String.toLocalVoiceCachePath(): String? {
    val source = trim()
    if (source.startsWith('/')) return source
    val uri = runCatching { java.net.URI(source) }.getOrNull() ?: return null
    return when {
        uri.scheme.equals("file", ignoreCase = true) -> uri.path
        else -> null
    }
}

private data class CachedBlockStatus(
    val status: BlockStatus,
    val cachedAtEpochMillis: Long,
)

@Serializable
private data class PendingTextPayload(
    val text: String,
    val reply: PendingReplyPayload? = null,
    val messageType: String = "text",
)

@Serializable
private data class PendingGifPayload(val url: String)

@Serializable
private data class PendingReplyPayload(
    val messageId: String,
    val content: String,
    val senderName: String,
    val kind: String,
)
