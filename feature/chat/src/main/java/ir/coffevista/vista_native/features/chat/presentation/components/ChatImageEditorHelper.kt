package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
)

enum class CropDragHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    MOVE,
}

data class PreviewTransformGeometry(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val unrotatedImageWidth: Float,
    val unrotatedImageHeight: Float,
    val translationX: Float,
    val translationY: Float,
    val normalizedRotationDegrees: Float,
)

data class ChatDrawingStroke(
    val colorArgb: Int,
    val strokeWidthNorm: Float, // relative to viewport size
    val points: List<Offset>, // normalized 0f..1f coordinates
)

data class ImageEditState(
    val rotationDegrees: Float = 0f,
    val cropRectNorm: NormalizedCropRect = NormalizedCropRect(), // 0..1 normalized
    val strokes: List<ChatDrawingStroke> = emptyList(),
) {
    val isModified: Boolean
        get() = (rotationDegrees % 360f != 0f) ||
            cropRectNorm.left > 0.001f || cropRectNorm.top > 0.001f ||
            cropRectNorm.right < 0.999f || cropRectNorm.bottom < 0.999f ||
            strokes.isNotEmpty()
}

object ChatImageEditorHelper {

    fun computeRotatedDimensions(
        srcWidth: Float,
        srcHeight: Float,
        rotationDegrees: Float,
    ): Size {
        val safeW = srcWidth.coerceAtLeast(1f)
        val safeH = srcHeight.coerceAtLeast(1f)
        val normalized = ((rotationDegrees % 360f) + 360f) % 360f
        val quarterTurns = ((normalized / 90f).roundToInt() % 4 + 4) % 4
        return if (quarterTurns == 1 || quarterTurns == 3) {
            Size(safeH, safeW)
        } else {
            Size(safeW, safeH)
        }
    }

    fun computeFittedImageRect(
        containerWidth: Float,
        containerHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        rotationDegrees: Float = 0f,
    ): Rect {
        val safeContainerW = containerWidth.coerceAtLeast(1f)
        val safeContainerH = containerHeight.coerceAtLeast(1f)
        val rotated = computeRotatedDimensions(imageWidth, imageHeight, rotationDegrees)
        val scale = min(safeContainerW / rotated.width, safeContainerH / rotated.height)
        val fittedW = rotated.width * scale
        val fittedH = rotated.height * scale
        val left = (safeContainerW - fittedW) / 2f
        val top = (safeContainerH - fittedH) / 2f
        return Rect(left, top, left + fittedW, top + fittedH)
    }

    fun computePreviewTransformGeometry(
        containerWidth: Float,
        containerHeight: Float,
        srcWidth: Float,
        srcHeight: Float,
        rotationDegrees: Float,
        cropRectNorm: NormalizedCropRect,
    ): PreviewTransformGeometry {
        val safeContainerW = containerWidth.coerceAtLeast(1f)
        val safeContainerH = containerHeight.coerceAtLeast(1f)
        val safeSrcW = srcWidth.coerceAtLeast(1f)
        val safeSrcH = srcHeight.coerceAtLeast(1f)
        val normalizedRotation = ((rotationDegrees % 360f) + 360f) % 360f
        val rotated = computeRotatedDimensions(safeSrcW, safeSrcH, normalizedRotation)

        val safeLeft = cropRectNorm.left.coerceIn(0f, 0.95f)
        val safeTop = cropRectNorm.top.coerceIn(0f, 0.95f)
        val safeRight = cropRectNorm.right.coerceIn(safeLeft + 0.05f, 1f)
        val safeBottom = cropRectNorm.bottom.coerceIn(safeTop + 0.05f, 1f)
        val cropW = safeRight - safeLeft
        val cropH = safeBottom - safeTop

        val effW = (rotated.width * cropW).coerceAtLeast(1f)
        val effH = (rotated.height * cropH).coerceAtLeast(1f)
        val scale = min(safeContainerW / effW, safeContainerH / effH)

        val viewportW = effW * scale
        val viewportH = effH * scale
        val fullRotW = rotated.width * scale
        val fullRotH = rotated.height * scale
        val offsetX = (-safeLeft * fullRotW) + (fullRotW - viewportW) / 2f
        val offsetY = (-safeTop * fullRotH) + (fullRotH - viewportH) / 2f

        return PreviewTransformGeometry(
            viewportWidth = viewportW,
            viewportHeight = viewportH,
            unrotatedImageWidth = safeSrcW * scale,
            unrotatedImageHeight = safeSrcH * scale,
            translationX = offsetX,
            translationY = offsetY,
            normalizedRotationDegrees = normalizedRotation,
        )
    }

