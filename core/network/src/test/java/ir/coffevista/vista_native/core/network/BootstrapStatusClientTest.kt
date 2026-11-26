package ir.coffevista.vista_native.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootstrapStatusClientTest {
    @Test
    fun parsesControlPlaneV3BrandOrnamentAmbientCopyAndPricing() {
        val raw = """
            {
              "version": 12,
              "chat": { "max_group_members": 35 },
              "ui": {
                "brand_ornament": {
                  "enabled": true,
                  "preset": "santa_hat",
                  "offset_x": 30,
                  "offset_y": -12,
                  "scale": 1.35,
                  "rotation_deg": -15,
                  "tap_action": "modal",
                  "tap_title": "سال نو مبارک",
                  "tap_message": "جشنواره زمستانی ویستا"
                },
                "ambient_effect": {
                  "effect": "snow",
                  "density": "medium"
                },
                "navigation_slots": [
                  { "key": "services", "label": "بازارچه", "visible": true, "sort_order": 2, "badge_text": "جدید", "icon": "grid", "route": "vista://services" },
                  { "key": "ai", "label": "ویستا AI", "visible": true, "sort_order": 5, "badge_text": "AI", "icon": "sparkles", "route": "https://ai.cafevista.ir" }
                ],
                "copy_overrides": {
                  "feed_for_you_tab": "پیشنهادی",
                  "feed_following_tab": "دوستان",
                  "maintenance_title": "به‌روزرسانی فوری",
                  "maintenance_message": "تا ۱۰ دقیقه دیگر برمی‌گردیم"
                }
              },
              "pricing": {
                "payment_gateway": "zibal",
                "plans": {
                  "monthly": { "enabled": true, "original_price_toman": 99000, "badge_text": "تخفیف", "recommended": false },
                  "yearly": { "enabled": false, "original_price_toman": 890000, "badge_text": "ویژه", "recommended": true }
                },
                "premium_features": [
                  "تیک طلایی ویستا",
                  "استوری ۴۸ ساعته"
                ]
              }
            }
        """.trimIndent()

        val parsed = parseRemoteConfigSnapshot(Json.parseToJsonElement(raw).jsonObject)

        assertEquals(12L, parsed.version)
        assertEquals(35, parsed.maxGroupMembers)
        assertTrue(parsed.brandOrnament.enabled)
        assertEquals("santa_hat", parsed.brandOrnament.preset)
        // offsetX is clamped to [-24..24]
        assertEquals(24, parsed.brandOrnament.offsetX)
        assertEquals(-12, parsed.brandOrnament.offsetY)
        assertEquals(1.35f, parsed.brandOrnament.scale, 0.001f)
        assertEquals(-15, parsed.brandOrnament.rotationDeg)
        assertEquals("modal", parsed.brandOrnament.tapAction)
        assertEquals("سال نو مبارک", parsed.brandOrnament.tapTitle)

        assertEquals("snow", parsed.ambientEffect.effect)
        assertEquals("medium", parsed.ambientEffect.density)

        assertEquals(2, parsed.navigationSlots.size)
        assertEquals("بازارچه", parsed.navigationSlots.first().label)
        assertEquals("جدید", parsed.navigationSlots.first().badgeText)
        assertEquals("grid", parsed.navigationSlots.first().icon)
        assertEquals("vista://services", parsed.navigationSlots.first().route)
        assertEquals("ai", parsed.navigationSlots[1].key)
        assertEquals("sparkles", parsed.navigationSlots[1].icon)
        assertEquals("https://ai.cafevista.ir", parsed.navigationSlots[1].route)

        assertEquals("پیشنهادی", parsed.copyOverrides.feedForYouTab)
        assertEquals("دوستان", parsed.copyOverrides.feedFollowingTab)
        assertEquals("به‌روزرسانی فوری", parsed.copyOverrides.maintenanceTitle)

        assertEquals("zibal", parsed.paymentGateway)
        assertEquals(99000L, parsed.pricingMeta["monthly"]?.originalPriceToman)
        assertFalse(parsed.pricingMeta["yearly"]?.enabled ?: true)
        assertEquals(listOf("تیک طلایی ویستا", "استوری ۴۸ ساعته"), parsed.premiumFeatures)
    }
}
