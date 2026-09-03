package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

/** All values are physical pixels in the same dialog/window coordinate space. */
internal data class MessageContextMenuPlacement(
    val menu: Offset,
    val reactions: Offset?,
)

internal data class MessageContextMenuSafeInsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
)

/**
 * موتور محاسبه موقعیت منوی کانتکست و پنل واکنش‌ها (Reactions) به سبک تلگرام.
 *
 * تضمین‌های ساختاری و ریاضی:
 * ۱. منوی کانتکست و پنل ری‌اکشن به عنوان یک ساختار واحد عمودی محاسبه می‌شوند و تحت هیچ
 *    شرایطی (چه در بالای پیام و چه در پایین پیام) روی هم قرار نمی‌گیرند و تداخل نخواهند داشت.
 * ۲. در صورت قرارگیری در بالا: منو در بالا، ری‌اکشن در زیر منو (بالای حباب پیام).
 * ۳. در صورت قرارگیری در پایین: ری‌اکشن در زیر حباب پیام، منو در زیر ری‌اکشن.
 * ۴. هر دو عنصر به طور دقیق در محدوده امن سیستم و کیبورد (Safe Insets) مهار می‌شوند.
 */
internal fun calculateMessageContextMenuPlacement(
    anchor: Rect,
    viewport: IntSize,
    safeInsets: MessageContextMenuSafeInsets,
    isMine: Boolean,
    menuWidth: Float,
    menuHeight: Float,
    reactionsWidth: Float,
    reactionsHeight: Float,
    showReactions: Boolean,
    edgeGap: Float,
    elementGap: Float,
): MessageContextMenuPlacement {
    val minX = safeInsets.left + edgeGap
    val maxX = (viewport.width - safeInsets.right - edgeGap).coerceAtLeast(minX)

    // محاسبه افقی (X) برای منو
    val maxMenuX = (maxX - menuWidth).coerceAtLeast(minX)
    val rawMenuX = if (isMine) anchor.right - menuWidth else anchor.left
    val clampedMenuX = rawMenuX.coerceIn(minX, maxMenuX)

    val minY = safeInsets.top + edgeGap
    val maxY = (viewport.height - safeInsets.bottom - edgeGap).coerceAtLeast(minY)

    val totalStackHeight = if (showReactions) {
        reactionsHeight + elementGap + menuHeight
    } else {
        menuHeight
    }

    val spaceBelow = maxY - (anchor.bottom + elementGap)
    val spaceAbove = (anchor.top - elementGap) - minY

    // تصمیم‌گیری درباره قرارگیری پشته در بالا یا پایین حباب پیام
    val placeBelow = if (spaceBelow >= totalStackHeight) {
        true
    } else if (spaceAbove >= totalStackHeight) {
        false
    } else {
        // در صورت عدم گنجایش کامل در هر دو طرف، طرفی با فضای بیشتر انتخاب می‌شود
        spaceBelow >= spaceAbove
    }

    var finalMenuY: Float
    var finalReactionsY: Float? = null

    if (placeBelow) {
        if (showReactions) {
            var reactionsY = anchor.bottom + elementGap
            var menuY = reactionsY + reactionsHeight + elementGap

            // اگر انتهای منو از حد پایین تجاوز کند، کل پشته با هم به بالا شیفت داده می‌شود
            val bottomOverflow = (menuY + menuHeight) - maxY
            if (bottomOverflow > 0f) {
                reactionsY -= bottomOverflow
                menuY -= bottomOverflow
            }

            // اطمینان از عدم خروج بالای پشته از حد بالای صفحه
            val topOverflow = minY - reactionsY
            if (topOverflow > 0f) {
                reactionsY += topOverflow
                menuY += topOverflow
            }

            finalReactionsY = reactionsY
            finalMenuY = menuY
        } else {
            var menuY = anchor.bottom + elementGap
            val bottomOverflow = (menuY + menuHeight) - maxY
            if (bottomOverflow > 0f) {
                menuY -= bottomOverflow
            }
            finalMenuY = menuY.coerceAtLeast(minY)
        }
    } else {
        // قرارگیری در بالای حباب پیام
        if (showReactions) {
            var reactionsY = anchor.top - elementGap - reactionsHeight
            var menuY = reactionsY - elementGap - menuHeight

            // اگر بالای منو از حد بالای صفحه تجاوز کند، کل پشته با هم به پایین شیفت داده می‌شود
            val topOverflow = minY - menuY
            if (topOverflow > 0f) {
                menuY += topOverflow
                reactionsY += topOverflow
            }

            // اطمینان از عدم خروج پایین پشته از حد پایین صفحه
            val bottomOverflow = (reactionsY + reactionsHeight) - maxY
            if (bottomOverflow > 0f) {
                menuY -= bottomOverflow
                reactionsY -= bottomOverflow
            }

            finalReactionsY = reactionsY
            finalMenuY = menuY
        } else {
            var menuY = anchor.top - elementGap - menuHeight
            val topOverflow = minY - menuY
            if (topOverflow > 0f) {
                menuY += topOverflow
            }
            finalMenuY = menuY.coerceAtMost((maxY - menuHeight).coerceAtLeast(minY))
        }
    }

    val reactionsOffset = if (showReactions && finalReactionsY != null) {
        val maxReactionsX = (maxX - reactionsWidth).coerceAtLeast(minX)
        val rawReactionsX = if (isMine) anchor.right - reactionsWidth else anchor.left
        val clampedReactionsX = rawReactionsX.coerceIn(minX, maxReactionsX)
        Offset(clampedReactionsX, finalReactionsY)
    } else {
        null
    }

    return MessageContextMenuPlacement(
        menu = Offset(clampedMenuX, finalMenuY),
        reactions = reactionsOffset,
    )
}
