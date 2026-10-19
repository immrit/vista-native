package ir.coffevista.vista_native.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingShareCacheTest {
    @Test
    fun mimeTypeValidationRejectsControlCharactersAndOversizedValues() {
        assertTrue(IncomingShareCache.isSafeMimeType("image/jpeg"))
        assertTrue(IncomingShareCache.isSafeMimeType("*/*"))
        assertFalse(IncomingShareCache.isSafeMimeType("image/jpeg\r\ntext/plain"))
        assertFalse(IncomingShareCache.isSafeMimeType("a/" + "b".repeat(128)))
    }

    @Test
    fun cacheExtensionUsesOnlyAllowlistedMimeMappings() {
        assertEquals(".jpg", IncomingShareCache.extensionFor("image/jpeg"))
        assertEquals(".mp4", IncomingShareCache.extensionFor("video/mp4"))
        assertEquals(".bin", IncomingShareCache.extensionFor("application/x-custom"))
    }
}
