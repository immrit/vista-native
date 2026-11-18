package ir.coffevista.vista_native.features.profile.settings.pricing

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
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRequestingPayment: Boolean = false,
    val isVerifyingPayment: Boolean = false,
    val pendingBazaarVerification: BazaarVerifyRequestDto? = null,
    val pendingConsumeToken: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class PricingViewModel @Inject constructor(
    private val api: PricingPlansApi,
    private val authenticationStateProvider: AuthenticationStateProvider,
    private val ownProfileRepository: OwnProfileRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PricingUiState())
    val uiState: StateFlow<PricingUiState> = mutableUiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        mutableUiState.value = mutableUiState.value.copy(isLoading = true, errorMessage = null)
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

    fun onBazaarPurchaseStarted() {
        mutableUiState.update {
            it.copy(
                isRequestingPayment = true,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onBazaarPurchaseCanceled() {
        mutableUiState.update {
            it.copy(
                isRequestingPayment = false,
                errorMessage = "پرداخت توسط کاربر لغو شد.",
            )
        }
    }

    fun onBazaarPurchaseFailed(reason: String?) {
        val message = reason?.takeIf { it.isNotBlank() }
            ?: "پرداخت از طریق درگاه کافه‌بازار ناموفق بود. دوباره تلاش کنید."
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
        if (purchaseToken.isBlank() || productId.isBlank()) {
            onBazaarPurchaseFailed("اطلاعات تراکنش بازار ناقص است.")
            return
        }
        val request = BazaarVerifyRequestDto(
            purchaseToken = purchaseToken,
            productId = productId,
            packageName = packageName,
        )
        verifyBazaarRequest(request)
    }

    fun retryBazaarVerification() {
        val pending = mutableUiState.value.pendingBazaarVerification ?: return
        if (mutableUiState.value.isVerifyingPayment) return
        verifyBazaarRequest(pending)
    }

    fun onPurchaseConsumed() {
        mutableUiState.update { it.copy(pendingConsumeToken = null) }
    }

    private fun verifyBazaarRequest(request: BazaarVerifyRequestDto) {
        mutableUiState.update {
            it.copy(
                isRequestingPayment = false,
                isVerifyingPayment = true,
                pendingBazaarVerification = request,
                errorMessage = null,
                successMessage = null,
            )
        }
        viewModelScope.launch {
            val response = runCatching { api.verifyBazaar(request) }.getOrNull()
            val body = response?.takeIf { it.isSuccessful }?.body()
            if (body?.success != true) {
                mutableUiState.update {
                    it.copy(
                        isVerifyingPayment = false,
                        errorMessage = body?.message ?: "تایید خرید در سرور ناموفق بود. لطفاً دوباره تلاش کنید.",
                    )
                }
                return@launch
            }
            val accountId = (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId
            if (!accountId.isNullOrBlank()) {
                runCatching { ownProfileRepository.fetchAndCacheOwnProfile(accountId) }
            }
            val isCoinPurchase = request.productId.startsWith("coins_")
            mutableUiState.update {
                it.copy(
                    isVerifyingPayment = false,
                    pendingBazaarVerification = null,
                    pendingConsumeToken = if (isCoinPurchase) request.purchaseToken else it.pendingConsumeToken,
                    successMessage = body.message ?: if (isCoinPurchase) {
                        "بسته سکه با موفقیت به حساب شما اضافه شد."
                    } else {
                        "اشتراک ویژه با موفقیت فعال شد!"
                    },
                )
            }
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
}
