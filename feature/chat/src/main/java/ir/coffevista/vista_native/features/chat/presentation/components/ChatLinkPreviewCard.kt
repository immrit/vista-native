package ir.coffevista.vista_native.features.chat.presentation.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors

private val URL_REGEX = Regex(
    """https?://[^\s<>"'{}|\\^`]+""",
    RegexOption.IGNORE_CASE,
)

internal fun extractFirstUrl(text: String): String? {
    return URL_REGEX.find(text)?.value
}

private fun extractDomain(url: String): String {
    return runCatching {
        val uri = Uri.parse(url)
        val host = uri.host.orEmpty().removePrefix("www.")
        if (host.isNotBlank()) host else url.take(32)
    }.getOrDefault(url.take(32))
}

@Composable
fun ChatLinkPreviewCard(
    url: String,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onUrlClick: ((String) -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current
    val domain = remember(url) { extractDomain(url) }

    val accentColor = if (isMine) Color.White.copy(alpha = 0.95f) else VistaBrandColors.Indigo
    val cardBackground = if (isMine) {
        Color.White.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }
    val subTextColor = if (isMine) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBackground)
            .clickable {
                if (onUrlClick != null) {
                    onUrlClick(url)
                } else {
                    runCatching { uriHandler.openUri(url) }
                }
            }
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .fillMaxHeight()
                .background(accentColor),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = domain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr),
                )
            }

            Text(
                text = url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.5.sp,
                color = subTextColor,
                style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Ltr),
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = subTextColor.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(end = 10.dp)
                .size(16.dp),
        )
    }
}
