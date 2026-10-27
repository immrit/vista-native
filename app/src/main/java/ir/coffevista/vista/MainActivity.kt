package ir.coffevista.vista

import android.os.Bundle
import android.content.Intent
import android.net.Uri
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ir.coffevista.vista_native.BuildConfig
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.network.BootstrapStatusClient
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
    private var pendingSharedContent by mutableStateOf<ir.coffevista.vista_native.share.SharedIncomingContent?>(null)
    private var routedSharedContent by mutableStateOf<ir.coffevista.vista_native.share.RoutedSharedContent?>(null)
    private var availableUpdate by mutableStateOf<ir.coffevista.vista_native.update.UpdateCheckResult?>(null)
    private var isDownloadingUpdate by mutableStateOf(false)
    private var shareIntentGeneration = 0L
    private var lastSignedInUserId: String? = null

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

    @Inject
    lateinit var bootstrapStatusClient: BootstrapStatusClient

    @Inject
    lateinit var vistaUpdateManager: ir.coffevista.vista_native.update.VistaUpdateManager

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
            pendingSharedContent = savedInstanceState.readSharedContent(KEY_PENDING_SHARED_CONTENT)
            val routedContent = savedInstanceState.readSharedContent(KEY_ROUTED_SHARED_CONTENT)
            val routedDestination = savedInstanceState.getString(KEY_ROUTED_SHARED_DESTINATION)
                ?.let { value ->
                    runCatching {
                        ir.coffevista.vista_native.share.SharedContentDestination.valueOf(value)
                    }.getOrNull()
                }
            if (routedContent != null && routedDestination != null) {
                routedSharedContent = ir.coffevista.vista_native.share.RoutedSharedContent(
                    content = routedContent,
                    destination = routedDestination,
                )
            }
        } else {
            biometricUnlockRequired = sessionStore.isBiometricEnabled()
        }
        notificationManager.createNotificationChannels()
        pushTokenRegistrar.syncTokenAsync()
        configureStartupFixtures(intent)
        submitDeepLink(intent)
        if (savedInstanceState == null) handleShareIntent(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val status = runCatching { bootstrapStatusClient.fetch() }.getOrNull()
                    if (status != null && status.fcmResyncEpoch > 0L) {
                        pushTokenRegistrar.onControlPlaneEpoch(status.fcmResyncEpoch)
                    }
                    kotlinx.coroutines.delay(60_000L)
                }
            }
        }
        lifecycleScope.launch {
            authenticationStateOwner.state
                .map { state ->
                    (state as? AuthenticationState.SignedIn)?.context?.userId
                }
                .distinctUntilChanged()
                .collectLatest { userId ->
                    if (userId != null) {
                        lastSignedInUserId = userId
                        pushTokenRegistrar.syncTokenAsync(userId)
                        ownProfileRepository.fetchAndCacheOwnProfile(userId)
                        reconcilePendingChatNotification()
                    } else if (lastSignedInUserId != null) {
                        lastSignedInUserId = null
                        pushTokenRegistrar.unregisterTokenAsync()
                    }
                }
        }
        checkForAppUpdate(showUpToDateToast = false)
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
                                routedSharedContent = routedSharedContent,
                                onSharedContentConsumed = { requestId ->
                                    if (routedSharedContent?.content?.requestId == requestId) {
                                        routedSharedContent = null
                                    }
                                },
                                onCheckForUpdate = { checkForAppUpdate(showUpToDateToast = true) },
                                onExitRequested = ::finish,
                            )
                            availableUpdate?.let { update ->
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = {
                                        if (!update.isMandatory && !isDownloadingUpdate) {
                                            availableUpdate = null
                                        }
                                    },
                                    title = {
                                        Text(
                                            text = if (update.isMandatory) "به‌روزرسانی ضروری ویستا" else "نسخه جدید ویستا منتشر شد",
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = buildString {
                                                append("نسخه ")
                                                append(update.versionName ?: "جدید")
                                                append(" ویستا آماده دریافت است.")
                                                if (update.isMandatory) {
                                                    append("\nبرای ادامه استفاده از برنامه، لطفاً به نسخه جدید به‌روزرسانی کنید.")
                                                }
                                            },
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            enabled = !isDownloadingUpdate,
                                            onClick = {
                                                val downloadUrl = update.downloadUrl?.takeIf(String::isNotBlank)
                                                if (downloadUrl != null) {
                                                    isDownloadingUpdate = true
                                                    lifecycleScope.launch {
                                                        val ok = vistaUpdateManager.downloadAndInstallApk(
                                                            downloadUrl = downloadUrl,
                                                            expectedSha256 = update.sha256,
                                                        )
                                                        isDownloadingUpdate = false
                                                        if (!ok) {
                                                            vistaUpdateManager.openStoreForUpdate()
                                                        }
                                                    }
                                                } else {
                                                    vistaUpdateManager.openStoreForUpdate()
                                                    if (!update.isMandatory) availableUpdate = null
                                                }
                                            },
                                        ) {
                                            Text(if (isDownloadingUpdate) "در حال دریافت..." else "به‌روزرسانی")
                                        }
                                    },
                                    dismissButton = if (!update.isMandatory) {
                                        {
                                            androidx.compose.material3.TextButton(
                                                enabled = !isDownloadingUpdate,
                                                onClick = { availableUpdate = null },
                                            ) {
                                                Text("بعداً")
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                )
                            }
                            if (biometricUnlockRequired && sessionStore.isBiometricEnabled()) {
                                BiometricLockOverlay(
                                    onUnlock = {
                                        biometricAuthenticator.authenticate(this@MainActivity) { authenticated ->
                                            if (authenticated) biometricUnlockRequired = false
                                        }
                                    },
                                )
                            }
                            pendingSharedContent?.let { shared ->
                                ir.coffevista.vista_native.share.ShareTargetBottomSheet(
                                    content = shared,
                                    onSendToChat = {
                                        routeSharedContent(
                                            content = it,
                                            destination = ir.coffevista.vista_native.share.SharedContentDestination.CHAT,
                                            deepLink = "vista://chat/share",
                                        )
                                    },
                                    onSendToPost = {
                                        routeSharedContent(
                                            content = it,
                                            destination = ir.coffevista.vista_native.share.SharedContentDestination.POST,
                                            deepLink = "vista://post/share",
                                        )
                                    },
                                    onSendToStory = {
                                        routeSharedContent(
                                            content = it,
                                            destination = ir.coffevista.vista_native.share.SharedContentDestination.STORY,
                                            deepLink = "vista://post/share_story",
                                        )
                                    },
                                    onDismiss = {
                                        pendingSharedContent = null
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
        handleShareIntent(intent)
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

    private fun checkForAppUpdate(showUpToDateToast: Boolean) {
        lifecycleScope.launch {
            val result = vistaUpdateManager.checkForUpdate(
                backendBaseUrl = BuildConfig.API_BASE_URL.trimEnd('/'),
                currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
                flavor = BuildConfig.FLAVOR.ifBlank { "direct" },
            )
            if (result.updateAvailable) {
                availableUpdate = result
            } else if (showUpToDateToast) {
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "شما از آخرین نسخه ویستا استفاده می‌کنید",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun notificationDeepLink(data: Map<String, String>): String? {
        data["deeplink"]?.trim()?.takeIf(String::isNotBlank)?.let { return it }
        val type = (data["type"] ?: data["notification_type"]).orEmpty().lowercase()
        return when {
            type in setOf("post_admin_edit", "post_admin_removed", "moderation_notice", "appeal_resolved") ->
                data["post_id"]?.takeIf(String::isNotBlank)?.let { "vista://appeal/$it" }
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
        if (!BuildConfig.DEBUG) return
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
        pendingSharedContent?.let { outState.writeSharedContent(KEY_PENDING_SHARED_CONTENT, it) }
        routedSharedContent?.let { routed ->
            outState.writeSharedContent(KEY_ROUTED_SHARED_CONTENT, routed.content)
            outState.putString(KEY_ROUTED_SHARED_DESTINATION, routed.destination.name)
        }
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return
        val generation = ++shareIntentGeneration
        lifecycleScope.launch {
            val content = ir.coffevista.vista_native.share.IncomingShareCache(this@MainActivity)
                .materialize(intent)
            if (generation != shareIntentGeneration || content == null) return@launch
            routedSharedContent = null
            pendingSharedContent = content
        }
    }

    private fun routeSharedContent(
        content: ir.coffevista.vista_native.share.SharedIncomingContent,
        destination: ir.coffevista.vista_native.share.SharedContentDestination,
        deepLink: String,
    ) {
        pendingSharedContent = null
        routedSharedContent = ir.coffevista.vista_native.share.RoutedSharedContent(content, destination)
        deepLinkCoordinator.submit(
            deepLink,
            authenticated = authenticationStateOwner.state.value is AuthenticationState.SignedIn,
        )
    }

    private fun Bundle.writeSharedContent(
        key: String,
        content: ir.coffevista.vista_native.share.SharedIncomingContent,
    ) {
        putBundle(
            key,
            Bundle().apply {
                putString(KEY_SHARED_REQUEST_ID, content.requestId)
                putString(KEY_SHARED_MIME_TYPE, content.mimeType)
                putString(KEY_SHARED_TEXT, content.text)
                putStringArrayList(KEY_SHARED_URIS, ArrayList(content.uris.map(Uri::toString)))
                putInt(KEY_SHARED_REJECTED_COUNT, content.rejectedItemCount)
            },
        )
    }

    private fun Bundle.readSharedContent(key: String): ir.coffevista.vista_native.share.SharedIncomingContent? {
        val shared = getBundle(key) ?: return null
        val requestId = shared.getString(KEY_SHARED_REQUEST_ID)?.takeIf(String::isNotBlank) ?: return null
        val mimeType = shared.getString(KEY_SHARED_MIME_TYPE)?.takeIf(String::isNotBlank) ?: return null
        val text = shared.getString(KEY_SHARED_TEXT)
        val uris = shared.getStringArrayList(KEY_SHARED_URIS).orEmpty().mapNotNull { raw ->
            runCatching { Uri.parse(raw) }.getOrNull()
        }
        if (text.isNullOrBlank() && uris.isEmpty()) return null
        return ir.coffevista.vista_native.share.SharedIncomingContent(
            requestId = requestId,
            mimeType = mimeType,
            text = text,
            uris = uris,
            rejectedItemCount = shared.getInt(KEY_SHARED_REJECTED_COUNT),
        )
    }

    private companion object {
        const val FOUNDATION_FIXTURE_EXTRA = "vista.foundation.fixture"
        const val KEY_BIOMETRIC_LOCKED = "vista.lifecycle.biometric_locked"
        const val KEY_PENDING_CHAT_NOTIFICATION = "vista.lifecycle.pending_chat_notification"
        const val KEY_PENDING_SHARED_CONTENT = "vista.lifecycle.pending_shared_content"
        const val KEY_ROUTED_SHARED_CONTENT = "vista.lifecycle.routed_shared_content"
        const val KEY_ROUTED_SHARED_DESTINATION = "vista.lifecycle.routed_shared_destination"
        const val KEY_SHARED_REQUEST_ID = "request_id"
        const val KEY_SHARED_MIME_TYPE = "mime_type"
        const val KEY_SHARED_TEXT = "text"
        const val KEY_SHARED_URIS = "uris"
        const val KEY_SHARED_REJECTED_COUNT = "rejected_count"
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
