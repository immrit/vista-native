package ir.coffevista.vista_native.core.designsystem.component

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.R
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Immutable
data class VistaAmbientEffectSpec(
    val effect: String = "none",
    val density: String = "low",
)

val LocalVistaAmbientEffect = compositionLocalOf { VistaAmbientEffectSpec() }

private data class AppBarParticleSeed(
    val xFraction: Float,
    val phaseOffset: Float,
    val speedFactor: Float,
    val sizeDp: Float,
    val spinDeg: Float,
    val variant: Int,
)

@Composable
fun VistaAppBarAmbientOverlay(
    modifier: Modifier = Modifier,
    spec: VistaAmbientEffectSpec = LocalVistaAmbientEffect.current,
) {
    val normalized = spec.effect.trim().lowercase()
    if (normalized == "none" || normalized !in setOf("blossom", "snow", "stars", "confetti")) return

    val context = LocalContext.current
    val animationsDisabled = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    if (animationsDisabled) return

    val (resA, resB) = when (normalized) {
        "blossom" -> R.drawable.vista_particle_blossom_1 to R.drawable.vista_particle_blossom_2
        "snow" -> R.drawable.vista_particle_snow_1 to R.drawable.vista_particle_snow_2
        "stars" -> R.drawable.vista_particle_star_1 to R.drawable.vista_particle_star_2
        else -> R.drawable.vista_particle_confetti_1 to R.drawable.vista_particle_confetti_2
    }

    val bitmapA = ImageBitmap.imageResource(res = context.resources, id = resA)
    val bitmapB = ImageBitmap.imageResource(res = context.resources, id = resB)

    val count = if (spec.density.equals("medium", ignoreCase = true)) 14 else 8
    val seeds = remember(normalized, count) {
        List(count) { i ->
            AppBarParticleSeed(
                xFraction = ((i * 31 + 9) % 94 + 3) / 100f,
                phaseOffset = ((i * 47 + 11) % 100) / 100f,
                speedFactor = 0.75f + ((i * 17) % 6) * 0.1f,
                sizeDp = 13f + ((i * 13) % 6),
                spinDeg = ((i * 53) % 70) - 35f,
                variant = i % 2,
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "appbarAmbientTransition")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "appbarAmbientPhase",
    )

    Canvas(modifier = modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        seeds.forEachIndexed { idx, seed ->
            val progress = (seed.phaseOffset + phase * seed.speedFactor) % 1f
            val bmp = if (seed.variant == 0) bitmapA else bitmapB
            val sizePx = seed.sizeDp.dp.toPx().roundToInt().coerceAtLeast(16)
            val swayPx = sin((progress * 2f * PI).toFloat() + idx * 1.1f) * 10.dp.toPx()

            val cx = (seed.xFraction * w + swayPx).coerceIn(sizePx * 0.4f, (w - sizePx * 0.4f).coerceAtLeast(sizePx * 0.4f))
            val cy = -sizePx * 0.6f + progress * (h + sizePx * 1.2f)

            val alpha = when {
                progress < 0.15f -> (progress / 0.15f).coerceIn(0f, 1f)
                progress > 0.72f -> ((1f - progress) / 0.28f).coerceIn(0f, 1f)
                else -> 1f
            } * 0.88f

            if (alpha > 0.01f) {
                val deg = seed.spinDeg + progress * if (idx % 2 == 0) 140f else -110f
                rotate(degrees = deg, pivot = Offset(cx, cy)) {
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(
                            x = (cx - sizePx / 2f).roundToInt(),
                            y = (cy - sizePx / 2f).roundToInt(),
                        ),
                        dstSize = IntSize(sizePx, sizePx),
                        alpha = alpha,
                    )
                }
            }
        }
    }
}
