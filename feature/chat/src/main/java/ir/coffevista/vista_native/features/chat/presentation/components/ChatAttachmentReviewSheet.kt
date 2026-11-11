package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import kotlin.math.min

private enum class ReviewMode {
    VIEW,
    CROP,
    DRAW,
}

private val ReviewAccentBlue = Color(0xFF2AABEE)
private val ReviewAccentBlueDeep = Color(0xFF229ED9)
private val ReviewPillBackground = Color(0xFF1E232F)

@OptIn(ExperimentalLayoutApi::class)
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
    val localView = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val captionFocusRequester = remember { FocusRequester() }
    val dismissFocusRequester = remember { FocusRequester() }
    val captionInteractionSource = remember { MutableInteractionSource() }
    var isCaptionFocused by remember { mutableStateOf(false) }

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
    val isImeVisible = WindowInsets.isImeVisible
    val isKeyboardOpen = isImeVisible || imeBottomDp > 60.dp
    val navBottomDp = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    var closedControlsHeightDp by remember { mutableStateOf(0.dp) }

    // Ensure WindowInsets.ime reports keyboard height on Android < 11 even when
    // MainActivity uses adjustNothing for the main chat composer.
    DisposableEffect(localView) {
        val window = localView.context.findActivity()
            ?.takeUnless { it.isFinishing || it.isDestroyed }
            ?.window
        val previousSoftInputMode = window?.attributes?.softInputMode
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && window != null) {
            @Suppress("DEPRECATION")
            runCatching { window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) }
        }
        onDispose {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && window != null && previousSoftInputMode != null) {
                runCatching { window.setSoftInputMode(previousSoftInputMode) }
            }
        }
    }

    LaunchedEffect(captionInteractionSource) {
        captionInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                keyboardController?.show()
            }
        }
    }

    val hideKeyboardAndClearFocus = {
        if (isCaptionFocused) {
            runCatching { dismissFocusRequester.requestFocus() }
        } else {
            focusManager.clearFocus(force = true)
        }
        keyboardController?.hide()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(localView.windowToken, 0)
        Unit
    }

    var wasKeyboardOpen by remember { mutableStateOf(false) }
    LaunchedEffect(isKeyboardOpen) {
        if (wasKeyboardOpen && !isKeyboardOpen && isCaptionFocused) {
            runCatching { dismissFocusRequester.requestFocus() }
        }
        wasKeyboardOpen = isKeyboardOpen
    }

    BackHandler {
        when (currentMode) {
            ReviewMode.CROP, ReviewMode.DRAW -> currentMode = ReviewMode.VIEW
            ReviewMode.VIEW -> {
                if (isKeyboardOpen) {
                    hideKeyboardAndClearFocus()
                } else if (!isProcessingSend) {
                    hideKeyboardAndClearFocus()
                    onDismiss()
                }
            }
        }
    }

    val onPerformSend = {
        if (!isProcessingSend) {
            hideKeyboardAndClearFocus()
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
            .testTag("attachment-review-sheet"),
    ) {
        // Zero-size non-text focus sink so dismissing caption focus never leaves the window
        // without a focused node (which causes ViewRootImpl to bounce focus back to BasicTextField).
        Box(
            modifier = Modifier
                .size(0.dp)
                .focusRequester(dismissFocusRequester)
                .focusable(),
        )
        // Backdrop absorbs touches so underlying chat views never receive clicks,
        // without participating in focus traversal, and dismisses the keyboard on tap.
        Box(
            modifier = Modifier
                .matchParentSize()
                .focusProperties { canFocus = false }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (isKeyboardOpen || isCaptionFocused) {
                            hideKeyboardAndClearFocus()
                        }
                    },
                ),
        )
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
                        if (!isProcessingSend) {
                            hideKeyboardAndClearFocus()
                            onDismiss()
                        }
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
                        // Keep stage sized to the resting (closed-controls) bounds so opening the keyboard
                        // slides the caption bar over the photo instead of shrinking or lifting the image.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = closedControlsHeightDp)
                                .testTag("attachment-review-stage"),
                            contentAlignment = Alignment.Center,
                        ) {
                            AttachmentMediaViewStage(
                                draft = selectedDraft,
                                editState = currentEditState,
                            )
                        }
                    }
                    ReviewMode.CROP -> {
                        AttachmentCropStage(
                            draft = selectedDraft,
                            initialCrop = currentEditState.cropRectNorm,
                            rotationDegrees = currentEditState.rotationDegrees,
                            onRotate = { rotatedCrop ->
                                val nextRotation = (currentEditState.rotationDegrees + 90f) % 360f
                                editStates[selectedDraft.uri] = currentEditState.copy(
                                    rotationDegrees = nextRotation,
                                    cropRectNorm = rotatedCrop,
                                )
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
                            cropRectNorm = currentEditState.cropRectNorm,
                            onApplyStrokes = { newStrokes ->
                                editStates[selectedDraft.uri] = currentEditState.copy(strokes = newStrokes)
                                currentMode = ReviewMode.VIEW
                            },
                            onCancel = { currentMode = ReviewMode.VIEW },
                        )
                    }
                }

                // Bottom Controls overlay in VIEW mode: floats above IME without resizing the photo stage
                androidx.compose.animation.AnimatedVisibility(
                    visible = currentMode == ReviewMode.VIEW,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isKeyboardOpen) {
                                    Modifier.background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.72f),
                                            ),
                                        ),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .navigationBarsPadding()
                            .imePadding(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onSizeChanged { size ->
                                    if (!isKeyboardOpen && !isCaptionFocused) {
                                        closedControlsHeightDp = with(density) { size.height.toDp() } + navBottomDp
                                    }
                                },
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

                    // Keep a single BasicTextField in a stable tree position across keyboard open/close
                    // transitions so IME focus and the active TextInputSession are never disposed.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isKeyboardOpen) 12.dp else 14.dp,
                                vertical = 6.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val captionShape = RoundedCornerShape(if (isKeyboardOpen) 26.dp else 24.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(captionShape)
                                .background(ReviewPillBackground)
                                .border(
                                    width = 1.dp,
                                    color = if (isCaptionFocused || isKeyboardOpen) {
                                        ReviewAccentBlue.copy(alpha = 0.55f)
                                    } else {
                                        Color.White.copy(alpha = 0.08f)
                                    },
                                    shape = captionShape,
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        captionFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                }
                                .padding(
                                    start = 16.dp,
                                    end = if (isKeyboardOpen) 8.dp else 16.dp,
                                    top = 4.dp,
                                    bottom = 4.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BasicTextField(
                                value = caption,
                                onValueChange = { caption = it.take(MAX_ATTACHMENT_CAPTION_LENGTH) },
                                interactionSource = captionInteractionSource,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    textDirection = TextDirection.ContentOrRtl,
                                ),
                                cursorBrush = SolidColor(ReviewAccentBlue),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Default,
                                ),
                                maxLines = 4,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(captionFocusRequester)
                                    .onFocusChanged { focusState ->
                                        isCaptionFocused = focusState.isFocused
                                    }
                                    .testTag("attachment-review-caption"),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 40.dp)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        if (caption.isEmpty()) {
                                            Text(
                                                text = "افزودن شرح…",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 15.sp,
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                            )

                            if (caption.isNotEmpty()) {
                                Text(
                                    text = "${caption.length}/${MAX_ATTACHMENT_CAPTION_LENGTH}".toPersianDigits(),
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                )
                            }

                            if (isKeyboardOpen && caption.isNotEmpty()) {
                                IconButton(
                                    onClick = { caption = "" },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "پاک کردن متن",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }

                            if (isKeyboardOpen) {
                                AttachmentReviewSendButton(
                                    buttonSize = 38.dp,
                                    iconSize = 18.dp,
                                    shadowElevation = 0.dp,
                                    enabled = !isProcessingSend,
                                    onClick = onPerformSend,
                                )
                            }
                        }

                        if (!isKeyboardOpen) {
                            // Bottom Toolbar (Tools on start, Send on end)
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
                                            onClick = {
                                                hideKeyboardAndClearFocus()
                                                currentMode = ReviewMode.CROP
                                            },
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
                                            onClick = {
                                                hideKeyboardAndClearFocus()
                                                currentMode = ReviewMode.DRAW
                                            },
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
                                                editStates[selectedDraft.uri] = currentEditState.copy(
                                                    rotationDegrees = nextRotation,
                                                    cropRectNorm = ChatImageEditorHelper.rotateCropRectClockwise90(
                                                        currentEditState.cropRectNorm,
                                                    ),
                                                )
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

                                AttachmentReviewSendButton(
                                    buttonSize = 50.dp,
                                    iconSize = 22.dp,
                                    shadowElevation = 6.dp,
                                    enabled = !isProcessingSend,
                                    onClick = onPerformSend,
                                )
                            }
                        }
                    }
                }
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
        }
    }
}

