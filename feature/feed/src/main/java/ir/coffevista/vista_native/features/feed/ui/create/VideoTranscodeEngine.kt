package ir.coffevista.vista_native.features.feed.ui.create

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.Surface
import ir.coffevista.vista_native.features.feed.music.AudioTrimEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Hardware GPU surface-to-surface H.264 + AAC trim transcoder (`MediaExtractor` -> `MediaCodec`
 * decoder -> `SurfaceTexture`/GLES20 -> `MediaCodec` AVC encoder with 0 B-frames -> `MediaMuxer`
 * -> `Mp4FastStart`).
 *
 * Used whenever lossless stream-copy cannot guarantee smooth universal playback (B-frames,
 * Open-GOP, HEVC/VP9/AV1, HDR transfer, non-AAC audio, or excessive keyframe drift).
 */
internal suspend fun transcodeTrimVideo(
    context: Context,
    uri: Uri,
    startMs: Long,
    endMs: Long,
    maxDurationMs: Long,
    rotation: Int,
): File = withContext(Dispatchers.IO) {
    if (endMs <= startMs) throw VideoTrimException("لطفاً بازه زمانی معتبری انتخاب کنید")

    val startUs = startMs * 1000L
    val endUs = minOf(endMs * 1000L, startUs + maxDurationMs * 1000L)

    val cacheDir = context.cacheDir
    val videoOnlyFile = File.createTempFile("trim-vid-only-", ".mp4", cacheDir)
    val muxedTempFile = File.createTempFile("trim-muxed-", ".mp4", cacheDir)
    val finalOutput = File.createTempFile("trimmed-", ".mp4", cacheDir)
    var transcodedAudioFile: File? = null
    var finalSuccess = false

    try {
        // Stage A: Decode & re-encode video to strictly monotonic H.264 (0 B-frames) in [startUs, endUs].
        val videoStageResult = transcodeVideoTrackOnly(
            context = context,
            sourceUri = uri,
            startUs = startUs,
            endUs = endUs,
            outputVideoOnlyFile = videoOnlyFile,
        )

        // Stage B: Prepare matching AAC audio if the source contains an audio track.
        var audioUriForMux: Uri? = null
        var audioStartUs = 0L
        var audioEndUs = Long.MAX_VALUE
        if (videoStageResult.hasAudio) {
            val isAlreadyAac = videoStageResult.audioMime.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true)
            if (isAlreadyAac) {
                audioUriForMux = uri
                audioStartUs = videoStageResult.actualStartUs
                audioEndUs = videoStageResult.actualEndUs
            } else {
                val trimAudioStartMs = (videoStageResult.actualStartUs / 1000L).coerceAtLeast(0L)
                val trimAudioEndMs = (videoStageResult.actualEndUs / 1000L).coerceAtLeast(trimAudioStartMs + 100L)
                transcodedAudioFile = runCatching {
                    AudioTrimEngine.trimAudio(
                        context = context,
                        audioUri = uri,
                        startMs = trimAudioStartMs,
                        endMs = trimAudioEndMs,
                    )
                }.getOrNull()
                if (transcodedAudioFile != null) {
                    audioUriForMux = Uri.fromFile(transcodedAudioFile)
                    audioStartUs = 0L
                    audioEndUs = Long.MAX_VALUE
                }
            }
        }

        // Stage C: Clean monotonic mux (videoOnly.mp4 + AAC audio) + faststart moov relocation.
        muxCleanVideoAndAudio(
            context = context,
            videoOnlyFile = videoOnlyFile,
            audioSourceUri = audioUriForMux,
            audioStartUs = audioStartUs,
            audioEndUs = audioEndUs,
            rotation = rotation,
            muxedOutFile = muxedTempFile,
        )

        if (!Mp4FastStart.relocateMoovToFront(muxedTempFile, finalOutput)) {
            muxedTempFile.copyTo(finalOutput, overwrite = true)
        }

        if (!finalOutput.exists() || finalOutput.length() == 0L) {
            throw VideoTrimException("خطا در ایجاد فایل ویدیو برش‌خورده")
        }
        finalSuccess = true
        finalOutput
    } finally {
        runCatching { videoOnlyFile.delete() }
        runCatching { muxedTempFile.delete() }
        runCatching { transcodedAudioFile?.delete() }
        if (!finalSuccess) {
            runCatching { finalOutput.delete() }
        }
    }
}

