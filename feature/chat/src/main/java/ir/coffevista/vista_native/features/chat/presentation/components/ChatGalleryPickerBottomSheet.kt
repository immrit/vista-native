package ir.coffevista.vista_native.features.chat.presentation.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * آیتم رسانه گالری دستگاه (عکس یا ویدیو).
 */
internal data class ChatGalleryMediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val dateAddedSec: Long = 0L,
)

/**
 * استخراج سریع آخرین رسانه‌های گالری از MediaStore با تفکیک تصویر و ویدیو.
 */
internal suspend fun queryChatGalleryMedia(
    context: Context,
    limit: Int = 200,
): List<ChatGalleryMediaItem> = withContext(Dispatchers.IO) {
    runCatching {
        val items = ArrayList<ChatGalleryMediaItem>(limit)

        // ۱. واکشی تصاویر
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
        )
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val id = cursor.getLong(idCol)
                val dateSec = cursor.getLong(dateCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                items.add(ChatGalleryMediaItem(id = id, uri = uri, isVideo = false, dateAddedSec = dateSec))
                count++
            }
        }

        // ۲. واکشی ویدیوها
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val id = cursor.getLong(idCol)
                val dur = cursor.getLong(durCol)
                val dateSec = cursor.getLong(dateCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                items.add(ChatGalleryMediaItem(id = id, uri = uri, isVideo = true, durationMs = dur, dateAddedSec = dateSec))
                count++
            }
        }

        items.sortedByDescending { it.dateAddedSec }.take(limit)
    }.getOrDefault(emptyList())
}

/**
 * بررسی دسترسی خواندن رسانه‌ها بر اساس نسخه سیستم‌عامل اندروید.
 */
internal fun checkChatGalleryPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * محتوای اختصاصی انتخاب‌گر گالری به سبک تلگرام با پشتیبانی از انتخاب چندگانه،
 * نمایش ویدیوها و زمان، درخواست دسترسی هوشمند و دکمه ارسال اختصاصی.
 */
@Composable
fun ChatGalleryPickerContent(
    onDismiss: () -> Unit,
    onBack: (() -> Unit)? = null,
    onMediaSelected: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(checkChatGalleryPermission(context)) }
    var mediaItems by remember { mutableStateOf<List<ChatGalleryMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val selectedUris = remember { mutableStateListOf<Uri>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        hasPermission = granted
    }

    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onMediaSelected(uris)
            onDismiss()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            mediaItems = queryChatGalleryMedia(context)
            isLoading = false
        }
    }

    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // هدر انتخاب‌گر گالری (دکمه بازگشت / بستن، عنوان، دکمه انتخابگر سیستم)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = "گالری",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (selectedUris.isNotEmpty()) {
                            Text(
                                text = "${selectedUris.size} مورد انتخاب شده".toPersianDigits(),
                                fontSize = 11.5.sp,
                                color = VistaBrandColors.Indigo,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            systemPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "انتخاب از سیستم",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.8.dp,
            )

            // بدنه محتوا: گرید رسانه‌ها یا پیام درخواست دسترسی
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    !hasPermission -> {
                        // حالت عدم اعطای دسترسی به گالری
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(VistaBrandColors.Indigo.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = VistaBrandColors.Indigo,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "دسترسی به تصاویر و ویدیوها",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "برای مشاهده و انتخاب سریع تصاویر و ویدیوها، به اجازه دسترسی به حافظه دستگاه نیاز است.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                            )
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        arrayOf(
                                            Manifest.permission.READ_MEDIA_IMAGES,
                                            Manifest.permission.READ_MEDIA_VIDEO,
                                        )
                                    } else {
                                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                    permissionLauncher.launch(permissions)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Indigo),
                            ) {
                                Text("تأیید و اعطای دسترسی", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    systemPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                            ) {
                                Text("استفاده از انتخاب‌گر سیستم", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = VistaBrandColors.Indigo,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }

                    mediaItems.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "هیچ تصویری در دستگاه یافت نشد",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                        }
                    }

                    else -> {
                        // گرید رسانه‌ها ۳ ستونه مشابه تلگرام
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(mediaItems, key = { it.id }) { item ->
                                val selectedIndex = selectedUris.indexOf(item.uri)
                                val isSelected = selectedIndex >= 0

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = 2.5.dp,
                                                    color = VistaBrandColors.Indigo,
                                                    shape = RoundedCornerShape(4.dp),
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            if (isSelected) {
                                                selectedUris.remove(item.uri)
                                            } else {
                                                selectedUris.add(item.uri)
                                            }
                                        },
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(item.uri)
                                            .crossfade(true)
                                            .size(320)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )

                                    // گرادیان ملایم و نمایش مدت زمان ویدیو
                                    if (item.isVideo) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomStart)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                                    )
                                                )
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp),
                                                )
                                                Text(
                                                    text = formatGalleryDuration(item.durationMs),
                                                    color = Color.White,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                        }
                                    }

                                    // نشانگر دایره‌ای انتخاب چندگانه به سبک تلگرام
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.background(
                                                        Brush.linearGradient(
                                                            listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep)
                                                        )
                                                    )
                                                } else {
                                                    Modifier
                                                        .background(Color.Black.copy(alpha = 0.35f))
                                                        .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isSelected) {
                                            Text(
                                                text = (selectedIndex + 1).toString().toPersianDigits(),
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

            // نوار پایینی دکمه ارسال به سبک تلگرام
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectedUris.isNotEmpty()) {
                        TextButton(onClick = { selectedUris.clear() }) {
                            Text(
                                text = "لغو انتخاب‌ها",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                            )
                        }

                        Button(
                            onClick = {
                                onMediaSelected(selectedUris.toList())
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Indigo),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "ارسال (${selectedUris.size})".toPersianDigits(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    } else {
                        Text(
                            text = "تصاویر یا ویدیوهای مورد نظر را انتخاب کنید",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * باتم‌شیت کامل اختصاصی گالری به سبک تلگرام برای گفت‌وگو.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGalleryPickerBottomSheet(
    onDismiss: () -> Unit,
    onMediaSelected: (List<Uri>) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        ChatGalleryPickerContent(
            onDismiss = onDismiss,
            onMediaSelected = onMediaSelected,
        )
    }
}

private fun formatGalleryDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00".toPersianDigits()
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds).toPersianDigits()
}