    fun rotateCropRectClockwise90(crop: NormalizedCropRect): NormalizedCropRect {
        val safeLeft = crop.left.coerceIn(0f, 0.95f)
        val safeTop = crop.top.coerceIn(0f, 0.95f)
        val safeRight = crop.right.coerceIn(safeLeft + 0.05f, 1f)
        val safeBottom = crop.bottom.coerceIn(safeTop + 0.05f, 1f)
        return NormalizedCropRect(
            left = (1f - safeBottom).coerceIn(0f, 0.95f),
            top = safeLeft,
            right = (1f - safeTop).coerceIn((1f - safeBottom).coerceIn(0f, 0.95f) + 0.05f, 1f),
            bottom = safeRight,
        )
    }

    fun aspectRatioCropRect(
        targetAspectRatio: Float?,
        imageWidth: Float,
        imageHeight: Float,
    ): NormalizedCropRect {
        if (targetAspectRatio == null || targetAspectRatio <= 0f) {
            return NormalizedCropRect(0f, 0f, 1f, 1f)
        }
        val safeW = imageWidth.coerceAtLeast(1f)
        val safeH = imageHeight.coerceAtLeast(1f)
        val imgAspect = safeW / safeH
        val normW: Float
        val normH: Float
        if (imgAspect >= targetAspectRatio) {
            normH = 1f
            normW = (targetAspectRatio / imgAspect).coerceIn(0.12f, 1f)
        } else {
            normW = 1f
            normH = (imgAspect / targetAspectRatio).coerceIn(0.12f, 1f)
        }
        val left = ((1f - normW) / 2f).coerceIn(0f, 1f - normW)
        val top = ((1f - normH) / 2f).coerceIn(0f, 1f - normH)
        return NormalizedCropRect(
            left = left,
            top = top,
            right = left + normW,
            bottom = top + normH,
        )
    }

    fun hitTestCropHandle(
        touchPoint: Offset,
        cropPixelRect: Rect,
        handleRadiusPx: Float,
    ): CropDragHandle {
        val corners = listOf(
            CropDragHandle.TOP_LEFT to Offset(cropPixelRect.left, cropPixelRect.top),
            CropDragHandle.TOP_RIGHT to Offset(cropPixelRect.right, cropPixelRect.top),
            CropDragHandle.BOTTOM_LEFT to Offset(cropPixelRect.left, cropPixelRect.bottom),
            CropDragHandle.BOTTOM_RIGHT to Offset(cropPixelRect.right, cropPixelRect.bottom),
        )
        var bestCorner = CropDragHandle.NONE
        var bestDist = handleRadiusPx
        for ((handle, pt) in corners) {
            val d = hypot(touchPoint.x - pt.x, touchPoint.y - pt.y)
            if (d <= bestDist) {
                bestDist = d
                bestCorner = handle
            }
        }
        if (bestCorner != CropDragHandle.NONE) return bestCorner

        val inVerticalSpan = touchPoint.y in (cropPixelRect.top - handleRadiusPx)..(cropPixelRect.bottom + handleRadiusPx)
        val inHorizontalSpan = touchPoint.x in (cropPixelRect.left - handleRadiusPx)..(cropPixelRect.right + handleRadiusPx)
        val edgeCandidates = listOfNotNull(
            if (inVerticalSpan && abs(touchPoint.x - cropPixelRect.left) <= handleRadiusPx) {
                CropDragHandle.LEFT to abs(touchPoint.x - cropPixelRect.left)
            } else null,
            if (inVerticalSpan && abs(touchPoint.x - cropPixelRect.right) <= handleRadiusPx) {
                CropDragHandle.RIGHT to abs(touchPoint.x - cropPixelRect.right)
            } else null,
            if (inHorizontalSpan && abs(touchPoint.y - cropPixelRect.top) <= handleRadiusPx) {
                CropDragHandle.TOP to abs(touchPoint.y - cropPixelRect.top)
            } else null,
            if (inHorizontalSpan && abs(touchPoint.y - cropPixelRect.bottom) <= handleRadiusPx) {
                CropDragHandle.BOTTOM to abs(touchPoint.y - cropPixelRect.bottom)
            } else null,
        )
        edgeCandidates.minByOrNull { it.second }?.let { return it.first }

        return if (touchPoint.x in cropPixelRect.left..cropPixelRect.right &&
            touchPoint.y in cropPixelRect.top..cropPixelRect.bottom
        ) {
            CropDragHandle.MOVE
        } else {
            CropDragHandle.NONE
        }
    }

