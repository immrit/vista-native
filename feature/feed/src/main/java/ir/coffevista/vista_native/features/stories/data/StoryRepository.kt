package ir.coffevista.vista_native.features.stories.data

import ir.coffevista.vista_native.features.stories.domain.CreateStoryRequestDto
import ir.coffevista.vista_native.features.stories.domain.Story
import ir.coffevista.vista_native.features.stories.domain.StoryReactRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryReplyRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryUser
import ir.coffevista.vista_native.features.stories.domain.StoryView
import ir.coffevista.vista_native.features.stories.domain.StoryVoteRequestDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository internal constructor(
    private val api: StoryApi,
    private val ioDispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(api: StoryApi) : this(api, Dispatchers.IO)

    private val scope = CoroutineScope(ioDispatcher)
    private val _activeStoryUsers = MutableStateFlow<List<StoryUser>>(emptyList())
    val activeStoryUsers: StateFlow<List<StoryUser>> = _activeStoryUsers.asStateFlow()

    suspend fun refreshActiveStories(): Result<List<StoryUser>> = withContext(ioDispatcher) {
        try {
            val response = api.getActiveStories()
            _activeStoryUsers.value = response.users
            Result.success(response.users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserStories(userId: String): Result<List<Story>> = withContext(ioDispatcher) {
        try {
            val response = api.getUserStories(userId)
            Result.success(response.stories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createStory(request: CreateStoryRequestDto): Result<Story> = withContext(ioDispatcher) {
        try {
            val story = api.createStory(request)
            refreshActiveStories()
            Result.success(story)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStory(storyId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            api.deleteStory(storyId)
            _activeStoryUsers.update { users ->
                users.map { user ->
                    user.copy(stories = user.stories.filterNot { it.id == storyId })
                }.filter { it.stories.isNotEmpty() }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun markStoryAsViewed(storyId: String) {
        // 1. Update state immediately in-memory for zero latency
        _activeStoryUsers.update { users ->
            users.map { user ->
                val updatedStories = user.stories.map { story ->
                    if (story.id == storyId) story.copy(isViewed = true) else story
                }
                user.copy(stories = updatedStories)
            }
        }
        // 2. Track on backend asynchronously
        scope.launch {
            try {
                api.trackView(storyId)
            } catch (_: Exception) {
                // Non-critical network tracking failure
            }
        }
    }

    suspend fun reactToStory(storyId: String, reaction: String = "heart"): Result<Unit> = withContext(ioDispatcher) {
        try {
            api.reactToStory(storyId, StoryReactRequestDto(reaction))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun replyToStory(storyId: String, message: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            api.replyToStory(storyId, StoryReplyRequestDto(message))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun votePoll(storyId: String, optionId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            api.votePoll(storyId, StoryVoteRequestDto(optionId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStoryViews(storyId: String, limit: Int = 50, offset: Int = 0): Result<List<StoryView>> = withContext(ioDispatcher) {
        try {
            val response = api.getStoryViews(storyId, limit, offset)
            Result.success(response.views)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
