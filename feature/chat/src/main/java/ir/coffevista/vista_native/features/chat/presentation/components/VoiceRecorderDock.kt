package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Composable
fun VoiceRecorderDock(
    modifier: Modifier = Modifier,
    onSendVoice: (ChatAttachmentDraft) -> Unit,
    onPermissionRequired: () -> Unit,
    hasRecordPermission: Boolean,
    onHoldingStateChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var interaction by remember { mutableStateOf(VoiceCaptureInteraction()) }
    var durationSeconds by remember { mutableStateOf(0) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val waveformAmplitudes = remember { mutableStateListOf<Float>() }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var meterJob by remember { mutableStateOf<Job?>(null) }
    var ownsAudioFocus by remember { mutableStateOf(false) }
    var audioFocusRequest by remember { mutableStateOf<AudioFocusRequest?>(null) }
    val lockThresholdPx = with(LocalDensity.current) { 140.dp.toPx() }
    val cancelThresholdPx = with(LocalDensity.current) { 180.dp.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "recording-pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    fun cleanupRecorder(deleteFile: Boolean) {
        meterJob?.cancel()
        meterJob = null
        mediaRecorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        mediaRecorder = null
        if (ownsAudioFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            ownsAudioFocus = false
        }
        audioFocusRequest = null
        if (deleteFile) {
            recordingFile?.let { runCatching { it.delete() } }
        }
        recordingFile = null
        waveformAmplitudes.clear()
        durationSeconds = 0
        dragOffsetX = 0f
        dragOffsetY = 0f
        interaction = VoiceCapturePolicy.finish(interaction)
        onHoldingStateChange(false)
    }

    fun startRecording() {
        if (!hasRecordPermission) {
            onPermissionRequired()
            return
        }
        if (interaction.isActive) return
        interaction = VoiceCapturePolicy.begin(interaction)
        val session = interaction.session
        val dir = File(context.cacheDir, "chat-voice").apply { mkdirs() }
        val file = File(dir, "voice_${UUID.randomUUID()}.m4a")
        recordingFile = file

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            val focusResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .build()
                audioFocusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
                )
            }
            ownsAudioFocus = focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!ownsAudioFocus) {
                runCatching { recorder.release() }
                cleanupRecorder(deleteFile = true)
                return
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(96000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            if (interaction.session != session || interaction.phase != VoiceCapturePhase.STARTING) {
                runCatching { recorder.stop() }
                runCatching { recorder.release() }
                return
            }
            mediaRecorder = recorder
            interaction = VoiceCapturePolicy.started(interaction, session)
            onHoldingStateChange(true)
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

            meterJob = coroutineScope.launch {
                var secondsCounter = 0L
                while (isActive && interaction.session == session && mediaRecorder === recorder) {
                    delay(100)
                    secondsCounter += 100
                    durationSeconds = (secondsCounter / 1000).toInt()

                    val maxAmp = runCatching { recorder.maxAmplitude }.getOrDefault(0)
                    val normalized = (maxAmp / 32767f).coerceIn(0.08f, 1f)
                    if (waveformAmplitudes.size >= 24) {
                        waveformAmplitudes.removeAt(0)
                    }
                    waveformAmplitudes.add(normalized)
                }
            }
        } catch (e: Exception) {
            cleanupRecorder(deleteFile = true)
        }
    }

    fun finishAndSend() {
        if (!interaction.isActive) return
        val file = recordingFile
        val duration = durationSeconds
        meterJob?.cancel()
        meterJob = null
        mediaRecorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        mediaRecorder = null

        if (ownsAudioFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            ownsAudioFocus = false
        }
        audioFocusRequest = null

        if (file != null && file.exists() && duration >= 1) {
            val draft = ChatAttachmentDraft(
                uri = file.absolutePath,
                fileName = file.name,
                mimeType = "audio/mp4",
                sizeBytes = file.length(),
                kind = AttachmentKind.VOICE,
                durationSeconds = duration,
            )
            onSendVoice(draft)
        }
        cleanupRecorder(deleteFile = false)
    }

    DisposableEffect(Unit) {
        onDispose { cleanupRecorder(deleteFile = true) }
    }

    val lockProgress = (-dragOffsetY / 160f).coerceIn(0f, 1f)
    val isCanceling = dragOffsetX < -cancelThresholdPx

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        // Vertical Lock Indicator (while holding)
        if (interaction.phase == VoiceCapturePhase.HOLDING) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(y = (-70).dp)
                    .width(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                    .shadow(4.dp, RoundedCornerShape(22.dp))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { lockProgress },
                        strokeWidth = 2.dp,
                        color = VistaBrandColors.VioletDeep,
                        trackColor = Color.LightGray.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxSize()
                    )
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل ضبط",
                        tint = if (lockProgress > 0.7f) VistaBrandColors.VioletDeep else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer { translationY = -lockProgress * 10 }
                )
            }
        }

        // Locked Bar or Holding Indicator
        AnimatedVisibility(
            visible = interaction.isActive,
            enter = fadeIn(tween(180)) + expandHorizontally(),
            exit = fadeOut(tween(160)) + shrinkHorizontally()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (isCanceling) Color(0xFFFFEBEE)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete / Cancel Button
                IconButton(
                    onClick = { cleanupRecorder(deleteFile = true) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "لغو و حذف ویس",
                        tint = Color.Red,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Red Blinking Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = pulseAlpha))
                )

                Spacer(Modifier.width(8.dp))

                // Timer Pill
                val mins = durationSeconds / 60
                val secs = durationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    fontWeight = FontWeight.Bold,
                    color = if (isCanceling) Color.Red else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )

                Spacer(Modifier.width(12.dp))

                // Waveform / Slide-to-cancel Label
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (interaction.phase == VoiceCapturePhase.LOCKED) {
                        // Live Audio Waveform Bars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val count = 20
                            for (i in 0 until count) {
                                val amp = waveformAmplitudes.getOrNull(i) ?: 0.15f
                                val h = (6 + (amp * 20)).dp
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(h)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(VistaBrandColors.VioletDeep)
                                )
                            }
                        }
                    } else {
                        // Slide to cancel hint
                        val cancelText = if (isCanceling) "رها کنید برای لغو" else "‹ برای لغو به چپ بکشید"
                        Text(
                            text = cancelText,
                            color = if (isCanceling) Color.Red else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = if (isCanceling) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.graphicsLayer { translationX = dragOffsetX * 0.4f }
                        )
                    }
                }

                // Send Button in Locked State
                if (interaction.phase == VoiceCapturePhase.LOCKED) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep)
                                )
                            )
                            .clickable(onClick = ::finishAndSend),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ارسال ویس",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Mic Button (Touch & Gesture Target)
        if (interaction.phase != VoiceCapturePhase.LOCKED) {
            val scaleAnim by animateFloatAsState(
                targetValue = if (interaction.phase == VoiceCapturePhase.HOLDING) 1.25f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                label = "micScale"
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .scale(scaleAnim)
                    .clip(CircleShape)
                    .background(
                        if (interaction.phase == VoiceCapturePhase.HOLDING)
                            Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFC62828)))
                        else
                            Brush.linearGradient(listOf(VistaBrandColors.Indigo.copy(alpha = 0.15f), VistaBrandColors.VioletDeep.copy(alpha = 0.15f)))
                    )
                    .pointerInput(hasRecordPermission) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                startRecording()

                                val pointerId = down.id

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId }
                                    if (change == null || !change.pressed) {
                                        if (interaction.phase == VoiceCapturePhase.HOLDING) {
                                            if (isCanceling) {
                                                view.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
                                                cleanupRecorder(deleteFile = true)
                                            } else {
                                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                                finishAndSend()
                                            }
                                        }
                                        break
                                    }

                                    val dragX = change.position.x - down.position.x
                                    val dragY = change.position.y - down.position.y
                                    dragOffsetX = dragX
                                    dragOffsetY = dragY

                                    if (dragOffsetY < -lockThresholdPx && interaction.phase == VoiceCapturePhase.HOLDING) {
                                        interaction = VoiceCapturePolicy.lock(interaction)
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                        break
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "نگه‌داشتن برای ضبط صدا",
                    tint = if (interaction.phase == VoiceCapturePhase.HOLDING) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
