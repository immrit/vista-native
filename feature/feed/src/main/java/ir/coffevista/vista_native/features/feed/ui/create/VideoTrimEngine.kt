package ir.coffevista.vista_native.features.feed.ui.create

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.TrackOutput
import androidx.media3.extractor.mp4.Mp4Extractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile
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
            val timeUs = ((durationMs * 1000L) * (index * 2L + 1L) / (count * 2L)).coerceAtLeast(0L)

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
 * Preserves true video presentation timestamps (including non-monotonic H.264/HEVC B-frames)
 * shifted relative to [baseUs]. Open-GOP samples before [baseUs] return `null` and are skipped.
 */
internal fun normalizeVideoSamplePtsUs(sampleTimeUs: Long, baseUs: Long): Long? {
    if (sampleTimeUs < baseUs) return null
    return sampleTimeUs - baseUs
}

/**
 * Ensures strictly monotonic audio timestamps relative to [baseUs] for [MediaMuxer].
 */
internal fun normalizeAudioSamplePtsUs(
    sampleTimeUs: Long,
    baseUs: Long,
    lastAudioPtsUs: Long?,
): Long {
    val raw = (sampleTimeUs - baseUs).coerceAtLeast(0L)
    return if (lastAudioPtsUs != null && raw <= lastAudioPtsUs) {
        lastAudioPtsUs + 1_000L
    } else {
        raw
    }
}

/**
 * Decides whether `[startMs, endMs]` is safe to trim via zero-reencode stream copy (`MediaMuxer`)
 * rather than GPU surface-to-surface H.264 transcoding.
 */
internal fun shouldUseLosslessFastPath(
    videoMime: String,
    audioMime: String?,
    isHdr: Boolean,
    hasNonMonotonicVideoPts: Boolean,
    startsWithSyncFrame: Boolean,
    videoSampleCount: Int,
    seekDriftUs: Long,
): Boolean =
    videoMime.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) &&
        (audioMime == null || audioMime.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true)) &&
        !isHdr &&
        !hasNonMonotonicVideoPts &&
        startsWithSyncFrame &&
        videoSampleCount >= 2 &&
        seekDriftUs <= MAX_LOSSLESS_SEEK_DRIFT_US

private data class TrimPreScan(
    val videoMime: String,
    val audioMime: String?,
    val rotation: Int,
    val isHdr: Boolean,
    val hasNonMonotonicVideoPts: Boolean,
    val startsWithSyncFrame: Boolean,
    val videoSampleCount: Int,
    val seekDriftUs: Long,
) {
    val canUseLossless: Boolean
        get() = shouldUseLosslessFastPath(
            videoMime = videoMime,
            audioMime = audioMime,
            isHdr = isHdr,
            hasNonMonotonicVideoPts = hasNonMonotonicVideoPts,
            startsWithSyncFrame = startsWithSyncFrame,
            videoSampleCount = videoSampleCount,
            seekDriftUs = seekDriftUs,
        )
}

/**
 * Hybrid trim of `[startMs, endMs]` into a faststart, universally playable MP4:
 *
 * - Pre-scans track codecs and timestamps in `[startMs, endMs]`.
 * - Uses lossless keyframe-aligned copy (`MediaExtractor -> MediaMuxer -> Mp4FastStart`) when the
 *   slice is standard SDR H.264/AAC with strictly monotonic timestamps and small keyframe drift.
 * - Automatically uses GPU hardware transcoding (`VideoTranscodeEngine`, 0 B-frames + `Mp4FastStart`)
 *   when the source contains B-frames, Open-GOP pre-sync frames, HEVC/VP9/AV1, HDR, non-AAC audio,
 *   large keyframe drift, or if lossless output fails self-verification.
 */
