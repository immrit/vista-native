package ir.coffevista.vista_native.features.chat.presentation.components

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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.features.chat.domain.model.DuelInviteDraft
import ir.coffevista.vista_native.features.chat.domain.model.DuelPreview

enum class DuelPreviewLoadStatus {
    IDLE,
    LOADING,
    LOADED,
    EXPIRED_OR_NOT_FOUND,
}

/**
 * Rich "Start Game" chat bubble for Vista Quiz duel invites and shared duel URLs.
 * Hydrates live match state, entry fee, and challenger info via [DuelPreview].
 */
@Composable
fun DuelInviteCard(
    invite: DuelInviteDraft,
    isMine: Boolean,
    preview: DuelPreview?,
    previewStatus: DuelPreviewLoadStatus,
    onRequestPreview: (String) -> Unit,
    onStartGame: (targetGamePath: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(invite.matchId) {
        onRequestPreview(invite.matchId)
    }

    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1F1B33) else Color(0xFFFAF8FF)
    val cardBorder = if (isDark) Color(0xFF4C3F7A) else Color(0xFFDDD6FE)
    val primaryText = if (isDark) Color(0xFFF5F3FF) else Color(0xFF312E81)
    val secondaryText = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
    val chipBg = if (isDark) Color(0xFF2B2447) else Color(0xFFF3EDFF)
    val chipBorder = if (isDark) Color(0xFF5B4B91) else Color(0xFFE9D5FF)

    val inviterName = preview?.player1?.name?.takeIf { it.isNotBlank() }
        ?: invite.inviterName?.takeIf { it.isNotBlank() }
        ?: "بازیکن ویستا"
    val inviterAvatar = preview?.player1?.avatarUrl?.takeIf { it.isNotBlank() }
        ?: invite.inviterAvatar
    val player2 = preview?.player2
    val entryFee = preview?.entryFee ?: invite.entryFee ?: 0L
    val winBonus = preview?.winCoinBonus ?: 0L

    val isLoading = previewStatus == DuelPreviewLoadStatus.LOADING || previewStatus == DuelPreviewLoadStatus.IDLE
    val isExpired = previewStatus == DuelPreviewLoadStatus.EXPIRED_OR_NOT_FOUND ||
        preview?.isEffectivelyExpired() == true
    val status = preview?.status.orEmpty()
    val isCancelled = status == "cancelled"
    val isFinished = status == "finished"
    val isStartedWithOpponent = player2 != null && !isFinished && !isCancelled && !isExpired
    val isInProgress = status == "in_progress" || isStartedWithOpponent
    val isWaiting = !isExpired && player2 == null && (status == "waiting" || status == "waiting_for_opponent")
    val isParticipant = preview?.isParticipant == true
    val canJoin = preview?.canJoin == true && !isExpired && player2 == null
    val remainingText = if (isWaiting) preview?.remainingText()?.toPersianDigits() else null

    val (badgeText, badgeBg, badgeFg) = when {
        isLoading && preview == null -> Triple("در حال بررسی...", Color.White.copy(alpha = 0.22f), Color.White)
        isExpired -> Triple("منقضی شده", Color(0xFF475569), Color.White)
        isCancelled -> Triple("لغو شده", Color(0xFFBE123C), Color.White)
        isFinished -> Triple("تمام شده", Color(0xFF4B5563), Color.White)
        isInProgress -> if (isParticipant) {
            Triple("نوبت / در جریان", Color(0xFF047857), Color.White)
        } else {
            Triple("شروع شد (۲ نفر)", Color(0xFFD97706), Color.White)
        }
        isWaiting -> if (isParticipant) {
            Triple("در انتظار حریف", Color(0xFF6D28D9), Color.White)
        } else {
            Triple("آماده شروع", Color(0xFF059669), Color.White)
        }
        else -> Triple("دعوت دوئل", Color.White.copy(alpha = 0.22f), Color.White)
    }

    val ctaEnabled = when {
        isExpired || isCancelled -> false
        isParticipant -> true
        canJoin -> true
        else -> false
    }
    val ctaLabel = when {
        isExpired -> "دعوت منقضی شده"
        isCancelled -> "دوئل لغو شده"
        isParticipant -> if (isWaiting) "ورود به اتاق انتظار" else "ادامه بازی"
        canJoin -> "شروع بازی"
        preview != null && isFinished -> "پایان دوئل (۲ نفر)"
        preview != null && (player2 != null || isInProgress) -> "ظرفیت تکمیل شد (۲ نفر)"
        else -> "شروع بازی"
    }

    val targetGamePath = when {
        preview != null && (isParticipant || !canJoin) -> "/game/match/${invite.matchId}"
        else -> invite.targetGamePath
    }

    val headerGradient = if (isExpired || isCancelled) {
        listOf(Color(0xFF64748B), Color(0xFF475569))
    } else {
        listOf(Color(0xFFEC4899), Color(0xFF7C3AED))
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .widthIn(min = 248.dp, max = 296.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                .semantics { contentDescription = "کارت دعوت به دوئل ویستا کوییز از طرف $inviterName" },
        ) {
            // Gradient Header Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = headerGradient,
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "دوئل ویستا کوییز",
                            color = Color.White,
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "رقابت ۶ راندی دوستانه",
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = VistaFontFamily,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = badgeText,
                        color = badgeFg,
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Challenger / Versus Section
                if (player2 != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF282242) else Color.White)
                            .border(0.8.dp, cardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(chipBg)
                                        .border(1.5.dp, Color(0xFFC4B5FD), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!inviterAvatar.isNullOrBlank()) {
                                        AsyncImage(
                                            model = inviterAvatar,
                                            contentDescription = inviterName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize().clip(CircleShape),
                                        )
                                    } else {
                                        Text(
                                            text = inviterName.take(1),
                                            color = primaryText,
                                            fontFamily = VistaFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = inviterName,
                                    color = primaryText,
                                    fontFamily = VistaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(chipBg)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "VS",
                                    color = secondaryText,
                                    fontFamily = VistaFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = player2.name.ifBlank { "حریف" },
                                    color = primaryText,
                                    fontFamily = VistaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF42213F) else Color(0xFFFCE7F3))
                                        .border(1.5.dp, Color(0xFFF9A8D4), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!player2.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = player2.avatarUrl,
                                            contentDescription = player2.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize().clip(CircleShape),
                                        )
                                    } else {
                                        Text(
                                            text = player2.name.take(1).ifBlank { "؟" },
                                            color = primaryText,
                                            fontFamily = VistaFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Group,
                                contentDescription = null,
                                tint = secondaryText,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (isFinished) {
                                    "مسابقه بین «$inviterName» و «${player2.name}» به پایان رسید"
                                } else {
                                    "بازی بین «$inviterName» و «${player2.name}» شروع شد"
                                },
                                color = secondaryText,
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(chipBg)
                                .border(1.5.dp, Color(0xFFC4B5FD), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!inviterAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = inviterAvatar,
                                    contentDescription = inviterName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize().clip(CircleShape),
                                )
                            } else {
                                Text(
                                    text = inviterName.take(1),
                                    color = primaryText,
                                    fontFamily = VistaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                )
                            }
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = inviterName,
                                color = primaryText,
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = when {
                                    isExpired -> preview?.cancelReason?.takeIf { it.isNotBlank() }
                                        ?: "مهلت پذیرش این دعوت دوئل به پایان رسید"
                                    isCancelled -> preview?.cancelReason?.takeIf { it.isNotBlank() }
                                        ?: "این دوئل لغو شده است"
                                    isMine -> "دعوت دوئل ارسال شد"
                                    else -> "شما را به دوئل دعوت کرده است"
                                },
                                color = secondaryText,
                                fontFamily = VistaFontFamily,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Entry fee + Reward row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(chipBg)
                            .border(0.7.dp, chipBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (entryFee > 0L) {
                                "ورودی: ${entryFee.toString().toPersianDigits()} سکه"
                            } else {
                                "ورودی: رایگان"
                            },
                            color = primaryText,
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF391D35) else Color(0xFFFDF2F8))
                            .border(
                                0.7.dp,
                                if (isDark) Color(0xFF7A3569) else Color(0xFFFBCFE8),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFDB2777),
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (winBonus > 0L) {
                                "+${winBonus.toString().toPersianDigits()} جایزه"
                            } else {
                                "جایزه سکه"
                            },
                            color = if (isDark) Color(0xFFF9A8D4) else Color(0xFF9D174D),
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (remainingText != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(chipBg)
                            .border(0.7.dp, chipBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "مهلت باقی‌مانده: $remainingText",
                            color = secondaryText,
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }

                if (!invite.captionText.isNullOrBlank()) {
                    Text(
                        text = invite.captionText,
                        color = primaryText,
                        fontFamily = VistaFontFamily,
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Action CTA Button
                val buttonBrush = when {
                    !ctaEnabled -> Brush.horizontalGradient(
                        listOf(Color(0xFF9CA3AF), Color(0xFF6B7280)),
                    )
                    isParticipant && !canJoin -> Brush.horizontalGradient(
                        listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)),
                    )
                    else -> Brush.horizontalGradient(
                        listOf(Color(0xFF10B981), Color(0xFF059669)),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(buttonBrush)
                        .then(
                            if (ctaEnabled) {
                                Modifier.clickable(role = Role.Button) { onStartGame(targetGamePath) }
                            } else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (isLoading && preview == null) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(15.dp),
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    isExpired || isCancelled -> Icons.Rounded.Close
                                    !ctaEnabled -> Icons.Rounded.Lock
                                    else -> Icons.Rounded.PlayArrow
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = ctaLabel,
                            color = Color.White,
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }

                if (isExpired && isMine) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF282242) else Color.White)
                            .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { onStartGame("/game/duel/create") },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "ساخت دعوت جدید",
                            color = secondaryText,
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}
