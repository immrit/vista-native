package ir.coffevista.vista_native.features.shell

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ripple
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import ir.coffevista.vista_native.core.designsystem.component.LocalBottomBarVisibility
import ir.coffevista.vista_native.core.designsystem.component.LocalHomeTabClick
import ir.coffevista.vista_native.core.designsystem.component.LocalMiniPlayerSlot
import ir.coffevista.vista_native.core.designsystem.component.LocalNavigateToPremium
import ir.coffevista.vista_native.core.designsystem.component.VistaMiniPlayerSlot
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.view.WindowInsetsCompat
import ir.coffevista.vista_native.core.model.navigation.VistaGameUrlHelper
import ir.coffevista.vista_native.core.designsystem.navigation.LocalVistaInternalLinkNavigator
import ir.coffevista.vista_native.core.designsystem.navigation.VistaInternalLinkNavigator
import androidx.navigation.NavController
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
import ir.coffevista.vista_native.core.designsystem.component.VistaAvatar
import ir.coffevista.vista_native.core.designsystem.component.VistaBadge
import ir.coffevista.vista_native.core.designsystem.component.VistaBadgedBox
import ir.coffevista.vista_native.core.designsystem.component.VistaBottomSheet
import ir.coffevista.vista_native.core.designsystem.component.VistaButton
import ir.coffevista.vista_native.core.designsystem.component.VistaButtonVariant
import ir.coffevista.vista_native.core.designsystem.component.VistaDialog
import ir.coffevista.vista_native.core.designsystem.component.VistaDivider
import ir.coffevista.vista_native.core.designsystem.component.VistaEmptyState
import ir.coffevista.vista_native.core.designsystem.component.VistaMediaCard
import ir.coffevista.vista_native.core.designsystem.component.VistaNotificationBadge
import ir.coffevista.vista_native.core.designsystem.component.VistaSurface
import ir.coffevista.vista_native.core.designsystem.component.VistaTextField
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassBlurSupported
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassHalo
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassSource
import ir.coffevista.vista_native.core.designsystem.tokens.VistaLayout
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSpacing
import ir.coffevista.vista_native.core.model.session.AuthenticatedContext
import kotlinx.coroutines.launch

/** Saved-state key under which the video trimmer returns its output file to the Add Post entry. */
private const val TRIMMED_VIDEO_PATH_KEY = "trimmed_video_path"

