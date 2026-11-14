package ir.coffevista.vista_native.core.designsystem.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Palette matching Instagram & Threads modern dark share sheets.
 */
object VistaShareSheetTokens {
    val SheetBackground = Color(0xFF16171A)
    val CardBackground = Color(0xFF212328)
    val CardBorder = Color(0xFF2E3138)
    val CircleBackground = Color(0xFF26282E)
    val CircleBorder = Color(0xFF383B44)
    val DividerColor = Color(0xFF2A2C32)
    val HandleColor = Color(0xFF52555E)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFA0A4B0)
}

data class VistaShareRailAction(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaShareModalSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VistaShareSheetTokens.SheetBackground,
        scrimColor = Color.Black.copy(alpha = 0.58f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 14.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(VistaShareSheetTokens.HandleColor),
                )

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = VistaShareSheetTokens.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )

                Spacer(Modifier.height(10.dp))

                content()
            }
        }
    }
}

@Composable
fun VistaShareSheetActionRowTile(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    leadingIcon: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = VistaShareSheetTokens.CardBackground,
        border = BorderStroke(1.dp, VistaShareSheetTokens.CardBorder),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(VistaShareSheetTokens.CircleBackground),
                contentAlignment = Alignment.Center,
            ) {
                leadingIcon()
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VistaShareSheetTokens.TextPrimary,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = VistaShareSheetTokens.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun VistaShareActionRail(
    actions: List<VistaShareRailAction>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = VistaShareSheetTokens.DividerColor,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(actions, key = { it.id }) { action ->
                VistaShareRailButton(action = action)
            }
        }
    }
}

@Composable
private fun VistaShareRailButton(action: VistaShareRailAction) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable(
                enabled = action.enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = action.onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable(enabled = action.enabled, onClick = action.onClick),
            contentAlignment = Alignment.Center,
        ) {
            action.icon()
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = action.label,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (action.enabled) VistaShareSheetTokens.TextPrimary else VistaShareSheetTokens.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Dashed Story circle with "+" matching Instagram's "Add to story" glyph.
 */
@Composable
fun VistaAddToStoryCircleIcon(
    gradientAccent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val brush = if (gradientAccent) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF58529),
                Color(0xFFDD2A7B),
                Color(0xFF8134AF),
                Color(0xFF515BD4),
            ),
        )
    } else {
        Brush.linearGradient(listOf(Color.White, Color(0xFFD6DBE8)))
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VistaShareSheetTokens.CircleBackground, CircleShape)
            .border(1.dp, VistaShareSheetTokens.CircleBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val strokePx = 2.1.dp.toPx()
            val ringRadius = size.minDimension * 0.45f
            val center = Offset(size.width / 2f, size.height / 2f)
            // Half-dashed / half-solid Instagram/Threads story ring
            drawArc(
                brush = brush,
                startAngle = -65f,
                sweepAngle = 210f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2f, ringRadius * 2f),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            drawArc(
                brush = brush,
                startAngle = 160f,
                sweepAngle = 122f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2f, ringRadius * 2f),
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f),
                ),
            )
            val plusArm = size.minDimension * 0.18f
            drawLine(
                brush = brush,
                start = Offset(center.x - plusArm, center.y),
                end = Offset(center.x + plusArm, center.y),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
            drawLine(
                brush = brush,
                start = Offset(center.x, center.y - plusArm),
                end = Offset(center.x, center.y + plusArm),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun VistaCopyLinkCircleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VistaShareSheetTokens.CircleBackground, CircleShape)
            .border(1.dp, VistaShareSheetTokens.CircleBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val strokePx = 2.1.dp.toPx()
            val w = size.width
            val h = size.height
            rotate(degrees = -45f, pivot = Offset(w / 2f, h / 2f)) {
                val capW = w * 0.42f
                val capH = h * 0.28f
                val corner = CornerRadius(capH / 2f, capH / 2f)
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.10f, (h - capH) / 2f),
                    size = Size(capW, capH),
                    cornerRadius = corner,
                    style = Stroke(width = strokePx),
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.48f, (h - capH) / 2f),
                    size = Size(capW, capH),
                    cornerRadius = corner,
                    style = Stroke(width = strokePx),
                )
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.38f, h / 2f),
                    end = Offset(w * 0.62f, h / 2f),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
