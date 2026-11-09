package ir.coffevista.vista_native.core.designsystem.glass

import android.app.ActivityManager
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.PowerManager
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Vista "Liquid Glass" engine inspired by Telegram Android's `blur3` (`BlurredBackgroundDrawable`,
 * `BlurredBackgroundDrawableRenderNode`, `BlurredBackgroundProvider`) and optical meniscus glass.
 *
 * Architecture:
 * 1. A *source* container ([vistaGlassSource]) records its rendered hierarchy into a hardware
 *    [GraphicsLayer] and listens to nested scroll deltas so sibling glass surfaces stay synchronized
 *    on every scroll frame with 0% idle overhead.
 * 2. A *glass* surface ([vistaGlass]) re-draws the source layer with an overscan halo, applies a
 *    hardware `RenderEffect` pipeline (Gaussian Blur + Vibrancy Saturation Boost + AGSL Meniscus
 *    Lens Refraction & OLED Anti-Banding on API 33+), and finishes with adaptive chromatic tint,
 *    inner convex sheen, and a 4-stop diagonal Fresnel specular rim border.
 * 3. A *glass halo* ([vistaGlassHalo]) creates a soft, feathered backdrop blur behind floating
 *    dock elements (such as bottom navigation and chat composer).
 * 4. Below API 31, on low-RAM devices, or in battery-saver mode, runtime shaders are bypassed in
 *    favor of a calibrated high-contrast translucent acrylic surface.
 */
@Stable
class VistaGlassState internal constructor(val layer: GraphicsLayer) {
    var sourceOrigin by mutableStateOf(Offset.Zero)
        internal set
    var invalidationTick by mutableIntStateOf(0)
        internal set
    private var lastFrameMs: Long = 0L

    /** Triggers a draw-only refresh on all subscribed [vistaGlass] surfaces (coalesced per 6ms window). */
    fun invalidate() {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastFrameMs >= 6L) {
            lastFrameMs = now
            invalidationTick++
        }
    }

    internal fun forceInvalidate() {
        lastFrameMs = android.os.SystemClock.uptimeMillis()
        invalidationTick++
    }

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (consumed.x != 0f || consumed.y != 0f) {
                invalidate()
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            forceInvalidate()
            return Velocity.Zero
        }
    }
}

@Composable
fun rememberVistaGlassState(): VistaGlassState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { VistaGlassState(layer) }
}

/**
 * Synchronizes a [VistaGlassState] with programmatic or user scrolls on [scrollableState]
 * (e.g. `LazyListState.animateScrollToItem` in Chat). Zero work is performed when idle.
 */
@Composable
fun VistaGlassScrollSync(
    state: VistaGlassState,
    scrollableState: ScrollableState,
) {
    val isScrolling = scrollableState.isScrollInProgress
    LaunchedEffect(state, isScrolling) {
        while (isScrolling) {
            withFrameNanos {
                state.invalidate()
            }
        }
        state.forceInvalidate()
    }
}

/** Lets deeply nested glass surfaces (e.g. a chat composer or voice dock) find the nearest glass source. */
val LocalVistaGlassState = staticCompositionLocalOf<VistaGlassState?> { null }

/** True when real-time hardware backdrop blur can be used on this device right now. */
@Composable
fun rememberVistaGlassBlurSupported(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !context.isPowerSaveMode() &&
            !context.isLowRamDevice()
    }
}

