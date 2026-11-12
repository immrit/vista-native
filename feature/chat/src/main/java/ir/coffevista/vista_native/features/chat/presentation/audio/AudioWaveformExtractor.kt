package ir.coffevista.vista_native.features.chat.presentation.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts real audio amplitude waveforms from local audio files (`.m4a`, `.mp3`, `.aac`, `.ogg`,
 * `.wav`, etc.) using Android's [MediaExtractor] and [MediaCodec] (with a fast direct PCM path
 * for WAV files).
 *
 * Unlike synthetic `idx % 7` waveforms, every bar represents the real RMS energy of that time slice
 * of the audio file, normalized perceptually so quiet and loud recordings both read clearly.
 */
object AudioWaveformExtractor {
    private const val DEFAULT_BARS = 48
    private const val MAX_CACHE_ENTRIES = 256
    private const val DECODE_TIMEOUT_US = 8_000L
    private const val MAX_DECODED_SAMPLES = 450_000L

    private val cache = ConcurrentHashMap<String, List<Float>>()

    fun getCached(uriOrPath: String): List<Float>? = cache[uriOrPath]

    fun hasMeaningfulVariation(samples: List<Float>): Boolean {
        if (samples.size < 4) return false
        val min = samples.minOrNull() ?: return false
        val max = samples.maxOrNull() ?: return false
        return (max - min) >= 0.04f
    }

    fun encodeWaveform(samples: List<Float>): String {
        if (samples.isEmpty()) return ""
        return "wf:" + samples.joinToString(",") { value ->
            String.format(Locale.US, "%.2f", value.coerceIn(0.05f, 1.0f))
        }
    }

