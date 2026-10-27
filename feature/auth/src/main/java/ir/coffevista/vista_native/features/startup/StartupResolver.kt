package ir.coffevista.vista_native.features.startup

import ir.coffevista.vista_native.core.common.EpochClock
import ir.coffevista.vista_native.core.common.FoundationSignal
import ir.coffevista.vista_native.core.common.FoundationTelemetry
import ir.coffevista.vista_native.core.common.NoOpFoundationTelemetry
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.datastore.OnboardingStore
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.core.security.StoredSession
import ir.coffevista.vista_native.core.network.AssumeOnlineNetworkMonitor
import ir.coffevista.vista_native.core.network.NetworkMonitor
import ir.coffevista.vista_native.core.network.NetworkState
import ir.coffevista.vista_native.core.network.SystemGateMonitor
import ir.coffevista.vista_native.features.auth.domain.AuthRepository
import ir.coffevista.vista_native.features.auth.DirectSessionRefreshCoordinator
import ir.coffevista.vista_native.features.auth.RefreshResolution
import ir.coffevista.vista_native.features.auth.SessionRefreshCoordinator
import javax.inject.Inject

class StartupResolver @Inject constructor(
    private val authRepository: AuthRepository,
    private val onboardingStore: OnboardingStore,
    private val sessionStore: SessionStore,
    private val clock: EpochClock,
    private val networkMonitor: NetworkMonitor = AssumeOnlineNetworkMonitor,
    private val telemetry: FoundationTelemetry = NoOpFoundationTelemetry,
    private val refreshCoordinator: SessionRefreshCoordinator =
        DirectSessionRefreshCoordinator(authRepository, sessionStore),
    private val fixtures: Set<@JvmSuppressWildcards StartupFixture> = emptySet(),
) {
    suspend fun resolve(): StartupDestination {
        fixtures.firstNotNullOfOrNull { fixture -> fixture.destinationOrNull() }
            ?.let { destination -> return destination }

        val isOnline = networkMonitor.currentState() == NetworkState.ONLINE
        if (isOnline) {
            when (val maintenance = authRepository.maintenanceMode()) {
                is Outcome.Success -> if (maintenance.value) {
                    return StartupDestination.Maintenance
                }
                is Outcome.Failure -> if (
                    maintenance.error.code?.lowercase() in setOf("banned", "account_banned", "account_suspended")
                ) {
                    return StartupDestination.Banned(maintenance.error.messageFa)
                }
            }
        }
        if (SystemGateMonitor.isBanned.value) {
            return StartupDestination.Banned(SystemGateMonitor.bannedReasonFa.value)
        }

        val stored = runCatching(sessionStore::read).getOrElse {
            return StartupDestination.RecoverableError(
                "خواندن اطلاعات نشست ممکن نشد. لطفاً دوباره تلاش کنید",
            )
        } ?: return unauthenticatedDestination()

        if (!isOnline) {
            if (stored.requiresOnlineValidation) {
                return StartupDestination.RecoverableError(
                    "برای انتقال امن نشست قبلی، اتصال اینترنت و تأیید دوباره لازم است",
                )
            }
            telemetry.record(
                FoundationSignal(
                    name = "foundation.startup.session",
                    outcome = "offline_fallback",
                ),
            )
            return stored.authenticated(offline = true)
        }

        if (!stored.requiresOnlineValidation &&
            stored.expiresAtEpochSeconds > clock.nowEpochSeconds() + CLOCK_SKEW_SECONDS
        ) {
            return stored.authenticated(offline = false)
        }

        if (stored.refreshToken.isBlank()) {
            sessionStore.clear()
            return unauthenticatedDestination()
        }

        return when (val refresh = refreshCoordinator.refresh(stored.refreshToken)) {
            is RefreshResolution.Refreshed -> {
                if (refresh.payload.user.accountStatus.lowercase() in setOf("banned", "suspended")) {
                    sessionStore.clear()
                    return StartupDestination.Banned("حساب کاربری شما به دلیل تخلف از قوانین مسدود شده است.")
                }
                StartupDestination.Authenticated(
                    AuthenticatedContext(
                        userId = refresh.payload.user.id,
                        profileCompleted = refresh.payload.user.profileCompleted,
                        passwordRequired = refresh.payload.user.passwordRequired,
                        offline = false,
                        displayName = refresh.payload.user.welcomeName,
                    ),
                )
            }
            RefreshResolution.TerminalSession -> {
                if (SystemGateMonitor.isBanned.value) {
                    StartupDestination.Banned(SystemGateMonitor.bannedReasonFa.value)
                } else {
                    unauthenticatedDestination()
                }
            }
            RefreshResolution.TransientFailure -> if (stored.requiresOnlineValidation) {
                StartupDestination.RecoverableError(
                    "تأیید نشست منتقل‌شده ممکن نشد. لطفاً دوباره تلاش کنید",
                )
            } else {
                stored.authenticated(offline = true)
            }
            RefreshResolution.PersistenceFailure -> StartupDestination.RecoverableError(
                "ذخیره امن نشست ممکن نشد. لطفاً دوباره تلاش کنید",
            )
        }
    }

    private suspend fun unauthenticatedDestination(): StartupDestination {
        return if (onboardingStore.isCompleted()) {
            StartupDestination.Authentication
        } else {
            StartupDestination.Onboarding
        }
    }

    private fun StoredSession.authenticated(offline: Boolean): StartupDestination {
        return StartupDestination.Authenticated(
            AuthenticatedContext(
                userId = userId,
                profileCompleted = profileCompleted,
                passwordRequired = passwordRequired,
                offline = offline,
                displayName = displayName,
            ),
        )
    }

    private companion object {
        const val CLOCK_SKEW_SECONDS = 30L
    }
}
