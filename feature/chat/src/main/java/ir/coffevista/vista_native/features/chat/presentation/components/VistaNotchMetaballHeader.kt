package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassHalo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * هدر جمع‌شونده جزئیات گفتگو و پروفایل با انیمیشن متابال قطره مایع به سمت ناچ دوربین بالا
 * و دکمه‌های شناور شیشه مایع (`vistaGlass`).
 */
@Composable
fun VistaNotchMetaballHeader(
    displayName: String,
    subtitle: String,
    isSubtitleHighlighted: Boolean = false,
    avatarUrl: String?,
    scrollOffsetPx: Int,
    glassState: VistaGlassState?,
    onBack: () -> Unit,
    backContentDescription: String = "بازگشت",
    menuContentDescription: String = "گزینه‌ها",
    onMenuClick: () -> Unit,
    menuDropdownContent: @Composable BoxScope.() -> Unit = {},
    verificationBadgeContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Collapse over 145.dp of scroll
    val collapseRangePx = with(density) { 145.dp.toPx() }.coerceAtLeast(1f)
    val collapseProgress = (scrollOffsetPx.toFloat() / collapseRangePx).coerceIn(0f, 1f)

    // Avatar geometry: starts at 92.dp diameter below status bar, shrinks and rises into top-center camera notch
    val baseAvatarSize = 92.dp
    val avatarScale = androidx.compose.ui.util.lerp(1.0f, 0.18f, collapseProgress)
    val currentAvatarRadiusDp = (baseAvatarSize * avatarScale) / 2f

    val startAvatarCenterY = statusBarTop + 54.dp
    // Top camera punch-hole center sits ~13.dp below the physical top edge of the screen
    val endAvatarCenterY = 13.dp
    val currentAvatarCenterY = lerp(startAvatarCenterY, endAvatarCenterY, collapseProgress)

    // Progressive blur & notch blackout as the avatar approaches and merges with the camera notch
    val metaballPhase = ((collapseProgress - 0.10f) / 0.80f).coerceIn(0f, 1f)
    val avatarBlurDp = lerp(0.dp, 13.dp, ((collapseProgress - 0.12f) / 0.60f).coerceIn(0f, 1f))
    val notchBlackoutAlpha = ((collapseProgress - 0.16f) / 0.58f).coerceIn(0f, 1f)
    val avatarVisibilityAlpha = (1f - ((collapseProgress - 0.86f) / 0.12f).coerceIn(0f, 1f))

    // Title & subtitle stay directly below the bottom edge of the rising teardrop avatar,
    // and dock at statusBarTop + 7.dp once the avatar enters the camera notch
    val avatarBottomY = currentAvatarCenterY + currentAvatarRadiusDp
    val currentTitleTopY = maxOf(
        statusBarTop + 7.dp,
        avatarBottomY + lerp(10.dp, 3.dp, collapseProgress),
    )
    val titleScale = androidx.compose.ui.util.lerp(1.0f, 0.84f, collapseProgress)
    val totalHeaderHeight = maxOf(statusBarTop + 56.dp, currentTitleTopY + 52.dp)

    val glassPillTint = if (isDark) Color(0xFF16191E) else Color(0xFFFFFFFF)
    val glassPillAlpha = if (isDark) 0.58f else 0.64f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeaderHeight),
    ) {
        // Telegram-style top frosted halo: covers status bar + floating pills and feathers ~36.dp below
        val haloTint = if (isDark) Color(0xFF0E1115) else Color(0xFFFFFFFF)
        val haloHeight = statusBarTop + 94.dp
        val peakAlpha = 0.94f
        if (glassState != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(haloHeight)
                    .vistaGlassHalo(
                        state = glassState,
                        isDark = isDark,
                        tint = haloTint,
                        blurRadius = 26.dp,
                        startOffsetFraction = 0f,
                        maxAlpha = peakAlpha,
                        fromTop = true,
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(haloHeight)
                    .background(
                        Brush.verticalGradient(
                            0f to haloTint.copy(alpha = peakAlpha),
                            0.22f to haloTint.copy(alpha = peakAlpha * 0.95f),
                            0.44f to haloTint.copy(alpha = peakAlpha * 0.78f),
                            0.62f to haloTint.copy(alpha = peakAlpha * 0.52f),
                            0.76f to haloTint.copy(alpha = peakAlpha * 0.24f),
                            0.89f to haloTint.copy(alpha = peakAlpha * 0.08f),
                            0.97f to haloTint.copy(alpha = peakAlpha * 0.02f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
        }

        // Soft ambient radial glow behind the expanded avatar (smoothly fades to transparent inside header bounds)
        if (collapseProgress < 0.95f) {
            val ambientAlpha = (1f - collapseProgress) * (if (isDark) 0.24f else 0.15f)
            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, with(density) { startAvatarCenterY.toPx() })
                val maxVerticalRadius = (size.height - center.y).coerceAtLeast(1f) * 0.96f
                val radius = minOf(size.width * 0.48f, maxVerticalRadius).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        0.00f to primaryColor.copy(alpha = ambientAlpha),
                        0.32f to primaryColor.copy(alpha = ambientAlpha * 0.62f),
                        0.62f to primaryColor.copy(alpha = ambientAlpha * 0.22f),
                        0.84f to primaryColor.copy(alpha = ambientAlpha * 0.05f),
                        1.00f to Color.Transparent,
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
        }

        // 1. Liquid Teardrop / Metaball Connector + Rising Avatar into Camera Notch
        if (avatarVisibilityAlpha > 0.01f) {
            val avatarRadiusPx = with(density) { currentAvatarRadiusDp.toPx() }
            val avatarCenterYPx = with(density) { currentAvatarCenterY.toPx() }
            val avatarTopYPx = avatarCenterYPx - avatarRadiusPx

            // Draw the black metaball liquid bridge connecting the top camera punch-hole and the avatar circle
            if (metaballPhase > 0.01f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = avatarVisibilityAlpha },
                ) {
                    val cx = size.width / 2f
                    // Top circular punch-hole anchor (bowling-pin head)
                    val r1 = 7.5.dp.toPx() * (0.75f + 0.25f * metaballPhase)
                    val c1 = Offset(cx, 13.dp.toPx())
                    // Rising avatar circle
                    val r2 = avatarRadiusPx
                    val c2 = Offset(cx, avatarCenterYPx)

                    val v = (0.42f + 0.36f * metaballPhase).coerceIn(0.3f, 0.80f)
                    // Connect punch-hole circle -> rising avatar circle (the bowling-pin / teardrop neck)
                    val avatarBridgePath = Path()
                    if (buildMetaballBridgePath(c1, r1, c2, r2, v, avatarBridgePath)) {
                        drawPath(path = avatarBridgePath, color = Color.Black)
                    }
                    // Draw top camera punch-hole circle head
                    drawCircle(
                        color = Color.Black,
                        radius = r1,
                        center = c1,
                    )
                }
            }

            // Rising & shrinking circular avatar with progressive blur and top-down notch blackout gradient
            val cleanAvatar = avatarUrl?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (avatarTopYPx - with(density) { (baseAvatarSize.toPx() - avatarRadiusPx * 2f) / 2f }).roundToInt(),
                        )
                    }
                    .size(baseAvatarSize)
                    .graphicsLayer {
                        scaleX = avatarScale
                        scaleY = avatarScale
                        alpha = avatarVisibilityAlpha
                    }
                    .shadow(
                        elevation = lerp(6.dp, 0.dp, collapseProgress),
                        shape = CircleShape,
                        clip = false,
                    )
                    .clip(CircleShape)
                    .border(
                        width = lerp(1.2.dp, 0.dp, collapseProgress),
                        color = if (isDark) Color.White.copy(alpha = 0.22f * (1f - collapseProgress))
                        else Color.White.copy(alpha = 0.85f * (1f - collapseProgress)),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (avatarBlurDp > 0.5.dp) Modifier.blur(avatarBlurDp) else Modifier,
                        ),
                ) {
                    if (!cleanAvatar.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = cleanAvatar,
                            contentDescription = "تصویر $displayName",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = { AvatarGradientFallback(displayName) },
                            error = { AvatarGradientFallback(displayName) },
                        )
                    } else {
                        AvatarGradientFallback(displayName)
                    }
                }

                // Top-down liquid blackout gradient as the avatar merges into the black camera notch
                if (notchBlackoutAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Black.copy(alpha = (notchBlackoutAlpha * 1.15f).coerceIn(0f, 1f)),
                                    0.48f to Color.Black.copy(alpha = (notchBlackoutAlpha * 0.82f).coerceIn(0f, 1f)),
                                    1.0f to Color.Black.copy(alpha = (notchBlackoutAlpha * 0.45f).coerceIn(0f, 1f)),
                                ),
                            ),
                    )
                }
            }
        }

        // 2. Title + Verification Badge + Subtitle (glides from below avatar into top bar center)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 64.dp)
                .offset {
                    IntOffset(
                        x = 0,
                        y = with(density) { currentTitleTopY.toPx().roundToInt() },
                    )
                }
                .graphicsLayer {
                    scaleX = titleScale
                    scaleY = titleScale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = displayName,
                    modifier = Modifier.weight(1f, fill = false),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                verificationBadgeContent()
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = if (isSubtitleHighlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = if (isSubtitleHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 3. Floating Liquid Glass Back Button (Top-Start)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = statusBarTop + 6.dp, start = 12.dp)
                .size(42.dp)
                .then(
                    if (glassState != null) {
                        Modifier.vistaGlass(
                            state = glassState,
                            shape = CircleShape,
                            tint = glassPillTint,
                            isDark = isDark,
                            blurRadius = VistaGlassTokens.PillBlur,
                            tintAlphaOverride = glassPillAlpha,
                            drawHighlight = true,
                            rimWidth = 0.6.dp,
                            shadowElevation = 4.dp,
                        )
                    } else {
                        Modifier
                            .shadow(3.dp, CircleShape)
                            .background(glassPillTint.copy(alpha = 0.88f), CircleShape)
                    },
                )
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = backContentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(21.dp),
            )
        }

        // 4. Floating Liquid Glass Menu Button (Top-End)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = statusBarTop + 6.dp, end = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .then(
                        if (glassState != null) {
                            Modifier.vistaGlass(
                                state = glassState,
                                shape = CircleShape,
                                tint = glassPillTint,
                                isDark = isDark,
                                blurRadius = VistaGlassTokens.PillBlur,
                                tintAlphaOverride = glassPillAlpha,
                                drawHighlight = true,
                                rimWidth = 0.6.dp,
                                shadowElevation = 4.dp,
                            )
                        } else {
                            Modifier
                                .shadow(3.dp, CircleShape)
                                .background(glassPillTint.copy(alpha = 0.88f), CircleShape)
                        },
                    )
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onMenuClick)
                    .semantics { contentDescription = menuContentDescription },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp),
                )
            }
            menuDropdownContent()
        }
    }
}

