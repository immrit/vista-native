package ir.coffevista.vista_native.features.feed.ui.location

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.database.profile.OwnProfileDao
import ir.coffevista.vista_native.core.database.profile.hasBlueBadge
import ir.coffevista.vista_native.core.database.profile.hasPremiumPrivileges
import ir.coffevista.vista_native.core.database.profile.tierCapabilities
import ir.coffevista.vista_native.core.designsystem.component.VistaAppBarAmbientOverlay
import ir.coffevista.vista_native.core.designsystem.component.VistaVerificationBadge
import ir.coffevista.vista_native.core.designsystem.navigation.LocalVistaInternalLinkNavigator
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import ir.coffevista.vista_native.features.feed.data.LocationMatcher
import ir.coffevista.vista_native.features.feed.ui.CommentsBottomSheet
import ir.coffevista.vista_native.features.feed.ui.CommentsViewModel
import ir.coffevista.vista_native.features.feed.ui.PostShareBottomSheet
import ir.coffevista.vista_native.features.feed.ui.VistaFeedPostCard
import ir.coffevista.vista_native.features.feed.ui.components.EditPostDialog
import ir.coffevista.vista_native.features.feed.ui.components.ReportReasonDialog
import ir.coffevista.vista_native.features.feed.ui.components.VistaFullscreenImageViewer
import ir.coffevista.vista_native.features.stories.data.StoryRepository
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import ir.coffevista.vista_native.features.stories.domain.StoryUser
import ir.coffevista.vista_native.features.stories.domain.StoryVerificationType
import ir.coffevista.vista_native.features.stories.ui.player.StoryPlayerScreen
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationFeedUiState(
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val stories: List<StoryUser> = emptyList(),
    val posts: List<FeedPost> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val hasMore: Boolean = true,
    val nextOffset: Int = 0,
    val errorMessage: String? = null,
    val viewerUserId: String? = null,
    val isViewerBlueTick: Boolean = false,
    val viewerHasPremiumEdit: Boolean = false,
    val maxPostChars: Int = 300,
)