private fun Context.isPowerSaveMode(): Boolean =
    (getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true

private fun Context.isLowRamDevice(): Boolean =
    (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true

/** Design tokens for Telegram-grade Liquid Glass surfaces. */
object VistaGlassTokens {
    val BarBlur: Dp = 28.dp
    val PillBlur: Dp = 26.dp
    val PopupBlur: Dp = 32.dp
    const val TintAlphaBlurredLight = 0.82f
    const val TintAlphaBlurredDark = 0.80f
    const val TintAlphaFallback = 0.94f
    const val VibrancySaturationLight = 1.25f
    const val VibrancySaturationDark = 1.30f
}

/** Marks the content that glass surfaces should see through (list, wallpaper, nav host...). */
fun Modifier.vistaGlassSource(state: VistaGlassState): Modifier =
    this
        .nestedScroll(state.nestedScrollConnection)
        .onGloballyPositioned {
            val pos = it.positionInWindow()
            if (pos != state.sourceOrigin) {
                state.sourceOrigin = pos
            }
            state.invalidate()
        }
        .drawWithContent {
            val layer = state.layer
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
        }

/**
 * Paints a Telegram-grade Liquid Glass surface behind this element's content.
 *
 * @param tint base colour of the glass (theme surface tone).
 * @param isDark selects highlight/rim/vibrancy strengths.
 * @param blurRadius optical Gaussian blur radius.
 * @param tintAlphaOverride optional custom tint opacity when blur is active.
 * @param drawHighlight paint the soft inner convex specular sheen.
 * @param rimWidth width of the hairline light-refracting rim; use `0.dp` to disable.
 * @param enableRefraction enables AGSL meniscus lens refraction on rounded shapes (API 33+).
 */
@Composable
fun Modifier.vistaGlass(
    state: VistaGlassState,
    shape: Shape,
    tint: Color,
    isDark: Boolean,
    blurRadius: Dp = VistaGlassTokens.BarBlur,
    tintAlphaOverride: Float? = null,
    drawHighlight: Boolean = false,
    rimWidth: Dp = 0.6.dp,
    enableRefraction: Boolean = shape !== RectangleShape,
    shadowElevation: Dp = 0.dp,
): Modifier {
    val blurSupported = rememberVistaGlassBlurSupported()
    val blurLayer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val blurPx = with(density) { blurRadius.toPx() }
    val rimPx = with(density) { rimWidth.toPx() }
    val shadowPx = with(density) { shadowElevation.toPx() }
    val shadowPaint = remember(shadowPx, isDark) {
        if (shadowPx > 0.5f) {
            AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                style = AndroidPaint.Style.FILL
                color = Color.Black.copy(alpha = if (isDark) 0.34f else 0.16f).toArgb()
                maskFilter = BlurMaskFilter(shadowPx * 1.15f, BlurMaskFilter.Blur.NORMAL)
            }
        } else {
            null
        }
    }
    val defaultBlurredAlpha = if (isDark) {
        VistaGlassTokens.TintAlphaBlurredDark
    } else {
        VistaGlassTokens.TintAlphaBlurredLight
    }
    val tintAlpha = if (blurSupported) {
        tintAlphaOverride ?: defaultBlurredAlpha
    } else {
        VistaGlassTokens.TintAlphaFallback
    }
    val saturation = if (isDark) {
        VistaGlassTokens.VibrancySaturationDark
    } else {
        VistaGlassTokens.VibrancySaturationLight
    }
    val agslShader = remember(blurSupported, enableRefraction) {
        if (blurSupported && enableRefraction && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            createLiquidGlassAgslShaderOrNull()
        } else {
            null
        }
    }

    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = outline.toPath()
            val androidPath = if (shadowPaint != null) path.asAndroidPath() else null
            val cornerRadiusPx = outline.approxCornerRadius(size)
            val shadowDy = (shadowPx * 0.28f).coerceAtMost(8f)

            val canBlur = blurSupported && size.width >= 2f && size.height >= 2f && blurPx >= 0.5f
            val downsample = if (canBlur && blurPx >= 8f) 0.56f else 1.0f
            val invDownsample = 1f / downsample
            val scaledW = size.width * downsample
            val scaledH = size.height * downsample
            val scaledBlurPx = blurPx * downsample
            val overscan = if (canBlur) min(scaledBlurPx, 16f).roundToInt() else 0
            val recW = (scaledW.roundToInt() + overscan * 2).coerceAtLeast(1)
            val recH = (scaledH.roundToInt() + overscan * 2).coerceAtLeast(1)

            if (canBlur) {
                blurLayer.renderEffect = buildLiquidGlassRenderEffect(
                    blurPx = scaledBlurPx,
                    saturation = saturation,
                    agslShader = agslShader,
                    width = scaledW,
                    height = scaledH,
                    overscan = overscan.toFloat(),
                    cornerRadiusPx = cornerRadiusPx * downsample,
                    isDark = isDark,
                )
            }

            val tintColor = tint.copy(alpha = tintAlpha)
            val highlightBrush = if (drawHighlight) {
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (isDark) 0.04f else 0.10f),
                    0.45f to Color.White.copy(alpha = if (isDark) 0.01f else 0.03f),
                    1f to Color.Transparent,
                )
            } else {
                null
            }
            val rimBrush = if (rimPx > 0f) {
                if (isDark) {
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.12f),
                        0.5f to Color.White.copy(alpha = 0.05f),
                        1f to Color.White.copy(alpha = 0.02f),
                    )
                } else {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.07f),
                        0.5f to Color.Black.copy(alpha = 0.03f),
                        1f to Color.Black.copy(alpha = 0.06f),
                    )
                }
            } else {
                null
            }
            val rimStroke = if (rimPx > 0f) Stroke(width = rimPx) else null

            var lastRecordedTick = Int.MIN_VALUE
            var lastRecordedDelta = Offset.Unspecified

            onDrawBehind {
                val tick = state.invalidationTick

                if (shadowPaint != null && androidPath != null) {
                    clipPath(path, clipOp = ClipOp.Difference) {
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.save()
                            canvas.nativeCanvas.translate(0f, shadowDy)
                            canvas.nativeCanvas.drawPath(androidPath, shadowPaint)
                            canvas.nativeCanvas.restore()
                        }
                    }
                }

                clipPath(path) {
                    if (canBlur) {
                        val delta = origin - state.sourceOrigin
                        if (lastRecordedTick != tick || lastRecordedDelta != delta) {
                            lastRecordedTick = tick
                            lastRecordedDelta = delta
                            blurLayer.record(
                                density = this,
                                layoutDirection = layoutDirection,
                                size = IntSize(recW, recH),
                            ) {
                                translate(
                                    left = overscan.toFloat(),
                                    top = overscan.toFloat(),
                                ) {
                                    scale(downsample, downsample, Offset.Zero) {
                                        translate(
                                            left = -delta.x,
                                            top = -delta.y,
                                        ) {
                                            drawLayer(state.layer)
                                        }
                                    }
                                }
                            }
                        }
                        scale(invDownsample, invDownsample, Offset.Zero) {
                            translate(left = -overscan.toFloat(), top = -overscan.toFloat()) {
                                drawLayer(blurLayer)
                            }
                        }
                    }
                    drawRect(tintColor)
                    if (highlightBrush != null) {
                        drawRect(highlightBrush)
                    }
                }
                if (rimBrush != null && rimStroke != null) {
                    drawPath(
                        path = path,
                        brush = rimBrush,
                        style = rimStroke,
                    )
                }
            }
        }
}

