package ir.coffevista.vista_native.features.stories.ui.canvas

import ir.coffevista.vista_native.features.stories.domain.StoryElement
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure (Android-free) geometry for the story canvas. Everything in this file is unit-testable on
 * the JVM and is shared by the editor (live gestures) and the player (static placement) so both
 * resolve an element to exactly the same pixels.
 */
internal object StoryStageSpec {
    const val RefWidth = 360f
    const val RefHeight = 640f
    const val Aspect = 9f / 16f

    const val MinScale = 0.25f
    const val MaxScale = 6f

    /** Horizontal safe margin in reference units (element edges magnetise to it). */
    const val SideMargin = 24f

    /** Fractions of the stage height reserved for the header / reply bar of the player. */
    const val TopSafeFraction = 0.12f
    const val BottomSafeFraction = 0.14f

    /** Pixel-to-offset factors of the legacy (schema 1) renderer, and its reference device. */
    const val LegacyOffsetXPx = 200f
    const val LegacyOffsetYPx = 300f
    const val LegacyRefWidthPx = 1080f
    const val LegacyRefHeightPx = 1920f
    const val LegacyMaxOffset = 1.5f
}

/** Centre (normalised), uniform scale and clockwise rotation (degrees) of a story element. */
internal data class StoryTransform(
    val cx: Float,
    val cy: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
)

internal data class StageSize(val width: Float, val height: Float) {
    /** Pixels per reference unit. */
    val unit: Float get() = width / StoryStageSpec.RefWidth
}

// ---------------------------------------------------------------------------------------------
// Schema bridging
// ---------------------------------------------------------------------------------------------

internal object LegacyStoryMapper {
    fun resolve(element: StoryElement): StoryTransform {
        val xn = element.xNorm
        val yn = element.yNorm
        val scale = element.scale.takeIf { it.isFinite() && it > 0f } ?: 1f
        val rotation = element.rotation.takeIf { it.isFinite() } ?: 0f
        return if (xn != null && yn != null && xn.isFinite() && yn.isFinite()) {
            StoryTransform(xn, yn, scale, rotation)
        } else {
            StoryTransform(
                cx = 0.5f + element.x * StoryStageSpec.LegacyOffsetXPx / StoryStageSpec.LegacyRefWidthPx,
                cy = 0.5f + element.y * StoryStageSpec.LegacyOffsetYPx / StoryStageSpec.LegacyRefHeightPx,
                scale = scale,
                rotation = rotation,
            )
        }
    }

    fun legacyOffset(cx: Float, cy: Float): Pair<Float, Float> {
        val lim = StoryStageSpec.LegacyMaxOffset
        val x = (cx - 0.5f) * StoryStageSpec.LegacyRefWidthPx / StoryStageSpec.LegacyOffsetXPx
        val y = (cy - 0.5f) * StoryStageSpec.LegacyRefHeightPx / StoryStageSpec.LegacyOffsetYPx
        return x.coerceIn(-lim, lim) to y.coerceIn(-lim, lim)
    }
}

internal fun StoryElement.resolvedTransform(): StoryTransform = LegacyStoryMapper.resolve(this)

internal fun StoryElement.withTransform(t: StoryTransform): StoryElement {
    val (lx, ly) = LegacyStoryMapper.legacyOffset(t.cx, t.cy)
    return copy(
        schema = 2,
        xNorm = t.cx,
        yNorm = t.cy,
        scale = t.scale,
        rotation = t.rotation,
        x = lx,
        y = ly,
    )
}

// ---------------------------------------------------------------------------------------------
// Transform maths
// ---------------------------------------------------------------------------------------------

internal object StoryTransformMath {
    private const val HardMinScale = 0.05f
    private const val HardMaxScale = 16f
    private const val OVERSHOOT = 0.2f

    fun normalizeAngle(degrees: Float): Float {
        var a = degrees % 360f
        if (a > 180f) a -= 360f
        if (a <= -180f) a += 360f
        return a
    }

    fun angleDiff(a: Float, b: Float): Float = normalizeAngle(a - b)

    fun applyGesture(
        t: StoryTransform,
        stage: StageSize,
        prevCentroidX: Float,
        prevCentroidY: Float,
        centroidX: Float,
        centroidY: Float,
        zoom: Float,
        rotationDeg: Float,
    ): StoryTransform {
        val z = if (zoom.isFinite() && zoom > 0f) zoom else 1f
        val rot = if (rotationDeg.isFinite()) rotationDeg else 0f
        val rad = rot * (PI.toFloat() / 180f)
        val c = cos(rad)
        val s = sin(rad)
        val dx = t.cx * stage.width - prevCentroidX
        val dy = t.cy * stage.height - prevCentroidY
        val rx = dx * c - dy * s
        val ry = dx * s + dy * c
        val nx = centroidX + z * rx
        val ny = centroidY + z * ry
        return StoryTransform(
            cx = nx / stage.width,
            cy = ny / stage.height,
            scale = (t.scale * z).coerceIn(HardMinScale, HardMaxScale),
            rotation = normalizeAngle(t.rotation + rot),
        )
    }

