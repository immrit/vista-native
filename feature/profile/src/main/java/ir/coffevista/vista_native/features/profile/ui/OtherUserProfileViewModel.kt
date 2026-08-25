package ir.coffevista.vista_native.features.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.profile.data.FollowMutationResult
import ir.coffevista.vista_native.features.profile.data.ProfileRefreshResult
import ir.coffevista.vista_native.features.profile.data.UserProfileRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtherUserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UserProfileRepository,
    authenticationStateProvider: AuthenticationStateProvider,
) : ViewModel() {
    private val profileUserId: String = requireNotNull(savedStateHandle[PROFILE_USER_ID_KEY])
    private val viewerAccountId: String? =
        (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)
            ?.context
            ?.userId

    private val mutableUiState = MutableStateFlow(OtherUserProfileUiState())
    val uiState: StateFlow<OtherUserProfileUiState> = mutableUiState.asStateFlow()

    private val effectChannel = Channel<OtherUserProfileEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        val viewer = viewerAccountId
        if (viewer == null) {
            mutableUiState.update {
                it.copy(
                    isInitialLoading = false,
                    initialError = ir.coffevista.vista_native.core.common.AppError(
                        kind = ir.coffevista.vista_native.core.common.ErrorKind.UNAUTHORIZED,
                        messageFa = "برای مشاهده نمایه وارد حساب شوید",
                    ),
                )
            }
        } else if (viewer == profileUserId) {
            mutableUiState.update { it.copy(isInitialLoading = false) }
            viewModelScope.launch {
                effectChannel.send(OtherUserProfileEffect.RedirectToOwnProfile)
            }
        } else {
            observeProfile(viewer)
            refresh(viewer)
        }
    }

    fun onAction(action: OtherUserProfileAction) {
        when (action) {
            OtherUserProfileAction.Refresh -> {
                if (mutableUiState.value.followMutationPending == null) {
                    viewerAccountId?.let(::refresh)
                }
            }
            OtherUserProfileAction.Retry -> viewerAccountId?.let(::refresh)
            OtherUserProfileAction.FollowClicked -> mutate(following = true)
            OtherUserProfileAction.UnfollowClicked -> mutate(following = false)
            OtherUserProfileAction.BackClicked -> viewModelScope.launch {
                effectChannel.send(OtherUserProfileEffect.NavigateBack)
            }
        }
    }

    private fun observeProfile(viewer: String) {
        viewModelScope.launch {
            repository.observeProfile(viewer, profileUserId).collect { profile ->
                if (profile != null) {
                    mutableUiState.update {
                        it.copy(
                            isInitialLoading = false,
                            profile = profile,
                            initialError = null,
                            userNotFound = false,
                        )
                    }
                }
            }
        }
    }

    private fun refresh(viewer: String) {
        viewModelScope.launch {
            val hadContent = mutableUiState.value.profile != null
            mutableUiState.update {
                it.copy(
                    isInitialLoading = !hadContent,
                    isRefreshing = hadContent,
                    initialError = null,
                    userNotFound = false,
                    followMutationError = null,
                )
            }
            when (val result = repository.refreshProfile(viewer, profileUserId)) {
                ProfileRefreshResult.Success -> mutableUiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        isStale = false,
                    )
                }
                ProfileRefreshResult.SelfProfile -> {
                    mutableUiState.update {
                        it.copy(isInitialLoading = false, isRefreshing = false)
                    }
                    effectChannel.send(OtherUserProfileEffect.RedirectToOwnProfile)
                }
                is ProfileRefreshResult.Failure -> {
                    val notFound = result.error.code == PROFILE_NOT_FOUND_CODE
                    mutableUiState.update {
                        it.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            isStale = result.hadCache,
                            initialError = result.error.takeUnless { result.hadCache || notFound },
                            userNotFound = notFound && !result.hadCache,
                        )
                    }
                    if (result.hadCache) {
                        effectChannel.send(
                            OtherUserProfileEffect.ShowSnackbar(result.error.messageFa),
                        )
                    }
                }
            }
        }
    }

    private fun mutate(following: Boolean) {
        val viewer = viewerAccountId ?: return
        if (mutableUiState.value.followMutationPending != null) return
        mutableUiState.update {
            it.copy(
                followMutationPending = if (following) {
                    FollowMutationPending.Follow
                } else {
                    FollowMutationPending.Unfollow
                },
                followMutationError = null,
            )
        }
        viewModelScope.launch {
            val result = if (following) {
                repository.follow(viewer, profileUserId)
            } else {
                repository.unfollow(viewer, profileUserId)
            }
            when (result) {
                is FollowMutationResult.Success -> mutableUiState.update {
                    it.copy(followMutationPending = null, followMutationError = null)
                }
                is FollowMutationResult.Failure -> {
                    mutableUiState.update {
                        it.copy(
                            followMutationPending = null,
                            followMutationError = result.error,
                        )
                    }
                    effectChannel.send(
                        OtherUserProfileEffect.ShowSnackbar(result.error.messageFa),
                    )
                }
                FollowMutationResult.IgnoredConcurrent -> mutableUiState.update {
                    it.copy(followMutationPending = null)
                }
                FollowMutationResult.Unavailable -> {
                    mutableUiState.update { it.copy(followMutationPending = null) }
                    effectChannel.send(
                        OtherUserProfileEffect.ShowSnackbar("این عملیات برای این نمایه در دسترس نیست"),
                    )
                }
            }
        }
    }

    companion object {
        const val PROFILE_USER_ID_KEY = "userId"
        private const val PROFILE_NOT_FOUND_CODE = "PROFILE_NOT_FOUND"
    }
}
