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

    @POST("v1/payment/bazaar-verify")
    suspend fun verifyBazaar(@Body request: BazaarVerifyRequestDto): Response<BazaarVerifyResponseDto>

    @GET("v1/payment/packages")
    suspend fun getCoinPackages(): Response<CoinPackagesResponseDto> = Response.success(CoinPackagesResponseDto())
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

@Serializable
data class BazaarVerifyRequestDto(
    @SerialName("purchase_token") val purchaseToken: String,
    @SerialName("product_id") val productId: String,
    @SerialName("package_name") val packageName: String,
)

@Serializable
data class BazaarVerifyResponseDto(
    val success: Boolean = false,
    val message: String? = null,
    val plan: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("days_added") val daysAdded: Int? = null,
)

@Serializable
data class CoinPackageDto(
    val id: String,
    val coins: Long,
    val bonus: Long = 0,
    @SerialName("priceToman") val priceToman: Long = 0,
    @SerialName("amountRial") val amountRial: Long = 0,
)

@Serializable
data class CoinPackagesResponseDto(
    val packages: List<CoinPackageDto> = emptyList(),
)

data class VerificationBadgeUiModel(
    val id: String,
    val title: String,
    val description: String,
    val priceToman: Long,
    val priceFormatted: String,
    val badgeType: String,
    val zarinpalUrl: String,
)
