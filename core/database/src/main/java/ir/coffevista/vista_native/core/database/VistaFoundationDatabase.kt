package ir.coffevista.vista_native.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity

@Database(
    entities = [
        VerifiedTlsPolicyEntity::class,
        OwnProfileEntity::class
    ],
    version = 3,
    exportSchema = true,
)
abstract class VistaFoundationDatabase : RoomDatabase() {
    abstract fun verifiedTlsPolicyDao(): VerifiedTlsPolicyDao
    abstract fun ownProfileDao(): OwnProfileDao

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
    }
}
