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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
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

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && exoPlayer.repeatMode == Player.REPEAT_MODE_OFF) {
                    playingTrackId = null
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAudioPermission = granted || checkAudioReadPermission(context)
    }

    val audioDocumentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            coroutineScope.launch {
                val extracted = extractAudioTrackFromUri(context, uri)
                localDeviceTracks = listOf(extracted) + localDeviceTracks.filterNot { it.musicUrl == extracted.musicUrl }
                stagedTrack = extracted
                editTitle = extracted.title
                editArtist = extracted.artist
                clipStartFraction = 0f
                playClipPreview(
                    exoPlayer = exoPlayer,
                    track = extracted,
                    startMs = 0,
                    endMs = resolveEndMs(extracted.durationMs, 0, clipDurationSec),
                )
                playingTrackId = extracted.id
            }
        }
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
        val endMs = resolveEndMs(track.durationMs, 0, clipDurationSec)
        playClipPreview(exoPlayer, track, 0, endMs)
        playingTrackId = track.id
    }

    ModalBottomSheet(
        onDismissRequest = {
            exoPlayer.stop()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color(0xF5131520),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 420.dp, max = 640.dp)
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

                // Direct File Picker Action Card on Tab 0 ("گوشی من")
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
                                audioDocumentPicker.launch(arrayOf("audio/*"))
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
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "انتخاب فایل صوتی از پوشه‌ها و دانلودها",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                                Text(
                                    text = "پشتیبانی از MP3، M4A، AAC، WAV و فایل‌های ذخیره شده تلگرام",
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
                                    text = "برای نمایش خودکار تمام آهنگ‌های موجود در حافظه گوشی، دسترسی خواندن فایل‌های صوتی را فعال کنید یا از دکمه بالا فایل دلخواه را انتخاب نمایید.",
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
                                        selectedTab == 0 -> "آهنگی در پوشه موسیقی گوشی یافت نشد؛ از دکمه انتخاب فایل بالا استفاده کنید"
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
                                            Text(
                                                text = track.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
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

                // Interactive Chorus Trimmer & Confirmation Dock
                stagedTrack?.let { active ->
                    val totalDurMs = active.durationMs.takeIf { it > 0L }
                        ?: exoPlayer.duration.takeIf { it > 0L }
                        ?: 180_000L
                    val windowMs = (clipDurationSec * 1_000L).coerceAtMost(totalDurMs)
                    val maxStartMs = (totalDurMs - windowMs).coerceAtLeast(0L)
                    val computedStartMs = (clipStartFraction * maxStartMs).roundToInt()
                    val computedEndMs = (computedStartMs + windowMs.toInt()).coerceAtMost(totalDurMs.toInt())

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1B1E2E))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "بخش انتخابی: ${formatMillisPersian(computedStartMs.toLong())} تا ${formatMillisPersian(computedEndMs.toLong())}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(15, 30, 45, 60).forEach { sec ->
                                    val sel = clipDurationSec == sec
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (sel) VistaBrandColors.Pink else Color.White.copy(alpha = 0.08f))
                                            .clickable {
                                                clipDurationSec = sec
                                                val newWindow = (sec * 1_000L).coerceAtMost(totalDurMs)
                                                val newStart = (clipStartFraction * (totalDurMs - newWindow).coerceAtLeast(0L)).roundToInt()
                                                playClipPreview(
                                                    exoPlayer = exoPlayer,
                                                    track = active,
                                                    startMs = newStart,
                                                    endMs = (newStart + newWindow.toInt()).coerceAtMost(totalDurMs.toInt()),
                                                )
                                                playingTrackId = active.id
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = "${sec.toString().toPersianDigits()}ث",
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }

                        if (maxStartMs > 1_000L) {
                            Slider(
                                value = clipStartFraction,
                                onValueChange = { frac ->
                                    clipStartFraction = frac.coerceIn(0f, 1f)
                                },
                                onValueChangeFinished = {
                                    val newStart = (clipStartFraction * maxStartMs).roundToInt()
                                    val newEnd = (newStart + windowMs.toInt()).coerceAtMost(totalDurMs.toInt())
                                    playClipPreview(exoPlayer, active, newStart, newEnd)
                                    playingTrackId = active.id
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = VistaBrandColors.Pink,
                                    activeTrackColor = VistaBrandColors.Pink,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp),
                            )
                        }

                        if (active.isLocalDeviceFile) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editTitle,
                                    onValueChange = { editTitle = it },
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
                                    onValueChange = { editArtist = it },
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
                                    .clickable { saveToVistaLibrary = !saveToVistaLibrary }
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

                        Button(
                            onClick = {
                                exoPlayer.stop()
                                val finalTrack = active.copy(
                                    title = editTitle.trim().ifBlank { active.title.ifBlank { "موزیک" } },
                                    artist = editArtist.trim().ifBlank { active.artist.ifBlank { "موزیک انتخابی" } },
                                )
                                if (onRichMusicSelected != null) {
                                    onRichMusicSelected(
                                        SelectedStoryMusicResult(
                                            track = finalTrack,
                                            startMs = computedStartMs,
                                            endMs = computedEndMs,
                                            saveToVistaLibrary = active.isLocalDeviceFile && saveToVistaLibrary,
                                        ),
                                    )
                                } else {
                                    onMusicSelected(finalTrack)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VistaBrandColors.Pink,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 11.dp),
                        ) {
                            Text(
                                text = "افزودن موزیک به استوری",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                            )
                        }
                    }
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

private fun playClipPreview(
    exoPlayer: ExoPlayer,
    track: MusicTrack,
    startMs: Int,
    endMs: Int,
) {
    runCatching {
        exoPlayer.stop()
        val baseItem = MediaItem.fromUri(Uri.parse(track.musicUrl))
        val clipped = if (startMs > 0 || endMs > startMs) {
            val builder = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs.coerceAtLeast(0).toLong())
            if (endMs > startMs) {
                builder.setEndPositionMs(endMs.toLong())
            }
            baseItem.buildUpon()
                .setClippingConfiguration(builder.build())
                .build()
        } else {
            baseItem
        }
        exoPlayer.setMediaItem(clipped)
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.prepare()
        exoPlayer.play()
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
        val selection = "${MediaStore.Audio.Media.DURATION} >= 4000"
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
            while (cursor.moveToNext() && count < 150) {
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

    var metaTitle = ""
    var metaArtist = ""
    var durationMs = 0L
    var embeddedCoverUri: String? = null

    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim().orEmpty()
        metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim().orEmpty()
        durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val picBytes = retriever.embeddedPicture
        if (picBytes != null && picBytes.isNotEmpty()) {
            val coverDir = File(context.cacheDir, "camera").apply { mkdirs() }
            val coverFile = File(coverDir, "audio_cover_${System.currentTimeMillis()}.jpg")
            coverFile.writeBytes(picBytes)
            embeddedCoverUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                coverFile,
            ).toString()
        }
    } catch (_: Exception) {
        // Ignore corrupt ID3 frames
    } finally {
        runCatching { retriever.release() }
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
        musicUrl = uri.toString(),
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
