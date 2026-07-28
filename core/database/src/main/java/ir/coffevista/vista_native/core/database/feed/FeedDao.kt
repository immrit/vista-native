package ir.coffevista.vista_native.core.database.feed

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feed_post WHERE account_id = :accountId ORDER BY sort_order ASC")
    fun observeFeed(accountId: String): Flow<List<FeedPostEntity>>

    @Query("SELECT * FROM feed_page_state WHERE account_id = :accountId LIMIT 1")
    fun observePageState(accountId: String): Flow<FeedPageStateEntity?>

    @Query("SELECT * FROM feed_page_state WHERE account_id = :accountId LIMIT 1")
    suspend fun getPageState(accountId: String): FeedPageStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<FeedPostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPageState(state: FeedPageStateEntity)

    @Query("DELETE FROM feed_post WHERE account_id = :accountId")
    suspend fun deletePosts(accountId: String)

    @Query("DELETE FROM feed_page_state WHERE account_id = :accountId")
    suspend fun deletePageState(accountId: String)

    @Query("SELECT MAX(sort_order) FROM feed_post WHERE account_id = :accountId")
    suspend fun getMaxSortOrder(accountId: String): Long?

    @Query("SELECT COUNT(*) FROM feed_post WHERE account_id = :accountId")
    suspend fun getPostCount(accountId: String): Int

    @Query("SELECT id FROM feed_post WHERE account_id = :accountId AND id IN (:postIds)")
    suspend fun getExistingPostIds(accountId: String, postIds: List<String>): List<String>

    @Query("SELECT * FROM feed_post WHERE account_id = :accountId AND id = :postId LIMIT 1")
    fun observePostById(accountId: String, postId: String): Flow<FeedPostEntity?>

    @Transaction
    suspend fun replaceFirstPage(
        accountId: String,
        posts: List<FeedPostEntity>,
        state: FeedPageStateEntity,
    ) {
        deletePosts(accountId)
        insertPosts(posts)
        upsertPageState(state)
    }

    @Transaction
    suspend fun appendPage(
        posts: List<FeedPostEntity>,
        state: FeedPageStateEntity,
    ) {
        insertPosts(posts)
        upsertPageState(state)
    }

    @Transaction
    suspend fun clearAccount(accountId: String) {
        deletePosts(accountId)
        deletePageState(accountId)
    }
}
