package ir.coffevista.vista_native.features.profile.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire contract of Flutter's authenticated POST /me/profile/update endpoint. */
@Serializable
data class ProfileUpdateRequestDto(
    @SerialName("username") val username: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("bio") val bio: String,
    @SerialName("email") val email: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("website_url") val websiteUrl: String,
    @SerialName("birth_date") val birthDate: String,
    @SerialName("gender") val gender: String,
    @SerialName("marital_status") val maritalStatus: String,
    @SerialName("show_email") val showEmail: Boolean,
    @SerialName("show_birth_date") val showBirthDate: Boolean,
    @SerialName("show_gender") val showGender: Boolean,
    @SerialName("show_marital_status") val showMaritalStatus: Boolean,
)
