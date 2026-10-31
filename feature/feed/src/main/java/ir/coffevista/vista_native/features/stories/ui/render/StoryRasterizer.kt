package ir.coffevista.vista_native.features.stories.ui.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import ir.coffevista.vista_native.features.stories.domain.StoryElement
import ir.coffevista.vista_native.features.stories.ui.canvas.LegacyStoryMapper
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryStageSpec
import ir.coffevista.vista_native.features.stories.ui.canvas.StoryTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * High-resolution (1080x1920) offscreen rasterizer for exporting stories with 1:1 preview parity.
 * Mirrors Telegram's StoryEntry export pipeline:
 *  - Layer 0: Dominant dual-color background gradient extracted from media
 *  - Layer 1: Transformable primary media layer
 *  - Layer 2: Multi-shader freehand drawing layer (Pen, Marker, Neon Glow, Eraser)
 *  - Layer 3: Z-ordered entities (Continuous multi-line pill text, 4-theme widgets, Photo cutouts)
 */
object StoryRasterizer {

    const val EXPORT_WIDTH = 1080
    const val EXPORT_HEIGHT = 1920

    /**
     * Extracts top and bottom dominant colors from a media bitmap (Telegram GradientTools parity).
     */
    internal fun extractDominantColors(bitmap: Bitmap?): StoryDominantColors {
        if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return StoryDominantColors()
        }
        val w = bitmap.width
        val h = bitmap.height
        val sampleCols = 12
        val sampleRows = 6

        fun averageRegion(startY: Int, endY: Int): Color {
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0L
            val stepX = (w / sampleCols).coerceAtLeast(1)
            val stepY = ((endY - startY) / sampleRows).coerceAtLeast(1)
            var y = startY
            while (y < endY && y < h) {
                var x = 0
                while (x < w) {
                    val px = bitmap.getPixel(x, y)
                    rSum += android.graphics.Color.red(px)
                    gSum += android.graphics.Color.green(px)
                    bSum += android.graphics.Color.blue(px)
                    count++
                    x += stepX
                }
                y += stepY
            }
            if (count == 0L) return Color(0xFF1A1B2F)
            val r = (rSum / count).toInt().coerceIn(0, 255)
            val g = (gSum / count).toInt().coerceIn(0, 255)
            val b = (bSum / count).toInt().coerceIn(0, 255)
            return Color(android.graphics.Color.rgb(r, g, b))
        }