internal suspend fun trimVideo(
    context: Context,
    uri: Uri,
    startMs: Long,
    endMs: Long,
    maxDurationMs: Long,
): File = withContext(Dispatchers.IO) {
    if (endMs <= startMs) throw VideoTrimException("لطفاً بازه زمانی معتبری انتخاب کنید")

    val preScan = inspectTrimRange(context, uri, startMs, endMs, maxDurationMs)
    if (preScan.canUseLossless) {
        val losslessFile = try {
            trimVideoLossless(
                context = context,
                uri = uri,
                startMs = startMs,
                endMs = endMs,
                maxDurationMs = maxDurationMs,
                rotation = preScan.rotation,
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        if (losslessFile != null) {
            if (validateTrimmedMp4(losslessFile)) {
                return@withContext losslessFile
            }
            losslessFile.delete()
        }
    }

    currentCoroutineContext().ensureActive()
    val transcoded = transcodeTrimVideo(
        context = context,
        uri = uri,
        startMs = startMs,
        endMs = endMs,
        maxDurationMs = maxDurationMs,
        rotation = preScan.rotation,
    )
    if (!validateTrimmedMp4(transcoded)) {
        transcoded.delete()
        throw VideoTrimException("خطا در ایجاد فایل ویدیو برش‌خورده")
    }
    transcoded
}

private suspend fun inspectTrimRange(
    context: Context,
    uri: Uri,
    startMs: Long,
    endMs: Long,
    maxDurationMs: Long,
): TrimPreScan {
    val extractor = MediaExtractor()
    try {
        try {
            extractor.setDataSource(context, uri, null)
        } catch (e: Exception) {
            throw VideoTrimException("امکان باز کردن فایل ویدیو وجود ندارد", e)
        }

        var videoTrack = -1
        var videoFormat: MediaFormat? = null
        var audioMime: String? = null
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("video/") && videoTrack < 0) {
                videoTrack = index
                videoFormat = format
            } else if (mime.startsWith("audio/") && audioMime == null) {
                audioMime = mime
            }
        }
        val vFormat = videoFormat
        val vMime = vFormat?.getString(MediaFormat.KEY_MIME)
        if (videoTrack < 0 || vFormat == null || vMime.isNullOrBlank()) {
            throw VideoTrimException("ویدیویی در این فایل یافت نشد")
        }

        val rotation = readRotation(context, uri, vFormat)
        val colorTransfer = runCatching {
            if (vFormat.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                vFormat.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
            } else {
                0
            }
        }.getOrDefault(0)
        val isHdr = colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084 ||
            colorTransfer == MediaFormat.COLOR_TRANSFER_HLG
        val declaredMaxBFrames = runCatching {
            if (vFormat.containsKey("max-bframes")) vFormat.getInteger("max-bframes") else 0
        }.getOrDefault(0)

        extractor.selectTrack(videoTrack)
        val requestedStartUs = startMs * 1000L
        extractor.seekTo(requestedStartUs + 999L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val seekBaseUs = extractor.sampleTime.coerceAtLeast(0L)
        val seekDriftUs = (requestedStartUs - seekBaseUs).coerceAtLeast(0L)
        val endUs = minOf(endMs * 1000L, seekBaseUs + maxDurationMs * 1000L)
        val scanLimitUs = endUs + 600_000L

        var hasNonMonotonic = declaredMaxBFrames > 0
        var startsWithSync = false
        var sampleCount = 0
        var scannedCount = 0
        var prevPtsUs = Long.MIN_VALUE

        while (true) {
            val timeUs = extractor.sampleTime
            if (timeUs < 0L || timeUs > scanLimitUs) break
            val isSync = (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0
            if (scannedCount == 0) {
                startsWithSync = isSync
            }
            if (timeUs < seekBaseUs) {
                hasNonMonotonic = true
            }
            if (prevPtsUs != Long.MIN_VALUE && timeUs <= prevPtsUs) {
                hasNonMonotonic = true
            }
            prevPtsUs = timeUs
            if (timeUs <= endUs) {
                sampleCount++
            }
            scannedCount++
            if (!extractor.advance()) break
            if (scannedCount % CANCEL_CHECK_INTERVAL == 0) {
                currentCoroutineContext().ensureActive()
            }
        }

        return TrimPreScan(
            videoMime = vMime,
            audioMime = audioMime,
            rotation = rotation,
            isHdr = isHdr,
            hasNonMonotonicVideoPts = hasNonMonotonic,
            startsWithSyncFrame = startsWithSync,
            videoSampleCount = sampleCount,
            seekDriftUs = seekDriftUs,
        )
    } finally {
        runCatching { extractor.release() }
    }
}

private suspend fun trimVideoLossless(
    context: Context,
    uri: Uri,
    startMs: Long,
    endMs: Long,
    maxDurationMs: Long,
    rotation: Int,
): File {
    val muxedTemp = File.createTempFile("trimmed-raw-", ".mp4", context.cacheDir)
    val output = File.createTempFile("trimmed-", ".mp4", context.cacheDir)
    val videoExtractor = MediaExtractor()
    var audioExtractor: MediaExtractor? = null
    var muxer: MediaMuxer? = null
    var muxerStarted = false
    var success = false
    return try {
        try {
            videoExtractor.setDataSource(context, uri, null)
        } catch (e: Exception) {
            throw VideoTrimException("امکان باز کردن فایل ویدیو وجود ندارد", e)
        }

        var videoTrack = -1
        var audioTrack = -1
        var videoFormat: MediaFormat? = null
        var audioFormat: MediaFormat? = null
        var bufferSize = DEFAULT_BUFFER_BYTES
        for (index in 0 until videoExtractor.trackCount) {
            val format = videoExtractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            when {
                mime.startsWith("video/") && videoTrack < 0 -> {
                    videoTrack = index
                    videoFormat = format
                }
                mime.startsWith("audio/") && audioTrack < 0 -> {
                    audioTrack = index
                    audioFormat = format
                }
                else -> continue
            }
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                val declaredSize = runCatching { format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) }.getOrDefault(0)
                if (declaredSize > 0) bufferSize = maxOf(bufferSize, declaredSize)
            }
        }
        val resolvedVideoFormat = videoFormat
        if (videoTrack < 0 || resolvedVideoFormat == null) {
            throw VideoTrimException("ویدیویی در این فایل یافت نشد")
        }

        bufferSize = bufferSize.coerceIn(DEFAULT_BUFFER_BYTES, MAX_BUFFER_BYTES)
        val newMuxer = MediaMuxer(muxedTemp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer = newMuxer
        newMuxer.setOrientationHint(rotation)

        val muxerVideoTrack: Int
        val muxerAudioTrack: Int
        try {
            muxerVideoTrack = newMuxer.addTrack(resolvedVideoFormat)
            muxerAudioTrack = if (audioFormat != null) newMuxer.addTrack(audioFormat) else -1
        } catch (e: Exception) {
            throw VideoTrimException("فرمت این ویدیو برای برش پشتیبانی نمی‌شود", e)
        }

        videoExtractor.selectTrack(videoTrack)
        videoExtractor.seekTo(startMs * 1000L + 999L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val baseUs = videoExtractor.sampleTime.coerceAtLeast(0L)
        val endUs = minOf(endMs * 1000L, baseUs + maxDurationMs * 1000L)

        if (audioTrack >= 0 && muxerAudioTrack >= 0) {
            audioExtractor = MediaExtractor().apply {
                try {
                    setDataSource(context, uri, null)
                } catch (e: Exception) {
                    throw VideoTrimException("امکان باز کردن فایل ویدیو وجود ندارد", e)
                }
                selectTrack(audioTrack)
                seekTo(baseUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }
        }

        newMuxer.start()
        muxerStarted = true

        var videoDone = false
        var audioDone = audioExtractor == null
        var videoSamples = 0
        var written = 0
        var lastVideoPtsUs: Long? = null
        var lastAudioPtsUs: Long? = null
        var buffer = ByteBuffer.allocate(bufferSize)
        val info = MediaCodec.BufferInfo()

        while (!videoDone || !audioDone) {
            val vTime = if (!videoDone) videoExtractor.sampleTime else Long.MAX_VALUE
            val aTime = if (!audioDone) audioExtractor!!.sampleTime else Long.MAX_VALUE

            if (vTime < 0L || vTime > endUs) {
                videoDone = true
            }
            if (aTime < 0L || aTime > endUs) {
                audioDone = true
            }
            if (videoDone && audioDone) break

            val resolvedVTime = if (!videoDone) vTime else Long.MAX_VALUE
            val resolvedATime = if (!audioDone) aTime else Long.MAX_VALUE
            if (resolvedVTime <= resolvedATime) {
                val rawPts = normalizeVideoSamplePtsUs(resolvedVTime, baseUs)
                if (rawPts == null) {
                    if (!videoExtractor.advance()) videoDone = true
                    continue
                }
                val ptsUs = if (lastVideoPtsUs != null && rawPts <= lastVideoPtsUs) {
                    lastVideoPtsUs + 1_000L
                } else {
                    rawPts
                }
                lastVideoPtsUs = ptsUs
                buffer = ensureBufferCapacity(videoExtractor, buffer)
                buffer.clear()
                val size = videoExtractor.readSampleData(buffer, 0)
                if (size < 0) {
                    videoDone = true
                    continue
                }
                info.set(0, size, ptsUs, toMuxerFlags(videoExtractor.sampleFlags))
                newMuxer.writeSampleData(muxerVideoTrack, buffer, info)
                videoSamples++
                if (!videoExtractor.advance()) videoDone = true
            } else {
                val aExt = audioExtractor!!
                if (resolvedATime < baseUs) {
                    if (!aExt.advance()) audioDone = true
                    continue
                }
                buffer = ensureBufferCapacity(aExt, buffer)
                buffer.clear()
                val size = aExt.readSampleData(buffer, 0)
                if (size < 0) {
                    audioDone = true
                    continue
                }
                val ptsUs = normalizeAudioSamplePtsUs(resolvedATime, baseUs, lastAudioPtsUs)
                lastAudioPtsUs = ptsUs
                info.set(0, size, ptsUs, toMuxerFlags(aExt.sampleFlags))
                newMuxer.writeSampleData(muxerAudioTrack, buffer, info)
                if (!aExt.advance()) audioDone = true
            }

            if (++written % CANCEL_CHECK_INTERVAL == 0) {
                currentCoroutineContext().ensureActive()
            }
        }
        if (videoSamples == 0) throw VideoTrimException("بازه انتخاب‌شده خالی است")

        muxerStarted = false
        try {
            newMuxer.stop()
        } catch (e: Exception) {
            throw VideoTrimException("خطا در ایجاد فایل ویدیو برش‌خورده", e)
        }
        if (!muxedTemp.exists() || muxedTemp.length() == 0L) {
            throw VideoTrimException("خطا در ایجاد فایل ویدیو برش‌خورده")
        }

        if (!Mp4FastStart.relocateMoovToFront(muxedTemp, output)) {
            muxedTemp.copyTo(output, overwrite = true)
        }
        if (!output.exists() || output.length() == 0L) {
            throw VideoTrimException("خطا در ایجاد فایل ویدیو برش‌خورده")
        }
        success = true
        output
    } finally {
        if (muxerStarted) runCatching { muxer?.stop() }
        runCatching { muxer?.release() }
        runCatching { videoExtractor.release() }
        runCatching { audioExtractor?.release() }
        runCatching { muxedTemp.delete() }
        if (!success) runCatching { output.delete() }
    }
}

internal fun validateTrimmedMp4(file: File): Boolean {
    if (!file.exists() || file.length() <= 0L) return false
    val retriever = MediaMetadataRetriever()
    val hasValidMetadataAndFrame = try {
        retriever.setDataSource(file.absolutePath)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        if (durationMs <= 0L) {
            false
        } else {
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            if (frame != null) {
                frame.recycle()
                true
            } else {
                false
            }
        }
    } catch (_: Throwable) {
        false
    } finally {
        runCatching { retriever.release() }
    }
    if (!hasValidMetadataAndFrame) return false

    val extractor = MediaExtractor()
    val extractorOk = try {
        extractor.setDataSource(file.absolutePath)
        val videoTrack = (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        } ?: return false
        extractor.selectTrack(videoTrack)

        var samples = 0
        var prevPts = Long.MIN_VALUE
        while (true) {
            val pts = extractor.sampleTime
            if (pts < 0L) break
            if (samples == 0 && (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) == 0) {
                return false
            }
            if (prevPts != Long.MIN_VALUE && pts <= prevPts) {
                return false
            }
            prevPts = pts
            samples++
            if (!extractor.advance()) break
        }
        if (samples < 1) return false

        val audioTrack = (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        if (audioTrack != null) {
            extractor.unselectTrack(videoTrack)
            extractor.selectTrack(audioTrack)
            if (extractor.sampleTime < 0L) return false
        }
        true
    } catch (_: Throwable) {
        false
    } finally {
        runCatching { extractor.release() }
    }
    if (!extractorOk) return false

    return verifyMp4WithMedia3Extractor(file)
}

/**
 * Parses [file] through Media3's own [Mp4Extractor] end-to-end (sniff + moov/stbl parsing +
 * reading every sample) so any container or NAL-length error ExoPlayer would hit is caught before
 * accepting a trimmed or faststart-relocated file.
 */
@OptIn(UnstableApi::class)
internal fun verifyMp4WithMedia3Extractor(file: File): Boolean {
    if (!file.exists() || file.length() < 16L) return false
    val mp4Extractor = Mp4Extractor()
    return try {
        RandomAccessFile(file, "r").use { raf ->
            val fileLength = raf.length()
            val reader = DataReader { target, offset, length ->
                raf.read(target, offset, length)
            }
            val drainBuffer = ByteArray(16 * 1024)
            var videoTrackId = -1
            var audioTrackId = -1
            var videoFormatSeen = false
            var hasAudioTrack = false
            var audioFormatSeen = false
            var videoSamples = 0
            var audioSamples = 0
            var tracksEnded = false
            var seekMap: SeekMap? = null

            val output = object : ExtractorOutput {
                override fun track(id: Int, type: Int): TrackOutput {
                    if (type == C.TRACK_TYPE_VIDEO && videoTrackId < 0) {
                        videoTrackId = id
                    } else if (type == C.TRACK_TYPE_AUDIO && audioTrackId < 0) {
                        audioTrackId = id
                        hasAudioTrack = true
                    }
                    return object : TrackOutput {
                        override fun format(format: Format) {
                            if (id == videoTrackId && format.sampleMimeType?.startsWith("video/") == true) {
                                videoFormatSeen = true
                            } else if (id == audioTrackId && format.sampleMimeType?.startsWith("audio/") == true) {
                                audioFormatSeen = true
                            }
                        }

                        override fun sampleData(
                            input: DataReader,
                            length: Int,
                            allowEndOfInput: Boolean,
                            sampleDataPart: Int,
                        ): Int {
                            var remaining = length
                            var totalRead = 0
                            while (remaining > 0) {
                                val chunk = minOf(remaining, drainBuffer.size)
                                val read = input.read(drainBuffer, 0, chunk)
                                if (read == C.RESULT_END_OF_INPUT || read < 0) {
                                    if (totalRead == 0) {
                                        if (allowEndOfInput) return C.RESULT_END_OF_INPUT
                                        throw EOFException("Unexpected EOF inside Media3 sampleData")
                                    }
                                    return totalRead
                                }
                                totalRead += read
                                remaining -= read
                            }
                            return totalRead
                        }

                        override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) {
                            data.skipBytes(length)
                        }

                        override fun sampleMetadata(
                            timeUs: Long,
                            flags: Int,
                            size: Int,
                            offset: Int,
                            cryptoData: TrackOutput.CryptoData?,
                        ) {
                            if (id == videoTrackId) {
                                videoSamples++
                            } else if (id == audioTrackId) {
                                audioSamples++
                            }
                        }
                    }
                }

                override fun endTracks() {
                    tracksEnded = true
                }

                override fun seekMap(seekMapArg: SeekMap) {
                    seekMap = seekMapArg
                }
            }

            raf.seek(0L)
            var extractorInput = DefaultExtractorInput(reader, 0L, fileLength)
            if (!mp4Extractor.sniff(extractorInput)) {
                return false
            }

            raf.seek(0L)
            extractorInput = DefaultExtractorInput(reader, 0L, fileLength)
            mp4Extractor.init(output)

            val positionHolder = PositionHolder()
            var iterations = 0
            while (iterations++ < 500_000) {
                when (mp4Extractor.read(extractorInput, positionHolder)) {
                    Extractor.RESULT_CONTINUE -> Unit
                    Extractor.RESULT_SEEK -> {
                        val nextPos = positionHolder.position
                        if (nextPos < 0L || nextPos > fileLength) return false
                        raf.seek(nextPos)
                        extractorInput = DefaultExtractorInput(reader, nextPos, fileLength)
                    }
                    Extractor.RESULT_END_OF_INPUT -> break
                }
            }

            val resolvedSeekMap = seekMap
            tracksEnded &&
                resolvedSeekMap != null &&
                resolvedSeekMap.isSeekable &&
                resolvedSeekMap.durationUs > 0L &&
                videoFormatSeen &&
                videoSamples >= 1 &&
                (!hasAudioTrack || (audioFormatSeen && audioSamples >= 1))
        }
    } catch (_: Throwable) {
        false
    } finally {
        runCatching { mp4Extractor.release() }
    }
}

private fun ensureBufferCapacity(extractor: MediaExtractor, current: ByteBuffer): ByteBuffer {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        val sampleSize = extractor.sampleSize
        if (sampleSize > current.capacity() && sampleSize <= MAX_BUFFER_BYTES) {
            return ByteBuffer.allocate(sampleSize.toInt())
        }
    }
    return current
}

private fun toMuxerFlags(sampleFlags: Int): Int =
    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
        MediaCodec.BUFFER_FLAG_KEY_FRAME
    } else {
        0
    }

private fun readRotation(context: Context, uri: Uri, videoFormat: MediaFormat?): Int {
    val retriever = MediaMetadataRetriever()
    val retrieverDegrees = try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
    if (retrieverDegrees != null && retrieverDegrees in VALID_ROTATIONS) {
        return retrieverDegrees
    }
    if (videoFormat != null && videoFormat.containsKey(MediaFormat.KEY_ROTATION)) {
        val formatDegrees = runCatching { videoFormat.getInteger(MediaFormat.KEY_ROTATION) }.getOrNull()
        if (formatDegrees != null && formatDegrees in VALID_ROTATIONS) {
            return formatDegrees
        }
    }
    return 0
}

private val VALID_ROTATIONS = listOf(0, 90, 180, 270)
private const val DEFAULT_BUFFER_BYTES = 2 * 1024 * 1024
private const val MAX_BUFFER_BYTES = 16 * 1024 * 1024
private const val CANCEL_CHECK_INTERVAL = 100
private const val MAX_LOSSLESS_SEEK_DRIFT_US = 2_500_000L
