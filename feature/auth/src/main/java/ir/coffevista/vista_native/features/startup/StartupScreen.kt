package ir.coffevista.vista_native.features.startup

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.coffevista.vista_native.core.designsystem.component.VistaErrorState

@Composable
fun StartupScreen(
    state: StartupUiState,
    onRetry: () -> Unit,
    accentColor: Color = Color.Unspecified,
    brand: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            brand(Modifier)
            Spacer(Modifier.height(30.dp))

            when (val destination = state.destination) {
                StartupDestination.Loading -> ProgressiveDotsIndicator(
                    color = Color.White,
                    size = 50.dp,
                )
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

@Composable
fun ProgressiveDotsIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 50.dp,
) {
    val dotCount = 5
    val dotSize = 8.dp
    val transition = rememberInfiniteTransition(label = "progressiveDots")
    val animDuration = 1200

    Row(
        modifier = modifier.width(size),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until dotCount) {
            val delay = (animDuration / dotCount) * i
            val scale by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = animDuration
                        0.35f at 0
                        1.0f at ((delay + 250) % animDuration)
                        0.35f at ((delay + 500) % animDuration)
                        0.35f at animDuration
                    },
                    repeatMode = RepeatMode.Restart,
                ),
                label = "dotScale$i",
            )
            val alpha by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = animDuration
                        0.35f at 0
                        1.0f at ((delay + 250) % animDuration)
                        0.35f at ((delay + 500) % animDuration)
                        0.35f at animDuration
                    },
                    repeatMode = RepeatMode.Restart,
                ),
                label = "dotAlpha$i",
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .background(color, CircleShape),
            )
        }
    }
}

