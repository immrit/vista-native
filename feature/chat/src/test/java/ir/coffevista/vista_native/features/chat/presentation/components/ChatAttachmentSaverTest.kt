package ir.coffevista.vista_native.features.chat.presentation.components

import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAttachmentSaverTest {

    @Test
    fun `buildAttachmentSaveSpec assigns correct directories and extensions for image, document, and music`() {
        val imageSpec = buildAttachmentSaveSpec(
            kind = AttachmentKind.IMAGE,
            fileName = "vacation.png",
            mimeType = "image/png",
            sourceUrl = "https://cdn.example.test/u/img1",
            epochMillis = 1_000L,
        )
        assertEquals("vacation.png", imageSpec.displayName)
        assertEquals("image/png", imageSpec.mimeType)
        assertTrue(imageSpec.relativePath.endsWith("/Vista"))
        assertTrue(imageSpec.relativePath.contains("Pictures"))
        assertEquals("تصویر در گالری ذخیره شد", imageSpec.successMessage)

        val docSpec = buildAttachmentSaveSpec(
            kind = AttachmentKind.DOCUMENT,
            fileName = "report/../../final.pdf",
            mimeType = null,
            sourceUrl = "https://cdn.example.test/u/doc",
            epochMillis = 2_000L,
        )
        assertEquals("report_.._.._final.pdf", docSpec.displayName)
        assertEquals("application/pdf", docSpec.mimeType)
        assertTrue(docSpec.relativePath.contains("Download"))
        assertEquals("فایل در پوشه دانلودها ذخیره شد", docSpec.successMessage)

        val musicSpec = buildAttachmentSaveSpec(
            kind = AttachmentKind.AUDIO,
            fileName = "Track Name",
            mimeType = "audio/mpeg",
            sourceUrl = "https://cdn.example.test/u/song.mp3?token=1",
            epochMillis = 3_000L,
        )
        assertEquals("Track Name.mp3", musicSpec.displayName)
        assertEquals("audio/mpeg", musicSpec.mimeType)
        assertTrue(musicSpec.relativePath.contains("Music"))
        assertEquals("موسیقی در پوشه موسیقی ذخیره شد", musicSpec.successMessage)
    }

    @Test
    fun `isSecretLocalUri detects secret upload and encrypted cache paths`() {
        assertTrue(isSecretLocalUri("/data/user/0/app/cache/sec_up_123.tmp"))
        assertTrue(isSecretLocalUri("file:///data/user/0/app/files/enc_chat_abc.dat"))
        assertTrue(isSecretLocalUri("/data/files/secret_pending_uploads/item.jpg"))
        assertFalse(isSecretLocalUri("/data/user/0/app/files/chat_downloads/song.mp3"))
        assertFalse(isSecretLocalUri("https://cdn.vista.ir/media/file.pdf"))
        assertFalse(isSecretLocalUri(null))
    }
}
