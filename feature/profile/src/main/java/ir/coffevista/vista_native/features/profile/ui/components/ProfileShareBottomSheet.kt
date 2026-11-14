package ir.coffevista.vista_native.features.profile.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.component.VistaShareActionRail
import ir.coffevista.vista_native.core.designsystem.component.VistaShareModalSheet
import ir.coffevista.vista_native.core.designsystem.component.VistaShareSheetActionRowTile
import ir.coffevista.vista_native.core.designsystem.component.VistaShareSheetTokens
import ir.coffevista.vista_native.core.designsystem.component.VistaVerificationBadge
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

    VistaShareModalSheet(
        title = "اشتراک‌گذاری پروفایل",
        onDismiss = onDismiss,
    ) {
        // Mini Profile Preview Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = VistaShareSheetTokens.CardBackground,
            border = BorderStroke(1.dp, VistaShareSheetTokens.CardBorder),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF383B44), CircleShape)
                        .background(Color(0xFF2B2D34)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = fullName ?: username,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        val initial = (fullName?.firstOrNull() ?: username.firstOrNull() ?: 'U')
                            .uppercaseChar().toString()
                        Text(
                            text = initial,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fullName?.ifBlank { username } ?: username,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isVerified) {
                            Spacer(Modifier.width(5.dp))
                            VistaVerificationBadge(
                                isVerified = true,
                                verificationType = verificationType,
                                role = role,
                                size = 15.dp,
                                isDarkSurface = true,
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "@$username",
                        fontSize = 12.5.sp,
                        color = VistaShareSheetTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!bio.isNullOrBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = bio.trim(),
                            fontSize = 11.5.sp,
                            color = Color(0xFF838794),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (onSendDirectMessage != null) {
            Spacer(Modifier.height(10.dp))
            VistaShareSheetActionRowTile(
                title = "ارسال از طریق پیام مستقیم",
                subtitle = "ارسال مستقیم پروفایل در چت ویستا",
                onClick = {
                    onDismiss()
                    onSendDirectMessage.invoke()
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
        }

        if (onShowQrCard != null) {
            Spacer(Modifier.height(10.dp))
            VistaShareSheetActionRowTile(
                title = "کارت ویستا و QR",
                subtitle = "نمایش کارت هویت سه‌بعدی پروفایل",
                onClick = {
                    onDismiss()
                    onShowQrCard.invoke()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.QrCode2,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }

        Spacer(Modifier.height(14.dp))

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
        )
        VistaShareActionRail(actions = railActions)
    }
}