    fun rubberBandScale(raw: Float, min: Float = StoryStageSpec.MinScale, max: Float = StoryStageSpec.MaxScale): Float {
        return when {
            raw > max -> max * (1f + OVERSHOOT * (1f - exp(-(raw / max - 1f) / OVERSHOOT)))
            raw < min -> min / (1f + OVERSHOOT * (1f - exp(-(min / raw - 1f) / OVERSHOOT)))
            else -> raw
        }
    }

    fun clampScale(scale: Float): Float = scale.coerceIn(StoryStageSpec.MinScale, StoryStageSpec.MaxScale)

    fun restingTransform(t: StoryTransform, halfExtentsUnrotatedPx: Pair<Float, Float>, stage: StageSize): StoryTransform {
        val scale = clampScale(t.scale)
        val (ex, ey) = rotatedHalfExtents(halfExtentsUnrotatedPx.first, halfExtentsUnrotatedPx.second, scale, t.rotation)
        val keepX = min(ex, stage.width * 0.5f)
        val keepY = min(ey, stage.height * 0.5f)
        val minVisible = 28f * stage.unit
        val minCx = (minVisible - keepX).coerceAtMost(stage.width * 0.5f)
        val maxCx = (stage.width + keepX - minVisible).coerceAtLeast(stage.width * 0.5f)
        val minCy = (minVisible - keepY).coerceAtMost(stage.height * 0.5f)
        val maxCy = (stage.height + keepY - minVisible).coerceAtLeast(stage.height * 0.5f)
        val cxPx = (t.cx * stage.width).coerceIn(minCx, maxCx)
        val cyPx = (t.cy * stage.height).coerceIn(minCy, maxCy)
        return t.copy(cx = cxPx / stage.width, cy = cyPx / stage.height, scale = scale)
    }

    fun rotatedHalfExtents(halfW: Float, halfH: Float, scale: Float, rotationDeg: Float): Pair<Float, Float> {
        val rad = rotationDeg * (PI.toFloat() / 180f)
        val c = abs(cos(rad))
        val s = abs(sin(rad))
        return scale * (halfW * c + halfH * s) to scale * (halfW * s + halfH * c)
    }
}

// ---------------------------------------------------------------------------------------------
// Hit testing
// ---------------------------------------------------------------------------------------------

internal object StoryHitTest {
    fun contains(
        t: StoryTransform,
        stage: StageSize,
        halfW: Float,
        halfH: Float,
        px: Float,
        py: Float,
        minTouchHalfPx: Float,
    ): Boolean {
        if (halfW <= 0f || halfH <= 0f) return false
        val rad = -t.rotation * (PI.toFloat() / 180f)
        val dx = px - t.cx * stage.width
        val dy = py - t.cy * stage.height
        val lx = dx * cos(rad) - dy * sin(rad)
        val ly = dx * sin(rad) + dy * cos(rad)
        val hw = max(halfW * t.scale, minTouchHalfPx)
        val hh = max(halfH * t.scale, minTouchHalfPx)
        return abs(lx) <= hw && abs(ly) <= hh
    }
}

// ---------------------------------------------------------------------------------------------
// Magnetic snapping
// ---------------------------------------------------------------------------------------------

internal enum class SnapTarget { Center, MinMargin, MaxMargin }

internal data class SnapState(
    val x: SnapTarget? = null,
    val y: SnapTarget? = null,
    val rotationDetent: Int? = null,
) {
    fun newlyEngagedComparedTo(previous: SnapState): Boolean =
        (x != null && x != previous.x) ||
            (y != null && y != previous.y) ||
            (rotationDetent != null && rotationDetent != previous.rotationDetent)

    companion object {
        val None = SnapState()
    }
}

internal data class SnapGuides(
    val vertical: List<Float> = emptyList(),
    val horizontal: List<Float> = emptyList(),
    val rotationLocked: Boolean = false,
) {
    companion object {
        val None = SnapGuides()
    }
}

internal data class SnapResult(
    val transform: StoryTransform,
    val state: SnapState,
    val guides: SnapGuides,
)

internal class StorySnapEngine(private val stage: StageSize) {
    private val enter = ENTER_UNITS * stage.unit
    private val hold = HOLD_UNITS * stage.unit
    private val sideMargin = StoryStageSpec.SideMargin * stage.unit
    private val topSafe = StoryStageSpec.TopSafeFraction * stage.height
    private val bottomSafe = StoryStageSpec.BottomSafeFraction * stage.height

