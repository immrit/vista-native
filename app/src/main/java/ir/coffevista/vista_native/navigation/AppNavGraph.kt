package ir.coffevista.vista_native.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.coffevista.vista_native.core.di.AppContainer
import ir.coffevista.vista_native.R
import ir.coffevista.vista_native.features.auth.AuthScreen
import ir.coffevista.vista_native.features.auth.AuthViewModel
import ir.coffevista.vista_native.features.auth.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthVisuals
import ir.coffevista.vista_native.features.authenticated.AuthenticatedBoundaryScreen
import ir.coffevista.vista_native.features.onboarding.OnboardingSlide
import ir.coffevista.vista_native.features.onboarding.OnboardingScreen
import ir.coffevista.vista_native.features.onboarding.OnboardingViewModel
import ir.coffevista.vista_native.features.startup.MaintenanceScreen
import ir.coffevista.vista_native.features.startup.StartupDestination
import ir.coffevista.vista_native.features.startup.StartupResolver
import ir.coffevista.vista_native.features.startup.StartupScreen
import ir.coffevista.vista_native.features.startup.StartupViewModel
import ir.coffevista.vista_native.ui.components.VistaBrandAsset
import ir.coffevista.vista_native.ui.components.VistaBrandMark
import ir.coffevista.vista_native.ui.theme.VistaColors

object Routes {
    const val STARTUP = "startup"
    const val ONBOARDING = "onboarding"
    const val AUTH = "auth"
    const val AUTHENTICATED = "authenticated-boundary"
    const val MAINTENANCE = "maintenance"
}

@Composable
fun VistaApp(container: AppContainer) {
    val navController = rememberNavController()
    val authState by container.authenticationStateOwner.state.collectAsStateWithLifecycle()
    val authVisuals = AuthVisuals(
        accentColor = VistaColors.Cyan,
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
            accent = VistaColors.Cyan,
            accentDeep = VistaColors.CyanDeep,
            image = painterResource(R.drawable.viu_connect),
            imageScale = 1.23f,
        ),
        OnboardingSlide(
            kicker = "برای هر لحظه",
            title = "لحظه‌هایت را زنده کن",
            description = "از عکس و ویدیو تا استوری و موسیقی؛ خلاقیتت را ساده و با حال‌وهوای خودت به اشتراک بگذار.",
            accent = VistaColors.Coral,
            accentDeep = Color(0xFFE84F5E),
            image = painterResource(R.drawable.viu_create),
            imageScale = 1.1f,
        ),
        OnboardingSlide(
            kicker = "با خیال راحت",
            title = "فضای تو، انتخاب تو",
            description = "حریم خصوصی و گفت‌وگوهای امن، با کنترل‌هایی که همیشه در دست خودت می‌مانند.",
            accent = VistaColors.Navy,
            accentDeep = Color(0xFF142E69),
            image = painterResource(R.drawable.viu_private),
            imageScale = 1.16f,
        ),
    )

    NavHost(
        navController = navController,
        startDestination = Routes.STARTUP,
    ) {
        composable(Routes.STARTUP) {
            val startupViewModel: StartupViewModel = viewModel(
                factory = StartupViewModel.Factory(
                    StartupResolver(
                        authRepository = container.authRepository,
                        onboardingStore = container.onboardingStore,
                        sessionStore = container.sessionStore,
                    ),
                ),
            )
            val state by startupViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.destination) {
                val route = when (val destination = state.destination) {
                    StartupDestination.Loading,
                    is StartupDestination.RecoverableError,
                    -> null
                    StartupDestination.Maintenance -> Routes.MAINTENANCE
                    StartupDestination.Onboarding -> Routes.ONBOARDING
                    StartupDestination.Authentication -> {
                        container.authenticationStateOwner.signOut()
                        Routes.AUTH
                    }
                    is StartupDestination.Authenticated -> {
                        container.authenticationStateOwner.accept(destination.context)
                        if (destination.context.passwordRequired) {
                            Routes.AUTH
                        } else {
                            Routes.AUTHENTICATED
                        }
                    }
                }
                if (route != null) {
                    navController.navigate(route) {
                        popUpTo(Routes.STARTUP) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            StartupScreen(
                state = state,
                onRetry = startupViewModel::retry,
                accentColor = VistaColors.Cyan,
                brand = { modifier ->
                    VistaBrandMark(
                        modifier = modifier,
                        asset = VistaBrandAsset.SPLASH,
                    )
                },
            )
        }

        composable(Routes.MAINTENANCE) {
            MaintenanceScreen(
                onRetry = {
                    navController.navigate(Routes.STARTUP) {
                        popUpTo(Routes.MAINTENANCE) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ONBOARDING) {
            val onboardingViewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModel.Factory(container.onboardingStore),
            )
            val state by onboardingViewModel.state.collectAsStateWithLifecycle()
            OnboardingScreen(
                state = state,
                onAction = onboardingViewModel::onAction,
                slides = onboardingSlides,
                brand = { modifier -> VistaBrandMark(modifier) },
                onCompleted = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.AUTH) {
            val startInPasswordSetup =
                (authState as? AuthenticationState.SignedIn)?.context?.passwordRequired == true
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(
                    repository = container.authRepository,
                    sessionStore = container.sessionStore,
                    authStateOwner = container.authenticationStateOwner,
                    startInPasswordSetup = startInPasswordSetup,
                ),
            )
            val state by authViewModel.state.collectAsStateWithLifecycle()
            AuthScreen(
                state = state,
                onAction = authViewModel::onAction,
                visuals = authVisuals,
                onAuthenticated = {
                    navController.navigate(Routes.AUTHENTICATED) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.AUTHENTICATED) {
            val signedIn = authState as? AuthenticationState.SignedIn
            if (signedIn == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.STARTUP) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                }
            } else {
                AuthenticatedBoundaryScreen(
                    context = signedIn.context,
                )
            }
        }
    }
}
