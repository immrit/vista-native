package ir.coffevista.vista_native.features.chat.presentation.components

import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.TransferState

/**
 * Conversation-scoped capabilities supplied by the route. This deliberately
 * does not infer permissions from a message: server roles and moderation
 * policy must eventually arrive here as explicit values.
 */
internal data class MessageContextCapabilities(
    val conversationType: ConversationType = ConversationType.PRIVATE,
    val allowsReactions: Boolean = true,
    val allowsForwarding: Boolean = true,
    val allowsPinning: Boolean = true,
)

/**
 * The only source of truth for what a message context surface may expose.
 *
 * A local, failed, or deleted message must not accidentally inherit remote
 * message actions merely because it still has text or an old server id.
 */
internal data class MessageContextActionPolicy(
    val canReact: Boolean,
    val canReply: Boolean,
    val canCopy: Boolean,
    val canForward: Boolean,
    val canTogglePinned: Boolean,
    val canEdit: Boolean,
    val canSelect: Boolean,
    val canViewInfo: Boolean,
    val canDelete: Boolean,
) {
    val visibleActionCount: Int
        get() = listOf(
            canReply,
            canCopy,
            canForward,
            canTogglePinned,
            canEdit,
            canSelect,
            canViewInfo,
            canDelete,
        ).count { it }
}

internal fun Message.contextActionPolicy(
    capabilities: MessageContextCapabilities = MessageContextCapabilities(),
): MessageContextActionPolicy {
    val isDeleted = content is MessageContent.Deleted || deletedAtEpochMillis != null
    val isTextOnly = content is MessageContent.Text && attachment == null
    val isAttachmentTransferComplete = attachment?.transferState == null ||
        attachment.transferState == TransferState.COMPLETE
    val isServerConfirmed = serverId != null && status !in setOf(
        MessageStatus.PENDING,
        MessageStatus.FAILED,
    )
    val canInteractRemotely = !isDeleted && isServerConfirmed && isAttachmentTransferComplete

    return MessageContextActionPolicy(
        canReact = canInteractRemotely && capabilities.allowsReactions,
        canReply = canInteractRemotely,
        canCopy = !isDeleted && isTextOnly,
        // Secret conversations must never expose a forwarding or pin route.
        // The API remains authoritative for all other conversation policies.
        canForward = canInteractRemotely &&
            capabilities.allowsForwarding &&
            capabilities.conversationType != ConversationType.SECRET,
        canTogglePinned = canInteractRemotely &&
            capabilities.allowsPinning &&
            capabilities.conversationType != ConversationType.SECRET,
        canEdit = canInteractRemotely && isMine && isTextOnly,
        canSelect = !isDeleted,
        canViewInfo = isServerConfirmed,
        canDelete = !isDeleted,
    )
}
