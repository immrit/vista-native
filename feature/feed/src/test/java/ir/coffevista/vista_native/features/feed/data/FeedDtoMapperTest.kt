package ir.coffevista.vista_native.features.feed.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedDtoMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun successResponseMatchesBackendEnvelopeAndTimestampFormat() {
        val response = json.decodeFromString<FeedResponseDto>(SUCCESS_RESPONSE)

        assertEquals(1, response.posts.size)
        assertTrue(response.hasMore)
        assertEquals("2026-07-27T17:21:49.123456789Z", response.nextCursor)
        with(response.posts.single()) {
            assertEquals("post-1", id)
            assertEquals("user-1", userId)
            assertEquals("2026-07-27T17:21:49.123456789Z", createdAt)
            assertEquals("personal", feedSource)
            assertEquals(0.91, feedScore ?: 0.0, 0.0)
            assertEquals(listOf("vista"), tags)
            assertEquals("Track", musicTitle)
            assertFalse(hideLikeCount)
            assertTrue(hideCommentCount)
        }
    }

    @Test
    fun nullableAuthorAndMediaFieldsRemainNull() {
        val dto = postDto(
            content = null,
            imageUrl = null,
            imageUrls = emptyList(),
            videoUrl = null,
            author = author(username = null, avatarUrl = null, verificationType = null),
        )

        val entity = dto.asEntity(accountId = "account-a", sortOrder = 0)
        val model = entity.asExternalModel()

        assertNull(model.content)
        assertNull(model.primaryImageUrl)
        assertNull(model.videoUrl)
        assertNull(model.authorUsername)
        assertNull(model.authorAvatarUrl)
        assertNull(model.authorVerificationType)
    }

    @Test
    fun imagePostUsesFirstGalleryImageAsPrimaryMedia() {
        val entity = postDto(
            imageUrl = "https://cdn.example/cover.jpg",
            imageUrls = listOf(
                "https://cdn.example/one.jpg",
                "https://cdn.example/two.jpg",
            ),
        ).asEntity(accountId = "account-a", sortOrder = 0)

        assertEquals(
            "https://cdn.example/one.jpg",
            entity.asExternalModel().primaryImageUrl,
        )
    }

    @Test
    fun videoPostUsesBackendImageAsThumbnailWithoutGuessedField() {
        val model = postDto(
            imageUrl = "https://cdn.example/video-cover.jpg",
            videoUrl = "https://cdn.example/video.mp4",
        ).asEntity(accountId = "account-a", sortOrder = 0).asExternalModel()

        assertEquals("https://cdn.example/video-cover.jpg", model.videoThumbnailUrl)
        assertEquals("https://cdn.example/video.mp4", model.videoUrl)
    }

    @Test
    fun emptyFeedEnvelopeDecodesPredictably() {
        val response = json.decodeFromString<FeedResponseDto>(
            """{"posts":[],"has_more":false}""",
        )

        assertTrue(response.posts.isEmpty())
        assertFalse(response.hasMore)
        assertNull(response.nextCursor)
    }

    @Test
    fun malformedAndBackendErrorEnvelopesAreNotAcceptedAsFeed() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<FeedResponseDto>(
                """{"posts":"invalid","has_more":false}""",
            )
        }
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<FeedResponseDto>(
                """{"code":"POSTS_UNAVAILABLE","message":"temporary"}""",
            )
        }
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<FeedResponseDto>(
                """{"error":"internal server error"}""",
            )
        }
    }

    private fun postDto(
        id: String = "post-1",
        content: String? = "caption",
        imageUrl: String? = null,
        imageUrls: List<String> = emptyList(),
        videoUrl: String? = null,
        author: AuthorInfoDto = author(),
    ) = FeedPostDto(
        id = id,
        userId = author.userId,
        content = content,
        imageUrl = imageUrl,
        imageUrls = imageUrls,
        videoUrl = videoUrl,
        musicUrl = null,
        aspectRatio = "1:1",
        musicTitle = null,
        tags = emptyList(),
        likeCount = 12,
        commentCount = 3,
        isLiked = false,
        isSaved = false,
        hideLikeCount = false,
        hideCommentCount = false,
        author = author,
        createdAt = "2026-07-27T17:21:49.123456789Z",
        updatedAt = "2026-07-27T17:22:00Z",
    )

    private fun author(
        username: String? = "vista",
        avatarUrl: String? = "https://cdn.example/avatar.jpg",
        verificationType: String? = "blueTick",
    ) = AuthorInfoDto(
        userId = "user-1",
        username = username,
        fullName = "Vista User",
        avatarUrl = avatarUrl,
        isVerified = verificationType != null,
        verificationType = verificationType,
    )

    private companion object {
        val SUCCESS_RESPONSE = """
            {
              "posts": [{
                "id": "post-1",
                "user_id": "user-1",
                "content": "caption",
                "image_url": "https://cdn.example/cover.jpg",
                "image_urls": [],
                "video_url": "https://cdn.example/video.mp4",
                "music_url": "https://cdn.example/music.mp3",
                "aspect_ratio": "16:9",
                "music_title": "Track",
                "tags": ["vista"],
                "like_count": 4,
                "comment_count": 2,
                "is_liked": false,
                "is_saved": true,
                "feed_source": "personal",
                "feed_score": 0.91,
                "author_follow_status": "following",
                "hide_like_count": false,
                "hide_comment_count": true,
                "author": {
                  "user_id": "user-1",
                  "username": "vista",
                  "full_name": "Vista User",
                  "avatar_url": null,
                  "is_verified": true,
                  "verification_type": "blueTick"
                },
                "created_at": "2026-07-27T17:21:49.123456789Z",
                "updated_at": "2026-07-27T17:22:00Z"
              }],
              "has_more": true,
              "next_cursor": "2026-07-27T17:21:49.123456789Z"
            }
        """.trimIndent()
    }
}
