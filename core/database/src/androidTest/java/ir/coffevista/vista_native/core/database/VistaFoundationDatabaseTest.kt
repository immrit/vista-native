package ir.coffevista.vista_native.core.database

import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.coffevista.vista_native.core.database.feed.FeedPageStateEntity
import ir.coffevista.vista_native.core.database.feed.FeedPostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VistaFoundationDatabaseTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        VistaFoundationDatabase::class.java,
    )

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun cleanUp() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun freshSchemaPersistsOnlyVerifiedTlsPolicy() = runBlocking(Dispatchers.IO) {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            VistaFoundationDatabase::class.java,
        ).build()
        val entity = policy(revision = 7)

        database.verifiedTlsPolicyDao().upsert(entity)

        assertEquals(entity, database.verifiedTlsPolicyDao().read())
        database.close()
    }

    @Test
    fun migrationFromSinglePinSchemaPreservesPolicyAsCurrentPin() {
        migrationHelper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS verified_tls_policy (
                    singleton_id INTEGER NOT NULL,
                    revision INTEGER NOT NULL,
                    mode TEXT NOT NULL,
                    fingerprint_sha256 TEXT,
                    expires_at_epoch_seconds INTEGER,
                    PRIMARY KEY(singleton_id)
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO verified_tls_policy
                VALUES (1, 3, 'MONITOR', '${"a".repeat(64)}', 1900000000)
                """.trimIndent(),
            )
            close()
        }

        migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            VistaFoundationDatabase.MIGRATION_1_2,
        ).use { database ->
            database.query(
                "SELECT contract_version, revision, current_pin_sha256, next_pin_sha256 " +
                    "FROM verified_tls_policy",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
                assertEquals(3L, cursor.getLong(1))
                assertEquals("a".repeat(64), cursor.getString(2))
                assertNull(cursor.getString(3))
            }
        }
    }

    @Test
    fun migrationFromV2ToV3CreatesProfileTable() {
        migrationHelper.createDatabase(TEST_DATABASE, 2).apply {
            close()
        }

        migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            VistaFoundationDatabase.MIGRATION_1_2,
            VistaFoundationDatabase.MIGRATION_2_3
        ).use { database ->
            database.query(
                "SELECT user_id, full_name, is_verified, post_count, follower_count, following_count " +
                    "FROM own_profile",
            ).use { cursor ->
                assertEquals(0, cursor.count) // Table is created and empty
            }
        }
    }

    @Test
    fun failedTransactionRollsBackClearAndInsert() = runBlocking(Dispatchers.IO) {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            VistaFoundationDatabase::class.java,
        ).build()
        val dao = database.verifiedTlsPolicyDao()
        dao.upsert(policy(revision = 1))

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                dao.replaceAtomically(
                    policy(revision = 2),
                    failAfterWriteForTest = true,
                )
            }
        }

        assertEquals(1L, dao.read()?.revision)
        database.close()
    }

    @Test
    fun corruptSchemaIsNotDestructivelyRecreated() {
        migrationHelper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL("DROP TABLE verified_tls_policy")
            close()
        }
        val database = Room.databaseBuilder(
            context,
            VistaFoundationDatabase::class.java,
            TEST_DATABASE,
        )
            .addMigrations(VistaFoundationDatabase.MIGRATION_1_2, VistaFoundationDatabase.MIGRATION_2_3)
            .addMigrations(VistaFoundationDatabase.MIGRATION_3_4)
            .build()

        val failure = assertThrows(RuntimeException::class.java) {
            runBlocking(Dispatchers.IO) {
                database.verifiedTlsPolicyDao().read()
            }
        }
        assertTrue(failure is SQLiteException || failure is IllegalStateException)
        database.close()
    }

    @Test
    fun downgradeIsRejectedInsteadOfDestructiveFallback() {
        migrationHelper.createDatabase(TEST_DATABASE, 2).apply {
            version = 3
            close()
        }
        val database = Room.databaseBuilder(
            context,
            VistaFoundationDatabase::class.java,
            TEST_DATABASE,
        )
            .addMigrations(VistaFoundationDatabase.MIGRATION_1_2)
            .build()

        assertThrows(IllegalStateException::class.java) {
            database.openHelper.writableDatabase
        }
        database.close()
    }

    @Test
    fun feedDaoReadsPostsInStoredFeedOrder() = runBlocking(Dispatchers.IO) {
        withDatabase { database ->
            database.feedDao().insertPosts(
                listOf(
                    feedPost("account-a", "third", sortOrder = 2),
                    feedPost("account-a", "first", sortOrder = 0),
                    feedPost("account-a", "second", sortOrder = 1),
                ),
            )

            assertEquals(
                listOf("first", "second", "third"),
                database.feedDao().observeFeed("account-a").first().map { it.id },
            )
        }
    }

    @Test
    fun feedDaoReplaceFirstPageIsAtomicAndRemovesOldRows() = runBlocking(Dispatchers.IO) {
        withDatabase { database ->
            val dao = database.feedDao()
            dao.replaceFirstPage(
                accountId = "account-a",
                posts = listOf(feedPost("account-a", "old", sortOrder = 0)),
                state = feedState("account-a", nextOffset = 1, hasMore = true),
            )

            dao.replaceFirstPage(
                accountId = "account-a",
                posts = listOf(feedPost("account-a", "fresh", sortOrder = 0)),
                state = feedState("account-a", nextOffset = 1, hasMore = false),
            )

            assertEquals(
                listOf("fresh"),
                dao.observeFeed("account-a").first().map { it.id },
            )
            assertEquals(false, dao.getPageState("account-a")?.hasMore)
        }
    }

    @Test
    fun feedDaoAppendPreservesRowsAndUpdatesCursorMetadata() = runBlocking(Dispatchers.IO) {
        withDatabase { database ->
            val dao = database.feedDao()
            dao.replaceFirstPage(
                accountId = "account-a",
                posts = listOf(feedPost("account-a", "first", sortOrder = 0)),
                state = feedState("account-a", nextOffset = 1, hasMore = true),
            )

            dao.appendPage(
                posts = listOf(feedPost("account-a", "second", sortOrder = 1)),
                state = feedState(
                    "account-a",
                    nextOffset = 2,
                    hasMore = false,
                    nextCursor = "2026-07-27T17:21:49.123456789Z",
                ),
            )

            assertEquals(
                listOf("first", "second"),
                dao.observeFeed("account-a").first().map { it.id },
            )
            assertEquals(2, dao.getPageState("account-a")?.nextOffset)
            assertEquals(
                "2026-07-27T17:21:49.123456789Z",
                dao.getPageState("account-a")?.nextCursor,
            )
        }
    }

    @Test
    fun feedDaoPrimaryKeyDeduplicatesPerAccountAndPostId() = runBlocking(Dispatchers.IO) {
        withDatabase { database ->
            val dao = database.feedDao()
            dao.insertPosts(listOf(feedPost("account-a", "same", content = "old")))
            dao.insertPosts(listOf(feedPost("account-a", "same", content = "new")))

            val rows = dao.observeFeed("account-a").first()
            assertEquals(1, rows.size)
            assertEquals("new", rows.single().content)
        }
    }

    @Test
    fun feedDaoKeepsAccountsIsolated() = runBlocking(Dispatchers.IO) {
        withDatabase { database ->
            val dao = database.feedDao()
            dao.insertPosts(
                listOf(
                    feedPost("account-a", "same", content = "a"),
                    feedPost("account-b", "same", content = "b"),
                ),
            )

            assertEquals("a", dao.observeFeed("account-a").first().single().content)
            assertEquals("b", dao.observeFeed("account-b").first().single().content)
        }
    }

    @Test
    fun feedDaoClearAccountRemovesPostsAndMetadataOnlyForThatAccount() =
        runBlocking(Dispatchers.IO) {
            withDatabase { database ->
                val dao = database.feedDao()
                dao.insertPosts(
                    listOf(
                        feedPost("account-a", "a"),
                        feedPost("account-b", "b"),
                    ),
                )
                dao.upsertPageState(feedState("account-a", 1, true))
                dao.upsertPageState(feedState("account-b", 1, true))

                dao.clearAccount("account-a")

                assertTrue(dao.observeFeed("account-a").first().isEmpty())
                assertNull(dao.getPageState("account-a"))
                assertEquals(listOf("b"), dao.observeFeed("account-b").first().map { it.id })
                assertEquals(1, dao.getPageState("account-b")?.nextOffset)
            }
        }

    @Test
    fun migrationFromV3ToV4CreatesFeedTablesAndMetadata() {
        migrationHelper.createDatabase(TEST_DATABASE, 3).apply { close() }

        migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            VistaFoundationDatabase.MIGRATION_3_4,
        ).use { database ->
            database.query("SELECT COUNT(*) FROM feed_post").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            database.query("SELECT COUNT(*) FROM feed_page_state").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private fun policy(revision: Long) = VerifiedTlsPolicyEntity(
        contractVersion = 1,
        revision = revision,
        mode = "MONITOR",
        currentPinSha256 = "a".repeat(64),
        nextPinSha256 = "b".repeat(64),
        expiresAtEpochSeconds = 1_900_000_000,
    )

    private fun feedPost(
        accountId: String,
        id: String,
        sortOrder: Long = 0,
        content: String = "caption",
    ) = FeedPostEntity(
        accountId = accountId,
        id = id,
        userId = "author",
        content = content,
        imageUrl = null,
        imageUrls = emptyList(),
        videoUrl = null,
        musicUrl = null,
        aspectRatio = null,
        musicTitle = null,
        tags = emptyList(),
        likeCount = 0,
        commentCount = 0,
        isLiked = false,
        isSaved = false,
        hideLikeCount = false,
        hideCommentCount = false,
        authorId = "author",
        authorUsername = "author",
        authorFullName = "Author",
        authorAvatarUrl = null,
        authorIsVerified = false,
        authorVerificationType = null,
        createdAt = "2026-07-27T17:21:49Z",
        updatedAt = "2026-07-27T17:21:49Z",
        sortOrder = sortOrder,
    )

    private fun feedState(
        accountId: String,
        nextOffset: Int,
        hasMore: Boolean,
        nextCursor: String? = null,
    ) = FeedPageStateEntity(
        accountId = accountId,
        nextOffset = nextOffset,
        hasMore = hasMore,
        nextCursor = nextCursor,
        lastRefreshEpochMillis = 123,
    )

    private suspend fun withDatabase(
        block: suspend (VistaFoundationDatabase) -> Unit,
    ) {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            VistaFoundationDatabase::class.java,
        ).build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "fnd-room-test.db"
    }
}
