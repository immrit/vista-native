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
import java.time.Instant

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
        colors = listOf(VistaBrandColors.Indigo, VistaBrandColors.Violet, VistaBrandColors.Pink),
    )
    val primaryGradient = Brush.linearGradient(
        colors = listOf(VistaBrandColors.Indigo, VistaBrandColors.Violet),
    )

    val sortedUsers = storyUsers
        .filter { it.stories.isNotEmpty() }
        .sortedWith(
            compareByDescending<StoryUser> { it.hasUnseenStories }
                .thenByDescending { user ->
                    runCatching {
                        user.lastStoryAt?.let(Instant::parse)
                            ?: user.stories.maxOfOrNull { Instant.parse(it.createdAt) }
                    }.getOrNull()
                },
        )

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.Top,
    ) {
        item(key = "story_tray_add_story") {
            AddStoryItem(
                primaryGradient = primaryGradient,
                onClick = onCreateStory,
            )
        }

        itemsIndexed(
            items = sortedUsers,
            key = { _, user -> "story_user_${user.id}" },
        ) { _, user ->
            StoryUserItem(
                user = user,
                brandGradient = brandGradient,
                // The player consumes the repository's original list while the
                // tray is intentionally sorted for unseen/recency priority.
                // Passing this LazyRow index could open a different user.
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
private fun AddStoryItem(
    primaryGradient: Brush,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(86.dp)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.size(74.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.26f), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(primaryGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "استوری جدید",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Text(
            text = "استوری جدید",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f),
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
    onClick: () -> Unit,
) {
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
                    if (user.hasUnseenStories) {
                        Modifier
                            .clip(CircleShape)
                            .background(brandGradient)
                            .padding(2.dp)
                    } else {
                        Modifier
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

        androidx.compose.foundation.layout.Row(
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
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(3.dp))
                ir.coffevista.vista_native.features.feed.ui.VerifiedMark(
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
