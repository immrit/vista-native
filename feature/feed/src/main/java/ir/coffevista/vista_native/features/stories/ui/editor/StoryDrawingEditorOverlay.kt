package ir.coffevista.vista_native.features.stories.ui.editor

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.ui.render.StoryBrush
import ir.coffevista.vista_native.features.stories.ui.render.StoryDrawingLayer
import ir.coffevista.vista_native.features.stories.ui.render.StoryPalette
import ir.coffevista.vista_native.features.stories.ui.render.StoryStroke

@Composable
internal fun StoryDrawingEditorOverlay(
    initialStrokes: List<StoryStroke>,
    onDone: (List<StoryStroke>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var strokes by remember { mutableStateOf(initialStrokes) }
    var activePoints by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    var selectedBrush by remember { mutableStateOf(StoryBrush.Pen) }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var selectedWidth by remember { mutableFloatStateOf(8f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

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

        StoryDrawingLayer(
            strokes = strokes,
            activeStroke = activeStroke,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedBrush, selectedColor, selectedWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activePoints = listOf(offset.x / widthPx to offset.y / heightPx)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            activePoints = activePoints + (change.position.x / widthPx to change.position.y / heightPx)
                        },
                        onDragEnd = {
                            if (activePoints.isNotEmpty()) {
                                strokes = strokes + StoryStroke(
                                    brush = selectedBrush,
                                    argb = selectedColor.toArgb(),
                                    width = selectedWidth,
                                    points = activePoints,
                                )
                                activePoints = emptyList()
                            }
                        },
                        onDragCancel = {
                            activePoints = emptyList()
                        },
                    )
                },
        )

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
                        if (strokes.isNotEmpty()) strokes = strokes.dropLast(1)
                    },
                    enabled = strokes.isNotEmpty(),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "بازگشت",
                        tint = if (strokes.isNotEmpty()) Color.White else Color.Gray,
                    )
                }

                IconButton(
                    onClick = { strokes = emptyList() },
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.88f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BrushTypeChip(
                        label = "قلم",
                        icon = Icons.Default.Edit,
                        selected = selectedBrush == StoryBrush.Pen,
                        onClick = { selectedBrush = StoryBrush.Pen },
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

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
