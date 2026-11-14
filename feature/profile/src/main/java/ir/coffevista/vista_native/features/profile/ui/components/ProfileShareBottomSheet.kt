package ir.coffevista.vista_native.features.profile.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.R as DesignR
import ir.coffevista.vista_native.core.designsystem.component.VistaVerificationBadge
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

@OptIn(ExperimentalMaterial3Api::class)
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
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var isGeneratingStory by remember { mutableStateOf(false) }
    val shareUrl = remember(username) { profileShareUrl(username) }
    val shareText = remember(username, fullName) { profileShareText(username, fullName) }

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
                // Drag handle
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFBDBDBD)),
                )

                // Title
                Text(
                    text = "اشتراک‌گذاری پروفایل",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                )

                // Mini Profile Info Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFF383838), CircleShape)
                            .background(Color(0xFF2A2A2A)),
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
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = fullName?.ifBlank { username } ?: username,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isVerified) {
                                Spacer(Modifier.width(6.dp))
                                VistaVerificationBadge(
                                    isVerified = true,
                                    verificationType = verificationType,
                                    role = role,
                                    size = 16.dp,
                                    isDarkSurface = true,
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "@$username",
                            fontSize = 13.sp,
                            color = Color(0xFFAAAAAA),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!bio.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = bio.trim(),
                                fontSize = 12.sp,
                                color = Color(0xFF888888),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Send via Direct Message option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onSendDirectMessage?.invoke()
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
                    color = Color(0xFF424242),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                // Action buttons row (Copy Link, Share..., Vista ID/QR)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Copy Link
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ProfileShareActionButton(
                            label = "کپی لینک",
                            onClick = {
                                clipboard.setText(AnnotatedString(shareUrl))
                                Toast.makeText(context, "لینک پروفایل کپی شد", Toast.LENGTH_SHORT).show()
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
                        ProfileShareActionButton(
                            label = "اشتراک‌گذاری...",
                            onClick = {
                                onDismiss()
                                shareExternalProfile(context, shareText)
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

                    // Vista Card / QR
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ProfileShareActionButton(
                            label = "کارت ویستا",
                            onClick = {
                                onDismiss()
                                onShowQrCard?.invoke()
                            },
                            iconContent = {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode2,
                                    contentDescription = "کارت ویستا",
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp),
                                )
                            },
                        )
                    }
                }

                // Divider
                HorizontalDivider(
                    color = Color(0xFF424242),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                // App icons row (Instagram Story, WhatsApp, Gmail, Rubika)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Story (Instagram)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ProfileAppIconButton(
                            label = if (isGeneratingStory) "در حال ساخت..." else "استوری",
                            onClick = {
                                if (isGeneratingStory) return@ProfileAppIconButton
                                isGeneratingStory = true
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
                                    } finally {
                                        isGeneratingStory = false
                                        onDismiss()
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

                    // WhatsApp
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        ProfileAppIconButton(
                            label = "پیام‌رسان",
                            onClick = {
                                onDismiss()
                                shareProfileToWhatsApp(context, shareText)
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
                        ProfileAppIconButton(
                            label = "جیمیل",
                            onClick = {
                                onDismiss()
                                shareProfileToGmail(context, shareText, fullName ?: username)
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
                        ProfileAppIconButton(
                            label = "روبیکا",
                            onClick = {
                                onDismiss()
                                shareProfileToRubika(context, shareText)
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
}

@Composable
private fun ProfileShareActionButton(
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
private fun ProfileAppIconButton(
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

private fun shareExternalProfile(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری پروفایل"))
}

private fun shareProfileToWhatsApp(context: Context, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        shareExternalProfile(context, text)
    }
}

private fun shareProfileToGmail(context: Context, text: String, profileName: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.google.android.gm"
            putExtra(Intent.EXTRA_SUBJECT, "پروفایل ویستا: $profileName")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "پروفایل ویستا: $profileName")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(mailtoIntent)
        } catch (_: Exception) {
            shareExternalProfile(context, text)
        }
    }
}

private fun shareProfileToRubika(context: Context, text: String) {
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
        } catch (_: Exception) {}
    }
    if (!launched) {
        shareExternalProfile(context, text)
    }
}
