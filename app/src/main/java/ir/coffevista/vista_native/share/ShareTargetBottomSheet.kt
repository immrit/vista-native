package ir.coffevista.vista_native.share

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors

data class SharedIncomingContent(
    val requestId: String,
    val mimeType: String,
    val text: String? = null,
    val uris: List<Uri> = emptyList(),
    val rejectedItemCount: Int = 0,
) {
    val isMedia: Boolean get() = mimeType.startsWith("image/") || mimeType.startsWith("video/")
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isText: Boolean get() = mimeType.startsWith("text/") || (uris.isEmpty() && !text.isNullOrBlank())

    val destinationSummary: String
        get() = when {
            uris.isNotEmpty() && !text.isNullOrBlank() -> "${uris.size} فایل همراه با متن"
            uris.size > 1 -> "${uris.size} فایل"
            uris.size == 1 -> "یک فایل"
            !text.isNullOrBlank() -> text.lineSequence().firstOrNull().orEmpty().take(80)
            else -> "محتوای اشتراکی"
        }
}

enum class SharedContentDestination { CHAT, POST, STORY }

data class RoutedSharedContent(
    val content: SharedIncomingContent,
    val destination: SharedContentDestination,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTargetBottomSheet(
    content: SharedIncomingContent,
    onSendToChat: (SharedIncomingContent) -> Unit,
    onSendToPost: (SharedIncomingContent) -> Unit,
    onSendToStory: (SharedIncomingContent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(VistaBrandColors.Pink, VistaBrandColors.Indigo),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    ShareHeaderCanvasIcon(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "اشتراک‌گذاری در ویستا",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = getSubtitle(content),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Media Preview Thumbnail
            if (content.uris.isNotEmpty() && content.isMedia) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (content.isImage) {
                        AsyncImage(
                            model = content.uris.first(),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        VideoPlayCanvasIcon(
                            modifier = Modifier.size(44.dp),
                            color = VistaBrandColors.Pink,
                        )
                    }

                    if (content.uris.size > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "${content.uris.size} فایل",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 8.dp),
            )

            // Options
            ShareOptionItem(
                gradient = listOf(Color(0xFF0288D1), Color(0xFF00B0FF)),
                title = "ارسال پیام",
                subtitle = "برای یک نفر یا گروه بفرست",
                onClick = { onSendToChat(content) },
                icon = { ChatSendCanvasIcon(modifier = Modifier.size(20.dp), color = Color.White) },
            )

            if (content.isMedia || content.isText) {
                ShareOptionItem(
                    gradient = listOf(Color(0xFFE91E63), Color(0xFFFF9800)),
                    title = "ایجاد پست",
                    subtitle = "در صفحه پروفایلت منتشر کن",
                    onClick = { onSendToPost(content) },
                    icon = { PostAddCanvasIcon(modifier = Modifier.size(20.dp), color = Color.White) },
                )
            }

            if (content.isMedia && content.uris.isNotEmpty()) {
                ShareOptionItem(
                    gradient = listOf(Color(0xFF9C27B0), Color(0xFFE040FB)),
                    title = "ایجاد استوری",
                    subtitle = "۲۴ ساعت برای همه نمایش داده می‌شود",
                    onClick = { onSendToStory(content) },
                    icon = { StoryStarCanvasIcon(modifier = Modifier.size(20.dp), color = Color.White) },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cancel Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onDismiss),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "انصراف",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareOptionItem(
    gradient: List<Color>,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(colors = gradient)),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ChevronLeftCanvasIcon(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShareHeaderCanvasIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = w * 0.14f
        val strokeWidth = w * 0.09f
        val p1 = Offset(w * 0.76f, h * 0.24f)
        val p2 = Offset(w * 0.24f, h * 0.50f)
        val p3 = Offset(w * 0.76f, h * 0.76f)
        drawLine(color, p2, p1, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawLine(color, p2, p3, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawCircle(color, r, p1)
        drawCircle(color, r, p2)
        drawCircle(color, r, p3)
    }
}

@Composable
private fun VideoPlayCanvasIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.32f, size.height * 0.22f)
            lineTo(size.width * 0.78f, size.height * 0.50f)
            lineTo(size.width * 0.32f, size.height * 0.78f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun ChatSendCanvasIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.18f)
            lineTo(size.width * 0.88f, size.height * 0.50f)
            lineTo(size.width * 0.15f, size.height * 0.82f)
            lineTo(size.width * 0.36f, size.height * 0.50f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun PostAddCanvasIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val stroke = size.width * 0.12f
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val r = size.width * 0.35f
        drawLine(color, Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy - r), Offset(cx, cy + r), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun StoryStarCanvasIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val r = size.width * 0.42f
        val path = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r * 0.28f, cy - r * 0.28f)
            lineTo(cx + r, cy)
            lineTo(cx + r * 0.28f, cy + r * 0.28f)
            lineTo(cx, cy + r)
            lineTo(cx - r * 0.28f, cy + r * 0.28f)
            lineTo(cx - r, cy)
            lineTo(cx - r * 0.28f, cy - r * 0.28f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun ChevronLeftCanvasIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.65f, size.height * 0.20f)
            lineTo(size.width * 0.35f, size.height * 0.50f)
            lineTo(size.width * 0.65f, size.height * 0.80f)
        }
        drawPath(path, color, style = Stroke(width = size.width * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun getSubtitle(content: SharedIncomingContent): String {
    return when {
        content.isText -> "متن"
        content.isImage -> if (content.uris.size > 1) "${content.uris.size} تصویر" else "یک تصویر"
        content.isVideo -> if (content.uris.size > 1) "${content.uris.size} ویدیو" else "یک ویدیو"
        else -> "فایل"
    }
}
