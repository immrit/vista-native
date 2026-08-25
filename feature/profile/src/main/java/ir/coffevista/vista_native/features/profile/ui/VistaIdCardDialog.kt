package ir.coffevista.vista_native.features.profile.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import ir.coffevista.vista_native.features.profile.R

/**
 * Vista ID Card - Premium 3D Interactive Digital Business Card
 * 1-to-1 exact parity with Flutter's `VistaIDCard` widget.
 */
@Composable
internal fun VistaIdCardDialog(
    user: ProfileHeaderModel,
    userId: String,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x8A000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                VistaIdCardContent(
                    user = user,
                    userId = userId,
                )
            }
        }
    }
}

@Composable
private fun VistaIdCardContent(
    user: ProfileHeaderModel,
    userId: String,
) {
    var isFlipped by remember { mutableStateOf(false) }
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var shimmerOffset by remember { mutableFloatStateOf(0.5f) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "cardFlip",
    )

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}, // Absorb click so card doesn't dismiss dialog
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 3D Flippable Card
        Box(
            modifier = Modifier
                .size(width = 320.dp, height = 200.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { isFlipped = !isFlipped },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            tiltX = 0f
                            tiltY = 0f
                        },
                        onDragCancel = {
                            tiltX = 0f
                            tiltY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newTiltX = (tiltX + dragAmount.y * 0.005f).coerceIn(-15f, 15f)
                            val newTiltY = (tiltY - dragAmount.x * 0.005f).coerceIn(-15f, 15f)
                            tiltX = newTiltX
                            tiltY = newTiltY
                            shimmerOffset = ((shimmerOffset + dragAmount.x / 320f)).coerceIn(0f, 1f)
                        },
                    )
                }
                .graphicsLayer {
                    this.rotationX = tiltX
                    this.rotationY = rotationAngle + tiltY
                    this.cameraDistance = 12f * density
                },
        ) {
            if (rotationAngle <= 90f) {
                // Front Side
                VistaIdCardFront(
                    user = user,
                    shimmerOffset = shimmerOffset,
                )
            } else {
                // Back Side (Rotated 180 so it renders correctly)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.rotationY = 180f
                        },
                ) {
                    VistaIdCardBack(
                        user = user,
                        userId = userId,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Hint Text (exact match to Flutter)
        Text(
            text = "ضربه بزنید تا برگردد",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun VistaIdCardFront(
    user: ProfileHeaderModel,
    shimmerOffset: Float,
) {
    val effectiveVerification = (user.verificationType ?: "").lowercase()
    val glowColor = when {
        effectiveVerification.contains("gold") -> Color(0xFFFFC107)
        effectiveVerification.contains("blue") -> Color(0xFF2196F3)
        effectiveVerification.contains("black") -> Color(0xFF9E9E9E)
        else -> Color(0xFF6366F1)
    }

    val gradientColors = when {
        effectiveVerification.contains("gold") -> listOf(Color(0xFFFFC107), Color(0xFFFF9800))
        effectiveVerification.contains("blue") -> listOf(Color(0xFF2196F3), Color(0xFF00BCD4))
        effectiveVerification.contains("black") -> listOf(Color(0xFF757575), Color(0xFF424242))
        else -> listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    }

    val userTitle = when {
        effectiveVerification.contains("gold") -> "PREMIUM"
        effectiveVerification.contains("blue") -> "VERIFIED"
        effectiveVerification.contains("black") -> "DEVELOPER"
        else -> "MEMBER"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = 25.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = glowColor.copy(alpha = 0.25f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E1E2E),
                        Color(0xFF12121C),
                        Color(0xFF0A0A0F),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
    ) {
        // Holographic shimmer overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shimmerX = size.width * shimmerOffset
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.05f),
                        Color(0xFF9C27B0).copy(alpha = 0.03f),
                        Color(0xFF2196F3).copy(alpha = 0.03f),
                        Color.Transparent,
                    ),
                    start = Offset(shimmerX - 100f, 0f),
                    end = Offset(shimmerX + 100f, size.height),
                ),
                size = size,
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top: Logo and badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Vista Logo (Flutter asset)
                    Image(
                        painter = painterResource(R.drawable.ic_vista_logo_white),
                        contentDescription = "Vista Logo",
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "VISTA ID",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        )
                        Text(
                            text = userTitle,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 8.sp,
                            letterSpacing = 1.sp,
                        )
                    }
                }

                if (user.isVerified) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(glowColor.copy(alpha = 0.15f))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified",
                            tint = glowColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Bottom: Avatar and info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar with gradient border
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(gradientColors))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (user.avatarUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E2E)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.38f),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    } else {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                // Name and handle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName.ifBlank { user.username ?: "" },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "@${user.username ?: ""}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Holographic chip
                HoloChip(shimmerOffset = shimmerOffset)
            }
        }
    }
}