private data class VideoTranscodeStageResult(
    val hasAudio: Boolean,
    val audioMime: String?,
    val actualStartUs: Long,
    val actualEndUs: Long,
)

private suspend fun transcodeVideoTrackOnly(
    context: Context,
    sourceUri: Uri,
    startUs: Long,
    endUs: Long,
    outputVideoOnlyFile: File,
): VideoTranscodeStageResult {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var encoder: MediaCodec? = null
    var inputSurface: CodecInputSurface? = null
    var outputSurface: CodecOutputSurface? = null
    var muxer: MediaMuxer? = null
    var muxerStarted = false

    try {
        try {
            extractor.setDataSource(context, sourceUri, null)
        } catch (e: Exception) {
            throw VideoTrimException("امکان باز کردن فایل ویدیو وجود ندارد", e)
        }

        var videoTrack = -1
        var videoFormat: MediaFormat? = null
        var audioMime: String? = null
        var hasAudio = false
        for (index in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(index)
            val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("video/") && videoTrack < 0) {
                videoTrack = index
                videoFormat = fmt
            } else if (mime.startsWith("audio/") && !hasAudio) {
                hasAudio = true
                audioMime = mime
            }
        }
        val sourceFormat = videoFormat ?: throw VideoTrimException("ویدیویی در این فایل یافت نشد")
        val sourceMime = sourceFormat.getString(MediaFormat.KEY_MIME)
            ?: throw VideoTrimException("فرمت این ویدیو برای برش پشتیبانی نمی‌شود")

        val rawWidth = runCatching { sourceFormat.getInteger(MediaFormat.KEY_WIDTH) }.getOrDefault(0)
        val rawHeight = runCatching { sourceFormat.getInteger(MediaFormat.KEY_HEIGHT) }.getOrDefault(0)
        if (rawWidth <= 0 || rawHeight <= 0) {
            throw VideoTrimException("ابعاد این ویدیو قابل خواندن نیست")
        }
        val sourceFps = readFrameRate(sourceFormat)

        val encoderInstance = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder = encoderInstance
        val (targetWidth, targetHeight) = resolveSupportedEncoderSize(encoderInstance, rawWidth, rawHeight)
        val targetBitrate = resolveTargetBitrate(targetWidth, targetHeight)

        configureAvcEncoder(
            encoder = encoderInstance,
            width = targetWidth,
            height = targetHeight,
            fps = sourceFps,
            bitrate = targetBitrate,
        )

        val encoderInputSurface = CodecInputSurface(encoderInstance.createInputSurface())
        inputSurface = encoderInputSurface
        encoderInputSurface.makeCurrent()

        val decoderOutputSurface = CodecOutputSurface(targetWidth, targetHeight)
        outputSurface = decoderOutputSurface

        encoderInstance.start()

        val decoderFormat = copyFormatWithZeroRotation(sourceFormat)
        decoder = MediaCodec.createDecoderByType(sourceMime).apply {
            configure(decoderFormat, decoderOutputSurface.surface, null, 0)
            start()
        }

        muxer = MediaMuxer(outputVideoOnlyFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        extractor.selectTrack(videoTrack)
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val feedStopUs = endUs + REORDER_WINDOW_US

        val decInfo = MediaCodec.BufferInfo()
        val encInfo = MediaCodec.BufferInfo()
        val muxInfo = MediaCodec.BufferInfo()
        var muxerVideoTrack = -1
        var extractorEndReached = false
        var decoderEosQueued = false
        var decoderDone = false
        var encoderDone = false

        var firstKeptPtsUs = -1L
        var lastSubmittedPtsUs = -1L
        var lastMuxedPtsUs = -1L
        var keptFrames = 0
        var emptyEncoderDrainCount = 0
        var eosDrainStartMs = 0L
        var loopIterations = 0

        fun drainEncoder(endOfStream: Boolean) {
            val timeoutUs = if (endOfStream) CODEC_TIMEOUT_US else 0L
            while (!encoderDone) {
                val outIdx = encoderInstance.dequeueOutputBuffer(encInfo, timeoutUs)
                when {
                    outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!endOfStream) break
                        val now = SystemClock.elapsedRealtime()
                        if (eosDrainStartMs == 0L) eosDrainStartMs = now
                        if (++emptyEncoderDrainCount > MAX_EMPTY_EOS_DRAIN || now - eosDrainStartMs > EOS_DRAIN_TIMEOUT_MS) {
                            throw VideoTrimException("خطا در کدگذاری نهایی ویدیو")
                        }
                    }
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            muxerVideoTrack = muxer.addTrack(encoderInstance.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    outIdx >= 0 -> {
                        emptyEncoderDrainCount = 0
                        val outBuf = encoderInstance.getOutputBuffer(outIdx)
                        if ((encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            encInfo.size = 0
                        }
                        if (encInfo.size > 0 && muxerStarted && muxerVideoTrack >= 0 && outBuf != null) {
                            val clampedPts = encInfo.presentationTimeUs.coerceAtLeast(0L)
                            val safePts = if (lastMuxedPtsUs >= 0L && clampedPts <= lastMuxedPtsUs) {
                                lastMuxedPtsUs + 1_000L
                            } else {
                                clampedPts
                            }
                            lastMuxedPtsUs = safePts
                            val safeFlags = if ((encInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                                MediaCodec.BUFFER_FLAG_KEY_FRAME
                            } else {
                                0
                            }
                            muxInfo.set(0, encInfo.size, safePts, safeFlags)
                            outBuf.position(encInfo.offset)
                            outBuf.limit(encInfo.offset + encInfo.size)
                            muxer.writeSampleData(muxerVideoTrack, outBuf, muxInfo)
                        }
                        val reachedEos = (encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        encoderInstance.releaseOutputBuffer(outIdx, false)
                        if (reachedEos) {
                            encoderDone = true
                            break
                        }
                    }
                }
            }
        }

        while (!encoderDone) {
            if (++loopIterations % TRANSCODE_CANCEL_CHECK_INTERVAL == 0) {
                currentCoroutineContext().ensureActive()
            }

            // 1. Feed extractor samples -> decoder input (guarantees EOS is always queued)
            if (!decoderEosQueued) {
                val inIdx = decoder.dequeueInputBuffer(CODEC_TIMEOUT_US)
                if (inIdx >= 0) {
                    val inBuf = decoder.getInputBuffer(inIdx)
                    if (inBuf != null) {
                        if (extractorEndReached) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            decoderEosQueued = true
                        } else {
                            inBuf.clear()
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            val sampleTimeUs = extractor.sampleTime
                            if (sampleSize < 0 || sampleTimeUs < 0L || sampleTimeUs > feedStopUs) {
                                decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                extractorEndReached = true
                                decoderEosQueued = true
                            } else {
                                val inFlags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                                } else {
                                    0
                                }
                                decoder.queueInputBuffer(inIdx, 0, sampleSize, sampleTimeUs, inFlags)
                                if (!extractor.advance()) {
                                    extractorEndReached = true
                                }
                            }
                        }
                    }
                }
            }

            // 2. Drain decoder output -> render kept frames via GL -> signal EOS when done
            if (!decoderDone) {
                val decOutIdx = decoder.dequeueOutputBuffer(decInfo, CODEC_TIMEOUT_US)
                when {
                    decOutIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    decOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    decOutIdx >= 0 -> {
                        val isEos = (decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        val ptsUs = decInfo.presentationTimeUs
                        val shouldKeep = !isEos && ptsUs in startUs..endUs
                        if (shouldKeep) {
                            if (firstKeptPtsUs < 0L) firstKeptPtsUs = ptsUs
                            val rawRelative = (ptsUs - firstKeptPtsUs).coerceAtLeast(0L)
                            val normalizedPtsUs = if (lastSubmittedPtsUs < 0L) {
                                rawRelative
                            } else {
                                maxOf(rawRelative, lastSubmittedPtsUs + 1_000L)
                            }
                            lastSubmittedPtsUs = normalizedPtsUs

                            decoderOutputSurface.prepareForRender()
                            decoder.releaseOutputBuffer(decOutIdx, true)
                            decoderOutputSurface.awaitNewImage()
                            decoderOutputSurface.drawImage()
                            encoderInputSurface.setPresentationTime(normalizedPtsUs * 1_000L)
                            encoderInputSurface.swapBuffers()
                            keptFrames++
                        } else {
                            decoder.releaseOutputBuffer(decOutIdx, false)
                        }

                        if (isEos) {
                            if (keptFrames == 0) {
                                throw VideoTrimException("بازه انتخاب‌شده خالی است")
                            }
                            decoderDone = true
                            encoderInstance.signalEndOfInputStream()
                        }
                    }
                }
            }

            // 3. Pump encoder output continuously so its buffer pool never stalls
            drainEncoder(endOfStream = decoderDone)
        }

        if (keptFrames == 0 || !muxerStarted) {
            throw VideoTrimException("خطا در ایجاد فایل ویدیو برش‌خورده")
        }

        muxerStarted = false
        muxer.stop()

        val actualStart = firstKeptPtsUs.coerceAtLeast(startUs)
        val frameStepUs = (1_000_000L / sourceFps.coerceAtLeast(1)).coerceIn(16_000L, 45_000L)
        val actualEnd = minOf(
            endUs + frameStepUs,
            actualStart + lastSubmittedPtsUs.coerceAtLeast(0L) + frameStepUs,
        )
        return VideoTranscodeStageResult(
            hasAudio = hasAudio,
            audioMime = audioMime,
            actualStartUs = actualStart,
            actualEndUs = actualEnd,
        )
    } finally {
        if (muxerStarted) runCatching { muxer?.stop() }
        runCatching { muxer?.release() }
        runCatching {
            decoder?.stop()
            decoder?.release()
        }
        runCatching {
            encoder?.stop()
            encoder?.release()
        }
        runCatching { outputSurface?.release() }
        runCatching { inputSurface?.release() }
        runCatching { extractor.release() }
    }
}

private fun configureAvcEncoder(
    encoder: MediaCodec,
    width: Int,
    height: Int,
    fps: Int,
    bitrate: Int,
) {
    val primaryFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Explicitly force 0 B-frames so output presentation timestamps are strictly monotonic.
            setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0)
        }
    }
    try {
        encoder.configure(primaryFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    } catch (_: Exception) {
        val fallbackFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps.coerceAtMost(30))
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        encoder.reset()
        encoder.configure(fallbackFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }
}

