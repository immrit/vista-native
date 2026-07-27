package ir.coffevista.vista_native.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ir.coffevista.vista_native.features.auth.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthenticationStateOwner
import ir.coffevista.vista_native.features.auth.AuthVisuals
import ir.coffevista.vista_native.features.shell.ShellDeepLinkRequest
import ir.coffevista.vista_native.features.shell.ShellDeferredKind
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

@Composable
fun VistaApp(
    authenticationStateOwner: AuthenticationStateOwner,
    deepLinkCoordinator: DeepLinkCoordinator,
    sessionStore: SessionStore,
    onExitRequested: () -> Unit,
) {
    val navController = rememberNavController()
    val authState by authenticationStateOwner.state.collectAsStateWithLifecycle()
    val deepLinkState by deepLinkCoordinator.state.collectAsStateWithLifecycle()
    var shellDeepLinkRequest by remember { mutableStateOf<ShellDeepLinkRequest?>(null) }
    val authVisuals = AuthVisuals(
        accentColor = VistaBrandColors.Indigo,
        personIcon = painterResource(R.drawable.ic_person_outline),
        lockIcon = painterResource(R.drawable.ic_lock_outline),
        visibilityIcon = painterResource(R.drawable.ic_visibility),
        visibilityOffIcon = painterResource(R.drawable.ic_visibility_off),
        brand = { modifier, compact ->
            VistaBrandMark(
                modifier = modifier,
                asset = if (compact) VistaBrandAsset.MARK else VistaBrandAsset.AUTH,
            )
        },
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
                LaunchedEffect(Unit) {
                    navController.navigate(AppRoute.Startup) {
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
                        sessionStore.clear()
                        authenticationStateOwner.signOut()
                        shellDeepLinkRequest = null
                        navController.navigate(AppRoute.Authentication) {
                            popUpTo<AppRoute.AuthenticatedBoundary> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onExitRequested = onExitRequested,
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
