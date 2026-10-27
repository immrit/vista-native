package ir.coffevista.vista_native.core.common

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val TehranZone: ZoneId = ZoneId.of("Asia/Tehran")
private const val PersianDigits = "۰۱۲۳۴۵۶۷۸۹"

fun String.toPersianDigits(): String = buildString(length) {
    this@toPersianDigits.forEach { character ->
        append(if (character in '0'..'9') PersianDigits[character - '0'] else character)
    }
}

fun Long.toPersianDigits(): String = toString().toPersianDigits()
fun Int.toPersianDigits(): String = toString().toPersianDigits()

data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    fun display(): String = "%04d/%02d/%02d".format(year, month, day).toPersianDigits()
}

fun ZonedDateTime.toJalali(): JalaliDate {
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
    if (days >= 366) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
    val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
    return JalaliDate(jy, jm, jd)
}

private val TIME_FORMATTERS = listOf(
    DateTimeFormatter.ISO_OFFSET_DATE_TIME,
    DateTimeFormatter.ISO_INSTANT,
    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
)

fun parseToInstant(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    runCatching { return Instant.parse(raw) }
    for (formatter in TIME_FORMATTERS) {
        val parsed = runCatching {
            val temporal = formatter.parseBest(raw, ZonedDateTime::from, LocalDateTime::from, Instant::from)
            when (temporal) {
                is Instant -> temporal
                is ZonedDateTime -> temporal.toInstant()
                is LocalDateTime -> temporal.atZone(TehranZone).toInstant()
                else -> null
            }
        }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

fun formatJalaliFull(raw: String?, zone: ZoneId = TehranZone): String? {
    val instant = parseToInstant(raw) ?: return raw?.take(10)?.toPersianDigits()
    return instant.atZone(zone).toJalali().display()
}

fun persianRelativeTime(
    raw: String?,
    now: Instant = Instant.now(),
    zone: ZoneId = TehranZone,
): String {
    if (raw.isNullOrBlank()) return ""
    val published = parseToInstant(raw) ?: return raw.take(10).toPersianDigits()
    val elapsed = Duration.between(published, now).coerceAtLeast(Duration.ZERO)
    return when {
        elapsed.toMinutes() < 1 -> "هم اکنون"
        elapsed.toMinutes() < 60 -> "${elapsed.toMinutes().toPersianDigits()} دقیقه پیش"
        elapsed.toHours() < 24 -> "${elapsed.toHours().toPersianDigits()} ساعت پیش"
        elapsed.toDays() < 7 -> "${elapsed.toDays().toPersianDigits()} روز پیش"
        else -> published.atZone(zone).toJalali().display()
    }
}
