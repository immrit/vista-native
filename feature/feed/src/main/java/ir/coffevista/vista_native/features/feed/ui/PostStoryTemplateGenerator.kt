package ir.coffevista.vista_native.features.feed.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import ir.coffevista.vista_native.core.designsystem.R as DesignR
import ir.coffevista.vista_native.features.feed.data.FeedPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * تولید تصویر قالب استوری ویستا با کیفیت بسیار بالا، تمیز، مدرن و مینیمال
 * منطبق با طراحی نسخه فلاتر (VistaStoryTemplateWidget / X / Threads)
 */
object PostStoryTemplateGenerator {

    private const val CANVAS_WIDTH = 1080
    private const val CANVAS_HEIGHT = 1920
    private const val CARD_WIDTH = 860f
    private const val CARD_RADIUS = 28f
    private const val TEMP_DIR_NAME = "story_shares"
    private const val MAX_TEMP_FILE_AGE_MS = 3600_000L // 1 hour

    suspend fun generateTemplate(
        context: Context,
        post: FeedPost,
        theme: String = "dark",
    ): File = withContext(Dispatchers.IO) {
        cleanOldTempFiles(context)

        val avatarBitmap = loadBitmapSafe(context, post.authorAvatarUrl)
        val mediaUrl = post.primaryImageUrl ?: post.videoThumbnailUrl
        val mediaBitmap = loadBitmapSafe(context, mediaUrl)

        val logoBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, DesignR.drawable.ic_vista_logo_black)
        }.getOrNull()

        val bitmap = renderToBitmap(
            post = post,
            theme = theme,
            avatarBitmap = avatarBitmap,
            mediaBitmap = mediaBitmap,
            logoBitmap = logoBitmap,
        )

        val dir = File(context.cacheDir, TEMP_DIR_NAME).apply { mkdirs() }
        val file = File(dir, "story_post_${post.id}_${theme}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()
        file
    }

    fun cleanOldTempFiles(context: Context) {
        try {
            val dir = File(context.cacheDir, TEMP_DIR_NAME)
            if (dir.exists() && dir.isDirectory) {
                val threshold = System.currentTimeMillis() - MAX_TEMP_FILE_AGE_MS
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < threshold) {
                        file.delete()
                    }
                }
            }
        } catch (_: Throwable) {
            // Best effort cleanup
        }
    }

    internal fun renderToBitmap(
        post: FeedPost,
        theme: String,
        avatarBitmap: Bitmap? = null,
        mediaBitmap: Bitmap? = null,
        logoBitmap: Bitmap? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawThemeBackground(canvas, theme)
        drawPostCard(canvas, post, theme, avatarBitmap, mediaBitmap, logoBitmap)

        return bitmap
    }

    // =========================================================================
    // BACKGROUNDS (Dark, Light, Vista)
    // =========================================================================

    private fun drawThemeBackground(canvas: Canvas, theme: String) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (theme.lowercase()) {
            "light" -> {
                // 1. Luxury warm linen gradient
                bgPaint.shader = LinearGradient(
                    0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(),
                    intArrayOf(
                        Color.parseColor("#F7F2EC"),
                        Color.parseColor("#EDE6DE"),
                        Color.parseColor("#E4DDD6"),
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)

                // 2. Soft pastel ambient color blobs
                drawGlowBlob(canvas, CANVAS_WIDTH * 0.85f, CANVAS_HEIGHT * 0.15f, CANVAS_WIDTH * 0.45f, Color.argb(56, 255, 180, 162))
                drawGlowBlob(canvas, CANVAS_WIDTH * 0.12f, CANVAS_HEIGHT * 0.78f, CANVAS_WIDTH * 0.40f, Color.argb(46, 180, 197, 255))
                drawGlowBlob(canvas, CANVAS_WIDTH * 0.55f, CANVAS_HEIGHT * 0.55f, CANVAS_WIDTH * 0.35f, Color.argb(31, 255, 214, 165))

                // 3. Subtle dot grid
                drawDotGrid(canvas, Color.argb(12, 0, 0, 0))

                // 4. Subtle watermark
                drawWatermark(canvas, "VISTA", Color.argb(14, 0, 0, 0))
            }

            "vista" -> {
                // Vista Signature Theme: pure white with kinetic typography black concentric rings
                canvas.drawColor(Color.WHITE)

                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 120f
                }

                val r1Center = Pair(CANVAS_WIDTH * 0.10f, CANVAS_HEIGHT * 0.10f)
                val r1Radius = CANVAS_WIDTH * 0.40f
                canvas.drawCircle(r1Center.first, r1Center.second, r1Radius, ringPaint)
                drawVistaRingText(canvas, r1Center.first, r1Center.second, r1Radius)

                val r2Center = Pair(CANVAS_WIDTH * 0.90f, CANVAS_HEIGHT * 0.50f)
                val r2Radius = CANVAS_WIDTH * 0.35f
                canvas.drawCircle(r2Center.first, r2Center.second, r2Radius, ringPaint)
                drawVistaRingText(canvas, r2Center.first, r2Center.second, r2Radius)

                val r3Center = Pair(CANVAS_WIDTH * 0.20f, CANVAS_HEIGHT * 0.90f)
                val r3Radius = CANVAS_WIDTH * 0.45f
                canvas.drawCircle(r3Center.first, r3Center.second, r3Radius, ringPaint)
                drawVistaRingText(canvas, r3Center.first, r3Center.second, r3Radius)
            }

            else -> {
                // dark (default) - Deep 4-stop OLED background
                bgPaint.shader = LinearGradient(
                    0f, 0f, 0f, CANVAS_HEIGHT.toFloat(),
                    intArrayOf(
                        Color.parseColor("#030303"),
                        Color.parseColor("#0D0D0D"),
                        Color.parseColor("#060606"),
                        Color.parseColor("#101010"),
                    ),
                    floatArrayOf(0f, 0.35f, 0.7f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)

                // Atmospheric glows
                drawGlowBlob(canvas, CANVAS_WIDTH * 0.50f, CANVAS_HEIGHT * 0.38f, CANVAS_WIDTH * 0.50f, Color.argb(23, 255, 255, 255))
                drawGlowBlob(canvas, CANVAS_WIDTH * 0.18f, CANVAS_HEIGHT * 0.20f, CANVAS_WIDTH * 0.28f, Color.argb(16, 74, 111, 165))
                drawGlowBlob(canvas, CANVAS_WIDTH * 0.82f, CANVAS_HEIGHT * 0.72f, CANVAS_WIDTH * 0.32f, Color.argb(13, 224, 168, 56))

                // Ultra-fine geometric accent rings
                drawAccentRings(canvas, Color.argb(14, 255, 255, 255), 1.5f)
                drawSingleRing(canvas, CANVAS_WIDTH * 0.50f, CANVAS_HEIGHT * 0.82f, CANVAS_WIDTH * 0.55f, Color.argb(7, 255, 255, 255), 1f)

                // Dot grid texture
                drawDotGrid(canvas, Color.argb(7, 255, 255, 255))

                // Vignette around edges
                drawVignette(canvas)

                // Large angled watermark
                drawWatermark(canvas, "VISTA", Color.argb(6, 255, 255, 255))
            }
        }
    }

    private fun drawGlowBlob(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(color, Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawDotGrid(canvas: Canvas, color: Int) {
        val spacing = 48f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
        }
        var y = spacing
        while (y < CANVAS_HEIGHT) {
            var x = spacing
            while (x < CANVAS_WIDTH) {
                canvas.drawCircle(x, y, 1.2f, paint)
                x += spacing
            }
            y += spacing
        }
    }

    private fun drawAccentRings(canvas: Canvas, color: Int, strokeWidth: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        canvas.drawCircle(CANVAS_WIDTH * 0.12f, CANVAS_HEIGHT * 0.18f, CANVAS_WIDTH * 0.42f, paint)
        canvas.drawCircle(CANVAS_WIDTH * 0.88f, CANVAS_HEIGHT * 0.72f, CANVAS_WIDTH * 0.38f, paint)
    }

    private fun drawSingleRing(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, strokeWidth: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawVignette(canvas: Canvas) {
        val cx = CANVAS_WIDTH * 0.5f
        val cy = CANVAS_HEIGHT * 0.45f
        val radius = CANVAS_WIDTH * 0.75f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(115, 0, 0, 0)),
                floatArrayOf(0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), paint)
    }

    private fun drawWatermark(canvas: Canvas, text: String, color: Int) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 140f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        val textWidth = textPaint.measureText(text)
        canvas.save()
        canvas.translate(CANVAS_WIDTH * 0.5f, CANVAS_HEIGHT * 0.88f)
        canvas.rotate(-13f)
        canvas.drawText(text, -textWidth / 2f, 0f, textPaint)
        canvas.restore()
    }

    private fun drawVistaRingText(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val text = "VISTA"
        val textWidth = textPaint.measureText(text)
        val circumference = (2 * Math.PI * radius).toFloat()
        val repetitions = (circumference / (textWidth * 1.15f)).toInt().coerceIn(4, 18)

        for (i in 0 until repetitions) {
            val angle = (i.toFloat() / repetitions.toFloat()) * (2 * Math.PI.toFloat())
            val x = centerX + radius * cos(angle.toDouble()).toFloat()
            val y = centerY + radius * sin(angle.toDouble()).toFloat()

            canvas.save()
            canvas.translate(x, y)
            val angleDeg = Math.toDegrees(angle.toDouble()).toFloat() + 90f
            canvas.rotate(angleDeg)
            canvas.drawText(text, -textWidth / 2f, 28f, textPaint)
            canvas.restore()
        }
    }

    // =========================================================================
    // POST CARD RENDERING (Exact Flutter Dimensions & Geometry)
    // =========================================================================

    private fun drawPostCard(
        canvas: Canvas,
        post: FeedPost,
        theme: String,
        avatarBitmap: Bitmap?,
        mediaBitmap: Bitmap?,
        logoBitmap: Bitmap?,
    ) {
        val cardLeft = (CANVAS_WIDTH - CARD_WIDTH) / 2f
        val cardRight = cardLeft + CARD_WIDTH
        val padding = 36f
        val innerWidth = CARD_WIDTH - (padding * 2)

        val rawText = post.content.orEmpty().trim()
        val isRtl = isPersianText(rawText) || isPersianText(post.authorFullName)

        // 1. Measure Text
        val limitedText = if (rawText.length > 280) "${rawText.take(280)}…" else rawText
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#141414")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = -0.01f
        }

        val textLayout = if (limitedText.isNotEmpty()) {
            StaticLayout.Builder.obtain(limitedText, 0, limitedText.length, textPaint, innerWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
                .setLineSpacing(0f, 1.45f)
                .setMaxLines(10)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        } else null

        // Dimensions of sections
        val brandMarkHeight = 36f + 28f // 36px icon + 28px margin
        val avatarDiameter = 68f // 34px radius * 2
        val authorRowHeight = avatarDiameter
        val textSectionHeight = if (textLayout != null) textLayout.height.toFloat() + 28f else 0f

        val hasMedia = mediaBitmap != null
        // 16 / 10 aspect ratio exactly matching Flutter
        val mediaSectionHeight = if (hasMedia) (innerWidth * 10f / 16f) + 28f else 0f

        val hasMusic = !post.musicTitle.isNullOrBlank() || !post.musicUrl.isNullOrBlank()
        val musicSectionHeight = if (hasMusic) 80f + 24f else 0f

        val statsSectionHeight = 28f + 34f + 24f // padding top + stats + padding bottom
        val brandFooterHeight = 54f // top border + height

        val totalCardHeight = 32f + brandMarkHeight + authorRowHeight + textSectionHeight +
                musicSectionHeight + mediaSectionHeight + statsSectionHeight + brandFooterHeight

        // Vertical positioning (Alignment(0, -0.08) in Flutter, slightly above center)
        val targetCenterY = (CANVAS_HEIGHT * 0.5f) - (CANVAS_HEIGHT * 0.04f)
        val cardTop = (targetCenterY - (totalCardHeight / 2f)).coerceAtLeast(60f)
        val cardBottom = cardTop + totalCardHeight
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        // 2. Draw Luxurious Multi-Layer Soft Drop Shadow
        drawCardDropShadow(canvas, cardRect, theme)

        // 3. Draw Card Background
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawRoundRect(cardRect, CARD_RADIUS, CARD_RADIUS, cardPaint)

        var currentY = cardTop + 32f

        // 4. Brand Mark (Top-Start)
        val brandSize = 36f
        val brandX = if (isRtl) cardRight - padding - brandSize else cardLeft + padding
        if (logoBitmap != null) {
            val logoDst = RectF(brandX, currentY, brandX + brandSize, currentY + brandSize)
            canvas.drawBitmap(logoBitmap, null, logoDst, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            drawVistaBolt(canvas, brandX + brandSize / 2f, currentY + brandSize / 2f, brandSize, Color.parseColor("#141414"))
        }
        currentY += brandMarkHeight

        // 5. Author Row
        val avatarRadius = 34f
        val avatarCenterX = if (isRtl) cardRight - padding - avatarRadius else cardLeft + padding + avatarRadius
        val avatarCenterY = currentY + avatarRadius

        drawAvatar(canvas, avatarBitmap, avatarCenterX, avatarCenterY, avatarRadius, post)

        // Author Name & Handle
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#141414")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val handlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#737373")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }

        val authorName = post.authorFullName.ifBlank { post.authorUsername.orEmpty().ifBlank { "کاربر ویستا" } }
        val handleText = post.authorUsername?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: ""

        if (isRtl) {
            val textStartX = avatarCenterX - avatarRadius - 20f
            canvas.drawText(authorName, textStartX, avatarCenterY - 4f, namePaint)
            if (post.authorIsVerified) {
                val nameWidth = namePaint.measureText(authorName)
                val badgeX = textStartX - nameWidth - 18f
                drawVerifiedBadge(canvas, badgeX, avatarCenterY - 14f, 11f)
            }
            if (handleText.isNotEmpty()) {
                canvas.drawText(handleText, textStartX, avatarCenterY + 24f, handlePaint)
            }
        } else {
            val textStartX = avatarCenterX + avatarRadius + 20f
            canvas.drawText(authorName, textStartX, avatarCenterY - 4f, namePaint)
            if (post.authorIsVerified) {
                val nameWidth = namePaint.measureText(authorName)
                val badgeX = textStartX + nameWidth + 18f
                drawVerifiedBadge(canvas, badgeX, avatarCenterY - 14f, 11f)
            }
            if (handleText.isNotEmpty()) {
                canvas.drawText(handleText, textStartX, avatarCenterY + 24f, handlePaint)
            }
        }

        currentY += authorRowHeight

        // 6. Post Content Text
        if (textLayout != null) {
            currentY += 28f
            canvas.save()
            canvas.translate(cardLeft + padding, currentY)
            textLayout.draw(canvas)
            canvas.restore()
            currentY += textLayout.height.toFloat()
        }

        // 7. Media (16:10 with Center-Crop)
        if (mediaBitmap != null) {
            currentY += 28f
            val mediaHeight = innerWidth * 10f / 16f
            val mediaRect = RectF(cardLeft + padding, currentY, cardRight - padding, currentY + mediaHeight)

            val clipPath = Path().apply {
                addRoundRect(mediaRect, 18f, 18f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)

            val srcCrop = calculateCenterCropSrcRect(mediaBitmap.width, mediaBitmap.height, mediaRect.width(), mediaRect.height())
            canvas.drawBitmap(mediaBitmap, srcCrop, mediaRect, Paint(Paint.FILTER_BITMAP_FLAG))

            // Video Play Button Overlay
            if (!post.videoUrl.isNullOrBlank()) {
                val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(45, 0, 0, 0)
                }
                canvas.drawRect(mediaRect, overlayPaint)
                drawPlayButton(canvas, mediaRect.centerX(), mediaRect.centerY(), 96f)
            }
            canvas.restore()
            currentY += mediaHeight
        }

        // 8. Music Track Banner
        if (hasMusic) {
            currentY += 24f
            val musicRect = RectF(cardLeft + padding, currentY, cardRight - padding, currentY + 80f)
            drawMusicBanner(canvas, musicRect, post, isRtl)
            currentY += 80f
        }

        // 9. Stats & Date Row
        currentY += 28f
        val statsY = currentY + 18f
        val statPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#737373")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#999999")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val dateText = feedRelativeTime(post.createdAt)

        if (isRtl) {
            // Stats on right, date on left
            var cursorX = cardRight - padding
            if (!post.hideLikeCount) {
                drawHeart(canvas, cursorX - 14f, statsY - 8f, 28f, if (post.isLiked) Color.parseColor("#E0245E") else Color.parseColor("#737373"), post.isLiked)
                cursorX -= 34f
                val countText = formatNumber(post.likeCount)
                statPaint.color = if (post.isLiked) Color.parseColor("#E0245E") else Color.parseColor("#737373")
                statPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(countText, cursorX, statsY, statPaint)
                cursorX -= statPaint.measureText(countText) + 28f
            }
            if (!post.hideCommentCount) {
                drawCommentBubble(canvas, cursorX - 14f, statsY - 8f, 28f, Color.parseColor("#737373"))
                cursorX -= 34f
                val countText = formatNumber(post.commentCount)
                statPaint.color = Color.parseColor("#737373")
                statPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(countText, cursorX, statsY, statPaint)
            }
            // Date at left
            datePaint.textAlign = Paint.Align.LEFT
            canvas.drawText(dateText, cardLeft + padding, statsY, datePaint)
        } else {
            // Stats on left, date on right
            var cursorX = cardLeft + padding
            if (!post.hideLikeCount) {
                drawHeart(canvas, cursorX + 14f, statsY - 8f, 28f, if (post.isLiked) Color.parseColor("#E0245E") else Color.parseColor("#737373"), post.isLiked)
                cursorX += 34f
                val countText = formatNumber(post.likeCount)
                statPaint.color = if (post.isLiked) Color.parseColor("#E0245E") else Color.parseColor("#737373")
                statPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(countText, cursorX, statsY, statPaint)
                cursorX += statPaint.measureText(countText) + 28f
            }
            if (!post.hideCommentCount) {
                drawCommentBubble(canvas, cursorX + 14f, statsY - 8f, 28f, Color.parseColor("#737373"))
                cursorX += 34f
                val countText = formatNumber(post.commentCount)
                statPaint.color = Color.parseColor("#737373")
                statPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(countText, cursorX, statsY, statPaint)
            }
            // Date at right
            datePaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(dateText, cardRight - padding, statsY, datePaint)
        }

        currentY += 34f + 24f

        // 10. Brand Footer
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F0F0F0")
            strokeWidth = 1.5f
        }
        canvas.drawLine(cardLeft, currentY, cardRight, currentY, dividerPaint)

        val footerLogoSize = 22f
        val footerY = currentY + 16f
        val footerBrandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#737373")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = -0.02f
        }
        val sitePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AAAAAA")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        if (logoBitmap != null) {
            val logoDst = RectF(cardLeft + padding, footerY, cardLeft + padding + footerLogoSize, footerY + footerLogoSize)
            canvas.drawBitmap(logoBitmap, null, logoDst, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            drawVistaBolt(canvas, cardLeft + padding + footerLogoSize / 2f, footerY + footerLogoSize / 2f, footerLogoSize, Color.parseColor("#737373"))
        }

        canvas.drawText("Vista", cardLeft + padding + footerLogoSize + 10f, footerY + 18f, footerBrandPaint)
        canvas.drawText("cafevista.ir", cardRight - padding, footerY + 18f, sitePaint)
    }

    // =========================================================================
    // VISUAL DETAILS & HELPERS
    // =========================================================================

    private fun drawCardDropShadow(canvas: Canvas, cardRect: RectF, theme: String) {
        val baseAlpha = when (theme.lowercase()) {
            "light" -> 40
            "vista" -> 35
            else -> 115
        }

        try {
            // Layer 1: Deep ambient soft glow shadow
            val ambientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(baseAlpha, 0, 0, 0)
                maskFilter = BlurMaskFilter(64f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawRoundRect(
                RectF(cardRect.left + 4f, cardRect.top + 28f, cardRect.right - 4f, cardRect.bottom + 28f),
                CARD_RADIUS + 4f,
                CARD_RADIUS + 4f,
                ambientPaint,
            )

            // Layer 2: Key light crisp shadow
            val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((baseAlpha * 0.45f).toInt(), 0, 0, 0)
                maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawRoundRect(
                RectF(cardRect.left, cardRect.top + 8f, cardRect.right, cardRect.bottom + 8f),
                CARD_RADIUS + 2f,
                CARD_RADIUS + 2f,
                keyPaint,
            )
        } catch (_: Throwable) {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(baseAlpha, 0, 0, 0)
            }
            canvas.drawRoundRect(
                RectF(cardRect.left - 2f, cardRect.top + 12f, cardRect.right + 2f, cardRect.bottom + 16f),
                CARD_RADIUS + 4f,
                CARD_RADIUS + 4f,
                fallbackPaint,
            )
        }
    }

    private fun drawAvatar(
        canvas: Canvas,
        avatarBitmap: Bitmap?,
        cx: Float,
        cy: Float,
        radius: Float,
        post: FeedPost,
    ) {
        if (avatarBitmap != null) {
            val clipPath = Path().apply {
                addCircle(cx, cy, radius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            val srcCrop = calculateCenterCropSrcRect(avatarBitmap.width, avatarBitmap.height, radius * 2, radius * 2)
            val dstRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawBitmap(avatarBitmap, srcCrop, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EFEFEF")
            }
            canvas.drawCircle(cx, cy, radius, bgPaint)

            val initial = (post.authorFullName.firstOrNull() ?: post.authorUsername?.firstOrNull() ?: 'U')
                .uppercaseChar().toString()
            val initialPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#666666")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(initial, cx, cy + 10f, initialPaint)
        }

        // Circular border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8E8E8")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(cx, cy, radius, borderPaint)
    }

    private fun drawVerifiedBadge(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D9BF0")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = radius * 0.22f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val checkPath = Path().apply {
            moveTo(cx - radius * 0.42f, cy)
            lineTo(cx - radius * 0.10f, cy + radius * 0.32f)
            lineTo(cx + radius * 0.44f, cy - radius * 0.30f)
        }
        canvas.drawPath(checkPath, checkPaint)
    }

    private fun drawPlayButton(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val radius = size / 2f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(cx, cy, radius, borderPaint)

        val trianglePath = Path().apply {
            val h = radius * 0.55f
            moveTo(cx - h * 0.6f, cy - h)
            lineTo(cx + h * 0.9f, cy)
            lineTo(cx - h * 0.6f, cy + h)
            close()
        }
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(trianglePath, iconPaint)
    }

    private fun drawMusicBanner(canvas: Canvas, rect: RectF, post: FeedPost, isRtl: Boolean) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                rect.left, 0f, rect.right, 0f,
                intArrayOf(Color.parseColor("#1DB954"), Color.parseColor("#169C46")),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(rect, 18f, 18f, bgPaint)

        // Square icon box on left
        val iconBoxSize = 52f
        val iconBoxRect = RectF(
            rect.left + 14f,
            rect.centerY() - iconBoxSize / 2f,
            rect.left + 14f + iconBoxSize,
            rect.centerY() + iconBoxSize / 2f,
        )
        val iconBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(50, 255, 255, 255)
        }
        canvas.drawRoundRect(iconBoxRect, 12f, 12f, iconBoxPaint)
        drawMusicNote(canvas, iconBoxRect.centerX(), iconBoxRect.centerY(), 28f, Color.WHITE)

        // Equalizer wave on right
        drawEqualizerWave(canvas, rect.right - 20f, rect.centerY(), Color.argb(220, 255, 255, 255))

        // Titles
        val titleText = post.musicTitle?.ifBlank { "قطعه صوتی" } ?: "موزیک"
        val artistText = post.authorUsername?.takeIf { it.isNotBlank() } ?: "Vista"

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 255, 255, 255)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }

        val textLeft = iconBoxRect.right + 16f
        val textRight = rect.right - 60f

        if (isRtl) {
            canvas.drawText(titleText, textRight, rect.centerY() - 2f, titlePaint)
            canvas.drawText(artistText, textRight, rect.centerY() + 26f, artistPaint)
        } else {
            canvas.drawText(titleText, textLeft, rect.centerY() - 2f, titlePaint)
            canvas.drawText(artistText, textLeft, rect.centerY() + 26f, artistPaint)
        }
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int, filled: Boolean) {
        val s = size / 24f
        val path = Path().apply {
            moveTo(cx, cy - 4f * s)
            cubicTo(cx - 1f * s, cy - 8f * s, cx - 8f * s, cy - 8f * s, cx - 8f * s, cy - 3f * s)
            cubicTo(cx - 8f * s, cy + 2f * s, cx - 3f * s, cy + 6.5f * s, cx, cy + 9.5f * s)
            cubicTo(cx + 3f * s, cy + 6.5f * s, cx + 8f * s, cy + 2f * s, cx + 8f * s, cy - 3f * s)
            cubicTo(cx + 8f * s, cy - 8f * s, cx + 1f * s, cy - 8f * s, cx, cy - 4f * s)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            if (filled) {
                style = Paint.Style.FILL
            } else {
                style = Paint.Style.STROKE
                strokeWidth = 2.4f * s
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawCommentBubble(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val s = size / 24f
        val path = Path().apply {
            val rect = RectF(cx - 8.5f * s, cy - 7f * s, cx + 8.5f * s, cy + 5f * s)
            addRoundRect(rect, 4f * s, 4f * s, Path.Direction.CW)
            moveTo(cx - 3f * s, cy + 5f * s)
            lineTo(cx - 6f * s, cy + 8.5f * s)
            lineTo(cx - 1f * s, cy + 5f * s)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * s
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, paint)
    }

    private fun drawVistaBolt(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val s = size / 24f
        val path = Path().apply {
            moveTo(cx + 1f * s, cy - 10f * s)
            lineTo(cx - 6f * s, cy + 1f * s)
            lineTo(cx - 1f * s, cy + 1f * s)
            lineTo(cx - 2f * s, cy + 10f * s)
            lineTo(cx + 6f * s, cy - 2f * s)
            lineTo(cx + 1f * s, cy - 2f * s)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, paint)
    }

    private fun drawEqualizerWave(canvas: Canvas, right: Float, cy: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        val heights = floatArrayOf(14f, 26f, 18f, 28f, 12f)
        for (i in heights.indices) {
            val x = right - (heights.size - 1 - i) * 7f
            val h = heights[i]
            canvas.drawLine(x, cy - h / 2f, x, cy + h / 2f, paint)
        }
    }

    private fun drawMusicNote(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val s = size / 24f
        val path = Path().apply {
            moveTo(cx + 3f * s, cy - 7f * s)
            lineTo(cx + 3f * s, cy + 4f * s)
            addCircle(cx, cy + 4f * s, 3f * s, Path.Direction.CW)
            moveTo(cx + 3f * s, cy - 7f * s)
            cubicTo(cx + 5f * s, cy - 7f * s, cx + 7f * s, cy - 5f * s, cx + 7f * s, cy - 2f * s)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2.4f * s
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(path, paint)
    }

    private fun calculateCenterCropSrcRect(
        bitmapWidth: Int,
        bitmapHeight: Int,
        targetWidth: Float,
        targetHeight: Float,
    ): Rect {
        val srcAspect = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        val targetAspect = targetWidth / targetHeight
        return if (srcAspect > targetAspect) {
            val cropWidth = (bitmapHeight * targetAspect).toInt()
            val left = ((bitmapWidth - cropWidth) / 2).coerceAtLeast(0)
            Rect(left, 0, (left + cropWidth).coerceAtMost(bitmapWidth), bitmapHeight)
        } else {
            val cropHeight = (bitmapWidth / targetAspect).toInt()
            val top = ((bitmapHeight - cropHeight) / 2).coerceAtLeast(0)
            Rect(0, top, bitmapWidth, (top + cropHeight).coerceAtMost(bitmapHeight))
        }
    }

    private fun isPersianText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val persianRegex = Regex("[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]")
        return persianRegex.containsMatchIn(text)
    }

    private fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", number / 1_000_000f)
            number >= 1_000L -> String.format(java.util.Locale.US, "%.1fK", number / 1_000f)
            else -> number.toString()
        }
    }

    private suspend fun loadBitmapSafe(context: Context, url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        try {
            withTimeoutOrNull(4000L) {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap
                    } else {
                        val w = drawable.intrinsicWidth.coerceAtLeast(1)
                        val h = drawable.intrinsicHeight.coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val cv = Canvas(bmp)
                        drawable.setBounds(0, 0, w, h)
                        drawable.draw(cv)
                        bmp
                    }
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
