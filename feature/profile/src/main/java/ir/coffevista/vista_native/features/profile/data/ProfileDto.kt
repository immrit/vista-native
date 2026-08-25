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
@SerialName("email") val email: String? = null,
@SerialName("phone_number") val phoneNumber: String? = null,
@SerialName("website_url") val websiteUrl: String? = null,
@SerialName("birth_date") val birthDate: String? = null,
@SerialName("gender") val gender: String? = null,
@SerialName("marital_status") val maritalStatus: String? = null,
@SerialName("show_email") val showEmail: Boolean = false,
@SerialName("show_birth_date") val showBirthDate: Boolean = false,
@SerialName("show_gender") val showGender: Boolean = false,
@SerialName("show_marital_status") val showMaritalStatus: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("verification_type") val verificationType: String? = null,
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("post_count") val postCount: Long = 0,
    @SerialName("follower_count") val followerCount: Long = 0,
    @SerialName("following_count") val followingCount: Long = 0,
    @SerialName("join_order") val joinOrder: Long = 0,
    @SerialName("subscription_plan") val subscriptionPlan: String? = null,
    @SerialName("premium_days_remaining") val premiumDaysRemaining: Int? = null,
    @SerialName("message_privacy") val messagePrivacy: String = "everyone",
    @SerialName("allow_profile_zoom") val allowProfileZoom: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null
)
