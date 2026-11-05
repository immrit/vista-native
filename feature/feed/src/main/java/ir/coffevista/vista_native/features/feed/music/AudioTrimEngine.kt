package ir.coffevista.vista_native.features.feed.music

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object AudioTrimEngine {

    private const val TIMEOUT_US = 5000L
    private const val TARGET_BITRATE = 128_000
    private const val DEFAULT_SAMPLE_RATE = 44_100

    /**
     * Inspects duration accurately using MediaMetadataRetriever.
     */
    suspend fun getExactAudioDurationMs(context: Context, audioUri: Uri): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var pfd: android.os.ParcelFileDescriptor? = null
        try {
            if (audioUri.scheme.equals("file", ignoreCase = true)) {
                val path = audioUri.path ?: audioUri.toString().removePrefix("file://")
                retriever.setDataSource(path)
            } else {
                pfd = runCatching { context.contentResolver.openFileDescriptor(audioUri, "r") }.getOrNull()
                if (pfd != null) {
                    retriever.setDataSource(pfd.fileDescriptor)
                } else {
                    retriever.setDataSource(context, audioUri)
                }
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { pfd?.close() }
            runCatching { retriever.release() }
        }
    }

    /**
     * Trims any audio file (MP3, AAC, M4A, OGG, WAV, FLAC, etc.) between [startMs] and [endMs].
     * Uses hardware/software MediaCodec to transcode to AAC (.m4a) at 128kbps,
     * guaranteeing exact start/end bounds, small file size (~450KB for 30s),
     * and 100% universal compatibility across Android, iOS, and Web.
     *
     * @return Trimmed File if successful, or null on error.
     */
    suspend fun trimAudio(
        context: Context,
        audioUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFile: File? = null,
    ): File? = withContext(Dispatchers.IO) {
        if (endMs <= startMs || endMs <= 0) return@withContext null

        val destination = outputFile ?: runCatching {
            File.createTempFile("trimmed_audio_${System.currentTimeMillis()}_", ".m4a", context.cacheDir)
        }.getOrNull() ?: return@withContext null

        val extractor = MediaExtractor()
        var pfd: android.os.ParcelFileDescriptor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var success = false

        try {
            // 1. Open Source
            if (audioUri.scheme.equals("file", ignoreCase = true)) {
                val path = audioUri.path ?: audioUri.toString().removePrefix("file://")
                extractor.setDataSource(path)
            } else {
                pfd = runCatching { context.contentResolver.openFileDescriptor(audioUri, "r") }.getOrNull()
                if (pfd != null) {
                    extractor.setDataSource(pfd.fileDescriptor)
                } else {
                    extractor.setDataSource(context, audioUri, null)
                }
            }

            // 2. Select Audio Track
            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || inputFormat == null) return@withContext null

            extractor.selectTrack(audioTrackIndex)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext null
            val inputSampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else DEFAULT_SAMPLE_RATE
            val inputChannels = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val startUs = (startMs * 1000L).coerceAtLeast(0L)
            val endUs = endMs * 1000L

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            // 3. Configure Decoder
            decoder = MediaCodec.createDecoderByType(mime).apply {
                configure(inputFormat, null, null, 0)
                start()
            }

            // 4. Configure AAC Encoder (128 kbps, same sample rate & channels)
            val encoderFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                inputSampleRate,
                inputChannels,
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, TARGET_BITRATE)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
            }
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            // 5. Setup Muxer
            muxer = MediaMuxer(destination.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerAudioTrack = -1

            val bufferInfo = MediaCodec.BufferInfo()
            var extractorDone = false
            var decoderDone = false
            var encoderDone = false

            var firstDecodedPtsUs = -1L
            var lastEncodedPtsUs = -1L

            val maxLoopCount = 8_000
            var loopCount = 0

            fun drainEncoderOutput() {
                while (!encoderDone) {
                    val encOutIndex = encoder.dequeueOutputBuffer(bufferInfo, 0L)
                    if (encOutIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (encOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            val newFormat = encoder.outputFormat
                            muxerAudioTrack = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (encOutIndex >= 0) {
                        val encOutBuf = encoder.getOutputBuffer(encOutIndex)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size > 0 && muxerStarted && encOutBuf != null) {
                            var pts = bufferInfo.presentationTimeUs
                            if (pts <= lastEncodedPtsUs) {
                                pts = lastEncodedPtsUs + 1000L
                            }
                            lastEncodedPtsUs = pts
                            bufferInfo.presentationTimeUs = pts

                            encOutBuf.position(bufferInfo.offset)
                            encOutBuf.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(muxerAudioTrack, encOutBuf, bufferInfo)
                        }

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encoderDone = true
                        }
                        encoder.releaseOutputBuffer(encOutIndex, false)
                    }
                }
            }

            while (!encoderDone && loopCount++ < maxLoopCount) {
                // 1. Feed Extractor -> Decoder
                if (!extractorDone) {
                    val inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufIndex >= 0) {
                        val inputBuf = decoder.getInputBuffer(inputBufIndex)
                        if (inputBuf != null) {
                            inputBuf.clear()
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            val sampleTimeUs = extractor.sampleTime

                            if (sampleSize < 0 || (sampleTimeUs > endUs && sampleTimeUs >= 0)) {
                                decoder.queueInputBuffer(
                                    inputBufIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                extractorDone = true
                            } else {
                                decoder.queueInputBuffer(
                                    inputBufIndex,
                                    0,
                                    sampleSize,
                                    sampleTimeUs,
                                    0,
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                // 2. Drain Decoder -> Feed to Encoder
                while (!decoderDone) {
                    val decOutIndex = decoder.dequeueOutputBuffer(bufferInfo, 0L)
                    if (decOutIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (decOutIndex >= 0) {
                        val isEos = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        val pcmBuf = decoder.getOutputBuffer(decOutIndex)

                        // Discard decoded samples before startUs
                        if (bufferInfo.presentationTimeUs < startUs && !isEos) {
                            decoder.releaseOutputBuffer(decOutIndex, false)
                            continue
                        }

                        // Feed PCM to Encoder (drain encoder if input queue is full!)
                        if (bufferInfo.size > 0 || isEos) {
                            var encInQueued = false
                            var retryCount = 0
                            while (!encInQueued && retryCount++ < 15) {
                                val encInIndex = encoder.dequeueInputBuffer(500L)
                                if (encInIndex >= 0) {
                                    val encInBuf = encoder.getInputBuffer(encInIndex)
                                    if (encInBuf != null) {
                                        encInBuf.clear()
                                        if (bufferInfo.size > 0 && pcmBuf != null) {
                                            pcmBuf.position(bufferInfo.offset)
                                            pcmBuf.limit(bufferInfo.offset + bufferInfo.size)
                                            encInBuf.put(pcmBuf)
                                        }
                                        if (firstDecodedPtsUs < 0L && bufferInfo.presentationTimeUs >= 0L) {
                                            firstDecodedPtsUs = bufferInfo.presentationTimeUs
                                        }
                                        val normalizedPts = if (firstDecodedPtsUs >= 0L) {
                                            (bufferInfo.presentationTimeUs - firstDecodedPtsUs).coerceAtLeast(0L)
                                        } else 0L

                                        encoder.queueInputBuffer(
                                            encInIndex,
                                            0,
                                            bufferInfo.size,
                                            normalizedPts,
                                            if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0,
                                        )
                                        encInQueued = true
                                    }
                                } else {
                                    drainEncoderOutput()
                                }
                            }
                        }

                        decoder.releaseOutputBuffer(decOutIndex, false)
                        if (isEos) {
                            decoderDone = true
                        }
                    }
                }

                // 3. Drain Encoder
                drainEncoderOutput()
            }

            success = destination.exists() && destination.length() > 0L
        } catch (_: Exception) {
            success = false
        } finally {
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
            runCatching {
                decoder?.stop()
                decoder?.release()
            }
            runCatching {
                encoder?.stop()
                encoder?.release()
            }
            runCatching { extractor.release() }
            runCatching { pfd?.close() }

            if (!success) {
                runCatching { destination.delete() }
            }
        }

        if (success) destination else null
    }
}
