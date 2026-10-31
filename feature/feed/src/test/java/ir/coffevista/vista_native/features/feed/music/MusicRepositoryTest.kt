package ir.coffevista.vista_native.features.feed.music

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class MusicRepositoryTest {

    private class FakeMusicApi(
        var musicsToReturn: List<MusicDto> = emptyList(),
        var publishResult: MusicDto? = null,
        var shouldFail: Boolean = false,
    ) : MusicApi {
        var incrementedId: String? = null
        var lastPublishRequest: PublishMusicRequestDto? = null

        override suspend fun fetchMusics(limit: Int, offset: Int): Response<MusicListResponseDto> {
            if (shouldFail) {
                return Response.error(
                    500,
                    """{"code":"internal_error","message":"Server error"}""".toResponseBody("application/json".toMediaType()),
                )
            }
            return Response.success(MusicListResponseDto(music = musicsToReturn))
        }

        override suspend fun publishMusic(request: PublishMusicRequestDto): Response<MusicDto> {
            lastPublishRequest = request
            if (shouldFail) {
                return Response.error(
                    400,
                    """{"code":"invalid_data","message":"Invalid music"}""".toResponseBody("application/json".toMediaType()),
                )
            }
            return Response.success(publishResult ?: MusicDto(id = "new_1", title = request.title, artist = request.artist, musicUrl = request.musicUrl))
        }

        override suspend fun incrementPlayCount(musicId: String): Response<Unit> {
            incrementedId = musicId
            return Response.success(Unit)
        }
    }

    @Test
    fun `fetchMusics parses DTO to Domain and updates cachedTracks`() = runTest {
        val fakeApi = FakeMusicApi(
            musicsToReturn = listOf(
                MusicDto(
                    id = "m1",
                    title = "آهنگ اول",
                    artist = "خواننده ۱",
                    musicUrl = "https://cdn.example.com/song1.mp3",
                    coverUrl = "https://cdn.example.com/cover1.jpg",
                    playCount = 42,
                    genres = listOf("سنتی", "پاپ"),
                ),
            ),
        )
        val repository = DefaultMusicRepository(fakeApi)

        val result = repository.fetchMusics(limit = 20, offset = 0)
        assertTrue(result.isSuccess)
        val tracks = result.getOrThrow()
        assertEquals(1, tracks.size)
        assertEquals("m1", tracks.first().id)
        assertEquals("آهنگ اول", tracks.first().title)
        assertEquals("خواننده ۱", tracks.first().artist)
        assertEquals(42, tracks.first().playCount)
        assertEquals(listOf("سنتی", "پاپ"), tracks.first().genres)

        assertEquals(1, repository.cachedTracks.value.size)
        assertEquals("m1", repository.cachedTracks.value.first().id)
    }

    @Test
    fun `publishMusic delegates to API and adds to cache`() = runTest {
        val fakeApi = FakeMusicApi(
            publishResult = MusicDto(
                id = "pub_10",
                title = "ترانه بهاری",
                artist = "هنرمند ویستا",
                musicUrl = "https://cdn.example.com/spring.mp3",
            ),
        )
        val repository = DefaultMusicRepository(fakeApi)

        val result = repository.publishMusic(
            title = "ترانه بهاری",
            artist = "هنرمند ویستا",
            musicUrl = "https://cdn.example.com/spring.mp3",
            coverUrl = "https://cdn.example.com/spring.jpg",
            genres = listOf("پاپ"),
        )

        assertTrue(result.isSuccess)
        val track = result.getOrThrow()
        assertEquals("pub_10", track.id)
        assertEquals("ترانه بهاری", track.title)
        assertEquals("هنرمند ویستا", fakeApi.lastPublishRequest?.artist)

        assertTrue(repository.cachedTracks.value.any { it.id == "pub_10" })
    }

    @Test
    fun `incrementPlayCount delegates to API`() = runTest {
        val fakeApi = FakeMusicApi()
        val repository = DefaultMusicRepository(fakeApi)

        val result = repository.incrementPlayCount("track_99")
        assertTrue(result.isSuccess)
        assertEquals("track_99", fakeApi.incrementedId)
    }

    @Test
    fun `fetchMusics failure maps error to Result failure`() = runTest {
        val fakeApi = FakeMusicApi(shouldFail = true)
        val repository = DefaultMusicRepository(fakeApi)

        val result = repository.fetchMusics()
        assertTrue(result.isFailure)
    }

    @Test
    fun `fetchUserMusics filters tracks to signed-in user and search query`() = runTest {
        val fakeApi = FakeMusicApi(
            musicsToReturn = listOf(
                MusicDto(id = "u1_a", userId = "user_1", title = "شب بارانی", artist = "شادمهر", musicUrl = "https://cdn.example.com/1.mp3"),
                MusicDto(id = "u2_a", userId = "user_2", title = "شب مهتابی", artist = "سایر", musicUrl = "https://cdn.example.com/2.mp3"),
                MusicDto(id = "u1_b", userId = "user_1", title = "طلوع آفتاب", artist = "شادمهر", musicUrl = "https://cdn.example.com/3.mp3"),
            ),
        )
        val repository = DefaultMusicRepository(fakeApi)

        val allMine = repository.fetchUserMusics(userId = "user_1")
        assertTrue(allMine.isSuccess)
        assertEquals(2, allMine.getOrThrow().size)
        assertTrue(allMine.getOrThrow().all { it.userId == "user_1" })

        val searchedMine = repository.fetchUserMusics(userId = "user_1", query = "بارانی")
        assertTrue(searchedMine.isSuccess)
        assertEquals(1, searchedMine.getOrThrow().size)
        assertEquals("u1_a", searchedMine.getOrThrow().first().id)
    }
}
