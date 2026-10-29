package ir.coffevista.vista_native.features.feed.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import ir.coffevista.vista_native.core.designsystem.component.LocalBottomBarVisibility
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import ir.coffevista.vista_native.core.database.profile.hasBlueBadge
import ir.coffevista.vista_native.core.database.profile.hasPremiumPrivileges
import ir.coffevista.vista_native.core.designsystem.component.VistaPremiumPromptDialog
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.ClickableText
import ir.coffevista.vista_native.features.feed.ui.components.EditPostDialog
import ir.coffevista.vista_native.features.feed.ui.components.PostModerationBanner
import ir.coffevista.vista_native.features.feed.ui.components.ReportReasonDialog
import ir.coffevista.vista_native.core.designsystem.component.VistaBadgedBox
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiText
import ir.coffevista.vista_native.core.designsystem.component.VistaFloatingActionButton
import ir.coffevista.vista_native.core.designsystem.component.VistaNotificationBadge
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ripple
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import ir.coffevista.vista_native.features.feed.data.PostUploadState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR
import ir.coffevista.vista_native.features.feed.data.FeedKind
import ir.coffevista.vista_native.features.feed.data.FeedPost
import java.time.Duration
import java.time.Instant
import ir.coffevista.vista_native.core.designsystem.component.LocalHomeTabClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    viewModel: FeedViewModel,
    commentsViewModel: CommentsViewModel,
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onAppealClick: (String) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onStoryClick: () -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    onOpenStoryPlayer: (Int) -> Unit = {},
    onCreateStory: () -> Unit = {},
    onSendDirectMessage: (FeedPost) -> Unit = {},
    onNavigateToStoryEditor: (android.net.Uri) -> Unit = {},
    onVideoClick: (String) -> Unit = onPostClick,
    onBottomBarVisibilityChange: ((Boolean) -> Unit)? = null,
    homeTabClickFlow: SharedFlow<Unit>? = null,
) {
    val bottomBarVisibilityCallback = onBottomBarVisibilityChange ?: LocalBottomBarVisibility.current
    DisposableEffect(Unit) {
        onDispose {
            bottomBarVisibilityCallback(true)
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedKind by viewModel.selectedKind.collectAsStateWithLifecycle()
    val viewerUserId by viewModel.viewerUserId.collectAsStateWithLifecycle()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    var showCommentsForPostId by remember { mutableStateOf<String?>(null) }
    var showShareForPost by remember { mutableStateOf<FeedPost?>(null) }
    var reportPostTarget by remember { mutableStateOf<FeedPost?>(null) }
    var isReporting by remember { mutableStateOf(false) }
    var editPostTarget by remember { mutableStateOf<FeedPost?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    val storyUsers by viewModel.activeStoryUsers.collectAsStateWithLifecycle()
    val ownProfile by viewModel.ownProfile.collectAsStateWithLifecycle()
    val postUploadState by viewModel.postUploadState.collectAsStateWithLifecycle()
    val isViewerBlueTick = ownProfile?.hasBlueBadge == true
    val viewerHasPremiumEdit = ownProfile?.hasPremiumPrivileges == true
    var premiumPromptFeature by remember { mutableStateOf<String?>(null) }

    FeedScreenContent(
        uiState = uiState,
        viewerUserId = viewerUserId,
        isViewerBlueTick = isViewerBlueTick,
        viewerHasPremiumEdit = viewerHasPremiumEdit,
        onPremiumPrompt = { feature -> premiumPromptFeature = feature },
        currentUserAvatar = ownProfile?.avatarUrl,
        unreadNotificationCount = unreadNotificationCount,
        selectedKind = selectedKind,
        storyUsers = storyUsers,
        postUploadState = postUploadState,
        onRetryUpload = viewModel::retryPostUpload,
        onCancelUpload = viewModel::cancelPostUpload,
        onDismissUpload = viewModel::dismissPostUpload,
        onKindSelected = viewModel::selectKind,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onPostClick = { postId ->
            viewModel.trackEvent(postId, "open")
            onPostClick(postId)
        },
        onVideoClick = { postId ->
            viewModel.trackEvent(postId, "open")
            onVideoClick(postId)
        },
        onAuthorClick = onAuthorClick,
        onLikeClick = viewModel::toggleLike,
        onSaveClick = viewModel::toggleSave,
        onCommentClick = { postId ->
            viewModel.trackEvent(postId, "comment")
            showCommentsForPostId = postId
        },
        onShareClick = { post -> showShareForPost = post },
        onEditPost = { post -> editPostTarget = post },
        onDeletePost = viewModel::deletePost,
        onReportPost = { post -> reportPostTarget = post },
        onNotInterested = viewModel::markNotInterested,
        onVisibilityChange = viewModel::updateEngagementVisibility,
        onHashtagClick = onHashtagClick,
        onMentionClick = onMentionClick,
        onAppealClick = onAppealClick,
        onNotificationClick = onNotificationClick,
        onStoryClick = onStoryClick,
        onCreatePostClick = onCreatePostClick,
        onOpenStoryPlayer = onOpenStoryPlayer,
        onCreateStory = onCreateStory,
        onBarsVisibilityChange = bottomBarVisibilityCallback,
        homeTabClickFlow = homeTabClickFlow,
        onTrackEvent = viewModel::trackEvent,
    )

    showCommentsForPostId?.let { postId ->
        val postOwnerId = (uiState as? FeedUiState.Content)?.posts?.firstOrNull { it.id == postId }?.userId
        CommentsBottomSheet(
            postId = postId,
            onDismissRequest = { showCommentsForPostId = null },
            viewModel = commentsViewModel,
            postOwnerId = postOwnerId,
            onAuthorClick = onAuthorClick,
            onHashtagClick = onHashtagClick,
        )
    }
    showShareForPost?.let { post ->
        PostShareBottomSheet(
            post = post,
            onDismiss = { showShareForPost = null },
            onShared = { viewModel.trackEvent(post.id, "share") },
            onSendDirectMessage = onSendDirectMessage,
            onNavigateToStoryEditor = onNavigateToStoryEditor,
        )
    }
    reportPostTarget?.let { target ->
        ReportReasonDialog(
            isSubmitting = isReporting,
            onDismiss = { if (!isReporting) reportPostTarget = null },
            onSubmit = { reason, details ->
                isReporting = true
                viewModel.reportPost(target, reason, details) {
                    isReporting = false
                    reportPostTarget = null
                }
            },
        )
    }
    editPostTarget?.let { target ->
        val maxLen = if (isViewerBlueTick) 10000 else if (viewerHasPremiumEdit) 1000 else 500
        val title = if (isViewerBlueTick && target.userId != viewerUserId) "ویرایش ناظر" else "ویرایش پست"
        EditPostDialog(
            initialContent = target.content.orEmpty(),
            maxCharLength = maxLen,
            title = title,
            isSubmitting = isEditing,
            onDismiss = { if (!isEditing) editPostTarget = null },
            onConfirm = { newContent ->
                isEditing = true
                viewModel.updatePostContent(target.id, newContent) {
                    isEditing = false
                    editPostTarget = null
                }
            },
        )
    }
    premiumPromptFeature?.let { feature ->
        VistaPremiumPromptDialog(
            feature = feature,
            onDismiss = { premiumPromptFeature = null },
            onUpgradeClick = { premiumPromptFeature = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedScreenContent(
    uiState: FeedUiState,
    viewerUserId: String? = null,
    isViewerBlueTick: Boolean = false,
    viewerHasPremiumEdit: Boolean = false,
    onPremiumPrompt: (String) -> Unit = {},
    currentUserAvatar: String? = null,
    storyUsers: List<ir.coffevista.vista_native.features.stories.domain.StoryUser> = emptyList(),
    unreadNotificationCount: Int = 0,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
    onVideoClick: (String) -> Unit = onPostClick,
    onAuthorClick: (String) -> Unit = {},
    onLikeClick: (String, Boolean, Long) -> Unit = { _, _, _ -> },
    onSaveClick: (String, Boolean) -> Unit = { _, _ -> },
    onCommentClick: (String) -> Unit = {},
    onShareClick: (FeedPost) -> Unit = {},
    onEditPost: (FeedPost) -> Unit = {},
    onDeletePost: (String) -> Unit = {},
    onReportPost: (FeedPost) -> Unit = {},
    onNotInterested: (String) -> Unit = {},
    onVisibilityChange: (FeedPost, Boolean?, Boolean?) -> Unit = { _, _, _ -> },
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onAppealClick: (String) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onStoryClick: () -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    onOpenStoryPlayer: (Int) -> Unit = {},
    onCreateStory: () -> Unit = {},
    postUploadState: PostUploadState = PostUploadState(),
    onRetryUpload: () -> Unit = {},
    onCancelUpload: () -> Unit = {},
    onDismissUpload: () -> Unit = {},
    selectedKind: FeedKind = FeedKind.Explore,
    onKindSelected: (FeedKind) -> Unit = {},
    onBarsVisibilityChange: (Boolean) -> Unit = {},
    homeTabClickFlow: SharedFlow<Unit>? = null,
    onTrackEvent: (String, String) -> Unit = { _, _ -> },
) {
    val exploreListState = rememberLazyListState()
    val followingListState = rememberLazyListState()
    val listState = if (selectedKind == FeedKind.Explore) exploreListState else followingListState
    var isBarsVisible by rememberSaveable { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var idleJob by remember { mutableStateOf<Job?>(null) }
    var fullscreenMedia by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    fun updateBarsVisibility(visible: Boolean) {
        if (isBarsVisible != visible) {
            isBarsVisible = visible
            onBarsVisibilityChange(visible)
        }
    }

    val homeTabFlow = homeTabClickFlow ?: LocalHomeTabClick.current
    LaunchedEffect(homeTabFlow) {
        homeTabFlow.collect {
            idleJob?.cancel()
            updateBarsVisibility(true)
            coroutineScope.launch {
                if (listState.firstVisibleItemIndex > 10) {
                    listState.scrollToItem(10)
                }
                listState.animateScrollToItem(0)
            }
            onRefresh()
        }
    }

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -4f) {
                    updateBarsVisibility(false)
                    idleJob?.cancel()
                    idleJob = coroutineScope.launch {
                        delay(1200)
                        updateBarsVisibility(true)
                    }
                } else if (delta > 4f) {
                    idleJob?.cancel()
                    updateBarsVisibility(true)
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress,
            )
        }.collect { (index, offset, isScrolling) ->
            if (index == 0 && offset == 0) {
                idleJob?.cancel()
                updateBarsVisibility(true)
            } else if (isScrolling) {
                val delta = if (index != previousIndex) {
                    (index - previousIndex) * 1000 + (offset - previousOffset)
                } else {
                    offset - previousOffset
                }

                if (delta > 4) {
                    idleJob?.cancel()
                    updateBarsVisibility(false)
                    idleJob = coroutineScope.launch {
                        delay(1200)
                        updateBarsVisibility(true)
                    }
                } else if (delta < -4) {
                    idleJob?.cancel()
                    updateBarsVisibility(true)
                }
            }
            previousIndex = index
            previousOffset = offset
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topHeaderHeight = statusBarTop + 56.dp + 48.dp + 115.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
        ) {
            when (val state = uiState) {
                FeedUiState.Loading -> FeedSkeletonList(topPadding = topHeaderHeight + 8.dp)
                is FeedUiState.Error -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topHeaderHeight),
                ) {
                    FeedInitialError(
                        message = state.message,
                        onRetry = onRefresh,
                    )
                }
                is FeedUiState.Content -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(FeedTestTags.PullToRefresh),
                ) {
                    FeedContent(
                        state = state,
                        listState = listState,
                        viewerUserId = viewerUserId,
                        isViewerBlueTick = isViewerBlueTick,
                        viewerHasPremiumEdit = viewerHasPremiumEdit,
                        onPremiumPrompt = onPremiumPrompt,
                        selectedKind = selectedKind,
                        onLoadMore = onLoadMore,
                        onPostClick = onPostClick,
                        onVideoClick = onVideoClick,
                        onMediaClick = { imgs, idx -> fullscreenMedia = imgs to idx },
                        onAuthorClick = onAuthorClick,
                        onLikeClick = onLikeClick,
                        onSaveClick = onSaveClick,
                        onCommentClick = onCommentClick,
                        onShareClick = onShareClick,
                        onEditPost = onEditPost,
                        onDeletePost = onDeletePost,
                        onReportPost = onReportPost,
                        onNotInterested = onNotInterested,
                        onVisibilityChange = onVisibilityChange,
                        onHashtagClick = onHashtagClick,
                        onMentionClick = onMentionClick,
                        onAppealClick = onAppealClick,
                        onTrackEvent = onTrackEvent,
                        topPadding = topHeaderHeight + 8.dp,
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = isBarsVisible,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                exit = scaleOut(animationSpec = tween(200)) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp),
            ) {
                VistaFloatingActionButton(
                    onClick = onCreatePostClick,
                    contentDescription = "ارسال پست جدید",
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isBarsVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(250),
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                ) {
                    FeedAppBar(
                        onNotificationClick = onNotificationClick,
                        unreadNotificationCount = unreadNotificationCount,
                    )
                    FeedTabs(
                        selectedKind = selectedKind,
                        onKindSelected = { kind ->
                            updateBarsVisibility(true)
                            onKindSelected(kind)
                        },
                    )
                    FeedPostUploadBanner(
                        uploadState = postUploadState,
                        onRetry = onRetryUpload,
                        onCancel = onCancelUpload,
                        onDismiss = onDismissUpload,
                    )
                    ir.coffevista.vista_native.features.stories.ui.tray.StoryTray(
                        storyUsers = storyUsers,
                        currentUserId = viewerUserId.orEmpty(),
                        currentUserAvatar = currentUserAvatar,
                        onOpenStoryPlayer = onOpenStoryPlayer,
                        onCreateStory = onCreateStory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp),
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )
                }
            }
        }
        fullscreenMedia?.let { (images, initialIndex) ->
            ir.coffevista.vista_native.features.feed.ui.components.VistaFullscreenImageViewer(
                images = images,
                initialIndex = initialIndex,
                onDismiss = { fullscreenMedia = null },
            )
        }
    }
}

@Composable
private fun FeedAppBar(
    onNotificationClick: () -> Unit,
    unreadNotificationCount: Int = 0,
) {
    // Use the active Compose palette, which also respects Vista's in-app theme setting.
    // Reading the system configuration here previously selected the black asset when
    // the app was dark while Android itself was still in light mode.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val logoId = if (dark) {
        DesignSystemR.drawable.vista_auth_logo_dark
    } else {
        DesignSystemR.drawable.vista_auth_logo_light
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.background)
            .testTag(FeedTestTags.AppBar),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(logoId),
            contentDescription = "Vista",
            modifier = Modifier.size(35.dp),
        )
        NotificationBell(
            unreadCount = unreadNotificationCount,
            onClick = onNotificationClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
        )
    }
}

