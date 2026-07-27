package ir.coffevista.vista_native.core.database.profile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnProfileDao {
    @Query("SELECT * FROM own_profile WHERE user_id = :userId")
    fun getOwnProfile(userId: String): Flow<OwnProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: OwnProfileEntity)

    @Query("DELETE FROM own_profile WHERE user_id = :userId")
    suspend fun deleteProfile(userId: String)
    
    @Query("DELETE FROM own_profile")
    suspend fun deleteAll()
}
