package ir.coffevista.vista_native.features.profile.data

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("username") val username: String? = null,
    @SerialName("full_name") val fullName: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("post_count") val postCount: Long = 0,
    @SerialName("follower_count") val followerCount: Long = 0,
    @SerialName("following_count") val followingCount: Long = 0,
    @SerialName("updated_at") val updatedAt: String? = null
)
