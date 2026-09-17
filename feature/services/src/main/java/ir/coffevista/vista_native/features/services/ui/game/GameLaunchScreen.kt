package ir.coffevista.vista_native.features.services.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.features.services.data.ServicesRepository
import ir.coffevista.vista_native.features.services.ui.ServicesActionIcon
import ir.coffevista.vista_native.features.services.ui.ServicesActionKind
import java.net.URLEncoder
import kotlin.math.sin

private enum class GameLaunchPhase {
    LOADING,
    ERROR,
}

@Composable
fun GameLaunchScreen(
    repository: ServicesRepository,
    onBack: () -> Unit,
    onLaunchSuccess: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var phase by remember { mutableStateOf(GameLaunchPhase.LOADING) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun launch() {
        phase = GameLaunchPhase.LOADING
        errorMessage = null
    }

    LaunchedEffect(phase) {
        if (phase == GameLaunchPhase.LOADING) {
            runCatching {
                val ticket = repository.createGameSsoTicket()
                val encodedTicket = URLEncoder.encode(ticket, "UTF-8")
                val targetUrl = "https://coffevista.ir/game/sso?ticket=$encodedTicket"
                onLaunchSuccess(targetUrl, "ویستا کوییز")
            }.onFailure { error ->
                errorMessage = mapGameError(error)
                phase = GameLaunchPhase.ERROR
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0A3D6B),
                            Color(0xFF1A6EBD),
                            Color(0xFF1B82C9),
                        ),
                    ),
                ),
        ) {
            // Subtle Dot Grid Background
            DotGridBackground(modifier = Modifier.fillMaxSize())

            // Close Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "بستن",
                    tint = Color.White.copy(alpha = 0.54f),
                    modifier = Modifier.size(28.dp),
                )
            }

            // Central Animated Panel
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(350)) },
                    label = "phaseAnimation",
                ) { currentPhase ->
                    when (currentPhase) {
                        GameLaunchPhase.LOADING -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                // Glowing Gamepad Card
                                Box(
                                    modifier = Modifier
                                        .scale(pulseScale)
                                        .size(108.dp)
                                        .shadow(
                                            elevation = 20.dp,
                                            shape = RoundedCornerShape(32.dp),
                                            ambientColor = Color(0xFF1A6EBD).copy(alpha = 0.75f),
                                            spotColor = Color(0xFF1A6EBD).copy(alpha = 0.85f),
                                        )
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF2596D6), Color(0xFF1262A8)),
                                            ),
                                            shape = RoundedCornerShape(32.dp),
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = Color.White.copy(alpha = 0.22f),
                                            shape = RoundedCornerShape(32.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ServicesActionIcon(
                                        kind = ServicesActionKind.GAME,
                                        color = Color.White,
                                        modifier = Modifier.size(56.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(38.dp))

                                Text(
                                    text = "ویستا کوییز",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = VistaFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp,
                                        color = Color.White,
                                        letterSpacing = 0.3.sp,
                                    ),
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "در حال ورود به لابی بازی...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VistaFontFamily,
                                        fontSize = 15.sp,
                                        color = Color.White.copy(alpha = 0.65f),
                                    ),
                                )

                                Spacer(modifier = Modifier.height(30.dp))

                                BouncingDots()
                            }
                        }
                        GameLaunchPhase.ERROR -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .background(
                                            Color.Red.copy(alpha = 0.14f),
                                            RoundedCornerShape(28.dp),
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = Color(0xFFFF5252).copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(28.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ServicesActionIcon(
                                        kind = ServicesActionKind.GAME,
                                        color = Color(0xFFFF5252),
                                        modifier = Modifier.size(46.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                Text(
                                    text = "ورود به بازی ممکن نشد",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = VistaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White,
                                    ),
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = errorMessage ?: "خطای ناشناخته‌ای رخ داد.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = VistaFontFamily,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.65f),
                                        lineHeight = 22.sp,
                                        textAlign = TextAlign.Center,
                                    ),
                                )

                                Spacer(modifier = Modifier.height(36.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = onBack,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.25f),
                                        ),
                                    ) {
                                        Text(
                                            text = "بازگشت",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = VistaFontFamily,
                                                color = Color.White.copy(alpha = 0.8f),
                                            ),
                                        )
                                    }

                                    Button(
                                        onClick = { launch() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF78C02C),
                                            contentColor = Color.White,
                                        ),
                                    ) {
                                        Text(
                                            text = "تلاش مجدد",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = VistaFontFamily,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BouncingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dotsAnim",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val phase = (animProgress + index / 3f) % 1f
            val t = sin(phase * Math.PI).toFloat().coerceIn(0f, 1f)
            val offsetY = -t * 10f
            val alpha = 0.35f + t * 0.65f

            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(9.dp)
                    .background(Color.White.copy(alpha = alpha), CircleShape),
            )
        }
    }
}

@Composable
private fun DotGridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val step = 52.dp.toPx()
        val radius = 2.5.dp.toPx()
        val dotColor = Color.White.copy(alpha = 0.05f)

        var x = step / 2f
        while (x < size.width + step) {
            var y = step / 2f
            while (y < size.height + step) {
                drawCircle(
                    color = dotColor,
                    radius = radius,
                    center = Offset(x, y),
                )
                y += step
            }
            x += step
        }
    }
}

private fun mapGameError(e: Throwable): String {
    val s = e.message.orEmpty().lowercase()
    return when {
        s.contains("not logged in") || s.contains("401") || s.contains("unauthorized") ->
            "برای ورود به بازی باید در ویستا وارد باشید."
        s.contains("timeout") || s.contains("connection") || s.contains("network") ||
            s.contains("socket") || s.contains("failed host lookup") ||
            s.contains("unable to resolve host") ->
            "اتصال اینترنت را بررسی کنید و دوباره تلاش کنید."
        s.contains("429") || s.contains("rate limit") ->
            "درخواست‌های زیادی ارسال شده، چند ثانیه صبر کنید."
        s.contains("503") || s.contains("unavailable") || s.contains("sso_unavailable") ->
            "سرویس بازی موقتاً در دسترس نیست."
        else -> "ورود به بازی ممکن نشد. دوباره تلاش کنید."
    }
}