@Composable
private fun AvatarGradientFallback(displayName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF6C5CE7),
                        Color(0xFF0984E3),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayName.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * معادله هندسی متابال دودایره‌ای با منحنی‌های بزیه درجه سه برای ایجاد گردنه قطره مایع بین ناچ دوربین و آواتار.
 */
private fun buildMetaballBridgePath(
    c1: Offset,
    radius1: Float,
    c2: Offset,
    radius2: Float,
    v: Float,
    outPath: Path,
): Boolean {
    val halfPi = (PI / 2.0).toFloat()
    val handleSize = 20.0f

    val d = hypot(c1.x - c2.x, c1.y - c2.y)
    val maxDist = radius1 + radius2 * 2.55f
    if (radius1 <= 0f || radius2 <= 0f || d > maxDist || d <= abs(radius1 - radius2)) {
        return false
    }

    val u1: Float
    val u2: Float
    if (d < radius1 + radius2) {
        u1 = acos(((radius1 * radius1 + d * d - radius2 * radius2) / (2f * radius1 * d)).coerceIn(-1f, 1f))
        u2 = acos(((radius2 * radius2 + d * d - radius1 * radius1) / (2f * radius2 * d)).coerceIn(-1f, 1f))
    } else {
        u1 = 0f
        u2 = 0f
    }

    val angleBetweenCenters = atan2(c2.y - c1.y, c2.x - c1.x)
    val maxSpread = acos(((radius1 - radius2) / d).coerceIn(-1f, 1f))

    val angle1 = angleBetweenCenters + u1 + (maxSpread - u1) * v
    val angle2 = angleBetweenCenters - u1 - (maxSpread - u1) * v
    val angle3 = angleBetweenCenters + PI.toFloat() - u2 - (PI.toFloat() - u2 - maxSpread) * v
    val angle4 = angleBetweenCenters - PI.toFloat() + u2 + (PI.toFloat() - u2 - maxSpread) * v

    val p1 = pointOnCircle(c1, angle1, radius1)
    val p2 = pointOnCircle(c1, angle2, radius1)
    val p3 = pointOnCircle(c2, angle3, radius2)
    val p4 = pointOnCircle(c2, angle4, radius2)

    val totalRadius = radius1 + radius2
    val d2Base = minOf(v * handleSize, hypot(p1.x - p3.x, p1.y - p3.y) / totalRadius)
    val d2 = d2Base * minOf(1f, (d * 2f) / totalRadius)

    val r1 = radius1 * d2
    val r2 = radius2 * d2

    val h1 = pointOnCircle(p1, angle1 - halfPi, r1)
    val h2 = pointOnCircle(p2, angle2 + halfPi, r1)
    val h3 = pointOnCircle(p3, angle3 + halfPi, r2)
    val h4 = pointOnCircle(p4, angle4 - halfPi, r2)

    val a1 = pointOnCircle(p3, angle3 + halfPi, r2 * 0.55f)
    val a2 = pointOnCircle(p4, angle4 - halfPi, r2 * 0.55f)

    outPath.reset()
    outPath.moveTo(p1.x, p1.y)
    outPath.cubicTo(h1.x, h1.y, h3.x, h3.y, p3.x, p3.y)
    outPath.cubicTo(a1.x, a1.y, a2.x, a2.y, p4.x, p4.y)
    outPath.cubicTo(h4.x, h4.y, h2.x, h2.y, p2.x, p2.y)
    outPath.close()
    return true
}

