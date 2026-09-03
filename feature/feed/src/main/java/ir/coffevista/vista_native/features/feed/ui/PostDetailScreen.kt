package ir.coffevista.vista_native.features.feed.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.features.feed.ui.components.EditPostDialog
import ir.coffevista.vista_native.features.feed.ui.components.ReportReasonDialog
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR

@Composable
fun PostDetailScreen(
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onAppealClick: (String) -> Unit = {},
    onSendDirectMessage: (FeedPost) -> Unit = {},
    viewModel: PostDetailViewModel,
    commentsViewModel: CommentsViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val commentsState by commentsViewModel.uiState.collectAsStateWithLifecycle()
    val currentPost = (uiState as? PostDetailUiState.Content)?.post
    val postId = currentPost?.id
    var showShareSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isReporting by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(postId) { postId?.let(commentsViewModel::loadComments) }
    PostDetailContent(
        uiState = uiState,
        commentsState = commentsState,
        onBack = onBack,
        onAuthorClick = onAuthorClick,
        onLikeClick = { _, isLiked, count -> viewModel.toggleLike(isLiked, count) },
        onSaveClick = { _, isSaved -> viewModel.toggleSave(isSaved) },
        onShareClick = { showShareSheet = true },
        onEditPost = { showEditDialog = true },
        onDeletePost = viewModel::deletePost,
        onReportPost = { showReportDialog = true },
        onNotInterested = viewModel::markNotInterested,
        onVisibilityChange = viewModel::updateEngagementVisibility,
        onHashtagClick = onHashtagClick,
        onMentionClick = onMentionClick,
        onAppealClick = onAppealClick,
        onLoadMoreComments = commentsViewModel::loadMore,
        onLoadReplies = commentsViewModel::loadReplies,
        onRetryComments = commentsViewModel::retry,
        onReplyComment = commentsViewModel::setReplyingTo,
        onEditComment = { comment, newContent -> commentsViewModel.updateComment(comment.id, newContent) },
        onDeleteComment = commentsViewModel::deleteComment,
        onReportComment = { commentsViewModel.reportComment(it, "گزارش تخلف") },
        canEdit = commentsViewModel::canEdit,
        canDelete = { commentsViewModel.canDelete(it, (uiState as? PostDetailUiState.Content)?.post?.userId) },
        canReport = commentsViewModel::canReport,
        isCommentAuthor = { commentsViewModel.isCurrentUser(it.authorUserId) },
        onSubmitComment = { draft ->
            commentsViewModel.submitComment(
                content = draft.content,
                mentionedUserIds = draft.mentionedUserIds,
            )
        },
        onCancelReplyOrEdit = {
            commentsViewModel.setReplyingTo(null)
            commentsViewModel.setEditingComment(null)
        },
    )
    currentPost?.takeIf { showShareSheet }?.let { post ->
        PostShareBottomSheet(
            post = post,
            onDismiss = { showShareSheet = false },
            onShared = viewModel::trackShare,
            onSendDirectMessage = onSendDirectMessage,
        )
    }
    if (showReportDialog) {
        ReportReasonDialog(
            isSubmitting = isReporting,
            onDismiss = { if (!isReporting) showReportDialog = false },
            onSubmit = { reason, details ->
                isReporting = true
                viewModel.reportPost(reason, details) {
                    isReporting = false
                    showReportDialog = false
                }
            },
        )
    }
    if (showEditDialog) {
        EditPostDialog(
            initialContent = currentPost?.content.orEmpty(),
            isSubmitting = isEditing,
            onDismiss = { if (!isEditing) showEditDialog = false },
            onConfirm = { newContent ->
                isEditing = true
                viewModel.updatePostContent(newContent) {
                    isEditing = false
                    showEditDialog = false
                }
            },
        )
    }
}

