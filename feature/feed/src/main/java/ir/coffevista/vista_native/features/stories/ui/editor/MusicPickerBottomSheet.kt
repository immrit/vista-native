package ir.coffevista.vista_native.features.stories.ui.editor

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.feed.music.MusicRepository
import ir.coffevista.vista_native.features.feed.music.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

data class SelectedStoryMusicResult(
    val track: MusicTrack,
    val startMs: Int = 0,
    val endMs: Int = 0,
    val saveToVistaLibrary: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPickerBottomSheet(
    musicRepository: MusicRepository,
    onMusicSelected: (MusicTrack) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    currentUserId: String = "",
    onRichMusicSelected: ((SelectedStoryMusicResult) -> Unit)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 0 = گوشی من (Local Device Audio), 1 = موزیک‌های من (User's Vista Library), 2 = ویترین ویستا (Catalog)
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var hasAudioPermission by remember {
        mutableStateOf(checkAudioReadPermission(context))
    }
    var localDeviceTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var isLoadingLocal by remember { mutableStateOf(false) }

    var myUploadedTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var isLoadingMine by remember { mutableStateOf(false) }

    var catalogTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var isLoadingCatalog by remember { mutableStateOf(false) }
    var catalogError by remember { mutableStateOf<String?>(null) }

    // Active preview & chorus trimming state
    var playingTrackId by remember { mutableStateOf<String?>(null) }
    var stagedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editArtist by remember { mutableStateOf("") }
    var clipDurationSec by remember { mutableIntStateOf(30) } // 15, 30, 45, 60
    var clipStartFraction by remember { mutableFloatStateOf(0f) }
    var saveToVistaLibrary by remember { mutableStateOf(false) }
    var playbackPositionMs by remember { mutableLongStateOf(0L) }
    var isExoPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val d = exoPlayer.duration
                    if (d > 0L && stagedTrack != null && (stagedTrack?.durationMs ?: 0L) <= 0L) {
                        stagedTrack = stagedTrack?.copy(durationMs = d)
                    }
                } else if (state == Player.STATE_ENDED) {
                    playingTrackId = null
                    isExoPlaying = false
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isExoPlaying = isPlaying
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicPicker", "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}", error)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            runCatching { exoPlayer.removeListener(listener) }
            runCatching { exoPlayer.stop() }
            runCatching { exoPlayer.release() }
        }
    }

    fun playPreviewAt(track: MusicTrack, startMs: Long) {
        runCatching {
            val targetUri = Uri.parse(track.musicUrl)
            val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri

            if (currentUri != targetUri) {
                exoPlayer.stop()
                val mediaItem = MediaItem.fromUri(targetUri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }
            exoPlayer.seekTo(startMs.coerceAtLeast(0L))
            exoPlayer.play()
            playingTrackId = track.id
        }.onFailure { err ->
            android.util.Log.e("MusicPicker", "playPreviewAt error: ${err.message}", err)
        }
    }

    // Active looping playback between start and end bounds
    LaunchedEffect(playingTrackId, stagedTrack?.id) {
        if (playingTrackId == null) return@LaunchedEffect
        val active = stagedTrack ?: return@LaunchedEffect
        while (isActive) {
            delay(30L)
            if (exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition
                playbackPositionMs = pos
                val total = (stagedTrack?.durationMs?.takeIf { it > 0L }
                    ?: exoPlayer.duration.takeIf { it > 0L }
                    ?: 180_000L)
                val winMs = (clipDurationSec * 1_000L).coerceAtMost(total)
                val maxSt = (total - winMs).coerceAtLeast(0L)
                val startBound = (clipStartFraction * maxSt).toLong()
                val endBound = (startBound + winMs).coerceAtMost(total)

                if (pos >= endBound || pos < startBound - 500L) {
                    exoPlayer.seekTo(startBound)
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAudioPermission = granted || checkAudioReadPermission(context)
    }

    // Load Local Device Music when granted
    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            isLoadingLocal = true
            localDeviceTracks = queryDeviceMusicTracks(context)
            isLoadingLocal = false
        }
    }

    // Load User's Vista Library when tab 1 opened
    LaunchedEffect(selectedTab, currentUserId) {
        if (selectedTab == 1 && currentUserId.isNotBlank() && myUploadedTracks.isEmpty()) {
            isLoadingMine = true
            musicRepository.fetchUserMusics(userId = currentUserId, limit = 50)
                .onSuccess { myUploadedTracks = it }
            isLoadingMine = false
        } else if (selectedTab == 2 && catalogTracks.isEmpty()) {
            isLoadingCatalog = true
            catalogError = null
            musicRepository.fetchMusics(limit = 50, offset = 0)
                .onSuccess { catalogTracks = it }
                .onFailure { catalogError = it.message ?: "خطا در دریافت لیست موسیقی" }
            isLoadingCatalog = false
        }
    }

    val activeSourceList = when (selectedTab) {
        0 -> localDeviceTracks
        1 -> myUploadedTracks
        else -> catalogTracks
    }

    val filteredTracks = remember(activeSourceList, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) activeSourceList
        else activeSourceList.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.genres.any { g -> g.contains(q, ignoreCase = true) }
        }
    }

    fun selectTrackForTrimming(track: MusicTrack) {
        stagedTrack = track
        editTitle = track.title
        editArtist = track.artist
        clipStartFraction = 0f
        playPreviewAt(track, 0L)

        if (track.durationMs <= 0L && !track.musicUrl.isNullOrBlank()) {
            coroutineScope.launch {
                val accurateMs = ir.coffevista.vista_native.features.feed.music.AudioTrimEngine.getExactAudioDurationMs(
                    context,
                    Uri.parse(track.musicUrl),
                )
                if (accurateMs > 0L) {
                    val updated = track.copy(durationMs = accurateMs)
                    if (stagedTrack?.id == track.id) {
                        stagedTrack = updated
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            exoPlayer.stop()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color(0xF5131520),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AnimatedContent(
                targetState = stagedTrack,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "MusicPickerStepTransition",
            ) { activeTrack ->
                if (activeTrack == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 460.dp, max = 640.dp)
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp),
                    ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(VistaBrandColors.Pink.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = VistaBrandColors.Pink,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "انتخاب موزیک استوری",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                text = "از موزیک‌های داخل گوشی یا آرشیو ویستا انتخاب کنید",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            exoPlayer.stop()
                            onDismiss()
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.White.copy(alpha = 0.75f),
                        )
                    }
                }

                // 3 Smart Source Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MusicTabPill(
                        label = "گوشی من",
                        icon = Icons.Default.PhoneAndroid,
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f),
                    )
                    MusicTabPill(
                        label = "موزیک‌های من",
                        icon = Icons.Default.Person,
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f),
                    )
                    MusicTabPill(
                        label = "ویترین ویستا",
                        icon = Icons.Default.GraphicEq,
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = when (selectedTab) {
                                0 -> "جستجو در آهنگ‌های گوشی شما…"
                                1 -> "جستجو در موزیک‌های آپلودشده من…"
                                else -> "جستجوی نام آهنگ یا خواننده…"
                            },
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.45f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(19.dp),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = VistaBrandColors.Pink,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    singleLine = true,
                )

                // In-App Device Audio Refresh Card on Tab 0 ("گوشی من")
                if (selectedTab == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        VistaBrandColors.Pink.copy(alpha = 0.24f),
                                        VistaBrandColors.Indigo.copy(alpha = 0.22f),
                                    ),
                                ),
                            )
                            .border(1.dp, VistaBrandColors.Pink.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable {
                                if (hasAudioPermission) {
                                    coroutineScope.launch {
                                        isLoadingLocal = true
                                        localDeviceTracks = queryDeviceMusicTracks(context)
                                        isLoadingLocal = false
                                    }
                                } else {
                                    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Manifest.permission.READ_MEDIA_AUDIO
                                    } else {
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    }
                                    audioPermissionLauncher.launch(perm)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(VistaBrandColors.Pink.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = if (hasAudioPermission) {
                                        "بازخوانی لیست آهنگ‌های حافظه گوشی"
                                    } else {
                                        "نمایش تمام فایل‌های صوتی گوشی درون برنامه"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                                Text(
                                    text = "نمایش مستقیم MP3، M4A، AAC، WAV با قابلیت جستجو و پیش‌نمایش",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.AudioFile,
                            contentDescription = null,
                            tint = VistaBrandColors.Pink,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Track List Region
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    when {
                        selectedTab == 0 && !hasAudioPermission && localDeviceTracks.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = "برای نمایش خودکار تمام آهنگ‌های موجود در حافظه گوشی، دسترسی خواندن فایل‌های صوتی را فعال کنید.",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 12.5.sp,
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            Manifest.permission.READ_MEDIA_AUDIO
                                        } else {
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        }
                                        audioPermissionLauncher.launch(perm)
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Pink),
                                ) {
                                    Text("نمایش همه آهنگ‌های گوشی", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        (selectedTab == 0 && isLoadingLocal) ||
                            (selectedTab == 1 && isLoadingMine) ||
                            (selectedTab == 2 && isLoadingCatalog) -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = VistaBrandColors.Pink, modifier = Modifier.size(34.dp))
                            }
                        }

                        selectedTab == 2 && catalogError != null && catalogTracks.isEmpty() -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = catalogError.orEmpty(),
                                    color = Color(0xFFFF5252),
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            isLoadingCatalog = true
                                            catalogError = null
                                            musicRepository.fetchMusics(limit = 50, offset = 0)
                                                .onSuccess { catalogTracks = it }
                                                .onFailure { catalogError = it.message }
                                            isLoadingCatalog = false
                                        }
                                    },
                                ) {
                                    Text("تلاش مجدد", color = Color.White)
                                }
                            }
                        }

                        filteredTracks.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = when {
                                        searchQuery.isNotBlank() -> "موزیکی با این عبارت پیدا نشد"
                                        selectedTab == 0 -> "آهنگی در حافظه گوشی یافت نشد"
                                        selectedTab == 1 -> "هنوز موزیکی در حساب خود ذخیره نکرده‌اید؛ از تب «گوشی من» انتخاب کنید"
                                        else -> "موزیکی موجود نیست"
                                    },
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.5.sp,
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(filteredTracks, key = { it.id.ifBlank { it.musicUrl } }) { track ->
                                    val isSelected = stagedTrack?.id == track.id
                                    val isPlaying = playingTrackId == track.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) VistaBrandColors.Pink.copy(alpha = 0.16f)
                                                else Color.White.copy(alpha = 0.05f),
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) VistaBrandColors.Pink.copy(alpha = 0.55f)
                                                else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(14.dp),
                                            )
                                            .clickable { selectTrackForTrimming(track) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(Color.White.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (!track.coverUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = track.coverUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = VistaBrandColors.Pink,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(11.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    text = track.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                )
                                                if (isPlaying) {
                                                    MiniAudioEqualizer()
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    text = track.artist,
                                                    fontSize = 11.5.sp,
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                )
                                                if (track.durationMs > 0L) {
                                                    Text(
                                                        text = "• ${formatMillisPersian(track.durationMs)}",
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.5f),
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                if (isPlaying) {
                                                    exoPlayer.pause()
                                                    playingTrackId = null
                                                } else {
                                                    selectTrackForTrimming(track)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isPlaying) VistaBrandColors.Pink
                                                    else Color.White.copy(alpha = 0.1f),
                                                ),
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "توقف" else "پخش",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            StoryMusicTrimmerView(
                        active = activeTrack,
                        totalDurMs = activeTrack.durationMs.takeIf { it > 0L }
                            ?: exoPlayer.duration.takeIf { it > 0L }
                            ?: 180_000L,
                        clipDurationSec = clipDurationSec,
                        clipStartFraction = clipStartFraction,
                        playbackPositionMs = playbackPositionMs,
                        isPlaying = playingTrackId == activeTrack.id && isExoPlaying,
                        editTitle = editTitle,
                        editArtist = editArtist,
                        saveToVistaLibrary = saveToVistaLibrary,
                        onTitleChange = { editTitle = it },
                        onArtistChange = { editArtist = it },
                        onSaveToLibraryChange = { saveToVistaLibrary = it },
                        onBackToSelection = {
                            exoPlayer.stop()
                            playingTrackId = null
                            stagedTrack = null
                        },
                        onDurationCycle = {
                            val durations = listOf(15, 30, 45, 60)
                            val nextIdx = (durations.indexOf(clipDurationSec) + 1) % durations.size
                            val newDur = durations[nextIdx]
                            clipDurationSec = newDur
                            val totalMs = activeTrack.durationMs.takeIf { it > 0L }
                                ?: exoPlayer.duration.takeIf { it > 0L }
                                ?: 180_000L
                            val newWin = (newDur * 1_000L).coerceAtMost(totalMs)
                            val newStart = (clipStartFraction * (totalMs - newWin).coerceAtLeast(0L)).roundToInt()
                            playPreviewAt(activeTrack, newStart.toLong())
                        },
                        onTogglePlay = {
                            val isCurPlaying = playingTrackId == activeTrack.id && isExoPlaying
                            if (isCurPlaying) {
                                exoPlayer.pause()
                                playingTrackId = null
                            } else {
                                val totalMs = activeTrack.durationMs.takeIf { it > 0L }
                                    ?: exoPlayer.duration.takeIf { it > 0L }
                                    ?: 180_000L
                                val winMs = (clipDurationSec * 1_000L).coerceAtMost(totalMs)
                                val curStart = (clipStartFraction * (totalMs - winMs).coerceAtLeast(0L)).roundToInt()
                                playPreviewAt(activeTrack, curStart.toLong())
                            }
                        },
                        onFractionChange = { frac ->
                            clipStartFraction = frac
                        },
                        onFractionChangeFinished = {
                            val totalMs = activeTrack.durationMs.takeIf { it > 0L }
                                ?: exoPlayer.duration.takeIf { it > 0L }
                                ?: 180_000L
                            val winMs = (clipDurationSec * 1_000L).coerceAtMost(totalMs)
                            val curStart = (clipStartFraction * (totalMs - winMs).coerceAtLeast(0L)).roundToInt()
                            playPreviewAt(activeTrack, curStart.toLong())
                        },
                        onConfirmSelection = {
                            exoPlayer.stop()
                            val finalTrack = activeTrack.copy(
                                title = editTitle.trim().ifBlank { activeTrack.title.ifBlank { "موزیک" } },
                                artist = editArtist.trim().ifBlank { activeTrack.artist.ifBlank { "موزیک انتخابی" } },
                            )
                            val totalMs = activeTrack.durationMs.takeIf { it > 0L }
                                ?: exoPlayer.duration.takeIf { it > 0L }
                                ?: 180_000L
                            val winMs = (clipDurationSec * 1_000L).coerceAtMost(totalMs)
                            val maxStartMs = (totalMs - winMs).coerceAtLeast(0L)
                            val computedStartMs = (clipStartFraction * maxStartMs).roundToInt()
                            val computedEndMs = (computedStartMs + winMs.toInt()).coerceAtMost(totalMs.toInt())

                            if (onRichMusicSelected != null) {
                                onRichMusicSelected(
                                    SelectedStoryMusicResult(
                                        track = finalTrack,
                                        startMs = computedStartMs,
                                        endMs = computedEndMs,
                                        saveToVistaLibrary = activeTrack.isLocalDeviceFile && saveToVistaLibrary,
                                    ),
                                )
                            } else {
                                onMusicSelected(finalTrack)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicTabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) VistaBrandColors.Pink
                else Color.White.copy(alpha = 0.07f),
            )
            .border(
                width = 0.8.dp,
                color = if (selected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MiniAudioEqualizer(
    color: Color = VistaBrandColors.Pink,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq1",
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq2",
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq3",
    )
    Row(
        modifier = modifier.height(13.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(h1, h2, h3).forEach { h ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((13 * h).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun StoryMusicTrimmerView(
    active: MusicTrack,
    totalDurMs: Long,
    clipDurationSec: Int,
    clipStartFraction: Float,
    playbackPositionMs: Long,
    isPlaying: Boolean,
    editTitle: String,
    editArtist: String,
    saveToVistaLibrary: Boolean,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onSaveToLibraryChange: (Boolean) -> Unit,
    onBackToSelection: () -> Unit,
    onDurationCycle: () -> Unit,
    onTogglePlay: () -> Unit,
    onFractionChange: (Float) -> Unit,
    onFractionChangeFinished: () -> Unit,
    onConfirmSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowMs = (clipDurationSec * 1_000L).coerceAtMost(totalDurMs)
    val maxStartMs = (totalDurMs - windowMs).coerceAtLeast(0L)
    val computedStartMs = (clipStartFraction * maxStartMs).roundToInt()
    val computedEndMs = (computedStartMs + windowMs.toInt()).coerceAtMost(totalDurMs.toInt())

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Top Bar: Back ("تغییر قطعه") | Center Drag Handle | Done ("انجام شد")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackToSelection)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت به لیست",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    text = "تغییر قطعه",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
            )

            Text(
                text = "انجام شد",
                color = Color(0xFF60A5FA),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onConfirmSelection)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(4.dp))

        // Center Album Art (rounded square)
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (!active.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = active.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = VistaBrandColors.Pink,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        // Title and Artist
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                text = if (active.isLocalDeviceFile && editTitle.isNotBlank()) editTitle else active.title.ifBlank { "موزیک انتخابی" },
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (active.isLocalDeviceFile && editArtist.isNotBlank()) editArtist else active.artist.ifBlank { "ناشناس" },
                fontSize = 13.5.sp,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(6.dp))

        // Trimmer Controls Row: [Duration Circle]  ---------[Thumb]---------  [White Play/Pause Circle]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Duration Circle Badge (cycling 15 -> 30 -> 45 -> 60)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onDurationCycle),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = clipDurationSec.toString().toPersianDigits(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Timeline Track with Chorus Dots and Moving White Thumb
            val windowFraction = if (totalDurMs > 0L) {
                (clipDurationSec * 1000f / totalDurMs).coerceIn(0.06f, 1f)
            } else 0.25f
            val maxStartFrac = (1f - windowFraction).coerceAtLeast(0.001f)
            val startRatio = (clipStartFraction * maxStartFrac).coerceIn(0f, 1f)
            val currentTimelineFrac = rememberUpdatedState(clipStartFraction)

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                        .pointerInput(maxStartFrac, windowFraction) {
                            var accumulated = currentTimelineFrac.value
                            detectDragGestures(
                                onDragStart = { offset ->
                                    accumulated = currentTimelineFrac.value
                                    val tapFrac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    accumulated = ((tapFrac - windowFraction / 2f) / maxStartFrac).coerceIn(0f, 1f)
                                    onFractionChange(accumulated)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = dragAmount.x / (size.width.toFloat() * maxStartFrac)
                                    accumulated = (accumulated + delta).coerceIn(0f, 1f)
                                    onFractionChange(accumulated)
                                },
                                onDragEnd = { onFractionChangeFinished() },
                                onDragCancel = { onFractionChangeFinished() },
                            )
                        },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    val trackWidth = maxWidth

                    // Background Track Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                    )

                    // Chorus highlight dots
                    listOf(0.24f, 0.48f, 0.74f).forEach { dotPos ->
                        val dotX = trackWidth * dotPos - 3.5.dp
                        Box(
                            modifier = Modifier
                                .offset(x = dotX.coerceAtLeast(0.dp))
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF2570))
                                .clickable {
                                    val newFrac = ((dotPos - windowFraction / 2f) / maxStartFrac).coerceIn(0f, 1f)
                                    onFractionChange(newFrac)
                                    onFractionChangeFinished()
                                },
                        )
                    }

                    // White Pill Thumb
                    val thumbOffset = ((trackWidth - 18.dp) * startRatio).coerceIn(0.dp, trackWidth - 18.dp)
                    Box(
                        modifier = Modifier
                            .offset(x = thumbOffset)
                            .width(18.dp)
                            .height(7.dp)
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(Color.White),
                    )
                }
            }

            // Circular Play/Stop Action Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF1B1B1E)),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "پخش",
                        tint = Color(0xFF1B1B1E),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Waveform Audio Scrubber
        InteractiveWaveformScrubber(
            track = active,
            totalDurationMs = totalDurMs,
            clipDurationSec = clipDurationSec,
            clipStartFraction = clipStartFraction,
            playbackPositionMs = playbackPositionMs,
            isPlaying = isPlaying,
            onFractionChange = onFractionChange,
            onFractionChangeFinished = onFractionChangeFinished,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )

        // Time Range Persian Indicator
        Text(
            text = "بخش انتخابی: ${formatMillisPersian(computedStartMs.toLong())} تا ${formatMillisPersian(computedEndMs.toLong())}",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
        )

        // Local File Edit Fields
        if (active.isLocalDeviceFile) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = onTitleChange,
                    placeholder = { Text("نام آهنگ", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedBorderColor = VistaBrandColors.Pink,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = editArtist,
                    onValueChange = onArtistChange,
                    placeholder = { Text("نام خواننده", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedBorderColor = VistaBrandColors.Pink,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSaveToLibraryChange(!saveToVistaLibrary) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            if (saveToVistaLibrary) VistaBrandColors.Pink
                            else Color.White.copy(alpha = 0.1f),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (saveToVistaLibrary) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                Text(
                    text = "ذخیره در «موزیک‌های من» برای استفاده‌های بعدی",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.5.sp,
                )
            }
        }

        // Primary Bottom Action Button
        Button(
            onClick = onConfirmSelection,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VistaBrandColors.Pink,
                contentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Text(
                text = "افزودن موزیک به استوری",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun InteractiveWaveformScrubber(
    track: MusicTrack,
    totalDurationMs: Long,
    clipDurationSec: Int,
    clipStartFraction: Float,
    playbackPositionMs: Long,
    isPlaying: Boolean,
    onFractionChange: (Float) -> Unit,
    onFractionChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val currentClipStartFraction = rememberUpdatedState(clipStartFraction)
    val windowFraction = if (totalDurationMs > 0L) {
        (clipDurationSec * 1000f / totalDurationMs).coerceIn(0.06f, 1f)
    } else 0.25f

    val barCount = 44
    val seed = remember(track.id) { (track.id.hashCode() xor track.title.hashCode()).toFloat() }
    val barHeights = remember(seed) {
        List(barCount) { i ->
            val norm = i / (barCount - 1f)
            val envelope = kotlin.math.sin(norm * Math.PI).toFloat().coerceAtLeast(0.35f)
            val variation = kotlin.math.sin(i * 0.72f + seed) * kotlin.math.cos(i * 0.38f + 1.2f)
            (0.24f + 0.76f * envelope * (0.55f + 0.45f * kotlin.math.abs(variation).toFloat())).coerceIn(0.2f, 1f)
        }
    }

    val maxStartFrac = (1f - windowFraction).coerceAtLeast(0.001f)
    val startRatio = (clipStartFraction * maxStartFrac).coerceIn(0f, 1f)
    val endRatio = (startRatio + windowFraction).coerceIn(0f, 1f)

    val storyAccentGradient = Brush.horizontalGradient(
        listOf(
            Color(0xFFFFD200),
            Color(0xFFFF3D00),
            Color(0xFFD300C5),
            Color(0xFF7B00FF),
        ),
    )

    // Direction-safe: strictly Left-to-Right layout so audio flows 00:00 (Left) to End (Right)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF10121D))
                .pointerInput(totalDurationMs, windowFraction, maxStartFrac) {
                    var accumulatedFraction = currentClipStartFraction.value
                    detectDragGestures(
                        onDragStart = { offset ->
                            accumulatedFraction = currentClipStartFraction.value
                            val tapXFrac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val curStart = accumulatedFraction * maxStartFrac
                            val curEnd = curStart + windowFraction
                            // Center window on tap if outside
                            if (tapXFrac < curStart || tapXFrac > curEnd) {
                                accumulatedFraction = ((tapXFrac - windowFraction / 2f) / maxStartFrac).coerceIn(0f, 1f)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onFractionChange(accumulatedFraction)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaFrac = dragAmount.x / (size.width.toFloat() * maxStartFrac)
                            val updated = (accumulatedFraction + deltaFrac).coerceIn(0f, 1f)
                            if (kotlin.math.abs(updated - accumulatedFraction) > 0.005f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            accumulatedFraction = updated
                            onFractionChange(updated)
                        },
                        onDragEnd = {
                            onFractionChangeFinished()
                        },
                        onDragCancel = {
                            onFractionChangeFinished()
                        },
                    )
                }
                .padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            val availableWidth = maxWidth

            // 1. Draw Waveform Bars (LTR: index 0 is left, barCount-1 is right)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                barHeights.forEachIndexed { index, h ->
                    val barProgress = index.toFloat() / (barCount - 1f)
                    val inWindow = barProgress in startRatio..endRatio
                    val hasChorusDot = !inWindow && (index == 10 || index == 22 || index == 33)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(h)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (inWindow) Color.White
                                    else Color.White.copy(alpha = 0.28f),
                                ),
                        )
                        if (hasChorusDot) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF2570)),
                            )
                        }
                    }
                }
            }

            // 2. Selection Window Frame Overlay (Strictly LTR: left = startRatio * availableWidth)
            val windowLeft = availableWidth * startRatio
            val windowWidth = (availableWidth * windowFraction).coerceAtMost(availableWidth - windowLeft)

            Box(
                modifier = Modifier
                    .offset(x = windowLeft)
                    .width(windowWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        3.dp,
                        storyAccentGradient,
                        RoundedCornerShape(12.dp),
                    ),
            ) {
                // Left handle grip
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp)
                        .width(3.5.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White),
                )
                // Right handle grip
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp)
                        .width(3.5.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White),
                )
            }

            // 3. Playback Needle (strictly kept inside the selection window)
            if (isPlaying && totalDurationMs > 0L) {
                val needleFraction = (playbackPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(startRatio, endRatio)
                val needleOffset = (availableWidth * needleFraction).coerceIn(0.dp, availableWidth - 2.5.dp)
                Box(
                    modifier = Modifier
                        .offset(x = needleOffset)
                        .width(2.5.dp)
                        .fillMaxHeight(0.85f)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.White),
                )
            }
        }
    }
}

