package ir.coffevista.vista_native.core.designsystem.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Spannable
import android.text.style.ReplacementSpan
import android.util.JsonReader
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * مدیریت نگاشت شکلک‌ها به فایل‌های تصویری گرافیکی محلی ویستا (Apple/Telegram Style)
 */
object VistaEmojiLookup {
    private val emojiToAsset = mutableMapOf<String, String>()
    @Volatile
    private var isLoaded = false

    fun initialize(context: Context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            try {
                context.assets.open("emoji/modern_emoji_map.json").use { inputStream ->
                    val reader = JsonReader(inputStream.bufferedReader())
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val name = reader.nextName()
                        if (name == "map") {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val emoji = reader.nextName()
                                val path = reader.nextString()
                                val fileName = path.substringAfterLast("/")
                                val assetUrl = "file:///android_asset/emoji/modern/$fileName"
                                emojiToAsset[emoji] = assetUrl

                                val withoutVs16 = emoji.replace("\uFE0F", "")
                                if (withoutVs16 != emoji) {
                                    emojiToAsset.putIfAbsent(withoutVs16, assetUrl)
                                }
                            }
                            reader.endObject()
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                    reader.close()
                }
                isLoaded = true
            } catch (e: Exception) {
                isLoaded = true
            }
        }
    }

    fun getAssetUrl(emoji: String): String? {
        val direct = emojiToAsset[emoji]
        if (direct != null) return direct

        val withoutVs16 = emoji.replace("\uFE0F", "")
        return emojiToAsset[withoutVs16]
    }
}

/**
 * اسپان جایگزین گرافیکی برای رندر شکلک در Editable و Spannable
 */
class VistaEmojiSpan(
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
            VistaEmojiLookup.initialize(context)
            val assetUrl = VistaEmojiLookup.getAssetUrl(emoji) ?: return null
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
                val existing = spannable.getSpans(0, spannable.length, VistaEmojiSpan::class.java)
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
                val existing = spannable.getSpans(0, spannable.length, VistaEmojiSpan::class.java)
                for (span in existing) {
                    spannable.removeSpan(span)
                }
                return
            }

            val segments = VistaEmojiParser.parseSegments(text)
            val existingSpans = spannable.getSpans(0, spannable.length, VistaEmojiSpan::class.java)

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
                            VistaEmojiSpan(context, segment.content, sizePx),
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

data class VistaEmojiTextSegment(
    val content: String,
    val isEmoji: Boolean,
    val assetUrl: String? = null,
)

object VistaEmojiParser {
    fun parseSegments(text: String): List<VistaEmojiTextSegment> {
        if (text.isEmpty()) return emptyList()
        val segments = mutableListOf<VistaEmojiTextSegment>()
        var i = 0
        var plainBuffer = StringBuilder()

        while (i < text.length) {
            val c = text[i]
            if (c.code < 0x2000 && c != '#' && c != '*' && (c < '0' || c > '9')) {
                plainBuffer.append(c)
                i++
                continue
            }

            val maxLen = (text.length - i).coerceAtMost(16)
            var matchedEmoji: String? = null
            var matchedAsset: String? = null
            var matchedLen = 0

            for (len in maxLen downTo 1) {
                val candidate = text.substring(i, i + len)
                val asset = VistaEmojiLookup.getAssetUrl(candidate)
                if (asset != null) {
                    matchedEmoji = candidate
                    matchedAsset = asset
                    matchedLen = len
                    break
                }
            }

            if (matchedEmoji != null && matchedAsset != null) {
                if (plainBuffer.isNotEmpty()) {
                    segments.add(VistaEmojiTextSegment(plainBuffer.toString(), isEmoji = false))
                    plainBuffer = StringBuilder()
                }
                segments.add(VistaEmojiTextSegment(matchedEmoji, isEmoji = true, assetUrl = matchedAsset))
                i += matchedLen
            } else {
                val codePoint = text.codePointAt(i)
                val charCount = Character.charCount(codePoint)
                plainBuffer.append(text.substring(i, i + charCount))
                i += charCount
            }
        }

        if (plainBuffer.isNotEmpty()) {
            segments.add(VistaEmojiTextSegment(plainBuffer.toString(), isEmoji = false))
        }

        return segments
    }
}

/**
 * کامپوننت نمایش یک شکلک منفرد با تصویر گرافیکی باکیفیت محلی
 */
@Composable
fun VistaEmoji(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = remember(size, density) { with(density) { size.roundToPx() } }
    val bitmap = remember(emoji, sizePx) {
        VistaEmojiSpan.getBitmap(context, emoji, sizePx)
    }

    if (bitmap != null) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription ?: emoji,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emoji,
                fontSize = (size.value * 0.85f).sp,
            )
        }
    }
}

/**
 * کامپوننت جامع رندر متن در سراسر اپلیکیشن با پشتیبانی از شکلک‌های بومی گرافیکی محلی ویستا (Apple/Telegram Style)
 */
@Composable
fun VistaEmojiText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    emojiSize: Dp = if (style.fontSize.isSp && style.fontSize.value > 0) (style.fontSize.value * 1.15f).dp else 18.dp,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    highlightQuery: String? = null,
    highlightColor: Color = Color.Transparent,
) {
    val context = LocalContext.current
    remember { VistaEmojiLookup.initialize(context) }

    val segments = remember(text) { VistaEmojiParser.parseSegments(text) }
    val hasEmojis = remember(segments) { segments.any { it.isEmoji } }

    if (!hasEmojis) {
        Text(
            text = AnnotatedString(text).withSearchHighlights(highlightQuery, highlightColor),
            modifier = modifier,
            style = style,
            textAlign = textAlign,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
        )
        return
    }

    val annotatedString = remember(segments, highlightQuery, highlightColor) {
        AnnotatedString.Builder().apply {
            segments.forEachIndexed { index, segment ->
                if (segment.isEmoji) {
                    appendInlineContent("emoji_$index", segment.content)
                } else {
                    append(segment.content)
                }
            }
        }.toAnnotatedString().withSearchHighlights(highlightQuery, highlightColor)
    }

    val inlineContent = remember(segments, emojiSize) {
        val map = mutableMapOf<String, InlineTextContent>()
        segments.forEachIndexed { index, segment ->
            if (segment.isEmoji) {
                map["emoji_$index"] = InlineTextContent(
                    Placeholder(
                        width = (emojiSize.value + 2.5f).sp,
                        height = emojiSize.value.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        VistaEmoji(
                            emoji = segment.content,
                            size = emojiSize,
                        )
                    }
                }
            }
        }
        map
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        inlineContent = inlineContent,
    )
}

private fun AnnotatedString.withSearchHighlights(
    query: String?,
    color: Color,
): AnnotatedString {
    val needle = query?.trim().orEmpty()
    if (needle.isEmpty() || color == Color.Transparent) return this

    val builder = AnnotatedString.Builder().apply { append(this@withSearchHighlights) }
    var start = text.indexOf(needle, ignoreCase = true)
    while (start >= 0) {
        builder.addStyle(SpanStyle(background = color), start, start + needle.length)
        start = text.indexOf(needle, startIndex = start + needle.length, ignoreCase = true)
    }
    return builder.toAnnotatedString()
}
