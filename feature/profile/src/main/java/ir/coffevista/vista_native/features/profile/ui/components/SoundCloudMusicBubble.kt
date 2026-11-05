package ir.coffevista.vista_native.features.profile.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import ir.coffevista.vista_native.core.designsystem.component.VistaSoundCloudWaveform
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Resolves optional start/end ms from musicUrl fragment (#t=start,end) or query params.
 */
fun resolveMusicRange(musicUrl: String): Pair<Long, Long?> {
    val fragment = musicUrl.substringAfter('#', "").trim()
    if (fragment.startsWith("t=")) {
        val parts = fragment.removePrefix("t=").split(',')
        val start = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val end = parts.getOrNull(1)?.toLongOrNull()
        return Pair(start, end)
    }
    val uri = runCatching { Uri.parse(musicUrl) }.getOrNull()
    val startParam = uri?.getQueryParameter("startMs")?.toLongOrNull()
        ?: uri?.getQueryParameter("t")?.substringBefore(',')?.toLongOrNull()
        ?: 0L
    val endParam = uri?.getQueryParameter("endMs")?.toLongOrNull()
        ?: uri?.getQueryParameter("t")?.substringAfter(',', "")?.toLongOrNull()
    return Pair(startParam, endParam)
}

/**
 * Minimalist, elegant audio player bubble for profile posts.
 * Includes interactive waveform scrubbing with fluid horizontal fill,
 * clean typography, and lazy ExoPlayer initialization.
 */
@Composable
fun SoundCloudMusicBubble(
    musicUrl: String,
    musicTitle: String? = null,
    modifier: Modifier = Modifier,
    subtitle: String = "موسیقی پست",
) {
    val cleanUrl = musicUrl.trim()
    if (cleanUrl.isEmpty()) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isPlaying by remember(cleanUrl) { mutableStateOf(false) }
    var isBuffering by remember(cleanUrl) { mutableStateOf(false) }
    var currentPositionMs by remember(cleanUrl) { mutableLongStateOf(0L) }
    var durationMs by remember(cleanUrl) { mutableLongStateOf(0L) }
    var isScrubbing by remember(cleanUrl) { mutableStateOf(false) }
    var scrubFraction by remember(cleanUrl) { mutableFloatStateOf(0f) }

    var player by remember(cleanUrl) { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(cleanUrl) {
        onDispose {
            player?.release()
            player = null
        }
    }

    // High-resolution polling for silky smooth continuous fill
    LaunchedEffect(isPlaying, isScrubbing) {
        while (isPlaying && !isScrubbing && isActive) {
            player?.let { p ->
                currentPositionMs = p.currentPosition.coerceAtLeast(0L)
                val dur = p.duration
                if (dur > 0L) {
                    durationMs = dur
                }
            }
            delay(25L)
        }
    }

    fun obtainPlayer(): ExoPlayer {
        return player ?: run {
            val (startMs, endMs) = resolveMusicRange(cleanUrl)
            val cleanUri = Uri.parse(cleanUrl.substringBefore('#'))
            val baseItem = MediaItem.fromUri(cleanUri)
            val finalItem = if (startMs > 0 || (endMs != null && endMs > startMs)) {
                val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.coerceAtLeast(0L))
                if (endMs != null && endMs > startMs) {
                    clipBuilder.setEndPositionMs(endMs)
                }
                baseItem.buildUpon()
                    .setClippingConfiguration(clipBuilder.build())
                    .build()
            } else {
                baseItem
            }

            ExoPlayer.Builder(context).build().apply {
                setMediaItem(finalItem)
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = (playbackState == Player.STATE_BUFFERING)
                        if (playbackState == Player.STATE_READY) {
                            if (duration > 0L) {
                                durationMs = duration
                            }
                        } else if (playbackState == Player.STATE_ENDED) {
                            isPlaying = false
                            currentPositionMs = 0L
                            seekTo(0L)
                        }
                    }
                })
                prepare()
            }.also { player = it }
        }
    }

    fun togglePlayPause() {
        val p = obtainPlayer()
        if (p.isPlaying) {
            p.pause()
        } else {
            p.play()
        }
    }

    fun onWaveformSeek(fraction: Float) {
        scrubFraction = fraction
        isScrubbing = true
        val p = obtainPlayer()
        val targetMs = (fraction * (if (durationMs > 0L) durationMs else 30000L)).toLong()
        currentPositionMs = targetMs
        p.seekTo(targetMs)
        coroutineScope.launch {
            delay(100L)
            isScrubbing = false
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bubbleBg = if (isDark) {
        Color(0xFF141722).copy(alpha = 0.90f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
    }
    val cardBorder = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    val effectiveTitle = musicTitle?.takeIf(String::isNotBlank) ?: "موسیقی پست"
    val seed = remember(cleanUrl, effectiveTitle) {
        (cleanUrl.hashCode().toLong() shl 16) xor effectiveTitle.hashCode().toLong()
    }

    val progress = if (isScrubbing) {
        scrubFraction
    } else if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(width = 0.8.dp, color = cardBorder, shape = RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = bubbleBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // Header: Minimal Play/Pause button + Track Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sleek Minimal Play Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "توقف موسیقی" else "پخش موسیقی",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = effectiveTitle,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Minimalist Waveform with continuous, fluid horizontal fill
            VistaSoundCloudWaveform(
                progress = progress,
                onSeek = { frac -> onWaveformSeek(frac) },
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                seed = seed,
                barCount = 42,
                waveformHeight = 32.dp,
                activeBrush = SolidColor(MaterialTheme.colorScheme.primary),
                inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                showTimeLabels = true,
            )
        }
    }
}
