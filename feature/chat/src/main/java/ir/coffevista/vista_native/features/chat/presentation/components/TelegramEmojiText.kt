package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.component.VistaEmoji
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiLookup
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiParser
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiSpan
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiText
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiTextSegment

typealias TelegramEmojiLookup = VistaEmojiLookup
typealias TelegramEmojiSpan = VistaEmojiSpan
typealias EmojiTextSegment = VistaEmojiTextSegment
typealias TelegramEmojiParser = VistaEmojiParser

@Composable
fun TelegramEmoji(
    emoji: String,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    contentDescription: String? = null,
) = VistaEmoji(emoji, modifier, size, contentDescription)

@Composable
fun TelegramEmojiText(
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
) = VistaEmojiText(
    text = text,
    modifier = modifier,
    style = style,
    emojiSize = emojiSize,
    textAlign = textAlign,
    overflow = overflow,
    softWrap = softWrap,
    maxLines = maxLines,
    highlightQuery = highlightQuery,
    highlightColor = highlightColor,
)
