package ir.coffevista.vista_native.features.chat.presentation.audio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.component.formatAudioTime
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.chat.presentation.components.formatSpeedLabel
import ir.coffevista.vista_native.features.chat.presentation.toPersianDigits

/**
 * مینی‌پلیر تلگرامی زیر اپ‌بار چت (الگوبرداری از `FragmentContextView` تلگرام)
 * با افکت شیشه‌ای لیکویید گلس ویستا (`vistaGlass`):
 * - نمایش عنوان، خواننده/فرستنده و زمان پخش زنده برای موزیک و پیام صوتی
 * - کنترل Play/Pause انیمیشنی، تغییر سرعت پخش (لمس برای چرخش، لمس طولانی برای منو سرعت)، ترک بعدی و بستن
 * - نوار پیشرفت باریک در لبه پایینی و باز کردن پلیر تمام‌صفحه/باتم‌شیت موزیک با لمس نوار
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMiniAudioPlayer(
    glassState: VistaGlassState?,
    onOpenFullPlayer: () -> Unit,
    onJumpToMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerState by ChatAudioPlayer.state.collectAsState()
    val positionMs by ChatAudioPlayer.position.collectAsState()
    val track = playerState.track
    val isDark = isSystemInDarkTheme()
    var speedMenuExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = track != null,
        enter = expandVertically(tween(220)) + fadeIn(tween(180)),
        exit = shrinkVertically(tween(180)) + fadeOut(tween(140)),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (track == null) return@AnimatedVisibility

        val tintColor = if (isDark) Color(0xFF1C242F) else Color(0xFFFFFFFF)
        val glassModifier = if (glassState != null) {
            Modifier.vistaGlass(
                state = glassState,
                shape = RectangleShape,
                tint = tintColor,
                isDark = isDark,
                blurRadius = VistaGlassTokens.BarBlur,
                tintAlphaOverride = if (isDark) 0.78f else 0.84f,
                drawHighlight = false,
                rimWidth = 0.dp,
            )
        } else {
            Modifier.background(tintColor.copy(alpha = 0.94f))
        }

        val durationMs = playerState.durationMs.takeIf { it > 0L } ?: track.durationMs
        val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        val accentColor = if (isDark) Color(0xFF7CA4FF) else VistaBrandColors.Indigo

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(glassModifier)
                .testTag("chat-mini-audio-player"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (playerState.isPlaying) ChatAudioPlayer.pause() else ChatAudioPlayer.resume()
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    if (playerState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accentColor,
                        )
                    } else {
                        AnimatedContent(
                            targetState = playerState.isPlaying,
                            transitionSpec = {
                                (fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.75f))
                                    .togetherWith(fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.75f))
                            },
                            label = "mini-player-play-pause",
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "مکث" else "پخش",
                                tint = accentColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                // بخش مرکزی: با لمس برای موزیک پلیر کامل باز می‌شود و برای وویس به پیام اسکرول می‌کند
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = {
                                if (track.kind == ChatAudioKind.MUSIC) {
                                    onOpenFullPlayer()
                                } else {
                                    onJumpToMessage(track.id)
                                }
                            },
                        )
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    AnimatedContent(
                        targetState = track.id,
                        transitionSpec = {
                            (slideInVertically { it / 2 } + fadeIn(tween(180)))
                                .togetherWith(slideOutVertically { -it / 2 } + fadeOut(tween(140)))
                        },
                        label = "mini-player-track-switch",
                    ) { _ ->
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = if (track.kind == ChatAudioKind.MUSIC) track.title else track.senderName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val timeStr = if (durationMs > 0L) {
                                "${formatAudioTime(positionMs)} / ${formatAudioTime(durationMs)}"
                            } else {
                                formatAudioTime(positionMs)
                            }
                            val sub = when (track.kind) {
                                ChatAudioKind.MUSIC -> listOfNotNull(
                                    track.artist?.takeIf(String::isNotBlank),
                                    timeStr.toPersianDigits(),
                                ).joinToString(" • ")
                                ChatAudioKind.VOICE -> "پیام صوتی • ${timeStr.toPersianDigits()}"
                            }
                            Text(
                                text = sub,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // دکمه سرعت پخش (تغییر سریع با لمس، منو سرعت‌های دقیق با نگه‌داشتن طولانی مثل تلگرام)
                val showSpeedButton = track.kind == ChatAudioKind.VOICE || durationMs >= 10 * 60 * 1000L
                if (showSpeedButton) {
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.14f))
                                .combinedClickable(
                                    role = Role.Button,
                                    onClick = { ChatAudioPlayer.cycleSpeed() },
                                    onLongClick = { speedMenuExpanded = true },
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                .semantics { contentDescription = "تغییر سرعت پخش" },
                        ) {
                            Text(
                                text = formatSpeedLabel(playerState.speed),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                            )
                        }
                        DropdownMenu(
                            expanded = speedMenuExpanded,
                            onDismissRequest = { speedMenuExpanded = false },
                        ) {
                            ChatAudioPlayer.SPEED_CHOICES.forEach { speedOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${speedOption}×",
                                            fontWeight = if (kotlin.math.abs(playerState.speed - speedOption) < 0.05f) {
                                                FontWeight.Bold
                                            } else FontWeight.Normal,
                                            color = if (kotlin.math.abs(playerState.speed - speedOption) < 0.05f) {
                                                accentColor
                                            } else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        ChatAudioPlayer.setSpeed(speedOption)
                                        speedMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(2.dp))
                }

                if (track.kind == ChatAudioKind.MUSIC && playerState.queue.size > 1) {
                    IconButton(
                        onClick = { ChatAudioPlayer.next() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "آهنگ بعدی",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                IconButton(
                    onClick = { ChatAudioPlayer.stop() },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن پخش‌کننده",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // نوار پیشرفت زنده در لبه پایینی مینی‌پلیر (جهت چپ به راست مانند تلگرام)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        ),
                ) {
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep),
                                    ),
                                ),
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = if (isDark) Color(0x18FFFFFF) else Color(0x12000000),
            )
        }
    }
}
