package ir.coffevista.vista_native.features.stories.ui.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.ui.canvas.LegacyStoryMapper
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryStageSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * High-resolution (1080x1920) offscreen rasterizer for exporting stories with 1:1 preview parity.
 * Accurately translates normalized transforms, text styles, joined multi-line badge backgrounds,
 * and drawing layers onto a standalone output bitmap file.
 */
object StoryRasterizer {

    const val EXPORT_WIDTH = 1080
    const val EXPORT_HEIGHT = 1920

    suspend fun renderStoryToBitmap(
        context: Context,
        mediaUri: Uri,
        elements: List<StoryElement>,
    ): Bitmap = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(EXPORT_WIDTH, EXPORT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Background Media (Image center-crop)
        val bgBitmap = loadMediaBitmap(context, mediaUri)
        if (bgBitmap != null) {
            drawCenterCrop(canvas, bgBitmap, EXPORT_WIDTH, EXPORT_HEIGHT)
        } else {
            canvas.drawColor(android.graphics.Color.BLACK)
        }

        val stageScale = EXPORT_WIDTH / StoryStageSpec.RefWidth // ~3.0

        // 2. Draw Drawing Layer (if present)
        val drawing = elements.firstOrNull { it.type == "drawing" }
        if (drawing != null && !drawing.content.isNullOrBlank()) {
            val strokes = StoryDrawingCodec.decode(drawing.content)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
            }
            strokes.forEach { stroke ->
                val strokeColor = stroke.argb
                val strokeWidthPx = stroke.width * stageScale
                strokePaint.color = strokeColor
                strokePaint.strokeWidth = strokeWidthPx
                strokePaint.strokeCap = when (stroke.brush) {
                    StoryBrush.Marker -> Paint.Cap.SQUARE
                    else -> Paint.Cap.ROUND
                }
                if (stroke.brush == StoryBrush.Marker) {
                    val alpha = (android.graphics.Color.alpha(strokeColor) * 0.5f).toInt()
                    strokePaint.alpha = alpha
                } else {
                    strokePaint.alpha = android.graphics.Color.alpha(strokeColor)
                }

                if (stroke.points.size > 1) {
                    val path = Path()
                    val p0 = stroke.points[0]
                    path.moveTo(p0.first * EXPORT_WIDTH, p0.second * EXPORT_HEIGHT)
                    for (i in 1 until stroke.points.size) {
                        val pt = stroke.points[i]
                        path.lineTo(pt.first * EXPORT_WIDTH, pt.second * EXPORT_HEIGHT)
                    }
                    canvas.drawPath(path, strokePaint)
                } else if (stroke.points.size == 1) {
                    val pt = stroke.points[0]
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = strokeColor
                    }
                    canvas.drawCircle(pt.first * EXPORT_WIDTH, pt.second * EXPORT_HEIGHT, strokeWidthPx / 2f, fillPaint)
                }
            }
        }

        // 3. Draw Story Elements in z-order
        elements.filterNot { it.type == "drawing" }.forEach { element ->
            val transform = LegacyStoryMapper.resolve(element)
            val cxPx = transform.cx * EXPORT_WIDTH
            val cyPx = transform.cy * EXPORT_HEIGHT

            canvas.save()
            canvas.translate(cxPx, cyPx)
            canvas.rotate(transform.rotation)
            canvas.scale(transform.scale, transform.scale)

            when (element.type) {
                "text" -> {
                    val spec = element.toTextSpec()
                    drawTextElement(canvas, spec, stageScale)
                }
                "poll", "question", "mention", "location", "link", "date", "weather", "countdown", "hashtag" -> {
                    drawWidgetElement(canvas, element, stageScale)
                }
            }

            canvas.restore()
        }

        bitmap
    }

    suspend fun exportStoryToFile(
        context: Context,
        mediaUri: Uri,
        elements: List<StoryElement>,
        outputFile: File,
    ): File = withContext(Dispatchers.IO) {
        val bitmap = renderStoryToBitmap(context, mediaUri, elements)
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        bitmap.recycle()
        outputFile
    }

    private suspend fun loadMediaBitmap(context: Context, uri: Uri): Bitmap? {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        return if (result is SuccessResult) {
            (result.drawable as? BitmapDrawable)?.bitmap
        } else null
    }

    private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, targetWidth: Int, targetHeight: Int) {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val targetRatio = targetWidth.toFloat() / targetHeight
        val srcRatio = srcW / srcH

        val srcRect = if (srcRatio > targetRatio) {
            val cropW = (srcH * targetRatio).roundToInt()
            val left = ((srcW - cropW) / 2f).roundToInt()
            Rect(left, 0, left + cropW, srcH.toInt())
        } else {
            val cropH = (srcW / targetRatio).roundToInt()
            val top = ((srcH - cropH) / 2f).roundToInt()
            Rect(0, top, srcW.toInt(), top + cropH)
        }

        val dstRect = Rect(0, 0, targetWidth, targetHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
    }

    private fun drawTextElement(canvas: Canvas, spec: StoryTextSpec, stageScale: Float) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = spec.fontSize * stageScale
            color = parseColor(spec.color)
            typeface = when (spec.fontStyle) {
                StoryFontStyle.Classic -> Typeface.SERIF
                StoryFontStyle.Typewriter -> Typeface.MONOSPACE
                StoryFontStyle.Marker -> Typeface.create("cursive", Typeface.BOLD)
                StoryFontStyle.Strong -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                else -> Typeface.DEFAULT_BOLD
            }
        }

        val text = if (spec.fontStyle.uppercaseLatin) spec.text.uppercase() else spec.text
        val maxTextWidth = (EXPORT_WIDTH * 0.82f).toInt()
        val alignment = when (spec.align) {
            StoryTextAlign.Left -> Layout.Alignment.ALIGN_NORMAL
            StoryTextAlign.Right -> Layout.Alignment.ALIGN_OPPOSITE
            StoryTextAlign.Center -> Layout.Alignment.ALIGN_CENTER
        }

        @Suppress("DEPRECATION")
        val staticLayout = StaticLayout(
            text,
            textPaint,
            maxTextWidth,
            alignment,
            1.2f,
            0f,
            false,
        )

        val totalHeight = staticLayout.height.toFloat()
        val totalWidth = staticLayout.width.toFloat()

        canvas.save()
        canvas.translate(-totalWidth / 2f, -totalHeight / 2f)

        // Draw badge background if any
        if (spec.background == StoryTextBackground.Solid || spec.background == StoryTextBackground.Soft) {
            val padX = spec.fontSize * stageScale * 0.5f
            val padY = spec.fontSize * stageScale * 0.18f
            val radius = spec.fontSize * stageScale * 0.55f

            val rects = (0 until staticLayout.lineCount).map { i ->
                LineRect(
                    left = staticLayout.getLineLeft(i) - padX,
                    top = staticLayout.getLineTop(i).toFloat() - padY,
                    right = staticLayout.getLineRight(i) + padX,
                    bottom = staticLayout.getLineBottom(i).toFloat() + padY,
                )
            }
            val commands = StoryLineBadge.build(rects, radius)
            val path = Path()
            commands.forEach { cmd ->
                when (cmd) {
                    is BadgeCommand.MoveTo -> path.moveTo(cmd.x, cmd.y)
                    is BadgeCommand.LineTo -> path.lineTo(cmd.x, cmd.y)
                    is BadgeCommand.CubicTo -> path.cubicTo(cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x3, cmd.y3)
                    is BadgeCommand.Close -> path.close()
                }
            }

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = parseColor(spec.backgroundColor)
            }
            canvas.drawPath(path, bgPaint)
        }

        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawWidgetElement(canvas: Canvas, element: StoryElement, stageScale: Float) {
        val label = element.content?.ifBlank { null }
            ?: element.location?.name
            ?: element.mention?.username?.let { "@$it" }
            ?: element.link?.title
            ?: element.poll?.question
            ?: "ویجت استوری"

        val widgetWidth = 240f * stageScale
        val widgetHeight = 52f * stageScale
        val radius = 26f * stageScale

        val rect = RectF(-widgetWidth / 2f, -widgetHeight / 2f, widgetWidth / 2f, widgetHeight / 2f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.parseColor("#D9000000") // dark glass
        }
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f * stageScale
            color = android.graphics.Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val yOffset = ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(label, 0f, -yOffset, textPaint)
    }

    private fun parseColor(color: androidx.compose.ui.graphics.Color): Int {
        val argb = color.value.toLong()
        return (argb and 0xFFFFFFFFL).toInt()
    }
}
