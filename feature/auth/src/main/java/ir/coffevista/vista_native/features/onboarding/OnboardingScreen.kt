package ir.coffevista.vista_native.features.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.core.designsystem.component.VistaButton
import ir.coffevista.vista_native.core.designsystem.component.VistaButtonVariant
import ir.coffevista.vista_native.core.designsystem.component.VistaSurface
import ir.coffevista.vista_native.core.designsystem.tokens.VistaElevation

data class OnboardingSlide(
    val kicker: String,
    val title: String,
    val description: String,
    val accent: Color,
    val accentDeep: Color,
    val image: Painter,
    val imageScale: Float,
)

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    onCompleted: () -> Unit,
    slides: List<OnboardingSlide>,
    brand: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(slides.size == OnboardingViewModel.PAGE_COUNT) {
        "Onboarding visuals must match the versioned page count"
    }
    val pagerState = rememberPagerState(
        initialPage = state.page,
        pageCount = { slides.size },
    )

    BackHandler(enabled = state.page > 0) {
        onAction(OnboardingAction.Previous)
    }

    LaunchedEffect(state.page) {
        if (pagerState.currentPage != state.page) {
            pagerState.animateScrollToPage(state.page)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != state.page) {
            onAction(OnboardingAction.PageChanged(pagerState.settledPage))
        }
    }
    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }

    val slide = slides[state.page]
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        slide.accent.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                    center = Offset(900f, 0f),
                    radius = 1_300f,
                ),
            )
            .padding(horizontal = 22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            brand(Modifier.size(44.dp))
            Column(Modifier.padding(start = 10.dp)) {
                Text(
                    text = "VISTA",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "دنیای نزدیک‌تر",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f),
                )
            }
            Spacer(Modifier.weight(1f))
            VistaButton(
                onClick = { onAction(OnboardingAction.Skip) },
                enabled = !state.isCompleting,
                variant = VistaButtonVariant.Text,
            ) {
                Text("رد کردن")
            }
        }

        ProgressRail(
            currentPage = state.page,
            pageCount = slides.size,
            accent = slide.accent,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            OnboardingPage(slides[page], page)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.page > 0) {
                VistaButton(
                    onClick = { onAction(OnboardingAction.Previous) },
                    enabled = !state.isCompleting,
                    modifier = Modifier.padding(end = 10.dp),
                    variant = VistaButtonVariant.Outline,
                ) {
                    Text("قبلی")
                }
            }
            VistaButton(
                onClick = { onAction(OnboardingAction.Next) },
                enabled = !state.isCompleting,
                loading = state.isCompleting,
                containerColor = slide.accentDeep,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Text(
                    text = if (state.page == slides.lastIndex) {
                        "ورود به ویستا"
                    } else {
                        "ادامه"
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProgressRail(
    currentPage: Int,
    pageCount: Int,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(pageCount) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(if (index <= currentPage) 4.dp else 3.dp)
                    .background(
                        color = if (index <= currentPage) {
                            accent
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun OnboardingPage(
    slide: OnboardingSlide,
    page: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VistaSurface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.62f),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = VistaElevation.High,
        ) {
            OnboardingHero(slide = slide)
        }
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = slide,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(180))
            },
            label = "slideCopy",
        ) { content ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VistaSurface(
                    color = content.accent.copy(alpha = 0.1f),
                    shape = CircleShape,
                ) {
                    Text(
                        text = content.kicker,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                        color = content.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    text = content.title,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = content.description,
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f),
                    textAlign = TextAlign.Center,
                    lineHeight = 25.sp,
                )
            }
        }
    }
}

@Composable
private fun OnboardingHero(
    slide: OnboardingSlide,
) {
    Image(
        painter = slide.image,
        contentDescription = "تصویر شخصیت ویو برای ${slide.title}",
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .graphicsLayer {
                scaleX = slide.imageScale
                scaleY = slide.imageScale
            },
        contentScale = ContentScale.Fit,
    )
}
