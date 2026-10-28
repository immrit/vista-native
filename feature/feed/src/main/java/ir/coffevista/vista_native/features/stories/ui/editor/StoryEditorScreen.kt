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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import ir.coffevista.vista_native.core.common.toJalali
import ir.coffevista.vista_native.core.common.toPersianDigits
import java.time.ZoneId
import java.time.ZonedDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.domain.StoryDurationType
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import ir.coffevista.vista_native.features.stories.ui.player.StoryElementsOverlay
import kotlin.math.roundToInt

private data class EditorDrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
)

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

    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    var isDrawingMode by remember { mutableStateOf(false) }
    var currentStrokes by remember { mutableStateOf(listOf<EditorDrawingStroke>()) }
    var activeStrokePoints by remember { mutableStateOf(listOf<Offset>()) }
    var selectedBrushColor by remember { mutableStateOf(Color.White) }
    var selectedBrushWidth by remember { mutableFloatStateOf(8f) }

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
    val hasUnsavedChanges = uiState.mediaUri != null || uiState.elements.isNotEmpty() || currentStrokes.isNotEmpty()

    val requestClose: () -> Unit = {
        if (hasUnsavedChanges) showDiscardDialog = true else onClose()
    }

    BackHandler(enabled = hasUnsavedChanges) {
        requestClose()
    }

    val context = androidx.compose.ui.platform.LocalContext.current

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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Black,
            modifier = modifier.fillMaxSize(),
        ) { padding ->
            if (uiState.mediaUri == null) {
                // Media Picker Selection View
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
                            // In RTL this first card renders on the right, matching Flutter's image action.
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

                            // In RTL this second card renders on the left, matching Flutter's video action.
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

                        // Native-only improvement: direct camera capture without changing
                        // the two primary Flutter-equivalent gallery actions.
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
                // Active Story Editor View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Media background: Video Player or Image
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

                    // Interactive Elements
                    StoryElementsOverlay(
                        elements = uiState.elements,
                        onPollVote = { _, _ -> },
                        onQuestionAnswer = { _, _ -> },
                        onLinkClick = {},
                        onMentionClick = {},
                    )

                    // Draggable & Deletable Handles for Elements in Editor mode
                    uiState.elements.forEach { element ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset { IntOffset((element.x * 200).roundToInt(), (element.y * 300).roundToInt()) }
                                .pointerInput(element.id) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        viewModel.updateElementPosition(
                                            element.id,
                                            dragAmount.x / 200f,
                                            dragAmount.y / 300f,
                                        )
                                    }
                                },
                        ) {
                            IconButton(
                                onClick = { viewModel.removeElement(element.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = 0.85f)),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "حذف استیکر",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }

                    // Freehand Drawing Canvas & Floating Toolbar
                    if (isDrawingMode) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                            val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(selectedBrushColor, selectedBrushWidth) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                activeStrokePoints = listOf(offset)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                activeStrokePoints = activeStrokePoints + change.position
                                            },
                                            onDragEnd = {
                                                if (activeStrokePoints.isNotEmpty()) {
                                                    currentStrokes = currentStrokes + EditorDrawingStroke(
                                                        points = activeStrokePoints,
                                                        color = selectedBrushColor,
                                                        strokeWidth = selectedBrushWidth,
                                                    )
                                                    activeStrokePoints = emptyList()
                                                }
                                            },
                                            onDragCancel = {
                                                activeStrokePoints = emptyList()
                                            },
                                        )
                                    },
                            ) {
                                currentStrokes.forEach { stroke ->
                                    if (stroke.points.size > 1) {
                                        for (i in 0 until stroke.points.size - 1) {
                                            drawLine(
                                                color = stroke.color,
                                                start = stroke.points[i],
                                                end = stroke.points[i + 1],
                                                strokeWidth = stroke.strokeWidth,
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                            )
                                        }
                                    } else if (stroke.points.size == 1) {
                                        drawCircle(
                                            color = stroke.color,
                                            radius = stroke.strokeWidth / 2f,
                                            center = stroke.points[0],
                                        )
                                    }
                                }
                                if (activeStrokePoints.size > 1) {
                                    for (i in 0 until activeStrokePoints.size - 1) {
                                        drawLine(
                                            color = selectedBrushColor,
                                            start = activeStrokePoints[i],
                                            end = activeStrokePoints[i + 1],
                                            strokeWidth = selectedBrushWidth,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        )
                                    }
                                } else if (activeStrokePoints.size == 1) {
                                    drawCircle(
                                        color = selectedBrushColor,
                                        radius = selectedBrushWidth / 2f,
                                        center = activeStrokePoints[0],
                                    )
                                }
                            }

                            // Floating Drawing Control Bar at Bottom
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.88f))
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(4f to "نازک", 10f to "متوسط", 22f to "ضخیم").forEach { (width, label) ->
                                            FilterChip(
                                                selected = selectedBrushWidth == width,
                                                onClick = { selectedBrushWidth = width },
                                                label = { Text(label, color = Color.White, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    containerColor = Color.Transparent,
                                                    selectedContainerColor = VistaBrandColors.Indigo,
                                                ),
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (currentStrokes.isNotEmpty()) {
                                                    currentStrokes = currentStrokes.dropLast(1)
                                                }
                                            },
                                            enabled = currentStrokes.isNotEmpty(),
                                        ) {
                                            Icon(Icons.Default.Undo, contentDescription = "بازگشت", tint = Color.White)
                                        }
                                        IconButton(
                                            onClick = { currentStrokes = emptyList() },
                                            enabled = currentStrokes.isNotEmpty(),
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "پاک کردن", tint = Color.White)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val paletteColors = listOf(
                                    Color.White,
                                    Color.Black,
                                    Color.Red,
                                    Color(0xFFFF9800),
                                    Color.Yellow,
                                    Color.Green,
                                    Color.Cyan,
                                    Color.Magenta,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    paletteColors.forEach { c ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .clickable { selectedBrushColor = c }
                                                .then(
                                                    if (selectedBrushColor == c) {
                                                        Modifier.border(2.dp, Color.White, CircleShape)
                                                    } else Modifier,
                                                ),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    TextButton(onClick = {
                                        currentStrokes = emptyList()
                                        isDrawingMode = false
                                    }) {
                                        Text("انصراف", color = Color.LightGray)
                                    }

                                    Button(
                                        onClick = {
                                            if (currentStrokes.isNotEmpty()) {
                                                val serialized = currentStrokes.joinToString("|") { stroke ->
                                                    val hex = "#%08X".format(stroke.color.value.toLong())
                                                    val pts = stroke.points.joinToString(";") { p ->
                                                        "%.4f,%.4f".format(p.x / widthPx, p.y / heightPx)
                                                    }
                                                    "$hex:${stroke.strokeWidth}:$pts"
                                                }
                                                viewModel.addDrawingElement(serialized)
                                                currentStrokes = emptyList()
                                            }
                                            isDrawingMode = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Indigo),
                                    ) {
                                        Text("تایید نقاشی", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Top Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(
                            onClick = {
                                if (hasUnsavedChanges) {
                                    showDiscardDialog = true
                                } else {
                                    onClose()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Text tool button
                            IconButton(
                                onClick = { showTextDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                            ) {
                                Icon(Icons.Default.TextFields, contentDescription = "متن", tint = Color.White)
                            }

                            // Drawing tool button
                            IconButton(
                                onClick = { isDrawingMode = !isDrawingMode },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isDrawingMode) VistaBrandColors.Indigo else Color.Black.copy(alpha = 0.5f)),
                            ) {
                                Icon(Icons.Default.Brush, contentDescription = "نقاشی", tint = Color.White)
                            }

                            // Sticker tool button
                            IconButton(
                                onClick = { showStickerSheet = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                            ) {
                                Icon(Icons.Default.Poll, contentDescription = "استیکرها", tint = Color.White)
                            }

                            // 24h / 48h Duration Chip
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
                                    containerColor = Color.Black.copy(alpha = 0.5f),
                                    selectedContainerColor = VistaBrandColors.Indigo,
                                ),
                            )
                        }
                    }

                    // Upload Progress Indicator
                    if (uiState.isUploading) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
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

                    // Bottom Action Bar (Privacy & Publish Button)
                    if (!isDrawingMode) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // Privacy Selector
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

                            // Publish Button
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
                }
            }

            // Text Tool Dialog
            if (showTextDialog) {
                AlertDialog(
                    onDismissRequest = { showTextDialog = false },
                    title = { Text("افزودن متن به استوری") },
                    text = {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("متن خود را بنویسید…") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addTextElement(textInput)
                            textInput = ""
                            showTextDialog = false
                        }) {
                            Text("تایید")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTextDialog = false }) { Text("انصراف") }
                    },
                )
            }

            // Sticker Selector Bottom Sheet
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

            // Question Sticker Dialog
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

            // Countdown Sticker Dialog
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

            // Weather Sticker Dialog
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

            // Hashtag Sticker Dialog
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

            // GIF Sticker Dialog
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

            // Music Picker Bottom Sheet
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

            // Poll Dialog
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

            // Link Dialog
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

            // Mention Dialog
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

            // Location Dialog
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

            // Discard Confirmation Dialog
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
