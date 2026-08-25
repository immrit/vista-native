package ir.coffevista.vista_native.features.stories.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.stories.data.StoryRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoryPlayerViewModel @Inject constructor(
    val repository: StoryRepository,
    private val authStateProvider: AuthenticationStateProvider,
) : ViewModel() {

    val activeStoryUsers = repository.activeStoryUsers

    val currentUserId: String
        get() = (authStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId.orEmpty()

    fun markViewed(storyId: String) {
        repository.markStoryAsViewed(storyId)
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            repository.deleteStory(storyId)
        }
    }

    fun reactToStory(storyId: String, reaction: String) {
        viewModelScope.launch {
            repository.reactToStory(storyId, reaction)
        }
    }

    fun replyToStory(storyId: String, message: String) {
        viewModelScope.launch {
            repository.replyToStory(storyId, message)
        }
    }

    fun votePoll(storyId: String, optionId: String) {
        viewModelScope.launch {
            repository.votePoll(storyId, optionId)
        }
    }
}
