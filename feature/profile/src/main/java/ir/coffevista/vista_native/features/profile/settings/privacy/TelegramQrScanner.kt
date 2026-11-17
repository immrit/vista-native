package ir.coffevista.vista_native.features.profile.settings.privacy

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

private val TelegramScannerIndigo = Color(0xFF6366F1)
private val TelegramScannerViolet = Color(0xFF8B5CF6)
private val TelegramScannerLocked = Color(0xFF10B981)

@Composable
internal fun TelegramQrScannerDialog(
    isApproving: Boolean,
    onQrRecognized: (String) -> Unit,
    onPickGallery: () -> Unit,
    onManualEntry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    var flashAvailable by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var lockedText by remember { mutableStateOf<String?>(null) }
    var wasApproving by remember { mutableStateOf(false) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
    var cameraSessionController by remember { mutableStateOf<TelegramCameraController?>(null) }

    LaunchedEffect(isApproving) {
        if (isApproving) {
            wasApproving = true
        } else if (wasApproving) {
            wasApproving = false
            // If approval finished without closing the dialog (e.g. invalid/expired QR), unlock so user can rescan.
            lockedText = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission && !permissionRequested) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(flashEnabled, cameraSessionController) {
        cameraSessionController?.setTorch(flashEnabled)
    }

    // Resume permission state if user returned from system app settings.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val handleDetected: (String) -> Unit = remember {
        { raw ->
            if (lockedText == null) {
                lockedText = raw
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    delay(240L)
                    onQrRecognized(raw)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isApproving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070912)),
            ) {
                // Live Camera2 Preview surface (Telegram-style continuous analyzer)
                if (hasPermission && cameraError == null) {
                    TelegramCameraPreview(
                        isPaused = lockedText != null || isApproving,
                        onControllerReady = { controller, hasFlash ->
                            cameraSessionController = controller
                            flashAvailable = hasFlash
                        },
                        onTapToFocus = { tap -> focusTapOffset = tap },
                        onQrDecoded = handleDetected,
                        onError = { err -> cameraError = err },
                    )
                }

                // Dimmed cutout vignette + glowing corner brackets + animated scan laser
                TelegramViewfinderOverlay(
                    isRecognized = lockedText != null,
                    focusTapOffset = focusTapOffset,
                    onFocusAnimationFinished = { focusTapOffset = null },
                )

                // Top Telegram Bar: Close button + Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        onClick = { if (!isApproving) onDismiss() },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.48f),
                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.16f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "بستن",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp),
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = TelegramScannerIndigo,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "اسکن کد QR",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Spacer to keep title centered
                    Spacer(Modifier.size(42.dp))
                }

                // Center permission / camera fallback card if needed
                if (!hasPermission || cameraError != null) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF141727).copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 28.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TelegramScannerIndigo.copy(alpha = 0.18f),
                                modifier = Modifier.size(56.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.CameraAlt,
                                        contentDescription = null,
                                        tint = TelegramScannerIndigo,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                            Text(
                                text = cameraError ?: "دسترسی به دوربین برای اسکن لازم است",
                                color = Color.White,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "برای اتصال فوری دستگاه با کد QR، دسترسی دوربین را تایید کنید یا تصویر QR را از گالری انتخاب نمایید.",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center,
                            )
                            Button(
                                onClick = {
                                    if (!hasPermission) {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null),
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        cameraError = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TelegramScannerIndigo),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = if (!hasPermission) "باز کردن تنظیمات مجوز" else "تلاش مجدد",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                // Bottom Guidance + Telegram Action Dock (Flash | Gallery | Manual Token)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    if (lockedText != null || isApproving) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = TelegramScannerLocked.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, TelegramScannerLocked.copy(alpha = 0.65f)),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            ) {
                                if (isApproving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = TelegramScannerLocked,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Text(
                                    text = "کد QR شناسایی شد، در حال اتصال...",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "کد QR نمایش‌داده‌شده در ویستا وب یا دسکتاپ را داخل کادر قرار دهید",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (flashAvailable) {
                            ScannerRoundAction(
                                icon = if (flashEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                                label = "چراغ قوه",
                                active = flashEnabled,
                                onClick = { flashEnabled = !flashEnabled },
                            )
                        }

                        ScannerRoundAction(
                            icon = Icons.Rounded.PhotoLibrary,
                            label = "گالری",
                            active = false,
                            onClick = onPickGallery,
                        )

                        ScannerRoundAction(
                            icon = Icons.Rounded.Keyboard,
                            label = "کد دستی",
                            active = false,
                            onClick = onManualEntry,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerRoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (active) TelegramScannerIndigo else Color.White.copy(alpha = 0.14f),
        label = "scanner_action_bg",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            border = BorderStroke(
                width = 1.dp,
                color = if (active) TelegramScannerIndigo else Color.White.copy(alpha = 0.24f),
            ),
            modifier = Modifier.size(54.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TelegramViewfinderOverlay(
    isRecognized: Boolean,
    focusTapOffset: Offset?,
    onFocusAnimationFinished: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "qr_beam")
    val beamProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "beam_y",
    )
    val cornerPulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "corner_alpha",
    )
    val lockScale by animateFloatAsState(
        targetValue = if (isRecognized) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "lock_scale",
    )
    val cornerColor by animateColorAsState(
        targetValue = if (isRecognized) TelegramScannerLocked else Color.White,
        label = "corner_color",
    )

    var tapRingVisible by remember(focusTapOffset) { mutableStateOf(focusTapOffset != null) }
    val tapRingScale by animateFloatAsState(
        targetValue = if (tapRingVisible) 1f else 0.45f,
        animationSpec = tween(220),
        label = "tap_ring",
    )
    LaunchedEffect(focusTapOffset) {
        if (focusTapOffset != null) {
            tapRingVisible = true
            delay(550L)
            tapRingVisible = false
            onFocusAnimationFinished()
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val boxSide = (minOf(size.width, size.height) * 0.66f)
            .coerceIn(220.dp.toPx(), 300.dp.toPx()) * lockScale
        val left = (size.width - boxSide) / 2f
        val top = (size.height - boxSide) / 2f - 28.dp.toPx()
        val cornerRadiusPx = 28.dp.toPx()

        // Telegram-style dark translucent scrim outside the rounded reticle
        drawRect(Color(0xB8060812))

        // Punched clear window
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = ComposeSize(boxSide, boxSide),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            blendMode = BlendMode.Clear,
        )

        // Animated gradient scan beam strictly clipped inside the rounded scan box
        if (!isRecognized) {
            val cutoutPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = Rect(left, top, left + boxSide, top + boxSide),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    ),
                )
            }
            clipPath(cutoutPath) {
                val beamTop = top + beamProgress * (boxSide - 46.dp.toPx())
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TelegramScannerIndigo.copy(alpha = 0.34f),
                            TelegramScannerViolet.copy(alpha = 0.65f),
                        ),
                        startY = beamTop,
                        endY = beamTop + 44.dp.toPx(),
                    ),
                    topLeft = Offset(left, beamTop),
                    size = ComposeSize(boxSide, 44.dp.toPx()),
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TelegramScannerIndigo,
                            Color.White,
                            TelegramScannerIndigo,
                            Color.Transparent,
                        ),
                        startX = left,
                        endX = left + boxSide,
                    ),
                    start = Offset(left, beamTop + 44.dp.toPx()),
                    end = Offset(left + boxSide, beamTop + 44.dp.toPx()),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        // Subtle inner rim around cutout
        drawRoundRect(
            color = Color.White.copy(alpha = 0.16f),
            topLeft = Offset(left, top),
            size = ComposeSize(boxSide, boxSide),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = 1.dp.toPx()),
        )

        // Telegram 4 rounded corner brackets (thick rounded arcs)
        val strokePx = 4.2.dp.toPx()
        val legLen = 28.dp.toPx()
        val arcDia = cornerRadiusPx * 2f
        val bracketColor = cornerColor.copy(alpha = if (isRecognized) 1f else cornerPulse)
        val accentGlow = (if (isRecognized) TelegramScannerLocked else TelegramScannerIndigo).copy(alpha = 0.45f)

        val corners = Path().apply {
            // Top-left
            moveTo(left, top + cornerRadiusPx + legLen)
            lineTo(left, top + cornerRadiusPx)
            arcTo(Rect(left, top, left + arcDia, top + arcDia), 180f, 90f, false)
            lineTo(left + cornerRadiusPx + legLen, top)

            // Top-right
            moveTo(left + boxSide - cornerRadiusPx - legLen, top)
            lineTo(left + boxSide - cornerRadiusPx, top)
            arcTo(Rect(left + boxSide - arcDia, top, left + boxSide, top + arcDia), 270f, 90f, false)
            lineTo(left + boxSide, top + cornerRadiusPx + legLen)

            // Bottom-right
            moveTo(left + boxSide, top + boxSide - cornerRadiusPx - legLen)
            lineTo(left + boxSide, top + boxSide - cornerRadiusPx)
            arcTo(
                Rect(left + boxSide - arcDia, top + boxSide - arcDia, left + boxSide, top + boxSide),
                0f,
                90f,
                false,
            )
            lineTo(left + boxSide - cornerRadiusPx - legLen, top + boxSide)

            // Bottom-left
            moveTo(left + cornerRadiusPx + legLen, top + boxSide)
            lineTo(left + cornerRadiusPx, top + boxSide)
            arcTo(
                Rect(left, top + boxSide - arcDia, left + arcDia, top + boxSide),
                90f,
                90f,
                false,
            )
            lineTo(left, top + boxSide - cornerRadiusPx - legLen)
        }

        drawPath(
            path = corners,
            color = accentGlow,
            style = Stroke(width = strokePx + 5.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = corners,
            color = bracketColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )

        if (tapRingVisible && focusTapOffset != null) {
            drawCircle(
                color = TelegramScannerIndigo.copy(alpha = 0.85f),
                radius = 28.dp.toPx() * tapRingScale,
                center = focusTapOffset,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

private class TelegramCameraController(
    private val context: Context,
    private val textureView: TextureView,
    private val isPausedProvider: () -> Boolean,
    private val onControllerReady: (TelegramCameraController, Boolean) -> Unit,
    private val onQrDecoded: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var thread: HandlerThread? = null
    private var bgHandler: Handler? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var imageReader: ImageReader? = null
    private var cameraId: String? = null
    private var characteristics: CameraCharacteristics? = null
    private var previewSize: Size = Size(1280, 720)
    private var isTorchOn = false
    private val decodingFrame = AtomicBoolean(false)
    private var frameCounter = 0L

    @Volatile
    private var closed = false

    fun start() {
        val t = HandlerThread("VistaTelegramQrCamera").apply { start() }
        thread = t
        bgHandler = Handler(t.looper)

        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        }
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera(width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                configureTransform(width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(viewWidth: Int, viewHeight: Int) {
        if (closed) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (cameraDevice != null) {
            configureTransform(viewWidth, viewHeight)
            return
        }

        val selectedId = selectBackCameraId()
        if (selectedId == null) {
            onError("دوربین پشت در این دستگاه یافت نشد")
            return
        }
        cameraId = selectedId
        val chars = manager.getCameraCharacteristics(selectedId)
        characteristics = chars
        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        previewSize = chooseOptimalSize(map?.getOutputSizes(SurfaceTexture::class.java), 1280, 720)
        val analysisSize = chooseOptimalSize(map?.getOutputSizes(ImageFormat.YUV_420_888), 1280, 720)

        val reader = ImageReader.newInstance(
            analysisSize.width,
            analysisSize.height,
            ImageFormat.YUV_420_888,
            2,
        )
        reader.setOnImageAvailableListener({ r ->
            val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                if (closed || isPausedProvider() || !decodingFrame.compareAndSet(false, true)) {
                    return@setOnImageAvailableListener
                }
                val width = img.width
                val height = img.height
                val yBytes = extractTightYPlane(img.planes[0].buffer, width, height, img.planes[0].rowStride)
                val currentFrame = ++frameCounter
                try {
                    val text = decodeQrFromYPlane(yBytes, width, height, includeFullFrame = currentFrame % 2L == 0L)
                    if (!text.isNullOrBlank()) {
                        onQrDecoded(text)
                    }
                } finally {
                    decodingFrame.set(false)
                }
            } finally {
                img.close()
            }
        }, bgHandler)
        imageReader = reader

        configureTransform(viewWidth, viewHeight)

        runCatching {
            manager.openCamera(
                selectedId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(device: CameraDevice) {
                        if (closed) {
                            device.close()
                            return
                        }
                        cameraDevice = device
                        createSession(device, reader, hasFlash)
                    }

                    override fun onDisconnected(device: CameraDevice) {
                        device.close()
                        cameraDevice = null
                    }

                    override fun onError(device: CameraDevice, error: Int) {
                        device.close()
                        cameraDevice = null
                        onError("باز کردن دوربین ناموفق بود (کد $error)")
                    }
                },
                bgHandler,
            )
        }.onFailure {
            onError("خطا در راه‌اندازی دوربین")
        }
    }

    private fun createSession(device: CameraDevice, reader: ImageReader, hasFlash: Boolean) {
        val surfaceTexture = textureView.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)
        val analysisSurface = reader.surface

        runCatching {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                addTarget(analysisSurface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                if (isTorchOn && hasFlash) {
                    set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
                }
            }
            previewBuilder = builder

            @Suppress("DEPRECATION")
            device.createCaptureSession(
                listOf(previewSurface, analysisSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (closed) return
                        captureSession = session
                        runCatching {
                            session.setRepeatingRequest(builder.build(), null, bgHandler)
                        }
                        textureView.post {
                            if (!closed) onControllerReady(this@TelegramCameraController, hasFlash)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError("پیکربندی نشست دوربین ناموفق بود")
                    }
                },
                bgHandler,
            )
        }.onFailure {
            onError("خطا در نمایش تصویر دوربین")
        }
    }

    fun setTorch(enabled: Boolean) {
        isTorchOn = enabled
        val builder = previewBuilder ?: return
        val session = captureSession ?: return
        val hasFlash = characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        if (!hasFlash) return
        bgHandler?.post {
            runCatching {
                builder.set(
                    CaptureRequest.FLASH_MODE,
                    if (enabled) CameraMetadata.FLASH_MODE_TORCH else CameraMetadata.FLASH_MODE_OFF,
                )
                session.setRepeatingRequest(builder.build(), null, bgHandler)
            }
        }
    }

    fun triggerTapFocus(x: Float, y: Float, viewW: Int, viewH: Int) {
        val builder = previewBuilder ?: return
        val session = captureSession ?: return
        val chars = characteristics ?: return
        val sensorRect = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return
        val maxAfRegions = chars.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0
        if (maxAfRegions <= 0 || viewW <= 0 || viewH <= 0) return

        val nx = (y / viewH).coerceIn(0f, 1f)
        val ny = (1f - x / viewW).coerceIn(0f, 1f)
        val centerX = (nx * sensorRect.width()).toInt()
        val centerY = (ny * sensorRect.height()).toInt()
        val half = 150
        val region = MeteringRectangle(
            (centerX - half).coerceIn(0, sensorRect.width() - 1),
            (centerY - half).coerceIn(0, sensorRect.height() - 1),
            (half * 2).coerceAtMost(sensorRect.width()),
            (half * 2).coerceAtMost(sensorRect.height()),
            MeteringRectangle.METERING_WEIGHT_MAX,
        )

        bgHandler?.post {
            runCatching {
                builder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(region))
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
                session.capture(builder.build(), null, bgHandler)
                builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
                session.setRepeatingRequest(builder.build(), null, bgHandler)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        if (viewWidth <= 0 || viewHeight <= 0) return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = wm.defaultDisplay?.rotation ?: Surface.ROTATION_0
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = maxOf(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width,
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else {
            // Portrait CenterCrop scaling (previewSize width > height on sensor)
            val targetRatio = previewSize.height.toFloat() / previewSize.width.toFloat()
            val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
            val scaleX: Float
            val scaleY: Float
            if (viewRatio > targetRatio) {
                scaleX = 1f
                scaleY = viewRatio / targetRatio
            } else {
                scaleX = targetRatio / viewRatio
                scaleY = 1f
            }
            matrix.setScale(scaleX, scaleY, centerX, centerY)
            if (rotation == Surface.ROTATION_180) {
                matrix.postRotate(180f, centerX, centerY)
            }
        }
        textureView.post {
            textureView.setTransform(matrix)
        }
    }

    private fun selectBackCameraId(): String? {
        for (id in manager.cameraIdList) {
            val chars = manager.getCameraCharacteristics(id)
            if (chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
        }
        return manager.cameraIdList.firstOrNull()
    }

    private fun chooseOptimalSize(choices: Array<out Size>?, targetW: Int, targetH: Int): Size {
        if (choices.isNullOrEmpty()) return Size(targetW, targetH)
        return choices
            .filter { it.width <= 1920 && it.height <= 1080 }
            .minByOrNull { abs(it.width * it.height - targetW * targetH) }
            ?: choices.first()
    }

    fun close() {
        closed = true
        runCatching { captureSession?.close() }
        captureSession = null
        runCatching { cameraDevice?.close() }
        cameraDevice = null
        runCatching { imageReader?.close() }
        imageReader = null
        thread?.quitSafely()
        thread = null
        bgHandler = null
    }
}

@Composable
private fun TelegramCameraPreview(
    isPaused: Boolean,
    onControllerReady: (TelegramCameraController, Boolean) -> Unit,
    onTapToFocus: (Offset) -> Unit,
    onQrDecoded: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    var latestPaused by remember { mutableStateOf(isPaused) }
    latestPaused = isPaused

    val textureView = remember { TextureView(context) }
    val controller = remember(textureView) {
        TelegramCameraController(
            context = context,
            textureView = textureView,
            isPausedProvider = { latestPaused },
            onControllerReady = onControllerReady,
            onQrDecoded = onQrDecoded,
            onError = onError,
        )
    }

    DisposableEffect(controller) {
        textureView.setOnTouchListener { v, ev ->
            if (ev.action == MotionEvent.ACTION_UP) {
                onTapToFocus(Offset(ev.x, ev.y))
                controller.triggerTapFocus(ev.x, ev.y, v.width, v.height)
            }
            true
        }
        controller.start()
        onDispose {
            controller.close()
        }
    }

    AndroidView(
        factory = { textureView },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun extractTightYPlane(
    buffer: java.nio.ByteBuffer,
    width: Int,
    height: Int,
    rowStride: Int,
): ByteArray {
    val out = ByteArray(width * height)
    buffer.rewind()
    if (rowStride == width) {
        val copyLen = minOf(out.size, buffer.remaining())
        buffer.get(out, 0, copyLen)
        return out
    }
    val rowBuf = ByteArray(rowStride)
    for (row in 0 until height) {
        if (buffer.remaining() < width) break
        val toRead = minOf(rowStride, buffer.remaining())
        buffer.get(rowBuf, 0, toRead)
        System.arraycopy(rowBuf, 0, out, row * width, width)
    }
    return out
}

private val qrHints: Map<DecodeHintType, Any> = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
    put(DecodeHintType.TRY_HARDER, true)
    put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
}

private fun decodeWithReader(reader: QRCodeReader, source: LuminanceSource): String? {
    runCatching {
        return reader.decode(BinaryBitmap(HybridBinarizer(source)), qrHints).text
    }
    runCatching {
        return reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), qrHints).text
    }
    runCatching {
        return reader.decode(BinaryBitmap(HybridBinarizer(source.invert())), qrHints).text
    }
    return null
}

/**
 * Telegram-inspired multi-pass QR decoder for live YUV preview frames:
 * 1) Centered square RoI crop matching the scan reticle (normal + inverted + histogram).
 * 2) Full frame pass when alternating frames or when QR fills more of the viewport.
 */
internal fun decodeQrFromYPlane(
    yPlane: ByteArray,
    width: Int,
    height: Int,
    includeFullFrame: Boolean = true,
): String? {
    if (width <= 0 || height <= 0 || yPlane.size < width * height) return null
    val reader = QRCodeReader()

    val cropSide = (minOf(width, height) * 0.72f).toInt().coerceAtLeast(minOf(width, height) / 2)
    val left = ((width - cropSide) / 2).coerceAtLeast(0)
    val top = ((height - cropSide) / 2).coerceAtLeast(0)

    val centerSource = PlanarYUVLuminanceSource(
        yPlane,
        width,
        height,
        left,
        top,
        cropSide,
        cropSide,
        false,
    )
    decodeWithReader(reader, centerSource)?.let { return it }

    if (includeFullFrame) {
        val fullSource = PlanarYUVLuminanceSource(
            yPlane,
            width,
            height,
            0,
            0,
            width,
            height,
            false,
        )
        decodeWithReader(reader, fullSource)?.let { return it }
    }
    return null
}

/**
 * Decodes QR payload from a gallery image URI using multi-pass TRY_HARDER,
 * Hybrid/Global binarizers, and inverted luminance passes.
 */
internal fun decodeQrTextFromUri(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val raw = BitmapFactory.decodeStream(stream) ?: return@use null
        val maxDim = maxOf(raw.width, raw.height)
        val bitmap = if (maxDim > 1600) {
            val scale = 1600f / maxDim
            Bitmap.createScaledBitmap(
                raw,
                (raw.width * scale).toInt().coerceAtLeast(1),
                (raw.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            raw
        }
        val reader = QRCodeReader()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val fullSource = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        decodeWithReader(reader, fullSource)?.let { return@use it }

        // Retry on central square crop for wide screenshots containing small QR codes
        val side = (minOf(bitmap.width, bitmap.height) * 0.7f).toInt()
        if (side > 64 && fullSource.isCropSupported) {
            val cropped = fullSource.crop(
                (bitmap.width - side) / 2,
                (bitmap.height - side) / 2,
                side,
                side,
            )
            decodeWithReader(reader, cropped)?.let { return@use it }
        }
        null
    }
}.getOrNull()
