package ir.coffevista.vista_native.features.chat.presentation

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import java.util.Calendar
import kotlin.math.abs

internal data class MessageRenderItem(
    val primaryIndex: Int,
    val messages: List<Message>,
) {
    val primary: Message get() = messages.first()
    val oldest: Message get() = messages.last()
    val isAlbum: Boolean get() = messages.size > 1
    val stableKey: String = if (isAlbum) {
        "album:" + messages.joinToString("|") { it.stableKey }
    } else {
        primary.stableKey
    }
}

internal fun buildMessageRenderItems(messages: List<Message>): List<MessageRenderItem> {
    if (messages.isEmpty()) return emptyList()
    val result = mutableListOf<MessageRenderItem>()
    var index = 0
    while (index < messages.size) {
        val anchor = messages[index]
        if (anchor.isAlbumImage()) {
            val grouped = mutableListOf(anchor)
            val groupId = anchor.attachment?.mediaGroupId?.trim().orEmpty()
            var lookAhead = index + 1
            while (lookAhead < messages.size && grouped.size < MAX_ALBUM_ITEMS) {
                val candidate = messages[lookAhead]
                val append = if (groupId.isNotEmpty()) {
                    candidate.isAlbumImage() &&
                        candidate.senderId == anchor.senderId &&
                        candidate.attachment?.mediaGroupId?.trim() == groupId
                } else {
                    canAppendImplicitAlbum(grouped.last(), candidate, anchor)
                }
                if (!append) break
                grouped += candidate
                lookAhead += 1
            }
            if (grouped.size > 1) {
                result += MessageRenderItem(index, grouped)
                index = lookAhead
                continue
            }
        }
        result += MessageRenderItem(index, listOf(anchor))
        index += 1
    }
    return result
}

internal fun renderItemIndexForMessage(
    renderItems: List<MessageRenderItem>,
    messageKey: String,
): Int = renderItems.indexOfFirst { item ->
    item.messages.any { message ->
        message.stableKey == messageKey ||
            message.serverId == messageKey ||
            message.clientId == messageKey
    }
}
internal fun visibleMessageDateEpochMillis(
    renderItems: List<MessageRenderItem>,
    visibleIndices: List<Int>,
): Long? {
    if (renderItems.isEmpty() || visibleIndices.isEmpty()) return null
    val topItemIndex = visibleIndices
        .filter { it in renderItems.indices }
        .maxOrNull() ?: return null
    return renderItems[topItemIndex].oldest.createdAtEpochMillis
}

internal fun visibleMessageDateEpochMillis(
    renderItems: List<MessageRenderItem>,
    firstVisibleItemIndex: Int,
): Long? = renderItems.getOrNull(firstVisibleItemIndex)?.primary?.createdAtEpochMillis

internal fun albumRowIndices(itemCount: Int): List<List<Int>> {
    val count = itemCount.coerceIn(0, MAX_ALBUM_ITEMS)
    if (count == 0) return emptyList()
    if (count <= 4) {
        return when (count) {
            1 -> listOf(listOf(0))
            2 -> listOf(listOf(0, 1))
            3 -> listOf(listOf(0), listOf(1, 2))
            else -> listOf(listOf(0, 1), listOf(2, 3))
        }
    }
    val rows = (0 until count).chunked(3).mapTo(mutableListOf()) { it.toMutableList() }
    if (rows.size >= 2 && rows.last().size == 1 && rows[rows.lastIndex - 1].size == 3) {
        val previousRow = rows[rows.lastIndex - 1]
        rows.last().add(0, previousRow.removeAt(previousRow.lastIndex))
    }
    return rows
}

private fun Message.isAlbumImage(): Boolean =
    attachment?.kind == AttachmentKind.IMAGE &&
        (!attachment.localUri.isNullOrBlank() || !attachment.remoteUrl.isNullOrBlank())

private fun canAppendImplicitAlbum(
    previous: Message,
    candidate: Message,
    anchor: Message,
): Boolean =
    candidate.isAlbumImage() &&
        candidate.senderId == anchor.senderId &&
        abs(previous.createdAtEpochMillis - candidate.createdAtEpochMillis) <= IMPLICIT_ALBUM_NEIGHBOR_MILLIS &&
        abs(anchor.createdAtEpochMillis - candidate.createdAtEpochMillis) <= IMPLICIT_ALBUM_TOTAL_MILLIS

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

internal data class UnreadMessageWindow(
    val count: Int,
    val dividerIndex: Int?,
)

/**
 * Flutter anchors the unread divider to the first already-read/own row after
 * the newest contiguous unread peer messages. When every loaded row is still
 * unread, the oldest loaded row owns the divider.
 */
internal fun unreadMessageWindow(messages: List<Message>): UnreadMessageWindow {
    var unreadCount = 0
    messages.forEachIndexed { index, message ->
        if (message.isMine || message.status == MessageStatus.READ) {
            return UnreadMessageWindow(
                count = unreadCount,
                dividerIndex = index.takeIf { unreadCount > 0 },
            )
        }
        unreadCount += 1
    }
    return UnreadMessageWindow(
        count = unreadCount,
        dividerIndex = messages.lastIndex.takeIf { unreadCount > 0 },
    )
}

internal const val TELEGRAM_X_BUBBLE_RADIUS_DP = 18f
internal const val TELEGRAM_X_BUBBLE_MERGED_RADIUS_DP = 6f
internal const val TELEGRAM_X_IMAGE_INNER_RADIUS_DP = 4f
internal const val TELEGRAM_X_BUBBLE_GAP_SEPARATE_DP = 3f
internal const val TELEGRAM_X_BUBBLE_GAP_MERGED_DP = 1.5f

