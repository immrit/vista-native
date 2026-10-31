package ir.coffevista.vista_native.features.stories.ui.editor

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryHaptics
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

internal data class StoryGalleryMediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val dateAddedSec: Long = 0L,
)

/**
 * Production-grade Camera2 Story Studio (mirroring Telegram's `StoryRecorder` & `Camera2Session`)
 * paired with an Instagram-style Continuous Physics-Driven Swipe-Up Gallery Drawer.
 *
 * - Real hardware Camera2 live preview inside a 9:16 [StoryStage] with zero visual clutter.
 * - Instant zero-shutter-lag WYSIWYG 1080x1920 photo capture (with LED torch flash on back camera
 *   and Retina screen flash on front camera).
 * - Hold shutter (>= 200ms) for H.264 + AAC `.mp4` video recording up to 60s with smooth vertical
 *   drag hardware zoom (`SCALER_CROP_REGION`) and horizontal slide-to-lock hands-free recording.
 * - Tap-to-focus with animated reticle, 2-finger pinch-to-zoom, double-tap to flip camera.
 * - Continuous finger-tracking vertical swipe-up reveals a glassmorphic 3-column 9:16 gallery
 *   chronologically merging `MediaStore` photos and videos (`DATE_ADDED DESC`).
 */
@Composable
internal fun StoryCameraStudio(
    onMediaUriSelected: (Uri, Boolean) -> Unit,
    onPickSystemVisualMedia: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val density = LocalDensity.current
    val haptics = remember(view) { StoryHaptics(view) }
    val coroutineScope = rememberCoroutineScope()

    val latestOnMediaSelected by rememberUpdatedState(onMediaUriSelected)

    // Permissions state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasMediaPermission by remember {
        mutableStateOf(checkMediaReadPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasCameraPermission = result[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        hasMediaPermission = checkMediaReadPermission(context)
    }

    LaunchedEffect(Unit) {
        val needed = buildList {
            if (!hasCameraPermission) add(Manifest.permission.CAMERA)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (!hasMediaPermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                    add(Manifest.permission.READ_MEDIA_VIDEO)
                } else {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            latestOnMediaSelected(uri, mime.startsWith("video/", ignoreCase = true))
        }
    }

    // Camera hardware state
    var isFrontCamera by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(0) } // 0 = OFF, 1 = AUTO, 2 = ON
    var linearZoom by remember { mutableFloatStateOf(0f) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    var shutterFlashAlpha by remember { mutableFloatStateOf(0f) }

    val cameraController = remember(context) { StoryCamera2Controller(context) }

    // Video recording UI state
    var isRecordingVideo by remember { mutableStateOf(false) }
    var isRecordingLocked by remember { mutableStateOf(false) }
    var recordingMillis by remember { mutableLongStateOf(0L) }
    var lockSlideProgress by remember { mutableFloatStateOf(0f) }

    // Tap-to-focus reticle state (in stage px)
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    val focusRingScale = remember { Animatable(1.35f) }

    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            focusRingScale.snapTo(1.35f)
            focusRingScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            delay(900L)
            focusTapOffset = null
        }
    }

    // Bind Camera2 controller lifecycle
    DisposableEffect(lifecycleOwner, hasCameraPermission) {
        if (hasCameraPermission) {
            cameraController.start()
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (hasCameraPermission) cameraController.onResume()
                Lifecycle.Event.ON_PAUSE -> cameraController.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cameraController.release()
        }
    }

    LaunchedEffect(isFrontCamera, hasCameraPermission) {
        if (hasCameraPermission) {
            linearZoom = 0f
            cameraController.setLensFacing(front = isFrontCamera)
        }
    }

    // Recording timer up to 60 seconds
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            val startMs = System.currentTimeMillis()
            while (isRecordingVideo) {
                recordingMillis = System.currentTimeMillis() - startMs
                if (recordingMillis >= 60_000L) {
                    cameraController.stopVideoRecording()
                    break
                }
                delay(50L)
            }
        } else {
            recordingMillis = 0L
        }
    }

    // Continuous Swipe-Up Gallery Drawer Progress (0f = Camera, 1f = Full Gallery)
    val sheetProgress = remember { Animatable(0f) }
    var selectedAlbumFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Photos, 2 = Videos
    var galleryItems by remember { mutableStateOf<List<StoryGalleryMediaItem>>(emptyList()) }

    LaunchedEffect(hasMediaPermission, selectedAlbumFilter) {
        galleryItems = queryMergedGalleryMedia(context, selectedAlbumFilter)
    }

    fun animateSheetTo(target: Float, initialVelocity: Float = 0f) {
        coroutineScope.launch {
            sheetProgress.animateTo(
                targetValue = target.coerceIn(0f, 1f),
                initialVelocity = initialVelocity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    BackHandler(enabled = sheetProgress.value > 0.25f || isRecordingVideo) {
        if (isRecordingVideo) {
            isRecordingLocked = false
            cameraController.stopVideoRecording()
        } else {
            animateSheetTo(0f)
        }
    }

    fun capturePhotoNow() {
        if (!hasCameraPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            return
        }
        if (isCapturingPhoto || isRecordingVideo) return
        isCapturingPhoto = true
        haptics.snap()

        coroutineScope.launch {
            try {
                // Flash illumination if requested
                if (flashMode != 0) {
                    if (isFrontCamera) {
                        shutterFlashAlpha = 0.92f
                        delay(160L)
                    } else {
                        cameraController.setTorchTemporary(true)
                        delay(200L)
                    }
                } else {
                    shutterFlashAlpha = 0.38f
                }

                val photoFile = cameraController.captureWysiwygPhotoFile()
                shutterFlashAlpha = 0f
                if (!isFrontCamera && flashMode != 0) {
                    cameraController.setTorchTemporary(false)
                }

                if (photoFile != null && photoFile.exists() && photoFile.length() > 0L) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.files",
                        photoFile,
                    )
                    latestOnMediaSelected(uri, false)
                }
            } finally {
                shutterFlashAlpha = 0f
                isCapturingPhoto = false
            }
        }
    }

    fun startVideoRecording() {
        if (!hasCameraPermission || isRecordingVideo) return
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        isRecordingVideo = true
        haptics.trashHover()

        cameraController.startVideoRecording(
            enableAudio = hasAudio,
            enableTorch = !isFrontCamera && flashMode == 2,
            onFinalized = { videoFile ->
                isRecordingVideo = false
                isRecordingLocked = false
                lockSlideProgress = 0f
                linearZoom = 0f
                if (videoFile != null && videoFile.exists() && videoFile.length() > 1024L) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.files",
                        videoFile,
                    )
                    latestOnMediaSelected(uri, true)
                } else {
                    videoFile?.delete()
                }
            },
        )
    }

    fun stopVideoRecording() {
        cameraController.stopVideoRecording()
    }

    // Entry spring scale transition
    val entryScale = remember { Animatable(0.95f) }
    LaunchedEffect(Unit) {
        entryScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }

    val shutterScale by animateFloatAsState(
        targetValue = if (isRecordingVideo) 1.32f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "shutter_scale",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val containerHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val progress = sheetProgress.value.coerceIn(0f, 1f)

        // =========================================================================
        // 1. LIVE 9:16 CAMERA2 HARDWARE STAGE
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = entryScale.value * (1f - 0.07f * progress)
                    scaleX = s
                    scaleY = s
                },
            contentAlignment = Alignment.Center,
        ) {
            StoryStage(
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 22.dp,
            ) { stage ->
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                cameraController.attachTextureView(this)
                            }
                        },
                        update = { tv ->
                            cameraController.updateViewTransform(tv)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isFrontCamera, isRecordingVideo) {
                                var lastTapTimeMs = 0L
                                var lastTapPos = Offset.Zero
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val startPos = down.position
                                    var lastPos = startPos
                                    var isPinching = false
                                    var isSwipingVertical = false
                                    val velocityTracker = VelocityTracker()
                                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                                    do {
                                        val event = awaitPointerEvent()
                                        val pressed = event.changes.filter { it.pressed }
                                        if (pressed.size >= 2) {
                                            isPinching = true
                                            val span = event.calculateCentroidSize(useCurrent = true)
                                            if (span > 12f) {
                                                val zoomFactor = event.calculateZoom()
                                                if (abs(zoomFactor - 1f) > 0.004f) {
                                                    linearZoom = (linearZoom + (zoomFactor - 1f) * 0.65f)
                                                        .coerceIn(0f, 1f)
                                                    cameraController.setLinearZoom(linearZoom)
                                                }
                                            }
                                            event.changes.forEach { it.consume() }
                                        } else if (pressed.size == 1 && !isPinching && !isRecordingVideo) {
                                            val ch = pressed[0]
                                            lastPos = ch.position
                                            velocityTracker.addPosition(ch.uptimeMillis, ch.position)
                                            val dy = ch.position.y - startPos.y
                                            val dx = ch.position.x - startPos.x
                                            if (!isSwipingVertical && abs(dy) > 18f && abs(dy) > abs(dx) * 1.25f) {
                                                isSwipingVertical = true
                                            }
                                            if (isSwipingVertical) {
                                                val frameDy = ch.position.y - ch.previousPosition.y
                                                val nextProg = (sheetProgress.value - frameDy / (containerHeightPx * 0.72f))
                                                    .coerceIn(0f, 1f)
                                                coroutineScope.launch {
                                                    sheetProgress.snapTo(nextProg)
                                                }
                                                ch.consume()
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })

                                    if (isSwipingVertical) {
                                        val vy = velocityTracker.calculateVelocity().y
                                        val openGallery = vy < -650f ||
                                            (abs(vy) <= 650f && sheetProgress.value > 0.22f)
                                        if (openGallery) haptics.snap()
                                        animateSheetTo(if (openGallery) 1f else 0f)
                                    } else if (!isPinching && hypot(
                                            (lastPos.x - startPos.x).toDouble(),
                                            (lastPos.y - startPos.y).toDouble(),
                                        ) < 18.0 && sheetProgress.value < 0.05f
                                    ) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTimeMs < 280L &&
                                            (startPos - lastTapPos).getDistance() < 90f &&
                                            !isRecordingVideo
                                        ) {
                                            lastTapTimeMs = 0L
                                            isFrontCamera = !isFrontCamera
                                            haptics.snap()
                                        } else {
                                            lastTapTimeMs = now
                                            lastTapPos = startPos
                                            focusTapOffset = startPos
                                            if (stage.width > 0f && stage.height > 0f) {
                                                cameraController.focusAt(
                                                    normX = (startPos.x / stage.width).coerceIn(0f, 1f),
                                                    normY = (startPos.y / stage.height).coerceIn(0f, 1f),
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                    )
                } else {
                    // Clean minimal camera permission prompt (swipe-up gallery still works!)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF10121A), Color(0xFF171926), Color(0xFF0B0C12)),
                                ),
                            )
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        animateSheetTo(if (sheetProgress.value > 0.2f) 1f else 0f)
                                    },
                                ) { change, dragAmount ->
                                    change.consume()
                                    val next = (sheetProgress.value - dragAmount / (containerHeightPx * 0.72f))
                                        .coerceIn(0f, 1f)
                                    coroutineScope.launch { sheetProgress.snapTo(next) }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 36.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "دسترسی به دوربین",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "برای ثبت مستقیم عکس و ویدیو دسترسی دوربین را فعال کنید، یا صفحه را به بالا بکشید تا از گالری انتخاب کنید.",
                                    color = Color.White.copy(alpha = 0.68f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.CAMERA,
                                                Manifest.permission.RECORD_AUDIO,
                                            ),
                                        )
                                    },
                                    shape = RoundedCornerShape(22.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VistaBrandColors.Indigo,
                                        contentColor = Color.White,
                                    ),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                                ) {
                                    Text("فعال‌سازی دوربین", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Subtle Top & Bottom Edge Vignettes for HUD legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.52f), Color.Transparent),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f)),
                            ),
                        ),
                )

                // Tap-to-Focus Reticle Ring
                focusTapOffset?.let { tapPos ->
                    val ringSizeDp = 58.dp
                    val halfPx = with(density) { (ringSizeDp / 2).toPx() }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (tapPos.x - halfPx).roundToInt(),
                                    (tapPos.y - halfPx).roundToInt(),
                                )
                            }
                            .size(ringSizeDp)
                            .graphicsLayer {
                                scaleX = focusRingScale.value
                                scaleY = focusRingScale.value
                            }
                            .border(1.8.dp, Color(0xFFFFD54F), CircleShape),
                    )
                }

                // Shutter / Selfie Retina Flash Effect
                if (shutterFlashAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFFBF2).copy(alpha = shutterFlashAlpha)),
                    )
                }

                // Dim overlay as Gallery Sheet rises
                if (progress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.68f * progress)),
                    )
                }
            }
        }

        // =========================================================================
        // 2. MINIMALIST CAMERA HUD (Fades cleanly as Gallery Sheet opens)
        // =========================================================================
        if (progress < 0.85f) {
            val hudAlpha = (1f - progress * 1.4f).coerceIn(0f, 1f)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = hudAlpha },
                ) {
                    // Top Camera Bar: Close | Timer (if recording) | Flash + Flip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.42f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                        }

                        AnimatedVisibility(
                            visible = isRecordingVideo,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                        ) {
                            val totalSec = recordingMillis / 1000L
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFE53935))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                )
                                Text(
                                    text = "%02d:%02d".format(totalSec / 60, totalSec % 60).toPersianDigits(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }

                        if (!isRecordingVideo) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                IconButton(
                                    onClick = {
                                        flashMode = (flashMode + 1) % 3
                                        haptics.snap()
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (flashMode != 0) Color.White.copy(alpha = 0.24f)
                                            else Color.Black.copy(alpha = 0.42f),
                                        )
                                        .border(0.8.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                                ) {
                                    val flashIcon = when (flashMode) {
                                        1 -> Icons.Default.FlashAuto
                                        2 -> Icons.Default.FlashOn
                                        else -> Icons.Default.FlashOff
                                    }
                                    Icon(
                                        imageVector = flashIcon,
                                        contentDescription = "فلش",
                                        tint = if (flashMode == 2) Color(0xFFFFD54F) else Color.White,
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        isFrontCamera = !isFrontCamera
                                        haptics.snap()
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.42f))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "چرخش دوربین",
                                        tint = Color.White,
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.width(42.dp))
                        }
                    }

                    // Bottom Camera Bar: Gallery Thumbnail | Concentric Shutter | Flip/Lock
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Recent Gallery Thumbnail Button
                            val latestMedia = galleryItems.firstOrNull()
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isRecordingVideo) {
                                        haptics.snap()
                                        animateSheetTo(1f)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (latestMedia != null) {
                                    AsyncImage(
                                        model = latestMedia.uri,
                                        contentDescription = "گالری",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "گالری",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }

                            // Center Shutter Button (Tap = Photo, Hold = Video + Drag Zoom + Slide Lock)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(92.dp)
                                    .pointerInput(hasCameraPermission, isRecordingLocked) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            if (isRecordingLocked) {
                                                do {
                                                    val ev = awaitPointerEvent()
                                                } while (ev.changes.any { it.pressed })
                                                stopVideoRecording()
                                                return@awaitEachGesture
                                            }

                                            val downTime = down.uptimeMillis
                                            val startX = down.position.x
                                            val startY = down.position.y
                                            var holdStarted = false

                                            do {
                                                val event = awaitPointerEvent()
                                                val current = event.changes.firstOrNull() ?: break
                                                val elapsed = current.uptimeMillis - downTime
                                                if (!holdStarted && elapsed >= 210L && hasCameraPermission) {
                                                    holdStarted = true
                                                    startVideoRecording()
                                                }
                                                if (holdStarted) {
                                                    val dy = (startY - current.position.y).coerceAtLeast(0f)
                                                    val zoomNorm = (dy / 340f).coerceIn(0f, 1f)
                                                    linearZoom = zoomNorm
                                                    cameraController.setLinearZoom(zoomNorm)

                                                    val dx = abs(current.position.x - startX)
                                                    lockSlideProgress = (dx / 150f).coerceIn(0f, 1f)
                                                    if (lockSlideProgress >= 0.92f && !isRecordingLocked) {
                                                        isRecordingLocked = true
                                                        haptics.deleted()
                                                    }
                                                }
                                            } while (event.changes.any { it.pressed })

                                            if (holdStarted) {
                                                if (!isRecordingLocked) {
                                                    stopVideoRecording()
                                                }
                                                lockSlideProgress = 0f
                                            } else {
                                                capturePhotoNow()
                                            }
                                        }
                                    },
                            ) {
                                val recordSweep = ((recordingMillis / 60_000f) * 360f).coerceIn(0f, 360f)
                                Canvas(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .graphicsLayer {
                                            scaleX = shutterScale
                                            scaleY = shutterScale
                                        },
                                ) {
                                    val strokePx = 4.dp.toPx()
                                    drawCircle(
                                        color = Color.White.copy(alpha = if (isRecordingVideo) 0.35f else 0.95f),
                                        style = Stroke(width = strokePx),
                                    )
                                    if (isRecordingVideo) {
                                        drawArc(
                                            color = Color(0xFFFF3B30),
                                            startAngle = -90f,
                                            sweepAngle = recordSweep,
                                            useCenter = false,
                                            topLeft = Offset(strokePx / 2f, strokePx / 2f),
                                            size = ComposeSize(size.width - strokePx, size.height - strokePx),
                                            style = Stroke(width = strokePx * 1.15f, cap = StrokeCap.Round),
                                        )
                                    }
                                    if (isRecordingLocked) {
                                        val sq = size.minDimension * 0.34f
                                        drawRoundRect(
                                            color = Color(0xFFFF3B30),
                                            topLeft = Offset((size.width - sq) / 2f, (size.height - sq) / 2f),
                                            size = ComposeSize(sq, sq),
                                            cornerRadius = CornerRadius(6.dp.toPx()),
                                        )
                                    } else {
                                        drawCircle(
                                            color = if (isRecordingVideo) Color(0xFFFF3B30) else Color.White,
                                            radius = size.minDimension * (if (isRecordingVideo) 0.28f else 0.39f),
                                        )
                                    }
                                }
                            }

                            // Opposite Side: Hands-free Lock Target during hold, or Quick Camera Flip
                            if (isRecordingVideo && !isRecordingLocked) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .graphicsLayer {
                                            val s = 1f + lockSlideProgress * 0.22f
                                            scaleX = s
                                            scaleY = s
                                        }
                                        .clip(CircleShape)
                                        .background(
                                            if (lockSlideProgress > 0.6f) Color(0xFFE53935)
                                            else Color.Black.copy(alpha = 0.52f),
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "قفل ضبط",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.42f))
                                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                        .clickable(enabled = !isRecordingVideo) {
                                            isFrontCamera = !isFrontCamera
                                            haptics.snap()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "چرخش دوربین",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Subtle Swipe-Up Gallery Pill
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !isRecordingVideo) {
                                    haptics.snap()
                                    animateSheetTo(1f)
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "گالری",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. INSTAGRAM-STYLE CONTINUOUS SWIPE-UP GALLERY SHEET
        // =========================================================================
        if (progress > 0.005f) {
            val sheetOffsetPx = ((1f - progress) * containerHeightPx).roundToInt()
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.94f)
                        .align(Alignment.BottomCenter)
                        .offset { IntOffset(0, sheetOffsetPx) }
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                        .background(Color(0xF5111218))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                        )
                        .navigationBarsPadding(),
                ) {
                    // Draggable Sheet Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                val velocityTracker = VelocityTracker()
                                detectVerticalDragGestures(
                                    onDragStart = { velocityTracker.resetTracking() },
                                    onDragEnd = {
                                        val vy = velocityTracker.calculateVelocity().y
                                        val keepOpen = !(vy > 650f || (abs(vy) <= 650f && sheetProgress.value < 0.72f))
                                        animateSheetTo(if (keepOpen) 1f else 0f)
                                    },
                                ) { change, dragAmount ->
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    change.consume()
                                    val next = (sheetProgress.value - dragAmount / (containerHeightPx * 0.72f))
                                        .coerceIn(0f, 1f)
                                    coroutineScope.launch { sheetProgress.snapTo(next) }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.28f)),
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                IconButton(
                                    onClick = { animateSheetTo(0f) },
                                    modifier = Modifier.size(34.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "بستن گالری",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Text(
                                    text = "گالری",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                listOf(0 to "همه", 1 to "عکس", 2 to "ویدیو").forEach { (idx, label) ->
                                    val selected = selectedAlbumFilter == idx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (selected) Color.White
                                                else Color.White.copy(alpha = 0.09f),
                                            )
                                            .clickable {
                                                selectedAlbumFilter = idx
                                                haptics.snap()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (selected) Color(0xFF101218) else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            systemPickerLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo,
                                                ),
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = "پوشه‌ها",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Gallery Content Grid
                    if (galleryItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.45f),
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (!hasMediaPermission) {
                                        "برای نمایش مستقیم عکس‌ها و ویدیوها دسترسی گالری را فعال کنید"
                                    } else {
                                        "رسانه‌ای در این بخش یافت نشد"
                                    },
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (!hasMediaPermission) {
                                        Button(
                                            onClick = {
                                                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    arrayOf(
                                                        Manifest.permission.READ_MEDIA_IMAGES,
                                                        Manifest.permission.READ_MEDIA_VIDEO,
                                                    )
                                                } else {
                                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                                }
                                                permissionLauncher.launch(perms)
                                            },
                                            shape = RoundedCornerShape(18.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Indigo),
                                        ) {
                                            Text("اجازه دسترسی", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            onPickSystemVisualMedia()
                                            systemPickerLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo,
                                                ),
                                            )
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.14f),
                                            contentColor = Color.White,
                                        ),
                                    ) {
                                        Text("انتخاب از فایل‌ها", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                            verticalArrangement = Arrangement.spacedBy(2.5.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(
                                items = galleryItems,
                                key = { "${if (it.isVideo) "v" else "i"}_${it.id}" },
                            ) { item ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(9f / 16f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1C1D26))
                                        .clickable {
                                            haptics.snap()
                                            latestOnMediaSelected(item.uri, item.isVideo)
                                        },
                                ) {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                    if (item.isVideo) {
                                        val totalSec = (item.durationMs / 1000L).coerceAtLeast(0L)
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp),
                                            )
                                            Text(
                                                text = "%02d:%02d".format(totalSec / 60, totalSec % 60).toPersianDigits(),
                                                color = Color.White,
                                                fontSize = 10.sp,
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

// =============================================================================================
// TELEGRAM-GRADE HARDWARE CAMERA2 ENGINE (Mirroring DrKLO/Telegram Camera2Session + StoryRecorder)
// =============================================================================================

private class StoryCamera2Controller(
    private val context: Context,
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null

    @Volatile private var textureView: TextureView? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var activeRequestBuilder: CaptureRequest.Builder? = null

    private var mediaRecorder: MediaRecorder? = null
    private var currentVideoFile: File? = null
    private var onVideoFinalized: ((File?) -> Unit)? = null
    private var isRecording = false

    private var useFrontCamera: Boolean = false
    private var sensorOrientation: Int = 90
    private var previewSize: Size = Size(1920, 1080)
    private var videoSize: Size = Size(1920, 1080)
    private var sensorArrayRect: Rect? = null
    private var maxDigitalZoom: Float = 4f
    private var hasFlashUnit: Boolean = false
    private var currentCropRect: Rect? = null
    private var torchEnabled: Boolean = false
    private var isStarted: Boolean = false

    fun start() {
        isStarted = true
        ensureThread()
        postCamera { openCameraInternal() }
    }

    fun onResume() {
        if (!isStarted) return
        ensureThread()
        postCamera {
            if (cameraDevice == null) {
                openCameraInternal()
            }
        }
    }

    fun onPause() {
        postCamera {
            if (isRecording) {
                stopVideoRecordingInternal()
            }
            closeCameraInternal()
        }
    }

    fun release() {
        isStarted = false
        val handler = cameraHandler
        val thread = cameraThread
        cameraHandler = null
        cameraThread = null
        if (handler != null && thread != null) {
            handler.post {
                if (isRecording) {
                    stopVideoRecordingInternal()
                }
                closeCameraInternal()
                thread.quitSafely()
            }
        }
    }

    fun attachTextureView(tv: TextureView) {
        textureView = tv
        tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                updateViewTransform(tv)
                if (isStarted) {
                    ensureThread()
                    postCamera { openCameraInternal() }
                }
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                updateViewTransform(tv)
            }

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                postCamera { closeCameraInternal() }
                return true
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
        }
        if (tv.isAvailable && isStarted) {
            ensureThread()
            postCamera { openCameraInternal() }
        }
    }

    fun setLensFacing(front: Boolean) {
        if (useFrontCamera == front && cameraDevice != null) return
        useFrontCamera = front
        currentCropRect = null
        torchEnabled = false
        if (isStarted) {
            ensureThread()
            postCamera {
                closeCameraInternal()
                openCameraInternal()
            }
        }
    }

    fun setLinearZoom(zoomNorm: Float) {
        postCamera {
            val activeRect = sensorArrayRect ?: return@postCamera
            val z = 1f + zoomNorm.coerceIn(0f, 1f) * (maxDigitalZoom - 1f).coerceAtLeast(0f)
            val cropW = (activeRect.width() / z).roundToInt().coerceAtLeast(64)
            val cropH = (activeRect.height() / z).roundToInt().coerceAtLeast(64)
            val left = activeRect.centerX() - cropW / 2
            val top = activeRect.centerY() - cropH / 2
            val zoomRect = Rect(left, top, left + cropW, top + cropH)
            currentCropRect = zoomRect

            val builder = activeRequestBuilder ?: return@postCamera
            val session = captureSession ?: return@postCamera
            runCatching {
                builder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
                session.setRepeatingRequest(builder.build(), null, cameraHandler)
            }
        }
    }

    fun setTorchTemporary(enabled: Boolean) {
        postCamera {
            if (!hasFlashUnit) return@postCamera
            torchEnabled = enabled
            val builder = activeRequestBuilder ?: return@postCamera
            val session = captureSession ?: return@postCamera
            runCatching {
                builder.set(
                    CaptureRequest.FLASH_MODE,
                    if (enabled) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF,
                )
                session.setRepeatingRequest(builder.build(), null, cameraHandler)
            }
        }
    }

    fun focusAt(normX: Float, normY: Float) {
        postCamera {
            val activeRect = sensorArrayRect ?: return@postCamera
            val builder = activeRequestBuilder ?: return@postCamera
            val session = captureSession ?: return@postCamera

            val regionSize = (minOf(activeRect.width(), activeRect.height()) * 0.14f).roundToInt().coerceAtLeast(40)
            val cx = (activeRect.left + normX * activeRect.width()).roundToInt()
                .coerceIn(activeRect.left + regionSize, activeRect.right - regionSize)
            val cy = (activeRect.top + normY * activeRect.height()).roundToInt()
                .coerceIn(activeRect.top + regionSize, activeRect.bottom - regionSize)

            val metering = MeteringRectangle(
                Rect(cx - regionSize, cy - regionSize, cx + regionSize, cy + regionSize),
                MeteringRectangle.METERING_WEIGHT_MAX - 1,
            )

            runCatching {
                builder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(metering))
                builder.set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(metering))
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
                session.capture(builder.build(), null, cameraHandler)

                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
                builder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    if (isRecording) CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                    else CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                )
                session.setRepeatingRequest(builder.build(), null, cameraHandler)
            }
        }
    }

    /**
     * Captures the exact live hardware-rendered 9:16 frame from [TextureView] (Telegram StoryRecorder
     * zero-shutter-lag WYSIWYG capture) and writes a crisp 1080x1920 JPEG on [Dispatchers.IO].
     */
    suspend fun captureWysiwygPhotoFile(): File? {
        val tv = textureView ?: return null
        if (!tv.isAvailable || tv.width <= 0 || tv.height <= 0) return null

        val capturedPair = withContext(Dispatchers.Main) {
            val raw = tv.bitmap ?: return@withContext null
            val transform = Matrix()
            tv.getTransform(transform)
            Triple(raw, transform, tv.width to tv.height)
        } ?: return null

        return withContext(Dispatchers.IO) {
            val (rawBitmap, viewMatrix, viewSize) = capturedPair
            val (viewW, viewH) = viewSize
            val outBitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
            try {
                val canvas = AndroidCanvas(outBitmap)
                val fullMatrix = Matrix(viewMatrix)
                fullMatrix.postScale(1080f / viewW.toFloat(), 1920f / viewH.toFloat())
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(rawBitmap, fullMatrix, paint)

                val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
                val photoFile = File(cameraDir, "story_${System.currentTimeMillis()}.jpg")
                FileOutputStream(photoFile).use { out ->
                    outBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                photoFile
            } finally {
                rawBitmap.recycle()
                outBitmap.recycle()
            }
        }
    }

    fun startVideoRecording(
        enableAudio: Boolean,
        enableTorch: Boolean,
        onFinalized: (File?) -> Unit,
    ) {
        postCamera {
            val cam = cameraDevice
            val tv = textureView
            val st = tv?.surfaceTexture
            if (cam == null || tv == null || !tv.isAvailable || st == null || isRecording) {
                mainHandler.post { onFinalized(null) }
                return@postCamera
            }

            runCatching { captureSession?.close() }
            captureSession = null

            val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
            val outFile = File(cameraDir, "story_vid_${System.currentTimeMillis()}.mp4")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            try {
                if (enableAudio) {
                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setOutputFile(outFile.absolutePath)
                recorder.setVideoEncodingBitRate(8_000_000)
                recorder.setVideoFrameRate(30)
                recorder.setVideoSize(videoSize.width, videoSize.height)
                recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (enableAudio) {
                    recorder.setAudioEncodingBitRate(128_000)
                    recorder.setAudioSamplingRate(44_100)
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                }
                recorder.setOrientationHint(computeRecorderOrientationHint(tv))
                recorder.prepare()
            } catch (_: Exception) {
                runCatching { recorder.release() }
                outFile.delete()
                startPreviewSessionInternal()
                mainHandler.post { onFinalized(null) }
                return@postCamera
            }

            mediaRecorder = recorder
            currentVideoFile = outFile
            onVideoFinalized = onFinalized

            st.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface = Surface(st)
            val recorderSurface = recorder.surface

            try {
                val builder = cam.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                    addTarget(previewSurface)
                    addTarget(recorderSurface)
                    set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                    currentCropRect?.let { set(CaptureRequest.SCALER_CROP_REGION, it) }
                    if (hasFlashUnit && enableTorch) {
                        set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                    }
                }
                activeRequestBuilder = builder

                @Suppress("DEPRECATION")
                cam.createCaptureSession(
                    listOf(previewSurface, recorderSurface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            if (cameraDevice == null) return
                            captureSession = session
                            try {
                                session.setRepeatingRequest(builder.build(), null, cameraHandler)
                                recorder.start()
                                isRecording = true
                            } catch (_: Exception) {
                                stopVideoRecordingInternal()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            stopVideoRecordingInternal()
                        }
                    },
                    cameraHandler,
                )
            } catch (_: Exception) {
                stopVideoRecordingInternal()
            }
        }
    }

    fun stopVideoRecording() {
        postCamera {
            stopVideoRecordingInternal()
        }
    }

    private fun stopVideoRecordingInternal() {
        val callback = onVideoFinalized
        val file = currentVideoFile
        onVideoFinalized = null
        currentVideoFile = null
        val wasRecording = isRecording
        isRecording = false

        runCatching { captureSession?.stopRepeating() }
        runCatching { captureSession?.close() }
        captureSession = null

        val recorder = mediaRecorder
        mediaRecorder = null
        if (recorder != null) {
            if (wasRecording) {
                runCatching { recorder.stop() }
            }
            runCatching { recorder.reset() }
            runCatching { recorder.release() }
        }

        currentCropRect = null
        torchEnabled = false
        if (isStarted && cameraDevice != null) {
            startPreviewSessionInternal()
        }

        if (callback != null) {
            mainHandler.post { callback(file) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCameraInternal() {
        val tv = textureView ?: return
        if (!tv.isAvailable || cameraDevice != null) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val cameraId = selectCameraId() ?: return
        try {
            val chars = cameraManager.getCameraCharacteristics(cameraId)
            sensorOrientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
            sensorArrayRect = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            maxDigitalZoom = (chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 4f)
                .coerceIn(1f, 8f)
            hasFlashUnit = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (map != null) {
                previewSize = chooseOptimal16x9Size(
                    map.getOutputSizes(SurfaceTexture::class.java),
                    maxLongSide = 1920,
                )
                videoSize = chooseOptimal16x9Size(
                    map.getOutputSizes(MediaRecorder::class.java),
                    maxLongSide = 1920,
                )
            }

            mainHandler.post { updateViewTransform(tv) }

            cameraManager.openCamera(
                cameraId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(device: CameraDevice) {
                        cameraDevice = device
                        startPreviewSessionInternal()
                    }

                    override fun onDisconnected(device: CameraDevice) {
                        device.close()
                        if (cameraDevice === device) cameraDevice = null
                    }

                    override fun onError(device: CameraDevice, error: Int) {
                        device.close()
                        if (cameraDevice === device) cameraDevice = null
                    }
                },
                cameraHandler,
            )
        } catch (_: Exception) {
            // Ignore transient camera service contention
        }
    }

    private fun startPreviewSessionInternal() {
        val cam = cameraDevice ?: return
        val tv = textureView ?: return
        val st = tv.surfaceTexture ?: return
        if (!tv.isAvailable) return

        runCatching { captureSession?.close() }
        captureSession = null

        try {
            st.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface = Surface(st)
            val builder = cam.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                currentCropRect?.let { set(CaptureRequest.SCALER_CROP_REGION, it) }
            }
            activeRequestBuilder = builder

            @Suppress("DEPRECATION")
            cam.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        runCatching {
                            session.setRepeatingRequest(builder.build(), null, cameraHandler)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) = Unit
                },
                cameraHandler,
            )
        } catch (_: Exception) {
            // Surface released concurrently
        }
    }

    private fun closeCameraInternal() {
        runCatching { captureSession?.close() }
        captureSession = null
        activeRequestBuilder = null
        runCatching { cameraDevice?.close() }
        cameraDevice = null
    }

    private fun selectCameraId(): String? {
        val desiredFacing = if (useFrontCamera) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }
        val ids = runCatching { cameraManager.cameraIdList }.getOrNull() ?: return null
        for (id in ids) {
            val chars = runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull() ?: continue
            if (chars.get(CameraCharacteristics.LENS_FACING) == desiredFacing) {
                return id
            }
        }
        return ids.firstOrNull()
    }

    fun updateViewTransform(tv: TextureView) {
        val viewW = tv.width.toFloat()
        val viewH = tv.height.toFloat()
        if (viewW <= 0f || viewH <= 0f || previewSize.width <= 0 || previewSize.height <= 0) return

        val displayRotDeg = when (tv.display?.rotation ?: Surface.ROTATION_0) {
            Surface.ROTATION_90 -> 90f
            Surface.ROTATION_180 -> 180f
            Surface.ROTATION_270 -> 270f
            else -> 0f
        }

        val swap = sensorOrientation % 180 != 0
        val orientedW = (if (swap) previewSize.height else previewSize.width).toFloat()
        val orientedH = (if (swap) previewSize.width else previewSize.height).toFloat()

        val viewRatio = viewW / viewH
        val bufRatio = orientedW / orientedH

        val (scaleX, scaleY) = if (bufRatio > viewRatio) {
            (bufRatio / viewRatio) to 1f
        } else {
            1f to (viewRatio / bufRatio)
        }

        val matrix = Matrix()
        val cx = viewW * 0.5f
        val cy = viewH * 0.5f
        if (displayRotDeg != 0f) {
            matrix.postRotate(-displayRotDeg, cx, cy)
        }
        matrix.postScale(scaleX, scaleY, cx, cy)
        tv.setTransform(matrix)
    }

    private fun computeRecorderOrientationHint(tv: TextureView): Int {
        val displayDeg = when (tv.display?.rotation ?: Surface.ROTATION_0) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        return if (useFrontCamera) {
            (sensorOrientation + displayDeg) % 360
        } else {
            (sensorOrientation - displayDeg + 360) % 360
        }
    }

    private fun chooseOptimal16x9Size(choices: Array<Size>?, maxLongSide: Int): Size {
        if (choices.isNullOrEmpty()) return Size(1920, 1080)
        val targetRatio = 16f / 9f
        val valid = choices.filter { max(it.width, it.height) <= maxLongSide }
            .ifEmpty { choices.toList() }

        val exact16x9 = valid.filter {
            val r = max(it.width, it.height).toFloat() / minOf(it.width, it.height).coerceAtLeast(1)
            abs(r - targetRatio) < 0.05f
        }
        if (exact16x9.isNotEmpty()) {
            return exact16x9.maxBy { it.width.toLong() * it.height.toLong() }
        }
        return valid.minWith(
            compareBy<Size> {
                val r = max(it.width, it.height).toFloat() / minOf(it.width, it.height).coerceAtLeast(1)
                abs(r - targetRatio)
            }.thenByDescending { it.width.toLong() * it.height.toLong() },
        )
    }

    private fun ensureThread() {
        if (cameraThread == null) {
            val thread = HandlerThread("VistaStoryCamera2").apply { start() }
            cameraThread = thread
            cameraHandler = Handler(thread.looper)
        }
    }

    private fun postCamera(block: () -> Unit) {
        cameraHandler?.post(block)
    }
}

