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
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isRequestingPayment: Boolean = false,
    val isVerifyingPayment: Boolean = false,
    val paymentUrl: String? = null,
    val pendingTrackId: Long? = null,
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
            val plans = runCatching { api.getPlans() }
                .getOrNull()
                ?.takeIf { it.isSuccessful }
                ?.body()
                ?.plans
                .orEmpty()
                .filter { it.priceToman > 0L }
                .mapNotNull(::toUiPlan)
            mutableUiState.value = if (plans.isEmpty()) {
                PricingUiState(isLoading = false, errorMessage = "کاتالوگ اشتراک در دسترس نیست. دوباره تلاش کنید")
            } else PricingUiState(plans = plans, isLoading = false)
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