    fun updateCropRectOnDrag(
        current: NormalizedCropRect,
        handle: CropDragHandle,
        dxNorm: Float,
        dyNorm: Float,
        targetAspectRatio: Float?,
        imageWidth: Float,
        imageHeight: Float,
        minFraction: Float = 0.12f,
    ): NormalizedCropRect {
        if (handle == CropDragHandle.NONE) return current
        val safeW = imageWidth.coerceAtLeast(1f)
        val safeH = imageHeight.coerceAtLeast(1f)
        val curL = current.left.coerceIn(0f, 1f - minFraction)
        val curT = current.top.coerceIn(0f, 1f - minFraction)
        val curR = current.right.coerceIn(curL + minFraction, 1f)
        val curB = current.bottom.coerceIn(curT + minFraction, 1f)

        if (handle == CropDragHandle.MOVE) {
            val w = curR - curL
            val h = curB - curT
            val newL = (curL + dxNorm).coerceIn(0f, (1f - w).coerceAtLeast(0f))
            val newT = (curT + dyNorm).coerceIn(0f, (1f - h).coerceAtLeast(0f))
            return NormalizedCropRect(newL, newT, newL + w, newT + h)
        }

        if (targetAspectRatio == null || targetAspectRatio <= 0f) {
            var newL = curL
            var newT = curT
            var newR = curR
            var newB = curB
            when (handle) {
                CropDragHandle.LEFT -> newL = (curL + dxNorm).coerceIn(0f, curR - minFraction)
                CropDragHandle.RIGHT -> newR = (curR + dxNorm).coerceIn(curL + minFraction, 1f)
                CropDragHandle.TOP -> newT = (curT + dyNorm).coerceIn(0f, curB - minFraction)
                CropDragHandle.BOTTOM -> newB = (curB + dyNorm).coerceIn(curT + minFraction, 1f)
                CropDragHandle.TOP_LEFT -> {
                    newL = (curL + dxNorm).coerceIn(0f, curR - minFraction)
                    newT = (curT + dyNorm).coerceIn(0f, curB - minFraction)
                }
                CropDragHandle.TOP_RIGHT -> {
                    newR = (curR + dxNorm).coerceIn(curL + minFraction, 1f)
                    newT = (curT + dyNorm).coerceIn(0f, curB - minFraction)
                }
                CropDragHandle.BOTTOM_LEFT -> {
                    newL = (curL + dxNorm).coerceIn(0f, curR - minFraction)
                    newB = (curB + dyNorm).coerceIn(curT + minFraction, 1f)
                }
                CropDragHandle.BOTTOM_RIGHT -> {
                    newR = (curR + dxNorm).coerceIn(curL + minFraction, 1f)
                    newB = (curB + dyNorm).coerceIn(curT + minFraction, 1f)
                }
                else -> Unit
            }
            return NormalizedCropRect(newL, newT, newR, newB)
        }

        val normRatio = targetAspectRatio * (safeH / safeW)
        return when (handle) {
            CropDragHandle.TOP_LEFT,
            CropDragHandle.TOP_RIGHT,
            CropDragHandle.BOTTOM_LEFT,
            CropDragHandle.BOTTOM_RIGHT -> {
                val anchorX = if (handle == CropDragHandle.TOP_LEFT || handle == CropDragHandle.BOTTOM_LEFT) curR else curL
                val anchorY = if (handle == CropDragHandle.TOP_LEFT || handle == CropDragHandle.TOP_RIGHT) curB else curT
                val maxW = if (handle == CropDragHandle.TOP_LEFT || handle == CropDragHandle.BOTTOM_LEFT) anchorX else 1f - anchorX
                val maxH = if (handle == CropDragHandle.TOP_LEFT || handle == CropDragHandle.TOP_RIGHT) anchorY else 1f - anchorY

                val rawW = if (handle == CropDragHandle.TOP_LEFT || handle == CropDragHandle.BOTTOM_LEFT) {
                    anchorX - (curL + dxNorm)
                } else {
                    (curR + dxNorm) - anchorX
                }
                val rawH = if (handle == CropDragHandle.TOP_LEFT || handle == CropDragHandle.TOP_RIGHT) {
                    anchorY - (curT + dyNorm)
                } else {
                    (curB + dyNorm) - anchorY
                }
                val candH = if (abs(dxNorm) * safeW >= abs(dyNorm) * safeH) rawW / normRatio else rawH
                val maxPossibleH = min(maxH, maxW / normRatio).coerceAtLeast(0.05f)
                val minPossibleH = max(minFraction, minFraction / normRatio).coerceAtMost(maxPossibleH)
                val finalH = candH.coerceIn(minPossibleH, maxPossibleH)
                val finalW = (finalH * normRatio).coerceIn(0.05f, maxW)
                val adjustedH = finalW / normRatio

                val l = if (handle == CropDragHandle.TOP_LEFT || handle == CropDragHandle.BOTTOM_LEFT) anchorX - finalW else anchorX
                val r = if (handle == CropDragHandle.TOP_LEFT || handle == CropDragHandle.BOTTOM_LEFT) anchorX else anchorX + finalW
                val t = if (handle == CropDragHandle.TOP_LEFT || handle == CropDragHandle.TOP_RIGHT) anchorY - adjustedH else anchorY
                val b = if (handle == CropDragHandle.TOP_LEFT || handle == CropDragHandle.TOP_RIGHT) anchorY else anchorY + adjustedH
                NormalizedCropRect(l, t, r, b)
            }
            CropDragHandle.LEFT, CropDragHandle.RIGHT -> {
                val isLeft = handle == CropDragHandle.LEFT
                val anchorX = if (isLeft) curR else curL
                val maxW = min(if (isLeft) curR else 1f - curL, normRatio).coerceAtLeast(0.05f)
                val minW = max(minFraction, minFraction * normRatio).coerceAtMost(maxW)
                val rawW = if (isLeft) anchorX - (curL + dxNorm) else (curR + dxNorm) - anchorX
                val finalW = rawW.coerceIn(minW, maxW)
                val finalH = (finalW / normRatio).coerceIn(0.05f, 1f)
                val centerY = (curT + curB) / 2f
                val t = (centerY - finalH / 2f).coerceIn(0f, (1f - finalH).coerceAtLeast(0f))
                val b = t + finalH
                val l = if (isLeft) anchorX - finalW else anchorX
                val r = if (isLeft) anchorX else anchorX + finalW
                NormalizedCropRect(l, t, r, b)
            }
            CropDragHandle.TOP, CropDragHandle.BOTTOM -> {
                val isTop = handle == CropDragHandle.TOP
                val anchorY = if (isTop) curB else curT
                val maxH = min(if (isTop) curB else 1f - curT, 1f / normRatio).coerceAtLeast(0.05f)
                val minH = max(minFraction, minFraction / normRatio).coerceAtMost(maxH)
                val rawH = if (isTop) anchorY - (curT + dyNorm) else (curB + dyNorm) - anchorY
                val finalH = rawH.coerceIn(minH, maxH)
                val finalW = (finalH * normRatio).coerceIn(0.05f, 1f)
                val centerX = (curL + curR) / 2f
                val l = (centerX - finalW / 2f).coerceIn(0f, (1f - finalW).coerceAtLeast(0f))
                val r = l + finalW
                val t = if (isTop) anchorY - finalH else anchorY
                val b = if (isTop) anchorY else anchorY + finalH
                NormalizedCropRect(l, t, r, b)
            }
            else -> current
        }
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        return context.contentResolver.openInputStream(uri)?.use { exifStream ->
            runCatching {
                ExifInterface(exifStream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    }

    private fun applyExifOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }
        if (matrix.isIdentity) return source
        val transformed = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (transformed != source) {
            source.recycle()
        }
        return transformed
    }

    suspend fun loadImageDimensions(context: Context, uri: Uri): Size? =
        withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                if (options.outWidth <= 0 || options.outHeight <= 0) return@runCatching null
                val orientation = readExifOrientation(context, uri)
                val swapsAxes = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                    orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                    orientation == ExifInterface.ORIENTATION_TRANSVERSE
                if (swapsAxes) {
                    Size(options.outHeight.toFloat(), options.outWidth.toFloat())
                } else {
                    Size(options.outWidth.toFloat(), options.outHeight.toFloat())
                }
            }.getOrNull()
        }

    suspend fun loadBitmap(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Decode bounds
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                if (options.outWidth <= 0 || options.outHeight <= 0) return@runCatching null

                // 2. Compute sample size
                var inSampleSize = 1
                val maxActual = max(options.outWidth, options.outHeight)
                while (maxActual / (inSampleSize * 2) >= maxDimension) {
                    inSampleSize *= 2
                }

                // 3. Decode scaled bitmap and normalize EXIF orientation to match Coil display
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return@runCatching null

                val orientation = readExifOrientation(context, uri)
                applyExifOrientation(decoded, orientation)
            }.getOrNull()
        }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun cropBitmap(source: Bitmap, cropNorm: NormalizedCropRect): Bitmap {
        val safeLeft = cropNorm.left.coerceIn(0f, 0.95f)
        val safeTop = cropNorm.top.coerceIn(0f, 0.95f)
        val safeRight = cropNorm.right.coerceIn(safeLeft + 0.05f, 1f)
        val safeBottom = cropNorm.bottom.coerceIn(safeTop + 0.05f, 1f)

        val x = (safeLeft * source.width).roundToInt().coerceIn(0, source.width - 1)
        val y = (safeTop * source.height).roundToInt().coerceIn(0, source.height - 1)
        val w = ((safeRight - safeLeft) * source.width).roundToInt().coerceIn(1, source.width - x)
        val h = ((safeBottom - safeTop) * source.height).roundToInt().coerceIn(1, source.height - y)

        return Bitmap.createBitmap(source, x, y, w, h)
    }

    fun renderStrokesOnBitmap(source: Bitmap, strokes: List<ChatDrawingStroke>): Bitmap {
        if (strokes.isEmpty()) return source
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val bmpW = output.width.toFloat()
        val bmpH = output.height.toFloat()

        strokes.forEach { stroke ->
            if (stroke.points.size > 1) {
                paint.color = stroke.colorArgb
                paint.strokeWidth = max(2f, stroke.strokeWidthNorm * bmpW)

                val path = Path()
                val first = stroke.points[0]
                path.moveTo(first.x * bmpW, first.y * bmpH)

                for (i in 1 until stroke.points.size) {
                    val prev = stroke.points[i - 1]
                    val curr = stroke.points[i]
                    val midX = (prev.x + curr.x) / 2f * bmpW
                    val midY = (prev.y + curr.y) / 2f * bmpH
                    path.quadTo(prev.x * bmpW, prev.y * bmpH, midX, midY)
                }
                val last = stroke.points.last()
                path.lineTo(last.x * bmpW, last.y * bmpH)
                canvas.drawPath(path, paint)
            } else if (stroke.points.size == 1) {
                paint.color = stroke.colorArgb
                paint.strokeWidth = max(2f, stroke.strokeWidthNorm * bmpW)
                val pt = stroke.points[0]
                canvas.drawPoint(pt.x * bmpW, pt.y * bmpH, paint)
            }
        }
        return output
    }

    suspend fun applyEditsAndSave(
        context: Context,
        originalUri: Uri,
        editState: ImageEditState,
    ): Pair<Uri, Long>? = withContext(Dispatchers.IO) {
        runCatching {
            var bitmap = loadBitmap(context, originalUri) ?: return@runCatching null

            // 1. Rotation
            if (editState.rotationDegrees % 360f != 0f) {
                val rotated = rotateBitmap(bitmap, editState.rotationDegrees)
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }

            // 2. Crop
            if (editState.cropRectNorm.left > 0.001f || editState.cropRectNorm.top > 0.001f ||
                editState.cropRectNorm.right < 0.999f || editState.cropRectNorm.bottom < 0.999f
            ) {
                val cropped = cropBitmap(bitmap, editState.cropRectNorm)
                if (cropped != bitmap) {
                    bitmap.recycle()
                    bitmap = cropped
                }
            }

            // 3. Drawing strokes
            if (editState.strokes.isNotEmpty()) {
                val drawn = renderStrokesOnBitmap(bitmap, editState.strokes)
                if (drawn != bitmap) {
                    bitmap.recycle()
                    bitmap = drawn
                }
            }

            // 4. Save to unique temp file so multi-image albums never overwrite each other
            val editsDir = File(context.cacheDir, "chat_edits").apply { mkdirs() }
            val outputFile = File.createTempFile("chat_edit_", ".jpg", editsDir)
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bitmap.recycle()

            Pair(Uri.fromFile(outputFile), outputFile.length())
        }.getOrNull()
    }
}
