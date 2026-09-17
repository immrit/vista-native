package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft

internal const val MAX_ATTACHMENT_CAPTION_LENGTH = 1024

internal fun reorderAttachmentDrafts(
    drafts: List<ChatAttachmentDraft>,
    fromIndex: Int,
    toIndex: Int,
): List<ChatAttachmentDraft> {
    if (fromIndex !in drafts.indices || toIndex !in drafts.indices || fromIndex == toIndex) return drafts
    return drafts.toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
}

internal fun prepareAttachmentDraftsForSend(
    drafts: List<ChatAttachmentDraft>,
    groupAsAlbum: Boolean,
    caption: String,
    mediaGroupId: String?,
): List<ChatAttachmentDraft> {
    val normalizedCaption = caption.trim().take(MAX_ATTACHMENT_CAPTION_LENGTH)
    val normalizedGroupId = mediaGroupId?.trim()?.takeIf(String::isNotEmpty)
        ?.takeIf { groupAsAlbum && drafts.size > 1 }
    return drafts.mapIndexed { index, draft ->
        draft.copy(
            caption = if (index == 0) normalizedCaption else "",
            mediaGroupId = normalizedGroupId,
        )
    }
}