@Composable
private fun FeedStoryBar(onStoryClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(115.dp).clickable(onClick = onStoryClick).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(74.dp),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.26f)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "استوری جدید",
                modifier = Modifier.width(74.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FeedTabs(
    selectedKind: FeedKind,
    onKindSelected: (FeedKind) -> Unit,
) {
    val tabs = listOf(
        FeedKind.Explore to "برای شما",
        FeedKind.Following to "دنبال شده‌ها",
    )
    TabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selectedKind },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        indicator = { positions ->
            val index = tabs.indexOfFirst { it.first == selectedKind }
            val horizontalInset = (positions[index].width - 52.dp) / 2
            Box(
                Modifier
                    .tabIndicatorOffset(positions[index])
                    .padding(horizontal = horizontalInset)
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        },
        divider = {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        },
        modifier = Modifier.testTag(FeedTestTags.Tabs),
    ) {
        tabs.forEach { (kind, label) ->
            Tab(
                selected = kind == selectedKind,
                onClick = { onKindSelected(kind) },
                modifier = Modifier
                    .height(48.dp)
                    .testTag(FeedTestTags.tab(kind)),
                text = {
                    val selected = kind == selectedKind
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = if (selected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
            )
        }
    }
}

@Composable
internal fun FeedContent(
    state: FeedUiState.Content,
    viewerUserId: String? = null,
    isViewerBlueTick: Boolean = false,
    viewerHasPremiumEdit: Boolean = false,
    onPremiumPrompt: (String) -> Unit = {},
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
    onVideoClick: (String) -> Unit = onPostClick,
    onMediaClick: ((List<String>, Int) -> Unit)? = null,
    onAuthorClick: (String) -> Unit,
    onLikeClick: (String, Boolean, Long) -> Unit,
    onSaveClick: (String, Boolean) -> Unit,
    onCommentClick: (String) -> Unit,
    onShareClick: (FeedPost) -> Unit,
    onEditPost: (FeedPost) -> Unit = {},
    onDeletePost: (String) -> Unit,
    onReportPost: (FeedPost) -> Unit,
    onNotInterested: (String) -> Unit,
    onVisibilityChange: (FeedPost, Boolean?, Boolean?) -> Unit,
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onAppealClick: (String) -> Unit = {},
    selectedKind: FeedKind = FeedKind.Explore,
    listState: LazyListState = rememberLazyListState(),
    onTrackEvent: (String, String) -> Unit = { _, _ -> },
    topPadding: androidx.compose.ui.unit.Dp = 8.dp,
) {
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val viewedPostIds = remember { mutableSetOf<String>() }
    val postVisibilityStartTimes = remember { mutableMapOf<String, Long>() }

    LaunchedEffect(listState, state.posts) {
        val activeJobs = mutableMapOf<String, Job>()
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val visibleItems = layoutInfo.visibleItemsInfo

            visibleItems.mapNotNull { itemInfo ->
                val postId = itemInfo.key as? String ?: return@mapNotNull null
                val itemTop = itemInfo.offset
                val itemBottom = itemTop + itemInfo.size
                val visibleTop = maxOf(viewportStart, itemTop)
                val visibleBottom = minOf(viewportEnd, itemBottom)
                val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(0)
                val fraction = if (itemInfo.size > 0) visibleHeight.toFloat() / itemInfo.size else 0f
                if (fraction >= 0.60f) postId else null
            }.toSet()
        }.collect { currentlyVisibleIds ->
            val now = System.currentTimeMillis()

            val leftIds = postVisibilityStartTimes.keys - currentlyVisibleIds
            for (leftId in leftIds) {
                activeJobs[leftId]?.cancel()
                activeJobs.remove(leftId)
                val startTime = postVisibilityStartTimes.remove(leftId)
                if (startTime != null) {
                    val dwellTime = now - startTime
                    if (dwellTime >= 1500L) {
                        onTrackEvent(leftId, "dwell")
                    }
                }
            }

            for (visibleId in currentlyVisibleIds) {
                if (!postVisibilityStartTimes.containsKey(visibleId)) {
                    postVisibilityStartTimes[visibleId] = now
                }
                if (!viewedPostIds.contains(visibleId) && !activeJobs.containsKey(visibleId)) {
                    activeJobs[visibleId] = launch {
                        delay(800L)
                        viewedPostIds.add(visibleId)
                        onTrackEvent(visibleId, "view")
                    }
                }
            }
        }
    }

    LaunchedEffect(listState, state.posts.size, state.hasMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            state.posts.isNotEmpty() &&
                state.hasMore &&
                lastVisibleItem >= totalItems - 4
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(FeedTestTags.List),
        contentPadding = PaddingValues(top = topPadding, bottom = 110.dp + navigationBarInset),
    ) {
        if (state.isOffline || state.isStale) {
            item(key = "offline-banner") {
                OfflineBanner()
            }
        }
        items(state.posts, key = { it.id }) { post ->
            VistaFeedPostCard(
                post = post,
                isOwnPost = post.userId == viewerUserId,
                isBlueTick = isViewerBlueTick,
                hasPremiumEdit = viewerHasPremiumEdit,
                showFollowState = selectedKind == FeedKind.Explore,
                onPostClick = { onPostClick(post.id) },
                onVideoClick = { onVideoClick(post.id) },
                onMediaClick = onMediaClick,
                onAuthorClick = { onAuthorClick(post.userId) },
                onLikeClick = onLikeClick,
                onSaveClick = onSaveClick,
                onCommentClick = { onCommentClick(post.id) },
                onShareClick = { onShareClick(post) },
                onEditPost = { onEditPost(post) },
                onDeletePost = { onDeletePost(post.id) },
                onReportPost = { onReportPost(post) },
                onNotInterested = { onNotInterested(post.id) },
                onVisibilityChange = { hideLike, hideComment -> onVisibilityChange(post, hideLike, hideComment) },
                onPremiumPrompt = onPremiumPrompt,
                onHashtagClick = onHashtagClick,
                onMentionClick = onMentionClick,
                onAppealClick = onAppealClick,
            )
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        if (state.posts.isEmpty() && !state.isRefreshing) {
            item(key = "empty") {
                FeedEmptyState()
            }
        }
        if (state.isAppending) {
            item(key = "append-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .testTag(FeedTestTags.Appending),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        state.appendError?.let { message ->
            item(key = "append-error") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.testTag(FeedTestTags.AppendError),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "تلاش دوباره",
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable(onClick = onLoadMore),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
        if (!state.hasMore && state.posts.isNotEmpty()) {
            item(key = "end-reached") {
                Text(
                    text = "همهٔ پست‌ها نمایش داده شد",
                    modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(FeedTestTags.EndReached),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun VistaFeedPostCard(
    post: FeedPost,
    isOwnPost: Boolean = false,
    isBlueTick: Boolean = false,
    hasPremiumEdit: Boolean = false,
    verticalMenu: Boolean = false,
    onPostClick: () -> Unit,
    onVideoClick: (() -> Unit)? = null,
    onMediaClick: ((List<String>, Int) -> Unit)? = null,
    onAuthorClick: () -> Unit,
    onLikeClick: (String, Boolean, Long) -> Unit = { _, _, _ -> },
    onSaveClick: (String, Boolean) -> Unit = { _, _ -> },
    onCommentClick: (String) -> Unit = {},
    onShareClick: () -> Unit = {},
    onEditPost: () -> Unit = {},
    onDeletePost: () -> Unit = {},
    onReportPost: () -> Unit = {},
    onNotInterested: () -> Unit = {},
    onVisibilityChange: (Boolean?, Boolean?) -> Unit = { _, _ -> },
    onPremiumPrompt: (String) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onAppealClick: (String) -> Unit = {},
    showFollowState: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(post.id) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onPostClick)
            .testTag(FeedTestTags.post(post.id)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAuthorClick)
                    .testTag(FeedTestTags.author(post.userId)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PostAvatar(post)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VistaEmojiText(
                            text = post.authorUsername?.takeIf { it.isNotBlank() } ?: post.authorFullName,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        if (post.authorIsVerified) {
                            VerifiedMark(
                                modifier = Modifier.padding(start = 4.dp),
                                verificationType = post.authorVerificationType,
                            )
                        }
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                        Text(
                            text = relativeTime(post.createdAt),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            val followState = post.authorFollowStatus?.lowercase()
            if (showFollowState && (followState == null || followState == "none" ||
                    followState == "requested")
            ) {
                Surface(
                    onClick = onAuthorClick,
                    modifier = Modifier
                        .width(112.dp)
                        .height(28.dp)
                        .testTag(FeedTestTags.follow(post.id)),
                    shape = RoundedCornerShape(14.dp),
                    color = if (followState == "requested") {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (followState == "requested") {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                ) {
                    ScaleDownSingleLineText(
                        text = if (followState == "requested") {
                            "در انتظار تأیید"
                        } else {
                            "دنبال کردن"
                        },
                    )
                }
            }
        }

        PostModerationBanner(
            post = post,
            isOwner = isOwnPost,
            onAppealClick = onAppealClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        post.content?.takeIf(String::isNotBlank)?.let { caption ->
            HashtagMentionCaption(
                text = caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .then(if (verticalMenu) Modifier.testTag(PostDetailTestTags.Caption) else Modifier),
                style = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = if (expanded) Int.MAX_VALUE else 6,
                overflow = TextOverflow.Ellipsis,
                onHashtagClick = onHashtagClick,
                onMentionClick = onMentionClick,
                onPostClick = onPostClick,
            )
            if (!expanded && caption.length > 180) {
                Text(
                    text = "بیشتر",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp).clickable { expanded = true },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        PostMedia(
            post = post,
            onMediaClick = onMediaClick,
            onClick = {
                if (!post.videoUrl.isNullOrBlank() && onVideoClick != null) {
                    onVideoClick()
                } else {
                    onPostClick()
                }
            },
            onDoubleTap = {
                if (!post.isLiked) {
                    onLikeClick(post.id, post.isLiked, post.likeCount)
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .then(if (verticalMenu) Modifier.testTag(PostDetailTestTags.Media) else Modifier),
        )
        PostMusicBubble(post = post)
        Spacer(Modifier.height(4.dp))
        PostActionRow(
            post = post,
            onPostClick = onPostClick,
            onLikeClick = { onLikeClick(post.id, post.isLiked, post.likeCount) },
            onSaveClick = { onSaveClick(post.id, post.isSaved) },
            onCommentClick = { onCommentClick(post.id) },
            onShareClick = onShareClick,
            onEditPost = onEditPost,
            onDeletePost = onDeletePost,
            onReportPost = onReportPost,
            onNotInterested = onNotInterested,
            onVisibilityChange = onVisibilityChange,
            onPremiumPrompt = onPremiumPrompt,
            isOwnPost = isOwnPost,
            isBlueTick = isBlueTick,
            hasPremiumEdit = hasPremiumEdit,
            verticalMenu = verticalMenu,
        )
    }
}

/** Mirrors Flutter's FittedBox(scaleDown): normal 12sp unless the fixed CTA cannot contain it. */
@Composable
private fun ScaleDownSingleLineText(text: String) {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
        val measurer = rememberTextMeasurer()
        val measured = measurer.measure(text = text, style = style, maxLines = 1)
        val density = LocalDensity.current
        val scale = with(density) {
            minOf(
                1f,
                maxWidth.toPx() / measured.size.width.coerceAtLeast(1),
                maxHeight.toPx() / measured.size.height.coerceAtLeast(1),
            )
        }
        Text(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .graphicsLayer(scaleX = scale, scaleY = scale),
        )
    }
}

@Composable
private fun PostAvatar(post: FeedPost) {
    val modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
    if (!post.authorAvatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = post.authorAvatarUrl,
            contentDescription = "تصویر نمایه ${post.authorFullName}",
            modifier = modifier,
            placeholder = painterResource(DesignSystemR.drawable.vista_default_avatar),
            error = painterResource(DesignSystemR.drawable.vista_default_avatar),
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            painter = painterResource(DesignSystemR.drawable.vista_default_avatar),
            contentDescription = "تصویر پیش‌فرض نمایه ${post.authorFullName}",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

private fun parseAspectRatio(ratioStr: String?): Float {
    if (ratioStr.isNullOrBlank() || ratioStr.equals("auto", ignoreCase = true)) return 1f
    val parts = ratioStr.split(":")
    if (parts.size == 2) {
        val w = parts[0].trim().toFloatOrNull()
        val h = parts[1].trim().toFloatOrNull()
        if (w != null && h != null && h > 0f) {
            return (w / h).coerceIn(0.55f, 1.91f)
        }
    }
    return ratioStr.toFloatOrNull()?.coerceIn(0.55f, 1.91f) ?: 1f
}

@Composable
private fun PostMedia(
    post: FeedPost,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onMediaClick: ((List<String>, Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val mediaUrl = post.videoThumbnailUrl ?: post.primaryImageUrl
    if (mediaUrl == null && post.videoUrl == null) return
    val ratio = parseAspectRatio(post.aspectRatio)
    var heartBurstKey by remember(post.id) { mutableStateOf(0) }
    var showHeartBurst by remember(post.id) { mutableStateOf(false) }
    val pagerState = if (post.imageUrls.size > 1) {
        rememberPagerState(pageCount = { post.imageUrls.size })
    } else {
        null
    }
    LaunchedEffect(heartBurstKey) {
        if (heartBurstKey > 0) {
            showHeartBurst = true
            delay(620)
            showHeartBurst = false
        }
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(post.id, post.isLiked, post.likeCount) {
                detectTapGestures(
                    onDoubleTap = {
                        heartBurstKey += 1
                        onDoubleTap()
                    },
                    onTap = {
                        if (post.videoUrl.isNullOrBlank() && onMediaClick != null) {
                            val images = post.imageUrls.ifEmpty { listOfNotNull(mediaUrl) }
                            if (images.isNotEmpty()) {
                                val page = pagerState?.currentPage ?: 0
                                onMediaClick(images, page)
                                return@detectTapGestures
                            }
                        }
                        onClick()
                    },
                )
            }
            .testTag(FeedTestTags.media(post.id)),
    ) {
        if (!post.videoUrl.isNullOrBlank()) {
            ir.coffevista.vista_native.features.feed.ui.components.VistaFeedVideoPlayer(
                videoUrl = post.videoUrl,
                thumbnailUrl = mediaUrl,
                aspectRatio = ratio,
                autoPlay = true,
                initiallyMuted = true,
                showMuteToggle = true,
                onVideoClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            val mediaHeight = (maxWidth / ratio).coerceIn(180.dp, 560.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mediaHeight),
            ) {
                if (post.imageUrls.size > 1 && pagerState != null) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        AsyncImage(
                            model = post.imageUrls[page],
                            contentDescription = "تصویر ${page + 1} از پست",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.62f),
                        contentColor = Color.White,
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${post.imageUrls.size}".toPersianDigits(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 12.sp,
                        )
                    }
                } else if (mediaUrl != null) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "تصویر پست",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showHeartBurst,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(animationSpec = tween(90)) + scaleIn(initialScale = 0.72f),
            exit = fadeOut(animationSpec = tween(260)) + scaleOut(targetScale = 1.18f),
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier
                    .size(86.dp)
                    .graphicsLayer { shadowElevation = 10f },
            )
        }
    }
}

@Composable
private fun HashtagMentionCaption(
    text: String,
    style: TextStyle,
    maxLines: Int,
    overflow: TextOverflow,
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onPostClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val entityStyle = SpanStyle(
        color = Color(0xFF1E88E5),
        fontWeight = FontWeight.Bold,
    )
    val annotated = remember(text) {
        buildAnnotatedString {
            val pattern = Regex("""(?<![^\s\n])@([\u0600-\u06FF\w_]+)|#([\u0600-\u06FF\w_]+)""")
            var cursor = 0
            pattern.findAll(text).forEach { match ->
                if (match.range.first > cursor) {
                    append(text.substring(cursor, match.range.first))
                }
                val start = length
                append(match.value)
                addStyle(entityStyle, start, length)
                if (match.value.startsWith("#")) {
                    addStringAnnotation(
                        tag = "HASHTAG",
                        annotation = match.value,
                        start = start,
                        end = length,
                    )
                } else if (match.value.startsWith("@")) {
                    addStringAnnotation(
                        tag = "MENTION",
                        annotation = match.value.removePrefix("@"),
                        start = start,
                        end = length,
                    )
                }
                cursor = match.range.last + 1
            }
            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        onClick = { offset ->
            val hashtag = annotated.getStringAnnotations(tag = "HASHTAG", start = offset, end = offset).firstOrNull()
            if (hashtag != null) {
                onHashtagClick(hashtag.item)
                return@ClickableText
            }
            val mention = annotated.getStringAnnotations(tag = "MENTION", start = offset, end = offset).firstOrNull()
            if (mention != null) {
                onMentionClick(mention.item)
                return@ClickableText
            }
            onPostClick()
        },
    )
}

@Composable
private fun PostMusicBubble(post: FeedPost) {
    val musicUrl = post.musicUrl?.trim().orEmpty()
    if (musicUrl.isEmpty()) return

    val context = LocalContext.current
    var isPlaying by remember(post.id) { mutableStateOf(false) }
    var player by remember(musicUrl) { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(musicUrl) {
        onDispose {
            player?.release()
            player = null
        }
    }

    Surface(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clickable {
                val p = player ?: ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Uri.parse(musicUrl)))
                    repeatMode = Player.REPEAT_MODE_OFF
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying = playing
                        }
                    })
                    prepare()
                }.also { player = it }

                if (p.isPlaying) {
                    p.pause()
                } else {
                    p.play()
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "توقف موسیقی" else "پخش موسیقی",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolveMusicTitle(post),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "موسیقی پست",
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PostActionRow(
    post: FeedPost,
    isOwnPost: Boolean,
    isBlueTick: Boolean = false,
    hasPremiumEdit: Boolean = false,
    verticalMenu: Boolean,
    onPostClick: () -> Unit,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onEditPost: () -> Unit,
    onDeletePost: () -> Unit,
    onReportPost: () -> Unit,
    onNotInterested: () -> Unit,
    onVisibilityChange: (Boolean?, Boolean?) -> Unit,
    onPremiumPrompt: (String) -> Unit = {},
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val actionTint = if (isDark) {
        Color.White.copy(alpha = 0.70f)
    } else {
        Color.Black.copy(alpha = 0.54f)
    }
    val heartTint = if (isDark) actionTint else Color.Black.copy(alpha = 0.87f)
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    var likeScale by remember { mutableStateOf(1f) }
    val animatedLikeScale by animateFloatAsState(
        targetValue = likeScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        finishedListener = { likeScale = 1f },
        label = "likeScale",
    )

    var bookmarkScale by remember { mutableStateOf(1f) }
    val animatedBookmarkScale by animateFloatAsState(
        targetValue = bookmarkScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        finishedListener = { bookmarkScale = 1f },
        label = "bookmarkScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeartIcon(
            filled = post.isLiked,
            inactiveColor = heartTint,
            modifier = Modifier
                .size(19.dp)
                .graphicsLayer(scaleX = animatedLikeScale, scaleY = animatedLikeScale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        likeScale = 1.28f
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLikeClick()
                    }
                ),
        )
        if (!post.hideLikeCount) {
            Text(
                text = post.likeCount.toString(),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .testTag(FeedTestTags.likeCount(post.id)),
                color = actionTint,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.width(14.dp))
        Image(
            painter = painterResource(DesignSystemR.drawable.vista_post_comment),
            contentDescription = "دیدگاه‌ها",
            colorFilter = ColorFilter.tint(actionTint),
            modifier = Modifier
                .size(19.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCommentClick
                ),
        )
        if (!post.hideCommentCount) {
            Text(
                text = post.commentCount.toString(),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .testTag(FeedTestTags.commentCount(post.id)),
                color = actionTint,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.width(14.dp))
        BookmarkIcon(
            filled = post.isSaved,
            inactiveColor = actionTint,
            modifier = Modifier
                .size(19.dp)
                .graphicsLayer(scaleX = animatedBookmarkScale, scaleY = animatedBookmarkScale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        bookmarkScale = 1.28f
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSaveClick()
                    },
                ),
        )
        Spacer(Modifier.width(14.dp))
        Image(
            painter = painterResource(DesignSystemR.drawable.vista_post_send),
            contentDescription = "ارسال",
            colorFilter = ColorFilter.tint(actionTint),
            modifier = Modifier.size(19.dp).clickable(onClick = onShareClick),
        )
        Spacer(Modifier.weight(1f))
        Box {
            MoreDots(color = actionTint, vertical = verticalMenu, modifier = Modifier.size(32.dp).clickable { menuExpanded = true }.padding(5.dp))
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (!isOwnPost) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("گزارش پست")
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onReportPost()
                        }
                    )
                }
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("کپی متن")
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        clipboard.setText(AnnotatedString(post.content.orEmpty()))
                    }
                )
                if (!isOwnPost) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.RemoveCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("کمتر نشان بده")
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onNotInterested()
                        }
                    )
                }
                if (isOwnPost || isBlueTick) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("حذف پست", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            confirmDelete = true
                        }
                    )
                }
                if (isBlueTick || hasPremiumEdit) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isBlueTick) Icons.Default.AdminPanelSettings else Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isBlueTick) "ویرایش ناظر" else "ویرایش پست")
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onEditPost()
                        }
                    )
                } else if (isOwnPost) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("ویرایش پست", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onPremiumPrompt("ویرایش پست")
                        }
                    )
                }
                if (isOwnPost) {
                    if (hasPremiumEdit) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (post.hideLikeCount) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (post.hideLikeCount) "نمایش تعداد لایک" else "مخفی کردن تعداد لایک")
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onVisibilityChange(!post.hideLikeCount, null)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (post.hideCommentCount) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (post.hideCommentCount) "نمایش تعداد دیدگاه" else "مخفی کردن تعداد دیدگاه")
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onVisibilityChange(null, !post.hideCommentCount)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("کنترل آمار لایک/کامنت", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB800),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onPremiumPrompt("مخفی‌سازی آمار لایک و کامنت")
                            }
                        )
                    }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("حذف پست") },
        text = { Text("آیا مطمئن هستید که می‌خواهید این پست را حذف کنید؟") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDeletePost() }) { Text("حذف", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } },
    )
}

