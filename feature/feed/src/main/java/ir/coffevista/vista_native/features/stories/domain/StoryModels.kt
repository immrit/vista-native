package ir.coffevista.vista_native.features.stories.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class StoryMediaType {
    @SerialName("image") Image,
    @SerialName("video") Video,
}

enum class StoryPrivacyType {
    @SerialName("everyone") Everyone,
    @SerialName("close_friends") CloseFriends,
    @SerialName("followers") Followers,
    @SerialName("custom") Custom,
}

enum class StoryDurationType {
    @SerialName("hours24") Hours24,
    @SerialName("hours48") Hours48,
}

enum class StoryVerificationType {
    @SerialName("none") None,
    @SerialName("blue") Blue,
    @SerialName("gold") Gold,
    @SerialName("black") Black,
}

@Serializable
data class StoryPollOption(
    val id: String,
    val text: String,
    @SerialName("votes_count") val votesCount: Int = 0,
    @SerialName("is_voted") val isVoted: Boolean = false,
)

@Serializable
data class StoryPoll(
    val id: String,
    val question: String,
    val options: List<StoryPollOption> = emptyList(),
    @SerialName("total_votes") val totalVotes: Int = 0,
    @SerialName("user_voted_option_id") val userVotedOptionId: String? = null,
)

@Serializable
data class StoryLink(
    val url: String,
    val title: String? = null,
)

