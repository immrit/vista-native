package ir.coffevista.vista_native.features.profile.settings.about

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Matches Flutter ContactUsScreen's authenticated-when-available contact form. */
interface ContactRequestsApi {
    @POST("v1/contact-requests")
    suspend fun submit(@Body request: ContactRequestDto): Response<Unit>
}

@Serializable
data class ContactRequestDto(
    @SerialName("full_name") val fullName: String,
    val email: String,
    val subject: String,
    val message: String,
    val category: String = "contact",
)
