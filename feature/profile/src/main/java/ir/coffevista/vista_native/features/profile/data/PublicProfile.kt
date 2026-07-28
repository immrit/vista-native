package ir.coffevista.vista_native.features.profile.data

import ir.coffevista.vista_native.core.database.profile.PublicProfileEntity
import kotlinx.serialization.SerializationException

enum class FollowState(val wireName: String) {
    NotFollowing("none"),
    Following("following"),
    Requested("requested"),
    Unavailable("unavailable"),
    ;

    companion object {
        fun fromWireName(raw: String?): FollowState = when (raw?.trim()?.lowercase()) {
            null, "", "none" -> NotFollowing
            "following" -> Following
            "requested" -> Requested
            else -> throw SerializationException("Unsupported follow_status: $raw")
        }
    }
}

data class PublicProfile(
    val viewerAccountId: String,
    val userId: String,
    val username: String?,
    val fullName: String,
    val bio: String?,
    val avatarUrl: String?,
    val isVerified: Boolean,
    val verificationType: String?,
    val isPrivate: Boolean,
    val isBlocked: Boolean,
    val isPremium: Boolean,
    val postsCount: Long,
    val followersCount: Long,
    val followingCount: Long,
    val followState: FollowState,
    val updatedAt: String,
    val lastSyncedEpochMillis: Long,
)

internal fun PublicProfileDto.toEntity(
    viewerAccountId: String,
    syncedAtEpochMillis: Long,
): PublicProfileEntity {
    require(userId.isNotBlank()) { "Public profile user_id is blank" }
    require(fullName.isNotBlank()) { "Public profile full_name is blank" }
    require(updatedAt.isNotBlank()) { "Public profile updated_at is blank" }
    require(postCount >= 0 && followerCount >= 0 && followingCount >= 0) {
        "Public profile counts must be non-negative"
    }
    return PublicProfileEntity(
        viewerAccountId = viewerAccountId,
        profileUserId = userId,
        username = username,
        fullName = fullName,
        bio = bio,
        avatarUrl = avatarUrl,
        isVerified = isVerified,
        verificationType = verificationType,
        isPrivate = isPrivate,
        isBlocked = isBlocked,
        subscriptionPlan = subscriptionPlan,
        premiumDaysRemaining = premiumDaysRemaining,
        postCount = postCount,
        followerCount = followerCount,
        followingCount = followingCount,
        followStatus = FollowState.fromWireName(followStatus).wireName,
        updatedAt = updatedAt,
        lastSyncedEpochMillis = syncedAtEpochMillis,
    )
}

internal fun PublicProfileEntity.toDomain(): PublicProfile = PublicProfile(
    viewerAccountId = viewerAccountId,
    userId = profileUserId,
    username = username,
    fullName = fullName,
    bio = bio,
    avatarUrl = avatarUrl,
    isVerified = isVerified,
    verificationType = verificationType,
    isPrivate = isPrivate,
    isBlocked = isBlocked,
    isPremium = premiumDaysRemaining?.let { it > 0 }
        ?: subscriptionPlan?.let { it.isNotBlank() && !it.equals("free", ignoreCase = true) }
        ?: false,
    postsCount = postCount,
    followersCount = followerCount,
    followingCount = followingCount,
    followState = if (isBlocked) FollowState.Unavailable else FollowState.fromWireName(followStatus),
    updatedAt = updatedAt,
    lastSyncedEpochMillis = lastSyncedEpochMillis,
)
