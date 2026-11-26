package ir.coffevista.vista_native.features.services.ui.nearby

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCandidate

@Composable
fun NearbyGridView(
    candidates: List<NearbyCandidate>,
    onCandidateClick: (NearbyCandidate) -> Unit,
    onLikeClick: (NearbyCandidate) -> Unit,
    onChatRequestClick: (NearbyCandidate) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val uniqueCandidates = remember(candidates) {
        candidates.filter { it.userId.isNotBlank() }.distinctBy { it.userId }
    }

    LaunchedEffect(gridState, uniqueCandidates.size) {
        if (uniqueCandidates.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
            .distinctUntilChanged()
            .filter { lastVisible -> lastVisible >= (uniqueCandidates.size - 4).coerceAtLeast(0) }
            .collect { onLoadMore() }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        itemsIndexed(uniqueCandidates, key = { _, item -> item.userId }) { _, candidate ->
            NearbyRadarGridTile(
                candidate = candidate,
                onClick = { onCandidateClick(candidate) },
                onLike = { onLikeClick(candidate) },
                onChatRequest = { onChatRequestClick(candidate) },
            )
        }
    }
}

@Composable
private fun NearbyRadarGridTile(
    candidate: NearbyCandidate,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onChatRequest: () -> Unit,
) {
    val displayTitle = if (candidate.age > 0) "${candidate.fullName}، ${candidate.age}" else candidate.fullName

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF171928),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.84f),
            ) {
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
                                    listOf(Color(0xFF121417), VistaBrandColors.ViuBlueDeep),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = candidate.fullName.take(1).ifBlank { "؟" },
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = Color.White,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.25f),
                                    0.45f to Color.Transparent,
                                    1.0f to Color(0xFF0E101A).copy(alpha = 0.94f),
                                ),
                            ),
                        ),
                )

                // Top badges: Online + Zone
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (candidate.isOnlineNow) Color(0xFF22C55E) else Color(0xFF94A3B8)),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (candidate.isOnlineNow) "آنلاین" else "نزدیک",
                                fontFamily = VistaFontFamily,
                                fontSize = 11.sp,
                                color = Color.White,
                            )
                        }
                    }

                    if (candidate.zoneType == "same_city" || candidate.zoneType == "same_province") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (candidate.zoneType == "same_city") {
                                Color(0xFF10B981).copy(alpha = 0.85f)
                            } else {
                                VistaBrandColors.ViuBlueDeep.copy(alpha = 0.85f)
                            },
                        ) {
                            Text(
                                text = if (candidate.zoneType == "same_city") "هم‌شهری" else "هم‌استانی",
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                // Bottom overlay identity + distance
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(10.dp),
                ) {
                    if (candidate.statusText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VistaBrandColors.Coral.copy(alpha = 0.82f),
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            Text(
                                text = candidate.statusText,
                                fontFamily = VistaFontFamily,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayTitle,
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (candidate.isVerified) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }

                    if (candidate.locationLine.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            Icon(
                                Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = candidate.locationLine,
                                fontFamily = VistaFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Bottom quick action strip: Direct Chat Request + Quick Like
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = if (candidate.hasPendingChatRequest) {
                        Color(0xFF334155)
                    } else {
                        VistaBrandColors.ViuBlueDeep.copy(alpha = 0.22f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (candidate.hasPendingChatRequest) Color.White.copy(alpha = 0.1f)
                        else VistaBrandColors.ViuBlueDeep.copy(alpha = 0.55f),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clickable(enabled = !candidate.hasPendingChatRequest, onClick = onChatRequest),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    ) {
                        Icon(
                            imageVector = if (candidate.hasPendingChatRequest) Icons.Rounded.Schedule else Icons.Rounded.ChatBubbleOutline,
                            contentDescription = null,
                            tint = if (candidate.hasPendingChatRequest) Color(0xFF8A919C) else Color(0xFF1FB6E8),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (candidate.hasPendingChatRequest) "ارسال شد" else "چت",
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = if (candidate.hasPendingChatRequest) Color(0xFF94A3B8) else Color.White,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = VistaBrandColors.Coral.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, VistaBrandColors.Coral.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .size(34.dp)
                        .clickable(onClick = onLike),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = "پسند",
                            tint = Color(0xFFF472B6),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