private fun resolveSupportedEncoderSize(
    encoder: MediaCodec,
    rawWidth: Int,
    rawHeight: Int,
): Pair<Int, Int> {
    val caps = runCatching {
        encoder.codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).videoCapabilities
    }.getOrNull()
    val wAlign = (caps?.widthAlignment ?: 16).coerceAtLeast(2).let { maxOf(it, 16) }
    val hAlign = (caps?.heightAlignment ?: 16).coerceAtLeast(2).let { maxOf(it, 16) }

    val longEdge = maxOf(rawWidth, rawHeight).toFloat().coerceAtLeast(1f)
    for (capEdge in intArrayOf(1080, 720, 480, 360)) {
        val scale = minOf(1f, capEdge.toFloat() / longEdge)
        val scaledW = alignDimension((rawWidth * scale).toInt(), wAlign)
        val scaledH = alignDimension((rawHeight * scale).toInt(), hAlign)
        if (caps == null || caps.isSizeSupported(scaledW, scaledH)) {
            return scaledW to scaledH
        }
    }
    return alignDimension(rawWidth.coerceAtMost(480), wAlign) to alignDimension(rawHeight.coerceAtMost(480), hAlign)
}

private fun alignDimension(value: Int, alignment: Int): Int {
    val safeAlign = alignment.coerceAtLeast(2)
    return (((value + safeAlign / 2) / safeAlign) * safeAlign).coerceAtLeast(safeAlign)
}

