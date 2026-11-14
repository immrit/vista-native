package ir.coffevista.vista_native.features.feed.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import ir.coffevista.vista_native.core.designsystem.R as DesignR
import ir.coffevista.vista_native.features.feed.data.FeedPost
import kotlinx.coroutines.launch
import java.io.File

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
    onNavigateToStoryEditor: ((Uri) -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val text = remember(post) { postShareText(post) }
    val url = remember(post.id) { postShareUrl(post.id) }
    var showStoryThemeDialog by remember { mutableStateOf(false) }
    var isGeneratingStory by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Black,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFBDBDBD)), // Colors.grey[400]
                )

                // Title
                Text(
                    text = "اشتراک‌گذاری پست",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp),
                )

                // Send via Direct Message option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onSendDirectMessage?.invoke(post)
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MailOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "ارسال از طریق پیام مستقیم",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                    )
                }

                // Divider
                HorizontalDivider(
                    color = Color(0xFF424242), // Colors.grey[800]
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                // Action buttons row (Copy Link, Share..., Add to Story)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Copy Link
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ShareActionButton(
                            label = "کپی لینک",
                            onClick = {
                                clipboard.setText(AnnotatedString(url))
                                Toast.makeText(context, "لینک پست کپی شد", Toast.LENGTH_SHORT).show()
                                onShared()
                                onDismiss()
                            },
                            iconContent = {
                                Icon(
                                    painter = painterResource(DesignR.drawable.ic_share_link),
                                    contentDescription = "کپی لینک",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                        )
                    }

                    // Share via...
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ShareActionButton(
                            label = "اشتراک‌گذاری...",
                            onClick = {
                                shareExternal(context, text)
                                onShared()
                                onDismiss()
                            },
                            iconContent = {
                                Image(
                                    painter = painterResource(DesignR.drawable.ic_share_send),
                                    contentDescription = "اشتراک‌گذاری...",
                                    colorFilter = ColorFilter.tint(Color.Black),
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                        )
                    }

                    // Add to Story
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ShareActionButton(
                            label = "افزودن به استوری",
                            onClick = {
                                if (!isGeneratingStory) {
                                    showStoryThemeDialog = true
                                }
                            },
                            iconContent = {
                                Image(
                                    painter = painterResource(DesignR.drawable.ic_share_story),
                                    contentDescription = "افزودن به استوری",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                        )
                    }
                }

                // Divider
                HorizontalDivider(
                    color = Color(0xFF424242), // Colors.grey[800]
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                // App icons row (Story, Messenger, Gmail, Rubika)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Story (Social)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AppIconButton(
                            label = "استوری",
                            onClick = {
                                if (isGeneratingStory) return@AppIconButton
                                isGeneratingStory = true
                                Toast.makeText(context, "در حال تولید تصویر استوری...", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    try {
                                        val assets = PostStoryTemplateGenerator.generateInstagramStoryAssets(
                                            context = context,
                                            post = post,
                                        )
                                        val fallbackCombined = if (canResolveInstagramStoryIntent(context)) {
                                            assets.stickerFile
                                        } else {
                                            runCatching {
                                                PostStoryTemplateGenerator.generateTemplate(context, post, "vista")
                                            }.getOrDefault(assets.stickerFile)
                                        }
                                        isGeneratingStory = false
                                        shareAssetsToInstagramStory(
                                            context = context,
                                            backgroundFile = assets.backgroundFile,
                                            stickerFile = assets.stickerFile,
                                            fallbackCombinedFile = fallbackCombined,
                                            contentUrl = url,
                                            shareText = text,
                                        )
                                        onShared()
                                        onDismiss()
                                    } catch (_: Throwable) {
                                        isGeneratingStory = false
                                        Toast.makeText(context, "خطا در تولید تصویر استوری", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            iconContent = {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                ) {
                                    Image(
                                        painter = painterResource(DesignR.drawable.ic_share_social),
                                        contentDescription = "استوری",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            },
                        )
                    }

                    // Messaging (WhatsApp)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AppIconButton(
                            label = "پیام‌رسان",
                            onClick = {
                                onShared()
                                onDismiss()
                                shareToWhatsApp(context, text)
                            },
                            iconContent = {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(DesignR.drawable.ic_share_whatsapp),
                                        contentDescription = "پیام‌رسان",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            },
                        )
                    }

                    // Gmail
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AppIconButton(
                            label = "جیمیل",
                            onClick = {
                                onShared()
                                onDismiss()
                                shareToGmail(context, text)
                            },
                            iconContent = {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(DesignR.drawable.ic_share_gmail),
                                        contentDescription = "جیمیل",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            },
                        )
                    }

                    // Rubika
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AppIconButton(
                            label = "روبیکا",
                            onClick = {
                                onShared()
                                onDismiss()
                                shareToRubika(context, text)
                            },
                            iconContent = {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF6C5CE7))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(DesignR.drawable.ic_share_rubika),
                                        contentDescription = "روبیکا",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    fun handleStoryThemeSelection(theme: String) {
        if (isGeneratingStory) return
        showStoryThemeDialog = false
        if (onNavigateToStoryEditor != null) {
            isGeneratingStory = true
            Toast.makeText(context, "در حال آماده‌سازی تصویر استوری...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                try {
                    val file = PostStoryTemplateGenerator.generateTemplate(context, post, theme)
                    onShared()
                    onDismiss()
                    onNavigateToStoryEditor(Uri.fromFile(file))
                } catch (e: Throwable) {
                    isGeneratingStory = false
                    Toast.makeText(context, "خطا در ساخت قالب استوری", Toast.LENGTH_SHORT).show()
                    onAddToStory?.invoke(post, theme) ?: shareExternal(context, text)
                }
            }
        } else if (onAddToStory != null) {
            onShared()
            onDismiss()
            onAddToStory.invoke(post, theme)
        } else {
            onShared()
            onDismiss()
            shareExternal(context, text)
        }
    }

    if (showStoryThemeDialog) {
        Dialog(
            onDismissRequest = { if (!isGeneratingStory) showStoryThemeDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (!isGeneratingStory) showStoryThemeDialog = false
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .width(260.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { /* absorb click */ },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF252525),
                    shadowElevation = 14.dp,
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            StoryThemeMenuItem(
                                icon = Icons.Outlined.DarkMode,
                                label = "قالب تیره",
                                onClick = { handleStoryThemeSelection("dark") },
                            )
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.12f),
                                thickness = 0.5.dp,
                            )
                            StoryThemeMenuItem(
                                icon = Icons.Outlined.LightMode,
                                label = "قالب روشن",
                                onClick = { handleStoryThemeSelection("light") },
                            )
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.12f),
                                thickness = 0.5.dp,
                            )
                            StoryThemeMenuItem(
                                icon = Icons.Outlined.AutoAwesome,
                                label = "قالب ویستا",
                                onClick = { handleStoryThemeSelection("vista") },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareActionButton(
    label: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            iconContent()
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.height(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AppIconButton(
    label: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        iconContent()
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.height(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StoryThemeMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(19.dp),
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

internal fun canResolveInstagramStoryIntent(context: Context): Boolean {
    val probe = Intent("com.instagram.share.ADD_TO_STORY").apply {
        setPackage("com.instagram.android")
        type = "image/png"
    }
    return context.packageManager.resolveActivity(probe, 0) != null
}

fun shareAssetsToInstagramStory(
    context: Context,
    backgroundFile: File,
    stickerFile: File,
    fallbackCombinedFile: File = stickerFile,
    contentUrl: String,
    shareText: String,
) {
    try {
        val authority = "${context.packageName}.files"
        val backgroundUri = FileProvider.getUriForFile(context, authority, backgroundFile)
        val stickerUri = FileProvider.getUriForFile(context, authority, stickerFile)

        // Grant read URI permission for both background (Intent.data) and movable sticker extra.
        listOf(backgroundUri, stickerUri).forEach { assetUri ->
            try {
                context.grantUriPermission(
                    "com.instagram.android",
                    assetUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) {}
        }

        // 1. Official Instagram Story Intent: Vista background + transparent movable Post Card sticker
        val instagramStoryIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(backgroundUri, "image/png")
            setPackage("com.instagram.android")
            putExtra("interactive_asset_uri", stickerUri)
            putExtra("content_url", contentUrl)
            putExtra("source_application", context.packageName)
            clipData = ClipData.newRawUri("", backgroundUri).apply {
                addItem(ClipData.Item(stickerUri))
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(instagramStoryIntent)
            return
        } catch (_: android.content.ActivityNotFoundException) {
            // Instagram story action not registered, try standard send
        } catch (_: SecurityException) {
            // Permission or signature issue
        }

        // 2. Direct Send to Instagram app using single combined image
        val fallbackUri = FileProvider.getUriForFile(context, authority, fallbackCombinedFile)
        try {
            context.grantUriPermission(
                "com.instagram.android",
                fallbackUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: Exception) {}

        val directInstagramIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            setPackage("com.instagram.android")
            putExtra(Intent.EXTRA_STREAM, fallbackUri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(directInstagramIntent)
            return
        } catch (_: android.content.ActivityNotFoundException) {
            // Instagram not installed at all
        }

        // 3. Fallback when Instagram is not installed
        Toast.makeText(context, "برنامه اینستاگرام بر روی دستگاه شما نصب نیست", Toast.LENGTH_SHORT).show()
        shareStoryImageFile(context, fallbackCombinedFile, shareText)
    } catch (_: Exception) {
        shareStoryImageFile(context, fallbackCombinedFile, shareText)
    }
}

private fun shareStoryImageFile(context: Context, file: File, caption: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری در استوری"))
    } catch (_: Exception) {
        shareExternal(context, caption)
    }
}

private fun shareToWhatsApp(context: Context, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        shareExternal(context, text)
    }
}

private fun shareToGmail(context: Context, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.google.android.gm"
            putExtra(Intent.EXTRA_SUBJECT, "Vista - اشتراک‌گذاری پست")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "Vista - اشتراک‌گذاری پست")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(mailtoIntent)
        } catch (_: Exception) {
            shareExternal(context, text)
        }
    }
}

private fun shareToRubika(context: Context, text: String) {
    val packages = listOf("app.rubika.im", "ir.resaneh1.iptv")
    var launched = false
    for (pkg in packages) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = pkg
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(intent)
            launched = true
            break
        } catch (_: Exception) {
            // try next package
        }
    }
    if (!launched) {
        shareExternal(context, text)
    }
}
