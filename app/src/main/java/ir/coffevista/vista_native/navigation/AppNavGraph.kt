package ir.coffevista.vista_native.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.coffevista.vista_native.R
import ir.coffevista.vista_native.features.auth.AuthScreen
import ir.coffevista.vista_native.features.auth.AuthViewModel
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.presentation.ConversationListRoute
import ir.coffevista.vista_native.features.chat.presentation.MessageDetailRoute
import ir.coffevista.vista_native.features.chat.presentation.newmessage.NewMessageRoute
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthenticationStateOwner
import ir.coffevista.vista_native.features.auth.AuthVisuals
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import ir.coffevista.vista_native.features.feed.ui.CommentsViewModel
import ir.coffevista.vista_native.features.feed.ui.FeedScreen
import ir.coffevista.vista_native.features.feed.ui.PostDetailScreen
import ir.coffevista.vista_native.features.feed.ui.ProfilePostsViewModel
import ir.coffevista.vista_native.features.profile.ui.OtherUserProfileScreen
import ir.coffevista.vista_native.features.profile.ui.OwnProfileScreen
import ir.coffevista.vista_native.features.profile.ui.OwnProfileViewModel
import ir.coffevista.vista_native.features.profile.ui.OwnProfileUiState
import ir.coffevista.vista_native.features.profile.ui.ProfilePostUiModel
import ir.coffevista.vista_native.features.profile.ui.ProfilePostsPresentationState
import ir.coffevista.vista_native.features.profile.data.UserProfileRepository
import ir.coffevista.vista_native.features.profile.settings.SettingsScreen
import ir.coffevista.vista_native.features.profile.settings.editprofile.EditProfileScreen
import ir.coffevista.vista_native.features.profile.settings.pricing.PricingScreen
import ir.coffevista.vista_native.features.profile.settings.privacy.PrivacySecurityScreen
import ir.coffevista.vista_native.features.profile.settings.privacy.ActiveSessionsScreen
import ir.coffevista.vista_native.features.profile.settings.privacy.BlockedUsersScreen
import ir.coffevista.vista_native.features.profile.settings.notifications.NotificationSettingsScreen
import ir.coffevista.vista_native.features.profile.settings.appearance.ThemeSettingsScreen
import ir.coffevista.vista_native.features.profile.settings.datastorage.DataStorageSettingsScreen
import ir.coffevista.vista_native.features.profile.settings.saved.SavedPostsScreen
import ir.coffevista.vista_native.features.profile.settings.password.ChangePasswordScreen
import ir.coffevista.vista_native.features.profile.settings.verification.VerificationRequestScreen
import ir.coffevista.vista_native.features.profile.settings.about.AboutSettingsScreen
import ir.coffevista.vista_native.features.profile.settings.about.VistaAboutSlideshowScreen
import ir.coffevista.vista_native.features.profile.settings.about.ContactUsScreen
import ir.coffevista.vista_native.features.profile.settings.about.FAQScreen
import ir.coffevista.vista_native.features.profile.settings.about.TermsAndConditionsScreen
import ir.coffevista.vista_native.features.profile.settings.about.PrivacyPolicyScreen
import ir.coffevista.vista_native.features.search.data.SearchRepository
import ir.coffevista.vista_native.features.search.ui.SearchLauncherScreen
import ir.coffevista.vista_native.features.search.ui.SearchWorkspaceScreen
import ir.coffevista.vista_native.features.shell.ShellDeepLinkRequest
import ir.coffevista.vista_native.features.shell.ShellDeferredKind
import ir.coffevista.vista_native.features.shell.ShellFeatureContent
import ir.coffevista.vista_native.features.shell.VistaShell
import ir.coffevista.vista_native.features.onboarding.OnboardingSlide
import ir.coffevista.vista_native.features.onboarding.OnboardingScreen
import ir.coffevista.vista_native.features.onboarding.OnboardingViewModel
import ir.coffevista.vista_native.features.startup.MaintenanceScreen
import ir.coffevista.vista_native.features.startup.StartupDestination
import ir.coffevista.vista_native.features.startup.StartupScreen
import ir.coffevista.vista_native.features.startup.StartupViewModel
import ir.coffevista.vista_native.ui.components.VistaBrandAsset
import ir.coffevista.vista_native.ui.components.VistaBrandMark
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.core.security.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import ir.coffevista.vista_native.features.services.data.ServicesRepository
import ir.coffevista.vista_native.features.services.ui.ServicesScreen
import ir.coffevista.vista_native.features.services.ui.contacts.ContactsScreen
import ir.coffevista.vista_native.features.services.ui.game.GameLaunchScreen
import ir.coffevista.vista_native.features.services.ui.topgroups.TopGroupsScreen
import ir.coffevista.vista_native.features.services.ui.web.InAppWebScreen
import ir.coffevista.vista_native.features.services.ui.nearby.NearbyScreen
import ir.coffevista.vista_native.features.services.ui.nearby.NearbyLikesScreen

