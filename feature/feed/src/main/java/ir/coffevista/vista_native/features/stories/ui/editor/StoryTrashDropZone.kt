package ir.coffevista.vista_native.features.stories.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun BoxScope.StoryTrashDropZone(
    isVisible: Boolean,
    isHovered: Boolean,
    proximity: Float = if (isHovered) 1f else 0f,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 40.dp),
    ) {
        val scale by animateFloatAsState(
            targetValue = if (isHovered) 1.32f else (1f + 0.12f * proximity.coerceIn(0f, 1f)),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "trash_scale",
        )
        val lidAngle by animateFloatAsState(
            targetValue = if (isHovered) -32f else (-14f * proximity.coerceIn(0f, 1f)),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "trash_lid_angle",
        )
        val bgColor = if (isHovered) Color(0xFFE53935) else Color(0xB818181C)
        val borderColor = if (isHovered) Color.White else Color.White.copy(alpha = 0.35f)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isHovered) {
                Text(
                    text = "رها کنید تا حذف شود",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
            }
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(2.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // Custom animated trash bin with opening lid (Telegram EntityView trash icon parity)
                Canvas(modifier = Modifier.size(26.dp)) {
                    val w = size.width
                    val h = size.height
                    val strokeW = 2.1.dp.toPx()

                    // Animated Trash Lid
                    rotate(degrees = lidAngle, pivot = Offset(w * 0.2f, h * 0.26f)) {
                        drawLine(
                            color = Color.White,
                            start = Offset(w * 0.14f, h * 0.25f),
                            end = Offset(w * 0.86f, h * 0.25f),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round,
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(w * 0.38f, h * 0.13f),
                            size = Size(w * 0.24f, h * 0.12f),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                            style = Stroke(width = strokeW * 0.85f),
                        )
                    }

                    // Trash Body
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.22f, h * 0.32f),
                        size = Size(w * 0.56f, h * 0.56f),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                        style = Stroke(width = strokeW),
                    )
                    // Inner vertical ribs
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.40f, h * 0.44f),
                        end = Offset(w * 0.40f, h * 0.75f),
                        strokeWidth = strokeW * 0.8f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.60f, h * 0.44f),
                        end = Offset(w * 0.60f, h * 0.75f),
                        strokeWidth = strokeW * 0.8f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
