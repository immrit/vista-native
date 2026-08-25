package ir.coffevista.vista_native.features.feed.ui.create

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimmerScreen(
    videoUri: Uri,
    maxDurationMs: Long = 60_000L, // 60s standard, 120s premium
    onBack: () -> Unit,
    onTrimComplete: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var startValueMs by remember { mutableFloatStateOf(0f) }
    var endValueMs by remember { mutableFloatStateOf(0f) }
    var isTrimming by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(videoUri) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L
                withContext(Dispatchers.Main) {
                    totalDurationMs = duration
                    val initialEnd = duration.coerceAtMost(maxDurationMs).toFloat()
                    startValueMs = 0f
                    endValueMs = initialEnd
                }
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    fun handleSave() {
        val startMs = startValueMs.toLong()
        val endMs = endValueMs.toLong()
        val durationMs = endMs - startMs
        if (durationMs <= 0) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("لطفاً بازه زمانی معتبری انتخاب کنید")
            }
            return
        }
        if (durationMs > maxDurationMs) {
            val maxSec = maxDurationMs / 1000
            coroutineScope.launch {
                snackbarHostState.showSnackbar("طول ویدیو نمی‌تواند بیش از $maxSec ثانیه باشد")
            }
            return
        }

        isTrimming = true
        exoPlayer.pause()
        coroutineScope.launch {
            try {
                val trimmedFile = withContext(Dispatchers.IO) {
                    trimVideoFast(context, videoUri, startMs, endMs)
                }
                isTrimming = false
                onTrimComplete(trimmedFile)
            } catch (e: Exception) {
                isTrimming = false
                snackbarHostState.showSnackbar(e.message ?: "خطا در برش ویدیو")
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "برش ویدیو",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    if (isTrimming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 12.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = ::handleSave) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تایید و برش",
                                tint = Color(0xFF4CAF50),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Video Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                )

                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "پخش",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            // Controls & Range Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(20.dp),
            ) {
                val selectedSec = ((endValueMs - startValueMs) / 1000f)
                val maxSec = (maxDurationMs / 1000f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "مدت زمان انتخابی: ${String.format("%.1f", selectedSec)} ثانیه",
                        color = if (selectedSec > maxSec) Color(0xFFFF5252) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "حداکثر: ${maxSec.toInt()} ثانیه",
                        color = Color.Gray,
                        fontSize = 13.sp,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (totalDurationMs > 0) {
                    RangeSlider(
                        value = startValueMs..endValueMs,
                        onValueChange = { range ->
                            val newStart = range.start
                            val newEnd = range.endInclusive
                            if (newEnd - newStart <= maxDurationMs) {
                                startValueMs = newStart
                                endValueMs = newEnd
                                exoPlayer.seekTo(newStart.toLong())
                            }
                        },
                        valueRange = 0f..totalDurationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.DarkGray,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatTime(startValueMs.toLong()),
                        color = Color.LightGray,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = formatTime(endValueMs.toLong()),
                        color = Color.LightGray,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

/**
 * Super fast MP4 trimmer using native Android MediaExtractor + MediaMuxer.
 * Clips tracks at keyframe boundaries without slow re-encoding.
 */
private fun trimVideoFast(
    context: Context,
    videoUri: Uri,
    startMs: Long,
    endMs: Long,
): File {
    val extractor = MediaExtractor()
    val fd = context.contentResolver.openFileDescriptor(videoUri, "r")
        ?: throw IllegalStateException("امکان باز کردن فایل ویدیو وجود ندارد")

    extractor.setDataSource(fd.fileDescriptor)
    val trackCount = extractor.trackCount

    val outputFile = File.createTempFile("trimmed-", ".mp4", context.cacheDir)
    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    val indexMap = HashMap<Int, Int>(trackCount)
    var bufferSize = 1024 * 1024

    for (i in 0 until trackCount) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
        if (mime.startsWith("video/") || mime.startsWith("audio/")) {
            extractor.selectTrack(i)
            val muxerTrackIndex = muxer.addTrack(format)
            indexMap[i] = muxerTrackIndex
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                val size = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                if (size > bufferSize) bufferSize = size
            }
        }
    }

    muxer.start()

    val startUs = startMs * 1000
    val endUs = endMs * 1000
    val buffer = ByteBuffer.allocate(bufferSize)
    val bufferInfo = MediaCodec.BufferInfo()

    try {
        for (i in 0 until trackCount) {
            val muxerTrack = indexMap[i] ?: continue
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.presentationTimeUs = extractor.sampleTime
                if (extractor.sampleTrackIndex == i) {
                    if (bufferInfo.presentationTimeUs > endUs) break
                    if (bufferInfo.presentationTimeUs >= startUs) {
                        bufferInfo.flags = extractor.sampleFlags
                        muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                    }
                }
                extractor.advance()
            }
        }
    } finally {
        runCatching { muxer.stop() }
        runCatching { muxer.release() }
        extractor.release()
        fd.close()
    }

    return outputFile
}
