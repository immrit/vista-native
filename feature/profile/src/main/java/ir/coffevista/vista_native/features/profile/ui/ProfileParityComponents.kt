package ir.coffevista.vista_native.features.profile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

internal data class ProfileHeaderModel(
    val fullName: String,
    val username: String?,
    val bio: String?,
    val avatarUrl: String?,
    val isVerified: Boolean,
    val isPremium: Boolean,
    val isPrivate: Boolean,
    val postCount: Long,
    val followerCount: Long,
    val followingCount: Long,
    val joinOrder: Long,
)

@Composable
internal fun ProfileParityList(
    header: ProfileHeaderModel,
    postsState: ProfilePostsPresentationState,
    actionContent: @Composable () -> Unit,
    privatePosts: Boolean,
    onPostClick: (String) -> Unit,
    onLoadMore: () -> Unit,
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 110.dp),
    ) {
        item(key = "profile-header") {
            ProfileHeader(header)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                actionContent()
            }
            ProfileTabStrip()
        }
        when {
            privatePosts -> item(key = "private-posts") {
                ProfileLockedState()
            }
            postsState.isInitialLoading -> profilePostsSkeleton()
            postsState.posts.isEmpty() && postsState.error != null -> item(key = "posts-error") {
                ProfilePostsError(postsState.error)
            }
            postsState.posts.isEmpty() -> item(key = "posts-empty") {
                ProfilePostsEmpty()
            }
            else -> {
                items(
                    count = postsState.posts.size,
                    key = { postsState.posts[it].id },
                ) { index ->
                    ProfilePostCard(
                        post = postsState.posts[index],
                        onClick = { onPostClick(postsState.posts[index].id) },
                    )
                    HorizontalDivider(
                        thickness = .5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
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
private fun ProfileHeader(header: ProfileHeaderModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(header)
            Spacer(Modifier.width(28.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStat(header.postCount, "پست")
                ProfileStat(header.followerCount, "دنبال‌کننده")
                ProfileStat(header.followingCount, "دنبال‌شونده")
            }
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(header.fullName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (header.isVerified) VerifiedBadge(Modifier.padding(start = 4.dp))
            if (header.isPremium) {
                Text(
                    text = "V",
                    modifier = Modifier
                        .padding(start = 4.dp)
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
        header.bio?.takeIf(String::isNotBlank)?.let {
            Text(
                text = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                lineHeight = 19.6.sp,
            )
        }
        if (header.joinOrder > 0) {
            Surface(
                modifier = Modifier.padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = "عضو شماره ${header.joinOrder} ویستا",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(header: ProfileHeaderModel) {
    val modifier = Modifier
        .size(84.dp)
        .clip(CircleShape)
    if (header.avatarUrl.isNullOrBlank()) {
        Image(
            painter = painterResource(DesignSystemR.drawable.vista_default_avatar),
            contentDescription = "تصویر پیش‌فرض ${header.fullName}",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        AsyncImage(
            model = header.avatarUrl,
            contentDescription = "تصویر نمایه ${header.fullName}",
            modifier = modifier,
            placeholder = painterResource(DesignSystemR.drawable.vista_default_avatar),
            error = painterResource(DesignSystemR.drawable.vista_default_avatar),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun ProfileStat(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            modifier = Modifier.padding(top = 1.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ProfileTabStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_profile_grid),
            contentDescription = "پست‌ها",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_profile_play),
            contentDescription = "کلیپ‌ها",
            modifier = Modifier.size(26.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_profile_music),
            contentDescription = "موسیقی‌ها",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(thickness = .5.dp, color = MaterialTheme.colorScheme.outlineVariant)
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
private fun ProfileLockedState() = ProfileState(
    icon = "🔒",
    title = "این حساب خصوصی است",
    message = "برای دیدن پست‌ها باید این حساب را دنبال کنید.",
    tag = ProfileParityTestTags.Private,
)

@Composable
private fun ProfilePostsEmpty() = ProfileState(
    icon = "▦",
    title = "هنوز پستی منتشر نشده",
    message = "پست‌های این نمایه اینجا نمایش داده می‌شوند.",
    tag = ProfileParityTestTags.Empty,
)

@Composable
private fun ProfilePostsError(message: String) = ProfileState(
    icon = "!",
    title = "دریافت پست‌ها ناموفق بود",
    message = message,
    tag = ProfileParityTestTags.Error,
)

@Composable
private fun ProfileState(icon: String, title: String, message: String, tag: String) {
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
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 40.sp)
            }
        }
        Text(
            title,
            modifier = Modifier.padding(top = 16.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            message,
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProfilePostCard(post: ProfilePostUiModel, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(ProfileParityTestTags.post(post.id)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (post.authorAvatarUrl.isNullOrBlank()) {
                Image(
                    painterResource(DesignSystemR.drawable.vista_default_avatar),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                AsyncImage(
                    model = post.authorAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        post.authorFullName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (post.authorIsVerified) VerifiedBadge(Modifier.padding(start = 4.dp))
                    Text(
                        " • ${profileRelativeTime(post.createdAt)}",
                        modifier = Modifier.padding(start = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        post.content?.takeIf(String::isNotBlank)?.let {
            Text(
                it,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
        }
        post.imageUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = if (post.videoUrl == null) "تصویر پست" else "ویدیو",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(260.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileHeart(filled = post.isLiked, Modifier.size(19.dp))
            if (!post.hideLikeCount) {
                Text(post.likeCount.toString(), Modifier.padding(start = 4.dp), fontSize = 13.sp)
            }
            Spacer(Modifier.width(14.dp))
            Image(
                painterResource(DesignSystemR.drawable.vista_post_comment),
                contentDescription = "دیدگاه",
                modifier = Modifier.size(19.dp),
            )
            if (!post.hideCommentCount) {
                Text(
                    post.commentCount.toString(),
                    Modifier.padding(start = 4.dp),
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.width(14.dp))
            Image(
                painterResource(DesignSystemR.drawable.vista_post_send),
                contentDescription = "ارسال",
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun VerifiedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(14.dp)
            .background(Color(0xFF2196F3), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
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

private fun profileRelativeTime(raw: String): String = runCatching {
    val duration = Duration.between(Instant.parse(raw), Instant.now())
    when {
        duration.isNegative || duration.toMinutes() < 1 -> "اکنون"
        duration.toHours() < 1 -> "${duration.toMinutes()} دقیقه"
        duration.toDays() < 1 -> "${duration.toHours()} ساعت"
        duration.toDays() < 7 -> "${duration.toDays()} روز"
        else -> "${duration.toDays() / 7} هفته"
    }
}.getOrDefault(raw.take(10))

internal object ProfileParityTestTags {
    const val List = "profile-parity-list"
    const val Empty = "profile-posts-empty"
    const val Private = "profile-posts-private"
    const val Error = "profile-posts-error"
    fun post(id: String) = "profile-post-$id"
}
