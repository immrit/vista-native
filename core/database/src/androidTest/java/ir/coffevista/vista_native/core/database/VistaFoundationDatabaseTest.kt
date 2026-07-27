package ir.coffevista.vista_native.core.database

import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
            .addMigrations(VistaFoundationDatabase.MIGRATION_1_2)
            .build()

        assertThrows(SQLiteException::class.java) {
            runBlocking(Dispatchers.IO) {
                database.verifiedTlsPolicyDao().read()
            }
        }
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

    private fun policy(revision: Long) = VerifiedTlsPolicyEntity(
        contractVersion = 1,
        revision = revision,
        mode = "MONITOR",
        currentPinSha256 = "a".repeat(64),
        nextPinSha256 = "b".repeat(64),
        expiresAtEpochSeconds = 1_900_000_000,
    )

    private companion object {
        const val TEST_DATABASE = "fnd-room-test.db"
    }
}
