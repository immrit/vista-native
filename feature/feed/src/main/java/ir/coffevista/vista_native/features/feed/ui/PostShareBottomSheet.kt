package ir.coffevista.vista_native.features.feed.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import ir.coffevista.vista_native.core.designsystem.component.VistaShareActionRail
import ir.coffevista.vista_native.core.designsystem.component.VistaShareModalSheet
import ir.coffevista.vista_native.core.designsystem.component.VistaShareSheetActionRowTile
import ir.coffevista.vista_native.core.designsystem.component.buildVistaStandardShareActions
import ir.coffevista.vista_native.core.designsystem.component.shareTextToTelegram
import ir.coffevista.vista_native.core.designsystem.component.shareTextToThreads
import ir.coffevista.vista_native.core.designsystem.component.shareTextToWhatsApp
import ir.coffevista.vista_native.core.designsystem.component.shareTextToX
import ir.coffevista.vista_native.core.designsystem.component.shareTextViaSystemChooser
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
    var isGeneratingStory by remember { mutableStateOf(false) }

    val handleAddToVistaStory: () -> Unit = handleStory@{
        if (isGeneratingStory) return@handleStory
        if (onNavigateToStoryEditor != null) {
            isGeneratingStory = true
            Toast.makeText(context, "در حال آماده‌سازی استوری ویستا...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                try {
                    val assets = PostStoryTemplateGenerator.generateInstagramStoryAssets(
                        context = context,
                        post = post,
                    )
                    val launchUri = PostStoryTemplateGenerator.buildVistaStoryLaunchUri(assets)
                    onShared()
                    onNavigateToStoryEditor(launchUri)
                    onDismiss()
                } catch (_: Throwable) {
                    Toast.makeText(context, "خطا در آماده‌سازی استوری ویستا", Toast.LENGTH_SHORT).show()
                } finally {
                    isGeneratingStory = false
                }
            }
        } else if (onAddToStory != null) {
            onShared()
            onAddToStory.invoke(post, "vista")
            onDismiss()
        } else {
            Toast.makeText(context, "امکان باز کردن ویرایشگر استوری وجود ندارد", Toast.LENGTH_SHORT).show()
        }
    }

    val handleShareInstagramStory: () -> Unit = handleIg@{
        if (isGeneratingStory) return@handleIg
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
                Toast.makeText(context, "خطا در تولید تصویر استوری", Toast.LENGTH_SHORT).show()
            } finally {
                isGeneratingStory = false
            }
        }
    }

    VistaShareModalSheet(
        title = "اشتراک‌گذاری پست",
        onDismiss = onDismiss,
    ) {
        if (onSendDirectMessage != null) {
            VistaShareSheetActionRowTile(
                title = "ارسال از طریق پیام مستقیم",
                subtitle = "ارسال مستقیم پست به گفتگوهای ویستا",
                onClick = {
                    onDismiss()
                    onSendDirectMessage.invoke(post)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.MailOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
            Spacer(Modifier.height(14.dp))
        }

        val railActions = buildVistaStandardShareActions(
            isBusy = isGeneratingStory,
            showAddToVistaStory = onNavigateToStoryEditor != null || onAddToStory != null,
            onAddToVistaStory = handleAddToVistaStory,
            onShareInstagramStory = handleShareInstagramStory,
            onCopyLink = {
                clipboard.setText(AnnotatedString(url))
                Toast.makeText(context, "لینک پست کپی شد", Toast.LENGTH_SHORT).show()
                onShared()
                onDismiss()
            },
            onShareTelegram = {
                onShared()
                onDismiss()
                shareTextToTelegram(context, text, url)
            },
            onShareWhatsApp = {
                onShared()
                onDismiss()
                shareTextToWhatsApp(context, text)
            },
            onShareThreads = {
                onShared()
                onDismiss()
                shareTextToThreads(context, text)
            },
            onShareX = {
                onShared()
                onDismiss()
                shareTextToX(context, text)
            },
            onShareMore = {
                onShared()
                onDismiss()
                shareTextViaSystemChooser(context, text, "اشتراک‌گذاری پست")
            },
        )
        VistaShareActionRail(actions = railActions)
    }
}

private fun shareExternal(context: Context, text: String) {
    shareTextViaSystemChooser(context, text, "اشتراک‌گذاری پست")
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