        val top = averageRegion(0, (h * 0.25f).toInt().coerceAtLeast(1))
        val bottom = averageRegion((h * 0.75f).toInt().coerceAtMost(h - 1), h)
        return StoryDominantColors(topColor = top, bottomColor = bottom)
    }

    internal suspend fun loadMediaBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            (result.drawable as? BitmapDrawable)?.bitmap
        } else null
    }

    suspend fun renderStoryToBitmap(
        context: Context,
        mediaUri: Uri,
        elements: List<StoryElement>,
        mediaTransform: StoryTransform = StoryTransform(cx = 0.5f, cy = 0.5f, scale = 1f, rotation = 0f),
    ): Bitmap = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(EXPORT_WIDTH, EXPORT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 0. Load Media & Draw Layer 0 Dominant Dual-Color Background Gradient
        val bgBitmap = loadMediaBitmap(context, mediaUri)
        val dominant = extractDominantColors(bgBitmap)
        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, EXPORT_HEIGHT.toFloat(),
                parseColor(dominant.topColor),
                parseColor(dominant.bottomColor),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, EXPORT_WIDTH.toFloat(), EXPORT_HEIGHT.toFloat(), gradPaint)

        // 1. Draw Layer 1 Transformable Primary Media (Fit inside 9:16 stage over dominant gradient)
        if (bgBitmap != null) {
            canvas.save()
            val cxPx = mediaTransform.cx * EXPORT_WIDTH
            val cyPx = mediaTransform.cy * EXPORT_HEIGHT
            canvas.translate(cxPx, cyPx)
            canvas.rotate(mediaTransform.rotation)
            canvas.scale(mediaTransform.scale, mediaTransform.scale)
            canvas.translate(-EXPORT_WIDTH * 0.5f, -EXPORT_HEIGHT * 0.5f)
            drawCenterFit(canvas, bgBitmap, EXPORT_WIDTH, EXPORT_HEIGHT)
            canvas.restore()
        }

        val stageScale = EXPORT_WIDTH / StoryStageSpec.RefWidth // 3.0f

        // 2. Draw Layer 2 Freehand Drawing Layer (with offscreen saveLayer for Eraser & Neon)
        val drawing = elements.firstOrNull { it.type == "drawing" }
        if (drawing != null && !drawing.content.isNullOrBlank()) {
            val strokes = StoryDrawingCodec.decode(drawing.content)
            val layerId = canvas.saveLayer(0f, 0f, EXPORT_WIDTH.toFloat(), EXPORT_HEIGHT.toFloat(), null)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
            }
            strokes.forEach { stroke ->
                val strokeColor = stroke.argb
                val strokeWidthPx = stroke.width * stageScale
                strokePaint.maskFilter = null
                strokePaint.xfermode = null
                strokePaint.color = strokeColor
                strokePaint.strokeWidth = strokeWidthPx
                strokePaint.strokeCap = when (stroke.brush) {
                    StoryBrush.Marker -> Paint.Cap.SQUARE
                    else -> Paint.Cap.ROUND
                }
                when (stroke.brush) {
                    StoryBrush.Marker -> {
                        strokePaint.alpha = (android.graphics.Color.alpha(strokeColor) * 0.5f).toInt()
                        strokePaint.strokeWidth = strokeWidthPx * 1.8f
                    }
                    StoryBrush.Eraser -> {
                        strokePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        strokePaint.strokeWidth = strokeWidthPx * 1.6f
                    }
                    else -> {
                        strokePaint.alpha = android.graphics.Color.alpha(strokeColor)
                    }
                }

                if (stroke.points.size > 1) {
                    val path = Path()
                    val p0 = stroke.points[0]
                    path.moveTo(p0.first * EXPORT_WIDTH, p0.second * EXPORT_HEIGHT)
                    for (i in 1 until stroke.points.size - 1) {
                        val pt = stroke.points[i]
                        val next = stroke.points[i + 1]
                        val midX = (pt.first + next.first) * 0.5f * EXPORT_WIDTH
                        val midY = (pt.second + next.second) * 0.5f * EXPORT_HEIGHT
                        path.quadTo(pt.first * EXPORT_WIDTH, pt.second * EXPORT_HEIGHT, midX, midY)
                    }
                    val last = stroke.points.last()
                    path.lineTo(last.first * EXPORT_WIDTH, last.second * EXPORT_HEIGHT)

                    if (stroke.brush == StoryBrush.Neon) {
                        val glowPaint = Paint(strokePaint).apply {
                            color = strokeColor
                            alpha = 180
                            strokeWidth = strokeWidthPx * 2.2f
                            maskFilter = BlurMaskFilter(strokeWidthPx * 1.2f, BlurMaskFilter.Blur.NORMAL)
                        }
                        canvas.drawPath(path, glowPaint)
                        val corePaint = Paint(strokePaint).apply {
                            color = android.graphics.Color.WHITE
                            alpha = 245
                            strokeWidth = strokeWidthPx * 0.58f
                        }
                        canvas.drawPath(path, corePaint)
                    } else {
                        canvas.drawPath(path, strokePaint)
                    }
                } else if (stroke.points.size == 1) {
                    val pt = stroke.points[0]
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = strokeColor
                        if (stroke.brush == StoryBrush.Eraser) {
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        }
                    }
                    canvas.drawCircle(pt.first * EXPORT_WIDTH, pt.second * EXPORT_HEIGHT, strokeWidthPx / 2f, fillPaint)
                }
            }
            canvas.restoreToCount(layerId)
        }

        // 3. Draw Layer 3 Story Elements in Z-order
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
                "photo" -> {
                    val cutUri = element.content?.takeIf { it.isNotBlank() }?.let(Uri::parse)
                    val cutBmp = if (cutUri != null) loadMediaBitmap(context, cutUri) else null
                    if (cutBmp != null) {
                        drawPhotoCutoutElement(canvas, cutBmp, element.variant, stageScale)
                    }
                }
                "poll", "question", "mention", "location", "link", "date", "weather", "countdown", "hashtag", "reaction", "emoji", "music" -> {
                    drawWidgetElement(canvas, element, stageScale, dominant)
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
        mediaTransform: StoryTransform = StoryTransform(cx = 0.5f, cy = 0.5f, scale = 1f, rotation = 0f),
    ): File = withContext(Dispatchers.IO) {
        val bitmap = renderStoryToBitmap(context, mediaUri, elements, mediaTransform)
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        bitmap.recycle()
        outputFile
    }

    /**
     * Exports Layer 0 (Dominant dual-color gradient) + Layer 1 (Transformable fitted primary photo)
     * into a 1080x1920 9:16 JPEG without baking interactive vector overlays, ensuring 1:1 stage
     * parity and crisp interactive widgets in [StoryPlayerScreen].
     */
    suspend fun exportBaseCanvasToFile(
        context: Context,
        mediaUri: Uri,
        mediaTransform: StoryTransform,
        outputFile: File,
    ): File? = withContext(Dispatchers.IO) {
        val bgBitmap = loadMediaBitmap(context, mediaUri) ?: return@withContext null
        val bitmap = Bitmap.createBitmap(EXPORT_WIDTH, EXPORT_HEIGHT, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            val dominant = extractDominantColors(bgBitmap)
            val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, EXPORT_HEIGHT.toFloat(),
                    parseColor(dominant.topColor),
                    parseColor(dominant.bottomColor),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, EXPORT_WIDTH.toFloat(), EXPORT_HEIGHT.toFloat(), gradPaint)

            canvas.save()
            val cxPx = mediaTransform.cx * EXPORT_WIDTH
            val cyPx = mediaTransform.cy * EXPORT_HEIGHT
            canvas.translate(cxPx, cyPx)
            canvas.rotate(mediaTransform.rotation)
            canvas.scale(mediaTransform.scale, mediaTransform.scale)
            canvas.translate(-EXPORT_WIDTH * 0.5f, -EXPORT_HEIGHT * 0.5f)
            drawCenterFit(canvas, bgBitmap, EXPORT_WIDTH, EXPORT_HEIGHT)
            canvas.restore()

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
            }
            outputFile
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawCenterFit(canvas: Canvas, bitmap: Bitmap, targetWidth: Int, targetHeight: Int) {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        if (srcW <= 0f || srcH <= 0f) return
        val scale = min(targetWidth / srcW, targetHeight / srcH)
        val dstW = srcW * scale
        val dstH = srcH * scale
        val left = (targetWidth - dstW) * 0.5f
        val top = (targetHeight - dstH) * 0.5f
        val dstRect = RectF(left, top, left + dstW, top + dstH)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, dstRect, paint)
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
            if (spec.background == StoryTextBackground.None && spec.fontStyle != StoryFontStyle.Neon) {
                setShadowLayer(10f * stageScale * 0.35f, 0f, 3f * stageScale * 0.35f, 0x94000000.toInt())
            } else if (spec.fontStyle == StoryFontStyle.Neon) {
                setShadowLayer(spec.fontSize * stageScale * 0.45f, 0f, 0f, parseColor(spec.color))
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

        // 1. Draw continuous multi-line rounded pill background (Modes Solid & Soft)
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
                if (spec.background == StoryTextBackground.Soft) {
                    alpha = 165
                }
            }
            canvas.drawPath(path, bgPaint)

            if (spec.background == StoryTextBackground.Soft) {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f * stageScale
                    color = 0x3DFFFFFF
                }
                canvas.drawPath(path, borderPaint)
            }
        }

        // 2. Draw Outlined Text Stroke pass (Mode Outline)
        if (spec.background == StoryTextBackground.Outline) {
            val strokeTextPaint = TextPaint(textPaint).apply {
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = spec.fontSize * stageScale * 0.32f
                color = parseColor(spec.backgroundColor.copy(alpha = 1f))
                clearShadowLayer()
            }
            @Suppress("DEPRECATION")
            val strokeLayout = StaticLayout(
                text,
                strokeTextPaint,
                maxTextWidth,
                alignment,
                1.2f,
                0f,
                false,
            )
            strokeLayout.draw(canvas)
        }

        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawPhotoCutoutElement(
        canvas: Canvas,
        cutoutBitmap: Bitmap,
        variant: Int,
        stageScale: Float,
    ) {
        val boxSize = 164f * stageScale
        val half = boxSize * 0.5f
        val path = Path()
        when (Math.floorMod(variant, 4)) {
            0 -> {
                val r = boxSize * 0.22f
                path.addRoundRect(RectF(-half, -half, half, half), r, r, Path.Direction.CW)
            }
            1 -> {
                path.addCircle(0f, 0f, half * 0.96f, Path.Direction.CW)
            }
            2 -> {
                val outerR = half * 0.96f
                val innerR = half * 0.46f
                for (i in 0 until 10) {
                    val rad = if (i % 2 == 0) outerR else innerR
                    val angle = Math.toRadians((i * 36.0) - 90.0)
                    val px = (rad * cos(angle)).toFloat()
                    val py = (rad * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            else -> {
                val s = boxSize * 0.45f
                path.moveTo(0f, s * 0.85f)
                path.cubicTo(-s * 1.15f, s * 0.25f, -s * 0.95f, -s * 0.85f, 0f, -s * 0.35f)
                path.cubicTo(s * 0.95f, -s * 0.85f, s * 1.15f, s * 0.25f, 0f, s * 0.85f)
                path.close()
            }
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(
            cutoutBitmap,
            null,
            RectF(-half, -half, half, half),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        canvas.restore()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xB8FFFFFF.toInt()
            strokeWidth = 2f * stageScale
        }
        canvas.drawPath(path, borderPaint)
    }

    private fun drawWidgetElement(
        canvas: Canvas,
        element: StoryElement,
        stageScale: Float,
        dominant: StoryDominantColors,
    ) {
        if (element.type == "emoji") {
            val emojiPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 56f * stageScale
                textAlign = Paint.Align.CENTER
            }
            val yOffset = (emojiPaint.descent() + emojiPaint.ascent()) / 2f
            canvas.drawText(element.content?.ifBlank { null } ?: "🔥", 0f, -yOffset, emojiPaint)
            return
        }

        val label = when (element.type) {
            "hashtag" -> "#${element.content.orEmpty()}"
            "mention" -> element.mention?.username?.let { "@$it" } ?: "@vista"
            "location" -> element.location?.name ?: "تهران"
            "link" -> element.link?.title?.ifBlank { null } ?: element.link?.url ?: "پیوند"
            "weather" -> {
                val parts = element.content.orEmpty().split("|")
                "${parts.getOrNull(1) ?: "۲۴°C"} • ${parts.getOrNull(0) ?: "تهران"}"
            }
            "music" -> element.music?.let { "${it.title} — ${it.artist}" } ?: "موزیک"
            "reaction" -> element.content?.ifBlank { null } ?: "❤️"
            else -> element.content?.ifBlank { null }
                ?: element.poll?.question
                ?: "ویجت استوری"
        }

        val variant = effectiveVariant(element)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = (if (element.type == "reaction") 28f else 16f) * stageScale
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val measuredTextW = textPaint.measureText(label)
        val widgetWidth = (measuredTextW + 44f * stageScale).coerceAtLeast(96f * stageScale)
        val widgetHeight = 46f * stageScale
        val radius = 23f * stageScale

        val rect = RectF(-widgetWidth / 2f, -widgetHeight / 2f, widgetWidth / 2f, widgetHeight / 2f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * stageScale
        }

        when (Math.floorMod(variant, 4)) {
            0 -> { // Crisp Light Solid
                bgPaint.color = android.graphics.Color.WHITE
                borderPaint.color = 0x1A000000
                textPaint.color = 0xFF121216.toInt()
            }
            1 -> { // Frosted Dark Blur
                bgPaint.color = 0xCC141418.toInt()
                borderPaint.color = 0x2EFFFFFF
                textPaint.color = android.graphics.Color.WHITE
            }
            2 -> { // Chameleon / Accent
                bgPaint.shader = LinearGradient(
                    -widgetWidth / 2f, -widgetHeight / 2f,
                    widgetWidth / 2f, widgetHeight / 2f,
                    parseColor(dominant.topColor),
                    parseColor(dominant.bottomColor),
                    Shader.TileMode.CLAMP,
                )
                borderPaint.color = 0x40FFFFFF
                textPaint.color = android.graphics.Color.WHITE
            }
            else -> { // Translucent Glass
                bgPaint.color = 0x52FFFFFF
                borderPaint.color = 0x73FFFFFF
                textPaint.color = android.graphics.Color.WHITE
            }
        }

        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, 0f, -yOffset, textPaint)
    }

    private fun parseColor(color: androidx.compose.ui.graphics.Color): Int = color.toArgb()
}
