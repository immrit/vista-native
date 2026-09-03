package ir.coffevista.vista_native.features.feed.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.features.feed.ui.components.ReportReasonDialog
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiText
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR
import ir.coffevista.vista_native.features.feed.data.Comment
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.time.Duration
import java.time.Instant

/** Flutter's CommentItem stack anchors the rail at `right: 35`, below `top: 52`. */
internal const val CommentTimelineRailOffsetDp = 35f
internal const val CommentTimelineLowerSegmentStartDp = 52f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    postId: String,
    onDismissRequest: () -> Unit,
    viewModel: CommentsViewModel,
    onAuthorClick: (String) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var reportCommentTargetId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(postId) {
        viewModel.loadComments(postId)
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.vista_post_comment),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "نظرات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (uiState.isLoading && uiState.comments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.error != null && uiState.comments.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = uiState.error ?: "خطایی رخ داده است",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "تلاش مجدد",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.retry() },
                        )
                    }
                } else if (uiState.comments.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(DesignSystemR.drawable.vista_post_comment),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            ),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "هنوز نظری ثبت نشده\nاولین نفری باشید که نظر می‌دهد!",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                        )
                    }
                } else {
                    CommentsList(
                        comments = uiState.comments,
                        hasMore = uiState.hasMore,
                        isAppending = uiState.isAppending,
                        isRefreshing = uiState.isLoading && uiState.comments.isNotEmpty(),
                        loadingReplyIds = uiState.loadingReplyIds,
                        onRefresh = viewModel::retry,
                        onLoadMore = viewModel::loadMore,
                        onLoadReplies = viewModel::loadReplies,
                        onReply = viewModel::setReplyingTo,
                        onEdit = { comment, newContent -> viewModel.updateComment(comment.id, newContent) },
                        onDelete = viewModel::deleteComment,
                        onReport = { commentId -> reportCommentTargetId = commentId },
                        canEdit = viewModel::canEdit,
                        canDelete = { viewModel.canDelete(it) },
                        canReport = viewModel::canReport,
                        isCommentAuthor = { viewModel.isCurrentUser(it.authorUserId) },
                        onAuthorClick = onAuthorClick,
                        onHashtagClick = onHashtagClick,
                    )
                }
            }
            
            CommentComposer(
                replyingTo = uiState.replyingTo,
                editingComment = uiState.editingComment,
                mentionCandidates = commentMentionCandidates(uiState.comments),
                onSubmit = { draft ->
                    viewModel.submitComment(
                        content = draft.content,
                        mentionedUserIds = draft.mentionedUserIds,
                    )
                },
                onCancelReplyOrEdit = {
                    viewModel.setReplyingTo(null)
                    viewModel.setEditingComment(null)
                },
                isSubmitting = uiState.isSubmitting,
                error = uiState.error.takeIf { uiState.comments.isNotEmpty() },
                currentUserAvatarUrl = uiState.currentUserAvatarUrl,
            )
        }
    }

    reportCommentTargetId?.let { commentId ->
        ReportReasonDialog(
            title = "گزارش نظر",
            onDismiss = { reportCommentTargetId = null },
            onSubmit = { reason, _ ->
                viewModel.reportComment(commentId, reason)
                reportCommentTargetId = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommentsList(
    comments: List<Comment>,
    hasMore: Boolean,
    isAppending: Boolean,
    isRefreshing: Boolean,
    loadingReplyIds: Set<String>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onLoadReplies: (String) -> Unit,
    onReply: (Comment) -> Unit,
    onEdit: (Comment, String) -> Unit,
    onDelete: (String) -> Unit,
    onReport: (String) -> Unit,
    canEdit: (Comment) -> Boolean,
    canDelete: (Comment) -> Boolean,
    canReport: (Comment) -> Boolean,
    isCommentAuthor: (Comment) -> Boolean,
    onAuthorClick: (String) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(listState, comments.size, hasMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            comments.isNotEmpty() && hasMore && lastVisibleItem >= totalItems - 2
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
        items(comments, key = { it.id }) { comment ->
            CommentThread(
                comment = comment,
                onReply = onReply,
                onLoadReplies = onLoadReplies,
                isLoadingReplies = comment.id in loadingReplyIds,
                onEdit = onEdit,
                onDelete = onDelete,
                onReport = onReport,
                canEdit = canEdit,
                canDelete = canDelete,
                canReport = canReport,
                isCommentAuthor = isCommentAuthor,
                onAuthorClick = onAuthorClick,
                onHashtagClick = onHashtagClick,
                depth = 0,
            )
        }
            if (isAppending) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CommentThread(
    comment: Comment,
    onReply: (Comment) -> Unit,
    onLoadReplies: (String) -> Unit,
    isLoadingReplies: Boolean,
    onEdit: (Comment, String) -> Unit,
    onDelete: (String) -> Unit,
    onReport: (String) -> Unit,
    canEdit: (Comment) -> Boolean,
    canDelete: (Comment) -> Boolean,
    canReport: (Comment) -> Boolean,
    isCommentAuthor: (Comment) -> Boolean,
    onAuthorClick: (String) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    depth: Int = 0,
) {
    val flattenedReplies = remember(comment.replies) { flattenReplies(comment.replies) }
    var visibleReplyCount by remember(comment.id) { mutableIntStateOf(1) }
    val shownReplies = flattenedReplies.take(visibleReplyCount)
    val hasReplies = flattenedReplies.isNotEmpty()

    CommentItem(
        comment = comment,
        onReply = { onReply(comment) },
        onEdit = { newContent -> onEdit(comment, newContent) },
        onDelete = { onDelete(comment.id) },
        onReport = { onReport(comment.id) },
        canEdit = canEdit(comment),
        canDelete = canDelete(comment),
        canReport = canReport(comment),
        isCommentAuthor = isCommentAuthor(comment),
        onAuthorClick = onAuthorClick,
        onHashtagClick = onHashtagClick,
        isReply = depth > 0,
        hasLineAbove = depth > 0,
        hasLineBelow = (depth == 0 && hasReplies && visibleReplyCount > 0) || depth > 0,
    )

    if (depth > 0) return

    // Flutter renders this explicit empty-reply state under each root comment.
    // It is intentionally not shown for reply rows: those are leaves in the flattened thread.
    if (!hasReplies) {
        Text(
            text = "هنوز پاسخی وجود ندارد",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            textAlign = TextAlign.Center,
        )
    }

    shownReplies.forEachIndexed { index, reply ->
        val isLastReply = index == shownReplies.lastIndex
        CommentItem(
            comment = reply,
            onReply = { onReply(reply) },
            onEdit = { newContent -> onEdit(reply, newContent) },
            onDelete = { onDelete(reply.id) },
            onReport = { onReport(reply.id) },
            canEdit = canEdit(reply),
            canDelete = canDelete(reply),
            canReport = canReport(reply),
            isCommentAuthor = isCommentAuthor(reply),
            onAuthorClick = onAuthorClick,
            onHashtagClick = onHashtagClick,
            isReply = true,
            hasLineAbove = true,
            hasLineBelow = !isLastReply && !isLoadingReplies,
        )
    }

    if (hasReplies) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 64.dp, top = 4.dp, bottom = 8.dp)
                .clickable(enabled = !isLoadingReplies) {
                    if (visibleReplyCount <= 1) onLoadReplies(comment.id)
                    visibleReplyCount = nextVisibleReplyCount(
                        currentCount = visibleReplyCount,
                        loadedReplyCount = flattenedReplies.size,
                    )
                },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            )
            Spacer(Modifier.width(8.dp))
            if (isLoadingReplies) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else Text(
                text = when {
                    visibleReplyCount == 0 -> "مشاهده ${flattenedReplies.size} پاسخ"
                    visibleReplyCount < flattenedReplies.size ->
                        "مشاهده ${flattenedReplies.size - visibleReplyCount} پاسخ دیگر"
                    else -> "پنهان کردن پاسخ‌ها"
                },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun flattenReplies(replies: List<Comment>): List<Comment> = buildList {
    fun append(items: List<Comment>) {
        items.forEach { reply ->
            add(reply)
            append(reply.replies)
        }
    }
    append(replies)
}

/** A mention is stored by stable user ID; the username is presentation-only. */
internal data class CommentMention(
    val userId: String,
    val username: String,
)

internal data class CommentDraft(
    val content: String,
    val mentionedUserIds: List<String>,
)

private fun Comment.asMentionCandidate(): CommentMention? =
    authorUsername
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let { username -> CommentMention(authorUserId, username) }

internal fun commentMentionCandidates(comments: List<Comment>): List<CommentMention> = buildList {
    fun collect(items: List<Comment>) {
        items.forEach { comment ->
            comment.asMentionCandidate()?.let(::add)
            collect(comment.replies)
        }
    }
    collect(comments)
}.distinctBy { it.userId }

/** Mirrors Flutter's `_loadMoreReplies`: promote 0/1 to 10 before async data returns. */
internal fun nextVisibleReplyCount(currentCount: Int, loadedReplyCount: Int): Int = when {
    currentCount <= 1 -> 10
    currentCount < loadedReplyCount -> minOf(currentCount + 10, loadedReplyCount)
    else -> 0
}

@Composable
private fun CommentItem(
    comment: Comment,
    onReply: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    canEdit: Boolean,
    canDelete: Boolean,
    canReport: Boolean,
    isCommentAuthor: Boolean,
    onAuthorClick: (String) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isReply: Boolean = false,
    hasLineAbove: Boolean = false,
    hasLineBelow: Boolean = false,
) {
    // Flutter creates every CommentItem at 0.8 scale with a 300ms fade/elastic entrance.
    // Scope it to the stable item ID so normal scrolling/recomposition does not replay it.
    val entrance = remember(comment.id) { Animatable(0f) }
    LaunchedEffect(comment.id) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        )
    }
    val opacity = entrance.value.coerceIn(0f, 1f)
    val scale = 0.8f + (0.2f * entrance.value)
    var confirmDelete by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isEditingInline by remember { mutableStateOf(false) }
    var editContent by remember(comment.content) { mutableStateOf(comment.content) }

    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) Color(0xFF424242) else Color(0xFFE0E0E0)
    Row(
        modifier = modifier
            .graphicsLayer {
                alpha = opacity
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .drawBehind {
                // Draw before Row padding. Flutter positions the rail in the outer Stack
                // (`right: 35`, `top: 52`); drawing after padding incorrectly shifts it
                // 16dp away from the avatar center and moves each segment vertically.
                val lineX = size.width - CommentTimelineRailOffsetDp.dp.toPx()
                if (hasLineAbove) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(lineX, 0f),
                        end = Offset(lineX, 12.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                if (hasLineBelow) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(lineX, CommentTimelineLowerSegmentStartDp.dp.toPx()),
                        end = Offset(lineX, size.height),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
    ) {
        val avatarSize = if (isReply) 32.dp else 40.dp
        val avatarModifier = Modifier
            .padding(horizontal = if (isReply) 4.dp else 0.dp)
            .size(avatarSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onAuthorClick(comment.authorUserId) }

        if (!comment.authorAvatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = comment.authorAvatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = avatarModifier,
                placeholder = painterResource(DesignSystemR.drawable.vista_default_avatar),
                error = painterResource(DesignSystemR.drawable.vista_default_avatar),
            )
        } else {
            Image(
                painter = painterResource(DesignSystemR.drawable.vista_default_avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = avatarModifier,
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // Author header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAuthorClick(comment.authorUserId) }
            ) {
                VistaEmojiText(
                    text = comment.authorUsername?.takeIf { it.isNotBlank() } ?: comment.authorFullName,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                if (comment.authorIsVerified) {
                    Spacer(Modifier.width(4.dp))
                    CommentVerificationBadge(
                        isVerified = comment.authorIsVerified,
                        verificationType = comment.authorVerificationType,
                        role = comment.authorRole,
                        size = 14.dp,
                    )
                }
                if (isCommentAuthor) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "نویسنده",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            if (isEditingInline) {
                // Inline editing field matching Flutter
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .background(
                                MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = editContent,
                            onValueChange = { editContent = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                lineHeight = 19.6.sp,
                                textDirection = if (isRtlText(editContent)) TextDirection.Rtl else TextDirection.Ltr,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                if (editContent.isEmpty()) {
                                    Text(
                                        text = "ویرایش نظر خود...",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontSize = 14.sp,
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                editContent = comment.content
                                isEditingInline = false
                            },
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("انصراف")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (editContent.isNotBlank()) {
                                    onEdit(editContent.trim())
                                    isEditingInline = false
                                }
                            },
                            enabled = editContent.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ثبت تغییرات")
                        }
                    }
                }
            } else {
                // Normal comment text
                ClickableCommentText(
                    text = comment.content,
                    onHashtagClick = onHashtagClick,
                )
                
                Spacer(Modifier.height(8.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = commentRelativeTime(comment.createdAt),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "پاسخ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onReply() }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    if (canEdit) {
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "ویرایش",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    editContent = comment.content
                                    isEditingInline = true
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                    
                    Spacer(Modifier.weight(1f))
                     
                    Box {
                        Text(
                            text = "•••",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showMenu = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            if (canEdit) {
                                DropdownMenuItem(
                                    text = { Text("ویرایش") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showMenu = false
                                        editContent = comment.content
                                        isEditingInline = true
                                    },
                                )
                            }
                            if (canDelete) {
                                DropdownMenuItem(
                                    text = { Text("حذف", color = Color.Red) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showMenu = false
                                        confirmDelete = true
                                    },
                                )
                            }
                            if (canReport) {
                                DropdownMenuItem(
                                    text = { Text("گزارش") },
                                    leadingIcon = {
                                        CustomFlagIcon(modifier = Modifier.size(18.dp))
                                    },
                                    onClick = {
                                        showMenu = false
                                        onReport()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف دیدگاه") },
            text = { Text("آیا از حذف این دیدگاه اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) {
                    Text("حذف", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("انصراف")
                }
            },
        )
    }
}

@Composable
private fun ClickableCommentText(text: String, onHashtagClick: (String) -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val tagColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, textColor, tagColor) {
        buildAnnotatedString {
            var cursor = 0
            HASHTAG.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                pushStringAnnotation(HASHTAG_ANNOTATION, match.value.removePrefix("#"))
                withStyle(SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold)) { append(match.value) }
                pop()
                cursor = match.range.last + 1
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
    }
    ClickableText(
        text = annotated,
        style = TextStyle(
            fontSize = 14.sp,
            color = textColor,
            lineHeight = 19.6.sp,
            textDirection = if (isRtlText(text)) TextDirection.Rtl else TextDirection.Ltr,
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(HASHTAG_ANNOTATION, offset, offset)
                .firstOrNull()?.item?.takeIf(String::isNotBlank)?.let(onHashtagClick)
        },
    )
}

private const val HASHTAG_ANNOTATION = "comment_hashtag"
private val HASHTAG = Regex("#[\\p{L}\\p{N}_]+")

@Composable
internal fun CommentVerificationBadge(
    isVerified: Boolean,
    verificationType: String?,
    role: String? = null,
    size: Dp = 14.dp,
) {
    if (!isVerified) return
    val isDark = isSystemInDarkTheme()
    val normType = verificationType?.lowercase()?.replace("[^a-z]".toRegex(), "") ?: ""
    val badgeColor = when {
        normType.contains("gold") -> Color(0xFFFFA000)
        normType.contains("black") -> if (isDark) Color.White else Color.Black
        else -> Color(0xFF2196F3)
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

@Composable
private fun CustomReplyIcon(modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    Canvas(modifier = modifier) {
        val stroke = size.width * 0.12f
        val path = Path().apply {
            moveTo(size.width * 0.4f, size.height * 0.2f)
            lineTo(size.width * 0.15f, size.height * 0.45f)
            lineTo(size.width * 0.4f, size.height * 0.7f)
        }
        drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawLine(
            color = tint,
            start = Offset(size.width * 0.18f, size.height * 0.45f),
            end = Offset(size.width * 0.7f, size.height * 0.45f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CustomFlagIcon(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier = modifier) {
        val stroke = size.width * 0.1f
        // Flag pole
        drawLine(
            color = tint,
            start = Offset(size.width * 0.25f, size.height * 0.15f),
            end = Offset(size.width * 0.25f, size.height * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        // Flag banner
        val flagPath = Path().apply {
            moveTo(size.width * 0.25f, size.height * 0.2f)
            lineTo(size.width * 0.8f, size.height * 0.35f)
            lineTo(size.width * 0.25f, size.height * 0.5f)
            close()
        }
        drawPath(flagPath, color = tint)
    }
}

private fun isRtlText(text: String): Boolean {
    val rtlRegex = Regex("[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF]")
    return rtlRegex.containsMatchIn(text)
}

private fun commentRelativeTime(raw: String): String = runCatching {
    val duration = Duration.between(Instant.parse(raw), Instant.now())
    when {
        duration.toMinutes() < 1 -> "همین الان"
        duration.toHours() < 1 -> "${duration.toMinutes()} دقیقه پیش"
        duration.toDays() < 1 -> "${duration.toHours()} ساعت پیش"
        else -> "${duration.toDays()} روز پیش"
    }
}.getOrDefault(raw.take(10))

@Composable
internal fun CommentComposer(
    replyingTo: Comment?,
    editingComment: Comment?,
    mentionCandidates: List<CommentMention> = emptyList(),
    onSubmit: (CommentDraft) -> Unit,
    onCancelReplyOrEdit: () -> Unit,
    isSubmitting: Boolean = false,
    error: String? = null,
    currentUserAvatarUrl: String? = null,
) {
    var text by rememberSaveable(replyingTo?.id, editingComment?.id) { 
        mutableStateOf(editingComment?.content ?: "") 
    }
    val isDark = isSystemInDarkTheme()
    val canSend = text.trim().isNotEmpty() && text.trim().codePointCount(0, text.trim().length) <= 2200 && !isSubmitting

    val mentionMatch = remember(text) {
        val lastWord = text.substringAfterLast(" ", text).substringAfterLast("\n", text)
        if (lastWord.startsWith("@") && lastWord.length > 1) {
            lastWord.removePrefix("@").lowercase()
        } else null
    }

    var selectedMentions by rememberSaveable(replyingTo?.id, editingComment?.id) {
        mutableStateOf(emptyMap<String, String>())
    }
    val availableMentions = remember(mentionCandidates, replyingTo) {
        (mentionCandidates + listOfNotNull(replyingTo?.asMentionCandidate()))
            .filter { it.userId.isNotBlank() && it.username.isNotBlank() }
            .distinctBy { it.userId }
    }

    Surface(
        color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Mention autocomplete chip row
            if (mentionMatch != null && availableMentions.isNotEmpty()) {
                val matches = availableMentions.filter {
                    it.username.lowercase().contains(mentionMatch)
                }
                if (matches.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(matches, key = { it.userId }) { candidate ->
                            Surface(
                                onClick = {
                                    val currentToken = "@$mentionMatch"
                                    val prefix = text.removeSuffix(currentToken)
                                    text = "$prefix@${candidate.username} "
                                    selectedMentions = selectedMentions + (candidate.userId to candidate.username)
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "@${candidate.username}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                    fontSize = 12.sp,
                )
            }

            // Reply banner matching Flutter CommentInputField
            if (replyingTo != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF303030) else Color(0xFFF5F5F5))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CustomReplyIcon(
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "در حال پاسخ به ${replyingTo.authorUsername ?: replyingTo.authorFullName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF424242),
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "لغو پاسخ",
                            tint = if (isDark) Color(0xFFBDBDBD) else Color(0xFF757575),
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { onCancelReplyOrEdit() },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else if (editingComment != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF303030) else Color(0xFFF5F5F5))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "ویرایش نظر",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF424242),
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "انصراف",
                        tint = if (isDark) Color(0xFFBDBDBD) else Color(0xFF757575),
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { onCancelReplyOrEdit() },
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val composerAvatarModifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)

                if (currentUserAvatarUrl.isNullOrBlank()) {
                    Image(
                        painter = painterResource(DesignSystemR.drawable.vista_default_avatar),
                        contentDescription = "تصویر نمایه شما",
                        contentScale = ContentScale.Crop,
                        modifier = composerAvatarModifier,
                    )
                } else {
                    AsyncImage(
                        model = currentUserAvatarUrl,
                        contentDescription = "تصویر نمایه شما",
                        contentScale = ContentScale.Crop,
                        modifier = composerAvatarModifier,
                        placeholder = painterResource(DesignSystemR.drawable.vista_default_avatar),
                        error = painterResource(DesignSystemR.drawable.vista_default_avatar),
                    )
                }
                
                Spacer(Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { updated ->
                            text = updated
                            // A mention is valid only while its visible token remains in the draft.
                            selectedMentions = selectedMentions.filterValues { username ->
                                Regex("(?<![\\p{L}\\p{N}_])@${Regex.escape(username)}(?:\\b|\\s|$)")
                                    .containsMatchIn(updated)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            textDirection = if (isRtlText(text)) TextDirection.Rtl else TextDirection.Ltr,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 4,
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text(
                                    text = if (replyingTo != null) "پاسخ خود را بنویسید..." else "نظر خود را بنویسید...",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Send button matching Flutter #007AFF
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) Color(0xFF007AFF)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        .clickable(enabled = canSend) {
                            val submitText = text.trim()
                            if (submitText.isNotEmpty()) {
                                onSubmit(
                                    CommentDraft(
                                        content = submitText,
                                        mentionedUserIds = selectedMentions.keys.toList(),
                                    ),
                                )
                                text = ""
                                selectedMentions = emptyMap()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Image(
                            painter = painterResource(DesignSystemR.drawable.vista_post_send),
                            contentDescription = "ارسال نظر",
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(
                                if (canSend) Color.White
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            ),
                        )
                    }
                }
            }
        }
    }
}
