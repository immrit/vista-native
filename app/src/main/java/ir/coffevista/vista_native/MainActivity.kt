package ir.coffevista.vista_native

import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.features.auth.AuthenticationStateOwner
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import ir.coffevista.vista_native.features.feed.data.FeedApiFixture
import ir.coffevista.vista_native.features.feed.data.CommentApiFixture
import ir.coffevista.vista_native.features.profile.data.PublicProfileApiFixture
import ir.coffevista.vista_native.features.profile.data.OwnProfileApiFixture
import ir.coffevista.vista_native.features.profile.data.UserProfileRepository
import ir.coffevista.vista_native.features.profile.data.OwnProfileRepository
import ir.coffevista.vista_native.features.search.data.SearchRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatRepository
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import ir.coffevista.vista_native.features.chat.presentation.navigation.ChatNavigationContract
import ir.coffevista.vista_native.features.startup.StartupFixture
import ir.coffevista.vista_native.navigation.VistaApp
import ir.coffevista.vista_native.navigation.DeepLinkCoordinator
import ir.coffevista.vista_native.core.security.SessionStore
import ir.coffevista.vista_native.core.security.BiometricAuthenticator
import ir.coffevista.vista_native.core.designsystem.theme.VistaTheme
import ir.coffevista.vista_native.core.datastore.SettingsPreferenceStore
import ir.coffevista.vista_native.core.datastore.SettingsPreferences
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var pendingChatNotification: Map<String, String>? = null
    private var notificationReconciliationJob: Job? = null
    private var biometricUnlockRequired by mutableStateOf(false)

    @Inject
    lateinit var authenticationStateOwner: AuthenticationStateOwner

    @Inject
    lateinit var deepLinkCoordinator: DeepLinkCoordinator

    @Inject
    lateinit var startupFixtures: Set<@JvmSuppressWildcards StartupFixture>

    @Inject
    lateinit var sessionStore: SessionStore

    @Inject
    lateinit var biometricAuthenticator: BiometricAuthenticator

    @Inject
    lateinit var feedRepository: FeedRepository

    @Inject
    lateinit var feedApiFixtures: Set<@JvmSuppressWildcards FeedApiFixture>

    @Inject
    lateinit var commentApiFixtures: Set<@JvmSuppressWildcards CommentApiFixture>

    @Inject
    lateinit var publicProfileApiFixtures: Set<@JvmSuppressWildcards PublicProfileApiFixture>

    @Inject
    lateinit var ownProfileApiFixtures: Set<@JvmSuppressWildcards OwnProfileApiFixture>

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    @Inject
    lateinit var ownProfileRepository: OwnProfileRepository

    @Inject
    lateinit var searchRepository: SearchRepository

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var servicesRepository: ir.coffevista.vista_native.features.services.data.ServicesRepository

    @Inject
    lateinit var settingsPreferenceStore: SettingsPreferenceStore

    @Inject
    lateinit var pushTokenRegistrar: ir.coffevista.vista_native.notifications.PushTokenRegistrar

    @Inject
    lateinit var notificationManager: ir.coffevista.vista_native.notifications.VistaNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            biometricUnlockRequired = savedInstanceState.getBoolean(
                KEY_BIOMETRIC_LOCKED,
                sessionStore.isBiometricEnabled(),
            )
            savedInstanceState.getBundle(KEY_PENDING_CHAT_NOTIFICATION)?.let { b ->
                pendingChatNotification = b.keySet().associateWith { key -> b.getString(key).orEmpty() }
            }
        } else {
            biometricUnlockRequired = sessionStore.isBiometricEnabled()
        }
        notificationManager.createNotificationChannels()
        pushTokenRegistrar.syncTokenAsync()
        configureStartupFixtures(intent)
        submitDeepLink(intent)
        lifecycleScope.launch {
            authenticationStateOwner.state
                .map { state ->
                    (state as? AuthenticationState.SignedIn)?.context?.userId
                }
                .distinctUntilChanged()
                .collectLatest { userId ->
                    if (userId != null) {
                        pushTokenRegistrar.syncTokenAsync()
                        ownProfileRepository.fetchAndCacheOwnProfile(userId)
                        reconcilePendingChatNotification()
                    }
                }
        }
        enableEdgeToEdge()
        setContent {
            val settings by settingsPreferenceStore.preferences.collectAsStateWithLifecycle(
                initialValue = SettingsPreferences(),
            )
            val darkTheme = when (settings.themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            VistaTheme(
                darkTheme = darkTheme,
                reduceMotion = settings.reduceMotion,
                chatEntryMode = settings.chatEntryMode,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val layoutDirection = if (settings.locale == "en") {
                        LayoutDirection.Ltr
                    } else {
                        LayoutDirection.Rtl
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            VistaApp(
                                authenticationStateOwner = authenticationStateOwner,
                                deepLinkCoordinator = deepLinkCoordinator,
                                sessionStore = sessionStore,
                                feedRepository = feedRepository,
                                userProfileRepository = userProfileRepository,
                                searchRepository = searchRepository,
                                chatRepository = chatRepository,
                                servicesRepository = servicesRepository,
                                biometricAuthenticator = biometricAuthenticator,
                                onExitRequested = ::finish,
                            )
                            if (biometricUnlockRequired && sessionStore.isBiometricEnabled()) {
                                BiometricLockOverlay(
                                    onUnlock = {
                                        biometricAuthenticator.authenticate(this@MainActivity) { authenticated ->
                                            if (authenticated) biometricUnlockRequired = false
                                        }
                                    },
                                )
                            }
                        }
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

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { chatRepository.setForeground(true) }
        reconcilePendingChatNotification()
    }

    override fun onStop() {
        if (!isChangingConfigurations && sessionStore.isBiometricEnabled()) {
            biometricUnlockRequired = true
        }
        lifecycleScope.launch { chatRepository.setForeground(false) }
        super.onStop()
    }

    private fun submitDeepLink(intent: Intent?) {
        val notificationData = intent?.extras
            ?.keySet()
            ?.mapNotNull { key -> intent.extras?.getString(key)?.let { key to it } }
            ?.toMap()
            ?.let(ChatNavigationContract::normalizeNotificationData)
            .orEmpty()
        val chatDestination = ChatNavigationContract.fromNotification(notificationData)
        if (chatDestination != null) pendingChatNotification = notificationData
        val notificationDeepLink = notificationDeepLink(notificationData)
        deepLinkCoordinator.submit(
            rawUri = chatDestination?.let { destination ->
                buildString {
                    append("vista://chat/")
                    append(destination.conversationId)
                    destination.messageId?.takeIf(String::isNotBlank)?.let { messageId ->
                        append("?messageId=")
                        append(messageId)
                    }
                }
            } ?: notificationDeepLink ?: intent?.dataString,
            authenticated = authenticationStateOwner.state.value is AuthenticationState.SignedIn,
        )
        reconcilePendingChatNotification()
    }

    private fun notificationDeepLink(data: Map<String, String>): String? {
        val type = (data["type"] ?: data["notification_type"]).orEmpty().lowercase()
        return when {
            type in setOf("post", "like", "comment", "comment_reply", "mention", "suggest_post") ->
                data["post_id"]?.takeIf(String::isNotBlank)?.let { "vista://post/$it" }
            type in setOf("follow", "follow_request", "follow_request_accepted", "suggest_follow") ->
                (data["user_id"] ?: data["sender_id"] ?: data["follower_id"])
                    ?.takeIf(String::isNotBlank)
                    ?.let { "vista://profile/$it" }
            type == "story" ->
                data["user_id"]?.takeIf(String::isNotBlank)?.let { "vista://profile/$it" }
            else -> null
        }
    }

    private fun reconcilePendingChatNotification() {
        if (authenticationStateOwner.state.value !is AuthenticationState.SignedIn) return
        if (notificationReconciliationJob?.isActive == true) return
        val payload = pendingChatNotification ?: return
        notificationReconciliationJob = lifecycleScope.launch {
            when (val result = chatRepository.reconcileNotification(payload)) {
                is ChatResult.Success -> pendingChatNotification = null
                is ChatResult.Failure -> if (!result.retryable) pendingChatNotification = null
            }
        }
    }

    private fun configureStartupFixtures(intent: Intent?) {
        val scenario = intent?.getStringExtra(FOUNDATION_FIXTURE_EXTRA)
        startupFixtures.forEach { fixture -> fixture.configure(scenario) }
        feedApiFixtures.forEach { fixture -> fixture.configure(scenario) }
        commentApiFixtures.forEach { fixture -> fixture.configure(scenario) }
        publicProfileApiFixtures.forEach { fixture -> fixture.configure(scenario) }
        ownProfileApiFixtures.forEach { fixture -> fixture.configure(scenario) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_BIOMETRIC_LOCKED, biometricUnlockRequired)
        pendingChatNotification?.let { data ->
            val b = Bundle()
            data.forEach { (k, v) -> b.putString(k, v) }
            outState.putBundle(KEY_PENDING_CHAT_NOTIFICATION, b)
        }
    }

    private companion object {
        const val FOUNDATION_FIXTURE_EXTRA = "vista.foundation.fixture"
        const val KEY_BIOMETRIC_LOCKED = "vista.lifecycle.biometric_locked"
        const val KEY_PENDING_CHAT_NOTIFICATION = "vista.lifecycle.pending_chat_notification"
    }
}

@androidx.compose.runtime.Composable
private fun BiometricLockOverlay(onUnlock: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("ویستا قفل است", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))
            Text("برای ادامه هویت خود را تایید کنید", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onUnlock) { Text("باز کردن قفل") }
        }
    }
}
