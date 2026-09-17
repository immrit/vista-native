package ir.coffevista.vista_native.features.services.ui.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.theme.vistaColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.nearby.NearbyLikeResult
import ir.coffevista.vista_native.features.services.data.nearby.NearbyMatch
import ir.coffevista.vista_native.features.services.data.nearby.NearbyReceivedLike
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyLikesScreen(
    onBack: () -> Unit,
    onOpenChat: (conversationId: String, otherUserId: String, username: String, avatarUrl: String) -> Unit,
    viewModel: NearbyViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var matchedResult by remember { mutableStateOf<Pair<NearbyReceivedLike, NearbyLikeResult>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadLikesAndMatches()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "لایک‌ها و مَچ‌ها",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = VistaBrandColors.Indigo,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = VistaBrandColors.Indigo,
                        )
                    },
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "لایک‌های دریافتی (${state.receivedLikes.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VistaFontFamily,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                ),
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "مَچ‌ها (${state.matches.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VistaFontFamily,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                ),
                            )
                        },
                    )
                }

                if (state.isLoadingLikesMatches) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            color = VistaBrandColors.Indigo,
                        )
                    }
                } else {
                    when (selectedTab) {
                        0 -> ReceivedLikesTabContent(
                            likes = state.receivedLikes,
                            onLike = { like ->
                                viewModel.respondToReceivedLike(
                                    like = like,
                                    action = "like",
                                    onSuccess = { matched, matchId ->
                                        if (matched && matchId != null) {
                                            matchedResult = like to NearbyLikeResult(
                                                matched = true,
                                                matchId = matchId,
                                                match = NearbyMatch(
                                                    matchId = matchId,
                                                    userId = like.userId,
                                                    username = like.username,
                                                    fullName = like.fullName,
                                                    avatarUrl = like.avatarUrl,
                                                    isVerified = like.isVerified,
                                                    verificationType = like.verificationType,
                                                    matchedAt = "",
                                                ),
                                            )
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar("لایک شد ✓") }
                                        }
                                    },
                                    onError = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                )
                            },
                            onPass = { like ->
                                viewModel.respondToReceivedLike(
                                    like = like,
                                    action = "pass",
                                    onError = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                )
                            },
                        )
                        1 -> MatchesTabContent(
                            matches = state.matches,
                            onChat = { match ->
                                onOpenChat(match.matchId, match.userId, match.fullName, match.avatarUrl)
                            },
                            onUnmatch = { viewModel.unmatch(it.matchId) },
                        )
                    }
                }
            }

            matchedResult?.let { (like, result) ->
                NearbyMatchDialog(
                    matchResult = result,
                    candidate = null,
                    onDismiss = { matchedResult = null },
                    onStartChat = { matchId, otherUserId, username, avatarUrl ->
                        matchedResult = null
                        onOpenChat(matchId, otherUserId, username, avatarUrl)
                    },
                )
            }
        }
    }
}

@Composable
private fun ReceivedLikesTabContent(
    likes: List<NearbyReceivedLike>,
    onLike: (NearbyReceivedLike) -> Unit,
    onPass: (NearbyReceivedLike) -> Unit,
) {
    if (likes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = VistaBrandColors.Indigo.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "هنوز کسی لایکت نکرده!\nبا کاوش بیشتر، دیده می‌شی",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VistaFontFamily,
                        color = MaterialTheme.vistaColors.contentSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    ),
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(likes, key = { it.userId }) { like ->
                ReceivedLikeItem(like = like, onLike = { onLike(like) }, onPass = { onPass(like) })
            }
        }
    }
}

@Composable
private fun ReceivedLikeItem(
    like: NearbyReceivedLike,
    onLike: () -> Unit,
    onPass: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (like.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = like.avatarUrl,
                    contentDescription = like.fullName,
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
                    Text(
                        text = like.fullName.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = VistaBrandColors.Indigo,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = like.fullName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (like.action == "superlike") "سوپرلایکت کرده ⭐" else "لایکت کرده",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = VistaFontFamily,
                        color = MaterialTheme.vistaColors.contentSecondary,
                        fontSize = 12.sp,
                    ),
                )
            }

            IconButton(onClick = onPass) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "رد کردن",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(22.dp),
                )
            }

            IconButton(onClick = onLike) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "پسندیدن",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun MatchesTabContent(
    matches: List<NearbyMatch>,
    onChat: (NearbyMatch) -> Unit,
    onUnmatch: (NearbyMatch) -> Unit,
) {
    if (matches.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "هنوز مَچی نداری!\nبا کاوش در «اطراف من» شروع کن",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VistaFontFamily,
                        color = MaterialTheme.vistaColors.contentSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    ),
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(matches, key = { it.matchId }) { match ->
                MatchItemRow(match = match, onChat = { onChat(match) }, onUnmatch = { onUnmatch(match) })
            }
        }
    }
}

@Composable
private fun MatchItemRow(
    match: NearbyMatch,
    onChat: () -> Unit,
    onUnmatch: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (match.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = match.avatarUrl,
                    contentDescription = match.fullName,
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
                    Text(
                        text = match.fullName.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = VistaBrandColors.Indigo,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.fullName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (match.username.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@${match.username}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = VistaFontFamily,
                            color = MaterialTheme.vistaColors.contentSecondary,
                            fontSize = 12.sp,
                        ),
                    )
                }
            }

            IconButton(onClick = onChat) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Chat,
                    contentDescription = "گفتگو",
                    tint = VistaBrandColors.Indigo,
                    modifier = Modifier.size(22.dp),
                )
            }

            IconButton(onClick = onUnmatch) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "حذف مچ",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
