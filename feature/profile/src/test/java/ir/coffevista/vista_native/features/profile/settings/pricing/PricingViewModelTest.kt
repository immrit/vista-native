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

    @Test
    fun verifyBazaarPurchase_coinsSetsConsumeTokenAndMessage() = runTest {
        val fakeApi = FakePricingPlansApi(
            bazaarResponse = BazaarVerifyResponseDto(
                success = true,
                message = "1000 سکه به موجودی شما اضافه شد.",
                type = "coins",
                coinsAdded = 1000L,
                newBalance = 2000L,
            ),
        )
        authStateProvider.setSignedIn("user-123")
        val viewModel = PricingViewModel(fakeApi, authStateProvider, ownProfileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.verifyBazaarPurchase(
            productId = "coins_1000",
            purchaseToken = "coin-token-1",
            packageName = "ir.coffevista.vista",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("coin-token-1", state.pendingConsumeToken)
        assertEquals(StoreChannel.Bazaar, state.pendingConsumeChannel)
        assertEquals("1000 سکه به موجودی شما اضافه شد.", state.successMessage)

        viewModel.onPurchaseConsumed()
        assertNull(viewModel.uiState.value.pendingConsumeToken)
    }

    @Test
    fun verifyMyketPurchase_subscriptionVerifiesOnMyketEndpointAndSchedulesConsume() = runTest {
        val fakeApi = FakePricingPlansApi(
            bazaarResponse = BazaarVerifyResponseDto(),
            myketResponse = BazaarVerifyResponseDto(
                success = true,
                message = "مدت اشتراک شما 30 روز اضافه شد.",
                plan = "monthly",
                daysAdded = 30,
            ),
        )
        authStateProvider.setSignedIn("user-myket-1")
        val viewModel = PricingViewModel(
            api = fakeApi,
            authenticationStateProvider = authStateProvider,
            ownProfileRepository = ownProfileRepository,
            storeBillingConfig = StoreBillingConfig(channel = StoreChannel.Myket, myketPublicKey = "rsa-test"),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StoreChannel.Myket, viewModel.uiState.value.storeChannel)

        viewModel.verifyMyketPurchase(
            productId = "vista_premium_monthly",
            purchaseToken = "myket-tok-99",
            packageName = "ir.coffevista.vista",
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isVerifyingPayment)
        assertNull(state.pendingStoreVerification)
        assertEquals("myket-tok-99", state.pendingConsumeToken)
        assertEquals(StoreChannel.Myket, state.pendingConsumeChannel)
        assertEquals("myket-tok-99", fakeApi.lastMyketRequest?.purchaseToken)
        assertEquals("مدت اشتراک شما 30 روز اضافه شد.", state.successMessage)
        assertEquals(1, ownProfileRepository.fetchCount)
    }

    @Test
    fun startZibalPurchaseAndVerifyTrackId_completesCheckout() = runTest {
        val fakeApi = FakePricingPlansApi(
            bazaarResponse = BazaarVerifyResponseDto(),
            zibalRequestResponse = ZibalRequestResponseDto(
                trackId = 987654L,
                url = "https://gateway.zibal.ir/start/987654",
            ),
            zibalVerifyResponse = BazaarVerifyResponseDto(
                success = true,
                message = "پرداخت با موفقیت انجام شد.",
                type = "subscription",
                plan = "monthly",
            ),
        )
        authStateProvider.setSignedIn("user-zibal-1")
        val viewModel = PricingViewModel(fakeApi, authStateProvider, ownProfileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startZibalPurchase("vista_premium_monthly")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("https://gateway.zibal.ir/start/987654", viewModel.uiState.value.pendingZibalCheckoutUrl)
        assertEquals(987654L, viewModel.uiState.value.pendingZibalTrackId)

        viewModel.consumeZibalCheckoutUrl()
        assertNull(viewModel.uiState.value.pendingZibalCheckoutUrl)

        viewModel.verifyZibalTrackId(987654L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingZibalTrackId)
        assertEquals("پرداخت با موفقیت انجام شد.", viewModel.uiState.value.successMessage)
        assertEquals(1, ownProfileRepository.fetchCount)
    }
}

private class FakePricingPlansApi(
    var bazaarResponse: BazaarVerifyResponseDto,
    var myketResponse: BazaarVerifyResponseDto = BazaarVerifyResponseDto(success = true),
    var zibalRequestResponse: ZibalRequestResponseDto = ZibalRequestResponseDto(),
    var zibalVerifyResponse: BazaarVerifyResponseDto = BazaarVerifyResponseDto(success = true),
) : PricingPlansApi {
    var lastBazaarRequest: BazaarVerifyRequestDto? = null
    var lastMyketRequest: BazaarVerifyRequestDto? = null

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

    override suspend fun verifyBazaar(request: BazaarVerifyRequestDto): Response<BazaarVerifyResponseDto> {
        lastBazaarRequest = request
        return Response.success(bazaarResponse)
    }

    override suspend fun verifyMyket(request: BazaarVerifyRequestDto): Response<BazaarVerifyResponseDto> {
        lastMyketRequest = request
        return Response.success(myketResponse)
    }

    override suspend fun requestZibal(request: ZibalRequestDto): Response<ZibalRequestResponseDto> =
        Response.success(zibalRequestResponse)

    override suspend fun verifyZibal(request: ZibalVerifyRequestDto): Response<BazaarVerifyResponseDto> =
        Response.success(zibalVerifyResponse)

    override suspend fun getCoinPackages(): Response<CoinPackagesResponseDto> =
        Response.success(CoinPackagesResponseDto())
}