private fun resolveTargetBitrate(width: Int, height: Int): Int {
    val pixels = width.toLong() * height.toLong()
    return when {
        pixels <= 854L * 480L -> 2_200_000
        pixels <= 1280L * 720L -> 3_800_000
        else -> 5_500_000
    }
}

private fun readFrameRate(format: MediaFormat): Int {
    val raw = runCatching {
        if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
            runCatching { format.getInteger(MediaFormat.KEY_FRAME_RATE) }
                .getOrElse { format.getFloat(MediaFormat.KEY_FRAME_RATE).toInt() }
        } else {
            30
        }
    }.getOrDefault(30)
    return raw.coerceIn(24, 60)
}

private fun copyFormatWithZeroRotation(source: MediaFormat): MediaFormat {
    // Setting rotation to 0 stops Surface decoders from pre-rotating away from coded dimensions;
    // the original orientation hint is preserved on the final MediaMuxer container.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return MediaFormat(source).apply {
            setInteger(MediaFormat.KEY_ROTATION, 0)
        }
    }
    val mime = source.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_VIDEO_AVC
    val width = runCatching { source.getInteger(MediaFormat.KEY_WIDTH) }.getOrDefault(0)
    val height = runCatching { source.getInteger(MediaFormat.KEY_HEIGHT) }.getOrDefault(0)
    return MediaFormat.createVideoFormat(mime, width, height).apply {
        setInteger(MediaFormat.KEY_ROTATION, 0)
        for (csdKey in listOf("csd-0", "csd-1", "csd-2")) {
            runCatching {
                if (source.containsKey(csdKey)) {
                    source.getByteBuffer(csdKey)?.let { buf ->
                        val dup = buf.duplicate()
                        val copy = ByteBuffer.allocateDirect(dup.remaining())
                        copy.put(dup)
                        copy.flip()
                        setByteBuffer(csdKey, copy)
                    }
                }
            }
        }
        if (source.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            runCatching { setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, source.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)) }
        }
    }
}

