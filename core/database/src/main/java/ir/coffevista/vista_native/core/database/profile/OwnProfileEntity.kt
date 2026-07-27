package ir.coffevista.vista_native.core.database.profile

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "own_profile")
data class OwnProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "username")
    val username: String?,
    
    @ColumnInfo(name = "full_name")
    val fullName: String,
    
    @ColumnInfo(name = "bio")
    val bio: String?,
    
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean,
    
    @ColumnInfo(name = "account_type")
    val accountType: String?,
    
    @ColumnInfo(name = "post_count")
    val postCount: Long,
    
    @ColumnInfo(name = "follower_count")
    val followerCount: Long,
    
    @ColumnInfo(name = "following_count")
    val followingCount: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String?
)
