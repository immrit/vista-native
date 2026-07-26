package ir.coffevista.vista_native.features.startup

import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.datastore.OnboardingStore
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.core.security.StoredSession
import ir.coffevista.vista_native.features.auth.domain.AuthRepository

class StartupResolver(
    private val authRepository: AuthRepository,
    private val onboardingStore: OnboardingStore,
    private val sessionStore: SessionStore,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000L },
) {
    suspend fun resolve(): StartupDestination {
        val maintenance = authRepository.maintenanceMode()
        if (maintenance is Outcome.Success && maintenance.value) {
            return StartupDestination.Maintenance
        }

        val stored = runCatching(sessionStore::read).getOrElse {
            return StartupDestination.RecoverableError(
                "خواندن اطلاعات نشست ممکن نشد. لطفاً دوباره تلاش کنید",
            )
        } ?: return unauthenticatedDestination()

        if (stored.expiresAtEpochSeconds > nowEpochSeconds() + CLOCK_SKEW_SECONDS) {
            return stored.authenticated(offline = false)
        }

        if (stored.refreshToken.isBlank()) {
            sessionStore.clear()
            return unauthenticatedDestination()
        }

        return when (val refresh = authRepository.refresh(stored.refreshToken)) {
            is Outcome.Success -> {
                runCatching { sessionStore.save(refresh.value) }.getOrElse {
                    return StartupDestination.RecoverableError(
                        "ذخیره امن نشست ممکن نشد. لطفاً دوباره تلاش کنید",
                    )
                }
                StartupDestination.Authenticated(
                    AuthenticatedContext(
                        userId = refresh.value.user.id,
                        profileCompleted = refresh.value.user.profileCompleted,
                        passwordRequired = refresh.value.user.passwordRequired,
                        offline = false,
                        displayName = refresh.value.user.welcomeName,
                    ),
                )
            }
            is Outcome.Failure -> {
                if (refresh.error.kind == ErrorKind.UNAUTHORIZED ||
                    refresh.error.kind == ErrorKind.ACCOUNT_DISABLED
                ) {
                    runCatching(sessionStore::clear)
                    unauthenticatedDestination()
                } else {
                    stored.authenticated(offline = true)
                }
            }
        }
    }

    private fun unauthenticatedDestination(): StartupDestination {
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