private suspend fun muxCleanVideoAndAudio(
    context: Context,
    videoOnlyFile: File,
    audioSourceUri: Uri?,
    audioStartUs: Long,
    audioEndUs: Long,
    rotation: Int,
    muxedOutFile: File,
) {
    val videoExt = MediaExtractor()
    var audioExt: MediaExtractor? = null
    var muxer: MediaMuxer? = null
    var muxerStarted = false

    try {
        videoExt.setDataSource(videoOnlyFile.absolutePath)
        val vTrack = (0 until videoExt.trackCount).firstOrNull { idx ->
            videoExt.getTrackFormat(idx).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        } ?: throw VideoTrimException("خطا در خواندن ترک ویدیو")
        val vFormat = videoExt.getTrackFormat(vTrack)
        videoExt.selectTrack(vTrack)

        var aTrack = -1
        var aFormat: MediaFormat? = null
        if (audioSourceUri != null) {
            val candidateExt = MediaExtractor()
            val setupOk = runCatching {
                candidateExt.setDataSource(context, audioSourceUri, null)
                aTrack = (0 until candidateExt.trackCount).firstOrNull { idx ->
                    candidateExt.getTrackFormat(idx).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                } ?: -1
                if (aTrack >= 0) {
                    aFormat = candidateExt.getTrackFormat(aTrack)
                    candidateExt.selectTrack(aTrack)
                    candidateExt.seekTo(audioStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    true
                } else {
                    false
                }
            }.getOrDefault(false)
            if (setupOk) {
                audioExt = candidateExt
            } else {
                runCatching { candidateExt.release() }
            }
        }

        val newMuxer = MediaMuxer(muxedOutFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer = newMuxer
        newMuxer.setOrientationHint(rotation)

        val muxVideoTrack = newMuxer.addTrack(vFormat)
        val muxAudioTrack = if (audioExt != null && aFormat != null) {
            runCatching { newMuxer.addTrack(aFormat!!) }.getOrDefault(-1)
        } else {
            -1
        }
        if (muxAudioTrack < 0) {
            runCatching { audioExt?.release() }
            audioExt = null
        }

        newMuxer.start()
        muxerStarted = true

        var buffer = ByteBuffer.allocate(MUX_BUFFER_BYTES)
        val info = MediaCodec.BufferInfo()
        var videoDone = false
        var audioDone = audioExt == null
        var lastVideoPtsUs = -1L
        var lastAudioPtsUs: Long? = null
        var written = 0

        while (!videoDone || !audioDone) {
            val vRaw = if (!videoDone) videoExt.sampleTime else Long.MAX_VALUE
            val aRaw = if (!audioDone) audioExt!!.sampleTime else Long.MAX_VALUE

            if (vRaw < 0L) videoDone = true
            if (aRaw < 0L || aRaw > audioEndUs) audioDone = true
            if (videoDone && audioDone) break

            val vEffective = if (!videoDone) vRaw else Long.MAX_VALUE
            val aNormalized = if (!audioDone) (aRaw - audioStartUs) else Long.MAX_VALUE

            if (vEffective <= aNormalized) {
                val sampleSize = videoExt.sampleSize.toInt()
                if (sampleSize > buffer.capacity() && sampleSize <= MAX_MUX_BUFFER_BYTES) {
                    buffer = ByteBuffer.allocate(sampleSize)
                }
                buffer.clear()
                val readSize = videoExt.readSampleData(buffer, 0)
                if (readSize < 0) {
                    videoDone = true
                    continue
                }
                val ptsUs = if (lastVideoPtsUs >= 0L && vRaw <= lastVideoPtsUs) {
                    lastVideoPtsUs + 1_000L
                } else {
                    vRaw.coerceAtLeast(0L)
                }
                lastVideoPtsUs = ptsUs
                val flags = if ((videoExt.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else {
                    0
                }
                info.set(0, readSize, ptsUs, flags)
                newMuxer.writeSampleData(muxVideoTrack, buffer, info)
                if (!videoExt.advance()) videoDone = true
            } else {
                val aExt = audioExt!!
                if (aRaw < audioStartUs) {
                    if (!aExt.advance()) audioDone = true
                    continue
                }
                val sampleSize = aExt.sampleSize.toInt()
                if (sampleSize > buffer.capacity() && sampleSize <= MAX_MUX_BUFFER_BYTES) {
                    buffer = ByteBuffer.allocate(sampleSize)
                }
                buffer.clear()
                val readSize = aExt.readSampleData(buffer, 0)
                if (readSize < 0) {
                    audioDone = true
                    continue
                }
                val ptsUs = normalizeAudioSamplePtsUs(aRaw, audioStartUs, lastAudioPtsUs)
                lastAudioPtsUs = ptsUs
                info.set(0, readSize, ptsUs, MediaCodec.BUFFER_FLAG_KEY_FRAME)
                newMuxer.writeSampleData(muxAudioTrack, buffer, info)
                if (!aExt.advance()) audioDone = true
            }

            if (++written % TRANSCODE_CANCEL_CHECK_INTERVAL == 0) {
                currentCoroutineContext().ensureActive()
            }
        }

        muxerStarted = false
        newMuxer.stop()
    } finally {
        if (muxerStarted) runCatching { muxer?.stop() }
        runCatching { muxer?.release() }
        runCatching { videoExt.release() }
        runCatching { audioExt?.release() }
    }
}

private class CodecInputSurface(private val surface: Surface) {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    init {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL14 display" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "Unable to initialize EGL14" }

        val config = chooseEglConfig(eglDisplay)
        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        check(eglContext != EGL14.EGL_NO_CONTEXT && EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
            "Failed to create EGLContext"
        }

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surface, surfaceAttribs, 0)
        check(eglSurface != EGL14.EGL_NO_SURFACE && EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
            "Failed to create EGLSurface"
        }
    }

    private fun chooseEglConfig(display: EGLDisplay): EGLConfig {
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        val withRecordable = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE,
        )
        if (EGL14.eglChooseConfig(display, withRecordable, 0, configs, 0, configs.size, numConfigs, 0) &&
            numConfigs[0] > 0 && configs[0] != null
        ) {
            return configs[0]!!
        }
        // Some emulator / software GL implementations omit EGL_RECORDABLE_ANDROID configs.
        val withoutRecordable = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE,
        )
        check(
            EGL14.eglChooseConfig(display, withoutRecordable, 0, configs, 0, configs.size, numConfigs, 0) &&
                numConfigs[0] > 0 && configs[0] != null,
        ) { "Unable to find suitable RGB8888 ES2 EGLConfig" }
        return configs[0]!!
    }

    fun makeCurrent() {
        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            "eglMakeCurrent failed"
        }
    }

    fun setPresentationTime(nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
    }

    fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(eglDisplay, eglSurface)

    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        surface.release()
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }

    private companion object {
        const val EGL_RECORDABLE_ANDROID = 0x3142
    }
}

