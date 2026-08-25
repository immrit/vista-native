package ir.coffevista.vista_native.features.services.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ServicesApi {
    @GET("v1/services-hub")
    suspend fun getHub(): Response<ServicesHubResponseDto>

    @POST("v1/game-sso/ticket")
    suspend fun createGameSsoTicket(): Response<GameSsoTicketResponseDto>

    @POST("v1/services-hub/contacts")
    suspend fun findContacts(
        @Body body: FindContactsRequestDto,
    ): Response<FindContactsResponseDto>

    @GET("v1/services-hub/top-groups")
    suspend fun getTopGroups(): Response<TopGroupsResponseDto>
}
