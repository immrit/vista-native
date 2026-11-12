package ir.coffevista.vista_native.features.chat.presentation.components

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.chat.domain.model.DownloadState
import ir.coffevista.vista_native.features.chat.domain.model.DownloadTask
import ir.coffevista.vista_native.features.chat.presentation.audio.AudioWaveformExtractor
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioKind
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioPlayer
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioTrack
import java.util.Locale

/**
 * حباب حرفه‌ای پخش پیام صوتی با الگوبرداری از `SeekBarWaveform` و `MediaController` تلگرام:
 * - موج صدای واقعی استخراج‌شده از فایل صوتی (بدون موج تکراری ساختگی)
 * - برش پیکسلی دقیق روی میله‌ها هنگام پخش و کشیدن (Pixel-accurate progress clip)
 * - پخش متوالی خودکار، حفظ موقعیت مکث، و سرعت جهانی (1X / 1.5X / 2X)
 * - نقطه خوانده‌نشده برای پیام‌های صوتی دریافتی
 */
@Composable
fun VoicePlayerBubble(
    modifier: Modifier = Modifier,
    audioUrl: String,
    durationSeconds: Int,
    isOutgoing: Boolean,
    isDownloaded: Boolean = true,
    downloadTask: DownloadTask? = null,
    waveformAmplitudes: List<Float> = emptyList(),
    messageId: String? = null,
    fallbackUrl: String? = null,
    senderName: String = if (isOutgoing) "شما" else "پیام صوتی",
    conversationId: String = "",
    conversationTitle: String? = null,
    createdAtEpochMillis: Long = 0L,
    sizeBytes: Long? = null,
    voiceQueue: List<ChatAudioTrack> = emptyList(),
    onStartDownload: () -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onPlaybackFinished: () -> Unit = {},
) {
    val context = LocalContext.current
    LaunchedEffect(context) { ChatAudioPlayer.attach(context) }

    val trackId = messageId ?: audioUrl
    val effectiveUrl = downloadTask?.localUri?.takeIf(String::isNotBlank) ?: audioUrl
    val isDownloading = downloadTask?.state == DownloadState.DOWNLOADING || downloadTask?.state == DownloadState.QUEUED
    val readyToPlay = isOutgoing || isDownloaded ||
        (downloadTask?.state == DownloadState.COMPLETE && !downloadTask.localUri.isNullOrBlank())

    val track = remember(trackId, effectiveUrl, fallbackUrl, durationSeconds, senderName, conversationId, conversationTitle, createdAtEpochMillis) {
        ChatAudioTrack(
            id = trackId,
            url = effectiveUrl,
            kind = ChatAudioKind.VOICE,
            title = senderName,
            artist = null,
            durationMs = durationSeconds.coerceAtLeast(1) * 1000L,
            senderName = senderName,
            conversationId = conversationId,
            createdAtEpochMillis = createdAtEpochMillis,
            fallbackUrl = fallbackUrl?.takeIf { it.isNotBlank() && it != effectiveUrl },
            conversationTitle = conversationTitle,
        )
    }

    val playerState by ChatAudioPlayer.state.collectAsState()
    val globalPositionMs by ChatAudioPlayer.position.collectAsState()
    val listenedIds by ChatAudioPlayer.listened.collectAsState()
    val voiceSpeed by ChatAudioPlayer.voiceSpeedFlow.collectAsState()

    val isActive = playerState.isActive(trackId)
    val isPlaying = isActive && playerState.isPlaying
    val isBuffering = isActive && playerState.isBuffering
    val isListened = isOutgoing || (trackId in listenedIds)

    val totalDurationMs = if (isActive && playerState.durationMs > 0L) {
        playerState.durationMs
    } else {
        (durationSeconds.coerceAtLeast(1) * 1000L)
    }
    val currentPositionMs = if (isActive) {
        globalPositionMs
    } else {
        ChatAudioPlayer.savedPositionMs(trackId)
    }

    var isScrubbing by remember(trackId) { mutableStateOf(false) }
    var scrubFraction by remember(trackId) { mutableFloatStateOf(0f) }

    val liveProgress = if (totalDurationMs > 0L) {
        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val displayProgress = if (isScrubbing) scrubFraction else liveProgress

    // Extract real waveform from local audio file if server/message didn't supply meaningful samples.
    val hasSuppliedWaveform = remember(waveformAmplitudes) {
        AudioWaveformExtractor.hasMeaningfulVariation(waveformAmplitudes)
    }
    var extractedWaveform by remember(effectiveUrl) {
        mutableStateOf(AudioWaveformExtractor.getCached(effectiveUrl))
    }
    LaunchedEffect(effectiveUrl, hasSuppliedWaveform, readyToPlay) {
        if (!hasSuppliedWaveform && readyToPlay && extractedWaveform == null) {
            val decoded = AudioWaveformExtractor.extract(context, effectiveUrl, targetBars = 52)
            if (!decoded.isNullOrEmpty()) {
                extractedWaveform = decoded
            }
        }
    }

    val resolvedAmplitudes = remember(waveformAmplitudes, hasSuppliedWaveform, extractedWaveform) {
        when {
            hasSuppliedWaveform -> {
                val min = waveformAmplitudes.minOrNull() ?: 0f
                val max = waveformAmplitudes.maxOrNull() ?: 1f
                val range = (max - min).coerceAtLeast(0.01f)
                waveformAmplitudes.map { ((it - min) / range * 0.85f + 0.15f).coerceIn(0.10f, 1f) }
            }
            !extractedWaveform.isNullOrEmpty() -> extractedWaveform!!
            else -> List(44) { 0.22f }
        }
    }

    fun handlePrimaryAction() {
        if (!readyToPlay) {
            if (isDownloading) {
                downloadTask?.let { onCancelDownload(it.messageId) }
            } else {
                onStartDownload()
            }
            return
        }
        val queue = voiceQueue.takeIf { it.isNotEmpty() } ?: listOf(track)
        ChatAudioPlayer.toggle(track, queue)
    }

    val activeColor = if (isOutgoing) Color.White else VistaBrandColors.VioletDeep
    val inactiveColor = if (isOutgoing) {
        Color.White.copy(alpha = 0.36f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.26f)
    }
    val unreadAccent = Color(0xFF2AABEE)

    Row(
        modifier = modifier
            .widthIn(min = 224.dp, max = 286.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isOutgoing) {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.18f),
                            ),
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                VistaBrandColors.Indigo,
                                VistaBrandColors.VioletDeep,
                            ),
                        )
                    },
                )
                .clickable(role = Role.Button, onClick = ::handlePrimaryAction)
                .semantics {
                    contentDescription = when {
                        !readyToPlay && isDownloading -> "لغو دانلود پیام صوتی"
                        !readyToPlay -> "دانلود پیام صوتی"
                        isPlaying -> "مکث پیام صوتی"
                        else -> "پخش پیام صوتی"
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            when {
                !readyToPlay && isDownloading -> {
                    CircularProgressIndicator(
                        progress = { downloadTask?.progress?.takeIf { it > 0f } ?: 0.12f },
                        strokeWidth = 2.5.dp,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(38.dp),
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "لغو دانلود",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
                !readyToPlay -> {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "دانلود صدا",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                isBuffering -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                else -> {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            (fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.72f))
                                .togetherWith(fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.72f))
                        },
                        label = "voice-play-pause-icon",
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .testTag("voice-playback-waveform")
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(displayProgress, 0f..1f)
                            contentDescription = "نوار موقعیت پیام صوتی"
                        }
                        .pointerInput(trackId, totalDurationMs, readyToPlay) {
                            if (!readyToPlay) return@pointerInput
                            detectTapGestures { offset ->
                                if (size.width > 0) {
                                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val queue = voiceQueue.takeIf { it.isNotEmpty() } ?: listOf(track)
                                    ChatAudioPlayer.seekAndPlay(track, queue, frac)
                                }
                            }
                        }
                        .pointerInput(trackId, totalDurationMs, readyToPlay) {
                            if (!readyToPlay) return@pointerInput
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isScrubbing = true
                                    if (size.width > 0) {
                                        scrubFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (size.width > 0) {
                                        scrubFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    }
                                },
                                onDragEnd = {
                                    val queue = voiceQueue.takeIf { it.isNotEmpty() } ?: listOf(track)
                                    ChatAudioPlayer.seekAndPlay(track, queue, scrubFraction)
                                    isScrubbing = false
                                },
                                onDragCancel = {
                                    isScrubbing = false
                                },
                            )
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        if (w <= 0f || h <= 0f) return@Canvas

                        val barWidth = 2.dp.toPx()
                        val barPitch = 3.4.dp.toPx()
                        val barCount = (w / barPitch).toInt().coerceIn(16, 72)
                        val totalDrawnWidth = (barCount - 1) * barPitch + barWidth
                        val startX = ((w - totalDrawnWidth) / 2f).coerceAtLeast(0f)
                        val maxBarHeight = (h - 4.dp.toPx()).coerceAtLeast(6.dp.toPx())
                        val minBarHeight = 3.dp.toPx()
                        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)

                        fun drawBars(color: Color) {
                            for (i in 0 until barCount) {
                                val samplePos = if (barCount > 1) {
                                    (i.toFloat() / (barCount - 1).toFloat()) * (resolvedAmplitudes.size - 1)
                                } else 0f
                                val lo = samplePos.toInt().coerceIn(0, resolvedAmplitudes.lastIndex)
                                val hi = (lo + 1).coerceIn(0, resolvedAmplitudes.lastIndex)
                                val frac = samplePos - lo
                                val amp = resolvedAmplitudes[lo] * (1f - frac) + resolvedAmplitudes[hi] * frac
                                val barH = (minBarHeight + amp * (maxBarHeight - minBarHeight)).coerceIn(minBarHeight, maxBarHeight)
                                val x = startX + i * barPitch
                                val top = (h - barH) / 2f
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(x, top),
                                    size = Size(barWidth, barH),
                                    cornerRadius = corner,
                                )
                            }
                        }

                        val splitX = if (readyToPlay) (w * displayProgress).coerceIn(0f, w) else 0f
                        if (splitX > 0f) {
                            clipRect(left = 0f, top = 0f, right = splitX, bottom = h) {
                                drawBars(activeColor)
                            }
                        }
                        if (splitX < w) {
                            clipRect(left = splitX, top = 0f, right = w, bottom = h) {
                                drawBars(inactiveColor)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!readyToPlay) {
                    val sizeLabel = sizeBytes?.takeIf { it > 0L }?.let { Formatter.formatShortFileSize(context, it) }
                    val statusText = if (isDownloading) {
                        "${((downloadTask?.progress ?: 0f) * 100).toInt()}٪ • در حال دریافت"
                    } else if (sizeLabel != null) {
                        "${formatVoiceSeconds(durationSeconds)} • $sizeLabel"
                    } else {
                        "${formatVoiceSeconds(durationSeconds)} • لمس برای دانلود"
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        color = if (isOutgoing) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("voice-playback-position"),
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val displaySecs = if (isScrubbing) {
                            ((scrubFraction * totalDurationMs) / 1000L).toInt()
                        } else if (currentPositionMs > 0L) {
                            (currentPositionMs / 1000L).toInt()
                        } else {
                            (totalDurationMs / 1000L).toInt()
                        }
                        val totalSecs = (totalDurationMs / 1000L).toInt().coerceAtLeast(1)
                        val timeText = if (isActive || currentPositionMs > 0L || isScrubbing) {
                            "${formatVoiceSeconds(displaySecs)} / ${formatVoiceSeconds(totalSecs)}"
                        } else {
                            formatVoiceSeconds(totalSecs)
                        }
                        Text(
                            text = timeText,
                            fontSize = 11.sp,
                            color = if (isOutgoing) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("voice-playback-position"),
                        )
                        if (!isListened && !isActive) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(unreadAccent),
                            )
                        }
                    }

                    val speedLabel = formatSpeedLabel(voiceSpeed)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isOutgoing) Color.White.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            )
                            .clickable(role = Role.Button) {
                                if (!isActive) {
                                    val queue = voiceQueue.takeIf { it.isNotEmpty() } ?: listOf(track)
                                    ChatAudioPlayer.play(track, queue)
                                }
                                ChatAudioPlayer.cycleSpeed()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .semantics { contentDescription = "تغییر سرعت پخش صدا" },
                    ) {
                        Text(
                            text = speedLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOutgoing) Color.White else VistaBrandColors.VioletDeep,
                        )
                    }
                }
            }
        }
    }
}

private fun formatVoiceSeconds(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}

internal fun formatSpeedLabel(speed: Float): String = when {
    kotlin.math.abs(speed - 1f) < 0.05f -> "1X"
    kotlin.math.abs(speed - 1.5f) < 0.05f -> "1.5X"
    kotlin.math.abs(speed - 2f) < 0.05f -> "2X"
    else -> "${String.format(Locale.US, "%.1f", speed)}X"
}
