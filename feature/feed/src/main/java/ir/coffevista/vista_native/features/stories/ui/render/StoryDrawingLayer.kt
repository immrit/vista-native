package ir.coffevista.vista_native.features.stories.ui.render

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun StoryDrawingLayer(
    strokes: List<StoryStroke>,
    modifier: Modifier = Modifier,
    activeStroke: StoryStroke? = null,
) {
    if (strokes.isEmpty() && activeStroke == null) return
    Canvas(
        modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        strokes.forEach { drawStoryStroke(it) }
        activeStroke?.let { drawStoryStroke(it) }
    }
}

internal fun DrawScope.drawStoryStroke(stroke: StoryStroke) {
    if (stroke.points.isEmpty()) return
    val unit = size.width / 360f
    val points = stroke.points.map { Offset(it.first * size.width, it.second * size.height) }
    val base = Color(stroke.argb)
    val (color, widthPx, blend, cap) = when (stroke.brush) {
        StoryBrush.Pen -> BrushSpec(base, stroke.width * unit, BlendMode.SrcOver, StrokeCap.Round)
        StoryBrush.Marker -> BrushSpec(base.copy(alpha = base.alpha * 0.5f), stroke.width * unit * 1.8f, BlendMode.SrcOver, StrokeCap.Square)
        StoryBrush.Neon -> BrushSpec(base, stroke.width * unit, BlendMode.SrcOver, StrokeCap.Round)
        StoryBrush.Eraser -> BrushSpec(Color.Black, stroke.width * unit * 1.6f, BlendMode.Clear, StrokeCap.Round)
    }
    if (points.size == 1) {
        if (stroke.brush == StoryBrush.Neon) {
            drawCircle(color = color.copy(alpha = 0.25f), radius = widthPx * 1.45f, center = points[0], blendMode = blend)
            drawCircle(color = color.copy(alpha = 0.55f), radius = widthPx * 0.95f, center = points[0], blendMode = blend)
            drawCircle(color = Color.White, radius = widthPx * 0.42f, center = points[0], blendMode = blend)
        } else {
            drawCircle(color = color, radius = widthPx / 2f, center = points[0], blendMode = blend)
        }
        return
    }
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size - 1) {
            val mid = Offset((points[i].x + points[i + 1].x) / 2f, (points[i].y + points[i + 1].y) / 2f)
            quadraticBezierTo(points[i].x, points[i].y, mid.x, mid.y)
        }
        lineTo(points.last().x, points.last().y)
    }
    if (stroke.brush == StoryBrush.Neon) {
        // Multi-pass Telegram neon glow shader: outer halo, mid glow, and bright white core
        drawPath(
            path = path,
            color = color.copy(alpha = 0.22f),
            style = Stroke(width = widthPx * 2.6f, cap = cap, join = StrokeJoin.Round),
            blendMode = blend,
        )
        drawPath(
            path = path,
            color = color.copy(alpha = 0.55f),
            style = Stroke(width = widthPx * 1.55f, cap = cap, join = StrokeJoin.Round),
            blendMode = blend,
        )
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.95f),
            style = Stroke(width = widthPx * 0.58f, cap = cap, join = StrokeJoin.Round),
            blendMode = blend,
        )
    } else {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = widthPx, cap = cap, join = StrokeJoin.Round),
            blendMode = blend,
        )
    }
}

private data class BrushSpec(val color: Color, val width: Float, val blend: BlendMode, val cap: StrokeCap)
