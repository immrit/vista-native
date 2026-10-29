package ir.coffevista.vista_native.features.stories.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import ir.coffevista.vista_native.features.stories.ui.canvas.SnapGuides

@Composable
internal fun StorySnapGuidesOverlay(
    guides: SnapGuides,
    modifier: Modifier = Modifier,
) {
    if (guides.vertical.isEmpty() && guides.horizontal.isEmpty() && !guides.rotationLocked) return

    val guideColor = Color(0xFFFFD600)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

    Canvas(modifier = modifier.fillMaxSize()) {
        guides.vertical.forEach { x ->
            drawLine(
                color = guideColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2f,
                pathEffect = dashEffect,
            )
        }

        guides.horizontal.forEach { y ->
            drawLine(
                color = guideColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f,
                pathEffect = dashEffect,
            )
        }
    }
}
