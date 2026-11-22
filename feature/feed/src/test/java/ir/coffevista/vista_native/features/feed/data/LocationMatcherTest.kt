package ir.coffevista.vista_native.features.feed.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationMatcherTest {
    @Test
    fun `normalize unifies arabic and persian characters and strips zwnj`() {
        assertEquals("ابادان", LocationMatcher.normalize("آبادان"))
        assertEquals("کیلومتر یک", LocationMatcher.normalize("كيلومتر\u200C يك"))
        assertEquals("فلکه ذوالفقار", LocationMatcher.normalize("  فلکه\u200C ذوالفقار  "))
    }

    @Test
    fun `matches exact, primary token, and hierarchical location strings`() {
        assertTrue(LocationMatcher.matches("آبادان", "آبادان"))
        assertTrue(LocationMatcher.matches("آبادان، خوزستان", "آبادان"))
        assertTrue(LocationMatcher.matches("فلکه ذوالفقار، آبادان", "آبادان"))
        assertTrue(LocationMatcher.matches("آبادان", "آبادان، ایران"))
        assertTrue(LocationMatcher.matches("كافه رستوران امير", "کافه رستوران امیر"))
    }

    @Test
    fun `matches returns false for unrelated or blank locations`() {
        assertFalse(LocationMatcher.matches(null, "آبادان"))
        assertFalse(LocationMatcher.matches("   ", "آبادان"))
        assertFalse(LocationMatcher.matches("تهران", "آبادان"))
        assertFalse(LocationMatcher.matches("شیراز، فارس", "اصفهان"))
    }

    @Test
    fun `haversine computes realistic distance in meters`() {
        // Tehran Azadi Tower (35.6997, 51.3380) to Milad Tower (35.7448, 51.3753) ~ 6.0 km
        val dist = LocationMatcher.haversineMeters(35.6997, 51.3380, 35.7448, 51.3753)
        assertTrue("Expected ~6km but was $dist", dist in 5_500.0..6_500.0)
        assertEquals(0.0, LocationMatcher.haversineMeters(30.3392, 48.3043, 30.3392, 48.3043), 0.001)
    }

    @Test
    fun `formatDistanceFa formats meters and kilometers with persian digits`() {
        assertNull(LocationMatcher.formatDistanceFa(null))
        assertNull(LocationMatcher.formatDistanceFa(-10.0))
        assertEquals("کمتر از ۵۰ متر", LocationMatcher.formatDistanceFa(25.0))
        assertEquals("۸۰۰ متر", LocationMatcher.formatDistanceFa(810.0))
        assertEquals("۱.۵ کیلومتر", LocationMatcher.formatDistanceFa(1_520.0))
        assertEquals("۵ کیلومتر", LocationMatcher.formatDistanceFa(5_020.0))
        assertEquals("۲۰ کیلومتر", LocationMatcher.formatDistanceFa(20_300.0))
    }
}
