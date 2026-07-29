package ir.coffevista.vista_native.core.database.profile

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "public_profile",
    primaryKeys = ["viewer_account_id", "profile_user_id"],
)
data class PublicProfileEntity(
    @ColumnInfo(name = "viewer_account_id")
    val viewerAccountId: String,
    @ColumnInfo(name = "profile_user_id")
    val profileUserId: String,
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
    @ColumnInfo(name = "verification_type")
    val verificationType: String?,
    @ColumnInfo(name = "is_private")
    val isPrivate: Boolean,
    @ColumnInfo(name = "is_blocked")
    val isBlocked: Boolean,
    @ColumnInfo(name = "subscription_plan")
    val subscriptionPlan: String?,
    @ColumnInfo(name = "premium_days_remaining")
    val premiumDaysRemaining: Int?,
    @ColumnInfo(name = "post_count")
    val postCount: Long,
    @ColumnInfo(name = "follower_count")
    val followerCount: Long,
    @ColumnInfo(name = "following_count")
    val followingCount: Long,
    @ColumnInfo(name = "follow_status")
    val followStatus: String,
    @ColumnInfo(name = "join_order")
    val joinOrder: Long = 0,
    @ColumnInfo(name = "message_privacy")
    val messagePrivacy: String = "everyone",
    @ColumnInfo(name = "allow_profile_zoom")
    val allowProfileZoom: Boolean = true,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "last_synced_epoch_millis")
    val lastSyncedEpochMillis: Long,
)
