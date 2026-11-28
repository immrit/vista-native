package ir.coffevista.vista_native.features.feed.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.component.rememberVistaHaptics
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassBlurSupported
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.SocialCommentsSummaryDto
import ir.coffevista.vista_native.features.feed.data.SocialPostAnalysisDto
import kotlinx.coroutines.delay

data class PostAiUiState(
    val post: FeedPost? = null,
    val mode: String = "explain",
    val isLoading: Boolean = false,
    val result: SocialPostAnalysisDto? = null,
    val error: String? = null,
)

data class CommentsSummaryUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val result: SocialCommentsSummaryDto? = null,
    val error: String? = null,
)

private val VistaAiViolet = Color(0xFF7C5CFF)
private val VistaAiCyan = VistaBrandColors.ViuBlue

@Composable
internal fun VistaAiSparkleIcon(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostAiAnalysisBottomSheet(
    state: PostAiUiState,
    onSelectMode: (String) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val post = state.post ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val blurSupported = rememberVistaGlassBlurSupported()
    val haptics = rememberVistaHaptics()
    val clipboard = LocalClipboardManager.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var copied by remember(state.result) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
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
                                .background(Brush.linearGradient(listOf(VistaAiViolet, VistaAiCyan))),
                            contentAlignment = Alignment.Center,
                        ) {
                            VistaAiSparkleIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تحلیل هوشمند ویستا",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                    ),
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VistaAiViolet.copy(alpha = 0.14f),
                                ) {
                                    Text(
                                        text = "Vista-1",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VistaAiViolet,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(
                                text = "بررسی زمینه، لحن و ادعای پست",
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
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                val snippetText = post.content?.trim().orEmpty()
                if (snippetText.isNotBlank() || !post.authorUsername.isNullOrBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardSurface,
                        border = BorderStroke(1.dp, subtleBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(VistaAiViolet, VistaAiCyan))),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                post.authorUsername?.takeIf { it.isNotBlank() }?.let { uname ->
                                    Text(
                                        text = "\u2066@$uname\u2069",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VistaAiCyan,
                                    )
                                }
                                if (snippetText.isNotBlank()) {
                                    Text(
                                        text = snippetText,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 18.sp,
                                    )
                                }
                            }
                        }
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PostAiSegmentTab(
                            label = "توضیح عمیق پست",
                            selected = state.mode == "explain",
                            accent = VistaAiViolet,
                            isDark = isDark,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.tap()
                                onSelectMode("explain")
                            },
                        )
                        PostAiSegmentTab(
                            label = "راستی‌آزمایی ادعا",
                            selected = state.mode == "fact_check",
                            accent = VistaAiCyan,
                            isDark = isDark,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.tap()
                                onSelectMode("fact_check")
                            },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                when {
                    state.isLoading -> {
                        PostAiShimmerCard(
                            cardSurface = cardSurface,
                            subtleBorder = subtleBorder,
                            statusLabel = if (state.mode == "fact_check") {
                                "در حال راستی‌آزمایی ادعا و شواهد عمومی…"
                            } else {
                                "در حال رمزگشایی و تحلیل متن پست…"
                            },
                        )
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
                                    text = state.error,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        haptics.tap()
                                        onRetry()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VistaAiViolet),
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
                                        VistaAiViolet.copy(alpha = 0.45f),
                                        VistaAiCyan.copy(alpha = 0.35f),
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
                                    Text(
                                        text = res.headline.ifBlank { "تحلیل هوشمند پست" },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.5.sp,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    VerdictBadge(res.verdict)
                                }

                                if (res.summary.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = res.summary,
                                        fontSize = 13.5.sp,
                                        lineHeight = 23.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                if (res.keyPoints.isNotEmpty()) {
                                    Spacer(Modifier.height(14.dp))
                                    HorizontalDivider(color = subtleBorder, thickness = 0.8.dp)
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "نکات کلیدی",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    res.keyPoints.forEachIndexed { idx, pt ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 3.dp)
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(VistaAiViolet.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VistaAiViolet,
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = pt,
                                                fontSize = 12.5.sp,
                                                lineHeight = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                }

                                if (res.contextNote.isNotBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = VistaAiCyan.copy(alpha = if (isDark) 0.12f else 0.08f),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            text = res.contextNote,
                                            fontSize = 12.sp,
                                            lineHeight = 19.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = res.providerUsed?.takeIf { it.isNotBlank() } ?: "Vista-1 Social",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Surface(
                                        onClick = {
                                            haptics.confirm()
                                            val copyText = buildString {
                                                append(res.headline)
                                                if (res.summary.isNotBlank()) {
                                                    append("\n")
                                                    append(res.summary)
                                                }
                                            }
                                            clipboard.setText(AnnotatedString(copyText))
                                            copied = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (copied) Color(0xFF10B981).copy(alpha = 0.16f) else subtleBorder,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = if (copied) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp),
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = if (copied) "کپی شد" else "کپی تحلیل",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (copied) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        text = "تحلیل فقط براساس محتوای عمومی پست و نظرات عمومی انجام شده است.",
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
private fun PostAiSegmentTab(
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
                .padding(vertical = 9.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PostAiShimmerCard(
    cardSurface: Color,
    subtleBorder: Color,
    statusLabel: String,
) {
    val transition = rememberInfiniteTransition(label = "postAiShimmer")
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
            VistaAiViolet.copy(alpha = 0.10f),
            VistaAiCyan.copy(alpha = 0.24f),
            VistaAiViolet.copy(alpha = 0.10f),
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
                VistaAiSparkleIcon(
                    tint = VistaAiViolet,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer(scaleX = pulse, scaleY = pulse),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusLabel,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
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
                    .fillMaxWidth(0.86f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush),
            )
            Spacer(Modifier.height(7.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush),
            )
        }
    }
}

@Composable
private fun VerdictBadge(verdict: String) {
    val (label, tint) = when (verdict.lowercase()) {
        "verified" -> "تاییدشده" to Color(0xFF10B981)
        "misleading" -> "نیازمند دقت" to Color(0xFFF59E0B)
        "context_needed", "needs_context" -> "نیازمند زمینه" to Color(0xFFF59E0B)
        "opinion" -> "دیدگاه شخصی" to VistaAiCyan
        "unverified" -> "نیازمند منبع" to Color(0xFFEC4899)
        else -> "تحلیل" to VistaAiViolet
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.16f),
        border = BorderStroke(0.8.dp, tint.copy(alpha = 0.35f)),
    ) {
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun CommentsSummaryBanner(
    state: CommentsSummaryUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardSurface = if (isDark) Color(0xFF1A1D27) else Color(0xFFF3F6FB)

    AnimatedVisibility(
        visible = state.isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardSurface,
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        VistaAiViolet.copy(alpha = 0.42f),
                        VistaAiCyan.copy(alpha = 0.35f),
                    ),
                ),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VistaAiSparkleIcon(tint = VistaAiViolet, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "جمع‌بندی نظرات",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                        )
                        state.result?.overallMood?.takeIf { it.isNotBlank() }?.let { mood ->
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = VistaAiViolet.copy(alpha = 0.14f),
                            ) {
                                Text(
                                    text = mood,
                                    fontSize = 10.5.sp,
                                    color = VistaAiViolet,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن خلاصه",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(onClick = onDismiss),
                    )
                }

                when {
                    state.isLoading -> {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "در حال جمع‌بندی دیدگاه‌های عمومی…",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.error != null -> {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            Text(state.error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            Text(
                                text = "تلاش مجدد",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VistaAiViolet,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clickable(onClick = onRetry),
                            )
                        }
                    }
                    state.result != null -> {
                        if (state.result.summary.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.result.summary,
                                fontSize = 12.5.sp,
                                lineHeight = 20.sp,
                            )
                        }
                        if (state.result.viewpoints.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            state.result.viewpoints.forEach { vp ->
                                Text(
                                    text = "• $vp",
                                    fontSize = 11.5.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 1.5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
