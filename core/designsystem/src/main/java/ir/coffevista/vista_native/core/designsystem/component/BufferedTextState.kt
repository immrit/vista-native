package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Tracks locally emitted text edits against asynchronous external state updates
 * (for example, ViewModel `StateFlow` updates) so stale echoes from earlier
 * keystrokes never overwrite newer characters during fast typing.
 */
class BufferedTextTracker(
    initialText: String,
    private val maxPending: Int = 32,
) {
    var currentText: String = initialText
        private set

    private val pendingEmits = ArrayDeque<String>()

    fun onLocalInput(newText: String) {
        pendingEmits.addLast(newText)
        while (pendingEmits.size > maxPending) {
            pendingEmits.removeFirst()
        }
        currentText = newText
    }

    fun reconcileExternal(externalText: String): String {
        if (externalText == currentText) {
            pendingEmits.clear()
            return currentText
        }
        val idx = pendingEmits.indexOf(externalText)
        if (idx >= 0) {
            repeat(idx + 1) {
                pendingEmits.removeFirst()
            }
            return currentText
        }
        pendingEmits.clear()
        currentText = externalText
        return externalText
    }
}

@Composable
fun rememberBufferedTextState(
    externalValue: String,
    onValueChange: (String) -> Unit,
    maxLength: Int? = null,
): Pair<String, (String) -> Unit> {
    val tracker = remember { BufferedTextTracker(externalValue) }
    var text by remember { mutableStateOf(externalValue) }

    val reconciled = tracker.reconcileExternal(externalValue)
    if (text != reconciled) {
        text = reconciled
    }

    val onLocalChange: (String) -> Unit = { updated ->
        if (maxLength == null || updated.length <= maxLength) {
            tracker.onLocalInput(updated)
            text = updated
            onValueChange(updated)
        }
    }

    return text to onLocalChange
}
