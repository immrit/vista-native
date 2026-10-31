package ir.coffevista.vista_native.features.stories.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryStage
import ir.coffevista.vista_native.features.stories.ui.render.LocalStoryDominantColors
import ir.coffevista.vista_native.features.stories.ui.render.StoryBrush
import ir.coffevista.vista_native.features.stories.ui.render.StoryDrawingLayer
import ir.coffevista.vista_native.features.stories.ui.render.StoryPalette
import ir.coffevista.vista_native.features.stories.ui.render.StoryStroke
import kotlin.math.roundToInt

@Composable
internal fun StoryDrawingEditorOverlay(
    initialStrokes: List<StoryStroke>,
    onDone: (List<StoryStroke>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    referenceBitmap: Bitmap? = null,
) {
    var strokes by remember { mutableStateOf(initialStrokes) }
    var redoStrokes by remember { mutableStateOf(listOf<StoryStroke>()) }
    var activePoints by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    var selectedBrush by remember { mutableStateOf(StoryBrush.Pen) }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var selectedWidth by remember { mutableFloatStateOf(8f) }

    var isEyedropperActive by remember { mutableStateOf(false) }
    var eyedropperNormPos by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    val dominantColors = LocalStoryDominantColors.current

    BackHandler {
        if (isEyedropperActive) {
            isEyedropperActive = false
        } else {
            onDone(strokes)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f)),
    ) {
        val activeStroke = remember(activePoints, selectedBrush, selectedColor, selectedWidth) {
            if (activePoints.isNotEmpty()) {
                StoryStroke(
                    brush = selectedBrush,
                    argb = selectedColor.toArgb(),
                    width = selectedWidth,
                    points = activePoints,
                )
            } else null
        }

        // Exact 9:16 LTR stage so live strokes match canvas & 1080x1920 export 1:1
        StoryStage(modifier = Modifier.fillMaxSize()) { stage ->
            val widthPx = stage.width.coerceAtLeast(1f)
            val heightPx = stage.height.coerceAtLeast(1f)

            StoryDrawingLayer(
                strokes = strokes,
                activeStroke = activeStroke,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedBrush, selectedColor, selectedWidth, isEyedropperActive, widthPx, heightPx) {
                        if (isEyedropperActive) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                activePoints = listOf(
                                    (offset.x / widthPx).coerceIn(0f, 1f) to (offset.y / heightPx).coerceIn(0f, 1f),
                                )
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                activePoints = activePoints + (
                                    (change.position.x / widthPx).coerceIn(0f, 1f) to
                                        (change.position.y / heightPx).coerceIn(0f, 1f)
                                )
                            },
                            onDragEnd = {
                                if (activePoints.isNotEmpty()) {
                                    strokes = strokes + StoryStroke(
                                        brush = selectedBrush,
                                        argb = selectedColor.toArgb(),
                                        width = selectedWidth,
                                        points = activePoints,
                                    )
                                    redoStrokes = emptyList()
                                    activePoints = emptyList()
                                }
                            },
                            onDragCancel = {
                                activePoints = emptyList()
                            },
                        )
                    },
            )

            // Live Eyedropper Loupe in Drawing Mode (inside LTR stage coordinates)
            if (isEyedropperActive) {
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
                        .pointerInput(referenceBitmap, dominantColors, widthPx, heightPx) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    eyedropperNormPos = Offset(
                                        (offset.x / widthPx).coerceIn(0.05f, 0.95f),
                                        (offset.y / heightPx).coerceIn(0.08f, 0.88f),
                                    )
                                    selectedColor = sampleColorAtNormalizedPoint(
                                        eyedropperNormPos.x,
                                        eyedropperNormPos.y,
                                        referenceBitmap,
                                        dominantColors.topColor,
                                        dominantColors.bottomColor,
                                    )
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    eyedropperNormPos = Offset(
                                        (change.position.x / widthPx).coerceIn(0.05f, 0.95f),
                                        (change.position.y / heightPx).coerceIn(0.08f, 0.88f),
                                    )
                                    selectedColor = sampleColorAtNormalizedPoint(
                                        eyedropperNormPos.x,
                                        eyedropperNormPos.y,
                                        referenceBitmap,
                                        dominantColors.topColor,
                                        dominantColors.bottomColor,
                                    )
                                },
                                onDragEnd = { isEyedropperActive = false },
                            )
                        },
                ) {
                    val loupeCenterX = (eyedropperNormPos.x * widthPx).roundToInt()
                    val loupeCenterY = ((eyedropperNormPos.y * heightPx) - loupeRadiusPx * 1.4f).roundToInt()
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
                    }
                }
            }
        }

        // Top HUD Bar with Close, Undo, Redo, Eyedropper, Clear, Done
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                Icon(Icons.Default.Close, contentDescription = "انصراف", tint = Color.White)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (strokes.isNotEmpty()) {
                            val removed = strokes.last()
                            strokes = strokes.dropLast(1)
                            redoStrokes = redoStrokes + removed
                        }
                    },
                    enabled = strokes.isNotEmpty(),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "بازگشت",
                        tint = if (strokes.isNotEmpty()) Color.White else Color.Gray,
                    )
                }

                IconButton(
                    onClick = {
                        if (redoStrokes.isNotEmpty()) {
                            val restored = redoStrokes.last()
                            redoStrokes = redoStrokes.dropLast(1)
                            strokes = strokes + restored
                        }
                    },
                    enabled = redoStrokes.isNotEmpty(),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "انجام مجدد",
                        tint = if (redoStrokes.isNotEmpty()) Color.White else Color.Gray,
                    )
                }

                IconButton(
                    onClick = { isEyedropperActive = !isEyedropperActive },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isEyedropperActive) VistaBrandColors.Pink else Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        Icons.Default.Colorize,
                        contentDescription = "قطره‌چکان رنگ",
                        tint = Color.White,
                    )
                }

                IconButton(
                    onClick = {
                        if (strokes.isNotEmpty()) {
                            redoStrokes = strokes.reversed()
                            strokes = emptyList()
                        }
                    },
                    enabled = strokes.isNotEmpty(),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "پاک کردن همه",
                        tint = if (strokes.isNotEmpty()) Color.White else Color.Gray,
                    )
                }
            }

            IconButton(
                onClick = { onDone(strokes) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VistaBrandColors.Indigo),
            ) {
                Icon(Icons.Default.Check, contentDescription = "تایید نقاشی", tint = Color.White)
            }
        }

        // Bottom Glassmorphic Brush Studio Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE0141418))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrushTypeChip(
                    label = "قلم",
                    icon = Icons.Default.Edit,
                    selected = selectedBrush == StoryBrush.Pen,
                    onClick = { selectedBrush = StoryBrush.Pen },
                )
                BrushTypeChip(
                    label = "نئون",
                    icon = Icons.Default.Bolt,
                    selected = selectedBrush == StoryBrush.Neon,
                    onClick = { selectedBrush = StoryBrush.Neon },
                )
                BrushTypeChip(
                    label = "ماژیک",
                    icon = Icons.Default.Brush,
                    selected = selectedBrush == StoryBrush.Marker,
                    onClick = { selectedBrush = StoryBrush.Marker },
                )
                BrushTypeChip(
                    label = "پاک‌کن",
                    icon = Icons.Default.AutoFixNormal,
                    selected = selectedBrush == StoryBrush.Eraser,
                    onClick = { selectedBrush = StoryBrush.Eraser },
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StrokeWidthChip(
                    label = "نازک",
                    selected = selectedWidth == 4f,
                    onClick = { selectedWidth = 4f },
                )
                StrokeWidthChip(
                    label = "متوسط",
                    selected = selectedWidth == 10f,
                    onClick = { selectedWidth = 10f },
                )
                StrokeWidthChip(
                    label = "ضخیم",
                    selected = selectedWidth == 22f,
                    onClick = { selectedWidth = 22f },
                )
            }

            if (selectedBrush != StoryBrush.Eraser) {
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StoryPalette.presets.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.5.dp, Color.White, CircleShape)
                                    } else Modifier,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrushTypeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            selectedContainerColor = VistaBrandColors.Indigo,
            labelColor = Color.White,
            selectedLabelColor = Color.White,
            iconColor = Color.White,
            selectedLeadingIconColor = Color.White,
        ),
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun StrokeWidthChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            selectedContainerColor = Color.White.copy(alpha = 0.25f),
            labelColor = Color.White,
            selectedLabelColor = Color.White,
        ),
        shape = RoundedCornerShape(12.dp),
    )
}
