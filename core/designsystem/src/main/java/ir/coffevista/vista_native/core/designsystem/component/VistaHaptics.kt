package ir.coffevista.vista_native.core.designsystem.component

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Lightweight, API-compatible haptic feedback helper bound to the current Android [View].
 * Respects system haptic settings (never forces `FLAG_IGNORE_GLOBAL_SETTING`).
 */
class VistaHaptics(private val view: View) {
    fun tap() = perform(HapticFeedbackConstants.CLOCK_TICK)

    fun confirm() {
        val constant = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.CONFIRM
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> HapticFeedbackConstants.CONTEXT_CLICK
            else -> HapticFeedbackConstants.VIRTUAL_KEY
        }
        perform(constant)
    }

    fun heavy() = perform(HapticFeedbackConstants.LONG_PRESS)

    private fun perform(constant: Int) {
        runCatching {
            view.performHapticFeedback(constant)
        }
    }
}

@Composable
fun rememberVistaHaptics(): VistaHaptics {
    val view = LocalView.current
    return remember(view) { VistaHaptics(view) }
}
