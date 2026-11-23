package ir.coffevista.vista_native.features.services.ui.nearby

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import ir.coffevista.vista_native.core.designsystem.component.VistaViuStateIllustration
import ir.coffevista.vista_native.core.designsystem.component.VistaViuStateKind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.theme.vistaColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.nearby.NearbyChatRequest
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
                            text = "صندوق اطراف من",
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
                                text = "درخواست چت (${state.pendingChatRequestsCount})",
                                style = MaterialTheme.typography.bodySmall.copy(
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
                                text = "لایک‌ها (${state.receivedLikes.size})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VistaFontFamily,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                ),
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = "مَچ‌ها (${state.matches.size})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VistaFontFamily,
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
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
                        0 -> ChatRequestsTabContent(
                            incoming = state.incomingChatRequests,
                            outgoing = state.outgoingChatRequests,
                            onAccept = { req ->
                                viewModel.respondToChatRequest(
                                    request = req,
                                    action = "accept",
                                    onAcceptedChat = { convId, peerId, name, avatar ->
                                        onOpenChat(convId, peerId, name, avatar)
                                    },
                                    onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                )
                            },
                            onReject = { req ->
                                viewModel.respondToChatRequest(
                                    request = req,
                                    action = "reject",
                                    onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                )
                            },
                            onCancel = { req ->
                                viewModel.respondToChatRequest(
                                    request = req,
                                    action = "cancel",
                                    onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                                )
                            },
                            onOpenAcceptedChat = { req ->
                                onOpenChat(req.conversationId, req.peerId, req.fullName, req.avatarUrl)
                            },
                        )
                        1 -> ReceivedLikesTabContent(
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
                        else -> MatchesTabContent(
                            matches = state.matches,
                            onChat = { match ->
                                viewModel.openMatchChat(match.matchId, match.userId) { convId ->
                                    onOpenChat(convId, match.userId, match.fullName, match.avatarUrl)
                                }
                            },
                            onUnmatch = { viewModel.unmatch(it.matchId) },
                        )
                    }
                }
            }

            matchedResult?.let { (_, result) ->
                NearbyMatchDialog(
                    matchResult = result,
                    candidate = null,
                    onDismiss = { matchedResult = null },
                    onStartChat = { matchId, otherUserId, username, avatarUrl ->
                        matchedResult = null
                        viewModel.openMatchChat(matchId, otherUserId) { convId ->
                            onOpenChat(convId, otherUserId, username, avatarUrl)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatRequestsTabContent(
    incoming: List<NearbyChatRequest>,
    outgoing: List<NearbyChatRequest>,
    onAccept: (NearbyChatRequest) -> Unit,
    onReject: (NearbyChatRequest) -> Unit,
    onCancel: (NearbyChatRequest) -> Unit,
    onOpenAcceptedChat: (NearbyChatRequest) -> Unit,
) {
    if (incoming.isEmpty() && outgoing.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                VistaViuStateIllustration(
                    kind = VistaViuStateKind.EmptyChat,
                    size = 136.dp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "هنوز درخواست چتی ثبت نشده است.\nاز صفحه اطراف من می‌توانید مستقیم درخواست چت بفرستید.",
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
        return
    }

    val uniqueIncoming = remember(incoming) { incoming.distinctBy { it.id } }
    val uniqueOutgoing = remember(outgoing) { outgoing.distinctBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uniqueIncoming.isNotEmpty()) {
            item {
                Text(
                    text = "درخواست‌های دریافتی (${uniqueIncoming.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            }
            itemsIndexed(uniqueIncoming, key = { idx, it -> it.id.ifBlank { "in-$idx" } }) { _, req ->
                ChatRequestCardItem(
                    req = req,
                    isIncoming = true,
                    onAccept = { onAccept(req) },
                    onReject = { onReject(req) },
                    onCancel = {},
                    onOpenChat = { onOpenAcceptedChat(req) },
                )
            }
        }

        if (uniqueOutgoing.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "درخواست‌های ارسالی شما (${uniqueOutgoing.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            }
            itemsIndexed(uniqueOutgoing, key = { idx, it -> it.id.ifBlank { "out-$idx" } }) { _, req ->
                ChatRequestCardItem(
                    req = req,
                    isIncoming = false,
                    onAccept = {},
                    onReject = {},
                    onCancel = { onCancel(req) },
                    onOpenChat = { onOpenAcceptedChat(req) },
                )
            }
        }
    }
}

@Composable
private fun ChatRequestCardItem(
    req: NearbyChatRequest,
    isIncoming: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    onOpenChat: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (req.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = req.avatarUrl,
                        contentDescription = req.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(VistaBrandColors.Indigo.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = req.fullName.firstOrNull()?.toString() ?: "؟",
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = VistaBrandColors.Indigo,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = req.fullName,
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (req.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    val subtitle = buildString {
                        if (req.cityName.isNotBlank()) append(req.cityName)
                        if (req.isOnlineNow) {
                            if (isNotEmpty()) append(" • ")
                            append("آنلاین")
                        }
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            fontFamily = VistaFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.vistaColors.contentSecondary,
                        )
                    }
                }

                val statusColor = when (req.status) {
                    "accepted" -> Color(0xFF10B981)
                    "pending" -> Color(0xFFF59E0B)
                    else -> Color(0xFF94A3B8)
                }
                val statusLabel = when (req.status) {
                    "accepted" -> "تایید شد"
                    "pending" -> "در انتظار"
                    else -> req.status
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = statusLabel,
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (req.introMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "«${req.introMessage}»",
                        fontFamily = VistaFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                req.status == "accepted" && req.conversationId.isNotBlank() -> {
                    Button(
                        onClick = onOpenChat,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Indigo),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ورود به گفتگو", fontFamily = VistaFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                isIncoming && req.status == "pending" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onAccept,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("قبول و شروع چت", fontFamily = VistaFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = onReject,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f)),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("رد", fontFamily = VistaFontFamily, fontSize = 12.sp, color = Color(0xFFEF4444))
                        }
                    }
                }

                !isIncoming && req.status == "pending" -> {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("لغو درخواست", fontFamily = VistaFontFamily, fontSize = 12.sp)
                    }
                }
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
                VistaViuStateIllustration(
                    kind = VistaViuStateKind.Empty,
                    size = 136.dp,
                )
                Spacer(modifier = Modifier.height(12.dp))
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
        val uniqueLikes = remember(likes) { likes.distinctBy { it.userId } }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(uniqueLikes, key = { idx, it -> it.userId.ifBlank { "like-$idx" } }) { _, like ->
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
                VistaViuStateIllustration(
                    kind = VistaViuStateKind.Empty,
                    size = 136.dp,
                )
                Spacer(modifier = Modifier.height(12.dp))
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
        val uniqueMatches = remember(matches) { matches.distinctBy { it.matchId } }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(uniqueMatches, key = { idx, it -> it.matchId.ifBlank { "match-$idx" } }) { _, match ->
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
