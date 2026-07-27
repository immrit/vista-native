package ir.coffevista.vista_native.features.auth

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.model.auth.AuthSession
import ir.coffevista.vista_native.core.model.auth.AuthUser
import ir.coffevista.vista_native.core.model.auth.IdentifierLookup
import ir.coffevista.vista_native.core.model.auth.OtpChallenge
import ir.coffevista.vista_native.core.model.auth.OtpVerification
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.core.security.StoredSession
import ir.coffevista.vista_native.core.worker.AccountWorkController
import ir.coffevista.vista_native.core.worker.NoOpAccountWorkController
import ir.coffevista.vista_native.features.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRefreshCoordinatorTest {
    @Test
    fun concurrentRefreshBurstExecutesExactlyOneRefreshAndOnePersist() = runTest {
        val repository = FakeRefreshRepository(
            result = Outcome.Success(payload()),
            delayMillis = 100,
        )
        val store = RecordingSessionStore()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val coordinator = SingleFlightSessionRefreshCoordinator(
            repository = repository,
            sessionStore = store,
            accountWorkController = NoOpAccountWorkController,
            applicationScope = scope,
        )

        val results = (1..12).map {
            async { coordinator.refresh("same-rotating-token") }
        }
        advanceUntilIdle()

        assertTrue(results.awaitAll().all { it is RefreshResolution.Refreshed })
        assertEquals(1, repository.refreshCalls)
        assertEquals(1, store.saveCalls)
    }

    @Test
    fun explicitUnauthorizedClearsWhileTransientFailureKeepsSession() = runTest {
        val terminalStore = RecordingSessionStore(storedUserId = "account-42")
        val workController = RecordingAccountWorkController()
        val terminal = DirectSessionRefreshCoordinator(
            FakeRefreshRepository(
                Outcome.Failure(AppError(ErrorKind.UNAUTHORIZED, "expired")),
            ),
            terminalStore,
            workController,
        ).refresh("expired")

        assertEquals(RefreshResolution.TerminalSession, terminal)
        assertEquals(1, terminalStore.clearCalls)
        assertEquals(listOf("account-42"), workController.cancelledAccounts)

        val transientStore = RecordingSessionStore()
        val transient = DirectSessionRefreshCoordinator(
            FakeRefreshRepository(
                Outcome.Failure(AppError(ErrorKind.SERVER, "temporary")),
            ),
            transientStore,
        ).refresh("valid")

        assertEquals(RefreshResolution.TransientFailure, transient)
        assertEquals(0, transientStore.clearCalls)
    }
}

private class RecordingSessionStore(
    private val storedUserId: String? = null,
) : SessionStore {
    var saveCalls = 0
    var clearCalls = 0

    override fun read(): StoredSession? = storedUserId?.let { userId ->
        StoredSession(
            accessToken = "access",
            refreshToken = "refresh",
            userId = userId,
            expiresAtEpochSeconds = 1_900_000_000L,
            profileCompleted = true,
            passwordRequired = false,
        )
    }
    override fun save(payload: AuthPayload) {
        saveCalls += 1
    }
    override fun markPasswordConfigured() = Unit
    override fun clear() {
        clearCalls += 1
    }
}

private class RecordingAccountWorkController : AccountWorkController {
    val cancelledAccounts = mutableListOf<String>()

    override fun cancelAccountWork(accountId: String) {
        cancelledAccounts += accountId
    }
}

private class FakeRefreshRepository(
    private val result: Outcome<AuthPayload>,
    private val delayMillis: Long = 0,
) : AuthRepository {
    var refreshCalls = 0

    override suspend fun lookupIdentifier(identifier: String): Outcome<IdentifierLookup> =
        error("unused")
    override suspend fun login(identifier: String, password: String): Outcome<AuthPayload> =
        error("unused")
    override suspend fun sendOtp(phoneNumber: String): Outcome<OtpChallenge> = error("unused")
    override suspend fun verifyOtp(phoneNumber: String, code: String): Outcome<OtpVerification> =
        error("unused")
    override suspend fun verifyTwoFactor(token: String, password: String): Outcome<AuthPayload> =
        error("unused")
    override suspend fun setPassword(accessToken: String, password: String): Outcome<Unit> =
        error("unused")
    override suspend fun maintenanceMode(): Outcome<Boolean> = error("unused")

    override suspend fun refresh(refreshToken: String): Outcome<AuthPayload> {
        refreshCalls += 1
        if (delayMillis > 0) delay(delayMillis)
        return result
    }
}

private fun payload() = AuthPayload(
    user = AuthUser("user", "09123456789", true, true, false, "active"),
    session = AuthSession("access", "refresh", 1_900_000_000),
    isNewUser = false,
)
