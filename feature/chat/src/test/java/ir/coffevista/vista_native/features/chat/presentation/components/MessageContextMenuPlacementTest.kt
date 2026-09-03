package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageContextMenuPlacementTest {
    private val viewport = IntSize(1080, 2400)
    private val insets = MessageContextMenuSafeInsets(top = 96f, bottom = 144f)
    private val menuHeight = 336f // 8 actions
    private val menuWidth = 195f
    private val reactionsHeight = 44f
    private val reactionsWidth = 300f
    private val edgeGap = 12f
    private val elementGap = 8f

    @Test
    fun `bottom anchor places reactions directly above bubble and menu above reactions without any overlap`() {
        // Simulating the user's screenshot near the bottom of the screen
        val anchor = Rect(left = 700f, top = 2000f, right = 1000f, bottom = 2080f)
        val placement = placement(anchor = anchor, isMine = true)

        assertNotNull(placement.reactions)
        val reactions = placement.reactions!!

        // Reactions capsule must be above the anchor bubble
        assertEquals(anchor.top - elementGap - reactionsHeight, reactions.y, 0.01f)

        // Menu must be above the reactions capsule, separated by elementGap
        assertEquals(reactions.y - elementGap - menuHeight, placement.menu.y, 0.01f)

        // Verify strictly: bottom of menu is strictly above top of reactions
        assertTrue(placement.menu.y + menuHeight <= reactions.y)
        assertEquals(elementGap, reactions.y - (placement.menu.y + menuHeight), 0.01f)
    }

    @Test
    fun `top anchor places reactions directly below bubble and menu below reactions without any overlap`() {
        val anchor = Rect(left = 40f, top = 200f, right = 350f, bottom = 280f)
        val placement = placement(anchor = anchor, isMine = false)

        assertNotNull(placement.reactions)
        val reactions = placement.reactions!!

        // Reactions capsule must be below the anchor bubble
        assertEquals(anchor.bottom + elementGap, reactions.y, 0.01f)

        // Menu must be below the reactions capsule, separated by elementGap
        assertEquals(reactions.y + reactionsHeight + elementGap, placement.menu.y, 0.01f)

        // Verify strictly: bottom of reactions is strictly above top of menu
        assertTrue(reactions.y + reactionsHeight <= placement.menu.y)
        assertEquals(elementGap, placement.menu.y - (reactions.y + reactionsHeight), 0.01f)
    }

    @Test
    fun `own message aligns horizontal bounds to right side`() {
        val anchor = Rect(left = 800f, top = 500f, right = 1040f, bottom = 580f)
        val placement = placement(anchor = anchor, isMine = true)

        assertEquals(anchor.right - menuWidth, placement.menu.x, 0.01f)
        assertEquals(anchor.right - reactionsWidth, placement.reactions!!.x, 0.01f)
    }

    @Test
    fun `peer message aligns horizontal bounds to left side`() {
        val anchor = Rect(left = 36f, top = 500f, right = 320f, bottom = 580f)
        val placement = placement(anchor = anchor, isMine = false)

        assertEquals(anchor.left, placement.menu.x, 0.01f)
        assertEquals(anchor.left, placement.reactions!!.x, 0.01f)
    }

    @Test
    fun `cramped viewport shifts stack together without causing overlap`() {
        val smallViewport = IntSize(1080, 600)
        val anchor = Rect(left = 200f, top = 280f, right = 600f, bottom = 360f)
        val placement = calculateMessageContextMenuPlacement(
            anchor = anchor,
            viewport = smallViewport,
            safeInsets = insets,
            isMine = false,
            menuWidth = menuWidth,
            menuHeight = menuHeight,
            reactionsWidth = reactionsWidth,
            reactionsHeight = reactionsHeight,
            showReactions = true,
            edgeGap = edgeGap,
            elementGap = elementGap,
        )

        assertNotNull(placement.reactions)
        val reactions = placement.reactions!!

        // In all shifted scenarios, menu and reactions MUST NOT overlap
        val noOverlap = (placement.menu.y + menuHeight + elementGap <= reactions.y) ||
            (reactions.y + reactionsHeight + elementGap <= placement.menu.y)
        assertTrue("Menu and reactions must never overlap", noOverlap)
    }

    @Test
    fun `hidden reactions produces null reactions and valid menu placement`() {
        val anchor = Rect(left = 200f, top = 400f, right = 600f, bottom = 500f)
        val placement = placement(
            anchor = anchor,
            isMine = false,
            showReactions = false,
        )

        assertNull(placement.reactions)
        assertEquals(anchor.bottom + elementGap, placement.menu.y, 0.01f)
    }

    private fun placement(
        anchor: Rect,
        isMine: Boolean,
        showReactions: Boolean = true,
    ) = calculateMessageContextMenuPlacement(
        anchor = anchor,
        viewport = viewport,
        safeInsets = insets,
        isMine = isMine,
        menuWidth = menuWidth,
        menuHeight = menuHeight,
        reactionsWidth = reactionsWidth,
        reactionsHeight = reactionsHeight,
        showReactions = showReactions,
        edgeGap = edgeGap,
        elementGap = elementGap,
    )
}
