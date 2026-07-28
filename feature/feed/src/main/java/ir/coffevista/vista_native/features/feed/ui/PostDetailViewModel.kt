package ir.coffevista.vista_native.features.feed.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.features.auth.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PostDetailUiState {
    object Loading : PostDetailUiState
    data class Content(val post: FeedPost) : PostDetailUiState
    data class Error(val message: String) : PostDetailUiState
}

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val repository: FeedRepository,
    private val authStateProvider: AuthenticationStateProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: String = savedStateHandle.get<String>("reference") ?: ""

    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    init {
        if (postId.isNotEmpty()) {
            observePost()
        } else {
            _uiState.value = PostDetailUiState.Error("Post ID is missing")
        }
    }

    private fun observePost() {
        viewModelScope.launch {
            authStateProvider.state.collectLatest { authState ->
                if (authState is AuthenticationState.SignedIn) {
                    repository.getPostById(authState.context.userId, postId)
                        .catch { e ->
                            _uiState.value = PostDetailUiState.Error(e.message ?: "Failed to load post")
                        }
                        .collectLatest { post ->
                            if (post != null) {
                                _uiState.value = PostDetailUiState.Content(post)
                            } else {
                                _uiState.value = PostDetailUiState.Error("Post not found")
                            }
                        }
                } else if (authState is AuthenticationState.SignedOut) {
                    _uiState.value = PostDetailUiState.Error("Not signed in")
                }
            }
        }
    }
}
