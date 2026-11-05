package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * نتیجه محاسبه چینش متن پیام و متادیتای فوتر (ساعت، تیک و ویرایش).
 */
internal data class ChatTextBubbleLayoutResult(
    val size: IntSize,
    val textOffset: IntOffset,
    val footerOffset: IntOffset,
    val isInline: Boolean,
)

/**
 * موتور خالص محاسبه موقعیت متن پیام و فوتر متادیتا.
 *
 * طبق درخواست طراحی، فوتر متادیتا (ساعت و وضعیت تیک) همواره در زیر محتوای متن پیام (در سطر پایین)
 * قرار می‌گیرد؛ چه در پیام‌های تک‌کلمه‌ای و کوتاه و چه در پیام‌های طولانی و چندسطری.
 */
internal fun calculateChatTextBubblePlacement(
    textWidth: Int,
    textHeight: Int,
    footerWidth: Int,
    footerHeight: Int,
    maxWidthPx: Int,
    spacingPx: Int = 8,
    verticalGapPx: Int = 2,
    layoutDirection: LayoutDirection = LayoutDirection.Rtl,
): ChatTextBubbleLayoutResult {
    val totalWidth = max(textWidth, footerWidth).coerceAtMost(maxWidthPx)
    val totalHeight = textHeight + verticalGapPx + footerHeight
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val textOffset = IntOffset(x = 0, y = 0)
    val footerOffset = if (isRtl) {
        IntOffset(x = 0, y = textHeight + verticalGapPx)
    } else {
        IntOffset(x = (totalWidth - footerWidth).coerceAtLeast(0), y = textHeight + verticalGapPx)
    }

    return ChatTextBubbleLayoutResult(
        size = IntSize(totalWidth, totalHeight),
        textOffset = textOffset,
        footerOffset = footerOffset,
        isInline = false,
    )
}

/**
 * لایوت سفارشی تک‌مسیره (Single-pass) برای حباب پیام متنی با چیدمان هوشمند متادیتا.
 */
@Composable
fun ChatTextBubbleLayout(
    text: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    verticalGap: Dp = 2.dp,
    layoutDirection: LayoutDirection = LocalLayoutDirection.current,
) {
    Layout(
        content = {
            Box(Modifier.layoutId("text")) { text() }
            Box(Modifier.layoutId("footer")) { footer() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val textMeasurable = measurables.first { it.layoutId == "text" }
        val footerMeasurable = measurables.first { it.layoutId == "footer" }

        val footerPlaceable = footerMeasurable.measure(Constraints())
        val textConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val textPlaceable = textMeasurable.measure(textConstraints)

        val spacingPx = spacing.roundToPx()
        val verticalGapPx = verticalGap.roundToPx()

        val placement = calculateChatTextBubblePlacement(
            textWidth = textPlaceable.width,
            textHeight = textPlaceable.height,
            footerWidth = footerPlaceable.width,
            footerHeight = footerPlaceable.height,
            maxWidthPx = constraints.maxWidth,
            spacingPx = spacingPx,
            verticalGapPx = verticalGapPx,
            layoutDirection = layoutDirection,
        )

        layout(placement.size.width, placement.size.height) {
            textPlaceable.placeRelative(placement.textOffset.x, placement.textOffset.y)
            footerPlaceable.placeRelative(placement.footerOffset.x, placement.footerOffset.y)
        }
    }
}
