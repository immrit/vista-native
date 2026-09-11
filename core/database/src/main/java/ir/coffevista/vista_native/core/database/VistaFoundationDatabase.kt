package ir.coffevista.vista_native.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import ir.coffevista.vista_native.core.database.profile.PublicProfileDao
import ir.coffevista.vista_native.core.database.profile.PublicProfileEntity
import ir.coffevista.vista_native.core.database.feed.FeedPostEntity
import ir.coffevista.vista_native.core.database.feed.FeedPageStateEntity
import ir.coffevista.vista_native.core.database.feed.FeedDao
import ir.coffevista.vista_native.core.database.feed.FeedConverters
import androidx.room.TypeConverters
import ir.coffevista.vista_native.core.database.search.SearchHistoryDao
import ir.coffevista.vista_native.core.database.search.SearchHistoryEntity

@Database(
    entities = [
        VerifiedTlsPolicyEntity::class,
        OwnProfileEntity::class,
        FeedPostEntity::class,
        FeedPageStateEntity::class,
        PublicProfileEntity::class,
        SearchHistoryEntity::class,
        ir.coffevista.vista_native.core.database.chat.ConversationEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
@TypeConverters(
    FeedConverters::class,
    ir.coffevista.vista_native.core.database.util.InstantConverter::class
)
abstract class VistaFoundationDatabase : RoomDatabase() {
    abstract fun verifiedTlsPolicyDao(): VerifiedTlsPolicyDao
    abstract fun ownProfileDao(): OwnProfileDao
    abstract fun feedDao(): FeedDao
    abstract fun publicProfileDao(): PublicProfileDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun conversationDao(): ir.coffevista.vista_native.core.database.chat.ConversationDao

    companion object {
        const val DATABASE_NAME = "vista_foundation.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS verified_tls_policy_new (
                        singleton_id INTEGER NOT NULL,
                        contract_version INTEGER NOT NULL,
                        revision INTEGER NOT NULL,
                        mode TEXT NOT NULL,
                        current_pin_sha256 TEXT,
                        next_pin_sha256 TEXT,
                        expires_at_epoch_seconds INTEGER,
                        PRIMARY KEY(singleton_id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO verified_tls_policy_new (
                        singleton_id,
                        contract_version,
                        revision,
                        mode,
                        current_pin_sha256,
                        next_pin_sha256,
                        expires_at_epoch_seconds
                    )
                    SELECT
                        singleton_id,
                        1,
                        revision,
                        mode,
                        fingerprint_sha256,
                        NULL,
                        expires_at_epoch_seconds
                    FROM verified_tls_policy
                    """.trimIndent(),
                )
                database.execSQL("DROP TABLE verified_tls_policy")
                database.execSQL(
                    "ALTER TABLE verified_tls_policy_new RENAME TO verified_tls_policy",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS own_profile (
                        user_id TEXT NOT NULL,
                        username TEXT,
                        full_name TEXT NOT NULL,
                        bio TEXT,
                        avatar_url TEXT,
                        is_verified INTEGER NOT NULL,
                        account_type TEXT,
                        post_count INTEGER NOT NULL,
                        follower_count INTEGER NOT NULL,
                        following_count INTEGER NOT NULL,
                        updated_at TEXT,
                        PRIMARY KEY(user_id)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS feed_post (
                        account_id TEXT NOT NULL,
                        id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        content TEXT,
                        image_url TEXT,
                        image_urls TEXT NOT NULL,
                        video_url TEXT,
                        music_url TEXT,
                        aspect_ratio TEXT,
                        music_title TEXT,
                        tags TEXT NOT NULL,
                        like_count INTEGER NOT NULL,
                        comment_count INTEGER NOT NULL,
                        is_liked INTEGER NOT NULL,
                        is_saved INTEGER NOT NULL,
                        hide_like_count INTEGER NOT NULL,
                        hide_comment_count INTEGER NOT NULL,
                        author_id TEXT NOT NULL,
                        author_username TEXT,
                        author_full_name TEXT NOT NULL,
                        author_avatar_url TEXT,
                        author_is_verified INTEGER NOT NULL,
                        author_verification_type TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        sort_order INTEGER NOT NULL,
                        PRIMARY KEY(account_id, id)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS feed_page_state (
                        account_id TEXT NOT NULL,
                        next_offset INTEGER NOT NULL,
                        has_more INTEGER NOT NULL,
                        next_cursor TEXT,
                        last_refresh_epoch_millis INTEGER NOT NULL,
                        PRIMARY KEY(account_id)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS public_profile (
                        viewer_account_id TEXT NOT NULL,
                        profile_user_id TEXT NOT NULL,
                        username TEXT,
                        full_name TEXT NOT NULL,
                        bio TEXT,
                        avatar_url TEXT,
                        is_verified INTEGER NOT NULL,
                        verification_type TEXT,
                        is_private INTEGER NOT NULL,
                        is_blocked INTEGER NOT NULL,
                        subscription_plan TEXT,
                        premium_days_remaining INTEGER,
                        post_count INTEGER NOT NULL,
                        follower_count INTEGER NOT NULL,
                        following_count INTEGER NOT NULL,
                        follow_status TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        last_synced_epoch_millis INTEGER NOT NULL,
                        PRIMARY KEY(viewer_account_id, profile_user_id)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                addFeedProfileParityColumns(database)
                createSearchHistoryTable(database)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Both feature branches shipped an independent schema v6.
                // Complete whichever shape is installed, while remaining safe
                // for a database that already contains both feature sets.
                addFeedProfileParityColumns(database)
                createSearchHistoryTable(database)
            }
        }

        private fun addFeedProfileParityColumns(database: SupportSQLiteDatabase) {
            database.addColumnIfMissing("feed_post", "author_follow_status", "TEXT")
            database.addColumnIfMissing("feed_post", "feed_source", "TEXT")
            database.addColumnIfMissing("own_profile", "verification_type", "TEXT")
            database.addColumnIfMissing(
                "own_profile",
                "is_private",
                "INTEGER NOT NULL DEFAULT 0",
            )
            database.addColumnIfMissing(
                "own_profile",
                "join_order",
                "INTEGER NOT NULL DEFAULT 0",
            )
            database.addColumnIfMissing("own_profile", "subscription_plan", "TEXT")
            database.addColumnIfMissing(
                "own_profile",
                "premium_days_remaining",
                "INTEGER",
            )
            database.addColumnIfMissing(
                "own_profile",
                "message_privacy",
                "TEXT NOT NULL DEFAULT 'everyone'",
            )
            database.addColumnIfMissing(
                "own_profile",
                "allow_profile_zoom",
                "INTEGER NOT NULL DEFAULT 1",
            )
            database.addColumnIfMissing(
                "public_profile",
                "join_order",
                "INTEGER NOT NULL DEFAULT 0",
            )
            database.addColumnIfMissing(
                "public_profile",
                "message_privacy",
                "TEXT NOT NULL DEFAULT 'everyone'",
            )
            database.addColumnIfMissing(
                "public_profile",
                "allow_profile_zoom",
                "INTEGER NOT NULL DEFAULT 1",
            )
        }

        private fun createSearchHistoryTable(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS search_history (
                    account_id TEXT NOT NULL,
                    query TEXT NOT NULL,
                    search_type TEXT NOT NULL,
                    timestamp_epoch_millis INTEGER NOT NULL,
                    PRIMARY KEY(account_id, query)
                )
                """.trimIndent(),
            )
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conversations` (
                        `id` TEXT NOT NULL, 
                        `created_at` INTEGER NOT NULL, 
                        `updated_at` INTEGER NOT NULL, 
                        `last_message` TEXT, 
                        `last_message_time` INTEGER, 
                        `unread_count` INTEGER NOT NULL, 
                        `has_unread_messages` INTEGER NOT NULL, 
                        `is_pinned` INTEGER NOT NULL, 
                        `is_muted` INTEGER NOT NULL, 
                        `is_archived` INTEGER NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `other_user_name` TEXT, 
                        `other_user_avatar` TEXT, 
                        `other_user_id` TEXT, 
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addColumnIfMissing("own_profile", "email", "TEXT")
                database.addColumnIfMissing("own_profile", "phone_number", "TEXT")
                database.addColumnIfMissing("own_profile", "website_url", "TEXT")
                database.addColumnIfMissing("own_profile", "birth_date", "TEXT")
                database.addColumnIfMissing("own_profile", "gender", "TEXT")
                database.addColumnIfMissing("own_profile", "marital_status", "TEXT")
                database.addColumnIfMissing("own_profile", "show_email", "INTEGER NOT NULL DEFAULT 0")
                database.addColumnIfMissing("own_profile", "show_birth_date", "INTEGER NOT NULL DEFAULT 0")
                database.addColumnIfMissing("own_profile", "show_gender", "INTEGER NOT NULL DEFAULT 0")
                database.addColumnIfMissing("own_profile", "show_marital_status", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addColumnIfMissing("feed_post", "edited_by_vista", "INTEGER NOT NULL DEFAULT 0")
                database.addColumnIfMissing("feed_post", "moderation_reason", "TEXT")
                database.addColumnIfMissing("feed_post", "comments_disabled", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.addColumnIfMissing("own_profile", "created_at", "TEXT")
                database.addColumnIfMissing("public_profile", "created_at", "TEXT")
            }
        }

        private fun SupportSQLiteDatabase.addColumnIfMissing(
            table: String,
            column: String,
            definition: String,
        ) {
            if (hasColumn(table, column)) return
            execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
        }

        private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean =
            query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == column) return@use true
                }
                false
            }
    }
}
