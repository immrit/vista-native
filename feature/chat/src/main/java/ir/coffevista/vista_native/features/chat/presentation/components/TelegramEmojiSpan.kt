package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Editable
import android.text.Spannable
import android.text.style.ReplacementSpan
import android.util.LruCache

/**
 * ReplacementSpan اختصاصی برای رندر دقیق و اصیل شکلک‌های گرافیکی تلگرام (Apple-Style) در داخل EditText
 * با فرمول ابعاد و موقعیت‌یابی دقیق برگرفته از مخزن اصلی Telegram و Telegram X
 */
class TelegramEmojiSpan(
    private val context: Context,
    val emoji: String,
    val sizePx: Int = 0,
) : ReplacementSpan() {

    private var cachedBitmap: Bitmap? = null

    init {
        val targetSize = if (sizePx > 0) sizePx else 48
        cachedBitmap = getBitmap(context, emoji, targetSize)
    }

    companion object {
        private val bitmapCache = LruCache<String, Bitmap>(600)

        fun getBitmap(context: Context, emoji: String, sizePx: Int): Bitmap? {
            TelegramEmojiLookup.initialize(context)
            val assetUrl = TelegramEmojiLookup.getAssetUrl(emoji) ?: return null
            val cacheKey = "$assetUrl-$sizePx"
            bitmapCache.get(cacheKey)?.let { return it }

            return try {
                val relativePath = assetUrl.removePrefix("file:///android_asset/")
                context.assets.open(relativePath).use { stream ->
                    val original = BitmapFactory.decodeStream(stream) ?: return null
                    val scaled = if (sizePx > 0 && (original.width != sizePx || original.height != sizePx)) {
                        Bitmap.createScaledBitmap(original, sizePx, sizePx, true)
                    } else {
                        original
                    }
                    bitmapCache.put(cacheKey, scaled)
                    scaled
                }
            } catch (e: Exception) {
                null
            }
        }

        fun applySpans(context: Context, spannable: Spannable, sizePx: Int = 0) {
            val text = spannable.toString()
            if (text.isEmpty()) {
                val existing = spannable.getSpans(0, spannable.length, TelegramEmojiSpan::class.java)
                for (span in existing) {
                    spannable.removeSpan(span)
                }
                return
            }

            var hasPotentialEmoji = false
            for (i in text.indices) {
                val c = text[i]
                if (c.code >= 0x2000 || c == '#' || c == '*' || (c in '0'..'9')) {
                    hasPotentialEmoji = true
                    break
                }
            }
            if (!hasPotentialEmoji) {
                val existing = spannable.getSpans(0, spannable.length, TelegramEmojiSpan::class.java)
                for (span in existing) {
                    spannable.removeSpan(span)
                }
                return
            }

            val segments = TelegramEmojiParser.parseSegments(text)
            val existingSpans = spannable.getSpans(0, spannable.length, TelegramEmojiSpan::class.java)

            var offset = 0
            var matchingCount = 0
            var totalEmojiCount = 0

            for (seg in segments) {
                val segLen = seg.content.length
                if (seg.isEmoji) {
                    totalEmojiCount++
                    val start = offset
                    val end = offset + segLen
                    val alreadyMatched = existingSpans.any { s ->
                        spannable.getSpanStart(s) == start &&
                        spannable.getSpanEnd(s) == end &&
                        s.emoji == seg.content
                    }
                    if (alreadyMatched) {
                        matchingCount++
                    }
                }
                offset += segLen
            }

            // اگر تمام اسپن‌ها قبلاً دقیقاً در جای خودشان قرار دارند، نیازی به دستکاری نیست
            if (matchingCount == totalEmojiCount && existingSpans.size == totalEmojiCount) {
                return
            }

            for (span in existingSpans) {
                spannable.removeSpan(span)
            }

            var currentOffset = 0
            for (segment in segments) {
                val segLength = segment.content.length
                if (segment.isEmoji) {
                    val start = currentOffset
                    val end = currentOffset + segLength
                    if (start >= 0 && end <= spannable.length && start < end) {
                        spannable.setSpan(
                            TelegramEmojiSpan(context, segment.content, sizePx),
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                }
                currentOffset += segLength
            }
        }
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        val emojiSize = if (sizePx > 0) sizePx else paint.textSize.toInt()

        if (fm != null) {
            val fontHeight = fm.descent - fm.ascent
            if (fontHeight < emojiSize) {
                val diff = emojiSize - fontHeight
                fm.ascent -= diff / 2
                fm.top -= diff / 2
                fm.descent += diff / 2
                fm.bottom += diff / 2
            }
        }
        return emojiSize
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val emojiSize = if (sizePx > 0) sizePx else paint.textSize.toInt()
        val bitmap = cachedBitmap ?: getBitmap(context, emoji, emojiSize)
        if (bitmap != null) {
            val fontMetrics = paint.fontMetricsInt
            val textCenter = y + (fontMetrics.descent + fontMetrics.ascent) / 2
            val drawTop = textCenter - emojiSize / 2
            val destRect = Rect(
                x.toInt(),
                drawTop,
                (x + emojiSize).toInt(),
                drawTop + emojiSize,
            )
            canvas.drawBitmap(bitmap, null, destRect, paint)
        }
    }
}
