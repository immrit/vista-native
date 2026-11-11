package ir.coffevista.vista_native.features.chat.presentation.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.component.VistaGalleryFilter
import ir.coffevista.vista_native.core.designsystem.component.VistaGalleryMediaItem
import ir.coffevista.vista_native.core.designsystem.component.VistaMediaBrowserHelpers
import ir.coffevista.vista_native.core.designsystem.component.hasVistaGalleryPermission
import ir.coffevista.vista_native.core.designsystem.component.openVistaAppSettings
import ir.coffevista.vista_native.core.designsystem.component.queryVistaGalleryMedia
import ir.coffevista.vista_native.core.designsystem.component.requiredVistaGalleryPermissions
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassScrollSync
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassSource
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import java.util.Locale
import kotlin.math.abs

/**
 * نشانگر دایره‌ای انتخاب چندگانه تلگرامی.
 */
@Composable
internal fun TelegramSelectionNumberBadge(
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val isSelected = selectedIndex >= 0
    Box(
        modifier = modifier
            .size(23.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2AABEE), VistaBrandColors.Indigo),
                        ),
                    )
                } else {
                    Modifier
                        .background(Color.Black.copy(alpha = 0.28f))
                        .border(1.6.dp, Color.White.copy(alpha = 0.92f), CircleShape)
                },
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

/**
 * گرید گالری تلگرامی (با کاشی ۲-ردیفه پیش‌نمایش دوربین در ابتدای گرید و انتخاب چندگانه).
 * بدون استفاده از گالری یا فایل‌منیجر سیستم‌عامل.
 */
