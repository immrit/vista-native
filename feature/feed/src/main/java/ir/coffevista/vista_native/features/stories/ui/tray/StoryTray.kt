package ir.coffevista.vista_native.features.stories.ui.tray

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val brandGradient = Brush.linearGradient(
        colors = listOf(VistaBrandColors.Indigo, VistaBrandColors.Pink),
    )

    val ownStoryUser = storyUsers.firstOrNull { it.id == currentUserId }
    val otherStoryUsers = storyUsers.filterNot { it.id == currentUserId }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Current User Story item
        item(key = "story_tray_my_story") {
            MyStoryItem(
                avatarUrl = currentUserAvatar ?: ownStoryUser?.avatarUrl,
                hasActiveStory = ownStoryUser?.stories?.isNotEmpty() == true,
                hasUnseenStory = ownStoryUser?.hasUnseenStories == true,
                brandGradient = brandGradient,
                onClick = {
                    if (ownStoryUser?.stories?.isNotEmpty() == true) {
                        onOpenStoryPlayer(0)
                    } else {
                        onCreateStory()
                    }
                },
                onAddClick = onCreateStory,
            )
        }

        // 2. Other Users' Story items
        itemsIndexed(
            items = otherStoryUsers,
            key = { _, user -> "story_user_${user.id}" },
        ) { index, user ->
            val playerIndex = if (ownStoryUser != null) index + 1 else index
            StoryUserItem(
                user = user,
                brandGradient = brandGradient,
                onClick = { onOpenStoryPlayer(playerIndex) },
            )
        }
    }
}

@Composable
private fun MyStoryItem(
    avatarUrl: String?,
    hasActiveStory: Boolean,
    hasUnseenStory: Boolean,
    brandGradient: Brush,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Ring
            val ringModifier = if (hasActiveStory) {
                if (hasUnseenStory) {
                    Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(brandGradient)
                        .padding(2.5.dp)
                } else {
                    Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .padding(2.5.dp)
                }
            } else {
                Modifier.size(64.dp)
            }

            Box(
                modifier = ringModifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "استوری شما",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "استوری شما",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            // Plus Badge
            if (!hasActiveStory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(VistaBrandColors.Indigo)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable(onClick = onAddClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "افزودن استوری",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Text(
            text = "استوری شما",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun StoryUserItem(
    user: StoryUser,
    brandGradient: Brush,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .then(
                    if (user.hasUnseenStories) {
                        Modifier
                            .background(brandGradient)
                            .padding(2.5.dp)
                    } else {
                        Modifier
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .padding(2.5.dp)
                    },
                )
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
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

        Text(
            text = user.username,
            fontSize = 11.sp,
            fontWeight = if (user.hasUnseenStories) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
