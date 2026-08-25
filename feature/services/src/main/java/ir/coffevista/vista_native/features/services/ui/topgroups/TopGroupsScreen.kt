package ir.coffevista.vista_native.features.services.ui.topgroups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.theme.vistaColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.ServicesRepository
import ir.coffevista.vista_native.features.services.data.TopGroup

sealed interface TopGroupsUiState {
    data object Loading : TopGroupsUiState
    data object Empty : TopGroupsUiState
    data class Success(val groups: List<TopGroup>) : TopGroupsUiState
    data class Error(val message: String) : TopGroupsUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopGroupsScreen(
    repository: ServicesRepository,
    onBack: () -> Unit,
    onGroupClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf<TopGroupsUiState>(TopGroupsUiState.Loading) }

    LaunchedEffect(Unit) {
        state = TopGroupsUiState.Loading
        runCatching {
            repository.getTopGroups()
        }.onSuccess { groups ->
            state = if (groups.isEmpty()) TopGroupsUiState.Empty else TopGroupsUiState.Success(groups)
        }.onFailure { error ->
            state = TopGroupsUiState.Error(error.message ?: "خطا در دریافت لیست گروه‌ها")
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "برترین گروه‌ها",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (val uiState = state) {
                    is TopGroupsUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                color = VistaBrandColors.Indigo,
                            )
                        }
                    }
                    is TopGroupsUiState.Empty -> {
                        EmptyTopGroupsState()
                    }
                    is TopGroupsUiState.Error -> {
                        ErrorTopGroupsState(
                            message = uiState.message,
                            onRetry = {
                                state = TopGroupsUiState.Loading
                            },
                        )
                    }
                    is TopGroupsUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                        ) {
                            item {
                                HeroCompetitionBanner(
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
                                )
                            }

                            itemsIndexed(uiState.groups, key = { _, group -> group.id }) { index, group ->
                                TopGroupItemRow(
                                    group = group,
                                    rankIndex = index,
                                    onClick = { onGroupClick(group.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCompetitionBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = VistaBrandColors.Indigo.copy(alpha = 0.3f),
                spotColor = VistaBrandColors.Indigo.copy(alpha = 0.4f),
            ),
        shape = RoundedCornerShape(20.dp),
        color = VistaBrandColors.Indigo,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep),
                    ),
                )
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "رقابت محبوب‌ترین‌ها",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "گروه خود را بسازید و با جذب کاربران فعال و ویژه، به صدر جدول برسید!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = VistaFontFamily,
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TopGroupItemRow(
    group: TopGroup,
    rankIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTop3 = rankIndex < 3
    val rankColor = when (rankIndex) {
        0 -> Color(0xFFF59E0B) // Gold
        1 -> Color(0xFFC0C0C0) // Silver
        2 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.vistaColors.contentSecondary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isTop3) 1.5.dp else 1.dp,
            color = if (isTop3) rankColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Rank Number
            Text(
                text = "#${rankIndex + 1}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = if (isTop3) 18.sp else 16.sp,
                    color = rankColor,
                ),
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Avatar
            if (!group.image.isNullOrBlank()) {
                AsyncImage(
                    model = group.image,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(VistaBrandColors.Indigo.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.People,
                        contentDescription = null,
                        tint = VistaBrandColors.Indigo,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.People,
                        contentDescription = null,
                        tint = MaterialTheme.vistaColors.contentSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${group.memberCount} عضو",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = VistaFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.vistaColors.contentSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${group.score}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = VistaFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800),
                        ),
                    )
                }
            }

            // Badges
            if (group.premiumCount > 0 || group.verifiedCount > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    if (group.verifiedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${group.verifiedCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VistaFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFF3B82F6),
                                    ),
                                )
                            }
                        }
                    }
                    if (group.premiumCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFA855F7).copy(alpha = 0.12f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${group.premiumCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = VistaFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFFA855F7),
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

@Composable
private fun EmptyTopGroupsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.People,
            contentDescription = null,
            tint = MaterialTheme.vistaColors.contentSecondary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "گروه عمومی‌ای یافت نشد",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = VistaFontFamily,
                fontSize = 16.sp,
                color = MaterialTheme.vistaColors.contentSecondary,
            ),
        )
    }
}

@Composable
private fun ErrorTopGroupsState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "خطا در دریافت لیست گروه‌ها",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = VistaFontFamily,
                color = MaterialTheme.vistaColors.contentSecondary,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text(
                text = "تلاش مجدد",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = VistaFontFamily,
                    color = VistaBrandColors.Indigo,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}
