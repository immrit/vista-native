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
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import ir.coffevista.vista_native.core.designsystem.R as DesignR
import ir.coffevista.vista_native.core.designsystem.component.ResolvedVerificationBadgeType
import ir.coffevista.vista_native.core.designsystem.component.resolveVerificationBadgeType
import ir.coffevista.vista_native.core.designsystem.component.verificationBadgeColorArgb
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
    private const val STICKER_SHADOW_PAD = 36f
    private const val TEMP_DIR_NAME = "story_shares"
    private const val MAX_TEMP_FILE_AGE_MS = 3600_000L // 1 hour

    data class InstagramStoryAssets(
        val backgroundFile: File,
        val stickerFile: File,
    )

    suspend fun generateTemplate(
        context: Context,
        post: FeedPost,
        theme: String = "dark",
    ): File = withContext(Dispatchers.IO) {
        cleanOldTempFiles(context)

        val avatarBitmap = loadBitmapSafe(context, post.authorAvatarUrl)
        val mediaUrl = post.primaryImageUrl ?: post.videoThumbnailUrl
        val mediaBitmap = loadBitmapSafe(context, mediaUrl)
        val logoBitmap = loadLogoBitmap(context)
        val verifiedDrawable = loadVerifiedDrawable(context)

        val bitmap = renderToBitmap(
            post = post,
            theme = theme,
            avatarBitmap = avatarBitmap,
            mediaBitmap = mediaBitmap,
            logoBitmap = logoBitmap,
            verifiedDrawable = verifiedDrawable,
        )

        saveBitmapToTempFile(context, bitmap, "story_post_${post.id}_${theme}_${System.currentTimeMillis()}.png")
    }

    /**
     * Generates separate Instagram Story background (`vista` pattern, no post) and
     * transparent-backed movable Post Card sticker matching the exact in-app post layout.
     */
    suspend fun generateInstagramStoryAssets(
        context: Context,
        post: FeedPost,
    ): InstagramStoryAssets = withContext(Dispatchers.IO) {
        cleanOldTempFiles(context)

        val avatarBitmap = loadBitmapSafe(context, post.authorAvatarUrl)
        val mediaUrl = post.primaryImageUrl ?: post.videoThumbnailUrl
        val mediaBitmap = loadBitmapSafe(context, mediaUrl)
        val logoBitmap = loadLogoBitmap(context)
        val verifiedDrawable = loadVerifiedDrawable(context)

        val stamp = System.currentTimeMillis()
        val backgroundBitmap = renderVistaBackgroundBitmap()
        val backgroundFile = saveBitmapToTempFile(context, backgroundBitmap, "story_bg_${post.id}_$stamp.png")

        val stickerBitmap = renderPostStickerBitmap(
            post = post,
            avatarBitmap = avatarBitmap,
            mediaBitmap = mediaBitmap,
            logoBitmap = logoBitmap,
            verifiedDrawable = verifiedDrawable,
        )
        val stickerFile = saveBitmapToTempFile(context, stickerBitmap, "story_sticker_${post.id}_$stamp.png")

        InstagramStoryAssets(backgroundFile = backgroundFile, stickerFile = stickerFile)
    }

    /**
     * Generates separate Instagram Story background (`vista` pattern) and a transparent-backed
     * Profile Card sticker for sharing user profiles directly to Instagram Stories.
     */
    suspend fun generateProfileInstagramStoryAssets(
        context: Context,
        username: String,
        fullName: String?,
        avatarUrl: String?,
        bio: String?,
        isVerified: Boolean,
        verificationType: String?,
        role: String?,
    ): InstagramStoryAssets = withContext(Dispatchers.IO) {
        cleanOldTempFiles(context)

        val avatarBitmap = loadBitmapSafe(context, avatarUrl)
        val logoBitmap = loadLogoBitmap(context)
        val verifiedDrawable = loadVerifiedDrawable(context)
        val stamp = System.currentTimeMillis()
        val safeHandle = username.replace(Regex("[^A-Za-z0-9_.-]"), "_").ifBlank { "profile" }

        val backgroundBitmap = renderVistaBackgroundBitmap()
        val backgroundFile = saveBitmapToTempFile(context, backgroundBitmap, "story_profile_bg_${safeHandle}_$stamp.png")

        val stickerBitmap = renderProfileStickerBitmap(
            username = username,
            fullName = fullName,
            bio = bio,
            isVerified = isVerified,
            verificationType = verificationType,
            role = role,
            avatarBitmap = avatarBitmap,
            logoBitmap = logoBitmap,
            verifiedDrawable = verifiedDrawable,
        )
        val stickerFile = saveBitmapToTempFile(context, stickerBitmap, "story_profile_sticker_${safeHandle}_$stamp.png")

        InstagramStoryAssets(backgroundFile = backgroundFile, stickerFile = stickerFile)
    }

    private fun saveBitmapToTempFile(context: Context, bitmap: Bitmap, fileName: String): File {
        val dir = File(context.cacheDir, TEMP_DIR_NAME).apply { mkdirs() }
        val file = File(dir, fileName)
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            file
        } finally {
            bitmap.recycle()
        }
    }

    private fun loadLogoBitmap(context: Context): Bitmap? = runCatching {
        BitmapFactory.decodeResource(context.resources, DesignR.drawable.ic_vista_logo_black)
    }.getOrNull()

    private fun loadVerifiedDrawable(context: Context): Drawable? = runCatching {
        ContextCompat.getDrawable(context, DesignR.drawable.ic_verified_badge)?.mutate()
    }.getOrNull()

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

    internal fun renderVistaBackgroundBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawThemeBackground(canvas, "vista")
        return bitmap
    }

    internal fun renderToBitmap(
        post: FeedPost,
        theme: String,
        avatarBitmap: Bitmap? = null,
        mediaBitmap: Bitmap? = null,
        logoBitmap: Bitmap? = null,
        verifiedDrawable: Drawable? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawThemeBackground(canvas, theme)
        drawPostCard(
            canvas = canvas,
            post = post,
            theme = theme,
            avatarBitmap = avatarBitmap,
            mediaBitmap = mediaBitmap,
            logoBitmap = logoBitmap,
            verifiedDrawable = verifiedDrawable,
        )

        return bitmap
    }

    internal fun renderPostStickerBitmap(
        post: FeedPost,
        avatarBitmap: Bitmap? = null,
        mediaBitmap: Bitmap? = null,
        logoBitmap: Bitmap? = null,
        verifiedDrawable: Drawable? = null,
    ): Bitmap {
        val layout = measurePostCardLayout(post, mediaBitmap)
        val stickerWidth = (CARD_WIDTH + STICKER_SHADOW_PAD * 2f).toInt()
        val stickerHeight = (layout.totalHeight + STICKER_SHADOW_PAD * 2f).toInt()
        val bitmap = Bitmap.createBitmap(stickerWidth, stickerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawPostCardAt(
            canvas = canvas,
            cardLeft = STICKER_SHADOW_PAD,
            cardTop = STICKER_SHADOW_PAD,
            layout = layout,
            post = post,
            theme = "vista",
            avatarBitmap = avatarBitmap,
            mediaBitmap = mediaBitmap,
            logoBitmap = logoBitmap,
            verifiedDrawable = verifiedDrawable,
        )
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

    private data class MeasuredPostCardLayout(
        val textLayout: StaticLayout?,
        val mediaHeight: Float,
        val hasMusic: Boolean,
        val padding: Float,
        val innerWidth: Float,
        val headerHeight: Float,
        val textSectionHeight: Float,
        val mediaSectionHeight: Float,
        val musicSectionHeight: Float,
        val actionRowHeight: Float,
        val brandFooterHeight: Float,
        val totalHeight: Float,
    )

    private fun measurePostCardLayout(
        post: FeedPost,
        mediaBitmap: Bitmap?,
    ): MeasuredPostCardLayout {
        val padding = 36f
        val innerWidth = CARD_WIDTH - (padding * 2f)
        val rawText = post.content.orEmpty().trim()
        val limitedText = if (rawText.length > 280) "${rawText.take(280)}…" else rawText

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#141414")
            textSize = 33f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = -0.01f
        }
        val textIsRtl = isPersianText(limitedText)
        val textLayout = if (limitedText.isNotEmpty()) {
            val styledText = buildEntityHighlightedText(limitedText)
            StaticLayout.Builder.obtain(styledText, 0, styledText.length, textPaint, innerWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(if (textIsRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
                .setLineSpacing(0f, 1.42f)
                .setMaxLines(9)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        } else null

        val headerHeight = 72f // 36px avatar radius * 2 matching in-app PostAvatar + single-line header
        val textSectionHeight = if (textLayout != null) textLayout.height.toFloat() + 24f else 0f

        val mediaHeight = if (mediaBitmap != null) {
            val naturalRatio = (mediaBitmap.width.toFloat() / mediaBitmap.height.coerceAtLeast(1).toFloat())
                .coerceIn(0.8f, 1.91f)
            (innerWidth / naturalRatio).coerceIn(240f, 640f)
        } else 0f
        val mediaSectionHeight = if (mediaHeight > 0f) mediaHeight + 24f else 0f

        val hasMusic = !post.musicTitle.isNullOrBlank() || !post.musicUrl.isNullOrBlank()
        val musicSectionHeight = if (hasMusic) 80f + 20f else 0f
        val actionRowHeight = 24f + 36f + 18f
        val brandFooterHeight = 56f

        val totalHeight = 28f + headerHeight + textSectionHeight +
            mediaSectionHeight + musicSectionHeight + actionRowHeight + brandFooterHeight

        return MeasuredPostCardLayout(
            textLayout = textLayout,
            mediaHeight = mediaHeight,
            hasMusic = hasMusic,
            padding = padding,
            innerWidth = innerWidth,
            headerHeight = headerHeight,
            textSectionHeight = textSectionHeight,
            mediaSectionHeight = mediaSectionHeight,
            musicSectionHeight = musicSectionHeight,
            actionRowHeight = actionRowHeight,
            brandFooterHeight = brandFooterHeight,
            totalHeight = totalHeight,
        )
    }

    private fun drawPostCard(
        canvas: Canvas,
        post: FeedPost,
        theme: String,
        avatarBitmap: Bitmap?,
        mediaBitmap: Bitmap?,
        logoBitmap: Bitmap?,
        verifiedDrawable: Drawable? = null,
    ) {
        val layout = measurePostCardLayout(post, mediaBitmap)
        val cardLeft = (CANVAS_WIDTH - CARD_WIDTH) / 2f
        val targetCenterY = (CANVAS_HEIGHT * 0.5f) - (CANVAS_HEIGHT * 0.04f)
        val cardTop = (targetCenterY - (layout.totalHeight / 2f)).coerceAtLeast(60f)
        drawPostCardAt(
            canvas = canvas,
            cardLeft = cardLeft,
            cardTop = cardTop,
            layout = layout,
            post = post,
            theme = theme,
            avatarBitmap = avatarBitmap,
            mediaBitmap = mediaBitmap,
            logoBitmap = logoBitmap,
            verifiedDrawable = verifiedDrawable,
        )
    }

    private fun drawPostCardAt(
        canvas: Canvas,
        cardLeft: Float,
        cardTop: Float,
        layout: MeasuredPostCardLayout,
        post: FeedPost,
        theme: String,
        avatarBitmap: Bitmap?,
        mediaBitmap: Bitmap?,
        logoBitmap: Bitmap?,
        verifiedDrawable: Drawable?,
    ) {
        val cardRight = cardLeft + CARD_WIDTH
        val padding = layout.padding
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardTop + layout.totalHeight)

        // Drop shadow + crisp card container (subtle border helps white cards on white/vista background)
        drawCardDropShadow(canvas, cardRect, theme)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(cardRect, CARD_RADIUS, CARD_RADIUS, cardPaint)
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(cardRect, CARD_RADIUS, CARD_RADIUS, cardBorderPaint)

        var currentY = cardTop + 28f

        // 1. In-App Author Header Row (RTL: Avatar -> Username -> Verification Badge -> "•" -> Relative Time)
        val avatarRadius = 36f
        val avatarCenterX = cardRight - padding - avatarRadius
        val avatarCenterY = currentY + avatarRadius
        drawAvatar(canvas, avatarBitmap, avatarCenterX, avatarCenterY, avatarRadius, post)

        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#141414")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#737373")
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        val primaryNameRaw = post.authorUsername?.takeIf { it.isNotBlank() }
            ?: post.authorFullName.ifBlank { "کاربر ویستا" }
        val timeLabel = feedRelativeTime(post.createdAt)
        val bulletText = "•"
        val badgeType = resolveVerificationBadgeType(post.authorIsVerified, post.authorVerificationType)
        val hasBadge = badgeType != ResolvedVerificationBadgeType.NONE
        val badgeSize = 26f
        val badgeSpace = if (hasBadge) badgeSize + 10f else 0f
        val timeWidth = if (timeLabel.isNotBlank()) {
            metaPaint.measureText(bulletText) + metaPaint.measureText(timeLabel) + 20f
        } else 0f

        val rowRight = avatarCenterX - avatarRadius - 20f
        val rowLeftLimit = cardLeft + padding
        val maxNameWidth = (rowRight - rowLeftLimit - badgeSpace - timeWidth).coerceAtLeast(140f)
        val primaryName = TextUtils.ellipsize(primaryNameRaw, namePaint, maxNameWidth, TextUtils.TruncateAt.END).toString()
        val measuredNameWidth = namePaint.measureText(primaryName)
        val textBaselineY = avatarCenterY + 10f

        var cursorX = rowRight
        canvas.drawText(primaryName, cursorX, textBaselineY, namePaint)
        cursorX -= measuredNameWidth

        if (hasBadge) {
            cursorX -= 10f
            val badgeCenterX = cursorX - (badgeSize / 2f)
            val badgeColorArgb = verificationBadgeColorArgb(badgeType, isDarkSurface = false)
            drawVerificationBadgeSeal(
                canvas = canvas,
                cx = badgeCenterX,
                cy = avatarCenterY,
                size = badgeSize,
                badgeColorArgb = badgeColorArgb,
                verifiedDrawable = verifiedDrawable,
            )
            cursorX -= badgeSize
        }

        if (timeLabel.isNotBlank() && cursorX - rowLeftLimit > 60f) {
            cursorX -= 12f
            canvas.drawText(bulletText, cursorX, textBaselineY - 1f, metaPaint)
            cursorX -= metaPaint.measureText(bulletText) + 12f
            val remainingForTime = (cursorX - rowLeftLimit).coerceAtLeast(0f)
            if (remainingForTime > 40f) {
                val ellipsizedTime = TextUtils.ellipsize(timeLabel, metaPaint, remainingForTime, TextUtils.TruncateAt.END).toString()
                canvas.drawText(ellipsizedTime, cursorX, textBaselineY - 1f, metaPaint)
            }
        }

        currentY += layout.headerHeight

        // 2. Post Caption Text (directly below header, matching in-app VistaFeedPostCard)
        layout.textLayout?.let { textLayout ->
            currentY += 24f
            canvas.save()
            canvas.translate(cardLeft + padding, currentY)
            textLayout.draw(canvas)
            canvas.restore()
            currentY += textLayout.height.toFloat()
        }

        // 3. Post Media
        if (mediaBitmap != null && layout.mediaHeight > 0f) {
            currentY += 24f
            val mediaRect = RectF(cardLeft + padding, currentY, cardRight - padding, currentY + layout.mediaHeight)
            val clipPath = Path().apply {
                addRoundRect(mediaRect, 18f, 18f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            val srcCrop = calculateCenterCropSrcRect(mediaBitmap.width, mediaBitmap.height, mediaRect.width(), mediaRect.height())
            canvas.drawBitmap(mediaBitmap, srcCrop, mediaRect, Paint(Paint.FILTER_BITMAP_FLAG))

            if (!post.videoUrl.isNullOrBlank()) {
                val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(45, 0, 0, 0) }
                canvas.drawRect(mediaRect, overlayPaint)
                drawPlayButton(canvas, mediaRect.centerX(), mediaRect.centerY(), 96f)
            } else if (post.imageUrls.size > 1) {
                drawMultiImageBadge(canvas, mediaRect, post.imageUrls.size)
            }
            canvas.restore()
            currentY += layout.mediaHeight
        }

        // 4. Music Track Bubble (matching PostMusicBubble position below media)
        if (layout.hasMusic) {
            currentY += 20f
            val musicRect = RectF(cardLeft + padding, currentY, cardRight - padding, currentY + 80f)
            drawMusicBanner(canvas, musicRect, post, isRtl = isPersianText(post.musicTitle) || isPersianText(primaryNameRaw))
            currentY += 80f
        }

        // 5. In-App Action Row (RTL: Heart + Count, Comment + Count, Bookmark, Send, ... at far left)
        currentY += 24f
        val actionCenterY = currentY + 14f
        val textBaselineActionY = actionCenterY + 9f
        val actionTint = Color.parseColor("#737373")
        val heartTint = if (post.isLiked) Color.parseColor("#E0245E") else Color.parseColor("#222222")
        val statPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = actionTint
            textSize = 25f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        var actionX = cardRight - padding
        // Heart
        drawHeart(canvas, actionX - 15f, actionCenterY, 32f, heartTint, post.isLiked)
        actionX -= 38f
        if (!post.hideLikeCount) {
            val countText = formatNumber(post.likeCount)
            statPaint.color = actionTint
            canvas.drawText(countText, actionX, textBaselineActionY, statPaint)
            actionX -= statPaint.measureText(countText) + 28f
        } else {
            actionX -= 16f
        }

        // Comment
        drawCommentBubble(canvas, actionX - 15f, actionCenterY, 30f, actionTint)
        actionX -= 38f
        if (!post.hideCommentCount) {
            val countText = formatNumber(post.commentCount)
            statPaint.color = actionTint
            canvas.drawText(countText, actionX, textBaselineActionY, statPaint)
            actionX -= statPaint.measureText(countText) + 28f
        } else {
            actionX -= 16f
        }

        // Bookmark
        val bookmarkColor = if (post.isSaved) Color.parseColor("#141414") else actionTint
        drawBookmarkIcon(canvas, actionX - 14f, actionCenterY, 28f, bookmarkColor, post.isSaved)
        actionX -= 48f

        // Send / Share
        drawSendIcon(canvas, actionX - 14f, actionCenterY, 28f, actionTint)

        // Subtle more dots at far left (matching PostActionRow)
        drawMoreDots(canvas, cardLeft + padding + 18f, actionCenterY, Color.parseColor("#999999"))

        currentY += 36f + 18f

        // 6. Dedicated Brand Footer (Vista logo & cafevista.ir in place)
        drawCardBrandingFooter(canvas, cardLeft, cardRight, padding, currentY, logoBitmap)
    }

    internal fun renderProfileStickerBitmap(
        username: String,
        fullName: String?,
        bio: String?,
        isVerified: Boolean,
        verificationType: String?,
        role: String?,
        avatarBitmap: Bitmap? = null,
        logoBitmap: Bitmap? = null,
        verifiedDrawable: Drawable? = null,
    ): Bitmap {
        val padding = 36f
        val innerWidth = CARD_WIDTH - (padding * 2f)
        val cleanBio = bio.orEmpty().trim().take(160)
        val bioPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val bioLayout = if (cleanBio.isNotEmpty()) {
            StaticLayout.Builder.obtain(cleanBio, 0, cleanBio.length, bioPaint, innerWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(if (isPersianText(cleanBio)) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
                .setLineSpacing(0f, 1.35f)
                .setMaxLines(3)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        } else null

        val headerHeight = 96f
        val bioSectionHeight = if (bioLayout != null) bioLayout.height.toFloat() + 24f else 0f
        val footerHeight = 56f
        val cardHeight = 32f + headerHeight + bioSectionHeight + 24f + footerHeight

        val width = (CARD_WIDTH + STICKER_SHADOW_PAD * 2f).toInt()
        val height = (cardHeight + STICKER_SHADOW_PAD * 2f).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cardLeft = STICKER_SHADOW_PAD
        val cardTop = STICKER_SHADOW_PAD
        val cardRight = cardLeft + CARD_WIDTH
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardTop + cardHeight)

        drawCardDropShadow(canvas, cardRect, "vista")
        canvas.drawRoundRect(cardRect, CARD_RADIUS, CARD_RADIUS, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        canvas.drawRoundRect(
            cardRect,
            CARD_RADIUS,
            CARD_RADIUS,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E5E7EB")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            },
        )

        var currentY = cardTop + 32f
        val avatarRadius = 48f
        val avatarCx = cardRight - padding - avatarRadius
        val avatarCy = currentY + avatarRadius
        drawAvatar(
            canvas = canvas,
            avatarBitmap = avatarBitmap,
            cx = avatarCx,
            cy = avatarCy,
            radius = avatarRadius,
            fallbackInitialSource = fullName?.ifBlank { null } ?: username,
        )

        val displayTitle = fullName?.takeIf { it.isNotBlank() } ?: username.ifBlank { "کاربر ویستا" }
        val handleText = username.takeIf { it.isNotBlank() }?.let { "@$it" }.orEmpty()

        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#141414")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val handlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#737373")
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        val badgeType = resolveVerificationBadgeType(isVerified, verificationType, role)
        val hasBadge = badgeType != ResolvedVerificationBadgeType.NONE
        val badgeSize = 26f
        val rowRight = avatarCx - avatarRadius - 22f
        val maxNameW = (rowRight - (cardLeft + padding) - if (hasBadge) badgeSize + 12f else 0f).coerceAtLeast(120f)
        val ellipsizedTitle = TextUtils.ellipsize(displayTitle, namePaint, maxNameW, TextUtils.TruncateAt.END).toString()

        canvas.drawText(ellipsizedTitle, rowRight, avatarCy - 6f, namePaint)
        if (hasBadge) {
            val badgeCx = rowRight - namePaint.measureText(ellipsizedTitle) - 10f - (badgeSize / 2f)
            drawVerificationBadgeSeal(
                canvas = canvas,
                cx = badgeCx,
                cy = avatarCy - 16f,
                size = badgeSize,
                badgeColorArgb = verificationBadgeColorArgb(badgeType, isDarkSurface = false),
                verifiedDrawable = verifiedDrawable,
            )
        }
        if (handleText.isNotEmpty()) {
            canvas.drawText(handleText, rowRight, avatarCy + 28f, handlePaint)
        }

        currentY += headerHeight
        bioLayout?.let { layout ->
            currentY += 24f
            canvas.save()
            canvas.translate(cardLeft + padding, currentY)
            layout.draw(canvas)
            canvas.restore()
            currentY += layout.height.toFloat()
        }

        currentY += 24f
        drawCardBrandingFooter(canvas, cardLeft, cardRight, padding, currentY, logoBitmap)
        return bitmap
    }

    private fun drawCardBrandingFooter(
        canvas: Canvas,
        cardLeft: Float,
        cardRight: Float,
        padding: Float,
        currentY: Float,
        logoBitmap: Bitmap?,
    ) {
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F0F0F0")
            strokeWidth = 1.5f
        }
        canvas.drawLine(cardLeft, currentY, cardRight, currentY, dividerPaint)

        val footerLogoSize = 24f
        val footerY = currentY + 16f
        val footerBrandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#737373")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = -0.02f
        }
        val sitePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9CA3AF")
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

        canvas.drawText("Vista", cardLeft + padding + footerLogoSize + 10f, footerY + 19f, footerBrandPaint)
        canvas.drawText("cafevista.ir", cardRight - padding, footerY + 19f, sitePaint)
    }

    private fun buildEntityHighlightedText(text: String): SpannableString {
        val spannable = SpannableString(text)
        val pattern = Regex("""(?<![^\s\n])@([\u0600-\u06FF\w_]+)|#([\u0600-\u06FF\w_]+)""")
        val entityColor = Color.parseColor("#1E88E5")
        pattern.findAll(text).forEach { match ->
            spannable.setSpan(ForegroundColorSpan(entityColor), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    private fun drawMultiImageBadge(canvas: Canvas, mediaRect: RectF, total: Int) {
        val label = "1/$total"
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val textW = badgeTextPaint.measureText(label)
        val pillW = textW + 24f
        val pillH = 38f
        val left = mediaRect.left + 16f
        val top = mediaRect.top + 16f
        val pillRect = RectF(left, top, left + pillW, top + pillH)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(160, 0, 0, 0) }
        canvas.drawRoundRect(pillRect, 14f, 14f, pillPaint)
        canvas.drawText(label, left + 12f, top + 26f, badgeTextPaint)
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
        drawAvatar(
            canvas = canvas,
            avatarBitmap = avatarBitmap,
            cx = cx,
            cy = cy,
            radius = radius,
            fallbackInitialSource = post.authorFullName.ifBlank { post.authorUsername.orEmpty() },
        )
    }

    private fun drawAvatar(
        canvas: Canvas,
        avatarBitmap: Bitmap?,
        cx: Float,
        cy: Float,
        radius: Float,
        fallbackInitialSource: String,
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

            val initial = (fallbackInitialSource.firstOrNull() ?: 'U')
                .uppercaseChar().toString()
            val initialPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#666666")
                textSize = radius * 0.8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(initial, cx, cy + (radius * 0.28f), initialPaint)
        }

        // Circular border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8E8E8")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(cx, cy, radius, borderPaint)
    }

    private fun drawVerificationBadgeSeal(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        badgeColorArgb: Int,
        verifiedDrawable: Drawable?,
    ) {
        val half = size / 2f
        if (verifiedDrawable != null) {
            val tinted = DrawableCompat.wrap(verifiedDrawable.mutate())
            DrawableCompat.setTint(tinted, badgeColorArgb)
            tinted.setBounds(
                (cx - half).toInt(),
                (cy - half).toInt(),
                (cx + half).toInt(),
                (cy + half).toInt(),
            )
            tinted.draw(canvas)
            return
        }

        // Fallback scalloped seal path matching ic_verified_badge for off-device/unit rendering.
        val path = Path()
        val lobes = 10
        val outerR = half
        val innerR = half * 0.80f
        val totalSteps = lobes * 2
        for (i in 0 until totalSteps) {
            val angle = (i.toDouble() / totalSteps) * Math.PI * 2.0
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeColorArgb
            style = Paint.Style.FILL
        })

        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (badgeColorArgb == Color.WHITE) Color.parseColor("#141414") else Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = half * 0.22f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val checkPath = Path().apply {
            moveTo(cx - half * 0.38f, cy)
            lineTo(cx - half * 0.08f, cy + half * 0.28f)
            lineTo(cx + half * 0.40f, cy - half * 0.28f)
        }
        canvas.drawPath(checkPath, checkPaint)
    }

    private fun drawBookmarkIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
        filled: Boolean,
    ) {
        val s = size / 24f
        val path = Path().apply {
            moveTo(cx - 6.5f * s, cy - 8.5f * s)
            lineTo(cx + 6.5f * s, cy - 8.5f * s)
            lineTo(cx + 6.5f * s, cy + 9.0f * s)
            lineTo(cx, cy + 4.2f * s)
            lineTo(cx - 6.5f * s, cy + 9.0f * s)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            if (filled) {
                style = Paint.Style.FILL
            } else {
                style = Paint.Style.STROKE
                strokeWidth = 2.2f * s
                strokeJoin = Paint.Join.ROUND
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawSendIcon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
    ) {
        val s = size / 24f
        val path = Path().apply {
            moveTo(cx + 8.5f * s, cy - 8.5f * s)
            lineTo(cx - 9.0f * s, cy - 1.5f * s)
            lineTo(cx - 1.2f * s, cy + 1.5f * s)
            lineTo(cx + 1.5f * s, cy + 9.0f * s)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2.1f * s
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(path, paint)
        canvas.drawLine(cx + 8.5f * s, cy - 8.5f * s, cx - 1.2f * s, cy + 1.5f * s, paint)
    }

    private fun drawMoreDots(canvas: Canvas, cx: Float, cy: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - 9f, cy, 2.5f, paint)
        canvas.drawCircle(cx, cy, 2.5f, paint)
        canvas.drawCircle(cx + 9f, cy, 2.5f, paint)
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
