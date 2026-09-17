package ir.coffevista.vista_native.features.feed.ui

import android.content.Context
import android.graphics.Bitmap
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
import android.text.TextPaint
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import ir.coffevista.vista_native.features.feed.data.FeedPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream

object PostStoryTemplateGenerator {

    private const val CANVAS_WIDTH = 1080
    private const val CANVAS_HEIGHT = 1920
    private const val CARD_WIDTH = 860
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

        val bitmap = renderToBitmap(post, theme, avatarBitmap, mediaBitmap)

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
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawThemeBackground(canvas, theme)
        drawPostCard(canvas, post, theme, avatarBitmap, mediaBitmap)

        return bitmap
    }

    private fun drawThemeBackground(canvas: Canvas, theme: String) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (theme.lowercase()) {
            "light" -> {
                bgPaint.shader = LinearGradient(
                    0f, 0f, 0f, CANVAS_HEIGHT.toFloat(),
                    intArrayOf(Color.parseColor("#F7F2EC"), Color.parseColor("#E5DDD5")),
                    null,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)

                drawGlowBlob(canvas, 250f, 350f, 420f, Color.parseColor("#EDE4D8"))
                drawGlowBlob(canvas, 850f, 1550f, 500f, Color.parseColor("#E0D4C5"))
                drawDotGrid(canvas, Color.parseColor("#D5CCC0"))
                drawRings(canvas, Color.parseColor("#DDD4C7"), 2f)
                drawWatermark(canvas, "VISTA", Color.parseColor("#DDD3C5"))
            }
            "vista" -> {
                bgPaint.shader = LinearGradient(
                    0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(),
                    intArrayOf(
                        Color.parseColor("#833AB4"),
                        Color.parseColor("#FD1D1D"),
                        Color.parseColor("#FCB045"),
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)

                drawGlowBlob(canvas, 540f, 960f, 600f, Color.argb(40, 255, 255, 255))
                drawDotGrid(canvas, Color.argb(35, 255, 255, 255))
                drawRings(canvas, Color.argb(45, 255, 255, 255), 2.5f)
                drawWatermark(canvas, "VISTA", Color.argb(50, 255, 255, 255))
            }
            else -> {
                // dark (default)
                bgPaint.shader = LinearGradient(
                    0f, 0f, 0f, CANVAS_HEIGHT.toFloat(),
                    intArrayOf(Color.parseColor("#08080A"), Color.parseColor("#16161A")),
                    null,
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)

                drawGlowBlob(canvas, 200f, 300f, 400f, Color.parseColor("#1F162E"))
                drawGlowBlob(canvas, 880f, 1500f, 500f, Color.parseColor("#15202E"))
                drawDotGrid(canvas, Color.parseColor("#26262E"))
                drawRings(canvas, Color.parseColor("#262633"), 2f)
                drawWatermark(canvas, "VISTA", Color.parseColor("#1E1E24"))
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
                canvas.drawCircle(x, y, 1.5f, paint)
                x += spacing
            }
            y += spacing
        }
    }

    private fun drawRings(canvas: Canvas, color: Int, strokeWidth: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }
        canvas.drawCircle(CANVAS_WIDTH * 0.12f, CANVAS_HEIGHT * 0.18f, CANVAS_WIDTH * 0.42f, paint)
        canvas.drawCircle(CANVAS_WIDTH * 0.88f, CANVAS_HEIGHT * 0.75f, CANVAS_WIDTH * 0.38f, paint)
    }

    private fun drawWatermark(canvas: Canvas, text: String, color: Int) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 125f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.18f
        }
        val textWidth = textPaint.measureText(text)
        canvas.save()
        canvas.translate(CANVAS_WIDTH * 0.5f, CANVAS_HEIGHT * 0.88f)
        canvas.rotate(-15f)
        canvas.drawText(text, -textWidth / 2f, 0f, textPaint)
        canvas.restore()
    }

    private fun drawPostCard(
        canvas: Canvas,
        post: FeedPost,
        theme: String,
        avatarBitmap: Bitmap?,
        mediaBitmap: Bitmap?,
    ) {
        val cardLeft = (CANVAS_WIDTH - CARD_WIDTH) / 2f
        val cardRight = cardLeft + CARD_WIDTH

        val padding = 36f
        val innerWidth = CARD_WIDTH - (padding * 2)

        val contentText = post.content.orEmpty().trim()
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val textLayout = if (contentText.isNotEmpty()) {
            StaticLayout.Builder.obtain(contentText, 0, contentText.length, textPaint, innerWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1.15f)
                .setMaxLines(8)
                .build()
        } else null

        val headerHeight = 110f
        val textHeight = (textLayout?.height?.toFloat() ?: 0f) + if (textLayout != null) 24f else 0f
        val hasMedia = mediaBitmap != null
        val mediaHeight = if (hasMedia) 440f else 0f
        val musicHeight = if (!post.musicTitle.isNullOrBlank() || !post.musicUrl.isNullOrBlank()) 90f else 0f
        val statsHeight = 60f
        val footerHeight = 56f

        val totalCardHeight = headerHeight + textHeight + mediaHeight + musicHeight + statsHeight + footerHeight + (padding * 2)
        val cardTop = (CANVAS_HEIGHT - totalCardHeight) / 2f - 40f
        val cardBottom = cardTop + totalCardHeight

        // Shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (theme.lowercase()) {
                "light" -> Color.argb(40, 0, 0, 0)
                "vista" -> Color.argb(35, 0, 0, 0)
                else -> Color.argb(100, 0, 0, 0)
            }
        }
        val shadowRect = RectF(cardLeft - 2f, cardTop + 12f, cardRight + 2f, cardBottom + 16f)
        canvas.drawRoundRect(shadowRect, CARD_RADIUS + 4f, CARD_RADIUS + 4f, shadowPaint)

        // Card background
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        canvas.drawRoundRect(cardRect, CARD_RADIUS, CARD_RADIUS, cardPaint)

        var currentY = cardTop + padding

        // 1. Brand Mark
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#737373")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("⚡ Vista", cardLeft + padding, currentY + 18f, brandPaint)
        currentY += 36f

        // 2. Author Row
        val avatarRadius = 32f
        val avatarCenterX = cardRight - padding - avatarRadius
        val avatarCenterY = currentY + avatarRadius

        if (avatarBitmap != null) {
            val clipPath = Path().apply {
                addCircle(avatarCenterX, avatarCenterY, avatarRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            val srcRect = Rect(0, 0, avatarBitmap.width, avatarBitmap.height)
            val dstRect = RectF(
                avatarCenterX - avatarRadius,
                avatarCenterY - avatarRadius,
                avatarCenterX + avatarRadius,
                avatarCenterY + avatarRadius,
            )
            canvas.drawBitmap(avatarBitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EFEFEF")
            }
            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, fallbackPaint)

            val initial = (post.authorFullName.firstOrNull() ?: post.authorUsername?.firstOrNull() ?: 'U').uppercaseChar().toString()
            val initialPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#666666")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(initial, avatarCenterX, avatarCenterY + 10f, initialPaint)
        }

        // Author Name
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val authorName = post.authorFullName.ifBlank { post.authorUsername.orEmpty().ifBlank { "کاربر ویستا" } }
        val nameX = avatarCenterX - avatarRadius - 16f
        canvas.drawText(authorName, nameX, avatarCenterY - 4f, namePaint)

        // Verified Badge
        if (post.authorIsVerified) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1D9BF0")
            }
            val badgeRadius = 10f
            val nameWidth = namePaint.measureText(authorName)
            val badgeX = nameX - nameWidth - 16f
            val badgeY = avatarCenterY - 14f
            canvas.drawCircle(badgeX, badgeY, badgeRadius, badgePaint)

            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(badgeX - 4f, badgeY, badgeX - 1f, badgeY + 3.5f, checkPaint)
            canvas.drawLine(badgeX - 1f, badgeY + 3.5f, badgeX + 4.5f, badgeY - 3f, checkPaint)
        }

        val usernamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#777777")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val handle = post.authorUsername?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: ""
        if (handle.isNotEmpty()) {
            canvas.drawText(handle, nameX, avatarCenterY + 24f, usernamePaint)
        }

        currentY += (avatarRadius * 2) + 24f

        // 3. Post Content Text
        if (textLayout != null) {
            canvas.save()
            canvas.translate(cardLeft + padding, currentY)
            textLayout.draw(canvas)
            canvas.restore()
            currentY += textLayout.height + 24f
        }

        // 4. Media (Image / Video)
        if (mediaBitmap != null) {
            val mediaRect = RectF(cardLeft + padding, currentY, cardRight - padding, currentY + mediaHeight - 16f)
            val clipPath = Path().apply {
                addRoundRect(mediaRect, 18f, 18f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            val srcRect = Rect(0, 0, mediaBitmap.width, mediaBitmap.height)
            canvas.drawBitmap(mediaBitmap, srcRect, mediaRect, Paint(Paint.FILTER_BITMAP_FLAG))

            if (!post.videoUrl.isNullOrBlank()) {
                val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(120, 0, 0, 0)
                }
                val playRadius = 36f
                val playCx = mediaRect.centerX()
                val playCy = mediaRect.centerY()
                canvas.drawCircle(playCx, playCy, playRadius, overlayPaint)

                val trianglePath = Path().apply {
                    moveTo(playCx - 10f, playCy - 16f)
                    lineTo(playCx + 16f, playCy)
                    lineTo(playCx - 10f, playCy + 16f)
                    close()
                }
                val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawPath(trianglePath, iconPaint)
            }
            canvas.restore()
            currentY += mediaHeight
        }

        // 5. Music Section
        if (!post.musicTitle.isNullOrBlank() || !post.musicUrl.isNullOrBlank()) {
            val musicRect = RectF(cardLeft + padding, currentY, cardRight - padding, currentY + 70f)
            val musicPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    musicRect.left, 0f, musicRect.right, 0f,
                    intArrayOf(Color.parseColor("#1DB954"), Color.parseColor("#169C46")),
                    null,
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRoundRect(musicRect, 16f, 16f, musicPaint)

            val musicTitle = post.musicTitle?.ifBlank { "قطعه صوتی" } ?: "موزیک"
            val musicTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(musicTitle, musicRect.right - 24f, musicRect.centerY() + 8f, musicTitlePaint)

            currentY += 86f
        }

        // 6. Stats & Date Row
        val statsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#737373")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val dateText = feedRelativeTime(post.createdAt)
        canvas.drawText(dateText, cardLeft + padding + statsPaint.measureText(dateText), currentY + 28f, statsPaint)

        var statsCursor = cardRight - padding
        if (!post.hideLikeCount) {
            val likeText = "❤️ ${post.likeCount}"
            canvas.drawText(likeText, statsCursor, currentY + 28f, statsPaint)
            statsCursor -= statsPaint.measureText(likeText) + 32f
        }
        if (!post.hideCommentCount) {
            val commentText = "💬 ${post.commentCount}"
            canvas.drawText(commentText, statsCursor, currentY + 28f, statsPaint)
        }

        currentY += 46f

        // 7. Bottom Brand Border & Link
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F0F0F0")
            strokeWidth = 1.5f
        }
        canvas.drawLine(cardLeft, currentY, cardRight, currentY, dividerPaint)
        currentY += 28f

        val footerBrandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#737373")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Vista", cardLeft + padding, currentY + 4f, footerBrandPaint)

        val sitePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#AAAAAA")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("cafevista.ir", cardRight - padding, currentY + 4f, sitePaint)
    }

    private suspend fun loadBitmapSafe(context: Context, url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        try {
            withTimeoutOrNull(3000L) {
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
