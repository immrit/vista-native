package ir.coffevista.vista_native.features.feed.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.ClickableText
import ir.coffevista.vista_native.features.feed.ui.components.EditPostDialog
import ir.coffevista.vista_native.features.feed.ui.components.PostModerationBanner
import ir.coffevista.vista_native.features.feed.ui.components.ReportReasonDialog
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiText
import ir.coffevista.vista_native.core.designsystem.component.VistaFloatingActionButton
import androidx.compose.material.icons.filled.Add
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
import kotlinx.coroutines.delay
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
) {
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

    FeedScreenContent(
        uiState = uiState,
        viewerUserId = viewerUserId,
        currentUserAvatar = ownProfile?.avatarUrl,
        unreadNotificationCount = unreadNotificationCount,
        selectedKind = selectedKind,
        storyUsers = storyUsers,
        onKindSelected = viewModel::selectKind,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onPostClick = onPostClick,
        onAuthorClick = onAuthorClick,
        onLikeClick = viewModel::toggleLike,
        onSaveClick = viewModel::toggleSave,
        onCommentClick = { showCommentsForPostId = it },
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
    )

    showCommentsForPostId?.let { postId ->
        CommentsBottomSheet(
            postId = postId,
            onDismissRequest = { showCommentsForPostId = null },
            viewModel = commentsViewModel,
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
        EditPostDialog(
            initialContent = target.content.orEmpty(),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedScreenContent(
    uiState: FeedUiState,
    viewerUserId: String? = null,
    currentUserAvatar: String? = null,
    storyUsers: List<ir.coffevista.vista_native.features.stories.domain.StoryUser> = emptyList(),
    unreadNotificationCount: Int = 0,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
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
    selectedKind: FeedKind = FeedKind.Explore,
    onKindSelected: (FeedKind) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FeedAppBar(
            onNotificationClick = onNotificationClick,
            unreadNotificationCount = unreadNotificationCount,
        )
        FeedTabs(selectedKind = selectedKind, onKindSelected = onKindSelected)
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
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                FeedUiState.Loading -> FeedSkeletonList()
                is FeedUiState.Error -> FeedInitialError(
                    message = state.message,
                    onRetry = onRefresh,
                )
                is FeedUiState.Content -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(FeedTestTags.PullToRefresh),
                ) {
                    FeedContent(
                        state = state,
                        viewerUserId = viewerUserId,
                        selectedKind = selectedKind,
                        onLoadMore = onLoadMore,
                        onPostClick = onPostClick,
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
                    )
                }
            }
            VistaFloatingActionButton(
                onClick = onCreatePostClick,
                contentDescription = "ارسال پست جدید",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp),
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
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(horizontal = 16.dp)
                .size(40.dp).clickable(onClick = onNotificationClick).padding(8.dp),
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
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
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
) {
    val listState = rememberLazyListState()
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
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
        contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp + navigationBarInset),
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
                showFollowState = selectedKind == FeedKind.Explore,
                onPostClick = { onPostClick(post.id) },
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
    verticalMenu: Boolean = false,
    onPostClick: () -> Unit,
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
                            VerifiedMark(Modifier.padding(start = 4.dp))
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
            onClick = onPostClick,
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
            isOwnPost = isOwnPost,
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

@Composable
private fun PostMedia(
    post: FeedPost,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaUrl = post.videoThumbnailUrl ?: post.primaryImageUrl
    if (mediaUrl == null && post.videoUrl == null) return
    val ratio = post.aspectRatio?.toFloatOrNull()?.coerceIn(0.55f, 1.91f) ?: 1f
    var heartBurstKey by remember(post.id) { mutableStateOf(0) }
    var showHeartBurst by remember(post.id) { mutableStateOf(false) }
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
                    onTap = { onClick() },
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
            val mediaHeight = (maxWidth / ratio).coerceAtMost(280.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mediaHeight),
            ) {
                if (post.imageUrls.size > 1) {
                    val pagerState = rememberPagerState(pageCount = { post.imageUrls.size })
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
    val player = remember(musicUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(musicUrl)))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
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

    Surface(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clickable {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSaveClick
                )
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
                        text = { Text("گزارش پست") },
                        onClick = {
                            menuExpanded = false
                            onReportPost()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("کپی متن") },
                    onClick = {
                        menuExpanded = false
                        clipboard.setText(AnnotatedString(post.content.orEmpty()))
                    }
                )
                if (!isOwnPost) {
                    DropdownMenuItem(
                        text = { Text("کمتر نشان بده") },
                        onClick = {
                            menuExpanded = false
                            onNotInterested()
                        }
                    )
                }
                if (isOwnPost) {
                    DropdownMenuItem(
                        text = { Text("ویرایش پست") },
                        onClick = {
                            menuExpanded = false
                            onEditPost()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (post.hideLikeCount) "نمایش تعداد لایک" else "مخفی کردن تعداد لایک") },
                        onClick = {
                            menuExpanded = false
                            onVisibilityChange(!post.hideLikeCount, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (post.hideCommentCount) "نمایش تعداد دیدگاه" else "مخفی کردن تعداد دیدگاه") },
                        onClick = {
                            menuExpanded = false
                            onVisibilityChange(null, !post.hideCommentCount)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف پست", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            confirmDelete = true
                        }
                    )
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
private fun FeedSkeletonList() {
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
        contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp),
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
internal fun VerifiedMark(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(16.dp)
            .semantics {
                contentDescription = "نشان تأیید"
                role = Role.Image
            },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension * 0.50f
        val inner = size.minDimension * 0.43f
        val badge = Path().apply {
            for (i in 0 until 24) {
                val angle = -Math.PI / 2.0 + i * Math.PI / 12.0
                val radius = if (i % 2 == 0) outer else inner
                val point = Offset(
                    x = center.x + kotlin.math.cos(angle).toFloat() * radius,
                    y = center.y + kotlin.math.sin(angle).toFloat() * radius,
                )
                if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
        drawPath(badge, Color(0xFF2196F3))
        val check = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.43f, size.height * 0.67f)
            lineTo(size.width * 0.74f, size.height * 0.34f)
        }
        drawPath(
            path = check,
            color = Color.White,
            style = Stroke(width = size.minDimension * 0.12f, cap = StrokeCap.Round),
        )
    }
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
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onBackground
    Box(modifier = modifier.semantics { contentDescription = "اعلان‌ها" }) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(size.width * .23f, size.height * .18f),
                size = Size(size.width * .54f, size.height * .62f),
                style = Stroke(width = size.minDimension * .085f, cap = StrokeCap.Round),
            )
            drawLine(
                color,
                Offset(size.width * .23f, size.height * .5f),
                Offset(size.width * .18f, size.height * .78f),
                strokeWidth = size.minDimension * .085f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color,
                Offset(size.width * .77f, size.height * .5f),
                Offset(size.width * .82f, size.height * .78f),
                strokeWidth = size.minDimension * .085f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color,
                Offset(size.width * .18f, size.height * .78f),
                Offset(size.width * .82f, size.height * .78f),
                strokeWidth = size.minDimension * .085f,
                cap = StrokeCap.Round,
            )
            drawCircle(color, size.minDimension * .07f, Offset(size.width * .5f, size.height * .91f))
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = (-6).dp)
                    .background(Color(0xFFF44336), CircleShape)
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
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

