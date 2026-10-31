package ir.coffevista.vista_native.features.stories.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.common.toJalali
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.data.StorySecurityGuard
import ir.coffevista.vista_native.features.stories.domain.StoryDurationType
import ir.coffevista.vista_native.features.stories.domain.StoryPoll
import ir.coffevista.vista_native.features.stories.domain.StoryPollOption
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import ir.coffevista.vista_native.features.stories.ui.canvas.PRIMARY_MEDIA_GESTURE_ID
import ir.coffevista.vista_native.features.stories.ui.canvas.SnapGuides
import ir.coffevista.vista_native.features.stories.ui.canvas.SnapState
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryGestureCallbacks
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryHaptics
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryHitTest
import ir.coffevista.vista_native.features.stories.ui.canvas.StorySnapEngine
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryStage
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryTransform
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryTransformMath
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryTrashZone
import ir.coffevista.vista_native.features.stories.ui.canvas.detectStoryGestures
import ir.coffevista.vista_native.features.stories.ui.canvas.resolvedTransform
import ir.coffevista.vista_native.features.stories.ui.render.ElementVisualState
import ir.coffevista.vista_native.features.stories.ui.render.LinkIcon
import ir.coffevista.vista_native.features.stories.ui.render.LocalStoryDominantColors
import ir.coffevista.vista_native.features.stories.ui.render.StoryCountdownWidget
import ir.coffevista.vista_native.features.stories.ui.render.StoryDominantColors
import ir.coffevista.vista_native.features.stories.ui.render.StoryDrawingCodec
import ir.coffevista.vista_native.features.stories.ui.render.StoryElementLayers
import ir.coffevista.vista_native.features.stories.ui.render.StoryPillWidget
import ir.coffevista.vista_native.features.stories.ui.render.StoryPollWidget
import ir.coffevista.vista_native.features.stories.ui.render.StoryQuestionWidget
import ir.coffevista.vista_native.features.stories.ui.render.StoryRasterizer
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextSpec
import ir.coffevista.vista_native.features.stories.ui.render.toTextSpec
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StoryEditorScreen(
    onClose: () -> Unit,
    onStoryPublished: () -> Unit,
    modifier: Modifier = Modifier,
    initialMediaUri: Uri? = null,
    initialMediaMimeType: String? = null,
    initialSharedContentId: String? = null,
    onInitialSharedContentConsumed: (String) -> Unit = {},
    viewModel: StoryEditorViewModel = hiltViewModel(),
) {
    LaunchedEffect(initialSharedContentId, initialMediaUri, initialMediaMimeType) {
        initialMediaUri?.let { uri ->
            viewModel.onMediaSelected(
                uri,
                isVideo = initialMediaMimeType?.startsWith("video/", ignoreCase = true) == true,
            )
            initialSharedContentId?.let(onInitialSharedContentConsumed)
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current
    val haptics = remember(view) { StoryHaptics(view) }
    val context = LocalContext.current

    // Loaded Bitmap & Extracted Dual-Color Dominant Gradient (Telegram StoryEntry parity)
    var loadedMediaBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dominantColors by remember { mutableStateOf(StoryDominantColors()) }

    LaunchedEffect(uiState.mediaUri, uiState.isVideo) {
        val uri = uiState.mediaUri
        if (uri != null && !uiState.isVideo) {
            val bmp = StoryRasterizer.loadMediaBitmap(context, uri)
            loadedMediaBitmap = bmp
            dominantColors = StoryRasterizer.extractDominantColors(bmp)
        } else {
            loadedMediaBitmap = null
            dominantColors = StoryDominantColors()
        }
    }

    var showTextEditor by remember { mutableStateOf(false) }
    var editingTextElementId by remember { mutableStateOf<String?>(null) }
    var textEditorInitialSpec by remember { mutableStateOf(StoryTextSpec("")) }

    var showDrawingEditor by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }

    var showPollDialog by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("بله") }
    var pollOption2 by remember { mutableStateOf("خیر") }
    var pollPreviewVariant by remember { mutableIntStateOf(0) }

    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("") }
    var linkTitle by remember { mutableStateOf("") }
    var linkPreviewVariant by remember { mutableIntStateOf(0) }

    var showMentionDialog by remember { mutableStateOf(false) }
    var mentionUsername by remember { mutableStateOf("") }
    var mentionPreviewVariant by remember { mutableIntStateOf(1) }

    var showLocationDialog by remember { mutableStateOf(false) }
    var locationPreviewVariant by remember { mutableIntStateOf(1) }

    var showQuestionDialog by remember { mutableStateOf(false) }
    var questionText by remember { mutableStateOf("") }
    var questionPreviewVariant by remember { mutableIntStateOf(0) }

    var showCountdownDialog by remember { mutableStateOf(false) }
    var countdownTitle by remember { mutableStateOf("") }
    var countdownHours by remember { mutableIntStateOf(24) }
    var countdownPreviewVariant by remember { mutableIntStateOf(1) }

    var showWeatherDialog by remember { mutableStateOf(false) }
    var weatherPreviewVariant by remember { mutableIntStateOf(0) }

    var showHashtagDialog by remember { mutableStateOf(false) }
    var hashtagText by remember { mutableStateOf("") }
    var hashtagPreviewVariant by remember { mutableIntStateOf(2) }

    var showGifDialog by remember { mutableStateOf(false) }
    var gifUrlInput by remember { mutableStateOf("") }

    var showMusicPickerSheet by remember { mutableStateOf(false) }
    var showCloseFriendsDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = uiState.mediaUri != null || uiState.elements.isNotEmpty()

    val requestClose: () -> Unit = {
        if (hasUnsavedChanges) showDiscardDialog = true else onClose()
    }

    BackHandler(enabled = hasUnsavedChanges && !showTextEditor && !showDrawingEditor) {
        requestClose()
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(uiState.mediaUri, uiState.isVideo) {
        val uri = uiState.mediaUri
        if (uiState.isVideo && uri != null) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
        } else null
    }

    // Mute original video audio when story music is attached so tracks never clash
    LaunchedEffect(exoPlayer, uiState.musicTrack) {
        exoPlayer?.volume = if (uiState.musicTrack != null) 0f else 1f
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    // Live Editor Canvas Music Preview (with trimmed chorus loop)
    val activeMusicUrl = uiState.musicTrack?.musicUrl?.takeIf { it.isNotBlank() && uiState.mediaUri != null }
    val editorMusicPlayer = remember(activeMusicUrl, uiState.musicStartMs, uiState.musicEndMs) {
        if (activeMusicUrl != null) {
            ExoPlayer.Builder(context).build().apply {
                val clipBuilder = MediaItem.Builder().setUri(activeMusicUrl)
                if (uiState.musicStartMs > 0 || uiState.musicEndMs > uiState.musicStartMs) {
                    clipBuilder.setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(uiState.musicStartMs.toLong().coerceAtLeast(0L))
                            .apply {
                                if (uiState.musicEndMs > uiState.musicStartMs) {
                                    setEndPositionMs(uiState.musicEndMs.toLong())
                                }
                            }
                            .build(),
                    )
                }
                setMediaItem(clipBuilder.build())
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = !showMusicPickerSheet && !uiState.isUploading && !uiState.isSuccess
            }
        } else null
    }

    val shouldPlayCanvasMusic = !showMusicPickerSheet && !uiState.isUploading && !uiState.isSuccess
    LaunchedEffect(editorMusicPlayer, shouldPlayCanvasMusic) {
        editorMusicPlayer?.playWhenReady = shouldPlayCanvasMusic
    }

    val latestShouldPlayMusic by rememberUpdatedState(shouldPlayCanvasMusic)
    DisposableEffect(lifecycleOwner, exoPlayer, editorMusicPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer?.playWhenReady = false
                    editorMusicPlayer?.playWhenReady = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!uiState.isUploading && !uiState.isSuccess) {
                        exoPlayer?.playWhenReady = true
                    }
                    editorMusicPlayer?.playWhenReady = latestShouldPlayMusic
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            editorMusicPlayer?.release()
        }
    }

    // Secondary Photo-on-Photo Cutout Picker (Telegram PhotoPaintView parity)
    val photoCutoutPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.addPhotoCutoutElement(uri)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onStoryPublished()
        }
    }

    uiState.errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    var selectedElementId by remember { mutableStateOf<String?>(null) }
    var activeDraggingId by remember { mutableStateOf<String?>(null) }
    var isHoveredTrash by remember { mutableStateOf(false) }
    var trashProximity by remember { mutableFloatStateOf(0f) }
    var snapGuides by remember { mutableStateOf(SnapGuides.None) }
    var snapState by remember { mutableStateOf(SnapState.None) }

    val transientTransforms = remember { mutableStateMapOf<String, StoryTransform>() }
    val elementSizes = remember { mutableStateMapOf<String, IntSize>() }

    // Animated trash scale/alpha progress (1.0 -> 0.30x scale interpolation on trash hover)
    val trashHoverProgress by animateFloatAsState(
        targetValue = if (isHoveredTrash) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "trash_capture_progress",
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalStoryDominantColors provides dominantColors,
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Black,
            modifier = modifier.fillMaxSize(),
        ) { padding ->
            if (uiState.mediaUri == null) {
                // PILLAR 1: Live Hardware CameraX Studio + Instagram Swipe-Up Gallery Drawer
                StoryCameraStudio(
                    onMediaUriSelected = { uri, isVid ->
                        viewModel.onMediaSelected(uri, isVideo = isVid)
                    },
                    onPickSystemVisualMedia = {},
                    onClose = onClose,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            } else {
                // PILLARS 2 & 3: Multi-Layered Compositing Canvas (PaintView & EntityView parity)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    StoryStage(
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 18.dp,
                    ) { stage ->
                        // LAYER 0: Dynamic Dual-Color Dominant Background Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(dominantColors.topColor, dominantColors.bottomColor),
                                    ),
                                ),
                        )

                        // LAYER 1: Transformable Primary Media Layer (Pinch/Pan/Rotate support)
                        val activeMediaTransform = transientTransforms[PRIMARY_MEDIA_GESTURE_ID] ?: uiState.mediaTransform
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = (activeMediaTransform.cx - 0.5f) * stage.width
                                    translationY = (activeMediaTransform.cy - 0.5f) * stage.height
                                    scaleX = activeMediaTransform.scale
                                    scaleY = activeMediaTransform.scale
                                    rotationZ = activeMediaTransform.rotation
                                },
                        ) {
                            if (uiState.isVideo && exoPlayer != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = exoPlayer
                                            useController = false
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            layoutParams = FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                AsyncImage(
                                    model = uiState.mediaUri,
                                    contentDescription = "پیش‌نمایش استوری",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }

                        val snapEngine = remember(stage) { StorySnapEngine(stage) }
                        val trashZone = remember(stage) { StoryTrashZone(stage) }

                        // LAYERS 2 & 3: Freehand Drawing Layer + Z-Ordered Interactive Entities
                        StoryElementLayers(
                            elements = uiState.elements,
                            stage = stage,
                            selectedElementId = if (activeDraggingId == null) selectedElementId else null,
                            visualFor = { el ->
                                val active = transientTransforms[el.id]
                                val baseTransform = active ?: el.resolvedTransform()
                                val isBeingDragged = el.id == activeDraggingId
                                val effectiveTransform = if (isBeingDragged && trashHoverProgress > 0.001f) {
                                    trashZone.applyMagneticPull(baseTransform, trashHoverProgress)
                                } else {
                                    baseTransform
                                }
                                ElementVisualState(
                                    transform = { effectiveTransform },
                                    extraScale = {
                                        if (isBeingDragged) {
                                            StoryTrashZone.interpolateScaleMultiplier(trashHoverProgress)
                                        } else 1f
                                    },
                                    alpha = {
                                        if (isBeingDragged) {
                                            StoryTrashZone.interpolateAlpha(trashHoverProgress)
                                        } else 1f
                                    },
                                )
                            },
                            onSizeChanged = { id, size ->
                                elementSizes[id] = size
                            },
                        )

                        // LAYER 4A: Magnetic Alignment Guidelines Overlay
                        StorySnapGuidesOverlay(guides = snapGuides)

                        val latestElements by rememberUpdatedState(uiState.elements)
                        val latestMediaTransform by rememberUpdatedState(uiState.mediaTransform)

                        val gestureCallbacks = remember(stage) {
                            object : StoryGestureCallbacks {
                                override val selectedId: String? get() = selectedElementId

                                override fun hitTest(x: Float, y: Float): String? {
                                    val nonDrawing = latestElements.filterNot { it.type == "drawing" }.reversed()
                                    for (el in nonDrawing) {
                                        val t = transientTransforms[el.id] ?: el.resolvedTransform()
                                        val size = elementSizes[el.id]
                                        val halfW = (size?.width?.toFloat() ?: 140f) / 2f
                                        val halfH = (size?.height?.toFloat() ?: 68f) / 2f
                                        if (StoryHitTest.contains(t, stage, halfW, halfH, x, y, minTouchHalfPx = 40f)) {
                                            return el.id
                                        }
                                    }
                                    return null
                                }

                                override fun onGestureStart(id: String) {
                                    activeDraggingId = id
                                    if (id == PRIMARY_MEDIA_GESTURE_ID) {
                                        transientTransforms[id] = latestMediaTransform
                                    } else {
                                        selectedElementId = id
                                        val el = latestElements.firstOrNull { it.id == id }
                                        if (el != null) {
                                            transientTransforms[id] = el.resolvedTransform()
                                        }
                                        viewModel.bringToFront(id)
                                    }
                                    snapState = SnapState.None
                                    snapGuides = SnapGuides.None
                                }

                                override fun onGestureUpdate(
                                    prevCx: Float,
                                    prevCy: Float,
                                    cx: Float,
                                    cy: Float,
                                    zoom: Float,
                                    rotationDeg: Float,
                                    pointerX: Float,
                                    pointerY: Float,
                                ) {
                                    val id = activeDraggingId ?: return
                                    val current = transientTransforms[id] ?: return

                                    val updated = StoryTransformMath.applyGesture(
                                        t = current,
                                        stage = stage,
                                        prevCentroidX = prevCx,
                                        prevCentroidY = prevCy,
                                        centroidX = cx,
                                        centroidY = cy,
                                        zoom = zoom,
                                        rotationDeg = rotationDeg,
                                    )

                                    if (id == PRIMARY_MEDIA_GESTURE_ID) {
                                        val snapRes = snapEngine.snap(
                                            raw = updated,
                                            halfW = stage.width * 0.5f,
                                            halfH = stage.height * 0.5f,
                                            previous = snapState,
                                            snapPosition = true,
                                            snapRotation = true,
                                        )
                                        if (snapRes.state.newlyEngagedComparedTo(snapState)) {
                                            haptics.snap()
                                        }
                                        snapState = snapRes.state
                                        snapGuides = snapRes.guides
                                        transientTransforms[id] = snapRes.transform
                                        isHoveredTrash = false
                                        trashProximity = 0f
                                        return
                                    }

                                    val size = elementSizes[id]
                                    val halfW = (size?.width?.toFloat() ?: 140f) / 2f
                                    val halfH = (size?.height?.toFloat() ?: 68f) / 2f
                                    val snapRes = snapEngine.snap(
                                        raw = updated,
                                        halfW = halfW,
                                        halfH = halfH,
                                        previous = snapState,
                                    )

                                    if (snapRes.state.newlyEngagedComparedTo(snapState)) {
                                        haptics.snap()
                                    }
                                    snapState = snapRes.state
                                    snapGuides = snapRes.guides

                                    transientTransforms[id] = snapRes.transform

                                    val wasHovered = isHoveredTrash
                                    val nowHovered = trashZone.isHovered(pointerX, pointerY)
                                    trashProximity = trashZone.proximity(pointerX, pointerY)
                                    if (nowHovered != wasHovered) {
                                        haptics.trashHover()
                                    }
                                    isHoveredTrash = nowHovered
                                }

                                override fun onGestureEnd(id: String, cancelled: Boolean) {
                                    val finalTransform = transientTransforms[id]
                                    transientTransforms.remove(id)
                                    snapGuides = SnapGuides.None
                                    snapState = SnapState.None

                                    if (id == PRIMARY_MEDIA_GESTURE_ID) {
                                        if (finalTransform != null && !cancelled) {
                                            val clampedScale = finalTransform.scale.coerceIn(0.35f, 4.0f)
                                            viewModel.updateMediaTransform(finalTransform.copy(scale = clampedScale))
                                        }
                                        activeDraggingId = null
                                        isHoveredTrash = false
                                        trashProximity = 0f
                                        return
                                    }

                                    if (isHoveredTrash && !cancelled) {
                                        haptics.deleted()
                                        viewModel.removeElement(id)
                                        if (selectedElementId == id) selectedElementId = null
                                    } else if (finalTransform != null && !cancelled) {
                                        val size = elementSizes[id]
                                        val halfW = (size?.width?.toFloat() ?: 140f) / 2f
                                        val halfH = (size?.height?.toFloat() ?: 68f) / 2f
                                        val resting = StoryTransformMath.restingTransform(
                                            t = finalTransform,
                                            halfExtentsUnrotatedPx = halfW to halfH,
                                            stage = stage,
                                        )
                                        viewModel.updateElementTransform(id, resting)
                                    }

                                    activeDraggingId = null
                                    isHoveredTrash = false
                                    trashProximity = 0f
                                }

                                override fun onTap(x: Float, y: Float, hitId: String?) {
                                    if (hitId != null) {
                                        selectedElementId = hitId
                                        viewModel.bringToFront(hitId)
                                        val el = latestElements.firstOrNull { it.id == hitId }
                                        if (el != null) {
                                            if (el.type == "text") {
                                                editingTextElementId = el.id
                                                textEditorInitialSpec = el.toTextSpec()
                                                showTextEditor = true
                                            } else {
                                                // Tap-to-Cycle Visual Theme or Photo Cutout Shape Mask
                                                viewModel.cycleWidgetVariant(el.id)
                                                haptics.boundary()
                                            }
                                        }
                                    } else {
                                        selectedElementId = null
                                    }
                                }

                                override fun onDoubleTap(x: Float, y: Float, hitId: String?) {
                                    if (hitId != null) {
                                        val el = latestElements.firstOrNull { it.id == hitId }
                                        if (el != null && el.type == "text") {
                                            editingTextElementId = el.id
                                            textEditorInitialSpec = el.toTextSpec()
                                            showTextEditor = true
                                        }
                                    } else {
                                        viewModel.resetMediaTransform()
                                        haptics.snap()
                                    }
                                }
                            }
                        }

                        val latestGestureCallbacks by rememberUpdatedState(gestureCallbacks)

                        // Key ONLY on `stage` so z-index reordering on drag-start NEVER cancels active touch streams!
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(stage) {
                                    detectStoryGestures { latestGestureCallbacks }
                                },
                        )

                        // Subtle Top & Bottom Scrims inside stage for crisp toolbar contrast
                        if (activeDraggingId == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                                        ),
                                    ),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f)),
                                        ),
                                    ),
                            )
                        }
                    }

                    // LAYER 4B: Interactive Trash Target with Lid Animation & Magnetic Proximity
                    StoryTrashDropZone(
                        isVisible = activeDraggingId != null && activeDraggingId != PRIMARY_MEDIA_GESTURE_ID,
                        isHovered = isHoveredTrash,
                        proximity = trashProximity,
                    )

                    // LAYER 4C: Floating Emoji Reaction Strip when a Reaction Widget is Selected
                    val selectedReactionElement = remember(selectedElementId, uiState.elements) {
                        uiState.elements.firstOrNull { it.id == selectedElementId && it.type == "reaction" }
                    }
                    AnimatedVisibility(
                        visible = selectedReactionElement != null && activeDraggingId == null,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 88.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xEB151620))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            listOf("❤️", "🔥", "👍", "🎉", "🤩", "😂", "⚡", "👏").forEach { emoji ->
                                val isCurrent = selectedReactionElement?.content == emoji
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrent) VistaBrandColors.Indigo else Color.Transparent)
                                        .clickable {
                                            selectedReactionElement?.let {
                                                viewModel.updateReactionEmoji(it.id, emoji)
                                                haptics.snap()
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    // LAYER 4D: Clean Flagship Top Studio Toolbar (Zero Clutter)
                    AnimatedVisibility(
                        visible = activeDraggingId == null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // Right side (in RTL): Close + Undo / Redo
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StudioCircleIconButton(
                                    icon = Icons.Default.Close,
                                    contentDescription = "بستن",
                                    onClick = requestClose,
                                )

                                if (uiState.canUndo) {
                                    StudioCircleIconButton(
                                        icon = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "بازگشت",
                                        onClick = {
                                            viewModel.undo()
                                            haptics.snap()
                                        },
                                    )
                                }
                                if (uiState.canRedo) {
                                    StudioCircleIconButton(
                                        icon = Icons.AutoMirrored.Filled.Redo,
                                        contentDescription = "انجام مجدد",
                                        onClick = {
                                            viewModel.redo()
                                            haptics.snap()
                                        },
                                    )
                                }
                            }

                            // Left side (in RTL): Aa | Sticker/Widget | Brush | Photo Cutout | Music
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // Typography Studio ("Aa")
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x8C101218))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                        .clickable {
                                            editingTextElementId = null
                                            textEditorInitialSpec = StoryTextSpec("")
                                            showTextEditor = true
                                            haptics.snap()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Aa",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                    )
                                }

                                // Telegram Interactive Widget & Sticker Drawer
                                StudioCircleIconButton(
                                    icon = Icons.Default.EmojiEmotions,
                                    contentDescription = "ویجت و استیکر",
                                    onClick = {
                                        showStickerSheet = true
                                        haptics.snap()
                                    },
                                )

                                // Freehand Neon / Marker / Pen Brush Studio
                                StudioCircleIconButton(
                                    icon = Icons.Default.Brush,
                                    contentDescription = "نقاشی",
                                    active = uiState.elements.any { it.type == "drawing" },
                                    onClick = {
                                        showDrawingEditor = true
                                        haptics.snap()
                                    },
                                )

                                // Photo-on-Photo Cutout Sticker
                                StudioCircleIconButton(
                                    icon = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "برش عکس",
                                    onClick = {
                                        photoCutoutPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                )

                                // Music Picker
                                StudioCircleIconButton(
                                    icon = Icons.Default.MusicNote,
                                    contentDescription = "موزیک",
                                    active = uiState.musicTrack != null || uiState.elements.any { it.type == "music" },
                                    onClick = {
                                        showMusicPickerSheet = true
                                        haptics.snap()
                                    },
                                )
                            }
                        }
                    }

                    // LAYER 4E: Sleek Bottom Audience, Duration & Publish Bar
                    AnimatedVisibility(
                        visible = activeDraggingId == null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // Audience + Duration compact glass pills
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val isCloseFriends = uiState.privacyType == StoryPrivacyType.CloseFriends
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(
                                            if (isCloseFriends) Color(0xFF1F8B4C)
                                            else Color(0x9912141C),
                                        )
                                        .border(
                                            0.8.dp,
                                            Color.White.copy(alpha = if (isCloseFriends) 0.35f else 0.18f),
                                            RoundedCornerShape(22.dp),
                                        )
                                        .clickable {
                                            haptics.snap()
                                            if (isCloseFriends) {
                                                viewModel.setPrivacy(StoryPrivacyType.Everyone)
                                            } else {
                                                viewModel.setPrivacy(StoryPrivacyType.CloseFriends)
                                                showCloseFriendsDialog = true
                                                viewModel.loadCloseFriends()
                                            }
                                        }
                                        .padding(horizontal = 13.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        imageVector = if (isCloseFriends) Icons.Default.Star else Icons.Default.People,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = if (isCloseFriends) {
                                            val count = uiState.selectedCloseFriendIds.size
                                            if (count > 0) "دوستان نزدیک (${count.toPersianDigits()})" else "دوستان نزدیک"
                                        } else {
                                            "همه"
                                        },
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }

                                val is48h = uiState.durationType == StoryDurationType.Hours48
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(
                                            if (is48h) VistaBrandColors.Indigo.copy(alpha = 0.88f)
                                            else Color(0x9912141C),
                                        )
                                        .border(
                                            0.8.dp,
                                            Color.White.copy(alpha = if (is48h) 0.35f else 0.18f),
                                            RoundedCornerShape(22.dp),
                                        )
                                        .clickable {
                                            haptics.snap()
                                            viewModel.setDuration(
                                                if (is48h) StoryDurationType.Hours24 else StoryDurationType.Hours48,
                                            )
                                        }
                                        .padding(horizontal = 11.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (is48h) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(13.dp),
                                        )
                                    }
                                    Text(
                                        text = if (is48h) "۴۸س" else "۲۴س",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }

                            // Primary Publish CTA Button
                            Button(
                                onClick = viewModel::publishStory,
                                enabled = uiState.canPublish,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0E1018),
                                    disabledContainerColor = Color.White.copy(alpha = 0.4f),
                                    disabledContentColor = Color.Black.copy(alpha = 0.5f),
                                ),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                            ) {
                                Text(
                                    text = "انتشار استوری",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // Upload Progress Modal Overlay
                    if (uiState.isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable(enabled = false) {},
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color(0xF2151722))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
                                    .padding(horizontal = 28.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator(
                                    progress = { uiState.uploadProgress.coerceIn(0.05f, 1f) },
                                    color = VistaBrandColors.Indigo,
                                    trackColor = Color.White.copy(alpha = 0.15f),
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = uiState.uploadStatus ?: "در حال انتشار استوری…",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            // Full-Screen Text Entry Overlay (TextPaintView & EditTextOutline parity)
            if (showTextEditor) {
                StoryTextEditorOverlay(
                    initialSpec = textEditorInitialSpec,
                    referenceBitmap = loadedMediaBitmap,
                    onDone = { spec ->
                        viewModel.addOrUpdateTextElement(spec, editingTextElementId)
                        showTextEditor = false
                        editingTextElementId = null
                    },
                    onDismiss = {
                        showTextEditor = false
                        editingTextElementId = null
                    },
                )
            }

            // Freehand Drawing Studio Overlay (PaintView Brush & Neon Shaders)
            if (showDrawingEditor) {
                val currentStrokes = remember(uiState.elements) {
                    val drawingElem = uiState.elements.firstOrNull { it.type == "drawing" }
                    StoryDrawingCodec.decode(drawingElem?.content)
                }
                StoryDrawingEditorOverlay(
                    initialStrokes = currentStrokes,
                    referenceBitmap = loadedMediaBitmap,
                    onDone = { strokes ->
                        viewModel.setDrawingStrokes(strokes)
                        showDrawingEditor = false
                    },
                    onDismiss = { showDrawingEditor = false },
                )
            }

            // PILLAR 5: Telegram EmojiBottomSheet Glassmorphic Widget & Sticker Drawer
            if (showStickerSheet) {
                TelegramEmojiWidgetBottomSheet(
                    onDismiss = { showStickerSheet = false },
                    onSelectLocation = {
                        showStickerSheet = false
                        showLocationDialog = true
                    },
                    onSelectReaction = {
                        showStickerSheet = false
                        viewModel.addReactionElement("❤️")
                    },
                    onSelectLink = {
                        showStickerSheet = false
                        showLinkDialog = true
                    },
                    onSelectPhotoCutout = {
                        showStickerSheet = false
                        photoCutoutPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onSelectWeather = {
                        showStickerSheet = false
                        showWeatherDialog = true
                    },
                    onSelectMusic = {
                        showStickerSheet = false
                        showMusicPickerSheet = true
                    },
                    onSelectPoll = {
                        showStickerSheet = false
                        showPollDialog = true
                    },
                    onSelectQuestion = {
                        showStickerSheet = false
                        showQuestionDialog = true
                    },
                    onSelectMention = {
                        showStickerSheet = false
                        showMentionDialog = true
                    },
                    onSelectHashtag = {
                        showStickerSheet = false
                        showHashtagDialog = true
                    },
                    onSelectDate = {
                        showStickerSheet = false
                        val nowJalali = ZonedDateTime.now(ZoneId.of("Asia/Tehran")).toJalali()
                        val persianMonths = listOf(
                            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
                        )
                        val mName = persianMonths.getOrElse(nowJalali.month - 1) { "" }
                        val dateStr = "${nowJalali.day.toPersianDigits()} $mName ${nowJalali.year.toPersianDigits()}"
                        viewModel.addDateElement(dateStr)
                    },
                    onSelectCountdown = {
                        showStickerSheet = false
                        showCountdownDialog = true
                    },
                    onSelectGif = {
                        showStickerSheet = false
                        showGifDialog = true
                    },
                    onSelectEmojiSticker = { emoji ->
                        showStickerSheet = false
                        viewModel.addEmojiStickerElement(emoji)
                    },
                )
            }

            if (showMusicPickerSheet) {
                MusicPickerBottomSheet(
                    musicRepository = viewModel.musicRepository,
                    currentUserId = viewModel.signedInUserId,
                    onMusicSelected = { track ->
                        viewModel.addMusicElement(track)
                        showMusicPickerSheet = false
                    },
                    onRichMusicSelected = { result ->
                        viewModel.onMusicConfigured(result)
                        showMusicPickerSheet = false
                    },
                    onDismiss = { showMusicPickerSheet = false },
                )
            }

            // =================================================================================
            // UNIFIED GLASSMORPHIC WIDGET CONFIGURATION DIALOGS WITH LIVE INTERACTIVE PREVIEWS
            // =================================================================================

            if (showLocationDialog) {
                DynamicLocationPickerBottomSheet(
                    contextService = viewModel.contextService,
                    initialVariant = locationPreviewVariant,
                    onConfirm = { place, variant ->
                        locationPreviewVariant = variant
                        viewModel.addLocationElement(
                            name = place.name,
                            latitude = place.latitude,
                            longitude = place.longitude,
                            address = place.subtitle.takeIf { it.isNotBlank() },
                            variant = variant,
                        )
                        showLocationDialog = false
                    },
                    onDismiss = { showLocationDialog = false },
                )
            }

            if (showLinkDialog) {
                val safeLinkCandidate = remember(linkUrl) { StorySecurityGuard.normalizeSafeHttpUrl(linkUrl) }
                StoryStudioModalDialog(
                    title = "افزودن پیوند وب",
                    confirmLabel = "افزودن",
                    confirmEnabled = safeLinkCandidate != null,
                    onDismiss = { showLinkDialog = false },
                    onConfirm = {
                        if (viewModel.addLinkElement(
                                linkUrl,
                                linkTitle.ifBlank { null },
                                variant = linkPreviewVariant,
                            )
                        ) {
                            linkUrl = ""
                            linkTitle = ""
                            showLinkDialog = false
                        }
                    },
                ) {
                    LiveWidgetPreviewStage(
                        onCycleVariant = {
                            linkPreviewVariant = (linkPreviewVariant + 1) % 4
                            haptics.snap()
                        },
                    ) {
                        StoryPillWidget(
                            text = linkTitle.ifBlank {
                                StorySecurityGuard.extractDisplayHost(linkUrl).ifBlank { "coffevista.ir" }
                            },
                            variant = linkPreviewVariant,
                            icon = LinkIcon,
                            categoryAccent = Color(0xFF2481CC),
                        )
                    }
                    StoryStudioTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        placeholder = "https://example.com",
                    )
                    if (linkUrl.isNotBlank() && safeLinkCandidate == null) {
                        Text(
                            text = "لطفاً یک نشانی وب معتبر (http یا https) وارد کنید",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp,
                        )
                    }
                    StoryStudioTextField(
                        value = linkTitle,
                        onValueChange = { linkTitle = it },
                        placeholder = "متن جایگزین پیوند (اختیاری)",
                    )
                }
            }

            if (showPollDialog) {
                StoryStudioModalDialog(
                    title = "نظرسنجی تعاملی",
                    confirmLabel = "افزودن",
                    confirmEnabled = pollOption1.isNotBlank() && pollOption2.isNotBlank(),
                    onDismiss = { showPollDialog = false },
                    onConfirm = {
                        viewModel.addPollElement(
                            question = pollQuestion.ifBlank { "نظر شما چیست؟" },
                            option1 = pollOption1,
                            option2 = pollOption2,
                            variant = pollPreviewVariant,
                        )
                        pollQuestion = ""
                        showPollDialog = false
                    },
                ) {
                    LiveWidgetPreviewStage(
                        onCycleVariant = {
                            pollPreviewVariant = (pollPreviewVariant + 1) % 4
                            haptics.snap()
                        },
                    ) {
                        StoryPollWidget(
                            poll = StoryPoll(
                                id = "preview",
                                question = pollQuestion.ifBlank { "نظر شما چیست؟" },
                                options = listOf(
                                    StoryPollOption("1", pollOption1.ifBlank { "بله" }),
                                    StoryPollOption("2", pollOption2.ifBlank { "خیر" }),
                                ),
                            ),
                            variant = pollPreviewVariant,
                            isAuthor = false,
                            onVote = null,
                        )
                    }
                    StoryStudioTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        placeholder = "سوال نظرسنجی…",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StoryStudioTextField(
                            value = pollOption1,
                            onValueChange = { pollOption1 = it },
                            placeholder = "گزینه اول",
                            modifier = Modifier.weight(1f),
                        )
                        StoryStudioTextField(
                            value = pollOption2,
                            onValueChange = { pollOption2 = it },
                            placeholder = "گزینه دوم",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (showQuestionDialog) {
                StoryStudioModalDialog(
                    title = "باکس پرسش و پاسخ",
                    confirmLabel = "افزودن",
                    confirmEnabled = true,
                    onDismiss = { showQuestionDialog = false },
                    onConfirm = {
                        viewModel.addQuestionElement(
                            question = questionText.ifBlank { "از من بپرسید" },
                            variant = questionPreviewVariant,
                        )
                        questionText = ""
                        showQuestionDialog = false
                    },
                ) {
                    LiveWidgetPreviewStage(
                        onCycleVariant = {
                            questionPreviewVariant = (questionPreviewVariant + 1) % 4
                            haptics.snap()
                        },
                    ) {
                        StoryQuestionWidget(
                            prompt = questionText.ifBlank { "از من بپرسید" },
                            variant = questionPreviewVariant,
                            isAuthor = false,
                            onAnswerClick = null,
                            onViewAnswers = null,
                        )
                    }
                    StoryStudioTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        placeholder = "متن سوال (مثلاً: هر سوالی دارید بپرسید…)",
                    )
                }
            }

            if (showWeatherDialog) {
                DynamicWeatherPickerBottomSheet(
                    contextService = viewModel.contextService,
                    initialVariant = weatherPreviewVariant,
                    onConfirm = { city, formattedTemp, conditionEmoji, variant ->
                        weatherPreviewVariant = variant
                        viewModel.addWeatherElement(
                            city = city,
                            temperature = formattedTemp,
                            conditionEmoji = conditionEmoji,
                            variant = variant,
                        )
                        showWeatherDialog = false
                    },
                    onDismiss = { showWeatherDialog = false },
                )
            }

            if (showMentionDialog) {
                DynamicMentionPickerBottomSheet(
                    candidates = uiState.closeFriendCandidates,
                    isLoading = uiState.isLoadingCloseFriends,
                    initialVariant = mentionPreviewVariant,
                    onRefreshCandidates = viewModel::loadCloseFriends,
                    onConfirm = { chosenUsername, chosenUserId, variant ->
                        mentionPreviewVariant = variant
                        viewModel.addMentionElement(
                            username = chosenUsername,
                            userId = chosenUserId,
                            variant = variant,
                        )
                        mentionUsername = ""
                        showMentionDialog = false
                    },
                    onDismiss = { showMentionDialog = false },
                )
            }

            if (showHashtagDialog) {
                StoryStudioModalDialog(
                    title = "برچسب هشتگ",
                    confirmLabel = "افزودن",
                    confirmEnabled = hashtagText.removePrefix("#").isNotBlank(),
                    onDismiss = { showHashtagDialog = false },
                    onConfirm = {
                        viewModel.addHashtagElement(hashtagText, variant = hashtagPreviewVariant)
                        hashtagText = ""
                        showHashtagDialog = false
                    },
                ) {
                    LiveWidgetPreviewStage(
                        onCycleVariant = {
                            hashtagPreviewVariant = (hashtagPreviewVariant + 1) % 4
                            haptics.snap()
                        },
                    ) {
                        StoryPillWidget(
                            text = "#${hashtagText.removePrefix("#").ifBlank { "ویستا" }}",
                            variant = hashtagPreviewVariant,
                            categoryAccent = Color(0xFF4092FF),
                        )
                    }
                    StoryStudioTextField(
                        value = hashtagText,
                        onValueChange = { hashtagText = it },
                        placeholder = "عنوان هشتگ (بدون #)",
                    )
                }
            }

            if (showCountdownDialog) {
                StoryStudioModalDialog(
                    title = "شمارش معکوس رویداد",
                    confirmLabel = "افزودن",
                    confirmEnabled = true,
                    onDismiss = { showCountdownDialog = false },
                    onConfirm = {
                        val targetTime = System.currentTimeMillis() + (countdownHours * 3600 * 1000L)
                        viewModel.addCountdownElement(
                            title = countdownTitle.ifBlank { "رویداد ویژه" },
                            targetTimestampMs = targetTime,
                            variant = countdownPreviewVariant,
                        )
                        countdownTitle = ""
                        showCountdownDialog = false
                    },
                ) {
                    LiveWidgetPreviewStage(
                        onCycleVariant = {
                            countdownPreviewVariant = (countdownPreviewVariant + 1) % 4
                            haptics.snap()
                        },
                    ) {
                        StoryCountdownWidget(
                            title = countdownTitle.ifBlank { "رویداد ویژه" },
                            targetMs = System.currentTimeMillis() + (countdownHours * 3600 * 1000L),
                            variant = countdownPreviewVariant,
                        )
                    }
                    StoryStudioTextField(
                        value = countdownTitle,
                        onValueChange = { countdownTitle = it },
                        placeholder = "عنوان رویداد…",
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(6 to "۶ ساعت", 12 to "۱۲ ساعت", 24 to "۲۴ ساعت", 48 to "۲ روز").forEach { (hrs, label) ->
                            StudioQuickChip(
                                label = label,
                                selected = countdownHours == hrs,
                                onClick = { countdownHours = hrs },
                            )
                        }
                    }
                }
            }

            if (showGifDialog) {
                val sampleGifs = listOf(
                    "https://media.giphy.com/media/26AHONQ79FdWZhAI0/giphy.gif" to "جشن",
                    "https://media.giphy.com/media/l0MYt5jPR6QX5pnqM/giphy.gif" to "خنده",
                    "https://media.giphy.com/media/3o7TKMt1VVNkHV2PaE/giphy.gif" to "تشویق",
                )
                val safeGifCandidate = remember(gifUrlInput) { StorySecurityGuard.normalizeSafeHttpUrl(gifUrlInput) }
                StoryStudioModalDialog(
                    title = "استیکر متحرک (GIF)",
                    confirmLabel = "افزودن",
                    confirmEnabled = safeGifCandidate != null,
                    onDismiss = { showGifDialog = false },
                    onConfirm = {
                        if (viewModel.addGifElement(gifUrlInput.trim())) {
                            gifUrlInput = ""
                            showGifDialog = false
                        }
                    },
                ) {
                    StoryStudioTextField(
                        value = gifUrlInput,
                        onValueChange = { gifUrlInput = it },
                        placeholder = "آدرس تصویر متحرک (https://…)",
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sampleGifs.forEach { (url, label) ->
                            StudioQuickChip(
                                label = label,
                                selected = gifUrlInput == url,
                                onClick = { gifUrlInput = url },
                            )
                        }
                    }
                }
            }

            if (showCloseFriendsDialog) {
                StoryStudioModalDialog(
                    title = "انتخاب دوستان نزدیک",
                    confirmLabel = "ذخیره",
                    confirmEnabled = !uiState.isLoadingCloseFriends,
                    onDismiss = { showCloseFriendsDialog = false },
                    onConfirm = {
                        viewModel.saveCloseFriends()
                        showCloseFriendsDialog = false
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        when {
                            uiState.isLoadingCloseFriends -> Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = VistaBrandColors.Indigo)
                            }
                            uiState.closeFriendsError != null -> {
                                Text(
                                    text = uiState.closeFriendsError.orEmpty(),
                                    color = Color(0xFFFF5252),
                                    fontSize = 13.sp,
                                )
                                TextButton(onClick = viewModel::loadCloseFriends) {
                                    Text("تلاش دوباره", color = Color.White)
                                }
                            }
                            uiState.closeFriendCandidates.isEmpty() -> {
                                Text(
                                    text = "کاربر دنبال‌شده‌ای برای انتخاب وجود ندارد",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 13.sp,
                                )
                            }
                            else -> uiState.closeFriendCandidates.forEach { user ->
                                val selected = user.id in uiState.selectedCloseFriendIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selected) Color(0xFF1F8B4C).copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.06f),
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) Color(0xFF2ECC71) else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(14.dp),
                                        )
                                        .clickable { viewModel.toggleCloseFriend(user.id) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "@${user.username}",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                    )
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF2ECC71),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showDiscardDialog) {
                StoryStudioModalDialog(
                    title = "صرف‌نظر از ویرایش؟",
                    confirmLabel = "حذف تغییرات",
                    confirmColor = Color(0xFFE53935),
                    confirmEnabled = true,
                    onDismiss = { showDiscardDialog = false },
                    onConfirm = {
                        showDiscardDialog = false
                        onClose()
                    },
                ) {
                    Text(
                        text = "در صورت خروج، تغییرات انجام‌شده روی این استوری ذخیره نخواهد شد.",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                if (active) VistaBrandColors.Indigo.copy(alpha = 0.85f)
                else Color(0x8C101218),
            )
            .border(0.8.dp, Color.White.copy(alpha = if (active) 0.45f else 0.2f), CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun StoryStudioModalDialog(
    title: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmColor: Color = VistaBrandColors.Indigo,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF5151722))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                content()
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف", color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = confirmColor,
                            contentColor = Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.12f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f),
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(confirmLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveWidgetPreviewStage(
    onCycleVariant: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dominant = LocalStoryDominantColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        dominant.topColor.copy(alpha = 0.85f),
                        dominant.bottomColor.copy(alpha = 0.85f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
            .clickable(onClick = onCycleVariant)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
        Spacer(Modifier.height(8.dp))
        Text(
            text = "برای تغییر تم شیشه‌ای روی پیش‌نمایش بزنید",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun StoryStudioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.07f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            focusedBorderColor = VistaBrandColors.Indigo,
            unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = VistaBrandColors.Indigo,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun StudioQuickChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) VistaBrandColors.Indigo else Color.White.copy(alpha = 0.08f))
            .border(
                0.8.dp,
                if (selected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.14f),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// =============================================================================================
// PILLAR 5: TELEGRAM EMOJIBOTTOMSHEET GLASSMORPHIC BENTO WIDGET & STICKER DRAWER
// =============================================================================================

private data class StoryWidgetCardSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val hero: Boolean,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelegramEmojiWidgetBottomSheet(
    onDismiss: () -> Unit,
    onSelectLocation: () -> Unit,
    onSelectReaction: () -> Unit,
    onSelectLink: () -> Unit,
    onSelectPhotoCutout: () -> Unit,
    onSelectWeather: () -> Unit,
    onSelectMusic: () -> Unit,
    onSelectPoll: () -> Unit,
    onSelectQuestion: () -> Unit,
    onSelectMention: () -> Unit,
    onSelectHashtag: () -> Unit,
    onSelectDate: () -> Unit,
    onSelectCountdown: () -> Unit,
    onSelectGif: () -> Unit,
    onSelectEmojiSticker: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = همه, 1 = ویجت‌ها, 2 = ایموجی

    val widgetCards = remember {
        listOf(
            StoryWidgetCardSpec("موقعیت مکانی", "تشخیص زنده GPS", Icons.Default.AddLocation, Color(0xFFFF4B55), true, onSelectLocation),
            StoryWidgetCardSpec("واکنش سریع", "قلب و ایموجی", Icons.Default.Favorite, Color(0xFFFF2D55), true, onSelectReaction),
            StoryWidgetCardSpec("پیوند وب", "لینک قابل کلیک", Icons.Default.Link, Color(0xFF4092FF), true, onSelectLink),
            StoryWidgetCardSpec("عکس روی عکس", "قاب دایره، ستاره، قلب", Icons.Default.AddPhotoAlternate, Color(0xFF00E676), true, onSelectPhotoCutout),
            StoryWidgetCardSpec("آب‌وهوا", "دمای زنده هر شهر", Icons.Default.WbSunny, Color(0xFFFFB300), true, onSelectWeather),
            StoryWidgetCardSpec("موزیک", "از گوشی یا ویترین", Icons.Default.MusicNote, VistaBrandColors.Pink, true, onSelectMusic),
            StoryWidgetCardSpec("نظرسنجی", "رای‌گیری تعاملی", Icons.Default.Poll, VistaBrandColors.Indigo, true, onSelectPoll),
            StoryWidgetCardSpec("پرسش و پاسخ", "دریافت نظر مخاطب", Icons.Default.ChatBubbleOutline, VistaBrandColors.Violet, true, onSelectQuestion),
            StoryWidgetCardSpec("@منشن", "اشاره", Icons.Default.AlternateEmail, Color(0xFFAB47BC), false, onSelectMention),
            StoryWidgetCardSpec("#هشتگ", "موضوع", Icons.Default.Tag, Color(0xFF29B6F6), false, onSelectHashtag),
            StoryWidgetCardSpec("تاریخ شمسی", "امروز", Icons.Default.CalendarMonth, Color(0xFF26A69A), false, onSelectDate),
            StoryWidgetCardSpec("شمارش معکوس", "تایمر", Icons.Default.AccessTime, Color(0xFFFF7043), false, onSelectCountdown),
            StoryWidgetCardSpec("گیف متحرک", "GIF", Icons.Default.Animation, Color(0xFFFFCA28), false, onSelectGif),
        )
    }

    val filteredWidgets = remember(searchQuery, widgetCards) {
        if (searchQuery.isBlank()) widgetCards
        else widgetCards.filter {
            it.title.contains(searchQuery.trim(), ignoreCase = true) ||
                it.subtitle.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    val heroWidgets = remember(filteredWidgets) { filteredWidgets.filter { it.hero } }
    val compactWidgets = remember(filteredWidgets) { filteredWidgets.filterNot { it.hero } }

    val emojiStickers = remember {
        listOf(
            "🔥", "❤️", "😍", "😂", "🥳", "✨",
            "🎉", "💯", "🚀", "🌟", "👏", "🙌",
            "💎", "👑", "⚡", "🌈", "😎", "🥹",
            "🤍", "💜", "🌹", "☕", "🎵", "📸",
            "🦋", "🌙", "🍀", "🎯", "🏆", "🫶",
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF5131520),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "جستجوی ویجت تلگرامی یا استیکر…",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.48f),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.65f),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                        focusedBorderColor = VistaBrandColors.Indigo,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(0 to "همه", 1 to "ویجت‌های تعاملی", 2 to "ایموجی‌ها").forEach { (idx, label) ->
                        StudioQuickChip(
                            label = label,
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Virtualized 6-Column Bento Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (selectedTab == 0 || selectedTab == 1) {
                        // 2-Column Bento Hero Cards (each spans 3 of 6 columns)
                        items(
                            items = heroWidgets,
                            key = { "hero_${it.title}" },
                            span = { GridItemSpan(3) },
                        ) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                item.accent.copy(alpha = 0.22f),
                                                Color.White.copy(alpha = 0.06f),
                                            ),
                                        ),
                                    )
                                    .border(1.dp, item.accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                    .clickable(onClick = item.onClick)
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(item.accent.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = item.accent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = item.subtitle,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        // Compact Widget Pills (each spans 2 of 6 columns -> 3 per row)
                        items(
                            items = compactWidgets,
                            key = { "compact_${it.title}" },
                            span = { GridItemSpan(2) },
                        ) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                                    .clickable(onClick = item.onClick)
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    if (selectedTab == 0 || selectedTab == 2) {
                        item(key = "emoji_header", span = { GridItemSpan(6) }) {
                            Text(
                                text = "ایموجی‌ها و واکنش‌ها",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                        }

                        items(
                            items = emojiStickers,
                            key = { "emoji_$it" },
                            span = { GridItemSpan(1) },
                        ) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .clickable { onSelectEmojiSticker(emoji) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = emoji, fontSize = 26.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
