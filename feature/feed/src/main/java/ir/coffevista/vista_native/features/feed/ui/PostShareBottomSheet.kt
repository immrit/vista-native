package ir.coffevista.vista_native.features.feed.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.features.feed.data.FeedPost

private fun postShareUrl(postId: String): String = "https://cafevista.ir/post/$postId"

private fun postShareText(post: FeedPost): String = buildString {
    append("Vista پست جدید")
    post.authorUsername?.takeIf(String::isNotBlank)?.let {
        append(" @$it")
    }
    post.content?.takeIf(String::isNotBlank)?.let {
        append("\n\n")
        append(it.take(120))
        if (it.length > 120) append("...")
    }
    append("\n\n🌐 مشاهده در Vista: ")
    append(postShareUrl(post.id))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostShareBottomSheet(
    post: FeedPost,
    onDismiss: () -> Unit,
    onShared: () -> Unit,
    onSendDirectMessage: ((FeedPost) -> Unit)? = null,
    onAddToStory: ((FeedPost, String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val text = remember(post) { postShareText(post) }
    val url = remember(post.id) { postShareUrl(post.id) }
    var showStoryThemeDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
            )

            // Title
            Text(
                text = "اشتراک‌گذاری پست",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            // Send via Direct Message option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (onSendDirectMessage != null) {
                            onDismiss()
                            onSendDirectMessage(post)
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MailOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "ارسال از طریق پیام مستقیم",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // Action buttons row (Copy Link, Share..., Add to Story)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ShareActionButton(
                    icon = Icons.Outlined.ContentCopy,
                    label = "کپی لینک",
                    onClick = {
                        clipboard.setText(AnnotatedString(url))
                        Toast.makeText(context, "لینک پست کپی شد", Toast.LENGTH_SHORT).show()
                        onShared()
                        onDismiss()
                    },
                )
                ShareActionButton(
                    icon = Icons.Outlined.Share,
                    label = "اشتراک‌گذاری...",
                    onClick = {
                        shareExternal(context, text)
                        onShared()
                        onDismiss()
                    },
                )
                ShareActionButton(
                    icon = Icons.Outlined.AutoStories,
                    label = "افزودن به استوری",
                    onClick = {
                        showStoryThemeDialog = true
                    },
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // App shortcuts row (Stories, WhatsApp, Gmail, Rubika)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AppShortcutButton(
                    label = "استوری",
                    background = Brush.linearGradient(
                        colors = listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                    ),
                    icon = Icons.Outlined.AutoStories,
                    iconColor = Color.White,
                    onClick = {
                        showStoryThemeDialog = true
                    },
                )
                AppShortcutButton(
                    label = "پیام‌رسان",
                    background = Brush.linearGradient(
                        colors = listOf(Color(0xFF25D366), Color(0xFF128C7E))
                    ),
                    icon = Icons.Outlined.Share,
                    iconColor = Color.White,
                    onClick = {
                        shareToTargetApp(
                            context = context,
                            text = text,
                            packageName = "com.whatsapp",
                            fallbackName = "واتساپ",
                        )
                        onShared()
                        onDismiss()
                    },
                )
                AppShortcutButton(
                    label = "جیمیل",
                    background = Brush.linearGradient(
                        colors = listOf(Color(0xFFEA4335), Color(0xFFD93025))
                    ),
                    icon = Icons.Outlined.Email,
                    iconColor = Color.White,
                    onClick = {
                        shareToTargetApp(
                            context = context,
                            text = text,
                            packageName = "com.google.android.gm",
                            fallbackName = "جیمیل",
                        )
                        onShared()
                        onDismiss()
                    },
                )
                AppShortcutButton(
                    label = "روبیکا",
                    background = Brush.linearGradient(
                        colors = listOf(Color(0xFF6C5CE7), Color(0xFF5B4BC4))
                    ),
                    icon = Icons.Outlined.Share,
                    iconColor = Color.White,
                    onClick = {
                        shareToTargetApp(
                            context = context,
                            text = text,
                            packageName = "ir.resaneh1.iptv",
                            fallbackPackageName = "app.rbmain.a",
                            fallbackName = "روبیکا",
                        )
                        onShared()
                        onDismiss()
                    },
                )
            }
        }
    }

    if (showStoryThemeDialog) {
        AlertDialog(
            onDismissRequest = { showStoryThemeDialog = false },
            title = {
                Text(
                    text = "انتخاب قالب استوری",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StoryThemeOption(
                        icon = Icons.Outlined.DarkMode,
                        label = "قالب تیره",
                        onClick = {
                            showStoryThemeDialog = false
                            onShared()
                            onDismiss()
                            onAddToStory?.invoke(post, "dark") ?: shareExternal(context, text)
                        },
                    )
                    StoryThemeOption(
                        icon = Icons.Outlined.LightMode,
                        label = "قالب روشن",
                        onClick = {
                            showStoryThemeDialog = false
                            onShared()
                            onDismiss()
                            onAddToStory?.invoke(post, "light") ?: shareExternal(context, text)
                        },
                    )
                    StoryThemeOption(
                        icon = Icons.Outlined.AutoAwesome,
                        label = "قالب ویستا",
                        onClick = {
                            showStoryThemeDialog = false
                            onShared()
                            onDismiss()
                            onAddToStory?.invoke(post, "vista") ?: shareExternal(context, text)
                        },
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStoryThemeDialog = false }) {
                    Text("انصراف")
                }
            },
        )
    }
}

@Composable
private fun ShareActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(54.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppShortcutButton(
    label: String,
    background: Brush,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(background)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoryThemeOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun shareExternal(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری پست"))
}

private fun shareToTargetApp(
    context: Context,
    text: String,
    packageName: String,
    fallbackPackageName: String? = null,
    fallbackName: String,
) {
    val pm = context.packageManager
    var targetPackage: String? = null

    try {
        pm.getPackageInfo(packageName, 0)
        targetPackage = packageName
    } catch (_: Exception) {
        if (fallbackPackageName != null) {
            try {
                pm.getPackageInfo(fallbackPackageName, 0)
                targetPackage = fallbackPackageName
            } catch (_: Exception) {
                targetPackage = null
            }
        }
    }

    if (targetPackage != null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(targetPackage)
        }
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "برنامه $fallbackName نصب نیست", Toast.LENGTH_SHORT).show()
        shareExternal(context, text)
    }
}
