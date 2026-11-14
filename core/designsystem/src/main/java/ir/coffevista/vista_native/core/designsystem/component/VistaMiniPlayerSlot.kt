package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.glass.LocalVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassState

@Immutable
data class VistaMiniPlayerSlot(
    val extraHeight: Dp = 0.dp,
    val content: @Composable (glassState: VistaGlassState?, modifier: Modifier, shape: Shape) -> Unit = { _, _, _ -> },
)

val LocalMiniPlayerSlot = compositionLocalOf { VistaMiniPlayerSlot() }

/**
 * میزبان مینی‌پلیر سراسری برای قرارگیری یکپارچه زیر اپ‌بار هر صفحه،
 * به‌طوری که ارتفاع اپ‌بار به‌صورت طبیعی باز شود و محتوای صفحه زیر نوار نرود.
 */
@Composable
fun VistaMiniPlayerHost(
    glassState: VistaGlassState? = LocalVistaGlassState.current,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
) {
    LocalMiniPlayerSlot.current.content(glassState, modifier, shape)
}
