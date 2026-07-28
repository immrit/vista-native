package ir.coffevista.vista_native.features.feed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.component.*
import ir.coffevista.vista_native.features.feed.data.FeedPost
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    viewModel: FeedViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    FeedScreenContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onPostClick = onPostClick,
        onAuthorClick = onAuthorClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedScreenContent(
    uiState: FeedUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit = {},
) {
    VistaScaffold(
        topBar = {
            VistaTopAppBar(title = "فید")
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is FeedUiState.Loading -> {
                    VistaLoadingState(
                        label = "در حال بارگذاری",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is FeedUiState.Error -> {
                    VistaErrorState(
                        title = "خطا",
                        message = state.message,
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is FeedUiState.Content -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(FeedTestTags.PullToRefresh),
                    ) {
                        FeedContent(
                            state = state,
                            onLoadMore = onLoadMore,
                            onPostClick = onPostClick,
                            onAuthorClick = onAuthorClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FeedContent(
    state: FeedUiState.Content,
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()

    // Infinite scroll logic
    LaunchedEffect(listState, state.posts.size, state.hasMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            state.posts.isNotEmpty() &&
                state.hasMore &&
                lastVisibleItem >= totalItems - 5
        }
        .distinctUntilChanged()
        .filter { it }
        .collect {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(FeedTestTags.List),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (state.isOffline || state.isStale) {
            item(key = "offline-banner") {
                Text(
                    text = "نمایش نسخه ذخیره‌شده",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag(FeedTestTags.Offline),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        items(state.posts, key = { it.id }) { post ->
            FeedPostItem(
                post = post,
                onClick = { onPostClick(post.id) },
                onAuthorClick = { onAuthorClick(post.userId) },
            )
            VistaDivider()
        }

        if (state.posts.isEmpty() && !state.isRefreshing) {
            item(key = "empty") {
                VistaEmptyState(
                    title = "فید خالی است",
                    message = "هنوز پستی برای نمایش وجود ندارد.",
                    modifier = Modifier
                        .fillParentMaxSize()
                        .testTag(FeedTestTags.Empty),
                )
            }
        }

        if (state.isAppending) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .testTag(FeedTestTags.Appending),
                    )
                }
            }
        }
        if (state.appendError != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.appendError,
                        modifier = Modifier.testTag(FeedTestTags.AppendError),
                        color = MaterialTheme.colorScheme.error,
                    )
                    VistaButton(
                        variant = VistaButtonVariant.Text,
                        onClick = onLoadMore,
                    ) {
                        Text("تلاش دوباره")
                    }
                }
            }
        }
        if (!state.hasMore && state.posts.isNotEmpty()) {
            item(key = "end") {
                Text(
                    text = "به پایان فید رسیدید",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag(FeedTestTags.EndReached),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FeedPostItem(
    post: FeedPost,
    onClick: () -> Unit,
    onAuthorClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(FeedTestTags.post(post.id))
            .padding(16.dp)
    ) {
        // Author Info Row
        Row(
            modifier = Modifier
                .clickable(onClick = onAuthorClick)
                .testTag(FeedTestTags.author(post.userId)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VistaAvatar(
                displayName = post.authorFullName
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = post.authorFullName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                if (post.authorUsername != null) {
                    Text(
                        text = "@${post.authorUsername}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        if (!post.content.isNullOrEmpty()) {
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        post.primaryImageUrl?.let { mediaUrl ->
            AsyncImage(
                model = mediaUrl,
                contentDescription = if (post.videoUrl != null) {
                    "تصویر بندانگشتی ویدیو"
                } else {
                    "تصویر پست"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag(FeedTestTags.media(post.id)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (post.hideLikeCount) "پسندها —" else "${post.likeCount} پسند",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (post.hideCommentCount) "دیدگاه‌ها —" else "${post.commentCount} دیدگاه",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal object FeedTestTags {
    const val PullToRefresh = "feed-pull-to-refresh"
    const val List = "feed-list"
    const val Offline = "feed-offline"
    const val Empty = "feed-empty"
    const val Appending = "feed-appending"
    const val AppendError = "feed-append-error"
    const val EndReached = "feed-end-reached"
    fun post(id: String) = "feed-post-$id"
    fun media(id: String) = "feed-media-$id"
    fun author(userId: String) = "feed-author-$userId"
}
