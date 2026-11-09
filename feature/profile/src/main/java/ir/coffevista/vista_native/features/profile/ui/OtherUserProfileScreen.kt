package ir.coffevista.vista_native.features.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassScrollSync
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import ir.coffevista.vista_native.features.profile.ui.components.ProfileShareBottomSheet
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.core.designsystem.component.VistaEmptyState
import ir.coffevista.vista_native.core.designsystem.component.VistaEditPostDialog
import ir.coffevista.vista_native.core.designsystem.component.VistaErrorState
import ir.coffevista.vista_native.core.designsystem.component.VistaPremiumPromptDialog
import ir.coffevista.vista_native.core.designsystem.component.VistaReportReasonDialog
import ir.coffevista.vista_native.features.profile.R
import ir.coffevista.vista_native.features.profile.data.FollowState
import ir.coffevista.vista_native.features.profile.data.PublicProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    viewModel: OtherUserProfileViewModel,
    onBack: () -> Unit,
    onSelfProfile: () -> Unit,
    postsState: ProfilePostsPresentationState = ProfilePostsPresentationState(),
    onPostsRefresh: () -> Unit = {},
    onPostsLoadMore: () -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onReelClick: (String) -> Unit = onPostClick,
    onLikeClick: (String, Boolean, Long) -> Unit = { _, _, _ -> },
    onSaveClick: (String, Boolean) -> Unit = { _, _ -> },
    onEditPost: (ProfilePostUiModel, String, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onDeletePost: (String) -> Unit = {},
    onReportPost: (ProfilePostUiModel, String, String?, (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    onReportUser: (String, String?, (Boolean, String?) -> Unit) -> Unit = { _, _, _ -> },
    onVisibilityChange: (ProfilePostUiModel, Boolean?, Boolean?) -> Unit = { _, _, _ -> },
    onMessage: (() -> Unit)? = null,
    onOpenFollowers: ((userId: String, initialTab: Int) -> Unit)? = null,
    onNavigateToPricing: (() -> Unit)? = null,
    onHighlightClick: ((ir.coffevista.vista_native.features.profile.data.ProfileHighlightDto) -> Unit)? = null,
    onNavigateToStoryEditor: ((android.net.Uri) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val activeNote by viewModel.activeNote.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OtherUserProfileEffect.NavigateBack -> onBack()
                OtherUserProfileEffect.RedirectToOwnProfile -> onSelfProfile()
                is OtherUserProfileEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    OtherUserProfileContent(
        state = state,
        highlights = highlights,
        activeNote = activeNote,
        snackbarHostState = snackbarHostState,
        postsState = postsState,
        onPostsRefresh = onPostsRefresh,
        onPostsLoadMore = onPostsLoadMore,
        onPostClick = onPostClick,
        onReelClick = onReelClick,
        onLikeClick = onLikeClick,
        onSaveClick = onSaveClick,
        onEditPost = onEditPost,
        onDeletePost = onDeletePost,
        onReportPost = onReportPost,
        onReportUser = viewModel::reportProfile,
        onVisibilityChange = onVisibilityChange,
        onMessage = onMessage,
        onOpenFollowers = onOpenFollowers,
        onNavigateToPricing = onNavigateToPricing,
        onHighlightClick = onHighlightClick,
        onNavigateToStoryEditor = onNavigateToStoryEditor,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OtherUserProfileContent(
    state: OtherUserProfileUiState,
    highlights: List<ir.coffevista.vista_native.features.profile.data.ProfileHighlightDto> = emptyList(),
    activeNote: ir.coffevista.vista_native.features.profile.data.ProfileNoteDto? = null,
    snackbarHostState: SnackbarHostState,
    onAction: (OtherUserProfileAction) -> Unit,
    postsState: ProfilePostsPresentationState = ProfilePostsPresentationState(),
    onPostsRefresh: () -> Unit = {},
    onPostsLoadMore: () -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onReelClick: (String) -> Unit = onPostClick,
    onLikeClick: (String, Boolean, Long) -> Unit = { _, _, _ -> },
    onSaveClick: (String, Boolean) -> Unit = { _, _ -> },
    onEditPost: (ProfilePostUiModel, String, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onDeletePost: (String) -> Unit = {},
    onReportPost: (ProfilePostUiModel, String, String?, (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    onReportUser: (String, String?, (Boolean, String?) -> Unit) -> Unit = { _, _, _ -> },
    onVisibilityChange: (ProfilePostUiModel, Boolean?, Boolean?) -> Unit = { _, _, _ -> },
    onMessage: (() -> Unit)? = null,
    onOpenFollowers: ((userId: String, initialTab: Int) -> Unit)? = null,
    onNavigateToPricing: (() -> Unit)? = null,
    onHighlightClick: ((ir.coffevista.vista_native.features.profile.data.ProfileHighlightDto) -> Unit)? = null,
    onNavigateToStoryEditor: ((android.net.Uri) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showOptionsSheet by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showShareForPost by remember { mutableStateOf<ir.coffevista.vista_native.features.feed.data.FeedPost?>(null) }
    var editPostTarget by remember { mutableStateOf<ProfilePostUiModel?>(null) }
    var isEditingPost by remember { mutableStateOf(false) }
    var reportPostTarget by remember { mutableStateOf<ProfilePostUiModel?>(null) }
    var isReportingPost by remember { mutableStateOf(false) }
    var showReportUserDialog by remember { mutableStateOf(false) }
    var isReportingUser by remember { mutableStateOf(false) }
    var premiumPromptFeature by remember { mutableStateOf<String?>(null) }
    var showAccountDetails by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    val otherProfileGlassState = rememberVistaGlassState()
    val otherProfileListState = rememberLazyListState()
    VistaGlassScrollSync(otherProfileGlassState, otherProfileListState)
    val isDarkOtherProfile = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarHeight = statusBarTop + 56.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .vistaGlassSource(otherProfileGlassState)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                state.userNotFound -> VistaEmptyState(
                    title = "کاربر یافت نشد",
                    message = "این نمایه در دسترس نیست.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topBarHeight)
                        .testTag(OtherProfileTestTags.NotFound),
                )
                state.profile == null && state.initialError != null -> VistaErrorState(
                    title = "خطا در دریافت نمایه",
                    message = state.initialError.messageFa,
                    onRetry = { onAction(OtherUserProfileAction.Retry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topBarHeight)
                        .testTag(OtherProfileTestTags.Error),
                )
                state.profile == null && state.isInitialLoading -> Box(Modifier.padding(top = topBarHeight)) {
                    OtherProfileLoading()
                }
                state.profile != null -> {
                    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }

                    if (showAccountDetails) {
                        val profile = state.profile
                        AccountDetailsScreen(
                            profile = profile.headerModel(),
                            userId = profile.userId,
                            onBack = { showAccountDetails = false },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing || postsState.isRefreshing,
                            onRefresh = {
                                onAction(OtherUserProfileAction.Refresh)
                                onPostsRefresh()
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(OtherProfileTestTags.PullToRefresh),
                        ) {
                            val profile = state.profile
                            ProfileParityList(
                                header = profile.headerModel(),
                                topContentPadding = topBarHeight,
                                listState = otherProfileListState,
                                postsState = postsState.copy(
                                    isOffline = postsState.isOffline || state.isStale,
                                ),
                                privatePosts = profile.isPrivate &&
                                    profile.followState != FollowState.Following,
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                onPostClick = onPostClick,
                                onReelClick = onReelClick,
                                onLikeClick = onLikeClick,
                                onSaveClick = onSaveClick,
                                onShareClick = { post -> showShareForPost = post.toFeedPost() },
                                onEditPost = { post -> editPostTarget = post },
                                onDeletePost = onDeletePost,
                                onReportPost = { post -> reportPostTarget = post },
                                onVisibilityChange = onVisibilityChange,
                                onPremiumPrompt = { feature -> premiumPromptFeature = feature },
                                onLoadMore = onPostsLoadMore,
                                onRefresh = onPostsRefresh,
                                activeNote = activeNote,
                                onNoteClick = {
                                    if (onMessage != null) {
                                        onMessage.invoke()
                                    } else {
                                        android.widget.Toast.makeText(context, activeNote?.content.orEmpty(), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onMemberBadgeClick = {
                                    showAccountDetails = true
                                },
                                onFollowersClick = {
                                    onOpenFollowers?.invoke(profile.userId, 0)
                                },
                                onFollowingClick = {
                                    onOpenFollowers?.invoke(profile.userId, 1)
                                },
                                modifier = Modifier.testTag(OtherProfileTestTags.Content),
                                actionContent = {
                                    OtherProfileActions(
                                        profile = profile,
                                        pending = state.followMutationPending,
                                        onAction = onAction,
                                        onMessage = onMessage,
                                    )
                                },
                                highlightsContent = {
                                    ProfileHighlightsRow(
                                        highlights = highlights,
                                        isOwnProfile = false,
                                        onHighlightClick = { highlight ->
                                            if (onHighlightClick != null) {
                                                onHighlightClick.invoke(highlight)
                                            } else {
                                                android.widget.Toast.makeText(context, highlight.title, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    )
                                },
                            )
                            if (state.isStale) {
                                Text(
                                    text = "نمایش نسخه ذخیره‌شده",
                                    modifier = Modifier
                                        .padding(top = topBarHeight)
                                        .fillMaxWidth()
                                        .background(androidx.compose.ui.graphics.Color(0xFFEF6C00))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .testTag(OtherProfileTestTags.Stale),
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!showAccountDetails) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .vistaGlass(
                        state = otherProfileGlassState,
                        shape = RectangleShape,
                        tint = MaterialTheme.colorScheme.surface,
                        isDark = isDarkOtherProfile,
                        blurRadius = VistaGlassTokens.BarBlur,
                        rimWidth = 0.dp,
                        shadowElevation = 3.dp,
                    )
                    .statusBarsPadding(),
            ) {
                OtherProfileAppBar(
                    username = state.profile?.username,
                    verified = state.profile?.isVerified == true,
                    verificationType = state.profile?.verificationType,
                    onBack = { onAction(OtherUserProfileAction.BackClicked) },
                    onMoreClick = { showOptionsSheet = true },
                )
            }
        }

        if (showOptionsSheet && state.profile != null) {
                ProfileOptionsBottomSheet(
                    onDismiss = { showOptionsSheet = false },
                    onShare = {
                        showOptionsSheet = false
                        showShareSheet = true
                    },
                    onCopyLink = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("https://coffevista.ir/u/${state.profile.username ?: ""}"))
                        android.widget.Toast.makeText(context, "لینک پروفایل کپی شد", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onReport = {
                        showReportUserDialog = true
                    },
                    onBlock = {
                        android.widget.Toast.makeText(context, "کاربر مسدود شد", android.widget.Toast.LENGTH_SHORT).show()
                    },
                )
            }

            if (showShareSheet && state.profile != null) {
                val profile = state.profile
                ProfileShareBottomSheet(
                    username = profile.username.orEmpty(),
                    fullName = profile.fullName,
                    avatarUrl = profile.avatarUrl,
                    bio = profile.bio,
                    isVerified = profile.isVerified,
                    onDismiss = { showShareSheet = false },
                    onSendDirectMessage = onMessage,
                    onShowQrCard = null,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )

            editPostTarget?.let { target ->
                val maxLen = if (postsState.viewerIsBlueTick) 10000 else if (postsState.viewerHasPremiumEdit) 1000 else 500
                val title = if (postsState.viewerIsBlueTick && target.userId != postsState.viewerUserId) "ویرایش ناظر" else "ویرایش پست"
                VistaEditPostDialog(
                    initialContent = target.content.orEmpty(),
                    maxCharLength = maxLen,
                    title = title,
                    isSubmitting = isEditingPost,
                    onDismiss = { if (!isEditingPost) editPostTarget = null },
                    onConfirm = { newContent ->
                        isEditingPost = true
                        onEditPost(target, newContent) {
                            isEditingPost = false
                            editPostTarget = null
                        }
                    },
                )
            }

            reportPostTarget?.let { target ->
                VistaReportReasonDialog(
                    isSubmitting = isReportingPost,
                    onDismiss = { if (!isReportingPost) reportPostTarget = null },
                    onSubmit = { reason, details ->
                        isReportingPost = true
                        onReportPost(target, reason, details) {
                            isReportingPost = false
                            reportPostTarget = null
                        }
                    },
                )
            }

            if (showReportUserDialog) {
                val usernameText = state.profile?.username?.let { "@$it" } ?: (state.profile?.fullName ?: "کاربر")
                VistaReportReasonDialog(
                    title = "گزارش کاربر $usernameText",
                    isSubmitting = isReportingUser,
                    onDismiss = { if (!isReportingUser) showReportUserDialog = false },
                    onSubmit = { reason, details ->
                        isReportingUser = true
                        onReportUser(reason, details) { success, errorMsg ->
                            isReportingUser = false
                            showReportUserDialog = false
                            val msg = if (success) "گزارش تخلف کاربر با موفقیت ثبت شد" else (errorMsg ?: "خطا در ثبت گزارش")
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

            premiumPromptFeature?.let { feature ->
                VistaPremiumPromptDialog(
                    feature = feature,
                    onDismiss = { premiumPromptFeature = null },
                    onUpgradeClick = {
                        premiumPromptFeature = null
                        onNavigateToPricing?.invoke()
                    },
                )
            }

            showShareForPost?.let { post ->
                ir.coffevista.vista_native.features.feed.ui.PostShareBottomSheet(
                    post = post,
                    onDismiss = { showShareForPost = null },
                    onShared = {},
                    onSendDirectMessage = onMessage?.let { cb -> { _ -> cb() } },
                    onNavigateToStoryEditor = onNavigateToStoryEditor,
                )
            }
    }
}

@Composable
private fun OtherProfileAppBar(
    username: String?,
    verified: Boolean,
    verificationType: String? = null,
    role: String? = null,
    onBack: () -> Unit,
    onMoreClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = username ?: "نمایه",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            if (verified) {
                VerificationBadgeIcon(
                    isVerified = true,
                    verificationType = verificationType,
                    role = role,
                    size = 18.dp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .testTag(OtherProfileTestTags.Back),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_profile_back),
                contentDescription = "بازگشت",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_profile_more),
                contentDescription = "گزینه‌های بیشتر",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = .5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun OtherProfileActions(
    profile: PublicProfile,
    pending: FollowMutationPending?,
    onAction: (OtherUserProfileAction) -> Unit,
    onMessage: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val action = when (profile.followState) {
            FollowState.NotFollowing -> OtherUserProfileAction.FollowClicked
            FollowState.Following -> OtherUserProfileAction.UnfollowClicked
            FollowState.Requested,
            FollowState.Unavailable,
            -> null
        }
        val label = when {
            pending == FollowMutationPending.Follow -> "در حال دنبال کردن..."
            pending == FollowMutationPending.Unfollow -> "در حال لغو..."
            profile.followState == FollowState.Following -> "دنبال می‌کنید"
            profile.followState == FollowState.Requested -> "در انتظار تأیید"
            profile.followState == FollowState.Unavailable -> "در دسترس نیست"
            else -> "دنبال کردن"
        }
        ProfileActionButton(
            label = label,
            onClick = action?.let { { onAction(it) } },
            primary = profile.followState == FollowState.NotFollowing,
            pending = pending != null,
            modifier = Modifier
                .weight(1f)
                .testTag(OtherProfileTestTags.FollowButton),
        )
        if (profile.messagePrivacy != "nobody") {
            ProfileActionButton(
                label = "پیام",
                onClick = onMessage,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OtherProfileLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag(OtherProfileTestTags.Loading),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .height(84.dp)
                    .fillMaxWidth(.22f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        androidx.compose.foundation.shape.CircleShape,
                    ),
            )
            Box(
                Modifier
                    .padding(start = 28.dp)
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    ),
            )
        }
    }
}

private fun PublicProfile.headerModel() = ProfileHeaderModel(
    fullName = fullName,
    username = username,
    bio = bio,
    avatarUrl = avatarUrl,
    isVerified = isVerified,
    verificationType = verificationType,
    isPremium = isPremium,
    isPrivate = isPrivate,
    postCount = postsCount,
    followerCount = followersCount,
    followingCount = followingCount,
    joinOrder = joinOrder,
    createdAt = createdAt,
)

internal object OtherProfileTestTags {
    const val Back = "other-profile-back"
    const val Loading = "other-profile-loading"
    const val Error = "other-profile-error"
    const val NotFound = "other-profile-not-found"
    const val Content = "other-profile-content"
    const val PullToRefresh = "other-profile-pull-to-refresh"
    const val Stale = "other-profile-stale"
    const val FollowButton = "other-profile-follow-button"
}
