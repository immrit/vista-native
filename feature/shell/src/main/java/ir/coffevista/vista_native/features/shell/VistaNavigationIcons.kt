package ir.coffevista.vista_native.features.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

@Composable
internal fun VistaNavigationIcon(
    tab: ShellTab,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "vistaNavScale",
    )
    val pathData = remember(tab, selected) {
        when (tab) {
            ShellTab.Feed -> if (selected) HOME_FILLED else HOME_OUTLINE
            ShellTab.Search -> SEARCH
            ShellTab.Services -> null
            ShellTab.Chat -> if (selected) CHAT_FILLED else CHAT_OUTLINE
            ShellTab.Profile -> if (selected) PROFILE_FILLED else PROFILE_OUTLINE
        }
    }
    val path = remember(pathData) {
        pathData?.let { PathParser().parsePathString(it).toPath() }
    }
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val iconScale = unit * scale
        val offset = Offset(
            (size.width - 24f * iconScale) / 2f,
            (size.height - 24f * iconScale) / 2f,
        )
        withTransform({
            translate(offset.x, offset.y)
            scale(iconScale, iconScale, pivot = Offset.Zero)
        }) {
            when (tab) {
                ShellTab.Services -> {
                    val stroke = Stroke(
                        width = 1.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                    listOf(
                        Offset(3f, 3f),
                        Offset(13f, 3f),
                        Offset(3f, 13f),
                        Offset(13f, 13f),
                    ).forEach { topLeft ->
                        drawRoundRect(
                            color = color,
                            topLeft = topLeft,
                            size = Size(8f, 8f),
                            cornerRadius = CornerRadius(2f, 2f),
                            style = stroke,
                        )
                    }
                }
                ShellTab.Feed -> {
                    if (selected) drawPath(path!!, color) else {
                        drawPath(
                            path!!,
                            color,
                            style = Stroke(
                                width = 2.1f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
                ShellTab.Search -> drawPath(
                    path!!,
                    color,
                    style = Stroke(
                        width = 2.1f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                ShellTab.Chat -> {
                    if (selected) drawPath(path!!, color) else {
                        drawPath(
                            path!!,
                            color,
                            style = Stroke(
                                width = 2.1f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                    listOf(9f, 12f, 15f).forEach { x ->
                        drawCircle(color, radius = 1.05f, center = Offset(x, 10.75f))
                    }
                }
                ShellTab.Profile -> {
                    if (selected) {
                        drawCircle(color, radius = 4.15f, center = Offset(12f, 7.5f))
                        drawPath(path!!, color)
                    } else {
                        drawCircle(
                            color,
                            radius = 3.75f,
                            center = Offset(12f, 7.5f),
                            style = Stroke(width = 2.1f),
                        )
                        drawPath(
                            path!!,
                            color,
                            style = Stroke(
                                width = 2.1f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private const val HOME_FILLED =
    "M12 4 L19 10.5 L19 18.5 L5 18.5 L5 10.5 Z " +
        "M9.5 18.5 L9.5 15 A2.5 2.5 0 0 1 14.5 15 L14.5 18.5 Z"
private const val HOME_OUTLINE =
    "M12 4 L19 10.5 L19 18.5 L5 18.5 L5 10.5 Z " +
        "M9.5 18.5 L9.5 15 A2.5 2.5 0 0 1 14.5 15 L14.5 18.5 Z"
private const val SEARCH =
    "M17.25 10.75 A6.5 6.5 0 1 1 4.25 10.75 A6.5 6.5 0 1 1 17.25 10.75 " +
        "M15.5 15.5 L19.75 19.75"
private const val CHAT_FILLED =
    "M8 4.5 H16 A4 4 0 0 1 20 8.5 V13 A4 4 0 0 1 16 17 H10 " +
        "L7 20.5 L8 17 A4 4 0 0 1 4 13 V8.5 A4 4 0 0 1 8 4.5 Z"
private const val CHAT_OUTLINE = CHAT_FILLED
private const val PROFILE_FILLED =
    "M4.5 20.5 V18.75 A7.5 7.5 0 0 1 12 11.25 " +
        "A7.5 7.5 0 0 1 19.5 18.75 V20.5 Z"
private const val PROFILE_OUTLINE =
    "M5.25 20 V18.75 A6.75 6.75 0 0 1 12 12 " +
        "A6.75 6.75 0 0 1 18.75 18.75 V20"
