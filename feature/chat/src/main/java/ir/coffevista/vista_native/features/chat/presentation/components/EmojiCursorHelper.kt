package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.text.BreakIterator

/**
 * مدیریت کِرسر، درج و حذف هوشمند شکلک‌ها و کاراکترهای یونیکد منطبق بر استانداردهای تلگرام X
 * از BreakIterator استاندارد یونیکد (ICU) برای جلوگیری از قرار گرفتن کرسر در میانه جفت‌های جانشین (Surrogates)
 * و توالی‌های ZWJ، ایموجی‌های پرچم و ترکیب‌های چندبایتی استفاده می‌کند.
 */
object EmojiCursorHelper {

    /**
     * پیدا کردن مرز معتبر گرافیمی در متن و جلوگیری از افتادن کرسر در وسط ایموجی
     */
    fun snapToGraphemeBoundary(text: String, index: Int): Int {
        if (text.isEmpty() || index <= 0) return 0
        if (index >= text.length) return text.length

        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(text)
        if (iterator.isBoundary(index)) {
            return index
        }
        val preceding = iterator.preceding(index)
        val following = iterator.following(index)
        if (preceding == BreakIterator.DONE) return following.coerceAtMost(text.length)
        if (following == BreakIterator.DONE) return preceding.coerceAtLeast(0)

        return if (index - preceding <= following - index) preceding else following
    }

    /**
     * تراز کردن محدوده انتخابی متن (Selection / Cursor) با مرزهای واقعی ایموجی‌ها
     */
    fun snapSelection(text: String, selection: TextRange): TextRange {
        if (text.isEmpty()) return TextRange.Zero
        val safeStart = snapToGraphemeBoundary(text, selection.start)
        val safeEnd = if (selection.collapsed) safeStart else snapToGraphemeBoundary(text, selection.end)
        return TextRange(safeStart, safeEnd)
    }

    /**
     * درج ایموجی در محل دقیق کرسر و انتقال نشانگر به انتهای ایموجی درج‌شده
     */
    fun insertEmoji(currentValue: TextFieldValue, emoji: String): TextFieldValue {
        val text = currentValue.text
        val selection = snapSelection(text, currentValue.selection)
        val start = selection.min
        val end = selection.max
        val newText = text.substring(0, start) + emoji + text.substring(end)
        val newCursor = start + emoji.length
        return TextFieldValue(
            text = newText,
            selection = TextRange(newCursor),
        )
    }

    /**
     * حذف ایمن کاراکتر یا ایموجی قبل از نشانگر تایپ بدون تکه‌تکه شدن یونیکدها
     */
    fun deleteBeforeCursor(currentValue: TextFieldValue): TextFieldValue {
        val text = currentValue.text
        if (text.isEmpty()) return currentValue
        val selection = snapSelection(text, currentValue.selection)

        if (!selection.collapsed) {
            val start = selection.min
            val end = selection.max
            val newText = text.substring(0, start) + text.substring(end)
            return TextFieldValue(newText, TextRange(start))
        } else {
            val cursor = selection.start
            if (cursor <= 0) return currentValue
            val iterator = BreakIterator.getCharacterInstance()
            iterator.setText(text)
            val prevBoundary = iterator.preceding(cursor)
            val safeStart = if (prevBoundary != BreakIterator.DONE) prevBoundary else (cursor - 1).coerceAtLeast(0)
            val newText = text.substring(0, safeStart) + text.substring(cursor)
            return TextFieldValue(newText, TextRange(safeStart))
        }
    }
}
