package ir.coffevista.vista_native.features.profile.settings.pricing

import ir.coffevista.vista_native.features.profile.ui.FakeAuthenticationStateProvider
import ir.coffevista.vista_native.features.profile.ui.FakeOwnProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PricingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val authStateProvider = FakeAuthenticationStateProvider()
    private val ownProfileRepository = FakeOwnProfileRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun verifyBazaarPurchase_verifiesOnServerAndRefreshesOwnProfile() = runTest {
        val fakeApi = FakePricingPlansApi(
            bazaarResponse = BazaarVerifyResponseDto(
                success = true,
                message = "مدت اشتراک شما 30 روز اضافه شد.",
                plan = "monthly",
                daysAdded = 30,
            ),
        )
        authStateProvider.setSignedIn("user-123")
        val viewModel = PricingViewModel(fakeApi, authStateProvider, ownProfileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("user_user-123", viewModel.resolveBazaarPayload())

        viewModel.verifyBazaarPurchase(
            productId = "vista_premium_monthly",
            purchaseToken = "bazaar-token-xyz",
            packageName = "ir.coffevista.vista",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isVerifyingPayment)
        assertNull(state.pendingBazaarVerification)
        assertEquals("مدت اشتراک شما 30 روز اضافه شد.", state.successMessage)
        assertEquals(1, ownProfileRepository.fetchCount)
        assertEquals("bazaar-token-xyz", fakeApi.lastBazaarRequest?.purchaseToken)
        assertEquals("vista_premium_monthly", fakeApi.lastBazaarRequest?.productId)
        assertEquals("ir.coffevista.vista", fakeApi.lastBazaarRequest?.packageName)
    }

    @Test
    fun verifyBazaarPurchase_retainsPendingRequestOnFailureAndRecoversOnRetry() = runTest {
        val fakeApi = FakePricingPlansApi(
            bazaarResponse = BazaarVerifyResponseDto(
                success = false,
                message = "خطای موقت در ارتباط با بازار",
            ),
        )
        authStateProvider.setSignedIn("user-123")
        val viewModel = PricingViewModel(fakeApi, authStateProvider, ownProfileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.verifyBazaarPurchase(
            productId = "vista_premium_3monthly",
            purchaseToken = "token-retry-1",
            packageName = "ir.coffevista.vista",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.pendingBazaarVerification)
        assertEquals("خطای موقت در ارتباط با بازار", viewModel.uiState.value.errorMessage)

        fakeApi.bazaarResponse = BazaarVerifyResponseDto(
            success = true,
            message = "اشتراک ویژه فعال شد!",
            plan = "three_monthly",
            daysAdded = 90,
        )
        viewModel.retryBazaarVerification()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingBazaarVerification)
        assertEquals("اشتراک ویژه فعال شد!", viewModel.uiState.value.successMessage)
        assertEquals(1, ownProfileRepository.fetchCount)
    }
}

private class FakePricingPlansApi(
    var bazaarResponse: BazaarVerifyResponseDto,
) : PricingPlansApi {
    var lastBazaarRequest: BazaarVerifyRequestDto? = null

    override suspend fun getPlans(): Response<PricingPlansResponseDto> =
        Response.success(
            PricingPlansResponseDto(
                plans = listOf(
                    PricingPlanDto(planType = "monthly", priceToman = 99_000L),
                    PricingPlanDto(planType = "three_monthly", priceToman = 249_000L),
                    PricingPlanDto(planType = "yearly", priceToman = 799_000L),
                ),
            ),
        )

    override suspend fun requestZibal(request: ZibalRequestDto): Response<ZibalRequestResponseDto> =
        Response.success(ZibalRequestResponseDto(trackId = 100L, url = "https://gateway.zibal.ir/start/100"))

    override suspend fun verifyZibal(request: ZibalVerifyRequestDto): Response<ZibalVerifyResponseDto> =
        Response.success(ZibalVerifyResponseDto(success = true, message = "ok"))

    override suspend fun verifyBazaar(request: BazaarVerifyRequestDto): Response<BazaarVerifyResponseDto> {
        lastBazaarRequest = request
        return Response.success(bazaarResponse)
    }

    override suspend fun getCoinPackages(): Response<CoinPackagesResponseDto> =
        Response.success(CoinPackagesResponseDto())
}
