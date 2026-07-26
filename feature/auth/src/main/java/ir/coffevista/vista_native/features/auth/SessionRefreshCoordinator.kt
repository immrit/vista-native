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

    override suspend fun refresh(refreshToken: String): RefreshResolution {
        val deferred = inFlight.computeIfAbsent(refreshToken) {
            applicationScope.async {
                performRefresh(repository, sessionStore, accountWorkController, refreshToken)
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
): RefreshResolution = when (val result = repository.refresh(refreshToken)) {
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
            val accountId = runCatching { sessionStore.read()?.userId }.getOrNull()
            runCatching(sessionStore::clear)
            accountId?.let { id ->
                runCatching { accountWorkController.cancelAccountWork(id) }
            }
            RefreshResolution.TerminalSession
        } else {
            RefreshResolution.TransientFailure
        }
    }
}
