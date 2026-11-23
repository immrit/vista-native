package ir.coffevista.vista_native.core.designsystem.component

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ir.coffevista.vista_native.core.designsystem.R
import ir.coffevista.vista_native.core.designsystem.theme.vistaMotion
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class VistaViuStateKind {
    SearchEmpty,
    Offline,
    Error,
    Empty,
    EmptyChat,
    NotFound,
    Locked,
    Maintenance,
}

private val OfflineHints = listOf(
    "اینترنت",
    "آفلاین",
    "اتصال",
    "شبکه",
    "ارتباط",
    "offline",
    "network",
    "timeout",
    "unknownhost",
    "socket",
)

private val MaintenanceHints = listOf(
    "بروزرسانی",
    "به‌روزرسانی",
    "تعمیر",
    "نگهداری",
    "maintenance",
)

private val LockedHints = listOf(
    "خصوصی",
    "قفل",
    "مسدود",
    "محرمانه",
)

private val SearchHints = listOf(
    "نتیجه",
    "جستجو",
    "یافت نشد",
    "پیدا نشد",
)

private val ChatHints = listOf(
    "پیام",
    "گفتگو",
    "چت",
    "اعلان",
    "مکالمه",
)

fun inferVistaViuStateKind(
    text: String?,
    isError: Boolean = false,
): VistaViuStateKind {
    val normalized = text.orEmpty().lowercase()
    return when {
        OfflineHints.any { normalized.contains(it) } -> VistaViuStateKind.Offline
        MaintenanceHints.any { normalized.contains(it) } -> VistaViuStateKind.Maintenance
        LockedHints.any { normalized.contains(it) } -> VistaViuStateKind.Locked
        SearchHints.any { normalized.contains(it) } ->
            if (isError) VistaViuStateKind.Error else VistaViuStateKind.SearchEmpty
        ChatHints.any { normalized.contains(it) } -> VistaViuStateKind.EmptyChat
        isError -> VistaViuStateKind.Error
        else -> VistaViuStateKind.Empty
    }
}

@DrawableRes
private fun drawableResForKind(kind: VistaViuStateKind): Int = when (kind) {
    VistaViuStateKind.SearchEmpty -> R.drawable.viu_state_search_empty
    VistaViuStateKind.Offline -> R.drawable.viu_state_offline
    VistaViuStateKind.Error,
    VistaViuStateKind.Maintenance,
    -> R.drawable.viu_state_error
    VistaViuStateKind.Empty -> R.drawable.viu_state_empty
    VistaViuStateKind.EmptyChat -> R.drawable.viu_state_empty_chat
    VistaViuStateKind.NotFound,
    VistaViuStateKind.Locked,
    -> R.drawable.viu_state_not_found
}

@RawRes
private fun rawAnimResForKind(kind: VistaViuStateKind): Int = when (kind) {
    VistaViuStateKind.SearchEmpty -> R.raw.viu_state_search_empty_anim
    VistaViuStateKind.Offline -> R.raw.viu_state_offline_anim
    VistaViuStateKind.Error,
    VistaViuStateKind.Maintenance,
    -> R.raw.viu_state_error_anim
    VistaViuStateKind.Empty -> R.raw.viu_state_empty_anim
    VistaViuStateKind.EmptyChat -> R.raw.viu_state_empty_chat_anim
    VistaViuStateKind.NotFound,
    VistaViuStateKind.Locked,
    -> R.raw.viu_state_not_found_anim
}

/**
 * Hardware-decoded animated 24-frame WebP puppet illustration of Vista's official mascot Viu (ویو)
 * with independent ear motion, eye blinks, animated props, and synchronized vector motion aura.
 */
