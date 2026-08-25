package ir.coffevista.vista_native.features.profile.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FollowUserDto(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("verification_type") val verificationType: String? = null,
    @SerialName("is_following") val isFollowing: Boolean = false,
    @SerialName("is_follower") val isFollower: Boolean = false,
    val bio: String? = null,
)

@Serializable
data class FollowListResponseDto(
    val users: List<FollowUserDto> = emptyList(),
    val total: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
)
