package ir.coffevista.vista_native.features.services.data

import ir.coffevista.vista_native.core.network.BackendErrorParser
import ir.coffevista.vista_native.core.network.RemoteFailure
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

interface ServicesRepository {
    suspend fun getHub(): ServicesHubData
    suspend fun createGameSsoTicket(): String
    suspend fun findContacts(phoneNumbers: List<String>): List<ContactVistaUser>
    suspend fun getTopGroups(): List<TopGroup>
}

class DefaultServicesRepository(
    private val api: ServicesApi,
) : ServicesRepository {

    override suspend fun getHub(): ServicesHubData {
        val response = api.getHub().bodyOrThrow("services_hub_empty")
        return response.toDomain()
    }

    override suspend fun createGameSsoTicket(): String {
        val response = api.createGameSsoTicket().bodyOrThrow("game_ticket_empty")
        val ticket = response.ticket?.trim().orEmpty()
        if (ticket.isEmpty()) {
            throw RemoteFailure(
                statusCode = 200,
                code = "game_ticket_empty",
                message = "Failed to create game session",
            )
        }
        return ticket
    }

    override suspend fun findContacts(phoneNumbers: List<String>): List<ContactVistaUser> {
        if (phoneNumbers.isEmpty()) return emptyList()
        val response = api.findContacts(FindContactsRequestDto(phoneNumbers))
            .bodyOrThrow("contacts_empty")
        return response.users.map { it.toDomain() }
    }

    override suspend fun getTopGroups(): List<TopGroup> {
        val response = api.getTopGroups().bodyOrThrow("top_groups_empty")
        return response.groups.map { it.toDomain() }
    }

    private fun <T> Response<T>.bodyOrThrow(emptyCode: String): T {
        if (isSuccessful) {
            return body() ?: throw RemoteFailure(
                statusCode = code(),
                code = emptyCode,
                message = "پاسخ سرور خالی بود",
            )
        }
        val parsed = BackendErrorParser.parse(
            rawBody = errorBody()?.string().orEmpty(),
            retryAfterHeader = headers()["Retry-After"],
        )
        throw RemoteFailure(
            statusCode = code(),
            code = parsed.code,
            message = parsed.message,
            retryAfterSeconds = parsed.retryAfterSeconds,
        )
    }
}