fun VistaWhatsAppCircleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF25D366), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.1.dp.toPx()
            // Clean speech bubble outline + bottom-left tail
            drawArc(
                color = Color.White,
                startAngle = 145f,
                sweepAngle = 320f,
                useCenter = false,
                topLeft = Offset(w * 0.12f, h * 0.10f),
                size = Size(w * 0.76f, h * 0.76f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val tail = Path().apply {
                moveTo(w * 0.21f, h * 0.71f)
                lineTo(w * 0.13f, h * 0.89f)
                lineTo(w * 0.33f, h * 0.82f)
            }
            drawPath(
                path = tail,
                color = Color.White,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Classic telephone handset inside bubble
            val phone = Path().apply {
                moveTo(w * 0.37f, h * 0.33f)
                cubicTo(w * 0.32f, h * 0.43f, w * 0.42f, h * 0.60f, w * 0.59f, h * 0.65f)
                cubicTo(w * 0.64f, h * 0.66f, w * 0.68f, h * 0.62f, w * 0.69f, h * 0.58f)
                lineTo(w * 0.60f, h * 0.54f)
                lineTo(w * 0.56f, h * 0.57f)
                cubicTo(w * 0.50f, h * 0.55f, w * 0.45f, h * 0.50f, w * 0.43f, h * 0.44f)
                lineTo(w * 0.46f, h * 0.40f)
                lineTo(w * 0.42f, h * 0.31f)
                close()
            }
            drawPath(path = phone, color = Color.White)
        }
    }
}

@Composable
fun VistaTelegramCircleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF26A5E4), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(25.dp)) {
            val w = size.width
            val h = size.height
            val plane = Path().apply {
                moveTo(w * 0.88f, h * 0.18f)
                lineTo(w * 0.10f, h * 0.48f)
                lineTo(w * 0.37f, h * 0.58f)
                lineTo(w * 0.74f, h * 0.32f)
                lineTo(w * 0.45f, h * 0.62f)
                lineTo(w * 0.44f, h * 0.82f)
                lineTo(w * 0.56f, h * 0.70f)
                lineTo(w * 0.73f, h * 0.84f)
                close()
            }
            drawPath(path = plane, color = Color.White)
        }
    }
}

@Composable
fun VistaThreadsCircleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VistaShareSheetTokens.CircleBackground, CircleShape)
            .border(1.dp, VistaShareSheetTokens.CircleBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.0.dp.toPx()
            // Outer Threads C-loop
            drawArc(
                color = Color.White,
                startAngle = 35f,
                sweepAngle = 295f,
                useCenter = false,
                topLeft = Offset(w * 0.12f, h * 0.08f),
                size = Size(w * 0.76f, h * 0.84f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Inner "@" loop + flourish characteristic of Threads glyph
            val inner = Path().apply {
                moveTo(w * 0.65f, h * 0.44f)
                cubicTo(w * 0.65f, h * 0.31f, w * 0.54f, h * 0.25f, w * 0.44f, h * 0.27f)
                cubicTo(w * 0.33f, h * 0.30f, w * 0.31f, h * 0.47f, w * 0.41f, h * 0.53f)
                cubicTo(w * 0.51f, h * 0.59f, w * 0.66f, h * 0.51f, w * 0.65f, h * 0.40f)
                cubicTo(w * 0.66f, h * 0.64f, w * 0.56f, h * 0.74f, w * 0.44f, h * 0.72f)
                cubicTo(w * 0.34f, h * 0.70f, w * 0.32f, h * 0.60f, w * 0.38f, h * 0.54f)
                cubicTo(w * 0.48f, h * 0.46f, w * 0.74f, h * 0.50f, w * 0.80f, h * 0.66f)
            }
            drawPath(
                path = inner,
                color = Color.White,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
fun VistaXBrandCircleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E0F12), CircleShape)
            .border(1.dp, VistaShareSheetTokens.CircleBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(21.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.0.dp.toPx()
            // Double-bar left-to-right diagonal (authentic X mark silhouette)
            val outline = Path().apply {
                moveTo(w * 0.14f, h * 0.14f)
                lineTo(w * 0.33f, h * 0.14f)
                lineTo(w * 0.86f, h * 0.86f)
                lineTo(w * 0.67f, h * 0.86f)
                close()
            }
            drawPath(
                path = outline,
                color = Color.White,
                style = Stroke(width = stroke * 0.72f, join = StrokeJoin.Round),
            )
            // Crossing solid stroke right-to-left
            drawLine(
                color = Color.White,
                start = Offset(w * 0.82f, h * 0.15f),
                end = Offset(w * 0.18f, h * 0.85f),
                strokeWidth = stroke,
                cap = StrokeCap.Square,
            )
        }
    }
}

@Composable
fun VistaMoreShareCircleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VistaShareSheetTokens.CircleBackground, CircleShape)
            .border(1.dp, VistaShareSheetTokens.CircleBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.0.dp.toPx()
            // Modern share-out tray + upward arrow (matches Threads "More" icon)
            val tray = Path().apply {
                moveTo(w * 0.18f, h * 0.48f)
                lineTo(w * 0.18f, h * 0.82f)
                lineTo(w * 0.82f, h * 0.82f)
                lineTo(w * 0.82f, h * 0.48f)
            }
            drawPath(
                path = tray,
                color = Color.White,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawLine(
                color = Color.White,
                start = Offset(w * 0.50f, h * 0.62f),
                end = Offset(w * 0.50f, h * 0.16f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            val arrow = Path().apply {
                moveTo(w * 0.33f, h * 0.33f)
                lineTo(w * 0.50f, h * 0.15f)
                lineTo(w * 0.67f, h * 0.33f)
            }
            drawPath(
                path = arrow,
                color = Color.White,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

fun shareTextToTelegram(context: Context, text: String, shareUrl: String) {
    val telegramPackages = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",
    )
    for (pkg in telegramPackages) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = pkg
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Try next client package
        }
    }
    try {
        val webUri = Uri.parse("https://t.me/share/url").buildUpon()
            .appendQueryParameter("url", shareUrl)
            .appendQueryParameter("text", text)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        shareTextViaSystemChooser(context, text, "اشتراک‌گذاری در تلگرام")
    }
}

fun shareTextToWhatsApp(context: Context, text: String) {
    for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = pkg
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Try next WhatsApp package
        }
    }
    shareTextViaSystemChooser(context, text, "اشتراک‌گذاری در واتساپ")
}

