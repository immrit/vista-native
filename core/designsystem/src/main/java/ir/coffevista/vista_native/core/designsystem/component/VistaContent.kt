package ir.coffevista.vista_native.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import ir.coffevista.vista_native.core.designsystem.theme.vistaMotion
import ir.coffevista.vista_native.core.designsystem.tokens.VistaComponentSize
import ir.coffevista.vista_native.core.designsystem.tokens.VistaLayout
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSpacing

@Composable
fun VistaAvatar(
    displayName: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val initials = displayName.trim().split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .ifBlank { "V" }
    Box(
        modifier = modifier
            .size(VistaComponentSize.Avatar)
            .clip(CircleShape)
            .background(background)
            .semantics { contentDescription = "تصویر نمایه $displayName" },
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VistaBadge(label: String, modifier: Modifier = Modifier) {
    VistaSurface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = VistaSpacing.Medium,
                vertical = VistaSpacing.XSmall,
            ),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun VistaMediaCard(
    title: String,
    subtitle: String,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    VistaSurface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = ir.coffevista.vista_native.core.designsystem.tokens.VistaElevation.Low,
    ) {
        Row(
            modifier = Modifier.padding(VistaSpacing.Large),
            horizontalArrangement = Arrangement.spacedBy(VistaSpacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                VistaSkeleton(Modifier.size(VistaComponentSize.Avatar))
            } else {
                VistaAvatar(title)
            }
            Column(Modifier.weight(1f)) {
                if (loading) {
                    VistaSkeleton(Modifier.fillMaxWidth().height(VistaSpacing.Large))
                    Spacer(Modifier.height(VistaSpacing.Small))
                    VistaSkeleton(Modifier.fillMaxWidth(0.65f).height(VistaSpacing.Medium))
                } else {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun VistaSkeleton(modifier: Modifier = Modifier) {
    val duration = MaterialTheme.vistaMotion.standard
    val alpha = if (duration == 0) {
        0.35f
    } else {
        val transition = rememberInfiniteTransition(label = "vistaSkeleton")
        val animated by transition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.48f,
            animationSpec = infiniteRepeatable(
                animation = tween(duration),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "vistaSkeletonAlpha",
        )
        animated
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            .semantics { contentDescription = "در حال بارگذاری" },
    )
}

@Composable
fun VistaEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) = VistaStateLayout(title, message, modifier) { }

@Composable
fun VistaErrorState(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) = VistaStateLayout(title, message, modifier) {
    VistaButton(onClick = onRetry) { Text("تلاش دوباره") }
}

@Composable
fun VistaLoadingState(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(VistaLayout.ScreenHorizontal)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            label,
            modifier = Modifier.padding(top = VistaSpacing.Large),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VistaStateLayout(
    title: String,
    message: String,
    modifier: Modifier,
    action: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(VistaLayout.ScreenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            message,
            modifier = Modifier.padding(top = VistaSpacing.Small, bottom = VistaSpacing.Large),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action()
    }
}