private class CodecOutputSurface(
    private val viewportWidth: Int,
    private val viewportHeight: Int,
) : SurfaceTexture.OnFrameAvailableListener {

    private val textureRender = TextureRender()
    private val callbackThread = HandlerThread("vista-trim-st").apply { start() }
    private val callbackHandler = Handler(callbackThread.looper)
    private val surfaceTexture: SurfaceTexture
    val surface: Surface

    private val frameSync = Object()
    private var frameAvailable = false

    init {
        textureRender.surfaceCreated()
        surfaceTexture = SurfaceTexture(textureRender.textureId)
        surfaceTexture.setOnFrameAvailableListener(this, callbackHandler)
        surface = Surface(surfaceTexture)
    }

    fun prepareForRender() {
        synchronized(frameSync) {
            frameAvailable = false
        }
    }

    override fun onFrameAvailable(st: SurfaceTexture) {
        synchronized(frameSync) {
            frameAvailable = true
            frameSync.notifyAll()
        }
    }

    fun awaitNewImage() {
        val deadline = SystemClock.elapsedRealtime() + FRAME_WAIT_TIMEOUT_MS
        synchronized(frameSync) {
            while (!frameAvailable) {
                val remaining = deadline - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    throw VideoTrimException("زمان انتظار برای پردازش فریم ویدیو تمام شد")
                }
                frameSync.wait(remaining)
            }
            frameAvailable = false
        }
        surfaceTexture.updateTexImage()
    }

    fun drawImage() {
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        textureRender.drawFrame(surfaceTexture)
    }

    fun release() {
        runCatching { surface.release() }
        runCatching { surfaceTexture.release() }
        runCatching { callbackThread.quitSafely() }
    }

    private companion object {
        const val FRAME_WAIT_TIMEOUT_MS = 6_000L
    }
}