    fun snap(
        raw: StoryTransform,
        halfW: Float,
        halfH: Float,
        previous: SnapState,
        snapPosition: Boolean = true,
        snapRotation: Boolean = true,
    ): SnapResult {
        var rotation = raw.rotation
        var detent: Int? = null
        if (snapRotation) {
            val nearestIndex = ((raw.rotation / DETENT_STEP).roundToInt())
            val nearest = nearestIndex * DETENT_STEP
            val diff = abs(StoryTransformMath.angleDiff(raw.rotation, nearest))
            val index = Math.floorMod(nearestIndex, 8)
            val threshold = if (previous.rotationDetent == index) DETENT_HOLD else DETENT_ENTER
            if (diff <= threshold) {
                rotation = StoryTransformMath.normalizeAngle(nearest)
                detent = index
            }
        }

        var cxPx = raw.cx * stage.width
        var cyPx = raw.cy * stage.height
        var snapX: SnapTarget? = null
        var snapY: SnapTarget? = null
        val vertical = ArrayList<Float>(2)
        val horizontal = ArrayList<Float>(2)

        if (snapPosition) {
            val (ex, ey) = StoryTransformMath.rotatedHalfExtents(halfW, halfH, raw.scale, rotation)

            val fitsX = 2f * ex <= stage.width - 2f * sideMargin
            val xCandidates = buildList {
                add(SnapTarget.Center to stage.width / 2f)
                if (fitsX) {
                    add(SnapTarget.MinMargin to sideMargin + ex)
                    add(SnapTarget.MaxMargin to stage.width - sideMargin - ex)
                }
            }
            pick(cxPx, xCandidates, previous.x)?.let { (target, pos) ->
                snapX = target
                cxPx = pos
            }

            val fitsY = 2f * ey <= stage.height - topSafe - bottomSafe
            val yCandidates = buildList {
                add(SnapTarget.Center to stage.height / 2f)
                if (fitsY) {
                    add(SnapTarget.MinMargin to topSafe + ey)
                    add(SnapTarget.MaxMargin to stage.height - bottomSafe - ey)
                }
            }
            pick(cyPx, yCandidates, previous.y)?.let { (target, pos) ->
                snapY = target
                cyPx = pos
            }

            snapX?.let {
                vertical += when (it) {
                    SnapTarget.Center -> stage.width / 2f
                    SnapTarget.MinMargin -> sideMargin
                    SnapTarget.MaxMargin -> stage.width - sideMargin
                }
            }
            snapY?.let {
                horizontal += when (it) {
                    SnapTarget.Center -> stage.height / 2f
                    SnapTarget.MinMargin -> topSafe
                    SnapTarget.MaxMargin -> stage.height - bottomSafe
                }
            }
        }

        return SnapResult(
            transform = raw.copy(cx = cxPx / stage.width, cy = cyPx / stage.height, rotation = rotation),
            state = SnapState(snapX, snapY, detent),
            guides = SnapGuides(vertical, horizontal, rotationLocked = detent != null),
        )
    }

    private fun pick(
        value: Float,
        candidates: List<Pair<SnapTarget, Float>>,
        previous: SnapTarget?,
    ): Pair<SnapTarget, Float>? {
        var best: Pair<SnapTarget, Float>? = null
        var bestDistance = Float.MAX_VALUE
        for (candidate in candidates) {
            val distance = abs(candidate.second - value)
            val limit = if (candidate.first == previous) hold else enter
            if (distance <= limit && distance < bestDistance) {
                best = candidate
                bestDistance = distance
            }
        }
        return best
    }

    companion object {
        const val ENTER_UNITS = 6f
        const val HOLD_UNITS = 11f
        const val DETENT_STEP = 45f
        const val DETENT_ENTER = 4f
        const val DETENT_HOLD = 8f
    }
}

// ---------------------------------------------------------------------------------------------
// Trash zone
// ---------------------------------------------------------------------------------------------

internal class StoryTrashZone(private val stage: StageSize) {
    val centerX: Float = stage.width / 2f
    val centerY: Float = stage.height - TRASH_BOTTOM_UNITS * stage.unit
    val radius: Float = TRASH_RADIUS_UNITS * stage.unit

    fun isHovered(px: Float, py: Float): Boolean =
        hypot(px - centerX, py - centerY) <= radius * HOVER_FACTOR

    private companion object {
        const val TRASH_BOTTOM_UNITS = 76f
        const val TRASH_RADIUS_UNITS = 28f
        const val HOVER_FACTOR = 1.9f
    }
}
