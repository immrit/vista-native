package ir.coffevista.vista_native.features.chat.presentation.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import ir.coffevista.vista_native.features.chat.presentation.MAX_ATTACHMENT_CAPTION_LENGTH
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private enum class ReviewMode {
    VIEW,
    CROP,
    DRAW,
}

private val ReviewAccentBlue = Color(0xFF2AABEE)
private val ReviewAccentBlueDeep = Color(0xFF229ED9)
private val ReviewPillBackground = Color(0xFF1E232F)

@Composable
internal fun ChatAttachmentReviewSheet(
    drafts: List<ChatAttachmentDraft>,
    recipientTitle: String = "",
    onDismiss: () -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onSend: (String) -> Unit,
    onUpdateDraft: (Int, ChatAttachmentDraft) -> Unit = { _, _ -> },
) {
    if (drafts.isEmpty()) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by rememberSaveable { mutableStateOf(drafts.first().uri) }
    var caption by rememberSaveable { mutableStateOf("") }
    var currentMode by remember { mutableStateOf(ReviewMode.VIEW) }
    var isProcessingSend by remember { mutableStateOf(false) }
    var isHd by rememberSaveable { mutableStateOf(false) }

    // Map of edit states for each draft (by original Uri)
    val editStates = remember { mutableStateMapOf<String, ImageEditState>() }

    val selectedIndex = drafts.indexOfFirst { it.uri == selectedUri }
    val safeIndex = selectedIndex.takeIf { it >= 0 } ?: 0
    val selectedDraft = drafts[safeIndex]
    val isImage = selectedDraft.kind == AttachmentKind.IMAGE

    val currentEditState = editStates[selectedDraft.uri] ?: ImageEditState()

    val layoutDirection = LocalLayoutDirection.current
    val dragStepPx = with(LocalDensity.current) { 80.dp.toPx() }

    val density = LocalDensity.current
    val imeBottomDp = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val isKeyboardOpen = imeBottomDp > 60.dp

    BackHandler {
        when (currentMode) {
            ReviewMode.CROP, ReviewMode.DRAW -> currentMode = ReviewMode.VIEW
            ReviewMode.VIEW -> if (!isProcessingSend) onDismiss()
        }
    }

    val onPerformSend = {
        if (!isProcessingSend) {
            isProcessingSend = true
            coroutineScope.launch {
                try {
                    // Apply any edits to drafts
                    drafts.forEachIndexed { idx, draft ->
                        val edit = editStates[draft.uri]
                        if (edit != null && edit.isModified && draft.kind == AttachmentKind.IMAGE) {
                            val saved = ChatImageEditorHelper.applyEditsAndSave(
                                context = context,
                                originalUri = Uri.parse(draft.uri),
                                editState = edit,
                            )
                            if (saved != null) {
                                val updated = draft.copy(
                                    uri = saved.first.toString(),
                                    sizeBytes = saved.second,
                                )
                                onUpdateDraft(idx, updated)
                            }
                        }
                    }
                    onSend(caption)
                } finally {
                    isProcessingSend = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}, // Absorb all clicks
            )
            .testTag("attachment-review-sheet"),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Minimalist Top Action Bar
            AttachmentReviewTopBar(
                mode = currentMode,
                totalCount = drafts.size,
                currentIndex = safeIndex,
                hasModifications = currentEditState.isModified,
                recipientTitle = recipientTitle,
                onCloseOrBack = {
                    if (currentMode == ReviewMode.VIEW) {
                        if (!isProcessingSend) onDismiss()
                    } else {
                        currentMode = ReviewMode.VIEW
                    }
                },
                onResetEdits = {
                    editStates.remove(selectedDraft.uri)
                },
            )

            // Middle Viewport (Interactive Stage)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when (currentMode) {
                    ReviewMode.VIEW -> {
                        AttachmentMediaViewStage(
                            draft = selectedDraft,
                            editState = currentEditState,
                        )
                    }
                    ReviewMode.CROP -> {
                        AttachmentCropStage(
                            draft = selectedDraft,
                            initialCrop = currentEditState.cropRectNorm,
                            rotationDegrees = currentEditState.rotationDegrees,
                            onRotate = {
                                val nextRotation = (currentEditState.rotationDegrees + 90f) % 360f
                                editStates[selectedDraft.uri] = currentEditState.copy(rotationDegrees = nextRotation)
                            },
                            onApplyCrop = { newCrop ->
                                editStates[selectedDraft.uri] = currentEditState.copy(cropRectNorm = newCrop)
                                currentMode = ReviewMode.VIEW
                            },
                            onCancel = { currentMode = ReviewMode.VIEW },
                        )
                    }
                    ReviewMode.DRAW -> {
                        AttachmentDrawStage(
                            draft = selectedDraft,
                            initialStrokes = currentEditState.strokes,
                            rotationDegrees = currentEditState.rotationDegrees,
                            onApplyStrokes = { newStrokes ->
                                editStates[selectedDraft.uri] = currentEditState.copy(strokes = newStrokes)
                                currentMode = ReviewMode.VIEW
                            },
                            onCancel = { currentMode = ReviewMode.VIEW },
                        )
                    }
                }

                if (isProcessingSend) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CircularProgressIndicator(
                                color = ReviewAccentBlue,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                text = "در حال پردازش تصویر…",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            // Bottom Controls (Visible in VIEW mode, adapts to keyboard)
            AnimatedVisibility(
                visible = currentMode == ReviewMode.VIEW,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    // Multi-attachment thumbnails strip (when keyboard is closed)
                    if (drafts.size > 1 && !isKeyboardOpen) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            itemsIndexed(drafts, key = { _, draft -> draft.uri }) { index, draft ->
                                var accumulatedDrag by remember(draft.uri, index) { mutableFloatStateOf(0f) }
                                val isSelected = index == safeIndex
                                val itemEdit = editStates[draft.uri]

                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1B1E28))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) ReviewAccentBlue else Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .pointerInput(index, drafts.size, layoutDirection) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { accumulatedDrag = 0f },
                                                onDragCancel = { accumulatedDrag = 0f },
                                                onDragEnd = { accumulatedDrag = 0f },
                                            ) { change, dragAmount ->
                                                change.consume()
                                                accumulatedDrag += dragAmount.x
                                                if (abs(accumulatedDrag) >= dragStepPx) {
                                                    val physicalDirection = if (accumulatedDrag > 0f) 1 else -1
                                                    val logicalDirection = if (layoutDirection == LayoutDirection.Rtl) {
                                                        -physicalDirection
                                                    } else {
                                                        physicalDirection
                                                    }
                                                    val targetIndex = (index + logicalDirection).coerceIn(drafts.indices)
                                                    if (targetIndex != index) onMove(index, targetIndex)
                                                    accumulatedDrag = 0f
                                                }
                                            }
                                        }
                                        .clickable { selectedUri = draft.uri }
                                        .semantics {
                                            customActions = listOfNotNull(
                                                if (index > 0) {
                                                    CustomAccessibilityAction("انتقال به جایگاه قبلی") {
                                                        onMove(index, index - 1)
                                                        true
                                                    }
                                                } else null,
                                                if (index < drafts.lastIndex) {
                                                    CustomAccessibilityAction("انتقال به جایگاه بعدی") {
                                                        onMove(index, index + 1)
                                                        true
                                                    }
                                                } else null,
                                            )
                                        }
                                        .testTag("attachment-review-thumb:$index"),
                                ) {
                                    if (draft.kind in MEDIA_KINDS) {
                                        AsyncImage(
                                            model = Uri.parse(draft.uri),
                                            contentDescription = draft.fileName,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    rotationZ = itemEdit?.rotationDegrees ?: 0f
                                                },
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Description,
                                            draft.fileName,
                                            Modifier.align(Alignment.Center).size(24.dp),
                                            tint = ReviewAccentBlue,
                                        )
                                    }

                                    // Badge for index
                                    Text(
                                        text = (index + 1).toString().toPersianDigits(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(topEnd = 6.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                    )

                                    // Edit indicator dot if modified
                                    if (itemEdit?.isModified == true) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(3.dp)
                                                .size(6.dp)
                                                .background(Color(0xFF00C853), CircleShape),
                                        )
                                    }

                                    // Remove thumbnail button
                                    IconButton(
                                        onClick = {
                                            onRemove(index)
                                            if (selectedUri == draft.uri) {
                                                val next = drafts.getOrNull(index + 1) ?: drafts.getOrNull(index - 1)
                                                if (next != null) selectedUri = next.uri
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                                            .testTag("attachment-review-remove:$index"),
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "حذف " + draft.fileName,
                                            modifier = Modifier.size(11.dp),
                                            tint = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isKeyboardOpen) {
                        // Keyboard OPEN state: Capsule docked right above the keyboard
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(ReviewPillBackground)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(26.dp))
                                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SentimentSatisfiedAlt,
                                contentDescription = "ایموجی",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp),
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("attachment-review-caption"),
                            ) {
                                if (caption.isEmpty()) {
                                    Text(
                                        text = "افزودن شرح…",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 15.sp,
                                    )
                                }
                                BasicTextField(
                                    value = caption,
                                    onValueChange = { caption = it.take(MAX_ATTACHMENT_CAPTION_LENGTH) },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 15.sp,
                                    ),
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            if (caption.isNotEmpty()) {
                                IconButton(
                                    onClick = { caption = "" },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "پاک کردن متن",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }

                            // Blue Circular Send Button docked inside capsule
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ReviewAccentBlue, ReviewAccentBlueDeep),
                                        ),
                                    )
                                    .clickable(enabled = !isProcessingSend, onClick = onPerformSend)
                                    .testTag("attachment-review-send"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "ارسال",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    } else {
                        // Keyboard CLOSED state: Caption capsule + bottom action bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Row 1: Full-width Caption Capsule
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(ReviewPillBackground)
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("attachment-review-caption"),
                            ) {
                                if (caption.isEmpty()) {
                                    Text(
                                        text = "افزودن شرح…",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 15.sp,
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    BasicTextField(
                                        value = caption,
                                        onValueChange = { caption = it.take(MAX_ATTACHMENT_CAPTION_LENGTH) },
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 15.sp,
                                        ),
                                        maxLines = 4,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (caption.isNotEmpty()) {
                                        Text(
                                            text = "${caption.length}/${MAX_ATTACHMENT_CAPTION_LENGTH}".toPersianDigits(),
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(start = 6.dp),
                                        )
                                    }
                                }
                            }

                            // Row 2: Bottom Toolbar (Tools on start, Send on end)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                // Left Editing Tools
                                if (isImage) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        // Crop & Rotate Tool
                                        IconButton(
                                            onClick = { currentMode = ReviewMode.CROP },
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Crop,
                                                contentDescription = "برش و چرخش",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }

                                        // Doodle / Brush Tool
                                        IconButton(
                                            onClick = { currentMode = ReviewMode.DRAW },
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Brush,
                                                contentDescription = "نقاشی",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }

                                        // HD Quality Toggle Badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isHd) ReviewAccentBlue else Color.White.copy(alpha = 0.7f),
                                                    shape = RoundedCornerShape(6.dp),
                                                )
                                                .background(if (isHd) ReviewAccentBlue.copy(alpha = 0.25f) else Color.Transparent)
                                                .clickable { isHd = !isHd }
                                                .padding(horizontal = 7.dp, vertical = 3.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "HD",
                                                color = if (isHd) ReviewAccentBlue else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }

                                        // Adjustments / Quick Rotate Tool
                                        IconButton(
                                            onClick = {
                                                val nextRotation = (currentEditState.rotationDegrees + 90f) % 360f
                                                editStates[selectedDraft.uri] = currentEditState.copy(rotationDegrees = nextRotation)
                                            },
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = "تنظیمات و چرخش ۹۰ درجه",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(40.dp))
                                }

                                // Right: Send Button
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .shadow(6.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(ReviewAccentBlue, ReviewAccentBlueDeep),
                                            ),
                                        )
                                        .clickable(enabled = !isProcessingSend, onClick = onPerformSend)
                                        .testTag("attachment-review-send"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "ارسال",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentReviewTopBar(
    mode: ReviewMode,
    totalCount: Int,
    currentIndex: Int,
    hasModifications: Boolean,
    recipientTitle: String,
    onCloseOrBack: () -> Unit,
    onResetEdits: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Back / Close
        IconButton(
            onClick = onCloseOrBack,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "بازگشت",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        // Title (Recipient title or Mode title)
        Text(
            text = when (mode) {
                ReviewMode.VIEW -> recipientTitle.ifBlank { "پیش‌نمایش تصویر" }
                ReviewMode.CROP -> "برش و چرخش"
                ReviewMode.DRAW -> "نقاشی روی تصویر"
            },
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        // End Action (Reset button or Selection indicator badge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hasModifications) {
                IconButton(
                    onClick = onResetEdits,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "بازنشانی تغییرات",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (mode == ReviewMode.VIEW) {
                if (totalCount > 1) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(ReviewAccentBlue, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (currentIndex + 1).toString().toPersianDigits(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun AttachmentMediaViewStage(
    draft: ChatAttachmentDraft,
    editState: ImageEditState,
) {
    val animatedRotation by animateFloatAsState(
        targetValue = editState.rotationDegrees,
        label = "rotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (draft.kind in MEDIA_KINDS) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = animatedRotation
                    },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = Uri.parse(draft.uri),
                    contentDescription = draft.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )

                // Render live strokes on top of image
                if (editState.strokes.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        editState.strokes.forEach { stroke ->
                            if (stroke.points.size > 1) {
                                val p = Path()
                                val first = stroke.points[0]
                                p.moveTo(first.x * canvasW, first.y * canvasH)

                                for (i in 1 until stroke.points.size) {
                                    val prev = stroke.points[i - 1]
                                    val curr = stroke.points[i]
                                    val midX = (prev.x + curr.x) / 2f * canvasW
                                    val midY = (prev.y + curr.y) / 2f * canvasH
                                    p.quadraticTo(prev.x * canvasW, prev.y * canvasH, midX, midY)
                                }
                                val last = stroke.points.last()
                                p.lineTo(last.x * canvasW, last.y * canvasH)

                                drawPath(
                                    path = p,
                                    color = Color(stroke.colorArgb),
                                    style = Stroke(
                                        width = stroke.strokeWidthNorm * canvasW,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            if (draft.kind == AttachmentKind.VIDEO) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "ویدئو",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        } else {
            // Document card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1B1E28))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(32.dp),
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = ReviewAccentBlue,
                )
                Text(
                    text = draft.fileName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = formatAttachmentBytes(draft.sizeBytes),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun AttachmentCropStage(
    draft: ChatAttachmentDraft,
    initialCrop: NormalizedCropRect,
    rotationDegrees: Float,
    onRotate: () -> Unit,
    onApplyCrop: (NormalizedCropRect) -> Unit,
    onCancel: () -> Unit,
) {
    var cropRect by remember { mutableStateOf(initialCrop) }
    var selectedRatio by remember { mutableStateOf("free") }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            // Base Image
            AsyncImage(
                model = Uri.parse(draft.uri),
                contentDescription = "برش",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.rotationZ = rotationDegrees
                    },
            )

            // Crop Overlay with Handles and 3x3 Grid
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(widthPx, heightPx, selectedRatio) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dx = dragAmount.x / widthPx
                            val dy = dragAmount.y / heightPx

                            val current = cropRect
                            val newRight = (current.right + dx).coerceIn(current.left + 0.15f, 1f)
                            val newBottom = (current.bottom + dy).coerceIn(current.top + 0.15f, 1f)

                            cropRect = NormalizedCropRect(
                                left = current.left,
                                top = current.top,
                                right = newRight,
                                bottom = newBottom,
                            )
                        }
                    },
            ) {
                val left = cropRect.left * widthPx
                val top = cropRect.top * heightPx
                val right = cropRect.right * widthPx
                val bottom = cropRect.bottom * heightPx
                val cropW = right - left
                val cropH = bottom - top

                // Scrim around crop rect
                drawRect(Color.Black.copy(alpha = 0.55f))
                // Clear cut in crop rect
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(cropW, cropH),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear,
                )
                // Crop border
                drawRect(
                    color = Color.White,
                    topLeft = Offset(left, top),
                    size = Size(cropW, cropH),
                    style = Stroke(width = 1.5.dp.toPx()),
                )

                // 3x3 Grid
                val oneThirdW = cropW / 3f
                val oneThirdH = cropH / 3f
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(left + oneThirdW, top),
                    end = Offset(left + oneThirdW, bottom),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(left + 2 * oneThirdW, top),
                    end = Offset(left + 2 * oneThirdW, bottom),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(left, top + oneThirdH),
                    end = Offset(right, top + oneThirdH),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(left, top + 2 * oneThirdH),
                    end = Offset(right, top + 2 * oneThirdH),
                    strokeWidth = 1.dp.toPx(),
                )

                // Corner brackets
                val cornerLen = 16.dp.toPx()
                val cornerStroke = 3.dp.toPx()
                drawLine(Color.White, Offset(left, top), Offset(left + cornerLen, top), cornerStroke)
                drawLine(Color.White, Offset(left, top), Offset(left, top + cornerLen), cornerStroke)
                drawLine(Color.White, Offset(right, top), Offset(right - cornerLen, top), cornerStroke)
                drawLine(Color.White, Offset(right, top), Offset(right, top + cornerLen), cornerStroke)
                drawLine(Color.White, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerStroke)
                drawLine(Color.White, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerStroke)
                drawLine(Color.White, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerStroke)
                drawLine(Color.White, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerStroke)
            }
        }

        // Crop Tools Toolbar (Aspect Ratios + Rotate + Actions)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131620))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Aspect Ratio Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                RatioChip(
                    title = "آزاد",
                    isSelected = selectedRatio == "free",
                    onClick = {
                        selectedRatio = "free"
                        cropRect = NormalizedCropRect(0f, 0f, 1f, 1f)
                    },
                )
                RatioChip(
                    title = "۱:۱",
                    isSelected = selectedRatio == "1:1",
                    onClick = {
                        selectedRatio = "1:1"
                        cropRect = NormalizedCropRect(0.1f, 0.1f, 0.9f, 0.9f)
                    },
                )
                RatioChip(
                    title = "۴:۵",
                    isSelected = selectedRatio == "4:5",
                    onClick = {
                        selectedRatio = "4:5"
                        cropRect = NormalizedCropRect(0.1f, 0.05f, 0.9f, 0.95f)
                    },
                )
                RatioChip(
                    title = "۱۶:۹",
                    isSelected = selectedRatio == "16:9",
                    onClick = {
                        selectedRatio = "16:9"
                        cropRect = NormalizedCropRect(0.05f, 0.2f, 0.95f, 0.8f)
                    },
                )
            }

            // Bottom Buttons: Cancel, Rotate, Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "انصراف", tint = Color.White)
                }

                IconButton(
                    onClick = onRotate,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "چرخش", tint = Color.White)
                }

                IconButton(
                    onClick = { onApplyCrop(cropRect) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(ReviewAccentBlue, CircleShape),
                ) {
                    Icon(Icons.Default.Check, contentDescription = "تأیید برش", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun RatioChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = title,
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) ReviewAccentBlue else Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun AttachmentDrawStage(
    draft: ChatAttachmentDraft,
    initialStrokes: List<ChatDrawingStroke>,
    rotationDegrees: Float,
    onApplyStrokes: (List<ChatDrawingStroke>) -> Unit,
    onCancel: () -> Unit,
) {
    var strokes by remember { mutableStateOf(initialStrokes) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }

    val colors = remember {
        listOf(
            Color(0xFFFFFFFF), // White
            Color(0xFF1E202A), // Black
            Color(0xFFFF3B30), // Red
            Color(0xFFFF9500), // Orange
            Color(0xFFFFCC00), // Yellow
            Color(0xFF34C759), // Green
            Color(0xFF32ADE6), // Cyan
            Color(0xFF007AFF), // Blue
            Color(0xFF5856D6), // Purple
            Color(0xFFFF2D55), // Pink
        )
    }
    var selectedColor by remember { mutableStateOf(colors[0]) }
    var selectedWidthNorm by remember { mutableFloatStateOf(0.012f) } // medium

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            // Base Image
            AsyncImage(
                model = Uri.parse(draft.uri),
                contentDescription = "نقاشی",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.rotationZ = rotationDegrees
                    },
            )

            // Drawing Surface
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(widthPx, heightPx, selectedColor, selectedWidthNorm) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints = listOf(
                                    Offset(
                                        (offset.x / widthPx).coerceIn(0f, 1f),
                                        (offset.y / heightPx).coerceIn(0f, 1f),
                                    )
                                )
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + Offset(
                                    (change.position.x / widthPx).coerceIn(0f, 1f),
                                    (change.position.y / heightPx).coerceIn(0f, 1f),
                                )
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    strokes = strokes + ChatDrawingStroke(
                                        colorArgb = selectedColor.toArgb(),
                                        strokeWidthNorm = selectedWidthNorm,
                                        points = currentPoints,
                                    )
                                    currentPoints = emptyList()
                                }
                            },
                            onDragCancel = {
                                currentPoints = emptyList()
                            },
                        )
                    },
            ) {
                // Render completed strokes
                strokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val p = Path()
                        val first = stroke.points[0]
                        p.moveTo(first.x * widthPx, first.y * heightPx)

                        for (i in 1 until stroke.points.size) {
                            val prev = stroke.points[i - 1]
                            val curr = stroke.points[i]
                            val midX = (prev.x + curr.x) / 2f * widthPx
                            val midY = (prev.y + curr.y) / 2f * heightPx
                            p.quadraticTo(prev.x * widthPx, prev.y * heightPx, midX, midY)
                        }
                        val last = stroke.points.last()
                        p.lineTo(last.x * widthPx, last.y * heightPx)

                        drawPath(
                            path = p,
                            color = Color(stroke.colorArgb),
                            style = Stroke(
                                width = stroke.strokeWidthNorm * widthPx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }

                // Render active stroke
                if (currentPoints.size > 1) {
                    val p = Path()
                    val first = currentPoints[0]
                    p.moveTo(first.x * widthPx, first.y * heightPx)

                    for (i in 1 until currentPoints.size) {
                        val prev = currentPoints[i - 1]
                        val curr = currentPoints[i]
                        val midX = (prev.x + curr.x) / 2f * widthPx
                        val midY = (prev.y + curr.y) / 2f * heightPx
                        p.quadraticTo(prev.x * widthPx, prev.y * heightPx, midX, midY)
                    }
                    val last = currentPoints.last()
                    p.lineTo(last.x * widthPx, last.y * heightPx)

                    drawPath(
                        path = p,
                        color = selectedColor,
                        style = Stroke(
                            width = selectedWidthNorm * widthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }

        // Drawing Palette Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131620))
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Colors strip
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(colors.size) { idx ->
                    val c = colors[idx]
                    val isSelected = c == selectedColor
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape,
                            )
                            .clickable { selectedColor = c },
                    )
                }
            }

            // Brush Width Selector + Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Brush widths: Thin, Medium, Thick
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "نازک",
                        color = if (selectedWidthNorm < 0.009f) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = if (selectedWidthNorm < 0.009f) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedWidthNorm < 0.009f) ReviewAccentBlue else Color.White.copy(alpha = 0.06f))
                            .clickable { selectedWidthNorm = 0.006f }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    Text(
                        text = "متوسط",
                        color = if (selectedWidthNorm in 0.009f..0.016f) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = if (selectedWidthNorm in 0.009f..0.016f) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedWidthNorm in 0.009f..0.016f) ReviewAccentBlue else Color.White.copy(alpha = 0.06f))
                            .clickable { selectedWidthNorm = 0.012f }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    Text(
                        text = "ضخیم",
                        color = if (selectedWidthNorm > 0.016f) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = if (selectedWidthNorm > 0.016f) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedWidthNorm > 0.016f) ReviewAccentBlue else Color.White.copy(alpha = 0.06f))
                            .clickable { selectedWidthNorm = 0.024f }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }

                // Actions: Undo, Cancel, Apply
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                strokes = strokes.dropLast(1)
                            }
                        },
                        enabled = strokes.isNotEmpty(),
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "بازگشت",
                            tint = if (strokes.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "انصراف", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { onApplyStrokes(strokes) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(ReviewAccentBlue, CircleShape),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "تأیید نقاشی", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private val MEDIA_KINDS = setOf(AttachmentKind.IMAGE, AttachmentKind.VIDEO, AttachmentKind.GIF)

private fun formatAttachmentBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B".toPersianDigits()
    bytes < 1024 * 1024 -> "%.1f KB".format(Locale.US, bytes / 1024f).toPersianDigits()
    else -> "%.1f MB".format(Locale.US, bytes / (1024f * 1024f)).toPersianDigits()
}
