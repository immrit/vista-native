package ir.coffevista.vista_native.features.chat.presentation.components

import android.icu.text.BreakIterator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.viewinterop.AndroidView
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.text.InputType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiSpan

/**
 * کنترلر فیلد ورودی پیام برای شکلک‌ها، صفحه‌کلید و عملکرد سریع
 */
class ChatComposerController {
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

typealias TelegramComposerController = ChatComposerController
/**
 * فیلد ورودی پیام با کارایی بالا، پشتیبانی کامل از متن فارسی/RTL و تعامل بدون تأخیر
 */
@Composable
fun ChatComposerField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusText: () -> Unit,
    controller: ChatComposerController,
    modifier: Modifier = Modifier,
    isEmojiPanelOpen: Boolean = false,
    focusRequester: FocusRequester? = null,
    hint: String = "پیام...",
    maxLines: Int = 6,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val actualFocusRequester = focusRequester ?: remember { FocusRequester() }
    // Resolve alignment from the first strong character.  `ContentOrRtl`
    // falls back to RTL while a fresh Latin draft is still being composed,
    // which makes English text look as if it starts in the middle.
    val firstStrongCharacter = value.text.firstOrNull { it.isLetterOrDigit() }
    val isRtlParagraph = firstStrongCharacter?.let { character ->
        character in '\u0590'..'\u08FF' || character in '\uFB1D'..'\uFEFC'
    } ?: true
    val latestValue = rememberUpdatedState(value)
    val latestOnValueChange = rememberUpdatedState(onValueChange)
    val latestKeyboardController = rememberUpdatedState(keyboardController)

    DisposableEffect(controller, actualFocusRequester) {
        controller.onInsertEmoji = { symbol ->
            val currentValue = latestValue.value
            val start = currentValue.selection.min
            val end = currentValue.selection.max
            val currentText = currentValue.text
            val newText = currentText.substring(0, start) + symbol + currentText.substring(end)
            val newCursor = start + symbol.length
            latestOnValueChange.value(TextFieldValue(newText, TextRange(newCursor)))
        }
        controller.onDispatchBackspace = {
            val currentValue = latestValue.value
            val start = currentValue.selection.min
            val end = currentValue.selection.max
            val currentText = currentValue.text
            if (start != end) {
                val newText = currentText.substring(0, start) + currentText.substring(end)
                latestOnValueChange.value(TextFieldValue(newText, TextRange(start)))
            } else if (start > 0) {
                val deleteStart = previousGraphemeStart(currentText, start)
                val newText = currentText.substring(0, deleteStart) + currentText.substring(start)
                latestOnValueChange.value(TextFieldValue(newText, TextRange(deleteStart)))
            }
        }
        controller.onRequestKeyboard = {
            actualFocusRequester.requestFocus()
            latestKeyboardController.value?.show()
        }
        controller.onHideKeyboard = {
            latestKeyboardController.value?.hide()
        }
        onDispose {
            controller.onInsertEmoji = null
            controller.onDispatchBackspace = null
            controller.onRequestKeyboard = null
            controller.onHideKeyboard = null
        }
    }

    // The parent Composer is RTL for the Persian UI.  Give the editable
    // paragraph its own physical layout direction as well as TextDirection;
    // otherwise a Latin paragraph can still inherit an RTL placement.
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtlParagraph) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
        val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f).toArgb()
        AndroidView(
            factory = { context ->
                EditText(context).apply {
                    background = null
                    setPadding(0, 0, 0, 0)
                    inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                    setSingleLine(false)
                    setMaxLines(maxLines)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                        override fun afterTextChanged(editable: Editable?) {
                            if (editable == null || this@apply.tag == true) return
                            VistaEmojiSpan.applySpans(context, editable)
                            onValueChange(
                                TextFieldValue(
                                    editable.toString(),
                                    TextRange(selectionStart.coerceAtLeast(0)),
                                ),
                            )
                        }
                    })
                }
            },
            modifier = modifier
                .focusRequester(actualFocusRequester)
                .onFocusChanged { if (it.isFocused && !isEmojiPanelOpen) onFocusText() }
                .semantics {
                    contentDescription = hint
                },
            update = { editText ->
                editText.hint = hint
                editText.textDirection = if (isRtlParagraph) android.view.View.TEXT_DIRECTION_RTL else android.view.View.TEXT_DIRECTION_LTR
                editText.setTextColor(textColor)
                editText.setHintTextColor(hintColor)
                if (editText.text.toString() != value.text) {
                    editText.tag = true
                    editText.setText(value.text)
                    editText.setSelection(value.selection.min.coerceIn(0, editText.text.length))
                    VistaEmojiSpan.applySpans(editText.context, editText.text)
                    editText.tag = false
                } else if (editText.selectionStart != value.selection.min) {
                    editText.setSelection(value.selection.min.coerceIn(0, editText.text.length))
                }
            },
        )
    }
}

private fun previousGraphemeStart(text: String, cursor: Int): Int {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    return iterator.preceding(cursor).takeIf { it != BreakIterator.DONE } ?: 0
}
