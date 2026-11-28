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
import androidx.compose.ui.graphics.PathOperation
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Vista Liquid Glass optical rendering engine.
 *
 * Architecture:
 * 1. A source container ([vistaGlassSource]) records its rendered hierarchy into a hardware
 *    [GraphicsLayer] and listens to nested scroll deltas so sibling glass surfaces stay synchronized
 *    on every scroll frame with 0% idle overhead.
 * 2. A glass surface ([vistaGlass]) re-draws the downsampled source layer with Skia sigma-scaled
 *    Gaussian blur, high-vibrancy saturation boost, and an AGSL 3D meniscus Snell refraction lens
 *    shader (API 33+) with premultiplied optical tinting, shifted inner specular crescents, and
 *    an outer hairline ring + soft ambient shadow.
 * 3. A progressive glass halo ([vistaGlassHalo]) creates a feathered backdrop blur fade behind
 *    floating header capsules, bottom navigation islands, and composer bars.
 * 4. Below API 31, on low-RAM devices, or in battery-saver mode, runtime shaders are bypassed in
 *    favor of a calibrated translucent surface with specular crescent rims.
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
 * Synchronizes a [VistaGlassState] with programmatic or user scrolls on [scrollableState].
 * Zero work is performed when idle.
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

/** Lets deeply nested glass surfaces find the nearest glass source. */
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

