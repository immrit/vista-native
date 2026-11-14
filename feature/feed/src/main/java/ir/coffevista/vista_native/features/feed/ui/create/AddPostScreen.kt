package ir.coffevista.vista_native.features.feed.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import ir.coffevista.vista_native.core.designsystem.component.VistaPremiumBottomSheet
import ir.coffevista.vista_native.features.feed.ui.components.SoundCloudMusicBubble
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
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    onBack: () -> Unit,
    onNavigateToTrimmer: (Uri) -> Unit,
    onPostCreated: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToPremium: () -> Unit = {},
    preloadedText: String? = null,
    preloadedMediaUris: List<Uri> = emptyList(),
    preloadedRequestId: String? = null,
    onPreloadedConsumed: (String) -> Unit = {},
    trimmedVideoPath: String? = null,
    onTrimmedVideoConsumed: () -> Unit = {},
    viewModel: AddPostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showLocationDialog by remember { mutableStateOf(false) }
    var showMusicTrimSheet by remember { mutableStateOf(false) }
    var showDiscardDraftDialog by remember { mutableStateOf(false) }
    var showPremiumBottomSheet by remember { mutableStateOf(false) }
    var premiumPromptFeature by remember { mutableStateOf<String?>(null) }
    val captionFocusRequester = remember { FocusRequester() }

    val hasUnsavedContent = remember(uiState.content, uiState.selectedImages, uiState.selectedVideo, uiState.selectedMusicUri) {
        uiState.content.isNotBlank() || uiState.selectedImages.isNotEmpty() || uiState.selectedVideo != null || uiState.selectedMusicUri != null
    }

    androidx.activity.compose.BackHandler(enabled = hasUnsavedContent) {
        showDiscardDraftDialog = true
    }

    fun handleBack() {
        if (hasUnsavedContent) {
            showDiscardDraftDialog = true
        } else {
            onBack()
        }
    }

    // Share intent / preloaded media
    LaunchedEffect(preloadedRequestId, preloadedText, preloadedMediaUris) {
        viewModel.initPreloaded(preloadedText, preloadedMediaUris)
        preloadedRequestId?.let(onPreloadedConsumed)
    }

    // Result of the video trimmer screen, handed over by the navigation layer.
    LaunchedEffect(trimmedVideoPath) {
        val path = trimmedVideoPath ?: return@LaunchedEffect
        viewModel.onVideoTrimmedFromPath(path)
        onTrimmedVideoConsumed()
    }

    // The composer opens ready to type, unless it was opened with media.
    LaunchedEffect(Unit) {
        if (preloadedMediaUris.isEmpty() && trimmedVideoPath == null) {
            runCatching { captionFocusRequester.requestFocus() }
        }
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
        try {
            val cacheDir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File(cacheDir, "post_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.files",
                file,
            )
            tempCameraImageUri = uri
            cameraLauncher.launch(uri)
        } catch (_: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("امکان باز کردن دوربین وجود ندارد")
            }
        }
    }

    var activeInAppPickerMode by remember {
        mutableStateOf<ir.coffevista.vista_native.core.designsystem.component.VistaInAppPickerMode?>(null)
    }

    activeInAppPickerMode?.let { pickerMode ->
        val remainingSlots = (uiState.maxGalleryImages - uiState.selectedImages.size).coerceAtLeast(1)
        ir.coffevista.vista_native.core.designsystem.component.VistaInAppMediaPickerBottomSheet(
            mode = pickerMode,
            maxSelection = remainingSlots,
            onDismiss = { activeInAppPickerMode = null },
            onSingleSelected = { uri ->
                when (pickerMode) {
                    ir.coffevista.vista_native.core.designsystem.component.VistaInAppPickerMode.VIDEO_SINGLE -> {
                        viewModel.onVideoPicked(uri)
                        onNavigateToTrimmer(uri)
                    }
                    ir.coffevista.vista_native.core.designsystem.component.VistaInAppPickerMode.AUDIO_SINGLE -> {
                        viewModel.onMusicFileSelected(uri)
                    }
                    else -> viewModel.onImagesSelected(listOf(uri))
                }
            },
            onMultipleSelected = { uris ->
                if (uris.isNotEmpty()) {
                    viewModel.onImagesSelected(uris)
                }
            },
        )
    }

    fun pickImages() {
        if (uiState.selectedImages.size >= uiState.maxGalleryImages) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("حداکثر ${uiState.maxGalleryImages} تصویر می‌توانید انتخاب کنید")
            }
        } else {
            activeInAppPickerMode = ir.coffevista.vista_native.core.designsystem.component.VistaInAppPickerMode.IMAGE_MULTI
        }
    }

    fun pickVideo() {
        activeInAppPickerMode = ir.coffevista.vista_native.core.designsystem.component.VistaInAppPickerMode.VIDEO_SINGLE
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "پست جدید",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = ::handleBack) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            bottomBar = {
                // Docked above the keyboard: slim media bar, then counter ring + send button.
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding(),
                    ) {
                        AddPostMediaBar(
                            imageCount = uiState.selectedImages.size,
                            maxImages = uiState.maxGalleryImages,
                            hasVideo = uiState.isVideo && (uiState.selectedVideo != null || uiState.selectedVideoUri != null),
                            hasMusic = uiState.selectedMusicUri != null || uiState.selectedMusicTitle != null,
                            hasLocation = uiState.selectedLocation != null,
                            onPickImages = ::pickImages,
                            onCaptureCamera = ::launchCamera,
                            onPickVideo = ::pickVideo,
                            onPickMusic = {
                                if (!uiState.canPostMusic) {
                                    premiumPromptFeature = "انتشار موسیقی در پست"
                                    showPremiumBottomSheet = true
                                } else {
                                    activeInAppPickerMode = ir.coffevista.vista_native.core.designsystem.component.VistaInAppPickerMode.AUDIO_SINGLE
                                }
                            },
                            onPickLocation = { showLocationDialog = true },
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Character counter indicator ring
                            val maxLen = uiState.maxCharLength
                            val count = uiState.content.length
                            val progress = (count.toFloat() / maxLen).coerceIn(0f, 1f)
                            val remaining = maxLen - count

                            val indicatorColor = when {
                                count > maxLen -> MaterialTheme.colorScheme.error
                                count > maxLen * 0.8f -> Color(0xFFFFA726)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(44.dp),
                            ) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(42.dp),
                                    color = indicatorColor,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    strokeWidth = 3.dp,
                                )
                                Text(
                                    text = remaining.toString(),
                                    fontSize = if (remaining.toString().length >= 4) 9.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = indicatorColor,
                                )
                            }

                            // Send button: brand color, clearly disabled until there is something to post.
                            Button(
                                onClick = viewModel::submitPost,
                                enabled = uiState.canSubmit && !uiState.isUploading,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                            ) {
                                if (uiState.isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
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
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
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

                    val hasVideo = uiState.isVideo && (uiState.selectedVideo != null || uiState.selectedVideoUri != null)
                    val hasMedia = hasVideo || uiState.selectedImages.isNotEmpty()

                    // Author + caption + compact previews of everything attached.
                    AddPostComposer(
                        avatarUrl = uiState.authorAvatarUrl,
                        username = uiState.authorUsername,
                        fullName = uiState.authorFullName,
                        isVerified = uiState.authorIsVerified,
                        verificationType = uiState.authorVerificationType,
                        content = uiState.content,
                        onContentChanged = viewModel::onContentChanged,
                        focusRequester = captionFocusRequester,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when {
                                hasVideo -> {
                                    val previewUri = remember(uiState.selectedVideo, uiState.selectedVideoUri) {
                                        uiState.selectedVideo?.let(Uri::fromFile) ?: uiState.selectedVideoUri
                                    }
                                    if (previewUri != null) {
                                        VideoPreviewCard(
                                            videoUri = previewUri,
                                            aspectRatio = uiState.aspectRatio.floatValue,
                                            durationMs = uiState.videoDurationMs,
                                            onEdit = { onNavigateToTrimmer(uiState.videoSourceUri ?: previewUri) },
                                            onRemove = viewModel::removeVideo,
                                        )
                                    }
                                }

                                uiState.selectedImages.isNotEmpty() -> {
                                    ImagePreviewRow(
                                        images = uiState.selectedImages,
                                        aspectRatio = uiState.aspectRatio.floatValue,
                                        canAddMore = uiState.selectedImages.size < uiState.maxGalleryImages,
                                        onRemove = viewModel::removeImage,
                                        onAddMore = ::pickImages,
                                    )
                                }
                            }

                            if (uiState.selectedMusicUri != null || uiState.selectedMusicTitle != null) {
                                val musicUriString = uiState.selectedMusicUri?.toString().orEmpty()
                                val rangeSubtitle = if (uiState.musicDurationMs > 0) {
                                    "برش: ${TrimMath.formatTime(uiState.musicStartMs.toLong())} تا ${TrimMath.formatTime(uiState.musicEndMs.toLong())}"
                                } else {
                                    "موسیقی پست"
                                }

                                if (musicUriString.isNotBlank()) {
                                    SoundCloudMusicBubble(
                                        musicUrl = musicUriString,
                                        musicTitle = uiState.selectedMusicTitle ?: "موسیقی پست",
                                        subtitle = rangeSubtitle,
                                        trailingAction = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { showMusicTrimSheet = true },
                                                    modifier = Modifier.size(34.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Tune,
                                                        contentDescription = "تنظیم و برش موسیقی",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(19.dp),
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.onMusicSelected(null, null) },
                                                    modifier = Modifier.size(34.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "حذف موسیقی",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(19.dp),
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                } else {
                                    MusicAttachmentChip(
                                        title = uiState.selectedMusicTitle ?: "موسیقی انتخاب‌شده",
                                        rangeLabel = if (uiState.musicDurationMs > 0) {
                                            "${TrimMath.formatTime(uiState.musicStartMs.toLong())} – ${TrimMath.formatTime(uiState.musicEndMs.toLong())}"
                                        } else {
                                            null
                                        },
                                        onTap = { showMusicTrimSheet = true },
                                        onRemove = { viewModel.onMusicSelected(null, null) },
                                    )
                                }
                            }

                            uiState.selectedLocation?.let { location ->
                                InputChip(
                                    selected = true,
                                    onClick = { showLocationDialog = true },
                                    label = { Text(location, fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "حذف مکان",
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { viewModel.onLocationSelected(null) },
                                        )
                                    },
                                )
                            }
                        }
                    }

                    // Aspect Ratio Selector
                    if (hasMedia) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "نسبت تصویر:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
                    if (hasMedia) {
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

                            if (uiState.selectedMusicUri != null || uiState.selectedMusicTitle != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("پخش در پس‌زمینه", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text("موسیقی هنگام مشاهده پست به صورت خودکار در پس‌زمینه پخش می‌شود", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    ir.coffevista.vista_native.core.designsystem.component.VistaSwitch(
                                        checked = uiState.isMusicBackgroundMode,
                                        onCheckedChange = { viewModel.toggleMusicBackgroundMode(it) },
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !uiState.canHideEngagementCounts) {
                                        premiumPromptFeature = "مخفی کردن تعداد لایک‌ها"
                                        showPremiumBottomSheet = true
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("مخفی کردن تعداد لایک‌ها", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        if (!uiState.canHideEngagementCounts) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            PremiumTag()
                                        }
                                    }
                                    Text("تعداد لایک‌های این پست برای دیگران نمایش داده نمی‌شود", fontSize = 11.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                ir.coffevista.vista_native.core.designsystem.component.VistaSwitch(
                                    checked = uiState.hideLikeCount,
                                    enabled = uiState.canHideEngagementCounts,
                                    onCheckedChange = {
                                        if (uiState.canHideEngagementCounts) {
                                            viewModel.toggleHideLikeCount(it)
                                        } else {
                                            premiumPromptFeature = "مخفی کردن تعداد لایک‌ها"
                                            showPremiumBottomSheet = true
                                        }
                                    },
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !uiState.canHideEngagementCounts) {
                                        premiumPromptFeature = "مخفی کردن تعداد نظرات"
                                        showPremiumBottomSheet = true
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("مخفی کردن تعداد نظرات", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        if (!uiState.canHideEngagementCounts) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            PremiumTag()
                                        }
                                    }
                                    Text("تعداد نظرات این پست برای دیگران نمایش داده نمی‌شود", fontSize = 11.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                ir.coffevista.vista_native.core.designsystem.component.VistaSwitch(
                                    checked = uiState.hideCommentCount,
                                    enabled = uiState.canHideEngagementCounts,
                                    onCheckedChange = {
                                        if (uiState.canHideEngagementCounts) {
                                            viewModel.toggleHideCommentCount(it)
                                        } else {
                                            premiumPromptFeature = "مخفی کردن تعداد نظرات"
                                            showPremiumBottomSheet = true
                                        }
                                    },
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

                // Promotional upgrade banner ONLY for normal users (clicking opens Vista Premium Bottom Sheet)
                if (!uiState.tierCapabilities.isTickOrPremium) {
                    val bannerGradient = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4F46E5),
                            Color(0xFF8B5CF6),
                            Color(0xFFD946EF),
                        ),
                    )
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 8.dp, start = 14.dp, end = 14.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(bannerGradient)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFFFD54F).copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .clickable {
                                    premiumPromptFeature = null
                                    showPremiumBottomSheet = true
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFFFE082), Color(0xFFFFB300)),
                                            ),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        text = "ارتقا به ویستا پریمیوم",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "تا ۱۰۰۰ کاراکتر، موزیک و امکانات ویژه",
                                        color = Color.White.copy(alpha = 0.88f),
                                        fontSize = 10.5.sp,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 24.dp)
                                .size(9.dp)
                                .graphicsLayer { rotationZ = 45f }
                                .background(Color(0xFF4F46E5)),
                        )
                    }
                }
            }
        }

        // Location Dialog
        if (showDiscardDraftDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDraftDialog = false },
                title = {
                    Text(
                        text = "ذخیره پیش‌نویس؟",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                },
                text = {
                    Text(
                        text = "می‌توانید پست خود را ذخیره کنید تا بعداً به تکمیل آن ادامه دهید.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.saveDraft()
                            showDiscardDraftDialog = false
                            onBack()
                        },
                    ) {
                        Text("ذخیره پیش‌نویس", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                viewModel.discardDraft()
                                showDiscardDraftDialog = false
                                onBack()
                            },
                        ) {
                            Text("حذف", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { showDiscardDraftDialog = false }) {
                            Text("انصراف")
                        }
                    }
                },
            )
        }

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
        val trimMusicUri = uiState.selectedMusicUri
        if (showMusicTrimSheet && trimMusicUri != null) {
            MusicTrimSheet(
                title = uiState.selectedMusicTitle.orEmpty(),
                musicUri = trimMusicUri,
                durationMs = uiState.musicDurationMs.takeIf { it > 0 } ?: 30_000,
                initialStartMs = uiState.musicStartMs,
                initialEndMs = uiState.musicEndMs,
                maxClipMs = if (uiState.isPremium) 60_000 else 15_000,
                isBackgroundMode = uiState.isMusicBackgroundMode,
                onBackgroundModeChange = viewModel::toggleMusicBackgroundMode,
                onConfirm = { start, end ->
                    viewModel.onMusicTrimChanged(start, end)
                    showMusicTrimSheet = false
                },
                onRemove = {
                    viewModel.onMusicSelected(null, null)
                    showMusicTrimSheet = false
                },
                onDismiss = { showMusicTrimSheet = false },
            )
        }

        if (showPremiumBottomSheet) {
            VistaPremiumBottomSheet(
                feature = premiumPromptFeature,
                onDismiss = {
                    showPremiumBottomSheet = false
                    premiumPromptFeature = null
                },
                onUpgradeClick = onNavigateToPremium,
            )
        }
    }
}

@Composable
private fun PremiumTag() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFFFB300),
    ) {
        Text(
            text = "پریمیوم",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
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
