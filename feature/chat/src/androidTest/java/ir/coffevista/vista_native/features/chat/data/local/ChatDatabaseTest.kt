package ir.coffevista.vista_native.features.chat.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatDatabaseTest {
    private lateinit var database: ChatDatabase
    private lateinit var dao: ChatDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChatDatabase::class.java)
            .setQueryExecutor(Executors.newSingleThreadExecutor())
            .build()
        dao = database.chatDao()
    }

    @After
    fun close() = database.close()

    @Test
    fun accountIsolationAndLogoutCleanup() = runBlocking {
        dao.upsertConversations(listOf(conversation("account-a"), conversation("account-b")))
        dao.upsertProfileNotes(listOf(note("account-a"), note("account-b")))
        assertEquals(1, dao.observeConversations("account-a", false).first().size)
        assertEquals(1, dao.observeConversations("account-b", false).first().size)
        assertEquals(1, dao.observeProfileNotes("account-a").first().size)
        assertEquals(1, dao.observeProfileNotes("account-b").first().size)

        dao.clearAccount("account-a")

        assertEquals(0, dao.observeConversations("account-a", false).first().size)
        assertEquals(1, dao.observeConversations("account-b", false).first().size)
        assertEquals(0, dao.observeProfileNotes("account-a").first().size)
        assertEquals(1, dao.observeProfileNotes("account-b").first().size)
    }

    @Test
    fun messageRequestResolutionIsDurableInLocalCache() = runBlocking {
        val request = conversation("account-a").copy(
            requestStatus = "pending",
            isMessageRequest = true,
            lastMessageType = "image",
            lastMessageIsMine = true,
            lastMessageStatus = "READ",
        )
        dao.upsertConversations(listOf(request))

        dao.resolveMessageRequest("account-a", request.id, "accepted")

        val accepted = dao.conversation("account-a", request.id)
        assertEquals(false, accepted?.isMessageRequest)
        assertEquals("accepted", accepted?.requestStatus)
        assertEquals("image", accepted?.lastMessageType)
        assertEquals(true, accepted?.lastMessageIsMine)
        assertEquals("READ", accepted?.lastMessageStatus)

        dao.upsertMessage(message("request-message"))
        dao.deleteConversationWithMessages("account-a", request.id)

        assertEquals(null, dao.conversation("account-a", request.id))
        assertEquals(emptyList<MessageEntity>(), dao.messages("account-a", request.id))
    }

    @Test
    fun messageOrderingUsesTimestampThenStableIdentity() = runBlocking {
        val base = message("a")
        dao.upsertMessages(listOf(base, base.copy(clientId = "b", serverId = "b")))
        assertEquals(listOf("b", "a"), dao.observeMessages("account-a", "conversation-a").first().map { it.clientId })
    }

    @Test
    fun remoteCursorAndPendingQueueRemainAccountScoped() = runBlocking {
        dao.upsertRemoteKey(RemoteKeyEntity("account-a", "messages:conversation-a", "cursor-a", true, 1))
        dao.upsertRemoteKey(RemoteKeyEntity("account-b", "messages:conversation-a", "cursor-b", false, 1))
        dao.upsertPending(pending("account-a", "client-a"))
        dao.upsertPending(pending("account-b", "client-b"))

        assertEquals("cursor-a", dao.remoteKey("account-a", "messages:conversation-a")?.nextCursor)
        assertEquals(listOf("client-a"), dao.pendingForAccount("account-a").map { it.clientId })
        assertEquals(listOf("client-b"), dao.pendingForAccount("account-b").map { it.clientId })
    }

    @Test
    fun retentionKeepsPendingAndFailedRows() = runBlocking {
        val old = message("sent-old").copy(createdAtEpochMillis = 1, status = "SENT")
        val pending = message("pending-old").copy(createdAtEpochMillis = 1, status = "PENDING")
        val failed = message("failed-old").copy(createdAtEpochMillis = 1, status = "FAILED")
        dao.upsertMessages(listOf(old, pending, failed))

        dao.trimMessages("account-a", cutoffEpochMillis = 2)

        assertEquals(
            setOf("pending-old", "failed-old"),
            dao.messages("account-a", "conversation-a").map { it.clientId }.toSet(),
        )
    }

    @Test
    fun transferQueueSurvivesProgressUpdatesAndRemainsAccountScoped() = runBlocking {
        dao.upsertTransfer(transfer("account-a", "client-a"))
        dao.upsertTransfer(transfer("account-b", "client-b"))

        assertEquals(listOf("client-a"), dao.pendingTransfers("account-a").map { it.clientId })
        assertEquals(listOf("client-b"), dao.pendingTransfers("account-b").map { it.clientId })

        dao.updateTransfer("account-a", "client-a", progress = 0.65f, state = "UPLOADING")
        val restored = dao.transfer("account-a", "client-a")
        assertEquals(0.65f, restored?.progress)
        assertEquals("UPLOADING", restored?.state)

        dao.clearAccount("account-a")
        assertEquals(null, dao.transfer("account-a", "client-a"))
        assertEquals("client-b", dao.transfer("account-b", "client-b")?.clientId)
    }

    @Test
    fun downloadQueueIsMonotonicRecoverableAndAccountScoped() = runBlocking {
        dao.upsertDownload(download("account-a", "message-a"))
        dao.upsertDownload(download("account-b", "message-b"))

        assertEquals(listOf("message-a"), dao.recoverableDownloads("account-a").map { it.messageId })
        assertEquals(listOf("message-b"), dao.recoverableDownloads("account-b").map { it.messageId })

        dao.advanceDownload("account-a", "message-a", receivedBytes = 80, totalBytes = 100, updatedAt = 2)
        dao.advanceDownload("account-a", "message-a", receivedBytes = 40, totalBytes = 100, updatedAt = 3)
        val progressed = dao.download("account-a", "message-a")
        assertEquals(80L, progressed?.receivedBytes)
        assertEquals(100L, progressed?.totalBytes)
        assertEquals("DOWNLOADING", progressed?.state)
        assertEquals(listOf("message-a"), dao.downloadsForAccount("account-a").map { it.messageId })
        assertEquals(listOf("message-b"), dao.downloadsForAccount("account-b").map { it.messageId })

        dao.clearAccount("account-a")
        assertEquals(null, dao.download("account-a", "message-a"))
        assertEquals("message-b", dao.download("account-b", "message-b")?.messageId)
    }

    @Test
    fun orphanConversationDetectionIsAccountScopedAndStopsAfterRecovery() = runBlocking {
        dao.upsertMessage(message("orphan-a"))
        dao.upsertMessage(
            message("orphan-b").copy(
                accountId = "account-b",
                conversationId = "conversation-b",
            ),
        )

        assertEquals(listOf("conversation-a"), dao.orphanConversationIds("account-a"))
        assertEquals(listOf("conversation-b"), dao.orphanConversationIds("account-b"))

        dao.upsertConversations(listOf(conversation("account-a")))

        assertEquals(emptyList<String>(), dao.orphanConversationIds("account-a"))
        assertEquals(listOf("conversation-b"), dao.orphanConversationIds("account-b"))
    }

    @Test
    fun mergeTransactionReplacesOptimisticIdentityAndUpdatesPreview() = runBlocking {
        dao.upsertConversations(listOf(conversation("account-a")))
        dao.upsertMessage(message("optimistic").copy(serverId = null, status = "PENDING"))
        val acknowledged = message("server").copy(serverId = "server", status = "SENT", createdAtEpochMillis = 20)

        dao.mergeMessageAndConversation(
            message = acknowledged,
            replacedClientId = "optimistic",
            fallbackConversation = conversation("account-a"),
            previewCiphertext = byteArrayOf(9),
            incrementUnread = true,
            mutationAtEpochMillis = 30,
        )

        assertEquals(listOf("server"), dao.messages("account-a", "conversation-a").map { it.clientId })
        val mergedConversation = dao.conversation("account-a", "conversation-a")
        assertEquals(1, mergedConversation?.unreadCount)
        assertEquals(20L, mergedConversation?.lastMessageAtEpochMillis)
        assertEquals(listOf<Byte>(9), mergedConversation?.lastMessageCiphertext?.toList())
    }

    @Test
    fun migrationTwoToThreeCreatesAccountScopedEncryptedNotesTable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "chat-migration-2-3.db"
        context.deleteDatabase(name)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val versionTwo = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        versionTwo.writableDatabase
        versionTwo.close()

        val versionThree = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(2, oldVersion)
                        assertEquals(3, newVersion)
                        ChatDatabase.MIGRATION_2_3.migrate(db)
                    }
                })
                .build(),
        )
        try {
            val db = versionThree.writableDatabase
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='chat_profile_note'").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
            val columns = mutableSetOf<String>()
            db.query("PRAGMA table_info(chat_profile_note)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
            }
            assertEquals(
                setOf(
                    "accountId",
                    "userId",
                    "id",
                    "contentCiphertext",
                    "createdAtEpochMillis",
                    "expiresAtEpochMillis",
                    "isMine",
                ),
                columns,
            )
        } finally {
            versionThree.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migrationThreeToFourCreatesEncryptedAccountScopedDownloadQueue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "chat-migration-3-4.db"
        context.deleteDatabase(name)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val versionThree = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        versionThree.writableDatabase
        versionThree.close()

        val versionFour = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(3, oldVersion)
                        assertEquals(4, newVersion)
                        ChatDatabase.MIGRATION_3_4.migrate(db)
                    }
                })
                .build(),
        )
        try {
            val db = versionFour.writableDatabase
            db.query("PRAGMA table_info(chat_download_task)").use { cursor ->
                val columns = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
                assertEquals(true, "remoteUrlCiphertext" in columns)
                assertEquals(true, "fileNameCiphertext" in columns)
                assertEquals(true, "localPath" in columns)
                assertEquals(false, "remoteUrl" in columns)
                assertEquals(false, "fileName" in columns)
            }
        } finally {
            versionFour.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migrationFourToFivePreservesConversationsAndBackfillsPendingRequests() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "chat-migration-4-5.db"
        context.deleteDatabase(name)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val versionFour = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE chat_conversation (accountId TEXT NOT NULL, id TEXT NOT NULL, title TEXT NOT NULL, requestStatus TEXT, PRIMARY KEY(accountId, id))")
                        db.execSQL("INSERT INTO chat_conversation (accountId, id, title, requestStatus) VALUES ('account', 'normal', 'normal', NULL)")
                        db.execSQL("INSERT INTO chat_conversation (accountId, id, title, requestStatus) VALUES ('account', 'request', 'request', 'pending')")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        versionFour.writableDatabase
        versionFour.close()

        val versionFive = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(4, oldVersion)
                        assertEquals(5, newVersion)
                        ChatDatabase.MIGRATION_4_5.migrate(db)
                    }
                })
                .build(),
        )
        try {
            val db = versionFive.writableDatabase
            db.query("SELECT id, isMessageRequest, lastMessageType, lastMessageIsMine, lastMessageStatus FROM chat_conversation ORDER BY id").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("normal", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
                assertEquals(null, cursor.getString(2))
                assertEquals(0, cursor.getInt(3))
                assertEquals("SENT", cursor.getString(4))
                assertEquals(true, cursor.moveToNext())
                assertEquals("request", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
            }
        } finally {
            versionFive.close()
            context.deleteDatabase(name)
        }
    }


    @Test
    fun migrationFiveToSixPreservesTransfersAndAddsMediaGroupId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "chat-migration-5-6.db"
        context.deleteDatabase(name)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val versionFive = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE chat_transfer (accountId TEXT NOT NULL, conversationId TEXT NOT NULL, clientId TEXT NOT NULL, localUri TEXT NOT NULL, objectKey TEXT NOT NULL, mimeType TEXT NOT NULL, fileName TEXT NOT NULL, sizeBytes INTEGER NOT NULL, kind TEXT NOT NULL, durationSeconds INTEGER, captionCiphertext BLOB NOT NULL, progress REAL NOT NULL, state TEXT NOT NULL, attempts INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(accountId, clientId))")
                        db.execSQL("INSERT INTO chat_transfer (accountId, conversationId, clientId, localUri, objectKey, mimeType, fileName, sizeBytes, kind, durationSeconds, captionCiphertext, progress, state, attempts, createdAtEpochMillis) VALUES ('account', 'conversation', 'client', 'content://fixture/image', 'chat/image.jpg', 'image/jpeg', 'image.jpg', 12, 'IMAGE', NULL, X'01', 0, 'QUEUED', 0, 1)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build(),
        )
        versionFive.writableDatabase
        versionFive.close()

        val versionSix = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(5, oldVersion)
                        assertEquals(6, newVersion)
                        ChatDatabase.MIGRATION_5_6.migrate(db)
                    }
                })
                .build(),
        )
        try {
            val db = versionSix.writableDatabase
            db.query("PRAGMA table_info(chat_transfer)").use { cursor ->
                val columns = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
                assertEquals(true, "mediaGroupId" in columns)
            }
            db.query("SELECT clientId, mediaGroupId, state FROM chat_transfer").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("client", cursor.getString(0))
                assertEquals(null, cursor.getString(1))
                assertEquals("QUEUED", cursor.getString(2))
            }
            db.execSQL("UPDATE chat_transfer SET mediaGroupId = 'album-1' WHERE clientId = 'client'")
            db.query("SELECT mediaGroupId FROM chat_transfer WHERE clientId = 'client'").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("album-1", cursor.getString(0))
            }
        } finally {
            versionSix.close()
            context.deleteDatabase(name)
        }
    }

    private fun conversation(accountId: String) = ConversationEntity(
        accountId = accountId,
        id = "conversation-a",
        type = "PRIVATE",
        title = "fixture",
        avatarUrl = null,
        peerId = "peer",
        lastMessageCiphertext = null,
        lastMessageAtEpochMillis = 1,
        unreadCount = 0,
        isArchived = false,
        isPinned = false,
        isMuted = false,
        requestStatus = null,
        lastSyncedAtEpochMillis = 1,
    )

    private fun message(clientId: String) = MessageEntity(
        accountId = "account-a",
        conversationId = "conversation-a",
        clientId = clientId,
        serverId = clientId,
        senderId = "peer",
        contentCiphertext = byteArrayOf(1),
        contentKind = "text",
        createdAtEpochMillis = 1,
        editedAtEpochMillis = null,
        deletedAtEpochMillis = null,
        status = "SENT",
        replyToMessageId = null,
        replyToContentCiphertext = null,
        isMine = false,
        lastMutationAtEpochMillis = 1,
    )

    private fun pending(accountId: String, clientId: String) = PendingOperationEntity(
        accountId = accountId,
        clientId = clientId,
        conversationId = "conversation-a",
        type = "SEND_TEXT",
        payloadCiphertext = byteArrayOf(1),
        attempts = 0,
        createdAtEpochMillis = 1,
    )

    private fun transfer(accountId: String, clientId: String) = TransferTaskEntity(
        accountId = accountId,
        conversationId = "conversation-a",
        clientId = clientId,
        localUri = "content://fixture/$clientId",
        objectKey = "chat/$clientId.txt",
        mimeType = "text/plain",
        fileName = "$clientId.txt",
        sizeBytes = 12,
        kind = "DOCUMENT",
        durationSeconds = null,
        captionCiphertext = byteArrayOf(3),
        progress = 0f,
        state = "QUEUED",
        attempts = 0,
        createdAtEpochMillis = 1,
    )

    private fun download(accountId: String, messageId: String) = DownloadTaskEntity(
        accountId = accountId,
        conversationId = "conversation-a",
        messageId = messageId,
        remoteUrlCiphertext = byteArrayOf(1, 2),
        fileNameCiphertext = byteArrayOf(3, 4),
        mimeType = "application/pdf",
        localPath = null,
        receivedBytes = 0,
        totalBytes = 0,
        eTag = null,
        state = "QUEUED",
        attempts = 0,
        updatedAtEpochMillis = 1,
    )

    private fun note(accountId: String) = ProfileNoteEntity(
        accountId = accountId,
        userId = "peer-$accountId",
        id = "note-$accountId",
        contentCiphertext = byteArrayOf(7),
        createdAtEpochMillis = 1,
        expiresAtEpochMillis = Long.MAX_VALUE,
        isMine = false,
    )
}
