package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class EmojiTextSegment(
    val content: String,
    val isEmoji: Boolean,
    val assetUrl: String? = null,
)

object TelegramEmojiParser {
    fun parseSegments(text: String): List<EmojiTextSegment> {
        if (text.isEmpty()) return emptyList()
        val segments = mutableListOf<EmojiTextSegment>()
        var i = 0
        var plainBuffer = StringBuilder()

        while (i < text.length) {
            val c = text[i]
            // سریع‌ترین میان‌بر: کاراکترهای اسکی و فارسی و اعداد معمولی قطعاً ایموجی نیستند
            if (c.code < 0x2000 && c != '#' && c != '*' && (c < '0' || c > '9')) {
                plainBuffer.append(c)
                i++
                continue
            }

            // بررسی احتمال تطابق با ایموجی تلگرام
            val maxLen = (text.length - i).coerceAtMost(16)
            var matchedEmoji: String? = null
            var matchedAsset: String? = null
            var matchedLen = 0

            for (len in maxLen downTo 1) {
                val candidate = text.substring(i, i + len)
                val asset = TelegramEmojiLookup.getAssetUrl(candidate)
                if (asset != null) {
                    matchedEmoji = candidate
                    matchedAsset = asset
                    matchedLen = len
                    break
                }
            }

            if (matchedEmoji != null && matchedAsset != null) {
                if (plainBuffer.isNotEmpty()) {
                    segments.add(EmojiTextSegment(plainBuffer.toString(), isEmoji = false))
                    plainBuffer = StringBuilder()
                }
                segments.add(EmojiTextSegment(matchedEmoji, isEmoji = true, assetUrl = matchedAsset))
                i += matchedLen
            } else {
                val codePoint = text.codePointAt(i)
                val charCount = Character.charCount(codePoint)
                plainBuffer.append(text.substring(i, i + charCount))
                i += charCount
            }
        }

        if (plainBuffer.isNotEmpty()) {
            segments.add(EmojiTextSegment(plainBuffer.toString(), isEmoji = false))
        }

        return segments
    }
}

/**
 * کامپوننت نمایش متن با پشتیبانی کامل از رندر ایموجی‌های گرافیکی اختصاصی تلگرام (Apple-Style)
 */
@Composable
fun TelegramEmojiText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    emojiSize: Dp = 18.dp,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    val context = LocalContext.current
    remember { TelegramEmojiLookup.initialize(context) }

    val segments = remember(text) { TelegramEmojiParser.parseSegments(text) }
    val hasEmojis = remember(segments) { segments.any { it.isEmoji } }

    if (!hasEmojis) {
        Text(
            text = text,
            modifier = modifier,
            style = style,
            textAlign = textAlign,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
        )
        return
    }

    val annotatedString = remember(segments) {
        AnnotatedString.Builder().apply {
            segments.forEachIndexed { index, segment ->
                if (segment.isEmoji) {
                    appendInlineContent("emoji_$index", segment.content)
                } else {
                    append(segment.content)
                }
            }
        }.toAnnotatedString()
    }

    val inlineContent = remember(segments, emojiSize) {
        val map = mutableMapOf<String, InlineTextContent>()
        segments.forEachIndexed { index, segment ->
            if (segment.isEmoji) {
                map["emoji_$index"] = InlineTextContent(
                    Placeholder(
                        width = (emojiSize.value + 3.0f).sp,
                        height = emojiSize.value.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1.5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TelegramEmoji(
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
