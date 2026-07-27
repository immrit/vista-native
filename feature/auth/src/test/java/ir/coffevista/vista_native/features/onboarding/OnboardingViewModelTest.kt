package ir.coffevista.vista_native.features.onboarding

import ir.coffevista.vista_native.core.datastore.OnboardingStore
import ir.coffevista.vista_native.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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
    fun finalNextPersistsCompletionExactlyOnce() = runTest {
        val store = RecordingOnboardingStore()
        val viewModel = OnboardingViewModel(store)
        viewModel.onAction(OnboardingAction.PageChanged(2))

        viewModel.onAction(OnboardingAction.Next)
        viewModel.onAction(OnboardingAction.Next)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.completed)
        assertEquals(1, store.markCalls)
    }

    @Test
    fun skipPersistsCompletionFromAnyPage() = runTest {
        val store = RecordingOnboardingStore()
        val viewModel = OnboardingViewModel(store)

        viewModel.onAction(OnboardingAction.Skip)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.completed)
        assertEquals(1, store.markCalls)
    }
}

private class RecordingOnboardingStore : OnboardingStore {
    var markCalls = 0

    override suspend fun isCompleted() = markCalls > 0

    override suspend fun markCompleted() {
        markCalls += 1
    }
}