@Composable
private fun AttachmentReviewSendButton(
    buttonSize: Dp,
    iconSize: Dp,
    shadowElevation: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .let { mod -> if (shadowElevation > 0.dp) mod.shadow(shadowElevation, CircleShape) else mod }
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(ReviewAccentBlue, ReviewAccentBlueDeep),
                ),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .testTag("attachment-review-send"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "ارسال",
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
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
private fun rememberResolvedImageSize(
    uri: Uri,
    fallbackSize: Size = Size(1000f, 1000f),
): Pair<Size, (Int, Int) -> Unit> {
    val context = LocalContext.current
    var resolvedSize by remember(uri) { mutableStateOf<Size?>(null) }

    LaunchedEffect(uri) {
        val loaded = ChatImageEditorHelper.loadImageDimensions(context, uri)
        if (loaded != null && loaded.width > 0f && loaded.height > 0f) {
            resolvedSize = loaded
        }
    }

    val onDrawableLoaded: (Int, Int) -> Unit = remember(uri) {
        { width, height ->
            if (width > 0 && height > 0) {
                resolvedSize = Size(width.toFloat(), height.toFloat())
            }
        }
    }

    return (resolvedSize ?: fallbackSize) to onDrawableLoaded
}

private fun DrawScope.drawNormalizedStrokes(
    strokes: List<ChatDrawingStroke>,
    activePoints: List<Offset> = emptyList(),
    activeColor: Color = Color.White,
    activeWidthNorm: Float = 0.012f,
) {
    fun drawSingleStroke(points: List<Offset>, color: Color, widthNorm: Float) {
        if (points.isEmpty() || size.width <= 0f || size.height <= 0f) return
        val canvasW = size.width
        val canvasH = size.height
        val strokeWidthPx = (widthNorm * canvasW).coerceAtLeast(2f)
        if (points.size == 1) {
            val pt = points[0]
            drawCircle(
                color = color,
                radius = strokeWidthPx / 2f,
                center = Offset(pt.x * canvasW, pt.y * canvasH),
            )
            return
        }
        val path = Path().apply {
            val first = points[0]
            moveTo(first.x * canvasW, first.y * canvasH)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f * canvasW
                val midY = (prev.y + curr.y) / 2f * canvasH
                quadraticTo(prev.x * canvasW, prev.y * canvasH, midX, midY)
            }
            val last = points.last()
            lineTo(last.x * canvasW, last.y * canvasH)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }

    strokes.forEach { stroke ->
        drawSingleStroke(
            points = stroke.points,
            color = Color(stroke.colorArgb),
            widthNorm = stroke.strokeWidthNorm,
        )
    }
    if (activePoints.isNotEmpty()) {
        drawSingleStroke(
            points = activePoints,
            color = activeColor,
            widthNorm = activeWidthNorm,
        )
    }
}

@Composable
private fun TransformedAttachmentImage(
    uri: Uri,
    contentDescription: String?,
    editState: ImageEditState,
    modifier: Modifier = Modifier,
    overlayContent: @Composable BoxScope.() -> Unit = {},
) {
    val (imageSize, onDrawableLoaded) = rememberResolvedImageSize(uri)
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val containerW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val geometry = remember(
            containerW,
            containerH,
            imageSize.width,
            imageSize.height,
            editState.rotationDegrees,
            editState.cropRectNorm,
        ) {
            ChatImageEditorHelper.computePreviewTransformGeometry(
                containerWidth = containerW,
                containerHeight = containerH,
                srcWidth = imageSize.width,
                srcHeight = imageSize.height,
                rotationDegrees = editState.rotationDegrees,
                cropRectNorm = editState.cropRectNorm,
            )
        }

        val viewportWDp = with(density) { geometry.viewportWidth.toDp() }
        val viewportHDp = with(density) { geometry.viewportHeight.toDp() }
        val unrotatedWDp = with(density) { geometry.unrotatedImageWidth.toDp() }
        val unrotatedHDp = with(density) { geometry.unrotatedImageHeight.toDp() }

        Box(
            modifier = Modifier
                .size(viewportWDp, viewportHDp)
                .clipToBounds()
                .testTag("attachment-transformed-viewport"),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = uri,
                contentDescription = contentDescription,
                contentScale = ContentScale.FillBounds,
                onSuccess = { success ->
                    onDrawableLoaded(
                        success.result.drawable.intrinsicWidth,
                        success.result.drawable.intrinsicHeight,
                    )
                },
                modifier = Modifier
                    .requiredSize(unrotatedWDp, unrotatedHDp)
                    .graphicsLayer {
                        translationX = geometry.translationX
                        translationY = geometry.translationY
                        rotationZ = geometry.normalizedRotationDegrees
                    },
            )
            overlayContent()
        }
    }
}