private class TextureRender {
    private val triangleVerticesData = floatArrayOf(
        -1.0f, -1.0f, 0f, 0f, 0f,
        1.0f, -1.0f, 0f, 1f, 0f,
        -1.0f, 1.0f, 0f, 0f, 1f,
        1.0f, 1.0f, 0f, 1f, 1f,
    )
    private val triangleVertices: FloatBuffer = ByteBuffer
        .allocateDirect(triangleVerticesData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(triangleVerticesData)
            position(0)
        }

    private val mvpMatrix = FloatArray(16)
    private val stMatrix = FloatArray(16)

    private var program = 0
    var textureId = -12345
        private set
    private var mvpMatrixHandle = 0
    private var stMatrixHandle = 0
    private var positionHandle = 0
    private var textureHandle = 0

    init {
        Matrix.setIdentityM(stMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)
    }

    fun surfaceCreated() {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        check(program != 0) { "Failed creating GL program" }
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        textureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        stMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    fun drawFrame(st: SurfaceTexture) {
        st.getTransformMatrix(stMatrix)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        triangleVertices.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 20, triangleVertices)
        GLES20.glEnableVertexAttribArray(positionHandle)

        triangleVertices.position(3)
        GLES20.glVertexAttribPointer(textureHandle, 2, GLES20.GL_FLOAT, false, 20, triangleVertices)
        GLES20.glEnableVertexAttribArray(textureHandle)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFinish()
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0
        val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) return 0
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vertexShader)
        GLES20.glAttachShader(prog, pixelShader)
        GLES20.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteProgram(prog)
            return 0
        }
        return prog
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        val shader = GLES20.glCreateShader(shaderType)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private companion object {
        const val VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "  gl_Position = uMVPMatrix * aPosition;\n" +
                "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                "}\n"

        const val FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n"
    }
}

private const val CODEC_TIMEOUT_US = 5_000L
private const val REORDER_WINDOW_US = 600_000L
private const val EOS_DRAIN_TIMEOUT_MS = 20_000L
private const val MAX_EMPTY_EOS_DRAIN = 2_400
private const val MUX_BUFFER_BYTES = 2 * 1024 * 1024
private const val MAX_MUX_BUFFER_BYTES = 16 * 1024 * 1024
private const val TRANSCODE_CANCEL_CHECK_INTERVAL = 50
