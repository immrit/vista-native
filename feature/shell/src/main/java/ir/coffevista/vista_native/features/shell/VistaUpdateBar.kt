package ir.coffevista.vista_native.features.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val TelegramUpdateBlue = Color(0xFF229ED9)
private val TelegramUpdateReadyBlue = Color(0xFF1D93CC)
private val TelegramUpdateErrorRed = Color(0xFFD93848)

@Composable
fun VistaUpdateBar(
    state: ShellUpdateBarState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor by animateColorAsState(
        targetValue = when (state.stage) {
            ShellUpdateStage.Error -> TelegramUpdateErrorRed
            ShellUpdateStage.ReadyToInstall -> TelegramUpdateReadyBlue
            else -> TelegramUpdateBlue
        },
        animationSpec = tween(durationMillis = 250),
        label = "update-bar-color",
    )

    val labelText = when (state.stage) {
        ShellUpdateStage.Available -> "به‌روزرسانی ویستا"
        ShellUpdateStage.Downloading -> {
            val pct = (state.progress * 100f).toInt().coerceIn(0, 100)
            if (state.totalBytes > 0L) "در حال دریافت... $pct%" else "در حال دریافت..."
        }
        ShellUpdateStage.Verifying -> "در حال بررسی فایل..."
        ShellUpdateStage.ReadyToInstall -> "نصب نسخه جدید"
        ShellUpdateStage.Error -> "تلاش مجدد برای آپدیت"
    }

    val trailingText = remember(
        state.stage,
        state.fileSizeBytes,
        state.downloadedBytes,
        state.totalBytes,
        state.versionName,
    ) {
        when (state.stage) {
            ShellUpdateStage.Downloading -> {
                when {
                    state.totalBytes > 0L ->
                        "${formatMb(state.downloadedBytes)} / ${formatMb(state.totalBytes)}"
                    state.downloadedBytes > 0L -> formatMb(state.downloadedBytes)
                    else -> ""
                }
            }
            ShellUpdateStage.ReadyToInstall -> "v${state.versionName}"
            else -> {
                if (state.fileSizeBytes > 0L) formatMb(state.fileSizeBytes)
                else "v${state.versionName}"
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vista-persistent-update-bar"),
        color = barColor,
        shadowElevation = 10.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = Color.White.copy(alpha = 0.25f)),
                    onClick = onClick,
                )
                .semantics {
                    contentDescription = "$labelText $trailingText"
                    role = Role.Button
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(48.dp)
                .padding(horizontal = 16.dp),
        ) {
            // Centered circular badge + bold white action label (matching Telegram's UpdateLayout)
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                UpdateStageBadgeIcon(
                    stage = state.stage,
                    progress = state.progress,
                    accentColor = barColor,
                )
                AnimatedContent(
                    targetState = labelText,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith
                            fadeOut(animationSpec = tween(150))
                    },
                    label = "update-bar-label",
                ) { text ->
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Trailing size / version indicator in LTR (matching "77.9 MB" on the right edge)
            if (trailingText.isNotBlank()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = trailingText,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateStageBadgeIcon(
    stage: ShellUpdateStage,
    progress: Float,
    accentColor: Color,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "update-icon-motion")
    val arrowBounce by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "update-arrow-bounce",
    )
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "update-verify-spin",
    )
    val readyScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "update-ready-pulse",
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 160),
        label = "update-download-progress",
    )

    Box(
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                if (stage == ShellUpdateStage.ReadyToInstall) {
                    scaleX = readyScale
                    scaleY = readyScale
                }
            }
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            when (stage) {
                ShellUpdateStage.Available -> {
                    val strokeW = 2.1.dp.toPx()
                    val yOffset = arrowBounce.dp.toPx() * 0.55f
                    drawLine(
                        color = accentColor,
                        start = Offset(cx, h * 0.22f + yOffset),
                        end = Offset(cx, h * 0.72f + yOffset),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round,
                    )
                    val head = Path().apply {
                        moveTo(cx - w * 0.22f, h * 0.52f + yOffset)
                        lineTo(cx, h * 0.75f + yOffset)
                        lineTo(cx + w * 0.22f, h * 0.52f + yOffset)
                    }
                    drawPath(
                        path = head,
                        color = accentColor,
                        style = Stroke(
                            width = strokeW,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                ShellUpdateStage.Downloading -> {
                    val ringStroke = 2.3.dp.toPx()
                    val pad = ringStroke / 2f + 0.5.dp.toPx()
                    val arcSize = Size(w - pad * 2f, h - pad * 2f)
                    drawArc(
                        color = accentColor.copy(alpha = 0.20f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = ringStroke),
                    )
                    val sweep = if (animatedProgress > 0.02f) animatedProgress * 360f else 65f
                    val start = if (animatedProgress > 0.02f) -90f else spinAngle
                    drawArc(
                        color = accentColor,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = arcSize,
                        style = Stroke(width = ringStroke, cap = StrokeCap.Round),
                    )
                    // Small cancel square in center
                    val sq = w * 0.24f
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(cx - sq / 2f, cy - sq / 2f),
                        size = Size(sq, sq),
                    )
                }

                ShellUpdateStage.Verifying -> {
                    val ringStroke = 2.3.dp.toPx()
                    val pad = ringStroke / 2f + 0.5.dp.toPx()
                    drawArc(
                        color = accentColor,
                        startAngle = spinAngle,
                        sweepAngle = 250f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = Size(w - pad * 2f, h - pad * 2f),
                        style = Stroke(width = ringStroke, cap = StrokeCap.Round),
                    )
                }

                ShellUpdateStage.ReadyToInstall -> {
                    val strokeW = 2.3.dp.toPx()
                    val check = Path().apply {
                        moveTo(w * 0.24f, h * 0.52f)
                        lineTo(w * 0.43f, h * 0.71f)
                        lineTo(w * 0.77f, h * 0.31f)
                    }
                    drawPath(
                        path = check,
                        color = accentColor,
                        style = Stroke(
                            width = strokeW,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                ShellUpdateStage.Error -> {
                    val strokeW = 2.3.dp.toPx()
                    drawLine(
                        color = accentColor,
                        start = Offset(cx, h * 0.24f),
                        end = Offset(cx, h * 0.56f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = accentColor,
                        radius = 1.4.dp.toPx(),
                        center = Offset(cx, h * 0.74f),
                    )
                }
            }
        }
    }
}

private fun formatMb(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}