/** Optical tokens for crystal-clear Liquid Glass surfaces. */
object VistaGlassTokens {
    val BarBlur: Dp = 9.dp
    val PillBlur: Dp = 7.dp
    val PopupBlur: Dp = 18.dp
    val LensThickness: Dp = 11.dp
    const val TintAlphaBlurredLight = 0.62f
    const val TintAlphaBlurredDark = 0.60f
    const val TintAlphaFallback = 0.92f
    const val VibrancySaturationLight = 2.55f
    const val VibrancySaturationDark = 2.30f
    const val RefractionIntensity = 0.75f
    const val RefractiveIndex = 1.5f
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
 * Paints a crystal-clear Liquid Glass surface behind this element's content.
 *
 * @param tint base colour of the glass (theme surface tone).
 * @param isDark selects highlight/rim/vibrancy strengths.
 * @param blurRadius optical Gaussian blur radius (kept subtle in Liquid Glass mode so shapes refract clearly).
 * @param tintAlphaOverride optional custom tint opacity when blur is active.
 * @param drawHighlight paint the shifted inner specular crescents.
 * @param rimWidth width of the light-refracting rim; use `0.dp` to disable.
 * @param enableRefraction enables AGSL 3D meniscus lens refraction on rounded shapes (API 33+).
 */
@Composable
fun Modifier.vistaGlass(
    state: VistaGlassState,
    shape: Shape,
    tint: Color,
    isDark: Boolean,
    blurRadius: Dp = VistaGlassTokens.BarBlur,
    tintAlphaOverride: Float? = null,
    drawHighlight: Boolean = true,
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
    val lensThicknessPx = with(density) { VistaGlassTokens.LensThickness.toPx() }
    val topCrescentPx = with(density) { 0.95.dp.toPx() }
    val bottomCrescentPx = with(density) { 0.65.dp.toPx() }
    val outerHairlinePx = with(density) { 0.45.dp.toPx() }
    val shadowPx = with(density) { shadowElevation.toPx() }
    val shadowPaint = remember(shadowPx, isDark) {
        if (shadowPx > 0.4f) {
            AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                style = AndroidPaint.Style.FILL
                color = Color.Black.copy(alpha = if (isDark) 0.28f else 0.125f).toArgb()
                maskFilter = BlurMaskFilter(
                    (shadowPx * 0.85f).coerceIn(2f, 16f),
                    BlurMaskFilter.Blur.NORMAL,
                )
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
    // Clamp legacy high-opacity caller overrides (0.80f..0.88f) down into the liquid-glass transmittance window
    val rawBlurredAlpha = tintAlphaOverride ?: defaultBlurredAlpha
    val tintAlpha = if (blurSupported) {
        if (rawBlurredAlpha in 0.78f..0.90f) {
            if (isDark) 0.62f else 0.64f
        } else {
            rawBlurredAlpha
        }
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
            val cornerRadii = outline.extractCornerRadii(size)
            val shadowDy = (shadowPx * 0.26f).coerceIn(1f, 5f)

            val canBlur = blurSupported && size.width >= 2f && size.height >= 2f && blurPx >= 0.5f
            val downsampleFactor = if (canBlur) 3f else 1f
            val downsample = 1f / downsampleFactor
            val invDownsample = downsampleFactor
            val scaledW = size.width * downsample
            val scaledH = size.height * downsample
            val scaledBlurPx = if (canBlur) downscaleRadius(blurPx, downsampleFactor) else 0f
            val overscan = if (canBlur) max(scaledBlurPx.roundToInt() + 2, 4).coerceAtMost(14) else 0
            val recW = (scaledW.roundToInt() + overscan * 2).coerceAtLeast(1)
            val recH = (scaledH.roundToInt() + overscan * 2).coerceAtLeast(1)

            val tintColor = tint.copy(alpha = tintAlpha)
            val shaderHandlesTint = canBlur &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                agslShader != null &&
                cornerRadii.maxRadius > 1.5f &&
                scaledW > 6f &&
                scaledH > 6f

            if (canBlur) {
                blurLayer.renderEffect = buildLiquidGlassRenderEffect(
                    blurPx = scaledBlurPx,
                    saturation = saturation,
                    agslShader = if (shaderHandlesTint) agslShader else null,
                    width = scaledW,
                    height = scaledH,
                    overscan = overscan.toFloat(),
                    radii = cornerRadii.scale(downsample),
                    lensThicknessPx = lensThicknessPx * downsample,
                    tintColor = tintColor,
                )
            }

            // Shifted inner specular crescents (top-lit & bottom-refraction crescents)
            val hasCrescents = (drawHighlight || rimPx > 0f) &&
                shape !== RectangleShape &&
                size.width > 8f &&
                size.height > 8f
            val topCrescentPath = if (hasCrescents) {
                val shifted = Path().apply { addPath(path, Offset(0f, topCrescentPx)) }
                Path().apply { op(path, shifted, PathOperation.Difference) }
            } else {
                null
            }
            val bottomCrescentPath = if (hasCrescents) {
                val shifted = Path().apply { addPath(path, Offset(0f, -bottomCrescentPx)) }
                Path().apply { op(path, shifted, PathOperation.Difference) }
            } else {
                null
            }
            val topCrescentColor = if (isDark) {
                Color.White.copy(alpha = 0.22f)
            } else {
                Color.White.copy(alpha = 0.94f)
            }
            val bottomCrescentColor = if (isDark) {
                Color.White.copy(alpha = 0.11f)
            } else {
                Color.White.copy(alpha = 0.68f)
            }

            // Outer hairline ring (drawn just outside the shape clip so glass pills stand out on white backgrounds)
            val outerRingBrush = if (rimPx > 0f) {
                if (isDark) {
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.10f),
                        0.5f to Color.White.copy(alpha = 0.05f),
                        1f to Color.Black.copy(alpha = 0.28f),
                    )
                } else {
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.08f),
                        0.5f to Color.Black.copy(alpha = 0.11f),
                        1f to Color.Black.copy(alpha = 0.16f),
                    )
                }
            } else {
                null
            }
            val outerRingStroke = if (rimPx > 0f) Stroke(width = outerHairlinePx * 2f) else null

            var lastRecordedTick = Int.MIN_VALUE
            var lastRecordedDelta = Offset.Unspecified

            onDrawBehind {
                val tick = state.invalidationTick

                if ((shadowPaint != null && androidPath != null) || (outerRingBrush != null && outerRingStroke != null)) {
                    clipPath(path, clipOp = ClipOp.Difference) {
                        if (shadowPaint != null && androidPath != null) {
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.save()
                                canvas.nativeCanvas.translate(0f, shadowDy)
                                canvas.nativeCanvas.drawPath(androidPath, shadowPaint)
                                canvas.nativeCanvas.restore()
                            }
                        }
                        if (outerRingBrush != null && outerRingStroke != null) {
                            drawPath(
                                path = path,
                                brush = outerRingBrush,
                                style = outerRingStroke,
                            )
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
                    if (!shaderHandlesTint) {
                        drawRect(tintColor)
                    }
                    if (topCrescentPath != null) {
                        drawPath(topCrescentPath, topCrescentColor)
                    }
                    if (bottomCrescentPath != null) {
                        drawPath(bottomCrescentPath, bottomCrescentColor)
                    }
                }
            }
        }
}

