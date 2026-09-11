package ir.coffevista.vista_native.features.feed.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.feed.ui.VerifiedMark
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    onBack: () -> Unit,
    onNavigateToTrimmer: (Uri) -> Unit,
    onPostCreated: () -> Unit,
    modifier: Modifier = Modifier,
    preloadedText: String? = null,
    preloadedMediaUris: List<Uri> = emptyList(),
    viewModel: AddPostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showLocationDialog by remember { mutableStateOf(false) }
    var showMusicTrimSheet by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }

    // Share intent / preloaded media
    LaunchedEffect(preloadedText, preloadedMediaUris) {
        viewModel.initPreloaded(preloadedText, preloadedMediaUris)
    }

    // Camera launcher setup
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success && tempCameraImageUri != null) {
            viewModel.onImageCaptured(tempCameraImageUri!!)
        }
    }

    fun launchCamera() {
        val cacheDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(cacheDir, "post_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file,
        )
        tempCameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    // Launcher for Images (multi-selection)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = uiState.maxGalleryImages),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(uris)
        }
    }

    // Launcher for Video
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onVideoSelected(uri)
            onNavigateToTrimmer(uri)
        }
    }

    // Launcher for Music
    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let(viewModel::onMusicFileSelected)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onPostCreated()
        }
    }

    LaunchedEffect(uiState.selectedMusicUri) {
        if (uiState.selectedMusicUri != null) showMusicTrimSheet = true
    }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    val isDark = MaterialTheme.colorScheme.surface.let { color ->
        val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
        luminance < 0.5
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "افزودن پست جدید",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            bottomBar = {
                // Fixed Bottom Action Bar: Character Counter Ring + Send Post Button
                Surface(
                    shadowElevation = 8.dp,
                    color = if (isDark) Color(0xFF13131E) else Color.White,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Character counter indicator ring + upgrade tooltip
                        val maxLen = uiState.maxCharLength
                        val count = uiState.content.length
                        val progress = (count.toFloat() / maxLen).coerceIn(0f, 1f)
                        val remaining = maxLen - count

                        val indicatorColor = when {
                            count > maxLen -> MaterialTheme.colorScheme.error
                            count > maxLen * 0.8f -> Color(0xFFFFA726)
                            else -> if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.54f)
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(44.dp),
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(42.dp),
                                color = indicatorColor,
                                trackColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f),
                                strokeWidth = 3.dp,
                            )
                            Text(
                                text = remaining.toString(),
                                fontSize = if (remaining.toString().length >= 4) 9.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = indicatorColor,
                            )
                        }

                        // Send Post Button (solid black in light mode, solid white in dark mode)
                        Button(
                            onClick = viewModel::submitPost,
                            enabled = !uiState.isUploading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color.White else Color.Black,
                                contentColor = if (isDark) Color.Black else Color.White,
                                disabledContainerColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.38f),
                                disabledContentColor = (if (isDark) Color.Black else Color.White).copy(alpha = 0.38f),
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        ) {
                            if (uiState.isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = if (isDark) Color.Black else Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "ارسال پست",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isDark) Color.Black else Color.White,
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isDark) Color.Black else Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                // Upload Progress Banner
                AnimatedVisibility(visible = uiState.isUploading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = uiState.uploadStatusMessage ?: "در حال آپلود…",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "${(uiState.uploadProgress * 100).toInt()}٪",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uiState.uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                    }
                }

                // 1. Author Card
                AddPostAuthorCard(
                    avatarUrl = uiState.authorAvatarUrl,
                    username = uiState.authorUsername,
                    fullName = uiState.authorFullName,
                    isVerified = uiState.authorIsVerified,
                    isDark = isDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )

                // 2. Media Preview / Pickers — runtime Flutter places media first.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    when {
                        // Video Selected
                        uiState.isVideo && uiState.selectedVideo != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(uiState.aspectRatio.floatValue)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "ویدیو انتخاب و آماده شد\n(${uiState.selectedVideo?.name})",
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                )

                                // Action buttons over video
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = {
                                            uiState.selectedVideoUri?.let(onNavigateToTrimmer)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Black.copy(alpha = 0.7f),
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCut,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ویرایش مجدد", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = viewModel::removeVideo,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD32F2F).copy(alpha = 0.8f),
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حذف", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Images Selected
                        uiState.selectedImages.isNotEmpty() -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    itemsIndexed(uiState.selectedImages) { idx, imageUri ->
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(14.dp),
                                                )
                                                .clickable { selectedImageIndex = idx },
                                        ) {
                                            AsyncImage(
                                                model = imageUri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )

                                            // Scrim on top
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(36.dp)
                                                    .background(Color.Black.copy(alpha = 0.25f)),
                                            )

                                            IconButton(
                                                onClick = { viewModel.removeImage(imageUri) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.6f)),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "حذف",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }

                                            // Counter badge
                                            Text(
                                                text = "${idx + 1}/${uiState.selectedImages.size}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(6.dp),
                                            )
                                        }
                                    }

                                    if (uiState.selectedImages.size < uiState.maxGalleryImages) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .size(140.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable {
                                                        imagePickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "افزودن عکس بیشتر",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(32.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                // Instagram-Style Floating Music Overlay on Photos
                                if (uiState.selectedMusicUri != null || uiState.selectedMusicTitle != null) {
                                    MusicOverlayPill(
                                        title = uiState.selectedMusicTitle ?: "موسیقی انتخاب‌شده",
                                        onTap = { showMusicTrimSheet = true },
                                        onRemove = { viewModel.onMusicSelected(null, null) },
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }

                        // Empty State: DottedBorder container with 4 action buttons
                        else -> {
                            MediaUploadSection(
                                isDark = isDark,
                                onPickImages = {
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                onCaptureCamera = ::launchCamera,
                                onPickVideo = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                                    )
                                },
                                onPickMusic = {
                                    musicPickerLauncher.launch("audio/*")
                                },
                            )
                        }
                    }
                }

                // 3. Caption follows the media chooser, matching the Flutter journey.
                AddPostCaptionField(
                    content = uiState.content,
                    onContentChanged = viewModel::onContentChanged,
                    isDark = isDark,
                )

                // Aspect Ratio Selector
                if (uiState.selectedImages.isNotEmpty() || uiState.selectedVideo != null) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(
                            text = "نسبت تصویر:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PostAspectRatio.entries.forEach { ratio ->
                                FilterChip(
                                    selected = uiState.aspectRatio == ratio,
                                    onClick = { viewModel.onAspectRatioChanged(ratio) },
                                    label = { Text(ratio.label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hashtags Autocomplete Card
                AnimatedVisibility(
                    visible = uiState.isLoadingHashtagSuggestions || uiState.hashtagSuggestions.isNotEmpty(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        if (uiState.isLoadingHashtagSuggestions && uiState.hashtagSuggestions.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                        } else {
                            uiState.hashtagSuggestions.take(6).forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectHashtagSuggestion(suggestion.tag) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("#", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("#${suggestion.tag}", fontWeight = FontWeight.SemiBold)
                                        Text("${suggestion.usageCount} پست", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("افزودن", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Keep secondary controls out of the empty composer. The Flutter
                // reference exposes the media picker then caption; these controls
                // belong to the post-editing state once media exists.
                if (uiState.selectedImages.isNotEmpty() || uiState.selectedVideo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.selectedLocation != null,
                        onClick = { showLocationDialog = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = {
                            Text(
                                text = uiState.selectedLocation ?: "افزودن موقعیت",
                                fontSize = 12.sp,
                            )
                        },
                    )

                    FilterChip(
                        selected = uiState.selectedMusicTitle != null,
                        onClick = { musicPickerLauncher.launch("audio/*") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = {
                            Text(
                                text = uiState.selectedMusicTitle ?: "افزودن موسیقی",
                                fontSize = 12.sp,
                            )
                        },
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // Advanced Post Settings
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "تنظیمات پیشرفته پست",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("مخفی کردن تعداد لایک‌ها", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("تعداد لایک‌های این پست برای دیگران نمایش داده نمی‌شود", fontSize = 11.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        ir.coffevista.vista_native.core.designsystem.component.VistaSwitch(
                            checked = uiState.hideLikeCount,
                            onCheckedChange = { viewModel.toggleHideLikeCount(it) },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("بستن نظرات", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("امکان ثبت نظر روی این پست غیرفعال می‌شود", fontSize = 11.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        ir.coffevista.vista_native.core.designsystem.component.VistaSwitch(
                            checked = uiState.commentsDisabled,
                            onCheckedChange = { viewModel.toggleCommentsDisabled(it) },
                        )
                    }
                }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            // Promotional upgrade tooltip for standard users (max 500 chars)
            if (uiState.maxCharLength == 500) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 6.dp, start = 14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4A90E2),
                        shadowElevation = 4.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = "ارتقا به پریمیوم",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "۴۰۰ کاراکتر بنویسید",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .size(9.dp)
                            .graphicsLayer { rotationZ = 45f }
                            .background(Color(0xFF4A90E2)),
                    )
                }
            }
        }
    }

        // Location Dialog
        if (showLocationDialog) {
            var customCity by remember { mutableStateOf(uiState.selectedLocation.orEmpty()) }
            val quickCities = listOf("تهران، ایران", "شیراز، فارس", "اصفهان", "مشهد، خراسان", "تبریز، آذربایجان", "کیش، هرمزگان")
            AlertDialog(
                onDismissRequest = { showLocationDialog = false },
                title = { Text("انتخاب موقعیت مکانی", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customCity,
                            onValueChange = { customCity = it },
                            label = { Text("نام شهر یا مکان") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("مکان‌های پیشنهادی:", fontSize = 12.sp, color = Color.Gray)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            quickCities.forEach { city ->
                                Text(
                                    text = "📍 $city",
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onLocationSelected(city)
                                            showLocationDialog = false
                                        }
                                        .padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onLocationSelected(customCity.trim().ifBlank { null })
                        showLocationDialog = false
                    }) { Text("تایید") }
                },
                dismissButton = {
                    if (uiState.selectedLocation != null) {
                        TextButton(onClick = {
                            viewModel.onLocationSelected(null)
                            showLocationDialog = false
                        }) { Text("حذف مکان") }
                    } else {
                        TextButton(onClick = { showLocationDialog = false }) { Text("انصراف") }
                    }
                },
            )
        }

        // Music Trim Bottom Sheet
        if (showMusicTrimSheet && uiState.selectedMusicUri != null && uiState.musicDurationMs > 0) {
            val maxClipMs = if (uiState.isPremium) 60_000 else 15_000
            var trimRange by remember(uiState.selectedMusicUri) {
                mutableStateOf(uiState.musicStartMs.toFloat()..uiState.musicEndMs.toFloat())
            }
            ModalBottomSheet(onDismissRequest = { showMusicTrimSheet = false }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("برش موسیقی", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(uiState.selectedMusicTitle.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("بازه: ${(trimRange.start / 1000).toInt()} تا ${(trimRange.endInclusive / 1000).toInt()} ثانیه")
                    RangeSlider(
                        value = trimRange,
                        onValueChange = { value ->
                            val start = value.start.coerceAtLeast(0f)
                            val end = value.endInclusive.coerceAtMost(uiState.musicDurationMs.toFloat())
                            trimRange = start..minOf(end, start + maxClipMs)
                        },
                        valueRange = 0f..uiState.musicDurationMs.toFloat(),
                    )
                    Text(
                        if (uiState.isPremium) "حداکثر برش: ۶۰ ثانیه" else "حداکثر برش: ۱۵ ثانیه",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            viewModel.onMusicTrimChanged(trimRange.start.toInt(), trimRange.endInclusive.toInt())
                            showMusicTrimSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("تایید برش") }
                    TextButton(
                        onClick = {
                            viewModel.onMusicSelected(null, null)
                            showMusicTrimSheet = false
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) { Text("حذف موسیقی") }
                }
            }
        }
    }
}

@Composable
private fun AddPostCaptionField(
    content: String,
    onContentChanged: (String) -> Unit,
    isDark: Boolean,
) {
    val cardBgColor = if (isDark) Color(0xFF13131E) else Color(0xFFF3F4FF)
    val primaryTextColor = if (isDark) Color.White else Color.Black
    val hintTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.54f)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        androidx.compose.material3.TextField(
            value = content,
            onValueChange = onContentChanged,
            placeholder = {
                Text(
                    text = "چیزی بنویسید...",
                    fontSize = 16.sp,
                    color = hintTextColor,
                )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 200.dp),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = primaryTextColor,
                unfocusedTextColor = primaryTextColor,
            ),
            maxLines = 7,
            minLines = 3,
        )
    }
}

@Composable
private fun AddPostAuthorCard(
    avatarUrl: String?,
    username: String,
    fullName: String,
    isVerified: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val cardBgColor = if (isDark) Color(0xFF13131E) else Color(0xFFF3F4FF)
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.54f)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2A2A3E) else Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "تصویر نمایه $username",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = (username.firstOrNull() ?: fullName.firstOrNull() ?: 'V').uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = username.ifBlank { fullName.ifBlank { "کاربر ویستا" } },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColor,
                    )
                    if (isVerified) {
                        VerifiedMark(modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "در حال ایجاد پست جدید...",
                    fontSize = 12.sp,
                    color = secondaryTextColor,
                )
            }
        }
    }
}

@Composable
private fun MediaUploadSection(
    isDark: Boolean,
    onPickImages: () -> Unit,
    onCaptureCamera: () -> Unit,
    onPickVideo: () -> Unit,
    onPickMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.22f)
    val bgColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .dashedBorder(
                width = 1.dp,
                color = borderColor,
                cornerRadius = 12.dp,
                dashLength = 6.dp,
                gapLength = 4.dp,
            )
            .padding(vertical = 18.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "محتوای چندرسانه‌ای اضافه کنید",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.54f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaOptionButton(
                    icon = Icons.Default.Image,
                    label = "تصویر",
                    tintColor = Color(0xFF3897F0),
                    isDark = isDark,
                    onClick = onPickImages,
                )
                MediaOptionButton(
                    icon = Icons.Default.CameraAlt,
                    label = "دوربین",
                    tintColor = Color(0xFF8B5CF6),
                    isDark = isDark,
                    onClick = onCaptureCamera,
                )
                MediaOptionButton(
                    icon = Icons.Default.Videocam,
                    label = "ویدیو",
                    tintColor = Color(0xFFE0457B),
                    isDark = isDark,
                    onClick = onPickVideo,
                )
                MediaOptionButton(
                    icon = Icons.Default.MusicNote,
                    label = "موزیک",
                    tintColor = Color(0xFF1DB954),
                    isDark = isDark,
                    onClick = onPickMusic,
                )
            }
        }
    }
}

@Composable
private fun MediaOptionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tintColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val buttonBg = tintColor.copy(alpha = if (isDark) 0.22f else 0.12f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(buttonBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MusicOverlayPill(
    title: String,
    onTap: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RotatingVinylDisc(size = 24.dp)
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 130.dp),
            )
            AnimatedEqualizerBars(
                barCount = 3,
                height = 14.dp,
                color = Color.White,
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "حذف موسیقی",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
fun RotatingVinylDisc(size: Dp = 28.dp, isPlaying: Boolean = true) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vinylAngle",
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = if (isPlaying) angle else 0f }
            .clip(CircleShape)
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center,
    ) {
        // Grooves
        Box(
            modifier = Modifier
                .size(size * 0.65f)
                .border(1.dp, Color(0xFF383838), CircleShape),
        )
        // Center label
        Box(
            modifier = Modifier
                .size(size * 0.35f)
                .clip(CircleShape)
                .background(Color(0xFFE91E63)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.12f)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

@Composable
fun AnimatedEqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    barCount: Int = 3,
    height: Dp = 14.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(barCount) { index ->
            val duration = 400 + index * 150
            val barHeightFraction by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar_$index",
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .fillMaxHeight(barHeightFraction)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color),
            )
        }
    }
}

fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 4.dp,
): Modifier = this.drawWithContent {
    drawContent()
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx()),
            0f,
        ),
    )
    val r = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(width.toPx() / 2f, width.toPx() / 2f),
        size = Size(size.width - width.toPx(), size.height - width.toPx()),
        cornerRadius = CornerRadius(r, r),
        style = stroke,
    )
}