internal data class TelegramXCornerRadiiDp(
    val topLeft: Float,
    val topRight: Float,
    val bottomLeft: Float,
    val bottomRight: Float,
)

internal fun TelegramXCornerRadiiDp.toAbsoluteShape(): AbsoluteRoundedCornerShape =
    AbsoluteRoundedCornerShape(
        topLeft = topLeft.dp,
        topRight = topRight.dp,
        bottomLeft = bottomLeft.dp,
        bottomRight = bottomRight.dp,
    )

internal fun telegramXOuterCornerRadii(
    cornerRounding: MessageBubbleCornerRounding,
): TelegramXCornerRadiiDp = TelegramXCornerRadiiDp(
    topLeft = if (cornerRounding.topLeft) TELEGRAM_X_BUBBLE_RADIUS_DP else TELEGRAM_X_BUBBLE_MERGED_RADIUS_DP,
    topRight = if (cornerRounding.topRight) TELEGRAM_X_BUBBLE_RADIUS_DP else TELEGRAM_X_BUBBLE_MERGED_RADIUS_DP,
    bottomLeft = if (cornerRounding.bottomLeft) TELEGRAM_X_BUBBLE_RADIUS_DP else TELEGRAM_X_BUBBLE_MERGED_RADIUS_DP,
    bottomRight = if (cornerRounding.bottomRight) TELEGRAM_X_BUBBLE_RADIUS_DP else TELEGRAM_X_BUBBLE_MERGED_RADIUS_DP,
)

internal fun telegramXInnerCornerRadii(
    cornerRounding: MessageBubbleCornerRounding,
    insetDp: Float = 2f,
    roundTop: Boolean = true,
    roundBottom: Boolean = true,
    flatCornerDp: Float = TELEGRAM_X_IMAGE_INNER_RADIUS_DP,
): TelegramXCornerRadiiDp {
    val outer = telegramXOuterCornerRadii(cornerRounding)
    fun inner(edgeRounded: Boolean, outerDp: Float): Float =
        if (!edgeRounded) flatCornerDp else (outerDp - insetDp).coerceAtLeast(flatCornerDp)

    return TelegramXCornerRadiiDp(
        topLeft = inner(roundTop, outer.topLeft),
        topRight = inner(roundTop, outer.topRight),
        bottomLeft = inner(roundBottom, outer.bottomLeft),
        bottomRight = inner(roundBottom, outer.bottomRight),
    )
}

/**
 * Exact Telegram X corner rounding policy (`TGMessage.java:3884-3900`):
 * `mergeTop = !headerDisabled()` (true for all regular bubbles) and
 * `mergeBottom = !groupPosition.isLastInGroup`.
 *
 * Outgoing (right-aligned): topRight is always 6.dp; bottomRight is 18.dp on standalone/last, 6.dp otherwise.
 * Incoming (left-aligned): topLeft is always 6.dp; bottomLeft is 18.dp on standalone/last, 6.dp otherwise.
 */
internal fun messageBubbleCornerRounding(
    isMine: Boolean,
    groupPosition: MessageGroupPosition,
): MessageBubbleCornerRounding =
    if (isMine) {
        MessageBubbleCornerRounding(
            topLeft = true,
            topRight = false,
            bottomLeft = true,
            bottomRight = groupPosition.isLastInGroup,
        )
    } else {
        MessageBubbleCornerRounding(
            topLeft = false,
            topRight = true,
            bottomLeft = groupPosition.isLastInGroup,
            bottomRight = true,
        )
    }

internal fun messageGroupPosition(
    messages: List<Message>,
    index: Int,
    spanLength: Int = 1,
): MessageGroupPosition {
    val currentNewest = messages.getOrNull(index)
        ?: return MessageGroupPosition(isFirstInGroup = true, isLastInGroup = true)

    val endIndex = (index + spanLength.coerceAtLeast(1) - 1).coerceAtMost(messages.lastIndex)
    val currentOldest = messages[endIndex]
    // A larger index is older and therefore visually above the current item.
    val above = messages.getOrNull(endIndex + 1)
    // A smaller index is newer and therefore visually below the current item.
    val below = messages.getOrNull(index - 1)
    return MessageGroupPosition(
        isFirstInGroup = !messagesShareVisualGroup(currentOldest, above),
        isLastInGroup = !messagesShareVisualGroup(currentNewest, below),
    )
}

private fun messagesShareVisualGroup(current: Message, neighbor: Message?): Boolean =
    neighbor != null &&
        !current.isSystemNotice &&
        !neighbor.isSystemNotice &&
        current.senderId == neighbor.senderId &&
        current.isMine == neighbor.isMine &&
        abs(current.createdAtEpochMillis - neighbor.createdAtEpochMillis) < GROUP_WINDOW_MILLIS &&
        isSameCalendarDay(current.createdAtEpochMillis, neighbor.createdAtEpochMillis)

private fun isSameCalendarDay(firstEpochMillis: Long, secondEpochMillis: Long): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = firstEpochMillis }
    val second = Calendar.getInstance().apply { timeInMillis = secondEpochMillis }
    return first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

private const val GROUP_WINDOW_MILLIS = 15 * 60 * 1_000L
private const val IMPLICIT_ALBUM_NEIGHBOR_MILLIS = 25_000L
private const val IMPLICIT_ALBUM_TOTAL_MILLIS = 60_000L
private const val MAX_ALBUM_ITEMS = 10
