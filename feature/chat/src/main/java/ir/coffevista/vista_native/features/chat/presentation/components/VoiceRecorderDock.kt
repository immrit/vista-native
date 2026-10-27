package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
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

/**
 * داک اختصاصی ضبط و ارسال وویس منطبق بر تعاملات و گرافیک تلگرام.
 */
@Composable
fun VoiceRecorderDock(
    modifier: Modifier = Modifier,
    onSendVoice: (ChatAttachmentDraft) -> Unit,
    onPermissionRequired: () -> Unit,
    hasRecordPermission: Boolean,
    onActiveStateChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var interaction by remember { mutableStateOf(VoiceCaptureInteraction()) }
    var durationSeconds by remember { mutableIntStateOf(0) }
    var recordingStartElapsedRealtime by remember { mutableLongStateOf(0L) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isCancelingState by remember { mutableStateOf(false) }

    val waveformAmplitudes = remember { mutableStateListOf<Float>() }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var meterJob by remember { mutableStateOf<Job?>(null) }
    var ownsAudioFocus by remember { mutableStateOf(false) }
    var audioFocusRequest by remember { mutableStateOf<AudioFocusRequest?>(null) }

    val density = LocalDensity.current
    val lockThresholdPx = with(density) { 60.dp.toPx() }
    val cancelThresholdPx = with(density) { 55.dp.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "recording-pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun abandonAudioFocus() {
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
    }

    fun cleanupRecorder(deleteFile: Boolean) {
        meterJob?.cancel()
        meterJob = null
        mediaRecorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        mediaRecorder = null
        abandonAudioFocus()
        if (deleteFile) {
            recordingFile?.let { runCatching { it.delete() } }
        }
        recordingFile = null
        waveformAmplitudes.clear()
        durationSeconds = 0
        dragOffsetX = 0f
        dragOffsetY = 0f
        isCancelingState = false
        interaction = VoiceCapturePolicy.finish(interaction)
        onActiveStateChange(false)
    }

    fun startRecording() {
        if (!hasRecordPermission) {
            onPermissionRequired()
            return
        }
        if (interaction.isActive) return
        interaction = VoiceCapturePolicy.begin(interaction)
        onActiveStateChange(true)
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
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
                )
            }
            ownsAudioFocus = focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

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
            recordingStartElapsedRealtime = SystemClock.elapsedRealtime()
            mediaRecorder = recorder
            interaction = VoiceCapturePolicy.started(interaction, session)
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

            meterJob = coroutineScope.launch {
                while (isActive && interaction.session == session && mediaRecorder === recorder) {
                    delay(50)
                    val elapsedMs = SystemClock.elapsedRealtime() - recordingStartElapsedRealtime
                    durationSeconds = (elapsedMs / 1000).toInt()

                    val maxAmp = runCatching { recorder.maxAmplitude }.getOrDefault(0)
                    val normalized = (maxAmp / 32767f).coerceIn(0.12f, 1f)
                    if (waveformAmplitudes.size >= 48) {
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
        val elapsedMs = if (recordingStartElapsedRealtime > 0L) {
            SystemClock.elapsedRealtime() - recordingStartElapsedRealtime
        } else {
            durationSeconds * 1000L
        }
        val duration = (elapsedMs / 1000).toInt().coerceAtLeast(1)
        meterJob?.cancel()
        meterJob = null
        mediaRecorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        mediaRecorder = null
        abandonAudioFocus()

        if (file != null && file.exists() && file.length() > 0 && elapsedMs >= 700) {
            val encodedWaveform = waveformAmplitudes.joinToString(",") {
                String.format(Locale.US, "%.2f", it)
            }
            val draft = ChatAttachmentDraft(
                uri = file.absolutePath,
                fileName = file.name,
                mimeType = "audio/mp4",
                sizeBytes = file.length(),
                kind = AttachmentKind.VOICE,
                durationSeconds = duration,
                caption = if (encodedWaveform.isNotBlank()) "wf:$encodedWaveform" else "",
            )
            onSendVoice(draft)
        }
        cleanupRecorder(deleteFile = false)
    }

    DisposableEffect(Unit) {
        onDispose { cleanupRecorder(deleteFile = true) }
    }

    val lockProgress = (-dragOffsetY / lockThresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        // نوار ضبط صدای فعال (در حالت نگه‌داشتن یا قفل‌شده)
        AnimatedVisibility(
            visible = interaction.isActive,
            enter = fadeIn(tween(180)) + expandHorizontally(),
            exit = fadeOut(tween(160)) + shrinkHorizontally(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        when {
                            isCancelingState && isDark -> Color(0xFF381414)
                            isCancelingState -> Color(0xFFFFEBEE)
                            isDark -> MaterialTheme.colorScheme.surfaceVariant
                            else -> Color.White.copy(alpha = 0.98f)
                        }
                    )
                    .shadow(if (isDark) 0.dp else 4.dp, RoundedCornerShape(24.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // دکمه لغو / سطل زباله (در حالت قفل‌شده)
                if (interaction.phase == VoiceCapturePhase.LOCKED) {
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
                            cleanupRecorder(deleteFile = true)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "لغو و حذف ویس",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }

                // نقطه چشمک‌زن قرمز
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = pulseAlpha))
                )

                Spacer(Modifier.width(8.dp))

                // تایمر ضبط (دقیقه:ثانیه)
                val mins = durationSeconds / 60
                val secs = durationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelingState) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )

                Spacer(Modifier.width(12.dp))

                // نمایش فرم‌موج صوتی زنده یا راهنمای کشیدن برای لغو
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (interaction.phase == VoiceCapturePhase.LOCKED) {
                        // میله‌های زنده فرکانس صوتی
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val count = 22
                            for (i in 0 until count) {
                                val amp = waveformAmplitudes.getOrNull(i) ?: 0.12f
                                val h = (5 + (amp * 22)).dp
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
                        // راهنمای لغزشی تلگرام برای لغو
                        val cancelText = if (isCancelingState) "رها کنید برای لغو" else "‹ برای لغو به چپ بکشید"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.graphicsLayer { translationX = dragOffsetX.coerceAtMost(0f) * 0.45f }
                        ) {
                            if (isCancelingState) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = cancelText,
                                color = if (isCancelingState) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = if (isCancelingState) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }

                // دکمه ارسال در حالت قفل‌شده
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
                            .clickable {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                finishAndSend()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ارسال ویس",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    // فضای خالی برای قرارگیری دکمه ضبط میکروفون
                    Spacer(Modifier.size(48.dp))
                }
            }
        }

        // کپسول شناور قفل به سمت بالا (در حال نگه‌داشتن دکمه)
        if (interaction.phase == VoiceCapturePhase.HOLDING) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(y = (-70).dp)
                    .width(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(
                        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    )
                    .shadow(8.dp, RoundedCornerShape(23.dp))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { lockProgress },
                        strokeWidth = 2.5.dp,
                        color = VistaBrandColors.VioletDeep,
                        trackColor = Color.LightGray.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxSize()
                    )
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل ضبط",
                        tint = if (lockProgress > 0.65f) VistaBrandColors.VioletDeep else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.65f),
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { translationY = -lockProgress * 8 }
                )
            }
        }

        // دکمه دایره‌ای میکروفون
        if (interaction.phase != VoiceCapturePhase.LOCKED) {
            val scaleAnim by animateFloatAsState(
                targetValue = if (interaction.phase == VoiceCapturePhase.HOLDING) 1.22f else 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                label = "micScale"
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(scaleAnim)
                    .shadow(if (interaction.phase == VoiceCapturePhase.HOLDING) 6.dp else 4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (interaction.phase == VoiceCapturePhase.HOLDING)
                            Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFC62828)))
                        else
                            Brush.linearGradient(listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep))
                    )
                    .pointerInput(hasRecordPermission) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!hasRecordPermission) {
                                onPermissionRequired()
                                waitForUpOrCancellation()
                                return@awaitEachGesture
                            }
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                            isCancelingState = false
                            startRecording()

                            val pointerId = down.id
                            var cancelledByThreshold = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    if (interaction.phase == VoiceCapturePhase.HOLDING) {
                                        val elapsedMs = SystemClock.elapsedRealtime() - recordingStartElapsedRealtime
                                        val shouldCancel = dragOffsetX < -cancelThresholdPx || cancelledByThreshold
                                        if (shouldCancel) {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
                                            cleanupRecorder(deleteFile = true)
                                        } else if (elapsedMs < 700) {
                                            // ضبط کمتر از ۷۰۰ میلی‌ثانیه ناشی از تپ تصادفی دور ریخته می‌شود
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

                                // بررسی بی‌درنگ وضعیت کشیدن برای لغو
                                val currentlyCanceling = dragX < -cancelThresholdPx
                                if (currentlyCanceling != isCancelingState) {
                                    isCancelingState = currentlyCanceling
                                    if (currentlyCanceling) {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                }
                                if (dragX < -cancelThresholdPx * 1.6f && !cancelledByThreshold) {
                                    cancelledByThreshold = true
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
                                }

                                // بررسی کشیدن به بالا برای قفل ضبط
                                if (dragY < -lockThresholdPx && interaction.phase == VoiceCapturePhase.HOLDING && !cancelledByThreshold) {
                                    interaction = VoiceCapturePolicy.lock(interaction)
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                    break
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "نگه‌داشتن برای ضبط صدا",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