/**
 * Paints a soft, progressive optical glass blur halo behind floating dock or header elements.
 *
 * @param state the [VistaGlassState] recording the scrolling content underneath.
 * @param isDark selects halo tint and opacity.
 * @param tint base color of the halo.
 * @param blurRadius Gaussian blur radius for the backdrop.
 * @param startOffsetFraction fraction along the fade axis before the halo begins ramping in.
 * @param maxAlpha target maximum opacity of the halo surface at the anchored edge.
 * @param fromTop when true, fades from top (opaque/blurred at top edge) down to transparent at bottom.
 */
@Composable
fun Modifier.vistaGlassHalo(
    state: VistaGlassState,
    isDark: Boolean,
    tint: Color = if (isDark) Color(0xFF0E1115) else Color(0xFFF7F8FA),
    blurRadius: Dp = 16.dp,
    startOffsetFraction: Float = 0.16f,
    maxAlpha: Float = if (isDark) 0.44f else 0.46f,
    fromTop: Boolean = false,
): Modifier {
    val blurSupported = rememberVistaGlassBlurSupported()
    val blurLayer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val blurPx = with(density) { blurRadius.toPx() }
    val saturation = if (isDark) 1.65f else 1.75f

    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawWithCache {
            val canBlur = blurSupported && size.width >= 2f && size.height >= 2f && blurPx >= 0.5f
            val downsampleFactor = if (canBlur) 4f else 1f
            val downsample = 1f / downsampleFactor
            val invDownsample = downsampleFactor
            val scaledW = size.width * downsample
            val scaledH = size.height * downsample
            val scaledBlurPx = if (canBlur) downscaleRadius(blurPx, downsampleFactor) else 0f
            val overscan = if (canBlur) min(scaledBlurPx.roundToInt() + 2, 12) else 0
            val recW = (scaledW.roundToInt() + overscan * 2).coerceAtLeast(1)
            val recH = (scaledH.roundToInt() + overscan * 2).coerceAtLeast(1)

            val tintColor = tint.copy(alpha = maxAlpha)

            if (canBlur) {
                blurLayer.renderEffect = buildLiquidGlassRenderEffect(
                    blurPx = scaledBlurPx,
                    saturation = saturation,
                    agslShader = null,
                    width = scaledW,
                    height = scaledH,
                    overscan = overscan.toFloat(),
                    radii = GlassCornerRadii.Zero,
                    lensThicknessPx = 0f,
                    tintColor = tintColor,
                )
            }

            val s0 = startOffsetFraction.coerceIn(0f, 0.45f)
            val span = (1f - s0).coerceAtLeast(0.01f)
            // Keep blur mask near 1.0 across status bar + floating pills so sharp content never bleeds through
            // behind header controls, while topTintBrush feathers the frosted haze down into the skirt.
            val fallbackMaxAlpha = if (fromTop) maxAlpha.coerceIn(0f, 0.98f) else (1f - (1f - maxAlpha) * (1f - maxAlpha)).coerceIn(0f, 0.96f)

            val alphaGradient = if (fromTop) {
                val e0 = (1f - s0).coerceIn(0.55f, 1f)
                Brush.verticalGradient(
                    0f to Color.Black,
                    (e0 * 0.56f) to Color.Black,
                    (e0 * 0.68f) to Color.Black.copy(alpha = 0.88f),
                    (e0 * 0.78f) to Color.Black.copy(alpha = 0.56f),
                    (e0 * 0.88f) to Color.Black.copy(alpha = 0.24f),
                    (e0 * 0.96f) to Color.Black.copy(alpha = 0.05f),
                    e0 to Color.Transparent,
                    1f to Color.Transparent,
                )
            } else {
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    s0 to Color.Transparent,
                    (s0 + span * 0.16f) to Color.Black.copy(alpha = 0.02f),
                    (s0 + span * 0.32f) to Color.Black.copy(alpha = 0.08f),
                    (s0 + span * 0.50f) to Color.Black.copy(alpha = 0.24f),
                    (s0 + span * 0.70f) to Color.Black.copy(alpha = 0.54f),
                    (s0 + span * 0.88f) to Color.Black.copy(alpha = 0.82f),
                    1f to Color.Black,
                )
            }

            val topTintBrush = if (fromTop) {
                val e0 = (1f - s0).coerceIn(0.55f, 1f)
                Brush.verticalGradient(
                    0f to tint.copy(alpha = fallbackMaxAlpha),
                    (e0 * 0.24f) to tint.copy(alpha = fallbackMaxAlpha * 0.88f),
                    (e0 * 0.46f) to tint.copy(alpha = fallbackMaxAlpha * 0.68f),
                    (e0 * 0.64f) to tint.copy(alpha = fallbackMaxAlpha * 0.44f),
                    (e0 * 0.80f) to tint.copy(alpha = fallbackMaxAlpha * 0.18f),
                    (e0 * 0.93f) to tint.copy(alpha = fallbackMaxAlpha * 0.04f),
                    e0 to Color.Transparent,
                    1f to Color.Transparent,
                )
            } else {
                null
            }

            val fallbackBrush = if (fromTop) {
                val e0 = (1f - s0).coerceIn(0.55f, 1f)
                Brush.verticalGradient(
                    0f to tint.copy(alpha = fallbackMaxAlpha),
                    (e0 * 0.24f) to tint.copy(alpha = fallbackMaxAlpha * 0.94f),
                    (e0 * 0.46f) to tint.copy(alpha = fallbackMaxAlpha * 0.78f),
                    (e0 * 0.64f) to tint.copy(alpha = fallbackMaxAlpha * 0.52f),
                    (e0 * 0.78f) to tint.copy(alpha = fallbackMaxAlpha * 0.24f),
                    (e0 * 0.90f) to tint.copy(alpha = fallbackMaxAlpha * 0.08f),
                    (e0 * 0.97f) to tint.copy(alpha = fallbackMaxAlpha * 0.02f),
                    e0 to Color.Transparent,
                    1f to Color.Transparent,
                )
            } else {
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    s0 to Color.Transparent,
                    (s0 + span * 0.16f) to tint.copy(alpha = maxAlpha * 0.02f),
                    (s0 + span * 0.32f) to tint.copy(alpha = maxAlpha * 0.08f),
                    (s0 + span * 0.50f) to tint.copy(alpha = maxAlpha * 0.26f),
                    (s0 + span * 0.70f) to tint.copy(alpha = maxAlpha * 0.56f),
                    (s0 + span * 0.88f) to tint.copy(alpha = maxAlpha * 0.84f),
                    1f to tint.copy(alpha = maxAlpha),
                )
            }

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
                    if (topTintBrush != null) {
                        drawRect(brush = topTintBrush)
                    } else {
                        drawRect(tintColor)
                    }
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

