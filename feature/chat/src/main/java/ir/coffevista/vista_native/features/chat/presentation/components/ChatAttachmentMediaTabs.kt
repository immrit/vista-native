package ir.coffevista.vista_native.features.chat.presentation.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ir.coffevista.vista_native.core.designsystem.component.VistaAudioMediaItem
import ir.coffevista.vista_native.core.designsystem.component.VistaDeviceFileItem
import ir.coffevista.vista_native.core.designsystem.component.VistaFileCategoryFilter
import ir.coffevista.vista_native.core.designsystem.component.VistaMediaBrowserHelpers
import ir.coffevista.vista_native.core.designsystem.component.hasVistaAudioPermission
import ir.coffevista.vista_native.core.designsystem.component.hasVistaFilesReadPermission
import ir.coffevista.vista_native.core.designsystem.component.openVistaAppSettings
import ir.coffevista.vista_native.core.designsystem.component.queryVistaAudioTracks
import ir.coffevista.vista_native.core.designsystem.component.queryVistaDeviceFiles
import ir.coffevista.vista_native.core.designsystem.component.requiredVistaAudioPermissions
import ir.coffevista.vista_native.core.designsystem.component.requiredVistaFilesReadPermissions
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassScrollSync
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassSource
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors

private val TelegramAccentBlue = Color(0xFF2AABEE)

/**
 * تب موسیقی درون‌برنامه‌ای در باتم‌شیت پیوست چت:
 * نمایش آهنگ‌های حافظه دستگاه، سرچ زنده، پخش پیش‌نمایش و انتخاب چندگانه با بج عددی تلگرامی.
 */
