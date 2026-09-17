package ir.coffevista.vista_native.features.chat.presentation.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.AsyncImage
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import ir.coffevista.vista_native.features.chat.presentation.MAX_ATTACHMENT_CAPTION_LENGTH
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatAttachmentReviewSheet(
    drafts: List<ChatAttachmentDraft>,
    onDismiss: () -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onSend: (String) -> Unit,
) {
    if (drafts.isEmpty()) return
    var selectedUri by rememberSaveable { mutableStateOf(drafts.first().uri) }
    var caption by rememberSaveable { mutableStateOf("") }
    val selectedIndex = drafts.indexOfFirst { it.uri == selectedUri }
    val safeIndex = selectedIndex.takeIf { it >= 0 } ?: 0
    val selected = drafts[safeIndex]
    val isMedia = selected.kind in MEDIA_KINDS
    val layoutDirection = LocalLayoutDirection.current
    val dragStepPx = with(LocalDensity.current) { 80.dp.toPx() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("attachment-review-sheet"),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (drafts.size > 1) "پیش‌نمایش آلبوم" else "پیش‌نمایش پیوست",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (drafts.size > 1) {
                    Text((safeIndex + 1).toString() + "/" + drafts.size)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "بستن پیش‌نمایش")
                }
            }
            Box(
                Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (isMedia) Color.Black else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (isMedia) {
                    AsyncImage(
                        model = Uri.parse(selected.uri),
                        contentDescription = "پیش‌نمایش " + selected.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (selected.kind == AttachmentKind.VIDEO) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "ویدئو",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape).padding(12.dp),
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(Icons.Default.Description, null, Modifier.size(56.dp), MaterialTheme.colorScheme.primary)
                        Text(
                            selected.fileName,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(formatAttachmentBytes(selected.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                itemsIndexed(drafts, key = { _, draft -> draft.uri }) { index, draft ->
                    var accumulatedDrag by remember(draft.uri, index) { mutableStateOf(0f) }
                    Box(
                        Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                if (index == safeIndex) 2.dp else 0.dp,
                                if (index == safeIndex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(12.dp),
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
                                    } else {
                                        null
                                    },
                                    if (index < drafts.lastIndex) {
                                        CustomAccessibilityAction("انتقال به جایگاه بعدی") {
                                            onMove(index, index + 1)
                                            true
                                        }
                                    } else {
                                        null
                                    },
                                )
                            }
                            .testTag("attachment-review-thumb:" + index),
                    ) {
                        if (draft.kind in MEDIA_KINDS) {
                            AsyncImage(
                                Uri.parse(draft.uri),
                                draft.fileName,
                                Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Default.Description,
                                draft.fileName,
                                Modifier.align(Alignment.Center).size(30.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                                .background(Color.Black.copy(alpha = 0.68f), CircleShape)
                                .testTag("attachment-review-remove:" + index),
                        ) {
                            Icon(Icons.Default.Close, "حذف " + draft.fileName, Modifier.size(16.dp), Color.White)
                        }
                        if (drafts.size > 1) {
                            Text(
                                (index + 1).toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomStart)
                                    .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "نگه‌داشتن برای جابه‌جایی " + draft.fileName,
                                tint = Color.White,
                                modifier = Modifier.align(Alignment.BottomEnd).size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.62f), CircleShape)
                                    .padding(4.dp),
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it.take(MAX_ATTACHMENT_CAPTION_LENGTH) },
                modifier = Modifier.fillMaxWidth().testTag("attachment-review-caption"),
                minLines = 1,
                maxLines = 3,
                label = { Text("کپشن") },
                supportingText = {
                    Text(
                        caption.length.toString() + "/" + MAX_ATTACHMENT_CAPTION_LENGTH,
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                },
            )
            Button(
                onClick = { onSend(caption) },
                modifier = Modifier.fillMaxWidth().testTag("attachment-review-send"),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null)
                Spacer(Modifier.width(8.dp))
                Text(if (drafts.size > 1) "ارسال " + drafts.size + " فایل" else "ارسال")
            }
        }
    }
}

private val MEDIA_KINDS = setOf(AttachmentKind.IMAGE, AttachmentKind.VIDEO, AttachmentKind.GIF)

private fun formatAttachmentBytes(bytes: Long): String = when {
    bytes < 1024 -> bytes.toString() + " B"
    bytes < 1024 * 1024 -> "%.1f KB".format(Locale.US, bytes / 1024f)
    else -> "%.1f MB".format(Locale.US, bytes / (1024f * 1024f))
}
