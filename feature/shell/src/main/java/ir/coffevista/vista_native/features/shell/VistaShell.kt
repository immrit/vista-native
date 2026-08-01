package ir.coffevista.vista_native.features.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.hilt.navigation.compose.hiltViewModel
import ir.coffevista.vista_native.core.designsystem.component.VistaAvatar
import ir.coffevista.vista_native.core.designsystem.component.VistaBadge
import ir.coffevista.vista_native.core.designsystem.component.VistaBottomSheet
import ir.coffevista.vista_native.core.designsystem.component.VistaButton
import ir.coffevista.vista_native.core.designsystem.component.VistaButtonVariant
import ir.coffevista.vista_native.core.designsystem.component.VistaDialog
import ir.coffevista.vista_native.core.designsystem.component.VistaDivider
import ir.coffevista.vista_native.core.designsystem.component.VistaEmptyState
import ir.coffevista.vista_native.core.designsystem.component.VistaMediaCard
import ir.coffevista.vista_native.core.designsystem.component.VistaSurface
import ir.coffevista.vista_native.core.designsystem.component.VistaTextField
import ir.coffevista.vista_native.core.designsystem.tokens.VistaLayout
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSpacing
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import ir.coffevista.vista_native.features.profile.ui.OwnProfileScreen
import ir.coffevista.vista_native.features.profile.ui.OtherUserProfileScreen
import ir.coffevista.vista_native.features.profile.ui.ProfilePostUiModel
import ir.coffevista.vista_native.features.profile.ui.ProfilePostsPresentationState
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.ui.FeedScreen
import ir.coffevista.vista_native.features.feed.ui.PostDetailScreen
import ir.coffevista.vista_native.features.feed.ui.ProfilePostsViewModel
import ir.coffevista.vista_native.features.search.ui.SearchLauncherScreen
import ir.coffevista.vista_native.features.search.ui.SearchWorkspaceScreen
import kotlinx.coroutines.launch

