package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.util.Locale
import androidx.compose.ui.layout.layout
import kotlin.math.pow
import kotlin.math.sin
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
import ir.coffevista.vista_native.core.designsystem.glass.LocalVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
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

    val liveWaveformAmplitudes = remember { mutableStateListOf<Float>() }
    val allRecordedAmplitudes = remember { mutableListOf<Float>() }

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
        liveWaveformAmplitudes.clear()
        allRecordedAmplitudes.clear()
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
        liveWaveformAmplitudes.clear()
        allRecordedAmplitudes.clear()
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
                    val rawFraction = (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                    val amplitude = if (rawFraction > 0.002f) {
                        rawFraction.toDouble().pow(0.32).toFloat().coerceIn(0.12f, 1f)
                    } else {
                        val microFlutter = (sin(elapsedMs / 70.0).toFloat() * 0.04f).coerceAtLeast(0f)
                        (0.10f + microFlutter).coerceIn(0.08f, 0.16f)
                    }

                    allRecordedAmplitudes.add(amplitude)

                    if (liveWaveformAmplitudes.size >= 22) {
                        liveWaveformAmplitudes.removeAt(0)
                    }
                    liveWaveformAmplitudes.add(amplitude)
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
            val processedAmplitudes = if (allRecordedAmplitudes.size >= 10) {
                val peak = allRecordedAmplitudes.maxOrNull() ?: 0f
                val min = allRecordedAmplitudes.minOrNull() ?: 0f
                val range = peak - min
                if (peak > 0.18f && range > 0.08f) {
                    val factor = (0.90f / peak).coerceIn(1f, 3.5f)
                    allRecordedAmplitudes.map { (it * factor).coerceIn(0.12f, 0.98f) }
                } else {
                    synthesizeSpeechCadence(allRecordedAmplitudes.size)
                }
            } else {
                synthesizeSpeechCadence(28)
            }

            val targetCount = 36
            val downsampled = downsampleWaveform(processedAmplitudes, targetCount)
            val encodedWaveform = downsampled.joinToString(",") {
                String.format(Locale.US, "%.2f", it)
            }
            val draft = ChatAttachmentDraft(
                uri = file.absolutePath,
                fileName = file.name,
                mimeType = "audio/mp4",
                sizeBytes = file.length(),
                kind = AttachmentKind.VOICE,
                durationSeconds = duration,
                caption = "",
                waveform = if (encodedWaveform.isNotBlank()) "wf:$encodedWaveform" else null,
            )
            onSendVoice(draft)
        }
        cleanupRecorder(deleteFile = false)
    }

    DisposableEffect(Unit) {
        onDispose { cleanupRecorder(deleteFile = true) }
    }

    val lockProgress = (-dragOffsetY / lockThresholdPx).coerceIn(0f, 1f)
    val glassState = LocalVistaGlassState.current

    Box(
        modifier = modifier.height(46.dp),
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
                    .height(46.dp)
                    .then(
                        if (isCancelingState) {
                            Modifier.background(
                                Color(0xFFE53935).copy(alpha = if (isDark) 0.16f else 0.10f),
                                RoundedCornerShape(23.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(start = 10.dp, end = 4.dp),
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
                    Spacer(Modifier.width(6.dp))
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
                    color = if (isCancelingState) Color(0xFFE53935) else if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E2732),
                    fontSize = 14.sp
                )

                Spacer(Modifier.width(10.dp))

                // نمایش فرم‌موج صوتی زنده یا راهنمای کشیدن برای لغو
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (interaction.phase == VoiceCapturePhase.LOCKED) {
                        val liveTransition = rememberInfiniteTransition(label = "liveEqualizer")
                        val wavePhase by liveTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 6.283f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart,
                            ),
                            label = "wavePhase",
                        )
                        // میله‌های زنده فرکانس صوتی
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val count = 22
                            for (i in 0 until count) {
                                val liveAmp = liveWaveformAmplitudes.getOrNull(i) ?: 0f
                                val amp = if (liveAmp > 0.18f) {
                                    liveAmp
                                } else {
                                    val sine = sin(wavePhase + (i * 0.45f)).toFloat()
                                    (0.22f + (sine * 0.12f)).coerceIn(0.12f, 0.45f)
                                }
                                val h = (4 + (amp * 22)).dp
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(h)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(Color(0xFF2EA6FF))
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
                                color = if (isCancelingState) Color(0xFFE53935) else if (isDark) Color(0xFF7D8B99) else Color(0xFF707579),
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
                            .padding(end = 4.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2EA6FF))
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
                    Spacer(Modifier.size(42.dp))
                }
            }
        }

        // کپسول شناور قفل به سمت بالا (در حال نگه‌داشتن دکمه)
        if (interaction.phase == VoiceCapturePhase.HOLDING) {
            val lockTint = if (isDark) Color(0xFF1E2732) else Color.White
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        // Layout size is 0x0 so it never affects parent container measurement or causes composer expansion!
                        layout(0, 0) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .offset(y = (-64).dp, x = (-4).dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(42.dp)
                        .then(
                            if (glassState != null) {
                                Modifier.vistaGlass(
                                    state = glassState,
                                    shape = RoundedCornerShape(21.dp),
                                    tint = lockTint,
                                    isDark = isDark,
                                    blurRadius = VistaGlassTokens.PillBlur,
                                    rimWidth = 0.6.dp,
                                    shadowElevation = 6.dp,
                                )
                            } else {
                                Modifier
                                    .shadow(6.dp, RoundedCornerShape(21.dp))
                                    .background(lockTint.copy(alpha = 0.96f))
                            },
                        )
                        .clip(RoundedCornerShape(21.dp))
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
                            color = Color(0xFF2EA6FF),
                            trackColor = Color.LightGray.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxSize()
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "قفل ضبط",
                            tint = if (lockProgress > 0.65f) Color(0xFF2EA6FF) else Color.Gray,
                            modifier = Modifier.size(15.dp)
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
        }

        // دکمه دایره‌ای میکروفون
        if (interaction.phase != VoiceCapturePhase.LOCKED) {
            val scaleAnim by animateFloatAsState(
                targetValue = if (interaction.phase == VoiceCapturePhase.HOLDING) 1.06f else 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
                label = "micScale"
            )

            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(38.dp)
                    .scale(scaleAnim)
                    .shadow(if (interaction.phase == VoiceCapturePhase.HOLDING) 4.dp else 2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (interaction.phase == VoiceCapturePhase.HOLDING)
                            Color(0xFFE53935)
                        else
                            Color(0xFF2EA6FF)
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
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun synthesizeSpeechCadence(count: Int): List<Float> {
    val size = count.coerceIn(24, 64)
    val random = java.util.Random(System.currentTimeMillis())
    return List(size) { idx ->
        val phase = idx % 7
        val base = when (phase) {
            0 -> 0.18f
            1 -> 0.42f
            2 -> 0.88f
            3 -> 0.65f
            4 -> 0.82f
            5 -> 0.38f
            else -> 0.16f
        }
        val jitter = (random.nextFloat() * 0.18f) - 0.09f
        (base + jitter).coerceIn(0.14f, 0.95f)
    }
}

private fun downsampleWaveform(samples: List<Float>, targetCount: Int): List<Float> {
    if (samples.size <= targetCount) return samples
    val step = samples.size.toFloat() / targetCount.toFloat()
    return List(targetCount) { i ->
        val startIdx = (i * step).toInt().coerceIn(0, samples.lastIndex)
        val endIdx = ((i + 1) * step).toInt().coerceIn(startIdx + 1, samples.size)
        val slice = samples.subList(startIdx, endIdx)
        slice.maxOrNull() ?: 0.15f
    }
}
