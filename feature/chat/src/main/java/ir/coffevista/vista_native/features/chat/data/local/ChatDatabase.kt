package ir.coffevista.vista_native.features.chat.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "chat_conversation",
    primaryKeys = ["accountId", "id"],
    indices = [Index("accountId", "isArchived", "lastMessageAtEpochMillis")],
)
data class ConversationEntity(
    val accountId: String,
    val id: String,
    val type: String,
    val title: String,
    val avatarUrl: String?,
    val peerId: String?,
    val lastMessageCiphertext: ByteArray?,
    val lastMessageAtEpochMillis: Long?,
    val unreadCount: Int,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val requestStatus: String?,
    val lastSyncedAtEpochMillis: Long,
    val isMessageRequest: Boolean = false,
    val lastMessageType: String? = null,
    val lastMessageIsMine: Boolean = false,
    val lastMessageStatus: String = "SENT",
)

@Entity(
    tableName = "chat_message",
    primaryKeys = ["accountId", "conversationId", "clientId"],
    indices = [
        Index("accountId", "conversationId", "createdAtEpochMillis"),
        Index("accountId", "conversationId", "serverId"),
    ],
)
data class MessageEntity(
    val accountId: String,
    val conversationId: String,
    val clientId: String,
    val serverId: String?,
    val senderId: String,
    val contentCiphertext: ByteArray,
    val contentKind: String,
    val createdAtEpochMillis: Long,
    val editedAtEpochMillis: Long?,
    val deletedAtEpochMillis: Long?,
    val status: String,
    val replyToMessageId: String?,
    val replyToContentCiphertext: ByteArray?,
    val isMine: Boolean,
    val lastMutationAtEpochMillis: Long,
    val messageType: String = "text",
    val mediaUrl: String? = null,
    val localUri: String? = null,
    val attachmentFileName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentSizeBytes: Long? = null,
    val durationSeconds: Int? = null,
    val audioTitle: String? = null,
    val audioArtist: String? = null,
    val audioAlbum: String? = null,
    val mediaGroupId: String? = null,
    val transferState: String = "COMPLETE",
    val transferProgress: Float = 1f,
    val replyToSenderName: String? = null,
    val replyToKind: String? = null,
    val reactionsJson: String = "{}",
    val isForwarded: Boolean = false,
    val forwardedFromSenderName: String? = null,
    val originalSenderId: String? = null,
    val originalMessageId: String? = null,
    val isPinned: Boolean = false,
)

@Entity(
    tableName = "chat_participant",
    primaryKeys = ["accountId", "conversationId", "userId"],
    indices = [Index("accountId", "conversationId")],
)
data class ParticipantEntity(
    val accountId: String,
    val conversationId: String,
    val userId: String,
    val displayName: String?,
    val avatarUrl: String?,
    val role: String?,
    val joinedAtEpochMillis: Long?,
)

@Entity(
    tableName = "chat_reaction",
    primaryKeys = ["accountId", "messageId", "userId", "emoji"],
    indices = [Index("accountId", "conversationId", "messageId")],
)
data class ReactionEntity(
    val accountId: String,
    val conversationId: String,
    val messageId: String,
    val userId: String,
    val emoji: String,
)