@Composable
fun VistaApp(
    authenticationStateOwner: AuthenticationStateOwner,
    deepLinkCoordinator: DeepLinkCoordinator,
    sessionStore: SessionStore,
    feedRepository: FeedRepository,
    userProfileRepository: UserProfileRepository,
    searchRepository: SearchRepository,
    chatRepository: ChatRepository,
    servicesRepository: ServicesRepository,
    onExitRequested: () -> Unit,
) {
    val navController = rememberNavController()
    val authState by authenticationStateOwner.state.collectAsStateWithLifecycle()
    val deepLinkState by deepLinkCoordinator.state.collectAsStateWithLifecycle()
    var shellDeepLinkRequest by remember { mutableStateOf<ShellDeepLinkRequest?>(null) }
    val applicationScope = rememberCoroutineScope()
    val authVisuals = AuthVisuals(
        backIcon = painterResource(R.drawable.ic_auth_arrow_back),
        personIcon = painterResource(R.drawable.ic_person_outline),
        lockIcon = painterResource(R.drawable.ic_lock_outline),
        visibilityIcon = painterResource(R.drawable.ic_visibility),
        visibilityOffIcon = painterResource(R.drawable.ic_visibility_off),
    )
    val onboardingSlides = listOf(
        OnboardingSlide(
            kicker = "به ویستا خوش آمدی",
            title = "آدم‌ها را نزدیک‌تر ببین",
            description = "گفت‌وگو، پست و لحظه‌های واقعی؛ همه در فضایی ساخته‌شده برای ارتباط‌های معنادار.",
            accent = VistaBrandColors.Indigo,
            accentDeep = VistaBrandColors.IndigoDeep,
            image = painterResource(R.drawable.viu_connect),
            imageScale = 1.23f,
        ),
        OnboardingSlide(
            kicker = "برای هر لحظه",
            title = "لحظه‌هایت را زنده کن",
            description = "از عکس و ویدیو تا استوری و موسیقی؛ خلاقیتت را ساده و با حال‌وهوای خودت به اشتراک بگذار.",
            accent = VistaBrandColors.Pink,
            accentDeep = VistaBrandColors.PinkDeep,
            image = painterResource(R.drawable.viu_create),
            imageScale = 1.1f,
        ),
        OnboardingSlide(
            kicker = "با خیال راحت",
            title = "فضای تو، انتخاب تو",
            description = "حریم خصوصی و گفت‌وگوهای امن، با کنترل‌هایی که همیشه در دست خودت می‌مانند.",
            accent = VistaBrandColors.Violet,
            accentDeep = VistaBrandColors.VioletDeep,
            image = painterResource(R.drawable.viu_private),
            imageScale = 1.16f,
        ),
    )

    LaunchedEffect(deepLinkState) {
        when (val delivery = deepLinkState) {
            is DeepLinkDeliveryState.Ready -> {
                shellDeepLinkRequest = ShellDeepLinkRequest(
                    deliveryId = delivery.id,
                    kind = ShellDeferredKind.valueOf(delivery.destination.kind.name),
                    reference = delivery.destination.reference,
                    secondaryReference = delivery.destination.secondaryReference,
                )
                navController.navigate(AppRoute.AuthenticatedBoundary) {
                    launchSingleTop = true
                }
            }
            is DeepLinkDeliveryState.Failure -> {
                navController.navigate(AppRoute.DeepLinkFailure(delivery.reason)) {
                    launchSingleTop = true
                }
                deepLinkCoordinator.consume(delivery.id)
            }
            DeepLinkDeliveryState.Idle,
            is DeepLinkDeliveryState.PendingSession,
            is DeepLinkDeliveryState.PendingFailure,
            is DeepLinkDeliveryState.PendingAuthentication,
            -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Startup,
    ) {
        composable<AppRoute.Startup> { startupEntry ->
            val startupViewModel: StartupViewModel = hiltViewModel()
            val state by startupViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.destination) {
                val destination = state.destination
                val route: AppRoute? = when (destination) {
                    StartupDestination.Loading,
                    is StartupDestination.RecoverableError,
                    -> null
                    StartupDestination.Maintenance -> AppRoute.Maintenance
                    StartupDestination.Onboarding -> AppRoute.Onboarding
                    StartupDestination.Authentication -> {
                        authenticationStateOwner.signOut()
                        AppRoute.Authentication
                    }
                    is StartupDestination.Authenticated -> {
                        authenticationStateOwner.accept(destination.context)
                        if (destination.context.passwordRequired) {
                            AppRoute.Authentication
                        } else {
                            AppRoute.AuthenticatedBoundary
                        }
                    }
                }
                if (route != null) {
                    if (navController.currentBackStackEntry?.id != startupEntry.id) {
                        return@LaunchedEffect
                    }
                    navController.navigate(route) {
                        popUpTo<AppRoute.Startup> { inclusive = true }
                        launchSingleTop = true
                    }
                    when {
                        destination is StartupDestination.Authenticated &&
                            !destination.context.passwordRequired -> {
                            deepLinkCoordinator.onSessionResolved(authenticated = true)
                        }
                        destination == StartupDestination.Onboarding ||
                            destination == StartupDestination.Authentication -> {
                            deepLinkCoordinator.onSessionResolved(authenticated = false)
                        }
                    }
                }
            }

            StartupScreen(
                state = state,
                onRetry = startupViewModel::retry,
                accentColor = VistaBrandColors.Indigo,
                brand = { modifier ->
                    VistaBrandMark(
                        modifier = modifier,
                        asset = VistaBrandAsset.SPLASH,
                    )
                },
            )
        }

        composable<AppRoute.Maintenance> {
            MaintenanceScreen(
                onRetry = {
                    navController.navigate(AppRoute.Startup) {
                        popUpTo<AppRoute.Maintenance> { inclusive = true }
                    }
                },
            )
        }

        composable<AppRoute.Onboarding> {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val state by onboardingViewModel.state.collectAsStateWithLifecycle()
            OnboardingScreen(
                state = state,
                onAction = onboardingViewModel::onAction,
                slides = onboardingSlides,
                brand = { modifier -> VistaBrandMark(modifier) },
                onCompleted = {
                    navController.navigate(AppRoute.Authentication) {
                        popUpTo<AppRoute.Onboarding> { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<AppRoute.Authentication> {
            val startInPasswordSetup =
                (authState as? AuthenticationState.SignedIn)?.context?.passwordRequired == true
            val authViewModel: AuthViewModel = hiltViewModel()
            LaunchedEffect(startInPasswordSetup) {
                if (startInPasswordSetup) {
                    authViewModel.requirePasswordSetup()
                }
            }
            val state by authViewModel.state.collectAsStateWithLifecycle()
            AuthScreen(
                state = state,
                onAction = authViewModel::onAction,
                visuals = authVisuals,
                onAuthenticated = {
                    navController.navigate(AppRoute.AuthenticatedBoundary) {
                        popUpTo<AppRoute.Authentication> { inclusive = true }
                        launchSingleTop = true
                    }
                    deepLinkCoordinator.onAuthenticationChanged(authenticated = true)
                },
            )
        }

        composable<AppRoute.AuthenticatedBoundary> {
            val signedIn = authState as? AuthenticationState.SignedIn
            if (signedIn == null) {
                val signedOut = authState == AuthenticationState.SignedOut
                LaunchedEffect(signedOut) {
                    val destination = if (signedOut) {
                        AppRoute.Authentication
                    } else {
                        AppRoute.Startup
                    }
                    navController.navigate(destination) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            } else {
                VistaShell(
                    context = signedIn.context,
                    deepLinkRequest = shellDeepLinkRequest,
                    onDeepLinkConsumed = { id ->
                        deepLinkCoordinator.consume(id)
                        shellDeepLinkRequest = null
                    },
                    onLogout = {
                        applicationScope.launch {
                            // Revoke the shared session before feature cleanup so no
                            // repository can rehydrate credentials or start new work
                            // while its account-scoped cache is being removed.
                            sessionStore.clear()
                            runCatching {
                                feedRepository.clearAccount(signedIn.context.userId)
                            }
                            runCatching {
                                userProfileRepository.clearAccount(signedIn.context.userId)
                            }
                            runCatching {
                                searchRepository.clearHistory(signedIn.context.userId)
                            }
                            runCatching {
                                chatRepository.clearAccount(signedIn.context.userId)
                            }
                            withContext(Dispatchers.Main.immediate) {
                                authenticationStateOwner.signOut()
                                shellDeepLinkRequest = null
                            }
                        }
                    },
                    onExitRequested = onExitRequested,
                    content = ShellFeatureContent(
                        feedRoot = { onPostClick, onAuthorClick, onCreatePost, onOpenStoryPlayer, onCreateStory ->
                            FeedScreen(
                                viewModel = hiltViewModel(),
                                commentsViewModel = hiltViewModel<CommentsViewModel>(),
                                onPostClick = onPostClick,
                                onAuthorClick = onAuthorClick,
                                onCreatePostClick = onCreatePost,
                                onOpenStoryPlayer = onOpenStoryPlayer,
                                onCreateStory = onCreateStory,
                            )
                        },
                        storyPlayer = { initialUserIndex, onClose, onOpenProfile, onOpenLink ->
                            ir.coffevista.vista_native.features.stories.ui.player.StoryPlayerScreen(
                                initialUserIndex = initialUserIndex,
                                onClose = onClose,
                                onOpenProfile = onOpenProfile,
                                onOpenLink = onOpenLink,
                            )
                        },
                        storyCreate = { onClose, onStoryPublished ->
                            ir.coffevista.vista_native.features.stories.ui.editor.StoryEditorScreen(
                                onClose = onClose,
                                onStoryPublished = onStoryPublished,
                            )
                        },
                        addPost = { onBack, onNavigateToTrimmer, onPostCreated ->
                            ir.coffevista.vista_native.features.feed.ui.create.AddPostScreen(
                                onBack = onBack,
                                onNavigateToTrimmer = onNavigateToTrimmer,
                                onPostCreated = onPostCreated,
                            )
                        },
                        videoTrimmer = { videoUri, onBack, onTrimComplete ->
                            ir.coffevista.vista_native.features.feed.ui.create.VideoTrimmerScreen(
                                videoUri = videoUri,
                                onBack = onBack,
                                onTrimComplete = onTrimComplete,
                            )
                        },
                        searchRoot = { onOpenWorkspace, onOpenQrScanner ->
                            SearchLauncherScreen(
                                viewModel = hiltViewModel(),
                                onOpenWorkspace = onOpenWorkspace,
                                onOpenQrScanner = onOpenQrScanner,
                            )
                        },
                        searchWorkspace = { onUserClick, onPostClick ->
                            SearchWorkspaceScreen(
                                viewModel = hiltViewModel(),
                                onUserClick = { onUserClick(it.id) },
                                onPostClick = { onPostClick(it.id) },
                            )
                        },
                        ownProfile = { onPostClick, onSettingsClick, logout, onOpenFollowers, onOpenQrScanner ->
                            val postsViewModel = hiltViewModel<ProfilePostsViewModel>()
                            val postsState by postsViewModel.uiState.collectAsStateWithLifecycle()
                            LaunchedEffect(signedIn.context.userId) {
                                postsViewModel.bind(signedIn.context.userId)
                            }
                            OwnProfileScreen(
                                viewModel = hiltViewModel(),
                                onLogout = logout,
                                onSettingsClick = onSettingsClick,
                                onOpenFollowers = onOpenFollowers,
                                onOpenQrScanner = onOpenQrScanner,
                                postsState = postsState.toPresentationState(),
                                onPostsRefresh = postsViewModel::refresh,
                                onPostsLoadMore = postsViewModel::loadMore,
                                onPostClick = onPostClick,
                                onLikeClick = postsViewModel::toggleLike,
                                onSaveClick = postsViewModel::toggleSave,
                            )
                        },
                        settingsRoot = { onBack, onEditProfile, onPremium, onAccountDetails, onPrivacySecurity, onNotifications, onAppearance, onDataStorage, onSavedPosts, onChangePassword, onVerificationRequest, onTerms, onAbout, onLogout ->
                            val profileViewModel = hiltViewModel<OwnProfileViewModel>()
                            val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                            val profileDto = (profileState as? OwnProfileUiState.Content)?.profile
                            SettingsScreen(
                                profile = profileDto,
                                onBack = onBack,
                                onEditProfileClick = onEditProfile,
                                onPremiumClick = onPremium,
                                onAccountDetailsClick = onAccountDetails,
                                onPrivacySecurityClick = onPrivacySecurity,
                                onNotificationsClick = onNotifications,
                                onAppearanceClick = onAppearance,
                                onDataStorageClick = onDataStorage,
                                onSavedPostsClick = onSavedPosts,
                                onChangePasswordClick = onChangePassword,
                                onVerificationRequestClick = onVerificationRequest,
                                onTermsClick = onTerms,
                                onAboutClick = onAbout,
                                onLogout = onLogout,
                            )
                        },
                        editProfile = { onBack ->
                            val profileViewModel = hiltViewModel<OwnProfileViewModel>()
                            val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                            val profileDto = (profileState as? OwnProfileUiState.Content)?.profile
                            EditProfileScreen(
                                profile = profileDto,
                                onBack = onBack,
                                onSave = profileViewModel::updateProfile,
                                onAvatarSelected = profileViewModel::updateAvatar,
                                onAvatarRemoved = profileViewModel::removeAvatar,
                            )
                        },
                        pricingPage = { onBack ->
                            PricingScreen(onBack = onBack)
                        },
                        privacySecurity = { onBack, onBlockedUsers, onActiveSessions ->
                            val profileViewModel = hiltViewModel<OwnProfileViewModel>()
                            val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                            val profile = (profileState as? OwnProfileUiState.Content)?.profile
                            PrivacySecurityScreen(
                                profile = profile,
                                onBack = onBack,
                                onBlockedUsersClick = onBlockedUsers,
                                onActiveSessionsClick = onActiveSessions,
                            )
                        },
                        activeSessions = { onBack ->
                            ActiveSessionsScreen(onBack = onBack)
                        },
                        blockedUsers = { onBack ->
                            BlockedUsersScreen(onBack = onBack)
                        },
                        notificationSettings = { onBack ->
                            NotificationSettingsScreen(onBack = onBack)
                        },
                        themeSettings = { onBack ->
                            ThemeSettingsScreen(onBack = onBack)
                        },
                        dataStorage = { onBack ->
                            DataStorageSettingsScreen(onBack = onBack)
                        },
                        savedPosts = { onBack, onPostClick ->
                            SavedPostsScreen(
                                onBack = onBack,
                                onPostClick = onPostClick,
                            )
                        },
                        changePassword = { onBack ->
                            ChangePasswordScreen(onBack = onBack)
                        },
                        verificationRequest = { onBack ->
                            VerificationRequestScreen(onBack = onBack)
                        },
                        termsConditions = { onBack ->
                            TermsAndConditionsScreen(onBack = onBack)
                        },
                        aboutSettings = { onBack, onSlideshow, onTerms, onContactUs, onPrivacyPolicy, onFaq ->
                            AboutSettingsScreen(
                                onBack = onBack,
                                onSlideshowClick = onSlideshow,
                                onTermsClick = onTerms,
                                onContactUsClick = onContactUs,
                                onPrivacyPolicyClick = onPrivacyPolicy,
                                onFaqClick = onFaq,
                            )
                        },
                        aboutSlideshow = { onClose ->
                            VistaAboutSlideshowScreen(onClose = onClose)
                        },
                        contactUs = { onBack, onOpenSupportConversation ->
                            ContactUsScreen(
                                onBack = onBack,
                                onOpenSupportConversation = onOpenSupportConversation,
                            )
                        },
                        privacyPolicy = { onBack ->
                            PrivacyPolicyScreen(onBack = onBack)
                        },
                        faqPage = { onBack ->
                            FAQScreen(onBack = onBack)
                        },
                        postDetail = { onBack, onAuthorClick ->
                            PostDetailScreen(
                                onBack = onBack,
                                onAuthorClick = onAuthorClick,
                                viewModel = hiltViewModel(),
                                commentsViewModel = hiltViewModel<CommentsViewModel>(),
                            )
                        },
                        userProfile = { userId, onBack, onSelfProfile, onPostClick, onOpenFollowers ->
                            val postsViewModel = hiltViewModel<ProfilePostsViewModel>()
                            val postsState by postsViewModel.uiState.collectAsStateWithLifecycle()
                            LaunchedEffect(userId) {
                                if (userId.isNotBlank()) postsViewModel.bind(userId)
                            }
                            OtherUserProfileScreen(
                                viewModel = hiltViewModel(),
                                onBack = onBack,
                                onSelfProfile = onSelfProfile,
                                onOpenFollowers = onOpenFollowers,
                                postsState = postsState.toPresentationState(),
                                onPostsRefresh = postsViewModel::refresh,
                                onPostsLoadMore = postsViewModel::loadMore,
                                onPostClick = onPostClick,
                                onLikeClick = postsViewModel::toggleLike,
                                onSaveClick = postsViewModel::toggleSave,
                            )
                        },
                        followersFollowing = { targetUserId, initialTab, onBack, onOpenProfile ->
                            ir.coffevista.vista_native.features.profile.ui.follow.FollowersFollowingScreen(
                                userId = targetUserId,
                                initialTab = initialTab,
                                onBack = onBack,
                                onOpenProfile = onOpenProfile,
                            )
                        },
                        qrScanner = { onBack, onUserFound ->
                            ir.coffevista.vista_native.features.profile.ui.qr.ProfileQrScannerScreen(
                                onBack = onBack,
                                onUserFound = onUserFound,
                            )
                        },
                        servicesRoot = { onNearby, onGame, onTopGroups, onContacts, onUserProfile, onWeb, onSectionRoute ->
                            ServicesScreen(
                                onNavigateToNearby = onNearby,
                                onNavigateToGame = onGame,
                                onNavigateToTopGroups = onTopGroups,
                                onNavigateToContacts = onContacts,
                                onNavigateToUserProfile = onUserProfile,
                                onNavigateToWeb = onWeb,
                                onNavigateToSectionRoute = onSectionRoute,
                            )
                        },
                        servicesContacts = { onBack, onUserClick ->
                            ContactsScreen(
                                onBack = onBack,
                                onContactClick = onUserClick,
                            )
                        },
                        servicesTopGroups = { onBack, onGroupClick ->
                            TopGroupsScreen(
                                repository = servicesRepository,
                                onBack = onBack,
                                onGroupClick = onGroupClick,
                            )
                        },
                        servicesGameLaunch = { onBack, onLaunchSuccess ->
                            GameLaunchScreen(
                                repository = servicesRepository,
                                onBack = onBack,
                                onLaunchSuccess = onLaunchSuccess,
                            )
                        },
                        servicesNearby = { onBack, onOpenLikes, onOpenProfile, onOpenChat ->
                            NearbyScreen(
                                onBack = onBack,
                                onOpenLikes = onOpenLikes,
                                onOpenProfile = onOpenProfile,
                                onOpenChat = onOpenChat,
                            )
                        },
                        servicesNearbyLikes = { onBack, onOpenChat ->
                            NearbyLikesScreen(
                                onBack = onBack,
                                onOpenChat = onOpenChat,
                            )
                        },
                        servicesWeb = { url, title, onBack ->
                            InAppWebScreen(
                                url = url,
                                title = title,
                                onBack = onBack,
                                restrictHost = "coffevista.ir",
                                appBarColor = androidx.compose.ui.graphics.Color(0xFF0A3D6B),
                                appBarForegroundColor = androidx.compose.ui.graphics.Color.White,
                                useBackButton = true,
                            )
                        },
                        chatRoot = { onConversationClick, onNewMessage ->
                            ConversationListRoute(
                                viewModel = hiltViewModel(),
                                onOpenConversation = { conversation ->
                                    onConversationClick(conversation.id, conversation.title)
                                },
                                onNewMessage = onNewMessage,
                            )
                        },
                        chatNewMessage = { onBack, onOpenConversation ->
                            NewMessageRoute(
                                viewModel = hiltViewModel(),
                                onBack = onBack,
                                onOpenConversation = onOpenConversation,
                            )
                        },
                        chatDetail = { conversationId, messageId, title, onBack, onOpenProfile ->
                            MessageDetailRoute(
                                conversationId = conversationId,
                                initialMessageId = messageId,
                                title = title,
                                viewModel = hiltViewModel(),
                                onBack = onBack,
                                onOpenProfile = onOpenProfile,
                            )
                        },
                    ),
                )
            }
        }

        composable<AppRoute.DeferredFeature> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.DeferredFeature>()
            DeferredDestinationScreen(
                kind = route.kind,
                onBack = navController::popBackStack,
            )
        }

        composable<AppRoute.DeepLinkFailure> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.DeepLinkFailure>()
            DeepLinkFailureScreen(
                reason = route.reason,
                onBack = navController::popBackStack,
            )
        }
    }
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
    authorVerificationType = authorVerificationType,
    content = content,
    imageUrl = videoThumbnailUrl ?: primaryImageUrl,
    videoUrl = videoUrl,
    aspectRatio = aspectRatio?.toFloatOrNull(),
    musicUrl = musicUrl,
    musicTitle = musicTitle,
    viewsCount = 0L,
    likeCount = likeCount,
    commentCount = commentCount,
    hideLikeCount = hideLikeCount,
    hideCommentCount = hideCommentCount,
    isLiked = isLiked,
    isSaved = isSaved,
    createdAt = createdAt,
)
