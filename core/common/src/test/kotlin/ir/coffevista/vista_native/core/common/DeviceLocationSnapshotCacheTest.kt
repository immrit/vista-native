package ir.coffevista.vista_native.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceLocationSnapshotCacheTest {

    @Test
    fun latestRespectsAgeAndPreservesCityWhenNearbyCoordinatesUpdate() {
        val cache = DeviceLocationSnapshotCache()
        val now = System.currentTimeMillis()
        cache.updateAddress(35.6892, 51.3890, city = "تهران", province = "تهران", capturedAtMillis = now)

        val snap = cache.latest(maxAgeMs = 60_000L)
        assertNotNull(snap)
        assertEquals("تهران", snap?.city)

        // Small jitter (~100m) keeps known city/province
        cache.updateCoordinates(35.6900, 51.3895, capturedAtMillis = now)
        assertEquals("تهران", cache.latest()?.city)

        // Large move clears stale city/province
        cache.updateCoordinates(29.5918, 52.5837, capturedAtMillis = now)
        assertNull(cache.latest()?.city)
    }

    @Test
    fun expiredSnapshotReturnsNullForLatestButStillExistsForLatestAnyUntilCleared() {
        val cache = DeviceLocationSnapshotCache()
        val oldTimestamp = System.currentTimeMillis() - 60 * 60_000L
        cache.updateCoordinates(32.6546, 51.6680, capturedAtMillis = oldTimestamp)

        assertNull(cache.latest(maxAgeMs = 15 * 60_000L))
        assertNotNull(cache.latestAny())

        cache.clear()
        assertNull(cache.latestAny())
    }
}
