package ir.coffevista.vista_native.features.profile.ui

import android.net.Uri
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import ir.coffevista.vista_native.core.model.note.ProfileNotePrompts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassScrollSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.style.TextDirection
import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import ir.coffevista.vista_native.features.profile.ui.components.ProfileShareBottomSheet
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.core.designsystem.component.VistaEditPostDialog
import ir.coffevista.vista_native.core.designsystem.component.VistaErrorState
import ir.coffevista.vista_native.core.designsystem.component.VistaPremiumPromptDialog
import ir.coffevista.vista_native.core.designsystem.component.VistaReportReasonDialog
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import ir.coffevista.vista_native.features.profile.R
import ir.coffevista.vista_native.features.profile.data.ProfileHighlightDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnProfileScreen(
    viewModel: OwnProfileViewModel,
    onLogout: () -> Unit,
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
    onVisibilityChange: (ProfilePostUiModel, Boolean?, Boolean?) -> Unit = { _, _, _ -> },
    onEditProfile: (() -> Unit)? = null,
    onShareProfile: (() -> Unit)? = null,
    onAddPost: (() -> Unit)? = null,
    onAddStory: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onOpenFollowers: ((userId: String, initialTab: Int) -> Unit)? = null,
    onOpenQrScanner: (() -> Unit)? = null,
    onNavigateToPricing: (() -> Unit)? = null,
    onHighlightClick: ((ProfileHighlightDto) -> Unit)? = null,
    onNavigateToStoryEditor: ((Uri) -> Unit)? = null,
    onSendDirectMessage: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val activeNote by viewModel.activeNote.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showOptionsSheet by rememberSaveable { mutableStateOf(false) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    var showVistaIdCard by rememberSaveable { mutableStateOf(false) }
    var showAccountDetails by rememberSaveable { mutableStateOf(false) }
    var showCreateHighlightDialog by remember { mutableStateOf(false) }
    var highlightForOptions by remember { mutableStateOf<ProfileHighlightDto?>(null) }
    var highlightToEdit by remember { mutableStateOf<ProfileHighlightDto?>(null) }
    var highlightToDelete by remember { mutableStateOf<ProfileHighlightDto?>(null) }
    var showNoteInputSheet by remember { mutableStateOf(false) }
    var showShareForPost by remember { mutableStateOf<ir.coffevista.vista_native.features.feed.data.FeedPost?>(null) }
    var editPostTarget by remember { mutableStateOf<ProfilePostUiModel?>(null) }
    var isEditingPost by remember { mutableStateOf(false) }
    var reportPostTarget by remember { mutableStateOf<ProfilePostUiModel?>(null) }
    var isReportingPost by remember { mutableStateOf(false) }
    var premiumPromptFeature by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentUsername = (uiState as? OwnProfileUiState.Content)?.profile?.username

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadNote()
        viewModel.loadHighlights()
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl,
    ) {
        val contentState = uiState as? OwnProfileUiState.Content
        if (showAccountDetails && contentState != null) {
            val profile = contentState.profile
            AccountDetailsScreen(
                profile = ProfileHeaderModel(
                    fullName = profile.fullName,
                    username = profile.username,
                    bio = profile.bio,
                    avatarUrl = profile.avatarUrl,
                    isVerified = profile.isVerified,
                    verificationType = profile.verificationType,
                    role = profile.accountType,
                    isPremium = profile.premiumDaysRemaining?.let { it > 0 }
                        ?: profile.subscriptionPlan?.let { it.isNotBlank() && it != "free" }
                        ?: false,
                    isPrivate = profile.isPrivate,
                    postCount = profile.postCount,
                    followerCount = profile.followerCount,
                    followingCount = profile.followingCount,
                    joinOrder = profile.joinOrder,
                    createdAt = profile.createdAt,
                    isOwnProfile = true,
                ),
                userId = profile.userId,
                onBack = { showAccountDetails = false },
                modifier = modifier,
            )
        } else {
            val profileGlassState = rememberVistaGlassState()
            val profileListState = rememberLazyListState()
            VistaGlassScrollSync(profileGlassState, profileListState)
            val isDarkProfile = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val profileTopBarHeight = statusBarTop + 56.dp
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .vistaGlassSource(profileGlassState)
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    when (val state = uiState) {
                        OwnProfileUiState.Loading -> Box(Modifier.padding(top = profileTopBarHeight)) {
                            ProfileLoadingSkeleton()
                        }
                        is OwnProfileUiState.Error -> VistaErrorState(
                            title = "خطا در دریافت اطلاعات",
                            message = state.error.messageFa,
                            onRetry = { viewModel.handleAction(OwnProfileAction.Retry) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = profileTopBarHeight),
                        )
                        is OwnProfileUiState.Content -> PullToRefreshBox(
                            isRefreshing = state.isRefreshing || postsState.isRefreshing,
                            onRefresh = {
                                viewModel.handleAction(OwnProfileAction.Refresh)
                                onPostsRefresh()
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(OwnProfileTestTags.PullToRefresh),
                        ) {
                            val profile = state.profile
                            val headerModel = ProfileHeaderModel(
                                fullName = profile.fullName,
                                username = profile.username,
                                bio = profile.bio,
                                avatarUrl = profile.avatarUrl,
                                isVerified = profile.isVerified,
                                verificationType = profile.verificationType,
                                role = profile.accountType,
                                isPremium = profile.premiumDaysRemaining?.let { it > 0 }
                                    ?: profile.subscriptionPlan?.let { it.isNotBlank() && it != "free" }
                                    ?: false,
                                isPrivate = profile.isPrivate,
                                postCount = profile.postCount,
                                followerCount = profile.followerCount,
                                followingCount = profile.followingCount,
                                joinOrder = profile.joinOrder,
                                createdAt = profile.createdAt,
                                isOwnProfile = true,
                            )

                            ProfileParityList(
                                header = headerModel,
                                topContentPadding = profileTopBarHeight,
                                listState = profileListState,
                                postsState = postsState,
                                privatePosts = false,
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
                                onNoteClick = { showNoteInputSheet = true },
                                onAvatarClick = { onAddStory?.invoke() },
                                onAvatarAddClick = onAddStory ?: {
                                    Toast.makeText(context, "افزودن استوری در دسترس نیست", Toast.LENGTH_SHORT).show()
                                },
                                onMemberBadgeClick = {
                                    showAccountDetails = true
                                },
                                onFollowersClick = {
                                    val userId = (uiState as? OwnProfileUiState.Content)?.profile?.userId
                                    if (userId != null) onOpenFollowers?.invoke(userId, 0)
                                },
                                onFollowingClick = {
                                    val userId = (uiState as? OwnProfileUiState.Content)?.profile?.userId
                                    if (userId != null) onOpenFollowers?.invoke(userId, 1)
                                },
                                actionContent = {
                                    OwnProfileActions(
                                        onEditProfile = onEditProfile ?: {
                                            Toast.makeText(context, "ویرایش پروفایل", Toast.LENGTH_SHORT).show()
                                        },
                                        onShareProfile = onShareProfile ?: {
                                            showShareSheet = true
                                        },
                                    )
                                },
                                highlightsContent = {
                                    ProfileHighlightsRow(
                                        highlights = highlights,
                                        isOwnProfile = true,
                                        onAddClick = {
                                            showCreateHighlightDialog = true
                                        },
                                        onHighlightClick = { highlight ->
                                            if (onHighlightClick != null) {
                                                onHighlightClick.invoke(highlight)
                                            } else {
                                                Toast.makeText(context, highlight.title, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onHighlightLongClick = { highlight ->
                                            highlightForOptions = highlight
                                        },
                                    )
                                },
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .vistaGlass(
                            state = profileGlassState,
                            shape = RectangleShape,
                            tint = MaterialTheme.colorScheme.surface,
                            isDark = isDarkProfile,
                            blurRadius = VistaGlassTokens.BarBlur,
                            rimWidth = 0.dp,
                            shadowElevation = 3.dp,
                        )
                        .statusBarsPadding(),
                ) {
                    OwnProfileAppBar(
                        username = currentUsername,
                        verified = (uiState as? OwnProfileUiState.Content)?.profile?.isVerified == true,
                        verificationType = (uiState as? OwnProfileUiState.Content)?.profile?.verificationType,
                        role = (uiState as? OwnProfileUiState.Content)?.profile?.accountType,
                        onSettingsClick = {
                            onSettingsClick?.invoke()
                        },
                        onOptionsClick = {
                            showOptionsSheet = true
                        },
                    )
                }

                // Highlight Management Dialogs & Bottom Sheet
                if (showCreateHighlightDialog) {
                    CreateHighlightDialog(
                        onDismiss = { showCreateHighlightDialog = false },
                        onConfirm = { title, coverUrl ->
                            showCreateHighlightDialog = false
                            viewModel.createHighlight(title, coverUrl) { success ->
                                if (success) {
                                    Toast.makeText(context, "هایلایت ایجاد شد", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "خطا در ایجاد هایلایت", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    )
                }

                highlightForOptions?.let { hl ->
                    HighlightOptionsBottomSheet(
                        highlight = hl,
                        onDismiss = { highlightForOptions = null },
                        onPlay = {
                            highlightForOptions = null
                            onHighlightClick?.invoke(hl)
                        },
                        onEdit = {
                            highlightForOptions = null
                            highlightToEdit = hl
                        },
                        onDelete = {
                            highlightForOptions = null
                            highlightToDelete = hl
                        },
                    )
                }

                highlightToEdit?.let { hl ->
                    EditHighlightDialog(
                        initialTitle = hl.title,
                        initialCoverUrl = hl.coverUrl,
                        onDismiss = { highlightToEdit = null },
                        onConfirm = { title, coverUrl ->
                            highlightToEdit = null
                            viewModel.updateHighlight(hl.id, title, coverUrl) { success ->
                                if (success) {
                                    Toast.makeText(context, "هایلایت ویرایش شد", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "خطا در ویرایش هایلایت", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    )
                }

                highlightToDelete?.let { hl ->
                    AlertDialog(
                        onDismissRequest = { highlightToDelete = null },
                        title = { Text("حذف هایلایت", fontWeight = FontWeight.Bold) },
                        text = { Text("آیا از حذف هایلایت «${hl.title}» مطمئن هستید؟") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val idToDelete = hl.id
                                    highlightToDelete = null
                                    viewModel.deleteHighlight(idToDelete) { success ->
                                        if (success) {
                                            Toast.makeText(context, "هایلایت حذف شد", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "خطا در حذف هایلایت", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            ) {
                                Text("حذف")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { highlightToDelete = null }) {
                                Text("انصراف")
                            }
                        },
                    )
                }

                // Profile Note Input Bottom Sheet
                if (showNoteInputSheet) {
                    NoteInputBottomSheet(
                        currentNote = activeNote?.content,
                        onDismiss = { showNoteInputSheet = false },
                        onSave = { content ->
                            showNoteInputSheet = false
                            viewModel.upsertNote(content) { success ->
                                if (success) {
                                    Toast.makeText(context, "یادداشت ذخیره شد", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "خطا در ثبت یادداشت", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDelete = if (activeNote != null) {
                            {
                                showNoteInputSheet = false
                                viewModel.deleteNote { success ->
                                    if (success) {
                                        Toast.makeText(context, "یادداشت حذف شد", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else null,
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    ProfileFAB(
                        onClick = {
                            onAddPost?.invoke() ?: Toast.makeText(context, "ایجاد پست جدید", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp),
                    )
                }

                // Options Bottom Sheet
                if (showOptionsSheet) {
                    ProfileOptionsBottomSheet(
                        onDismiss = { showOptionsSheet = false },
                        onShare = {
                            showOptionsSheet = false
                            showShareSheet = true
                        },
                        onCopyLink = {
                            val link = "https://coffevista.ir/u/${currentUsername ?: ""}"
                            clipboardManager.setText(AnnotatedString(link))
                            Toast.makeText(context, "لینک پروفایل کپی شد", Toast.LENGTH_SHORT).show()
                        },
                    )
                }

                // Profile Share Bottom Sheet
                if (showShareSheet && contentState != null) {
                    val profile = contentState.profile
                    ProfileShareBottomSheet(
                        username = profile.username.orEmpty(),
                        fullName = profile.fullName,
                        avatarUrl = profile.avatarUrl,
                        bio = profile.bio,
                        isVerified = profile.isVerified,
                        onDismiss = { showShareSheet = false },
                        onSendDirectMessage = null,
                        onShowQrCard = { showVistaIdCard = true },
                    )
                }

                // Vista ID Card Dialog with 3D Flip
                if (showVistaIdCard && contentState != null) {
                    val profile = contentState.profile
                    VistaIdCardDialog(
                        user = ProfileHeaderModel(
                            fullName = profile.fullName,
                            username = profile.username,
                            bio = profile.bio,
                            avatarUrl = profile.avatarUrl,
                            isVerified = profile.isVerified,
                            verificationType = profile.verificationType,
                            role = profile.accountType,
                            isPremium = profile.premiumDaysRemaining?.let { it > 0 }
                                ?: profile.subscriptionPlan?.let { it.isNotBlank() && it != "free" }
                                ?: false,
                            isPrivate = profile.isPrivate,
                            postCount = profile.postCount,
                            followerCount = profile.followerCount,
                            followingCount = profile.followingCount,
                            joinOrder = profile.joinOrder,
                            createdAt = profile.createdAt,
                            isOwnProfile = true,
                        ),
                        userId = profile.userId,
                        onDismissRequest = { showVistaIdCard = false },
                    )
                }

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
                        onSendDirectMessage = onSendDirectMessage?.let { callback -> { _ -> callback() } },
                        onNavigateToStoryEditor = onNavigateToStoryEditor,
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnProfileAppBar(
    username: String?,
    verified: Boolean,
    verificationType: String? = null,
    role: String? = null,
    onSettingsClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
    ) {
        // Start in RTL (Right side of screen): Username & Verified badge
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = username ?: "نمایه",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (verified) {
                VerificationBadgeIcon(
                    isVerified = true,
                    verificationType = verificationType,
                    role = role,
                    size = 18.dp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        // End in RTL (Left side of screen): more options, then settings.
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_profile_more),
                    contentDescription = "گزینه‌های بیشتر",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_profile_settings),
                    contentDescription = "تنظیمات",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun OwnProfileActions(
    onEditProfile: (() -> Unit)?,
    onShareProfile: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProfileActionButton(
            label = "ویرایش پروفایل",
            onClick = onEditProfile,
            modifier = Modifier.weight(1f),
        )
        ProfileActionButton(
            label = "اشتراک‌گذاری",
            onClick = onShareProfile,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun ProfileActionButton(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    pending: Boolean = false,
) {
    val interaction = if (onClick != null) {
        Modifier.clickable(enabled = !pending, onClick = onClick)
    } else {
        Modifier.semantics { disabled() }
    }
    Box(
        modifier = modifier
            .height(36.dp)
            .background(
                color = if (primary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(8.dp),
            )
            .then(interaction),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (primary) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProfileLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag(OwnProfileTestTags.Loading),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(84.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
            )
            Spacer(Modifier.width(28.dp))
            repeat(3) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(16.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(5.dp),
                            ),
                    )
                    Spacer(Modifier.height(7.dp))
                    Box(
                        Modifier
                            .width(52.dp)
                            .height(12.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(5.dp),
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .width(140.dp)
                .height(15.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(5.dp)),
        )
        Spacer(Modifier.height(9.dp))
        Box(
            Modifier
                .fillMaxWidth(.7f)
                .height(13.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(5.dp)),
        )
    }
}

internal object OwnProfileTestTags {
    const val Loading = "own-profile-loading"
    const val PullToRefresh = "own-profile-pull-to-refresh"
}

@Composable
private fun CreateHighlightDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, coverUrl: String?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("هایلایت جدید", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان هایلایت") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = { coverUrl = it },
                    label = { Text("لینک کاور (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), coverUrl.trim().ifBlank { null })
                    }
                },
                enabled = title.isNotBlank(),
            ) {
                Text("ایجاد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        },
    )
}

@Composable
private fun EditHighlightDialog(
    initialTitle: String,
    initialCoverUrl: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, coverUrl: String?) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var coverUrl by remember { mutableStateOf(initialCoverUrl.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("ویرایش هایلایت", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان هایلایت") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = { coverUrl = it },
                    label = { Text("لینک کاور") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), coverUrl.trim().ifBlank { null })
                    }
                },
                enabled = title.isNotBlank(),
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HighlightOptionsBottomSheet(
    highlight: ProfileHighlightDto,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = "هایلایت «${highlight.title}»",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("مشاهده استوری‌ها", fontSize = 15.sp)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text("ویرایش عنوان و کاور", fontSize = 15.sp)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDelete() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text("حذف هایلایت", fontSize = 15.sp, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteInputBottomSheet(
    currentNote: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var content by remember { mutableStateOf(currentNote.orEmpty()) }
    var activePrompt by remember { mutableStateOf(ProfileNotePrompts.randomPrompt()) }
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Live Preview Card with thought bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isDark) Color(0xFF141923) else Color(0xFFF1F5F9)
                    )
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val previewText = content.trim().ifEmpty { activePrompt }
                    val (fontSize, lineHeight, maxLines) = when {
                        previewText.length <= 14 -> Triple(12.sp, 16.sp, 2)
                        previewText.length <= 34 -> Triple(10.5.sp, 14.sp, 2)
                        else -> Triple(9.5.sp, 12.5.sp, 3)
                    }

                    Box(
                        modifier = Modifier
                            .widthIn(min = 60.dp, max = 220.dp)
                            .shadow(6.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xF21C2230) else Color(0xF8FFFFFF))
                            .border(
                                1.dp,
                                if (content.isNotBlank()) {
                                    if (isDark) Color(0x4DFFFFFF) else Color(0x22000000)
                                } else {
                                    Color(0xFF2196F3).copy(alpha = 0.6f)
                                },
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = previewText,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = fontSize,
                                lineHeight = lineHeight,
                                fontWeight = FontWeight.SemiBold,
                                color = if (content.isBlank()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                } else {
                                    if (isDark) Color.White else Color(0xFF0F172A)
                                },
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                textDirection = TextDirection.ContentOrRtl,
                            ),
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xF21C2230) else Color(0xF8FFFFFF))
                            .border(0.5.dp, if (isDark) Color(0x38FFFFFF) else Color(0x20000000), CircleShape),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xF21C2230) else Color(0xF8FFFFFF))
                            .border(0.5.dp, if (isDark) Color(0x38FFFFFF) else Color(0x20000000), CircleShape),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Header with Random Prompt Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "یادداشت وضعیت",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "۲۴ ساعت در بالای تصویر نمایه شما نمایش داده می‌شود.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { activePrompt = ProfileNotePrompts.randomPrompt() },
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "ایده دیگر",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick suggestion chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ProfileNotePrompts.quickChips) { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (content == chip) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { content = chip }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = chip,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (content == chip) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Input with Colored Counter
            val charColor = when {
                content.length <= 44 -> MaterialTheme.colorScheme.onSurfaceVariant
                content.length <= 54 -> Color(0xFFFFA000)
                else -> Color(0xFFE53935)
            }

            OutlinedTextField(
                value = content,
                onValueChange = {
                    if (it.length <= 60) content = it
                },
                label = { Text(activePrompt) },
                supportingText = {
                    Text(
                        text = "${content.length}/60",
                        modifier = Modifier.fillMaxWidth(),
                        color = charColor,
                        fontWeight = if (content.length > 54) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        if (content.isNotBlank()) {
                            onSave(content.trim())
                        }
                    },
                    enabled = content.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("اشتراک‌گذاری یادداشت", fontWeight = FontWeight.SemiBold)
                }
                if (!currentNote.isNullOrBlank() && onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("حذف")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("انصراف")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
