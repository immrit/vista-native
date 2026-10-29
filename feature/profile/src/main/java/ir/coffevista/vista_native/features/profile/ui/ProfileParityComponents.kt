package ir.coffevista.vista_native.features.profile.ui

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import ir.coffevista.vista_native.features.profile.data.ProfileHighlightDto
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiText
import ir.coffevista.vista_native.core.designsystem.component.VistaFloatingActionButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.features.profile.R
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

internal data class ProfileHeaderModel(
    val fullName: String,
    val username: String?,
    val bio: String?,
    val avatarUrl: String?,
    val isVerified: Boolean,
    val verificationType: String? = null,
    val role: String? = null,
    val isPremium: Boolean,
    val isPrivate: Boolean,
    val postCount: Long,
    val followerCount: Long,
    val followingCount: Long,
    val joinOrder: Long,
    val createdAt: String? = null,
    val isOwnProfile: Boolean = false,
)

@Composable
internal fun ProfileParityList(
    header: ProfileHeaderModel,
    postsState: ProfilePostsPresentationState,
    actionContent: @Composable () -> Unit,
    privatePosts: Boolean,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onPostClick: (String) -> Unit,
    onReelClick: (String) -> Unit = onPostClick,
    onLikeClick: (String, Boolean, Long) -> Unit,
    onSaveClick: (String, Boolean) -> Unit,
    onCommentClick: (String) -> Unit = onPostClick,
    onShareClick: (ProfilePostUiModel) -> Unit = {},
    onEditPost: (ProfilePostUiModel) -> Unit = {},
    onDeletePost: (String) -> Unit = {},
    onReportPost: (ProfilePostUiModel) -> Unit = {},
    onVisibilityChange: (ProfilePostUiModel, Boolean?, Boolean?) -> Unit = { _, _, _ -> },
    onPremiumPrompt: (String) -> Unit = {},
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit = {},
    onAvatarAddClick: (() -> Unit)? = null,
    onMemberBadgeClick: (() -> Unit)? = null,
    onFollowersClick: (() -> Unit)? = null,
    onFollowingClick: (() -> Unit)? = null,
    highlightsContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(
        listState,
        postsState.posts.size,
        postsState.hasMore,
        postsState.isAppending,
    ) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            postsState.posts.isNotEmpty() &&
                postsState.hasMore &&
                !postsState.isAppending &&
                last >= info.totalItemsCount - 4
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag(ProfileParityTestTags.List),
        contentPadding = PaddingValues(bottom = 110.dp),
    ) {
        item(key = "profile-header") {
            ProfileHeader(
                header = header,
                onAvatarAddClick = onAvatarAddClick,
                onMemberBadgeClick = onMemberBadgeClick,
                onFollowersClick = onFollowersClick,
                onFollowingClick = onFollowingClick,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                actionContent()
            }
            if (highlightsContent != null) {
                highlightsContent()
            }
            ProfileTabStrip(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        }

        if (privatePosts) {
            item(key = "private-posts") {
                ProfileLockedState()
            }
        } else if (postsState.isInitialLoading) {
            profilePostsSkeleton()
        } else if (postsState.posts.isEmpty() && postsState.error != null) {
            item(key = "posts-error") {
                ProfilePostsError(
                    message = postsState.error,
                    onRetry = onRefresh,
                )
            }
        } else {
            when (selectedTab) {
                0 -> {
                    // Posts Tab
                    if (postsState.posts.isEmpty()) {
                        item(key = "posts-empty") {
                            ProfileEmptyView(
                                iconRes = DesignSystemR.drawable.vista_post_send,
                                title = "هنوز پستی نیست",
                                subtitle = "اولین پست خود را به اشتراک بگذارید",
                                tag = ProfileParityTestTags.Empty,
                            )
                        }
                    } else {
                        items(
                            count = postsState.posts.size,
                            key = { postsState.posts[it].id },
                        ) { index ->
                            val post = postsState.posts[index]
                            val isOwn = post.userId == postsState.viewerUserId || header.isOwnProfile
                            val isBlue = postsState.viewerIsBlueTick
                            val hasPrem = postsState.viewerHasPremiumEdit
                            ProfilePostCard(
                                post = post,
                                isOwnPost = isOwn,
                                isBlueTick = isBlue,
                                hasPremiumEdit = hasPrem,
                                onClick = { onPostClick(post.id) },
                                onLikeClick = { onLikeClick(post.id, post.isLiked, post.likeCount) },
                                onSaveClick = { onSaveClick(post.id, post.isSaved) },
                                onCommentClick = { onCommentClick(post.id) },
                                onShareClick = { onShareClick(post) },
                                onEditPost = { onEditPost(post) },
                                onDeletePost = { onDeletePost(post.id) },
                                onReportPost = { onReportPost(post) },
                                onVisibilityChange = { hideLike, hideComment -> onVisibilityChange(post, hideLike, hideComment) },
                                onPremiumPrompt = onPremiumPrompt,
                            )
                            HorizontalDivider(
                                thickness = .5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
                1 -> {
                    // Reels Tab (3-Column Grid)
                    val reels = postsState.posts.filter { it.videoUrl != null }
                    if (reels.isEmpty()) {
                        item(key = "reels-empty") {
                            ProfileEmptyView(
                                iconVector = ImageVector.vectorResource(R.drawable.ic_profile_play),
                                title = "هنوز کلیپی نیست",
                                subtitle = "اولین کلیپ و ویدیوی خود را به اشتراک بگذارید",
                                tag = "profile-reels-empty",
                            )
                        }
                    } else {
                        item(key = "reels-grid") {
                            ProfileReelsInlineGrid(
                                reels = reels,
                                onReelClick = onReelClick,
                            )
                        }
                    }
                }
                2 -> {
                    // Music Tab
                    val musicPosts = postsState.posts.filter { !it.musicUrl.isNullOrBlank() || !it.musicTitle.isNullOrBlank() }
                    if (musicPosts.isEmpty()) {
                        item(key = "music-empty") {
                            ProfileEmptyView(
                                iconVector = ImageVector.vectorResource(R.drawable.ic_profile_music),
                                title = "هنوز موزیکی نیست",
                                subtitle = "برای پست‌ها و ویدیوهایتان موزیک اضافه کنید",
                                tag = "profile-music-empty",
                            )
                        }
                    } else {
                        items(
                            count = musicPosts.size,
                            key = { "music-${musicPosts[it].id}" },
                        ) { index ->
                            val post = musicPosts[index]
                            ProfileMusicListItem(
                                title = post.musicTitle ?: "موزیک پست",
                                subtitle = post.authorUsername ?: post.authorFullName,
                                onClick = { onPostClick(post.id) },
                            )
                            HorizontalDivider(
                                thickness = .5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }

        if (postsState.isAppending) {
            item(key = "posts-appending") {
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
        postsState.appendError?.let { message ->
            item(key = "posts-append-error") {
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
internal fun ProfileHeader(
    header: ProfileHeaderModel,
    onAvatarAddClick: (() -> Unit)? = null,
    onMemberBadgeClick: (() -> Unit)? = null,
    onFollowersClick: (() -> Unit)? = null,
    onFollowingClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Avatar + Stats Row (In RTL: Avatar on Right, Stats on Left)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(
                header = header,
                onAddClick = onAvatarAddClick,
            )
            Spacer(Modifier.width(28.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStat(header.postCount, "پست")
                ProfileStat(header.followerCount, "دنبال‌کننده", onClick = onFollowersClick)
                ProfileStat(header.followingCount, "دنبال‌شونده", onClick = onFollowingClick)
            }
        }

        // Full Name + Badges Row
        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VistaEmojiText(
                text = header.fullName.ifBlank { header.username ?: "" },
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
            if (header.isVerified) {
                VerificationBadgeIcon(
                    isVerified = true,
                    verificationType = header.verificationType,
                    role = header.role,
                    size = 18.dp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            if (header.isPremium) {
                Text(
                    text = "V",
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .background(Color(0xFFFFC107), CircleShape)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (header.isPrivate) {
                Text(" 🔒", fontSize = 13.sp)
            }
        }

        // Bio (RTL text with hashtags)
        header.bio?.takeIf(String::isNotBlank)?.let { bioText ->
            VistaEmojiText(
                text = bioText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }

        // Member Order Badge
        if (header.joinOrder > 0) {
            MemberOrderBadge(
                joinOrder = header.joinOrder,
                onClick = onMemberBadgeClick,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
internal fun MemberOrderBadge(
    joinOrder: Long,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // 1-100: Founding (عضو بنیان‌گذار ویستا) -> Gold gradient + WorkspacePremium icon
    // 101-1000: Early (از اولین هزار نفر) -> Blue gradient + RocketLaunch icon
    // 1001-10000: Pioneer (عضو پیشگام) -> Purple gradient + ElectricBolt icon
    // >10000: Member (عضو شماره) -> Slate gradient + AutoAwesome icon
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val (gradientColors, borderColor, textColor, badgeResId, label) = when {
        joinOrder <= 100 -> {
            Tuple5(
                if (isDark) listOf(Color(0xFF3D2800), Color(0xFF5A3A00))
                else listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)),
                if (isDark) Color(0xFFB8860B) else Color(0xFFF59E0B),
                if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                R.drawable.ic_badge_founder,
                "عضو بنیان‌گذار ویستا \u200E#$joinOrder",
            )
        }
        joinOrder <= 1000 -> {
            Tuple5(
                if (isDark) listOf(Color(0xFF001A3D), Color(0xFF002D6B))
                else listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)),
                if (isDark) Color(0xFF1565C0) else Color(0xFF3B82F6),
                if (isDark) Color(0xFF82BBFF) else Color(0xFF1D4ED8),
                R.drawable.ic_badge_rocket,
                "از اولین هزار نفر \u200E#$joinOrder",
            )
        }
        joinOrder <= 10000 -> {
            Tuple5(
                if (isDark) listOf(Color(0xFF1A0030), Color(0xFF2D0050))
                else listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF)),
                if (isDark) Color(0xFF7B2FBE) else Color(0xFF8B5CF6),
                if (isDark) Color(0xFFD08BFF) else Color(0xFF6D28D9),
                R.drawable.ic_badge_bolt,
                "عضو پیشگام \u200E#$joinOrder",
            )
        }
        else -> {
            Tuple5(
                if (isDark) listOf(Color(0xFF1E1E1E), Color(0xFF252525))
                else listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)),
                if (isDark) Color(0xFF404040) else Color(0xFF94A3B8),
                if (isDark) Color(0xFF999999) else Color(0xFF475569),
                R.drawable.ic_badge_star,
                "عضو شماره \u200E#$joinOrder",
            )
        }
    }

    val clickableMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
                RoundedCornerShape(20.dp),
            )
            .then(clickableMod),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(gradientColors))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(badgeResId),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = textColor,
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                )
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = textColor.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E,
)

@Composable
internal fun ProfileAvatar(
    header: ProfileHeaderModel,
    onAddClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.size(84.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        val imageModifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .border(
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                CircleShape,
            )

        if (header.avatarUrl.isNullOrBlank()) {
            Image(
                painter = painterResource(DesignSystemR.drawable.vista_default_avatar),
                contentDescription = "تصویر پیش‌فرض ${header.fullName}",
                modifier = imageModifier,
                contentScale = ContentScale.Crop,
            )
        } else {
            AsyncImage(
                model = header.avatarUrl,
                contentDescription = "تصویر نمایه ${header.fullName}",
                modifier = imageModifier,
                placeholder = painterResource(DesignSystemR.drawable.vista_default_avatar),
                error = painterResource(DesignSystemR.drawable.vista_default_avatar),
                contentScale = ContentScale.Crop,
            )
        }

        // Circular Add button for own profile
        if (header.isOwnProfile) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2196F3))
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.background), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onAddClick?.invoke() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(value: Long, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Text(
            text = value.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = label,
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun ProfileTabStrip(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tab 0: Posts (Grid) - Right in RTL
            ProfileTabItem(
                icon = ImageVector.vectorResource(R.drawable.ic_profile_grid),
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f),
            )
            // Tab 1: Reels (Play) - Center
            ProfileTabItem(
                icon = ImageVector.vectorResource(R.drawable.ic_profile_play),
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f),
            )
            // Tab 2: Music - Left in RTL
            ProfileTabItem(
                icon = ImageVector.vectorResource(R.drawable.ic_profile_music),
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(
            thickness = .5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun ProfileTabItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(MaterialTheme.colorScheme.onBackground)
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
internal fun ProfilePostCard(
    post: ProfilePostUiModel,
    isOwnPost: Boolean = false,
    isBlueTick: Boolean = false,
    hasPremiumEdit: Boolean = false,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCommentClick: () -> Unit = onClick,
    onShareClick: () -> Unit = {},
    onEditPost: () -> Unit = {},
    onDeletePost: () -> Unit = {},
    onReportPost: () -> Unit = {},
    onVisibilityChange: (Boolean?, Boolean?) -> Unit = { _, _ -> },
    onPremiumPrompt: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val actionTint = if (isDark) {
        Color.White.copy(alpha = 0.70f)
    } else {
        Color.Black.copy(alpha = 0.54f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(ProfileParityTestTags.post(post.id)),
    ) {
        // Author Row
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (post.authorAvatarUrl.isNullOrBlank()) {
                Image(
                    painterResource(DesignSystemR.drawable.vista_default_avatar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                AsyncImage(
                    model = post.authorAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.authorFullName.ifBlank { post.authorUsername ?: "" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (post.authorIsVerified) {
                        VerificationBadgeIcon(
                            isVerified = true,
                            verificationType = post.authorVerificationType,
                            role = post.authorRole,
                            size = 16.dp,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    Text(
                        text = " • ${profileRelativeTime(post.createdAt)}",
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // Post Text
        post.content?.takeIf(String::isNotBlank)?.let { contentText ->
            Text(
                text = contentText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Post Media
        val images = if (post.imageUrls.isNotEmpty()) post.imageUrls else listOfNotNull(post.imageUrl)
        if (images.isNotEmpty() || !post.videoUrl.isNullOrBlank()) {
            val ratio = post.aspectRatio?.coerceIn(0.75f, 1.91f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .then(
                        if (ratio != null) Modifier.aspectRatio(ratio)
                        else Modifier.height(260.dp)
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onClick() },
                contentAlignment = Alignment.Center,
            ) {
                if (images.size > 1) {
                    val pagerState = rememberPagerState(pageCount = { images.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        AsyncImage(
                            model = images[page],
                            contentDescription = "تصویر ${page + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                    ) {
                        Text(
                            text = "${(pagerState.currentPage + 1).toString().toPersianDigits()} / ${images.size.toString().toPersianDigits()}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                } else if (images.isNotEmpty()) {
                    AsyncImage(
                        model = images.first(),
                        contentDescription = if (post.videoUrl == null) "تصویر پست" else "ویدیو",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                if (!post.videoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "پخش ویدیو",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }

        // Action Buttons Row (Placed exactly like Feed: Like, Comment, Bookmark, Share, Spacer, 3-dots)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileHeart(
                filled = post.isLiked,
                modifier = Modifier
                    .size(19.dp)
                    .clickable { onLikeClick() },
            )
            if (!post.hideLikeCount) {
                Text(
                    text = post.likeCount.toString().toPersianDigits(),
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 13.sp,
                    color = actionTint,
                )
            }
            Spacer(Modifier.width(14.dp))
            Image(
                painter = painterResource(DesignSystemR.drawable.vista_post_comment),
                contentDescription = "دیدگاه",
                modifier = Modifier
                    .size(19.dp)
                    .clickable { onCommentClick() },
                colorFilter = ColorFilter.tint(actionTint),
            )
            if (!post.hideCommentCount) {
                Text(
                    text = post.commentCount.toString().toPersianDigits(),
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 13.sp,
                    color = actionTint,
                )
            }
            Spacer(Modifier.width(14.dp))
            BookmarkIcon(
                filled = post.isSaved,
                inactiveColor = actionTint,
                modifier = Modifier
                    .size(19.dp)
                    .clickable { onSaveClick() },
            )
            Spacer(Modifier.width(14.dp))
            Image(
                painter = painterResource(DesignSystemR.drawable.vista_post_send),
                contentDescription = "ارسال",
                modifier = Modifier
                    .size(19.dp)
                    .clickable {
                        if (onShareClick != {}) {
                            onShareClick()
                        } else {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "https://coffevista.ir/p/${post.id}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری پست"))
                        }
                    },
                colorFilter = ColorFilter.tint(actionTint),
            )
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "گزینه‌های بیشتر",
                        tint = actionTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    if (!isOwnPost) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("گزارش پست")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onReportPost()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("کپی متن")
                            }
                        },
                        onClick = {
                            showMenu = false
                            clipboardManager.setText(AnnotatedString(post.content.orEmpty()))
                            Toast.makeText(context, "متن پست کپی شد", Toast.LENGTH_SHORT).show()
                        },
                    )
                    if (isOwnPost || isBlueTick) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("حذف پست", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            onClick = {
                                showMenu = false
                                confirmDelete = true
                            },
                        )
                    }
                    if (isBlueTick || (isOwnPost && hasPremiumEdit)) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isBlueTick && !isOwnPost) Icons.Default.AdminPanelSettings else Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isBlueTick && !isOwnPost) "ویرایش ناظر" else "ویرایش پست")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEditPost()
                            },
                        )
                    } else if (isOwnPost) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("ویرایش پست", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB800),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onPremiumPrompt("ویرایش پست")
                            },
                        )
                    }
                    if (isOwnPost) {
                        if (hasPremiumEdit) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (post.hideLikeCount) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (post.hideLikeCount) "نمایش تعداد لایک" else "مخفی کردن تعداد لایک")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onVisibilityChange(!post.hideLikeCount, null)
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (post.hideCommentCount) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (post.hideCommentCount) "نمایش تعداد دیدگاه" else "مخفی کردن تعداد دیدگاه")
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onVisibilityChange(null, !post.hideCommentCount)
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("کنترل آمار لایک/کامنت", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.weight(1f))
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB800),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onPremiumPrompt("مخفی‌سازی آمار لایک و کامنت")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف پست") },
            text = { Text("آیا مطمئن هستید که می‌خواهید این پست را حذف کنید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeletePost()
                    },
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
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
internal fun ProfileReelsInlineGrid(
    reels: List<ProfilePostUiModel>,
    onReelClick: (String) -> Unit,
) {
    val chunked = reels.chunked(3)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                rowItems.forEach { reel ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(9f / 16f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onReelClick(reel.id) },
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        if (!reel.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = reel.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        // Gradient shadow at bottom for view count readability
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                    ),
                                ),
                        )
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_profile_play),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White,
                            )
                            Text(
                                text = if (reel.viewsCount > 0) reel.viewsCount.toString() else "۰",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                // Fill empty slots in row if less than 3 items
                if (rowItems.size < 3) {
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProfileMusicListItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Music icon container (46x46dp rounded)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_profile_music),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_profile_play),
            contentDescription = "پخش",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileOptionsBottomSheet(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onReport: (() -> Unit)? = null,
    onBlock: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            ProfileBottomSheetItem(
                icon = ImageVector.vectorResource(R.drawable.ic_profile_share),
                title = "اشتراک‌گذاری پروفایل",
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = {
                    onDismiss()
                    onShare()
                },
            )
            ProfileBottomSheetItem(
                icon = ImageVector.vectorResource(R.drawable.ic_profile_link),
                title = "کپی لینک پروفایل",
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = {
                    onDismiss()
                    onCopyLink()
                },
            )
            if (onReport != null) {
                ProfileBottomSheetItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_profile_report),
                    title = "گزارش کاربر",
                    tint = Color(0xFFE53935),
                    onClick = {
                        onDismiss()
                        onReport()
                    },
                )
            }
            if (onBlock != null) {
                ProfileBottomSheetItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_profile_report),
                    title = "مسدود کردن کاربر",
                    tint = Color(0xFFE53935),
                    onClick = {
                        onDismiss()
                        onBlock()
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileBottomSheetItem(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
        )
    }
}

@Composable
internal fun ProfileFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VistaFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        contentDescription = "ایجاد پست جدید",
    ) {
        Text(
            text = "+",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

typealias ResolvedVerificationBadgeType = ir.coffevista.vista_native.core.designsystem.component.ResolvedVerificationBadgeType

fun parseVerificationBadgeType(raw: String?): ResolvedVerificationBadgeType =
    ir.coffevista.vista_native.core.designsystem.component.parseVerificationBadgeType(raw)

fun resolveVerificationBadgeType(
    isVerified: Boolean,
    verificationType: String? = null,
    role: String? = null,
): ResolvedVerificationBadgeType =
    ir.coffevista.vista_native.core.designsystem.component.resolveVerificationBadgeType(isVerified, verificationType, role)

@Composable
internal fun VerificationBadgeIcon(
    isVerified: Boolean,
    verificationType: String? = null,
    role: String? = null,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 18.dp,
) {
    ir.coffevista.vista_native.core.designsystem.component.VistaVerificationBadge(
        isVerified = isVerified,
        verificationType = verificationType,
        role = role,
        modifier = modifier,
        size = size,
    )
}

@Composable
private fun ProfileHeart(filled: Boolean, modifier: Modifier = Modifier) {
    val color = if (filled) Color(0xFFE53935) else MaterialTheme.colorScheme.onBackground
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * .5f, size.height * .9f)
            cubicTo(
                size.width * .08f, size.height * .62f,
                size.width * .02f, size.height * .2f,
                size.width * .28f, size.height * .12f,
            )
            cubicTo(
                size.width * .42f, size.height * .08f,
                size.width * .5f, size.height * .22f,
                size.width * .5f, size.height * .22f,
            )
            cubicTo(
                size.width * .5f, size.height * .22f,
                size.width * .58f, size.height * .08f,
                size.width * .72f, size.height * .12f,
            )
            cubicTo(
                size.width * .98f, size.height * .2f,
                size.width * .92f, size.height * .62f,
                size.width * .5f, size.height * .9f,
            )
            close()
        }
        if (filled) drawPath(path, color) else {
            drawPath(path, color, style = Stroke(width = size.minDimension * .09f))
        }
    }
}

private val TehranZone: ZoneId = ZoneId.of("Asia/Tehran")
private const val PersianDigits = "۰۱۲۳۴۵۶۷۸۹"

private fun Long.toPersianDigits(): String = toString().toPersianDigits()
private fun String.toPersianDigits(): String = buildString(length) {
    this@toPersianDigits.forEach { character ->
        append(if (character in '0'..'9') PersianDigits[character - '0'] else character)
    }
}

private fun profileRelativeTime(raw: String): String = ir.coffevista.vista_native.core.common.persianRelativeTime(raw)

internal object ProfileParityTestTags {
    const val List = "profile-parity-list"
    const val Empty = "profile-posts-empty"
    const val Private = "profile-posts-private"
    const val Error = "profile-posts-error"
    fun post(id: String) = "profile-post-$id"
}

@Composable
private fun BookmarkIcon(
    filled: Boolean,
    modifier: Modifier = Modifier,
    inactiveColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
) {
    val color = if (filled) MaterialTheme.colorScheme.onBackground else inactiveColor
    Canvas(
        modifier.semantics {
            this.contentDescription = if (filled) "ذخیره شده" else "ذخیره نشده"
            this.role = Role.Image
        },
    ) {
        val path = Path().apply {
            moveTo(size.width * .22f, size.height * .08f)
            lineTo(size.width * .78f, size.height * .08f)
            lineTo(size.width * .78f, size.height * .92f)
            lineTo(size.width * .5f, size.height * .7f)
            lineTo(size.width * .22f, size.height * .92f)
            close()
        }
        if (filled) drawPath(path, color) else {
            drawPath(path, color, style = Stroke(width = size.minDimension * .09f))
        }
    }
}

private fun LazyListScope.profilePostsSkeleton() {
    items(3) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(5.dp),
                        ),
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(14.dp),
                    ),
            )
        }
    }
}

@Composable
private fun ProfileLockedState() = ProfileEmptyView(
    iconVector = null,
    iconText = "🔒",
    title = "این حساب خصوصی است",
    subtitle = "برای دیدن پست‌ها باید این حساب را دنبال کنید.",
    tag = ProfileParityTestTags.Private,
)

@Composable
private fun ProfilePostsError(
    message: String,
    onRetry: (() -> Unit)? = null,
) = ProfileEmptyView(
    iconVector = null,
    iconText = "!",
    title = "دریافت پست‌ها ناموفق بود",
    subtitle = message,
    tag = ProfileParityTestTags.Error,
    action = if (onRetry != null) {
        {
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = "تلاش مجدد",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    } else null,
)

@Composable
private fun ProfileEmptyView(
    title: String,
    subtitle: String,
    tag: String,
    iconVector: ImageVector? = null,
    iconRes: Int? = null,
    iconText: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 60.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    iconVector != null -> Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    iconRes != null -> Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                    iconText != null -> Text(iconText, fontSize = 36.sp)
                }
            }
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = 16.dp),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            action()
        }
    }
}

@Composable
fun ProfileHighlightsRow(
    highlights: List<ProfileHighlightDto>,
    isOwnProfile: Boolean,
    onAddClick: () -> Unit = {},
    onHighlightClick: (ProfileHighlightDto) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (highlights.isEmpty() && !isOwnProfile) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isOwnProfile) {
            item(key = "new_highlight_btn") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onAddClick() },
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "هایلایت جدید",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "جدید",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        items(
            items = highlights,
            key = { it.id },
        ) { highlight ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onHighlightClick(highlight) },
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!highlight.coverUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = highlight.coverUrl,
                            contentDescription = highlight.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = highlight.title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.widthIn(max = 68.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