@Composable
private fun FeedSkeletonList(
    topPadding: androidx.compose.ui.unit.Dp = 8.dp,
) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 1200,
                easing = androidx.compose.animation.core.LinearEasing,
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface
    val shimmerBrush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.6f),
            highlightColor.copy(alpha = 0.9f),
            baseColor.copy(alpha = 0.6f),
        ),
        start = androidx.compose.ui.geometry.Offset(x = translateAnim - 400f, y = translateAnim - 400f),
        end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim),
    )

    LazyColumn(
        contentPadding = PaddingValues(top = topPadding, bottom = 110.dp),
        userScrollEnabled = false,
    ) {
        items(4) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(shimmerBrush),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Box(
                            Modifier
                                .width(120.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(shimmerBrush),
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .width(82.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(shimmerBrush),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(shimmerBrush),
                )
            }
        }
    }
}

@Composable
private fun FeedInitialError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(FeedTestTags.InitialError),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("دریافت فید ناموفق بود", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Text(
            text = "تلاش دوباره",
            modifier = Modifier
                .padding(top = 16.dp)
                .clickable(onClick = onRetry),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FeedEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 72.dp)
            .testTag(FeedTestTags.Empty),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.vista_default_avatar),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                )
            }
        }
        Text(
            text = "هنوز پستی برای نمایش نیست",
            modifier = Modifier.padding(top = 16.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "بعداً دوباره سر بزنید.",
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun OfflineBanner() {
    Text(
        text = "نمایش نسخه ذخیره‌شده",
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEF6C00))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(FeedTestTags.Offline),
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
internal fun VerifiedMark(
    modifier: Modifier = Modifier,
    verificationType: String? = null,
) {
    ir.coffevista.vista_native.core.designsystem.component.VistaVerificationBadge(
        isVerified = true,
        verificationType = verificationType,
        size = 16.dp,
        modifier = modifier,
    )
}

@Composable
private fun VideoIndicator(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(30.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.62f),
        contentColor = Color.White,
    ) {
        Canvas(Modifier.padding(8.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.25f, size.height * 0.12f)
                lineTo(size.width * 0.88f, size.height * 0.5f)
                lineTo(size.width * 0.25f, size.height * 0.88f)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}

@Composable
private fun HeartIcon(
    filled: Boolean,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    val color = if (filled) Color(0xFFE53935) else inactiveColor
    Canvas(
        modifier.semantics {
            contentDescription = if (filled) "پسندیده شده" else "پسندیده نشده"
            role = Role.Image
        },
    ) {
        val path = Path().apply {
            moveTo(size.width * .5f, size.height * .9f)
            cubicTo(
                size.width * .42f, size.height * .82f,
                size.width * .08f, size.height * .58f,
                size.width * .08f, size.height * .32f,
            )
            cubicTo(
                size.width * .08f, size.height * .08f,
                size.width * .37f, size.height * .02f,
                size.width * .5f, size.height * .22f,
            )
            cubicTo(
                size.width * .63f, size.height * .02f,
                size.width * .92f, size.height * .08f,
                size.width * .92f, size.height * .32f,
            )
            cubicTo(
                size.width * .92f, size.height * .58f,
                size.width * .58f, size.height * .82f,
                size.width * .5f, size.height * .9f,
            )
            close()
        }
        if (filled) drawPath(path, color) else {
            drawPath(path, color, style = Stroke(width = size.minDimension * .09f))
        }
    }
}

@Composable
private fun BookmarkIcon(
    filled: Boolean,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    val color = if (filled) MaterialTheme.colorScheme.primary else inactiveColor
    Canvas(
        modifier.semantics {
            contentDescription = if (filled) "ذخیره شده" else "ذخیره نشده"
            role = Role.Image
        },
    ) {
        val path = Path().apply {
            moveTo(size.width * .22f, size.height * .08f)
            lineTo(size.width * .78f, size.height * .08f)
            lineTo(size.width * .78f, size.height * .92f)
            lineTo(size.width * .5f, size.height * .7f)
            lineTo(size.width * .22f, size.height * .92f)
            close()
        }
        if (filled) drawPath(path, color) else {
            drawPath(path, color, style = Stroke(width = size.minDimension * .09f))
        }
    }
}

@Composable
private fun MoreDots(color: Color, vertical: Boolean = false, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "گزینه‌های بیشتر" }) {
        val radius = size.minDimension * .08f
        drawCircle(color, radius, if (vertical) Offset(size.width * .5f, size.height * .22f) else Offset(size.width * .22f, size.height * .5f))
        drawCircle(color, radius, Offset(size.width * .5f, size.height * .5f))
        drawCircle(color, radius, if (vertical) Offset(size.width * .5f, size.height * .78f) else Offset(size.width * .78f, size.height * .5f))
    }
}

@Composable
private fun NotificationBell(
    unreadCount: Int = 0,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val iconColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 22.dp),
                        onClick = onClick,
                    )
                } else Modifier
            )
            .semantics { contentDescription = "اعلان‌ها" },
        contentAlignment = Alignment.Center,
    ) {
        VistaBadgedBox(
            badge = {
                if (unreadCount > 0) {
                    VistaNotificationBadge(
                        count = unreadCount,
                        borderColor = surfaceColor,
                        modifier = Modifier.testTag("feed-notification-badge"),
                    )
                }
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

private fun relativeTime(raw: String): String = feedRelativeTime(raw)

private fun resolveMusicTitle(post: FeedPost): String {
    val direct = post.musicTitle?.trim().orEmpty()
    if (direct.isNotEmpty()) return direct

    val url = post.musicUrl?.trim().orEmpty()
    if (url.isEmpty()) return "موزیک"

    val lastSegment = runCatching {
        Uri.parse(url).pathSegments.lastOrNull()
    }.getOrNull().takeUnless { it.isNullOrBlank() } ?: url.substringAfterLast('/')
    val withoutExtension = lastSegment.replace(Regex("""\.[^.]+$"""), "")
    val normalized = withoutExtension
        .replace(Regex("""^[^_]+_[0-9]+_"""), "")
        .replace('_', ' ')
        .trim()
    return normalized.ifEmpty { "موزیک" }
}

private fun String.toPersianDigits(): String = buildString(length) {
    this@toPersianDigits.forEach { character ->
        append(
            when (character) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> character
            }
        )
    }
}

internal object FeedTestTags {
    const val AppBar = "feed-app-bar"
    const val Tabs = "feed-tabs"
    const val PullToRefresh = "feed-pull-to-refresh"
    const val List = "feed-list"
    const val Offline = "feed-offline"
    const val Empty = "feed-empty"
    const val Appending = "feed-appending"
    const val AppendError = "feed-append-error"
    const val InitialError = "feed-initial-error"
    const val EndReached = "feed-end-reached"
    fun post(id: String) = "feed-post-$id"
    fun media(id: String) = "feed-media-$id"
    fun author(userId: String) = "feed-author-$userId"
    fun tab(kind: FeedKind) = "feed-tab-${kind.name.lowercase()}"
    fun follow(id: String) = "feed-follow-$id"
    fun likeCount(id: String) = "feed-like-count-$id"
    fun commentCount(id: String) = "feed-comment-count-$id"
}

@Composable
private fun FeedPostUploadBanner(
    uploadState: PostUploadState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = uploadState.isUploading || uploadState.errorMessage != null || uploadState.isSuccess,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        androidx.compose.material3.Surface(
            color = if (uploadState.errorMessage != null) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (uploadState.isUploading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                progress = { uploadState.progress },
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else if (uploadState.isSuccess) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        androidx.compose.material3.Text(
                            text = uploadState.statusMessage ?: uploadState.errorMessage ?: "در حال پردازش…",
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }

                    if (uploadState.errorMessage != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = onRetry) {
                                androidx.compose.material3.Text("تلاش مجدد", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                            androidx.compose.material3.IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                androidx.compose.material3.Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (uploadState.isUploading) {
                    LinearProgressIndicator(
                        progress = { uploadState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}

