package ir.coffevista.vista_native.features.feed.ui

import android.content.res.Configuration
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR
import ir.coffevista.vista_native.features.feed.data.FeedKind
import ir.coffevista.vista_native.features.feed.data.FeedPost
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    viewModel: FeedViewModel,
    commentsViewModel: CommentsViewModel,
    onNotificationClick: () -> Unit = {},
    onStoryClick: () -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    onOpenStoryPlayer: (Int) -> Unit = {},
    onCreateStory: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedKind by viewModel.selectedKind.collectAsStateWithLifecycle()
    val viewerUserId by viewModel.viewerUserId.collectAsStateWithLifecycle()
    var showCommentsForPostId by remember { mutableStateOf<String?>(null) }

    val storyUsers by viewModel.activeStoryUsers.collectAsStateWithLifecycle()

    FeedScreenContent(
        uiState = uiState,
        viewerUserId = viewerUserId,
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
        onShareClick = { post ->
            val shareText = buildString {
                post.content?.takeIf(String::isNotBlank)?.let(::append)
                if (isNotEmpty()) append("\n")
                append("https://coffevista.ir/posts/${post.id}")
            }
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    },
                    "اشتراک‌گذاری پست",
                ),
            )
            viewModel.trackEvent(post.id, "share")
        },
        onDeletePost = viewModel::deletePost,
        onReportPost = viewModel::reportPost,
        onNotInterested = viewModel::markNotInterested,
        onVisibilityChange = viewModel::updateEngagementVisibility,
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
            viewModel = commentsViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedScreenContent(
    uiState: FeedUiState,
    viewerUserId: String? = null,
    storyUsers: List<ir.coffevista.vista_native.features.stories.domain.StoryUser> = emptyList(),
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onLikeClick: (String, Boolean, Long) -> Unit = { _, _, _ -> },
    onSaveClick: (String, Boolean) -> Unit = { _, _ -> },
    onCommentClick: (String) -> Unit = {},
    onShareClick: (FeedPost) -> Unit = {},
    onDeletePost: (String) -> Unit = {},
    onReportPost: (FeedPost, String) -> Unit = { _, _ -> },
    onNotInterested: (String) -> Unit = {},
    onVisibilityChange: (FeedPost, Boolean?, Boolean?) -> Unit = { _, _, _ -> },
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
        FeedAppBar(onNotificationClick)
        FeedTabs(selectedKind = selectedKind, onKindSelected = onKindSelected)
        ir.coffevista.vista_native.features.stories.ui.tray.StoryTray(
            storyUsers = storyUsers,
            currentUserId = viewerUserId.orEmpty(),
            currentUserAvatar = null,
            onOpenStoryPlayer = onOpenStoryPlayer,
            onCreateStory = onCreateStory,
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
                        onDeletePost = onDeletePost,
                        onReportPost = onReportPost,
                        onNotInterested = onNotInterested,
                        onVisibilityChange = onVisibilityChange,
                    )
                }
            }
            Surface(
                onClick = onCreatePostClick,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 112.dp).size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 10.dp,
            ) { Box(contentAlignment = Alignment.Center) { Text("+", color = MaterialTheme.colorScheme.onPrimary, fontSize = 32.sp) } }
        }
    }
}

@Composable
private fun FeedAppBar(onNotificationClick: () -> Unit) {
    val dark = (LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
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
                    .height(46.dp)
                    .testTag(FeedTestTags.tab(kind)),
                text = {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = if (kind == selectedKind) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
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
    onDeletePost: (String) -> Unit,
    onReportPost: (FeedPost, String) -> Unit,
    onNotInterested: (String) -> Unit,
    onVisibilityChange: (FeedPost, Boolean?, Boolean?) -> Unit,
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
                onDeletePost = { onDeletePost(post.id) },
                onReportPost = { reason -> onReportPost(post, reason) },
                onNotInterested = { onNotInterested(post.id) },
                onVisibilityChange = { hideLike, hideComment -> onVisibilityChange(post, hideLike, hideComment) },
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
    onDeletePost: () -> Unit = {},
    onReportPost: (String) -> Unit = {},
    onNotInterested: () -> Unit = {},
    onVisibilityChange: (Boolean?, Boolean?) -> Unit = { _, _ -> },
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
                        Text(
                            text = post.authorUsername?.takeIf { it.isNotBlank() } ?: post.authorFullName,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (post.authorIsVerified) {
                            VerifiedMark(Modifier.padding(start = 4.dp))
                        }
                        Text(
                            text = " • ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
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

        post.content?.takeIf(String::isNotBlank)?.let { caption ->
            Text(
                text = caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .then(if (verticalMenu) Modifier.testTag(PostDetailTestTags.Caption) else Modifier),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 6,
                overflow = TextOverflow.Ellipsis,
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
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .then(if (verticalMenu) Modifier.testTag(PostDetailTestTags.Media) else Modifier),
        )
        Spacer(Modifier.height(4.dp))
        PostActionRow(
            post = post,
            onPostClick = onPostClick,
            onLikeClick = { onLikeClick(post.id, post.isLiked, post.likeCount) },
            onSaveClick = { onSaveClick(post.id, post.isSaved) },
            onCommentClick = { onCommentClick(post.id) },
            onShareClick = onShareClick,
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
    modifier: Modifier = Modifier,
) {
    val mediaUrl = post.videoThumbnailUrl ?: post.primaryImageUrl
    if (mediaUrl == null && post.videoUrl == null) return
    val ratio = post.aspectRatio?.toFloatOrNull()?.coerceIn(0.55f, 1.91f) ?: 1f
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
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
                if (mediaUrl != null) {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = "تصویر پست",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                if (post.imageUrls.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    contentColor = Color.White,
                ) {
                    Text(
                        text = "۱/${post.imageUrls.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 12.sp,
                    )
                }
            }
        }
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
    onDeletePost: () -> Unit,
    onReportPost: (String) -> Unit,
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onLikeClick() }
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
                if (!isOwnPost) DropdownMenuItem(text = { Text("گزارش پست") }, onClick = { menuExpanded = false; onReportPost("spam") })
                DropdownMenuItem(text = { Text("کپی متن") }, onClick = { menuExpanded = false; clipboard.setText(AnnotatedString(post.content.orEmpty())) })
                if (!isOwnPost) DropdownMenuItem(text = { Text("کمتر نشان بده") }, onClick = { menuExpanded = false; onNotInterested() })
                if (isOwnPost) {
                    DropdownMenuItem(text = { Text(if (post.hideLikeCount) "نمایش تعداد لایک" else "مخفی کردن تعداد لایک") }, onClick = { menuExpanded = false; onVisibilityChange(!post.hideLikeCount, null) })
                    DropdownMenuItem(text = { Text(if (post.hideCommentCount) "نمایش تعداد دیدگاه" else "مخفی کردن تعداد دیدگاه") }, onClick = { menuExpanded = false; onVisibilityChange(null, !post.hideCommentCount) })
                    DropdownMenuItem(text = { Text("حذف پست", color = MaterialTheme.colorScheme.error) }, onClick = { menuExpanded = false; confirmDelete = true })
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
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Box(
                            Modifier
                                .width(120.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .width(82.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
private fun VerifiedMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(Color(0xFF2196F3)),
        contentAlignment = Alignment.Center,
    ) {
        Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
private fun NotificationBell(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onBackground
    Canvas(modifier.semantics { contentDescription = "اعلان‌ها" }) {
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
}

private fun relativeTime(raw: String): String = feedRelativeTime(raw)

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
