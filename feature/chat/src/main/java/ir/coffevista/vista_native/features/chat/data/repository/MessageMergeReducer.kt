package ir.coffevista.vista_native.features.chat.data.repository

import ir.coffevista.vista_native.features.chat.domain.model.MergeSource
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus

object MessageMergeReducer {
    fun merge(existing: Message?, incoming: Message, source: MergeSource): Message {
        if (existing == null) return incoming
        require(existing.accountId == incoming.accountId)
        require(existing.conversationId == incoming.conversationId)
        require(sameIdentity(existing, incoming))

        val deletedAt = listOfNotNull(existing.deletedAtEpochMillis, incoming.deletedAtEpochMillis).maxOrNull()
        val editedAt = listOfNotNull(existing.editedAtEpochMillis, incoming.editedAtEpochMillis).maxOrNull()
        val content = when {
            deletedAt != null -> MessageContent.Deleted
            incoming.editedAtEpochMillis != null &&
                incoming.editedAtEpochMillis >= (existing.editedAtEpochMillis ?: Long.MIN_VALUE) -> incoming.content
            source == MergeSource.REST || source == MergeSource.REALTIME -> incoming.content
            else -> existing.content
        }
        return existing.copy(
            serverId = incoming.serverId ?: existing.serverId,
            senderId = incoming.senderId,
            content = content,
            createdAtEpochMillis = minOf(existing.createdAtEpochMillis, incoming.createdAtEpochMillis),
            editedAtEpochMillis = editedAt,
            deletedAtEpochMillis = deletedAt,
            status = if (
                source == MergeSource.OPTIMISTIC &&
                existing.status == MessageStatus.FAILED &&
                incoming.status == MessageStatus.PENDING
            ) {
                MessageStatus.PENDING
            } else {
                monotonicStatus(existing.status, incoming.status)
            },
            replyToMessageId = incoming.replyToMessageId ?: existing.replyToMessageId,
            replyToContent = incoming.replyToContent ?: existing.replyToContent,
            replyToSenderName = incoming.replyToSenderName ?: existing.replyToSenderName,
            replyToKind = incoming.replyToKind ?: existing.replyToKind,
            attachment = when {
                incoming.attachment == null -> existing.attachment
                existing.attachment == null -> incoming.attachment
                else -> incoming.attachment.copy(
                    localUri = existing.attachment.localUri ?: incoming.attachment.localUri,
                    progress = if (incoming.attachment.remoteUrl != null) 1f else incoming.attachment.progress,
                )
            },
            reactions = if (source == MergeSource.REST || source == MergeSource.REALTIME) {
                incoming.reactions
            } else if (incoming.reactions.isNotEmpty()) {
                incoming.reactions
            } else {
                existing.reactions
            },
            isForwarded = incoming.isForwarded || existing.isForwarded,
            forwardedFromSenderName = incoming.forwardedFromSenderName ?: existing.forwardedFromSenderName,
            originalSenderId = incoming.originalSenderId ?: existing.originalSenderId,
            originalMessageId = incoming.originalMessageId ?: existing.originalMessageId,
            // The regular history/realtime envelopes omit `is_pinned` on some
            // backend versions, which deserializes to false. The dedicated
            // pinned endpoint and explicit pin/unpin mutations are authoritative;
            // a generic message refresh must not erase their Room state.
            isPinned = existing.isPinned || incoming.isPinned,
            isMine = incoming.isMine,
        )
    }

    fun reduce(current: List<Message>, incoming: Iterable<Message>, source: MergeSource): List<Message> {
        val result = current.toMutableList()
        incoming.forEach { candidate ->
            val index = result.indexOfFirst { sameIdentity(it, candidate) }
            if (index < 0) result += candidate else result[index] = merge(result[index], candidate, source)
        }
        return result.distinctBy { it.serverId ?: "local:${it.clientId}" }
            .sortedWith(
                compareByDescending<Message> { it.createdAtEpochMillis }
                    .thenByDescending { it.serverId ?: it.clientId },
            )
    }

    fun sameIdentity(first: Message, second: Message): Boolean =
        first.clientId == second.clientId ||
            (first.serverId != null && first.serverId == second.serverId)

    fun monotonicStatus(first: MessageStatus, second: MessageStatus): MessageStatus {
        if (first == MessageStatus.FAILED && second == MessageStatus.PENDING) return first
        if (second == MessageStatus.FAILED && first.rank >= MessageStatus.SENT.rank) return first
        return if (first.rank >= second.rank) first else second
    }

    private val MessageStatus.rank: Int
        get() = when (this) {
            MessageStatus.PENDING -> 0
            MessageStatus.FAILED -> 1
            MessageStatus.SENT -> 2
            MessageStatus.DELIVERED -> 3
            MessageStatus.READ -> 4
        }
}
