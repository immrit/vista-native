package ir.coffevista.vista_native.features.feed.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    onBack: () -> Unit,
    onNavigateToTrimmer: (Uri) -> Unit,
    onPostCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddPostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showMusicTrimSheet by remember { mutableStateOf(false) }

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

    uiState.errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val brandGradient = Brush.linearGradient(
        colors = listOf(VistaBrandColors.Indigo, VistaBrandColors.Pink),
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
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
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = viewModel::submitPost,
                            enabled = uiState.canSubmit,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VistaBrandColors.Indigo,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            if (uiState.isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = "انتشار",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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

                // Media Preview / Pickers
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            Column {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    items(uiState.selectedImages) { imageUri ->
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(14.dp),
                                                ),
                                        ) {
                                            AsyncImage(
                                                model = imageUri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                            IconButton(
                                                onClick = { viewModel.removeImage(imageUri) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
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
                            }
                        }

                        // Empty State: Show Media Picker Cards
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Card(
                                    onClick = {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = VistaBrandColors.Indigo,
                                            modifier = Modifier.size(32.dp),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "انتخاب تصویر",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "تا ${uiState.maxGalleryImages} عکس",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                        )
                                    }
                                }

                                Card(
                                    onClick = {
                                        videoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(110.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = VistaBrandColors.Pink,
                                            modifier = Modifier.size(32.dp),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "انتخاب ویدیو",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "تا ${uiState.maxVideoDurationMs / 1000} ثانیه",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Aspect Ratio Selector
                if (uiState.selectedImages.isNotEmpty() || uiState.selectedVideo != null) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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

                Spacer(modifier = Modifier.height(12.dp))

                // Caption TextField
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChanged,
                    placeholder = {
                        Text(
                            text = "درباره این پست بنویسید… (از #هشتگ‌ها برای دیده شدن بیشتر استفاده کنید)",
                            fontSize = 14.sp,
                            color = Color.Gray,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(140.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    maxLines = 6,
                )

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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "${uiState.content.length}/${uiState.maxCharLength}",
                        fontSize = 12.sp,
                        color = if (uiState.content.length >= uiState.maxCharLength) MaterialTheme.colorScheme.error else Color.Gray,
                    )
                }

                // Location & Music Attachment Chips
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

                // Post Settings
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

                Spacer(modifier = Modifier.height(40.dp))
            }
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