@Composable
fun VistaViuStateIllustration(
    kind: VistaViuStateKind,
    modifier: Modifier = Modifier,
    size: Dp = 152.dp,
) {
    val animationsEnabled = MaterialTheme.vistaMotion.standard > 0
    val isPreview = LocalInspectionMode.current
    val useNativeAnimatedClip =
        animationsEnabled && !isPreview && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val density = LocalDensity.current

    val floatPhase: Float
    val tiltDeg: Float
    val orbitAngle: Float
    val pulsePhase: Float

    if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "vistaViuStateMotion")
        floatPhase = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "viuFloat",
        ).value
        tiltDeg = transition.animateFloat(
            initialValue = -1.2f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "viuTilt",
        ).value
        orbitAngle = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 11000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "viuOrbit",
        ).value
        pulsePhase = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "viuPulse",
        ).value
    } else {
        floatPhase = 0.2f
        tiltDeg = 0f
        orbitAngle = 0f
        pulsePhase = 0.5f
    }

    val subtleFloatPx = if (useNativeAnimatedClip) {
        with(density) { ((-4f * floatPhase) + 1f).dp.toPx() }
    } else {
        with(density) { ((-10f * floatPhase) + 2f).dp.toPx() }
    }
    val shadowScale = 1.0f - (0.20f * floatPhase)
    val shadowAlpha = 0.24f - (0.10f * floatPhase)

    val (primaryAccent, secondaryAccent) = when (kind) {
        VistaViuStateKind.SearchEmpty -> Color(0xFF00D4FF) to VistaBrandColors.Indigo
        VistaViuStateKind.Offline -> Color(0xFFF59E0B) to Color(0xFF00D4FF)
        VistaViuStateKind.Error -> Color(0xFFF43F5E) to Color(0xFFF96B5B)
        VistaViuStateKind.Maintenance -> Color(0xFFF59E0B) to Color(0xFF1FB6E8)
        VistaViuStateKind.EmptyChat -> Color(0xFF1FB6E8) to VistaBrandColors.Indigo
        VistaViuStateKind.Locked -> Color(0xFF10B981) to VistaBrandColors.Indigo
        VistaViuStateKind.NotFound -> VistaBrandColors.Indigo to Color(0xFF00D4FF)
        VistaViuStateKind.Empty -> Color(0xFF00D4FF) to VistaBrandColors.Violet
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val baseRadius = minOf(w, h) * 0.44f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = 0.20f + 0.06f * pulsePhase),
                        secondaryAccent.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy * 0.96f),
                    radius = baseRadius * 1.15f,
                ),
                radius = baseRadius * 1.15f,
                center = Offset(cx, cy * 0.96f),
            )

            val shadowW = w * 0.46f * shadowScale
            val shadowH = h * 0.075f * shadowScale
            drawOval(
                color = Color(0xFF0A193B).copy(alpha = shadowAlpha),
                topLeft = Offset(cx - shadowW / 2f, h * 0.89f - shadowH / 2f),
                size = Size(shadowW, shadowH),
            )

            val dashEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(baseRadius * 0.34f, baseRadius * 0.24f),
                phase = 0f,
            )
            rotate(degrees = orbitAngle, pivot = Offset(cx, cy)) {
                drawCircle(
                    color = primaryAccent.copy(alpha = 0.26f),
                    radius = baseRadius * 0.96f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.0f, cap = StrokeCap.Round, pathEffect = dashEffect),
                )
            }

            when (kind) {
                VistaViuStateKind.Offline -> {
                    for (i in 0..1) {
                        val p = ((pulsePhase + i * 0.45f) % 1f)
                        val r = baseRadius * (0.26f + 0.22f * p)
                        drawArc(
                            color = primaryAccent.copy(alpha = (1f - p) * 0.50f),
                            startAngle = -135f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(cx - r, h * 0.14f - r),
                            size = Size(r * 2f, r * 2f),
                            style = Stroke(width = 2.4f, cap = StrokeCap.Round),
                        )
                    }
                }

                VistaViuStateKind.EmptyChat -> {
                    for (idx in 0..2) {
                        val phase = ((pulsePhase + idx * 0.28f) % 1f)
                        val dotY = h * 0.20f - sin(phase * PI).toFloat() * h * 0.035f
                        drawCircle(
                            color = primaryAccent.copy(alpha = 0.45f + 0.45f * sin(phase * PI).toFloat()),
                            radius = w * 0.015f,
                            center = Offset(w * (0.80f + idx * 0.045f), dotY),
                        )
                    }
                }

                VistaViuStateKind.Error,
                VistaViuStateKind.Maintenance,
                -> {
                    rotate(degrees = -orbitAngle * 1.4f, pivot = Offset(cx, cy)) {
                        drawArc(
                            color = secondaryAccent.copy(alpha = 0.34f),
                            startAngle = 25f,
                            sweepAngle = 70f,
                            useCenter = false,
                            topLeft = Offset(cx - baseRadius * 0.82f, cy - baseRadius * 0.82f),
                            size = Size(baseRadius * 1.64f, baseRadius * 1.64f),
                            style = Stroke(width = 2.4f, cap = StrokeCap.Round),
                        )
                    }
                }

                else -> {
                    val radA = (orbitAngle * PI / 180.0)
                    val ax = cx + cos(radA).toFloat() * baseRadius * 0.94f
                    val ay = cy + sin(radA).toFloat() * baseRadius * 0.82f
                    drawCircle(
                        color = primaryAccent.copy(alpha = 0.80f),
                        radius = w * 0.020f * (0.75f + 0.35f * pulsePhase),
                        center = Offset(ax, ay),
                    )
                }
            }
        }

        if (useNativeAnimatedClip) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = false
                    }
                },
                update = { imageView ->
                    if (imageView.tag != kind) {
                        (imageView.drawable as? AnimatedImageDrawable)?.stop()
                        imageView.tag = kind
                        val rawRes = rawAnimResForKind(kind)
                        val decoded = runCatching {
                            val source = ImageDecoder.createSource(imageView.resources, rawRes)
                            ImageDecoder.decodeDrawable(source)
                        }.getOrNull()
                        if (decoded != null) {
                            imageView.setImageDrawable(decoded)
                            (decoded as? AnimatedImageDrawable)?.apply {
                                repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                                start()
                            }
                        } else {
                            imageView.setImageResource(drawableResForKind(kind))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize(0.92f)
                    .graphicsLayer {
                        translationY = subtleFloatPx
                    },
            )
        } else {
            Image(
                painter = painterResource(drawableResForKind(kind)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .graphicsLayer {
                        translationY = subtleFloatPx
                        rotationZ = tiltDeg
                    },
            )
        }
    }
}
