package ir.coffevista.vista_native.core.database.search

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query(
        """
        SELECT * FROM search_history
        WHERE account_id = :accountId
        ORDER BY timestamp_epoch_millis DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(accountId: String, limit: Int = DISPLAY_LIMIT): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SearchHistoryEntity)

    @Query(
        """
        DELETE FROM search_history
        WHERE account_id = :accountId
          AND query NOT IN (
              SELECT query FROM search_history
              WHERE account_id = :accountId
              ORDER BY timestamp_epoch_millis DESC
              LIMIT :maxSize
          )
        """,
    )
    suspend fun trimToSize(accountId: String, maxSize: Int)

    @Transaction
    suspend fun addAndTrim(entity: SearchHistoryEntity, maxSize: Int = MAX_STORED) {
        upsert(entity)
        trimToSize(entity.accountId, maxSize)
    }

    @Query("DELETE FROM search_history WHERE account_id = :accountId AND query = :query")
    suspend fun delete(accountId: String, query: String)

    @Query("DELETE FROM search_history WHERE account_id = :accountId")
    suspend fun clearAccount(accountId: String)

    companion object {
        const val DISPLAY_LIMIT = 12
        const val MAX_STORED = 20
    }
}
