package ir.coffevista.vista_native.features.stories.ui.editor

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
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
import ir.coffevista.vista_native.features.stories.domain.StoryDurationType
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import ir.coffevista.vista_native.features.stories.ui.canvas.SnapGuides
import ir.coffevista.vista_native.features.stories.ui.canvas.SnapState
import ir.coffevista.vista_native.features.stories.ui.canvas.StageSize
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
import ir.coffevista.vista_native.features.stories.ui.render.StoryDrawingCodec
import ir.coffevista.vista_native.features.stories.ui.render.StoryElementLayers
import ir.coffevista.vista_native.features.stories.ui.render.StoryStroke
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

    var showTextEditor by remember { mutableStateOf(false) }
    var editingTextElementId by remember { mutableStateOf<String?>(null) }
    var textEditorInitialSpec by remember { mutableStateOf(StoryTextSpec("")) }

    var showDrawingEditor by remember { mutableStateOf(false) }

    var showStickerSheet by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("بله") }
    var pollOption2 by remember { mutableStateOf("خیر") }

    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("") }
    var linkTitle by remember { mutableStateOf("") }

    var showMentionDialog by remember { mutableStateOf(false) }
    var mentionUsername by remember { mutableStateOf("") }

    var showLocationDialog by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf("") }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var questionText by remember { mutableStateOf("") }
    var showCountdownDialog by remember { mutableStateOf(false) }
    var countdownTitle by remember { mutableStateOf("") }
    var countdownHours by remember { mutableIntStateOf(24) }
    var showWeatherDialog by remember { mutableStateOf(false) }
    var weatherCity by remember { mutableStateOf("تهران") }
    var weatherTemp by remember { mutableStateOf("۲۴°C") }
    var showHashtagDialog by remember { mutableStateOf(false) }
    var hashtagText by remember { mutableStateOf("") }
    var showGifDialog by remember { mutableStateOf(false) }
    var gifUrlInput by remember { mutableStateOf("") }

    var showMusicPickerSheet by remember { mutableStateOf(false) }
    var showCloseFriendsDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = uiState.mediaUri != null || uiState.elements.isNotEmpty()

    val requestClose: () -> Unit = {
        if (hasUnsavedChanges) showDiscardDialog = true else onClose()
    }

    BackHandler(enabled = hasUnsavedChanges) {
        requestClose()
    }

    val context = LocalContext.current

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

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    var tempCameraStoryUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success && tempCameraStoryUri != null) {
            viewModel.onMediaSelected(tempCameraStoryUri!!, isVideo = false)
        }
    }

    fun launchCamera() {
        val cacheDir = java.io.File(context.cacheDir, "camera").apply { mkdirs() }
        val file = java.io.File(cacheDir, "story_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file,
        )
        tempCameraStoryUri = uri
        cameraLauncher.launch(uri)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onMediaSelected(uri, isVideo = false)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.onMediaSelected(uri, isVideo = true)
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
    var snapGuides by remember { mutableStateOf(SnapGuides.None) }
    var snapState by remember { mutableStateOf(SnapState.None) }

    val transientTransforms = remember { mutableStateMapOf<String, StoryTransform>() }
    val elementSizes = remember { mutableStateMapOf<String, IntSize>() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Black,
            modifier = modifier.fillMaxSize(),
        ) { padding ->
            if (uiState.mediaUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp),
                    ) {
                        Text(
                            text = "ایجاد استوری جدید",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "یک عکس یا ویدیو برای استوری خود انتخاب کنید",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(42.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Card(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                modifier = Modifier.weight(1f).height(156.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "عکس از گالری",
                                        tint = VistaBrandColors.Indigo,
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("عکس از گالری", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            Card(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                                    )
                                },
                                modifier = Modifier.weight(1f).height(156.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "ویدیو از گالری",
                                        tint = VistaBrandColors.Pink,
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("ویدیو از گالری", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        TextButton(onClick = ::launchCamera) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ثبت عکس با دوربین", color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = onClose) {
                            Text("انصراف و بازگشت", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    StoryStage(modifier = Modifier.fillMaxSize()) { stage ->
                        if (uiState.isVideo && exoPlayer != null) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
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
                                contentScale = ContentScale.Crop,
                            )
                        }

                        StoryElementLayers(
                            elements = uiState.elements,
                            stage = stage,
                            visualFor = { el ->
                                val active = transientTransforms[el.id]
                                val transform = active ?: el.resolvedTransform()
                                val isBeingDraggedToTrash = el.id == activeDraggingId && isHoveredTrash
                                ElementVisualState(
                                    transform = { transform },
                                    extraScale = { if (isBeingDraggedToTrash) 0.65f else 1f },
                                    alpha = { if (isBeingDraggedToTrash) 0.5f else 1f },
                                )
                            },
                            onSizeChanged = { id, size ->
                                elementSizes[id] = size
                            },
                        )

                        StorySnapGuidesOverlay(guides = snapGuides)

                        val snapEngine = remember(stage) { StorySnapEngine(stage) }
                        val trashZone = remember(stage) { StoryTrashZone(stage) }

                        val gestureCallbacks = remember(uiState.elements, stage) {
                            object : StoryGestureCallbacks {
                                override val selectedId: String? get() = selectedElementId

                                override fun hitTest(x: Float, y: Float): String? {
                                    val nonDrawing = uiState.elements.filterNot { it.type == "drawing" }.reversed()
                                    for (el in nonDrawing) {
                                        val t = transientTransforms[el.id] ?: el.resolvedTransform()
                                        val size = elementSizes[el.id]
                                        val halfW = (size?.width?.toFloat() ?: 120f) / 2f
                                        val halfH = (size?.height?.toFloat() ?: 60f) / 2f
                                        if (StoryHitTest.contains(t, stage, halfW, halfH, x, y, minTouchHalfPx = 36f)) {
                                            return el.id
                                        }
                                    }
                                    return null
                                }

                                override fun onGestureStart(id: String) {
                                    selectedElementId = id
                                    activeDraggingId = id
                                    viewModel.bringToFront(id)
                                    val el = uiState.elements.firstOrNull { it.id == id }
                                    if (el != null) {
                                        transientTransforms[id] = el.resolvedTransform()
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

                                    val size = elementSizes[id]
                                    val halfW = (size?.width?.toFloat() ?: 120f) / 2f
                                    val halfH = (size?.height?.toFloat() ?: 60f) / 2f
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

                                    if (isHoveredTrash && !cancelled) {
                                        haptics.deleted()
                                        viewModel.removeElement(id)
                                        if (selectedElementId == id) selectedElementId = null
                                    } else if (finalTransform != null && !cancelled) {
                                        val size = elementSizes[id]
                                        val halfW = (size?.width?.toFloat() ?: 120f) / 2f
                                        val halfH = (size?.height?.toFloat() ?: 60f) / 2f
                                        val resting = StoryTransformMath.restingTransform(
                                            t = finalTransform,
                                            halfExtentsUnrotatedPx = halfW to halfH,
                                            stage = stage,
                                        )
                                        viewModel.updateElementTransform(id, resting)
                                    }

                                    activeDraggingId = null
                                    isHoveredTrash = false
                                }

                                override fun onTap(x: Float, y: Float, hitId: String?) {
                                    if (hitId != null) {
                                        selectedElementId = hitId
                                        val el = uiState.elements.firstOrNull { it.id == hitId }
                                        if (el != null) {
                                            if (el.type == "text") {
                                                editingTextElementId = el.id
                                                textEditorInitialSpec = el.toTextSpec()
                                                showTextEditor = true
                                            } else {
                                                viewModel.cycleWidgetVariant(el.id)
                                                haptics.boundary()
                                            }
                                        }
                                    } else {
                                        selectedElementId = null
                                        editingTextElementId = null
                                        textEditorInitialSpec = StoryTextSpec("")
                                        showTextEditor = true
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(stage, uiState.elements) {
                                    detectStoryGestures { gestureCallbacks }
                                },
                        )
                    }

                    StoryTrashDropZone(
                        isVisible = activeDraggingId != null,
                        isHovered = isHoveredTrash,
                    )

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
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            IconButton(
                                onClick = requestClose,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f)),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                IconButton(
                                    onClick = {
                                        editingTextElementId = null
                                        textEditorInitialSpec = StoryTextSpec("")
                                        showTextEditor = true
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                ) {
                                    Icon(Icons.Default.TextFields, contentDescription = "متن", tint = Color.White)
                                }

                                IconButton(
                                    onClick = { showDrawingEditor = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                ) {
                                    Icon(Icons.Default.Brush, contentDescription = "نقاشی", tint = Color.White)
                                }

                                IconButton(
                                    onClick = { showStickerSheet = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                ) {
                                    Icon(Icons.Default.Poll, contentDescription = "استیکرها", tint = Color.White)
                                }

                                FilterChip(
                                    selected = uiState.durationType == StoryDurationType.Hours48,
                                    onClick = {
                                        viewModel.setDuration(
                                            if (uiState.durationType == StoryDurationType.Hours24) StoryDurationType.Hours48
                                            else StoryDurationType.Hours24,
                                        )
                                    },
                                    label = {
                                        Text(
                                            if (uiState.durationType == StoryDurationType.Hours48) "۴۸ ساعت (طلایی)" else "۲۴ ساعت",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color.Black.copy(alpha = 0.55f),
                                        selectedContainerColor = VistaBrandColors.Indigo,
                                    ),
                                )
                            }
                        }
                    }

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
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            FilterChip(
                                selected = uiState.privacyType == StoryPrivacyType.CloseFriends,
                                onClick = {
                                    if (uiState.privacyType == StoryPrivacyType.CloseFriends) {
                                        viewModel.setPrivacy(StoryPrivacyType.Everyone)
                                    } else {
                                        viewModel.setPrivacy(StoryPrivacyType.CloseFriends)
                                        showCloseFriendsDialog = true
                                        viewModel.loadCloseFriends()
                                    }
                                },
                                label = {
                                    Text(
                                        if (uiState.privacyType == StoryPrivacyType.CloseFriends) "دوستان صمیمی" else "همه کاربران",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.Black.copy(alpha = 0.6f),
                                    selectedContainerColor = Color(0xFF2E7D32),
                                ),
                            )

                            Button(
                                onClick = viewModel::publishStory,
                                enabled = uiState.canPublish,
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VistaBrandColors.Indigo,
                                    contentColor = Color.White,
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ارسال به استوری", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    if (uiState.isUploading) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = uiState.uploadStatus ?: "در حال انتشار استوری…",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            if (showTextEditor) {
                StoryTextEditorOverlay(
                    initialSpec = textEditorInitialSpec,
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

            if (showDrawingEditor) {
                val currentStrokes = remember(uiState.elements) {
                    val drawingElem = uiState.elements.firstOrNull { it.type == "drawing" }
                    StoryDrawingCodec.decode(drawingElem?.content)
                }
                StoryDrawingEditorOverlay(
                    initialStrokes = currentStrokes,
                    onDone = { strokes ->
                        viewModel.setDrawingStrokes(strokes)
                        showDrawingEditor = false
                    },
                    onDismiss = { showDrawingEditor = false },
                )
            }

            if (showStickerSheet) {
                ModalBottomSheet(onDismissRequest = { showStickerSheet = false }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(20.dp),
                    ) {
                        Text("انتخاب استیکر تعاملی", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            StickerOption(icon = Icons.Default.MusicNote, label = "موزیک") {
                                showStickerSheet = false
                                showMusicPickerSheet = true
                            }
                            StickerOption(icon = Icons.Default.Poll, label = "نظرسنجی") {
                                showStickerSheet = false
                                showPollDialog = true
                            }
                            StickerOption(icon = Icons.Default.ChatBubbleOutline, label = "سوال") {
                                showStickerSheet = false
                                showQuestionDialog = true
                            }
                            StickerOption(icon = Icons.Default.Link, label = "لینک") {
                                showStickerSheet = false
                                showLinkDialog = true
                            }
                            StickerOption(icon = Icons.Default.AlternateEmail, label = "منشن") {
                                showStickerSheet = false
                                showMentionDialog = true
                            }
                            StickerOption(icon = Icons.Default.AddLocation, label = "مکان") {
                                showStickerSheet = false
                                showLocationDialog = true
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            StickerOption(icon = Icons.Default.AccessTime, label = "شمارش") {
                                showStickerSheet = false
                                showCountdownDialog = true
                            }
                            StickerOption(icon = Icons.Default.WbSunny, label = "آب‌وهوا") {
                                showStickerSheet = false
                                showWeatherDialog = true
                            }
                            StickerOption(icon = Icons.Default.CalendarMonth, label = "تاریخ") {
                                showStickerSheet = false
                                val nowJalali = ZonedDateTime.now(ZoneId.of("Asia/Tehran")).toJalali()
                                val persianMonths = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
                                val mName = persianMonths.getOrElse(nowJalali.month - 1) { "" }
                                val dateStr = "${nowJalali.day.toPersianDigits()} $mName ${nowJalali.year.toPersianDigits()}"
                                viewModel.addDateElement(dateStr)
                            }
                            StickerOption(icon = Icons.Default.Tag, label = "هشتگ") {
                                showStickerSheet = false
                                showHashtagDialog = true
                            }
                            StickerOption(icon = Icons.Default.Animation, label = "گیف") {
                                showStickerSheet = false
                                showGifDialog = true
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            if (showQuestionDialog) {
                AlertDialog(
                    onDismissRequest = { showQuestionDialog = false },
                    title = { Text("افزودن جعبه سوال") },
                    text = {
                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            placeholder = { Text("متن سوال (مثلاً: از من بپرسید…)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.addQuestionElement(questionText.ifBlank { "از من بپرسید" })
                                questionText = ""
                                showQuestionDialog = false
                            },
                        ) {
                            Text("تایید")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showQuestionDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showCountdownDialog) {
                AlertDialog(
                    onDismissRequest = { showCountdownDialog = false },
                    title = { Text("افزودن شمارش معکوس") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = countdownTitle,
                                onValueChange = { countdownTitle = it },
                                placeholder = { Text("عنوان رویداد (مثلاً: رویداد ویژه، شروع تخفیف…)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text("مدت زمان شمارش:", fontSize = 12.sp, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                listOf(6 to "۶ ساعت", 12 to "۱۲ ساعت", 24 to "۲۴ ساعت", 48 to "۲ روز").forEach { (hrs, label) ->
                                    FilterChip(
                                        selected = countdownHours == hrs,
                                        onClick = { countdownHours = hrs },
                                        label = { Text(label, fontSize = 11.sp) },
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val targetTime = System.currentTimeMillis() + (countdownHours * 3600 * 1000L)
                                viewModel.addCountdownElement(
                                    title = countdownTitle.ifBlank { "شمارش معکوس" },
                                    targetTimestampMs = targetTime,
                                )
                                countdownTitle = ""
                                showCountdownDialog = false
                            },
                        ) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCountdownDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showWeatherDialog) {
                AlertDialog(
                    onDismissRequest = { showWeatherDialog = false },
                    title = { Text("افزودن استیکر آب و هوا") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = weatherCity,
                                onValueChange = { weatherCity = it },
                                placeholder = { Text("نام شهر (مثلاً: تهران، شیراز، اصفهان)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = weatherTemp,
                                onValueChange = { weatherTemp = it },
                                placeholder = { Text("دمای هوا (مثلاً: ۲۴°C)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.addWeatherElement(
                                    city = weatherCity.ifBlank { "تهران" },
                                    temperature = weatherTemp.ifBlank { "۲۴°C" },
                                )
                                showWeatherDialog = false
                            },
                        ) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWeatherDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showHashtagDialog) {
                AlertDialog(
                    onDismissRequest = { showHashtagDialog = false },
                    title = { Text("افزودن برچسب هشتگ") },
                    text = {
                        OutlinedTextField(
                            value = hashtagText,
                            onValueChange = { hashtagText = it },
                            placeholder = { Text("متن هشتگ (بدون #)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val cleanTag = hashtagText.removePrefix("#").trim()
                                if (cleanTag.isNotBlank()) {
                                    viewModel.addHashtagElement(cleanTag)
                                    hashtagText = ""
                                    showHashtagDialog = false
                                }
                            },
                        ) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showHashtagDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showGifDialog) {
                val sampleGifs = listOf(
                    "https://media.giphy.com/media/26AHONQ79FdWZhAI0/giphy.gif" to "جشن",
                    "https://media.giphy.com/media/l0MYt5jPR6QX5pnqM/giphy.gif" to "خنده",
                    "https://media.giphy.com/media/3o7TKMt1VVNkHV2PaE/giphy.gif" to "تشویق",
                )
                AlertDialog(
                    onDismissRequest = { showGifDialog = false },
                    title = { Text("افزودن استیکر متحرک (GIF)") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = gifUrlInput,
                                onValueChange = { gifUrlInput = it },
                                placeholder = { Text("آدرس تصویر متحرک (https://…)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text("استیکرهای پیشنهادی:", fontSize = 12.sp, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                sampleGifs.forEach { (url, label) ->
                                    FilterChip(
                                        selected = gifUrlInput == url,
                                        onClick = { gifUrlInput = url },
                                        label = { Text(label, fontSize = 11.sp) },
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (gifUrlInput.isNotBlank()) {
                                    viewModel.addGifElement(gifUrlInput.trim())
                                    gifUrlInput = ""
                                    showGifDialog = false
                                }
                            },
                        ) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showGifDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showMusicPickerSheet) {
                MusicPickerBottomSheet(
                    musicRepository = viewModel.musicRepository,
                    onMusicSelected = { track ->
                        viewModel.addMusicElement(track)
                        showMusicPickerSheet = false
                    },
                    onDismiss = { showMusicPickerSheet = false },
                )
            }

            if (showPollDialog) {
                AlertDialog(
                    onDismissRequest = { showPollDialog = false },
                    title = { Text("ایجاد نظرسنجی") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pollQuestion,
                                onValueChange = { pollQuestion = it },
                                placeholder = { Text("سوال نظرسنجی…") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = pollOption1,
                                onValueChange = { pollOption1 = it },
                                placeholder = { Text("گزینه اول") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = pollOption2,
                                onValueChange = { pollOption2 = it },
                                placeholder = { Text("گزینه دوم") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addPollElement(pollQuestion, pollOption1, pollOption2)
                            pollQuestion = ""
                            showPollDialog = false
                        }) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPollDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showLinkDialog) {
                AlertDialog(
                    onDismissRequest = { showLinkDialog = false },
                    title = { Text("افزودن پیوند اینترنتی") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = linkUrl,
                                onValueChange = { linkUrl = it },
                                placeholder = { Text("https://example.com") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = linkTitle,
                                onValueChange = { linkTitle = it },
                                placeholder = { Text("عنوان نمایشی (اختیاری)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addLinkElement(linkUrl, linkTitle.ifBlank { null })
                            linkUrl = ""
                            linkTitle = ""
                            showLinkDialog = false
                        }) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLinkDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showMentionDialog) {
                AlertDialog(
                    onDismissRequest = { showMentionDialog = false },
                    title = { Text("منشن کاربر") },
                    text = {
                        OutlinedTextField(
                            value = mentionUsername,
                            onValueChange = { mentionUsername = it },
                            placeholder = { Text("نام کاربری (بدون @)") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addMentionElement(mentionUsername)
                            mentionUsername = ""
                            showMentionDialog = false
                        }) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMentionDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showLocationDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationDialog = false },
                    title = { Text("افزودن برچسب مکان") },
                    text = {
                        OutlinedTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            placeholder = { Text("نام مکان یا شهر…") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addLocationElement(locationName)
                            locationName = ""
                            showLocationDialog = false
                        }) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLocationDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showCloseFriendsDialog) {
                AlertDialog(
                    onDismissRequest = { showCloseFriendsDialog = false },
                    title = { Text("انتخاب دوستان نزدیک") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            when {
                                uiState.isLoadingCloseFriends -> Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) { CircularProgressIndicator() }
                                uiState.closeFriendsError != null -> {
                                    Text(uiState.closeFriendsError.orEmpty(), color = MaterialTheme.colorScheme.error)
                                    TextButton(onClick = viewModel::loadCloseFriends) { Text("تلاش دوباره") }
                                }
                                uiState.closeFriendCandidates.isEmpty() -> {
                                    Text("کاربر دنبال‌شده‌ای برای انتخاب وجود ندارد")
                                }
                                else -> uiState.closeFriendCandidates.forEach { user ->
                                    FilterChip(
                                        selected = user.id in uiState.selectedCloseFriendIds,
                                        onClick = { viewModel.toggleCloseFriend(user.id) },
                                        label = {
                                            Text(
                                                text = "@${user.username}",
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.saveCloseFriends()
                                showCloseFriendsDialog = false
                            },
                            enabled = !uiState.isLoadingCloseFriends,
                        ) { Text("ذخیره") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCloseFriendsDialog = false }) { Text("انصراف") }
                    },
                )
            }

            if (showDiscardDialog) {
                AlertDialog(
                    onDismissRequest = { showDiscardDialog = false },
                    containerColor = Color(0xFF212121),
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(
                            text = "تغییرات ذخیره نشده",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    text = {
                        Text(
                            text = "آیا از انصراف مطمئن هستید؟ تغییرات شما از بین خواهد رفت.",
                            color = Color(0xFFBDBDBD),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDiscardDialog = false
                                onClose()
                            },
                        ) {
                            Text(
                                text = "انصراف",
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDiscardDialog = false },
                        ) {
                            Text(
                                text = "ادامه ویرایش",
                                color = Color.White,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun StickerOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp),
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
