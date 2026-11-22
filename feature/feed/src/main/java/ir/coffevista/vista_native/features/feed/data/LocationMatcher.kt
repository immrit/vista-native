package ir.coffevista.vista_native.features.feed.data

import ir.coffevista.vista_native.core.common.toPersianDigits
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object LocationMatcher {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private val DIACRITICS_REGEX = Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]")
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when (ch) {
                'ي', 'ى', 'ئ' -> sb.append('ی')
                'ك' -> sb.append('ک')
                'ة' -> sb.append('ه')
                'أ', 'إ', 'آ' -> sb.append('ا')
                'ؤ' -> sb.append('و')
                '\u200C', '\u200D', '\u200E', '\u200F', '\u0640' -> Unit
                else -> sb.append(ch)
            }
        }
        val stripped = DIACRITICS_REGEX.replace(sb.toString(), "")
        return WHITESPACE_REGEX.replace(stripped.trim(), " ").lowercase(Locale.ROOT)
    }

    fun primaryToken(raw: String): String {
        val firstSegment = raw.substringBefore(',').substringBefore('،')
        return normalize(firstSegment)
    }

    fun matches(candidate: String?, target: String): Boolean {
        if (candidate.isNullOrBlank() || target.isBlank()) return false
        val c = normalize(candidate)
        val t = normalize(target)
        if (c.isBlank() || t.isBlank()) return false

        val cPrimary = primaryToken(candidate)
        val tPrimary = primaryToken(target)

        if (c == t) return true
        if (cPrimary.isNotBlank() && cPrimary == tPrimary) return true

        val shorterLen = minOf(c.length, t.length)
        if (shorterLen >= 2 && (c.contains(t) || t.contains(c))) return true

        if (tPrimary.length >= 2 &&
            candidate.split('،', ',').any { part ->
                val p = normalize(part)
                p.isNotBlank() && p == tPrimary
            }
        ) {
            return true
        }
        if (cPrimary.length >= 2 &&
            target.split('،', ',').any { part ->
                val p = normalize(part)
                p.isNotBlank() && p == cPrimary
            }
        ) {
            return true
        }
        return false
    }

    fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2.0) * sin(dLat / 2.0) +
            cos(rLat1) * cos(rLat2) * sin(dLon / 2.0) * sin(dLon / 2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
        return EARTH_RADIUS_METERS * c
    }

    fun formatDistanceFa(distanceMeters: Double?): String? {
        if (distanceMeters == null || distanceMeters.isNaN() || distanceMeters.isInfinite() || distanceMeters < 0.0) {
            return null
        }
        return when {
            distanceMeters < 50.0 -> "کمتر از ۵۰ متر"
            distanceMeters < 100.0 -> {
                val rounded = ((distanceMeters / 10.0).roundToInt() * 10).coerceAtLeast(50)
                "${rounded.toString().toPersianDigits()} متر"
            }
            distanceMeters < 1000.0 -> {
                val rounded = ((distanceMeters / 50.0).roundToInt() * 50).coerceAtLeast(100)
                if (rounded >= 1000) {
                    "۱ کیلومتر"
                } else {
                    "${rounded.toString().toPersianDigits()} متر"
                }
            }
            distanceMeters < 10_000.0 -> {
                val km = distanceMeters / 1000.0
                val formatted = String.format(Locale.US, "%.1f", km).removeSuffix(".0")
                "${formatted.toPersianDigits()} کیلومتر"
            }
            else -> {
                val km = (distanceMeters / 1000.0).roundToInt()
                "${km.toString().toPersianDigits()} کیلومتر"
            }
        }
    }
}
