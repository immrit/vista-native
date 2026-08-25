package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView

/**
 * کنترلر مستقیم فیلد ورودی متن بومی بر پایه معماری کلاس EditTextEmoji در مخزن اصلی Telegram
 * تمامی عملیات درج شکلک و حذف بدون وابستگی به بازچینی‌های سنگین کامپوز انجام می‌شوند
 */
class TelegramComposerController {
    var internalEditText: EditText? = null
    var isUpdatingText = false

    fun insertEmoji(symbol: String) {
        val editText = internalEditText ?: return
        try {
            val start = editText.selectionStart.coerceAtLeast(0)
            val end = editText.selectionEnd.coerceAtLeast(0)
            val selStart = Math.min(start, end)
            val selEnd = Math.max(start, end)
            val text = editText.text
            if (text != null) {
                // Keep the editable surface Unicode-only. Constructing custom spans
                // can synchronously decode assets, which must never happen while the
                // IME is opening or a user is entering text.
                text.replace(selStart, selEnd, symbol)
                val newCursor = selStart + symbol.length
                editText.setSelection(newCursor.coerceIn(0, text.length))
            }
        } catch (e: Exception) {
            try {
                val curText = editText.text?.toString() ?: ""
                val selStart = editText.selectionStart.coerceIn(0, curText.length)
                val selEnd = editText.selectionEnd.coerceIn(0, curText.length)
                val minS = Math.min(selStart, selEnd)
                val maxS = Math.max(selStart, selEnd)
                val newText = curText.substring(0, minS) + symbol + curText.substring(maxS)
                editText.setText(newText)
                editText.setSelection((minS + symbol.length).coerceIn(0, newText.length))
            } catch (_: Exception) {}
        }
    }

    fun dispatchBackspace() {
        val editText = internalEditText ?: return
        try {
            editText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            editText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        } catch (_: Exception) {}
    }

    fun openKeyboard() {
        val editText = internalEditText ?: return
        // Wait until the AndroidView has completed its current layout pass. Asking
        // the IME to attach while Compose is also applying window insets causes a
        // visible first-focus hitch on slower physical devices.
        editText.post {
            if (!editText.hasFocus()) editText.requestFocus()
            val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun closeKeyboard() {
        val editText = internalEditText ?: return
        val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(editText.windowToken, 0)
    }
}

/**
 * فیلد ورودی متن پیشرفته با پشتیبانی کامل از رندر مستقیم شکلک‌های گرافیکی تلگرام (Apple-Style)
 * و سوییچ کاملاً خودکار، بدون پرش و بدون تغییر مختصات عمودی (الگوی رسمی Telegram)
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
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f).toArgb()

    AndroidView(
        modifier = modifier,
        factory = { ctx: Context ->
            EditText(ctx).apply {
                controller.internalEditText = this
                background = null
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                textDirection = View.TEXT_DIRECTION_ANY_RTL
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTextColor(textColor)
                setHintTextColor(hintColor)
                this.hint = hint
                this.maxLines = maxLines
                isSingleLine = false
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

                setPadding(0, 0, 0, 0)
                includeFontPadding = false

                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus && !isEmojiPanelOpen) {
                        onFocusText()
                    }
                }

                addTextChangedListener(object : TextWatcher {
                    private var isFormatting = false

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable?) {
                        if (controller.isUpdatingText || isFormatting || s == null) return
                        isFormatting = true
                        try {
                            val newText = s.toString()
                            val curStart = selectionStart.coerceIn(0, newText.length)
                            val curEnd = selectionEnd.coerceIn(0, newText.length)
                            if (newText != value.text || curStart != value.selection.start || curEnd != value.selection.end) {
                                onValueChange(TextFieldValue(newText, TextRange(curStart, curEnd)))
                            }
                        } catch (_: Exception) {
                        } finally {
                            isFormatting = false
                        }
                    }
                })
            }
        },
        update = { editText: EditText ->
            controller.internalEditText = editText
            editText.setTextColor(textColor)
            editText.setHintTextColor(hintColor)

            val currentText = editText.text?.toString() ?: ""
            if (currentText != value.text) {
                controller.isUpdatingText = true
                try {
                    // Keep the editor as plain Unicode text. Applying custom emoji
                    // spans here reparsed the entire draft and synchronously decoded
                    // assets on every keystroke. Rich emoji rendering is retained in
                    // the read-only message bubbles, where it does not block IME input.
                    editText.setText(value.text)
                    val safeStart = value.selection.start.coerceIn(0, value.text.length)
                    val safeEnd = value.selection.end.coerceIn(0, value.text.length)
                    editText.setSelection(safeStart, safeEnd)
                } catch (_: Exception) {
                } finally {
                    controller.isUpdatingText = false
                }
            } else {
                val curStart = editText.selectionStart
                val curEnd = editText.selectionEnd
                val targetStart = value.selection.start.coerceIn(0, value.text.length)
                val targetEnd = value.selection.end.coerceIn(0, value.text.length)
                if (curStart >= 0 && curEnd >= 0 && (curStart != targetStart || curEnd != targetEnd)) {
                    controller.isUpdatingText = true
                    try {
                        editText.setSelection(targetStart, targetEnd)
                    } catch (_: Exception) {
                    } finally {
                        controller.isUpdatingText = false
                    }
                }
            }
        },
    )
}
