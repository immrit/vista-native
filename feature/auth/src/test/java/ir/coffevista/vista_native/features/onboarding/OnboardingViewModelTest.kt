package ir.coffevista.vista_native.features.onboarding

import ir.coffevista.vista_native.core.datastore.OnboardingStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingViewModelTest {
    @Test
    fun nextAndPreviousStayInsideTheThreePageFlow() {
        val viewModel = OnboardingViewModel(RecordingOnboardingStore())

        viewModel.onAction(OnboardingAction.Next)
        viewModel.onAction(OnboardingAction.Next)
        viewModel.onAction(OnboardingAction.Previous)

        assertEquals(1, viewModel.state.value.page)
        assertFalse(viewModel.state.value.completed)
    }

    @Test
    fun finalNextPersistsCompletionExactlyOnce() {
        val store = RecordingOnboardingStore()
        val viewModel = OnboardingViewModel(store)
        viewModel.onAction(OnboardingAction.PageChanged(2))

        viewModel.onAction(OnboardingAction.Next)
        viewModel.onAction(OnboardingAction.Next)

        assertTrue(viewModel.state.value.completed)
        assertEquals(1, store.markCalls)
    }

    @Test
    fun skipPersistsCompletionFromAnyPage() {
        val store = RecordingOnboardingStore()
        val viewModel = OnboardingViewModel(store)

        viewModel.onAction(OnboardingAction.Skip)

        assertTrue(viewModel.state.value.completed)
        assertEquals(1, store.markCalls)
    }
}

private class RecordingOnboardingStore : OnboardingStore {
    var markCalls = 0

    override fun isCompleted() = markCalls > 0

    override fun markCompleted() {
        markCalls += 1
    }
}
