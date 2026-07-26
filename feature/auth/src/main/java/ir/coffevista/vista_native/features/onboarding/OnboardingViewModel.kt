package ir.coffevista.vista_native.features.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.datastore.OnboardingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

data class OnboardingUiState(
    val page: Int = 0,
    val isCompleting: Boolean = false,
    val completed: Boolean = false,
)

sealed interface OnboardingAction {
    data object Next : OnboardingAction
    data object Previous : OnboardingAction
    data object Skip : OnboardingAction
    data class PageChanged(val page: Int) : OnboardingAction
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val store: OnboardingStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = mutableState.asStateFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.Next -> {
                if (mutableState.value.page == LAST_PAGE) {
                    complete()
                } else {
                    mutableState.value = mutableState.value.copy(
                        page = mutableState.value.page + 1,
                    )
                }
            }
            OnboardingAction.Previous -> {
                mutableState.value = mutableState.value.copy(
                    page = (mutableState.value.page - 1).coerceAtLeast(0),
                )
            }
            OnboardingAction.Skip -> complete()
            is OnboardingAction.PageChanged -> {
                mutableState.value = mutableState.value.copy(
                    page = action.page.coerceIn(0, LAST_PAGE),
                )
            }
        }
    }

    private fun complete() {
        if (mutableState.value.isCompleting || mutableState.value.completed) return
        mutableState.value = mutableState.value.copy(isCompleting = true)
        viewModelScope.launch {
            val saved = runCatching { store.markCompleted() }.isSuccess
            mutableState.value = mutableState.value.copy(
                isCompleting = false,
                completed = saved,
            )
        }
    }

    companion object {
        const val PAGE_COUNT = 3
        private const val LAST_PAGE = PAGE_COUNT - 1
    }
}
