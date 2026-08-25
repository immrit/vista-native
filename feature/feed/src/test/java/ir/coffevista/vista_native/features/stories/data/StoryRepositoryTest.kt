package ir.coffevista.vista_native.features.stories.data

import ir.coffevista.vista_native.features.stories.domain.ActiveStoriesResponseDto
import ir.coffevista.vista_native.features.stories.domain.CreateStoryRequestDto
import ir.coffevista.vista_native.features.stories.domain.Story
import ir.coffevista.vista_native.features.stories.domain.StoryMediaType
import ir.coffevista.vista_native.features.stories.domain.StoryReactRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryReplyRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryUser
import ir.coffevista.vista_native.features.stories.domain.StoryViewsResponseDto
import ir.coffevista.vista_native.features.stories.domain.StoryVoteRequestDto
import ir.coffevista.vista_native.features.stories.domain.UserStoriesResponseDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoryRepositoryTest {

    private class FakeStoryApi : StoryApi {
        var lastStoryIdTracked: String? = null
        var lastReactionSent: String? = null
        var lastReplySent: String? = null
        var lastVoteOption: String? = null
        var isDeleteCalled: Boolean = false

        val sampleStory = Story(
            id = "story_1",
            userId = "user_1",
            mediaUrl = "https://example.com/story1.jpg",
            mediaType = StoryMediaType.Image,
            createdAt = "2026-08-24T10:00:00Z",
            expiresAt = "2026-08-25T10:00:00Z",
            isViewed = false,
        )

        val sampleUser = StoryUser(
            id = "user_1",
            username = "ali_vista",
            avatarUrl = "https://example.com/avatar.jpg",
            stories = listOf(sampleStory),
        )

        override suspend fun getActiveStories(): ActiveStoriesResponseDto {
            return ActiveStoriesResponseDto(users = listOf(sampleUser))
        }

        override suspend fun getUserStories(userId: String): UserStoriesResponseDto {
            return UserStoriesResponseDto(stories = listOf(sampleStory))
        }

        override suspend fun getStoryById(storyId: String): Story = sampleStory

        override suspend fun createStory(request: CreateStoryRequestDto): Story = sampleStory

        override suspend fun deleteStory(storyId: String) {
            isDeleteCalled = true
        }

        override suspend fun trackView(storyId: String) {
            lastStoryIdTracked = storyId
        }

        override suspend fun getStoryViews(storyId: String, limit: Int, offset: Int): StoryViewsResponseDto {
            return StoryViewsResponseDto(views = emptyList(), total = 0)
        }

        override suspend fun reactToStory(storyId: String, request: StoryReactRequestDto) {
            lastReactionSent = request.reaction
        }

        override suspend fun replyToStory(storyId: String, request: StoryReplyRequestDto) {
            lastReplySent = request.message
        }

        override suspend fun votePoll(storyId: String, request: StoryVoteRequestDto) {
            lastVoteOption = request.optionId
        }
    }

    private lateinit var fakeApi: FakeStoryApi
    private lateinit var repository: StoryRepository

    @Before
    fun setUp() {
        fakeApi = FakeStoryApi()
        repository = StoryRepository(fakeApi)
    }

    @Test
    fun `refreshActiveStories updates activeStoryUsers flow`() = runTest {
        val result = repository.refreshActiveStories()
        assertTrue(result.isSuccess)

        val users = repository.activeStoryUsers.value
        assertEquals(1, users.size)
        assertEquals("user_1", users.first().id)
        assertEquals("ali_vista", users.first().username)
        assertTrue(users.first().hasUnseenStories)
        assertFalse(users.first().allViewed)
    }

    @Test
    fun `markStoryAsViewed updates in-memory story state immediately`() = runTest {
        repository.refreshActiveStories()
        repository.markStoryAsViewed("story_1")

        val users = repository.activeStoryUsers.value
        val story = users.first().stories.first()
        assertTrue(story.isViewed)
        assertTrue(users.first().allViewed)
    }

    @Test
    fun `reactToStory sends correct reaction payload`() = runTest {
        val result = repository.reactToStory("story_1", "heart")
        assertTrue(result.isSuccess)
        assertEquals("heart", fakeApi.lastReactionSent)
    }

    @Test
    fun `replyToStory sends direct message`() = runTest {
        val result = repository.replyToStory("story_1", "عالی بود!")
        assertTrue(result.isSuccess)
        assertEquals("عالی بود!", fakeApi.lastReplySent)
    }

    @Test
    fun `votePoll records user choice`() = runTest {
        val result = repository.votePoll("story_1", "opt_1")
        assertTrue(result.isSuccess)
        assertEquals("opt_1", fakeApi.lastVoteOption)
    }
}
