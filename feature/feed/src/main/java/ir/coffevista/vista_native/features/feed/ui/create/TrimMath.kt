package ir.coffevista.vista_native.features.feed.ui.create

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure helpers shared by the music and video trimmers.
 *
 * The trimmers use a fixed-length "window" that sits over a timeline that scrolls underneath it,
 * so the whole selection logic reduces to a single number: the window start. Everything here is
 * free of Android/Compose types so it can be unit-tested on the JVM.
 */
internal object TrimMath {

    /** Smallest clip the user may pick (unless the source itself is shorter). */
    const val MIN_CLIP_MS: Long = 3_000L

    /** Smallest video trim range when dragging handles interactively. */
    const val MIN_VIDEO_TRIM_MS: Long = 1_000L

    /**
     * Clamps `[startMs, endMs]` inside `[0, durationMs]` while enforcing `[minLenMs, maxLenMs]`.
     * When [anchorStart] is `true`, `startMs` is kept fixed (as much as bounds allow) and `endMs`
     * adjusts; otherwise `endMs` stays fixed and `startMs` adjusts.
     */
    fun clampRange(
        startMs: Long,
        endMs: Long,
        durationMs: Long,
        minLenMs: Long = MIN_VIDEO_TRIM_MS,
        maxLenMs: Long = durationMs,
        anchorStart: Boolean = false,
    ): Pair<Long, Long> {
        if (durationMs <= 0L) return 0L to 0L
        val maxLen = maxLenMs.coerceIn(1L, durationMs)
        val minLen = minLenMs.coerceIn(1L, maxLen)
        var s = startMs.coerceIn(0L, durationMs)
        var e = endMs.coerceIn(0L, durationMs)
        if (e < s) {
            if (anchorStart) {
                e = (s + minLen).coerceAtMost(durationMs)
                if (e - s < minLen) s = (e - minLen).coerceAtLeast(0L)
            } else {
                s = (e - minLen).coerceAtLeast(0L)
                if (e - s < minLen) e = (s + minLen).coerceAtMost(durationMs)
            }
        }
        val len = e - s
        if (len < minLen) {
            if (anchorStart) {
                e = (s + minLen).coerceAtMost(durationMs)
                if (e - s < minLen) s = (e - minLen).coerceAtLeast(0L)
            } else {
                s = (e - minLen).coerceAtLeast(0L)
                if (e - s < minLen) e = (s + minLen).coerceAtMost(durationMs)
            }
        } else if (len > maxLen) {
            if (anchorStart) {
                e = (s + maxLen).coerceAtMost(durationMs)
            } else {
                s = (e - maxLen).coerceAtLeast(0L)
            }
        }
        return s.coerceIn(0L, durationMs) to e.coerceIn(0L, durationMs)
    }

    /**
     * Translates the selected window `[startMs, endMs]` by [deltaMs] without changing its length,
     * clamping cleanly inside `[0, durationMs]`.
     */
    fun slideRange(
        startMs: Long,
        endMs: Long,
        deltaMs: Long,
        durationMs: Long,
    ): Pair<Long, Long> {
        if (durationMs <= 0L) return 0L to 0L
        val len = (endMs - startMs).coerceIn(0L, durationMs)
        val newStart = (startMs + deltaMs).coerceIn(0L, (durationMs - len).coerceAtLeast(0L))
        return newStart to (newStart + len)
    }

    /**
     * Length of the window for a requested clip length: never longer than the source, never
     * longer than the allowed maximum and never shorter than [MIN_CLIP_MS] (unless the source is).
     */
    fun clampClipMs(durationMs: Long, requestedMs: Long, maxClipMs: Long): Long {
        if (durationMs <= 0L) return 0L
        val upper = minOf(durationMs, maxClipMs.coerceAtLeast(1L))
        val lower = minOf(MIN_CLIP_MS, upper)
        return requestedMs.coerceIn(lower, upper)
    }

    /** Biggest legal start for a window of [clipMs]. */
    fun maxStartMs(durationMs: Long, clipMs: Long): Long = (durationMs - clipMs).coerceAtLeast(0L)

    /** Keeps [startMs] inside the track for a window of [clipMs]. */
    fun clampStartMs(startMs: Long, durationMs: Long, clipMs: Long): Long =
        startMs.coerceIn(0L, maxStartMs(durationMs, clipMs))

    /** Window start that corresponds to a horizontal scroll offset. */
    fun startFromScroll(scrollPx: Int, pxPerMs: Float, durationMs: Long, clipMs: Long): Long {
        if (pxPerMs <= 0f || pxPerMs.isNaN() || pxPerMs.isInfinite()) return 0L
        return clampStartMs((scrollPx / pxPerMs).roundToInt().toLong(), durationMs, clipMs)
    }

    /** Horizontal scroll offset that places the window at [startMs]. */
    fun scrollFromStart(startMs: Long, pxPerMs: Float, durationMs: Long, clipMs: Long): Int {
        if (pxPerMs <= 0f || pxPerMs.isNaN() || pxPerMs.isInfinite()) return 0
        return (clampStartMs(startMs, durationMs, clipMs) * pxPerMs).roundToInt()
    }

    /**
     * Selectable clip lengths: a few evenly spaced fractions of the cap, plus the longest
     * allowed length. Short sources only get the single "whole track" option.
     */
    fun clipLengthOptions(durationMs: Long, maxClipMs: Long): List<Long> {
        if (durationMs <= 0L) return emptyList()
        val longest = clampClipMs(durationMs, maxClipMs, maxClipMs)
        val options = listOf(1, 2, 3).map { step ->
            // Round to whole seconds so the labels stay readable.
            val raw = longest * step / 3
            (raw / 1000L * 1000L).coerceAtLeast(1000L)
        }.map { clampClipMs(durationMs, it, maxClipMs) }
        return (options + longest).distinct().sorted()
    }

    /**
     * Nearest keyframe to [startMs] that still leaves room for a full window
     * (`<= maxStartMs`). Lossless MP4 cutting can only begin on a keyframe, so snapping the
     * window to one makes the exported file match exactly what the user sees.
     */
    fun snapToKeyframe(keyframesMs: LongArray, startMs: Long, maxStartMs: Long): Long {
        var best = -1L
        for (keyframe in keyframesMs) {
            if (keyframe > maxStartMs) continue
            if (best < 0L || kotlin.math.abs(keyframe - startMs) < kotlin.math.abs(best - startMs)) {
                best = keyframe
            }
        }
        return if (best < 0L) startMs.coerceIn(0L, maxStartMs) else best
    }

    /** `mm:ss` (or `h:mm:ss` for an hour and more) with Latin digits. */
    fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
