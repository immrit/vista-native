package ir.coffevista.vista_native.features.shell

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
@Keep
enum class ShellTab(val labelFa: String, val glyph: String) {
    Feed("خانه", "⌂"),
    Search("جستجو", "⌕"),
    Services("خدمات", "◇"),
    Chat("گفت‌وگو", "◌"),
    Profile("نمایه", "◎"),
}

@Serializable
@Keep
enum class ShellDeferredKind { POST, PROFILE, GROUP, CHAT }

data class ShellDeepLinkRequest(
    val deliveryId: Long,
    val kind: ShellDeferredKind,
    val reference: String,
    val secondaryReference: String? = null,
)

fun ShellDeferredKind.targetTab(): ShellTab = when (this) {
    ShellDeferredKind.POST, ShellDeferredKind.GROUP -> ShellTab.Feed
    ShellDeferredKind.PROFILE -> ShellTab.Profile
    ShellDeferredKind.CHAT -> ShellTab.Chat
}

fun shellBackAction(
    selectedTab: ShellTab,
    nested: Boolean,
): ShellBackAction = when {
    nested -> ShellBackAction.PopNested
    selectedTab != ShellTab.Feed -> ShellBackAction.SelectFeed
    else -> ShellBackAction.RequestExit
}

enum class ShellBackAction { PopNested, SelectFeed, RequestExit }
