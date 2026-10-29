package ir.coffevista.vista_native.features.stories.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.stories.ui.render.StoryFontStyle
import ir.coffevista.vista_native.features.stories.ui.render.StoryPalette
import ir.coffevista.vista_native.features.stories.ui.render.StoryRichText
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextAlign
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextBackground
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextEditorBinding
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextGradient
import ir.coffevista.vista_native.features.stories.ui.render.StoryTextSpec

@Composable
internal fun StoryTextEditorOverlay(
    initialSpec: StoryTextSpec,
    onDone: (StoryTextSpec) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialSpec.text,
                selection = TextRange(initialSpec.text.length),
            ),
        )
    }

    var fontStyle by remember { mutableStateOf(initialSpec.fontStyle) }
    var bgMode by remember { mutableStateOf(initialSpec.background) }
    var alignment by remember { mutableStateOf(initialSpec.align) }
    var textColor by remember { mutableStateOf(initialSpec.color) }
    var bgColor by remember { mutableStateOf(initialSpec.backgroundColor) }
    var selectedGradient by remember { mutableStateOf(initialSpec.gradient) }
    var fontSize by remember { mutableFloatStateOf(initialSpec.fontSize) }

    var showSlider by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val currentSpec = remember(
        textFieldValue.text, fontSize, fontStyle, bgMode, alignment, textColor, bgColor, selectedGradient,
    ) {
        StoryTextSpec(
            text = textFieldValue.text,
            fontSize = fontSize,
            fontStyle = fontStyle,
            background = bgMode,
            align = alignment,
            color = if (bgMode == StoryTextBackground.Solid) StoryPalette.contrastOf(bgColor) else textColor,
            backgroundColor = bgColor,
            gradient = if (bgMode == StoryTextBackground.Solid) null else selectedGradient,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(onClick = { }),
    ) {
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
                    .background(Color.White.copy(alpha = 0.15f)),
            ) {
                Icon(Icons.Default.Close, contentDescription = "انصراف", tint = Color.White)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = { alignment = alignment.next() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                ) {
                    val icon = when (alignment) {
                        StoryTextAlign.Left -> Icons.AutoMirrored.Filled.FormatAlignLeft
                        StoryTextAlign.Center -> Icons.Default.FormatAlignCenter
                        StoryTextAlign.Right -> Icons.AutoMirrored.Filled.FormatAlignRight
                    }
                    Icon(icon, contentDescription = "تراز متن", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when (bgMode) {
                                StoryTextBackground.None -> Color.White.copy(alpha = 0.15f)
                                StoryTextBackground.Solid -> Color.White
                                StoryTextBackground.Soft -> Color.White.copy(alpha = 0.4f)
                                StoryTextBackground.Outline -> Color.Transparent
                            },
                        )
                        .then(
                            if (bgMode == StoryTextBackground.Outline) {
                                Modifier.border(2.dp, Color.White, CircleShape)
                            } else Modifier,
                        )
                        .clickable { bgMode = bgMode.next() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "A",
                        color = if (bgMode == StoryTextBackground.Solid) Color.Black else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (showSlider) VistaBrandColors.Indigo else Color.White.copy(alpha = 0.15f))
                        .clickable { showSlider = !showSlider },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Aa",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }

            IconButton(
                onClick = { onDone(currentSpec) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VistaBrandColors.Indigo),
            ) {
                Icon(Icons.Default.Check, contentDescription = "تایید", tint = Color.White)
            }
        }

        AnimatedVisibility(
            visible = showSlider,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp, start = 24.dp, end = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("A-", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = StoryTextSpec.MinFontSize..StoryTextSpec.MaxFontSize,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = VistaBrandColors.Indigo,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Text("A+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val editorBinding = remember(focusRequester) {
                StoryTextEditorBinding(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    focusRequester = focusRequester,
                    placeholder = "شروع به نوشتن کنید…",
                )
            }
            StoryRichText(
                spec = currentSpec,
                editor = editorBinding,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
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
                    val isSelected = selectedGradient == null && (if (bgMode == StoryTextBackground.Solid) bgColor == color else textColor == color)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable {
                                selectedGradient = null
                                if (bgMode == StoryTextBackground.Solid) {
                                    bgColor = color
                                } else {
                                    textColor = color
                                    bgColor = color.copy(alpha = 0.65f)
                                }
                            }
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.5.dp, Color.White, CircleShape)
                                } else Modifier,
                            ),
                    )
                }

                if (bgMode != StoryTextBackground.Solid) {
                    StoryTextGradient.entries.forEach { grad ->
                        val isSelected = selectedGradient == grad
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(grad.colors))
                                .clickable {
                                    selectedGradient = grad
                                    textColor = grad.colors.first()
                                }
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
                color = if (isSelected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.18f),
            selectedContainerColor = Color.White,
        ),
        shape = RoundedCornerShape(16.dp),
    )
}
