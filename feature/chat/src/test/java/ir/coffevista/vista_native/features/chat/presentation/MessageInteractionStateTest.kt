package ir.coffevista.vista_native.features.chat.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageInteractionStateTest {

    @Test
    fun `toggle is ignored outside selection mode`() {
        assertEquals(
            MessageInteractionState.Idle,
            MessageInteractionState.Idle.toggleSelection("message-1"),
        )
    }

    @Test
    fun `selection drops missing keys and exits when no selectable message remains`() {
        val initial = MessageInteractionState.Selecting(listOf("one", "two"))

        assertEquals(
            MessageInteractionState.Selecting(listOf("two")),
            initial.retainSelection(setOf("two")),
        )
        assertEquals(
            MessageInteractionState.Idle,
            initial.retainSelection(emptySet()),
        )
    }

    @Test
    fun `selecting another message retains both stable keys`() {
        assertEquals(
            MessageInteractionState.Selecting(listOf("message-1", "message-2")),
            MessageInteractionState.Selecting(listOf("message-1")).toggleSelection("message-2"),
        )
    }

    @Test
    fun `removing the final selected message returns to idle`() {
        assertEquals(
            MessageInteractionState.Idle,
            MessageInteractionState.Selecting(listOf("message-1")).toggleSelection("message-1"),
        )
    }
}
