package ir.coffevista.vista_native.features.profile.settings.pricing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

/** Read-only catalogue used by Flutter before allowing a premium purchase. */
interface PricingPlansApi {
    @GET("v1/payment/subscription-plans")
    suspend fun getPlans(): Response<PricingPlansResponseDto>

    @POST("v1/payment/zibal/request")
    suspend fun requestZibal(@Body request: ZibalRequestDto): Response<ZibalRequestResponseDto>

    @POST("v1/payment/zibal/verify")
    suspend fun verifyZibal(@Body request: ZibalVerifyRequestDto): Response<ZibalVerifyResponseDto>
}

@Serializable
data class PricingPlansResponseDto(
    val plans: List<PricingPlanDto> = emptyList(),
)

@Serializable
data class PricingPlanDto(
    @SerialName("plan_type") val planType: String,
    @SerialName("price_toman") val priceToman: Long,
)

@Serializable
data class ZibalRequestDto(
    @SerialName("package_id") val packageId: String,
    @SerialName("callback_url") val callbackUrl: String,
)

@Serializable
data class ZibalRequestResponseDto(
    @SerialName("track_id") val trackId: Long,
    val url: String,
)

@Serializable
data class ZibalVerifyRequestDto(
    @SerialName("track_id") val trackId: Long,
)

@Serializable
data class ZibalVerifyResponseDto(
    val success: Boolean = false,
    val message: String? = null,
)