private fun checkMediaReadPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }
}

private suspend fun queryMergedGalleryMedia(
    context: Context,
    albumFilter: Int,
): List<StoryGalleryMediaItem> = withContext(Dispatchers.IO) {
    runCatching {
        val items = ArrayList<StoryGalleryMediaItem>(120)
        if (albumFilter == 0 || albumFilter == 1) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
            )
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                var count = 0
                while (cursor.moveToNext() && count < 90) {
                    val id = cursor.getLong(idCol)
                    val dateSec = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                    items += StoryGalleryMediaItem(
                        id = id,
                        uri = uri,
                        isVideo = false,
                        dateAddedSec = dateSec,
                    )
                    count++
                }
            }
        }
        if (albumFilter == 0 || albumFilter == 2) {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED,
            )
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                var count = 0
                while (cursor.moveToNext() && count < 60) {
                    val id = cursor.getLong(idCol)
                    val dur = cursor.getLong(durCol)
                    val dateSec = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                    items += StoryGalleryMediaItem(
                        id = id,
                        uri = uri,
                        isVideo = true,
                        durationMs = dur,
                        dateAddedSec = dateSec,
                    )
                    count++
                }
            }
        }
        items.sortedByDescending { it.dateAddedSec }.take(120)
    }.getOrDefault(emptyList())
}
