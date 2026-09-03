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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import ir.coffevista.vista_native.features.stories.data.StoryRepository
import ir.coffevista.vista_native.features.stories.domain.StoryMediaType
import ir.coffevista.vista_native.features.stories.domain.StoryUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StoryPlayerScreen(
    initialUserIndex: Int,
    onClose: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    onOpenLink: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoryPlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val storyUsers by viewModel.activeStoryUsers.collectAsStateWithLifecycle()
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
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var isLiked by remember(activeStory.id) { mutableStateOf(false) }
    var floatingReaction by remember(activeStory.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(floatingReaction) {
        if (floatingReaction != null) {
            delay(900)
            floatingReaction = null
        }
    }

    // Video Player
    val exoPlayer = remember(activeStory.id) {
        if (activeStory.mediaType == StoryMediaType.Video) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(activeStory.mediaUrl)))
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
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

    // Mark as viewed
    LaunchedEffect(activeStory.id) {
        repository.markStoryAsViewed(activeStory.id)
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

    // Timer & Progress Engine
    LaunchedEffect(activeStory.id, isPaused, currentUserIndex, currentStoryIndex) {
        if (isPaused) return@LaunchedEffect

        val isVideo = activeStory.mediaType == StoryMediaType.Video
        if (isVideo && exoPlayer != null) {
            while (isActive && !isPaused) {
                val duration = exoPlayer.duration.coerceAtLeast(1)
                val position = exoPlayer.currentPosition
                currentProgress = (position.toFloat() / duration).coerceIn(0f, 1f)
                if (exoPlayer.playbackState == Player.STATE_ENDED || currentProgress >= 1f) {
                    goToNextStory()
                    break
                }
                delay(30)
            }
        } else {
            val totalDurationMs = 5000L // 5 seconds for image
            val stepMs = 30L
            var elapsed = (currentProgress * totalDurationMs).toLong()

            while (isActive && !isPaused && elapsed < totalDurationMs) {
                delay(stepMs)
                elapsed += stepMs
                currentProgress = (elapsed.toFloat() / totalDurationMs).coerceIn(0f, 1f)
            }

            if (currentProgress >= 1f && !isPaused) {
                goToNextStory()
            }
        }
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
                        detectDragGestures { _, dragAmount ->
                            if (dragAmount.y > 60) {
                                onClose()
                            } else if (dragAmount.y < -60 && isOwnStory) {
                                showViewersSheet = true
                            }
                        }
                    },
            ) {
                // Media Layer
                if (activeStory.mediaType == StoryMediaType.Video && exoPlayer != null) {
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
                        model = activeStory.mediaUrl,
                        contentDescription = "استوری",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                // Interactive Elements Overlay (Poll, Mention, Link, etc.)
                if (activeStory.interactiveElements.isNotEmpty()) {
                    StoryElementsOverlay(
                        elements = activeStory.interactiveElements,
                        onPollVote = { optionId ->
                            coroutineScope.launch {
                                repository.votePoll(activeStory.id, optionId)
                            }
                        },
                        onLinkClick = onOpenLink,
                        onMentionClick = onOpenProfile,
                    )
                }

                // Tap Areas for Next / Previous / Pause
                Row(modifier = Modifier.fillMaxSize()) {
                    // Right area in RTL = Previous
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPaused = true
                                        exoPlayer?.pause()
                                        tryAwaitRelease()
                                        isPaused = false
                                        exoPlayer?.play()
                                    },
                                    onTap = { goToPreviousStory() },
                                )
                            },
                    )
                    // Left area in RTL = Next
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPaused = true
                                        exoPlayer?.pause()
                                        tryAwaitRelease()
                                        isPaused = false
                                        exoPlayer?.play()
                                    },
                                    onTap = { goToNextStory() },
                                )
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = activeUser.username,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "استوری ویستا",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOwnStory) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteStory(activeStory.id)
                                                goToNextStory()
                                            }
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف استوری",
                                            tint = Color.White,
                                        )
                                    }
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
                                                        repository.replyToStory(activeStory.id, msg)
                                                        snackbarHostState.showSnackbar("پاسخ شما ارسال شد")
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

                                IconButton(
                                    onClick = {
                                        isLiked = !isLiked
                                        coroutineScope.launch {
                                            repository.reactToStory(activeStory.id, if (isLiked) "heart" else "unlike")
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
        }
    }
}

private data class StoryQuickReaction(val apiValue: String, val emoji: String)

private val STORY_QUICK_REACTIONS = listOf(
    StoryQuickReaction("like", "❤️"),
    StoryQuickReaction("laugh", "😂"),
    StoryQuickReaction("wow", "😮"),
    StoryQuickReaction("sad", "😢"),
    StoryQuickReaction("fire", "🔥"),
)