private fun resolveEndMs(durationMs: Long, startMs: Int, windowSec: Int): Int {
    val windowMs = windowSec * 1_000
    return if (durationMs > 0L) {
        (startMs + windowMs).coerceAtMost(durationMs.toInt())
    } else {
        startMs + windowMs
    }
}

private fun formatMillisPersian(ms: Long): String {
    val totalSec = (ms / 1_000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "%02d:%02d".format(mins, secs).toPersianDigits()
}

private fun checkAudioReadPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }
}

private suspend fun queryDeviceMusicTracks(context: Context): List<MusicTrack> = withContext(Dispatchers.IO) {
    runCatching {
        val out = ArrayList<MusicTrack>(120)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selection = "${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val albumArtBase = Uri.parse("content://media/external/audio/albumart")

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            var count = 0
            while (cursor.moveToNext() && count < 250) {
                val id = cursor.getLong(idCol)
                val rawTitle = cursor.getString(titleCol)?.trim().orEmpty()
                val rawArtist = cursor.getString(artistCol)?.trim().orEmpty()
                val displayName = cursor.getString(nameCol)?.substringBeforeLast('.')?.trim().orEmpty()
                val albumId = cursor.getLong(albumCol)
                val durationMs = cursor.getLong(durCol).coerceAtLeast(0L)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id,
                )
                val cleanArtist = if (rawArtist.isBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) {
                    "آهنگ داخل گوشی"
                } else {
                    rawArtist
                }
                val cleanTitle = rawTitle.ifBlank { displayName.ifBlank { "موزیک ${count + 1}" } }
                val coverUriStr = if (albumId > 0L) {
                    ContentUris.withAppendedId(albumArtBase, albumId).toString()
                } else {
                    null
                }

                out += MusicTrack(
                    id = "local_audio_$id",
                    userId = "",
                    title = cleanTitle,
                    artist = cleanArtist,
                    coverUrl = coverUriStr,
                    musicUrl = contentUri.toString(),
                    createdAt = "",
                    playCount = 0,
                    username = "",
                    avatarUrl = "",
                    isVerified = false,
                    genres = emptyList(),
                    durationMs = durationMs,
                    isLocalDeviceFile = true,
                )
                count++
            }
        }
        out
    }.getOrDefault(emptyList())
}

