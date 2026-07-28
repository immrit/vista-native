package ir.coffevista.vista_native.features.profile.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.component.VistaAvatar
import ir.coffevista.vista_native.core.designsystem.component.VistaBadge
import ir.coffevista.vista_native.core.designsystem.component.VistaButton
import ir.coffevista.vista_native.core.designsystem.component.VistaButtonVariant
import ir.coffevista.vista_native.core.designsystem.component.VistaEmptyState
import ir.coffevista.vista_native.core.designsystem.component.VistaErrorState
import ir.coffevista.vista_native.core.designsystem.component.VistaLoadingState
import ir.coffevista.vista_native.core.designsystem.component.VistaScaffold
import ir.coffevista.vista_native.core.designsystem.component.VistaTopAppBar
import ir.coffevista.vista_native.core.designsystem.tokens.VistaLayout
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSpacing
import ir.coffevista.vista_native.features.profile.R
import ir.coffevista.vista_native.features.profile.data.FollowState
import ir.coffevista.vista_native.features.profile.data.PublicProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    viewModel: OtherUserProfileViewModel,
    onBack: () -> Unit,
    onSelfProfile: () -> Unit,
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
) {
    VistaScaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHostState = snackbarHostState,
        topBar = {
            VistaTopAppBar(
                title = "نمایه کاربر",
                navigationIcon = {
                    IconButton(
                        onClick = { onAction(OtherUserProfileAction.BackClicked) },
                        modifier = Modifier.testTag(OtherProfileTestTags.Back),
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_profile_back),
                            contentDescription = "بازگشت",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            state.userNotFound -> VistaEmptyState(
                title = "کاربر یافت نشد",
                message = "این نمایه در دسترس نیست.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(OtherProfileTestTags.NotFound),
            )
            state.profile == null && state.initialError != null -> VistaErrorState(
                title = "خطا در دریافت نمایه",
                message = state.initialError.messageFa,
                onRetry = { onAction(OtherUserProfileAction.Retry) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(OtherProfileTestTags.Error),
            )
            state.profile == null && state.isInitialLoading -> VistaLoadingState(
                label = "در حال دریافت نمایه...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(OtherProfileTestTags.Loading),
            )
            state.profile != null -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onAction(OtherUserProfileAction.Refresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(OtherProfileTestTags.PullToRefresh),
            ) {
                ProfileBody(
                    profile = state.profile,
                    isStale = state.isStale,
                    pending = state.followMutationPending,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun ProfileBody(
    profile: PublicProfile,
    isStale: Boolean,
    pending: FollowMutationPending?,
    onAction: (OtherUserProfileAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VistaLayout.ScreenHorizontal)
            .testTag(OtherProfileTestTags.Content),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isStale) {
            Text(
                text = "نمایش نسخه ذخیره‌شده",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = VistaSpacing.Small)
                    .testTag(OtherProfileTestTags.Stale),
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(modifier = Modifier.height(VistaSpacing.Large))
        Box(contentAlignment = Alignment.Center) {
            VistaAvatar(
                displayName = profile.fullName,
                modifier = Modifier.size(88.dp),
            )
            profile.avatarUrl?.takeIf(String::isNotBlank)?.let { avatarUrl ->
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "تصویر نمایه ${profile.fullName}",
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.height(VistaSpacing.Medium))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VistaSpacing.Small),
        ) {
            Text(
                text = profile.fullName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (profile.isVerified) VistaBadge("تأییدشده")
            if (profile.isPremium) VistaBadge("ویژه")
            if (profile.isPrivate) VistaBadge("خصوصی")
        }
        profile.username?.takeIf(String::isNotBlank)?.let {
            Text(
                text = "@$it",
                modifier = Modifier.padding(top = VistaSpacing.Micro),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        profile.bio?.takeIf(String::isNotBlank)?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = VistaSpacing.Large),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(VistaSpacing.XLarge))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            PublicProfileStat("پست‌ها", profile.postsCount)
            PublicProfileStat("دنبال‌کننده‌ها", profile.followersCount)
            PublicProfileStat("دنبال‌شوندگان", profile.followingCount)
        }
        Spacer(modifier = Modifier.height(VistaSpacing.XLarge))
        FollowButton(
            profile = profile,
            pending = pending,
            onAction = onAction,
        )
        Spacer(modifier = Modifier.height(VistaSpacing.XLarge))
        VistaEmptyState(
            title = "پست‌های عمومی",
            message = "نمایش شبکه پست‌ها در این مرحله پیاده‌سازی نمی‌شود.",
        )
    }
}

@Composable
private fun FollowButton(
    profile: PublicProfile,
    pending: FollowMutationPending?,
    onAction: (OtherUserProfileAction) -> Unit,
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
    VistaButton(
        onClick = { action?.let(onAction) },
        enabled = pending == null && action != null,
        variant = if (profile.followState == FollowState.Following) {
            VistaButtonVariant.Outline
        } else {
            VistaButtonVariant.Primary
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(OtherProfileTestTags.FollowButton),
    ) {
        Text(label)
    }
}

@Composable
private fun PublicProfileStat(label: String, value: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontWeight = FontWeight.Bold)
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

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
