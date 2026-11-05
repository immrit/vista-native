package ir.coffevista.vista_native.features.chat.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream

class FileSecurityScannerTest {

    @Test
    fun `valid pdf document passes safety verification`() {
        val pdfBytes = "%PDF-1.7\n1 0 obj...".encodeToByteArray()
        FileSecurityScanner.verifySafety("contract.pdf", pdfBytes)
    }

    @Test
    fun `disguised windows PE executable renamed as pdf is blocked`() {
        val fakePdf = "MZ".encodeToByteArray() + "This program cannot be run in DOS mode".encodeToByteArray()
        assertRejected("malware.pdf", fakePdf)
    }

    @Test
    fun `direct executable extensions are immediately blocked`() {
        val sample = "MZ".encodeToByteArray()
        assertRejected("app.exe", sample)
        assertRejected("game.apk", sample)
        assertRejected("script.bat", sample)
        assertRejected("hack.sh", sample)
        assertRejected("payload.vbs", sample)
        assertRejected("virus.js", sample)
        assertRejected("library.dll", sample)
    }

    @Test
    fun `disguised ELF binary is detected and rejected`() {
        val elfHeader = byteArrayOf(0x7F, 0x45, 0x4C, 0x46) + "ELF_CONTENT".encodeToByteArray()
        assertRejected("photo.png", elfHeader)
    }

    @Test
    fun `disguised android DEX bytecode is detected and rejected`() {
        val dexHeader = byteArrayOf(0x64, 0x65, 0x78, 0x0A) + "035\u0000".encodeToByteArray()
        assertRejected("music.mp3", dexHeader)
    }

    @Test
    fun `valid image formats pass verification`() {
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        FileSecurityScanner.verifySafety("photo.png", pngBytes)

        val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        FileSecurityScanner.verifySafety("banner.jpg", jpegBytes)

        val gifBytes = "GIF89a".encodeToByteArray()
        FileSecurityScanner.verifySafety("cat.gif", gifBytes)
    }

    @Test
    fun `valid audio formats pass verification`() {
        val mp3Bytes = "ID3".encodeToByteArray() + byteArrayOf(0x04, 0x00, 0x00)
        FileSecurityScanner.verifySafety("track.mp3", mp3Bytes)

        val oggBytes = "OggS".encodeToByteArray() + byteArrayOf(0x00, 0x02)
        FileSecurityScanner.verifySafety("sound.ogg", oggBytes)
    }

    @Test
    fun `unsupported document extensions are rejected`() {
        val docBytes = "plain text".encodeToByteArray()
        assertRejected("notes.docx", docBytes)
        assertRejected("archive.zip", docBytes)
        assertRejected("data.bin", docBytes)
    }

    @Test
    fun `sha256 calculation computes accurate cryptographic digest`() {
        val input = "Hello Vista Secure Chat".encodeToByteArray()
        val hash = FileSecurityScanner.computeSha256(ByteArrayInputStream(input))
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in "0123456789abcdef" })
    }

    private fun assertRejected(fileName: String, header: ByteArray) {
        try {
            FileSecurityScanner.verifySafety(fileName, header)
            fail("Expected security rejection for $fileName")
        } catch (_: SecurityException) {
            // expected
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
