package ir.coffevista.vista_native.features.feed.ui.create

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/** Raised with a message that is safe to show to the user. */
internal class VideoTrimException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** What the trimmer needs to know about a video before showing its timeline. */
internal class VideoProbe(
    val durationMs: Long,
    /** Presentation time (ms) of every sync sample of the first video track, ascending. */
    val keyframesMs: LongArray,
)

/**
 * Reads duration and keyframe positions without decoding anything (container metadata only).
 * Returns `null` when the file cannot be opened or has no usable video track.
 */
internal suspend fun probeVideo(context: Context, uri: Uri): VideoProbe? = withContext(Dispatchers.IO) {
    var durationMs = 0L
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: Throwable) {
        return@withContext null
    } finally {
        runCatching { retriever.release() }
    }
    if (durationMs <= 0L) return@withContext null

    val keyframes = ArrayList<Long>()
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(context, uri, null)
        val videoTrack = (0 until extractor.trackCount).firstOrNull { index ->
            runCatching {
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            }.getOrDefault(false)
        } ?: return@withContext null
        extractor.selectTrack(videoTrack)
        var sampleCount = 0
        while (true) {
            val time = extractor.sampleTime
            if (time < 0L) break
            if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) keyframes += time / 1000L
            if (!extractor.advance()) break
            if (++sampleCount % 200 == 0) currentCoroutineContext().ensureActive()
        }
    } catch (_: Throwable) {
        // A missing keyframe table only disables snapping; the trim itself still works.
    } finally {
        runCatching { extractor.release() }
    }
    VideoProbe(durationMs, keyframes.distinct().sorted().toLongArray())
}

/**
 * Loads [count] evenly spaced frames, scaled to [targetHeightPx] tall, reporting each as soon as
 * it is ready so the timeline fills in progressively.
 */
internal suspend fun loadVideoThumbnails(
    context: Context,
    uri: Uri,
    durationMs: Long,
    count: Int,
    targetHeightPx: Int,
    onFrame: (index: Int, bitmap: Bitmap) -> Unit,
) = withContext(Dispatchers.IO) {
    if (durationMs <= 0L || count <= 0 || targetHeightPx <= 0) return@withContext
    val safeHeight = targetHeightPx.coerceAtLeast(1)
    val safeWidth = (safeHeight * 16 / 9).coerceAtLeast(1)
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        for (index in 0 until count) {
            currentCoroutineContext().ensureActive()
            val timeUs = (durationMs * 1000L) * index / count + 1000L

            val frame: Bitmap? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                try {
                    retriever.getScaledFrameAtTime(
                        timeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        safeWidth,
                        safeHeight,
                    )
                } catch (_: Throwable) {
                    null
                }
            } else null

            val resolved = frame ?: try {
                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (_: Throwable) {
                null
            } ?: continue

            val scaled = try {
                val scale = safeHeight.toFloat() / resolved.height.coerceAtLeast(1)
                if (scale < 0.95f || scale > 1.05f) {
                    val targetW = (resolved.width * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(resolved, targetW, safeHeight, true).also {
                        if (it !== resolved) resolved.recycle()
                    }
                } else {
                    resolved
                }
            } catch (_: Throwable) {
                resolved
            }
            onFrame(index, scaled)
        }
    } catch (_: Throwable) {
        // Thumbnails are decoration only.
    } finally {
        runCatching { retriever.release() }
    }
}

/**
 * Lossless trim of `[startMs, endMs]` into a new MP4 (MediaExtractor → MediaMuxer, no re-encode).
 *
 * - Video and audio are written in one interleaved pass.
 * - The clip begins on the keyframe at or before [startMs] (the trimmer snaps the window to a
 *   keyframe, so the result matches what the user saw) and never exceeds [maxDurationMs].
 * - The source rotation is preserved so portrait videos are not exported sideways.
 * - Cancelling the calling coroutine stops the work and deletes the partial file.
 */
