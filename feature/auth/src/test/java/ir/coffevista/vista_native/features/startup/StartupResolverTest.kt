package ir.coffevista.vista_native.features.startup

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.common.EpochClock
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.datastore.OnboardingStore
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.AuthSession
import ir.coffevista.vista_native.core.model.auth.AuthUser
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification
import ir.coffevista.vista_native.core.network.NetworkMonitor
import ir.coffevista.vista_native.core.network.NetworkState
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.core.security.StoredSession
import ir.coffevista.vista_native.features.auth.domain.AuthRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupResolverTest {
    @Test
    fun firstRunWithoutSessionRoutesToOnboarding() = runBlocking {
        val destination = resolver(session = null, onboardingDone = false).resolve()

        assertEquals(StartupDestination.Onboarding, destination)
    }

    @Test
    fun returningSignedOutUserRoutesDirectlyToAuthentication() = runBlocking {
        val destination = resolver(session = null, onboardingDone = true).resolve()

        assertEquals(StartupDestination.Authentication, destination)
    }

    @Test
    fun freshAccessTokenDoesNotCallRefresh() = runBlocking {
        val auth = FakeAuthRepository()
        val destination = resolver(
            session = stored(expiresAt = NOW + 3_600),
            onboardingDone = true,
            auth = auth,
        ).resolve()

        assertTrue(destination is StartupDestination.Authenticated)
        assertFalse((destination as StartupDestination.Authenticated).context.offline)
        assertEquals(0, auth.refreshCalls)
    }

    @Test
    fun expiredSessionPersistsSuccessfulRefresh() = runBlocking {
        val auth = FakeAuthRepository(
            refreshResult = Outcome.Success(payload(profileCompleted = false)),
        )
        val store = FakeSessionStore(stored(expiresAt = NOW - 10))
        val destination = StartupResolver(
            auth,
            FakeOnboardingStore(true),
            store,
            clock = EpochClock { NOW },
        ).resolve()

        assertTrue(destination is StartupDestination.Authenticated)
        assertEquals(1, store.saveCalls)
        assertFalse((destination as StartupDestination.Authenticated).context.profileCompleted)
    }

    @Test
    fun terminalRefreshClearsSessionAndRoutesToAuth() = runBlocking {
        val store = FakeSessionStore(stored(expiresAt = NOW - 10))
        val onboarding = FakeOnboardingStore(true)
        val auth = FakeAuthRepository(
            refreshResult = Outcome.Failure(
                AppError(ErrorKind.UNAUTHORIZED, "نشست نامعتبر است"),
            ),
        )

        val destination = StartupResolver(
            auth,
            onboarding,
            store,
            clock = EpochClock { NOW },
        ).resolve()

        assertEquals(StartupDestination.Authentication, destination)
        assertEquals(1, store.clearCalls)
        assertTrue(onboarding.isCompleted())
    }

    @Test
    fun transientRefreshFailureKeepsOfflineSession() = runBlocking {
        val store = FakeSessionStore(stored(expiresAt = NOW - 10))
        val auth = FakeAuthRepository(
            refreshResult = Outcome.Failure(
                AppError(ErrorKind.NETWORK, "اینترنت در دسترس نیست"),
            ),
        )

        val destination = StartupResolver(
            auth,
            FakeOnboardingStore(true),
            store,
            clock = EpochClock { NOW },
        ).resolve()

        assertTrue(destination is StartupDestination.Authenticated)
        assertTrue((destination as StartupDestination.Authenticated).context.offline)
        assertEquals(0, store.clearCalls)
    }

    @Test
    fun maintenanceTakesPrecedenceOverLocalSession() = runBlocking {
        val auth = FakeAuthRepository(
            maintenanceResult = Outcome.Success(true),
        )

        val destination = resolver(
            session = stored(expiresAt = NOW + 3_600),
            onboardingDone = true,
            auth = auth,
        ).resolve()

        assertEquals(StartupDestination.Maintenance, destination)
    }

    @Test
    fun offlineSessionSkipsNetworkAndUsesDeterministicFallback() = runBlocking {
        val auth = FakeAuthRepository()
        val destination = StartupResolver(
            authRepository = auth,
            onboardingStore = FakeOnboardingStore(true),
            sessionStore = FakeSessionStore(stored(expiresAt = NOW - 10)),
            clock = EpochClock { NOW },
            networkMonitor = FakeNetworkMonitor(NetworkState.OFFLINE),
        ).resolve()

        assertTrue(destination is StartupDestination.Authenticated)
        assertTrue((destination as StartupDestination.Authenticated).context.offline)
        assertEquals(0, auth.refreshCalls)
        assertEquals(0, auth.maintenanceCalls)
    }

    @Test
    fun offlineWithoutSessionRoutesToAuthenticationWithoutNetworkCalls() = runBlocking {
        val auth = FakeAuthRepository()
        val destination = StartupResolver(
            authRepository = auth,
            onboardingStore = FakeOnboardingStore(true),
            sessionStore = FakeSessionStore(null),
            clock = EpochClock { NOW },
            networkMonitor = FakeNetworkMonitor(NetworkState.OFFLINE),
        ).resolve()

        assertEquals(StartupDestination.Authentication, destination)
        assertEquals(0, auth.refreshCalls)
        assertEquals(0, auth.maintenanceCalls)
    }

    @Test
    fun expiredSessionWithoutRefreshTokenClearsAndRoutesToAuthentication() = runBlocking {
        val store = FakeSessionStore(
            stored(expiresAt = NOW - 10).copy(refreshToken = ""),
        )

        val destination = StartupResolver(
            authRepository = FakeAuthRepository(),
            onboardingStore = FakeOnboardingStore(true),
            sessionStore = store,
            clock = EpochClock { NOW },
        ).resolve()

        assertEquals(StartupDestination.Authentication, destination)
        assertEquals(1, store.clearCalls)
    }

    @Test
    fun revokedAndDisabledRefreshesAreTerminal() = runBlocking {
        listOf(ErrorKind.FORBIDDEN, ErrorKind.ACCOUNT_DISABLED).forEach { kind ->
            val store = FakeSessionStore(stored(expiresAt = NOW - 10))
            val destination = StartupResolver(
                authRepository = FakeAuthRepository(
                    refreshResult = Outcome.Failure(AppError(kind, "terminal")),
                ),
                onboardingStore = FakeOnboardingStore(true),
                sessionStore = store,
                clock = EpochClock { NOW },
            ).resolve()

            assertEquals(StartupDestination.Authentication, destination)
            assertEquals(1, store.clearCalls)
        }
    }

    @Test
    fun debugFixtureShortCircuitsProductionCollaboratorsDeterministically() = runBlocking {
        val auth = FakeAuthRepository(
            maintenanceResult = Outcome.Success(true),
        )
        val fixture = object : StartupFixture {
            override fun configure(rawScenario: String?) = Unit
            override suspend fun destinationOrNull() = StartupDestination.Authentication
        }

        val destination = StartupResolver(
            authRepository = auth,
            onboardingStore = FakeOnboardingStore(false),
            sessionStore = FakeSessionStore(stored(expiresAt = NOW + 3_600)),
            clock = EpochClock { NOW },
            fixtures = setOf(fixture),
        ).resolve()

        assertEquals(StartupDestination.Authentication, destination)
        assertEquals(0, auth.maintenanceCalls)
        assertEquals(0, auth.refreshCalls)
    }

    private fun resolver(
        session: StoredSession?,
        onboardingDone: Boolean,
        auth: FakeAuthRepository = FakeAuthRepository(),
    ) = StartupResolver(
        authRepository = auth,
        onboardingStore = FakeOnboardingStore(onboardingDone),
        sessionStore = FakeSessionStore(session),
        clock = EpochClock { NOW },
    )

    private companion object {
        const val NOW = 1_800_000_000L
    }
}

