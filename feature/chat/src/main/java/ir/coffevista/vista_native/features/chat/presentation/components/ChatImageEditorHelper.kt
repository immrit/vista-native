package ir.coffevista.vista_native.features.chat.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
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

                // 3. Decode scaled bitmap
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }.getOrNull()
        }

    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun cropBitmap(source: Bitmap, cropNorm: NormalizedCropRect): Bitmap {
        val safeLeft = cropNorm.left.coerceIn(0f, 1f)
        val safeTop = cropNorm.top.coerceIn(0f, 1f)
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

            // 4. Save to cache
            val editsDir = File(context.cacheDir, "chat_edits").apply { mkdirs() }
            val outputFile = File(editsDir, "chat_edit_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bitmap.recycle()

            Pair(Uri.fromFile(outputFile), outputFile.length())
        }.getOrNull()
    }
}
