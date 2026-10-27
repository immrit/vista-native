package ir.coffevista.vista_native.features.feed.ui

import ir.coffevista.vista_native.core.common.persianRelativeTime
import java.time.Instant
import java.time.ZoneId

private val TehranZone: ZoneId = ZoneId.of("Asia/Tehran")

/** Mirrors Flutter [TimeUtils.timeAgo] for the Persian-first Feed UI. */
internal fun feedRelativeTime(
    raw: String,
    now: Instant = Instant.now(),
    zone: ZoneId = TehranZone,
): String = persianRelativeTime(raw, now, zone)

