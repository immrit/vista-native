package ir.coffevista.vista_native.features.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellContractTest {
    @Test
    fun `five tabs keep approved order`() {
        assertEquals(
            listOf(
                ShellTab.Feed,
                ShellTab.Search,
                ShellTab.Services,
                ShellTab.Chat,
                ShellTab.Profile,
            ),
            ShellTab.entries,
        )
    }

    @Test
    fun `deferred kinds target controlled tabs`() {
        assertEquals(ShellTab.Feed, ShellDeferredKind.POST.targetTab())
        assertEquals(ShellTab.Feed, ShellDeferredKind.GROUP.targetTab())
        assertEquals(ShellTab.Profile, ShellDeferredKind.PROFILE.targetTab())
        assertEquals(ShellTab.Chat, ShellDeferredKind.CHAT.targetTab())
    }

    @Test
    fun `root back returns to feed before exit`() {
        assertEquals(
            ShellBackAction.SelectFeed,
            shellBackAction(ShellTab.Chat, nested = false),
        )
        assertEquals(
            ShellBackAction.RequestExit,
            shellBackAction(ShellTab.Feed, nested = false),
        )
        assertEquals(
            ShellBackAction.PopNested,
            shellBackAction(ShellTab.Feed, nested = true),
        )
    }
}
