package ir.coffevista.vista_native.features.stories.data

import ir.coffevista.vista_native.features.stories.domain.CreateHighlightRequestDto
import ir.coffevista.vista_native.features.stories.domain.CreateStoryRequestDto
import ir.coffevista.vista_native.features.stories.domain.CloseFriendsUpdateRequestDto
import ir.coffevista.vista_native.features.stories.domain.HighlightDto
import ir.coffevista.vista_native.features.stories.domain.Story
import ir.coffevista.vista_native.features.stories.domain.StoryQuestionAnswerDto
import ir.coffevista.vista_native.features.stories.domain.StoryQuestionAnswerRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryReactRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryReplyAccessDto
import ir.coffevista.vista_native.features.stories.domain.StoryReplyRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryReportRequestDto
import ir.coffevista.vista_native.features.stories.domain.StorySettingsDto
import ir.coffevista.vista_native.features.stories.domain.StoryUser
import ir.coffevista.vista_native.features.stories.domain.StoryView
import ir.coffevista.vista_native.features.stories.domain.StoryVoteRequestDto
import ir.coffevista.vista_native.features.stories.domain.UpdateHighlightRequestDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
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

    suspend fun refreshActiveStories(currentUserId: String = ""): Result<List<StoryUser>> = withContext(ioDispatcher) {
        try {
            val response = api.getActiveStories()
            val normalized = normalizeAndSortStoryUsers(response.users, currentUserId)
            _activeStoryUsers.value = normalized
            Result.success(normalized)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserStories(userId: String): Result<List<Story>> = withContext(ioDispatcher) {
        try {
            val response = api.getUserStories(userId)
            Result.success(sortStoriesChronologically(response.stories))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowingUsers(userId: String): Result<List<StoryUser>> = withContext(ioDispatcher) {
        runCatching { api.getFollowingUsers(userId).profiles }
    }

    suspend fun getCloseFriendIds(): Result<List<String>> = withContext(ioDispatcher) {
        runCatching { api.getCloseFriends() }
    }

    suspend fun updateCloseFriends(friendIds: List<String>): Result<Unit> = withContext(ioDispatcher) {
        runCatching { api.updateCloseFriends(CloseFriendsUpdateRequestDto(friendIds.distinct())) }
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

    fun markStoryAsViewed(storyId: String, currentUserId: String = "") {
        var isOwnStory = false
        // 1. Update state immediately in-memory for zero latency
        _activeStoryUsers.update { users ->
            users.map { user ->
                val updatedStories = user.stories.map { story ->
                    if (story.id == storyId) {
                        if (currentUserId.isNotBlank() && (story.userId == currentUserId || user.id == currentUserId)) {
                            isOwnStory = true
                        }
                        story.copy(isViewed = true)
                    } else {
                        story
                    }
                }
                user.copy(stories = updatedStories)
            }
        }
        if (isOwnStory) return
        // 2. Track on backend asynchronously for non-owner viewers
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
            if (reaction.equals("unlike", ignoreCase = true) || reaction.equals("none", ignoreCase = true)) {
                api.removeStoryReaction(storyId)
            } else {
                api.reactToStory(storyId, StoryReactRequestDto(reaction))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeReaction(storyId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            api.removeStoryReaction(storyId)
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

    suspend fun votePoll(storyId: String, elementId: String, optionIndex: Int): Result<Unit> = withContext(ioDispatcher) {
        // Optimistic in-memory update
        _activeStoryUsers.update { users ->
            users.map { user ->
                user.copy(
                    stories = user.stories.map { story ->
                        if (story.id == storyId) {
                            val updatedPoll = story.poll?.let { poll ->
                                val updatedOptions = poll.options.mapIndexed { idx, opt ->
                                    if (idx == optionIndex) opt.copy(votesCount = opt.votesCount + 1, isVoted = true)
                                    else opt
                                }
                                poll.copy(
                                    options = updatedOptions,
                                    totalVotes = poll.totalVotes + 1,
                                    userVotedOptionId = poll.options.getOrNull(optionIndex)?.id,
                                )
                            }
                            val updatedElements = story.interactiveElements.map { elem ->
                                if ((elem.id == elementId || elem.type == "poll") && elem.poll != null) {
                                    val poll = elem.poll
                                    val updatedOptions = poll.options.mapIndexed { idx, opt ->
                                        if (idx == optionIndex) opt.copy(votesCount = opt.votesCount + 1, isVoted = true)
                                        else opt
                                    }
                                    elem.copy(
                                        poll = poll.copy(
                                            options = updatedOptions,
                                            totalVotes = poll.totalVotes + 1,
                                            userVotedOptionId = poll.options.getOrNull(optionIndex)?.id,
                                        ),
                                    )
                                } else elem
                            }
                            story.copy(
                                poll = updatedPoll ?: story.poll,
                                interactiveElements = updatedElements,
                            )
                        } else story
                    },
                )
            }
        }
        try {
            api.votePoll(storyId, elementId, StoryVoteRequestDto(optionIndex))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun votePoll(storyId: String, optionId: String): Result<Unit> =
        votePoll(storyId, elementId = "poll", optionIndex = optionId.toIntOrNull() ?: 0)

    suspend fun reportStory(storyId: String, reason: String, details: String = ""): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            api.reportStory(storyId, StoryReportRequestDto(reason = reason, details = details))
        }
    }

    suspend fun getReplyAccess(storyId: String): Result<StoryReplyAccessDto> = withContext(ioDispatcher) {
        runCatching {
            api.getReplyAccess(storyId)
        }
    }

    suspend fun answerQuestion(storyId: String, elementId: String, answer: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            api.answerQuestion(storyId, elementId, StoryQuestionAnswerRequestDto(answer))
        }
    }

    suspend fun getQuestionAnswers(storyId: String): Result<List<StoryQuestionAnswerDto>> = withContext(ioDispatcher) {
        runCatching {
            api.getQuestionAnswers(storyId).answers
        }
    }

    suspend fun getStorySettings(): Result<StorySettingsDto> = withContext(ioDispatcher) {
        runCatching {
            api.getStorySettings()
        }
    }

    suspend fun updateStorySettings(replyPermission: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            api.updateStorySettings(StorySettingsDto(replyPermission))
        }
    }

    suspend fun getUserHighlights(userId: String): Result<List<HighlightDto>> = withContext(ioDispatcher) {
        runCatching {
            api.getUserHighlights(userId).highlights.map { hl ->
                hl.copy(stories = sortStoriesChronologically(hl.stories))
            }
        }
    }

    suspend fun getHighlightCount(userId: String): Result<Int> = withContext(ioDispatcher) {
        runCatching {
            api.getHighlightCount(userId).count
        }
    }

    suspend fun createHighlight(title: String, coverUrl: String?, storyIds: List<String>): Result<HighlightDto> = withContext(ioDispatcher) {
        runCatching {
            api.createHighlight(CreateHighlightRequestDto(title = title, coverUrl = coverUrl, storyIds = storyIds))
        }
    }

    suspend fun updateHighlight(
        highlightId: String,
        title: String? = null,
        coverUrl: String? = null,
        storyIds: List<String>? = null,
    ): Result<HighlightDto> = withContext(ioDispatcher) {
        runCatching {
            api.updateHighlight(
                highlightId = highlightId,
                request = UpdateHighlightRequestDto(title = title, coverUrl = coverUrl, storyIds = storyIds),
            )
        }
    }

    suspend fun deleteHighlight(highlightId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            api.deleteHighlight(highlightId)
        }
    }

    suspend fun getStoryById(storyId: String): Result<Story> = withContext(ioDispatcher) {
        runCatching {
            api.getStoryById(storyId)
        }
    }

    suspend fun getStoryViews(storyId: String, limit: Int = 50, offset: Int = 0): Result<List<StoryView>> = withContext(ioDispatcher) {
        try {
            val response = api.getStoryViews(storyId, limit, offset)
            Result.success(response.toDomainViews())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun sortStoriesChronologically(stories: List<Story>): List<Story> =
            stories.sortedWith(
                compareBy<Story> { story ->
                    runCatching { Instant.parse(story.createdAt).toEpochMilli() }.getOrDefault(0L)
                }.thenBy { it.id },
            )

        fun normalizeAndSortStoryUsers(
            users: List<StoryUser>,
            currentUserId: String = "",
        ): List<StoryUser> =
            users
                .filter { it.stories.isNotEmpty() }
                .map { user ->
                    user.copy(stories = sortStoriesChronologically(user.stories))
                }
                .sortedWith(
                    compareByDescending<StoryUser> { user ->
                        currentUserId.isNotBlank() && user.id == currentUserId
                    }
                        .thenByDescending { it.hasUnseenStories }
                        .thenByDescending { user ->
                            runCatching {
                                user.lastStoryAt?.let(Instant::parse)?.toEpochMilli()
                                    ?: user.stories.maxOfOrNull { Instant.parse(it.createdAt).toEpochMilli() }
                            }.getOrNull() ?: 0L
                        },
                )
    }
}
