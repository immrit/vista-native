package ir.coffevista.vista_native.features.profile.settings.pricing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.profile.data.OwnProfileRepository
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PricingUiState(
    val plans: List<PricingPlanUiModel> = emptyList(),
    val coinPackages: List<CoinPackageDto> = emptyList(),
    val badges: List<VerificationBadgeUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRequestingPayment: Boolean = false,
    val isVerifyingPayment: Boolean = false,
    val paymentUrl: String? = null,
    val pendingTrackId: Long? = null,
    val pendingBazaarVerification: BazaarVerifyRequestDto? = null,
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

            val plans = plansDeferred.getOrNull()
                ?.takeIf { it.isSuccessful }
                ?.body()
                ?.plans
                .orEmpty()
                .filter { it.priceToman > 0L }
                .mapNotNull(::toUiPlan)

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

            val defaultBadges = listOf(
                VerificationBadgeUiModel(
                    id = "badge_gold",
                    title = "نشان تأیید طلایی",
                    description = "با نشان طلایی، حساب کاربری خود را ویژه کنید و از کاربران عادی متمایز شوید. اعتبار نشان به مدت یک ماه می‌باشد.",
                    priceToman = 80000L,
                    priceFormatted = "۸۰٬۰۰۰ تومان",
                    badgeType = "gold",
                    zarinpalUrl = "https://zarinp.al/694791",
                ),
                VerificationBadgeUiModel(
                    id = "badge_black",
                    title = "نشان تأیید مشکی (نخبگان)",
                    description = "نشان مشکی نماد برترین و معتبرترین حساب‌های کاربری در ویستا است. با این نشان، شما در گروه برگزیدگان قرار می‌گیرید.",
                    priceToman = 50000L,
                    priceFormatted = "۵۰٬۰۰۰ تومان",
                    badgeType = "black",
                    zarinpalUrl = "https://zarinp.al/694791",
                ),
            )

            mutableUiState.update {
                it.copy(
                    plans = plans,
                    coinPackages = packages,
                    badges = defaultBadges,
                    isLoading = false,
                    errorMessage = if (plans.isEmpty() && packages.isEmpty()) "کاتالوگ در دسترس نیست. دوباره تلاش کنید" else null,
                )
            }
        }
    }

    fun requestPayment(planId: String) {
        if (mutableUiState.value.isRequestingPayment || planId.isBlank()) return
        mutableUiState.update { it.copy(isRequestingPayment = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = runCatching {
                api.requestZibal(
                    ZibalRequestDto(
                        packageId = planId,
                        callbackUrl = "https://cafevista.ir/payment/callback",
                    ),
                )
            }.getOrNull()
            val body = result?.takeIf { it.isSuccessful }?.body()
            if (body == null || body.trackId <= 0L || !body.url.startsWith("https://")) {
                mutableUiState.update {
                    it.copy(
                        isRequestingPayment = false,
                        errorMessage = "شروع پرداخت ناموفق بود. دوباره تلاش کنید",
                    )
                }
            } else {
                mutableUiState.update {
                    it.copy(
                        isRequestingPayment = false,
                        paymentUrl = body.url,
                        pendingTrackId = body.trackId,
                    )
                }
            }
        }
    }

    fun paymentGatewayLaunched() {
        mutableUiState.update { it.copy(paymentUrl = null) }
    }

    fun verifyPayment() {
        val trackId = mutableUiState.value.pendingTrackId ?: return
        if (mutableUiState.value.isVerifyingPayment) return
        mutableUiState.update { it.copy(isVerifyingPayment = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val body = runCatching { api.verifyZibal(ZibalVerifyRequestDto(trackId)) }
                .getOrNull()
                ?.takeIf { it.isSuccessful }
                ?.body()
            if (body?.success != true) {
                mutableUiState.update {
                    it.copy(
                        isVerifyingPayment = false,
                        errorMessage = body?.message ?: "تایید پرداخت ناموفق بود. دوباره تلاش کنید",
                    )
                }
                return@launch
            }
            val accountId = (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId
            if (!accountId.isNullOrBlank()) runCatching { ownProfileRepository.fetchAndCacheOwnProfile(accountId) }
            mutableUiState.update {
                it.copy(
                    isVerifyingPayment = false,
                    pendingTrackId = null,
                    successMessage = body.message ?: "پرداخت با موفقیت انجام شد.",
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
            mutableUiState.update {
                it.copy(
                    isVerifyingPayment = false,
                    pendingBazaarVerification = null,
                    successMessage = body.message ?: "اشتراک ویژه با موفقیت فعال شد! 🎉",
                )
            }
        }
    }

    private fun toUiPlan(plan: PricingPlanDto): PricingPlanUiModel? {
        val meta = when (plan.planType) {
            "monthly" -> PlanMeta("vista_premium_monthly", "ماهانه", "انعطاف‌پذیر برای شروع")
            "three_monthly" -> PlanMeta("vista_premium_3monthly", "سه‌ماهه", "سه ماه پریمیوم پیوسته", "پیشنهاد ویستا")
            "yearly" -> PlanMeta("vista_premium_yearly", "سالانه", "یک سال کامل پریمیوم")
            else -> return null
        }
        val amount = NumberFormat.getIntegerInstance(Locale("fa")).format(plan.priceToman)
        return PricingPlanUiModel(meta.productId, meta.title, "$amount تومان", meta.description, meta.badge)
    }

    private data class PlanMeta(
        val productId: String,
        val title: String,
        val description: String,
        val badge: String? = null,
    )
}
