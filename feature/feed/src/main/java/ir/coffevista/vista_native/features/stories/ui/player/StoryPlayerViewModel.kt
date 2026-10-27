package ir.coffevista.vista_native.features.stories.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.stories.data.StoryRepository
import ir.coffevista.vista_native.features.stories.domain.HighlightDto
import ir.coffevista.vista_native.features.stories.domain.StoryQuestionAnswerDto
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

    fun votePoll(storyId: String, elementId: String, optionIndex: Int) {
        viewModelScope.launch {
            repository.votePoll(storyId, elementId, optionIndex)
        }
    }

    fun votePoll(storyId: String, optionId: String) {
        viewModelScope.launch {
            repository.votePoll(storyId, optionId)
        }
    }

    fun reportStory(storyId: String, reason: String, details: String = "", onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repository.reportStory(storyId, reason, details)
                .onSuccess { onResult(true, null) }
                .onFailure { onResult(false, it.message) }
        }
    }

    fun answerQuestion(storyId: String, elementId: String, answer: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repository.answerQuestion(storyId, elementId, answer)
                .onSuccess { onResult(true, null) }
                .onFailure { onResult(false, it.message) }
        }
    }

    fun loadUserHighlights(onResult: (List<HighlightDto>) -> Unit) {
        viewModelScope.launch {
            val res = repository.getUserHighlights(currentUserId)
            onResult(res.getOrDefault(emptyList()))
        }
    }

    fun addToHighlight(highlight: HighlightDto, storyId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val updatedStoryIds = (highlight.storyIds + storyId).distinct()
            repository.updateHighlight(
                highlightId = highlight.id,
                storyIds = updatedStoryIds,
            ).onSuccess { onResult(true) }.onFailure { onResult(false) }
        }
    }

    fun createHighlight(title: String, coverUrl: String?, storyId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.createHighlight(
                title = title,
                coverUrl = coverUrl,
                storyIds = listOf(storyId),
            ).onSuccess { onResult(true) }.onFailure { onResult(false) }
        }
    }
}
