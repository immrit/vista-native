package ir.coffevista.vista_native.features.stories.ui.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

internal class StoryTextEditorBinding(
    val value: TextFieldValue,
    val onValueChange: (TextFieldValue) -> Unit,
    val focusRequester: FocusRequester,
    val placeholder: String,
)

internal fun StoryTextAlign.toComposeAlign(): TextAlign = when (this) {
    StoryTextAlign.Left -> TextAlign.Left
    StoryTextAlign.Center -> TextAlign.Center
    StoryTextAlign.Right -> TextAlign.Right
}

@Composable
internal fun StoryRichText(
    spec: StoryTextSpec,
    modifier: Modifier = Modifier,
    editor: StoryTextEditorBinding? = null,
) {
    val density = LocalDensity.current
    val hasBadge = spec.background == StoryTextBackground.Solid || spec.background == StoryTextBackground.Soft
    val padXUnits = if (hasBadge) spec.fontSize * 0.5f else 4f
    val padYUnits = if (hasBadge) spec.fontSize * 0.18f else 4f
    val radiusUnits = spec.fontSize * 0.55f

    val padXPx = with(density) { padXUnits.dp.toPx() }
    val padYPx = with(density) { padYUnits.dp.toPx() }
    val radiusPx = with(density) { radiusUnits.dp.toPx() }

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val neon = spec.fontStyle == StoryFontStyle.Neon
    val fillColor = if (neon && spec.gradient == null) lerp(spec.color, Color.White, 0.72f) else spec.color
    val baseStyle = remember(spec.fontSize, spec.fontStyle, spec.align) {
        TextStyle(
            fontFamily = spec.fontStyle.family,
            fontWeight = spec.fontStyle.weight,
            fontSize = spec.fontSize.sp,
            lineHeight = (spec.fontSize * 1.3f).sp,
            letterSpacing = spec.fontStyle.letterSpacingEm.em,
            textAlign = spec.align.toComposeAlign(),
            textDirection = TextDirection.Content,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        )
    }
    val fillStyle = remember(baseStyle, fillColor, spec.gradient, spec.background, neon) {
        val gradient = spec.gradient
        val styled = if (gradient != null) baseStyle.copy(brush = gradient.brush()) else baseStyle.copy(color = fillColor)
        if (spec.background == StoryTextBackground.None && !neon) {
            styled.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.58f),
                    offset = Offset(0f, 3f),
                    blurRadius = 10f,
                ),
            )
        } else {
            styled
        }
    }
    val glowPx = with(density) { (spec.fontSize * 0.55f).dp.toPx() }
    val strokePx = with(density) { (spec.fontSize * 0.16f).dp.toPx() }
    val glassBorderStrokePx = with(density) { 1.dp.toPx() }

    val badgePath = remember(layout, padXPx, padYPx, radiusPx, hasBadge) {
        val result = layout
        if (!hasBadge || result == null || result.lineCount == 0 || result.layoutInput.text.isBlank()) {
            null
        } else {
            val last = result.lineCount - 1
            val rects = (0..last).map { i ->
                LineRect(
                    left = result.getLineLeft(i),
                    top = if (i == 0) 0f else padYPx + result.getLineTop(i),
                    right = result.getLineRight(i) + 2f * padXPx,
                    bottom = if (i == last) result.size.height + 2f * padYPx else padYPx + result.getLineBottom(i),
                )
            }
            StoryLineBadge.build(rects, radiusPx).toComposePath()
        }
    }

    val text = if (spec.fontStyle.uppercaseLatin) spec.text.uppercase() else spec.text

    Box(
        modifier = modifier
            .drawBehind {
                badgePath?.let { path ->
                    val pillFill = if (spec.background == StoryTextBackground.Soft) {
                        spec.backgroundColor.copy(alpha = spec.backgroundColor.alpha.coerceIn(0.42f, 0.68f))
                    } else {
                        spec.backgroundColor.copy(alpha = 1f)
                    }
                    drawPath(path, color = pillFill)
                    if (spec.background == StoryTextBackground.Soft) {
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = 0.24f),
                            style = Stroke(width = glassBorderStrokePx, join = StrokeJoin.Round),
                        )
                    }
                }
            }
            .padding(horizontal = padXUnits.dp, vertical = padYUnits.dp),
    ) {
        if (spec.background == StoryTextBackground.Outline && text.isNotEmpty()) {
            BasicText(
                text = text,
                style = baseStyle.copy(
                    color = spec.backgroundColor.copy(alpha = 1f),
                    drawStyle = Stroke(width = strokePx * 2f, join = StrokeJoin.Round),
                ),
                modifier = Modifier.align(Alignment.Center).clearAndSetSemantics { },
            )
        }
        if (neon && text.isNotEmpty()) {
            val glow = if (spec.gradient != null) spec.gradient.colors.first() else spec.color
            BasicText(
                text = text,
                style = baseStyle.copy(
                    color = glow,
                    shadow = Shadow(color = glow.copy(alpha = 0.95f), offset = Offset.Zero, blurRadius = glowPx),
                ),
                modifier = Modifier.align(Alignment.Center).clearAndSetSemantics { },
            )
        }

        if (editor == null) {
            BasicText(
                text = text,
                style = if (neon) fillStyle.copy(shadow = Shadow(glowColor(spec).copy(alpha = 0.8f), Offset.Zero, glowPx * 0.4f)) else fillStyle,
                onTextLayout = { layout = it },
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            BasicTextField(
                value = editor.value,
                onValueChange = editor.onValueChange,
                textStyle = fillStyle,
                cursorBrush = SolidColor(if (spec.gradient != null) Color.White else fillColor),
                onTextLayout = { layout = it },
                visualTransformation = if (spec.fontStyle.uppercaseLatin) UppercaseWhenSafe else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .defaultMinSize(minWidth = 120.dp)
                    .focusRequester(editor.focusRequester),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (editor.value.text.isEmpty()) {
                            BasicText(
                                text = editor.placeholder,
                                style = baseStyle.copy(color = fillColor.copy(alpha = 0.45f)),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

private fun glowColor(spec: StoryTextSpec): Color = spec.gradient?.colors?.first() ?: spec.color

private val UppercaseWhenSafe = VisualTransformation { text ->
    val upper = text.text.uppercase()
    if (upper.length == text.text.length) {
        TransformedText(AnnotatedString(upper), OffsetMapping.Identity)
    } else {
        TransformedText(text, OffsetMapping.Identity)
    }
}

internal fun List<BadgeCommand>.toComposePath(): Path {
    val path = Path()
    forEach { command ->
        when (command) {
            is BadgeCommand.MoveTo -> path.moveTo(command.x, command.y)
            is BadgeCommand.LineTo -> path.lineTo(command.x, command.y)
            is BadgeCommand.CubicTo -> path.cubicTo(command.x1, command.y1, command.x2, command.y2, command.x3, command.y3)
            BadgeCommand.Close -> path.close()
        }
    }
    return path
}
