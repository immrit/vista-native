package ir.coffevista.vista_native.features.feed.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR
import ir.coffevista.vista_native.features.feed.data.Comment
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    postId: String,
    onDismissRequest: () -> Unit,
    viewModel: CommentsViewModel
) {
    LaunchedEffect(postId) {
        viewModel.loadComments(postId)
    }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding())
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.vista_post_comment),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "نظرات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Box(modifier = Modifier.weight(1f, fill = false)) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.error != null && uiState.comments.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::retry) { Text("تلاش دوباره") }
                    }
                } else if (uiState.comments.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(DesignSystemR.drawable.vista_post_comment),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "هنوز نظری ثبت نشده\nاولین نفری باشید که نظر می‌دهد!",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                } else {
                    CommentsList(
                        comments = uiState.comments,
                        hasMore = uiState.hasMore,
                        isAppending = uiState.isAppending,
                        onLoadMore = viewModel::loadMore,
                        onReply = viewModel::setReplyingTo,
                        onEdit = viewModel::setEditingComment,
                        onDelete = viewModel::deleteComment,
                        onReport = { viewModel.reportComment(it, "spam") },
                        isCurrentUser = viewModel::isCurrentUser,
                    )
                }
            }
            
            CommentComposer(
                replyingTo = uiState.replyingTo,
                editingComment = uiState.editingComment,
                onSubmit = { content -> viewModel.submitComment(content) },
                onCancelReplyOrEdit = {
                    viewModel.setReplyingTo(null)
                    viewModel.setEditingComment(null)
                },
                isSubmitting = uiState.isSubmitting,
                error = uiState.error.takeIf { uiState.comments.isNotEmpty() },
            )
        }
    }
}

@Composable
internal fun CommentsList(
    comments: List<Comment>,
    hasMore: Boolean,
    isAppending: Boolean,
    onLoadMore: () -> Unit,
    onReply: (Comment) -> Unit,
    onEdit: (Comment) -> Unit,
    onDelete: (String) -> Unit,
    onReport: (String) -> Unit,
    isCurrentUser: (String) -> Boolean,
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
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(comments, key = { it.id }) { comment ->
            CommentThread(
                comment = comment,
                onReply = onReply,
                onEdit = onEdit,
                onDelete = onDelete,
                onReport = onReport,
                isCurrentUser = isCurrentUser,
                depth = 0,
                isLastItem = comments.indexOf(comment) == comments.size - 1
            )
        }
        if (isAppending) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
internal fun CommentThread(
    comment: Comment,
    onReply: (Comment) -> Unit,
    onEdit: (Comment) -> Unit,
    onDelete: (String) -> Unit,
    onReport: (String) -> Unit,
    isCurrentUser: (String) -> Boolean,
    depth: Int,
    isLastItem: Boolean = true
) {
    val hasReplies = comment.replies.isNotEmpty()
    CommentItem(
        comment = comment,
        onReply = { onReply(comment) },
        onEdit = { onEdit(comment) },
        onDelete = { onDelete(comment.id) },
        onReport = { onReport(comment.id) },
        isCurrentUser = isCurrentUser(comment.authorUserId),
        modifier = Modifier.padding(start = (depth * 24).dp),
        hasLineBelow = hasReplies || (!isLastItem && depth > 0)
    )
    
    comment.replies.forEach { reply ->
        CommentThread(
            comment = reply,
            onReply = onReply,
            onEdit = onEdit,
            onDelete = onDelete,
            onReport = onReport,
            isCurrentUser = isCurrentUser,
            depth = depth + 1,
            isLastItem = comment.replies.indexOf(reply) == comment.replies.size - 1
        )
    }
}


@Composable
private fun CommentItem(
    comment: Comment,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    hasLineBelow: Boolean = false
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .drawBehind {
                if (hasLineBelow) {
                    val strokeWidth = 1.dp.toPx()
                    val startX = 18.dp.toPx() // center of 36dp avatar
                    val startY = 40.dp.toPx() // below avatar
                    drawLine(
                        color = dividerColor,
                        start = Offset(startX, startY),
                        end = Offset(startX, size.height + 8.dp.toPx()),
                        strokeWidth = strokeWidth
                    )
                }
            }
    ) {
        val avatarModifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)

        if (comment.authorAvatarUrl != null) {
            AsyncImage(
                model = comment.authorAvatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = avatarModifier,
            )
        } else {
            Box(modifier = avatarModifier)
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorFullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (comment.authorIsVerified) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Row(
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مدتی پیش", // Mock time ago since not available in Comment model
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "پاسخ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { onReply() }
                )
                if (isCurrentUser) {
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "ویرایش",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { onEdit() }
                    )
                }
                
                Spacer(Modifier.weight(1f))
                
                Box {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "بیشتر",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { showMenu = true }
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isCurrentUser) {
                            DropdownMenuItem(
                                text = { Text("ویرایش") },
                                onClick = { showMenu = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; confirmDelete = true }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("گزارش") },
                                onClick = { showMenu = false; onReport() }
                            )
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("حذف دیدگاه") },
        text = { Text("آیا از حذف این دیدگاه اطمینان دارید؟") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("حذف") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } }
    )
}


@Composable
internal fun CommentComposer(
    replyingTo: Comment?,
    editingComment: Comment?,
    onSubmit: (String) -> Unit,
    onCancelReplyOrEdit: () -> Unit,
    isSubmitting: Boolean = false,
    error: String? = null,
) {
    var text by remember(replyingTo, editingComment) { 
        mutableStateOf(editingComment?.content ?: "") 
    }
    
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontSize = 12.sp
                )
            }
            if (replyingTo != null || editingComment != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingComment != null) "ویرایش نظر" else "پاسخ به ${replyingTo?.authorFullName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "لغو",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onCancelReplyOrEdit() }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("نظر خود را بنویسید...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { if (text.isNotBlank()) onSubmit(text) },
                    enabled = text.isNotBlank() && !isSubmitting,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (text.isNotBlank()) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("↑", color = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
