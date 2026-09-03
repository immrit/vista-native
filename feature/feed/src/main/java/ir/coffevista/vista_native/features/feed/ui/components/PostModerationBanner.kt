package ir.coffevista.vista_native.features.feed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.features.feed.data.FeedPost

private val AmberBgDark = Color(0xFFFFC107).copy(alpha = 0.12f)
private val AmberBgLight = Color(0xFFFFC107).copy(alpha = 0.15f)
private val AmberBorderDark = Color(0xFFFFC107).copy(alpha = 0.35f)
private val AmberBorderLight = Color(0xFFFFC107).copy(alpha = 0.45f)
private val AmberIconDark = Color(0xFFFFD54F)
private val AmberIconLight = Color(0xFFFF6F00)
private val AmberTitleDark = Color(0xFFFFECB3)
private val AmberTitleLight = Color(0xFF3E2723)
private val AmberReasonDark = Color(0xFFFFECB3).copy(alpha = 0.85f)
private val AmberReasonLight = Color(0xFF4E342E)

@Composable
fun PostModerationBanner(
    post: FeedPost,
    isOwner: Boolean,
    onAppealClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (!post.editedByVista) return

    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) AmberBgDark else AmberBgLight
    val borderColor = if (isDark) AmberBorderDark else AmberBorderLight
    val iconColor = if (isDark) AmberIconDark else AmberIconLight
    val titleColor = if (isDark) AmberTitleDark else AmberTitleLight
    val reasonColor = if (isDark) AmberReasonDark else AmberReasonLight

    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(shape)
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "این پست توسط تیم Vista ویرایش شده است",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    lineHeight = 18.sp
                )

                val reason = post.moderationReason?.trim()
                if (!reason.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reason,
                        fontSize = 12.sp,
                        color = reasonColor,
                        lineHeight = 17.sp
                    )
                }

                if (isOwner && onAppealClick != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { onAppealClick(post.id) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Gavel,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ثبت اعتراض",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFFE082) else Color(0xFFE65100)
                        )
                    }
                }
            }
        }
    }
}
