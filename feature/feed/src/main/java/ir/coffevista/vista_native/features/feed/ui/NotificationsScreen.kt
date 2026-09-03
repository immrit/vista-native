package ir.coffevista.vista_native.features.feed.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR
import ir.coffevista.vista_native.features.feed.data.FollowRequestActionResult
import ir.coffevista.vista_native.features.feed.data.FollowRequestActionState
import ir.coffevista.vista_native.features.feed.data.NotificationFilter
import ir.coffevista.vista_native.features.feed.data.VistaNotification
import ir.coffevista.vista_native.features.feed.data.notificationTypeMatchesFilter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onChatClick: (String, String?) -> Unit,
    onSuggestionsClick: () -> Unit,
    onAppealClick: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.markAllAsRead()
    }
    NotificationsScreenContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::fetchMore,
        onMarkAllAsRead = viewModel::markAllAsRead,
        onOpenItem = { item ->
            viewModel.markAsRead(item.items.map { it.id })
            openNotificationItem(item.primary, onPostClick, onUserClick, onChatClick, onSuggestionsClick, onAppealClick)
        },
        onOpenActor = { item ->
            viewModel.markAsRead(item.items.map { it.id })
            resolveActorId(item.primary)?.let(onUserClick)
        },
        onFollowRequestAction = { notification, accept, onDone ->
            viewModel.respondToFollowRequest(notification, accept, onDone)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsScreenContent(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onOpenItem: (NotificationFeedItem) -> Unit,
    onOpenActor: (NotificationFeedItem) -> Unit,
    onFollowRequestAction: (VistaNotification, Boolean, (FollowRequestActionResult) -> Unit) -> Unit,
) {
    var selectedFilter by rememberSaveable { mutableStateOf(NotificationFilter.All.type) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tabs = remember { notificationTabs() }
    val isInitialLoading = state.notifications.isEmpty() && state.isLoading
    val filtered = remember(state.notifications, selectedFilter) {
        state.notifications.filter { notificationTypeMatchesFilter(it.type, selectedFilter) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اعلان‌ها",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = onMarkAllAsRead) {
                        Icon(Icons.Default.DoneAll, contentDescription = "علامت‌گذاری همه به عنوان خوانده شده")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .testTag(NotificationTestTags.Screen),
        ) {
            if (isInitialLoading) {
                NotificationTabsShimmer()
                NotificationListShimmer()
            } else {
                NotificationFilterTabs(
                    tabs = tabs,
                    selectedFilter = selectedFilter,
                    unreadCounts = state.unreadCountsByFilter,
                    onSelected = { selectedFilter = it },
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
                PullToRefreshBox(
                    isRefreshing = state.isLoading && state.notifications.isNotEmpty(),
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    NotificationsList(
                        notifications = filtered,
                        hasMore = state.hasMore,
                        isFetching = state.isLoading,
                        actionLoadingIds = state.actionLoadingIds,
                        onLoadMore = onLoadMore,
                        onOpenItem = onOpenItem,
                        onOpenActor = onOpenActor,
                        onFollowRequestAction = { notification, accept ->
                            onFollowRequestAction(notification, accept) { result ->
                                val message = when (result.state) {
                                    FollowRequestActionState.Success -> if (accept) {
                                        "درخواست با موفقیت پذیرفته شد"
                                    } else {
                                        "درخواست رد شد"
                                    }
                                    FollowRequestActionState.AlreadyHandled -> "این درخواست قبلا مدیریت شده است"
                                    FollowRequestActionState.Failed -> "خطا در پردازش درخواست"
                                }
                                scope.launch { snackbarHostState.showSnackbar(result.message.ifBlank { message }) }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationFilterTabs(
    tabs: List<NotificationTabData>,
    selectedFilter: String,
    unreadCounts: Map<String, Int>,
    onSelected: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs, key = { it.type }) { tab ->
            val selected = tab.type == selectedFilter
            Surface(
                onClick = { onSelected(tab.type) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                border = if (selected) null else {
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    )
                },
                modifier = Modifier
                    .height(40.dp)
                    .testTag(NotificationTestTags.tab(tab.type)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(tab.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    val count = unreadCounts[tab.type] ?: 0
                    if (count > 0) {
                        Text(
                            text = count.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFFF44336), RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsList(
    notifications: List<VistaNotification>,
    hasMore: Boolean,
    isFetching: Boolean,
    actionLoadingIds: Set<String>,
    onLoadMore: () -> Unit,
    onOpenItem: (NotificationFeedItem) -> Unit,
    onOpenActor: (NotificationFeedItem) -> Unit,
    onFollowRequestAction: (VistaNotification, Boolean) -> Unit,
) {
    if (notifications.isEmpty()) {
        NotificationEmptyState(onRetry = onLoadMore)
        return
    }

    val rows = remember(notifications) { buildNotificationRows(notifications) }
    val listState = rememberLazyListState()
    LaunchedEffect(listState, rows.size, hasMore) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && rows.isNotEmpty() && last >= info.totalItemsCount - 3
        }
            .distinctUntilChanged()
            .collect { shouldLoad -> if (shouldLoad) onLoadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                is NotificationHeaderRow -> NotificationHeader(row.title)
                is NotificationItemRow -> NotificationRow(
                    item = row.item,
                    isActionLoading = row.item.primary.id in actionLoadingIds,
                    onOpenItem = { onOpenItem(row.item) },
                    onOpenActor = { onOpenActor(row.item) },
                    onFollowRequestAction = onFollowRequestAction,
                )
            }
        }
        if (hasMore) {
            item(key = "notification_append_loader") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isFetching) CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: NotificationFeedItem,
    isActionLoading: Boolean,
    onOpenItem: () -> Unit,
    onOpenActor: () -> Unit,
    onFollowRequestAction: (VistaNotification, Boolean) -> Unit,
) {
    val notification = item.primary
    val isUnread = item.items.any { !it.isRead }
    val isBatched = item.items.size > 1
    val contentText = if (isBatched) buildBatchedLikeContent(item) else notification.content
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isUnread) highlight else Color.Transparent)
                .clickable(onClick = onOpenItem)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(54.dp)
                    .background(
                        if (isUnread) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(4.dp),
                    ),
            )
            Spacer(Modifier.width(10.dp))
            NotificationAvatar(notification = notification, onClick = onOpenActor)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.username.ifBlank { "کاربر" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (notification.userIsVerified) {
                        VerifiedMark(Modifier.padding(start = 4.dp))
                    }
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = contentText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.8.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                )
                if (isBatched) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${item.items.size} فعالیت مشابه",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (!isBatched && notification.canonicalType == "follow_request") {
                    Spacer(Modifier.height(10.dp))
                    FollowRequestActions(
                        isLoading = isActionLoading,
                        onReject = { onFollowRequestAction(notification, false) },
                        onAccept = { onFollowRequestAction(notification, true) },
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatNotificationTime(notification.createdAt),
                modifier = Modifier.width(96.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 14.dp, end = 86.dp),
            thickness = 0.6.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun NotificationAvatar(notification: VistaNotification, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
    ) {
        if (notification.avatarUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        } else {
            AsyncImage(
                model = notification.avatarUrl,
                contentDescription = notification.username,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = notificationIcon(notification.canonicalType),
                contentDescription = null,
                tint = notificationIconColor(notification.canonicalType),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun FollowRequestActions(
    isLoading: Boolean,
    onReject: () -> Unit,
    onAccept: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onReject,
            enabled = !isLoading,
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text("رد")
        }
        Button(
            onClick = onAccept,
            enabled = !isLoading,
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("پذیرفتن")
            }
        }
    }
}

@Composable
private fun NotificationHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 8.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun NotificationEmptyState(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Icon(
                Icons.Default.NotificationsOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "اعلانی برای این بخش وجود ندارد",
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onRetry) {
                Text("بررسی مجدد")
            }
        }
    }
}

@Composable
private fun NotificationTabsShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            )
        }
    }
}

@Composable
private fun NotificationListShimmer() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(Modifier.width(85.dp).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(180.dp).height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                }
            }
        }
    }
}

private fun notificationTabs(): List<NotificationTabData> = listOf(
    NotificationTabData("همه", NotificationFilter.All.type, Icons.Default.Notifications),
    NotificationTabData("درخواست‌ها", NotificationFilter.FollowRequest.type, Icons.Default.PersonAdd),
    NotificationTabData("دنبال‌کننده‌ها", NotificationFilter.Follow.type, Icons.Default.PersonAdd),
    NotificationTabData("لایک‌ها", NotificationFilter.Like.type, Icons.Default.Favorite),
    NotificationTabData("کامنت‌ها", NotificationFilter.Comment.type, Icons.Default.Comment),
    NotificationTabData("پاسخ‌ها", NotificationFilter.CommentReply.type, Icons.Default.Reply),
    NotificationTabData("منشن‌ها", NotificationFilter.Mention.type, Icons.Default.AlternateEmail),
    NotificationTabData("پیشنهادها", NotificationFilter.Suggestions.type, Icons.Default.AutoAwesome),
)

private data class NotificationTabData(
    val title: String,
    val type: String,
    val icon: ImageVector,
)

internal sealed interface NotificationFeedRow {
    val key: String
}

internal data class NotificationHeaderRow(val title: String) : NotificationFeedRow {
    override val key: String = "header_$title"
}

internal data class NotificationItemRow(val item: NotificationFeedItem) : NotificationFeedRow {
    override val key: String = if (item.items.size > 1) {
        "batch_${item.items.joinToString("_") { it.id }}"
    } else {
        item.primary.id
    }
}

internal data class NotificationFeedItem(
    val primary: VistaNotification,
    val items: List<VistaNotification>,
)

internal fun buildNotificationRows(notifications: List<VistaNotification>): List<NotificationFeedRow> {
    val rows = mutableListOf<NotificationFeedRow>()
    var currentGroup: String? = null
    var currentBatch: NotificationFeedItem? = null

    fun pushBatch() {
        currentBatch?.let { rows.add(NotificationItemRow(it)) }
        currentBatch = null
    }

    notifications.forEach { notification ->
        val group = groupTitleForDate(notification.createdAt)
        if (group != currentGroup) {
            pushBatch()
            currentGroup = group
            rows.add(NotificationHeaderRow(group))
        }

        val batch = currentBatch
        if (batch != null && canBatchTogether(batch.primary, notification)) {
            currentBatch = batch.copy(items = batch.items + notification)
        } else {
            pushBatch()
            currentBatch = NotificationFeedItem(notification, listOf(notification))
        }
    }
    pushBatch()
    return rows
}

private fun canBatchTogether(a: VistaNotification, b: VistaNotification): Boolean {
    if (a.canonicalType != "like" || b.canonicalType != "like") return false
    if (a.postId.isNullOrBlank() || b.postId.isNullOrBlank()) return false
    if (a.postId != b.postId) return false
    val aTime = parseInstant(a.createdAt) ?: return false
    val bTime = parseInstant(b.createdAt) ?: return false
    return abs(Duration.between(aTime, bTime).toHours()) <= 12
}

private fun buildBatchedLikeContent(item: NotificationFeedItem): String {
    val names = item.items.map { it.username.trim() }.filter(String::isNotEmpty).distinct()
    return when {
        names.isEmpty() -> "${item.items.size} نفر پست شما را لایک کردند"
        names.size == 1 -> "${names.first()} پست شما را لایک کرد"
        names.size == 2 -> "${names[0]} و ${names[1]} پست شما را لایک کردند"
        else -> "${names[0]}، ${names[1]} و ${names.size - 2} نفر دیگر پست شما را لایک کردند"
    }
}

private fun groupTitleForDate(createdAt: String): String {
    val date = parseInstant(createdAt)?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: return "قدیمی‌تر"
    val today = LocalDate.now()
    return when {
        date == today -> "امروز"
        date.isAfter(today.minusDays(7)) -> "این هفته"
        else -> "قدیمی‌تر"
    }
}

private fun formatNotificationTime(createdAt: String): String {
    val instant = parseInstant(createdAt) ?: return ""
    val diff = Duration.between(instant, Instant.now())
    return when {
        diff.isNegative || diff.toMinutes() < 1 -> "همین الان"
        diff.toHours() < 1 -> "${diff.toMinutes()} دقیقه پیش"
        diff.toDays() < 1 -> "${diff.toHours()} ساعت پیش"
        diff.toDays() < 7 -> "${diff.toDays()} روز پیش"
        else -> instant.atZone(ZoneId.systemDefault()).toLocalDate().let { date ->
            "${date.year}/${date.monthValue.toString().padStart(2, '0')}/${date.dayOfMonth.toString().padStart(2, '0')}"
        }
    }
}

private fun parseInstant(raw: String): Instant? = runCatching { Instant.parse(raw) }.getOrNull()

private fun notificationIcon(type: String): ImageVector = when (type) {
    "like" -> Icons.Default.Favorite
    "comment" -> Icons.Default.Comment
    "comment_reply" -> Icons.Default.Reply
    "follow" -> Icons.Default.PersonAdd
    "follow_request" -> Icons.Default.PersonAdd
    "follow_request_accepted" -> Icons.Default.CheckCircle
    "mention" -> Icons.Default.AlternateEmail
    "message" -> Icons.Default.ChatBubble
    "suggest_follow" -> Icons.Default.GroupAdd
    "suggest_post" -> Icons.Default.AutoAwesome
    "daily_suggestion_digest" -> Icons.Default.WbSunny
    else -> Icons.Default.Notifications
}

private fun notificationIconColor(type: String): Color = when (type) {
    "like" -> Color(0xFFE53935)
    "comment" -> Color(0xFF1E88E5)
    "comment_reply" -> Color(0xFFFB8C00)
    "follow", "follow_request_accepted" -> Color(0xFF43A047)
    "follow_request" -> Color(0xFFFFB300)
    "mention" -> Color(0xFF8E24AA)
    "suggest_follow" -> Color(0xFF00897B)
    "suggest_post" -> Color(0xFFF4511E)
    "daily_suggestion_digest" -> Color(0xFF3949AB)
    else -> Color(0xFF757575)
}

private fun openNotificationItem(
    notification: VistaNotification,
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onChatClick: (String, String?) -> Unit,
    onSuggestionsClick: () -> Unit,
    onAppealClick: (String) -> Unit,
) {
    when (notification.canonicalType) {
        "like", "comment", "comment_reply", "mention", "suggest_post" -> {
            notification.postId?.takeIf(String::isNotBlank)?.let(onPostClick)
        }
        "follow", "follow_request", "follow_request_accepted" -> {
            resolveActorId(notification)?.let(onUserClick)
        }
        "message" -> {
            notification.conversationId?.takeIf(String::isNotBlank)?.let {
                onChatClick(it, notification.metadata["message_id"])
            }
        }
        "daily_suggestion_digest", "suggest_follow" -> onSuggestionsClick()
        "moderation", "post_removed", "post_edited" -> {
            notification.postId?.takeIf(String::isNotBlank)?.let(onAppealClick)
        }
        else -> Unit
    }
}

private fun resolveActorId(notification: VistaNotification): String? {
    return notification.senderId.takeIf(String::isNotBlank)
        ?: notification.followerId?.takeIf(String::isNotBlank)
        ?: notification.metadata["user_id"]?.takeIf(String::isNotBlank)
        ?: notification.metadata["actor_id"]?.takeIf(String::isNotBlank)
        ?: notification.metadata["sender_id"]?.takeIf(String::isNotBlank)
        ?: notification.metadata["follower_id"]?.takeIf(String::isNotBlank)
}

object NotificationTestTags {
    const val Screen = "notifications-screen"
    fun tab(type: String) = "notifications-tab-$type"
}
