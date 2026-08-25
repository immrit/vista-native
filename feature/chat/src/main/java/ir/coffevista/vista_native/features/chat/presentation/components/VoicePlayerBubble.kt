package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun VoicePlayerBubble(
    modifier: Modifier = Modifier,
    audioUrl: String,
    durationSeconds: Int,
    isOutgoing: Boolean,
) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var isPlaying by remember { mutableStateOf(false) }
    var isPreparing by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0) }
    var totalDurationMs by remember { mutableStateOf(durationSeconds * 1000) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioFocusRequest by remember { mutableStateOf<AudioFocusRequest?>(null) }

    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        audioFocusRequest = null
    }

    fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange != AudioManager.AUDIOFOCUS_GAIN) {
                        runCatching { mediaPlayer?.pause() }
                        isPlaying = false
                    }
                }
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    DisposableEffect(audioUrl) {
        onDispose {
            mediaPlayer?.runCatching {
                if (isPlaying) stop()
                release()
            }
            abandonAudioFocus()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying && mediaPlayer != null) {
            val pos = runCatching { mediaPlayer?.currentPosition ?: 0 }.getOrDefault(0)
            currentPositionMs = pos
            delay(100)
        }
    }

    fun togglePlay() {
        if (isPreparing) return
        if (mediaPlayer == null) {
            val player = MediaPlayer()
            try {
                player.setDataSource(audioUrl)
                player.setOnCompletionListener {
                    isPlaying = false
                    currentPositionMs = 0
                    abandonAudioFocus()
                }
                player.setOnPreparedListener { preparedPlayer ->
                    totalDurationMs = preparedPlayer.duration.coerceAtLeast(durationSeconds * 1000)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        preparedPlayer.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
                    }
                    if (requestAudioFocus()) {
                        preparedPlayer.start()
                        isPlaying = true
                    }
                    isPreparing = false
                }
                player.setOnErrorListener { failedPlayer, _, _ ->
                    failedPlayer.release()
                    if (mediaPlayer === failedPlayer) mediaPlayer = null
                    isPreparing = false
                    isPlaying = false
                    abandonAudioFocus()
                    true
                }
                mediaPlayer = player
                isPreparing = true
                player.prepareAsync()
            } catch (e: Exception) {
                player.release()
                mediaPlayer = null
                isPreparing = false
                isPlaying = false
            }
        } else {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    isPlaying = false
                    abandonAudioFocus()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        player.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
                    }
                    if (requestAudioFocus()) {
                        player.start()
                        isPlaying = true
                    }
                }
            }
        }
    }

    fun toggleSpeed() {
        val nextSpeed = when (playbackSpeed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        playbackSpeed = nextSpeed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
            runCatching {
                mediaPlayer?.playbackParams = PlaybackParams().setSpeed(nextSpeed)
            }
        }
    }

    val progress = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val activeColor = if (isOutgoing) Color.White else VistaBrandColors.VioletDeep
    val inactiveColor = if (isOutgoing) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    Row(
        modifier = modifier
            .widthIn(min = 210.dp, max = 260.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause Circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isOutgoing) Color.White.copy(alpha = 0.25f) else VistaBrandColors.VioletDeep.copy(alpha = 0.12f))
                .clickable(onClick = ::togglePlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = when {
                    isPreparing -> "در حال آماده‌سازی ویس"
                    isPlaying -> "مکث ویس"
                    else -> "پخش ویس"
                },
                tint = if (isOutgoing) Color.White else VistaBrandColors.VioletDeep,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

            // Waveform + Timer
            Column(modifier = Modifier.weight(1f)) {
            // Simulated Waveform Bars with progress fill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalBars = 22
                for (i in 0 until totalBars) {
                    val barProgress = i.toFloat() / totalBars.toFloat()
                    val isFilled = barProgress <= progress
                    // Varied amplitude pattern
                    val amp = ((i * 7) % 5 + 2) / 7f
                    val h = (6 + (amp * 16)).dp

                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(h)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isFilled) activeColor else inactiveColor)
                    )
                }
                }

                Slider(
                    value = progress,
                    onValueChange = { fraction ->
                        val position = (fraction * totalDurationMs).toInt()
                        currentPositionMs = position
                        mediaPlayer?.let { player ->
                            runCatching { player.seekTo(position) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .testTag("voice-playback-position")
                        .semantics { contentDescription = "موقعیت پخش صدا" },
                )

                Spacer(Modifier.height(3.dp))

            // Time & Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displaySecs = if (currentPositionMs > 0) currentPositionMs / 1000 else (totalDurationMs / 1000)
                val mins = displaySecs / 60
                val secs = displaySecs % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    fontSize = 11.sp,
                    color = if (isOutgoing) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                // Speed Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isOutgoing) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = ::toggleSpeed)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (playbackSpeed == 1.0f) "1X" else "${playbackSpeed}X",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOutgoing) Color.White else VistaBrandColors.VioletDeep
                    )
                }
            }
        }
    }
}
