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
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.remember
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

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onPostCreated()
        }
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
                        Column {
                            Text("مخفی کردن تعداد لایک‌ها", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("تعداد لایک‌های این پست برای دیگران نمایش داده نمی‌شود", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = uiState.hideLikeCount,
                            onCheckedChange = viewModel::toggleHideLikeCount,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("بستن نظرات", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("امکان ثبت نظر روی این پست غیرفعال می‌شود", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = uiState.commentsDisabled,
                            onCheckedChange = viewModel::toggleCommentsDisabled,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
