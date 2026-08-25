package ir.coffevista.vista_native.features.services.data.nearby

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NearbyApi {
    @POST("/v1/nearby/location")
    suspend fun updateLocation(
        @Body request: LocationUpdateRequestDto,
    ): Response<Unit>

    @DELETE("/v1/nearby/location")
    suspend fun disableLocation(): Response<Unit>

    @GET("/v1/nearby/preferences")
    suspend fun getPreferences(): Response<NearbyPreferencesDto>

    @PUT("/v1/nearby/preferences")
    suspend fun updatePreferences(
        @Body request: NearbyPreferencesUpdateRequestDto,
    ): Response<NearbyPreferencesDto>

    @GET("/v1/nearby/discover")
    suspend fun discover(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): Response<NearbyDiscoverResponseDto>

    @GET("/v1/nearby/random-online")
    suspend fun discoverRandomOnline(
        @Query("limit") limit: Int = 20,
    ): Response<NearbyDiscoverResponseDto>

    @POST("/v1/nearby/like")
    suspend fun like(
        @Body request: NearbyLikeRequestDto,
    ): Response<NearbyLikeResponseDto>

    @GET("/v1/nearby/matches")
    suspend fun getMatches(): Response<NearbyMatchesResponseDto>

    @DELETE("/v1/nearby/matches/{matchId}")
    suspend fun unmatch(
        @Path("matchId") matchId: String,
    ): Response<Unit>

    @POST("/v1/nearby/matches/{matchId}/chat")
    suspend fun openChat(
        @Path("matchId") matchId: String,
    ): Response<NearbyOpenChatResponseDto>

    @GET("/v1/nearby/likes-received")
    suspend fun getLikesReceived(
        @Query("limit") limit: Int = 50,
    ): Response<NearbyReceivedLikesResponseDto>

    @POST("/v1/nearby/report")
    suspend fun report(
        @Body request: NearbyReportRequestDto,
    ): Response<Unit>
}
