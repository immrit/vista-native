package ir.coffevista.vista_native.features.feed.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.component.VistaFullscreenSurface
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun VistaFullscreenImageViewer(
    images: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    sourceBounds: Rect? = null,
) {
    if (images.isEmpty()) return

    VistaFullscreenSurface(onDismissRequest = onDismiss) {
        val scope = rememberCoroutineScope()
        val openProgress = remember { Animatable(0f) }
        val dragOffsetY = remember { Animatable(0f) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }
        var isClosing by remember { mutableStateOf(false) }

        val dismissWithAnimation: () -> Unit = {
            if (!isClosing) {
                isClosing = true
                scope.launch {
                    openProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.92f,
                            stiffness = 520f,
                        ),
                    )
                    onDismiss()
                }
            }
        }

        LaunchedEffect(Unit) {
            openProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = 420f,
                ),
            )
        }

        BackHandler(enabled = true, onBack = dismissWithAnimation)

        val pagerState = rememberPagerState(
            initialPage = initialIndex.coerceIn(0, images.size - 1),
            pageCount = { images.size },
        )
        var showChrome by remember { mutableStateOf(true) }
        var currentImageScale by remember { mutableFloatStateOf(1f) }
        val backdropInteraction = remember { MutableInteractionSource() }
        val density = LocalDensity.current
        val dismissThresholdPx = with(density) { 110.dp.toPx() }
        val dragFraction = (abs(dragOffsetY.value) / (dismissThresholdPx * 2.2f)).coerceIn(0f, 1f)
        val p = openProgress.value.coerceIn(0f, 1f)
        val scrimAlpha = (p * (1f - dragFraction * 0.72f)).coerceIn(0f, 1f)

        val originTransformModifier = if (sourceBounds != null && containerSize.width > 0 && containerSize.height > 0) {
            val targetW = containerSize.width.toFloat()
            val targetH = containerSize.height.toFloat()
            val startScaleX = (sourceBounds.width / targetW).coerceIn(0.12f, 1f)
            val startScaleY = (sourceBounds.height / targetH).coerceIn(0.12f, 1f)
            val startScale = maxOf(startScaleX, startScaleY)
            val startCenterX = sourceBounds.center.x
            val startCenterY = sourceBounds.center.y
            val targetCenterX = targetW / 2f
            val targetCenterY = targetH / 2f
            val cornerRadius = lerp(16.dp, 0.dp, p)
            Modifier
                .graphicsLayer {
                    val dragScale = 1f - dragFraction * 0.14f
                    val s = lerp(startScale, 1f, p) * dragScale
                    scaleX = s
                    scaleY = s
                    translationX = lerp(startCenterX - targetCenterX, 0f, p)
                    translationY = lerp(startCenterY - targetCenterY, 0f, p) + dragOffsetY.value
                    alpha = p.coerceIn(0.25f, 1f)
                }
                .clip(RoundedCornerShape(cornerRadius))
        } else {
            Modifier.graphicsLayer {
                val dragScale = 1f - dragFraction * 0.14f
                val s = lerp(0.92f, 1f, p) * dragScale
                scaleX = s
                scaleY = s
                translationY = dragOffsetY.value
                alpha = p
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = backdropInteraction,
                    indication = null,
                    onClick = {},
                )
                .pointerInput(currentImageScale) {
                    if (currentImageScale <= 1.05f) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (abs(dragOffsetY.value) > dismissThresholdPx) {
                                    dismissWithAnimation()
                                } else {
                                    scope.launch {
                                        dragOffsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.82f,
                                                stiffness = 460f,
                                            ),
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    dragOffsetY.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 460f))
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    dragOffsetY.snapTo(dragOffsetY.value + dragAmount)
                                }
                            },
                        )
                    }
                },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(originTransformModifier),
                userScrollEnabled = currentImageScale <= 1.02f && abs(dragOffsetY.value) < 6f,
            ) { page ->
                ZoomableImage(
                    imageUrl = images[page],
                    onToggleChrome = { showChrome = !showChrome },
                    onScaleChanged = { newScale ->
                        if (page == pagerState.currentPage) {
                            currentImageScale = newScale
                        }
                    },
                )
            }

            // Top Bar Chrome
            AnimatedVisibility(
                visible = showChrome && !isClosing && abs(dragOffsetY.value) < 24f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = dismissWithAnimation,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF121417).copy(alpha = 0.76f), CircleShape)
                            .border(0.6.dp, Color.White.copy(alpha = 0.24f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.White,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (images.size > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF121417).copy(alpha = 0.76f))
                                .border(0.6.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(20.dp)),
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} از ${images.size}".toPersianDigits(),
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    imageUrl: String,
    onToggleChrome: () -> Unit,
    onScaleChanged: (Float) -> Unit = {},
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(scale) {
        onScaleChanged(scale)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleChrome() },
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale > 1f) {
                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                            y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY),
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}
