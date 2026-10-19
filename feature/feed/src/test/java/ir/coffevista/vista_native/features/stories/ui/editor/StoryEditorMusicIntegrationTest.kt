package ir.coffevista.vista_native.features.stories.ui.editor

import ir.coffevista.vista_native.features.feed.music.MusicTrack
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.domain.StoryMusic
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StoryEditorMusicIntegrationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun createTestTrack(id: String, title: String, artist: String): MusicTrack = MusicTrack(
        id = id,
        userId = "user-$id",
        title = title,
        artist = artist,
        coverUrl = "https://cdn.example.com/$id.jpg",
        musicUrl = "https://cdn.example.com/$id.mp3",
        createdAt = "2026-09-28T12:00:00Z",
        playCount = 10,
        username = "artist_$id",
        avatarUrl = "https://cdn.example.com/avatar_$id.jpg",
        isVerified = true,
        genres = listOf("Pop"),
    )

    @Test
    fun `story element with music serializes and deserializes accurately`() {
        val music = StoryMusic(
            musicUrl = "https://cdn.example.com/audio/track_01.mp3",
            title = "Track Title",
            artist = "Artist Name",
            coverUrl = "https://cdn.example.com/images/cover.jpg",
        )
        val element = StoryElement(
            id = "music-elem-1",
            type = "music",
            music = music,
            y = 0.35f,
        )

        val encoded = json.encodeToString(StoryElement.serializer(), element)
        assertTrue(encoded.contains("\"type\":\"music\""))
        assertTrue(encoded.contains("Track Title"))
        assertTrue(encoded.contains("Artist Name"))

        val decoded = json.decodeFromString(StoryElement.serializer(), encoded)
        assertEquals("music", decoded.type)
        assertNotNull(decoded.music)
        assertEquals("Track Title", decoded.music?.title)
        assertEquals("Artist Name", decoded.music?.artist)
        assertEquals("https://cdn.example.com/audio/track_01.mp3", decoded.music?.musicUrl)
        assertEquals("https://cdn.example.com/images/cover.jpg", decoded.music?.coverUrl)
    }

    @Test
    fun `adding music element replaces any existing music element in the list`() {
        val track1 = createTestTrack("1", "Track 1", "Artist 1")
        val track2 = createTestTrack("2", "Track 2", "Artist 2")

        val elem1 = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "music",
            music = StoryMusic(track1.musicUrl, track1.title, track1.artist, track1.coverUrl),
        )
        val otherElem = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "text",
            content = "Hello",
        )

        val initialList = listOf(otherElem, elem1)

        val elem2 = StoryElement(
            id = UUID.randomUUID().toString(),
            type = "music",
            music = StoryMusic(track2.musicUrl, track2.title, track2.artist, track2.coverUrl),
        )

        val updatedList = initialList.filterNot { it.type == "music" } + elem2

        assertEquals(2, updatedList.size)
        assertEquals(1, updatedList.count { it.type == "music" })
        assertEquals("Track 2", updatedList.first { it.type == "music" }.music?.title)
        assertEquals(1, updatedList.count { it.type == "text" })
    }
}
