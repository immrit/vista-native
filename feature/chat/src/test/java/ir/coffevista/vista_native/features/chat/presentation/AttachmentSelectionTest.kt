package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttachmentSelectionTest {
    @Test
    fun albumSendSharesGroupIdAndKeepsCaptionOnlyOnFirstMessage() {
        val prepared = prepareAttachmentDraftsForSend(
            drafts = listOf(draft("one"), draft("two"), draft("three")),
            groupAsAlbum = true,
            caption = "  کپشن آلبوم  ",
            mediaGroupId = " album-1 ",
        )

        assertEquals(listOf("کپشن آلبوم", "", ""), prepared.map { it.caption })
        assertEquals(listOf("album-1", "album-1", "album-1"), prepared.map { it.mediaGroupId })
    }

    @Test
    fun reorderedFirstAttachmentOwnsAlbumCaption() {
        val reordered = reorderAttachmentDrafts(
            drafts = listOf(draft("one"), draft("two"), draft("three")),
            fromIndex = 2,
            toIndex = 0,
        )
        val prepared = prepareAttachmentDraftsForSend(
            drafts = reordered,
            groupAsAlbum = true,
            caption = "ترتیب تازه",
            mediaGroupId = "album-2",
        )

        assertEquals(listOf("three.jpg", "one.jpg", "two.jpg"), prepared.map { it.fileName })
        assertEquals(listOf("ترتیب تازه", "", ""), prepared.map { it.caption })
    }

    @Test
    fun independentFilesNeverReceiveMediaGroupId() {
        val prepared = prepareAttachmentDraftsForSend(
            drafts = listOf(draft("one"), draft("two")),
            groupAsAlbum = false,
            caption = "شرح",
            mediaGroupId = "album-ignored",
        )

        assertEquals(listOf("شرح", ""), prepared.map { it.caption })
        prepared.forEach { assertNull(it.mediaGroupId) }
    }

    private fun draft(name: String) = ChatAttachmentDraft(
        uri = "content://fixture/$name",
        fileName = "$name.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 12,
        kind = AttachmentKind.IMAGE,
    )
}
