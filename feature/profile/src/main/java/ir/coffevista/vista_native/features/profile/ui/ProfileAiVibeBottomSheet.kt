package ir.coffevista.vista_native.features.profile.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.component.rememberVistaHaptics
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassBlurSupported
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.profile.data.SocialProfileAnalysisDto
import kotlinx.coroutines.delay

data class ProfileAiUiState(
    val isVisible: Boolean = false,
    val mode: String = "insight",
    val isLoading: Boolean = false,
    val result: SocialProfileAnalysisDto? = null,
    val error: String? = null,
)

private val ProfileAiViolet = Color(0xFF7C5CFF)
private val ProfileAiCyan = VistaBrandColors.ViuBlue
private val ProfileAiPink = Color(0xFFEC4899)

@Composable
internal fun ProfileAiSparkleIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.45f
        val cy = h * 0.52f
        val r = minOf(w, h) * 0.42f
        val inner = r * 0.28f
        val star = Path().apply {
            moveTo(cx, cy - r)
            quadraticTo(cx + inner * 0.35f, cy - inner * 0.35f, cx + r, cy)
            quadraticTo(cx + inner * 0.35f, cy + inner * 0.35f, cx, cy + r)
            quadraticTo(cx - inner * 0.35f, cy + inner * 0.35f, cx - r, cy)
            quadraticTo(cx - inner * 0.35f, cy - inner * 0.35f, cx, cy - r)
            close()
        }
        drawPath(star, color = tint)

        val sCx = w * 0.80f
        val sCy = h * 0.22f
        val sR = minOf(w, h) * 0.16f
        val sInner = sR * 0.30f
        val smallStar = Path().apply {
            moveTo(sCx, sCy - sR)
            quadraticTo(sCx + sInner * 0.3f, sCy - sInner * 0.3f, sCx + sR, sCy)
            quadraticTo(sCx + sInner * 0.3f, sCy + sInner * 0.3f, sCx, sCy + sR)
            quadraticTo(sCx - sInner * 0.3f, sCy + sInner * 0.3f, sCx - sR, sCy)
            quadraticTo(sCx - sInner * 0.3f, sCy - sInner * 0.3f, sCx, sCy - sR)
            close()
        }
        drawPath(smallStar, color = tint.copy(alpha = 0.85f))
    }
}

