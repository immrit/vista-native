package ir.coffevista.vista_native.features.startup

import ir.coffevista.vista_native.core.model.session.AuthenticatedContext

sealed interface StartupDestination {
    data object Loading : StartupDestination
    data object Maintenance : StartupDestination
    data object Onboarding : StartupDestination
    data object Authentication : StartupDestination
    data class Authenticated(val context: AuthenticatedContext) : StartupDestination
    data class RecoverableError(val messageFa: String) : StartupDestination
}

data class StartupUiState(
    val destination: StartupDestination = StartupDestination.Loading,
)
