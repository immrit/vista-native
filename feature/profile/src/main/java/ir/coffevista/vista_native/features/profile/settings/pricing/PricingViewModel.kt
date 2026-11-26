package ir.coffevista.vista_native.features.profile.settings.pricing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.core.network.SystemGateMonitor
import ir.coffevista.vista_native.features.profile.data.OwnProfileRepository
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PricingUiState(
    val plans: List<PricingPlanUiModel> = emptyList(),
    val coinPackages: List<CoinPackageDto> = emptyList(),
    val remotePremiumFeatures: List<String> = emptyList(),
    val storeChannel: StoreChannel = StoreChannel.Bazaar,
    val remotePaymentGateway: String = "store",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRequestingPayment: Boolean = false,
    val isVerifyingPayment: Boolean = false,
    val pendingStoreVerification: Pair<StoreChannel, BazaarVerifyRequestDto>? = null,
    val pendingConsumeToken: String? = null,
    val pendingConsumeChannel: StoreChannel? = null,
    val pendingZibalCheckoutUrl: String? = null,
    val pendingZibalTrackId: Long? = null,
    val successMessage: String? = null,
) {
    /** Compatibility helper for callers inspecting pending verification payload. */
    val pendingBazaarVerification: BazaarVerifyRequestDto?
        get() = pendingStoreVerification?.second

    val activeGateway: String
        get() = if (remotePaymentGateway == "zibal" || storeChannel == StoreChannel.Direct) {
            "zibal"
        } else {
            storeChannel.wireName
        }
}

