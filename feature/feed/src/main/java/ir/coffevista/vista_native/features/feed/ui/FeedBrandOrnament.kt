package ir.coffevista.vista_native.features.feed.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.navigation.LocalVistaInternalLinkNavigator
import ir.coffevista.vista_native.core.network.RemoteAmbientEffect
import ir.coffevista.vista_native.core.network.RemoteBannerSlot
import ir.coffevista.vista_native.core.network.RemoteBrandOrnament
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun VistaBrandedFeedLogo(
    dark: Boolean,
    @DrawableRes defaultLogoRes: Int,
    ornament: RemoteBrandOrnament,
    modifier: Modifier = Modifier,
) {
    var showModal by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val customLogoUrl = if (dark) ornament.customLogoDarkUrl else ornament.customLogoLightUrl
    val isInteractive = ornament.enabled && ornament.tapAction != "none"

    Box(
        modifier = modifier
            .then(
                if (isInteractive) {
                    Modifier.clickable {
                        when (ornament.tapAction) {
                            "modal" -> if (ornament.tapTitle.isNotBlank() || ornament.tapMessage.isNotBlank()) {
                                showModal = true
                            }
                            "link" -> {
                                val target = ornament.tapUrl.trim()
                                if (target.startsWith("https://", ignoreCase = true) ||
                                    target.startsWith("http://", ignoreCase = true)
                                ) {
                                    runCatching { uriHandler.openUri(target) }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (customLogoUrl.startsWith("https://", ignoreCase = true)) {
            AsyncImage(
                model = customLogoUrl,
                contentDescription = "Vista",
                placeholder = painterResource(defaultLogoRes),
                error = painterResource(defaultLogoRes),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(35.dp),
            )
        } else {
            Image(
                painter = painterResource(defaultLogoRes),
                contentDescription = "Vista",
                modifier = Modifier.size(35.dp),
            )
        }

        if (ornament.enabled && ornament.preset != "none") {
            val ornamentModifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = ornament.offsetX.dp, y = ornament.offsetY.dp)
                .size(28.dp)
                .graphicsLayer {
                    scaleX = ornament.scale
                    scaleY = ornament.scale
                    rotationZ = ornament.rotationDeg.toFloat()
                }

            if (ornament.preset == "custom_image" && ornament.customImageUrl.startsWith("https://", ignoreCase = true)) {
                AsyncImage(
                    model = ornament.customImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = ornamentModifier,
                )
            } else if (ornament.preset != "custom_image") {
                Canvas(modifier = ornamentModifier) {
                    drawOrnamentPreset(ornament.preset)
                }
            }
        }
    }

    if (showModal) {
        AlertDialog(
            onDismissRequest = { showModal = false },
            title = {
                Text(
                    text = ornament.tapTitle.ifBlank { "پیام ویستا" },
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(text = ornament.tapMessage)
            },
            confirmButton = {
                if (ornament.tapUrl.startsWith("https://", ignoreCase = true)) {
                    TextButton(onClick = {
                        showModal = false
                        runCatching { uriHandler.openUri(ornament.tapUrl.trim()) }
                    }) {
                        Text("مشاهده")
                    }
                }
                TextButton(onClick = { showModal = false }) {
                    Text("بستن")
                }
            },
        )
    }
}

private fun DrawScope.drawOrnamentPreset(preset: String) {
    val w = size.width
    val h = size.height
    when (preset) {
        "santa_hat" -> {
            val hatPath = Path().apply {
                moveTo(w * 0.16f, h * 0.66f)
                quadraticTo(w * 0.36f, h * 0.14f, w * 0.80f, h * 0.24f)
                quadraticTo(w * 0.68f, h * 0.44f, w * 0.74f, h * 0.66f)
                close()
            }
            drawPath(hatPath, Color(0xFFDC2626))
            drawRoundRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(w * 0.12f, h * 0.60f),
                size = Size(w * 0.68f, h * 0.20f),
                cornerRadius = CornerRadius(h * 0.10f, h * 0.10f),
            )
            drawCircle(
                color = Color(0xFFFFFFFF),
                radius = w * 0.11f,
                center = Offset(w * 0.82f, h * 0.27f),
            )
        }
        "nowruz_sprout" -> {
            drawRoundRect(
                color = Color(0xFFD97706),
                topLeft = Offset(w * 0.25f, h * 0.64f),
                size = Size(w * 0.50f, h * 0.22f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            )
            val leftLeaf = Path().apply {
                moveTo(w * 0.50f, h * 0.64f)
                quadraticTo(w * 0.22f, h * 0.44f, w * 0.28f, h * 0.22f)
                quadraticTo(w * 0.48f, h * 0.34f, w * 0.50f, h * 0.64f)
            }
            val rightLeaf = Path().apply {
                moveTo(w * 0.50f, h * 0.64f)
                quadraticTo(w * 0.78f, h * 0.42f, w * 0.74f, h * 0.18f)
                quadraticTo(w * 0.52f, h * 0.32f, w * 0.50f, h * 0.64f)
            }
            drawPath(leftLeaf, Color(0xFF16A34A))
            drawPath(rightLeaf, Color(0xFF22C55E))
        }
        "yalda_pomegranate" -> {
            val crown = Path().apply {
                moveTo(w * 0.38f, h * 0.28f)
                lineTo(w * 0.43f, h * 0.14f)
                lineTo(w * 0.50f, h * 0.24f)
                lineTo(w * 0.57f, h * 0.14f)
                lineTo(w * 0.62f, h * 0.28f)
                close()
            }
            drawPath(crown, Color(0xFF991B1B))
            drawCircle(
                color = Color(0xFFBE123C),
                radius = w * 0.28f,
                center = Offset(w * 0.50f, h * 0.56f),
            )
            drawCircle(
                color = Color(0xFFFDA4AF),
                radius = w * 0.055f,
                center = Offset(w * 0.41f, h * 0.48f),
            )
        }
        "sparkle_crown" -> {
            val crown = Path().apply {
                moveTo(w * 0.14f, h * 0.72f)
                lineTo(w * 0.18f, h * 0.26f)
                lineTo(w * 0.36f, h * 0.48f)
                lineTo(w * 0.50f, h * 0.18f)
                lineTo(w * 0.64f, h * 0.48f)
                lineTo(w * 0.82f, h * 0.26f)
                lineTo(w * 0.86f, h * 0.72f)
                close()
            }
            drawPath(crown, Color(0xFFF59E0B))
            drawCircle(Color(0xFFEF4444), radius = w * 0.05f, center = Offset(w * 0.50f, h * 0.55f))
            drawCircle(Color(0xFF38BDF8), radius = w * 0.04f, center = Offset(w * 0.30f, h * 0.58f))
            drawCircle(Color(0xFF38BDF8), radius = w * 0.04f, center = Offset(w * 0.70f, h * 0.58f))
        }
    }
}

private data class AmbientParticleSeed(
    val xFraction: Float,
    val yOffsetFraction: Float,
    val speedFactor: Float,
    val radiusDp: Float,
    val colorIndex: Int,
)

@Composable
internal fun VistaFeedAmbientParticles(
    ambientEffect: RemoteAmbientEffect,
    modifier: Modifier = Modifier,
) {
    if (ambientEffect.effect == "none") return

    val count = if (ambientEffect.density == "medium") 26 else 14
    val seeds = remember(ambientEffect.effect, count) {
        List(count) { i ->
            AmbientParticleSeed(
                xFraction = ((i * 37 + 13) % 97) / 100f,
                yOffsetFraction = ((i * 53 + 7) % 100) / 100f,
                speedFactor = 0.7f + ((i * 19) % 7) * 0.1f,
                radiusDp = 2.2f + ((i * 11) % 4) * 0.8f,
                colorIndex = i,
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "ambientParticles")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ambientPhase",
    )

    val confettiColors = remember {
        listOf(
            Color(0xFF6366F1),
            Color(0xFFEC4899),
            Color(0xFF10B981),
            Color(0xFFF59E0B),
            Color(0xFF38BDF8),
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        seeds.forEach { seed ->
            val progress = (seed.yOffsetFraction + phase * seed.speedFactor) % 1f
            val sway = sin((progress * 2f * PI).toFloat() + seed.colorIndex) * 14f
            val x = (seed.xFraction * w + sway).coerceIn(0f, w)
            val y = progress * h
            val r = seed.radiusDp.dp.toPx()

            when (ambientEffect.effect) {
                "snow" -> drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = r,
                    center = Offset(x, y),
                )
                "blossom" -> drawCircle(
                    color = Color(0xFFF472B6).copy(alpha = 0.45f),
                    radius = r * 1.15f,
                    center = Offset(x, y),
                )
                "confetti" -> {
                    val col = confettiColors[seed.colorIndex % confettiColors.size].copy(alpha = 0.55f)
                    drawRoundRect(
                        color = col,
                        topLeft = Offset(x, y),
                        size = Size(r * 2f, r * 1.1f),
                        cornerRadius = CornerRadius(2f, 2f),
                    )
                }
                "stars" -> {
                    val twinkle = (0.35f + 0.55f * (0.5f + 0.5f * cos((phase * 4f * PI + seed.colorIndex).toFloat())))
                    drawLine(
                        color = Color(0xFFFDE047).copy(alpha = twinkle),
                        start = Offset(x - r, y),
                        end = Offset(x + r, y),
                        strokeWidth = 1.6f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = Color(0xFFFDE047).copy(alpha = twinkle),
                        start = Offset(x, y - r),
                        end = Offset(x, y + r),
                        strokeWidth = 1.6f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
internal fun VistaRemoteFeedBannerCard(
    banner: RemoteBannerSlot,
    modifier: Modifier = Modifier,
) {
    if (!banner.enabled || (banner.title.isBlank() && banner.subtitle.isBlank())) return
    val uriHandler = LocalUriHandler.current
    val internalNavigator = LocalVistaInternalLinkNavigator.current
    val bgColor = remember(banner.bgColor) { parseHexColor(banner.bgColor, Color(0xFF6366F1)) }
    val fgColor = remember(banner.textColor) { parseHexColor(banner.textColor, Color.White) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (banner.linkUrl.isNotBlank()) {
                    Modifier.clickable {
                        val target = banner.linkUrl.trim()
                        if (internalNavigator?.openUrl(target) != true &&
                            (target.startsWith("https://", ignoreCase = true) ||
                                target.startsWith("http://", ignoreCase = true))
                        ) {
                            runCatching { uriHandler.openUri(target) }
                        }
                    }
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (banner.imageUrl.startsWith("https://", ignoreCase = true)) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (banner.title.isNotBlank()) {
                    Text(
                        text = banner.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = fgColor,
                    )
                }
                if (banner.subtitle.isNotBlank()) {
                    Text(
                        text = banner.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = fgColor.copy(alpha = 0.88f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

private fun parseHexColor(raw: String, fallback: Color): Color {
    val clean = raw.trim().removePrefix("#")
    val value = clean.toLongOrNull(16) ?: return fallback
    return when (clean.length) {
        6 -> Color(0xFF000000L or value)
        8 -> Color(value)
        else -> fallback
    }
}
