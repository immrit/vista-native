package ir.coffevista.vista_native.features.feed.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.features.feed.data.FeedPost

@Composable
fun PostDetailScreen(
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    viewModel: PostDetailViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PostDetailContent(
        uiState = uiState,
        onBack = onBack,
        onAuthorClick = onAuthorClick,
    )
}

@Composable
internal fun PostDetailContent(
    uiState: PostDetailUiState,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        PostDetailAppBar(onBack)
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                PostDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(30.dp),
                        strokeWidth = 2.5.dp,
                    )
                }
                is PostDetailUiState.Error -> PostUnavailable(
                    message = state.message,
                    onBack = onBack,
                )
                is PostDetailUiState.Content -> ReadOnlyPostDetail(
                    state = state,
                    onAuthorClick = { onAuthorClick(state.post.userId) },
                )
            }
        }
    }
}

@Composable
private fun PostDetailAppBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(
            text = "جزئیات پست",
            modifier = Modifier.align(Alignment.Center),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        BackArrow(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 12.dp)
                .size(40.dp)
                .clickable(onClick = onBack)
                .padding(9.dp)
                .testTag(PostDetailTestTags.Back),
        )
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun ReadOnlyPostDetail(
    state: PostDetailUiState.Content,
    onAuthorClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(PostDetailTestTags.Content),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp),
    ) {
        if (state.isStale) {
            item {
                Text(
                    text = "نمایش نسخه ذخیره‌شده",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color(0xFFEF6C00))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        item {
            VistaFeedPostCard(
                post = state.post,
                onPostClick = {},
                onAuthorClick = onAuthorClick,
                modifier = Modifier.testTag(PostDetailTestTags.Author),
            )
        }
        item {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            ReadOnlyCommentsSummary(state.post)
        }
        if (state.isRefreshing) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyCommentsSummary(post: FeedPost) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(PostDetailTestTags.Counts),
    ) {
        Text(
            text = "دیدگاه‌ها",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (post.hideCommentCount) {
                "تعداد دیدگاه‌ها نمایش داده نمی‌شود."
            } else if (post.commentCount == 0L) {
                "هنوز دیدگاهی ثبت نشده است."
            } else {
                "${post.commentCount} دیدگاه"
            },
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun PostUnavailable(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(PostDetailTestTags.Error),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("×", color = MaterialTheme.colorScheme.error, fontSize = 42.sp)
        }
        Text(
            text = "این پست در دسترس نیست",
            modifier = Modifier.padding(top = 18.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 7.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Text(
            text = "بازگشت",
            modifier = Modifier
                .padding(top = 18.dp)
                .clickable(onClick = onBack),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BackArrow(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onBackground
    Canvas(modifier) {
        drawLine(
            color,
            Offset(size.width * .2f, size.height * .5f),
            Offset(size.width * .82f, size.height * .5f),
            strokeWidth = size.minDimension * .09f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(size.width * .2f, size.height * .5f),
            Offset(size.width * .48f, size.height * .22f),
            strokeWidth = size.minDimension * .09f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(size.width * .2f, size.height * .5f),
            Offset(size.width * .48f, size.height * .78f),
            strokeWidth = size.minDimension * .09f,
            cap = StrokeCap.Round,
        )
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
