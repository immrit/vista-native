package ir.coffevista.vista_native.features.feed.ui.create

import android.net.Uri
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import ir.coffevista.vista_native.core.designsystem.component.rememberVistaHaptics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.OptIn
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private enum class TrimmerDragMode {
    NONE,
    START,
    END,
    WINDOW,
    PLAYHEAD,
}

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
    val haptics = rememberVistaHaptics()

    var probe by remember(videoUri) { mutableStateOf<VideoProbe?>(null) }
    var loadFailed by remember(videoUri) { mutableStateOf(false) }
    var startMs by remember(videoUri) { mutableLongStateOf(0L) }
    var endMs by remember(videoUri) { mutableLongStateOf(0L) }
    var playheadMs by remember(videoUri) { mutableLongStateOf(0L) }
    var isDragging by remember(videoUri) { mutableStateOf(false) }
    var resumeAfterDrag by remember(videoUri) { mutableStateOf(false) }
    var isMuted by remember(videoUri) { mutableStateOf(false) }
    var isTrimming by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPlayerReleased by remember(videoUri) { mutableStateOf(false) }
    var lastScrubSeekMs by remember(videoUri) { mutableLongStateOf(-1L) }
    var lastScrubWallMs by remember(videoUri) { mutableLongStateOf(0L) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            setSeekParameters(SeekParameters.EXACT)
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(videoUri) {
        val result = probeVideo(context, videoUri)
        if (result == null) {
            loadFailed = true
        } else {
            val (initStart, initEnd) = TrimMath.clampRange(
                startMs = 0L,
                endMs = result.durationMs,
                durationMs = result.durationMs,
                minLenMs = TrimMath.MIN_VIDEO_TRIM_MS,
                maxLenMs = maxDurationMs,
                anchorStart = true,
            )
            startMs = initStart
            endMs = initEnd
            playheadMs = initStart
            probe = result
        }
    }

    LaunchedEffect(exoPlayer, isMuted) {
        if (!isPlayerReleased) {
            runCatching { exoPlayer.volume = if (isMuted) 0f else 1f }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isPlaying = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            isPlayerReleased = true
            exoPlayer.removeListener(listener)
            runCatching {
                exoPlayer.stop()
                exoPlayer.release()
            }
        }
    }

    val latestStartMs by rememberUpdatedState(startMs)
    val latestEndMs by rememberUpdatedState(endMs)

    // Smooth playhead tracking and [startMs, endMs] playback loop when not dragging.
    LaunchedEffect(probe, isPlaying, isDragging) {
        if (probe == null || isDragging || isPlayerReleased) return@LaunchedEffect
        while (!isPlayerReleased) {
            val s = latestStartMs
            val e = latestEndMs
            runCatching {
                val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                playheadMs = position.coerceIn(s, maxOf(s, e))
                if (isPlaying && (position >= e || position < s - 350L)) {
                    exoPlayer.seekTo(s)
                    playheadMs = s
                }
            }
            if (!isPlaying) break
            delay(33L)
        }
    }

    BackHandler(enabled = isTrimming) { /* ignore back while the file is being written */ }

    fun handleSave() {
        if (isTrimming || isPlayerReleased) return
        val loaded = probe ?: return
        val (safeStart, safeEnd) = TrimMath.clampRange(
            startMs = startMs,
            endMs = endMs,
            durationMs = loaded.durationMs,
            minLenMs = TrimMath.MIN_VIDEO_TRIM_MS,
            maxLenMs = maxDurationMs,
            anchorStart = true,
        )
        if (safeEnd <= safeStart) {
            coroutineScope.launch { snackbarHostState.showSnackbar("لطفاً بازه زمانی معتبری انتخاب کنید") }
            return
        }
        haptics.confirm()
        isTrimming = true
        runCatching {
            exoPlayer.pause()
            exoPlayer.stop()
        }
        coroutineScope.launch {
            try {
                val trimmedFile = trimVideo(context, videoUri, safeStart, safeEnd, maxDurationMs)
                isTrimming = false
                onTrimComplete(trimmedFile)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isTrimming = false
                if (!isPlayerReleased) {
                    runCatching { exoPlayer.prepare() }
                }
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
                    IconButton(
                        onClick = {
                            haptics.tap()
                            onBack()
                        },
                        enabled = !isTrimming,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptics.tap()
                            isMuted = !isMuted
                        },
                        enabled = !isTrimming,
                    ) {
                        Icon(
                            imageVector = if (isMuted) {
                                Icons.AutoMirrored.Filled.VolumeOff
                            } else {
                                Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = if (isMuted) "وصل صدا" else "بی‌صدا",
                            tint = Color.White,
                        )
                    }
                    Button(
                        onClick = ::handleSave,
                        enabled = probe != null && !isTrimming,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.padding(end = 12.dp),
                    ) { Text("تایید", fontWeight = FontWeight.Bold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
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
                        update = { playerView ->
                            if (playerView.player != exoPlayer && !isPlayerReleased) {
                                playerView.player = exoPlayer
                            }
                        },
                        onRelease = { playerView ->
                            playerView.player = null
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (!isPlayerReleased) {
                                    haptics.tap()
                                    runCatching {
                                        if (exoPlayer.isPlaying) {
                                            exoPlayer.pause()
                                        } else {
                                            val currentPos = exoPlayer.currentPosition
                                            if (currentPos < startMs || currentPos >= endMs) {
                                                exoPlayer.seekTo(startMs)
                                                playheadMs = startMs
                                            }
                                            exoPlayer.play()
                                        }
                                    }
                                }
                            },
                    )

                    if (!isPlaying && probe != null) {
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

                // Timeline + handles + range readout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141414))
                        .navigationBarsPadding()
                        .padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val loaded = probe
                    when {
                        loadFailed -> Text(
                            text = "این ویدیو قابل خواندن نیست. لطفاً ویدیوی دیگری انتخاب کنید.",
                            color = Color(0xFFFF8A80),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        )

                        loaded == null -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp) }

                        else -> InteractiveVideoTrimControls(
                            videoUri = videoUri,
                            probe = loaded,
                            maxDurationMs = maxDurationMs,
                            startMs = startMs,
                            endMs = endMs,
                            playheadMs = playheadMs,
                            onDragStart = {
                                resumeAfterDrag = isPlaying
                                isDragging = true
                                lastScrubSeekMs = -1L
                                lastScrubWallMs = 0L
                                if (!isPlayerReleased) {
                                    runCatching {
                                        exoPlayer.pause()
                                        exoPlayer.setSeekParameters(SeekParameters.CLOSEST_SYNC)
                                    }
                                }
                            },
                            onRangeScrub = { newStart, newEnd, previewMs ->
                                startMs = newStart
                                endMs = newEnd
                                playheadMs = previewMs.coerceIn(newStart, maxOf(newStart, newEnd))
                                val nowWall = SystemClock.elapsedRealtime()
                                if (!isPlayerReleased &&
                                    (nowWall - lastScrubWallMs >= 45L || abs(previewMs - lastScrubSeekMs) >= 400L)
                                ) {
                                    lastScrubWallMs = nowWall
                                    lastScrubSeekMs = previewMs
                                    runCatching { exoPlayer.seekTo(previewMs) }
                                }
                            },
                            onPlayheadScrub = { targetMs ->
                                val clamped = targetMs.coerceIn(startMs, maxOf(startMs, endMs))
                                playheadMs = clamped
                                val nowWall = SystemClock.elapsedRealtime()
                                if (!isPlayerReleased &&
                                    (nowWall - lastScrubWallMs >= 45L || abs(clamped - lastScrubSeekMs) >= 300L)
                                ) {
                                    lastScrubWallMs = nowWall
                                    lastScrubSeekMs = clamped
                                    runCatching { exoPlayer.seekTo(clamped) }
                                }
                            },
                            onDragEnd = { snapTargetMs ->
                                val snapped = snapTargetMs.coerceIn(startMs, maxOf(startMs, endMs))
                                playheadMs = snapped
                                if (!isPlayerReleased) {
                                    runCatching {
                                        exoPlayer.setSeekParameters(SeekParameters.EXACT)
                                        exoPlayer.seekTo(snapped)
                                        if (resumeAfterDrag) {
                                            exoPlayer.play()
                                        }
                                    }
                                }
                                isDragging = false
                            },
                            onTapSeek = { targetMs ->
                                val clamped = targetMs.coerceIn(startMs, maxOf(startMs, endMs))
                                playheadMs = clamped
                                if (!isPlayerReleased) {
                                    runCatching {
                                        exoPlayer.setSeekParameters(SeekParameters.EXACT)
                                        exoPlayer.seekTo(clamped)
                                    }
                                }
                            },
                        )
                    }
                }
            }

            if (isTrimming) {
                // Blocks every touch while the file is being written (no double submit).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .pointerInput(Unit) {},
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Text("در حال برش ویدیو…", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveVideoTrimControls(
    videoUri: Uri,
    probe: VideoProbe,
    maxDurationMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    onDragStart: () -> Unit,
    onRangeScrub: (startMs: Long, endMs: Long, previewMs: Long) -> Unit,
    onPlayheadScrub: (targetMs: Long) -> Unit,
    onDragEnd: (snapTargetMs: Long) -> Unit,
    onTapSeek: (targetMs: Long) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val total = probe.durationMs
    val selectedMs = (endMs - startMs).coerceAtLeast(0L)

    // Thumbnails fill in progressively; 12 tiles gives clean filmstrip coverage across any aspect ratio.
    val frames = remember(videoUri) { mutableStateMapOf<Int, ImageBitmap>() }
    val tileCount = remember(total) {
        if (total < 12_000L) 8 else 12
    }
    LaunchedEffect(videoUri, tileCount) {
        loadVideoThumbnails(
            context = context,
            uri = videoUri,
            durationMs = total,
            count = tileCount,
            targetHeightPx = with(density) { 64.dp.roundToPx() },
        ) { index, bitmap -> frames[index] = bitmap.asImageBitmap() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = TrimMath.formatTime(startMs),
            color = Color(0xFFB0BEC5),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        ) {
            Text(
                text = "بازه انتخابی: ${TrimMath.formatTime(selectedMs)}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(
            text = TrimMath.formatTime(endMs),
            color = Color(0xFFB0BEC5),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }

    VideoRangeTrimmerBar(
        durationMs = total,
        maxDurationMs = maxDurationMs,
        startMs = startMs,
        endMs = endMs,
        playheadMs = playheadMs,
        tileCount = tileCount,
        frames = frames,
        onDragStart = onDragStart,
        onRangeScrub = onRangeScrub,
        onPlayheadScrub = onPlayheadScrub,
        onDragEnd = onDragEnd,
        onTapSeek = onTapSeek,
    )

    Text(
        text = "دستگیره‌های ابتدا و انتها یا کادر میانی را بکشید (حداکثر ${maxDurationMs / 1000} ثانیه)",
        color = Color(0xFF8A94A6),
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

@Composable
private fun VideoRangeTrimmerBar(
    durationMs: Long,
    maxDurationMs: Long,
    startMs: Long,
    endMs: Long,
    playheadMs: Long,
    tileCount: Int,
    frames: Map<Int, ImageBitmap>,
    onDragStart: () -> Unit,
    onRangeScrub: (startMs: Long, endMs: Long, previewMs: Long) -> Unit,
    onPlayheadScrub: (targetMs: Long) -> Unit,
    onDragEnd: (snapTargetMs: Long) -> Unit,
    onTapSeek: (targetMs: Long) -> Unit,
) {
    val density = LocalDensity.current
    val haptics = rememberVistaHaptics()
    val handleWidthDp = 16.dp
    val handleWidthPx = with(density) { handleWidthDp.toPx() }
    val touchSlopZonePx = with(density) { 28.dp.toPx() }
    val accentColor = MaterialTheme.colorScheme.primary

    var barWidthPx by remember { mutableIntStateOf(0) }
    val latestStartMs by rememberUpdatedState(startMs)
    val latestEndMs by rememberUpdatedState(endMs)
    val latestPlayheadMs by rememberUpdatedState(playheadMs)
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnRangeScrub by rememberUpdatedState(onRangeScrub)
    val latestOnPlayheadScrub by rememberUpdatedState(onPlayheadScrub)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val latestOnTapSeek by rememberUpdatedState(onTapSeek)

    val trackWidthPx = (barWidthPx - handleWidthPx * 2f).coerceAtLeast(1f)
    val startFraction = if (durationMs > 0L) (startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val endFraction = if (durationMs > 0L) (endMs.toFloat() / durationMs.toFloat()).coerceIn(startFraction, 1f) else 1f
    val playheadFraction = if (durationMs > 0L) (playheadMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val startXPx = handleWidthPx + startFraction * trackWidthPx
    val endXPx = handleWidthPx + endFraction * trackWidthPx
    val playheadXPx = handleWidthPx + playheadFraction * trackWidthPx

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(68.dp)
                .onSizeChanged { barWidthPx = it.width }
                .pointerInput(durationMs, maxDurationMs) {
                    detectTapGestures { offset ->
                        val width = (barWidthPx - handleWidthPx * 2f).coerceAtLeast(1f)
                        if (durationMs <= 0L) return@detectTapGestures
                        val fraction = ((offset.x - handleWidthPx) / width).coerceIn(0f, 1f)
                        val tappedMs = (fraction * durationMs).roundToLong()
                        haptics.tap()
                        latestOnTapSeek(tappedMs)
                    }
                }
                .pointerInput(durationMs, maxDurationMs) {
                    var dragMode = TrimmerDragMode.NONE
                    var originStart = 0L
                    var originEnd = 0L
                    var originPlayhead = 0L
                    var accumulatedPx = 0f

                    detectDragGestures(
                        onDragStart = { offset ->
                            val trackW = (barWidthPx - handleWidthPx * 2f).coerceAtLeast(1f)
                            if (durationMs <= 0L) return@detectDragGestures
                            originStart = latestStartMs
                            originEnd = latestEndMs
                            originPlayhead = latestPlayheadMs
                            accumulatedPx = 0f

                            val sX = handleWidthPx + (originStart.toFloat() / durationMs) * trackW
                            val eX = handleWidthPx + (originEnd.toFloat() / durationMs) * trackW
                            val startCenter = sX - handleWidthPx * 0.5f
                            val endCenter = eX + handleWidthPx * 0.5f
                            val dStart = abs(offset.x - startCenter)
                            val dEnd = abs(offset.x - endCenter)

                            dragMode = when {
                                dStart <= touchSlopZonePx && dStart <= dEnd -> TrimmerDragMode.START
                                dEnd <= touchSlopZonePx -> TrimmerDragMode.END
                                offset.x >= sX && offset.x <= eX -> {
                                    if (durationMs - (originEnd - originStart) > 200L) {
                                        TrimmerDragMode.WINDOW
                                    } else {
                                        TrimmerDragMode.PLAYHEAD
                                    }
                                }
                                else -> TrimmerDragMode.PLAYHEAD
                            }
                            if (dragMode != TrimmerDragMode.NONE) {
                                haptics.tap()
                                latestOnDragStart()
                            }
                        },
                        onDragEnd = {
                            if (dragMode != TrimmerDragMode.NONE) {
                                haptics.tap()
                                val target = if (dragMode == TrimmerDragMode.PLAYHEAD) {
                                    latestPlayheadMs
                                } else {
                                    latestStartMs
                                }
                                latestOnDragEnd(target)
                            }
                            dragMode = TrimmerDragMode.NONE
                        },
                        onDragCancel = {
                            if (dragMode != TrimmerDragMode.NONE) {
                                val target = if (dragMode == TrimmerDragMode.PLAYHEAD) {
                                    latestPlayheadMs
                                } else {
                                    latestStartMs
                                }
                                latestOnDragEnd(target)
                            }
                            dragMode = TrimmerDragMode.NONE
                        },
                        onDrag = { change, dragAmount ->
                            if (dragMode == TrimmerDragMode.NONE || durationMs <= 0L) return@detectDragGestures
                            change.consume()
                            val trackW = (barWidthPx - handleWidthPx * 2f).coerceAtLeast(1f)
                            accumulatedPx += dragAmount.x
                            val deltaMs = ((accumulatedPx / trackW) * durationMs).roundToLong()

                            when (dragMode) {
                                TrimmerDragMode.START -> {
                                    val (s, e) = TrimMath.clampRange(
                                        startMs = originStart + deltaMs,
                                        endMs = originEnd,
                                        durationMs = durationMs,
                                        minLenMs = TrimMath.MIN_VIDEO_TRIM_MS,
                                        maxLenMs = maxDurationMs,
                                        anchorStart = false,
                                    )
                                    latestOnRangeScrub(s, e, s)
                                }

                                TrimmerDragMode.END -> {
                                    val (s, e) = TrimMath.clampRange(
                                        startMs = originStart,
                                        endMs = originEnd + deltaMs,
                                        durationMs = durationMs,
                                        minLenMs = TrimMath.MIN_VIDEO_TRIM_MS,
                                        maxLenMs = maxDurationMs,
                                        anchorStart = true,
                                    )
                                    latestOnRangeScrub(s, e, e)
                                }

                                TrimmerDragMode.WINDOW -> {
                                    val (s, e) = TrimMath.slideRange(
                                        startMs = originStart,
                                        endMs = originEnd,
                                        deltaMs = deltaMs,
                                        durationMs = durationMs,
                                    )
                                    latestOnRangeScrub(s, e, s)
                                }

                                TrimmerDragMode.PLAYHEAD -> {
                                    val target = (originPlayhead + deltaMs).coerceIn(originStart, originEnd)
                                    latestOnPlayheadScrub(target)
                                }

                                TrimmerDragMode.NONE -> Unit
                            }
                        },
                    )
                },
        ) {
            // Filmstrip track inset by handle width on both sides
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = handleWidthDp, vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF262626)),
            ) {
                repeat(tileCount) { index ->
                    val frame = frames[index]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        if (frame != null) {
                            Image(
                                bitmap = frame,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            if (barWidthPx > 0) {
                val trackRightPx = barWidthPx - handleWidthPx

                // Left darkened scrim outside selection
                val leftScrimW = (startXPx - handleWidthPx).coerceAtLeast(0f)
                if (leftScrimW > 0f) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(handleWidthPx.roundToInt(), with(density) { 4.dp.roundToPx() }) }
                            .width(with(density) { leftScrimW.toDp() })
                            .fillMaxHeight()
                            .padding(bottom = 8.dp)
                            .background(Color.Black.copy(alpha = 0.65f)),
                    )
                }

                // Right darkened scrim outside selection
                val rightScrimW = (trackRightPx - endXPx).coerceAtLeast(0f)
                if (rightScrimW > 0f) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(endXPx.roundToInt(), with(density) { 4.dp.roundToPx() }) }
                            .width(with(density) { rightScrimW.toDp() })
                            .fillMaxHeight()
                            .padding(bottom = 8.dp)
                            .background(Color.Black.copy(alpha = 0.65f)),
                    )
                }

                // Active window top and bottom highlight frame
                val windowW = (endXPx - startXPx).coerceAtLeast(0f)
                if (windowW > 0f) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(startXPx.roundToInt(), with(density) { 4.dp.roundToPx() }) }
                            .width(with(density) { windowW.toDp() })
                            .height(3.dp)
                            .background(accentColor),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset { IntOffset(startXPx.roundToInt(), with(density) { (-4).dp.roundToPx() }) }
                            .width(with(density) { windowW.toDp() })
                            .height(3.dp)
                            .background(accentColor),
                    )
                }

                // Start handle pill
                Box(
                    modifier = Modifier
                        .offset { IntOffset((startXPx - handleWidthPx).roundToInt(), with(density) { 4.dp.roundToPx() }) }
                        .width(handleWidthDp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(22.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }

                // End handle pill
                Box(
                    modifier = Modifier
                        .offset { IntOffset(endXPx.roundToInt(), with(density) { 4.dp.roundToPx() }) }
                        .width(handleWidthDp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(22.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }

                // Vertical white playhead indicator
                Box(
                    modifier = Modifier
                        .offset { IntOffset(playheadXPx.roundToInt(), 0) }
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(0.5.dp, Color.Black.copy(alpha = 0.5f), CircleShape),
                )
            }
        }
    }
}
