package ir.coffevista.vista_native.features.feed.music

import ir.coffevista.vista_native.core.network.BackendErrorParser
import ir.coffevista.vista_native.core.network.RemoteFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

interface MusicRepository {
    val cachedTracks: StateFlow<List<MusicTrack>>
    suspend fun fetchMusics(limit: Int = 20, offset: Int = 0): Result<List<MusicTrack>>
    suspend fun fetchUserMusics(
        userId: String? = null,
        query: String? = null,
        limit: Int = 40,
        offset: Int = 0,
    ): Result<List<MusicTrack>> = fetchMusics(limit = limit, offset = offset)
    suspend fun publishMusic(
        title: String,
        artist: String,
        musicUrl: String,
        coverUrl: String? = null,
        genres: List<String> = emptyList(),
    ): Result<MusicTrack>
    suspend fun incrementPlayCount(musicId: String): Result<Unit>
}

@Singleton
class DefaultMusicRepository @Inject constructor(
    private val musicApi: MusicApi,
) : MusicRepository {

    private val _cachedTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    override val cachedTracks: StateFlow<List<MusicTrack>> = _cachedTracks.asStateFlow()

    override suspend fun fetchMusics(limit: Int, offset: Int): Result<List<MusicTrack>> = runCatching {
        val response = musicApi.fetchMusics(limit = limit, offset = offset).bodyOrThrow("music_fetch_failed")
        val tracks = response.music.map { it.toDomain() }
        if (offset == 0) {
            _cachedTracks.value = tracks
        } else {
            val existing = _cachedTracks.value.toMutableList()
            tracks.forEach { newTrack ->
                val idx = existing.indexOfFirst { it.id == newTrack.id }
                if (idx >= 0) {
                    existing[idx] = newTrack
                } else {
                    existing.add(newTrack)
                }
            }
            _cachedTracks.value = existing
        }
        tracks
    }

    override suspend fun fetchUserMusics(
        userId: String?,
        query: String?,
        limit: Int,
        offset: Int,
    ): Result<List<MusicTrack>> = runCatching {
        val cleanUserId = userId?.trim()?.takeIf { it.isNotBlank() }
        val cleanQuery = query?.trim()?.takeIf { it.isNotBlank() }
        val response = musicApi.fetchFilteredMusics(
            userId = cleanUserId,
            query = cleanQuery,
            limit = limit,
            offset = offset,
        ).bodyOrThrow("music_fetch_failed")
        response.music
            .map { it.toDomain() }
            .filter { track ->
                (cleanUserId == null || track.userId == cleanUserId) &&
                    (cleanQuery == null ||
                        track.title.contains(cleanQuery, ignoreCase = true) ||
                        track.artist.contains(cleanQuery, ignoreCase = true))
            }
    }

    override suspend fun publishMusic(
        title: String,
        artist: String,
        musicUrl: String,
        coverUrl: String?,
        genres: List<String>,
    ): Result<MusicTrack> = runCatching {
        val request = PublishMusicRequestDto(
            title = title,
            artist = artist,
            musicUrl = musicUrl,
            coverUrl = coverUrl,
            genres = genres,
        )
        val response = musicApi.publishMusic(request).bodyOrThrow("music_publish_failed")
        val track = response.toDomain()
        _cachedTracks.value = listOf(track) + _cachedTracks.value.filterNot { it.id == track.id }
        track
    }

    override suspend fun incrementPlayCount(musicId: String): Result<Unit> = runCatching {
        musicApi.incrementPlayCount(musicId).bodyOrThrow("music_play_failed")
    }

    private fun <T> Response<T>.bodyOrThrow(defaultCode: String): T {
        if (isSuccessful) {
            return body() ?: throw RemoteFailure(
                statusCode = code(),
                code = defaultCode,
                message = "پاسخ سرور خالی بود",
            )
        }
        val parsed = BackendErrorParser.parse(
            rawBody = errorBody()?.string().orEmpty(),
            retryAfterHeader = headers()["Retry-After"],
        )
        throw RemoteFailure(
            statusCode = code(),
            code = parsed.code,
            message = parsed.message,
            retryAfterSeconds = parsed.retryAfterSeconds,
        )
    }
}
