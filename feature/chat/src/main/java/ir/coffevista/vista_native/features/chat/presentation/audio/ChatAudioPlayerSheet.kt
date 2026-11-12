package ir.coffevista.vista_native.features.chat.presentation.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import ir.coffevista.vista_native.core.designsystem.component.formatAudioTime
import ir.coffevista.vista_native.features.chat.presentation.toPersianDigits
import java.io.File
import kotlin.math.abs

private val TelegramAccentBlue = Color(0xFF4FA6E8)
private val TelegramAccentBlueDeep = Color(0xFF338CD1)

/**
 * پخش‌کننده تمام‌صفحه/باتم‌شیت موسیقی به سبک دقیق `AudioPlayerAlert` تلگرام:
 * - هدر با دکمه بازگشت، عنوان پلی‌لیست گفتگو و جست‌وجوی زنده در ترک‌ها
 * - لیست کامل ترک‌های گفتگو با کاور دایره‌ای واقعی (یا آیکون آبی تلگرامی)، دکمه پخش/مکث و منوی سه نقطه
 * - پنل ثابت پایین شامل نام آهنگ و خواننده، کاور مربعی گوشه‌گرد، SeekBar ظریف تلگرامی، و دکمه‌های کنترل
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAudioPlayerSheet(
    onDismiss: () -> Unit,
    onJumpToMessage: ((String) -> Unit)? = null,
    onShareTrack: ((ChatAudioTrack) -> Unit)? = null,
) {
    val context = LocalContext.current
    val playerState by ChatAudioPlayer.state.collectAsState()
    val positionMs by ChatAudioPlayer.position.collectAsState()
    val track = playerState.track ?: run {
        onDismiss()
        return
    }

    val isDark = isSystemInDarkTheme()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val durationMs = playerState.durationMs.takeIf { it > 0L } ?: track.durationMs

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var repeatMenuExpanded by remember { mutableStateOf(false) }
    var optionsMenuExpanded by remember { mutableStateOf(false) }
    var isScrubbing by remember(track.id) { mutableStateOf(false) }
    var scrubValue by remember(track.id) { mutableFloatStateOf(0f) }

    val queue = remember(playerState.queue, track) {
        playerState.queue.ifEmpty { listOf(track) }
    }
    val filteredQueue = remember(queue, searchQuery, searchActive) {
        val q = if (searchActive) searchQuery.trim() else ""
        if (q.isEmpty()) queue
        else queue.filter { item ->
            item.title.contains(q, ignoreCase = true) ||
                (item.artist?.contains(q, ignoreCase = true) == true) ||
                item.senderName.contains(q, ignoreCase = true)
        }
    }

    val sheetBg = if (isDark) Color(0xFF17212B) else Color(0xFFF6F8FB)
    val panelBg = if (isDark) Color(0xFF1F2B38) else Color(0xFFFFFFFF)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)
    val titleColor = if (isDark) Color.White else Color(0xFF121820)
    val subtitleColor = if (isDark) Color(0xFF8C9AA8) else Color(0xFF667382)

    val shareHandler: (ChatAudioTrack) -> Unit = remember(context, onShareTrack) {
        { item ->
            if (onShareTrack != null) {
                onShareTrack(item)
            } else {
                shareAudioTrack(context, item)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding(),
        ) {
            // هدر بالایی به سبک AudioPlayerAlert تلگرام
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (searchActive) {
                            searchActive = false
                            searchQuery = ""
                        } else {
                            onDismiss()
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = titleColor,
                    )
                }

                if (searchActive) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "جست‌وجوی آهنگ یا خواننده...",
                                color = subtitleColor,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = titleColor,
                                fontSize = 16.sp,
                            ),
                            cursorBrush = SolidColor(TelegramAccentBlue),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "پاک کردن",
                                tint = subtitleColor,
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                    ) {
                        Text(
                            text = track.conversationTitle?.takeIf(String::isNotBlank) ?: "لیست پخش گفتگو",
                            color = titleColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { searchActive = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "جست‌وجو",
                            tint = titleColor,
                        )
                    }
                }
            }

            HorizontalDivider(color = dividerColor)

            // لیست ترک‌های پلی‌لیست گفتگو
            if (filteredQueue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "آهنگی با این عنوان پیدا نشد",
                        color = subtitleColor,
                        fontSize = 14.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    items(filteredQueue, key = { it.id }) { item ->
                        val isCurrent = item.id == track.id
                        val isItemPlaying = isCurrent && playerState.isPlaying
                        val isItemBuffering = isCurrent && playerState.isBuffering
                        PlaylistTrackRow(
                            item = item,
                            isCurrent = isCurrent,
                            isPlaying = isItemPlaying,
                            isBuffering = isItemBuffering,
                            titleColor = titleColor,
                            subtitleColor = subtitleColor,
                            onClick = {
                                if (isCurrent) {
                                    if (playerState.isPlaying) ChatAudioPlayer.pause() else ChatAudioPlayer.resume()
                                } else {
                                    ChatAudioPlayer.playQueueItem(item)
                                }
                            },
                            onShare = { shareHandler(item) },
                            onJump = onJumpToMessage?.let { jump -> { jump(item.id) } },
                        )
                    }
                }
            }

            // پنل کنترل پایینی به سبک AudioPlayerAlert تلگرام
            Surface(
                color = panelBg,
                tonalElevation = 0.dp,
                shadowElevation = 10.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 14.dp),
                ) {
                    val activeCover = AudioCoverArtCache.rememberCoverArt(track.url, track.fallbackUrl)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = track.title,
                                transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(140))) },
                                label = "tg-player-title",
                            ) { currentTitle ->
                                Text(
                                    text = currentTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = titleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = track.artist?.takeIf(String::isNotBlank) ?: track.senderName,
                                    fontSize = 13.5.sp,
                                    color = subtitleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (abs(playerState.speed - 1.0f) >= 0.05f) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(TelegramAccentBlue.copy(alpha = 0.18f))
                                            .clickable { ChatAudioPlayer.cycleSpeed() }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "${playerState.speed}×",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TelegramAccentBlue,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(TelegramAccentBlue, TelegramAccentBlueDeep),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (activeCover != null) {
                                Image(
                                    bitmap = activeCover,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    val liveFraction = if (durationMs > 0L) {
                        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val displayedFraction = if (isScrubbing) scrubValue else liveFraction

                    // نوار جست‌وجوی باریک تلگرامی
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        TelegramThinSeekBar(
                            progress = displayedFraction,
                            enabled = durationMs > 0L,
                            isScrubbing = isScrubbing,
                            activeColor = TelegramAccentBlue,
                            trackColor = titleColor.copy(alpha = 0.16f),
                            onScrubChange = { next ->
                                isScrubbing = true
                                scrubValue = next
                            },
                            onScrubFinished = {
                                ChatAudioPlayer.seekToFraction(scrubValue)
                                isScrubbing = false
                            },
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val displayedMs = if (isScrubbing) (scrubValue * durationMs).toLong() else positionMs
                            Text(
                                text = formatAudioTime(displayedMs).toPersianDigits(),
                                fontSize = 12.sp,
                                color = subtitleColor,
                            )
                            Text(
                                text = formatAudioTime(durationMs).toPersianDigits(),
                                fontSize = 12.sp,
                                color = subtitleColor,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ردیف ۵ کنترل اصلی به سبک دقیق تلگرام
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box {
                                val modeActive = playerState.shuffle || playerState.repeatMode != ChatAudioRepeatMode.OFF
                                val modeIcon = when {
                                    playerState.repeatMode == ChatAudioRepeatMode.ONE -> Icons.Default.RepeatOne
                                    playerState.shuffle && playerState.repeatMode == ChatAudioRepeatMode.OFF -> Icons.Default.Shuffle
                                    else -> Icons.Default.Repeat
                                }
                                IconButton(onClick = { repeatMenuExpanded = true }) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = "حالت پخش",
                                        tint = if (modeActive) TelegramAccentBlue else titleColor,
                                        modifier = Modifier.size(23.dp),
                                    )
                                }
                                DropdownMenu(
                                    expanded = repeatMenuExpanded,
                                    onDismissRequest = { repeatMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("پخش تصادفی (Shuffle)") },
                                        leadingIcon = { Icon(Icons.Default.Shuffle, contentDescription = null) },
                                        trailingIcon = {
                                            if (playerState.shuffle) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = TelegramAccentBlue)
                                            }
                                        },
                                        onClick = {
                                            ChatAudioPlayer.toggleShuffle()
                                            repeatMenuExpanded = false
                                        },
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("بدون تکرار") },
                                        leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null) },
                                        trailingIcon = {
                                            if (playerState.repeatMode == ChatAudioRepeatMode.OFF) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = TelegramAccentBlue)
                                            }
                                        },
                                        onClick = {
                                            while (ChatAudioPlayer.state.value.repeatMode != ChatAudioRepeatMode.OFF) {
                                                ChatAudioPlayer.cycleRepeat()
                                            }
                                            repeatMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("تکرار کل لیست") },
                                        leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null) },
                                        trailingIcon = {
                                            if (playerState.repeatMode == ChatAudioRepeatMode.ALL) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = TelegramAccentBlue)
                                            }
                                        },
                                        onClick = {
                                            while (ChatAudioPlayer.state.value.repeatMode != ChatAudioRepeatMode.ALL) {
                                                ChatAudioPlayer.cycleRepeat()
                                            }
                                            repeatMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("تکرار همین آهنگ") },
                                        leadingIcon = { Icon(Icons.Default.RepeatOne, contentDescription = null) },
                                        trailingIcon = {
                                            if (playerState.repeatMode == ChatAudioRepeatMode.ONE) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = TelegramAccentBlue)
                                            }
                                        },
                                        onClick = {
                                            while (ChatAudioPlayer.state.value.repeatMode != ChatAudioRepeatMode.ONE) {
                                                ChatAudioPlayer.cycleRepeat()
                                            }
                                            repeatMenuExpanded = false
                                        },
                                    )
                                }
                            }

                            IconButton(onClick = { ChatAudioPlayer.previous() }) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "آهنگ قبلی",
                                    tint = titleColor,
                                    modifier = Modifier.size(30.dp),
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (playerState.isPlaying) ChatAudioPlayer.pause() else ChatAudioPlayer.resume()
                                },
                                modifier = Modifier.size(52.dp),
                            ) {
                                if (playerState.isBuffering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(26.dp),
                                        strokeWidth = 2.5.dp,
                                        color = TelegramAccentBlue,
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (playerState.isPlaying) "مکث" else "پخش",
                                        tint = titleColor,
                                        modifier = Modifier.size(34.dp),
                                    )
                                }
                            }

                            IconButton(onClick = { ChatAudioPlayer.next() }) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "آهنگ بعدی",
                                    tint = titleColor,
                                    modifier = Modifier.size(30.dp),
                                )
                            }

                            Box {
                                IconButton(onClick = { optionsMenuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "گزینه‌های بیشتر",
                                        tint = titleColor,
                                        modifier = Modifier.size(23.dp),
                                    )
                                }
                                DropdownMenu(
                                    expanded = optionsMenuExpanded,
                                    onDismissRequest = { optionsMenuExpanded = false },
                                ) {
                                    ChatAudioPlayer.SPEED_CHOICES.forEach { speedOption ->
                                        val selected = abs(playerState.speed - speedOption) < 0.05f
                                        DropdownMenuItem(
                                            text = { Text("سرعت ${speedOption}×") },
                                            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                            trailingIcon = {
                                                if (selected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = TelegramAccentBlue)
                                                }
                                            },
                                            onClick = {
                                                ChatAudioPlayer.setSpeed(speedOption)
                                                optionsMenuExpanded = false
                                            },
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("اشتراک‌گذاری") },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                        onClick = {
                                            optionsMenuExpanded = false
                                            shareHandler(track)
                                        },
                                    )
                                    if (onJumpToMessage != null) {
                                        DropdownMenuItem(
                                            text = { Text("نمایش در گفتگو") },
                                            leadingIcon = { Icon(Icons.Default.Forum, contentDescription = null) },
                                            onClick = {
                                                optionsMenuExpanded = false
                                                onJumpToMessage(track.id)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    item: ChatAudioTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    titleColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onJump: (() -> Unit)?,
) {
    val coverBitmap = AudioCoverArtCache.rememberCoverArt(item.url, item.fallbackUrl)
    var rowMenuExpanded by remember(item.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) TelegramAccentBlue.copy(alpha = 0.09f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(TelegramAccentBlue, TelegramAccentBlueDeep),
                    ),
                ),
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
                        .background(Color.Black.copy(alpha = 0.32f)),
                )
            }
            when {
                isBuffering -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                }
                else -> {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "مکث" else "پخش",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrent) TelegramAccentBlue else titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val artistLabel = item.artist?.takeIf(String::isNotBlank) ?: item.senderName
            val durationSuffix = item.durationMs.takeIf { it > 0L }?.let { " • ${formatAudioTime(it).toPersianDigits()}" }.orEmpty()
            Text(
                text = "$artistLabel$durationSuffix",
                fontSize = 13.sp,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box {
            IconButton(onClick = { rowMenuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "گزینه‌های آهنگ",
                    tint = subtitleColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = rowMenuExpanded,
                onDismissRequest = { rowMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(if (isPlaying) "مکث" else "پخش") },
                    leadingIcon = {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        rowMenuExpanded = false
                        onClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text("اشتراک‌گذاری") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = {
                        rowMenuExpanded = false
                        onShare()
                    },
                )
                if (onJump != null) {
                    DropdownMenuItem(
                        text = { Text("نمایش در گفتگو") },
                        leadingIcon = { Icon(Icons.Default.Forum, contentDescription = null) },
                        onClick = {
                            rowMenuExpanded = false
                            onJump()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TelegramThinSeekBar(
    progress: Float,
    enabled: Boolean,
    isScrubbing: Boolean,
    activeColor: Color,
    trackColor: Color,
    onScrubChange: (Float) -> Unit,
    onScrubFinished: () -> Unit,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val thumbRadius by animateDpAsState(
        targetValue = if (isScrubbing) 7.5.dp else 5.5.dp,
        animationSpec = tween(120),
        label = "tg-seek-thumb",
    )
    var dragRatio by remember { mutableFloatStateOf(clamped) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                    onScrubChange(ratio)
                    onScrubFinished()
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { startOffset ->
                        dragRatio = (startOffset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        onScrubChange(dragRatio)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragRatio = (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        onScrubChange(dragRatio)
                    },
                    onDragEnd = { onScrubFinished() },
                    onDragCancel = { onScrubFinished() },
                )
            },
    ) {
        val barHeightPx = 3.dp.toPx()
        val thumbPx = thumbRadius.toPx()
        val centerY = size.height / 2f
        val usableWidth = (size.width - thumbPx * 2f).coerceAtLeast(1f)
        val startX = thumbPx
        val fillX = startX + usableWidth * clamped

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(startX, centerY - barHeightPx / 2f),
            size = Size(usableWidth, barHeightPx),
            cornerRadius = CornerRadius(barHeightPx / 2f, barHeightPx / 2f),
        )
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(startX, centerY - barHeightPx / 2f),
            size = Size((usableWidth * clamped).coerceAtLeast(0f), barHeightPx),
            cornerRadius = CornerRadius(barHeightPx / 2f, barHeightPx / 2f),
        )
        drawCircle(
            color = activeColor,
            radius = thumbPx,
            center = Offset(fillX, centerY),
        )
    }
}

private fun shareAudioTrack(context: Context, track: ChatAudioTrack) {
    val label = listOfNotNull(
        track.title.takeIf(String::isNotBlank),
        track.artist?.takeIf(String::isNotBlank),
    ).joinToString(" - ")
    val rawUrl = track.url.trim()
    val localFile = when {
        rawUrl.startsWith("file://") -> Uri.parse(rawUrl).path?.let(::File)
        rawUrl.startsWith("/") -> File(rawUrl)
        else -> null
    }?.takeIf { it.exists() && it.canRead() }

    if (localFile != null) {
        val contentUri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.files", localFile)
        }.getOrNull()
        if (contentUri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, label)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری موسیقی").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
    }

    val shareText = buildString {
        if (label.isNotBlank()) append(label)
        val remote = track.fallbackUrl?.takeIf(String::isNotBlank)
            ?: rawUrl.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (!remote.isNullOrBlank()) {
            if (isNotEmpty()) append('\n')
            append(remote)
        }
    }.ifBlank { track.title }

    val textIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(textIntent, "اشتراک‌گذاری موسیقی").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
