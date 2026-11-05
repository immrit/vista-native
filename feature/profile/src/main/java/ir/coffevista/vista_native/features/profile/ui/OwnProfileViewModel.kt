package ir.coffevista.vista_native.features.profile.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.common.AppError
import ir.coffevista.vista_native.core.common.ErrorKind
import ir.coffevista.vista_native.core.common.Outcome
import ir.coffevista.vista_native.core.network.ErrorClassifier
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.profile.data.OwnProfileRepository
import ir.coffevista.vista_native.features.profile.data.ProfileAvatarUploader
import ir.coffevista.vista_native.features.profile.data.ProfileHighlightDto
import ir.coffevista.vista_native.features.profile.data.ProfileNoteDto
import ir.coffevista.vista_native.features.profile.data.PublicProfile
import ir.coffevista.vista_native.features.profile.data.ProfileRefreshResult
import ir.coffevista.vista_native.features.profile.data.FollowMutationResult
import ir.coffevista.vista_native.features.profile.data.ProfileUpdateRequestDto
import ir.coffevista.vista_native.features.profile.data.UserProfileRepository
import ir.coffevista.vista_native.core.model.note.ProfileNoteBridge
import ir.coffevista.vista_native.core.model.note.SharedProfileNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwnProfileViewModel @Inject constructor(
    private val repository: OwnProfileRepository,
    private val authStateProvider: AuthenticationStateProvider,
    private val userProfileRepository: UserProfileRepository,
    private val avatarUploader: ProfileAvatarUploader? = null,
) : ViewModel() {

    internal constructor(
        repository: OwnProfileRepository,
        authStateProvider: AuthenticationStateProvider,
    ) : this(
        repository = repository,
        authStateProvider = authStateProvider,
        userProfileRepository = NoOpUserProfileRepository,
        avatarUploader = null,
    )

    private val _highlights = MutableStateFlow<List<ProfileHighlightDto>>(emptyList())
    val highlights: StateFlow<List<ProfileHighlightDto>> = _highlights.asStateFlow()

    private val _activeNote = MutableStateFlow<ir.coffevista.vista_native.features.profile.data.ProfileNoteDto?>(null)
    val activeNote: StateFlow<ir.coffevista.vista_native.features.profile.data.ProfileNoteDto?> = _activeNote.asStateFlow()

    fun loadHighlights() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _highlights.value = userProfileRepository.getUserHighlights(userId)
        }
    }

    fun createHighlight(title: String, coverUrl: String? = null, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = userProfileRepository.createHighlight(title, coverUrl)
            val success = result.isSuccess
            if (success) {
                loadHighlights()
            }
            onResult(success)
        }
    }

    fun updateHighlight(id: String, title: String? = null, coverUrl: String? = null, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = userProfileRepository.updateHighlight(id, title, coverUrl)
            val success = result.isSuccess
            if (success) {
                loadHighlights()
            }
            onResult(success)
        }
    }

    fun deleteHighlight(id: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = userProfileRepository.deleteHighlight(id)
            val success = result.isSuccess
            if (success) {
                loadHighlights()
            }
            onResult(success)
        }
    }

    fun loadNote() {
        viewModelScope.launch {
            val res = userProfileRepository.getOwnNote()
            if (res.isSuccess) {
                _activeNote.value = res.getOrNull()
            }
        }
    }

    fun upsertNote(content: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = userProfileRepository.upsertNote(content)
            val success = res.isSuccess
            if (success) {
                _activeNote.value = res.getOrNull()
            }
            onResult(success)
        }
    }

    fun deleteNote(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = userProfileRepository.deleteNote()
            val success = res.isSuccess
            if (success) {
                _activeNote.value = null
            }
            onResult(success)
        }
    }

    suspend fun updateProfile(request: ProfileUpdateRequestDto): Outcome<Unit> =
        repository.updateOwnProfile(request)

    suspend fun updateAvatar(uri: Uri): Outcome<Unit> {
        val userId = currentUserId ?: return Outcome.Failure(
            AppError(ErrorKind.UNAUTHORIZED, "برای تغییر تصویر ابتدا وارد حساب شوید"),
        )
        val uploader = avatarUploader ?: return Outcome.Failure(
            AppError(ErrorKind.VALIDATION, "امکان آپلود تصویر در دسترس نیست"),
        )
        val previousUrl = (uiState.value as? OwnProfileUiState.Content)?.profile?.avatarUrl
        return try {
            val uploaded = uploader.upload(userId, uri)
            when (val result = repository.updateAvatar(uploaded.url)) {
                is Outcome.Success -> {
                    previousUrl?.takeIf { it.isNotBlank() && it != uploaded.url }?.let { oldUrl ->
                        runCatching { uploader.deleteByUrl(oldUrl) }
                    }
                    result
                }
                is Outcome.Failure -> {
                    runCatching { uploader.deleteByUrl(uploaded.url) }
                    result
                }
            }
        } catch (error: Exception) {
            Outcome.Failure(ErrorClassifier.classify(error, "آپلود تصویر نمایه"))
        }
    }

    suspend fun removeAvatar(): Outcome<Unit> {
        val previousUrl = (uiState.value as? OwnProfileUiState.Content)?.profile?.avatarUrl
            ?.takeIf { it.isNotBlank() }
            ?: return Outcome.Success(Unit)
        return when (val result = repository.updateAvatar("")) {
            is Outcome.Success -> {
                // The profile is already correct even if object-storage cleanup is unavailable.
                runCatching { avatarUploader?.deleteByUrl(previousUrl) }
                result
            }
            is Outcome.Failure -> result
        }
    }

    private val _uiState = MutableStateFlow<OwnProfileUiState>(OwnProfileUiState.Loading)
    val uiState: StateFlow<OwnProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            authStateProvider.state.collectLatest { authState ->
                if (authState is AuthenticationState.SignedIn) {
                    val userId = authState.context.userId
                    if (userId != currentUserId) {
                        currentUserId = userId
                        observeProfile(userId)
                    }
                } else if (authState is AuthenticationState.SignedOut) {
                    currentUserId = null
                    repository.clearProfileData()
                }
            }
        }
        viewModelScope.launch {
            ProfileNoteBridge.ownNote.collectLatest { sharedNote ->
                _activeNote.value = sharedNote?.let {
                    ProfileNoteDto(
                        userId = it.userId,
                        content = it.content,
                        createdAt = it.createdAt,
                        expiresAt = it.expiresAt,
                    )
                }
            }
        }
    }

    private fun observeProfile(userId: String) {
        viewModelScope.launch {
            loadHighlights()
            loadNote()
            // Initiate network fetch without awaiting, flow collection handles the rest
            launch { fetchProfile(userId) }
            
            repository.getOwnProfileFlow(userId).collectLatest { entity ->
                if (entity != null) {
                    _uiState.update { currentState ->
                        val isRefreshing = (currentState as? OwnProfileUiState.Content)?.isRefreshing ?: false
                        OwnProfileUiState.Content(profile = entity, isRefreshing = isRefreshing)
                    }
                }
            }
        }
    }

    private suspend fun fetchProfile(userId: String, isRefresh: Boolean = false) {
        if (isRefresh) {
            _uiState.update { state ->
                if (state is OwnProfileUiState.Content) state.copy(isRefreshing = true) else state
            }
        }
        
        when (val outcome = repository.fetchAndCacheOwnProfile(userId)) {
            is Outcome.Failure -> {
                // Only show error screen if we don't have cached content
                if (_uiState.value !is OwnProfileUiState.Content) {
                    _uiState.value = OwnProfileUiState.Error(outcome.error)
                } else if (isRefresh) {
                    _uiState.update { state ->
                        (state as? OwnProfileUiState.Content)?.copy(isRefreshing = false) ?: state
                    }
                }
            }
            is Outcome.Success -> {
                if (isRefresh) {
                    _uiState.update { state ->
                        (state as? OwnProfileUiState.Content)?.copy(isRefreshing = false) ?: state
                    }
                }
            }
        }
    }

    fun handleAction(action: OwnProfileAction) {
        val userId = currentUserId ?: return
        when (action) {
            OwnProfileAction.Refresh -> {
                viewModelScope.launch {
                    loadHighlights()
                    loadNote()
                    fetchProfile(userId, isRefresh = true)
                }
            }
            OwnProfileAction.Retry -> {
                _uiState.value = OwnProfileUiState.Loading
                viewModelScope.launch {
                    loadHighlights()
                    loadNote()
                    fetchProfile(userId)
                }
            }
        }
    }
}

private object NoOpUserProfileRepository : UserProfileRepository {
    override fun observeProfile(viewerAccountId: String, profileUserId: String) = kotlinx.coroutines.flow.emptyFlow<PublicProfile?>()
    override suspend fun refreshProfile(viewerAccountId: String, profileUserId: String) = ProfileRefreshResult.Success
    override suspend fun follow(viewerAccountId: String, profileUserId: String) = FollowMutationResult.Failure(ir.coffevista.vista_native.core.common.AppError(ir.coffevista.vista_native.core.common.ErrorKind.UNKNOWN, "noop"))
    override suspend fun unfollow(viewerAccountId: String, profileUserId: String) = FollowMutationResult.Failure(ir.coffevista.vista_native.core.common.AppError(ir.coffevista.vista_native.core.common.ErrorKind.UNKNOWN, "noop"))
    override suspend fun clearAccount(viewerAccountId: String) {}
    override suspend fun reportProfile(profileId: String, reason: String, additionalDetails: String?) = Result.success(Unit)
}
