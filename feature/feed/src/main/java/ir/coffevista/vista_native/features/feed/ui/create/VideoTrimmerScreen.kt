package ir.coffevista.vista_native.features.feed.ui.create

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.OptIn

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

    var probe by remember(videoUri) { mutableStateOf<VideoProbe?>(null) }
    var loadFailed by remember(videoUri) { mutableStateOf(false) }
    var clipMs by remember(videoUri) { mutableLongStateOf(0L) }
    var startMs by remember(videoUri) { mutableLongStateOf(0L) }
    var isTrimming by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPlayerReleased by remember(videoUri) { mutableStateOf(false) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(videoUri) {
        val result = probeVideo(context, videoUri)
        if (result == null) {
            loadFailed = true
        } else {
            clipMs = TrimMath.clampClipMs(result.durationMs, maxDurationMs, maxDurationMs)
            startMs = 0L
            probe = result
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

    // Loop only the selected window while previewing; follow it live while it is dragged.
    LaunchedEffect(isPlaying, probe) {
        while (isPlaying && probe != null && !isPlayerReleased) {
            delay(80L)
            if (isPlayerReleased) break
            runCatching {
                val position = exoPlayer.currentPosition
                if (position >= startMs + clipMs || position < startMs - 400L) {
                    exoPlayer.seekTo(startMs)
                }
            }
        }
    }
    LaunchedEffect(startMs, clipMs) {
        if (probe == null || isPlayerReleased) return@LaunchedEffect
        delay(60L)
        if (!isPlayerReleased) {
            runCatching { exoPlayer.seekTo(startMs) }
        }
    }

    BackHandler(enabled = isTrimming) { /* ignore back while the file is being written */ }

    fun handleSave() {
        if (isTrimming || isPlayerReleased) return
        val loaded = probe ?: return
        if (clipMs <= 0L) {
            coroutineScope.launch { snackbarHostState.showSnackbar("لطفاً بازه زمانی معتبری انتخاب کنید") }
            return
        }
        isTrimming = true
        runCatching { exoPlayer.pause() }
        val start = TrimMath.clampStartMs(startMs, loaded.durationMs, clipMs)
        coroutineScope.launch {
            try {
                val trimmedFile = trimVideo(context, videoUri, start, start + clipMs, maxDurationMs)
                isTrimming = false
                onTrimComplete(trimmedFile)
            } catch (e: CancellationException) {
                throw e
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
                    IconButton(onClick = onBack, enabled = !isTrimming) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = ::handleSave,
                        enabled = probe != null && !isTrimming,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp),
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
                            .clickable {
                                if (!isPlayerReleased) {
                                    runCatching {
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
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

                // Timeline + controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E))
                        .navigationBarsPadding()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val loaded = probe
                    when {
                        loadFailed -> Text(
                            text = "این ویدیو قابل خواندن نیست. لطفاً ویدیوی دیگری انتخاب کنید.",
                            color = Color(0xFFFF8A80),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        )

                        loaded == null -> Box(
                            modifier = Modifier.fillMaxWidth().height(96.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp) }

                        else -> TrimControls(
                            videoUri = videoUri,
                            probe = loaded,
                            maxDurationMs = maxDurationMs,
                            clipMs = clipMs,
                            startMs = startMs,
                            onClipChange = { newClip ->
                                clipMs = newClip
                                startMs = TrimMath.clampStartMs(startMs, loaded.durationMs, newClip)
                            },
                            onStartChange = { startMs = it },
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
private fun TrimControls(
    videoUri: Uri,
    probe: VideoProbe,
    maxDurationMs: Long,
    clipMs: Long,
    startMs: Long,
    onClipChange: (Long) -> Unit,
    onStartChange: (Long) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val total = probe.durationMs
    val lengthOptions = remember(total, maxDurationMs) { TrimMath.clipLengthOptions(total, maxDurationMs) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${TrimMath.formatTime(startMs)} – ${TrimMath.formatTime(startMs + clipMs)}",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
        Text(
            text = "${clipMs / 1000} ثانیه از ${TrimMath.formatTime(total)} (حداکثر ${maxDurationMs / 1000} ثانیه)",
            color = Color.LightGray,
            fontSize = 12.sp,
        )
    }

    // Thumbnails fill in as they are decoded; the count is capped so long videos stay cheap.
    val frames = remember(videoUri) { mutableStateMapOf<Int, androidx.compose.ui.graphics.ImageBitmap>() }
    val tileCount = remember(total) { (total / 2_000L).toInt().coerceIn(8, 40) }
    LaunchedEffect(videoUri, tileCount) {
        loadVideoThumbnails(
            context = context,
            uri = videoUri,
            durationMs = total,
            count = tileCount,
            targetHeightPx = with(density) { 76.dp.roundToPx() },
        ) { index, bitmap -> frames[index] = bitmap.asImageBitmap() }
    }

    TrimScrubber(
        durationMs = total,
        clipMs = clipMs,
        initialStartMs = startMs,
        scaleReferenceMs = maxDurationMs,
        onStartChanged = onStartChange,
        snapStart = { candidate ->
            if (probe.keyframesMs.isEmpty()) {
                candidate
            } else {
                TrimMath.snapToKeyframe(probe.keyframesMs, candidate, TrimMath.maxStartMs(total, clipMs))
            }
        },
    ) { contentWidth, contentHeight ->
        Row(
            modifier = Modifier
                .width(contentWidth)
                .height(contentHeight)
                .background(Color(0xFF2A2A2A)),
        ) {
            val tileWidth = contentWidth / tileCount
            repeat(tileCount) { index ->
                val frame = frames[index]
                Box(modifier = Modifier.width(tileWidth).fillMaxHeight()) {
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
    }

    Text(
        text = "نوار را بکشید تا بخش دلخواه زیر کادر قرار بگیرد",
        color = Color.Gray,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    if (lengthOptions.size > 1) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            lengthOptions.forEach { option ->
                FilterChip(
                    selected = clipMs == option,
                    onClick = { onClipChange(option) },
                    label = { Text("${option / 1000} ثانیه", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = Color.White,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}
