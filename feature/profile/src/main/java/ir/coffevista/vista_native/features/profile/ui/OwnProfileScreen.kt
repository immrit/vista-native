package ir.coffevista.vista_native.features.profile.ui

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
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
    onEditProfile: (() -> Unit)? = null,
    onShareProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        OwnProfileAppBar(
            username = (uiState as? OwnProfileUiState.Content)
                ?.profile
                ?.username,
            verified = (uiState as? OwnProfileUiState.Content)
                ?.profile
                ?.isVerified == true,
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
                ProfileParityList(
                    header = ProfileHeaderModel(
                        fullName = profile.fullName,
                        username = profile.username,
                        bio = profile.bio,
                        avatarUrl = profile.avatarUrl,
                        isVerified = profile.isVerified,
                        isPremium = profile.premiumDaysRemaining?.let { it > 0 }
                            ?: profile.subscriptionPlan
                                ?.let { it.isNotBlank() && it != "free" }
                            ?: false,
                        isPrivate = profile.isPrivate,
                        postCount = profile.postCount,
                        followerCount = profile.followerCount,
                        followingCount = profile.followingCount,
                        joinOrder = profile.joinOrder,
                    ),
                    postsState = postsState,
                    privatePosts = false,
                    onPostClick = onPostClick,
                    onLoadMore = onPostsLoadMore,
                    actionContent = {
                        OwnProfileActions(
                            onEditProfile = onEditProfile,
                            onShareProfile = onShareProfile,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun OwnProfileAppBar(username: String?, verified: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
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
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_profile_verified),
                    contentDescription = "تأیید شده",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(horizontal = 12.dp)
                .semantics { disabled() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_profile_settings),
                contentDescription = "تنظیمات",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(20.dp))
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_profile_more),
                contentDescription = "گزینه‌های بیشتر",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            thickness = .5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun OwnProfileActions(
    onEditProfile: (() -> Unit)?,
    onShareProfile: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(7.dp),
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
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
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
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(5.dp),
                            ),
                    )
                    Spacer(Modifier.height(7.dp))
                    Box(
                        Modifier
                            .width(52.dp)
                            .height(12.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
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
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp)),
        )
        Spacer(Modifier.height(9.dp))
        Box(
            Modifier
                .fillMaxWidth(.7f)
                .height(13.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp)),
        )
    }
}

internal object OwnProfileTestTags {
    const val Loading = "own-profile-loading"
    const val PullToRefresh = "own-profile-pull-to-refresh"
}