private data class GlassCornerRadii(
    val topLeft: Float,
    val topRight: Float,
    val bottomRight: Float,
    val bottomLeft: Float,
) {
    val maxRadius: Float
        get() = max(max(topLeft, topRight), max(bottomRight, bottomLeft))

    fun scale(factor: Float): GlassCornerRadii = GlassCornerRadii(
        topLeft = topLeft * factor,
        topRight = topRight * factor,
        bottomRight = bottomRight * factor,
        bottomLeft = bottomLeft * factor,
    )

    companion object {
        val Zero = GlassCornerRadii(0f, 0f, 0f, 0f)
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

private fun Outline.extractCornerRadii(size: androidx.compose.ui.geometry.Size): GlassCornerRadii = when (this) {
    is Outline.Rectangle -> GlassCornerRadii.Zero
    is Outline.Rounded -> {
        val maxR = min(size.width, size.height) * 0.5f
        GlassCornerRadii(
            topLeft = min(roundRect.topLeftCornerRadius.x, maxR),
            topRight = min(roundRect.topRightCornerRadius.x, maxR),
            bottomRight = min(roundRect.bottomRightCornerRadius.x, maxR),
            bottomLeft = min(roundRect.bottomLeftCornerRadius.x, maxR),
        )
    }
    is Outline.Generic -> {
        val r = min(size.width, size.height) * 0.5f
        GlassCornerRadii(r, r, r, r)
    }
}

private const val BLUR_SIGMA_SCALE = 0.57735f

private fun convertRadiusToSigma(radius: Float): Float =
    if (radius > 0f) BLUR_SIGMA_SCALE * radius + 0.5f else 0f

private fun convertSigmaToRadius(sigma: Float): Float =
    if (sigma > 0.5f) (sigma - 0.5f) / BLUR_SIGMA_SCALE else 0f

private fun downscaleRadius(radius: Float, scale: Float): Float =
    max(1f, convertSigmaToRadius(convertRadiusToSigma(radius) / scale))

private fun buildLiquidGlassRenderEffect(
    blurPx: Float,
    saturation: Float,
    agslShader: RuntimeShader?,
    width: Float,
    height: Float,
    overscan: Float,
    radii: GlassCornerRadii,
    lensThicknessPx: Float,
    tintColor: Color,
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
            radii.maxRadius > 1.5f &&
            width > 6f &&
            height > 6f
        ) {
            return runCatching {
                var rLT = radii.topLeft
                var rLB = radii.bottomLeft
                var rRT = radii.topRight
                var rRB = radii.bottomRight
                if (rLT + rLB > height && rLT + rLB > 0f) {
                    val a = rLT / (rLT + rLB)
                    rLT = height * a
                    rLB = height * (1f - a)
                }
                if (rRT + rRB > height && rRT + rRB > 0f) {
                    val a = rRT / (rRT + rRB)
                    rRT = height * a
                    rRB = height * (1f - a)
                }
                val thickness = max(min(lensThicknessPx, min(width, height) * 0.22f), 1f)
                val a = tintColor.alpha
                val r = tintColor.red * a
                val g = tintColor.green * a
                val b = tintColor.blue * a

                agslShader.setFloatUniform(
                    "center",
                    overscan + width * 0.5f,
                    overscan + height * 0.5f,
                )
                agslShader.setFloatUniform(
                    "size",
                    width * 0.5f,
                    height * 0.5f,
                )
                agslShader.setFloatUniform(
                    "radius",
                    rRB,
                    rRT,
                    rLB,
                    rLT,
                )
                agslShader.setFloatUniform("thickness", thickness)
                agslShader.setFloatUniform("refract_intensity", VistaGlassTokens.RefractionIntensity)
                agslShader.setFloatUniform("refract_index", VistaGlassTokens.RefractiveIndex)
                agslShader.setFloatUniform("foreground_color_premultiplied", r, g, b, a)

                AndroidRenderEffect.createChainEffect(
                    AndroidRenderEffect.createRuntimeShaderEffect(agslShader, "img"),
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
 * 3D Meniscus Snell Refraction & Premultiplied Optical Tint Shader:
 * - Evaluates 4-corner rounded rectangle SDF (`sdfRect`) and finite-difference surface gradient.
 * - Reconstructs 3D curved meniscus surface normal across `thickness` band.
 * - Applies Snell's law (`refract`) with crown-glass refractive index (`1.5`) and dome height ray path.
 * - Composites premultiplied glass tint over the refracted backdrop with sub-bit IGN anti-banding.
 */
private const val LIQUID_GLASS_AGSL = """
uniform shader img;

uniform float2 center;
uniform float2 size;
uniform float4 radius;
uniform float thickness;
uniform float refract_index;
uniform float refract_intensity;
uniform float4 foreground_color_premultiplied;

half sdfRect(half2 p, half4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    half2 q = abs(p) - size + r.x;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r.x;
}

half4 srcOver(half4 src, half4 dst) {
    half3 outRGB = (src.rgb + dst.rgb * (1.0 - src.a));
    float outA = src.a + (1.0 - src.a) * dst.a;
    return half4(outRGB, outA);
}

half4 main(in float2 fragCoord) {
    half2 p = fragCoord - center;
    half sdRaw = sdfRect(p, radius);
    half2 uv = fragCoord;
    half sd = min(sdRaw, 0.0);
    half sdX = sdfRect(p + half2(1.0, 0.0), radius);
    half sdY = sdfRect(p + half2(0.0, 1.0), radius);

    half n_cos = max(thickness + sd, 0.0) / thickness;
    half n_cos2 = n_cos * n_cos;
    half n_sin = sqrt(max(1.0 - n_cos2, 0.0));
    half3 normal = normalize(half3((sdX - sdRaw) * n_cos, (sdY - sdRaw) * n_cos, max(n_sin, 0.001)));

    half3 refract_vec = refract(half3(0.0, 0.0, -1.0), normal, 1.0 / refract_index);
    half h = sd < -thickness ? thickness : sqrt(max(sd * (-2.0 * thickness - sd), 0.0));
    half refract_length = (h + 8.0 * thickness) / max(-refract_vec.z, 0.001);

    uv += refract_vec.xy * refract_length * refract_intensity;
    uv = clamp(uv, center - size + 0.5, center + size - 0.5);

    half4 col = srcOver(half4(foreground_color_premultiplied), img.eval(uv));
    float ign = fract(52.9829189 * fract(dot(fragCoord, float2(0.06711056, 0.00583715)))) - 0.5;
    col.rgb += half3(ign * (1.0 / 255.0));
    return col;
}
"""