@Composable
fun VistaShell(
    context: AuthenticatedContext,
    deepLinkRequest: ShellDeepLinkRequest?,
    onDeepLinkConsumed: (Long) -> Unit,
    onLogout: () -> Unit,
    onExitRequested: () -> Unit,
    content: ShellFeatureContent,
    modifier: Modifier = Modifier,
) {
    VistaShellSystemBars()
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
        )
    val isBottomIslandVisibleState = rememberSaveable { mutableStateOf(true) }
    var isBottomIslandVisible by isBottomIslandVisibleState

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val androidContext = LocalContext.current

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, _, _ ->
            runCatching {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                val imm = androidContext.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val activity = androidContext as? Activity
                activity?.currentFocus?.let { view ->
                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                    view.clearFocus()
                }
                if (activity?.window != null) {
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                        .hide(WindowInsetsCompat.Type.ime())
                }
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    LaunchedEffect(selectedTab, backStackEntry?.destination?.route) {
        isBottomIslandVisible = true
    }
    var lastExitRequestAt by rememberSaveable { mutableStateOf(0L) }
    var lastDeepLinkId by rememberSaveable { mutableStateOf(0L) }
    var activeChatTitle by rememberSaveable { mutableStateOf("پیام‌ها") }

    fun selectTab(tab: ShellTab) {
        runCatching {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
        val startDestinationId = navController.graph.findStartDestination().id
        navController.navigate(tab.graphRoute()) {
            popUpTo(startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToGame(targetPath: String? = null, switchToServicesTab: Boolean = false) {
        val safePath = VistaGameUrlHelper.sanitizeGameTargetPath(targetPath)
        if (switchToServicesTab) {
            selectTab(ShellTab.Services)
        }
        val hasOpenWebView = runCatching {
            navController.currentBackStack.value.any { it.destination.route == ShellRoutes.ServicesWebRoute }
        }.getOrDefault(false)
        if (hasOpenWebView) {
            navController.popBackStack(ShellRoutes.ServicesWebRoute, inclusive = true)
        }
        navController.navigate(ShellRoutes.servicesGameLaunch(safePath)) {
            launchSingleTop = true
        }
    }

    val uriHandler = LocalUriHandler.current
    val internalLinkNavigator = remember(navController, context.userId) {
        VistaInternalLinkNavigator { rawUrl ->
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return@VistaInternalLinkNavigator false

            val gamePath = VistaGameUrlHelper.parseGameTargetPath(trimmed)
            if (gamePath != null) {
                navigateToGame(targetPath = gamePath, switchToServicesTab = false)
                return@VistaInternalLinkNavigator true
            }

            val parsed = runCatching { java.net.URI(trimmed) }.getOrNull()
                ?: return@VistaInternalLinkNavigator false
            val scheme = parsed.scheme?.lowercase().orEmpty()
            val host = parsed.host?.lowercase().orEmpty()
            val segments = parsed.path.orEmpty().split('/').filter(String::isNotBlank)

            when (scheme) {
                "vista" -> {
                    val target = segments.firstOrNull()
                    when (host) {
                        "post" -> if (!target.isNullOrBlank()) {
                            navController.navigate(ShellRoutes.postDetail(target))
                            true
                        } else false
                        "appeal" -> if (!target.isNullOrBlank()) {
                            navController.navigate(ShellRoutes.appeal(target))
                            true
                        } else false
                        "profile" -> if (!target.isNullOrBlank()) {
                            if (target == context.userId) selectTab(ShellTab.Profile)
                            else navController.navigate(ShellRoutes.userProfile(target))
                            true
                        } else false
                        "group", "chat" -> if (!target.isNullOrBlank()) {
                            selectTab(ShellTab.Chat)
                            navController.navigate(ShellRoutes.chatDetail(target))
                            true
                        } else false
                        else -> false
                    }
                }
                "https", "http" -> {
                    if (!VistaGameUrlHelper.isVistaWebHost(host)) return@VistaInternalLinkNavigator false
                    val head = segments.firstOrNull()?.lowercase()
                    val ref = segments.getOrNull(1)
                    when {
                        head == "post" && !ref.isNullOrBlank() -> {
                            navController.navigate(ShellRoutes.postDetail(ref))
                            true
                        }
                        (head == "profile" || head == "u") && !ref.isNullOrBlank() -> {
                            if (ref == context.userId) selectTab(ShellTab.Profile)
                            else navController.navigate(ShellRoutes.userProfile(ref))
                            true
                        }
                        (head == "group" || head == "join") && !ref.isNullOrBlank() -> {
                            selectTab(ShellTab.Chat)
                            navController.navigate(ShellRoutes.chatDetail(ref))
                            true
                        }
                        else -> {
                            navController.navigate(ShellRoutes.servicesWeb(trimmed, "ویستا"))
                            true
                        }
                    }
                }
                else -> false
            }
        }
    }

    LaunchedEffect(deepLinkRequest?.deliveryId) {
        val request = deepLinkRequest ?: return@LaunchedEffect
        if (request.deliveryId == lastDeepLinkId) return@LaunchedEffect


        when (request.kind) {
            ShellDeferredKind.POST -> {
                if (request.reference == "share") {
                    selectTab(ShellTab.Feed)
                    navController.navigate(ShellRoutes.AddPost)
                } else if (request.reference == "share_story") {
                    selectTab(ShellTab.Feed)
                    navController.navigate(ShellRoutes.storyCreate(null))
                } else if (request.secondaryReference == "appeal") {
                    selectTab(ShellTab.Feed)
                    navController.navigate(ShellRoutes.appeal(request.reference))
                } else {
                    selectTab(ShellTab.Feed)
                    navController.navigate(ShellRoutes.postDetail(request.reference))
                }
            }
            ShellDeferredKind.GROUP -> {
                selectTab(ShellTab.Chat)
                navController.navigate(ShellRoutes.chatDetail(request.reference))
            }
            ShellDeferredKind.PROFILE -> {
                if (request.reference == "payment_callback") {
                    selectTab(ShellTab.Profile)
                    navController.navigate(ShellRoutes.PricingPage)
                } else if (request.reference == context.userId) {
                    selectTab(ShellTab.Profile)
                } else {
                    navController.navigate(ShellRoutes.userProfile(request.reference))
                }
            }
            ShellDeferredKind.CHAT -> {
                selectTab(ShellTab.Chat)
                if (request.reference == "share") {
                    navController.navigate(ShellRoutes.ChatNewMessage) {
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(ShellRoutes.chatDetail(request.reference, request.secondaryReference))
                }
            }
            ShellDeferredKind.GAME -> {
                navigateToGame(targetPath = request.reference, switchToServicesTab = true)
            }
            ShellDeferredKind.NOTIFICATIONS -> {
                selectTab(ShellTab.Feed)
                if (request.reference == "suggestions") {
                    selectTab(ShellTab.Search)
                    navController.navigate(ShellRoutes.SearchWorkspace)
                } else {
                    navController.navigate(ShellRoutes.Notifications) {
                        launchSingleTop = true
                    }
                }
            }
            ShellDeferredKind.NEARBY -> {
                selectTab(ShellTab.Services)
                if (request.reference == "likes") {
                    navController.navigate(ShellRoutes.ServicesNearbyLikes) {
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(ShellRoutes.ServicesNearby) {
                        launchSingleTop = true
                    }
                }
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

    val homeTabClickFlow = remember { kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val glassState = rememberVistaGlassState()

    CompositionLocalProvider(
        LocalBottomBarVisibility provides { isBottomIslandVisible = it },
        LocalHomeTabClick provides homeTabClickFlow,
        LocalVistaInternalLinkNavigator provides internalLinkNavigator,
        LocalNavigateToPremium provides { navController.navigate(ShellRoutes.PricingPage) },
    ) {
        val isMiniPlayerActive = content.isMiniPlayerActive?.invoke() == true
        val miniPlayerExtraHeight by animateDpAsState(
            targetValue = if (isMiniPlayerActive) 47.dp else 0.dp,
            animationSpec = tween(220),
            label = "mini-player-extra-height",
        )
        val openChatFromMiniPlayer: (String, String?) -> Unit = remember(navController) {
            { conversationId, messageId ->
                if (conversationId.isNotBlank()) {
                    selectTab(ShellTab.Chat)
                    navController.navigate(ShellRoutes.chatDetail(conversationId, messageId))
                }
            }
        }
        val openPostFromMiniPlayer: (String) -> Unit = remember(navController) {
            { postId ->
                if (postId.isNotBlank()) {
                    navController.navigate(ShellRoutes.postDetail(postId))
                }
            }
        }
        val miniPlayerSlot = VistaMiniPlayerSlot(
            extraHeight = miniPlayerExtraHeight,
        ) { slotGlassState, slotModifier, slotShape ->
            content.miniPlayerContent?.invoke(
                slotGlassState,
                slotModifier,
                slotShape,
                openChatFromMiniPlayer,
                openPostFromMiniPlayer,
            )
        }
        Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalMiniPlayerSlot provides miniPlayerSlot) {
        NavHost(
            navController = navController,
            startDestination = ShellRoutes.FeedGraph,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(180))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(180))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(180))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(180))
            },
            modifier = Modifier
                .fillMaxSize()
                .vistaGlassSource(glassState)
                .background(MaterialTheme.colorScheme.background),
        ) {
            navigation(route = ShellRoutes.FeedGraph, startDestination = ShellRoutes.FeedRoot) {
                composable(ShellRoutes.FeedRoot) {
                    content.feedRoot(
                        { postId ->
                            navController.navigate(ShellRoutes.postDetail(postId))
                        },
                        { userId ->
                            if (userId == context.userId) {
                                selectTab(ShellTab.Profile)
                            } else {
                                navController.navigate(ShellRoutes.userProfile(userId))
                            }
                        },
                        {
                            navController.navigate(ShellRoutes.AddPost)
                        },
                        { userIndex ->
                            navController.navigate(ShellRoutes.storyPlayer(userIndex))
                        },
                        { uri ->
                            navController.navigate(ShellRoutes.storyCreate(uri?.toString()))
                        },
                        {
                            navController.navigate(ShellRoutes.Notifications)
                        },
                        { postId -> navController.navigate(ShellRoutes.appeal(postId)) },
                        { tag -> navController.navigate(ShellRoutes.hashtag(tag)) },
                        { postId -> navController.navigate(ShellRoutes.reels(postId)) },
                    )
                }
                composable(ShellRoutes.Notifications) {
                    content.notifications(
                        { navController.popBackStack() },
                        { postId -> navController.navigate(ShellRoutes.postDetail(postId)) },
                        { userId ->
                            if (userId == context.userId) {
                                selectTab(ShellTab.Profile)
                            } else {
                                navController.navigate(ShellRoutes.userProfile(userId))
                            }
                        },
                        { conversationId, messageId ->
                            selectTab(ShellTab.Chat)
                            navController.navigate(ShellRoutes.chatDetail(conversationId, messageId))
                        },
                        {
                            selectTab(ShellTab.Search)
                            navController.navigate(ShellRoutes.SearchWorkspace)
                        },
                        { postId -> navController.navigate(ShellRoutes.appeal(postId)) },
                    )
                }
                composable(
                    route = ShellRoutes.AppealRoute,
                    arguments = listOf(navArgument("postId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    content.appeal(
                        backStackEntry.arguments?.getString("postId").orEmpty(),
                    ) { navController.popBackStack() }
                }
                composable(
                    route = ShellRoutes.HashtagRoute,
                    arguments = listOf(navArgument("tag") { type = NavType.StringType }),
                ) { backStackEntry ->
                    content.hashtagPosts(
                        java.net.URLDecoder.decode(backStackEntry.arguments?.getString("tag").orEmpty(), "UTF-8"),
                        { navController.popBackStack() },
                        { postId -> navController.navigate(ShellRoutes.postDetail(postId)) },
                        { userId -> navController.navigate(ShellRoutes.userProfile(userId)) },
                        { uri -> navController.navigate(ShellRoutes.storyCreate(uri.toString())) },
                    )
                }
                composable(
                    route = ShellRoutes.StoryPlayerRoute,
                    arguments = listOf(
                        androidx.navigation.navArgument("userIndex") { type = androidx.navigation.NavType.IntType }
                    ),
                ) { backStackEntry ->
                    val userIndex = backStackEntry.arguments?.getInt("userIndex") ?: 0
                    val storyPlayer = content.storyPlayer
                    if (storyPlayer != null) {
                        storyPlayer(
                            userIndex,
                            { navController.popBackStack() },
                            { userId -> navController.navigate(ShellRoutes.userProfile(userId)) },
                            { url ->
                                if (!internalLinkNavigator.openUrl(url)) {
                                    runCatching { uriHandler.openUri(url) }
                                }
                            },
                            { tag -> navController.navigate(ShellRoutes.hashtag(tag)) },
                        )
                    }
                }
                composable(
                    route = ShellRoutes.StoryCreateRoute,
                    arguments = listOf(
                        navArgument("uri") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    ),
                ) { backStackEntry ->
                    val storyCreate = content.storyCreate
                    val rawUri = backStackEntry.arguments?.getString("uri")
                    val initialUri = rawUri?.takeIf { it.isNotBlank() }?.let {
                        android.net.Uri.parse(java.net.URLDecoder.decode(it, "UTF-8"))
                    }
                    if (storyCreate != null) {
                        storyCreate(
                            initialUri,
                            { navController.popBackStack() },
                            { navController.popBackStack() },
                        )
                    }
                }
                composable(ShellRoutes.AddPost) { addPostEntry ->
                    val addPost = content.addPost
                    if (addPost != null) {
                        // The trimmer hands its result back through this entry's saved state;
                        // it is forwarded to the screen explicitly because a Hilt ViewModel's
                        // SavedStateHandle is not the same object as the entry's.
                        val trimmedVideoPath by addPostEntry.savedStateHandle
                            .getStateFlow<String?>(TRIMMED_VIDEO_PATH_KEY, null)
                            .collectAsState()
                        addPost(
                            { navController.popBackStack() },
                            { uri -> navController.navigate(ShellRoutes.videoTrimmer(uri.toString())) },
                            { navController.popBackStack() },
                            trimmedVideoPath,
                            { addPostEntry.savedStateHandle[TRIMMED_VIDEO_PATH_KEY] = null },
                        )
                    }
                }
                composable(
                    route = ShellRoutes.VideoTrimmerRoute,
                    arguments = listOf(
                        androidx.navigation.navArgument("uri") { type = androidx.navigation.NavType.StringType }
                    ),
                ) { backStackEntry ->
                    val rawUri = backStackEntry.arguments?.getString("uri").orEmpty()
                    val decodedUri = runCatching { java.net.URLDecoder.decode(rawUri, "UTF-8") }.getOrDefault(rawUri)
                    val videoTrimmer = content.videoTrimmer
                    val parsedUri = runCatching { android.net.Uri.parse(decodedUri) }.getOrNull()
                    if (videoTrimmer != null && parsedUri != null && decodedUri.isNotBlank()) {
                        videoTrimmer(
                            parsedUri,
                            { navController.popBackStack() },
                            { trimmedFile ->
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(TRIMMED_VIDEO_PATH_KEY, trimmedFile.absolutePath)
                                navController.popBackStack()
                            },
                        )
                    } else if (decodedUri.isBlank() || parsedUri == null) {
                        navController.popBackStack()
                    }
                }
            }
            navigation(route = ShellRoutes.SearchGraph, startDestination = ShellRoutes.SearchRoot) {
                composable(ShellRoutes.SearchRoot) {
                    content.searchWorkspace(
                        { userId ->
                            navController.navigate(
                                if (userId == context.userId) {
                                    ShellRoutes.OwnProfileOverlay
                                } else {
                                    ShellRoutes.userProfile(userId)
                                },
                            )
                        },
                        { postId ->
                            navController.navigate(ShellRoutes.postDetail(postId))
                        },
                        { tag -> navController.navigate(ShellRoutes.hashtag(tag)) },
                        null,
                        { navController.navigate(ShellRoutes.QrScanner) },
                    )
                }
                composable(ShellRoutes.SearchWorkspace) {
                    content.searchWorkspace(
                        { userId ->
                            navController.navigate(
                                if (userId == context.userId) {
                                    ShellRoutes.OwnProfileOverlay
                                } else {
                                    ShellRoutes.userProfile(userId)
                                },
                            )
                        },
                        { postId ->
                            navController.navigate(ShellRoutes.postDetail(postId))
                        },
                        { tag -> navController.navigate(ShellRoutes.hashtag(tag)) },
                        { navController.popBackStack() },
                        { navController.navigate(ShellRoutes.QrScanner) },
                    )
                }
            }
            navigation(route = ShellRoutes.ServicesGraph, startDestination = ShellRoutes.ServicesRoot) {
                composable(ShellRoutes.ServicesRoot) {
                    content.servicesRoot(
                        { navController.navigate(ShellRoutes.ServicesNearby) },
                        { navController.navigate(ShellRoutes.ServicesGameLaunch) },
                        { navController.navigate(ShellRoutes.ServicesTopGroups) },
                        { navController.navigate(ShellRoutes.ServicesContacts) },
                        { userId -> navController.navigate(ShellRoutes.userProfile(userId)) },
                        { url, title ->
                            val gamePath = VistaGameUrlHelper.parseGameTargetPath(url)
                            if (gamePath != null) {
                                navigateToGame(targetPath = gamePath, switchToServicesTab = false)
                            } else {
                                navController.navigate(ShellRoutes.servicesWeb(url, title))
                            }
                        },
                        { route ->
                            val gamePath = VistaGameUrlHelper.parseGameTargetPath(route)
                            if (gamePath != null) {
                                navigateToGame(targetPath = gamePath, switchToServicesTab = false)
                            } else {
                                when (route.lowercase()) {
                                    "vista://nearby" -> navController.navigate(ShellRoutes.ServicesNearby)
                                    "vista://game", "vista://games" -> navController.navigate(ShellRoutes.ServicesGameLaunch)
                                    "vista://group", "vista://groups" -> navController.navigate(ShellRoutes.ServicesTopGroups)
                                    "vista://contact", "vista://contacts" -> navController.navigate(ShellRoutes.ServicesContacts)
                                    else -> navController.navigate(ShellRoutes.servicesDetail(route))
                                }
                            }
                        },
                    )
                }
                composable(ShellRoutes.ServicesNearby) {
                    content.servicesNearby(
                        { navController.popBackStack() },
                        { navController.navigate(ShellRoutes.ServicesNearbyLikes) },
                        { userId, _ -> navController.navigate(ShellRoutes.userProfile(userId)) },
                        { _, otherUserId, _, _ ->
                            selectTab(ShellTab.Chat)
                            navController.navigate(ShellRoutes.chatDetail(otherUserId))
                        },
                    )
                }
                composable(ShellRoutes.ServicesNearbyLikes) {
                    content.servicesNearbyLikes(
                        { navController.popBackStack() },
                        { _, otherUserId, _, _ ->
                            selectTab(ShellTab.Chat)
                            navController.navigate(ShellRoutes.chatDetail(otherUserId))
                        },
                    )
                }
                composable(ShellRoutes.ServicesContacts) {
                    content.servicesContacts(
                        { navController.popBackStack() },
                        { userId -> navController.navigate(ShellRoutes.userProfile(userId)) },
                    )
                }
                composable(ShellRoutes.ServicesTopGroups) {
                    content.servicesTopGroups(
                        { navController.popBackStack() },
                        { groupId ->
                            selectTab(ShellTab.Chat)
                            navController.navigate(ShellRoutes.chatDetail(groupId))
                        },
                    )
                }
                composable(
                    route = ShellRoutes.ServicesGameLaunchRoute,
                    arguments = listOf(
                        navArgument("targetPath") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    val rawTargetPath = entry.arguments?.getString("targetPath")
                    val decodedTargetPath = rawTargetPath?.takeIf { it.isNotBlank() }?.let { candidate ->
                        if (candidate.startsWith("/")) candidate
                        else runCatching { java.net.URLDecoder.decode(candidate, "UTF-8") }.getOrDefault(candidate)
                    }
                    content.servicesGameLaunch(
                        decodedTargetPath,
                        { navController.popBackStack() },
                        { url, title ->
                            navController.navigate(ShellRoutes.servicesWeb(url, title)) {
                                popUpTo(ShellRoutes.ServicesGameLaunchRoute) { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    route = ShellRoutes.ServicesWebRoute,
                    arguments = listOf(
                        navArgument("url") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val url = entry.arguments?.getString("url").orEmpty()
                    val title = entry.arguments?.getString("title").orEmpty()
                    content.servicesWeb(url, title) {
                        navController.popBackStack()
                    }
                }
                composable(ShellRoutes.ServicesDetailRoute) {
                    navController.popBackStack()
                }
            }
            navigation(route = ShellRoutes.ChatGraph, startDestination = ShellRoutes.ChatRoot) {
                composable(ShellRoutes.ChatRoot) {
                    content.chatRoot(
                            { reference, title ->
                                activeChatTitle = title.ifBlank { "پیام‌ها" }
                                navController.navigate(ShellRoutes.chatDetail(reference))
                            },
                            { navController.navigate(ShellRoutes.ChatNewMessage) },
                    )
                }
                composable(ShellRoutes.ChatNewMessage) {
                    content.chatNewMessage(navController::popBackStack) { reference, title ->
                            activeChatTitle = title.ifBlank { "پیام‌ها" }
                            navController.navigate(ShellRoutes.chatDetail(reference)) {
                                popUpTo(ShellRoutes.ChatNewMessage) { inclusive = true }
                            }
                    }
                }
                composable(
                    route = ShellRoutes.ChatDetailRoute,
                    arguments = listOf(
                        navArgument("reference") { type = NavType.StringType },
                        navArgument("messageId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    content.chatDetail(
                        entry.arguments?.getString("reference").orEmpty(),
                        entry.arguments?.getString("messageId"),
                        activeChatTitle,
                        navController::popBackStack,
                        { userId -> navController.navigate(ShellRoutes.userProfile(userId)) },
                        { conversationId, title ->
                            activeChatTitle = title.ifBlank { "گفتگوی محرمانه" }
                            navController.navigate(ShellRoutes.chatDetail(conversationId))
                        },
                        { postId -> navController.navigate(ShellRoutes.postDetail(postId)) },
                    )
                }
            }
            navigation(route = ShellRoutes.ProfileGraph, startDestination = ShellRoutes.ProfileRoot) {
                composable(ShellRoutes.ProfileRoot) {
                    content.ownProfile(
                        { postId ->
                            navController.navigate(ShellRoutes.postDetail(postId))
                        },
                        {
                            navController.navigate(ShellRoutes.SettingsRoot)
                        },
                        onLogout,
                        { userId, initialTab ->
                            navController.navigate(ShellRoutes.userFollow(userId, initialTab))
                        },
                        {
                            navController.navigate(ShellRoutes.QrScanner)
                        },
                        {
                            navController.navigate(ShellRoutes.EditProfile)
                        },
                        {
                            navController.navigate(ShellRoutes.StoryCreate)
                        },
                        { postId ->
                            navController.navigate(ShellRoutes.reels(postId))
                        },
                        {
                            navController.navigate(ShellRoutes.AddPost)
                        },
                    )
                }
                composable(ShellRoutes.SettingsRoot) {
                    content.settingsRoot(
                        { navController.popBackStack() },
                        { navController.navigate(ShellRoutes.EditProfile) },
                        { navController.navigate(ShellRoutes.PricingPage) },
                        { navController.navigate(ShellRoutes.userProfile(context.userId)) },
                        { navController.navigate(ShellRoutes.PrivacySecurity) },
                        { navController.navigate(ShellRoutes.NotificationSettings) },
                        { navController.navigate(ShellRoutes.ThemeSettings) },
                        { navController.navigate(ShellRoutes.DataStorage) },
                        { navController.navigate(ShellRoutes.SavedPosts) },
                        { navController.navigate(ShellRoutes.ChangePassword) },
                        { navController.navigate(ShellRoutes.VerificationRequest) },
                        { navController.navigate(ShellRoutes.TermsConditions) },
                        { navController.navigate(ShellRoutes.AboutSettings) },
                        onLogout,
                    )
                }
                composable(ShellRoutes.EditProfile) {
                    content.editProfile { navController.popBackStack() }
                }
                composable(ShellRoutes.PricingPage) {
                    content.pricingPage { navController.popBackStack() }
                }
                composable(ShellRoutes.BadgeStore) {
                    (content.badgeStore ?: content.pricingPage) { navController.popBackStack() }
                }
                composable(ShellRoutes.PrivacySecurity) {
                    content.privacySecurity(
                        { navController.popBackStack() },
                        { navController.navigate(ShellRoutes.BlockedUsers) },
                        { navController.navigate(ShellRoutes.ActiveSessions) },
                    )
                }
                composable(ShellRoutes.ActiveSessions) {
                    content.activeSessions { navController.popBackStack() }
                }
                composable(ShellRoutes.BlockedUsers) {
                    content.blockedUsers { navController.popBackStack() }
                }
                composable(ShellRoutes.NotificationSettings) {
                    content.notificationSettings { navController.popBackStack() }
                }
                composable(ShellRoutes.ThemeSettings) {
                    content.themeSettings { navController.popBackStack() }
                }
                composable(ShellRoutes.DataStorage) {
                    content.dataStorage { navController.popBackStack() }
                }
                composable(ShellRoutes.SavedPosts) {
                    content.savedPosts(
                        { navController.popBackStack() },
                        { postId -> navController.navigate(ShellRoutes.postDetail(postId)) },
                        { uri -> navController.navigate(ShellRoutes.storyCreate(uri.toString())) },
                    )
                }
                composable(ShellRoutes.ChangePassword) {
                    content.changePassword { navController.popBackStack() }
                }
                composable(ShellRoutes.VerificationRequest) {
                    content.verificationRequest { navController.popBackStack() }
                }
                composable(ShellRoutes.TermsConditions) {
                    content.termsConditions { navController.popBackStack() }
                }
                composable(ShellRoutes.AboutSettings) {
                    content.aboutSettings(
                        { navController.popBackStack() },
                        { navController.navigate(ShellRoutes.AboutSlideshow) },
                        { navController.navigate(ShellRoutes.TermsConditions) },
                        { navController.navigate(ShellRoutes.ContactUs) },
                        { navController.navigate(ShellRoutes.PrivacyPolicy) },
                        { navController.navigate(ShellRoutes.FAQPage) },
                        { navController.navigate(ShellRoutes.TickGuide) },
                    )
                }
                composable(ShellRoutes.TickGuide) {
                    content.tickGuide?.invoke(
                        { navController.popBackStack() },
                        { navController.navigate(ShellRoutes.PricingPage) },
                    )
                }
                composable(ShellRoutes.AboutSlideshow) {
                    content.aboutSlideshow { navController.popBackStack() }
                }
                composable(ShellRoutes.ContactUs) {
                    content.contactUs(
                        { navController.popBackStack() },
                        { conversationId, title ->
                            activeChatTitle = title.ifBlank { "پشتیبانی ویستا" }
                            selectTab(ShellTab.Chat)
                            navController.navigate(ShellRoutes.chatDetail(conversationId))
                        },
                    )
                }
                composable(ShellRoutes.PrivacyPolicy) {
                    content.privacyPolicy { navController.popBackStack() }
                }
                composable(ShellRoutes.FAQPage) {
                    content.faqPage { navController.popBackStack() }
                }
            }
            composable(
                route = ShellRoutes.ReelsRoute,
                arguments = listOf(
                    navArgument("postId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId")
                val reels = content.reels
                if (reels != null) {
                    reels(
                        postId,
                        { navController.popBackStack() },
                        { userId -> navController.navigate(ShellRoutes.userProfile(userId)) },
                        { pId -> navController.navigate(ShellRoutes.postDetail(pId)) },
                        { _ -> },
                        { uri -> navController.navigate(ShellRoutes.storyCreate(uri.toString())) },
                    )
                } else {
                    navController.popBackStack()
                }
            }
            composable(
                route = ShellRoutes.PostDetailRoute,
                arguments = listOf(navArgument("reference") { type = NavType.StringType }),
            ) {
                content.postDetail(
                    { navController.popBackStack() },
                    { userId ->
                        navController.navigate(
                            if (userId == context.userId) {
                                ShellRoutes.OwnProfileOverlay
                            } else {
                                ShellRoutes.userProfile(userId)
                            },
                        )
                    },
                    { tag -> navController.navigate(ShellRoutes.hashtag(tag)) },
                    { postId -> navController.navigate(ShellRoutes.appeal(postId)) },
                    { uri -> navController.navigate(ShellRoutes.storyCreate(uri.toString())) },
                )
            }
            composable(
                route = ShellRoutes.HashtagRoute,
                arguments = listOf(navArgument("tag") { type = NavType.StringType }),
            ) { backStackEntry ->
                content.hashtagPosts(
                    java.net.URLDecoder.decode(backStackEntry.arguments?.getString("tag").orEmpty(), "UTF-8"),
                    { navController.popBackStack() },
                    { postId -> navController.navigate(ShellRoutes.postDetail(postId)) },
                    { userId -> navController.navigate(ShellRoutes.userProfile(userId)) },
                    { uri -> navController.navigate(ShellRoutes.storyCreate(uri.toString())) },
                )
            }
            composable(
                route = ShellRoutes.UserProfileRoute,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { profileEntry ->
                val userId = profileEntry.arguments?.getString("userId").orEmpty()
                content.userProfile(
                    userId,
                    { navController.popBackStack() },
                    {
                        navController.navigate(ShellRoutes.OwnProfileOverlay)
                    },
                    { postId ->
                        navController.navigate(ShellRoutes.postDetail(postId))
                    },
                    { targetUserId, initialTab ->
                        navController.navigate(ShellRoutes.userFollow(targetUserId, initialTab))
                    },
                    { chatUserId ->
                        navController.navigate(ShellRoutes.chatDetail(chatUserId))
                    },
                    { postId ->
                        navController.navigate(ShellRoutes.reels(postId))
                    },
                )
            }
            composable(ShellRoutes.OwnProfileOverlay) {
                content.ownProfile(
                    { postId ->
                        navController.navigate(ShellRoutes.postDetail(postId))
                    },
                    {
                        navController.navigate(ShellRoutes.SettingsRoot)
                    },
                    onLogout,
                    { userId, initialTab ->
                        navController.navigate(ShellRoutes.userFollow(userId, initialTab))
                    },
                    {
                        navController.navigate(ShellRoutes.QrScanner)
                    },
                    {
                        navController.navigate(ShellRoutes.EditProfile)
                    },
                    {
                        navController.navigate(ShellRoutes.StoryCreate)
                    },
                    { postId ->
                        navController.navigate(ShellRoutes.reels(postId))
                    },
                    {
                        navController.navigate(ShellRoutes.AddPost)
                    },
                )
            }
            composable(
                route = ShellRoutes.UserFollowRoute,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("initialTab") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                ),
            ) { entry ->
                val targetUserId = entry.arguments?.getString("userId").orEmpty()
                val initialTab = entry.arguments?.getInt("initialTab") ?: 0
                content.followersFollowing(
                    targetUserId,
                    initialTab,
                    { navController.popBackStack() },
                    { clickedUserId ->
                        if (clickedUserId == context.userId) {
                            selectTab(ShellTab.Profile)
                        } else {
                            navController.navigate(ShellRoutes.userProfile(clickedUserId))
                        }
                    },
                )
            }
            composable(ShellRoutes.QrScanner) {
                content.qrScanner(
                    { navController.popBackStack() },
                    { userFound ->
                        navController.popBackStack()
                        navController.navigate(ShellRoutes.userProfile(userFound))
                    },
                )
            }
        }
        }
        val islandVisible = showBottomIsland && isBottomIslandVisible
        val snackbarBottomPadding by animateDpAsState(
            targetValue = if (islandVisible) 126.dp else 20.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "snackbarBottomPadding",
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = snackbarBottomPadding),
        )
        AnimatedVisibility(
            visible = islandVisible,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 250),
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            VistaBottomIsland(
                selectedTab = selectedTab,
                glassState = glassState,
                chatBadgeCount = content.chatBadgeCount,
                onSelect = { tab ->
                    if (tab == ShellTab.Feed) {
                        isBottomIslandVisible = true
                        if (selectedTab == ShellTab.Feed && atRoot) {
                            homeTabClickFlow.tryEmit(Unit)
                        } else {
                            selectTab(tab)
                        }
                    } else {
                        selectTab(tab)
                    }
                },
            )
        }
    }
}
}

@Composable
private fun VistaShellSystemBars() {
    val view = LocalView.current
    val shellBackground = MaterialTheme.colorScheme.background
    val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                window.isStatusBarContrastEnforced = false
                @Suppress("DEPRECATION")
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = lightBars
                isAppearanceLightNavigationBars = lightBars
            }
        }
    }
}

@Composable
private fun VistaBottomIsland(
    selectedTab: ShellTab,
    glassState: VistaGlassState,
    chatBadgeCount: Int,
    onSelect: (ShellTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val bottomInset = with(density) {
        maxOf(
            WindowInsets.navigationBars.getBottom(density),
            WindowInsets.ime.getBottom(density),
        ).toDp()
    }
    val background = MaterialTheme.colorScheme.background
    val blurSupported = rememberVistaGlassBlurSupported()
    // With real backdrop blur the content must stay visible beneath the capsule, so the legacy
    // opaque fade is toned down; without blur the original fade keeps the bar legible.
    val fadeScale = if (blurSupported) 0.68f else 1f
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val isDark = !isLight
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp + bottomInset)
            .vistaGlassHalo(
                state = glassState,
                isDark = isDark,
                blurRadius = 22.dp,
                startOffsetFraction = 0.16f,
                maxAlpha = if (isDark) 0.54f else 0.58f,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp + bottomInset)
                .fillMaxWidth()
                .height(58.dp)
                .vistaGlass(
                    state = glassState,
                    shape = RoundedCornerShape(29.dp),
                    tint = if (!isLight) Color(0xFF1C242F) else MaterialTheme.colorScheme.surface,
                    isDark = !isLight,
                    blurRadius = VistaGlassTokens.BarBlur,
                    tintAlphaOverride = if (!isLight) 0.82f else 0.85f,
                    drawHighlight = false,
                    rimWidth = 0.6.dp,
                    shadowElevation = 8.dp,
                ),
            shape = RoundedCornerShape(29.dp),
            color = Color.Transparent,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                ShellTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val tabScale by animateFloatAsState(
                        targetValue = if (selected) 1.05f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "tab_scale_${tab.name}",
                    )
                    val pillAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "tab_pill_${tab.name}",
                    )
                    val tabIconColor by animateColorAsState(
                        targetValue = if (selected) activeColor else inactiveColor,
                        animationSpec = tween(durationMillis = 180),
                        label = "tab_color_${tab.name}",
                    )
                    Box(
                        modifier = Modifier
                            .testTag("shell-tab-${tab.name.lowercase()}")
                            .graphicsLayer {
                                scaleX = tabScale
                                scaleY = tabScale
                            }
                            .width(58.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                activeColor.copy(alpha = (if (isLight) 0.12f else 0.20f) * pillAlpha),
                                RoundedCornerShape(22.dp),
                            )
                            .border(
                                width = 0.6.dp,
                                color = activeColor.copy(alpha = (if (isLight) 0.20f else 0.32f) * pillAlpha),
                                shape = RoundedCornerShape(22.dp),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = { onSelect(tab) },
                            )
                            .semantics {
                                contentDescription = tab.labelFa
                                role = Role.Tab
                                this.selected = selected
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        VistaBadgedBox(
                            badge = {
                                if (tab == ShellTab.Chat && chatBadgeCount > 0) {
                                    VistaNotificationBadge(
                                        count = chatBadgeCount,
                                        borderColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.testTag("shell-chat-badge"),
                                    )
                                }
                            }
                        ) {
                            VistaNavigationIcon(
                                tab = tab,
                                selected = selected,
                                color = tabIconColor,
                                modifier = Modifier.size(25.dp),
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
