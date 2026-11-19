package ir.coffevista.vista_native.features.feed.ui

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.R as DesignSystemR
import ir.coffevista.vista_native.core.designsystem.navigation.LocalVistaInternalLinkNavigator
import ir.coffevista.vista_native.core.network.RemoteBannerSlot
import ir.coffevista.vista_native.core.network.RemoteBrandOrnament
import kotlinx.coroutines.delay

@Composable
internal fun VistaBrandedFeedLogo(
    dark: Boolean,
    @DrawableRes defaultLogoRes: Int,
    ornament: RemoteBrandOrnament,
    modifier: Modifier = Modifier,
) {
    var showModal by remember { mutableStateOf(false) }
    var bouncePulse by remember { mutableIntStateOf(0) }
    var pressedBounce by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val customLogoUrl = if (dark) ornament.customLogoDarkUrl else ornament.customLogoLightUrl
    val isInteractive = ornament.enabled && ornament.tapAction != "none"

    val seasonalLogoRes = if (ornament.enabled) {
        when (ornament.preset) {
            "santa_hat" -> if (dark) {
                DesignSystemR.drawable.vista_logo_christmas_dark
            } else {
                DesignSystemR.drawable.vista_logo_christmas_light
            }
            "nowruz_sprout" -> DesignSystemR.drawable.vista_logo_nowruz
            "yalda_pomegranate" -> DesignSystemR.drawable.vista_logo_yalda
            "sparkle_crown" -> DesignSystemR.drawable.vista_logo_festival
            else -> null
        }
    } else {
        null
    }

    val effectiveLogoRes = seasonalLogoRes ?: defaultLogoRes
    val hasLivingMotion = ornament.enabled &&
        (seasonalLogoRes != null || customLogoUrl.startsWith("https://", ignoreCase = true) || ornament.preset == "custom_image")
    val animationsDisabled = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }

    val logoDp = if (seasonalLogoRes != null) 42.dp else 35.dp

    var floatDp = 0f
    var tiltDeg = 0f
    var breathScale = 1f
    if (hasLivingMotion && !animationsDisabled) {
        val infinite = rememberInfiniteTransition(label = "brandLogoLivingMotion")
        val animatedFloat by infinite.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "brandLogoFloatY",
        )
        val animatedTilt by infinite.animateFloat(
            initialValue = -2.5f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "brandLogoTilt",
        )
        val animatedScale by infinite.animateFloat(
            initialValue = 0.985f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "brandLogoScaleBreath",
        )
        floatDp = animatedFloat
        tiltDeg = animatedTilt
        breathScale = animatedScale
    }

    LaunchedEffect(bouncePulse) {
        if (bouncePulse > 0) {
            pressedBounce = true
            delay(180)
            pressedBounce = false
        }
    }

    val tapBounceScale by animateFloatAsState(
        targetValue = if (pressedBounce) 1.16f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "brandLogoTapBounce",
    )

    val seasonalTransformModifier = if (hasLivingMotion) {
        val floatPx = with(density) { floatDp.dp.toPx() }
        Modifier
            .offset(x = ornament.offsetX.dp, y = ornament.offsetY.dp)
            .graphicsLayer {
                translationY = floatPx
                scaleX = ornament.scale * breathScale * tapBounceScale
                scaleY = ornament.scale * breathScale * tapBounceScale
                rotationZ = ornament.rotationDeg.toFloat() + tiltDeg
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(
                if (isInteractive || hasLivingMotion) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        bouncePulse += 1
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
                placeholder = painterResource(effectiveLogoRes),
                error = painterResource(effectiveLogoRes),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(logoDp)
                    .then(seasonalTransformModifier),
            )
        } else {
            Image(
                painter = painterResource(effectiveLogoRes),
                contentDescription = "Vista",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(logoDp)
                    .then(seasonalTransformModifier),
            )
        }

        if (ornament.enabled && ornament.preset == "custom_image" && ornament.customImageUrl.startsWith("https://", ignoreCase = true)) {
            AsyncImage(
                model = ornament.customImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = ornament.offsetX.dp, y = ornament.offsetY.dp)
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = ornament.scale * breathScale * tapBounceScale
                        scaleY = ornament.scale * breathScale * tapBounceScale
                        rotationZ = ornament.rotationDeg.toFloat() + tiltDeg
                    },
            )
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
