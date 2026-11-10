package ir.coffevista.vista_native.core.designsystem.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Dispatches Vista internal web/deep links (`/game`, `/post`, `/profile`, `/group`,
 * `vista://...`) directly inside native shell routes rather than escaping to
 * the external browser. Returns true if the URL was handled in-app.
 */
fun interface VistaInternalLinkNavigator {
    fun openUrl(url: String): Boolean
}

val LocalVistaInternalLinkNavigator = staticCompositionLocalOf<VistaInternalLinkNavigator?> { null }
