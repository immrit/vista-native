package ir.coffevista.vista_native.features.stories.ui.tray

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.feed.ui.VerifiedMark
import ir.coffevista.vista_native.features.stories.data.StoryRepository
import ir.coffevista.vista_native.features.stories.domain.StoryPrivacyType
import ir.coffevista.vista_native.features.stories.domain.StoryUser

@Composable
fun StoryTray(
    storyUsers: List<StoryUser>,
    currentUserId: String,
    currentUserAvatar: String?,
    onOpenStoryPlayer: (userIndex: Int) -> Unit,
    onCreateStory: () -> Unit,
    modifier: Modifier = Modifier,
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
    val primaryGradient = remember {
        Brush.linearGradient(
            colors = listOf(VistaBrandColors.Indigo, VistaBrandColors.Violet),
        )
    }

    val normalizedUsers = remember(storyUsers, currentUserId) {
        StoryRepository.normalizeAndSortStoryUsers(storyUsers, currentUserId)
    }
    val ownStoryUser = remember(normalizedUsers, currentUserId) {
        if (currentUserId.isBlank()) null
        else normalizedUsers.firstOrNull { it.id == currentUserId && it.stories.isNotEmpty() }
    }
    val otherUsers = remember(normalizedUsers, currentUserId) {
        if (currentUserId.isBlank()) normalizedUsers
        else normalizedUsers.filterNot { it.id == currentUserId }
    }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.Top,
    ) {
        item(key = "story_tray_your_story") {
            YourStoryItem(
                ownStoryUser = ownStoryUser,
                currentUserAvatar = currentUserAvatar,
                brandGradient = brandGradient,
                closeFriendsGradient = closeFriendsGradient,
                primaryGradient = primaryGradient,
                onOpenOwnStories = {
                    val target = ownStoryUser
                    if (target != null) {
                        val idx = storyUsers.indexOfFirst { it.id == target.id }
                        if (idx >= 0) onOpenStoryPlayer(idx) else onCreateStory()
                    } else {
                        onCreateStory()
                    }
                },
                onAddStory = onCreateStory,
            )
        }

        items(
            items = otherUsers,
            key = { user -> "story_user_${user.id}" },
        ) { user ->
            StoryUserItem(
                user = user,
                brandGradient = brandGradient,
                closeFriendsGradient = closeFriendsGradient,
                onClick = {
                    storyUsers.indexOfFirst { it.id == user.id }
                        .takeIf { it >= 0 }
                        ?.let(onOpenStoryPlayer)
                },
            )
        }
    }
}

@Composable
private fun YourStoryItem(
    ownStoryUser: StoryUser?,
    currentUserAvatar: String?,
    brandGradient: Brush,
    closeFriendsGradient: Brush,
    primaryGradient: Brush,
    onOpenOwnStories: () -> Unit,
    onAddStory: () -> Unit,
) {
    val hasActiveStories = ownStoryUser != null && ownStoryUser.stories.isNotEmpty()
    val hasCloseFriendsUnseen = ownStoryUser?.stories?.any {
        !it.isViewed && it.privacyType == StoryPrivacyType.CloseFriends
    } == true
    val hasUnseen = ownStoryUser?.hasUnseenStories == true
    val resolvedAvatar = currentUserAvatar?.takeIf { it.isNotBlank() }
        ?: ownStoryUser?.avatarUrl?.takeIf { it.isNotBlank() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(86.dp)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .clickable(onClick = if (hasActiveStories) onOpenOwnStories else onAddStory),
    ) {
        Box(
            modifier = Modifier.size(74.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        when {
                            hasCloseFriendsUnseen -> Modifier
                                .clip(CircleShape)
                                .background(closeFriendsGradient)
                                .padding(2.2.dp)
                            hasActiveStories && hasUnseen -> Modifier
                                .clip(CircleShape)
                                .background(brandGradient)
                                .padding(2.2.dp)
                            hasActiveStories -> Modifier
                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .padding(2.dp)
                            else -> Modifier
                                .border(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                                    CircleShape,
                                )
                                .padding(2.dp)
                        },
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(if (hasActiveStories) 2.5.dp else 1.5.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (resolvedAvatar != null) {
                    AsyncImage(
                        model = resolvedAvatar,
                        contentDescription = "استوری شما",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "استوری شما",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            // Corner '+' badge — always allows adding another story even when own stories exist
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(primaryGradient)
                    .clickable(onClick = onAddStory),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "افزودن استوری جدید",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        Text(
            text = if (hasActiveStories) "استوری شما" else "استوری جدید",
            fontSize = 11.5.sp,
            fontWeight = if (hasActiveStories) FontWeight.SemiBold else FontWeight.Normal,
            color = if (hasActiveStories) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(74.dp)
                .padding(top = 4.dp),
        )
    }
}

@Composable
private fun StoryUserItem(
    user: StoryUser,
    brandGradient: Brush,
    closeFriendsGradient: Brush,
    onClick: () -> Unit,
) {
    val hasCloseFriendsUnseen = user.stories.any {
        !it.isViewed && it.privacyType == StoryPrivacyType.CloseFriends
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(86.dp)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .then(
                    when {
                        hasCloseFriendsUnseen -> Modifier
                            .clip(CircleShape)
                            .background(closeFriendsGradient)
                            .padding(2.2.dp)
                        user.hasUnseenStories -> Modifier
                            .clip(CircleShape)
                            .background(brandGradient)
                            .padding(2.2.dp)
                        else -> Modifier
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .padding(2.dp)
                    },
                )
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(2.5.dp),
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
                    contentDescription = user.username,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(74.dp)
                .padding(top = 4.dp),
        ) {
            Text(
                text = user.username,
                fontSize = 12.sp,
                fontWeight = if (user.hasUnseenStories) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (user.isVerified || user.isPremium) {
                Spacer(modifier = Modifier.width(3.dp))
                VerifiedMark(
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
