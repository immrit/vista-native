package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AdminEditPostResult(
    val content: String?,
    val moderationReason: String?,
    val clearContent: Boolean,
    val clearImage: Boolean,
    val clearVideo: Boolean,
    val clearMusic: Boolean,
    val notifyUser: Boolean,
)

private data class PresetOption(
    val label: String,
    val content: String,
    val reason: String,
)

private val PRESETS = listOf(
    PresetOption(
        label = "نقض قوانین عمومی",
        content = "این پست با قوانین Vista سازگار نیست و توسط تیم Vista ویرایش شده است.",
        reason = "نقض قوانین عمومی Vista",
    ),
    PresetOption(
        label = "تبلیغات ممنوع",
        content = "این پست به دلیل تبلیغات غیرمجاز توسط تیم Vista ویرایش شده است.",
        reason = "تبلیغات ممنوع",
    ),
    PresetOption(
        label = "محتوای نامناسب",
        content = "این پست به دلیل محتوای نامناسب توسط تیم Vista ویرایش شده است.",
        reason = "محتوای نامناسب",
    ),
    PresetOption(
        label = "کلاهبرداری / فریب",
        content = "این پست به دلیل محتوای گمراه‌کننده یا کلاهبرداری توسط تیم Vista ویرایش شده است.",
        reason = "کلاهبرداری یا فریب کاربر",
    ),
    PresetOption(
        label = "حذف متن (خالی)",
        content = "",
        reason = "حذف محتوای متنی",
    ),
)

@Composable
fun VistaAdminEditPostDialog(
    initialContent: String,
    hasImage: Boolean = false,
    hasVideo: Boolean = false,
    hasMusic: Boolean = false,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (AdminEditPostResult) -> Unit,
) {
    var content by remember { mutableStateOf(initialContent) }
    var reason by remember { mutableStateOf("") }
    var clearImage by remember { mutableStateOf(false) }
    var clearVideo by remember { mutableStateOf(false) }
    var clearMusic by remember { mutableStateOf(false) }
    var notifyUser by remember { mutableStateOf(true) }

    fun resolveDirection(text: String): LayoutDirection {
        val persianRegex = Regex("[\u0600-\u06FF]")
        val englishRegex = Regex("[a-zA-Z]")
        val persianCount = persianRegex.findAll(text).count()
        val englishCount = englishRegex.findAll(text).count()
        return if (persianCount >= englishCount) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF1E88E5),
                    modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ویرایش نظارتی پست",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "ابزار مدیریت محتوا و اعمال برچسب ویرایش Vista",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Banner Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF8E1))
                        .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF57F17),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "پیش‌نمایش بنر در اپلیکیشن",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFF57F17),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "این پست توسط تیم Vista ویرایش شده است",
                            fontSize = 13.sp,
                            color = Color(0xFF3E2723),
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (reason.isNotBlank()) {
                            Text(
                                text = "دلیل: $reason",
                                fontSize = 11.sp,
                                color = Color(0xFF5D4037),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Presets
                Text(
                    text = "متن‌های آماده",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PRESETS.forEach { preset ->
                        FilterChip(
                            selected = reason == preset.reason,
                            onClick = {
                                content = preset.content
                                reason = preset.reason
                            },
                            label = { Text(preset.label, fontSize = 11.sp) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Content TextField
                Text(
                    text = "متن جایگزین",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(4.dp))
                CompositionLocalProvider(LocalLayoutDirection provides resolveDirection(content)) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        placeholder = { Text("متن جدید پست را وارد کنید...") },
                        enabled = !isSubmitting,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Reason TextField
                Text(
                    text = "دلیل نظارت (جهت نمایش در بنر و اعلان کاربر)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(4.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثلاً نقض قوانین عمومی Vista") },
                        singleLine = true,
                        enabled = !isSubmitting,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Media & Notification Controls
                if (hasImage || hasVideo || hasMusic) {
                    Text(
                        text = "ابزارهای پاک‌سازی رسانه",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                }

                if (hasImage) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearImage = !clearImage }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = clearImage, onCheckedChange = { clearImage = it })
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("حذف تصویر / گالری پست", fontSize = 12.sp)
                    }
                }

                if (hasVideo) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearVideo = !clearVideo }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = clearVideo, onCheckedChange = { clearVideo = it })
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("حذف ویدیوی پست", fontSize = 12.sp)
                    }
                }

                if (hasMusic) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearMusic = !clearMusic }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = clearMusic, onCheckedChange = { clearMusic = it })
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("حذف موزیک پست", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { notifyUser = !notifyUser }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = notifyUser, onCheckedChange = { notifyUser = it })
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFF1E88E5),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("اطلاع‌رسانی به کاربر از طریق پیام سیستمی", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = content.trim()
                    val isClear = trimmed.isEmpty()
                    onConfirm(
                        AdminEditPostResult(
                            content = if (isClear) null else trimmed,
                            moderationReason = reason.trim().takeIf { it.isNotEmpty() },
                            clearContent = isClear,
                            clearImage = clearImage,
                            clearVideo = clearVideo,
                            clearMusic = clearMusic,
                            notifyUser = notifyUser,
                        )
                    )
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E88E5),
                ),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("ذخیره و اعمال نظارت", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
            ) {
                Text("لغو")
            }
        },
    )
}
