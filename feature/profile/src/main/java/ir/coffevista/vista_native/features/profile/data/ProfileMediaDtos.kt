package ir.coffevista.vista_native.features.profile.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class ProfileAvatarUpdateRequestDto(@SerialName("avatar_url") val avatarUrl: String)
@Serializable data class ProfileMediaPresignRequestDto(@SerialName("object_key") val objectKey: String, @SerialName("content_type") val contentType: String, @SerialName("file_size") val fileSize: Long)
@Serializable data class ProfileMediaPresignResponseDto(val url: String, val method: String = "PUT", val headers: Map<String, String> = emptyMap(), @SerialName("object_key") val objectKey: String, @SerialName("object_url") val objectUrl: String)
@Serializable data class ProfileMediaDeleteRequestDto(@SerialName("object_key") val objectKey: String)
@Serializable data class ProfileMediaDeleteResponseDto(val success: Boolean = false)
