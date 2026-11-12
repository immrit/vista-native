package ir.coffevista.vista_native.features.chat.presentation.audio

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.component.formatAudioTime
import ir.coffevista.vista_native.core.designsystem.component.resolveMusicTitle
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.chat.domain.model.Attachment
import ir.coffevista.vista_native.features.chat.domain.model.DownloadState
import ir.coffevista.vista_native.features.chat.domain.model.DownloadTask
import ir.coffevista.vista_native.features.chat.presentation.toPersianDigits

/**
 * حباب اختصاصی فایل موسیقی در چت (مشابه `DOCUMENT_ATTACH_TYPE_MUSIC` در تلگرام):
 * مجزا از حباب پیام صوتی، با نام آهنگ، خواننده، مدت زمان/حجم و نوار پیشرفت تعاملی هنگام پخش.
 */
@Composable
fun MusicPlayerBubble(
    attachment: Attachment,
    messageId: String,
    isOutgoing: Boolean,
    isDownloaded: Boolean,
    downloadTask: DownloadTask? = null,
    senderName: String = if (isOutgoing) "شما" else "مخاطب",
    conversationId: String = "",
    conversationTitle: String? = null,
    createdAtEpochMillis: Long = 0L,
    musicQueue: List<ChatAudioTrack> = emptyList(),
    onStartDownload: () -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onOpenFullPlayer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(context) { ChatAudioPlayer.attach(context) }

    val effectiveUrl = downloadTask?.localUri?.takeIf(String::isNotBlank)
        ?: attachment.localUri?.takeIf(String::isNotBlank)
        ?: attachment.remoteUrl.orEmpty()
    val isDownloading = downloadTask?.state == DownloadState.DOWNLOADING || downloadTask?.state == DownloadState.QUEUED
    val readyToPlay = isOutgoing || isDownloaded ||
        (downloadTask?.state == DownloadState.COMPLETE && !downloadTask.localUri.isNullOrBlank())

    val displayTitle = remember(attachment.audioTitle, attachment.fileName, attachment.remoteUrl) {
        resolveMusicTitle(attachment.audioTitle, attachment.fileName ?: attachment.remoteUrl)
    }
    val displayArtist = remember(attachment.audioArtist) {
        attachment.audioArtist?.trim()?.takeIf(String::isNotEmpty)
    }

    val track = remember(
        messageId,
        effectiveUrl,
        attachment.remoteUrl,
        displayTitle,
        displayArtist,
        attachment.durationSeconds,
        senderName,
        conversationId,
        conversationTitle,
        createdAtEpochMillis,
    ) {
        ChatAudioTrack(
            id = messageId,
            url = effectiveUrl,
            kind = ChatAudioKind.MUSIC,
            title = displayTitle,
            artist = displayArtist,
            durationMs = (attachment.durationSeconds ?: 0).coerceAtLeast(0) * 1000L,
            senderName = senderName,
            conversationId = conversationId,
            createdAtEpochMillis = createdAtEpochMillis,
            fallbackUrl = attachment.remoteUrl?.takeIf { it.isNotBlank() && it != effectiveUrl },
            conversationTitle = conversationTitle,
        )
    }

    val coverBitmap = AudioCoverArtCache.rememberCoverArt(effectiveUrl, attachment.remoteUrl)

    val playerState by ChatAudioPlayer.state.collectAsState()
    val positionMs by ChatAudioPlayer.position.collectAsState()

    val isActive = playerState.isActive(messageId)
    val isPlaying = isActive && playerState.isPlaying
    val isBuffering = isActive && playerState.isBuffering

    val durationMs = when {
        isActive && playerState.durationMs > 0L -> playerState.durationMs
        (attachment.durationSeconds ?: 0) > 0 -> (attachment.durationSeconds ?: 0) * 1000L
        else -> 0L
    }
    val currentPosMs = if (isActive) positionMs else ChatAudioPlayer.savedPositionMs(messageId)

    var isScrubbing by remember(messageId) { mutableStateOf(false) }
    var scrubFraction by remember(messageId) { mutableFloatStateOf(0f) }

    val liveProgress = if (durationMs > 0L) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val progress = if (isScrubbing) scrubFraction else liveProgress

    val titleColor = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isOutgoing) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
    val activeTrackColor = if (isOutgoing) Color.White else VistaBrandColors.Indigo
    val inactiveTrackColor = if (isOutgoing) Color.White.copy(alpha = 0.30f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)

    fun onPlayTap() {
        if (!readyToPlay) {
            if (isDownloading) {
                downloadTask?.let { onCancelDownload(it.messageId) }
            } else {
                onStartDownload()
            }
            return
        }
        val queue = musicQueue.takeIf { it.isNotEmpty() } ?: listOf(track)
        ChatAudioPlayer.toggle(track, queue)
    }

    Column(
        modifier = modifier
            .widthIn(min = 232.dp, max = 300.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (readyToPlay && isActive) {
                    onOpenFullPlayer()
                } else {
                    onPlayTap()
                }
            }
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOutgoing) {
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.28f),
                                    Color.White.copy(alpha = 0.16f),
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
                    .clickable(role = Role.Button, onClick = ::onPlayTap)
                    .semantics {
                        contentDescription = when {
                            !readyToPlay && isDownloading -> "لغو دانلود موسیقی"
                            !readyToPlay -> "دانلود موسیقی"
                            isPlaying -> "مکث موسیقی"
                            else -> "پخش موسیقی"
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.36f)),
                    )
                }
                when {
                    !readyToPlay && isDownloading -> {
                        CircularProgressIndicator(
                            progress = { downloadTask?.progress?.takeIf { it > 0f } ?: 0.12f },
                            strokeWidth = 2.5.dp,
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(40.dp),
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
                            contentDescription = "دانلود موسیقی",
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
                            label = "music-play-pause-icon",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isOutgoing) Color.White.copy(alpha = 0.85f) else VistaBrandColors.Indigo,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = displayTitle,
                        color = titleColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(2.dp))

                val sizeText = attachment.sizeBytes?.takeIf { it > 0L }?.let { Formatter.formatShortFileSize(context, it) }
                val durationLabel = durationMs.takeIf { it > 0L }?.let(::formatAudioTime)
                val subtitle = when {
                    !readyToPlay && isDownloading -> {
                        val pct = ((downloadTask?.progress ?: 0f) * 100).toInt()
                        val got = downloadTask?.receivedBytes?.takeIf { it > 0L }?.let { Formatter.formatShortFileSize(context, it) }
                        if (got != null && sizeText != null) "$pct٪ • $got / $sizeText" else "$pct٪ • در حال دانلود"
                    }
                    isActive -> {
                        val posText = formatAudioTime(if (isScrubbing && durationMs > 0L) (scrubFraction * durationMs).toLong() else currentPosMs)
                        val artistPrefix = displayArtist?.let { "$it • " }.orEmpty()
                        if (durationLabel != null) "$artistPrefix$posText / $durationLabel" else "$artistPrefix$posText"
                    }
                    else -> {
                        listOfNotNull(displayArtist, durationLabel, sizeText)
                            .joinToString(" • ")
                            .ifBlank { "فایل موسیقی" }
                    }
                }

                Text(
                    text = subtitle.toPersianDigits(),
                    color = subtitleColor,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("music-playback-position"),
                )
            }
        }

        AnimatedVisibility(
            visible = isActive && durationMs > 0L,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp)
                        .height(18.dp)
                        .pointerInput(messageId, durationMs) {
                            detectTapGestures { offset ->
                                if (size.width > 0) {
                                    ChatAudioPlayer.seekToFraction((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                                }
                            }
                        }
                        .pointerInput(messageId, durationMs) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isScrubbing = true
                                    if (size.width > 0) scrubFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (size.width > 0) scrubFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    ChatAudioPlayer.seekToFraction(scrubFraction)
                                    isScrubbing = false
                                },
                                onDragCancel = { isScrubbing = false },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barH = 3.dp.toPx()
                        val y = (size.height - barH) / 2f
                        val radius = CornerRadius(barH / 2f, barH / 2f)
                        drawRoundRect(
                            color = inactiveTrackColor,
                            topLeft = Offset(0f, y),
                            size = Size(size.width, barH),
                            cornerRadius = radius,
                        )
                        val fillW = (size.width * progress).coerceIn(0f, size.width)
                        if (fillW > 0f) {
                            drawRoundRect(
                                color = activeTrackColor,
                                topLeft = Offset(0f, y),
                                size = Size(fillW, barH),
                                cornerRadius = radius,
                            )
                        }
                        val thumbRadius = if (isScrubbing) 6.dp.toPx() else 4.5.dp.toPx()
                        drawCircle(
                            color = activeTrackColor,
                            radius = thumbRadius,
                            center = Offset(fillW.coerceIn(thumbRadius, (size.width - thumbRadius).coerceAtLeast(thumbRadius)), size.height / 2f),
                        )
                    }
                }
            }
        }
    }
}
