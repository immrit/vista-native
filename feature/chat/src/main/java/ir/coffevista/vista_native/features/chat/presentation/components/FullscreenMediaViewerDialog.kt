package ir.coffevista.vista_native.features.chat.presentation.components

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Window
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassSource
import ir.coffevista.vista_native.features.chat.presentation.toPersianDigits
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class MediaViewerItem(
    val url: String,
    val isVideo: Boolean,
    val title: String? = null,
    val caption: String? = null,
    val isGif: Boolean = false,
    val messageKey: String? = null,
    val senderName: String? = null,
    val epochMillis: Long? = null,
    val isMine: Boolean = false,
)

@OptIn(UnstableApi::class)
@Composable
fun FullscreenMediaViewerDialog(
    mediaItems: List<MediaViewerItem>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sourceBounds: Rect? = null,
    onForward: ((MediaViewerItem) -> Unit)? = null,
    onDelete: ((MediaViewerItem) -> Unit)? = null,
    onShowInChat: ((MediaViewerItem) -> Unit)? = null,
) {
    if (mediaItems.isEmpty()) return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val safeInitialPage = initialIndex.coerceIn(0, mediaItems.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeInitialPage) { mediaItems.size }

    var chromeVisible by remember { mutableStateOf(true) }
    var currentItemScale by remember { mutableFloatStateOf(1f) }
    var verticalDragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingToDismiss by remember { mutableStateOf(false) }
    var statusBannerText by remember { mutableStateOf<String?>(null) }
    var isSavingToGallery by remember { mutableStateOf(false) }

    // Dedicated animated-GIF-capable Coil ImageLoader for the viewer.
    val viewerImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
    DisposableEffect(viewerImageLoader) {
        onDispose { viewerImageLoader.shutdown() }
    }

    LaunchedEffect(statusBannerText) {
        if (statusBannerText != null) {
            delay(2_200L)
            statusBannerText = null
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        currentItemScale = 1f
        verticalDragOffsetY = 0f
        isDraggingToDismiss = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        ConfigureViewerSystemBars(chromeVisible = chromeVisible && !isDraggingToDismiss)

        val mediaGlassState = rememberVistaGlassState()
        val density = LocalDensity.current
        val dismissThresholdPx = with(density) { 130.dp.toPx() }
        val dragFraction = (abs(verticalDragOffsetY) / (dismissThresholdPx * 2.2f)).coerceIn(0f, 1f)
        val backgroundAlpha = (1f - dragFraction * 0.72f).coerceIn(0.22f, 1f)

        val currentItem = mediaItems.getOrNull(pagerState.currentPage)

        // Subtle opening entrance from source bounds if provided.
        val openScale = remember(sourceBounds) { Animatable(if (sourceBounds != null) 0.92f else 0.96f) }
        val openAlpha = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            launch { openAlpha.animateTo(1f, tween(180)) }
            launch {
                openScale.animateTo(
                    1f,
                    spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                )
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha)),
        ) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                beyondViewportPageCount = 1,
                userScrollEnabled = currentItemScale <= 1.02f && !isDraggingToDismiss,
                modifier = Modifier
                    .fillMaxSize()
                    .vistaGlassSource(mediaGlassState)
                    .graphicsLayer {
                        val s = openScale.value * (1f - dragFraction * 0.14f)
                        scaleX = s
                        scaleY = s
                        translationY = verticalDragOffsetY
                        alpha = openAlpha.value
                    },
            ) { page ->
                val item = mediaItems[page]
                if (item.isVideo) {
                    VideoPage(
                        videoUrl = item.url,
                        isCurrentPage = pagerState.currentPage == page,
                        chromeVisible = chromeVisible && !isDraggingToDismiss,
                        glassState = mediaGlassState,
                        onToggleChrome = { chromeVisible = !chromeVisible },
                        onVerticalDragDelta = { delta ->
                            isDraggingToDismiss = true
                            verticalDragOffsetY += delta
                        },
                        onVerticalDragRelease = {
                            if (abs(verticalDragOffsetY) >= dismissThresholdPx) {
                                onDismiss()
                            } else {
                                verticalDragOffsetY = 0f
                                isDraggingToDismiss = false
                            }
                        },
                    )
                } else {
                    ZoomableImagePage(
                        imageUrl = item.url,
                        isGif = item.isGif,
                        contentDescription = item.title ?: "تصویر",
                        imageLoader = viewerImageLoader,
                        isCurrentPage = pagerState.currentPage == page,
                        onSingleTap = { chromeVisible = !chromeVisible },
                        onScaleChanged = { scale ->
                            if (pagerState.currentPage == page) {
                                currentItemScale = scale
                            }
                        },
                        onVerticalDragDelta = { delta ->
                            isDraggingToDismiss = true
                            verticalDragOffsetY += delta
                        },
                        onVerticalDragRelease = {
                            if (abs(verticalDragOffsetY) >= dismissThresholdPx) {
                                onDismiss()
                            } else {
                                verticalDragOffsetY = 0f
                                isDraggingToDismiss = false
                            }
                        },
                    )
                }
            }

            // هدر تلگرامی با قابلیت مخفی‌سازی کامل با لمس عکس (نمایش تمام‌صفحه عکس)
            AnimatedVisibility(
                visible = chromeVisible && !isDraggingToDismiss,
                enter = fadeIn(tween(180)) + slideInVertically(tween(200)) { -it },
                exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { -it },
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                ViewerTopBar(
                    glassState = mediaGlassState,
                    currentIndex = pagerState.currentPage,
                    totalCount = mediaItems.size,
                    senderName = currentItem?.senderName,
                    epochMillis = currentItem?.epochMillis,
                    title = currentItem?.title,
                    onBack = onDismiss,
                )
            }

            // فوتر تلگرامی: کپشن قابل باز شدن + نوار اکشن‌ها (ذخیره، اشتراک، نمایش در چت، فوروارد، حذف)
            AnimatedVisibility(
                visible = chromeVisible && !isDraggingToDismiss && currentItem != null,
                enter = fadeIn(tween(180)) + slideInVertically(tween(200)) { it },
                exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                currentItem?.let { item ->
                    ViewerBottomBar(
                        glassState = mediaGlassState,
                        item = item,
                        isSaving = isSavingToGallery,
                        onSaveToGallery = {
                            if (!isSavingToGallery) {
                                isSavingToGallery = true
                                coroutineScope.launch {
                                    val saved = saveMediaToGallery(context, item)
                                    isSavingToGallery = false
                                    statusBannerText = if (saved) {
                                        "در گالری ذخیره شد"
                                    } else {
                                        "ذخیره رسانه انجام نشد"
                                    }
                                }
                            }
                        },
                        onShare = {
                            coroutineScope.launch {
                                val ok = shareMediaItem(context, item)
                                if (!ok) statusBannerText = "اشتراک‌گذاری امکان‌پذیر نیست"
                            }
                        },
                        onForward = onForward?.let { cb -> { cb(item) } },
                        onShowInChat = onShowInChat?.let { cb -> { cb(item) } },
                        onDelete = if (item.isMine && onDelete != null) {
                            { onDelete(item) }
                        } else null,
                    )
                }
            }

            // پیغام وضعیت شیشه‌ای (ذخیره در گالری / خطا)
            AnimatedVisibility(
                visible = statusBannerText != null,
                enter = fadeIn(tween(160)) + slideInVertically(tween(180)) { -it / 2 },
                exit = fadeOut(tween(140)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 68.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .vistaGlass(
                            state = mediaGlassState,
                            shape = RoundedCornerShape(20.dp),
                            tint = Color(0xFF161C24),
                            isDark = true,
                            blurRadius = VistaGlassTokens.PillBlur,
                            tintAlphaOverride = 0.78f,
                            rimWidth = 0.7.dp,
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = statusBannerText.orEmpty(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
fun FullscreenMediaViewerDialog(
    mediaUrl: String,
    isVideo: Boolean,
    title: String? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = FullscreenMediaViewerDialog(
    mediaItems = listOf(MediaViewerItem(url = mediaUrl, isVideo = isVideo, title = title)),
    initialIndex = 0,
    onDismiss = onDismiss,
    modifier = modifier,
)

@Composable
private fun ConfigureViewerSystemBars(chromeVisible: Boolean) {
    val view = LocalView.current
    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
    val activityWindow = remember(view) { view.context.findViewerActivity()?.window }

    DisposableEffect(dialogWindow, activityWindow) {
        val targetWindow = dialogWindow ?: activityWindow
        if (targetWindow != null) {
            WindowCompat.setDecorFitsSystemWindows(targetWindow, false)
            @Suppress("DEPRECATION")
            targetWindow.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            targetWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                targetWindow.attributes = targetWindow.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        onDispose {
            val restoreWindow = dialogWindow ?: activityWindow
            if (restoreWindow != null) {
                val controller = WindowCompat.getInsetsController(restoreWindow, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(chromeVisible, dialogWindow, activityWindow) {
        val targetWindow = dialogWindow ?: activityWindow ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(targetWindow, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        if (chromeVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun ViewerTopBar(
    glassState: VistaGlassState,
    currentIndex: Int,
    totalCount: Int,
    senderName: String?,
    epochMillis: Long?,
    title: String?,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.62f),
                        Color.Black.copy(alpha = 0.0f),
                    ),
                ),
            )
            .vistaGlass(
                state = glassState,
                shape = RectangleShape,
                tint = Color(0xFF0A0E14),
                isDark = true,
                blurRadius = VistaGlassTokens.BarBlur,
                tintAlphaOverride = 0.44f,
                drawHighlight = false,
                rimWidth = 0.dp,
            )
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بستن نمایشگر",
                    tint = Color.White,
                )
            }

            Spacer(Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                val primaryLabel = senderName?.takeIf(String::isNotBlank)
                    ?: title?.takeIf(String::isNotBlank)
                    ?: "رسانه"
                Text(
                    text = primaryLabel,
                    color = Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val dateLabel = epochMillis?.takeIf { it > 0L }?.let(::formatViewerTimestamp)
                if (dateLabel != null) {
                    Text(
                        text = dateLabel.toPersianDigits(),
                        color = Color.White.copy(alpha = 0.74f),
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
            }

            if (totalCount > 1) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${currentIndex + 1} از $totalCount".toPersianDigits(),
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerBottomBar(
    glassState: VistaGlassState,
    item: MediaViewerItem,
    isSaving: Boolean,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit,
    onForward: (() -> Unit)?,
    onShowInChat: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var captionExpanded by remember(item.url, item.caption) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.0f),
                        Color.Black.copy(alpha = 0.68f),
                    ),
                ),
            )
            .vistaGlass(
                state = glassState,
                shape = RectangleShape,
                tint = Color(0xFF0A0E14),
                isDark = true,
                blurRadius = VistaGlassTokens.BarBlur,
                tintAlphaOverride = 0.48f,
                drawHighlight = false,
                rimWidth = 0.dp,
            )
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val cleanCaption = item.caption?.trim().orEmpty()
        if (cleanCaption.isNotEmpty()) {
            Text(
                text = cleanCaption,
                color = Color.White.copy(alpha = 0.94f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = if (captionExpanded) 12 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(180))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { captionExpanded = !captionExpanded }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }

        // برای ویدیوها فاصله بالای نوار اکشن می‌افتد؛ نوار کنترل ویدیو در VideoPage بالای فوتر قرار می‌گیرد.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ViewerActionButton(
                icon = Icons.Default.Download,
                label = "ذخیره",
                isLoading = isSaving,
                onClick = onSaveToGallery,
            )
            ViewerActionButton(
                icon = Icons.Default.Share,
                label = "اشتراک",
                onClick = onShare,
            )
            if (onForward != null) {
                ViewerActionButton(
                    icon = Icons.AutoMirrored.Filled.Forward,
                    label = "فوروارد",
                    onClick = onForward,
                )
            }
            if (onShowInChat != null) {
                ViewerActionButton(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "در چت",
                    onClick = onShowInChat,
                )
            }
            if (onDelete != null) {
                ViewerActionButton(
                    icon = Icons.Default.DeleteOutline,
                    label = "حذف",
                    tint = Color(0xFFFF8A80),
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun ViewerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    isLoading: Boolean = false,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = tint,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = tint.copy(alpha = 0.88f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ZoomableImagePage(
    imageUrl: String,
    isGif: Boolean,
    contentDescription: String?,
    imageLoader: ImageLoader,
    isCurrentPage: Boolean,
    onSingleTap: () -> Unit,
    onScaleChanged: (Float) -> Unit,
    onVerticalDragDelta: (Float) -> Unit,
    onVerticalDragRelease: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val scaleAnim = remember(imageUrl) { Animatable(1f) }
    val offsetXAnim = remember(imageUrl) { Animatable(0f) }
    val offsetYAnim = remember(imageUrl) { Animatable(0f) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageIntrinsicSize by remember(imageUrl) { mutableStateOf(Size.Unspecified) }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && scaleAnim.value > 1.01f) {
            scaleAnim.snapTo(1f)
            offsetXAnim.snapTo(0f)
            offsetYAnim.snapTo(0f)
        }
    }

    fun clampOffset(rawX: Float, rawY: Float, targetScale: Float): Offset {
        if (targetScale <= 1f || containerSize.width == 0 || containerSize.height == 0) {
            return Offset.Zero
        }
        val cw = containerSize.width.toFloat()
        val ch = containerSize.height.toFloat()
        val (fittedW, fittedH) = if (imageIntrinsicSize != Size.Unspecified &&
            imageIntrinsicSize.width > 0f && imageIntrinsicSize.height > 0f
        ) {
            val aspect = imageIntrinsicSize.width / imageIntrinsicSize.height
            val containerAspect = cw / ch
            if (aspect > containerAspect) {
                cw to (cw / aspect)
            } else {
                (ch * aspect) to ch
            }
        } else {
            cw to ch
        }
        val maxX = ((fittedW * targetScale - cw) / 2f).coerceAtLeast(0f)
        val maxY = ((fittedH * targetScale - ch) / 2f).coerceAtLeast(0f)
        return Offset(rawX.coerceIn(-maxX, maxX), rawY.coerceIn(-maxY, maxY))
    }

    val context = LocalContext.current
    val request = remember(imageUrl, isGif) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(imageUrl) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            val currentScale = scaleAnim.value
                            if (currentScale > 1.15f) {
                                launch { scaleAnim.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
                                launch { offsetXAnim.animateTo(0f, tween(220, easing = FastOutSlowInEasing)) }
                                launch { offsetYAnim.animateTo(0f, tween(220, easing = FastOutSlowInEasing)) }
                                onScaleChanged(1f)
                            } else {
                                val targetScale = 2.5f
                                val centerX = containerSize.width / 2f
                                val centerY = containerSize.height / 2f
                                val rawX = (centerX - tapOffset.x) * (targetScale - 1f)
                                val rawY = (centerY - tapOffset.y) * (targetScale - 1f)
                                val clamped = clampOffset(rawX, rawY, targetScale)
                                onScaleChanged(targetScale)
                                launch { scaleAnim.animateTo(targetScale, tween(240, easing = FastOutSlowInEasing)) }
                                launch { offsetXAnim.animateTo(clamped.x, tween(240, easing = FastOutSlowInEasing)) }
                                launch { offsetYAnim.animateTo(clamped.y, tween(240, easing = FastOutSlowInEasing)) }
                            }
                        }
                    },
                )
            }
            // Pinch-to-zoom around centroid + pan when zoomed; passes single-finger unzoomed drags through.
            .pointerInput(imageUrl) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var verticalDismissActive = false
                    var accumulatedY = 0f
                    var accumulatedX = 0f
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }
                        if (pointerCount >= 2) {
                            verticalDismissActive = false
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val oldScale = scaleAnim.value
                            val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)
                            val centerX = containerSize.width / 2f
                            val centerY = containerSize.height / 2f
                            val factor = if (oldScale > 0f) newScale / oldScale else 1f
                            val nextX = (offsetXAnim.value + (centerX - centroid.x) * (factor - 1f)) + panChange.x
                            val nextY = (offsetYAnim.value + (centerY - centroid.y) * (factor - 1f)) + panChange.y
                            val clamped = clampOffset(nextX, nextY, newScale)
                            coroutineScope.launch {
                                scaleAnim.snapTo(newScale)
                                offsetXAnim.snapTo(clamped.x)
                                offsetYAnim.snapTo(clamped.y)
                            }
                            onScaleChanged(newScale)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (pointerCount == 1) {
                            val pan = event.calculatePan()
                            if (scaleAnim.value > 1.02f) {
                                val clamped = clampOffset(
                                    offsetXAnim.value + pan.x,
                                    offsetYAnim.value + pan.y,
                                    scaleAnim.value,
                                )
                                coroutineScope.launch {
                                    offsetXAnim.snapTo(clamped.x)
                                    offsetYAnim.snapTo(clamped.y)
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else {
                                accumulatedX += abs(pan.x)
                                accumulatedY += abs(pan.y)
                                if (!verticalDismissActive && accumulatedY > 18f && accumulatedY > accumulatedX * 1.35f) {
                                    verticalDismissActive = true
                                }
                                if (verticalDismissActive) {
                                    onVerticalDragDelta(pan.y)
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (verticalDismissActive) {
                        onVerticalDragRelease()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                val intrinsic = state.painter.intrinsicSize
                if (intrinsic != Size.Unspecified && intrinsic.width > 0f && intrinsic.height > 0f) {
                    imageIntrinsicSize = intrinsic
                }
            },
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp)
                }
            },
            error = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "بارگذاری تصویر ممکن نیست",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    translationX = offsetXAnim.value
                    translationY = offsetYAnim.value
                },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPage(
    videoUrl: String,
    isCurrentPage: Boolean,
    chromeVisible: Boolean,
    glassState: VistaGlassState,
    onToggleChrome: () -> Unit,
    onVerticalDragDelta: (Float) -> Unit,
    onVerticalDragRelease: () -> Unit,
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(isCurrentPage) }
    var isEnded by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1f) }

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = isCurrentPage
            prepare()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                    isEnded = playbackState == Player.STATE_ENDED
                    if (playbackState == Player.STATE_READY) {
                        totalDurationMs = duration.coerceAtLeast(0L)
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
            exoPlayer.play()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(exoPlayer, isCurrentPage) {
        while (isActive) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(150)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(videoUrl) {
                detectTapGestures(onTap = { onToggleChrome() })
            }
            .pointerInput(videoUrl) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        onVerticalDragDelta(dragAmount)
                    },
                    onDragEnd = { onVerticalDragRelease() },
                    onDragCancel = { onVerticalDragRelease() },
                )
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }

        // دکمه مرکزی Play / Pause / Replay
        AnimatedVisibility(
            visible = !isBuffering && (chromeVisible || !isPlaying || isEnded),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.56f))
                    .clickable {
                        when {
                            isEnded -> {
                                exoPlayer.seekTo(0)
                                exoPlayer.play()
                            }
                            isPlaying -> exoPlayer.pause()
                            else -> exoPlayer.play()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        isEnded -> Icons.Default.Replay
                        isPlaying -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = when {
                        isEnded -> "پخش مجدد"
                        isPlaying -> "مکث ویدیو"
                        else -> "پخش ویدیو"
                    },
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
        }

        // نوار کنترل ویدیو بالای نوار اکشن فوتر
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(tween(180)) + slideInVertically(tween(200)) { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(180)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 72.dp, start = 14.dp, end = 14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .vistaGlass(
                        state = glassState,
                        shape = RoundedCornerShape(18.dp),
                        tint = Color(0xFF0A0E14),
                        isDark = true,
                        blurRadius = VistaGlassTokens.PillBlur,
                        tintAlphaOverride = 0.58f,
                        rimWidth = 0.6.dp,
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Slider(
                        value = if (totalDurationMs > 0L) (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { fraction ->
                            val seekTo = (fraction * totalDurationMs).toLong()
                            exoPlayer.seekTo(seekTo)
                            currentPositionMs = seekTo
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.30f),
                        ),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (isEnded) {
                                    exoPlayer.seekTo(0)
                                    exoPlayer.play()
                                } else if (isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "مکث" else "پخش",
                                tint = Color.White,
                            )
                        }
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "وصل صدا" else "قطع صدا",
                                tint = Color.White,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .clickable {
                                    speed = when {
                                        abs(speed - 1f) < 0.05f -> 1.5f
                                        abs(speed - 1.5f) < 0.05f -> 2f
                                        else -> 1f
                                    }
                                    exoPlayer.setPlaybackSpeed(speed)
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = formatSpeedLabel(speed),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Text(
                        text = "${formatDuration(currentPositionMs)} / ${formatDuration(totalDurationMs)}".toPersianDigits(),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatViewerTimestamp(epochMillis: Long): String = runCatching {
    SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.US).format(Date(epochMillis))
}.getOrDefault("")

private tailrec fun Context.findViewerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findViewerActivity()
    else -> null
}

private suspend fun shareMediaItem(context: Context, item: MediaViewerItem): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val uri = Uri.parse(item.url)
        val mime = when {
            item.isVideo -> "video/*"
            item.isGif -> "image/gif"
            else -> "image/*"
        }
        val shareUri: Uri? = when (uri.scheme) {
            "content" -> uri
            "file", null -> {
                val file = File(uri.path ?: item.url)
                if (file.isFile) {
                    val shareDir = File(context.cacheDir, "chat_media").apply { mkdirs() }
                    val target = File(shareDir, item.title?.takeIf(String::isNotBlank) ?: file.name)
                    if (file.absolutePath != target.absolutePath) {
                        file.copyTo(target, overwrite = true)
                    }
                    FileProvider.getUriForFile(context, "${context.packageName}.files", target)
                } else null
            }
            else -> null
        }

        val intent = if (shareUri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri(item.title ?: "media", shareUri)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, item.url)
            }
        }
        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
        }
        true
    }.getOrDefault(false)
}

private suspend fun saveMediaToGallery(context: Context, item: MediaViewerItem): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val ext = when {
            item.isVideo -> "mp4"
            item.isGif -> "gif"
            else -> "jpg"
        }
        val mime = when {
            item.isVideo -> "video/mp4"
            item.isGif -> "image/gif"
            else -> "image/jpeg"
        }
        val fileName = (item.title?.takeIf(String::isNotBlank) ?: "Vista_${System.currentTimeMillis()}.$ext")
            .let { if (it.contains('.')) it else "$it.$ext" }

        val tempStream: InputStream = openMediaInputStream(context, item.url) ?: return@withContext false

        tempStream.use { input ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = if (item.isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }
                val relativePath = if (item.isVideo) {
                    "${Environment.DIRECTORY_MOVIES}/Vista"
                } else {
                    "${Environment.DIRECTORY_PICTURES}/Vista"
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val destUri = resolver.insert(collection, values) ?: return@withContext false
                resolver.openOutputStream(destUri)?.use { out ->
                    input.copyTo(out)
                } ?: return@withContext false
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(destUri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val baseDir = Environment.getExternalStoragePublicDirectory(
                    if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES,
                )
                val targetDir = File(baseDir, "Vista").apply { mkdirs() }
                val targetFile = File(targetDir, fileName)
                targetFile.outputStream().use { out -> input.copyTo(out) }
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(mime), null)
                true
            }
        }
    }.getOrDefault(false)
}

private fun openMediaInputStream(context: Context, url: String): InputStream? {
    val uri = Uri.parse(url)
    return when {
        url.startsWith("/") -> File(url).takeIf(File::isFile)?.let(::FileInputStream)
        uri.scheme == "file" && !uri.path.isNullOrBlank() -> File(uri.path!!).takeIf(File::isFile)?.let(::FileInputStream)
        uri.scheme == "content" -> context.contentResolver.openInputStream(uri)
        uri.scheme == "http" || uri.scheme == "https" -> {
            val client = OkHttpClient()
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            if (response.isSuccessful) response.body?.byteStream() else {
                response.close()
                null
            }
        }
        else -> null
    }
}