@Composable
internal fun TelegramGalleryGridContent(
    selectedUris: MutableList<Uri>,
    filter: VistaGalleryFilter = VistaGalleryFilter.ALL,
    onCameraTileClick: () -> Unit,
    hasCameraPermission: Boolean,
    isCameraPreviewActive: Boolean,
    gridState: LazyGridState = rememberLazyGridState(),
    glassSourceState: VistaGlassState? = null,
    bottomContentPadding: Dp = 92.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var hasPermission by remember { mutableStateOf(hasVistaGalleryPermission(context)) }
    var allMediaItems by remember { mutableStateOf<List<VistaGalleryMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.any { it } || hasVistaGalleryPermission(context)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            allMediaItems = queryVistaGalleryMedia(context, filter = VistaGalleryFilter.ALL, limit = 240)
            isLoading = false
        }
    }

    val mediaItems = remember(allMediaItems, filter) {
        VistaMediaBrowserHelpers.filterGalleryItems(allMediaItems, filter = filter)
    }

    if (glassSourceState != null) {
        VistaGlassScrollSync(glassSourceState, gridState)
    }

    val sourceModifier = if (glassSourceState != null) {
        modifier.vistaGlassSource(glassSourceState)
    } else {
        modifier
    }

    Box(modifier = sourceModifier.fillMaxSize()) {
        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = bottomContentPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2AABEE).copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF2AABEE),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "دسترسی به تصاویر و ویدیوها",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "برای مشاهده سریع گالری درون برنامه مثل تلگرام، اجازه دسترسی به رسانه‌های دستگاه را بدهید.",
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(requiredVistaGalleryPermissions())
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2AABEE)),
                    ) {
                        Text("تأیید و اعطای دسترسی", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { openVistaAppSettings(context) }) {
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

            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomContentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2AABEE),
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            mediaItems.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp, bottom = bottomContentPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = when (filter) {
                            VistaGalleryFilter.VIDEOS -> "هیچ ویدیویی در دستگاه یافت نشد"
                            VistaGalleryFilter.IMAGES -> "هیچ تصویری در دستگاه یافت نشد"
                            else -> "هیچ تصویر یا ویدیویی در دستگاه یافت نشد"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.5.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onCameraTileClick) {
                        Text("ثبت تصویر با دوربین", color = Color(0xFF2AABEE), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            else -> {
                val heroItems = mediaItems.take(4)
                val remainingItems = if (mediaItems.size > 4) mediaItems.drop(4) else emptyList()
                val tileSpacing = 2.dp

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        end = 2.dp,
                        top = 2.dp,
                        bottom = bottomContentPadding,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(tileSpacing),
                    verticalArrangement = Arrangement.spacedBy(tileSpacing),
                ) {
                    item(
                        key = "telegram-camera-hero-block",
                        span = { GridItemSpan(3) },
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val cellSize = ((maxWidth - tileSpacing * 2) / 3f).coerceAtLeast(48.dp)
                            val heroHeight = cellSize * 2 + tileSpacing

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(heroHeight),
                                horizontalArrangement = Arrangement.spacedBy(tileSpacing),
                            ) {
                                // کاشی ۲-ردیفه دوربین (مشابه تلگرام)
                                TelegramCameraHeroTile(
                                    hasCameraPermission = hasCameraPermission,
                                    isPreviewActive = isCameraPreviewActive,
                                    isDark = isDark,
                                    onClick = onCameraTileClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )

                                // ۴ کاشی مربع ۲×۲ در کنار دوربین
                                Column(
                                    modifier = Modifier
                                        .weight(2f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(tileSpacing),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(tileSpacing),
                                    ) {
                                        for (slot in 0..1) {
                                            val item = heroItems.getOrNull(slot)
                                            if (item != null) {
                                                val uri = item.toUri()
                                                TelegramGalleryMediaTile(
                                                    item = item,
                                                    selectedIndex = selectedUris.indexOf(uri),
                                                    onToggle = {
                                                        if (uri in selectedUris) {
                                                            selectedUris.remove(uri)
                                                        } else {
                                                            selectedUris.add(uri)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight(),
                                                )
                                            } else {
                                                Spacer(Modifier.weight(1f).fillMaxHeight())
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(tileSpacing),
                                    ) {
                                        for (slot in 2..3) {
                                            val item = heroItems.getOrNull(slot)
                                            if (item != null) {
                                                val uri = item.toUri()
                                                TelegramGalleryMediaTile(
                                                    item = item,
                                                    selectedIndex = selectedUris.indexOf(uri),
                                                    onToggle = {
                                                        if (uri in selectedUris) {
                                                            selectedUris.remove(uri)
                                                        } else {
                                                            selectedUris.add(uri)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight(),
                                                )
                                            } else {
                                                Spacer(Modifier.weight(1f).fillMaxHeight())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(remainingItems, key = { it.id }) { item ->
                        val uri = item.toUri()
                        TelegramGalleryMediaTile(
                            item = item,
                            selectedIndex = selectedUris.indexOf(uri),
                            onToggle = {
                                if (uri in selectedUris) {
                                    selectedUris.remove(uri)
                                } else {
                                    selectedUris.add(uri)
                                }
                            },
                            modifier = Modifier.aspectRatio(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelegramGalleryMediaTile(
    item: VistaGalleryMediaItem,
    selectedIndex: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val itemUri = remember(item.uriString) { item.toUri() }
    val isSelected = selectedIndex >= 0
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "gallery-tile-scale",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF141A22).copy(alpha = 0.25f))
            .clickable(onClick = onToggle),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(itemUri)
                .crossfade(true)
                .size(320)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(if (isSelected) 8.dp else 4.dp))
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = Color(0xFF2AABEE),
                            shape = RoundedCornerShape(8.dp),
                        )
                    } else {
                        Modifier
                    },
                ),
        )

        // برچسب زمان ویدیو
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                        ),
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

        // نشانگر دایره‌ای انتخاب چندگانه تلگرامی
        TelegramSelectionNumberBadge(
            selectedIndex = selectedIndex,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
        )
    }
}

@Composable
private fun TelegramCameraHeroTile(
    hasCameraPermission: Boolean,
    isPreviewActive: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    if (isDark) {
                        listOf(Color(0xFF1E2631), Color(0xFF11161D))
                    } else {
                        listOf(Color(0xFF273240), Color(0xFF161D26))
                    }
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (hasCameraPermission && isPreviewActive) {
            LightweightBackCameraPreview(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(30.dp),
                )
                Text(
                    text = "دوربین",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // بج دایره‌ای سفید دوربین در گوشه بالا (مشابه اسکرین‌شات تلگرام)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(7.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "دوربین",
                tint = Color(0xFF18202A),
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

/**
 * پیش‌نمایش سبک Camera2 برای کاشی دوربین در شیت پیوست.
 * هنگام بسته شدن شیت یا باز شدن دوربین سیستم به‌صورت خودکار و ایمن آزاد می‌شود.
 */
@SuppressLint("MissingPermission")
@Composable
private fun LightweightBackCameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val previewHolder = remember { mutableStateOf<CameraDevice?>(null) }
    val sessionHolder = remember { mutableStateOf<CameraCaptureSession?>(null) }
    val handlerThread = remember {
        HandlerThread("VistaAttachmentCamPreview").apply { start() }
    }
    val backgroundHandler = remember(handlerThread) { Handler(handlerThread.looper) }

    fun closeCamera() {
        runCatching { sessionHolder.value?.close() }
        sessionHolder.value = null
        runCatching { previewHolder.value?.close() }
        previewHolder.value = null
    }

    DisposableEffect(Unit) {
        onDispose {
            closeCamera()
            runCatching { handlerThread.quitSafely() }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        openPreviewCamera(
                            context = ctx,
                            textureView = this@apply,
                            surfaceTexture = surface,
                            viewWidth = width,
                            viewHeight = height,
                            handler = backgroundHandler,
                            onOpened = { device, session ->
                                previewHolder.value = device
                                sessionHolder.value = session
                            },
                        )
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        configurePreviewTransform(ctx, this@apply, width, height)
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        closeCamera()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
            }
        },
    )
}

@SuppressLint("MissingPermission")
private fun openPreviewCamera(
    context: Context,
    textureView: TextureView,
    surfaceTexture: SurfaceTexture,
    viewWidth: Int,
    viewHeight: Int,
    handler: Handler,
    onOpened: (CameraDevice, CameraCaptureSession) -> Unit,
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        return
    }
    runCatching {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        val backCameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
        } ?: manager.cameraIdList.firstOrNull() ?: return

        val previewSize = choosePreviewSize(manager, backCameraId)
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        configurePreviewTransform(context, textureView, viewWidth, viewHeight, previewSize)

        manager.openCamera(
            backCameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    runCatching {
                        val surface = Surface(surfaceTexture)
                        val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(surface)
                            set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                            )
                        }
                        @Suppress("DEPRECATION")
                        camera.createCaptureSession(
                            listOf(surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    runCatching {
                                        session.setRepeatingRequest(request.build(), null, handler)
                                        onOpened(camera, session)
                                    }.onFailure { camera.close() }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    camera.close()
                                }
                            },
                            handler,
                        )
                    }.onFailure { camera.close() }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                }
            },
            handler,
        )
    }
}

private fun choosePreviewSize(manager: CameraManager, cameraId: String): Size {
    val map = manager.getCameraCharacteristics(cameraId)
        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        ?: return Size(640, 480)
    val choices = map.getOutputSizes(SurfaceTexture::class.java)
        ?.filter { it.width in 320..1280 && it.height in 240..960 }
        ?.sortedBy { abs(it.width * it.height - 640 * 480) }
        .orEmpty()
    return choices.firstOrNull() ?: Size(640, 480)
}

private fun configurePreviewTransform(
    context: Context,
    textureView: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    bufferSize: Size = Size(640, 480),
) {
    if (viewWidth <= 0 || viewHeight <= 0) return
    val matrix = Matrix()
    val viewRectW = viewWidth.toFloat()
    val viewRectH = viewHeight.toFloat()
    val centerX = viewRectW / 2f
    val centerY = viewRectH / 2f

    // Portrait orientation swap for sensor buffer dimensions
    val bufW = bufferSize.height.toFloat().coerceAtLeast(1f)
    val bufH = bufferSize.width.toFloat().coerceAtLeast(1f)
    val scale = maxOf(viewRectW / bufW, viewRectH / bufH)
    val sx = (bufW * scale) / viewRectW
    val sy = (bufH * scale) / viewRectH
    matrix.setScale(sx, sy, centerX, centerY)
    textureView.post { textureView.setTransform(matrix) }
}

/**
 * محتوای انتخاب‌گر گالری مستقل (با هدر و نوار ارسال داخلی).
 */
@Composable
fun ChatGalleryPickerContent(
    onDismiss: () -> Unit,
    onBack: (() -> Unit)? = null,
    onMediaSelected: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var activeFilter by remember { mutableStateOf(VistaGalleryFilter.ALL) }
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        VistaGalleryFilter.ALL to "همه",
                        VistaGalleryFilter.IMAGES to "عکس",
                        VistaGalleryFilter.VIDEOS to "ویدیو",
                    ).forEach { (filterOption, label) ->
                        val selected = activeFilter == filterOption
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) Color(0xFF2AABEE).copy(alpha = 0.16f)
                                    else Color.Transparent,
                                )
                                .clickable { activeFilter = filterOption }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color(0xFF2AABEE) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.8.dp,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                TelegramGalleryGridContent(
                    selectedUris = selectedUris,
                    filter = activeFilter,
                    onCameraTileClick = { onBack?.invoke() },
                    hasCameraPermission = hasCameraPermission,
                    isCameraPreviewActive = false,
                    bottomContentPadding = 12.dp,
                )
            }

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

private fun formatGalleryDuration(durationMs: Long): String =
    VistaMediaBrowserHelpers.formatMediaDuration(durationMs)
