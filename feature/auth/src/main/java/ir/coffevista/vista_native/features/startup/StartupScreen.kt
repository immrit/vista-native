package ir.coffevista.vista_native.features.startup

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.component.VistaErrorState
import ir.coffevista.vista_native.core.designsystem.component.VistaLoadingState

@Composable
fun StartupScreen(
    state: StartupUiState,
    onRetry: () -> Unit,
    accentColor: Color,
    brand: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "splash")
    val brandAlpha by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_300),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "brandAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        accentColor.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .safeDrawingPadding()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            brand(Modifier.alpha(brandAlpha))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "دنیای نزدیک‌تر",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.64f),
            )
            Spacer(Modifier.height(34.dp))

            when (val destination = state.destination) {
                StartupDestination.Loading -> VistaLoadingState("در حال آماده‌سازی ویستا")
                is StartupDestination.RecoverableError -> VistaErrorState(
                    title = "راه‌اندازی کامل نشد",
                    message = destination.messageFa,
                    onRetry = onRetry,
                )
                else -> Unit
            }
        }
    }
}