@Composable
internal fun VistaAiProfileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "هوش مصنوعی",
) {
    val haptics = rememberVistaHaptics()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        shape = RoundedCornerShape(10.dp),
        color = ProfileAiViolet.copy(alpha = if (isDark) 0.16f else 0.10f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    ProfileAiViolet.copy(alpha = 0.55f),
                    ProfileAiCyan.copy(alpha = 0.50f),
                ),
            ),
        ),
        modifier = modifier.height(36.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            ProfileAiSparkleIcon(
                tint = if (isDark) Color(0xFFC4B5FD) else ProfileAiViolet,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFE0D7FF) else ProfileAiViolet,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAiVibeBottomSheet(
    username: String?,
    isOwnProfile: Boolean,
    state: ProfileAiUiState,
    onSelectMode: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.isVisible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val blurSupported = rememberVistaGlassBlurSupported()
    val haptics = rememberVistaHaptics()
    val clipboard = LocalClipboardManager.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var copiedIcebreaker by remember(state.result) { mutableStateOf(false) }
    LaunchedEffect(copiedIcebreaker) {
        if (copiedIcebreaker) {
            delay(1800)
            copiedIcebreaker = false
        }
    }

    val sheetSurface = if (isDark) {
        Color(0xFF14161D).copy(alpha = if (blurSupported) 0.94f else 0.99f)
    } else {
        Color(0xFFFDFEFE).copy(alpha = if (blurSupported) 0.95f else 0.99f)
    }
    val cardSurface = if (isDark) Color(0xFF1C202B) else Color(0xFFF4F6FA)
    val subtleBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)),
            )
        },
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 6.dp, bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(ProfileAiViolet, ProfileAiPink))),
                            contentAlignment = Alignment.Center,
                        ) {
                            ProfileAiSparkleIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (!username.isNullOrBlank()) "تحلیل پروفایل @$username" else "تحلیل هوشمند پروفایل",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                ),
                            )
                            Text(
                                text = "Vista-1 Social · تحلیل محتوای عمومی",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            haptics.tap()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(subtleBorder),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardSurface,
                    border = BorderStroke(1.dp, subtleBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        ProfileModeSegmentTab(
                            label = "پرسونا",
                            selected = state.mode == "insight",
                            accent = ProfileAiViolet,
                            isDark = isDark,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.tap()
                                onSelectMode("insight")
                            },
                        )
                        ProfileModeSegmentTab(
                            label = "شوخی / Roast",
                            selected = state.mode == "roast",
                            accent = ProfileAiPink,
                            isDark = isDark,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.tap()
                                onSelectMode("roast")
                            },
                        )
                        if (!isOwnProfile) {
                            ProfileModeSegmentTab(
                                label = "تفاهم",
                                selected = state.mode == "compatibility",
                                accent = ProfileAiCyan,
                                isDark = isDark,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.tap()
                                    onSelectMode("compatibility")
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                when {
                    state.isLoading -> {
                        ProfileAiShimmerCard(cardSurface = cardSurface, subtleBorder = subtleBorder)
                    }

                    state.error != null -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDark) 0.25f else 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    state.error,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        haptics.tap()
                                        onRetry()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ProfileAiViolet),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("تلاش مجدد", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    state.result != null -> {
                        val res = state.result
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = cardSurface,
                            border = BorderStroke(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(
                                        ProfileAiViolet.copy(alpha = 0.45f),
                                        ProfileAiCyan.copy(alpha = 0.35f),
                                    ),
                                ),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = res.title.ifBlank { "تحلیل پروفایل" },
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            lineHeight = 22.sp,
                                        )
                                    }
                                    res.compatibilityScore?.let { score ->
                                        Spacer(Modifier.width(12.dp))
                                        CompatibilityGaugeBadge(score = score)
                                    }
                                }

                                if (res.summary.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = res.summary,
                                        fontSize = 13.5.sp,
                                        lineHeight = 23.sp,
                                    )
                                }

                                if (res.highlights.isNotEmpty()) {
                                    Spacer(Modifier.height(14.dp))
                                    HorizontalDivider(color = subtleBorder, thickness = 0.8.dp)
                                    Spacer(Modifier.height(10.dp))
                                    res.highlights.forEach { hl ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 3.5.dp),
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            Text("✦", color = ProfileAiViolet, fontSize = 13.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = hl,
                                                fontSize = 12.5.sp,
                                                lineHeight = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                                            )
                                        }
                                    }
                                }

                                if (res.icebreaker.isNotBlank()) {
                                    Spacer(Modifier.height(14.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = ProfileAiViolet.copy(alpha = if (isDark) 0.14f else 0.09f),
                                        border = BorderStroke(1.dp, ProfileAiViolet.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                haptics.confirm()
                                                clipboard.setText(AnnotatedString(res.icebreaker))
                                                copiedIcebreaker = true
                                            },
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = "پیشنهاد شروع گفتگو",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ProfileAiViolet,
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (copiedIcebreaker) Icons.Default.Check else Icons.Default.ContentCopy,
                                                        contentDescription = null,
                                                        tint = if (copiedIcebreaker) Color(0xFF10B981) else ProfileAiViolet,
                                                        modifier = Modifier.size(12.dp),
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = if (copiedIcebreaker) "کپی شد" else "کپی",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (copiedIcebreaker) Color(0xFF10B981) else ProfileAiViolet,
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(5.dp))
                                            Text(
                                                text = res.icebreaker,
                                                fontSize = 12.5.sp,
                                                lineHeight = 20.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "فقط بر اساس بیو و پست‌های عمومی — چت‌ها و ارتباطات خصوصی هرگز بررسی نمی‌شوند.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompatibilityGaugeBadge(score: Int) {
    val clamped = score.coerceIn(0, 100)
    val animatedSweep by animateFloatAsState(
        targetValue = clamped / 100f * 360f,
        animationSpec = tween(durationMillis = 850),
        label = "compatSweep",
    )
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            val strokeWidth = 5.dp.toPx()
            drawArc(
                color = ProfileAiCyan.copy(alpha = 0.18f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(ProfileAiViolet, ProfileAiCyan, ProfileAiViolet)),
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$clamped٪",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ProfileAiCyan,
            )
            Text(
                text = "تفاهم",
                fontSize = 8.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileModeSegmentTab(
    label: String,
    selected: Boolean,
    accent: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val activeBg = if (isDark) Color(0xFF282D3B) else Color.White
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) activeBg else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, accent.copy(alpha = 0.45f)) else null,
        shadowElevation = if (selected && !isDark) 2.dp else 0.dp,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProfileAiShimmerCard(
    cardSurface: Color,
    subtleBorder: Color,
) {
    val transition = rememberInfiniteTransition(label = "profAiShimmer")
    val shift by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shift",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            ProfileAiViolet.copy(alpha = 0.10f),
            ProfileAiPink.copy(alpha = 0.20f),
            ProfileAiCyan.copy(alpha = 0.12f),
        ),
        start = Offset(shift * 600f - 200f, 0f),
        end = Offset(shift * 600f + 200f, 120f),
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardSurface,
        border = BorderStroke(1.dp, subtleBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAiSparkleIcon(
                    tint = ProfileAiViolet,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer(scaleX = pulse, scaleY = pulse),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "در حال بررسی بیو و پست‌های عمومی…",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush),
            )
            Spacer(Modifier.height(7.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush),
            )
        }
    }
}
