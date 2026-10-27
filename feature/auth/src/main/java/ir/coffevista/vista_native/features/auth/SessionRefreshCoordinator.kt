package ir.coffevista.vista_native.features.auth

import ir.coffevista.vista_native.core.common.ApplicationScope
import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.model.auth.AuthPayload
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.core.worker.AccountWorkController
import ir.coffevista.vista_native.core.worker.NoOpAccountWorkController
import ir.coffevista.vista_native.features.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RefreshResolution {
    data class Refreshed(val payload: AuthPayload) : RefreshResolution
    data object TerminalSession : RefreshResolution
    data object TransientFailure : RefreshResolution
    data object PersistenceFailure : RefreshResolution
}

interface SessionRefreshCoordinator {
    suspend fun refresh(refreshToken: String): RefreshResolution
}

class DirectSessionRefreshCoordinator(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore,
    private val accountWorkController: AccountWorkController = NoOpAccountWorkController,
) : SessionRefreshCoordinator {
    override suspend fun refresh(refreshToken: String): RefreshResolution =
        performRefresh(repository, sessionStore, accountWorkController, refreshToken)
}

@Singleton
class SingleFlightSessionRefreshCoordinator @Inject constructor(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore,
    private val accountWorkController: AccountWorkController,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : SessionRefreshCoordinator {
    private val inFlight = ConcurrentHashMap<String, Deferred<RefreshResolution>>()
    private val refreshMutex = Mutex()
    private val rotatedFrom = ConcurrentHashMap<String, AuthPayload>()

    @Volatile
    private var latestPayload: AuthPayload? = null

    override suspend fun refresh(refreshToken: String): RefreshResolution {
        rotatedFrom[refreshToken]?.let { return RefreshResolution.Refreshed(it) }
        val deferred = inFlight.computeIfAbsent(refreshToken) {
            applicationScope.async {
                refreshMutex.withLock {
                    rotatedFrom[refreshToken]?.let { return@withLock RefreshResolution.Refreshed(it) }
                    val storedToken = runCatching { sessionStore.read()?.refreshToken }.getOrNull()
                    val latest = latestPayload
                    if (
                        !storedToken.isNullOrBlank() &&
                        storedToken != refreshToken &&
                        latest != null &&
                        latest.session.refreshToken == storedToken
                    ) {
                        return@withLock RefreshResolution.Refreshed(latest)
                    }

                    val resolution = performRefresh(
                        repository = repository,
                        sessionStore = sessionStore,
                        accountWorkController = accountWorkController,
                        refreshToken = refreshToken,
                    )
                    if (resolution is RefreshResolution.Refreshed) {
                        rotatedFrom[refreshToken] = resolution.payload
                        latestPayload = resolution.payload
                        if (rotatedFrom.size > 32) {
                            val oldestKey = rotatedFrom.keys().nextElement()
                            if (oldestKey != refreshToken) rotatedFrom.remove(oldestKey)
                        }
                    }
                    resolution
                }
            }
        }
        return try {
            deferred.await()
        } finally {
            if (deferred.isCompleted) inFlight.remove(refreshToken, deferred)
        }
    }
}

private suspend fun performRefresh(
    repository: AuthRepository,
    sessionStore: SessionStore,
    accountWorkController: AccountWorkController,
    refreshToken: String,
): RefreshResolution {
    val tokenBeforeRefresh = runCatching { sessionStore.read()?.refreshToken }.getOrNull()
    return when (val result = repository.refresh(refreshToken)) {
        is Outcome.Success -> {
            runCatching { sessionStore.save(result.value) }
                .fold(
                    onSuccess = { RefreshResolution.Refreshed(result.value) },
                    onFailure = { RefreshResolution.PersistenceFailure },
                )
        }
        is Outcome.Failure -> {
            if (
                result.error.kind == ErrorKind.UNAUTHORIZED ||
                result.error.kind == ErrorKind.FORBIDDEN ||
                result.error.kind == ErrorKind.ACCOUNT_DISABLED
            ) {
                val currentStored = runCatching { sessionStore.read() }.getOrNull()
                val currentToken = currentStored?.refreshToken
                val rotatedDuringCall = !currentToken.isNullOrBlank() &&
                    currentToken != refreshToken &&
                    currentToken != tokenBeforeRefresh
                if (rotatedDuringCall) {
                    RefreshResolution.TransientFailure
                } else {
                    val accountId = currentStored?.userId
                    runCatching(sessionStore::clear)
                    accountId?.let { id ->
                        runCatching { accountWorkController.cancelAccountWork(id) }
                    }
                    RefreshResolution.TerminalSession
                }
            } else {
                RefreshResolution.TransientFailure
            }
        }
    }
}
