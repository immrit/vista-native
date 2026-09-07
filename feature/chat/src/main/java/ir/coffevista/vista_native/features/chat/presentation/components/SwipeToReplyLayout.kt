package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import kotlin.math.roundToInt

@Composable
fun SwipeToReplyLayout(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bubbleOnRight: Boolean = false,
    onReply: () -> Unit,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val threshold = with(density) { 72.dp.toPx() }
    // Reply bubbles follow the app's chat gesture convention: peer bubbles
    // respond to a left swipe and our bubbles respond to a right swipe.
    val replyDirection = if (bubbleOnRight) 1f else -1f
    val swipePolicy = remember(threshold, bubbleOnRight) {
        ReplySwipePolicy(
            thresholdPx = threshold,
            touchSlopPx = with(density) { 8.dp.toPx() },
            bubbleOnRight = bubbleOnRight,
        )
    }
    var targetOffsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val offsetX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = if (isDragging) snap() else spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "replySwipeOffset",
    )
    var triggeredHaptic by remember { mutableStateOf(false) }

    DisposableEffect(enabled) {
        if (!enabled) {
            isDragging = false
            targetOffsetX = 0f
            triggeredHaptic = false
        }
        onDispose { }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.pointerInput(enabled, threshold, bubbleOnRight) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                // Do not enter the reply interaction until the drag has
                                // proved that it moves toward the conversation center.
                                // This leaves rejected diagonal/opposite drags visually idle.
                                isDragging = false
                                triggeredHaptic = false
                            },
                            onDragEnd = {
                                val shouldReply = isDragging && swipePolicy.shouldTrigger(targetOffsetX)
                                isDragging = false
                                targetOffsetX = 0f
                                if (shouldReply) onReply()
                            },
                            onDragCancel = {
                                isDragging = false
                                targetOffsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                // Compose has already arbitrated this as a horizontal gesture.
                                // Only capture a new gesture in the reply direction, but keep
                                // owning it while it springs back from a partial reply swipe.
                                if (swipePolicy.shouldConsumeDrag(targetOffsetX, dragAmount)) {
                                    isDragging = true
                                    change.consume()
                                    val newOffset = swipePolicy.nextOffset(targetOffsetX, dragAmount)
                                    targetOffsetX = newOffset
                                    if (swipePolicy.shouldTrigger(newOffset) && !triggeredHaptic) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        triggeredHaptic = true
                                    } else if (newOffset < threshold && triggeredHaptic) {
                                        triggeredHaptic = false
                                    }
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center
    ) {
        // Reply Icon Indicator Behind Bubble
        val progress = (offsetX / threshold).coerceIn(0f, 1f)
        if (progress > 0.05f) {
            Box(
                modifier = Modifier
                    .align(if (bubbleOnRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 16.dp)
                    .size(36.dp)
                    .scale(progress)
                    .clip(CircleShape)
                    .background(VistaBrandColors.VioletDeep)
                    .graphicsLayer {
                        rotationZ = (1f - progress) * 90f * replyDirection
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "پاسخ",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Swipable Message Bubble Container
        Box(
            modifier = Modifier
                .offset { IntOffset((offsetX * replyDirection).roundToInt(), 0) }
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
