package ir.coffevista.vista_native.features.feed.ui

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

private val TehranZone: ZoneId = ZoneId.of("Asia/Tehran")
private const val PersianDigits = "۰۱۲۳۴۵۶۷۸۹"

/** Mirrors Flutter [TimeUtils.timeAgo] for the Persian-first Feed UI. */
internal fun feedRelativeTime(
    raw: String,
    now: Instant = Instant.now(),
    zone: ZoneId = TehranZone,
): String = runCatching {
    val published = Instant.parse(raw)
    val elapsed = Duration.between(published, now).coerceAtLeast(Duration.ZERO)
    when {
        elapsed.toMinutes() < 1 -> "هم اکنون"
        elapsed.toMinutes() < 60 -> "${elapsed.toMinutes().toPersianDigits()} دقیقه پیش"
        elapsed.toHours() < 24 -> "${elapsed.toHours().toPersianDigits()} ساعت پیش"
        elapsed.toDays() < 7 -> "${elapsed.toDays().toPersianDigits()} روز پیش"
        else -> published.atZone(zone).toJalali().display()
    }
}.getOrDefault(raw.take(10))

private fun Long.toPersianDigits(): String = toString().toPersianDigits()

private fun String.toPersianDigits(): String = buildString(length) {
    this@toPersianDigits.forEach { character ->
        append(if (character in '0'..'9') PersianDigits[character - '0'] else character)
    }
}

private data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    fun display(): String = "%04d/%02d/%02d".format(year, month, day).toPersianDigits()
}

private fun java.time.ZonedDateTime.toJalali(): JalaliDate {
    val gy = year
    val gm = monthValue
    val gd = dayOfMonth
    val monthDays = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val adjustedYear = if (gm > 2) gy + 1 else gy
    var days = 355666 + 365 * gy + (adjustedYear + 3) / 4 -
        (adjustedYear + 99) / 100 + (adjustedYear + 399) / 400 + gd + monthDays[gm - 1]
    var jy = -1595 + 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + days / 31
        jd = 1 + days % 31
    } else {
        jm = 7 + (days - 186) / 30
        jd = 1 + (days - 186) % 30
    }
    return JalaliDate(jy, jm, jd)
}