/**
 * Paints a soft, optical frosted-glass blur halo behind floating dock elements
 * (such as the main bottom navigation island or chat composer enter view).
 *
 * Starts very softly from slightly lower down (not from the top point), and progressively
 * deepens and expands all the way down to the bottom of the screen.
 *
 * @param state the [VistaGlassState] recording the scrolling content underneath.
 * @param isDark selects halo tint and opacity.
 * @param tint base color of the halo.
 * @param blurRadius Gaussian blur radius for the backdrop.
 * @param startOffsetFraction vertical fraction from top where halo starts appearing (e.g. 0.16f).
 * @param maxAlpha target maximum opacity of the halo surface at the bottom.
 */
@Composable
fun Modifier.vistaGlassHalo(
    state: VistaGlassState,
    isDark: Boolean,
    tint: Color = if (isDark) Color(0xFF141C24) else Color(0xFFF7F9FA),
    blurRadius: Dp = 20.dp,
    startOffsetFraction: Float = 0.16f,
    maxAlpha: Float = if (isDark) 0.54f else 0.58f,
): Modifier {
    val blurSupported = rememberVistaGlassBlurSupported()
    val blurLayer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val blurPx = with(density) { blurRadius.toPx() }
    val saturation = if (isDark) 1.25f else 1.20f

    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawWithCache {
            val canBlur = blurSupported && size.width >= 2f && size.height >= 2f && blurPx >= 0.5f
            val downsample = if (canBlur && blurPx >= 8f) 0.50f else 1.0f
            val invDownsample = 1f / downsample
            val scaledW = size.width * downsample
            val scaledH = size.height * downsample
            val scaledBlurPx = blurPx * downsample
            val overscan = if (canBlur) min(scaledBlurPx, 16f).roundToInt() else 0
            val recW = (scaledW.roundToInt() + overscan * 2).coerceAtLeast(1)
            val recH = (scaledH.roundToInt() + overscan * 2).coerceAtLeast(1)

            if (canBlur) {
                blurLayer.renderEffect = buildLiquidGlassRenderEffect(
                    blurPx = scaledBlurPx,
                    saturation = saturation,
                    agslShader = null,
                    width = scaledW,
                    height = scaledH,
                    overscan = overscan.toFloat(),
                    cornerRadiusPx = 0f,
                    isDark = isDark,
                )
            }

            val s0 = startOffsetFraction.coerceIn(0f, 0.45f)
            val span = (1f - s0).coerceAtLeast(0.01f)

            // Ultra-soft cubic ramp starting from slightly lower down and steadily increasing to the bottom
            val alphaGradient = Brush.verticalGradient(
                0f to Color.Transparent,
                s0 to Color.Transparent,
                (s0 + span * 0.16f) to Color.Black.copy(alpha = 0.02f),
                (s0 + span * 0.32f) to Color.Black.copy(alpha = 0.08f),
                (s0 + span * 0.50f) to Color.Black.copy(alpha = 0.24f),
                (s0 + span * 0.70f) to Color.Black.copy(alpha = 0.54f),
                (s0 + span * 0.88f) to Color.Black.copy(alpha = 0.82f),
                1f to Color.Black,
            )

            val fallbackBrush = Brush.verticalGradient(
                0f to Color.Transparent,
                s0 to Color.Transparent,
                (s0 + span * 0.16f) to tint.copy(alpha = maxAlpha * 0.02f),
                (s0 + span * 0.32f) to tint.copy(alpha = maxAlpha * 0.08f),
                (s0 + span * 0.50f) to tint.copy(alpha = maxAlpha * 0.26f),
                (s0 + span * 0.70f) to tint.copy(alpha = maxAlpha * 0.56f),
                (s0 + span * 0.88f) to tint.copy(alpha = maxAlpha * 0.84f),
                1f to tint.copy(alpha = maxAlpha),
            )

            val tintColor = tint.copy(alpha = maxAlpha)
            var lastRecordedTick = Int.MIN_VALUE
            var lastRecordedDelta = Offset.Unspecified

            onDrawBehind {
                val tick = state.invalidationTick
                if (canBlur) {
                    val delta = origin - state.sourceOrigin
                    if (lastRecordedTick != tick || lastRecordedDelta != delta) {
                        lastRecordedTick = tick
                        lastRecordedDelta = delta
                        blurLayer.record(
                            density = this,
                            layoutDirection = layoutDirection,
                            size = IntSize(recW, recH),
                        ) {
                            translate(
                                left = overscan.toFloat(),
                                top = overscan.toFloat(),
                            ) {
                                scale(downsample, downsample, Offset.Zero) {
                                    translate(
                                        left = -delta.x,
                                        top = -delta.y,
                                    ) {
                                        drawLayer(state.layer)
                                    }
                                }
                            }
                        }
                    }

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.saveLayer(0f, 0f, size.width, size.height, null)
                    }
                    scale(invDownsample, invDownsample, Offset.Zero) {
                        translate(left = -overscan.toFloat(), top = -overscan.toFloat()) {
                            drawLayer(blurLayer)
                        }
                    }
                    drawRect(tintColor)
                    drawRect(brush = alphaGradient, blendMode = BlendMode.DstIn)
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.restore()
                    }
                } else {
                    drawRect(fallbackBrush)
                }
            }
        }
}

