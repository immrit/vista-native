package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.component.VistaEmoji
import kotlinx.coroutines.launch

/**
 * Detects whether a message consists strictly of a single emoji with no other content.
 */
object SingleEmojiDetector {
    fun extractSingleEmoji(
        text: String,
        hasAttachment: Boolean,
        isForwarded: Boolean,
        hasReply: Boolean,
        hasSharedPost: Boolean,
        isDeleted: Boolean,
    ): String? {
        if (isDeleted || hasAttachment || isForwarded || hasReply || hasSharedPost) {
            return null
        }
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        var baseEmojiCount = 0
        var i = 0
        val len = trimmed.length

        while (i < len) {
            val cp = trimmed.codePointAt(i)
            val charCount = Character.charCount(cp)

            when {
                cp == 0x200D || cp == 0xFE0F || cp == 0xFE0E || (cp in 0x1F3FB..0x1F3FF) -> {
                    // Variation selector, skin tone modifier, or Zero Width Joiner
                }
                isEmojiCodePoint(cp) -> {
                    if (i > 0) {
                        val prevCp = Character.codePointBefore(trimmed, i)
                        if (prevCp != 0x200D) {
                            baseEmojiCount++
                        }
                    } else {
                        baseEmojiCount++
                    }
                }
                else -> {
                    // Non-emoji character encountered!
                    return null
                }
            }
            i += charCount
        }

        return if (baseEmojiCount == 1) trimmed else null
    }

    private fun isEmojiCodePoint(cp: Int): Boolean {
        return (cp in 0x1F000..0x1FAFF) || // Supplementary emoji blocks (Unicode 6.0 through 16.0)
            (cp in 0x2600..0x27BF) ||     // Miscellaneous Symbols, Dingbats
            (cp in 0x2300..0x23FF) ||     // Miscellaneous Technical
            (cp in 0x2B50..0x2B55) ||     // Stars and symbols
            (cp == 0x2934 || cp == 0x2935) ||
            (cp in 0x25AA..0x25FE)
    }
}

private enum class AnimatedEmojiKind {
    HEARTBEAT,
    LAUGH_BOUNCE,
    FLAME_FLICKER,
    THUMBS_SPRING,
    PARTY_WOBBLE,
    ROMANCE_FLOAT,
    JELLY_JIGGLE,
    IDLE_LIVING,
}

private fun resolveEmojiKind(emoji: String): AnimatedEmojiKind {
    return when {
        emoji in setOf(
            "❤️", "🩷", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎",
            "💔", "❤️‍🔥", "❤️‍🩹", "💖", "💗", "💓", "💞", "💕", "❣️", "💘", "💝",
        ) -> AnimatedEmojiKind.HEARTBEAT

        emoji in setOf("😂", "🤣", "😆", "😹") -> AnimatedEmojiKind.LAUGH_BOUNCE

        emoji in setOf("🔥", "💥", "⚡") -> AnimatedEmojiKind.FLAME_FLICKER

        emoji in setOf("👍", "👎", "👏", "🙌", "🤝", "👌", "👊") -> AnimatedEmojiKind.THUMBS_SPRING

        emoji in setOf("🎉", "🥳", "✨", "⭐", "🌟", "🎈") -> AnimatedEmojiKind.PARTY_WOBBLE

        emoji in setOf("😍", "🥰", "😘", "😻", "😚", "😙") -> AnimatedEmojiKind.ROMANCE_FLOAT

        emoji in setOf("💩", "👻", "🤪", "😜", "😝", "🫠") -> AnimatedEmojiKind.JELLY_JIGGLE

        else -> AnimatedEmojiKind.IDLE_LIVING
    }
}

/**
 * Large standalone emoji. Emoji that ship a real frame-by-frame animation
 * ([AnimatedEmojiCatalog]) are rendered with Lottie so the emoji artwork itself
 * moves; every other emoji falls back to the physics-based effect animation.
 */
@Composable
fun VistaAnimatedEmojiBubble(
    emoji: String,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    emojiSize: Dp = 96.dp,
    metaFooter: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val assetPath = remember(emoji) { AnimatedEmojiCatalog.assetPathFor(context, emoji) }
    if (assetPath != null) {
        LottieEmojiBubble(
            emoji = emoji,
            assetPath = assetPath,
            isMine = isMine,
            modifier = modifier,
            emojiSize = emojiSize,
            metaFooter = metaFooter,
        )
    } else {
        EffectEmojiBubble(
            emoji = emoji,
            isMine = isMine,
            modifier = modifier,
            emojiSize = emojiSize,
            metaFooter = metaFooter,
        )
    }
}

@Composable
private fun LottieEmojiBubble(
    emoji: String,
    assetPath: String,
    isMine: Boolean,
    modifier: Modifier,
    emojiSize: Dp,
    metaFooter: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    var playToken by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }

    // Plays once when the message appears and again on every tap.
    LaunchedEffect(composition, playToken) {
        val loaded = composition ?: return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = loaded.duration.toInt().coerceIn(600, 6000),
                easing = LinearEasing,
            ),
        )
    }

    Box(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(emojiSize)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playToken++
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (composition == null) {
                VistaEmoji(emoji = emoji, size = emojiSize * 0.8f)
            } else {
                LottieAnimation(
                    composition = composition,
                    progress = { progress.value },
                    modifier = Modifier.size(emojiSize),
                )
            }
        }

        Box(
            modifier = Modifier
                .align(if (isMine) Alignment.BottomEnd else Alignment.BottomStart)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides Color.White,
                LocalTextStyle provides TextStyle(color = Color.White),
            ) {
                metaFooter()
            }
        }
    }
}

/**
 * Fallback for emoji without a bundled animation: physics-based continuous loop
 * animations, tactile tap burst reaction, and floating timestamp badge.
 */
