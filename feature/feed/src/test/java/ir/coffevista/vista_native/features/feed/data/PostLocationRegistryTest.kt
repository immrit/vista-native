package ir.coffevista.vista_native.features.feed.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostLocationRegistryTest {

    @Test
    fun recordAndResolvePreferRemoteAndFallbackToRecordedLocation() {
        val registry = PostLocationRegistry()
        registry.record("post-1", "  تهران، ایران  ")

        assertEquals("تهران، ایران", registry.get("post-1"))
        assertEquals("تهران، ایران", registry.resolve("post-1", remoteLocation = null))
        assertEquals("تهران، ایران", registry.resolve("post-1", remoteLocation = "   "))
        assertEquals("شیراز", registry.resolve("post-1", remoteLocation = "شیراز"))
        assertEquals("شیراز", registry.get("post-1"))
    }

    @Test
    fun blanksAndExceedingMaxEntriesAreHandledPredictably() {
        val registry = PostLocationRegistry()
        registry.record("", "تهران")
        registry.record("post-blank", "   ")
        assertNull(registry.get(""))
        assertNull(registry.get("post-blank"))

        repeat(260) { index ->
            registry.record("p-$index", "City $index")
        }
        assertNull(registry.get("p-0"))
        assertEquals("City 259", registry.get("p-259"))
    }
}