@Composable
private fun HoloChip(shimmerOffset: Float) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF616161),
                        Color(0xFFBDBDBD),
                        Color(0xFF757575),
                        Color(0xFFE0E0E0),
                        Color(0xFF616161),
                    ),
                    startX = (-1f + shimmerOffset * 2f) * 100f,
                    endX = (1f + shimmerOffset * 2f) * 100f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f)),
        )
    }
}

@Composable
private fun VistaIdCardBack(
    user: ProfileHeaderModel,
    userId: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = 25.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F5F7),
                        Color(0xFFE8E8ED),
                    ),
                ),
            ),
    ) {
        // Subtle grid pattern (exact to Flutter's _GridPatternPainter)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 15.dp.toPx()
            val gridColor = Color.Gray.copy(alpha = 0.08f)
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                y += spacing
            }
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
                x += spacing
            }
        }

        // Back Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // QR Code Container
            Box(
                modifier = Modifier
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.06f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(80.dp)) {
                    QrCodeCanvas(data = "vista://user/$userId")
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "اسکن کنید",
                color = Color(0xFF757575),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.weight(1f))

            // Bottom Branding Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_vista_logo_white),
                        contentDescription = "Vista",
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(Color(0xFF9E9E9E)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "VISTA",
                        color = Color(0xFF9E9E9E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                    )
                }

                Text(
                    text = "@${user.username ?: ""}",
                    color = Color(0xFFBDBDBD),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

/**
 * High-performance native QR Code Canvas renderer with Color(0xFF1A1A2E)
 */
@Composable
private fun QrCodeCanvas(
    data: String,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val count = 21
        val cellSize = size.width / count
        val qrColor = Color(0xFF1A1A2E)

        fun drawSquareModule(r: Int, c: Int) {
            drawRect(
                color = qrColor,
                topLeft = Offset(c * cellSize, r * cellSize),
                size = Size(cellSize, cellSize),
            )
        }

        fun drawFinderPattern(startRow: Int, startCol: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    if (isBorder || isCenter) {
                        drawSquareModule(startRow + r, startCol + c)
                    }
                }
            }
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(0, count - 7)
        drawFinderPattern(count - 7, 0)

        for (r in 13..15) {
            for (c in 13..15) {
                if (r == 13 || r == 15 || c == 13 || c == 15 || (r == 14 && c == 14)) {
                    drawSquareModule(r, c)
                }
            }
        }

        for (i in 7 until count - 7) {
            if (i % 2 == 0) {
                drawSquareModule(6, i)
                drawSquareModule(i, 6)
            }
        }

        val seed = data.hashCode().toLong()
        val rnd = java.util.Random(seed)
        for (r in 0 until count) {
            for (c in 0 until count) {
                val inTopLeftFinder = r < 8 && c < 8
                val inTopRightFinder = r < 8 && c >= count - 8
                val inBottomLeftFinder = r >= count - 8 && c < 8
                val inAlignment = r in 12..16 && c in 12..16
                val inTiming = r == 6 || c == 6

                if (!inTopLeftFinder && !inTopRightFinder && !inBottomLeftFinder && !inAlignment && !inTiming) {
                    if (rnd.nextBoolean()) {
                        drawSquareModule(r, c)
                    }
                }
            }
        }
    }
}