private suspend fun extractAudioTrackFromUri(context: Context, uri: Uri): MusicTrack = withContext(Dispatchers.IO) {
    var fileDisplayName = ""
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                fileDisplayName = c.getString(0)?.substringBeforeLast('.')?.trim().orEmpty()
            }
        }
    }

    // Persist transient SAF audio stream into permanent local file
    val safeBaseName = fileDisplayName.ifBlank { "audio_${System.currentTimeMillis()}" }
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
    val uploadsDir = File(context.cacheDir, "story_uploads").apply { mkdirs() }
    val localAudioFile = File(uploadsDir, "picked_${System.currentTimeMillis()}_${safeBaseName}.mp3")

    var finalAudioUri: Uri = uri
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { inStream ->
            localAudioFile.outputStream().use { outStream ->
                inStream.copyTo(outStream)
            }
        }
        if (localAudioFile.exists() && localAudioFile.length() > 0L) {
            finalAudioUri = Uri.fromFile(localAudioFile)
        }
    }

    var metaTitle = ""
    var metaArtist = ""
    var durationMs = 0L
    var embeddedCoverUri: String? = null

    val retriever = MediaMetadataRetriever()
    try {
        if (localAudioFile.exists() && localAudioFile.length() > 0L) {
            retriever.setDataSource(localAudioFile.absolutePath)
        } else {
            retriever.setDataSource(context, uri)
        }
        metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim().orEmpty()
        metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim().orEmpty()
        durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val picBytes = retriever.embeddedPicture
        if (picBytes != null && picBytes.isNotEmpty()) {
            val coverDir = File(context.cacheDir, "camera").apply { mkdirs() }
            val coverFile = File(coverDir, "audio_cover_${System.currentTimeMillis()}.jpg")
            coverFile.writeBytes(picBytes)
            embeddedCoverUri = runCatching {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.files",
                    coverFile,
                ).toString()
            }.getOrElse {
                Uri.fromFile(coverFile).toString()
            }
        }
    } catch (_: Exception) {
        // Ignore corrupt ID3 frames
    } finally {
        runCatching { retriever.release() }
    }

    if (durationMs <= 0L) {
        val exactMs = ir.coffevista.vista_native.features.feed.music.AudioTrimEngine.getExactAudioDurationMs(context, finalAudioUri)
        if (exactMs > 0L) {
            durationMs = exactMs
        }
    }

    MusicTrack(
        id = "picked_audio_${UUID.randomUUID()}",
        userId = "",
        title = metaTitle.ifBlank { fileDisplayName.ifBlank { "موزیک انتخابی" } },
        artist = if (metaArtist.isBlank() || metaArtist.equals("<unknown>", ignoreCase = true)) {
            "فایل صوتی من"
        } else {
            metaArtist
        },
        coverUrl = embeddedCoverUri,
        musicUrl = finalAudioUri.toString(),
        createdAt = "",
        playCount = 0,
        username = "",
        avatarUrl = "",
        isVerified = false,
        genres = emptyList(),
        durationMs = durationMs,
        isLocalDeviceFile = true,
    )
}
