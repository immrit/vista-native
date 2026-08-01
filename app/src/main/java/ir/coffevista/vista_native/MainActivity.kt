package ir.coffevista.vista_native

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import dagger.hilt.android.AndroidEntryPoint
import ir.coffevista.vista_native.features.auth.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthenticationStateOwner
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import ir.coffevista.vista_native.features.feed.data.FeedApiFixture
import ir.coffevista.vista_native.features.profile.data.PublicProfileApiFixture
import ir.coffevista.vista_native.features.profile.data.OwnProfileApiFixture
import ir.coffevista.vista_native.features.profile.data.UserProfileRepository
import ir.coffevista.vista_native.features.search.data.SearchRepository
import ir.coffevista.vista_native.features.startup.StartupFixture
import ir.coffevista.vista_native.navigation.VistaApp
import ir.coffevista.vista_native.navigation.DeepLinkCoordinator
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.core.designsystem.theme.VistaTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authenticationStateOwner: AuthenticationStateOwner

    @Inject
    lateinit var deepLinkCoordinator: DeepLinkCoordinator

    @Inject
    lateinit var startupFixtures: Set<@JvmSuppressWildcards StartupFixture>

    @Inject
    lateinit var sessionStore: SessionStore

    @Inject
    lateinit var feedRepository: FeedRepository

    @Inject
    lateinit var feedApiFixtures: Set<@JvmSuppressWildcards FeedApiFixture>

    @Inject
    lateinit var publicProfileApiFixtures: Set<@JvmSuppressWildcards PublicProfileApiFixture>

    @Inject
    lateinit var ownProfileApiFixtures: Set<@JvmSuppressWildcards OwnProfileApiFixture>

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    @Inject
    lateinit var searchRepository: SearchRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureStartupFixtures(intent)
        submitDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            VistaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        VistaApp(
                            authenticationStateOwner = authenticationStateOwner,
                            deepLinkCoordinator = deepLinkCoordinator,
                            sessionStore = sessionStore,
                            feedRepository = feedRepository,
                            userProfileRepository = userProfileRepository,
                            searchRepository = searchRepository,
                            onExitRequested = ::finish,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureStartupFixtures(intent)
        submitDeepLink(intent)
    }

    private fun submitDeepLink(intent: Intent?) {
        deepLinkCoordinator.submit(
            rawUri = intent?.dataString,
            authenticated = authenticationStateOwner.state.value is AuthenticationState.SignedIn,
        )
    }

    private fun configureStartupFixtures(intent: Intent?) {
        val scenario = intent?.getStringExtra(FOUNDATION_FIXTURE_EXTRA)
        startupFixtures.forEach { fixture -> fixture.configure(scenario) }
        feedApiFixtures.forEach { fixture -> fixture.configure(scenario) }
        publicProfileApiFixtures.forEach { fixture -> fixture.configure(scenario) }
        ownProfileApiFixtures.forEach { fixture -> fixture.configure(scenario) }
    }

    private companion object {
        const val FOUNDATION_FIXTURE_EXTRA = "vista.foundation.fixture"
    }
}