@Entity(
    tableName = "chat_draft",
    primaryKeys = ["accountId", "conversationId"],
)
data class DraftEntity(
    val accountId: String,
    val conversationId: String,
    val contentCiphertext: ByteArray,
    val replyToMessageId: String?,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "chat_transfer",
    primaryKeys = ["accountId", "clientId"],
    indices = [Index("accountId", "conversationId", "createdAtEpochMillis")],
)
data class TransferTaskEntity(
    val accountId: String,
    val conversationId: String,
    val clientId: String,
    val localUri: String,
    val objectKey: String,
    val mimeType: String,
    val fileName: String,
    val sizeBytes: Long,
    val kind: String,
    val durationSeconds: Int?,
    val mediaGroupId: String? = null,
    val captionCiphertext: ByteArray,
    val progress: Float,
    val state: String,
    val attempts: Int,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "chat_download_task",
    primaryKeys = ["accountId", "messageId"],
    indices = [Index("accountId", "conversationId", "updatedAtEpochMillis")],
)
data class DownloadTaskEntity(
    val accountId: String,
    val conversationId: String,
    val messageId: String,
    val remoteUrlCiphertext: ByteArray,
    val fileNameCiphertext: ByteArray,
    val mimeType: String?,
    val localPath: String?,
    val receivedBytes: Long,
    val totalBytes: Long,
    val eTag: String?,
    val state: String,
    val attempts: Int,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "chat_tombstone",
    primaryKeys = ["accountId", "conversationId", "messageId"],
    indices = [Index("accountId", "createdAtEpochMillis")],
)
data class TombstoneEntity(
    val accountId: String,
    val conversationId: String,
    val messageId: String,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "chat_pending_operation",
    primaryKeys = ["accountId", "clientId"],
    indices = [Index("accountId", "conversationId", "createdAtEpochMillis")],
)
data class PendingOperationEntity(
    val accountId: String,
    val conversationId: String,
    val clientId: String,
    val type: String,
    val payloadCiphertext: ByteArray,
    val attempts: Int,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "chat_remote_key",
    primaryKeys = ["accountId", "scope"],
)
data class RemoteKeyEntity(
    val accountId: String,
    val scope: String,
    val nextCursor: String?,
    val hasMore: Boolean,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "chat_profile_note",
    primaryKeys = ["accountId", "userId"],
    indices = [Index("accountId", "expiresAtEpochMillis")],
)
data class ProfileNoteEntity(
    val accountId: String,
    val userId: String,
    val id: String,
    val contentCiphertext: ByteArray,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    val isMine: Boolean,
)

@Dao
interface ChatDao {
    @Query(
        """
        SELECT * FROM chat_conversation
        WHERE accountId = :accountId AND (:includeArchived OR isArchived = 0)
        ORDER BY isPinned DESC, lastMessageAtEpochMillis DESC, id DESC
        """,
    )
    fun observeConversations(accountId: String, includeArchived: Boolean): Flow<List<ConversationEntity>>

    @Query(
        "SELECT * FROM chat_profile_note WHERE accountId = :accountId ORDER BY isMine DESC, createdAtEpochMillis DESC",
    )
    fun observeProfileNotes(accountId: String): Flow<List<ProfileNoteEntity>>

    @Query("SELECT * FROM chat_conversation WHERE accountId = :accountId ORDER BY isPinned DESC, lastMessageAtEpochMillis DESC")
    suspend fun conversations(accountId: String): List<ConversationEntity>

