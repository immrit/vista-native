package ir.coffevista.vista_native.features.chat.presentation.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.features.chat.domain.model.SessionAlertPayload
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Vista Service new-login alert card with Telegram-style frosted glass
 * Approve / Immediate Reject inline buttons directly below the bubble.
 */
@Composable
fun SessionAlertCard(
    alert: SessionAlertPayload,
    isActionPending: Boolean,
    onAction: (action: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val status = alert.status.lowercase()
    val isPending = status == "pending"
    val isApproved = status == "approved"
    val isRejected = status == "rejected"

    val cardBg = if (isDark) Color(0xFF13131E) else Color(0xFFF8F9FF)
    val cardBorder = if (isDark) Color(0xFF2A2A45) else Color(0xFFE5E7EB)
    val titleColor = if (isDark) Color.White else Color(0xFF101828)
    val subtitleColor = if (isDark) Color(0xFF8B8BAD) else Color(0xFF6B7280)

    val accentColor = when {
        isApproved -> Color(0xFF22C55E)
        isRejected -> Color(0xFFEF4444)
        else -> Color(0xFF6366F1)
    }

    val isDesktopOrWeb = alert.platform.lowercase().let {
        it.contains("web") || it.contains("desktop") || it.contains("windows") || it.contains("mac") || it.contains("linux")
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier.widthIn(min = 250.dp, max = 310.dp),
        ) {
            // Main Alert Bubble Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when {
                                isApproved -> Icons.Rounded.CheckCircle
                                isRejected -> Icons.Rounded.Block
                                isDesktopOrWeb -> Icons.Rounded.Computer
                                else -> Icons.Rounded.PhoneAndroid
                            },
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isApproved -> "ورود تایید شد"
                                isRejected -> "نشست فوراً مسدود شد"
                                else -> "هشدار ورود جدید به حساب"
                            },
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = titleColor,
                        )
                        Text(
                            text = "سرویس امنیتی ویستا",
                            fontFamily = VistaFontFamily,
                            fontSize = 11.5.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Text(
                    text = when {
                        isApproved -> "این ورود به عنوان دستگاه معتبر شما تایید شد."
                        isRejected -> "این نشست باطل شد و دسترسی دستگاه مورد نظر بدون درنگ قطع گردید."
                        else -> "دستگاه جدیدی وارد حساب کاربری شما شد. اگر خودتان بودید تایید کنید، در غیر این صورت فوراً رد و خارج کنید:"
                    },
                    fontFamily = VistaFontFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = subtitleColor,
                )

                // Structured metadata box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AlertMetaRow(
                        icon = Icons.Rounded.Security,
                        label = "دستگاه",
                        value = alert.deviceName.ifBlank { "دستگاه ناشناس" },
                        labelColor = subtitleColor,
                        valueColor = titleColor,
                    )
                    val sysInfo = listOfNotNull(
                        alert.platform.takeIf { it.isNotBlank() },
                        alert.osVersion.takeIf { it.isNotBlank() },
                        alert.appVersion.takeIf { it.isNotBlank() }?.let { "v$it" },
                    ).joinToString(" • ")
                    if (sysInfo.isNotBlank()) {
                        AlertMetaRow(
                            icon = Icons.Rounded.Public,
                            label = "پلتفرم",
                            value = sysInfo.toPersianDigits(),
                            labelColor = subtitleColor,
                            valueColor = titleColor,
                        )
                    }
                    if (alert.location.isNotBlank()) {
                        AlertMetaRow(
                            icon = Icons.Rounded.LocationOn,
                            label = "موقعیت",
                            value = alert.location,
                            labelColor = subtitleColor,
                            valueColor = titleColor,
                        )
                    }
                    if (alert.ipAddress.isNotBlank()) {
                        AlertMetaRow(
                            icon = Icons.Rounded.Public,
                            label = "IP",
                            value = alert.ipAddress.toPersianDigits(),
                            labelColor = subtitleColor,
                            valueColor = titleColor,
                        )
                    }
                    if (alert.createdAt.isNotBlank()) {
                        AlertMetaRow(
                            icon = Icons.Rounded.Schedule,
                            label = "زمان",
                            value = formatAlertTime(alert.createdAt).toPersianDigits(),
                            labelColor = subtitleColor,
                            valueColor = titleColor,
                        )
                    }
                }
            }

            // Inline Frosted Glass Action Buttons (Telegram Bot Inline Keyboard Style)
            Spacer(Modifier.height(6.dp))
            if (isPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FrostedInlineActionButton(
                        text = "تایید می‌کنم",
                        icon = Icons.Rounded.CheckCircle,
                        accent = Color(0xFF22C55E),
                        isDark = isDark,
                        enabled = !isActionPending,
                        isLoading = isActionPending,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction("approve") },
                    )
                    FrostedInlineActionButton(
                        text = "خروج فوری",
                        icon = Icons.Rounded.Close,
                        accent = Color(0xFFEF4444),
                        isDark = isDark,
                        enabled = !isActionPending,
                        isLoading = isActionPending,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction("reject") },
                    )
                }
            } else {
                // Status pill badge matching frosted glass button geometry
                val badgeBg = if (isDark) {
                    accentColor.copy(alpha = 0.14f)
                } else {
                    accentColor.copy(alpha = 0.10f)
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isApproved) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isApproved) "تایید شده توسط شما" else "مسدود و باطل شده",
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = accentColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostedInlineActionButton(
    text: String,
    icon: ImageVector,
    accent: Color,
    isDark: Boolean,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glassBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                accent.copy(alpha = 0.24f),
                accent.copy(alpha = 0.14f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                accent.copy(alpha = 0.16f),
                accent.copy(alpha = 0.10f),
            ),
        )
    }
    val borderColor = if (isDark) {
        accent.copy(alpha = 0.36f)
    } else {
        accent.copy(alpha = 0.28f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(glassBrush)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = accent,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = text,
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = if (isDark) Color.White else accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AlertMetaRow(
    icon: ImageVector,
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = labelColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "$label:",
            fontFamily = VistaFontFamily,
            fontSize = 11.5.sp,
            color = labelColor,
        )
        Text(
            text = value,
            fontFamily = VistaFontFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatAlertTime(iso: String): String {
    return runCatching {
        val instant = Instant.parse(iso)
        val local = instant.atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.forLanguageTag("fa-IR")).format(local)
    }.getOrDefault(iso)
}
