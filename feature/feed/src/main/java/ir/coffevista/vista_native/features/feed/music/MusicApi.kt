package ir.coffevista.vista_native.features.feed.music

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicApi {
    @GET("v1/music")
    suspend fun fetchMusics(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): Response<MusicListResponseDto>

    @GET("v1/music")
    suspend fun fetchFilteredMusics(
        @Query("user_id") userId: String? = null,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
    ): Response<MusicListResponseDto> = fetchMusics(limit = limit, offset = offset)

    @POST("v1/music")
    suspend fun publishMusic(
        @Body request: PublishMusicRequestDto,
    ): Response<MusicDto>

    @POST("v1/music/{id}/play")
    suspend fun incrementPlayCount(
        @Path("id") musicId: String,
    ): Response<Unit>
}
