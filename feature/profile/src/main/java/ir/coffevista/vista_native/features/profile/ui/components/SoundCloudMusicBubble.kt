package ir.coffevista.vista_native.features.profile.ui.components

import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import ir.coffevista.vista_native.core.designsystem.component.VistaMusicPlayerCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Resolves optional start/end ms from musicUrl fragment (#t=start,end) or query params.
 */
fun resolveMusicRange(musicUrl: String): Pair<Long, Long?> =
    ir.coffevista.vista_native.core.designsystem.component.resolveMusicRange(musicUrl)

/**
 * Minimalist, elegant audio player bubble for profile posts.
 * Includes interactive waveform scrubbing with fluid horizontal fill,
 * clean typography, and lazy ExoPlayer initialization.
 *
 * Uses the unified [VistaMusicPlayerCard] design system component.
 */
@Composable
fun SoundCloudMusicBubble(
    musicUrl: String,
    musicTitle: String? = null,
    modifier: Modifier = Modifier,
    subtitle: String = "موسیقی پست",
    trailingAction: (@Composable () -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
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

    val effectiveTitle = musicTitle?.takeIf(String::isNotBlank) ?: "موسیقی پست"
    val seed = remember(cleanUrl, effectiveTitle) {
        (cleanUrl.hashCode().toLong() shl 16) xor effectiveTitle.hashCode().toLong()
    }

    val progress = if (isScrubbing) {
        scrubFraction
    } else if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    VistaMusicPlayerCard(
        title = effectiveTitle,
        subtitle = subtitle,
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        progress = progress,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        seed = seed,
        onPlayPauseClick = ::togglePlayPause,
        onSeek = ::onWaveformSeek,
        trailingAction = trailingAction,
        onCardClick = onCardClick,
        modifier = modifier,
    )
}