@HiltViewModel
class PricingViewModel @Inject constructor(
    private val api: PricingPlansApi,
    private val authenticationStateProvider: AuthenticationStateProvider,
    private val ownProfileRepository: OwnProfileRepository,
    val storeBillingConfig: StoreBillingConfig = StoreBillingConfig(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        PricingUiState(
            storeChannel = storeBillingConfig.channel,
            remotePaymentGateway = SystemGateMonitor.remoteConfig.value?.paymentGateway ?: "store",
        ),
    )
    val uiState: StateFlow<PricingUiState> = mutableUiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = true,
            errorMessage = null,
            storeChannel = storeBillingConfig.channel,
            remotePaymentGateway = SystemGateMonitor.remoteConfig.value?.paymentGateway ?: "store",
        )
        viewModelScope.launch {
            val plansDeferred = runCatching { api.getPlans() }
            val packagesDeferred = runCatching { api.getCoinPackages() }

            val plansBody = plansDeferred.getOrNull()
                ?.takeIf { it.isSuccessful }
                ?.body()

            val remoteCfg = SystemGateMonitor.remoteConfig.value
            val plans = plansBody?.plans
                .orEmpty()
                .filter { it.priceToman > 0L }
                .mapNotNull(::toUiPlan)

            val dynamicFeatures = plansBody?.premiumFeatures
                ?.takeIf { it.isNotEmpty() }
                ?: remoteCfg?.premiumFeatures.orEmpty()

            val packages = packagesDeferred.getOrNull()
                ?.takeIf { it.isSuccessful }
                ?.body()
                ?.packages
                .orEmpty()
                .ifEmpty {
                    listOf(
                        CoinPackageDto(id = "coins_1000", coins = 1000, bonus = 0, priceToman = 10000, amountRial = 100000),
                        CoinPackageDto(id = "coins_5000", coins = 5000, bonus = 500, priceToman = 45000, amountRial = 450000),
                        CoinPackageDto(id = "coins_12000", coins = 12000, bonus = 2000, priceToman = 90000, amountRial = 900000),
                    )
                }

            mutableUiState.update {
                it.copy(
                    plans = plans,
                    coinPackages = packages,
                    remotePremiumFeatures = dynamicFeatures,
                    remotePaymentGateway = remoteCfg?.paymentGateway ?: "store",
                    isLoading = false,
                    errorMessage = if (plans.isEmpty() && packages.isEmpty()) "کاتالوگ در دسترس نیست. دوباره تلاش کنید" else null,
                )
            }
        }
    }

    fun resolveBazaarPayload(): String {
        val userId = (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId.orEmpty()
        return "user_$userId"
    }

    fun onStorePurchaseStarted() {
        mutableUiState.update {
            it.copy(
                isRequestingPayment = true,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onBazaarPurchaseStarted() = onStorePurchaseStarted()

    fun onBazaarPurchaseCanceled() {
        mutableUiState.update {
            it.copy(
                isRequestingPayment = false,
                errorMessage = "پرداخت توسط کاربر لغو شد.",
            )
        }
    }

    fun onBazaarPurchaseFailed(reason: String?) {
        val storeName = if (storeBillingConfig.channel == StoreChannel.Myket) "مایکت" else "کافه‌بازار"
        val message = reason?.takeIf { it.isNotBlank() }
            ?: "پرداخت از طریق درگاه $storeName ناموفق بود. دوباره تلاش کنید."
        mutableUiState.update {
            it.copy(
                isRequestingPayment = false,
                errorMessage = message,
            )
        }
    }

    fun verifyBazaarPurchase(
        productId: String,
        purchaseToken: String,
        packageName: String,
    ) {
        verifyStorePurchase(StoreChannel.Bazaar, productId, purchaseToken, packageName)
    }

    fun verifyMyketPurchase(
        productId: String,
        purchaseToken: String,
        packageName: String,
    ) {
        verifyStorePurchase(StoreChannel.Myket, productId, purchaseToken, packageName)
    }

    private fun verifyStorePurchase(
        channel: StoreChannel,
        productId: String,
        purchaseToken: String,
        packageName: String,
    ) {
        if (purchaseToken.isBlank() || productId.isBlank()) {
            onBazaarPurchaseFailed("اطلاعات تراکنش فروشگاه ناقص است.")
            return
        }
        val request = BazaarVerifyRequestDto(
            purchaseToken = purchaseToken,
            productId = productId,
            packageName = packageName,
        )
        verifyStoreRequest(channel, request)
    }

    fun retryBazaarVerification() {
        val pending = mutableUiState.value.pendingStoreVerification ?: return
        if (mutableUiState.value.isVerifyingPayment) return
        verifyStoreRequest(pending.first, pending.second)
    }

    fun onPurchaseConsumed() {
        mutableUiState.update { it.copy(pendingConsumeToken = null, pendingConsumeChannel = null) }
    }

    fun consumeZibalCheckoutUrl() {
        mutableUiState.update { it.copy(pendingZibalCheckoutUrl = null) }
    }

    fun startZibalPurchase(
        packageId: String,
        callbackUrl: String = ZIBAL_MOBILE_CALLBACK_URL,
    ) {
        if (packageId.isBlank()) return
        mutableUiState.update {
            it.copy(
                isRequestingPayment = true,
                errorMessage = null,
                successMessage = null,
                pendingZibalCheckoutUrl = null,
            )
        }
        viewModelScope.launch {
            val response = runCatching {
                api.requestZibal(ZibalRequestDto(packageId = packageId, callbackUrl = callbackUrl))
            }.getOrNull()
            val body = response?.takeIf { it.isSuccessful }?.body()
            if (body == null || body.url.isBlank() || body.trackId <= 0L) {
                mutableUiState.update {
                    it.copy(
                        isRequestingPayment = false,
                        errorMessage = "اتصال به درگاه پرداخت آنلاین برقرار نشد. لطفاً دوباره تلاش کنید.",
                    )
                }
                return@launch
            }
            mutableUiState.update {
                it.copy(
                    isRequestingPayment = false,
                    pendingZibalCheckoutUrl = body.url,
                    pendingZibalTrackId = body.trackId,
                )
            }
        }
    }

    fun handlePaymentCallbackQuery(rawQuery: String?) {
        val query = rawQuery?.trim().orEmpty()
        if (query.isBlank()) return
        val parsed = runCatching { Uri.parse("vista://payment/callback?$query") }.getOrNull() ?: return
        val successParam = parsed.getQueryParameter("success")
        val trackId = parsed.getQueryParameter("trackId")?.toLongOrNull()
            ?: parsed.getQueryParameter("track_id")?.toLongOrNull()
            ?: mutableUiState.value.pendingZibalTrackId
            ?: 0L

        if (successParam == "0") {
            mutableUiState.update {
                it.copy(
                    isRequestingPayment = false,
                    isVerifyingPayment = false,
                    pendingZibalTrackId = null,
                    errorMessage = "پرداخت توسط کاربر لغو شد.",
                )
            }
            return
        }
        if (trackId > 0L) {
            verifyZibalTrackId(trackId)
        }
    }

    fun verifyZibalTrackId(trackId: Long) {
        if (trackId <= 0L || mutableUiState.value.isVerifyingPayment) return
        mutableUiState.update {
            it.copy(
                isRequestingPayment = false,
                isVerifyingPayment = true,
                errorMessage = null,
                successMessage = null,
            )
        }
        viewModelScope.launch {
            val response = runCatching { api.verifyZibal(ZibalVerifyRequestDto(trackId)) }.getOrNull()
            val body = response?.takeIf { it.isSuccessful }?.body()
            if (body?.success != true) {
                mutableUiState.update {
                    it.copy(
                        isVerifyingPayment = false,
                        errorMessage = body?.message ?: "تایید پرداخت ناموفق بود. در صورت کسر وجه مبلغ طی ۷۲ ساعت بازگشت داده می‌شود.",
                    )
                }
                return@launch
            }
            refreshOwnProfileIfSignedIn()
            mutableUiState.update {
                it.copy(
                    isVerifyingPayment = false,
                    pendingZibalTrackId = null,
                    successMessage = body.message ?: "پرداخت با موفقیت انجام و حساب شما ارتقا یافت!",
                )
            }
        }
    }

    private fun verifyStoreRequest(channel: StoreChannel, request: BazaarVerifyRequestDto) {
        mutableUiState.update {
            it.copy(
                isRequestingPayment = false,
                isVerifyingPayment = true,
                pendingStoreVerification = channel to request,
                errorMessage = null,
                successMessage = null,
            )
        }
        viewModelScope.launch {
            val response = runCatching {
                if (channel == StoreChannel.Myket) {
                    api.verifyMyket(request)
                } else {
                    api.verifyBazaar(request)
                }
            }.getOrNull()
            val body = response?.takeIf { it.isSuccessful }?.body()
            if (body?.success != true) {
                val storeLabel = if (channel == StoreChannel.Myket) "مایکت" else "بازار"
                mutableUiState.update {
                    it.copy(
                        isVerifyingPayment = false,
                        errorMessage = body?.message ?: "تایید خرید در سرور ($storeLabel) ناموفق بود. لطفاً دوباره تلاش کنید.",
                    )
                }
                return@launch
            }
            refreshOwnProfileIfSignedIn()
            val isCoinPurchase = request.productId.startsWith("coins_")
            // Per Myket spec, ALL products (including subscription SKUs implemented as consumable in-app)
            // should be consumed once verified on backend; on Bazaar only coin bundles are consumed.
            val shouldConsume = channel == StoreChannel.Myket || isCoinPurchase
            mutableUiState.update {
                it.copy(
                    isVerifyingPayment = false,
                    pendingStoreVerification = null,
                    pendingConsumeToken = if (shouldConsume) request.purchaseToken else it.pendingConsumeToken,
                    pendingConsumeChannel = if (shouldConsume) channel else it.pendingConsumeChannel,
                    successMessage = body.message ?: if (isCoinPurchase) {
                        "بسته سکه با موفقیت به حساب شما اضافه شد."
                    } else {
                        "اشتراک ویژه با موفقیت فعال شد!"
                    },
                )
            }
        }
    }

    private suspend fun refreshOwnProfileIfSignedIn() {
        val accountId = (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId
        if (!accountId.isNullOrBlank()) {
            runCatching { ownProfileRepository.fetchAndCacheOwnProfile(accountId) }
        }
    }

    private fun toUiPlan(plan: PricingPlanDto): PricingPlanUiModel? {
        val remoteMeta = SystemGateMonitor.remoteConfig.value?.pricingMeta?.get(plan.planType)
        val enabled = plan.meta?.enabled ?: remoteMeta?.enabled ?: true
        if (!enabled) return null

        val meta = when (plan.planType) {
            "monthly" -> PlanMeta("vista_premium_monthly", "ماهانه", "انعطاف‌پذیر برای شروع")
            "three_monthly" -> PlanMeta("vista_premium_3monthly", "سه‌ماهه", "سه ماه پریمیوم پیوسته", "پیشنهاد ویستا")
            "yearly" -> PlanMeta("vista_premium_yearly", "سالانه", "یک سال کامل پریمیوم")
            else -> return null
        }
        val formatter = NumberFormat.getIntegerInstance(Locale("fa"))
        val amount = formatter.format(plan.priceToman)
        val originalToman = plan.meta?.originalPriceToman?.takeIf { it > plan.priceToman }
            ?: remoteMeta?.originalPriceToman?.takeIf { it > plan.priceToman }
        val originalFormatted = originalToman?.let { "${formatter.format(it)} تومان" }
        val badgeText = plan.meta?.badgeText?.takeIf { it.isNotBlank() }
            ?: remoteMeta?.badgeText?.takeIf { it.isNotBlank() }
            ?: if (plan.meta?.recommended == true || remoteMeta?.recommended == true) "پیشنهاد ویستا" else meta.badge

        return PricingPlanUiModel(
            id = meta.productId,
            title = meta.title,
            price = "$amount تومان",
            description = meta.description,
            badge = badgeText,
            originalPrice = originalFormatted,
            recommended = plan.meta?.recommended ?: remoteMeta?.recommended ?: (plan.planType == "three_monthly"),
        )
    }

    private data class PlanMeta(
        val productId: String,
        val title: String,
        val description: String,
        val badge: String? = null,
    )

    companion object {
        const val ZIBAL_MOBILE_CALLBACK_URL = "https://cafevista.ir/payment/callback"
    }
}
