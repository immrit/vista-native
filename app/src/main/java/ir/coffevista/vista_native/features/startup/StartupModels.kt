package ir.coffevista.vista_native.features.startup

data class AuthenticatedContext(
    val userId: String,
    val profileCompleted: Boolean,
    val passwordRequired: Boolean,
    val offline: Boolean,
    val displayName: String = "کاربر ویستا",
)

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
