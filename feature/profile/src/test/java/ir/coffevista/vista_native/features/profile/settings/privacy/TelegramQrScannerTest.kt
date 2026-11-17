package ir.coffevista.vista_native.features.profile.settings.privacy

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Test

class TelegramQrScannerTest {

    @Test
    fun `decodeQrFromYPlane decodes normal and inverted vista login QR frames`() {
        val payload = "vista://login?token=AbCdEfGhIjKlMnOpQrStUvWx"
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 220, 220)

        val frameW = 640
        val frameH = 480
        val offsetX = (frameW - matrix.width) / 2
        val offsetY = (frameH - matrix.height) / 2

        // Light background with dark modules
        val normalY = ByteArray(frameW * frameH) { 0xE6.toByte() }
        // Dark background with bright modules (simulates dark mode screen inversion)
        val invertedY = ByteArray(frameW * frameH) { 0x1A.toByte() }

        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                val idx = (offsetY + y) * frameW + (offsetX + x)
                if (matrix.get(x, y)) {
                    normalY[idx] = 0x12.toByte()
                    invertedY[idx] = 0xF0.toByte()
                }
            }
        }

        assertEquals(payload, decodeQrFromYPlane(normalY, frameW, frameH))
        assertEquals(payload, decodeQrFromYPlane(invertedY, frameW, frameH))
    }
}
