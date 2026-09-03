package ir.coffevista.vista_native.features.feed.ui.reels

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReelsUiState(
    val reels: List<FeedPost> = emptyList(),
    val isLoading: Boolean = true,
    val isMuted: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ReelsViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authStateProvider: AuthenticationStateProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    init {
        loadReels()
    }

    fun loadReels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val userId = (authStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId.orEmpty()
            feedRepository.observeFeed(userId, ir.coffevista.vista_native.features.feed.data.FeedKind.Explore).collect { snapshot ->
                val videoPosts = snapshot.posts.filter { !it.videoUrl.isNullOrBlank() }
                _uiState.update { it.copy(reels = videoPosts, isLoading = false) }
            }
        }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleLike(postId: String) {
        val current = _uiState.value.reels.find { it.id == postId } ?: return
        val userId = (authStateProvider.state.value as? AuthenticationState.SignedIn)?.context?.userId.orEmpty()
        val newIsLiked = !current.isLiked
        val newCount = if (newIsLiked) current.likeCount + 1 else (current.likeCount - 1).coerceAtLeast(0)
        _uiState.update { state ->
            state.copy(
                reels = state.reels.map {
                    if (it.id == postId) it.copy(isLiked = newIsLiked, likeCount = newCount) else it
                },
            )
        }
        viewModelScope.launch {
            feedRepository.toggleLike(userId, postId, current.userId, newIsLiked, newCount)
        }
    }
}

@Composable
fun ReelsViewerScreen(
    initialPostId: String? = null,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onCommentsClick: (String) -> Unit = {},
    onShareClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ReelsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reels = uiState.reels

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            if (uiState.isLoading && reels.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            } else if (reels.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("ویدیویی برای نمایش وجود ندارد", color = Color.White, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
                    }
                }
            } else {
                val initialIndex = remember(reels, initialPostId) {
                    val index = reels.indexOfFirst { it.id == initialPostId }
                    if (index >= 0) index else 0
                }
                val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { reels.size })

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { reels[it].id },
                ) { page ->
                    val isCurrentPage = pagerState.currentPage == page
                    val reel = reels[page]
                    ReelsVideoItem(
                        post = reel,
                        isCurrentPage = isCurrentPage,
                        isMuted = uiState.isMuted,
                        onLikeClick = { viewModel.toggleLike(reel.id) },
                        onCommentsClick = { onCommentsClick(reel.id) },
                        onShareClick = { onShareClick(reel.id) },
                        onAuthorClick = { onAuthorClick(reel.userId) },
                    )
                }

                // Top Header Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        text = "ریلز",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = viewModel::toggleMute) {
                        Icon(
                            imageVector = if (uiState.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "صدا",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ReelsVideoItem(
    post: FeedPost,
    isCurrentPage: Boolean,
    isMuted: Boolean,
    onLikeClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onShareClick: () -> Unit,
    onAuthorClick: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var heartBurstKey by remember { mutableIntStateOf(0) }
    var showHeartBurst by remember { mutableStateOf(false) }

    val videoUrl = post.videoUrl.orEmpty()
    val player = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            repeatMode = Player.REPEAT_MODE_ONE
            if (videoUrl.isNotBlank()) {
                setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
                prepare()
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            player.playWhenReady = true
            player.play()
        } else {
            player.pause()
            player.seekTo(0)
        }
    }

    LaunchedEffect(isMuted) {
        player.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (isCurrentPage) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(heartBurstKey) {
        if (heartBurstKey > 0) {
            showHeartBurst = true
            delay(700)
            showHeartBurst = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotate")
    val diskRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(post.id) {
                detectTapGestures(
                    onDoubleTap = {
                        heartBurstKey += 1
                        if (!post.isLiked) onLikeClick()
                    },
                    onTap = {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                )
            },
    ) {
        // ExoPlayer AndroidView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
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

        // Gradient shadow overlay on bottom and top
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                        ),
                    ),
                ),
        )

        // Buffering indicator
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center),
                color = Color.White.copy(alpha = 0.8f),
                strokeWidth = 3.dp,
            )
        }

        // Pause/Play flash icon
        if (!isPlaying && !isBuffering) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "مکث",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        // Double-tap heart animation popup
        AnimatedVisibility(
            visible = showHeartBurst,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(100)) + scaleIn(initialScale = 0.6f, animationSpec = tween(200, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(300)) + scaleOut(targetScale = 1.2f),
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer { shadowElevation = 16f },
            )
        }

        // Bottom Info Section (Author, Caption, Music)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(start = 16.dp, bottom = 24.dp),
        ) {
            // Author Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onAuthorClick)
                    .padding(vertical = 4.dp),
            ) {
                AsyncImage(
                    model = post.authorAvatarUrl,
                    contentDescription = post.authorFullName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = post.authorFullName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    post.authorUsername?.let { username ->
                        Text(
                            text = "@$username",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Caption
            if (!post.content.isNullOrBlank()) {
                var isExpanded by remember { mutableStateOf(false) }
                Text(
                    text = post.content,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = if (isExpanded) 10 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { isExpanded = !isExpanded },
                )
            }

            Spacer(Modifier.height(8.dp))

            // Music track pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = post.musicTitle ?: "صدای اصلی - ${post.authorFullName}",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Right/Side Action Buttons Column
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Like Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onLikeClick, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "پسندیدن",
                        tint = if (post.isLiked) Color.Red else Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Text(
                    text = if (post.likeCount > 0) post.likeCount.toString() else "لایک",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Comments Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onCommentsClick, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "دیدگاه‌ها",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = if (post.commentCount > 0) post.commentCount.toString() else "نظر",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Share Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onShareClick, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "اشتراک‌گذاری",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    text = "ارسال",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Rotating Audio Vinyl Disc
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .rotate(if (isPlaying) diskRotation else 0f),
                contentAlignment = Alignment.Center,
            ) {
                if (!post.authorAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = post.authorAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
