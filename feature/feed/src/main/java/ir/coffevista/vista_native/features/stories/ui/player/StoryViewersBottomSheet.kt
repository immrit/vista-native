package ir.coffevista.vista_native.features.stories.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.common.persianRelativeTime
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.feed.ui.VerifiedMark
import ir.coffevista.vista_native.features.stories.data.StoryRepository
import ir.coffevista.vista_native.features.stories.data.StorySecurityGuard
import ir.coffevista.vista_native.features.stories.domain.StoryView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewersBottomSheet(
    storyId: String,
    repository: StoryRepository,
    onDismiss: () -> Unit,
    onUserClick: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var viewers by remember { mutableStateOf<List<StoryView>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(storyId) {
        isLoading = true
        val result = repository.getStoryViews(storyId)
        viewers = result.getOrDefault(emptyList())
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF5151722),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .heightIn(min = 240.dp, max = 500.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VistaBrandColors.Indigo.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = VistaBrandColors.Indigo,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "بازدیدکنندگان استوری",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                text = if (isLoading) "در حال دریافت…" else "${viewers.size.toPersianDigits()} بازدید یکتا",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.White.copy(alpha = 0.72f),
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.6.dp,
                    color = Color.White.copy(alpha = 0.1f),
                )

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = VistaBrandColors.Indigo,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }

                    viewers.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "هنوز بازدیدی برای این استوری ثبت نشده است",
                                color = Color.White.copy(alpha = 0.58f),
                                fontSize = 13.5.sp,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(viewers, key = { it.userId.ifBlank { it.username } }) { viewer ->
                                val reactionEmoji = remember(viewer.reaction) {
                                    StorySecurityGuard.mapReactionEmoji(viewer.reaction)
                                }
                                val timeAgo = remember(viewer.viewedAt) {
                                    if (viewer.viewedAt.isBlank()) ""
                                    else persianRelativeTime(viewer.viewedAt)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .border(
                                            0.8.dp,
                                            Color.White.copy(alpha = 0.07f),
                                            RoundedCornerShape(14.dp),
                                        )
                                        .clickable {
                                            if (viewer.userId.isNotBlank()) {
                                                onUserClick(viewer.userId)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Box(modifier = Modifier.size(44.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                if (!viewer.avatarUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = viewer.avatarUrl,
                                                        contentDescription = viewer.username,
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop,
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = viewer.username,
                                                        tint = Color.White.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                }
                                            }
                                            if (reactionEmoji != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF151722))
                                                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(text = reactionEmoji, fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = viewer.fullName.ifBlank { viewer.username },
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                if (viewer.isVerified) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    VerifiedMark(modifier = Modifier.size(13.dp))
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    text = "@${viewer.username}",
                                                    fontSize = 12.sp,
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                if (timeAgo.isNotBlank()) {
                                                    Text(
                                                        text = "• $timeAgo",
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.45f),
                                                        maxLines = 1,
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (reactionEmoji != null) {
                                        Text(
                                            text = reactionEmoji,
                                            fontSize = 19.sp,
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}