@Composable
internal fun ChatAttachmentMusicTabContent(
    selectedUris: MutableList<Uri>,
    listState: LazyListState = rememberLazyListState(),
    glassSourceState: VistaGlassState? = null,
    bottomContentPadding: Dp = 96.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var searchQuery by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(hasVistaAudioPermission(context)) }
    var audioItems by remember { mutableStateOf<List<VistaAudioMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var playingTrackId by remember { mutableStateOf<Long?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPreview() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        playingTrackId = null
    }

    fun togglePreview(item: VistaAudioMediaItem) {
        if (playingTrackId == item.id) {
            stopPreview()
            return
        }
        stopPreview()
        runCatching {
            val player = MediaPlayer().apply {
                setDataSource(context, item.toUri())
                setOnCompletionListener {
                    playingTrackId = null
                    runCatching { release() }
                    if (mediaPlayer === this) mediaPlayer = null
                }
                prepare()
                start()
            }
            mediaPlayer = player
            playingTrackId = item.id
        }.onFailure {
            stopPreview()
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        hasPermission = results.values.any { it } || hasVistaAudioPermission(context)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            audioItems = queryVistaAudioTracks(
                context = context,
                allowedExtensions = VistaMediaBrowserHelpers.CHAT_ALLOWED_AUDIO_EXTENSIONS,
                limit = 260,
            )
            isLoading = false
        }
    }

    val filteredItems = remember(audioItems, searchQuery) {
        VistaMediaBrowserHelpers.filterAudioItems(audioItems, searchQuery)
    }

    if (glassSourceState != null) {
        VistaGlassScrollSync(glassSourceState, listState)
    }

    val containerModifier = if (glassSourceState != null) {
        modifier.vistaGlassSource(glassSourceState)
    } else {
        modifier
    }

    Column(modifier = containerModifier.fillMaxSize()) {
        AttachmentGlassSearchInput(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "جستجو نام آهنگ، خواننده یا آلبوم...",
            isDark = isDark,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                !hasPermission -> {
                    AttachmentTabPermissionCard(
                        icon = Icons.Default.Audiotrack,
                        title = "دسترسی به موسیقی‌های دستگاه",
                        description = "برای مشاهده و جستجوی مستقیم تمام آهنگ‌ها درون همین صفحه، دسترسی خواندن فایل‌های صوتی را تأیید کنید.",
                        onGrantClick = { permissionLauncher.launch(requiredVistaAudioPermissions()) },
                        onSettingsClick = { openVistaAppSettings(context) },
                        bottomPadding = bottomContentPadding,
                    )
                }

                isLoading -> {
                    AttachmentTabLoadingState(bottomPadding = bottomContentPadding)
                }

                filteredItems.isEmpty() -> {
                    AttachmentTabEmptyState(
                        message = if (searchQuery.isNotBlank()) {
                            "آهنگی مطابق با «$searchQuery» پیدا نشد"
                        } else {
                            "فایل صوتی قابل ارسالی در حافظه دستگاه پیدا نشد"
                        },
                        bottomPadding = bottomContentPadding,
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 10.dp,
                            end = 10.dp,
                            top = 4.dp,
                            bottom = bottomContentPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filteredItems, key = { it.id }) { track ->
                            val trackUri = remember(track.uriString) { track.toUri() }
                            val selectedIndex = selectedUris.indexOf(trackUri)
                            val isSelected = selectedIndex >= 0
                            val isPlaying = playingTrackId == track.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(
                                        when {
                                            isSelected -> TelegramAccentBlue.copy(alpha = if (isDark) 0.18f else 0.10f)
                                            isDark -> Color.White.copy(alpha = 0.05f)
                                            else -> Color.White.copy(alpha = 0.72f)
                                        },
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                0.9.dp,
                                                TelegramAccentBlue.copy(alpha = 0.45f),
                                                RoundedCornerShape(15.dp),
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            selectedUris.remove(trackUri)
                                        } else {
                                            selectedUris.add(trackUri)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // کاور آلبوم + دکمه پخش/توقف پیش‌نمایش
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    if (isPlaying) TelegramAccentBlue else TelegramAccentBlue.copy(alpha = 0.75f),
                                                    VistaBrandColors.Indigo,
                                                ),
                                            ),
                                        )
                                        .clickable { togglePreview(track) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val artUri = remember(track.albumArtUriString) { track.toAlbumArtUri() }
                                    if (artUri != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(artUri)
                                                .size(160)
                                                .crossfade(true)
                                                .build(),
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
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "توقف" else "پخش",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        fontSize = 13.8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isPlaying) TelegramAccentBlue else MaterialTheme.colorScheme.onSurface,
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

                                TelegramSelectionNumberBadge(selectedIndex = selectedIndex)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * تب فایل‌ها و اسناد درون‌برنامه‌ای در باتم‌شیت پیوست چت:
 * نمایش فایل‌های حافظه و دانلودها، فیلتر (همه / PDF / دانلودها)، جستجو و انتخاب چندگانه.
 */
@Composable
internal fun ChatAttachmentFilesTabContent(
    selectedUris: MutableList<Uri>,
    selectedNamesByUri: MutableMap<Uri, String>,
    listState: LazyListState = rememberLazyListState(),
    glassSourceState: VistaGlassState? = null,
    bottomContentPadding: Dp = 96.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var searchQuery by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(VistaFileCategoryFilter.ALL) }
    var hasPermission by remember { mutableStateOf(hasVistaFilesReadPermission(context)) }
    var fileItems by remember { mutableStateOf<List<VistaDeviceFileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        hasPermission = results.values.any { it } || hasVistaFilesReadPermission(context)
    }

    LaunchedEffect(hasPermission) {
        isLoading = true
        fileItems = queryVistaDeviceFiles(
            context = context,
            allowedExtensions = VistaMediaBrowserHelpers.CHAT_ALLOWED_ALL_FILE_EXTENSIONS,
            limit = 260,
        )
        isLoading = false
    }

    val filteredItems = remember(fileItems, searchQuery, category) {
        VistaMediaBrowserHelpers.filterFileItems(
            items = fileItems,
            query = searchQuery,
            category = category,
        )
    }

    if (glassSourceState != null) {
        VistaGlassScrollSync(glassSourceState, listState)
    }

    val containerModifier = if (glassSourceState != null) {
        modifier.vistaGlassSource(glassSourceState)
    } else {
        modifier
    }

    Column(modifier = containerModifier.fillMaxSize()) {
        AttachmentGlassSearchInput(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "جستجو در نام فایل‌ها (PDF، دانلودها، ...)",
            isDark = isDark,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                VistaFileCategoryFilter.ALL to "همه فایل‌ها",
                VistaFileCategoryFilter.PDF_ONLY to "اسناد (PDF)",
                VistaFileCategoryFilter.DOWNLOADS to "دانلودها",
            ).forEach { (option, label) ->
                val isSelected = category == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) TelegramAccentBlue
                            else if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                        )
                        .clickable { category = option }
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (!hasPermission && fileItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TelegramAccentBlue.copy(alpha = if (isDark) 0.16f else 0.10f))
                    .clickable { permissionLauncher.launch(requiredVistaFilesReadPermissions()) }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "برای نمایش تمام فایل‌های حافظه، دسترسی را فعال کنید",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "اعطای دسترسی",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramAccentBlue,
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                !hasPermission && fileItems.isEmpty() && !isLoading -> {
                    AttachmentTabPermissionCard(
                        icon = Icons.Default.FolderOpen,
                        title = "دسترسی به فایل‌های دستگاه",
                        description = "برای مشاهده، جستجو و ارسال فایل‌ها مستقیماً درون همین صفحه، دسترسی به حافظه را فعال کنید.",
                        onGrantClick = { permissionLauncher.launch(requiredVistaFilesReadPermissions()) },
                        onSettingsClick = { openVistaAppSettings(context) },
                        bottomPadding = bottomContentPadding,
                    )
                }

                isLoading -> {
                    AttachmentTabLoadingState(bottomPadding = bottomContentPadding)
                }

                filteredItems.isEmpty() -> {
                    AttachmentTabEmptyState(
                        message = when {
                            searchQuery.isNotBlank() -> "فایلی با عبارت «$searchQuery» پیدا نشد"
                            category == VistaFileCategoryFilter.PDF_ONLY -> "سند PDF قابل دسترسی در حافظه یافت نشد"
                            category == VistaFileCategoryFilter.DOWNLOADS -> "فایلی در پوشه دانلودها یافت نشد"
                            else -> "فایل قابل ارسالی در حافظه دستگاه پیدا نشد"
                        },
                        bottomPadding = bottomContentPadding,
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 10.dp,
                            end = 10.dp,
                            top = 4.dp,
                            bottom = bottomContentPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filteredItems, key = { it.id }) { file ->
                            val fileUri = remember(file.uriString) { file.toUri() }
                            val selectedIndex = selectedUris.indexOf(fileUri)
                            val isSelected = selectedIndex >= 0
                            val visual = remember(file.extension) { resolveFileBadgeVisual(file.extension) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(
                                        when {
                                            isSelected -> TelegramAccentBlue.copy(alpha = if (isDark) 0.18f else 0.10f)
                                            isDark -> Color.White.copy(alpha = 0.05f)
                                            else -> Color.White.copy(alpha = 0.72f)
                                        },
                                    )
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                0.9.dp,
                                                TelegramAccentBlue.copy(alpha = 0.45f),
                                                RoundedCornerShape(15.dp),
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            selectedUris.remove(fileUri)
                                            selectedNamesByUri.remove(fileUri)
                                        } else {
                                            selectedUris.add(fileUri)
                                            selectedNamesByUri[fileUri] = file.displayName
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(visual.color.copy(alpha = if (isDark) 0.22f else 0.13f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = visual.icon,
                                            contentDescription = null,
                                            tint = visual.color,
                                            modifier = Modifier.size(19.dp),
                                        )
                                        Text(
                                            text = file.extension.uppercase().take(4),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = visual.color,
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.displayName,
                                        fontSize = 13.5.sp,
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

                                TelegramSelectionNumberBadge(selectedIndex = selectedIndex)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class FileBadgeVisual(
    val icon: ImageVector,
    val color: Color,
)

private fun resolveFileBadgeVisual(extension: String): FileBadgeVisual {
    return when (extension.lowercase()) {
        "pdf" -> FileBadgeVisual(Icons.Default.PictureAsPdf, Color(0xFFE11D48))
        in VistaMediaBrowserHelpers.CHAT_ALLOWED_AUDIO_EXTENSIONS ->
            FileBadgeVisual(Icons.Default.Audiotrack, Color(0xFF7C3AED))
        in VistaMediaBrowserHelpers.CHAT_ALLOWED_VIDEO_EXTENSIONS ->
            FileBadgeVisual(Icons.Default.VideoFile, Color(0xFF0284C7))
        in VistaMediaBrowserHelpers.CHAT_ALLOWED_IMAGE_EXTENSIONS ->
            FileBadgeVisual(Icons.Default.Image, Color(0xFF0D9488))
        else -> FileBadgeVisual(Icons.Default.Description, Color(0xFF2AABEE))
    }
}

@Composable
private fun AttachmentGlassSearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val rimColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(0.7.dp, rimColor, RoundedCornerShape(20.dp))
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
                    fontSize = 12.8.sp,
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
                cursorBrush = SolidColor(TelegramAccentBlue),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "پاک کردن جستجو",
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
private fun AttachmentTabPermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onGrantClick: () -> Unit,
    onSettingsClick: () -> Unit,
    bottomPadding: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(TelegramAccentBlue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TelegramAccentBlue,
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
            colors = ButtonDefaults.buttonColors(containerColor = TelegramAccentBlue),
        ) {
            Text("تأیید و اعطای دسترسی", fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("تنظیمات برنامه", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AttachmentTabLoadingState(bottomPadding: Dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = TelegramAccentBlue,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun AttachmentTabEmptyState(
    message: String,
    bottomPadding: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, bottom = bottomPadding),
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