@Composable
private fun AttachmentMediaViewStage(
    draft: ChatAttachmentDraft,
    editState: ImageEditState,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (draft.kind == AttachmentKind.IMAGE) {
            TransformedAttachmentImage(
                uri = Uri.parse(draft.uri),
                contentDescription = draft.fileName,
                editState = editState,
            ) {
                if (editState.strokes.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawNormalizedStrokes(strokes = editState.strokes)
                    }
                }
            }
        } else if (draft.kind == AttachmentKind.VIDEO) {
            AsyncImage(
                model = Uri.parse(draft.uri),
                contentDescription = draft.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
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

private fun aspectRatioValue(ratioKey: String): Float? = when (ratioKey) {
    "1:1" -> 1f
    "4:5" -> 4f / 5f
    "16:9" -> 16f / 9f
    else -> null
}

@Composable
private fun AttachmentCropStage(
    draft: ChatAttachmentDraft,
    initialCrop: NormalizedCropRect,
    rotationDegrees: Float,
    onRotate: (NormalizedCropRect) -> Unit,
    onApplyCrop: (NormalizedCropRect) -> Unit,
    onCancel: () -> Unit,
) {
    val imageUri = remember(draft.uri) { Uri.parse(draft.uri) }
    val (imageSize, onDrawableLoaded) = rememberResolvedImageSize(imageUri)
    val density = LocalDensity.current

    var cropRect by remember { mutableStateOf(initialCrop) }
    var selectedRatio by remember { mutableStateOf("free") }
    var hasCustomDrag by remember { mutableStateOf(false) }

    val rotatedImageSize = remember(imageSize.width, imageSize.height, rotationDegrees) {
        ChatImageEditorHelper.computeRotatedDimensions(
            srcWidth = imageSize.width,
            srcHeight = imageSize.height,
            rotationDegrees = rotationDegrees,
        )
    }

    LaunchedEffect(rotatedImageSize) {
        val ratio = aspectRatioValue(selectedRatio)
        if (ratio != null && !hasCustomDrag) {
            cropRect = ChatImageEditorHelper.aspectRatioCropRect(
                targetAspectRatio = ratio,
                imageWidth = rotatedImageSize.width,
                imageHeight = rotatedImageSize.height,
            )
        }
    }

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
            val fittedRect = remember(widthPx, heightPx, imageSize.width, imageSize.height, rotationDegrees) {
                ChatImageEditorHelper.computeFittedImageRect(
                    containerWidth = widthPx,
                    containerHeight = heightPx,
                    imageWidth = imageSize.width,
                    imageHeight = imageSize.height,
                    rotationDegrees = rotationDegrees,
                )
            }

            val normalizedRot = ((rotationDegrees % 360f) + 360f) % 360f
            val isQuarterTurn = normalizedRot == 90f || normalizedRot == 270f
            val unrotatedFittedW = if (isQuarterTurn) fittedRect.height else fittedRect.width
            val unrotatedFittedH = if (isQuarterTurn) fittedRect.width else fittedRect.height

            // Base Image scaled to exact fitted bounds so rotated dimensions never overflow or misalign
            AsyncImage(
                model = imageUri,
                contentDescription = "برش",
                contentScale = ContentScale.FillBounds,
                onSuccess = { success ->
                    onDrawableLoaded(
                        success.result.drawable.intrinsicWidth,
                        success.result.drawable.intrinsicHeight,
                    )
                },
                modifier = Modifier
                    .requiredSize(
                        with(density) { unrotatedFittedW.toDp() },
                        with(density) { unrotatedFittedH.toDp() },
                    )
                    .graphicsLayer {
                        this.rotationZ = normalizedRot
                    },
            )

            val latestFittedRect by rememberUpdatedState(fittedRect)
            val latestRotatedSize by rememberUpdatedState(rotatedImageSize)
            val latestTargetAspectRatio by rememberUpdatedState(aspectRatioValue(selectedRatio))
            val handleRadiusPx = with(density) { 28.dp.toPx() }

            // Crop Overlay bounded strictly to fittedRect; uses 4 outer scrim rects (no BlendMode.Clear)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("attachment-crop-overlay")
                    .pointerInput(handleRadiusPx) {
                        var activeHandle = CropDragHandle.NONE
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val curFitted = latestFittedRect
                                val curCrop = cropRect
                                val curCropPx = Rect(
                                    left = curFitted.left + curCrop.left * curFitted.width,
                                    top = curFitted.top + curCrop.top * curFitted.height,
                                    right = curFitted.left + curCrop.right * curFitted.width,
                                    bottom = curFitted.top + curCrop.bottom * curFitted.height,
                                )
                                activeHandle = ChatImageEditorHelper.hitTestCropHandle(
                                    touchPoint = startOffset,
                                    cropPixelRect = curCropPx,
                                    handleRadiusPx = handleRadiusPx,
                                )
                            },
                            onDrag = { change, dragAmount ->
                                if (activeHandle == CropDragHandle.NONE) return@detectDragGestures
                                change.consume()
                                val curFitted = latestFittedRect
                                if (curFitted.width <= 0f || curFitted.height <= 0f) return@detectDragGestures
                                hasCustomDrag = true
                                val dxNorm = dragAmount.x / curFitted.width
                                val dyNorm = dragAmount.y / curFitted.height
                                cropRect = ChatImageEditorHelper.updateCropRectOnDrag(
                                    current = cropRect,
                                    handle = activeHandle,
                                    dxNorm = dxNorm,
                                    dyNorm = dyNorm,
                                    targetAspectRatio = latestTargetAspectRatio,
                                    imageWidth = latestRotatedSize.width,
                                    imageHeight = latestRotatedSize.height,
                                )
                            },
                            onDragEnd = { activeHandle = CropDragHandle.NONE },
                            onDragCancel = { activeHandle = CropDragHandle.NONE },
                        )
                    },
            ) {
                val left = fittedRect.left + cropRect.left * fittedRect.width
                val top = fittedRect.top + cropRect.top * fittedRect.height
                val right = fittedRect.left + cropRect.right * fittedRect.width
                val bottom = fittedRect.top + cropRect.bottom * fittedRect.height
                val cropW = (right - left).coerceAtLeast(1f)
                val cropH = (bottom - top).coerceAtLeast(1f)

                val scrimColor = Color.Black.copy(alpha = 0.6f)
                // Four non-overlapping outer scrim bands leaving crop interior completely untouched
                if (top > fittedRect.top) {
                    drawRect(
                        color = scrimColor,
                        topLeft = Offset(fittedRect.left, fittedRect.top),
                        size = Size(fittedRect.width, top - fittedRect.top),
                    )
                }
                if (fittedRect.bottom > bottom) {
                    drawRect(
                        color = scrimColor,
                        topLeft = Offset(fittedRect.left, bottom),
                        size = Size(fittedRect.width, fittedRect.bottom - bottom),
                    )
                }
                if (left > fittedRect.left) {
                    drawRect(
                        color = scrimColor,
                        topLeft = Offset(fittedRect.left, top),
                        size = Size(left - fittedRect.left, cropH),
                    )
                }
                if (fittedRect.right > right) {
                    drawRect(
                        color = scrimColor,
                        topLeft = Offset(right, top),
                        size = Size(fittedRect.right - right, cropH),
                    )
                }

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

                // Corner brackets + edge midpoint handles
                val cornerLen = min(18.dp.toPx(), min(cropW, cropH) * 0.35f)
                val cornerStroke = 3.dp.toPx()
                drawLine(Color.White, Offset(left, top), Offset(left + cornerLen, top), cornerStroke)
                drawLine(Color.White, Offset(left, top), Offset(left, top + cornerLen), cornerStroke)
                drawLine(Color.White, Offset(right, top), Offset(right - cornerLen, top), cornerStroke)
                drawLine(Color.White, Offset(right, top), Offset(right, top + cornerLen), cornerStroke)
                drawLine(Color.White, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerStroke)
                drawLine(Color.White, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerStroke)
                drawLine(Color.White, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerStroke)
                drawLine(Color.White, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerStroke)

                val edgeHalfLen = min(10.dp.toPx(), min(cropW, cropH) * 0.2f)
                val midX = (left + right) / 2f
                val midY = (top + bottom) / 2f
                drawLine(Color.White, Offset(midX - edgeHalfLen, top), Offset(midX + edgeHalfLen, top), cornerStroke)
                drawLine(Color.White, Offset(midX - edgeHalfLen, bottom), Offset(midX + edgeHalfLen, bottom), cornerStroke)
                drawLine(Color.White, Offset(left, midY - edgeHalfLen), Offset(left, midY + edgeHalfLen), cornerStroke)
                drawLine(Color.White, Offset(right, midY - edgeHalfLen), Offset(right, midY + edgeHalfLen), cornerStroke)
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
                        hasCustomDrag = false
                        cropRect = NormalizedCropRect(0f, 0f, 1f, 1f)
                    },
                )
                RatioChip(
                    title = "۱:۱",
                    isSelected = selectedRatio == "1:1",
                    onClick = {
                        selectedRatio = "1:1"
                        hasCustomDrag = false
                        cropRect = ChatImageEditorHelper.aspectRatioCropRect(
                            targetAspectRatio = 1f,
                            imageWidth = rotatedImageSize.width,
                            imageHeight = rotatedImageSize.height,
                        )
                    },
                )
                RatioChip(
                    title = "۴:۵",
                    isSelected = selectedRatio == "4:5",
                    onClick = {
                        selectedRatio = "4:5"
                        hasCustomDrag = false
                        cropRect = ChatImageEditorHelper.aspectRatioCropRect(
                            targetAspectRatio = 4f / 5f,
                            imageWidth = rotatedImageSize.width,
                            imageHeight = rotatedImageSize.height,
                        )
                    },
                )
                RatioChip(
                    title = "۱۶:۹",
                    isSelected = selectedRatio == "16:9",
                    onClick = {
                        selectedRatio = "16:9"
                        hasCustomDrag = false
                        cropRect = ChatImageEditorHelper.aspectRatioCropRect(
                            targetAspectRatio = 16f / 9f,
                            imageWidth = rotatedImageSize.width,
                            imageHeight = rotatedImageSize.height,
                        )
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
                    onClick = {
                        val rotatedCrop = ChatImageEditorHelper.rotateCropRectClockwise90(cropRect)
                        cropRect = rotatedCrop
                        selectedRatio = "free"
                        hasCustomDrag = false
                        onRotate(rotatedCrop)
                    },
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
    cropRectNorm: NormalizedCropRect,
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

    val latestSelectedColor by rememberUpdatedState(selectedColor)
    val latestSelectedWidthNorm by rememberUpdatedState(selectedWidthNorm)

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            TransformedAttachmentImage(
                uri = Uri.parse(draft.uri),
                contentDescription = "نقاشی",
                editState = ImageEditState(
                    rotationDegrees = rotationDegrees,
                    cropRectNorm = cropRectNorm,
                ),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val vpW = size.width.toFloat().coerceAtLeast(1f)
                                    val vpH = size.height.toFloat().coerceAtLeast(1f)
                                    currentPoints = listOf(
                                        Offset(
                                            (offset.x / vpW).coerceIn(0f, 1f),
                                            (offset.y / vpH).coerceIn(0f, 1f),
                                        ),
                                    )
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val vpW = size.width.toFloat().coerceAtLeast(1f)
                                    val vpH = size.height.toFloat().coerceAtLeast(1f)
                                    currentPoints = currentPoints + Offset(
                                        (change.position.x / vpW).coerceIn(0f, 1f),
                                        (change.position.y / vpH).coerceIn(0f, 1f),
                                    )
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        strokes = strokes + ChatDrawingStroke(
                                            colorArgb = latestSelectedColor.toArgb(),
                                            strokeWidthNorm = latestSelectedWidthNorm,
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
                    drawNormalizedStrokes(
                        strokes = strokes,
                        activePoints = currentPoints,
                        activeColor = selectedColor,
                        activeWidthNorm = selectedWidthNorm,
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