/**
 * Applies OS Window-Composer hardware blur (`FLAG_BLUR_BEHIND` + `setBackgroundBlurRadius`)
 * to a `Dialog` or `ModalBottomSheet` window on Android 12+ (API 31+).
 */
@Composable
fun VistaWindowBlurEffect(
    blurBehindRadius: Dp = 20.dp,
    backgroundBlurRadius: Dp = 28.dp,
) {
    val blurSupported = rememberVistaGlassBlurSupported()
    val view = LocalView.current
    val density = LocalDensity.current
    val behindPx = with(density) { blurBehindRadius.roundToPx() }
    val bgPx = with(density) { backgroundBlurRadius.roundToPx() }

    if (blurSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        DisposableEffect(view, behindPx, bgPx) {
            val dialogWindow = (view.parent as? DialogWindowProvider)?.window
            if (dialogWindow != null) {
                runCatching {
                    dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val attrs = dialogWindow.attributes
                    attrs.blurBehindRadius = behindPx
                    dialogWindow.attributes = attrs
                    dialogWindow.setBackgroundBlurRadius(bgPx)
                }
            }
            onDispose { }
        }
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

private fun Outline.approxCornerRadius(size: androidx.compose.ui.geometry.Size): Float = when (this) {
    is Outline.Rectangle -> 0f
    is Outline.Rounded -> min(
        roundRect.topLeftCornerRadius.x,
        min(size.width, size.height) * 0.5f,
    )
    is Outline.Generic -> min(size.width, size.height) * 0.25f
}

private fun buildLiquidGlassRenderEffect(
    blurPx: Float,
    saturation: Float,
    agslShader: RuntimeShader?,
    width: Float,
    height: Float,
    overscan: Float,
    cornerRadiusPx: Float,
    isDark: Boolean,
): RenderEffect {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val blurEffect = AndroidRenderEffect.createBlurEffect(
            blurPx,
            blurPx,
            Shader.TileMode.CLAMP,
        )
        val matrix = ColorMatrix().apply { setSaturation(saturation) }
        val vibrancyEffect = AndroidRenderEffect.createColorFilterEffect(
            ColorMatrixColorFilter(matrix),
            blurEffect,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            agslShader != null &&
            cornerRadiusPx > 1.5f &&
            width > 12f &&
            height > 12f
        ) {
            return runCatching {
                val minDim = min(width, height)
                val bevelPx = min(cornerRadiusPx * 0.45f, minDim * 0.25f).coerceIn(3f, 14f)
                val refractPx = (bevelPx * 0.22f).coerceIn(1.0f, 3.5f)
                agslShader.setFloatUniform(
                    "uRect",
                    overscan + width * 0.5f,
                    overscan + height * 0.5f,
                    width * 0.5f,
                    height * 0.5f,
                )
                agslShader.setFloatUniform(
                    "uLens",
                    cornerRadiusPx,
                    bevelPx,
                    refractPx,
                    if (isDark) 1f else 0f,
                )
                AndroidRenderEffect.createChainEffect(
                    AndroidRenderEffect.createRuntimeShaderEffect(agslShader, "content"),
                    vibrancyEffect,
                ).asComposeRenderEffect()
            }.getOrElse { vibrancyEffect.asComposeRenderEffect() }
        }
        return vibrancyEffect.asComposeRenderEffect()
    }
    return BlurEffect(blurPx, blurPx, TileMode.Clamp)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createLiquidGlassAgslShaderOrNull(): RuntimeShader? = runCatching {
    RuntimeShader(LIQUID_GLASS_AGSL)
}.getOrNull()

/**
 * AGSL Liquid Glass v3 Shader:
 * 1. Exact Rounded-Box SDF Meniscus Lens Refraction (optical curved edge bend).
 * 2. Pure Snell edge deflection without artificial white specular glow or chromatic fringe.
 * 3. Sub-bit Interleaved Gradient Noise (IGN) to eliminate 8-bit OLED colour banding.
 */
private const val LIQUID_GLASS_AGSL = """
uniform shader content;
uniform float4 uRect; // cx, cy, halfW, halfH
uniform float4 uLens; // cornerRadius, bevelWidth, refractionStrength, isDark

half4 main(float2 fragCoord) {
    float2 p = fragCoord - uRect.xy;
    float r = min(uLens.x, min(uRect.z, uRect.w));
    float2 q = abs(p) - (uRect.zw - float2(r));
    float2 maxQ = max(q, 0.0);
    float maxQLen = length(maxQ);
    float d = min(max(q.x, q.y), 0.0) + maxQLen - r;

    float edgeDist = max(-d, 0.0);
    float bevel = max(uLens.y, 1.0);
    float t = clamp(1.0 - edgeDist / bevel, 0.0, 1.0);

    half4 col;
    if (t > 0.002) {
        float curve = t * t * (3.0 - 2.0 * t);
        float2 signP = float2(p.x >= 0.0 ? 1.0 : -1.0, p.y >= 0.0 ? 1.0 : -1.0);
        float2 grad = (maxQLen > 0.001)
            ? (signP * (maxQ / maxQLen))
            : ((q.x > q.y) ? float2(signP.x, 0.0) : float2(0.0, signP.y));

        float2 offset = grad * (curve * uLens.z);
        col = content.eval(fragCoord - offset);
    } else {
        col = content.eval(fragCoord);
    }

    // Sub-bit Interleaved Gradient Noise (IGN) to eliminate 8-bit OLED banding
    float ign = fract(52.9829189 * fract(dot(fragCoord, float2(0.06711056, 0.00583715)))) - 0.5;
    col.rgb += half3(ign * (1.2 / 255.0));
    return col;
}
"""
