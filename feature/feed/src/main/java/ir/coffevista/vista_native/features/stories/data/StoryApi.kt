package ir.coffevista.vista_native.features.stories.data

import ir.coffevista.vista_native.features.stories.domain.ActiveStoriesResponseDto
import ir.coffevista.vista_native.features.stories.domain.CreateStoryRequestDto
import ir.coffevista.vista_native.features.stories.domain.Story
import ir.coffevista.vista_native.features.stories.domain.StoryReactRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryReplyRequestDto
import ir.coffevista.vista_native.features.stories.domain.StoryViewsResponseDto
import ir.coffevista.vista_native.features.stories.domain.StoryVoteRequestDto
import ir.coffevista.vista_native.features.stories.domain.UserStoriesResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StoryApi {
    @GET("v1/stories/active")
    suspend fun getActiveStories(): ActiveStoriesResponseDto

    @GET("v1/stories/users/{userId}")
    suspend fun getUserStories(
        @Path("userId") userId: String,
    ): UserStoriesResponseDto

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

    @POST("v1/stories/{storyId}/react")
    suspend fun reactToStory(
        @Path("storyId") storyId: String,
        @Body request: StoryReactRequestDto,
    )

    @POST("v1/stories/{storyId}/reply")
    suspend fun replyToStory(
        @Path("storyId") storyId: String,
        @Body request: StoryReplyRequestDto,
    )

    @POST("v1/stories/{storyId}/poll/vote")
    suspend fun votePoll(
        @Path("storyId") storyId: String,
        @Body request: StoryVoteRequestDto,
    )
}