@Composable
internal fun PostDetailContent(
    uiState: PostDetailUiState,
    commentsState: CommentsUiState = CommentsUiState(),
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onLikeClick: (String, Boolean, Long) -> Unit = { _, _, _ -> },
    onSaveClick: (String, Boolean) -> Unit = { _, _ -> },
    onShareClick: (FeedPost) -> Unit = {},
    onEditPost: () -> Unit = {},
    onDeletePost: () -> Unit = {},
    onReportPost: () -> Unit = {},
    onNotInterested: () -> Unit = {},
    onVisibilityChange: (Boolean?, Boolean?) -> Unit = { _, _ -> },
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onAppealClick: (String) -> Unit = {},
    onLoadMoreComments: () -> Unit = {},
    onLoadReplies: (String) -> Unit = {},
    onRetryComments: () -> Unit = {},
    onReplyComment: (ir.coffevista.vista_native.features.feed.data.Comment) -> Unit = {},
    onEditComment: (ir.coffevista.vista_native.features.feed.data.Comment, String) -> Unit = { _, _ -> },
    onDeleteComment: (String) -> Unit = {},
    onReportComment: (String) -> Unit = {},
    canEdit: (ir.coffevista.vista_native.features.feed.data.Comment) -> Boolean = { false },
    canDelete: (ir.coffevista.vista_native.features.feed.data.Comment) -> Boolean = { false },
    canReport: (ir.coffevista.vista_native.features.feed.data.Comment) -> Boolean = { false },
    isCommentAuthor: (ir.coffevista.vista_native.features.feed.data.Comment) -> Boolean = { false },
    onSubmitComment: (CommentDraft) -> Unit = {},
    onCancelReplyOrEdit: () -> Unit = {},
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
                is PostDetailUiState.Content -> {
                    ReadOnlyPostDetail(
                        state = state,
                        onAuthorClick = { onAuthorClick(state.post.userId) },
                        onCommentAuthorClick = onAuthorClick,
                        onLikeClick = onLikeClick,
                        onSaveClick = onSaveClick,
                        onShareClick = onShareClick,
                        onEditPost = onEditPost,
                        onDeletePost = onDeletePost,
                        onReportPost = onReportPost,
                        onNotInterested = onNotInterested,
                        onVisibilityChange = onVisibilityChange,
                        onHashtagClick = onHashtagClick,
                        onMentionClick = onMentionClick,
                        onAppealClick = onAppealClick,
                        commentsState = commentsState,
                        onLoadMoreComments = onLoadMoreComments,
                        onLoadReplies = onLoadReplies,
                        onRetryComments = onRetryComments,
                        onReplyComment = onReplyComment,
                        onEditComment = onEditComment,
                        onDeleteComment = onDeleteComment,
                        onReportComment = onReportComment,
                        canEdit = canEdit,
                        canDelete = canDelete,
                        canReport = canReport,
                        isCommentAuthor = isCommentAuthor,
                        onSubmitComment = onSubmitComment,
                        onCancelReplyOrEdit = onCancelReplyOrEdit,
                    )
                }
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
    onCommentAuthorClick: (String) -> Unit,
    onLikeClick: (String, Boolean, Long) -> Unit,
    onSaveClick: (String, Boolean) -> Unit,
    onShareClick: (FeedPost) -> Unit,
    onEditPost: () -> Unit = {},
    onDeletePost: () -> Unit,
    onReportPost: () -> Unit = {},
    onNotInterested: () -> Unit,
    onVisibilityChange: (Boolean?, Boolean?) -> Unit,
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onAppealClick: (String) -> Unit = {},
    commentsState: CommentsUiState,
    onLoadMoreComments: () -> Unit,
    onLoadReplies: (String) -> Unit,
    onRetryComments: () -> Unit,
    onReplyComment: (ir.coffevista.vista_native.features.feed.data.Comment) -> Unit,
    onEditComment: (ir.coffevista.vista_native.features.feed.data.Comment, String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onReportComment: (String) -> Unit,
    canEdit: (ir.coffevista.vista_native.features.feed.data.Comment) -> Boolean,
    canDelete: (ir.coffevista.vista_native.features.feed.data.Comment) -> Boolean,
    canReport: (ir.coffevista.vista_native.features.feed.data.Comment) -> Boolean,
    isCommentAuthor: (ir.coffevista.vista_native.features.feed.data.Comment) -> Boolean,
    onSubmitComment: (CommentDraft) -> Unit,
    onCancelReplyOrEdit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(PostDetailTestTags.Content),
    ) {
        if (state.isStale) {
            Text(
                text = "نمایش نسخه ذخیره‌شده",
                modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFFEF6C00)).padding(horizontal = 16.dp, vertical = 10.dp),
                color = androidx.compose.ui.graphics.Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
          item {
            VistaFeedPostCard(
                post = state.post,
                isOwnPost = state.post.userId == state.viewerUserId,
                onPostClick = {},
                onAuthorClick = onAuthorClick,
                onLikeClick = onLikeClick,
                onSaveClick = onSaveClick,
                onCommentClick = {},
                onShareClick = { onShareClick(state.post) },
                onEditPost = onEditPost,
                onDeletePost = onDeletePost,
                onReportPost = onReportPost,
                onNotInterested = onNotInterested,
                onVisibilityChange = onVisibilityChange,
                onHashtagClick = onHashtagClick,
                onMentionClick = onMentionClick,
                onAppealClick = onAppealClick,
                verticalMenu = true,
                modifier = Modifier.testTag(PostDetailTestTags.Author),
            )
          }
          item {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.vista_post_comment),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("نظرات", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
          }
          when {
            commentsState.isLoading -> item { Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            commentsState.error != null && commentsState.comments.isEmpty() -> item {
              Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(commentsState.error, color = MaterialTheme.colorScheme.error)
                Text("تلاش دوباره", modifier = Modifier.padding(top = 8.dp).clickable(onClick = onRetryComments), color = MaterialTheme.colorScheme.primary)
              }
            }
            commentsState.comments.isEmpty() -> item { ReadOnlyCommentsSummary(state.post) }
            else -> items(commentsState.comments, key = { "detail-comment-${it.id}" }) { comment ->
              CommentThread(
                  comment = comment,
                  onReply = onReplyComment,
                  onLoadReplies = onLoadReplies,
                  isLoadingReplies = comment.id in commentsState.loadingReplyIds,
                  onEdit = onEditComment,
                  onDelete = onDeleteComment,
                  onReport = onReportComment,
                  canEdit = canEdit,
                  canDelete = canDelete,
                  canReport = canReport,
                  isCommentAuthor = isCommentAuthor,
                  onAuthorClick = onCommentAuthorClick,
                  onHashtagClick = onHashtagClick,
                  depth = 0,
              )
            }
          }
          if (commentsState.hasMore) item { Text("نمایش دیدگاه‌های بیشتر", Modifier.fillMaxWidth().clickable(onClick = onLoadMoreComments).padding(16.dp), color = MaterialTheme.colorScheme.primary) }
          if (state.isRefreshing) item {
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
        CommentComposer(
            replyingTo = commentsState.replyingTo,
            editingComment = commentsState.editingComment,
            mentionCandidates = commentMentionCandidates(commentsState.comments),
            onSubmit = onSubmitComment,
            onCancelReplyOrEdit = onCancelReplyOrEdit,
            isSubmitting = commentsState.isSubmitting,
            error = commentsState.error,
        )
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
        Image(
            painter = painterResource(DesignSystemR.drawable.vista_post_comment),
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterHorizontally).size(48.dp),
        )
        Text(
            text = if (post.hideCommentCount) "تعداد نظرات نمایش داده نمی‌شود."
            else if (post.commentCount == 0L) "هنوز نظری ثبت نشده\nاولین نفری باش که نظر میدی!"
            else "${post.commentCount} نظر",
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

