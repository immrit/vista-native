package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeToReplyLayout(
    modifier: Modifier = Modifier,
    onReply: () -> Unit,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var triggeredHaptic by remember { mutableStateOf(false) }

    val threshold = 72f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { triggeredHaptic = false },
                    onDragEnd = {
                        if (abs(offsetX.value) >= threshold) onReply()
                        coroutineScope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
                    },
                    onDragCancel = { coroutineScope.launch { offsetX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f)) } },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (offsetX.value + dragAmount * 0.6f).coerceIn(-120f, 120f)
                        coroutineScope.launch { offsetX.snapTo(newOffset) }
                        if (abs(newOffset) >= threshold && !triggeredHaptic) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            triggeredHaptic = true
                        } else if (abs(newOffset) < threshold && triggeredHaptic) triggeredHaptic = false
                    },
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Reply Icon Indicator Behind Bubble
        val progress = (abs(offsetX.value) / threshold).coerceIn(0f, 1f)
        if (progress > 0.05f) {
            val isLeft = offsetX.value < 0
            Box(
                modifier = Modifier
                    .align(if (isLeft) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 16.dp)
                    .size(36.dp)
                    .scale(progress)
                    .clip(CircleShape)
                    .background(VistaBrandColors.VioletDeep)
                    .graphicsLayer {
                        rotationZ = if (isLeft) (1f - progress) * -90f else (1f - progress) * 90f
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
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
