package ir.coffevista.vista_native.features.profile.settings.pricing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PricingPlansApi {
    @GET("v1/payment/subscription-plans")
    suspend fun getPlans(): Response<PricingPlansResponseDto>

    @POST("v1/payment/bazaar-verify")
    suspend fun verifyBazaar(@Body request: BazaarVerifyRequestDto): Response<BazaarVerifyResponseDto>

    @GET("v1/payment/packages")
    suspend fun getCoinPackages(): Response<CoinPackagesResponseDto>
}

@Serializable
data class PricingPlansResponseDto(
    val plans: List<PricingPlanDto> = emptyList(),
    @SerialName("premium_features") val premiumFeatures: List<String> = emptyList(),
)

@Serializable
data class PricingPlanMetaDto(
    val enabled: Boolean = true,
    @SerialName("original_price_toman") val originalPriceToman: Long = 0L,
    @SerialName("badge_text") val badgeText: String = "",
    val recommended: Boolean = false,
)

@Serializable
data class PricingPlanDto(
    @SerialName("plan_type") val planType: String,
    @SerialName("price_toman") val priceToman: Long,
    val meta: PricingPlanMetaDto? = null,
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
    val type: String? = null,
    val plan: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("days_added") val daysAdded: Int? = null,
    @SerialName("coins_added") val coinsAdded: Long? = null,
    @SerialName("new_balance") val newBalance: Long? = null,
    @SerialName("already_processed") val alreadyProcessed: Boolean = false,
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
