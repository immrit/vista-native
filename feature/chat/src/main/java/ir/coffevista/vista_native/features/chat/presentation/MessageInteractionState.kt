package ir.coffevista.vista_native.features.chat.presentation

import androidx.compose.ui.geometry.Rect

/**
 * Single owner for mutually exclusive message interaction modes.
 *
 * A message cannot be contextual and selected at the same time. Keeping this
 * invariant in one type prevents stale menu anchors and gesture conflicts
 * between the list, context surface, and bulk-action toolbar.
 */
sealed interface MessageInteractionState {
    data object Idle : MessageInteractionState

    data class Context(
        /**
         * A stable key is intentionally stored instead of a Message instance.
         * Repository updates can replace or delete a message while the surface
         * is open; the route must resolve the latest instance before an action.
         */
        val messageKey: String,
        val bubbleBounds: Rect?,
    ) : MessageInteractionState

    data class Selecting(
        val messageKeys: List<String>,
    ) : MessageInteractionState {
        init {
            require(messageKeys.isNotEmpty()) { "Selecting requires at least one message" }
        }
    }
}

internal fun MessageInteractionState.selectedMessageKeys(): List<String> =
    (this as? MessageInteractionState.Selecting)?.messageKeys.orEmpty()

internal fun MessageInteractionState.toggleSelection(messageKey: String): MessageInteractionState {
    val selecting = this as? MessageInteractionState.Selecting ?: return this
    val updated = if (messageKey in selecting.messageKeys) {
        selecting.messageKeys - messageKey
    } else {
        selecting.messageKeys + messageKey
    }
    return updated.takeIf { it.isNotEmpty() }
        ?.let(MessageInteractionState::Selecting)
        ?: MessageInteractionState.Idle
}

/**
 * Realtime pagination and deletion can replace the visible message list while
 * selection is open. Never retain an invisible or no-longer-selectable key:
 * an empty result exits selection instead of leaving a dead bulk toolbar.
 */
internal fun MessageInteractionState.retainSelection(availableKeys: Set<String>): MessageInteractionState {
    val selecting = this as? MessageInteractionState.Selecting ?: return this
    val retained = selecting.messageKeys.filterTo(linkedSetOf()) { it in availableKeys }
    return when {
        retained.size == selecting.messageKeys.size -> this
        retained.isEmpty() -> MessageInteractionState.Idle
        else -> MessageInteractionState.Selecting(retained.toList())
    }
}
