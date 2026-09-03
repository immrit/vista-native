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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.launch

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
    var lastExitRequestAt by rememberSaveable { mutableStateOf(0L) }
    var lastDeepLinkId by rememberSaveable { mutableStateOf(0L) }
    var activeChatTitle by rememberSaveable { mutableStateOf("پیام‌ها") }

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
                navController.navigate(ShellRoutes.chatDetail(request.reference, request.secondaryReference))
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
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
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
                        {
                            navController.navigate(ShellRoutes.StoryCreate)
                        },
                        {
                            navController.navigate(ShellRoutes.Notifications)
                        },
                        { postId -> navController.navigate(ShellRoutes.appeal(postId)) },
                        { tag -> navController.navigate(ShellRoutes.hashtag(tag)) },
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
                    )
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
                        )
                    } else {
                        navController.popBackStack()
                    }
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
                            { url -> navController.navigate(ShellRoutes.servicesWeb(url, "پیوند استوری")) },
                        )
                    }
                }
                composable(ShellRoutes.StoryCreate) {
                    val storyCreate = content.storyCreate
                    if (storyCreate != null) {
                        storyCreate(
                            { navController.popBackStack() },
                            { navController.popBackStack() },
                        )
                    }
                }
                composable(ShellRoutes.AddPost) {
                    val addPost = content.addPost
                    if (addPost != null) {
                        addPost(
                            { navController.popBackStack() },
                            { uri -> navController.navigate(ShellRoutes.videoTrimmer(uri.toString())) },
                            { navController.popBackStack() },
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
                    val decodedUri = java.net.URLDecoder.decode(rawUri, "UTF-8")
                    val videoTrimmer = content.videoTrimmer
                    if (videoTrimmer != null && decodedUri.isNotBlank()) {
                        videoTrimmer(
                            android.net.Uri.parse(decodedUri),
                            { navController.popBackStack() },
                            { trimmedFile ->
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("trimmed_video_path", trimmedFile.absolutePath)
                                navController.popBackStack()
                            },
                        )
                    }
                }
            }
            navigation(route = ShellRoutes.SearchGraph, startDestination = ShellRoutes.SearchRoot) {
                composable(ShellRoutes.SearchRoot) {
                    content.searchRoot(
                        {
                            navController.navigate(ShellRoutes.SearchWorkspace)
                        },
                        {
                            navController.navigate(ShellRoutes.QrScanner)
                        },
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
                        { url, title -> navController.navigate(ShellRoutes.servicesWeb(url, title)) },
                        { route ->
                            when (route.lowercase()) {
                                "vista://nearby" -> navController.navigate(ShellRoutes.ServicesNearby)
                                "vista://game", "vista://games" -> navController.navigate(ShellRoutes.ServicesGameLaunch)
                                "vista://group", "vista://groups" -> navController.navigate(ShellRoutes.ServicesTopGroups)
                                "vista://contact", "vista://contacts" -> navController.navigate(ShellRoutes.ServicesContacts)
                                else -> navController.navigate(ShellRoutes.servicesDetail(route))
                            }
                        },
                    )
                }
                composable(ShellRoutes.ServicesNearby) {
                    content.servicesNearby(
                        { navController.popBackStack() },
                        { navController.navigate(ShellRoutes.ServicesNearbyLikes) },
                        { userId, _ -> navController.navigate(ShellRoutes.userProfile(userId)) },
                        { _, otherUserId, _, _ -> navController.navigate(ShellRoutes.chatDetail(otherUserId)) },
                    )
                }
                composable(ShellRoutes.ServicesNearbyLikes) {
                    content.servicesNearbyLikes(
                        { navController.popBackStack() },
                        { _, otherUserId, _, _ -> navController.navigate(ShellRoutes.chatDetail(otherUserId)) },
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
                        { groupId -> navController.navigate(ShellRoutes.servicesDetail("group/$groupId")) },
                    )
                }
                composable(ShellRoutes.ServicesGameLaunch) {
                    content.servicesGameLaunch(
                        { navController.popBackStack() },
                        { url, title ->
                            navController.navigate(ShellRoutes.servicesWeb(url, title)) {
                                popUpTo(ShellRoutes.ServicesGameLaunch) { inclusive = true }
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
                    ) { userId -> navController.navigate(ShellRoutes.userProfile(userId)) }
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (showBottomIsland) 126.dp else 20.dp),
        )
        if (showBottomIsland) {
            VistaBottomIsland(
                selectedTab = selectedTab,
                chatBadgeCount = content.chatBadgeCount,
                onSelect = ::selectTab,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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
            window.statusBarColor = shellBackground.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = if (lightBars) {
                Color.White.toArgb()
            } else {
                shellBackground.toArgb()
            }
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
        val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
        Surface(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp + bottomInset)
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.16f),
                ),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (isLight) .90f else .85f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isLight) 0.65f else 0.22f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    ),
                ),
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
                                .height(48.dp)
                                .semantics {
                                    contentDescription = tab.labelFa
                                    role = Role.Tab
                                    this.selected = selected
                                },
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
                                .clickable { onSelect(tab) }
                                .semantics {
                                    contentDescription = tab.labelFa
                                    role = Role.Tab
                                    this.selected = selected
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box {
                                VistaNavigationIcon(
                                    tab = tab,
                                    selected = selected,
                                    color = if (selected) activeColor else inactiveColor,
                                    modifier = Modifier.size(30.dp),
                                )
                                if (tab == ShellTab.Chat && chatBadgeCount > 0) {
                                    VistaBadge(
                                        label = if (chatBadgeCount > 9) "۹+" else chatBadgeCount.toString(),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .testTag("shell-chat-badge"),
                                    )
                                }
                            }
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
