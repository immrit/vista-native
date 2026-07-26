package ir.coffevista.vista_native.navigation

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Startup : AppRoute

    @Serializable
    data object Maintenance : AppRoute

    @Serializable
    data object Onboarding : AppRoute

    @Serializable
    data object Authentication : AppRoute

    @Serializable
    data object AuthenticatedBoundary : AppRoute

    @Serializable
    data class DeferredFeature(
        val kind: DeferredFeatureKind,
        val reference: String,
    ) : AppRoute

    @Serializable
    data class DeepLinkFailure(
        val reason: DeepLinkFailureReason,
    ) : AppRoute
}

@Serializable
@Keep
enum class DeferredFeatureKind {
    POST,
    PROFILE,
    GROUP,
    CHAT,
}

@Serializable
@Keep
enum class DeepLinkFailureReason {
    UNSUPPORTED,
    INVALID_ARGUMENT,
}
