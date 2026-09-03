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
data class StoryElement(
    val id: String,
    val type: String, // "text", "poll", "question", "mention", "location", "link", "drawing"
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
data class StoryView(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("viewed_at") val viewedAt: String,
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
    val views: List<StoryView> = emptyList(),
    val total: Int = 0,
)

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
    @SerialName("option_id") val optionId: String,
)
