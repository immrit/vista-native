package ir.coffevista.vista_native.features.search.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchProfilesResponseDto(
    @SerialName("profiles")
    val profiles: List<SearchUserDto> = emptyList(),
)

@Serializable
data class SearchUserDto(
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("username")
    val username: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
)

@Serializable
data class ExactProfileDto(
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("username")
    val username: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    @SerialName("verification_type")
    val verificationType: String? = null,
    @SerialName("role")
    val role: String? = null,
)

@Serializable
data class SearchPostsResponseDto(
    @SerialName("posts")
    val posts: List<SearchPostDto> = emptyList(),
    @SerialName("has_more")
    val hasMore: Boolean = false,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
)

@Serializable
data class SearchPostDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("video_url")
    val videoUrl: String? = null,
)

@Serializable
data class HashtagResponseDto(
    @SerialName("hashtags")
    val hashtags: List<HashtagSuggestionDto> = emptyList(),
)

@Serializable
data class HashtagSuggestionDto(
    @SerialName("tag")
    val tag: String? = null,
    @SerialName("usage_count")
    val usageCount: Int = 0,
)

internal fun SearchUserDto.toDomainOrNull(): SearchUser? {
    val id = userId?.trim().orEmpty()
    if (id.isEmpty()) return null
    val normalizedFullName = fullName?.trim().orEmpty()
    val normalizedUsername = username?.trim().orEmpty()
        .ifEmpty { normalizedFullName }
    if (normalizedUsername.isEmpty()) return null
    return SearchUser(
        id = id,
        username = normalizedUsername,
        fullName = normalizedFullName.ifEmpty { normalizedUsername },
        avatarUrl = avatarUrl?.trim()?.takeIf(String::isNotEmpty),
        isVerified = false,
        verificationType = null,
        role = null,
    )
}

internal fun ExactProfileDto.toDomainOrNull(): SearchUser? {
    val id = userId?.trim().orEmpty()
    if (id.isEmpty()) return null
    val normalizedFullName = fullName?.trim().orEmpty()
    val normalizedUsername = username?.trim().orEmpty()
        .ifEmpty { normalizedFullName }
    if (normalizedUsername.isEmpty()) return null
    return SearchUser(
        id = id,
        username = normalizedUsername,
        fullName = normalizedFullName.ifEmpty { normalizedUsername },
        avatarUrl = avatarUrl?.trim()?.takeIf(String::isNotEmpty),
        isVerified = isVerified,
        verificationType = verificationType?.trim()?.takeIf(String::isNotEmpty),
        role = role?.trim()?.takeIf(String::isNotEmpty),
    )
}

internal fun SearchPostDto.toDomainOrNull(): SearchPost? {
    val normalizedId = id?.trim().orEmpty()
    if (normalizedId.isEmpty()) return null
    return SearchPost(
        id = normalizedId,
        imageUrl = sequenceOf(imageUrl, imageUrls.firstOrNull())
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .firstOrNull(),
        videoUrl = videoUrl?.trim()?.takeIf(String::isNotEmpty),
    )
}

internal fun HashtagSuggestionDto.toDomainOrNull(): HashtagSuggestion? {
    val normalized = tag?.trim()?.trimStart('#')?.takeIf(String::isNotEmpty)
        ?: return null
    return HashtagSuggestion(
        tag = normalized,
        usageCount = usageCount.coerceAtLeast(0),
    )
}
