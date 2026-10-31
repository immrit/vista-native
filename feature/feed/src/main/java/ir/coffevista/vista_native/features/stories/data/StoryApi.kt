package ir.coffevista.vista_native.features.stories.data

import ir.coffevista.vista_native.features.stories.domain.ActiveStoriesResponseDto
import ir.coffevista.vista_native.features.stories.domain.CreateHighlightRequestDto
import ir.coffevista.vista_native.features.stories.domain.CreateStoryRequestDto
import ir.coffevista.vista_native.features.stories.domain.HighlightCountResponseDto
import ir.coffevista.vista_native.features.stories.domain.HighlightDto
import ir.coffevista.vista_native.features.stories.domain.Story
import ir.coffevista.vista_native.features.stories.domain.StoryPlacesResponseDto
import ir.coffevista.vista_native.features.stories.domain.StoryQuestionAnswerRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryQuestionAnswersResponseDto
import ir.coffevista.vista_native.features.stories.domain.StoryReactRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryReplyAccessDto
import ir.coffevista.vista_native.features.stories.domain.StoryReplyRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryReportRequestDto
import ir.coffevista.vista_native.features.stories.domain.StorySettingsDto
import ir.coffevista.vista_native.features.stories.domain.StoryViewsResponseDto
import ir.coffevista.vista_native.features.stories.domain.StoryVoteRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryWeatherContextDto
import ir.coffevista.vista_native.features.stories.domain.UpdateHighlightRequestDto
import ir.coffevista.vista_native.features.stories.domain.UserHighlightsResponseDto
import ir.coffevista.vista_native.features.stories.domain.UserStoriesResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface StoryApi {
    @GET("v1/stories/active")
    suspend fun getActiveStories(): ActiveStoriesResponseDto

    @GET("v1/stories/users/{userId}")
    suspend fun getUserStories(
        @Path("userId") userId: String,
    ): UserStoriesResponseDto

    @GET("v1/profiles/following/{userId}")
    suspend fun getFollowingUsers(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
    ): ir.coffevista.vista_native.features.stories.domain.FollowingStoryUsersResponseDto

    @GET("v1/me/close-friends")
    suspend fun getCloseFriends(): List<String>

    @PUT("v1/me/close-friends")
    suspend fun updateCloseFriends(
        @Body request: ir.coffevista.vista_native.features.stories.domain.CloseFriendsUpdateRequestDto,
    )

    @GET("v1/stories/{storyId}")
    suspend fun getStoryById(
        @Path("storyId") storyId: String,
    ): Story

    @POST("v1/stories")
    suspend fun createStory(
        @Body request: CreateStoryRequestDto,
    ): Story

    @DELETE("v1/stories/{storyId}")
    suspend fun deleteStory(
        @Path("storyId") storyId: String,
    )

    @POST("v1/stories/{storyId}/view")
    suspend fun trackView(
        @Path("storyId") storyId: String,
    )

    @GET("v1/stories/{storyId}/views")
    suspend fun getStoryViews(
        @Path("storyId") storyId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): StoryViewsResponseDto

    @POST("v1/stories/{storyId}/reaction")
    suspend fun reactToStory(
        @Path("storyId") storyId: String,
        @Body request: StoryReactRequestDto,
    )

    @DELETE("v1/stories/{storyId}/reaction")
    suspend fun removeStoryReaction(
        @Path("storyId") storyId: String,
    ) {}

    @POST("v1/stories/{storyId}/reply")
    suspend fun replyToStory(
        @Path("storyId") storyId: String,
        @Body request: StoryReplyRequestDto,
    )

    @POST("v1/stories/{storyId}/poll/{elementId}/vote")
    suspend fun votePoll(
        @Path("storyId") storyId: String,
        @Path("elementId") elementId: String,
        @Body request: StoryVoteRequestDto,
    )

    @POST("v1/stories/{storyId}/report")
    suspend fun reportStory(
        @Path("storyId") storyId: String,
        @Body request: StoryReportRequestDto,
    ) {}

    @GET("v1/stories/{storyId}/reply-access")
    suspend fun getReplyAccess(
        @Path("storyId") storyId: String,
    ): StoryReplyAccessDto = StoryReplyAccessDto()

    @POST("v1/stories/{storyId}/question/{elementId}/answer")
    suspend fun answerQuestion(
        @Path("storyId") storyId: String,
        @Path("elementId") elementId: String,
        @Body request: StoryQuestionAnswerRequestDto,
    ) {}

    @GET("v1/stories/{storyId}/questions/answers")
    suspend fun getQuestionAnswers(
        @Path("storyId") storyId: String,
    ): StoryQuestionAnswersResponseDto = StoryQuestionAnswersResponseDto()

    @GET("v1/me/story-settings")
    suspend fun getStorySettings(): StorySettingsDto = StorySettingsDto()

    @PATCH("v1/me/story-settings")
    suspend fun updateStorySettings(
        @Body request: StorySettingsDto,
    ) {}

    @GET("v1/highlights/users/{userId}")
    suspend fun getUserHighlights(
        @Path("userId") userId: String,
    ): UserHighlightsResponseDto = UserHighlightsResponseDto()

    @GET("v1/highlights/users/{userId}/count")
    suspend fun getHighlightCount(
        @Path("userId") userId: String,
    ): HighlightCountResponseDto = HighlightCountResponseDto()

    @POST("v1/highlights")
    suspend fun createHighlight(
        @Body request: CreateHighlightRequestDto,
    ): HighlightDto = HighlightDto(id = "new_hl", title = request.title)

    @PATCH("v1/highlights/{highlightId}")
    suspend fun updateHighlight(
        @Path("highlightId") highlightId: String,
        @Body request: UpdateHighlightRequestDto,
    ): HighlightDto = HighlightDto(id = highlightId, title = request.title.orEmpty())

    @DELETE("v1/highlights/{highlightId}")
    suspend fun deleteHighlight(
        @Path("highlightId") highlightId: String,
    ) {}

    @GET("v1/stories/context/weather")
    suspend fun getStoryWeatherContext(
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("city") city: String? = null,
    ): StoryWeatherContextDto = StoryWeatherContextDto()

    @GET("v1/stories/context/places")
    suspend fun getStoryPlacesContext(
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("q") query: String? = null,
    ): StoryPlacesResponseDto = StoryPlacesResponseDto()
}
