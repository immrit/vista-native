package ir.coffevista.vista_native.core.designsystem.component

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ir.coffevista.vista_native.core.designsystem.R
import ir.coffevista.vista_native.core.designsystem.theme.vistaMotion
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors

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
 * Hardware-decoded 2.0-second staged action clip of Vista's official mascot Viu (ویو)
 * rendered cleanly on a soft ambient backdrop without outer wobble.
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
            val baseRadius = minOf(w, h) * 0.46f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = 0.16f),
                        secondaryAccent.copy(alpha = 0.05f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy * 0.96f),
                    radius = baseRadius * 1.1f,
                ),
                radius = baseRadius * 1.1f,
                center = Offset(cx, cy * 0.96f),
            )
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
                modifier = Modifier.fillMaxSize(0.94f),
            )
        } else {
            Image(
                painter = painterResource(drawableResForKind(kind)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.90f),
            )
        }
    }
}
