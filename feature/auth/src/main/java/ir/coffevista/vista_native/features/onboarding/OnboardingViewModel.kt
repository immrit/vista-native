package ir.coffevista.vista_native.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.coffevista.vista_native.core.datastore.OnboardingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

class OnboardingViewModel(
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
        store.markCompleted()
        mutableState.value = mutableState.value.copy(
            isCompleting = false,
            completed = true,
        )
    }

    class Factory(
        private val store: OnboardingStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OnboardingViewModel::class.java))
            return OnboardingViewModel(store) as T
        }
    }

    companion object {
        const val PAGE_COUNT = 3
        private const val LAST_PAGE = PAGE_COUNT - 1
    }
}
