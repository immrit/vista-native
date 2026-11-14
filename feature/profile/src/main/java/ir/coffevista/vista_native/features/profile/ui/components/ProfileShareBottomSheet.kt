package ir.coffevista.vista_native.features.profile.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import ir.coffevista.vista_native.core.designsystem.component.VistaUnifiedShareSheet
import ir.coffevista.vista_native.core.designsystem.component.buildVistaStandardShareActions
import ir.coffevista.vista_native.core.designsystem.component.shareTextToTelegram
import ir.coffevista.vista_native.core.designsystem.component.shareTextToThreads
import ir.coffevista.vista_native.core.designsystem.component.shareTextToWhatsApp
import ir.coffevista.vista_native.core.designsystem.component.shareTextToX
import ir.coffevista.vista_native.core.designsystem.component.shareTextViaSystemChooser
import ir.coffevista.vista_native.features.feed.ui.PostStoryTemplateGenerator
import ir.coffevista.vista_native.features.feed.ui.shareAssetsToInstagramStory
import kotlinx.coroutines.launch

private fun profileShareUrl(username: String): String = "https://cafevista.ir/u/$username"

private fun profileShareText(username: String, fullName: String?): String = buildString {
    append("پروفایل ")
    if (!fullName.isNullOrBlank()) {
        append(fullName)
        append(" (")
    }
    append("@$username")
    if (!fullName.isNullOrBlank()) {
        append(")")
    }
    append(" در ویستا\n\n🌐 مشاهده در ویستا: ")
    append(profileShareUrl(username))
}

@Composable
fun ProfileShareBottomSheet(
    username: String,
    fullName: String? = null,
    avatarUrl: String? = null,
    bio: String? = null,
    isVerified: Boolean = false,
    verificationType: String? = null,
    role: String? = null,
    onDismiss: () -> Unit,
    onSendDirectMessage: (() -> Unit)? = null,
    onShowQrCard: (() -> Unit)? = null,
    onNavigateToStoryEditor: ((Uri) -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var isGeneratingStory by remember { mutableStateOf(false) }
    val shareUrl = remember(username) { profileShareUrl(username) }
    val shareText = remember(username, fullName) { profileShareText(username, fullName) }

    val handleAddToVistaStory: () -> Unit = handleStory@{
        if (isGeneratingStory) return@handleStory
        if (onNavigateToStoryEditor == null) {
            Toast.makeText(context, "امکان باز کردن ویرایشگر استوری وجود ندارد", Toast.LENGTH_SHORT).show()
            return@handleStory
        }
        isGeneratingStory = true
        Toast.makeText(context, "در حال آماده‌سازی استوری ویستا...", Toast.LENGTH_SHORT).show()
        coroutineScope.launch {
            try {
                val assets = PostStoryTemplateGenerator.generateProfileInstagramStoryAssets(
                    context = context,
                    username = username,
                    fullName = fullName,
                    avatarUrl = avatarUrl,
                    bio = bio,
                    isVerified = isVerified,
                    verificationType = verificationType,
                    role = role,
                )
                val launchUri = PostStoryTemplateGenerator.buildVistaStoryLaunchUri(assets)
                onNavigateToStoryEditor(launchUri)
                onDismiss()
            } catch (_: Throwable) {
                Toast.makeText(context, "خطا در آماده‌سازی استوری ویستا", Toast.LENGTH_SHORT).show()
            } finally {
                isGeneratingStory = false
            }
        }
    }

    val handleShareInstagramStory: () -> Unit = handleIg@{
        if (isGeneratingStory) return@handleIg
        isGeneratingStory = true
        Toast.makeText(context, "در حال تولید تصویر استوری...", Toast.LENGTH_SHORT).show()
        coroutineScope.launch {
            try {
                val assets = PostStoryTemplateGenerator.generateProfileInstagramStoryAssets(
                    context = context,
                    username = username,
                    fullName = fullName,
                    avatarUrl = avatarUrl,
                    bio = bio,
                    isVerified = isVerified,
                    verificationType = verificationType,
                    role = role,
                )
                shareAssetsToInstagramStory(
                    context = context,
                    backgroundFile = assets.backgroundFile,
                    stickerFile = assets.stickerFile,
                    contentUrl = shareUrl,
                    shareText = shareText,
                )
                onDismiss()
            } catch (_: Throwable) {
                Toast.makeText(context, "خطا در تولید تصویر استوری", Toast.LENGTH_SHORT).show()
            } finally {
                isGeneratingStory = false
            }
        }
    }

    val railActions = buildVistaStandardShareActions(
        isBusy = isGeneratingStory,
        showAddToVistaStory = onNavigateToStoryEditor != null,
        onAddToVistaStory = handleAddToVistaStory,
        onShareInstagramStory = handleShareInstagramStory,
        onCopyLink = {
            clipboard.setText(AnnotatedString(shareUrl))
            Toast.makeText(context, "لینک پروفایل کپی شد", Toast.LENGTH_SHORT).show()
            onDismiss()
        },
        onShareTelegram = {
            onDismiss()
            shareTextToTelegram(context, shareText, shareUrl)
        },
        onShareWhatsApp = {
            onDismiss()
            shareTextToWhatsApp(context, shareText)
        },
        onShareThreads = {
            onDismiss()
            shareTextToThreads(context, shareText)
        },
        onShareX = {
            onDismiss()
            shareTextToX(context, shareText)
        },
        onShareMore = {
            onDismiss()
            shareTextViaSystemChooser(context, shareText, "اشتراک‌گذاری پروفایل")
        },
        onShowQrCard = onShowQrCard?.let { showQr ->
            {
                onDismiss()
                showQr()
            }
        },
    )

    VistaUnifiedShareSheet(
        onDismiss = onDismiss,
        actions = railActions,
        onSendToRecipients = { gateway, recipients ->
            recipients.count { recipient -> gateway.sendText(recipient, shareText) }
        },
        onSentSuccess = { sentCount, recipients ->
            val message = if (sentCount == 1) {
                "پروفایل برای ${recipients.first().displayName} ارسال شد"
            } else {
                "پروفایل برای $sentCount مخاطب ارسال شد"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
        sendFailureMessage = "ارسال پروفایل انجام نشد. دوباره تلاش کنید.",
    )
}
