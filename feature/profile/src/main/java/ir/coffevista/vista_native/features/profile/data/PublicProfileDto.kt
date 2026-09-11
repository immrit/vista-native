package ir.coffevista.vista_native.features.profile.data

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class PublicProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String? = null,
    @SerialName("full_name") val fullName: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean,
    @SerialName("verification_type") val verificationType: String? = null,
    @SerialName("is_private") val isPrivate: Boolean,
    @SerialName("is_blocked") val isBlocked: Boolean,
    @SerialName("subscription_plan") val subscriptionPlan: String? = null,
    @SerialName("premium_days_remaining") val premiumDaysRemaining: Int? = null,
    @SerialName("post_count") val postCount: Long,
    @SerialName("follower_count") val followerCount: Long,
    @SerialName("following_count") val followingCount: Long,
    @SerialName("follow_status") val followStatus: String? = null,
    @SerialName("join_order") val joinOrder: Long = 0,
    @SerialName("message_privacy") val messagePrivacy: String = "everyone",
    @SerialName("allow_profile_zoom") val allowProfileZoom: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

@Keep
@Serializable
data class FollowActionRequestDto(
    @SerialName("target_user_id") val targetUserId: String,
)

@Keep
@Serializable
data class FollowActionResponseDto(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
)

@Keep
@Serializable
data class UnfollowResponseDto(
    @SerialName("status") val status: String,
)
