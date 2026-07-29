package ir.coffevista.vista_native.features.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.core.designsystem.component.VistaEmptyState
import ir.coffevista.vista_native.core.designsystem.component.VistaErrorState
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
    onMessage: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
        snackbarHostState = snackbarHostState,
        postsState = postsState,
        onPostsRefresh = onPostsRefresh,
        onPostsLoadMore = onPostsLoadMore,
        onPostClick = onPostClick,
        onMessage = onMessage,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OtherUserProfileContent(
    state: OtherUserProfileUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (OtherUserProfileAction) -> Unit,
    modifier: Modifier = Modifier,
    postsState: ProfilePostsPresentationState = ProfilePostsPresentationState(),
    onPostsRefresh: () -> Unit = {},
    onPostsLoadMore: () -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onMessage: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        OtherProfileAppBar(
            username = state.profile?.username,
            verified = state.profile?.isVerified == true,
            onBack = { onAction(OtherUserProfileAction.BackClicked) },
        )
        Box(Modifier.fillMaxSize()) {
            when {
                state.userNotFound -> VistaEmptyState(
                    title = "کاربر یافت نشد",
                    message = "این نمایه در دسترس نیست.",
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(OtherProfileTestTags.NotFound),
                )
                state.profile == null && state.initialError != null -> VistaErrorState(
                    title = "خطا در دریافت نمایه",
                    message = state.initialError.messageFa,
                    onRetry = { onAction(OtherUserProfileAction.Retry) },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(OtherProfileTestTags.Error),
                )
                state.profile == null && state.isInitialLoading -> OtherProfileLoading()
                state.profile != null -> PullToRefreshBox(
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
                        postsState = postsState.copy(
                            isOffline = postsState.isOffline || state.isStale,
                        ),
                        privatePosts = profile.isPrivate &&
                            profile.followState != FollowState.Following,
                        onPostClick = onPostClick,
                        onLoadMore = onPostsLoadMore,
                        modifier = Modifier.testTag(OtherProfileTestTags.Content),
                        actionContent = {
                            OtherProfileActions(
                                profile = profile,
                                pending = state.followMutationPending,
                                onAction = onAction,
                                onMessage = onMessage,
                            )
                        },
                    )
                    if (state.isStale) {
                        Text(
                            text = "نمایش نسخه ذخیره‌شده",
                            modifier = Modifier
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
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun OtherProfileAppBar(
    username: String?,
    verified: Boolean,
    onBack: () -> Unit,
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
                androidx.compose.foundation.layout.Spacer(
                    Modifier.padding(horizontal = 2.dp),
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_profile_verified),
                    contentDescription = "تأیید شده",
                    tint = MaterialTheme.colorScheme.onBackground,
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
            )
        }
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_profile_more),
            contentDescription = "گزینه‌های بیشتر",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(horizontal = 16.dp),
            tint = MaterialTheme.colorScheme.onBackground,
        )
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = .5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
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
    isPremium = isPremium,
    isPrivate = isPrivate,
    postCount = postsCount,
    followerCount = followersCount,
    followingCount = followingCount,
    joinOrder = joinOrder,
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
