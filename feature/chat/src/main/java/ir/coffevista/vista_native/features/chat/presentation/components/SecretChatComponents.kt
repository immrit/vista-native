package ir.coffevista.vista_native.features.chat.presentation.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

val TelegramSecretGreen = Color(0xFF4FAE63)
val TelegramSecretGreenDark = Color(0xFF2E7D32)
val TelegramSecretBadgeBg = Color(0x1F4FAE63)

private val TELEGRAM_KEY_EMOJIS = listOf(
    "🔒", "🛡️", "⚡", "💎", "🚀", "🌟", "🗝️", "🔥",
    "🍀", "🪐", "🎯", "🔮", "👑", "🌊", "🌙", "☀️",
    "🦊", "🦋", "🌸", "⭐", "⚓", "🛸", "🏆", "🎨",
    "🎸", "🎪", "🏝️", "🕊️", "🦁", "🐬", "🌺", "🌈"
)

private val TELEGRAM_MATRIX_PALETTE = listOf(
    Color(0xFF4FAE63),
    Color(0xFF81C784),
    Color(0xFF2E7D32),
    Color(0xFF1B5E20),
)

fun deriveSecretKeyEmojis(seed: String?): List<String> {
    if (seed.isNullOrBlank()) return listOf("🔒", "🛡️", "⚡", "💎")
    val digest = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))
    return (0..3).map { i ->
        val byteVal = (digest[i].toInt() and 0xFF)
        TELEGRAM_KEY_EMOJIS[byteVal % TELEGRAM_KEY_EMOJIS.size]
    }
}

fun deriveSecretKeyMatrix(seed: String?): List<Int> {
    val clean = seed.orEmpty()
    val digest = MessageDigest.getInstance("SHA-256").digest(clean.toByteArray(Charsets.UTF_8))
    return (0 until 64).map { i ->
        val byteIndex = i % digest.size
        val bitShift = (i / digest.size) % 4
        ((digest[byteIndex].toInt() and 0xFF) shr bitShift) and 0x03
    }
}

