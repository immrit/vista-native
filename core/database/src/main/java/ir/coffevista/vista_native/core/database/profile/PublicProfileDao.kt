package ir.coffevista.vista_native.core.database.profile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicProfileDao {
    @Query(
        """
        SELECT * FROM public_profile
        WHERE viewer_account_id = :viewerAccountId
          AND profile_user_id = :profileUserId
        """,
    )
    fun observe(
        viewerAccountId: String,
        profileUserId: String,
    ): Flow<PublicProfileEntity?>

    @Query(
        """
        SELECT * FROM public_profile
        WHERE viewer_account_id = :viewerAccountId
          AND profile_user_id = :profileUserId
        """,
    )
    suspend fun get(
        viewerAccountId: String,
        profileUserId: String,
    ): PublicProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: PublicProfileEntity)

    @Query("DELETE FROM public_profile WHERE viewer_account_id = :viewerAccountId")
    suspend fun clearViewer(viewerAccountId: String)

    @Query("DELETE FROM public_profile")
    suspend fun clearAll()
}
