package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Stable
class VistaFullscreenOverlayController {
    internal val entries = mutableStateListOf<OverlayEntry>()

    internal data class OverlayEntry(
        val id: Any,
        val content: @Composable () -> Unit,
    )

    fun set(id: Any, content: @Composable () -> Unit) {
        val index = entries.indexOfFirst { it.id === id }
        if (index >= 0) {
            entries[index] = OverlayEntry(id, content)
        } else {
            entries.add(OverlayEntry(id, content))
        }
    }

    fun remove(id: Any) {
        entries.removeAll { it.id === id }
    }
}

val LocalVistaFullscreenOverlay = staticCompositionLocalOf<VistaFullscreenOverlayController?> { null }

@Composable
fun VistaFullscreenOverlayHost(
    controller: VistaFullscreenOverlayController,
    modifier: Modifier = Modifier,
) {
    if (controller.entries.isEmpty()) return
    Box(modifier = modifier.fillMaxSize()) {
        controller.entries.forEach { entry ->
            key(entry.id) {
                entry.content()
            }
        }
    }
}

/**
 * Renders fullscreen viewer content directly in the activity's edge-to-edge root window via
 * [LocalVistaFullscreenOverlay] when hosted, falling back to a [Dialog] in isolated tests/previews.
 */
@Composable
fun VistaFullscreenSurface(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val overlayController = LocalVistaFullscreenOverlay.current
    if (overlayController != null) {
        val overlayId = remember { Any() }
        val latestContent by rememberUpdatedState(content)
        SideEffect {
            overlayController.set(overlayId) {
                latestContent()
            }
        }
        DisposableEffect(overlayController, overlayId) {
            onDispose {
                overlayController.remove(overlayId)
            }
        }
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            content()
        }
    }
}
