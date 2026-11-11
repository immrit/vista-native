package ir.coffevista.vista_native.core.designsystem.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassScrollSync
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassSource
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class VistaInAppPickerMode {
    IMAGE_SINGLE,
    IMAGE_MULTI,
    VIDEO_SINGLE,
    IMAGE_AND_VIDEO_SINGLE,
    IMAGE_AND_VIDEO_MULTI,
    GIF_SINGLE,
    AUDIO_SINGLE,
    DOCUMENT_SINGLE,
}

private object VistaThumbnailMemoryCache {
    private val cache = LruCache<String, ImageBitmap>(80)

    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }
}

/**
 * انتخاب‌گر کاملاً درون‌برنامه‌ای رسانه‌ها، آهنگ‌ها و فایل‌های دستگاه با طراحی شیشه‌ای.
 * بدون باز کردن File Picker یا گالری سیستم‌عامل اندروید.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaInAppMediaPickerBottomSheet(
    mode: VistaInAppPickerMode,
    onDismiss: () -> Unit,
    onSingleSelected: (Uri) -> Unit = {},
    onMultipleSelected: (List<Uri>) -> Unit = {},
    maxSelection: Int = 10,
    title: String? = null,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val glassState = rememberVistaGlassState()

    val isMulti = mode == VistaInAppPickerMode.IMAGE_MULTI ||
        mode == VistaInAppPickerMode.IMAGE_AND_VIDEO_MULTI
    val galleryFilter = when (mode) {
        VistaInAppPickerMode.IMAGE_SINGLE, VistaInAppPickerMode.IMAGE_MULTI -> VistaGalleryFilter.IMAGES
        VistaInAppPickerMode.VIDEO_SINGLE -> VistaGalleryFilter.VIDEOS
        VistaInAppPickerMode.GIF_SINGLE -> VistaGalleryFilter.GIFS
        else -> VistaGalleryFilter.ALL
    }

    var searchQuery by remember(mode) { mutableStateOf("") }
    var selectedAlbum by remember(mode) { mutableStateOf<String?>(null) }
    var fileCategory by remember(mode) { mutableStateOf(VistaFileCategoryFilter.ALL) }
    val selectedUris = remember(mode) { mutableStateListOf<Uri>() }

    var hasPermission by remember(mode) {
        mutableStateOf(
            when (mode) {
                VistaInAppPickerMode.AUDIO_SINGLE -> hasVistaAudioPermission(context)
                VistaInAppPickerMode.DOCUMENT_SINGLE -> hasVistaFilesReadPermission(context)
                else -> hasVistaGalleryPermission(context, galleryFilter)
            },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        hasPermission = results.values.any { it } || when (mode) {
            VistaInAppPickerMode.AUDIO_SINGLE -> hasVistaAudioPermission(context)
            VistaInAppPickerMode.DOCUMENT_SINGLE -> hasVistaFilesReadPermission(context)
            else -> hasVistaGalleryPermission(context, galleryFilter)
        }
    }

    var isLoading by remember(mode) { mutableStateOf(true) }
    var galleryItems by remember(mode) { mutableStateOf<List<VistaGalleryMediaItem>>(emptyList()) }
    var audioItems by remember(mode) { mutableStateOf<List<VistaAudioMediaItem>>(emptyList()) }
    var fileItems by remember(mode) { mutableStateOf<List<VistaDeviceFileItem>>(emptyList()) }

    var playingAudioId by remember { mutableStateOf<Long?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopAudioPreview() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        playingAudioId = null
    }

    fun toggleAudioPreview(track: VistaAudioMediaItem) {
        if (playingAudioId == track.id) {
            stopAudioPreview()
            return
        }
        stopAudioPreview()
        runCatching {
            val player = MediaPlayer().apply {
                setDataSource(context, track.toUri())
                setOnCompletionListener {
                    playingAudioId = null
                    runCatching { release() }
                    if (mediaPlayer === this) mediaPlayer = null
                }
                prepare()
                start()
            }
            mediaPlayer = player
            playingAudioId = track.id
        }.onFailure {
            stopAudioPreview()
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopAudioPreview() }
    }

    LaunchedEffect(mode, hasPermission) {
        isLoading = true
        when (mode) {
            VistaInAppPickerMode.AUDIO_SINGLE -> {
                audioItems = if (hasPermission) queryVistaAudioTracks(context) else emptyList()
            }
            VistaInAppPickerMode.DOCUMENT_SINGLE -> {
                fileItems = queryVistaDeviceFiles(
                    context = context,
                    allowedExtensions = VistaMediaBrowserHelpers.CHAT_ALLOWED_ALL_FILE_EXTENSIONS,
                )
            }
            else -> {
                galleryItems = if (hasPermission) {
                    queryVistaGalleryMedia(context, filter = galleryFilter, limit = 260)
                } else {
                    emptyList()
                }
            }
        }
        isLoading = false
    }

    val resolvedTitle = title ?: when (mode) {
        VistaInAppPickerMode.IMAGE_SINGLE -> "انتخاب تصویر"
        VistaInAppPickerMode.IMAGE_MULTI -> "انتخاب تصاویر"
        VistaInAppPickerMode.VIDEO_SINGLE -> "انتخاب ویدیو"
        VistaInAppPickerMode.IMAGE_AND_VIDEO_SINGLE,
        VistaInAppPickerMode.IMAGE_AND_VIDEO_MULTI -> "انتخاب رسانه"
        VistaInAppPickerMode.GIF_SINGLE -> "انتخاب گیف از گوشی"
        VistaInAppPickerMode.AUDIO_SINGLE -> "انتخاب آهنگ از گوشی"
        VistaInAppPickerMode.DOCUMENT_SINGLE -> "انتخاب فایل"
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopAudioPreview()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = if (isDark) Color(0xFF131922) else Color(0xFFF7F9FC),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.86f)
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = {
                                stopAudioPreview()
                                onDismiss()
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = resolvedTitle,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (isMulti && selectedUris.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF2AABEE), VistaBrandColors.Indigo),
                                    ),
                                )
                                .clickable {
                                    onMultipleSelected(selectedUris.toList())
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = VistaMediaBrowserHelpers.toPersianDigits("تأیید (${selectedUris.size})"),
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                if (mode == VistaInAppPickerMode.AUDIO_SINGLE || mode == VistaInAppPickerMode.DOCUMENT_SINGLE) {
                    VistaInAppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = if (mode == VistaInAppPickerMode.AUDIO_SINGLE) {
                            "جستجو در آهنگ‌ها و خواننده‌ها..."
                        } else {
                            "جستجو در نام فایل‌ها..."
                        },
                        isDark = isDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }

                val albumNames = remember(galleryItems) {
                    VistaMediaBrowserHelpers.extractAlbumNames(galleryItems)
                }
                if (mode != VistaInAppPickerMode.AUDIO_SINGLE &&
                    mode != VistaInAppPickerMode.DOCUMENT_SINGLE &&
                    hasPermission &&
                    albumNames.isNotEmpty()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "all_albums") {
                            VistaFilterChipPill(
                                label = "همه",
                                selected = selectedAlbum == null,
                                isDark = isDark,
                                onClick = { selectedAlbum = null },
                            )
                        }
                        items(albumNames, key = { it }) { album ->
                            VistaFilterChipPill(
                                label = album,
                                selected = selectedAlbum == album,
                                isDark = isDark,
                                onClick = { selectedAlbum = album },
                            )
                        }
                    }
                } else if (mode == VistaInAppPickerMode.DOCUMENT_SINGLE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            VistaFileCategoryFilter.ALL to "همه فایل‌ها",
                            VistaFileCategoryFilter.PDF_ONLY to "اسناد PDF",
                            VistaFileCategoryFilter.DOWNLOADS to "دانلودها",
                        ).forEach { (cat, label) ->
                            VistaFilterChipPill(
                                label = label,
                                selected = fileCategory == cat,
                                isDark = isDark,
                                onClick = { fileCategory = cat },
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    thickness = 0.6.dp,
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    val showPermissionPrompt = when (mode) {
                        VistaInAppPickerMode.DOCUMENT_SINGLE -> !hasPermission && fileItems.isEmpty()
                        else -> !hasPermission
                    }

                    when {
                        showPermissionPrompt -> {
                            VistaInAppPermissionCard(
                                title = when (mode) {
                                    VistaInAppPickerMode.AUDIO_SINGLE -> "دسترسی به فایل‌های صوتی"
                                    VistaInAppPickerMode.DOCUMENT_SINGLE -> "دسترسی به فایل‌های دستگاه"
                                    else -> "دسترسی به رسانه‌های دستگاه"
                                },
                                description = "برای نمایش مستقیم فایل‌ها درون برنامه بدون باز شدن فایل‌های سیستم، لطفاً دسترسی لازم را فعال کنید.",
                                onGrantClick = {
                                    val perms = when (mode) {
                                        VistaInAppPickerMode.AUDIO_SINGLE -> requiredVistaAudioPermissions()
                                        VistaInAppPickerMode.DOCUMENT_SINGLE -> requiredVistaFilesReadPermissions()
                                        else -> requiredVistaGalleryPermissions(galleryFilter)
                                    }
                                    permissionLauncher.launch(perms)
                                },
                                onSettingsClick = { openVistaAppSettings(context) },
                            )
                        }

                        isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF2AABEE))
                            }
                        }

                        mode == VistaInAppPickerMode.AUDIO_SINGLE -> {
                            val filtered = remember(audioItems, searchQuery) {
                                VistaMediaBrowserHelpers.filterAudioItems(audioItems, searchQuery)
                            }
                            if (filtered.isEmpty()) {
                                VistaInAppEmptyMessage(
                                    message = if (searchQuery.isNotBlank()) {
                                        "آهنگی با این عبارت یافت نشد"
                                    } else {
                                        "فایل صوتی در حافظه دستگاه پیدا نشد"
                                    },
                                )
                            } else {
                                val listState = rememberLazyListState()
                                VistaGlassScrollSync(glassState, listState)
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .vistaGlassSource(glassState),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    items(filtered, key = { it.id }) { track ->
                                        val isPlaying = playingAudioId == track.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isDark) Color.White.copy(alpha = 0.05f)
                                                    else Color.White.copy(alpha = 0.85f),
                                                )
                                                .clickable {
                                                    stopAudioPreview()
                                                    onSingleSelected(track.toUri())
                                                    onDismiss()
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(
                                                                Color(0xFF2AABEE),
                                                                VistaBrandColors.Indigo,
                                                            ),
                                                        ),
                                                    )
                                                    .clickable { toggleAudioPreview(track) },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                track.toAlbumArtUri()?.let { artUri ->
                                                    VistaLocalThumbnailImage(
                                                        uri = artUri,
                                                        targetSizePx = 144,
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.36f)),
                                                    )
                                                }
                                                if (isPlaying) {
                                                    VistaPauseBarsGlyph(color = Color.White)
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "پخش",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = track.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = "${track.artist}  •  ${VistaMediaBrowserHelpers.formatMediaDuration(track.durationMs)}  •  ${VistaMediaBrowserHelpers.formatFileSize(track.sizeBytes)}",
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        mode == VistaInAppPickerMode.DOCUMENT_SINGLE -> {
                            val filtered = remember(fileItems, searchQuery, fileCategory) {
                                VistaMediaBrowserHelpers.filterFileItems(fileItems, searchQuery, fileCategory)
                            }
                            if (filtered.isEmpty()) {
                                VistaInAppEmptyMessage(
                                    message = if (searchQuery.isNotBlank()) {
                                        "فایلی با این عبارت یافت نشد"
                                    } else {
                                        "فایل قابل ارسالی در حافظه یافت نشد"
                                    },
                                )
                            } else {
                                val listState = rememberLazyListState()
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    items(filtered, key = { it.id }) { file ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isDark) Color.White.copy(alpha = 0.05f)
                                                    else Color.White.copy(alpha = 0.85f),
                                                )
                                                .clickable {
                                                    scope.launch {
                                                        val readyUri = ensureShareableContentUri(
                                                            context = context,
                                                            uri = file.toUri(),
                                                            fallbackDisplayName = file.displayName,
                                                        )
                                                        onSingleSelected(readyUri)
                                                        onDismiss()
                                                    }
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFFE11D48).copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = file.extension.uppercase(Locale.US).take(4),
                                                    color = Color(0xFFE11D48),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = file.displayName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = "${VistaMediaBrowserHelpers.formatFileSize(file.sizeBytes)}  •  ${file.folderLabel}",
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            val filtered = remember(galleryItems, galleryFilter, selectedAlbum) {
                                VistaMediaBrowserHelpers.filterGalleryItems(
                                    items = galleryItems,
                                    filter = galleryFilter,
                                    albumName = selectedAlbum,
                                )
                            }
                            if (filtered.isEmpty()) {
                                VistaInAppEmptyMessage(
                                    message = if (mode == VistaInAppPickerMode.GIF_SINGLE) {
                                        "هیچ فایل GIF در حافظه دستگاه یافت نشد"
                                    } else {
                                        "موردی در گالری دستگاه یافت نشد"
                                    },
                                )
                            } else {
                                val gridState = rememberLazyGridState()
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    items(filtered, key = { it.id }) { item ->
                                        val uri = item.toUri()
                                        val selectedIdx = selectedUris.indexOf(uri)
                                        val isSelected = selectedIdx >= 0
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.14f))
                                                .clickable {
                                                    if (!isMulti) {
                                                        onSingleSelected(uri)
                                                        onDismiss()
                                                    } else {
                                                        if (isSelected) {
                                                            selectedUris.remove(uri)
                                                        } else if (selectedUris.size < maxSelection) {
                                                            selectedUris.add(uri)
                                                        }
                                                    }
                                                }
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.border(
                                                            2.dp,
                                                            Color(0xFF2AABEE),
                                                            RoundedCornerShape(6.dp),
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                                ),
                                        ) {
                                            VistaLocalThumbnailImage(
                                                uri = uri,
                                                targetSizePx = 320,
                                                modifier = Modifier.fillMaxSize(),
                                            )

                                            if (item.isVideo) {
                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .fillMaxWidth()
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f)),
                                                            ),
                                                        )
                                                        .padding(horizontal = 5.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp),
                                                    )
                                                    Text(
                                                        text = VistaMediaBrowserHelpers.formatMediaDuration(item.durationMs),
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                    )
                                                }
                                            } else if (item.isGif) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(5.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color.Black.copy(alpha = 0.66f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                ) {
                                                    Text(
                                                        text = "GIF",
                                                        color = Color.White,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                            }

                                            if (isMulti) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(5.dp)
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .then(
                                                            if (isSelected) {
                                                                Modifier.background(Color(0xFF2AABEE))
                                                            } else {
                                                                Modifier
                                                                    .background(Color.Black.copy(alpha = 0.30f))
                                                                    .border(1.4.dp, Color.White, CircleShape)
                                                            },
                                                        ),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (isSelected) {
                                                        Text(
                                                            text = VistaMediaBrowserHelpers.toPersianDigits(
                                                                (selectedIdx + 1).toString(),
                                                            ),
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
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
            }
        }
    }
}

@Composable
private fun VistaLocalThumbnailImage(
    uri: Uri,
    targetSizePx: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val cacheKey = "${uri}_$targetSizePx"
    var bitmap by remember(cacheKey) { mutableStateOf(VistaThumbnailMemoryCache.get(cacheKey)) }

    LaunchedEffect(cacheKey) {
        if (bitmap != null) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val bmp: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching {
                        context.contentResolver.loadThumbnail(
                            uri,
                            Size(targetSizePx, targetSizePx),
                            null,
                        )
                    }.getOrNull()
                } else {
                    null
                }
                val resolved = bmp ?: context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(
                        stream,
                        null,
                        BitmapFactory.Options().apply { inSampleSize = 4 },
                    )
                }
                resolved?.asImageBitmap()
            }.getOrNull()
        }
        if (loaded != null) {
            VistaThumbnailMemoryCache.put(cacheKey, loaded)
            bitmap = loaded
        }
    }

    val currentBitmap = bitmap
    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF1E2632).copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun VistaPauseBarsGlyph(color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color),
        )
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(color),
        )
    }
}

@Composable
private fun VistaInAppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(bgColor)
            .border(0.7.dp, borderColor, RoundedCornerShape(21.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.5.sp,
                ),
                cursorBrush = SolidColor(Color(0xFF2AABEE)),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "پاک کردن",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun VistaFilterChipPill(
    label: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) Color(0xFF2AABEE)
                else if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.06f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VistaInAppPermissionCard(
    title: String,
    description: String,
    onGrantClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Color(0xFF2AABEE).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF2AABEE),
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onGrantClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE)),
        ) {
            Text("تأیید و اعطای دسترسی", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSettingsClick,
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("باز کردن تنظیمات برنامه")
        }
    }
}

@Composable
private fun VistaInAppEmptyMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
        )
    }
}
