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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * حباب اختصاصی پخش پیام صوتی با Waveform لمسی و تعاملی به سبک تلگرام.
 */
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
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(durationSeconds * 1000) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
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
            delay(50)
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

    fun seekToFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        val targetMs = (clamped * totalDurationMs).toInt()
        currentPositionMs = targetMs
        mediaPlayer?.let { player ->
            runCatching { player.seekTo(targetMs) }
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
    val inactiveColor = if (isOutgoing) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)

    Row(
        modifier = modifier
            .widthIn(min = 210.dp, max = 270.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // دکمه دایره‌ای پخش / مکث
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isOutgoing) Color.White.copy(alpha = 0.25f) else VistaBrandColors.VioletDeep.copy(alpha = 0.12f))
                .clickable(role = Role.Button, onClick = ::togglePlay)
                .semantics {
                    contentDescription = when {
                        isPreparing -> "در حال آماده‌سازی پیام صوتی"
                        isPlaying -> "مکث پیام صوتی"
                        else -> "پخش پیام صوتی"
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isOutgoing) Color.White else VistaBrandColors.VioletDeep,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        // ستون فرم‌موج تعاملی و زمان/سرعت
        Column(modifier = Modifier.weight(1f)) {
            // نوار فرم‌موج لمسی و قابل جابجایی
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("voice-playback-waveform")
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                        contentDescription = "نوار موقعیت پیام صوتی"
                    }
                    .pointerInput(totalDurationMs) {
                        detectTapGestures { offset ->
                            if (size.width > 0) {
                                seekToFraction(offset.x / size.width.toFloat())
                            }
                        }
                    }
                    .pointerInput(totalDurationMs) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            if (size.width > 0) {
                                seekToFraction(change.position.x / size.width.toFloat())
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                val totalBars = 26
                val barWidth = 2.5.dp

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 0 until totalBars) {
                        val barFraction = (i.toFloat() + 0.5f) / totalBars.toFloat()
                        val isFilled = barFraction <= progress
                        // الگوی دامنه فرکانسی طبیعی
                        val amp = ((i * 11 + 3) % 7 + 2) / 8.5f
                        val barHeight = (6 + (amp * 18)).dp

                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(barHeight)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isFilled) activeColor else inactiveColor),
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // زمان سپری‌شده و دکمه سرعت پخش
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val displaySecs = if (currentPositionMs > 0) currentPositionMs / 1000 else (totalDurationMs / 1000)
                val mins = displaySecs / 60
                val secs = displaySecs % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    fontSize = 11.sp,
                    color = if (isOutgoing) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )

                // دکمه انتخاب سرعت (1x, 1.5x, 2x)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isOutgoing) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(role = Role.Button, onClick = ::toggleSpeed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .semantics { contentDescription = "تغییر سرعت پخش به ${playbackSpeed} برابر" },
                ) {
                    Text(
                        text = if (playbackSpeed == 1.0f) "1X" else "${playbackSpeed}X",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOutgoing) Color.White else VistaBrandColors.VioletDeep,
                    )
                }
            }
        }
    }
}