@Composable
fun VistaShell(
    context: AuthenticatedContext,
    deepLinkRequest: ShellDeepLinkRequest?,
    onDeepLinkConsumed: (Long) -> Unit,
    onLogout: () -> Unit,
    onExitRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = backStackEntry?.destination?.toShellTab() ?: ShellTab.Feed
    val atRoot = backStackEntry?.destination?.isTabRoot() != false
    val showBottomIsland = atRoot ||
        backStackEntry?.destination?.route in setOf(
            ShellRoutes.SearchWorkspace,
            ShellRoutes.ServicesDetailRoute,
            ShellRoutes.ChatDetailRoute,
        )
    var lastExitRequestAt by rememberSaveable { mutableStateOf(0L) }
    var lastDeepLinkId by rememberSaveable { mutableStateOf(0L) }

    fun selectTab(tab: ShellTab) {
        val startDestinationId = navController.graph.findStartDestination().id
        navController.navigate(tab.graphRoute()) {
            popUpTo(startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(deepLinkRequest?.deliveryId) {
        val request = deepLinkRequest ?: return@LaunchedEffect
        if (request.deliveryId == lastDeepLinkId) return@LaunchedEffect


        when (request.kind) {
            ShellDeferredKind.POST, ShellDeferredKind.GROUP -> {
                selectTab(ShellTab.Feed)
                navController.navigate(ShellRoutes.postDetail(request.reference))
            }
            ShellDeferredKind.PROFILE -> {
                if (request.reference == context.userId) {
                    selectTab(ShellTab.Profile)
                } else {
                    navController.navigate(ShellRoutes.userProfile(request.reference))
                }
            }
            ShellDeferredKind.CHAT -> {
                selectTab(ShellTab.Chat)
                navController.navigate(ShellRoutes.chatDetail(request.reference))
            }
        }

        lastDeepLinkId = request.deliveryId
        onDeepLinkConsumed(request.deliveryId)
    }

    BackHandler(enabled = atRoot) {
        when (shellBackAction(selectedTab, nested = false)) {
            ShellBackAction.SelectFeed -> selectTab(ShellTab.Feed)
            ShellBackAction.RequestExit -> {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastExitRequestAt <= 2_000L) {
                    onExitRequested()
                } else {
                    lastExitRequestAt = now
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "برای خروج دوباره بازگشت را بزنید",
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
            ShellBackAction.PopNested -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = ShellRoutes.FeedGraph,
            modifier = Modifier.fillMaxSize(),
        ) {
            navigation(route = ShellRoutes.FeedGraph, startDestination = ShellRoutes.FeedRoot) {
                composable(ShellRoutes.FeedRoot) {
                    FeedScreen(
                        viewModel = hiltViewModel(),
                        onPostClick = { postId ->
                            navController.navigate(ShellRoutes.postDetail(postId))
                        },
                        onAuthorClick = { userId ->
                            if (userId == context.userId) {
                                selectTab(ShellTab.Profile)
                            } else {
                                navController.navigate(ShellRoutes.userProfile(userId))
                            }
                        },
                    )
                }
            }
            navigation(route = ShellRoutes.SearchGraph, startDestination = ShellRoutes.SearchRoot) {
                composable(ShellRoutes.SearchRoot) {
                    SearchLauncherScreen(
                        viewModel = hiltViewModel(),
                        onOpenWorkspace = {
                            navController.navigate(ShellRoutes.SearchWorkspace)
                        },
                    )
                }
                composable(ShellRoutes.SearchWorkspace) {
                    SearchWorkspaceScreen(
                        viewModel = hiltViewModel(),
                        onUserClick = { user ->
                            navController.navigate(
                                if (user.id == context.userId) {
                                    ShellRoutes.OwnProfileOverlay
                                } else {
                                    ShellRoutes.userProfile(user.id)
                                },
                            )
                        },
                        onPostClick = { post ->
                            navController.navigate(ShellRoutes.postDetail(post.id))
                        },
                    )
                }
            }
            navigation(route = ShellRoutes.ServicesGraph, startDestination = ShellRoutes.ServicesRoot) {
                composable(ShellRoutes.ServicesRoot) {
                    ServicesPlaceholderScreen(onDetails = { navController.navigate(ShellRoutes.servicesDetail("foundation")) })
                }
                composable(ShellRoutes.ServicesDetailRoute) { ControlledDetailScreen(ShellTab.Services) }
            }
            navigation(route = ShellRoutes.ChatGraph, startDestination = ShellRoutes.ChatRoot) {
                composable(ShellRoutes.ChatRoot) {
                    ChatPlaceholderScreen(onDetails = { navController.navigate(ShellRoutes.chatDetail("foundation")) })
                }
                composable(ShellRoutes.ChatDetailRoute) { ControlledDetailScreen(ShellTab.Chat) }
            }
            navigation(route = ShellRoutes.ProfileGraph, startDestination = ShellRoutes.ProfileRoot) {
                composable(ShellRoutes.ProfileRoot) {
                    val postsViewModel = hiltViewModel<ProfilePostsViewModel>()
                    val postsState by postsViewModel.uiState.collectAsStateWithLifecycle()
                    LaunchedEffect(context.userId) {
                        postsViewModel.bind(context.userId)
                    }
                    OwnProfileScreen(
                        viewModel = hiltViewModel(),
                        onLogout = onLogout,
                        postsState = postsState.toPresentationState(),
                        onPostsRefresh = postsViewModel::refresh,
                        onPostsLoadMore = postsViewModel::loadMore,
                        onPostClick = { postId ->
                            navController.navigate(ShellRoutes.postDetail(postId))
                        },
                    )
                }
                composable(ShellRoutes.ProfileDetailRoute) { ControlledDetailScreen(ShellTab.Profile) }
            }
            composable(
                route = ShellRoutes.PostDetailRoute,
                arguments = listOf(navArgument("reference") { type = NavType.StringType }),
            ) {
                PostDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAuthorClick = { userId ->
                        navController.navigate(
                            if (userId == context.userId) {
                                ShellRoutes.OwnProfileOverlay
                            } else {
                                ShellRoutes.userProfile(userId)
                            },
                        )
                    },
                    viewModel = hiltViewModel(),
                )
            }
            composable(
                route = ShellRoutes.UserProfileRoute,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { profileEntry ->
                val userId = profileEntry.arguments?.getString("userId").orEmpty()
                val postsViewModel = hiltViewModel<ProfilePostsViewModel>()
                val postsState by postsViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(userId) {
                    if (userId.isNotBlank()) postsViewModel.bind(userId)
                }
                OtherUserProfileScreen(
                    viewModel = hiltViewModel(),
                    onBack = { navController.popBackStack() },
                    onSelfProfile = {
                        navController.navigate(ShellRoutes.OwnProfileOverlay)
                    },
                    postsState = postsState.toPresentationState(),
                    onPostsRefresh = postsViewModel::refresh,
                    onPostsLoadMore = postsViewModel::loadMore,
                    onPostClick = { postId ->
                        navController.navigate(ShellRoutes.postDetail(postId))
                    },
                )
            }
            composable(ShellRoutes.OwnProfileOverlay) {
                val postsViewModel = hiltViewModel<ProfilePostsViewModel>()
                val postsState by postsViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(context.userId) {
                    postsViewModel.bind(context.userId)
                }
                OwnProfileScreen(
                    viewModel = hiltViewModel(),
                    onLogout = onLogout,
                    postsState = postsState.toPresentationState(),
                    onPostsRefresh = postsViewModel::refresh,
                    onPostsLoadMore = postsViewModel::loadMore,
                    onPostClick = { postId ->
                        navController.navigate(ShellRoutes.postDetail(postId))
                    },
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (showBottomIsland) 126.dp else 20.dp),
        )
        if (showBottomIsland) {
            VistaBottomIsland(
                selectedTab = selectedTab,
                onSelect = ::selectTab,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun VistaBottomIsland(
    selectedTab: ShellTab,
    onSelect: (ShellTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val bottomInset = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp + bottomInset)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    .35f to background.copy(alpha = .55f),
                    .65f to background.copy(alpha = .88f),
                    1f to background,
                ),
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp + bottomInset)
                .fillMaxWidth()
                .height(62.dp)
                .shadow(10.dp, RoundedCornerShape(30.dp)),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
            border = BorderStroke(
                .5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                ShellTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
                    if (tab == ShellTab.Services) {
                        Surface(
                            onClick = { onSelect(tab) },
                            modifier = Modifier
                                .testTag("shell-tab-${tab.name.lowercase()}")
                                .width(64.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) activeColor else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                inactiveColor
                            },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                VistaNavigationIcon(
                                    tab = tab,
                                    selected = selected,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        inactiveColor
                                    },
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .testTag("shell-tab-${tab.name.lowercase()}")
                                .size(54.dp)
                                .clickable { onSelect(tab) },
                            contentAlignment = Alignment.Center,
                        ) {
                            VistaNavigationIcon(
                                tab = tab,
                                selected = selected,
                                color = if (selected) activeColor else inactiveColor,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ServicesPlaceholderScreen(onDetails: () -> Unit) {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    PlaceholderLayout(
        title = "زیرساخت خدمات آماده است",
        message = "هیچ سرویس محصولی در DSN-01 پیاده‌سازی نشده است.",
        onDetails = onDetails,
    ) {
        VistaButton(
            onClick = { showInfo = true },
            variant = VistaButtonVariant.Outline,
            modifier = Modifier.padding(top = VistaSpacing.Large),
        ) { Text("درباره این placeholder") }
    }
    VistaBottomSheet(
        visible = showInfo,
        title = "محدوده DSN-01",
        onDismiss = { showInfo = false },
    ) {
        Text("این تب فقط مالکیت navigation و Design System را اثبات می‌کند.")
    }
}

@Composable
private fun ChatPlaceholderScreen(onDetails: () -> Unit) {
    PlaceholderLayout(
        title = "زیرساخت گفت‌وگو آماده است",
        message = "پیام، WebSocket و repository خارج از محدوده این فاز هستند.",
        onDetails = onDetails,
    )
}

@Composable
private fun PlaceholderLayout(
    title: String,
    message: String,
    onDetails: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(VistaLayout.ScreenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VistaEmptyState(title = title, message = message)
        content()
        VistaButton(
            onClick = onDetails,
            variant = VistaButtonVariant.Text,
            modifier = Modifier.padding(top = VistaSpacing.Medium),
        ) { Text("بررسی back stack کنترل‌شده") }
    }
}

@Composable
private fun ControlledDetailScreen(tab: ShellTab) {
    VistaSurface(
        modifier = Modifier
            .fillMaxSize()
            .padding(VistaLayout.ScreenHorizontal),
    ) {
        Column(
            modifier = Modifier.padding(VistaLayout.ScreenHorizontal),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${tab.labelFa}: مقصد داخلی کنترل‌شده")
            Text(
                "این مقصد فقط برای اثبات back stack مستقل است.",
                modifier = Modifier.padding(top = VistaSpacing.Small),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun NavDestination.toShellTab(): ShellTab = when {
    hierarchy.any { it.route == ShellRoutes.FeedGraph } -> ShellTab.Feed
    hierarchy.any { it.route == ShellRoutes.SearchGraph } -> ShellTab.Search
    hierarchy.any { it.route == ShellRoutes.ServicesGraph } -> ShellTab.Services
    hierarchy.any { it.route == ShellRoutes.ChatGraph } -> ShellTab.Chat
    hierarchy.any { it.route == ShellRoutes.ProfileGraph } -> ShellTab.Profile
    else -> ShellTab.Feed
}

private fun NavDestination.isTabRoot(): Boolean =
    route == ShellRoutes.FeedRoot ||
        route == ShellRoutes.SearchRoot ||
        route == ShellRoutes.ServicesRoot ||
        route == ShellRoutes.ChatRoot ||
        route == ShellRoutes.ProfileRoot

private fun ShellTab.graphRoute(): String = when (this) {
    ShellTab.Feed -> ShellRoutes.FeedGraph
    ShellTab.Search -> ShellRoutes.SearchGraph
    ShellTab.Services -> ShellRoutes.ServicesGraph
    ShellTab.Chat -> ShellRoutes.ChatGraph
    ShellTab.Profile -> ShellRoutes.ProfileGraph
}

private fun ShellTab.rootRoute(): String = when (this) {
    ShellTab.Feed -> ShellRoutes.FeedRoot
    ShellTab.Search -> ShellRoutes.SearchRoot
    ShellTab.Services -> ShellRoutes.ServicesRoot
    ShellTab.Chat -> ShellRoutes.ChatRoot
    ShellTab.Profile -> ShellRoutes.ProfileRoot
}

private fun ir.coffevista.vista_native.features.feed.ui.ProfilePostsUiState
    .toPresentationState() = ProfilePostsPresentationState(
    posts = posts.map(FeedPost::toProfilePostUiModel),
    isInitialLoading = isInitialLoading,
    isRefreshing = isRefreshing,
    isAppending = isAppending,
    isOffline = isOffline,
    hasMore = hasMore,
    error = error,
    appendError = appendError,
)

private fun FeedPost.toProfilePostUiModel() = ProfilePostUiModel(
    id = id,
    userId = userId,
    authorFullName = authorFullName,
    authorUsername = authorUsername,
    authorAvatarUrl = authorAvatarUrl,
    authorIsVerified = authorIsVerified,
    content = content,
    imageUrl = videoThumbnailUrl ?: primaryImageUrl,
    videoUrl = videoUrl,
    aspectRatio = aspectRatio?.toFloatOrNull(),
    likeCount = likeCount,
    commentCount = commentCount,
    hideLikeCount = hideLikeCount,
    hideCommentCount = hideCommentCount,
    isLiked = isLiked,
    isSaved = isSaved,
    createdAt = createdAt,
)
