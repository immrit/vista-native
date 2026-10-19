package ir.coffevista.vista_native.features.feed.music

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicProfileDto(
    @SerialName("username") val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
)

@Serializable
data class MusicDto(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("artist") val artist: String = "",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("music_url") val musicUrl: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("play_count") val playCount: Int = 0,
    @SerialName("profiles") val profile: MusicProfileDto? = null,
    @SerialName("genres") val genres: List<String> = emptyList(),
)

@Serializable
data class MusicListResponseDto(
    @SerialName("music") val music: List<MusicDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class PublishMusicRequestDto(
    @SerialName("title") val title: String,
    @SerialName("artist") val artist: String,
    @SerialName("music_url") val musicUrl: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("genres") val genres: List<String> = emptyList(),
)

data class MusicTrack(
    val id: String,
    val userId: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val musicUrl: String,
    val createdAt: String,
    val playCount: Int,
    val username: String,
    val avatarUrl: String,
    val isVerified: Boolean,
    val genres: List<String>,
)

fun MusicDto.toDomain(): MusicTrack = MusicTrack(
    id = id,
    userId = userId,
    title = title.ifBlank { "موزیک" },
    artist = artist.ifBlank { profile?.username.orEmpty().ifBlank { "هنرمند ناشناس" } },
    coverUrl = coverUrl,
    musicUrl = musicUrl,
    createdAt = createdAt,
    playCount = playCount,
    username = profile?.username.orEmpty(),
    avatarUrl = profile?.avatarUrl.orEmpty(),
    isVerified = profile?.isVerified ?: false,
    genres = genres,
)
