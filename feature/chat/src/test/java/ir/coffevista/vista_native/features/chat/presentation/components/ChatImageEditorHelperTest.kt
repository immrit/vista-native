package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatImageEditorHelperTest {

    @Test
    fun `default ImageEditState is not modified`() {
        val state = ImageEditState()
        assertFalse(state.isModified)
    }

    @Test
    fun `rotation marks ImageEditState as modified`() {
        val state90 = ImageEditState(rotationDegrees = 90f)
        assertTrue(state90.isModified)

        val state360 = ImageEditState(rotationDegrees = 360f)
        assertFalse(state360.isModified)
    }

    @Test
    fun `crop marks ImageEditState as modified`() {
        val cropped = ImageEditState(cropRectNorm = NormalizedCropRect(0.1f, 0.1f, 0.9f, 0.9f))
        assertTrue(cropped.isModified)
    }

    @Test
    fun `drawing stroke marks ImageEditState as modified`() {
        val stroke = ChatDrawingStroke(
            colorArgb = 0xFFFF0000.toInt(),
            strokeWidthNorm = 0.01f,
            points = listOf(Offset(0.2f, 0.2f), Offset(0.3f, 0.4f)),
        )
        val stateWithStrokes = ImageEditState(strokes = listOf(stroke))
        assertTrue(stateWithStrokes.isModified)
    }

    @Test
    fun `computeRotatedDimensions swaps axes only on quarter turns`() {
        val d0 = ChatImageEditorHelper.computeRotatedDimensions(200f, 400f, 0f)
        assertEquals(200f, d0.width, 0.01f)
        assertEquals(400f, d0.height, 0.01f)

        val d90 = ChatImageEditorHelper.computeRotatedDimensions(200f, 400f, 90f)
        assertEquals(400f, d90.width, 0.01f)
        assertEquals(200f, d90.height, 0.01f)

        val d180 = ChatImageEditorHelper.computeRotatedDimensions(200f, 400f, 180f)
        assertEquals(200f, d180.width, 0.01f)
        assertEquals(400f, d180.height, 0.01f)

        val d270 = ChatImageEditorHelper.computeRotatedDimensions(200f, 400f, 270f)
        assertEquals(400f, d270.width, 0.01f)
        assertEquals(200f, d270.height, 0.01f)
    }

    @Test
    fun `computeFittedImageRect centers image within container with and without rotation`() {
        val upright = ChatImageEditorHelper.computeFittedImageRect(
            containerWidth = 1000f,
            containerHeight = 1000f,
            imageWidth = 200f,
            imageHeight = 400f,
            rotationDegrees = 0f,
        )
        assertEquals(250f, upright.left, 0.01f)
        assertEquals(0f, upright.top, 0.01f)
        assertEquals(500f, upright.width, 0.01f)
        assertEquals(1000f, upright.height, 0.01f)

        val rotated = ChatImageEditorHelper.computeFittedImageRect(
            containerWidth = 1000f,
            containerHeight = 1000f,
            imageWidth = 200f,
            imageHeight = 400f,
            rotationDegrees = 90f,
        )
        assertEquals(0f, rotated.left, 0.01f)
        assertEquals(250f, rotated.top, 0.01f)
        assertEquals(1000f, rotated.width, 0.01f)
        assertEquals(500f, rotated.height, 0.01f)
    }

    @Test
    fun `computePreviewTransformGeometry zooms and centers cropped region accurately`() {
        val full = ChatImageEditorHelper.computePreviewTransformGeometry(
            containerWidth = 1000f,
            containerHeight = 1000f,
            srcWidth = 200f,
            srcHeight = 400f,
            rotationDegrees = 0f,
            cropRectNorm = NormalizedCropRect(0f, 0f, 1f, 1f),
        )
        assertEquals(500f, full.viewportWidth, 0.01f)
        assertEquals(1000f, full.viewportHeight, 0.01f)
        assertEquals(0f, full.translationX, 0.01f)
        assertEquals(0f, full.translationY, 0.01f)

        // Cropping top half of 200x400 creates a 1:1 square occupying full 1000x1000 viewport
        val topHalf = ChatImageEditorHelper.computePreviewTransformGeometry(
            containerWidth = 1000f,
            containerHeight = 1000f,
            srcWidth = 200f,
            srcHeight = 400f,
            rotationDegrees = 0f,
            cropRectNorm = NormalizedCropRect(0f, 0f, 1f, 0.5f),
        )
        assertEquals(1000f, topHalf.viewportWidth, 0.01f)
        assertEquals(1000f, topHalf.viewportHeight, 0.01f)
        assertEquals(1000f, topHalf.unrotatedImageWidth, 0.01f)
        assertEquals(2000f, topHalf.unrotatedImageHeight, 0.01f)
        assertEquals(0f, topHalf.translationX, 0.01f)
        assertEquals(500f, topHalf.translationY, 0.01f)
    }

    @Test
    fun `rotateCropRectClockwise90 cycles back after four turns`() {
        val original = NormalizedCropRect(0.1f, 0.2f, 0.6f, 0.5f)
        var current = original
        repeat(4) {
            current = ChatImageEditorHelper.rotateCropRectClockwise90(current)
        }
        assertEquals(original.left, current.left, 0.001f)
        assertEquals(original.top, current.top, 0.001f)
        assertEquals(original.right, current.right, 0.001f)
        assertEquals(original.bottom, current.bottom, 0.001f)
    }

    @Test
    fun `aspectRatioCropRect computes exact pixel ratios regardless of source dimensions`() {
        val rect1x1 = ChatImageEditorHelper.aspectRatioCropRect(1f, imageWidth = 400f, imageHeight = 800f)
        val pxW1 = (rect1x1.right - rect1x1.left) * 400f
        val pxH1 = (rect1x1.bottom - rect1x1.top) * 800f
        assertEquals(1f, pxW1 / pxH1, 0.005f)
        assertEquals(0f, rect1x1.left, 0.001f)
        assertEquals(1f, rect1x1.right, 0.001f)
        assertEquals(0.25f, rect1x1.top, 0.001f)
        assertEquals(0.75f, rect1x1.bottom, 0.001f)

        val rect4x5 = ChatImageEditorHelper.aspectRatioCropRect(4f / 5f, imageWidth = 1000f, imageHeight = 1000f)
        val pxW45 = (rect4x5.right - rect4x5.left) * 1000f
        val pxH45 = (rect4x5.bottom - rect4x5.top) * 1000f
        assertEquals(4f / 5f, pxW45 / pxH45, 0.005f)

        val rect16x9 = ChatImageEditorHelper.aspectRatioCropRect(16f / 9f, imageWidth = 900f, imageHeight = 1600f)
        val pxW169 = (rect16x9.right - rect16x9.left) * 900f
        val pxH169 = (rect16x9.bottom - rect16x9.top) * 1600f
        assertEquals(16f / 9f, pxW169 / pxH169, 0.005f)
    }

    @Test
    fun `hitTestCropHandle identifies corners edges center move and outside`() {
        val box = Rect(100f, 100f, 500f, 400f)
        val radius = 30f

        assertEquals(CropDragHandle.TOP_LEFT, ChatImageEditorHelper.hitTestCropHandle(Offset(105f, 105f), box, radius))
        assertEquals(CropDragHandle.TOP_RIGHT, ChatImageEditorHelper.hitTestCropHandle(Offset(495f, 105f), box, radius))
        assertEquals(CropDragHandle.BOTTOM_LEFT, ChatImageEditorHelper.hitTestCropHandle(Offset(105f, 395f), box, radius))
        assertEquals(CropDragHandle.BOTTOM_RIGHT, ChatImageEditorHelper.hitTestCropHandle(Offset(495f, 395f), box, radius))
        assertEquals(CropDragHandle.LEFT, ChatImageEditorHelper.hitTestCropHandle(Offset(105f, 250f), box, radius))
        assertEquals(CropDragHandle.RIGHT, ChatImageEditorHelper.hitTestCropHandle(Offset(495f, 250f), box, radius))
        assertEquals(CropDragHandle.TOP, ChatImageEditorHelper.hitTestCropHandle(Offset(300f, 105f), box, radius))
        assertEquals(CropDragHandle.BOTTOM, ChatImageEditorHelper.hitTestCropHandle(Offset(300f, 395f), box, radius))
        assertEquals(CropDragHandle.MOVE, ChatImageEditorHelper.hitTestCropHandle(Offset(300f, 250f), box, radius))
        assertEquals(CropDragHandle.NONE, ChatImageEditorHelper.hitTestCropHandle(Offset(20f, 20f), box, radius))
    }

    @Test
    fun `updateCropRectOnDrag resizes free handles moves cleanly and preserves locked ratios`() {
        val freeInitial = NormalizedCropRect(0.2f, 0.2f, 0.8f, 0.8f)
        val topLeftDragged = ChatImageEditorHelper.updateCropRectOnDrag(
            current = freeInitial,
            handle = CropDragHandle.TOP_LEFT,
            dxNorm = 0.1f,
            dyNorm = 0.15f,
            targetAspectRatio = null,
            imageWidth = 400f,
            imageHeight = 800f,
        )
        assertEquals(0.3f, topLeftDragged.left, 0.001f)
        assertEquals(0.35f, topLeftDragged.top, 0.001f)
        assertEquals(0.8f, topLeftDragged.right, 0.001f)
        assertEquals(0.8f, topLeftDragged.bottom, 0.001f)

        val moved = ChatImageEditorHelper.updateCropRectOnDrag(
            current = freeInitial,
            handle = CropDragHandle.MOVE,
            dxNorm = 0.5f,
            dyNorm = -0.5f,
            targetAspectRatio = null,
            imageWidth = 400f,
            imageHeight = 800f,
        )
        assertEquals(0.4f, moved.left, 0.001f)
        assertEquals(1.0f, moved.right, 0.001f)
        assertEquals(0.0f, moved.top, 0.001f)
        assertEquals(0.6f, moved.bottom, 0.001f)

        val lockedInitial = ChatImageEditorHelper.aspectRatioCropRect(1f, imageWidth = 400f, imageHeight = 800f)
        val lockedResized = ChatImageEditorHelper.updateCropRectOnDrag(
            current = lockedInitial,
            handle = CropDragHandle.BOTTOM_RIGHT,
            dxNorm = -0.25f,
            dyNorm = -0.1f,
            targetAspectRatio = 1f,
            imageWidth = 400f,
            imageHeight = 800f,
        )
        val actualPixelRatio =
            ((lockedResized.right - lockedResized.left) * 400f) /
                ((lockedResized.bottom - lockedResized.top) * 800f)
        assertEquals(1f, actualPixelRatio, 0.005f)
        assertEquals(lockedInitial.left, lockedResized.left, 0.001f)
        assertEquals(lockedInitial.top, lockedResized.top, 0.001f)
    }
}
