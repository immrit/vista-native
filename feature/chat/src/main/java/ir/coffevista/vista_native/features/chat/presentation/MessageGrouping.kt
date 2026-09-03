package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.domain.model.Message
import kotlin.math.abs

/**
 * Visual grouping contract shared with Vista Flutter's reverse message list.
 * Index zero is the newest (visually bottom) item. Consecutive messages from
 * the same sender join only when their timestamps are less than 15 minutes
 * apart; this prevents old messages from looking like one continuous bubble.
 */
internal data class MessageGroupPosition(
    val isFirstInGroup: Boolean,
    val isLastInGroup: Boolean,
)

/**
 * Corner contract for the reverse chat list. The first item in a group is the
 * oldest (visually above); the last item is the newest (visually below).
 *
 * Only the edge that joins two bubbles loses its outer radius. Keeping this
 * policy outside the composable prevents own and peer groups from drifting
 * apart as the bubble renderer evolves.
 */
internal data class MessageBubbleCornerRounding(
    val topLeft: Boolean,
    val topRight: Boolean,
    val bottomLeft: Boolean,
    val bottomRight: Boolean,
)

internal fun messageBubbleCornerRounding(
    isMine: Boolean,
    groupPosition: MessageGroupPosition,
): MessageBubbleCornerRounding =
    if (isMine) {
        MessageBubbleCornerRounding(
            topLeft = true,
            topRight = groupPosition.isFirstInGroup,
            bottomLeft = true,
            bottomRight = groupPosition.isLastInGroup,
        )
    } else {
        MessageBubbleCornerRounding(
            topLeft = groupPosition.isFirstInGroup,
            topRight = true,
            bottomLeft = groupPosition.isLastInGroup,
            bottomRight = true,
        )
    }

internal fun messageGroupPosition(
    messages: List<Message>,
    index: Int,
): MessageGroupPosition {
    val current = messages.getOrNull(index)
        ?: return MessageGroupPosition(isFirstInGroup = true, isLastInGroup = true)

    // A larger index is older and therefore visually above the current item.
    val above = messages.getOrNull(index + 1)
    // A smaller index is newer and therefore visually below the current item.
    val below = messages.getOrNull(index - 1)
    return MessageGroupPosition(
        isFirstInGroup = !messagesShareVisualGroup(current, above),
        isLastInGroup = !messagesShareVisualGroup(current, below),
    )
}

private fun messagesShareVisualGroup(current: Message, neighbor: Message?): Boolean =
    neighbor != null &&
        current.senderId == neighbor.senderId &&
        abs(current.createdAtEpochMillis - neighbor.createdAtEpochMillis) < GROUP_WINDOW_MILLIS

private const val GROUP_WINDOW_MILLIS = 15 * 60 * 1_000L
