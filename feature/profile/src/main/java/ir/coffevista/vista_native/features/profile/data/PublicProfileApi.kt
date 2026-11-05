package ir.coffevista.vista_native.features.profile.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PublicProfileApi {
    @GET("v1/profiles/{userId}")
    suspend fun fetchPublicProfile(
        @Path("userId") userId: String,
    ): Response<PublicProfileDto>

    @GET("v1/profiles/by-username/{username}")
    suspend fun fetchProfileByUsername(
        @Path("username") username: String,
    ): Response<PublicProfileDto>

    @POST("v1/me/follow")
    suspend fun follow(
        @Body request: FollowActionRequestDto,
    ): Response<FollowActionResponseDto>

    @POST("v1/me/unfollow")
    suspend fun unfollow(
        @Body request: FollowActionRequestDto,
    ): Response<UnfollowResponseDto>

    @GET("v1/profiles/followers/{userId}")
    suspend fun getFollowers(
        @Path("userId") userId: String,
        @retrofit2.http.Query("limit") limit: Int = 30,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): Response<FollowListResponseDto>

    @GET("v1/profiles/following/{userId}")
    suspend fun getFollowing(
        @Path("userId") userId: String,
        @retrofit2.http.Query("limit") limit: Int = 30,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): Response<FollowListResponseDto>

    @GET("v1/highlights/users/{userId}")
    suspend fun getUserHighlights(
        @Path("userId") userId: String,
    ): Response<ProfileHighlightsResponseDto>

    @POST("v1/highlights")
    suspend fun createHighlight(
        @Body request: CreateProfileHighlightRequestDto,
    ): Response<ProfileHighlightDto>

    @retrofit2.http.PATCH("v1/highlights/{highlightId}")
    suspend fun updateHighlight(
        @Path("highlightId") highlightId: String,
        @Body request: UpdateProfileHighlightRequestDto,
    ): Response<ProfileHighlightDto>

    @retrofit2.http.DELETE("v1/highlights/{highlightId}")
    suspend fun deleteHighlight(
        @Path("highlightId") highlightId: String,
    ): Response<Unit>

    @GET("v1/me/note")
    suspend fun getOwnNote(): Response<ProfileNoteDto?>

    @POST("v1/me/note")
    suspend fun upsertNote(
        @Body request: UpsertNoteRequestDto,
    ): Response<ProfileNoteDto>

    @retrofit2.http.DELETE("v1/me/note")
    suspend fun deleteNote(): Response<Unit>

    @POST("v1/profiles/notes/batch")
    suspend fun getBatchNotes(
        @Body request: BatchNotesRequestDto,
    ): Response<Map<String, ProfileNoteDto>>

    @POST("v1/profiles/report")
    suspend fun reportProfile(
        @Body request: ReportProfileRequestDto,
    ): Response<Unit>
}