fun shareTextToThreads(context: Context, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.instagram.barcelona"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return
    } catch (_: Exception) {
        // Fallback to Threads web composer
    }
    try {
        val webUri = Uri.parse("https://www.threads.net/intent/post").buildUpon()
            .appendQueryParameter("text", text)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        shareTextViaSystemChooser(context, text, "اشتراک‌گذاری در تردز")
    }
}

fun shareTextToX(context: Context, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.twitter.android"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return
    } catch (_: Exception) {
        // Fallback to X web intent
    }
    try {
        val webUri = Uri.parse("https://twitter.com/intent/tweet").buildUpon()
            .appendQueryParameter("text", text)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        shareTextViaSystemChooser(context, text, "اشتراک‌گذاری در X")
    }
}

fun shareTextViaSystemChooser(context: Context, text: String, title: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}

/**
 * Builds the unified Instagram + Threads inspired horizontal share rail used across Feed and Profile.
 */
fun buildVistaStandardShareActions(
    isBusy: Boolean,
    showAddToVistaStory: Boolean = true,
    onAddToVistaStory: () -> Unit,
    onShareInstagramStory: () -> Unit,
    onCopyLink: () -> Unit,
    onShareTelegram: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareThreads: () -> Unit,
    onShareX: () -> Unit,
    onShareMore: () -> Unit,
): List<VistaShareRailAction> = buildList {
    if (showAddToVistaStory) {
        add(
            VistaShareRailAction(
                id = "add_to_story",
                label = if (isBusy) "در حال ساخت..." else "افزودن به استوری",
                enabled = !isBusy,
                onClick = onAddToVistaStory,
                icon = { VistaAddToStoryCircleIcon(gradientAccent = false) },
            ),
        )
    }
    add(
        VistaShareRailAction(
            id = "instagram_story",
            label = if (isBusy) "در حال ساخت..." else "استوری اینستا",
            enabled = !isBusy,
            onClick = onShareInstagramStory,
            icon = { VistaAddToStoryCircleIcon(gradientAccent = true) },
        ),
    )
    add(
        VistaShareRailAction(
            id = "copy_link",
            label = "کپی لینک",
            onClick = onCopyLink,
            icon = { VistaCopyLinkCircleIcon() },
        ),
    )
    add(
        VistaShareRailAction(
            id = "telegram",
            label = "تلگرام",
            onClick = onShareTelegram,
            icon = { VistaTelegramCircleIcon() },
        ),
    )
    add(
        VistaShareRailAction(
            id = "whatsapp",
            label = "واتساپ",
            onClick = onShareWhatsApp,
            icon = { VistaWhatsAppCircleIcon() },
        ),
    )
    add(
        VistaShareRailAction(
            id = "threads",
            label = "تردز",
            onClick = onShareThreads,
            icon = { VistaThreadsCircleIcon() },
        ),
    )
    add(
        VistaShareRailAction(
            id = "x",
            label = "X",
            onClick = onShareX,
            icon = { VistaXBrandCircleIcon() },
        ),
    )
    add(
        VistaShareRailAction(
            id = "more",
            label = "بیشتر",
            onClick = onShareMore,
            icon = { VistaMoreShareCircleIcon() },
        ),
    )
}
