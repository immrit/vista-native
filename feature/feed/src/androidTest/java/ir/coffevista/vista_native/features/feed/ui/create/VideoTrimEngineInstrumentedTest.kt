package ir.coffevista.vista_native.features.feed.ui.create

import android.content.Context
import android.graphics.ImageFormat
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.coffevista.vista_native.features.feed.ui.components.FeedVideoPlaybackCoordinator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class VideoTrimEngineInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun trimBaselineAndBFrameVideos_produceFastStartMonotonicMp4ThatPlaysLocallyAndOverHttp() = runBlocking {
        val workDir = File(context.cacheDir, "video_trim_test_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            // 1. Baseline H.264 + AAC (90-degree rotation, 6.0s -> 180 frames, trimmed 1.0s..5.0s)
            val baselineSource = File(workDir, "src_baseline_rot90.mp4")
            synthesizeAvcAacMp4(
                outputFile = baselineSource,
                durationMs = 6_000L,
                width = 320,
                height = 240,
                fps = 30,
                orientationDegrees = 90,
                tryBFrames = false,
            )
            val baselineSrcInspect = inspectMp4(baselineSource)
            Log.i(TAG, "SRC_BASELINE: $baselineSrcInspect")

            val trimmedBaseline = trimVideo(
                context = context,
                uri = Uri.fromFile(baselineSource),
                startMs = 1_000L,
                endMs = 5_000L,
                maxDurationMs = 60_000L,
            )
            try {
                val trimmedInspect = inspectMp4(trimmedBaseline)
                Log.i(TAG, "TRIM_BASELINE: $trimmedInspect")
                assertTrue("Baseline trim must be faststart (moov before mdat): $trimmedInspect", Mp4FastStart.isFastStart(trimmedBaseline))
                assertEquals(90, trimmedInspect.rotation)
                assertEquals(0, trimmedInspect.backwardsPtsCount)
                assertEquals(0, trimmedInspect.duplicatePtsCount)
                assertTrue(trimmedInspect.firstSampleIsSync)
                assertTrue("Expected >= 90 baseline frames, got ${trimmedInspect.videoSamples}", trimmedInspect.videoSamples >= 90)
                assertEquals("audio/mp4a-latm", trimmedInspect.audioMime)

                verifyAllPlaybackModes("BASELINE", trimmedBaseline)
            } finally {
                trimmedBaseline.delete()
            }

            // 2. Real H.264 B-frame + AAC (6.0s -> 180 frames, moov at end, trimmed 0.5s..5.0s)
            val bframeSource = File(workDir, "src_bframe_enc.mp4")
            synthesizeAvcAacMp4(
                outputFile = bframeSource,
                durationMs = 6_000L,
                width = 320,
                height = 240,
                fps = 30,
                orientationDegrees = 0,
                tryBFrames = true,
            )
            val bframeSrcInspect = inspectMp4(bframeSource)
            Log.i(TAG, "SRC_BFRAME: $bframeSrcInspect")
            assertTrue("Expected B-frames from encoder, got $bframeSrcInspect", bframeSrcInspect.backwardsPtsCount >= 30)
            assertFalse("Expected trailing moov in B-frame source", bframeSrcInspect.moovBeforeMdat)

            val trimmedBframeMid = trimVideo(
                context = context,
                uri = Uri.fromFile(bframeSource),
                startMs = 500L,
                endMs = 5_000L,
                maxDurationMs = 60_000L,
            )
            try {
                val bframeMidInspect = inspectMp4(trimmedBframeMid)
                Log.i(TAG, "TRIM_BFRAME_MID: $bframeMidInspect")
                assertTrue("B-frame transcoded trim must be faststart (moov before mdat): $bframeMidInspect", Mp4FastStart.isFastStart(trimmedBframeMid))
                assertEquals("B-frame trim must output 0 backwards PTS: $bframeMidInspect", 0, bframeMidInspect.backwardsPtsCount)
                assertEquals(0, bframeMidInspect.duplicatePtsCount)
                assertTrue(bframeMidInspect.firstSampleIsSync)
                assertEquals("audio/mp4a-latm", bframeMidInspect.audioMime)
                assertTrue("Expected >= 90 transcoded frames, got ${bframeMidInspect.videoSamples}", bframeMidInspect.videoSamples >= 90)

                verifyAllPlaybackModes("BFRAME_MID", trimmedBframeMid)
            } finally {
                trimmedBframeMid.delete()
            }

            // 3. Real H.264 B-frame + AAC with 90-degree rotation (trimmed 0.0s..3.0s)
            val bframeRotSource = File(workDir, "src_bframe_rot90.mp4")
            synthesizeAvcAacMp4(
                outputFile = bframeRotSource,
                durationMs = 4_500L,
                width = 320,
                height = 240,
                fps = 30,
                orientationDegrees = 90,
                tryBFrames = true,
            )
            val trimmedBframeRot = trimVideo(
                context = context,
                uri = Uri.fromFile(bframeRotSource),
                startMs = 0L,
                endMs = 3_000L,
                maxDurationMs = 60_000L,
            )
            try {
                val rotInspect = inspectMp4(trimmedBframeRot)
                Log.i(TAG, "TRIM_BFRAME_ROT90: $rotInspect")
                assertEquals("Rotation metadata must survive transcoding", 90, rotInspect.rotation)
                assertEquals(0, rotInspect.backwardsPtsCount)
                assertTrue(Mp4FastStart.isFastStart(trimmedBframeRot))

                val srv = LocalMp4HttpServer(trimmedBframeRot, supportRange = true).apply { start() }
                val report = try {
                    playWithExoPlayer(srv.url, useFeedCacheFactory = true)
                } finally {
                    srv.close()
                }
                Log.i(TAG, "PLAY_BFRAME_ROT90_HTTP_RANGE: $report")
                assertPlaybackHealthy("BFRAME_ROT90_HTTP_RANGE", report)
            } finally {
                trimmedBframeRot.delete()
            }
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun verifyAllPlaybackModes(label: String, file: File) {
        val localReport = playWithExoPlayer(file.toURI().toString(), useFeedCacheFactory = false)
        Log.i(TAG, "PLAY_${label}_LOCAL: $localReport")
        assertPlaybackHealthy("${label}_LOCAL", localReport)

        val rangeServer = LocalMp4HttpServer(file, supportRange = true).apply { start() }
        val rangeReport = try {
            playWithExoPlayer(rangeServer.url, useFeedCacheFactory = true)
        } finally {
            rangeServer.close()
        }
        Log.i(TAG, "PLAY_${label}_HTTP_RANGE: $rangeReport")
        assertPlaybackHealthy("${label}_HTTP_RANGE", rangeReport)

        val noRangeServer = LocalMp4HttpServer(file, supportRange = false).apply { start() }
        val noRangeReport = try {
            playWithExoPlayer(noRangeServer.url, useFeedCacheFactory = true)
        } finally {
            noRangeServer.close()
        }
        Log.i(TAG, "PLAY_${label}_HTTP_NORANGE: $noRangeReport")
        assertPlaybackHealthy("${label}_HTTP_NORANGE", noRangeReport)
    }

    private fun assertPlaybackHealthy(label: String, report: PlaybackReport) {
        assertTrue("$label error: ${report.error}", report.error == null)
        assertTrue("$label did not render first frame: $report", report.renderedFirstFrame)
        assertTrue("$label too few rendered frames: $report", report.renderedFrames >= 10)
        assertTrue("$label playback stalled at ${report.endPositionMs}ms: $report", report.endPositionMs >= 500L)
    }

    private data class Mp4Inspection(
        val fileSize: Long,
        val topLevelBoxes: List<String>,
        val moovBeforeMdat: Boolean,
        val rotation: Int,
        val retrieverDurationMs: Long,
        val videoMime: String?,
        val audioMime: String?,
        val videoSamples: Int,
        val backwardsPtsCount: Int,
        val duplicatePtsCount: Int,
        val firstSampleIsSync: Boolean,
        val first12PtsUs: List<Long>,
    )

    private fun inspectMp4(file: File): Mp4Inspection {
        val boxes = parseTopLevelBoxNames(file)
        val moovIdx = boxes.indexOf("moov")
        val mdatIdx = boxes.indexOf("mdat")
        val moovBeforeMdat = moovIdx >= 0 && mdatIdx >= 0 && moovIdx < mdatIdx

        var rotation = 0
        var retrieverDur = 0L
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            retrieverDur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            runCatching { retriever.release() }
        }

        val extractor = MediaExtractor()
        var videoMime: String? = null
        var audioMime: String? = null
        var videoSamples = 0
        var backwards = 0
        var duplicates = 0
        var firstSync = false
        val first12 = ArrayList<Long>(12)
        try {
            extractor.setDataSource(file.absolutePath)
            var vTrack = -1
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true && vTrack < 0) {
                    vTrack = i
                    videoMime = mime
                } else if (mime?.startsWith("audio/") == true && audioMime == null) {
                    audioMime = mime
                }
            }
            if (vTrack >= 0) {
                extractor.selectTrack(vTrack)
                var prevPts = Long.MIN_VALUE
                while (true) {
                    val pts = extractor.sampleTime
                    if (pts < 0L) break
                    if (videoSamples == 0) {
                        firstSync = (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                    }
                    if (first12.size < 12) first12 += pts
                    if (prevPts != Long.MIN_VALUE) {
                        if (pts < prevPts) backwards++
                        else if (pts == prevPts) duplicates++
                    }
                    prevPts = pts
                    videoSamples++
                    if (!extractor.advance()) break
                }
            }
        } finally {
            runCatching { extractor.release() }
        }

        return Mp4Inspection(
            fileSize = file.length(),
            topLevelBoxes = boxes,
            moovBeforeMdat = moovBeforeMdat,
            rotation = rotation,
            retrieverDurationMs = retrieverDur,
            videoMime = videoMime,
            audioMime = audioMime,
            videoSamples = videoSamples,
            backwardsPtsCount = backwards,
            duplicatePtsCount = duplicates,
            firstSampleIsSync = firstSync,
            first12PtsUs = first12,
        )
    }

    private fun parseTopLevelBoxNames(file: File): List<String> {
        val result = ArrayList<String>()
        RandomAccessFile(file, "r").use { raf ->
            val len = raf.length()
            var offset = 0L
            val header = ByteArray(16)
            while (offset + 8L <= len) {
                raf.seek(offset)
                if (raf.read(header, 0, 8) != 8) break
                val buf = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN)
                val size32 = buf.getInt(0).toLong() and 0xFFFFFFFFL
                val type = String(header, 4, 4, Charsets.US_ASCII)
                val boxSize = when {
                    size32 == 0L -> len - offset
                    size32 == 1L -> {
                        if (offset + 16L > len || raf.read(header, 8, 8) != 8) break
                        ByteBuffer.wrap(header, 8, 8).order(ByteOrder.BIG_ENDIAN).long
                    }
                    else -> size32
                }
                if (boxSize < 8L) break
                result += type
                offset += boxSize
            }
        }
        return result
    }

    private data class PlaybackReport(
        val renderedFirstFrame: Boolean,
        val renderedFrames: Int,
        val durationMs: Long,
        val endPositionMs: Long,
        val error: String?,
        val endedEarly: Boolean,
    )

    @OptIn(UnstableApi::class)
    private fun playWithExoPlayer(
        url: String,
        useFeedCacheFactory: Boolean,
    ): PlaybackReport {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val consumerThread = HandlerThread("exo-image-consumer").apply { start() }
        val consumerHandler = Handler(consumerThread.looper)
        val imageReader = ImageReader.newInstance(320, 240, ImageFormat.PRIVATE, 4)
        val frameCount = AtomicInteger(0)
        imageReader.setOnImageAvailableListener({ reader ->
            runCatching {
                reader.acquireLatestImage()?.close()
            }
            frameCount.incrementAndGet()
        }, consumerHandler)
        val surface = imageReader.surface

        val firstFrameLatch = CountDownLatch(1)
        val startedPlayingLatch = CountDownLatch(1)
        val doneOrErrorLatch = CountDownLatch(1)
        val renderedFirstFrame = AtomicBoolean(false)
        val errorRef = AtomicReference<String?>(null)
        val endedEarly = AtomicBoolean(false)
        val playerRef = AtomicReference<ExoPlayer?>(null)

        try {
            instrumentation.runOnMainSync {
                val builder = ExoPlayer.Builder(context)
                    .setReleaseTimeoutMs(3_000L)
                if (useFeedCacheFactory) {
                    builder.setMediaSourceFactory(
                        DefaultMediaSourceFactory(
                            FeedVideoPlaybackCoordinator.buildCacheDataSourceFactory(context),
                        ),
                    )
                }
                val player = builder.build().apply {
                    setVideoSurface(surface)
                    addListener(object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            renderedFirstFrame.set(true)
                            firstFrameLatch.countDown()
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) startedPlayingLatch.countDown()
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                endedEarly.set(true)
                                doneOrErrorLatch.countDown()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            errorRef.set("${error.errorCodeName}: ${error.message} (${error.cause})")
                            firstFrameLatch.countDown()
                            startedPlayingLatch.countDown()
                            doneOrErrorLatch.countDown()
                        }
                    })
                    setMediaItem(MediaItem.fromUri(Uri.parse(url)))
                    prepare()
                    playWhenReady = true
                }
                playerRef.set(player)
            }

            startedPlayingLatch.await(5, TimeUnit.SECONDS)
            firstFrameLatch.await(5, TimeUnit.SECONDS)
            if (errorRef.get() == null) {
                doneOrErrorLatch.await(2_500, TimeUnit.MILLISECONDS)
            }

            val dur = AtomicLong(-1L)
            val pos = AtomicLong(-1L)
            instrumentation.runOnMainSync {
                playerRef.get()?.let { p ->
                    dur.set(p.duration)
                    pos.set(p.currentPosition)
                    p.stop()
                    p.release()
                }
            }
            return PlaybackReport(
                renderedFirstFrame = renderedFirstFrame.get(),
                renderedFrames = frameCount.get(),
                durationMs = dur.get(),
                endPositionMs = pos.get(),
                error = errorRef.get(),
                endedEarly = endedEarly.get(),
            )
        } finally {
            instrumentation.runOnMainSync {
                runCatching { playerRef.get()?.release() }
            }
            runCatching { imageReader.close() }
            runCatching { consumerThread.quitSafely() }
        }
    }

    private data class EncodedSample(
        val bytes: ByteArray,
        val ptsUs: Long,
        val flags: Int,
    )

    private fun synthesizeAvcAacMp4(
        outputFile: File,
        durationMs: Long,
        width: Int,
        height: Int,
        fps: Int,
        orientationDegrees: Int,
        tryBFrames: Boolean,
    ) {
        val videoPackets = encodeH264Samples(width, height, fps, durationMs, tryBFrames)
        val audioPackets = encodeAacSamples(durationMs)

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false
        try {
            muxer.setOrientationHint(orientationDegrees)
            val vTrack = muxer.addTrack(videoPackets.first)
            val aTrack = muxer.addTrack(audioPackets.first)
            muxer.start()
            started = true

            val vSamples = videoPackets.second
            val aSamples = audioPackets.second
            var vIdx = 0
            var aIdx = 0
            val info = MediaCodec.BufferInfo()
            while (vIdx < vSamples.size || aIdx < aSamples.size) {
                val writeVideo = when {
                    vIdx >= vSamples.size -> false
                    aIdx >= aSamples.size -> true
                    else -> vSamples[vIdx].ptsUs <= aSamples[aIdx].ptsUs
                }
                if (writeVideo) {
                    val s = vSamples[vIdx++]
                    info.set(0, s.bytes.size, s.ptsUs, s.flags)
                    muxer.writeSampleData(vTrack, ByteBuffer.wrap(s.bytes), info)
                } else {
                    val s = aSamples[aIdx++]
                    info.set(0, s.bytes.size, s.ptsUs, s.flags)
                    muxer.writeSampleData(aTrack, ByteBuffer.wrap(s.bytes), info)
                }
            }
        } finally {
            if (started) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    private fun encodeH264Samples(
        width: Int,
        height: Int,
        fps: Int,
        durationMs: Long,
        tryBFrames: Boolean,
    ): Pair<MediaFormat, List<EncodedSample>> {
        val totalFrames = ((durationMs * fps) / 1000L).toInt().coerceAtLeast(10)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, 900_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            if (tryBFrames && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileMain)
                setInteger(MediaFormat.KEY_MAX_B_FRAMES, 2)
                setInteger(MediaFormat.KEY_LATENCY, 4)
            }
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        try {
            try {
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            } catch (_: Exception) {
                val fallback = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                    setInteger(MediaFormat.KEY_BIT_RATE, 900_000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
                encoder.reset()
                encoder.configure(fallback, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
            encoder.start()

            val samples = ArrayList<EncodedSample>(totalFrames)
            var outFormat: MediaFormat? = null
            var inputFrame = 0
            var inputDone = false
            var outputDone = false
            val info = MediaCodec.BufferInfo()

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = encoder.dequeueInputBuffer(5_000L)
                    if (inIdx >= 0) {
                        if (inputFrame >= totalFrames) {
                            encoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val ptsUs = inputFrame * 1_000_000L / fps
                            val byteSize = fillSyntheticYuvFrame(encoder, inIdx, width, height, inputFrame)
                            encoder.queueInputBuffer(inIdx, 0, byteSize, ptsUs, 0)
                            inputFrame++
                        }
                    }
                }
                val outIdx = encoder.dequeueOutputBuffer(info, 5_000L)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outFormat = encoder.outputFormat
                    outIdx >= 0 -> {
                        val buf = encoder.getOutputBuffer(outIdx)
                        val isConfig = (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (!isConfig && info.size > 0 && buf != null) {
                            val copy = ByteArray(info.size)
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            buf.get(copy)
                            val flags = if ((info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                                MediaCodec.BUFFER_FLAG_KEY_FRAME
                            } else {
                                0
                            }
                            samples += EncodedSample(copy, info.presentationTimeUs, flags)
                        }
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                        encoder.releaseOutputBuffer(outIdx, false)
                    }
                }
            }
            return Pair(assertNotNullAndReturn(outFormat), samples)
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
        }
    }

    private fun <T : Any> assertNotNullAndReturn(value: T?): T {
        assertNotNull(value)
        return value!!
    }

    private fun fillSyntheticYuvFrame(
        encoder: MediaCodec,
        inputIdx: Int,
        width: Int,
        height: Int,
        frameIndex: Int,
    ): Int {
        val image = runCatching { encoder.getInputImage(inputIdx) }.getOrNull()
        val stripeX = (frameIndex * 6) % width
        if (image != null) {
            try {
                val yPlane = image.planes[0]
                val uPlane = image.planes[1]
                val vPlane = image.planes[2]
                val yBuf = yPlane.buffer
                for (row in 0 until height) {
                    val rowBase = row * yPlane.rowStride
                    for (col in 0 until width) {
                        val luma = if (col in stripeX until (stripeX + 24).coerceAtMost(width)) 220 else 64
                        yBuf.put(rowBase + col * yPlane.pixelStride, luma.toByte())
                    }
                }
                val chromaW = width / 2
                val chromaH = height / 2
                val uBuf = uPlane.buffer
                val vBuf = vPlane.buffer
                val uVal = ((96 + (frameIndex * 2) % 64) and 0xFF).toByte()
                val vVal = ((140 + (frameIndex * 3) % 64) and 0xFF).toByte()
                for (row in 0 until chromaH) {
                    val uRow = row * uPlane.rowStride
                    val vRow = row * vPlane.rowStride
                    for (col in 0 until chromaW) {
                        uBuf.put(uRow + col * uPlane.pixelStride, uVal)
                        vBuf.put(vRow + col * vPlane.pixelStride, vVal)
                    }
                }
                return width * height * 3 / 2
            } finally {
                image.close()
            }
        }
        val buf = encoder.getInputBuffer(inputIdx)!!
        buf.clear()
        val ySize = width * height
        val uvSize = ySize / 4
        val data = ByteArray(ySize + uvSize * 2) { idx ->
            when {
                idx < ySize -> if ((idx % width) in stripeX until (stripeX + 24)) 220.toByte() else 64.toByte()
                else -> 128.toByte()
            }
        }
        buf.put(data)
        return data.size
    }

    private fun encodeAacSamples(durationMs: Long): Pair<MediaFormat, List<EncodedSample>> {
        val sampleRate = 44_100
        val channels = 1
        val totalSamples = (durationMs * sampleRate) / 1000L
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 64_000)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val samples = ArrayList<EncodedSample>()
            var outFormat: MediaFormat? = null
            var writtenSamples = 0L
            var inputDone = false
            var outputDone = false
            val info = MediaCodec.BufferInfo()

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = encoder.dequeueInputBuffer(5_000L)
                    if (inIdx >= 0) {
                        if (writtenSamples >= totalSamples) {
                            encoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val chunkSamples = minOf(1024L, totalSamples - writtenSamples).toInt()
                            val buf = encoder.getInputBuffer(inIdx)!!.apply {
                                clear()
                                order(ByteOrder.LITTLE_ENDIAN)
                            }
                            val ptsUs = writtenSamples * 1_000_000L / sampleRate
                            for (i in 0 until chunkSamples) {
                                val sampleIdx = writtenSamples + i
                                val pcm = (sin(2.0 * PI * 440.0 * sampleIdx / sampleRate) * 12_000.0).toInt().toShort()
                                buf.putShort(pcm)
                            }
                            encoder.queueInputBuffer(inIdx, 0, chunkSamples * 2, ptsUs, 0)
                            writtenSamples += chunkSamples
                        }
                    }
                }
                val outIdx = encoder.dequeueOutputBuffer(info, 5_000L)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outFormat = encoder.outputFormat
                    outIdx >= 0 -> {
                        val buf = encoder.getOutputBuffer(outIdx)
                        val isConfig = (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (!isConfig && info.size > 0 && buf != null) {
                            val copy = ByteArray(info.size)
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            buf.get(copy)
                            samples += EncodedSample(copy, info.presentationTimeUs, 0)
                        }
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                        encoder.releaseOutputBuffer(outIdx, false)
                    }
                }
            }
            return Pair(assertNotNullAndReturn(outFormat), samples)
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
        }
    }

    /** Tiny localhost HTTP MP4 server simulating both Range (206) and non-Range (200) CDNs. */
    private class LocalMp4HttpServer(
        private val file: File,
        private val supportRange: Boolean,
    ) : Closeable {
        private val serverSocket = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))

        @Volatile
        private var running = true
        private var acceptThread: Thread? = null

        val url: String
            get() = "http://127.0.0.1:${serverSocket.localPort}/clip.mp4"

        fun start() {
            acceptThread = Thread {
                while (running) {
                    val client = runCatching { serverSocket.accept() }.getOrNull() ?: break
                    Thread { handleClient(client) }.apply { isDaemon = true }.start()
                }
            }.apply {
                isDaemon = true
                start()
            }
        }

        private fun handleClient(socket: Socket) {
            socket.use { sock ->
                val input = BufferedInputStream(sock.getInputStream())
                val requestLine = readLineAscii(input) ?: return
                val parts = requestLine.split(' ')
                val method = parts.getOrNull(0) ?: return
                var rangeHeader: String? = null
                while (true) {
                    val line = readLineAscii(input) ?: break
                    if (line.isEmpty()) break
                    if (line.startsWith("Range:", ignoreCase = true)) {
                        rangeHeader = line.substringAfter(':').trim()
                    }
                }

                val totalLen = file.length()
                val out = sock.getOutputStream()
                if (supportRange && rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    val spec = rangeHeader.removePrefix("bytes=").substringBefore(',')
                    val bounds = spec.split('-')
                    val start = bounds.getOrNull(0)?.toLongOrNull() ?: 0L
                    val end = bounds.getOrNull(1)?.toLongOrNull()?.coerceAtMost(totalLen - 1) ?: (totalLen - 1)
                    val len = (end - start + 1).coerceAtLeast(0L)
                    val hdr = buildString {
                        append("HTTP/1.1 206 Partial Content\r\n")
                        append("Content-Type: video/mp4\r\n")
                        append("Accept-Ranges: bytes\r\n")
                        append("Content-Range: bytes $start-$end/$totalLen\r\n")
                        append("Content-Length: $len\r\n")
                        append("Connection: close\r\n\r\n")
                    }
                    out.write(hdr.toByteArray(Charsets.US_ASCII))
                    if (!method.equals("HEAD", ignoreCase = true)) {
                        streamFileRange(out, start, len)
                    }
                } else {
                    val hdr = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: video/mp4\r\n")
                        append("Accept-Ranges: ${if (supportRange) "bytes" else "none"}\r\n")
                        append("Content-Length: $totalLen\r\n")
                        append("Connection: close\r\n\r\n")
                    }
                    out.write(hdr.toByteArray(Charsets.US_ASCII))
                    if (!method.equals("HEAD", ignoreCase = true)) {
                        streamFileRange(out, 0L, totalLen)
                    }
                }
                out.flush()
            }
        }

        private fun streamFileRange(out: java.io.OutputStream, start: Long, length: Long) {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val buf = ByteArray(32 * 1024)
                var remaining = length
                while (remaining > 0 && running) {
                    val toRead = minOf(buf.size.toLong(), remaining).toInt()
                    val n = raf.read(buf, 0, toRead)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                    remaining -= n
                }
            }
        }

        private fun readLineAscii(input: BufferedInputStream): String? {
            val sb = StringBuilder()
            while (true) {
                val b = input.read()
                if (b < 0) return if (sb.isEmpty()) null else sb.toString()
                if (b == '\n'.code) {
                    if (sb.endsWith('\r')) sb.setLength(sb.length - 1)
                    return sb.toString()
                }
                sb.append(b.toChar())
            }
        }

        override fun close() {
            running = false
            runCatching { serverSocket.close() }
        }
    }

    private companion object {
        const val TAG = "VideoTrimDiag"
    }
}