@Serializable
data class StoryLocation(
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class StoryMention(
    @SerialName("user_id") val userId: String,
    val username: String,
)

@Serializable
data class StoryMusic(
    @SerialName("music_url") val musicUrl: String,
    val title: String,
    val artist: String,
    @SerialName("cover_url") val coverUrl: String? = null,
)

@Serializable
data class StoryElement(
    val id: String,
    val type: String, // "text", "poll", "question", "mention", "location", "link", "drawing", "music"
    val content: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val color: String? = null,
    @SerialName("background_color") val backgroundColor: String? = null,
    val poll: StoryPoll? = null,
    val link: StoryLink? = null,
    val location: StoryLocation? = null,
    val mention: StoryMention? = null,
    val music: StoryMusic? = null,
)

@Serializable
data class Story(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("media_type") val mediaType: StoryMediaType = StoryMediaType.Image,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val caption: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("privacy_type") val privacyType: StoryPrivacyType = StoryPrivacyType.Everyone,
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("reactions_count") val reactionsCount: Int = 0,
    @SerialName("is_viewed") val isViewed: Boolean = false,
    @SerialName("viewer_can_reply") val viewerCanReply: Boolean = true,
    @SerialName("poll") val poll: StoryPoll? = null,
    @SerialName("link") val link: StoryLink? = null,
    @SerialName("location") val location: StoryLocation? = null,
    @SerialName("mentions") val mentions: List<StoryMention> = emptyList(),
    @SerialName("music_url") val musicUrl: String? = null,
    @SerialName("music_title") val musicTitle: String? = null,
    @SerialName("interactive_elements") val interactiveElements: List<StoryElement> = emptyList(),
)

@Serializable
data class StoryUser(
    val id: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_premium") val isPremium: Boolean = false,
    @SerialName("verification_type") val verificationType: StoryVerificationType = StoryVerificationType.None,
    val stories: List<Story> = emptyList(),
    @SerialName("last_story_at") val lastStoryAt: String? = null,
) {
    val allViewed: Boolean get() = stories.isNotEmpty() && stories.all { it.isViewed }
    val hasUnseenStories: Boolean get() = stories.any { !it.isViewed }
    val unseenCount: Int get() = stories.count { !it.isViewed }
}

@Serializable
data class StoryViewProfileDto(
    val username: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("verification_type") val verificationType: String? = null,
    val role: String? = null,
)

@Serializable
data class StoryViewDto(
    @SerialName("viewer_id") val viewerId: String = "",
    @SerialName("user_id") val legacyUserId: String = "",
    val profiles: StoryViewProfileDto? = null,
    val username: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("viewed_at") val viewedAt: String = "",
    @SerialName("reaction") val reaction: String? = null,
) {
    fun toDomain(): StoryView {
        val resolvedUserId = viewerId.ifBlank { legacyUserId }
        val resolvedUsername = profiles?.username?.ifBlank { null } ?: username
        val resolvedFullName = profiles?.fullName?.ifBlank { null } ?: fullName.ifBlank { resolvedUsername }
        val resolvedAvatarUrl = profiles?.avatarUrl ?: avatarUrl
        val resolvedIsVerified = profiles?.isVerified ?: isVerified
        return StoryView(
            userId = resolvedUserId,
            username = resolvedUsername,
            fullName = resolvedFullName,
            avatarUrl = resolvedAvatarUrl,
            isVerified = resolvedIsVerified,
            viewedAt = viewedAt,
            reaction = reaction,
        )
    }
}

@Serializable
data class StoryView(
    @SerialName("user_id") val userId: String = "",
    val username: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("viewed_at") val viewedAt: String = "",
    @SerialName("reaction") val reaction: String? = null,
)

@Serializable
data class ActiveStoriesResponseDto(
    val users: List<StoryUser> = emptyList(),
)

@Serializable
data class UserStoriesResponseDto(
    val stories: List<Story> = emptyList(),
)

@Serializable
data class FollowingStoryUsersResponseDto(
    val profiles: List<StoryUser> = emptyList(),
)

@Serializable
data class CloseFriendsUpdateRequestDto(
    @SerialName("friend_ids") val friendIds: List<String>,
)

@Serializable
data class CreateStoryRequestDto(
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("media_type") val mediaType: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val caption: String? = null,
    @SerialName("duration_type") val durationType: String = "hours24",
    @SerialName("privacy_type") val privacyType: String = "everyone",
    @SerialName("allowed_user_ids") val allowedUserIds: List<String> = emptyList(),
    @SerialName("excluded_user_ids") val excludedUserIds: List<String> = emptyList(),
    @SerialName("interactive_elements") val interactiveElements: List<StoryElement> = emptyList(),
    @SerialName("music_url") val musicUrl: String? = null,
)

@Serializable
data class StoryViewsResponseDto(
    val views: List<StoryViewDto> = emptyList(),
    val total: Int = 0,
) {
    fun toDomainViews(): List<StoryView> = views.map { it.toDomain() }
}

@Serializable
data class StoryReactRequestDto(
    val reaction: String = "heart",
)

@Serializable
data class StoryReplyRequestDto(
    val message: String,
)

@Serializable
data class StoryVoteRequestDto(
    @SerialName("option_index") val optionIndex: Int = 0,
    @SerialName("option_id") val legacyOptionId: String? = null,
)

@Serializable
data class StoryReportRequestDto(
    val reason: String,
    val details: String = "",
)

@Serializable
data class StoryReplyAccessDto(
    @SerialName("can_reply") val canReply: Boolean = true,
    val reason: String? = null,
)

@Serializable
data class StoryQuestionAnswerRequestDto(
    val answer: String,
)

@Serializable
data class StoryQuestionAnswerDto(
    val id: String = "",
    @SerialName("story_id") val storyId: String = "",
    @SerialName("user_id") val userId: String = "",
    val answer: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val profile: StoryViewProfileDto? = null,
)

@Serializable
data class StoryQuestionAnswersResponseDto(
    val answers: List<StoryQuestionAnswerDto> = emptyList(),
)

@Serializable
data class StorySettingsDto(
    @SerialName("reply_permission") val replyPermission: String = "everyone",
)

@Serializable
data class HighlightDto(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    val title: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("story_ids") val storyIds: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class CreateHighlightRequestDto(
    val title: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("story_ids") val storyIds: List<String>,
)

@Serializable
data class UpdateHighlightRequestDto(
    val title: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("story_ids") val storyIds: List<String>? = null,
)

@Serializable
data class UserHighlightsResponseDto(
    val highlights: List<HighlightDto> = emptyList(),
)

@Serializable
data class HighlightCountResponseDto(
    val count: Int = 0,
)

