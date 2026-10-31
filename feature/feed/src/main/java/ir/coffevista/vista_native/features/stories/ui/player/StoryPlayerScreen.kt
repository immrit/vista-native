package ir.coffevista.vista_native.features.stories.ui.player

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.core.designsystem.component.VistaReportReasonDialog
import ir.coffevista.vista_native.features.stories.data.StoryRepository
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Add
import ir.coffevista.vista_native.features.stories.domain.HighlightDto
import ir.coffevista.vista_native.features.stories.domain.StoryMediaType
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import ir.coffevista.vista_native.features.stories.domain.StoryQuestionAnswerDto
import ir.coffevista.vista_native.features.stories.domain.StoryUser
import ir.coffevista.vista_native.features.stories.domain.StoryVerificationType
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StoryPlayerScreen(
    initialUserIndex: Int,
    customStoryUsers: List<StoryUser>? = null,
    onClose: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    onOpenLink: (url: String) -> Unit,
    onOpenHashtag: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StoryPlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeStoryUsersFlow by viewModel.activeStoryUsers.collectAsStateWithLifecycle()
    val storyUsers = customStoryUsers ?: activeStoryUsersFlow
    val currentUserId = viewModel.currentUserId
    val repository = viewModel.repository
    if (storyUsers.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    var currentUserIndex by remember {
        mutableIntStateOf(initialUserIndex.coerceIn(0, (storyUsers.size - 1).coerceAtLeast(0)))
    }
    val activeUser = storyUsers.getOrNull(currentUserIndex)
    if (activeUser == null || activeUser.stories.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    var currentStoryIndex by remember(currentUserIndex) {
        // Start from the first unseen story, or from 0
        val firstUnseen = activeUser.stories.indexOfFirst { !it.isViewed }
        mutableIntStateOf(if (firstUnseen >= 0) firstUnseen else 0)
    }

    val activeStory = activeUser.stories.getOrNull(currentStoryIndex) ?: activeUser.stories.first()
    val isOwnStory = activeUser.id == currentUserId

    var isPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isImageReady by remember(activeStory.id) { mutableStateOf(false) }
    var isBuffering by remember(activeStory.id) { mutableStateOf(false) }
    var isAppInForeground by remember { mutableStateOf(true) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    var isQuestionDialogOpen by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showAddToHighlightDialog by remember { mutableStateOf(false) }
    var showNewHighlightDialog by remember { mutableStateOf(false) }
    var newHighlightTitle by remember { mutableStateOf("") }
    var userHighlights by remember { mutableStateOf<List<HighlightDto>>(emptyList()) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var showQuestionAnswersSheet by remember { mutableStateOf(false) }
    var questionAnswers by remember { mutableStateOf<List<StoryQuestionAnswerDto>>(emptyList()) }
    var isLoadingQuestionAnswers by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var isLiked by remember(activeStory.id) { mutableStateOf(false) }
    var floatingReaction by remember(activeStory.id) { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    isAppInForeground = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    isAppInForeground = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isPlaybackPaused = isPaused ||
        isQuestionDialogOpen ||
        showOptionsSheet ||
        showDeleteDialog ||
        showReportDialog ||
        showViewersSheet ||
        showAddToHighlightDialog ||
        showNewHighlightDialog ||
        showQuestionAnswersSheet ||
        replyText.isNotBlank() ||
        !isAppInForeground

    LaunchedEffect(floatingReaction) {
        if (floatingReaction != null) {
            delay(900)
            floatingReaction = null
        }
    }

    val hasMusic = !activeStory.musicUrl.isNullOrBlank() ||
        activeStory.interactiveElements.any { it.type == "music" }

    // Video Player
    val exoPlayer = remember(activeStory.id) {
        if (activeStory.mediaType == StoryMediaType.Video) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(activeStory.mediaUrl)))
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                volume = if (isMuted || hasMusic) 0f else 1f
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = state == Player.STATE_BUFFERING
                    }
                })
                prepare()
                playWhenReady = true
            }
        } else null
    }

    // Music Audio Player (for stories with attached music or music sticker, honoring selected chorus offset)
    val musicElement = remember(activeStory.id) {
        activeStory.interactiveElements.firstOrNull { it.type == "music" }?.music
    }
    val musicPlayer = remember(activeStory.id) {
        val musicUrl = activeStory.musicUrl ?: musicElement?.musicUrl
        if (!musicUrl.isNullOrBlank()) {
            val baseItem = MediaItem.fromUri(Uri.parse(musicUrl))
            val startMs = musicElement?.startMs ?: 0
            val endMs = musicElement?.endMs ?: 0
            val clippedItem = if (startMs > 0 || endMs > startMs) {
                val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.coerceAtLeast(0).toLong())
                if (endMs > startMs) {
                    clipBuilder.setEndPositionMs(endMs.toLong())
                }
                baseItem.buildUpon()
                    .setClippingConfiguration(clipBuilder.build())
                    .build()
            } else {
                baseItem
            }
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(clippedItem)
                volume = if (isMuted) 0f else 1f
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
        } else null
    }

    LaunchedEffect(isMuted, exoPlayer, musicPlayer, hasMusic) {
        exoPlayer?.volume = if (isMuted || hasMusic) 0f else 1f
        musicPlayer?.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(exoPlayer, musicPlayer) {
        onDispose {
            exoPlayer?.release()
            musicPlayer?.release()
        }
    }

    // Mark as viewed
    LaunchedEffect(activeStory.id) {
        repository.markStoryAsViewed(activeStory.id, currentUserId)
    }

    fun goToNextStory() {
        if (currentStoryIndex < activeUser.stories.size - 1) {
            currentStoryIndex++
            currentProgress = 0f
        } else if (currentUserIndex < storyUsers.size - 1) {
            currentUserIndex++
            currentProgress = 0f
        } else {
            onClose()
        }
    }

    fun goToPreviousStory() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            currentProgress = 0f
        } else if (currentUserIndex > 0) {
            currentUserIndex--
            currentProgress = 0f
        } else {
            currentProgress = 0f
        }
    }

    val updatedGoToNext by rememberUpdatedState(::goToNextStory)
    val updatedGoToPrevious by rememberUpdatedState(::goToPreviousStory)

    // Timer & Progress Engine
    LaunchedEffect(activeStory.id, isPlaybackPaused, currentUserIndex, currentStoryIndex) {
        if (isPlaybackPaused) {
            exoPlayer?.pause()
            musicPlayer?.pause()
            return@LaunchedEffect
        } else {
            exoPlayer?.play()
            musicPlayer?.play()
        }

        val isVideo = activeStory.mediaType == StoryMediaType.Video
        if (isVideo && exoPlayer != null) {
            while (isActive && !isPlaybackPaused) {
                if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.duration > 0L) {
                    val duration = exoPlayer.duration.coerceAtLeast(1L)
                    val position = exoPlayer.currentPosition
                    currentProgress = (position.toFloat() / duration).coerceIn(0f, 1f)
                }
                if (exoPlayer.playbackState == Player.STATE_ENDED || currentProgress >= 1f) {
                    updatedGoToNext()
                    break
                }
                delay(30)
            }
        } else {
            // Hold until image is actually loaded from CDN/Disk
            while (!isImageReady && isActive && !isPlaybackPaused) {
                delay(40)
            }

            val totalDurationMs = 5000L // 5 seconds for image
            val stepMs = 30L
            var elapsed = (currentProgress * totalDurationMs).toLong()

            while (isActive && !isPlaybackPaused && elapsed < totalDurationMs) {
                delay(stepMs)
                elapsed += stepMs
                currentProgress = (elapsed.toFloat() / totalDurationMs).coerceIn(0f, 1f)
            }

            if (currentProgress >= 1f && !isPlaybackPaused) {
                updatedGoToNext()
            }
        }
    }

    val videoTransformElement = remember(activeStory.id) {
        activeStory.interactiveElements.firstOrNull { it.type == "media_transform" }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Black,
            modifier = modifier.fillMaxSize(),
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { dragOffsetY = 0f },
                            onDragCancel = { dragOffsetY = 0f },
                            onDragEnd = {
                                when {
                                    dragOffsetY > 140f -> onClose()
                                    dragOffsetY < -140f && isOwnStory -> {
                                        dragOffsetY = 0f
                                        showViewersSheet = true
                                    }
                                    else -> dragOffsetY = 0f
                                }
                            },
                        ) { _, dragAmount ->
                            dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-280f, 400f)
                        }
                    },
            ) {
                // 9:16 Stage Media Layer (1:1 coordinate parity with StoryEditorScreen & StoryElementsOverlay)
                StoryStage(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = dragOffsetY
                            val s = (1f - (kotlin.math.abs(dragOffsetY) / 1400f)).coerceIn(0.72f, 1f)
                            scaleX = s
                            scaleY = s
                        },
                    cornerRadius = 16.dp,
                ) { stage ->
                    if (activeStory.mediaType == StoryMediaType.Video && exoPlayer != null) {
                        val tScale = videoTransformElement?.scale ?: 1f
                        val tRot = videoTransformElement?.rotation ?: 0f
                        val tCx = videoTransformElement?.xNorm ?: 0.5f
                        val tCy = videoTransformElement?.yNorm ?: 0.5f
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
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = (tCx - 0.5f) * stage.width
                                    translationY = (tCy - 0.5f) * stage.height
                                    scaleX = tScale
                                    scaleY = tScale
                                    rotationZ = tRot
                                },
                        )
                    } else {
                        AsyncImage(
                            model = activeStory.mediaUrl,
                            contentDescription = "استوری",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            onSuccess = { isImageReady = true },
                            onError = { isImageReady = true },
                        )
                    }

                    if (isBuffering || (activeStory.mediaType != StoryMediaType.Video && !isImageReady)) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp,
                            )
                        }
                    }
                }

                // Tap Areas for Next / Previous / Pause
                Row(modifier = Modifier.fillMaxSize()) {
                    // Right area in RTL = Previous (30% width)
                    Box(
                        modifier = Modifier
                            .weight(0.32f)
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPaused = true
                                        exoPlayer?.pause()
                                        musicPlayer?.pause()
                                        tryAwaitRelease()
                                        isPaused = false
                                        if (!isPlaybackPaused) {
                                            exoPlayer?.play()
                                            musicPlayer?.play()
                                        }
                                    },
                                    onTap = { updatedGoToPrevious() },
                                )
                            },
                    )
                    // Left area in RTL = Next (70% width)
                    Box(
                        modifier = Modifier
                            .weight(0.68f)
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPaused = true
                                        exoPlayer?.pause()
                                        musicPlayer?.pause()
                                        tryAwaitRelease()
                                        isPaused = false
                                        if (!isPlaybackPaused) {
                                            exoPlayer?.play()
                                            musicPlayer?.play()
                                        }
                                    },
                                    onTap = { updatedGoToNext() },
                                )
                            },
                    )
                }

                // Interactive Elements Overlay (Poll, Mention, Link, Question, etc.).
                // Composed above the tap areas so widgets receive their own taps; empty
                // regions fall through to next/previous/hold-to-pause underneath.
                if (activeStory.interactiveElements.isNotEmpty()) {
                    StoryElementsOverlay(
                        elements = activeStory.interactiveElements,
                        isAuthor = (activeStory.userId == currentUserId || activeUser.id == currentUserId || isOwnStory),
                        onPollVote = { elementId, optionIndex ->
                            coroutineScope.launch {
                                repository.votePoll(activeStory.id, elementId, optionIndex)
                            }
                        },
                        onQuestionAnswer = { elementId, answer ->
                            viewModel.answerQuestion(activeStory.id, elementId, answer) { success, err ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) "پاسخ شما با موفقیت ثبت شد"
                                        else (err ?: "خطا در ثبت پاسخ"),
                                    )
                                }
                            }
                        },
                        onViewQuestionAnswers = { _ ->
                            isPaused = true
                            isLoadingQuestionAnswers = true
                            showQuestionAnswersSheet = true
                            viewModel.loadQuestionAnswers(activeStory.id) { answers ->
                                questionAnswers = answers
                                isLoadingQuestionAnswers = false
                            }
                        },
                        onLinkClick = onOpenLink,
                        onMentionClick = onOpenProfile,
                        onHashtagClick = onOpenHashtag,
                        onReactionClick = { emoji ->
                            viewModel.reactToStory(activeStory.id, emoji)
                        },
                        onDialogVisibilityChanged = { visible ->
                            isQuestionDialogOpen = visible
                        },
                    )
                }

                // Top Gradient Scrim & UI Overlay
                AnimatedVisibility(
                    visible = !isPaused,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                                ),
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        // Multi-segment progress bar
                        StoryProgressBar(
                            totalCount = activeUser.stories.size,
                            currentIndex = currentStoryIndex,
                            currentProgress = currentProgress,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Header (User info, close, options)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onOpenProfile(activeUser.id) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .border(2.dp, Color.White, CircleShape)
                                        .clip(CircleShape)
                                        .background(Color(0xFF424242)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!activeUser.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = activeUser.avatarUrl,
                                            contentDescription = activeUser.username,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = activeUser.username,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (activeUser.isVerified || activeUser.isPremium) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            StoryVerificationBadge(
                                                isVerified = activeUser.isVerified,
                                                verificationType = activeUser.verificationType,
                                                isPremium = activeUser.isPremium,
                                                size = 14.dp,
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatStoryTimeAgo(activeStory.createdAt),
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                        )
                                        if (activeStory.privacyType == StoryPrivacyType.CloseFriends) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF2E7D32))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp),
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "دوستان نزدیک",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isMuted = !isMuted }) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = if (isMuted) "صدادار کردن" else "بی‌صدا کردن",
                                        tint = Color.White,
                                    )
                                }
                                IconButton(onClick = { showOptionsSheet = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "گزینه‌ها",
                                        tint = Color.White,
                                    )
                                }
                                IconButton(onClick = onClose) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "بستن",
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Gradient Scrim & Reply / Viewers Bar
                AnimatedVisibility(
                    visible = !isPaused,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                ),
                            )
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        if (isOwnStory) {
                            // Viewers Button for Own Story
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                                    .clickable { showViewersSheet = true }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "بازدیدکنندگان (${activeStory.viewsCount})",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                AnimatedVisibility(
                                    visible = floatingReaction != null,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                ) {
                                    Text(
                                        text = floatingReaction.orEmpty(),
                                        fontSize = 54.sp,
                                        modifier = Modifier.padding(bottom = 6.dp),
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Color.Black.copy(alpha = 0.28f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    STORY_QUICK_REACTIONS.forEach { reaction ->
                                        IconButton(
                                            onClick = {
                                                floatingReaction = reaction.emoji
                                                viewModel.reactToStory(activeStory.id, reaction.apiValue)
                                            },
                                            modifier = Modifier.size(38.dp),
                                        ) { Text(reaction.emoji, fontSize = 22.sp) }
                                    }
                                }
                                // Direct Reply Input & Heart Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                if (activeStory.viewerCanReply) {
                                    OutlinedTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        placeholder = {
                                            Text(
                                                text = "پاسخ به ${activeUser.username}…",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 13.sp,
                                            )
                                        },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        shape = RoundedCornerShape(25.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.White.copy(alpha = 0.8f),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                        ),
                                        trailingIcon = {
                                            if (replyText.isNotBlank()) {
                                                IconButton(
                                                    onClick = {
                                                        val msg = replyText
                                                        replyText = ""
                                                        coroutineScope.launch {
                                                            val res = repository.replyToStory(activeStory.id, msg)
                                                            if (res.isSuccess) {
                                                                snackbarHostState.showSnackbar("پاسخ شما ارسال شد")
                                                            } else {
                                                                snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "خطا در ارسال پاسخ")
                                                            }
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Send,
                                                        contentDescription = "ارسال پاسخ",
                                                        tint = VistaBrandColors.Pink,
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(25.dp))
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "امکان ارسال پاسخ برای این استوری غیرفعال است",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        isLiked = !isLiked
                                        coroutineScope.launch {
                                            if (isLiked) {
                                                repository.reactToStory(activeStory.id, "heart")
                                            } else {
                                                repository.removeReaction(activeStory.id)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.18f)),
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "لایک",
                                        tint = if (isLiked) VistaBrandColors.Pink else Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                            }
                        }
                    }
                }
            }

            // Viewers Bottom Sheet
            if (showViewersSheet) {
                StoryViewersBottomSheet(
                    storyId = activeStory.id,
                    repository = repository,
                    onDismiss = { showViewersSheet = false },
                    onUserClick = { userId ->
                        showViewersSheet = false
                        onOpenProfile(userId)
                    },
                )
            }

            // Question Answers Bottom Sheet for Author
            if (showQuestionAnswersSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showQuestionAnswersSheet = false
                        isPaused = false
                    },
                    containerColor = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(20.dp),
                    ) {
                        Text(
                            text = "پاسخ‌های دریافتی به سوال",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        if (isLoadingQuestionAnswers) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = VistaBrandColors.Indigo)
                            }
                        } else if (questionAnswers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "هنوز پاسخی برای این سوال ثبت نشده است",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                questionAnswers.forEach { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF2A2A2A),
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                val avatarUrl = item.resolvedAvatarUrl
                                                if (!avatarUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = avatarUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop,
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                                Text(
                                                    text = item.resolvedUsername?.let { "@$it" } ?: "کاربر ناشناس",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.answer,
                                                color = Color(0xFFE0E0E0),
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Options Bottom Sheet
            if (showOptionsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showOptionsSheet = false },
                    containerColor = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 8.dp),
                    ) {
                        if (isOwnStory) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showOptionsSheet = false
                                        showViewersSheet = true
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("مشاهده‌کنندگان", color = Color.White, fontSize = 15.sp)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showOptionsSheet = false
                                        viewModel.loadUserHighlights { userHighlights = it }
                                        showAddToHighlightDialog = true
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("افزودن به هایلایت", color = Color.White, fontSize = 15.sp)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showOptionsSheet = false
                                        showDeleteDialog = true
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("حذف استوری", color = Color(0xFFE53935), fontSize = 15.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showOptionsSheet = false
                                        showReportDialog = true
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Report, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("گزارش", color = Color.White, fontSize = 15.sp)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOptionsSheet = false
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, activeStory.mediaUrl)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "اشتراک‌گذاری استوری")
                                    context.startActivity(shareIntent)
                                }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("اشتراک‌گذاری", color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Add to Highlight Dialog
            if (showAddToHighlightDialog) {
                AlertDialog(
                    onDismissRequest = { showAddToHighlightDialog = false },
                    containerColor = Color(0xFF212121),
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(
                            text = "افزودن به هایلایت",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showAddToHighlightDialog = false
                                        newHighlightTitle = ""
                                        showNewHighlightDialog = true
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = VistaBrandColors.Pink)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("هایلایت جدید…", color = VistaBrandColors.Pink, fontWeight = FontWeight.Bold)
                            }
                            userHighlights.forEach { highlight ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showAddToHighlightDialog = false
                                            viewModel.addToHighlight(highlight, activeStory.id) { success ->
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (success) "به هایلایت «${highlight.title}» اضافه شد"
                                                        else "خطا در افزودن به هایلایت",
                                                    )
                                                }
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(highlight.title, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAddToHighlightDialog = false }) {
                            Text("انصراف", color = Color.White)
                        }
                    },
                )
            }

            // New Highlight Dialog
            if (showNewHighlightDialog) {
                AlertDialog(
                    onDismissRequest = { showNewHighlightDialog = false },
                    containerColor = Color(0xFF212121),
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(
                            text = "عنوان هایلایت جدید",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    text = {
                        OutlinedTextField(
                            value = newHighlightTitle,
                            onValueChange = { newHighlightTitle = it },
                            placeholder = { Text("مثلاً: خاطرات، سفر، کار…", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newHighlightTitle.isNotBlank()) {
                                    val title = newHighlightTitle
                                    showNewHighlightDialog = false
                                    viewModel.createHighlight(
                                        title = title,
                                        coverUrl = activeStory.thumbnailUrl ?: activeStory.mediaUrl,
                                        storyId = activeStory.id,
                                    ) { success ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (success) "هایلایت «$title» ایجاد شد"
                                                else "خطا در ایجاد هایلایت",
                                            )
                                        }
                                    }
                                }
                            },
                        ) {
                            Text("ایجاد", color = VistaBrandColors.Pink, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewHighlightDialog = false }) {
                            Text("انصراف", color = Color.White)
                        }
                    },
                )
            }

            // Delete Confirmation Dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    containerColor = Color(0xFF212121),
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(
                            text = "حذف استوری",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    text = {
                        Text(
                            text = "آیا از حذف این استوری مطمئن هستید؟",
                            color = Color(0xFFBDBDBD),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                coroutineScope.launch {
                                    repository.deleteStory(activeStory.id)
                                    goToNextStory()
                                }
                            },
                        ) {
                            Text(
                                text = "حذف",
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false },
                        ) {
                            Text(
                                text = "ادامه ویرایش",
                                color = Color.White,
                            )
                        }
                    },
                )
            }

            // Report Dialog
            if (showReportDialog) {
                VistaReportReasonDialog(
                    title = "گزارش استوری",
                    onDismiss = { showReportDialog = false },
                    onSubmit = { reason, details ->
                        showReportDialog = false
                        viewModel.reportStory(activeStory.id, reason, details ?: "") { success, err ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) "گزارش شما با موفقیت ثبت شد"
                                    else (err ?: "خطا در ثبت گزارش"),
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun StoryVerificationBadge(
    isVerified: Boolean,
    verificationType: StoryVerificationType,
    isPremium: Boolean,
    size: Dp = 14.dp,
) {
    if (!isVerified && !isPremium) return
    val badgeColor = when (verificationType) {
        StoryVerificationType.Gold -> Color(0xFFFFA000)
        StoryVerificationType.Black -> Color.White
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(badgeColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "✓",
            color = if (badgeColor == Color.White) Color.Black else Color.White,
            fontSize = (size.value * 0.55f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatStoryTimeAgo(createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return "به‌تازگی"
    return ir.coffevista.vista_native.core.common.persianRelativeTime(createdAt).ifEmpty { "به‌تازگی" }
}

private data class StoryQuickReaction(val apiValue: String, val emoji: String)

private val STORY_QUICK_REACTIONS = listOf(
    StoryQuickReaction("like", "❤️"),
    StoryQuickReaction("laugh", "😂"),
    StoryQuickReaction("wow", "😮"),
    StoryQuickReaction("sad", "😢"),
    StoryQuickReaction("fire", "🔥"),
)