internal suspend fun trimVideo(
    context: Context,
    uri: Uri,
    startMs: Long,
    endMs: Long,
    maxDurationMs: Long,
): File = withContext(Dispatchers.IO) {
    if (endMs <= startMs) throw VideoTrimException("لطفاً بازه زمانی معتبری انتخاب کنید")

    val output = File.createTempFile("trimmed-", ".mp4", context.cacheDir)
    val extractor = MediaExtractor()
    var muxer: MediaMuxer? = null
    var muxerStarted = false
    var success = false
    try {
        try {
            extractor.setDataSource(context, uri, null)
        } catch (e: Exception) {
            throw VideoTrimException("امکان باز کردن فایل ویدیو وجود ندارد", e)
        }

        var videoTrack = -1
        var audioTrack = -1
        var bufferSize = DEFAULT_BUFFER_BYTES
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            when {
                mime.startsWith("video/") && videoTrack < 0 -> videoTrack = index
                mime.startsWith("audio/") && audioTrack < 0 -> audioTrack = index
                else -> continue
            }
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                val declaredSize = runCatching { format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) }.getOrDefault(0)
                if (declaredSize > 0) bufferSize = maxOf(bufferSize, declaredSize)
            }
        }
        if (videoTrack < 0) throw VideoTrimException("ویدیویی در این فایل یافت نشد")

        bufferSize = bufferSize.coerceIn(DEFAULT_BUFFER_BYTES, 16 * 1024 * 1024)
        val rotation = readRotation(context, uri)
        val newMuxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer = newMuxer
        newMuxer.setOrientationHint(rotation)

        val trackMap = HashMap<Int, Int>(2)
        try {
            trackMap[videoTrack] = newMuxer.addTrack(extractor.getTrackFormat(videoTrack))
            if (audioTrack >= 0) trackMap[audioTrack] = newMuxer.addTrack(extractor.getTrackFormat(audioTrack))
        } catch (e: Exception) {
            throw VideoTrimException("فرمت این ویدیو برای برش پشتیبانی نمی‌شود", e)
        }

        // 1) Find the keyframe the clip will start on, looking at the video track alone.
        extractor.selectTrack(videoTrack)
        extractor.seekTo(startMs * 1000L + 999L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val baseUs = extractor.sampleTime.coerceAtLeast(0L)
        val endUs = minOf(endMs * 1000L, baseUs + maxDurationMs * 1000L)

        // 2) Bring audio in and cut both tracks from that point.
        if (audioTrack >= 0) extractor.selectTrack(audioTrack)
        extractor.seekTo(baseUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        newMuxer.start()
        muxerStarted = true

        var remainingTracks = trackMap.size
        var videoSamples = 0
        var written = 0
        val lastPtsUs = HashMap<Int, Long>(2)
        val buffer = ByteBuffer.allocate(bufferSize)
        val info = MediaCodec.BufferInfo()
        while (remainingTracks > 0) {
            val track = extractor.sampleTrackIndex
            if (track < 0) break
            val muxerTrack = trackMap[track]
            if (muxerTrack == null) {
                extractor.advance()
                continue
            }
            val timeUs = extractor.sampleTime
            if (timeUs > endUs) {
                extractor.unselectTrack(track)
                remainingTracks--
                extractor.advance()
                continue
            }
            if (track == audioTrack && timeUs < baseUs) {
                extractor.advance()
                continue
            }
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break

            var ptsUs = (timeUs - baseUs).coerceAtLeast(0L)
            val previousPts = lastPtsUs[muxerTrack]
            if (previousPts != null && ptsUs <= previousPts) {
                // Ensure strictly monotonic timestamps for MediaMuxer to avoid IllegalArgumentException.
                ptsUs = previousPts + 1_000L
            }
            lastPtsUs[muxerTrack] = ptsUs

            info.set(
                0,
                size,
                ptsUs,
                if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0,
            )
            newMuxer.writeSampleData(muxerTrack, buffer, info)
            if (track == videoTrack) videoSamples++
            extractor.advance()
            if (++written % CANCEL_CHECK_INTERVAL == 0) currentCoroutineContext().ensureActive()
        }
        if (videoSamples == 0) throw VideoTrimException("بازه انتخاب‌شده خالی است")

        muxerStarted = false
        try {
            newMuxer.stop()
        } catch (e: Exception) {
            throw VideoTrimException("خطا در ایجاد فایل ویدیو برش‌خورده", e)
        }
        if (!output.exists() || output.length() == 0L) {
            throw VideoTrimException("خطا در ایجاد فایل ویدیو برش‌خورده")
        }
        success = true
        output
    } finally {
        if (muxerStarted) runCatching { muxer?.stop() }
        runCatching { muxer?.release() }
        runCatching { extractor.release() }
        if (!success) output.delete()
    }
}

private fun readRotation(context: Context, uri: Uri): Int {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val degrees = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (degrees in listOf(0, 90, 180, 270)) degrees else 0
    } catch (_: Exception) {
        0
    } finally {
        runCatching { retriever.release() }
    }
}

private const val DEFAULT_BUFFER_BYTES = 2 * 1024 * 1024
private const val CANCEL_CHECK_INTERVAL = 100
