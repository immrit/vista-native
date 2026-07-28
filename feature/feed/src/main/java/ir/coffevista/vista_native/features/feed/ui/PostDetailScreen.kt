package ir.coffevista.vista_native.features.feed.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.component.*
import ir.coffevista.vista_native.features.feed.data.FeedPost

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    onBack: () -> Unit,
    viewModel: PostDetailViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PostDetailContent(uiState = uiState, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostDetailContent(
    uiState: PostDetailUiState,
    onBack: () -> Unit,
) {
    VistaScaffold(
        topBar = {
            TopAppBar(
                title = { Text("جزئیات پست") },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.testTag(PostDetailTestTags.Back),
                    ) {
                        Text("بازگشت")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                is PostDetailUiState.Loading -> {
                    VistaLoadingState(label = "در حال بارگذاری...")
                }
                is PostDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .testTag(PostDetailTestTags.Error),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "پست در حافظه موجود نیست",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        VistaButton(onClick = onBack) {
                            Text("بازگشت")
                        }
                    }
                }
                is PostDetailUiState.Content -> {
                    ReadOnlyPostDetail(post = state.post)
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyPostDetail(post: FeedPost) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(PostDetailTestTags.Content),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag(PostDetailTestTags.Author),
            ) {
                VistaAvatar(displayName = post.authorFullName)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = post.authorFullName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    post.authorUsername?.let { username ->
                        Text(
                            text = "@$username",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (!post.content.isNullOrEmpty()) {
            item {
                Text(
                    text = post.content,
                    modifier = Modifier.testTag(PostDetailTestTags.Caption),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        post.primaryImageUrl?.let { mediaUrl ->
            item {
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
                        .testTag(PostDetailTestTags.Media),
                    contentScale = ContentScale.Crop,
                )
                if (post.videoUrl != null) {
                    VistaBadge(
                        label = "ویدیو",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        if (post.videoUrl != null && post.videoThumbnailUrl == null) {
            item {
                VistaSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .testTag(PostDetailTestTags.Media),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "ویدیو",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.testTag(PostDetailTestTags.Counts),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = if (post.hideLikeCount) "پسندها —" else "${post.likeCount} پسند",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = if (post.hideCommentCount) {
                        "دیدگاه‌ها —"
                    } else {
                        "${post.commentCount} دیدگاه"
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

internal object PostDetailTestTags {
    const val Back = "post-detail-back"
    const val Content = "post-detail-content"
    const val Error = "post-detail-error"
    const val Author = "post-detail-author"
    const val Caption = "post-detail-caption"
    const val Media = "post-detail-media"
    const val Counts = "post-detail-counts"
}
