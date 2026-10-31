package ir.coffevista.vista_native.features.stories.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.ui.render.LocalStoryDominantColors
import ir.coffevista.vista_native.features.stories.ui.render.StoryFontStyle
import ir.coffevista.vista_native.features.stories.ui.render.StoryPalette
import ir.coffevista.vista_native.features.stories.ui.render.StoryRichText
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextAlign
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextBackground
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextEditorBinding
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextGradient
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextSpec
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun StoryTextEditorOverlay(
    initialSpec: StoryTextSpec,
    onDone: (StoryTextSpec) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    referenceBitmap: Bitmap? = null,
) {
    var textFieldValue by remember(initialSpec) {
        mutableStateOf(
            TextFieldValue(
                text = initialSpec.text,
                selection = TextRange(initialSpec.text.length),
            ),
        )
    }

    var fontStyle by remember(initialSpec) { mutableStateOf(initialSpec.fontStyle) }
    var bgMode by remember(initialSpec) { mutableStateOf(initialSpec.background) }
    var alignment by remember(initialSpec) { mutableStateOf(initialSpec.align) }
    var textColor by remember(initialSpec) { mutableStateOf(initialSpec.color) }
    var bgColor by remember(initialSpec) { mutableStateOf(initialSpec.backgroundColor) }
    var selectedGradient by remember(initialSpec) { mutableStateOf(initialSpec.gradient) }
    var fontSize by remember(initialSpec) { mutableFloatStateOf(initialSpec.fontSize) }

    var isDraggingVerticalSlider by remember { mutableStateOf(false) }

    // Live Canvas Eyedropper Loupe state
    var isEyedropperActive by remember { mutableStateOf(false) }
    var eyedropperNormPos by remember { mutableStateOf(Offset(0.5f, 0.45f)) }
    val dominantColors = LocalStoryDominantColors.current

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentSpec = StoryTextSpec(
        text = textFieldValue.text,
        fontSize = fontSize,
        fontStyle = fontStyle,
        background = bgMode,
        align = alignment,
        color = if (bgMode == StoryTextBackground.Solid) StoryPalette.contrastOf(bgColor) else textColor,
        backgroundColor = bgColor,
        gradient = if (bgMode == StoryTextBackground.Solid) null else selectedGradient,
    )

    fun commitOrDismiss() {
        keyboardController?.hide()
        if (currentSpec.text.isNotBlank() || initialSpec.text.isNotBlank()) {
            onDone(currentSpec)
        } else {
            onDismiss()
        }
    }

    BackHandler {
        if (isEyedropperActive) {
            isEyedropperActive = false
        } else {
            commitOrDismiss()
        }
    }

    LaunchedEffect(Unit) {
        delay(60L)
        runCatching { focusRequester.requestFocus() }
        keyboardController?.show()
    }

    fun applySampledColor(sampled: Color) {
        selectedGradient = null
        if (bgMode == StoryTextBackground.Solid) {
            bgColor = sampled
        } else {
            textColor = sampled
            bgColor = sampled.copy(alpha = 0.65f)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isEyedropperActive) 0.24f else 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (!isEyedropperActive) {
                        commitOrDismiss()
                    }
                },
            ),
    ) {
        val fullW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val fullH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // Centered Inline Text Entry with Continuous Multi-Line Pill Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 36.dp, vertical = 96.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Fresh binding keyed to textFieldValue so BasicTextField always receives live updates
            val editorBinding = remember(textFieldValue, focusRequester) {
                StoryTextEditorBinding(
                    value = textFieldValue,
                    onValueChange = { updated -> textFieldValue = updated },
                    focusRequester = focusRequester,
                    placeholder = "متن خود را بنویسید…",
                )
            }
            Box(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        runCatching { focusRequester.requestFocus() }
                        keyboardController?.show()
                    },
                ),
            ) {
                StoryRichText(
                    spec = currentSpec,
                    editor = editorBinding,
                )
            }
        }

        // Top Floating Studio Bar (Telegram / Instagram clean layout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    onDismiss()
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
            ) {
                Icon(Icons.Default.Close, contentDescription = "انصراف", tint = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Alignment Toggle
                IconButton(
                    onClick = { alignment = alignment.next() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f)),
                ) {
                    val icon = when (alignment) {
                        StoryTextAlign.Left -> Icons.AutoMirrored.Filled.FormatAlignLeft
                        StoryTextAlign.Center -> Icons.Default.FormatAlignCenter
                        StoryTextAlign.Right -> Icons.AutoMirrored.Filled.FormatAlignRight
                    }
                    Icon(icon, contentDescription = "تراز متن", tint = Color.White, modifier = Modifier.size(19.dp))
                }

                // Background Pill Mode Toggle (None -> Solid -> Frosted Glass -> Outline)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when (bgMode) {
                                StoryTextBackground.None -> Color.White.copy(alpha = 0.14f)
                                StoryTextBackground.Solid -> Color.White
                                StoryTextBackground.Soft -> Color.White.copy(alpha = 0.38f)
                                StoryTextBackground.Outline -> Color.Transparent
                            },
                        )
                        .then(
                            if (bgMode == StoryTextBackground.Outline) {
                                Modifier.border(1.8.dp, Color.White, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { bgMode = bgMode.next() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "A",
                        color = if (bgMode == StoryTextBackground.Solid) Color.Black else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                    )
                }

                // Live Canvas Eyedropper Loupe Button
                IconButton(
                    onClick = { isEyedropperActive = !isEyedropperActive },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isEyedropperActive) VistaBrandColors.Pink else Color.White.copy(alpha = 0.14f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Colorize,
                        contentDescription = "قطره‌چکان رنگ",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Crisp Done Pill Button
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable { commitOrDismiss() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF111318), modifier = Modifier.size(18.dp))
                Text(
                    text = "تایید",
                    color = Color(0xFF111318),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                )
            }
        }

        // Telegram Vertical Wedge Font-Size Slider on Screen Edge (expands width on drag)
        val sliderWidth by animateDpAsState(
            targetValue = if (isDraggingVerticalSlider) 30.dp else 16.dp,
            label = "vertical_slider_width",
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .width(sliderWidth)
                .height(210.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDraggingVerticalSlider = true
                            val norm = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                            fontSize = StoryTextSpec.MinFontSize +
                                norm * (StoryTextSpec.MaxFontSize - StoryTextSpec.MinFontSize)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val norm = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                            fontSize = StoryTextSpec.MinFontSize +
                                norm * (StoryTextSpec.MaxFontSize - StoryTextSpec.MinFontSize)
                        },
                        onDragEnd = { isDraggingVerticalSlider = false },
                        onDragCancel = { isDraggingVerticalSlider = false },
                    )
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val topHalfW = w * 0.44f
                val botHalfW = w * 0.12f
                val cx = w * 0.5f
                val wedgePath = Path().apply {
                    moveTo(cx - topHalfW, 0f)
                    lineTo(cx + topHalfW, 0f)
                    lineTo(cx + botHalfW, h)
                    lineTo(cx - botHalfW, h)
                    close()
                }
                drawPath(wedgePath, color = Color.White.copy(alpha = 0.26f))

                val fraction = ((fontSize - StoryTextSpec.MinFontSize) /
                    (StoryTextSpec.MaxFontSize - StoryTextSpec.MinFontSize)).coerceIn(0f, 1f)
                val thumbY = h * (1f - fraction)
                drawLine(
                    color = Color.White,
                    start = Offset(cx, h),
                    end = Offset(cx, thumbY),
                    strokeWidth = 3.dp.toPx(),
                )
                drawCircle(
                    color = Color.White,
                    radius = if (isDraggingVerticalSlider) 9.5.dp.toPx() else 7.dp.toPx(),
                    center = Offset(cx, thumbY),
                )
            }
        }

        // Live Canvas Eyedropper Magnifying Loupe Overlay (LTR pixel space)
        if (isEyedropperActive) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                val density = LocalDensity.current
                val loupeRadiusPx = with(density) { 38.dp.toPx() }
                val currentSampledColor = remember(eyedropperNormPos, referenceBitmap, dominantColors) {
                    sampleColorAtNormalizedPoint(
                        normX = eyedropperNormPos.x,
                        normY = eyedropperNormPos.y,
                        bitmap = referenceBitmap,
                        dominantTop = dominantColors.topColor,
                        dominantBottom = dominantColors.bottomColor,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(referenceBitmap, dominantColors) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    eyedropperNormPos = Offset(
                                        (offset.x / fullW).coerceIn(0.05f, 0.95f),
                                        (offset.y / fullH).coerceIn(0.08f, 0.88f),
                                    )
                                    applySampledColor(
                                        sampleColorAtNormalizedPoint(
                                            eyedropperNormPos.x,
                                            eyedropperNormPos.y,
                                            referenceBitmap,
                                            dominantColors.topColor,
                                            dominantColors.bottomColor,
                                        ),
                                    )
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    eyedropperNormPos = Offset(
                                        (change.position.x / fullW).coerceIn(0.05f, 0.95f),
                                        (change.position.y / fullH).coerceIn(0.08f, 0.88f),
                                    )
                                    applySampledColor(
                                        sampleColorAtNormalizedPoint(
                                            eyedropperNormPos.x,
                                            eyedropperNormPos.y,
                                            referenceBitmap,
                                            dominantColors.topColor,
                                            dominantColors.bottomColor,
                                        ),
                                    )
                                },
                                onDragEnd = {
                                    isEyedropperActive = false
                                },
                            )
                        },
                ) {
                    val loupeCenterX = (eyedropperNormPos.x * fullW).roundToInt()
                    val loupeCenterY = ((eyedropperNormPos.y * fullH) - loupeRadiusPx * 1.4f).roundToInt()

                    Canvas(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = loupeCenterX - loupeRadiusPx.roundToInt(),
                                    y = loupeCenterY - loupeRadiusPx.roundToInt(),
                                )
                            }
                            .size(76.dp),
                    ) {
                        val c = Offset(size.width * 0.5f, size.height * 0.5f)
                        val r = size.minDimension * 0.46f
                        drawCircle(color = currentSampledColor, radius = r, center = c)
                        drawCircle(color = Color.White, radius = r, center = c, style = Stroke(width = 4.dp.toPx()))
                        drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = r + 2.dp.toPx(), center = c, style = Stroke(width = 1.5.dp.toPx()))
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = c, style = Stroke(width = 1.5.dp.toPx()))
                    }
                }
            }
        }

        // Bottom Typography & Color Palette Bar above Keyboard
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StoryFontStyle.entries.forEach { style ->
                    FontStyleItem(
                        style = style,
                        isSelected = fontStyle == style,
                        onClick = { fontStyle = style },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StoryPalette.presets.forEach { color ->
                    val isSelected = selectedGradient == null &&
                        (if (bgMode == StoryTextBackground.Solid) bgColor == color else textColor == color)
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 32.dp else 28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                shape = CircleShape,
                            )
                            .clickable {
                                selectedGradient = null
                                if (bgMode == StoryTextBackground.Solid) {
                                    bgColor = color
                                } else {
                                    textColor = color
                                    bgColor = color.copy(alpha = 0.65f)
                                }
                            },
                    )
                }

                if (bgMode != StoryTextBackground.Solid) {
                    StoryTextGradient.entries.forEach { grad ->
                        val isSelected = selectedGradient == grad
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 32.dp else 28.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(grad.colors))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape,
                                )
                                .clickable {
                                    selectedGradient = grad
                                    textColor = grad.colors.first()
                                },
                        )
                    }
                }
            }
        }
    }
}

internal fun sampleColorAtNormalizedPoint(
    normX: Float,
    normY: Float,
    bitmap: Bitmap?,
    dominantTop: Color,
    dominantBottom: Color,
): Color {
    if (bitmap != null && !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
        val px = (normX.coerceIn(0f, 1f) * (bitmap.width - 1)).roundToInt().coerceIn(0, bitmap.width - 1)
        val py = (normY.coerceIn(0f, 1f) * (bitmap.height - 1)).roundToInt().coerceIn(0, bitmap.height - 1)
        return runCatching { Color(bitmap.getPixel(px, py)) }.getOrDefault(dominantTop)
    }
    return androidx.compose.ui.graphics.lerp(dominantTop, dominantBottom, normY.coerceIn(0f, 1f))
}

@Composable
private fun FontStyleItem(
    style: StoryFontStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = style.label,
                color = if (isSelected) Color(0xFF111318) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.16f),
            selectedContainerColor = Color.White,
        ),
        border = null,
        shape = RoundedCornerShape(18.dp),
    )
}