private fun pointOnCircle(center: Offset, angle: Float, radius: Float): Offset =
    Offset(
        x = center.x + radius * cos(angle),
        y = center.y + radius * sin(angle),
    )

/**
 * دکمه عملیات ظریف و شیشه‌ای صفحه جزئیات گفتگو و پروفایل (`58.dp` ارتفاع، `RoundedCornerShape(16.dp)`).
 */
@Composable
fun VistaProfileGlassActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    glassState: VistaGlassState? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy,
        ),
        label = "profileActionPressScale",
    )
    val foreground = when {
        destructive -> MaterialTheme.colorScheme.error
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    val cardShape = RoundedCornerShape(16.dp)
    val cardTint = if (isDark) Color(0xFF16191E) else Color(0xFFFFFFFF)
    Column(
        modifier = modifier
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (glassState != null) {
                    Modifier.vistaGlass(
                        state = glassState,
                        shape = cardShape,
                        tint = cardTint,
                        isDark = isDark,
                        blurRadius = VistaGlassTokens.PillBlur,
                        tintAlphaOverride = if (isDark) 0.62f else 0.72f,
                        drawHighlight = true,
                        rimWidth = 0.55.dp,
                        shadowElevation = 2.dp,
                    )
                } else {
                    Modifier
                        .shadow(if (isDark) 1.dp else 2.dp, cardShape)
                        .background(cardTint, cardShape)
                        .border(
                            0.55.dp,
                            if (isDark) Color(0x22FFFFFF) else Color(0x12000000),
                            cardShape,
                        )
                },
            )
            .clip(cardShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = foreground,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = foreground,
            fontSize = 11.5.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * نوار تب کپسولی شناور شیشه مایع برای دسته‌بندی رسانه‌ها و محتوای اشتراکی در صفحه جزئیات چت.
 */
@Composable
fun VistaSegmentedGlassTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glassState: VistaGlassState? = null,
) {
    val isDark = isSystemInDarkTheme()
    val containerShape = RoundedCornerShape(22.dp)
    val itemShape = RoundedCornerShape(18.dp)
    val containerTint = if (isDark) Color(0xFF16191E) else Color(0xFFFFFFFF)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(42.dp)
                .then(
                    if (glassState != null) {
                        Modifier.vistaGlass(
                            state = glassState,
                            shape = containerShape,
                            tint = containerTint,
                            isDark = isDark,
                            blurRadius = VistaGlassTokens.PillBlur,
                            tintAlphaOverride = if (isDark) 0.62f else 0.72f,
                            drawHighlight = true,
                            rimWidth = 0.55.dp,
                            shadowElevation = 2.dp,
                        )
                    } else {
                        Modifier
                            .shadow(if (isDark) 1.dp else 2.dp, containerShape)
                            .background(containerTint, containerShape)
                            .border(
                                0.55.dp,
                                if (isDark) Color(0x22FFFFFF) else Color(0x12000000),
                                containerShape,
                            )
                    },
                )
                .clip(containerShape)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val activeColor = MaterialTheme.colorScheme.primary
                val pillBg by animateColorAsState(
                    targetValue = if (selected) {
                        activeColor.copy(alpha = if (isDark) 0.22f else 0.13f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(180),
                    label = "seg_tab_bg_$index",
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) {
                        activeColor
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(180),
                    label = "seg_tab_text_$index",
                )
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .widthIn(min = 64.dp)
                        .clip(itemShape)
                        .background(pillBg, itemShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = { onTabSelected(index) },
                        )
                        .padding(horizontal = 14.dp)
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                            contentDescription = label
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
