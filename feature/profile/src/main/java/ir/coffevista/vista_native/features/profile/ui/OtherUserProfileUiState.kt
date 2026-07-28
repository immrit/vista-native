package ir.coffevista.vista_native.features.profile.ui

import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.features.profile.data.PublicProfile

data class OtherUserProfileUiState(
    val isInitialLoading: Boolean = true,
    val profile: PublicProfile? = null,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false,
    val initialError: AppError? = null,
    val userNotFound: Boolean = false,
    val followMutationPending: FollowMutationPending? = null,
    val followMutationError: AppError? = null,
)

enum class FollowMutationPending {
    Follow,
    Unfollow,
}

sealed interface OtherUserProfileAction {
    data object Refresh : OtherUserProfileAction
    data object Retry : OtherUserProfileAction
    data object FollowClicked : OtherUserProfileAction
    data object UnfollowClicked : OtherUserProfileAction
    data object BackClicked : OtherUserProfileAction
}

sealed interface OtherUserProfileEffect {
    data object NavigateBack : OtherUserProfileEffect
    data object RedirectToOwnProfile : OtherUserProfileEffect
    data class ShowSnackbar(val message: String) : OtherUserProfileEffect
}