private class FakeOnboardingStore(
    private val completed: Boolean,
) : OnboardingStore {
    override suspend fun isCompleted() = completed
    override suspend fun markCompleted() = Unit
}

private class FakeSessionStore(
    private var session: StoredSession?,
) : SessionStore {
    var saveCalls = 0
    var clearCalls = 0

    override fun read() = session

    override fun save(payload: AuthPayload) {
        saveCalls += 1
        session = stored(payload.session.expiresAtEpochSeconds)
    }

    override fun markPasswordConfigured() {
        session = session?.copy(passwordRequired = false)
    }

    override fun clear() {
        clearCalls += 1
        session = null
    }
}

private class FakeAuthRepository(
    private val refreshResult: Outcome<AuthPayload> = Outcome.Success(payload()),
    private val maintenanceResult: Outcome<Boolean> = Outcome.Success(false),
) : AuthRepository {
    var refreshCalls = 0
    var maintenanceCalls = 0

    override suspend fun lookupIdentifier(identifier: String): Outcome<IdentifierLookup> =
        error("unused")

    override suspend fun login(identifier: String, password: String): Outcome<AuthPayload> =
        error("unused")

    override suspend fun sendOtp(phoneNumber: String): Outcome<OtpChallenge> =
        error("unused")

    override suspend fun verifyOtp(
        phoneNumber: String,
        code: String,
    ): Outcome<OtpVerification> = error("unused")

    override suspend fun verifyTwoFactor(
        token: String,
        password: String,
    ): Outcome<AuthPayload> = error("unused")

    override suspend fun setPassword(
        accessToken: String,
        password: String,
    ): Outcome<Unit> = error("unused")

    override suspend fun refresh(refreshToken: String): Outcome<AuthPayload> {
        refreshCalls += 1
        return refreshResult
    }

    override suspend fun maintenanceMode(): Outcome<Boolean> {
        maintenanceCalls += 1
        return maintenanceResult
    }
}

private class FakeNetworkMonitor(
    initial: NetworkState,
) : NetworkMonitor {
    private val mutableState = MutableStateFlow(initial)
    override val state: StateFlow<NetworkState> = mutableState
    override fun currentState(): NetworkState = mutableState.value
}

private fun stored(expiresAt: Long) = StoredSession(
    accessToken = "access",
    refreshToken = "refresh",
    userId = "user-id",
    expiresAtEpochSeconds = expiresAt,
    profileCompleted = true,
    passwordRequired = false,
)

private fun payload(profileCompleted: Boolean = true) = AuthPayload(
    user = AuthUser(
        id = "user-id",
        phoneNumber = "09123456789",
        profileCompleted = profileCompleted,
        hasPassword = true,
        passwordRequired = false,
        accountStatus = "active",
    ),
    session = AuthSession(
        accessToken = "new-access",
        refreshToken = "new-refresh",
        expiresAtEpochSeconds = 1_900_000_000L,
    ),
    isNewUser = false,
)
