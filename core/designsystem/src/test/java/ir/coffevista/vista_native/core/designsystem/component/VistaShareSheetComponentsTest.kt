package ir.coffevista.vista_native.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VistaShareSheetComponentsTest {

    @Test
    fun buildVistaStandardShareActions_includesAllModernPlatformsWithoutRubikaOrGmail() {
        val actions = buildVistaStandardShareActions(
            isBusy = false,
            showAddToVistaStory = true,
            onAddToVistaStory = {},
            onShareInstagramStory = {},
            onCopyLink = {},
            onShareTelegram = {},
            onShareWhatsApp = {},
            onShareThreads = {},
            onShareX = {},
            onShareMore = {},
        )

        val ids = actions.map { it.id }
        assertEquals(
            listOf(
                "add_to_story",
                "instagram_story",
                "copy_link",
                "telegram",
                "whatsapp",
                "threads",
                "x",
                "more",
            ),
            ids,
        )
        assertFalse(ids.contains("rubika"))
        assertFalse(ids.contains("gmail"))
        assertTrue(actions.all { it.enabled })
    }

    @Test
    fun buildVistaStandardShareActions_disablesStoryButtonsWhenBusy() {
        val actions = buildVistaStandardShareActions(
            isBusy = true,
            showAddToVistaStory = true,
            onAddToVistaStory = {},
            onShareInstagramStory = {},
            onCopyLink = {},
            onShareTelegram = {},
            onShareWhatsApp = {},
            onShareThreads = {},
            onShareX = {},
            onShareMore = {},
        )

        assertFalse(actions.first { it.id == "add_to_story" }.enabled)
        assertFalse(actions.first { it.id == "instagram_story" }.enabled)
        assertTrue(actions.first { it.id == "copy_link" }.enabled)
    }
}
