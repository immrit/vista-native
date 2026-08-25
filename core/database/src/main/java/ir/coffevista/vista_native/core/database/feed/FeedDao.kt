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

    @Query(
        "DELETE FROM feed_post WHERE account_id = :accountId " +
            "OR substr(account_id, 1, length(:namespacePrefix)) = :namespacePrefix",
    )
    suspend fun deletePostsAndNamespaces(accountId: String, namespacePrefix: String)

    @Query(
        "DELETE FROM feed_page_state WHERE account_id = :accountId " +
            "OR substr(account_id, 1, length(:namespacePrefix)) = :namespacePrefix",
    )
    suspend fun deletePageStatesAndNamespaces(accountId: String, namespacePrefix: String)

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

    @Transaction
    suspend fun clearAccountNamespaces(accountId: String, namespacePrefix: String) {
        deletePostsAndNamespaces(accountId, namespacePrefix)
        deletePageStatesAndNamespaces(accountId, namespacePrefix)
    }

    @Query("UPDATE feed_post SET is_liked = :isLiked, like_count = :likeCount WHERE id = :postId")
    suspend fun updateLikeState(postId: String, isLiked: Boolean, likeCount: Long)

    @Query("UPDATE feed_post SET is_saved = :isSaved WHERE id = :postId")
    suspend fun updateSaveState(postId: String, isSaved: Boolean)

    @Query("UPDATE feed_post SET comment_count = comment_count + :delta WHERE id = :postId")
    suspend fun updateCommentCount(postId: String, delta: Long)

    @Query("UPDATE feed_post SET content = COALESCE(:content, content), hide_like_count = COALESCE(:hideLikeCount, hide_like_count), hide_comment_count = COALESCE(:hideCommentCount, hide_comment_count) WHERE id = :postId")
    suspend fun updatePostFields(postId: String, content: String?, hideLikeCount: Boolean?, hideCommentCount: Boolean?)

    @Query("DELETE FROM feed_post WHERE id = :postId")
    suspend fun deletePostEverywhere(postId: String)
}