@Composable
private fun EffectEmojiBubble(
    emoji: String,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    emojiSize: Dp = 76.dp,
    metaFooter: @Composable () -> Unit,
) {
    val kind = remember(emoji) { resolveEmojiKind(emoji) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val tapScale = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "animated_emoji_loop")

    // Scale animation
    val animScaleX by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = when (kind) {
            AnimatedEmojiKind.HEARTBEAT -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1250
                    1.00f at 0 using FastOutSlowInEasing
                    1.26f at 160 using FastOutSlowInEasing // Primary systolic thump
                    1.06f at 280 using FastOutSlowInEasing
                    1.18f at 400 using FastOutSlowInEasing // Secondary diastolic thump
                    1.00f at 560 using FastOutSlowInEasing
                    1.00f at 1250
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.FLAME_FLICKER -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 750
                    1.00f at 0
                    0.92f at 180
                    1.08f at 380
                    0.95f at 560
                    1.00f at 750
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.THUMBS_SPRING -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1500
                    1.00f at 0
                    1.20f at 220
                    0.96f at 440
                    1.08f at 660
                    1.00f at 880
                    1.00f at 1500
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.PARTY_WOBBLE -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    1.00f at 0
                    1.16f at 200
                    1.04f at 400
                    1.12f at 600
                    1.00f at 850
                    1.00f at 1200
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.JELLY_JIGGLE -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 950
                    1.00f at 0
                    1.18f at 240
                    0.86f at 520
                    1.08f at 740
                    1.00f at 950
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.ROMANCE_FLOAT -> infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            )
            AnimatedEmojiKind.LAUGH_BOUNCE -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 900
                    1.00f at 0
                    1.08f at 220
                    1.00f at 450
                    1.08f at 680
                    1.00f at 900
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.IDLE_LIVING -> infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "emoji_scale_x",
    )

    val animScaleY by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = when (kind) {
            AnimatedEmojiKind.HEARTBEAT -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1250
                    1.00f at 0
                    1.26f at 160
                    1.06f at 280
                    1.18f at 400
                    1.00f at 560
                    1.00f at 1250
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.FLAME_FLICKER -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 750
                    1.00f at 0
                    1.20f at 180
                    0.94f at 380
                    1.14f at 560
                    1.00f at 750
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.JELLY_JIGGLE -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 950
                    1.00f at 0
                    0.86f at 240
                    1.18f at 520
                    0.94f at 740
                    1.00f at 950
                },
                repeatMode = RepeatMode.Restart,
            )
            else -> infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "emoji_scale_y",
    )

    // Rotation / Tilt animation
    val animRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = when (kind) {
            AnimatedEmojiKind.LAUGH_BOUNCE -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 900
                    0f at 0
                    -8f at 220
                    0f at 450
                    8f at 680
                    0f at 900
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.PARTY_WOBBLE -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    0f at 0
                    -14f at 200
                    12f at 400
                    -8f at 600
                    4f at 800
                    0f at 1000
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.ROMANCE_FLOAT -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1800
                    -3.5f at 0
                    3.5f at 900
                    -3.5f at 1800
                },
                repeatMode = RepeatMode.Restart,
            )
            else -> infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Restart)
        },
        label = "emoji_rotation",
    )

    // Vertical bounce animation
    val animTranslationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = when (kind) {
            AnimatedEmojiKind.LAUGH_BOUNCE -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 900
                    0f at 0
                    -26f at 220
                    0f at 450
                    -22f at 680
                    0f at 900
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.FLAME_FLICKER -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 750
                    0f at 0
                    -12f at 180
                    0f at 380
                    -8f at 560
                    0f at 750
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.ROMANCE_FLOAT -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1800
                    0f at 0
                    -14f at 900
                    0f at 1800
                },
                repeatMode = RepeatMode.Restart,
            )
            AnimatedEmojiKind.IDLE_LIVING -> infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2200
                    0f at 0
                    -8f at 1100
                    0f at 2200
                },
                repeatMode = RepeatMode.Restart,
            )
            else -> infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Restart)
        },
        label = "emoji_trans_y",
    )

    // Optional glow pulse for hearts and fire
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (kind == AnimatedEmojiKind.HEARTBEAT || kind == AnimatedEmojiKind.FLAME_FLICKER) 0.35f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "emoji_glow",
    )

    val finalScaleX = animScaleX * tapScale.value
    val finalScaleY = animScaleY * tapScale.value

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Main animated emoji with tap burst reaction
        Box(
            modifier = Modifier
                .size(emojiSize + 18.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            tapScale.animateTo(
                                targetValue = 1.32f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                            )
                            tapScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    },
                )
                .drawBehind {
                    if (glowAlpha > 0.05f) {
                        val glowColor = if (kind == AnimatedEmojiKind.HEARTBEAT) {
                            Color(0xFFFF2D55).copy(alpha = glowAlpha)
                        } else {
                            Color(0xFFFF9500).copy(alpha = glowAlpha)
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor, Color.Transparent),
                                radius = size.minDimension * 0.65f,
                            ),
                            radius = size.minDimension * 0.65f,
                        )
                    }
                }
                .graphicsLayer {
                    scaleX = finalScaleX
                    scaleY = finalScaleY
                    rotationZ = animRotation
                    translationY = animTranslationY
                },
            contentAlignment = Alignment.Center,
        ) {
            VistaEmoji(
                emoji = emoji,
                size = emojiSize,
            )
        }

        // Floating semi-transparent corner metadata capsule
        Box(
            modifier = Modifier
                .align(if (isMine) Alignment.BottomEnd else Alignment.BottomStart)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides Color.White,
                LocalTextStyle provides TextStyle(color = Color.White),
            ) {
                metaFooter()
            }
        }
    }
}
