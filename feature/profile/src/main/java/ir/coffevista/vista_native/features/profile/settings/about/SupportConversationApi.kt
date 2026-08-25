package ir.coffevista.vista_native.features.profile.settings.about

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.POST

/** Creates or returns the authenticated support conversation used by Flutter. */
interface SupportConversationApi {
    @POST("v1/chat/support/conversation")
    suspend fun open(): Response<SupportConversationDto>
}

@Serializable
data class SupportConversationDto(
    val id: String = "",
    val name: String = "",
    @SerialName("peer_id") val peerId: String = "",
    val image: String? = null,
)
