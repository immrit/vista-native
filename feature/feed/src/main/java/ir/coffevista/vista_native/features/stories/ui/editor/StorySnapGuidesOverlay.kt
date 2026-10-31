package ir.coffevista.vista_native.features.stories.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import ir.coffevista.vista_native.features.stories.ui.canvas.SnapGuides
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryStageSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
internal fun StorySnapGuidesOverlay(
    guides: SnapGuides,
    modifier: Modifier = Modifier,
) {
    if (guides.vertical.isEmpty() &&
        guides.horizontal.isEmpty() &&
        !guides.rotationLocked &&
        !guides.marginLeftLocked &&
        !guides.marginRightLocked &&
        !guides.marginTopLocked &&
        !guides.marginBottomLocked
    ) return

    val centerGuideColor = Color(0xFF29B6F6) // Telegram cyan alignment line
    val angleGuideColor = Color(0xFFFFD600)
    val safeZoneColor = Color(0x75FFFFFF)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val unit = size.width / StoryStageSpec.RefWidth
        val sideMarginPx = StoryStageSpec.SideMargin * unit
        val topSafePx = StoryStageSpec.TopSafeFraction * size.height
        val bottomSafePx = StoryStageSpec.BottomSafeFraction * size.height

        // 1. Safe-zone boundary frame when any margin snap is engaged
        if (guides.marginLeftLocked || guides.marginRightLocked || guides.marginTopLocked || guides.marginBottomLocked) {
            drawRect(
                color = safeZoneColor,
                topLeft = Offset(sideMarginPx, topSafePx),
                size = Size(
                    width = (size.width - 2f * sideMarginPx).coerceAtLeast(1f),
                    height = (size.height - topSafePx - bottomSafePx).coerceAtLeast(1f),
                ),
                style = Stroke(width = 1.5f, pathEffect = dashEffect),
            )
        }

        // 2. Vertical Center / Margin Snap Lines
        guides.vertical.forEach { x ->
            val isCenter = kotlin.math.abs(x - size.width * 0.5f) < 2f
            drawLine(
                color = if (isCenter) centerGuideColor else safeZoneColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = if (isCenter) 2.2f else 1.5f,
                pathEffect = if (isCenter) null else dashEffect,
            )
        }

        // 3. Horizontal Center / Margin Snap Lines
        guides.horizontal.forEach { y ->
            val isCenter = kotlin.math.abs(y - size.height * 0.5f) < 2f
            drawLine(
                color = if (isCenter) centerGuideColor else safeZoneColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = if (isCenter) 2.2f else 1.5f,
                pathEffect = if (isCenter) null else dashEffect,
            )
        }

        // 4. Angular Snap Axis Ray through entity center (0, 45, 90, 180, 270 deg)
        if (guides.rotationLocked) {
            val (cx, cy) = guides.snappedCenterPx ?: (size.width * 0.5f to size.height * 0.5f)
            val deg = guides.snappedRotationDeg ?: 0f
            val rad = deg * (PI.toFloat() / 180f)
            val len = max(size.width, size.height)
            val cosA = cos(rad)
            val sinA = sin(rad)
            drawLine(
                color = angleGuideColor,
                start = Offset(cx - cosA * len, cy - sinA * len),
                end = Offset(cx + cosA * len, cy + sinA * len),
                strokeWidth = 2f,
                pathEffect = dashEffect,
            )
        }
    }
}