@HiltViewModel
class LocationFeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val storyRepository: StoryRepository,
    private val authenticationStateProvider: AuthenticationStateProvider,
    private val ownProfileDao: OwnProfileDao,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(LocationFeedUiState())
    val uiState: StateFlow<LocationFeedUiState> = mutableUiState.asStateFlow()

    private var boundLocation: String? = null
    private var observingProfileForAccount: String? = null

    fun bind(locationName: String, latitude: Double?, longitude: Double?) {
        val cleaned = locationName.trim()
        if (cleaned.isBlank()) return

        val accountId = (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)
            ?.context
            ?.userId
        if (accountId != null && observingProfileForAccount != accountId) {
            observingProfileForAccount = accountId
            mutableUiState.update { it.copy(viewerUserId = accountId) }
            viewModelScope.launch {
                ownProfileDao.getOwnProfile(accountId).collect { profile ->
                    if (profile != null) {
                        mutableUiState.update { state ->
                            state.copy(
                                isViewerBlueTick = profile.hasBlueBadge,
                                viewerHasPremiumEdit = profile.hasPremiumPrivileges,
                                maxPostChars = profile.tierCapabilities.maxPostChars,
                            )
                        }
                    }
                }
            }
        }

        if (cleaned == boundLocation) {
            if (latitude != null && longitude != null) {
                mutableUiState.update { it.copy(latitude = latitude, longitude = longitude) }
            }
            return
        }

        boundLocation = cleaned
        mutableUiState.update {
            it.copy(
                locationName = cleaned,
                latitude = latitude,
                longitude = longitude,
            )
        }
        refresh()
    }

    fun refresh() = request(offset = 0, append = false)

    fun loadMore() {
        val state = uiState.value
        if (state.isLoading || state.isRefreshing || state.isAppending || !state.hasMore) return
        request(offset = state.nextOffset, append = true)
    }

    private fun request(offset: Int, append: Boolean) {
        val location = boundLocation ?: return
        val accountId = (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)
            ?.context
            ?.userId
        if (accountId == null) {
            mutableUiState.update {
                it.copy(isLoading = false, errorMessage = "برای مشاهده محتوا وارد حساب شوید")
            }
            return
        }

        mutableUiState.update {
            when {
                append -> it.copy(isAppending = true, errorMessage = null)
                offset == 0 && (it.posts.isNotEmpty() || it.stories.isNotEmpty()) ->
                    it.copy(isRefreshing = true, errorMessage = null)
                else -> it.copy(isLoading = true, errorMessage = null)
            }
        }

        viewModelScope.launch {
            if (append) {
                runCatching { feedRepository.getLocationPosts(accountId, location, offset) }
                    .onSuccess { snapshot ->
                        mutableUiState.update { current ->
                            current.copy(
                                posts = (current.posts + snapshot.posts).distinctBy(FeedPost::id),
                                isAppending = false,
                                hasMore = snapshot.hasMore,
                                nextOffset = snapshot.nextOffset,
                                errorMessage = null,
                            )
                        }
                    }
                    .onFailure {
                        mutableUiState.update {
                            it.copy(
                                isAppending = false,
                                errorMessage = "دریافت ادامه پست‌ها ناموفق بود",
                            )
                        }
                    }
                return@launch
            }

            val (postsResult, storiesResult) = coroutineScope {
                val postsDeferred = async {
                    runCatching { feedRepository.getLocationPosts(accountId, location, 0) }
                }
                val storiesDeferred = async {
                    storyRepository.getStoriesByLocation(location, accountId)
                }
                postsDeferred.await() to storiesDeferred.await()
            }

            val stories = storiesResult.getOrDefault(emptyList())
            val firstGeo = stories.asSequence()
                .flatMap { it.stories.asSequence() }
                .mapNotNull { st ->
                    st.location ?: st.interactiveElements.firstOrNull { it.type == "location" }?.location
                }
                .firstOrNull { it.latitude != null && it.longitude != null && (it.latitude != 0.0 || it.longitude != 0.0) }

            postsResult
                .onSuccess { snapshot ->
                    mutableUiState.update { current ->
                        current.copy(
                            stories = stories,
                            posts = snapshot.posts,
                            latitude = current.latitude ?: firstGeo?.latitude,
                            longitude = current.longitude ?: firstGeo?.longitude,
                            isLoading = false,
                            isRefreshing = false,
                            isAppending = false,
                            hasMore = snapshot.hasMore,
                            nextOffset = snapshot.nextOffset,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure {
                    mutableUiState.update { current ->
                        current.copy(
                            stories = stories,
                            latitude = current.latitude ?: firstGeo?.latitude,
                            longitude = current.longitude ?: firstGeo?.longitude,
                            isLoading = false,
                            isRefreshing = false,
                            isAppending = false,
                            errorMessage = if (stories.isEmpty()) {
                                "دریافت پست‌های این مکان ناموفق بود؛ دوباره تلاش کنید"
                            } else {
                                null
                            },
                        )
                    }
                }
        }
    }

    fun toggleLike(postId: String, currentIsLiked: Boolean, currentCount: Long) {
        val accountId = uiState.value.viewerUserId ?: return
        val post = uiState.value.posts.firstOrNull { it.id == postId } ?: return
        val newIsLiked = !currentIsLiked
        val newCount = if (newIsLiked) currentCount + 1 else (currentCount - 1).coerceAtLeast(0)
        mutableUiState.update { state ->
            state.copy(
                posts = state.posts.map {
                    if (it.id == postId) it.copy(isLiked = newIsLiked, likeCount = newCount) else it
                },
            )
        }
        viewModelScope.launch {
            runCatching {
                feedRepository.toggleLike(accountId, postId, post.userId, newIsLiked, newCount)
            }
        }
    }

    fun toggleSave(postId: String, currentIsSaved: Boolean) {
        val accountId = uiState.value.viewerUserId ?: return
        val newIsSaved = !currentIsSaved
        mutableUiState.update { state ->
            state.copy(
                posts = state.posts.map {
                    if (it.id == postId) it.copy(isSaved = newIsSaved) else it
                },
            )
        }
        viewModelScope.launch {
            runCatching {
                feedRepository.toggleSave(accountId, postId, newIsSaved)
            }
        }
    }

    fun deletePost(postId: String) {
        val accountId = uiState.value.viewerUserId ?: return
        mutableUiState.update { state ->
            state.copy(posts = state.posts.filterNot { it.id == postId })
        }
        viewModelScope.launch {
            runCatching { feedRepository.deletePost(accountId, postId) }
        }
    }

    fun updatePostContent(
        postId: String,
        newContent: String,
        onDone: (Boolean) -> Unit,
    ) {
        val accountId = uiState.value.viewerUserId ?: return
        viewModelScope.launch {
            runCatching {
                feedRepository.updatePost(
                    accountId = accountId,
                    postId = postId,
                    content = newContent,
                )
            }.onSuccess { updated ->
                mutableUiState.update { state ->
                    state.copy(
                        posts = state.posts.map {
                            if (it.id == postId) it.copy(content = updated.content ?: newContent) else it
                        },
                    )
                }
                onDone(true)
            }.onFailure {
                onDone(false)
            }
        }
    }

    fun updateEngagementVisibility(
        post: FeedPost,
        hideLikeCount: Boolean?,
        hideCommentCount: Boolean?,
    ) {
        val accountId = uiState.value.viewerUserId ?: return
        viewModelScope.launch {
            runCatching {
                feedRepository.updatePost(
                    accountId = accountId,
                    postId = post.id,
                    hideLikeCount = hideLikeCount,
                    hideCommentCount = hideCommentCount,
                )
            }.onSuccess {
                mutableUiState.update { state ->
                    state.copy(
                        posts = state.posts.map {
                            if (it.id == post.id) {
                                it.copy(
                                    hideLikeCount = hideLikeCount ?: it.hideLikeCount,
                                    hideCommentCount = hideCommentCount ?: it.hideCommentCount,
                                )
                            } else {
                                it
                            }
                        },
                    )
                }
            }
        }
    }

    fun reportPost(
        post: FeedPost,
        reason: String,
        details: String?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                feedRepository.reportPost(
                    postId = post.id,
                    reportedUserId = post.userId,
                    reason = reason,
                    additionalDetails = details,
                )
            }
            onDone()
        }
    }

    fun markNotInterested(postId: String) {
        mutableUiState.update { state ->
            state.copy(posts = state.posts.filterNot { it.id == postId })
        }
        viewModelScope.launch {
            runCatching { feedRepository.markPostNotInterestedLocal(postId) }
        }
    }

    fun trackShare(postId: String) {
        viewModelScope.launch {
            runCatching { feedRepository.trackFeedEvent(postId, "share") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationFeedScreen(
    locationName: String,
    latitude: Double? = null,
    longitude: Double? = null,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit = {},
    onLocationClick: (name: String, lat: Double?, lon: Double?) -> Unit = { _, _, _ -> },
    onVideoClick: (String) -> Unit = onPostClick,
    onAppealClick: (String) -> Unit = {},
    onSendDirectMessage: (FeedPost) -> Unit = {},
    onNavigateToStoryEditor: ((Uri) -> Unit)? = null,
    onNavigateToPremium: () -> Unit = {},
    onOpenLink: (String) -> Unit = {},
    viewModel: LocationFeedViewModel = hiltViewModel(),
    commentsViewModel: CommentsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val internalLinkNavigator = LocalVistaInternalLinkNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var fullscreenMedia by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var showCommentsForPostId by remember { mutableStateOf<String?>(null) }
    var showShareForPost by remember { mutableStateOf<FeedPost?>(null) }
    var reportPostTarget by remember { mutableStateOf<FeedPost?>(null) }
    var isReporting by remember { mutableStateOf(false) }
    var editPostTarget by remember { mutableStateOf<FeedPost?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var activeStoryUserIndex by remember { mutableStateOf<Int?>(null) }

    val decodedLocation = remember(locationName) { locationName.trim() }

    LaunchedEffect(decodedLocation, latitude, longitude) {
        viewModel.bind(decodedLocation, latitude, longitude)
    }

    LaunchedEffect(listState, state.posts.size, state.hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && state.posts.isNotEmpty() && lastIndex >= state.posts.lastIndex) {
                    viewModel.loadMore()
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    VistaAppBarAmbientOverlay(modifier = Modifier.matchParentSize())
                    TopAppBar(
                        title = {
                            Text(
                                text = state.locationName.ifBlank { decodedLocation },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "بازگشت")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    openLocationInMapApp(
                                        context = context,
                                        name = state.locationName.ifBlank { decodedLocation },
                                        latitude = state.latitude,
                                        longitude = state.longitude,
                                    )
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Map,
                                    contentDescription = "مشاهده روی نقشه",
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                    )
                }
            },
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    state.isLoading && state.posts.isEmpty() && state.stories.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    state.posts.isEmpty() && state.stories.isEmpty() -> {
                        EmptyLocationState(
                            locationName = state.locationName.ifBlank { decodedLocation },
                            errorMessage = state.errorMessage,
                            onRetry = viewModel::refresh,
                            onOpenMap = {
                                openLocationInMapApp(
                                    context = context,
                                    name = state.locationName.ifBlank { decodedLocation },
                                    latitude = state.latitude,
                                    longitude = state.longitude,
                                )
                            },
                        )
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                        ) {
                            item(key = "location-hero") {
                                LocationHeroHeader(
                                    locationName = state.locationName.ifBlank { decodedLocation },
                                    postsCount = state.posts.size,
                                    storiesCount = state.stories.sumOf { it.stories.size },
                                    onOpenMap = {
                                        openLocationInMapApp(
                                            context = context,
                                            name = state.locationName.ifBlank { decodedLocation },
                                            latitude = state.latitude,
                                            longitude = state.longitude,
                                        )
                                    },
                                )
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }

                            if (state.stories.isNotEmpty()) {
                                item(key = "location-stories-rail") {
                                    LocationStoriesSection(
                                        storyUsers = state.stories,
                                        onStoryUserClick = { index -> activeStoryUserIndex = index },
                                    )
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }

                            if (state.posts.isNotEmpty()) {
                                if (state.stories.isNotEmpty()) {
                                    item(key = "location-posts-title") {
                                        Text(
                                            text = "پست‌های این مکان",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                                        )
                                    }
                                }
                                items(state.posts, key = FeedPost::id) { post ->
                                    VistaFeedPostCard(
                                        post = post,
                                        isOwnPost = !state.viewerUserId.isNullOrBlank() &&
                                            post.userId.equals(state.viewerUserId, ignoreCase = true),
                                        isBlueTick = state.isViewerBlueTick,
                                        hasPremiumEdit = state.viewerHasPremiumEdit,
                                        onPostClick = { onPostClick(post.id) },
                                        onVideoClick = { onVideoClick(post.id) },
                                        onMediaClick = { imgs, idx -> fullscreenMedia = imgs to idx },
                                        onAuthorClick = { onAuthorClick(post.userId) },
                                        onLikeClick = viewModel::toggleLike,
                                        onSaveClick = viewModel::toggleSave,
                                        onCommentClick = { showCommentsForPostId = post.id },
                                        onShareClick = { showShareForPost = post },
                                        onEditPost = { editPostTarget = post },
                                        onDeletePost = { viewModel.deletePost(post.id) },
                                        onReportPost = { reportPostTarget = post },
                                        onNotInterested = { viewModel.markNotInterested(post.id) },
                                        onVisibilityChange = { hideLike, hideComment ->
                                            viewModel.updateEngagementVisibility(post, hideLike, hideComment)
                                        },
                                        onHashtagClick = onHashtagClick,
                                        onMentionClick = onAuthorClick,
                                        onLocationClick = { clickedLoc ->
                                            if (LocationMatcher.matches(clickedLoc, state.locationName)) {
                                                viewModel.refresh()
                                            } else {
                                                onLocationClick(clickedLoc, null, null)
                                            }
                                        },
                                        onAppealClick = onAppealClick,
                                    )
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            } else {
                                item(key = "location-posts-empty-hint") {
                                    Text(
                                        text = "هنوز پستی برای این مکان ثبت نشده است.",
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                    )
                                }
                            }

                            if (state.isAppending) {
                                item(key = "location-append-loading") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(Modifier.size(24.dp))
                                    }
                                }
                            }

                            state.errorMessage?.takeIf { state.posts.isNotEmpty() }?.let { message ->
                                item(key = "location-append-error") {
                                    OutlinedButton(
                                        onClick = viewModel::loadMore,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    ) {
                                        Text(message)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        fullscreenMedia?.let { (imgs, idx) ->
            VistaFullscreenImageViewer(
                images = imgs,
                initialIndex = idx,
                onDismiss = { fullscreenMedia = null },
            )
        }

        showCommentsForPostId?.let { postId ->
            val postOwnerId = state.posts.firstOrNull { it.id == postId }?.userId
            CommentsBottomSheet(
                postId = postId,
                onDismissRequest = { showCommentsForPostId = null },
                viewModel = commentsViewModel,
                postOwnerId = postOwnerId,
                onAuthorClick = onAuthorClick,
                onHashtagClick = onHashtagClick,
                onNavigateToPremium = onNavigateToPremium,
            )
        }

        showShareForPost?.let { post ->
            PostShareBottomSheet(
                post = post,
                onDismiss = { showShareForPost = null },
                onShared = { viewModel.trackShare(post.id) },
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
            EditPostDialog(
                initialContent = target.content.orEmpty(),
                maxCharLength = state.maxPostChars,
                title = "ویرایش پست",
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

        activeStoryUserIndex?.let { startIndex ->
            Dialog(
                onDismissRequest = { activeStoryUserIndex = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                ),
            ) {
                StoryPlayerScreen(
                    initialUserIndex = startIndex,
                    customStoryUsers = state.stories,
                    onClose = { activeStoryUserIndex = null },
                    onOpenProfile = { uid ->
                        activeStoryUserIndex = null
                        onAuthorClick(uid)
                    },
                    onOpenLink = { url ->
                        activeStoryUserIndex = null
                        if (internalLinkNavigator?.openUrl(url) != true) {
                            onOpenLink(url)
                        }
                    },
                    onOpenHashtag = { tag ->
                        activeStoryUserIndex = null
                        onHashtagClick(tag)
                    },
                    onOpenLocation = { clickedName, clickedLat, clickedLon ->
                        activeStoryUserIndex = null
                        if (!LocationMatcher.matches(clickedName, state.locationName)) {
                            onLocationClick(clickedName, clickedLat, clickedLon)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LocationHeroHeader(
    locationName: String,
    postsCount: Int,
    storiesCount: Int,
    onOpenMap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF4B55).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = Color(0xFFFF4B55),
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = locationName,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            val stats = buildList {
                if (postsCount > 0) add("${postsCount.toString().toPersianDigits()} پست")
                if (storiesCount > 0) add("${storiesCount.toString().toPersianDigits()} استوری فعال")
            }.joinToString(" • ").ifBlank { "مکان برچسب‌شده" }
            Text(
                text = stats,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = onOpenMap,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Map,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "نقشه",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LocationStoriesSection(
    storyUsers: List<StoryUser>,
    onStoryUserClick: (Int) -> Unit,
) {
    val brandGradient = remember {
        Brush.linearGradient(
            colors = listOf(VistaBrandColors.Indigo, VistaBrandColors.Violet, VistaBrandColors.Pink),
        )
    }
    val closeFriendsGradient = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFF2ECC71), Color(0xFF00C853)),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "استوری‌های این مکان",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${storyUsers.sumOf { it.stories.size }.toString().toPersianDigits()} استوری",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(
                items = storyUsers,
                key = { idx, u -> "loc_story_${idx}_${u.id}" },
            ) { index, user ->
                val hasCloseFriendsUnseen = user.stories.any {
                    !it.isViewed && it.privacyType == StoryPrivacyType.CloseFriends
                }
                val hasUnseen = user.hasUnseenStories
                val ringModifier = when {
                    hasCloseFriendsUnseen -> Modifier.border(2.5.dp, closeFriendsGradient, CircleShape)
                    hasUnseen -> Modifier.border(2.5.dp, brandGradient, CircleShape)
                    else -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(74.dp)
                        .clickable { onStoryUserClick(index) }
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .then(ringModifier)
                                .padding(3.5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!user.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = user.avatarUrl,
                                    contentDescription = user.username,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        if (user.stories.size > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = user.stories.size.toString().toPersianDigits(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = user.username.ifBlank { user.fullName.orEmpty() }.ifBlank { "کاربر" },
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (user.isVerified) {
                            Spacer(Modifier.width(2.dp))
                            VistaVerificationBadge(
                                isVerified = true,
                                verificationType = when (user.verificationType) {
                                    StoryVerificationType.Blue -> "blue"
                                    StoryVerificationType.Gold -> "gold"
                                    StoryVerificationType.Black -> "black"
                                    StoryVerificationType.None -> null
                                },
                                size = 12.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLocationState(
    locationName: String,
    errorMessage: String?,
    onRetry: () -> Unit,
    onOpenMap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF4B55).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = Color(0xFFFF4B55),
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = locationName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = errorMessage ?: "هنوز پست یا استوری فعالی برای این موقعیت مکانی وجود ندارد.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onRetry) {
                Text("تلاش دوباره")
            }
            OutlinedButton(onClick = onOpenMap) {
                Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("مشاهده روی نقشه")
            }
        }
    }
}

private fun openLocationInMapApp(
    context: Context,
    name: String,
    latitude: Double?,
    longitude: Double?,
) {
    val geoUri = if (latitude != null && longitude != null && (latitude != 0.0 || longitude != 0.0)) {
        Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(name)})")
    } else {
        Uri.parse("geo:0,0?q=${Uri.encode(name)}")
    }
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, geoUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
