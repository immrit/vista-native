package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

/**
 * کنترلر فیلد ورودی پیام برای شکلک‌ها، صفحه‌کلید و عملکرد سریع
 */
class TelegramComposerController {
    var onInsertEmoji: ((String) -> Unit)? = null
    var onDispatchBackspace: (() -> Unit)? = null
    var onRequestKeyboard: (() -> Unit)? = null
    var onHideKeyboard: (() -> Unit)? = null

    fun insertEmoji(symbol: String) {
        onInsertEmoji?.invoke(symbol)
    }

    fun dispatchBackspace() {
        onDispatchBackspace?.invoke()
    }

    fun openKeyboard() {
        onRequestKeyboard?.invoke()
    }

    fun closeKeyboard() {
        onHideKeyboard?.invoke()
    }
}
/**
 * فیلد ورودی پیام با کارایی بالا، پشتیبانی کامل از متن فارسی/RTL و تعامل بدون تأخیر
 */
@Composable
fun TelegramComposerField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusText: () -> Unit,
    controller: TelegramComposerController,
    modifier: Modifier = Modifier,
    isEmojiPanelOpen: Boolean = false,
    focusRequester: FocusRequester? = null,
    hint: String = "پیام...",
    maxLines: Int = 6,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val actualFocusRequester = focusRequester ?: remember { FocusRequester() }

    DisposableEffect(controller, value, onValueChange) {
        controller.onInsertEmoji = { symbol ->
            val start = value.selection.min
            val end = value.selection.max
            val currentText = value.text
            val newText = currentText.substring(0, start) + symbol + currentText.substring(end)
            val newCursor = start + symbol.length
            onValueChange(TextFieldValue(newText, TextRange(newCursor)))
        }
        controller.onDispatchBackspace = {
            val start = value.selection.min
            val end = value.selection.max
            val currentText = value.text
            if (start != end) {
                val newText = currentText.substring(0, start) + currentText.substring(end)
                onValueChange(TextFieldValue(newText, TextRange(start)))
            } else if (start > 0) {
                val deleteLength = if (start >= 2 && Character.isSurrogatePair(currentText[start - 2], currentText[start - 1])) 2 else 1
                val newText = currentText.substring(0, start - deleteLength) + currentText.substring(start)
                onValueChange(TextFieldValue(newText, TextRange(start - deleteLength)))
            }
        }
        controller.onRequestKeyboard = {
            actualFocusRequester.requestFocus()
            keyboardController?.show()
        }
        controller.onHideKeyboard = {
            keyboardController?.hide()
        }
        onDispose {
            controller.onInsertEmoji = null
            controller.onDispatchBackspace = null
            controller.onRequestKeyboard = null
            controller.onHideKeyboard = null
        }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .focusRequester(actualFocusRequester)
            .onFocusChanged { if (it.isFocused && !isEmojiPanelOpen) onFocusText() }
            .semantics {
                contentDescription = hint
            },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textDirection = TextDirection.ContentOrRtl,
            textAlign = TextAlign.Start,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrect = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default,
        ),
        maxLines = maxLines,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.text.isEmpty()) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Start,
                        ),
                    )
                }
                innerTextField()
            }
        },
    )
}