@Composable
fun SecretChatIntroCard(
    onSetTimer: () -> Unit,
    onViewKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1A222D) else Color(0xFFF4F7F5)
    val borderCol = TelegramSecretGreen.copy(alpha = if (isDark) 0.35f else 0.25f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, borderCol),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                TelegramSecretGreen.copy(alpha = 0.30f),
                                TelegramSecretGreen.copy(alpha = 0.08f),
                                Color.Transparent,
                            )
                        )
                    )
                    .border(1.5.dp, TelegramSecretGreen.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TelegramSecretGreen,
                    modifier = Modifier.size(34.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "گفتگوی محرمانه",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF1A1C1E),
                fontSize = 18.sp,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "ویژگی‌های امنیتی گفتگوی محرمانه ویستا:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SecurityPillarRow(
                    icon = Icons.Default.Lock,
                    title = "رمزگذاری سرتاسری (E2EE)",
                    description = "فقط شما و مخاطب کلید رمزگشایی پیام‌ها را دارید",
                )
                SecurityPillarRow(
                    icon = Icons.Default.CloudOff,
                    title = "بدون ردپا روی سرورها",
                    description = "پیام‌ها پس از تحویل در هیچ سروری ذخیره نمی‌مانند",
                )
                SecurityPillarRow(
                    icon = Icons.Default.Timer,
                    title = "تایمر نابودی خودکار پیام‌ها",
                    description = "امکان تنظیم زمان محو خودکار پیام‌ها پس از رویت",
                )
                SecurityPillarRow(
                    icon = Icons.Default.Block,
                    title = "عدم امکان فوروارد پیام‌ها",
                    description = "بازنشر و فوروارد پیام‌ها در گفتگوی محرمانه مسدود است",
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = borderCol, thickness = 0.8.dp)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onSetTimer,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TelegramSecretGreen.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TelegramSecretGreen,
                    ),
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تنظیم تایمر", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onViewKey,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TelegramSecretGreen.copy(alpha = 0.18f),
                        contentColor = TelegramSecretGreen,
                    ),
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("کلید رمزنگاری", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SecurityPillarRow(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TelegramSecretBadgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TelegramSecretGreen,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
fun TelegramEncryptionKeyDialog(
    safetyNumber: String?,
    peerName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val emojis = deriveSecretKeyEmojis(safetyNumber)
    val matrix = deriveSecretKeyMatrix(safetyNumber)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TelegramSecretBadgeBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = TelegramSecretGreen,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "کلید رمزنگاری محرمانه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Text(
                            text = "تأیید هویت سرتاسری (E2EE)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TelegramSecretGreen,
                            fontSize = 11.sp,
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isLoading) {
                        Spacer(Modifier.height(24.dp))
                        CircularProgressIndicator(
                            color = TelegramSecretGreen,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "در حال محاسبه کلید امنیتی...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(24.dp))
                    } else if (!safetyNumber.isNullOrBlank()) {
                        // 1. Telegram Identicon Matrix (8x8 visual cryptographic grid)
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF141920) else Color(0xFFE8ECE9))
                                .border(1.dp, TelegramSecretGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                for (row in 0 until 8) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        for (col in 0 until 8) {
                                            // Symmetrical pattern like Telegram
                                            val symCol = if (col < 4) col else (7 - col)
                                            val colorIndex = matrix[(row * 4 + symCol) % matrix.size]
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(2.5.dp))
                                                    .background(TELEGRAM_MATRIX_PALETTE[colorIndex]),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // 2. 4 Telegram visual key emojis
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TelegramSecretGreen.copy(alpha = if (isDark) 0.15f else 0.10f),
                            border = BorderStroke(1.dp, TelegramSecretGreen.copy(alpha = 0.35f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                emojis.forEach { emoji ->
                                    Text(
                                        text = emoji,
                                        fontSize = 28.sp,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // 3. Formatted safety number digits
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = safetyNumber,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.8.sp,
                                    lineHeight = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // 4. Security assurance text
                        Text(
                            text = "اگر این تصویر، ۴ شکلک و ارقام امنیتی با آنچه روی صفحه «${peerName.ifBlank { "مخاطب" }}» نمایش داده می‌شود یکسان باشد، گفتگوی شما از رمزنگاری سرتاسری کامل بهره می‌برد و هیچ واسطه یا سروری قادر به استراق سمع نخواهد بود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Right,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                        )
                    } else {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "کلید امنیتی هنوز در دسترس نیست. پس از ارسال اولین پیام یا تأیید اتصال، کلید تولید می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            },
            confirmButton = {
                if (!safetyNumber.isNullOrBlank()) {
                    FilledTonalButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(safetyNumber))
                            Toast.makeText(context, "کد امنیتی کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = TelegramSecretGreen.copy(alpha = 0.2f),
                            contentColor = TelegramSecretGreen,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("کپی کلید", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("بستن", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

@Composable
fun TelegramAutoDeleteDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val options = listOf(
        0 to "خاموش",
        1 to "۱ ثانیه",
        2 to "۲ ثانیه",
        5 to "۵ ثانیه",
        10 to "۱۰ ثانیه",
        30 to "۳۰ ثانیه",
        60 to "۱ دقیقه",
        3600 to "۱ ساعت",
        86400 to "۱ روز",
        604800 to "۱ هفته",
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TelegramSecretBadgeBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = TelegramSecretGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("تایمر نابودی خودکار پیام‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = "پیام‌های جدید ارسال‌شده در این گفتگوی محرمانه پس از مدت مشخص‌شده، به صورت دوطرفه و خودکار حذف می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    options.forEach { (seconds, label) ->
                        val selected = currentSeconds == seconds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) TelegramSecretBadgeBg else Color.Transparent)
                                .clickable { onSelect(seconds) }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) TelegramSecretGreen else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                            )
                            RadioButton(
                                selected = selected,
                                onClick = { onSelect(seconds) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TelegramSecretGreen,
                                ),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}
