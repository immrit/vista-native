package ir.coffevista.vista_native.features.feed.ui.create

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.snapshotFlow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Music trimming sheet. The user simply drags the waveform sideways under a fixed highlighted
 * window; the preview loops exactly the highlighted part.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicTrimSheet(
    title: String,
    musicUri: Uri,
    durationMs: Int,
    initialStartMs: Int,
    initialEndMs: Int,
    maxClipMs: Int,
    isBackgroundMode: Boolean,
    onBackgroundModeChange: (Boolean) -> Unit,
    onConfirm: (startMs: Int, endMs: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val totalMs = durationMs.toLong().takeIf { it > 0L } ?: 30_000L
    val maxClip = maxClipMs.toLong().coerceAtLeast(TrimMath.MIN_CLIP_MS)

    var clipMs by remember(musicUri) {
        val requested = (initialEndMs - initialStartMs).toLong().takeIf { it > 0L } ?: maxClip
        mutableLongStateOf(TrimMath.clampClipMs(totalMs, requested, maxClip))
    }
    var startMs by remember(musicUri) {
        mutableLongStateOf(TrimMath.clampStartMs(initialStartMs.toLong(), totalMs, clipMs))
    }
    var isPlaying by remember { mutableStateOf(false) }
    var isPlayerReleased by remember(musicUri) { mutableStateOf(false) }
    val lengthOptions = remember(totalMs, maxClip) { TrimMath.clipLengthOptions(totalMs, maxClip) }

    val player = remember(musicUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(musicUri))
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isPlaying = false
            }
        }
        player.addListener(listener)
        onDispose {
            isPlayerReleased = true
            player.removeListener(listener)
            runCatching {
                player.stop()
                player.release()
            }
        }
    }

    // Loop only the highlighted part while previewing.
    LaunchedEffect(isPlaying) {
        while (isPlaying && !isPlayerReleased) {
            delay(80L)
            if (isPlayerReleased) break
            runCatching {
                val position = player.currentPosition
                if (position >= startMs + clipMs || position < startMs - 400L) {
                    player.seekTo(startMs)
                }
            }
        }
    }

    // Follow the window live while the user drags it during playback.
    LaunchedEffect(player) {
        snapshotFlow { startMs to clipMs }
            .drop(1)
            .collectLatest { (start, _) ->
                delay(100L)
                if (!isPlayerReleased) {
                    runCatching {
                        if (player.isPlaying) player.seekTo(start)
                    }
                }
            }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isPlayerReleased) runCatching { player.pause() }
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("برش موسیقی", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledIconButton(
                    onClick = {
                        if (!isPlayerReleased) {
                            runCatching {
                                if (isPlaying) {
                                    player.pause()
                                } else {
                                    player.seekTo(startMs)
                                    player.play()
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "توقف پیش‌نمایش" else "پخش پیش‌نمایش",
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${TrimMath.formatTime(startMs)} – ${TrimMath.formatTime(startMs + clipMs)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${clipMs / 1000} ثانیه از ${TrimMath.formatTime(totalMs)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val seed = remember(musicUri) { (musicUri.toString().hashCode() and 0xFFFF) / 1000f }
            TrimScrubber(
                durationMs = totalMs,
                clipMs = clipMs,
                initialStartMs = startMs,
                scaleReferenceMs = maxClip,
                onStartChanged = { startMs = it },
                height = 84.dp,
            ) { contentWidth, contentHeight ->
                Canvas(
                    modifier = Modifier
                        .size(contentWidth, contentHeight)
                        .background(trackColor),
                ) {
                    val step = 4.dp.toPx()
                    val barWidth = 2.5.dp.toPx()
                    val count = (size.width / step).toInt()
                    for (i in 0 until count) {
                        val amplitude = waveformAmplitude(i, seed)
                        val barHeight = size.height * (0.14f + 0.78f * amplitude)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(i * step, (size.height - barHeight) / 2f),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f),
                        )
                    }
                }
            }

            Text(
                text = "موج را بکشید تا بخش دلخواه زیر کادر قرار بگیرد",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            if (lengthOptions.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    lengthOptions.forEach { option ->
                        FilterChip(
                            selected = clipMs == option,
                            onClick = {
                                clipMs = option
                                startMs = TrimMath.clampStartMs(startMs, totalMs, option)
                            },
                            label = { Text("${option / 1000} ثانیه", fontSize = 12.sp) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("پخش به صورت موزیک پس‌زمینه", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = isBackgroundMode,
                    onCheckedChange = onBackgroundModeChange,
                )
            }

            Button(
                onClick = {
                    if (!isPlayerReleased) runCatching { player.pause() }
                    onConfirm(startMs.toInt(), (startMs + clipMs).toInt())
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("تایید برش") }
            TextButton(
                onClick = {
                    if (!isPlayerReleased) runCatching { player.pause() }
                    onRemove()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("حذف موسیقی") }
        }
    }
}

/** Deterministic, smooth pseudo-waveform in `0..1` so the timeline has a recognisable texture. */
private fun waveformAmplitude(index: Int, seed: Float): Float {
    val smooth = abs(sin(index * 0.19f + seed) * cos(index * 0.047f + seed * 1.7f))
    val jitterHash = (index * 2654435761L + (seed * 1000).toLong()) and 0xFF
    val jitter = jitterHash / 255f
    return (0.65f * smooth + 0.35f * jitter).coerceIn(0f, 1f)
}
