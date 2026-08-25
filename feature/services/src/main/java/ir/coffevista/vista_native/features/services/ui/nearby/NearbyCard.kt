package ir.coffevista.vista_native.features.services.ui.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCandidate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NearbyCard(
    candidate: NearbyCandidate,
    onTap: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (candidate.age > 0) "${candidate.fullName}، ${candidate.age}" else candidate.fullName

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1E1E2E),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-bleed Avatar or Fallback
            if (candidate.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = candidate.avatarUrl,
                    contentDescription = candidate.fullName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(VistaBrandColors.Indigo, Color(0xFF8B5CF6))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = candidate.fullName.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 80.sp,
                        ),
                    )
                }
            }

            // Bottom Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.92f),
                            ),
                            startY = 400f,
                        )
                    )
            )

            // Location & City Chip (Top-Start)
            if (candidate.locationLine.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = candidate.locationLine,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VistaFontFamily,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
            }

            // Report Button (Top-End)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                IconButton(
                    onClick = onReport,
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "گزارش",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Bottom Profile Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 24.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (candidate.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "تایید شده",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (candidate.gender.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        GenderBadge(gender = candidate.gender)
                    }
                }

                // Presence Label
                if (candidate.presenceLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (candidate.isOnlineNow) Color(0xFF10B981) else Color(0xFF94A3B8),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = candidate.presenceLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VistaFontFamily,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }

                // Username
                if (candidate.username.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@${candidate.username}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = VistaFontFamily,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                // Bio
                if (candidate.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = candidate.bio,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = VistaFontFamily,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Chips
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (candidate.gender.isNotBlank()) {
                        InfoChip(
                            icon = "people",
                            label = if (candidate.gender.lowercase() in listOf("male", "مرد")) "آقا" else "خانم",
                        )
                    }
                    if (candidate.maritalStatus.isNotBlank()) {
                        InfoChip(
                            icon = "star",
                            label = if (candidate.maritalStatus.lowercase() in listOf("single", "مجرد")) "مجرد" else "متأهل",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenderBadge(gender: String) {
    val isMale = gender.lowercase() in listOf("male", "مرد")
    val color = if (isMale) Color(0xFF3B9AE1) else Color(0xFFE13B82)
    val glyph = if (isMale) "♂" else "♀"

    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color.copy(alpha = 0.9f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = VistaFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp,
            ),
        )
    }
}

@Composable
private fun InfoChip(
    icon: String,
    label: String,
) {
    val imageVector = if (icon == "people") Icons.Rounded.People else Icons.Rounded.Star
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = VistaFontFamily,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
            )
        }
    }
}