    @Query(
        """
        SELECT * FROM chat_message
        WHERE accountId = :accountId AND (
            conversationId = :conversationId
            OR conversationId IN (
                SELECT id FROM chat_conversation
                WHERE accountId = :accountId AND (id = :conversationId OR peerId = :conversationId)
            )
        )
          AND NOT EXISTS (
            SELECT 1 FROM chat_tombstone AS tombstone
            WHERE tombstone.accountId = chat_message.accountId
              AND (tombstone.messageId = chat_message.clientId OR tombstone.messageId = chat_message.serverId)
          )
        ORDER BY createdAtEpochMillis DESC, COALESCE(serverId, clientId) DESC
        """,
    )
    fun observeMessages(accountId: String, conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM chat_conversation WHERE accountId = :accountId AND (id = :conversationId OR peerId = :conversationId) LIMIT 1")
    fun observeConversation(accountId: String, conversationId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM chat_conversation WHERE accountId = :accountId AND (id = :conversationId OR peerId = :conversationId) LIMIT 1")
    suspend fun conversation(accountId: String, conversationId: String): ConversationEntity?

    @Query(
        """
        SELECT * FROM chat_message
        WHERE accountId = :accountId AND conversationId = :conversationId
          AND NOT EXISTS (
            SELECT 1 FROM chat_tombstone AS tombstone
            WHERE tombstone.accountId = chat_message.accountId
              AND tombstone.conversationId = chat_message.conversationId
              AND (tombstone.messageId = chat_message.clientId OR tombstone.messageId = chat_message.serverId)
          )
        ORDER BY createdAtEpochMillis DESC
        """,
    )
    suspend fun messages(accountId: String, conversationId: String): List<MessageEntity>

    @Query(
        """
        SELECT DISTINCT message.conversationId
        FROM chat_message AS message
        LEFT JOIN chat_conversation AS conversation
          ON conversation.accountId = message.accountId
         AND conversation.id = message.conversationId
        WHERE message.accountId = :accountId AND conversation.id IS NULL
        """,
    )
    suspend fun orphanConversationIds(accountId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversations(items: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfileNotes(items: List<ProfileNoteEntity>)

    @Query("DELETE FROM chat_profile_note WHERE accountId = :accountId")
    suspend fun deleteProfileNotes(accountId: String)

    @Query("DELETE FROM chat_profile_note WHERE accountId = :accountId AND userId = :userId")
    suspend fun deleteProfileNote(accountId: String, userId: String)

    @Query("UPDATE chat_conversation SET unreadCount = 0 WHERE accountId = :accountId AND id = :conversationId")
    suspend fun resetUnread(accountId: String, conversationId: String)

    @Query("UPDATE chat_conversation SET isMessageRequest = 0, requestStatus = :status WHERE accountId = :accountId AND id = :conversationId")
    suspend fun resolveMessageRequest(accountId: String, conversationId: String, status: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(item: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(items: List<MessageEntity>)

    @Query("UPDATE chat_message SET isPinned = 0 WHERE accountId = :accountId AND conversationId = :conversationId AND isPinned = 1")
    suspend fun clearPinnedMessages(accountId: String, conversationId: String)

    @Query(
        """
        SELECT * FROM chat_message
        WHERE accountId = :accountId AND conversationId = :conversationId
          AND (clientId = :clientId OR (:serverId IS NOT NULL AND serverId = :serverId))
        LIMIT 1
        """,
    )
    suspend fun messageByIdentity(
        accountId: String,
        conversationId: String,
        clientId: String,
        serverId: String?,
    ): MessageEntity?

    @Query(
        """
        SELECT * FROM chat_message
        WHERE accountId = :accountId AND conversationId = :conversationId AND attachmentMimeType IS NOT NULL
          AND NOT EXISTS (
            SELECT 1 FROM chat_tombstone AS tombstone
            WHERE tombstone.accountId = chat_message.accountId
              AND tombstone.conversationId = chat_message.conversationId
              AND (tombstone.messageId = chat_message.clientId OR tombstone.messageId = chat_message.serverId)
          )
        ORDER BY createdAtEpochMillis DESC
        """
    )
    fun observeSharedMedia(accountId: String, conversationId: String): Flow<List<MessageEntity>>

    @Query(
        "DELETE FROM chat_message WHERE accountId = :accountId AND conversationId = :conversationId AND clientId = :clientId",
    )
    suspend fun deleteMessage(accountId: String, conversationId: String, clientId: String)

    @Query(
        """
        DELETE FROM chat_message
        WHERE accountId = :accountId AND conversationId = :conversationId
          AND (clientId = :messageId OR serverId = :messageId)
        """,
    )
    suspend fun deleteMessageByIdentity(accountId: String, conversationId: String, messageId: String)

    @Query("DELETE FROM chat_message WHERE accountId = :accountId AND conversationId = :conversationId")
    suspend fun deleteConversationMessages(accountId: String, conversationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPending(item: PendingOperationEntity)

    @Query("SELECT * FROM chat_pending_operation WHERE accountId = :accountId AND clientId = :clientId")
    suspend fun pending(accountId: String, clientId: String): PendingOperationEntity?

    @Query("SELECT * FROM chat_pending_operation WHERE accountId = :accountId ORDER BY createdAtEpochMillis ASC")
    suspend fun pendingForAccount(accountId: String): List<PendingOperationEntity>

    @Query("DELETE FROM chat_pending_operation WHERE accountId = :accountId AND clientId = :clientId")
    suspend fun deletePending(accountId: String, clientId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransfer(item: TransferTaskEntity)

    @Query("SELECT * FROM chat_transfer WHERE accountId = :accountId AND clientId = :clientId LIMIT 1")
    suspend fun transfer(accountId: String, clientId: String): TransferTaskEntity?

    @Query("SELECT * FROM chat_transfer WHERE accountId = :accountId AND state IN ('QUEUED', 'UPLOADING', 'FAILED') ORDER BY createdAtEpochMillis ASC")
    suspend fun pendingTransfers(accountId: String): List<TransferTaskEntity>

    @Query("SELECT * FROM chat_download_task WHERE accountId = :accountId AND conversationId = :conversationId ORDER BY updatedAtEpochMillis DESC")
    fun observeDownloads(accountId: String, conversationId: String): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM chat_download_task WHERE accountId = :accountId AND messageId = :messageId LIMIT 1")
    suspend fun download(accountId: String, messageId: String): DownloadTaskEntity?

    @Query("SELECT * FROM chat_download_task WHERE accountId = :accountId AND state IN ('QUEUED', 'DOWNLOADING') ORDER BY updatedAtEpochMillis ASC")
    suspend fun recoverableDownloads(accountId: String): List<DownloadTaskEntity>

    @Query("SELECT * FROM chat_download_task WHERE accountId = :accountId ORDER BY updatedAtEpochMillis ASC")
    suspend fun downloadsForAccount(accountId: String): List<DownloadTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(item: DownloadTaskEntity)

    @Query("UPDATE chat_download_task SET receivedBytes = CASE WHEN :receivedBytes > receivedBytes THEN :receivedBytes ELSE receivedBytes END, totalBytes = CASE WHEN :totalBytes > 0 THEN :totalBytes ELSE totalBytes END, state = 'DOWNLOADING', updatedAtEpochMillis = :updatedAt WHERE accountId = :accountId AND messageId = :messageId AND state IN ('QUEUED', 'DOWNLOADING')")
    suspend fun advanceDownload(accountId: String, messageId: String, receivedBytes: Long, totalBytes: Long, updatedAt: Long)

    @Query("DELETE FROM chat_download_task WHERE accountId = :accountId")
    suspend fun deleteDownloads(accountId: String)

    @Query("UPDATE chat_transfer SET progress = :progress, state = :state WHERE accountId = :accountId AND clientId = :clientId")
    suspend fun updateTransfer(accountId: String, clientId: String, progress: Float, state: String)

    @Query("UPDATE chat_message SET transferProgress = :progress, transferState = :state WHERE accountId = :accountId AND clientId = :clientId")
    suspend fun updateMessageTransfer(accountId: String, clientId: String, progress: Float, state: String)

    @Query(
        """
        UPDATE chat_transfer
        SET progress = CASE WHEN :progress > progress THEN :progress ELSE progress END,
            state = 'UPLOADING'
        WHERE accountId = :accountId AND clientId = :clientId
          AND state IN ('QUEUED', 'UPLOADING')
        """,
    )
    suspend fun advanceTransferProgress(accountId: String, clientId: String, progress: Float)

    @Query(
        """
        UPDATE chat_message
        SET transferProgress = CASE WHEN :progress > transferProgress THEN :progress ELSE transferProgress END,
            transferState = 'UPLOADING'
        WHERE accountId = :accountId AND clientId = :clientId
          AND transferState IN ('QUEUED', 'UPLOADING')
        """,
    )
    suspend fun advanceMessageTransferProgress(accountId: String, clientId: String, progress: Float)

    @Query("DELETE FROM chat_transfer WHERE accountId = :accountId AND clientId = :clientId")
    suspend fun deleteTransfer(accountId: String, clientId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReactions(items: List<ReactionEntity>)

    @Query("DELETE FROM chat_reaction WHERE accountId = :accountId AND messageId = :messageId")
    suspend fun deleteMessageReactions(accountId: String, messageId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTombstone(item: TombstoneEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM chat_tombstone WHERE accountId = :accountId AND conversationId = :conversationId AND messageId = :messageId)")
    suspend fun isTombstoned(accountId: String, conversationId: String, messageId: String): Boolean

    @Query("SELECT MAX(createdAtEpochMillis) FROM chat_tombstone WHERE accountId = :accountId AND conversationId = :conversationId")
    suspend fun latestTombstoneAt(accountId: String, conversationId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRemoteKey(key: RemoteKeyEntity)

    @Query("SELECT * FROM chat_remote_key WHERE accountId = :accountId AND scope = :scope")
    suspend fun remoteKey(accountId: String, scope: String): RemoteKeyEntity?

    @Query("DELETE FROM chat_conversation WHERE accountId = :accountId")
    suspend fun deleteConversations(accountId: String)

    @Query("DELETE FROM chat_conversation WHERE accountId = :accountId AND id = :conversationId")
    suspend fun deleteConversation(accountId: String, conversationId: String)

    @Transaction
    suspend fun deleteConversationWithMessages(accountId: String, conversationId: String) {
        deleteConversationMessages(accountId, conversationId)
        deleteConversation(accountId, conversationId)
    }

    @Query("DELETE FROM chat_message WHERE accountId = :accountId")
    suspend fun deleteMessages(accountId: String)

    @Query("DELETE FROM chat_pending_operation WHERE accountId = :accountId")
    suspend fun deletePending(accountId: String)

    @Query("DELETE FROM chat_remote_key WHERE accountId = :accountId")
    suspend fun deleteRemoteKeys(accountId: String)

    @Query("DELETE FROM chat_participant WHERE accountId = :accountId")
    suspend fun deleteParticipants(accountId: String)

    @Query("DELETE FROM chat_reaction WHERE accountId = :accountId")
    suspend fun deleteReactions(accountId: String)

    @Query("DELETE FROM chat_draft WHERE accountId = :accountId")
    suspend fun deleteDrafts(accountId: String)

    @Query("DELETE FROM chat_transfer WHERE accountId = :accountId")
    suspend fun deleteTransfers(accountId: String)

    @Query("DELETE FROM chat_tombstone WHERE accountId = :accountId")
    suspend fun deleteTombstones(accountId: String)

    @Query(
        """
        DELETE FROM chat_message
        WHERE accountId = :accountId AND createdAtEpochMillis < :cutoffEpochMillis
          AND status NOT IN ('PENDING', 'FAILED')
        """,
    )
    suspend fun trimMessages(accountId: String, cutoffEpochMillis: Long)

    @Transaction
    suspend fun replaceFirstConversationPage(
        accountId: String,
        conversations: List<ConversationEntity>,
        key: RemoteKeyEntity,
    ) {
        deleteConversations(accountId)
        upsertConversations(conversations)
        upsertRemoteKey(key)
    }

    @Transaction
    suspend fun clearAccount(accountId: String) {
        deleteProfileNotes(accountId)
        deleteTransfers(accountId)
        deleteDownloads(accountId)
        deleteDrafts(accountId)
        deleteReactions(accountId)
        deleteParticipants(accountId)
        deleteTombstones(accountId)
        deletePending(accountId)
        deleteMessages(accountId)
        deleteConversations(accountId)
        deleteRemoteKeys(accountId)
    }

    @Transaction
    suspend fun mergeMessageAndConversation(
        message: MessageEntity,
        replacedClientId: String?,
        fallbackConversation: ConversationEntity,
        previewCiphertext: ByteArray,
        incrementUnread: Boolean,
        mutationAtEpochMillis: Long,
    ) {
        if (replacedClientId != null && replacedClientId != message.clientId) {
            deleteMessage(message.accountId, message.conversationId, replacedClientId)
        }
        upsertMessage(message)
        val current = conversation(message.accountId, message.conversationId)
        val base = current ?: fallbackConversation
        val replacePreview = base.lastMessageAtEpochMillis == null ||
            message.createdAtEpochMillis >= base.lastMessageAtEpochMillis
        upsertConversations(
            listOf(
                base.copy(
                    lastMessageCiphertext = if (replacePreview) previewCiphertext else base.lastMessageCiphertext,
                    lastMessageAtEpochMillis = if (replacePreview) {
                        message.createdAtEpochMillis
                    } else {
                        base.lastMessageAtEpochMillis
                    },
                    unreadCount = base.unreadCount + if (incrementUnread) 1 else 0,
                    lastSyncedAtEpochMillis = mutationAtEpochMillis,
                ),
            ),
        )
    }
}

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ParticipantEntity::class,
        ReactionEntity::class,
        DraftEntity::class,
        TransferTaskEntity::class,
        DownloadTaskEntity::class,
        TombstoneEntity::class,
        PendingOperationEntity::class,
        RemoteKeyEntity::class,
        ProfileNoteEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        const val NAME = "vista_chat.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_message ADD COLUMN messageType TEXT NOT NULL DEFAULT 'text'")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN mediaUrl TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN localUri TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN attachmentFileName TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN attachmentMimeType TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN attachmentSizeBytes INTEGER")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN durationSeconds INTEGER")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN audioTitle TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN audioArtist TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN audioAlbum TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN mediaGroupId TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN transferState TEXT NOT NULL DEFAULT 'COMPLETE'")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN transferProgress REAL NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN replyToSenderName TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN replyToKind TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN reactionsJson TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN isForwarded INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN forwardedFromSenderName TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN originalSenderId TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN originalMessageId TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_participant (accountId TEXT NOT NULL, conversationId TEXT NOT NULL, userId TEXT NOT NULL, displayName TEXT, avatarUrl TEXT, role TEXT, joinedAtEpochMillis INTEGER, PRIMARY KEY(accountId, conversationId, userId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_participant_accountId_conversationId ON chat_participant(accountId, conversationId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_reaction (accountId TEXT NOT NULL, conversationId TEXT NOT NULL, messageId TEXT NOT NULL, userId TEXT NOT NULL, emoji TEXT NOT NULL, PRIMARY KEY(accountId, messageId, userId, emoji))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_reaction_accountId_conversationId_messageId ON chat_reaction(accountId, conversationId, messageId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_draft (accountId TEXT NOT NULL, conversationId TEXT NOT NULL, contentCiphertext BLOB NOT NULL, replyToMessageId TEXT, updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(accountId, conversationId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_transfer (accountId TEXT NOT NULL, conversationId TEXT NOT NULL, clientId TEXT NOT NULL, localUri TEXT NOT NULL, objectKey TEXT NOT NULL, mimeType TEXT NOT NULL, fileName TEXT NOT NULL, sizeBytes INTEGER NOT NULL, kind TEXT NOT NULL, durationSeconds INTEGER, captionCiphertext BLOB NOT NULL, progress REAL NOT NULL, state TEXT NOT NULL, attempts INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(accountId, clientId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_transfer_accountId_conversationId_createdAtEpochMillis ON chat_transfer(accountId, conversationId, createdAtEpochMillis)")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_tombstone (accountId TEXT NOT NULL, conversationId TEXT NOT NULL, messageId TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(accountId, conversationId, messageId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_tombstone_accountId_createdAtEpochMillis ON chat_tombstone(accountId, createdAtEpochMillis)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_profile_note (accountId TEXT NOT NULL, userId TEXT NOT NULL, id TEXT NOT NULL, contentCiphertext BLOB NOT NULL, createdAtEpochMillis INTEGER NOT NULL, expiresAtEpochMillis INTEGER NOT NULL, isMine INTEGER NOT NULL, PRIMARY KEY(accountId, userId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_profile_note_accountId_expiresAtEpochMillis ON chat_profile_note(accountId, expiresAtEpochMillis)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_download_task (accountId TEXT NOT NULL, conversationId TEXT NOT NULL, messageId TEXT NOT NULL, remoteUrlCiphertext BLOB NOT NULL, fileNameCiphertext BLOB NOT NULL, mimeType TEXT, localPath TEXT, receivedBytes INTEGER NOT NULL, totalBytes INTEGER NOT NULL, eTag TEXT, state TEXT NOT NULL, attempts INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(accountId, messageId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_download_task_accountId_conversationId_updatedAtEpochMillis ON chat_download_task(accountId, conversationId, updatedAtEpochMillis)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_transfer ADD COLUMN mediaGroupId TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_conversation ADD COLUMN isMessageRequest INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_conversation ADD COLUMN lastMessageType TEXT")
                db.execSQL("ALTER TABLE chat_conversation ADD COLUMN lastMessageIsMine INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_conversation ADD COLUMN lastMessageStatus TEXT NOT NULL DEFAULT 'SENT'")
                // v4 did not persist the independent server flag. Preserve the
                // only unambiguous legacy state so pending requests remain
                // available offline immediately after an in-place upgrade.
                db.execSQL("UPDATE chat_conversation SET isMessageRequest = 1 WHERE LOWER(requestStatus) = 'pending'")
            }
        }
    }
}
