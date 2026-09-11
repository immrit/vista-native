package ir.coffevista.vista_native.features.profile.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.core.designsystem.component.VistaErrorState
import ir.coffevista.vista_native.features.profile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnProfileScreen(
    viewModel: OwnProfileViewModel,
    onLogout: () -> Unit,
    postsState: ProfilePostsPresentationState = ProfilePostsPresentationState(),
    onPostsRefresh: () -> Unit = {},
    onPostsLoadMore: () -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onLikeClick: (String, Boolean, Long) -> Unit = { _, _, _ -> },
    onSaveClick: (String, Boolean) -> Unit = { _, _ -> },
    onEditProfile: (() -> Unit)? = null,
    onShareProfile: (() -> Unit)? = null,
    onAddPost: (() -> Unit)? = null,
    onAddStory: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    onOpenFollowers: ((userId: String, initialTab: Int) -> Unit)? = null,
    onOpenQrScanner: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showOptionsSheet by rememberSaveable { mutableStateOf(false) }
    var showVistaIdCard by rememberSaveable { mutableStateOf(false) }
    var showAccountDetails by rememberSaveable { mutableStateOf(false) }
    var showQrDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentUsername = (uiState as? OwnProfileUiState.Content)?.profile?.username

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
            Box(modifier = modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    OwnProfileAppBar(
                        username = currentUsername,
                        verified = (uiState as? OwnProfileUiState.Content)?.profile?.isVerified == true,
                        verificationType = (uiState as? OwnProfileUiState.Content)?.profile?.verificationType,
                        role = (uiState as? OwnProfileUiState.Content)?.profile?.accountType,
                        onQrClick = { showQrDialog = true },
                        onSettingsClick = {
                            onSettingsClick?.invoke()
                        },
                        onOptionsClick = {
                            showOptionsSheet = true
                        },
                    )
                    when (val state = uiState) {
                        OwnProfileUiState.Loading -> ProfileLoadingSkeleton()
                        is OwnProfileUiState.Error -> VistaErrorState(
                            title = "خطا در دریافت اطلاعات",
                            message = state.error.messageFa,
                            onRetry = { viewModel.handleAction(OwnProfileAction.Retry) },
                            modifier = Modifier.fillMaxSize(),
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
                                postsState = postsState,
                                privatePosts = false,
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                onPostClick = onPostClick,
                                onLikeClick = onLikeClick,
                                onSaveClick = onSaveClick,
                                onLoadMore = onPostsLoadMore,
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
                                            showVistaIdCard = true
                                        },
                                    )
                                },
                            )
                        }
                    }
                }

                if (showQrDialog) {
                    val profile = (uiState as? OwnProfileUiState.Content)?.profile
                    if (profile != null) {
                        ir.coffevista.vista_native.features.profile.ui.qr.ProfileQrDialog(
                            username = profile.username.orEmpty(),
                            fullName = profile.fullName.orEmpty(),
                            avatarUrl = profile.avatarUrl,
                            onDismiss = { showQrDialog = false },
                        )
                    }
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
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "https://coffevista.ir/u/${currentUsername ?: ""}")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "اشتراک‌گذاری پروفایل"))
                        },
                        onCopyLink = {
                            val link = "https://coffevista.ir/u/${currentUsername ?: ""}"
                            clipboardManager.setText(AnnotatedString(link))
                            Toast.makeText(context, "لینک پروفایل کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        onReport = {
                            Toast.makeText(context, "گزارش ارسال شد", Toast.LENGTH_SHORT).show()
                        },
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
    onQrClick: (() -> Unit)? = null,
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

        // End in RTL (Left side of screen): QR, more options, then settings.
        // This matches Flutter's three affordances in the profile header.
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onQrClick?.invoke() },
                enabled = onQrClick != null,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = "کد QR پروفایل",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.width(4.dp))
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