    suspend fun extract(
        context: Context,
        uriOrPath: String,
        targetBars: Int = DEFAULT_BARS,
    ): List<Float>? {
        val clean = uriOrPath.trim()
        if (clean.isEmpty()) return null
        val cacheKey = "$clean#$targetBars"
        cache[cacheKey]?.let { return it }

        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                extractWavIfApplicable(context, clean, targetBars)
                    ?: extractWithMediaCodec(context, clean, targetBars)
            }.getOrNull()
        } ?: return null

        if (cache.size > MAX_CACHE_ENTRIES) {
            cache.clear()
        }
        cache[cacheKey] = extracted
        return extracted
    }

    private fun extractWavIfApplicable(
        context: Context,
        uriOrPath: String,
        targetBars: Int,
    ): List<Float>? {
        val lower = uriOrPath.lowercase(Locale.US)
        if (!lower.endsWith(".wav")) return null
        val bytes = openBytes(context, uriOrPath, maxBytes = 8 * 1024 * 1024) ?: return null
        if (bytes.size < 44) return null
        val header = String(bytes, 0, 4, Charsets.US_ASCII)
        val wave = String(bytes, 8, 4, Charsets.US_ASCII)
        if (header != "RIFF" || wave != "WAVE") return null

        var offset = 12
        var bitsPerSample = 16
        var dataOffset = -1
        var dataSize = 0
        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4, Charsets.US_ASCII)
            val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val bodyStart = offset + 8
            if (chunkSize < 0 || bodyStart > bytes.size) break
            if (chunkId == "fmt " && chunkSize >= 16 && bodyStart + 16 <= bytes.size) {
                bitsPerSample = ByteBuffer.wrap(bytes, bodyStart + 14, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            } else if (chunkId == "data") {
                dataOffset = bodyStart
                dataSize = minOf(chunkSize, bytes.size - bodyStart)
                break
            }
            offset = bodyStart + chunkSize + (chunkSize and 1)
        }
        if (dataOffset < 0 || dataSize <= 0 || bitsPerSample != 16) return null

        val totalSamples = dataSize / 2
        if (totalSamples < targetBars) return null
        val samplesPerBucket = (totalSamples / targetBars).coerceAtLeast(1)
        val rawBuckets = FloatArray(targetBars)
        val bb = ByteBuffer.wrap(bytes, dataOffset, dataSize).order(ByteOrder.LITTLE_ENDIAN)

        for (bar in 0 until targetBars) {
            val startSample = bar * samplesPerBucket
            val endSample = if (bar == targetBars - 1) totalSamples else minOf(totalSamples, (bar + 1) * samplesPerBucket)
            var sumSq = 0.0
            var count = 0
            val step = ((endSample - startSample) / 256).coerceAtLeast(1)
            var idx = startSample
            while (idx < endSample) {
                val sample = bb.getShort(dataOffset + idx * 2).toFloat() / 32768f
                sumSq += (sample * sample).toDouble()
                count++
                idx += step
            }
            rawBuckets[bar] = if (count > 0) sqrt(sumSq / count).toFloat() else 0f
        }
        return normalizeBuckets(rawBuckets)
    }

    private fun extractWithMediaCodec(
        context: Context,
        uriOrPath: String,
        targetBars: Int,
    ): List<Float>? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        return try {
            val parsed = Uri.parse(uriOrPath)
            when {
                uriOrPath.startsWith("/") -> extractor.setDataSource(uriOrPath)
                parsed.scheme == "file" && !parsed.path.isNullOrBlank() -> extractor.setDataSource(parsed.path!!)
                parsed.scheme == "content" -> extractor.setDataSource(context, parsed, null)
                else -> return null
            }

            var audioTrack = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    format = trackFormat
                    break
                }
            }
            if (audioTrack < 0 || format == null) return null
            extractor.selectTrack(audioTrack)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bucketEnergy = DoubleArray(targetBars)
            val bucketCount = IntArray(targetBars)
            val fallbackRms = ArrayList<Float>(128)

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var totalDecodedSamples = 0L
            var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            } else {
                1
            }

            // For long files, skip evenly across the timeline so decoding always stays under ~100ms.
            val useSeekSampling = durationUs > 45_000_000L
            var currentSeekBucket = 0

            while (!outputDone && totalDecodedSamples < MAX_DECODED_SAMPLES) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(DECODE_TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inBuffer = codec.getInputBuffer(inIndex)
                        if (inBuffer != null) {
                            if (useSeekSampling && currentSeekBucket < targetBars) {
                                val targetTimeUs = (durationUs * currentSeekBucket) / targetBars
                                extractor.seekTo(targetTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            }
                            val sampleSize = extractor.readSampleData(inBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                val pts = extractor.sampleTime.coerceAtLeast(0L)
                                codec.queueInputBuffer(inIndex, 0, sampleSize, pts, 0)
                                if (useSeekSampling) {
                                    currentSeekBucket++
                                    if (currentSeekBucket >= targetBars) {
                                        inputDone = true
                                    }
                                } else {
                                    extractor.advance()
                                }
                            }
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, DECODE_TIMEOUT_US)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outFormat = codec.outputFormat
                        if (outFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                        }
                    }
                    outIndex >= 0 -> {
                        val outBuffer = codec.getOutputBuffer(outIndex)
                        if (outBuffer != null && bufferInfo.size > 0) {
                            outBuffer.position(bufferInfo.offset)
                            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val shortBuf = outBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val sampleCount = shortBuf.remaining()
                            var sumSq = 0.0
                            var counted = 0
                            val stride = (channels * 4).coerceAtLeast(1)
                            var idx = 0
                            while (idx < sampleCount) {
                                val norm = shortBuf.get(idx).toFloat() / 32768f
                                sumSq += (norm * norm).toDouble()
                                counted++
                                idx += stride
                            }
                            totalDecodedSamples += (sampleCount / channels).coerceAtLeast(1)
                            val frameRms = if (counted > 0) sqrt(sumSq / counted).toFloat() else 0f
                            if (durationUs > 0L) {
                                val bucket = ((bufferInfo.presentationTimeUs.coerceAtLeast(0L) * targetBars) / durationUs)
                                    .toInt()
                                    .coerceIn(0, targetBars - 1)
                                bucketEnergy[bucket] += frameRms.toDouble()
                                bucketCount[bucket] += 1
                            } else {
                                fallbackRms.add(frameRms)
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            val raw = FloatArray(targetBars)
            if (durationUs > 0L && bucketCount.any { it > 0 }) {
                for (i in 0 until targetBars) {
                    raw[i] = if (bucketCount[i] > 0) (bucketEnergy[i] / bucketCount[i]).toFloat() else 0f
                }
            } else if (fallbackRms.isNotEmpty()) {
                for (i in 0 until targetBars) {
                    val start = (i * fallbackRms.size) / targetBars
                    val end = (((i + 1) * fallbackRms.size) / targetBars).coerceAtLeast(start + 1).coerceAtMost(fallbackRms.size)
                    var sum = 0f
                    for (j in start until end) sum += fallbackRms[j]
                    raw[i] = sum / (end - start).coerceAtLeast(1)
                }
            } else {
                return null
            }

            normalizeBuckets(raw)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun normalizeBuckets(raw: FloatArray): List<Float> {
        val peak = raw.maxOrNull()?.coerceAtLeast(0.001f) ?: return emptyList()
        if (peak <= 0.0015f) {
            return List(raw.size) { 0.12f }
        }
        return raw.map { v ->
            val relative = (v / peak).coerceIn(0f, 1f)
            // Perceptual power curve mirrors Telegram's 5-bit logarithmic speech scaling.
            val perceptual = relative.toDouble().pow(0.55).toFloat()
            (0.10f + perceptual * 0.90f).coerceIn(0.10f, 1.0f)
        }
    }

    private fun openBytes(context: Context, uriOrPath: String, maxBytes: Int): ByteArray? {
        val parsed = Uri.parse(uriOrPath)
        val stream = when {
            uriOrPath.startsWith("/") -> File(uriOrPath).takeIf(File::isFile)?.let(::FileInputStream)
            parsed.scheme == "file" && !parsed.path.isNullOrBlank() ->
                File(parsed.path!!).takeIf(File::isFile)?.let(::FileInputStream)
            else -> runCatching { context.contentResolver.openInputStream(parsed) }.getOrNull()
        } ?: return null

        return stream.use { input ->
            val buf = ByteArray(maxBytes)
            var total = 0
            while (total < maxBytes) {
                val read = input.read(buf, total, maxBytes - total)
                if (read <= 0) break
                total += read
            }
            if (total == 0) null else buf.copyOf(total)
        }
    }

    fun normalizeLiveAmplitude(maxAmplitude: Int): Float {
        val fraction = (abs(maxAmplitude).toFloat() / 32767f).coerceIn(0f, 1f)
        return if (fraction > 0.001f) {
            (0.08f + fraction.toDouble().pow(0.40).toFloat() * 0.92f).coerceIn(0.08f, 1f)
        } else {
            0.08f
        }
    }
}
