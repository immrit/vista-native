package ir.coffevista.vista

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ir.coffevista.vista_native.BuildConfig
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.tier.RemoteTierCapabilitiesOverride
import ir.coffevista.vista_native.core.model.tier.UserTier
import ir.coffevista.vista_native.core.model.tier.UserTierPolicy
import ir.coffevista.vista_native.core.network.BootstrapStatusClient
import ir.coffevista.vista_native.core.network.RemoteConfigSnapshot
import ir.coffevista.vista_native.core.network.SystemGateMonitor
import ir.coffevista.vista_native.features.auth.AuthenticationStateOwner
import ir.coffevista.vista_native.features.auth.domain.AuthRepository
import kotlinx.coroutines.withTimeoutOrNull
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
    private var updateBarState by mutableStateOf<ir.coffevista.vista_native.features.shell.ShellUpdateBarState?>(null)
    private var downloadedUpdateApk: java.io.File? = null
    private var updateDownloadJob: Job? = null
    private var pendingInstallAfterPermission = false
    private var lastUpdateCheckAtMillis = 0L
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
    lateinit var authRepository: AuthRepository

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

    @Inject
    lateinit var notificationSettingsApi: ir.coffevista.vista_native.features.profile.settings.notifications.NotificationSettingsApi

    @Inject
    lateinit var presenceHeartbeatManager: ir.coffevista.vista_native.presence.PresenceHeartbeatManager

    private var lastInAppChimeAtMillis = 0L

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Permission result handled via system state */ }

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
        if (sessionStore.isBiometricEnabled()) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        notificationManager.createNotificationChannels()
        configureStartupFixtures(intent)
        submitDeepLink(intent)
        if (savedInstanceState == null) handleShareIntent(intent)
        lifecycleScope.launch {
            ir.coffevista.vista_native.core.common.NotificationRuntimeCoordinator.dismissedConversations
                .collect { conversationId ->
                    notificationManager.cancelChatNotification(conversationId)
                }
        }
        lifecycleScope.launch {
            ir.coffevista.vista_native.core.common.NotificationRuntimeCoordinator.inAppChatChimes
                .collect {
                    val now = System.currentTimeMillis()
                    if (now - lastInAppChimeAtMillis >= 1_500L) {
                        lastInAppChimeAtMillis = now
                        runCatching {
                            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            RingtoneManager.getRingtone(applicationContext, uri)?.play()
                        }
                    }
                }
        }
        lifecycleScope.launch {
            ir.coffevista.vista_native.core.common.NotificationRuntimeCoordinator.fcmResyncRequests
                .collect {
                    pushTokenRegistrar.syncTokenAsync(userId = lastSignedInUserId, force = true)
                }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val status = runCatching { bootstrapStatusClient.fetch() }.getOrNull()
                    if (status != null) {
                        status.remoteConfig?.let(::syncTierOverrides)
                        if (status.fcmResyncEpoch > 0L) {
                            pushTokenRegistrar.onControlPlaneEpoch(status.fcmResyncEpoch)
                        }
                    }
                    kotlinx.coroutines.delay(60_000L)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                presenceHeartbeatManager.sendHeartbeatIfEligible(force = true)
                while (true) {
                    kotlinx.coroutines.delay(55_000L)
                    presenceHeartbeatManager.sendHeartbeatIfEligible()
                }
            }
        }
        lifecycleScope.launch {
            authenticationStateOwner.state
                .map { state ->
                    (state as? AuthenticationState.SignedIn)?.context
                }
                .distinctUntilChanged { old, new -> old?.userId == new?.userId && old?.profileCompleted == new?.profileCompleted }
                .collectLatest { context ->
                    val userId = context?.userId
                    if (userId != null) {
                        lastSignedInUserId = userId
                        launch { presenceHeartbeatManager.sendHeartbeatIfEligible(force = true) }
                        pushTokenRegistrar.syncTokenAsync(userId)
                        ownProfileRepository.fetchAndCacheOwnProfile(userId)
                        reconcilePendingChatNotification()
                        launch {
                            runCatching { notificationSettingsApi.getSettings() }.getOrNull()?.let { resp ->
                                if (resp.isSuccessful) {
                                    resp.body()?.let { dto ->
                                        ir.coffevista.vista_native.core.common.NotificationRuntimeCoordinator
                                            .updateNotificationPreferences(
                                                inAppChatSounds = dto.in_app_chat_sounds,
                                                soundEnabled = dto.sound_enabled,
                                                vibrationEnabled = dto.vibration_enabled,
                                            )
                                    }
                                }
                            }
                        }
                        if (context.profileCompleted) {
                            maybePromptNotificationPermissionOnce()
                        }
                    } else if (lastSignedInUserId != null) {
                        lastSignedInUserId = null
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
                    val fullscreenOverlayController = androidx.compose.runtime.remember {
                        ir.coffevista.vista_native.core.designsystem.component.VistaFullscreenOverlayController()
                    }
                    val remoteCfg by SystemGateMonitor.remoteConfig.collectAsStateWithLifecycle()
                    val ambientSpec = androidx.compose.runtime.remember(remoteCfg?.ambientEffect) {
                        ir.coffevista.vista_native.core.designsystem.component.VistaAmbientEffectSpec(
                            effect = remoteCfg?.ambientEffect?.effect ?: "none",
                            density = remoteCfg?.ambientEffect?.density ?: "low",
                        )
                    }
                    CompositionLocalProvider(
                        LocalLayoutDirection provides layoutDirection,
                        ir.coffevista.vista_native.core.designsystem.component.LocalVistaFullscreenOverlay provides fullscreenOverlayController,
                        ir.coffevista.vista_native.core.designsystem.component.LocalVistaAmbientEffect provides ambientSpec,
                    ) {
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
                                updateBarState = updateBarState,
                                onUpdateBarClick = ::handleUpdateBarClick,
                                onBeforeClearSession = {
                                    runCatching {
                                        withTimeoutOrNull(1_200L) {
                                            presenceHeartbeatManager.sendOfflineImmediately()
                                        }
                                    }
                                    pushTokenRegistrar.unregisterTokenBeforeSignOut()
                                    runCatching {
                                        withTimeoutOrNull(1_500L) {
                                            authRepository.logout(sessionStore.read()?.refreshToken)
                                        }
                                    }
                                },
                                onExitRequested = ::finish,
                            )
                            val announcement = remoteCfg?.announcement
                            var dismissedAnnouncementId by androidx.compose.runtime.remember {
                                mutableStateOf<String?>(null)
                            }
                            if (
                                announcement != null &&
                                announcement.enabled &&
                                (announcement.title.isNotBlank() || announcement.message.isNotBlank()) &&
                                (!announcement.dismissible || dismissedAnnouncementId != announcement.id)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .safeDrawingPadding()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    color = if (announcement.severity == "critical") {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    },
                                    tonalElevation = 6.dp,
                                    shadowElevation = 6.dp,
                                ) {
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (announcement.title.isNotBlank()) {
                                                Text(
                                                    text = announcement.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                )
                                            }
                                            if (announcement.message.isNotBlank()) {
                                                Text(
                                                    text = announcement.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                        }
                                        if (announcement.dismissible) {
                                            androidx.compose.material3.TextButton(
                                                onClick = { dismissedAnnouncementId = announcement.id },
                                            ) {
                                                Text("بستن")
                                            }
                                        }
                                    }
                                }
                            }
                            val isAppLocked = biometricUnlockRequired && sessionStore.isBiometricEnabled()
                            if (!isAppLocked) {
                                availableUpdate?.takeIf { it.isMandatory }?.let { update ->
                                    val bar = updateBarState
                                    val downloading = bar?.stage == ir.coffevista.vista_native.features.shell.ShellUpdateStage.Downloading ||
                                        bar?.stage == ir.coffevista.vista_native.features.shell.ShellUpdateStage.Verifying
                                    val readyToInstall = bar?.stage == ir.coffevista.vista_native.features.shell.ShellUpdateStage.ReadyToInstall
                                    androidx.compose.material3.AlertDialog(
                                        onDismissRequest = { /* Mandatory update cannot be dismissed */ },
                                        title = {
                                            Text(text = "به‌روزرسانی ضروری ویستا")
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text(
                                                    text = buildString {
                                                        append("نسخه ")
                                                        append(update.versionName ?: "جدید")
                                                        append(" ویستا آماده دریافت است.\nبرای ادامه استفاده از برنامه، لطفاً به نسخه جدید به‌روزرسانی کنید.")
                                                    },
                                                )
                                                if (downloading && bar != null) {
                                                    androidx.compose.material3.LinearProgressIndicator(
                                                        progress = { bar.progress.coerceIn(0f, 1f) },
                                                    )
                                                    Text(
                                                        text = if (bar.stage == ir.coffevista.vista_native.features.shell.ShellUpdateStage.Verifying) {
                                                            "در حال بررسی فایل..."
                                                        } else {
                                                            "در حال دریافت: ${(bar.progress * 100f).toInt()}%"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                } else if (bar?.stage == ir.coffevista.vista_native.features.shell.ShellUpdateStage.Error && !bar.errorMessage.isNullOrBlank()) {
                                                    Text(
                                                        text = bar.errorMessage.orEmpty(),
                                                        color = MaterialTheme.colorScheme.error,
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                enabled = bar?.stage != ir.coffevista.vista_native.features.shell.ShellUpdateStage.Verifying,
                                                onClick = ::handleUpdateBarClick,
                                            ) {
                                                Text(
                                                    when {
                                                        downloading -> "لغو دریافت"
                                                        readyToInstall -> "نصب نسخه جدید"
                                                        else -> "به‌روزرسانی"
                                                    },
                                                )
                                            }
                                        },
                                        dismissButton = null,
                                    )
                                }
                                ir.coffevista.vista_native.core.designsystem.component.VistaFullscreenOverlayHost(
                                    controller = fullscreenOverlayController,
                                )
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
                            if (isAppLocked) {
                                androidx.activity.compose.BackHandler(enabled = true) { /* Prevent back bypass while locked */ }
                                BiometricLockOverlay(
                                    onUnlock = {
                                        biometricAuthenticator.authenticate(this@MainActivity) { authenticated ->
                                            if (authenticated) {
                                                biometricUnlockRequired = false
                                                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                                            }
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
        handleShareIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        ir.coffevista.vista_native.core.common.NotificationRuntimeCoordinator.setAppForeground(true)
        lifecycleScope.launch { chatRepository.setForeground(true) }
        reconcilePendingChatNotification()
        if (System.currentTimeMillis() - lastUpdateCheckAtMillis >= 15 * 60 * 1000L) {
            checkForAppUpdate(showUpToDateToast = false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (pendingInstallAfterPermission) {
            pendingInstallAfterPermission = false
            if (vistaUpdateManager.canInstallPackages()) {
                downloadedUpdateApk?.takeIf { it.exists() }?.let { apk ->
                    vistaUpdateManager.installApk(apk)
                }
            }
        }
    }

    override fun onStop() {
        ir.coffevista.vista_native.core.common.NotificationRuntimeCoordinator.setAppForeground(false)
        if (!isChangingConfigurations && sessionStore.isBiometricEnabled()) {
            biometricUnlockRequired = true
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        lifecycleScope.launch { chatRepository.setForeground(false) }
        presenceHeartbeatManager.sendOfflineAsync(isChangingConfigurations)
        super.onStop()
    }

    private fun isTrustedNotificationIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        val isInternalPackage = intent.`package` == packageName || intent.component?.packageName == packageName
        if (!isInternalPackage) return false
        // External browsers/apps invoking ACTION_VIEW with a data URI must not inject push notification extras
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) return false
        return ir.coffevista.vista_native.notifications.NotificationIntentAuthenticator.verify(intent)
    }

    private fun isAllowedNotificationDeepLink(raw: String): Boolean {
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return false
        return when (uri.scheme?.lowercase()) {
            "vista", "coffevista" -> true
            "https" -> uri.host?.lowercase() in setOf(
                "cafevista.ir",
                "www.cafevista.ir",
                "coffevista.ir",
                "www.coffevista.ir",
            )
            else -> false
        }
    }

    private fun submitDeepLink(intent: Intent?) {
        if (intent?.action == ir.coffevista.vista_native.core.designsystem.audio.VistaMusicPlayerService.ACTION_OPEN_PLAYER) {
            ir.coffevista.vista_native.core.designsystem.audio.VistaAudioPlayer.attach(applicationContext)
            ir.coffevista.vista_native.core.designsystem.audio.VistaAudioPlayer.requestOpenFromNotification()
            return
        }
        val notificationData = if (isTrustedNotificationIntent(intent)) {
            intent?.extras
                ?.keySet()
                ?.mapNotNull { key -> intent.extras?.getString(key)?.let { key to it } }
                ?.toMap()
                ?.let(ChatNavigationContract::normalizeNotificationData)
                .orEmpty()
        } else {
            emptyMap()
        }
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
        lastUpdateCheckAtMillis = System.currentTimeMillis()
        lifecycleScope.launch {
            val result = vistaUpdateManager.checkForUpdate(
                backendBaseUrl = BuildConfig.API_BASE_URL.trimEnd('/'),
                currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
                flavor = BuildConfig.FLAVOR.ifBlank { "direct" },
            )
            if (result.updateAvailable) {
                availableUpdate = result
                val currentBar = updateBarState
                val alreadyDownloadingSame = currentBar != null &&
                    currentBar.versionCode == result.versionCode &&
                    (currentBar.stage == ir.coffevista.vista_native.features.shell.ShellUpdateStage.Downloading ||
                        currentBar.stage == ir.coffevista.vista_native.features.shell.ShellUpdateStage.Verifying)
                if (!alreadyDownloadingSame) {
                    val cachedApk = vistaUpdateManager.getCachedValidApk(result.versionCode, result.sha256)
                    downloadedUpdateApk = cachedApk
                    updateBarState = ir.coffevista.vista_native.features.shell.ShellUpdateBarState(
                        versionCode = result.versionCode,
                        versionName = result.versionName ?: "جدید",
                        fileSizeBytes = result.fileSizeBytes,
                        releaseNotes = result.releaseNotes.orEmpty(),
                        isMandatory = result.isMandatory,
                        stage = if (cachedApk != null) {
                            ir.coffevista.vista_native.features.shell.ShellUpdateStage.ReadyToInstall
                        } else {
                            ir.coffevista.vista_native.features.shell.ShellUpdateStage.Available
                        },
                        progress = if (cachedApk != null) 1f else 0f,
                        downloadedBytes = if (cachedApk != null) cachedApk.length() else 0L,
                        totalBytes = result.fileSizeBytes,
                    )
                }
            } else {
                availableUpdate = null
                updateBarState = null
                downloadedUpdateApk = null
                if (showUpToDateToast) {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "شما از آخرین نسخه ویستا استفاده می‌کنید",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun handleUpdateBarClick() {
        val update = availableUpdate ?: return
        val currentBar = updateBarState ?: return
        when (currentBar.stage) {
            ir.coffevista.vista_native.features.shell.ShellUpdateStage.Downloading -> {
                updateDownloadJob?.cancel()
                updateDownloadJob = null
                updateBarState = currentBar.copy(
                    stage = ir.coffevista.vista_native.features.shell.ShellUpdateStage.Available,
                    progress = 0f,
                    downloadedBytes = 0L,
                    errorMessage = null,
                )
            }
            ir.coffevista.vista_native.features.shell.ShellUpdateStage.Verifying -> Unit
            ir.coffevista.vista_native.features.shell.ShellUpdateStage.ReadyToInstall -> {
                val cached = downloadedUpdateApk?.takeIf { it.exists() }
                    ?: vistaUpdateManager.getCachedValidApk(update.versionCode, update.sha256)
                if (cached != null) {
                    downloadedUpdateApk = cached
                    launchApkInstaller(cached)
                } else {
                    startUpdateDownload(update, currentBar)
                }
            }
            ir.coffevista.vista_native.features.shell.ShellUpdateStage.Available,
            ir.coffevista.vista_native.features.shell.ShellUpdateStage.Error -> {
                val downloadUrl = update.downloadUrl?.takeIf(String::isNotBlank)
                if (downloadUrl == null) {
                    vistaUpdateManager.openStoreForUpdate()
                } else {
                    startUpdateDownload(update, currentBar)
                }
            }
        }
    }

    private fun startUpdateDownload(
        update: ir.coffevista.vista_native.update.UpdateCheckResult,
        currentBar: ir.coffevista.vista_native.features.shell.ShellUpdateBarState,
    ) {
        updateDownloadJob?.cancel()
        updateBarState = currentBar.copy(
            stage = ir.coffevista.vista_native.features.shell.ShellUpdateStage.Downloading,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = update.fileSizeBytes,
            errorMessage = null,
        )
        updateDownloadJob = lifecycleScope.launch {
            val result = vistaUpdateManager.downloadApk(
                update = update,
                onProgress = { copied, total ->
                    val resolvedTotal = if (total > 0L) total else update.fileSizeBytes
                    val fraction = if (resolvedTotal > 0L) {
                        (copied.toFloat() / resolvedTotal.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    runOnUiThread {
                        updateBarState = updateBarState?.copy(
                            stage = ir.coffevista.vista_native.features.shell.ShellUpdateStage.Downloading,
                            progress = fraction,
                            downloadedBytes = copied,
                            totalBytes = resolvedTotal,
                        )
                    }
                },
                onVerifying = {
                    runOnUiThread {
                        updateBarState = updateBarState?.copy(
                            stage = ir.coffevista.vista_native.features.shell.ShellUpdateStage.Verifying,
                            progress = 1f,
                        )
                    }
                },
            )
            when (result) {
                is ir.coffevista.vista_native.update.ApkDownloadResult.ReadyToInstall -> {
                    downloadedUpdateApk = result.apkFile
                    updateBarState = updateBarState?.copy(
                        stage = ir.coffevista.vista_native.features.shell.ShellUpdateStage.ReadyToInstall,
                        progress = 1f,
                        downloadedBytes = result.apkFile.length(),
                        errorMessage = null,
                    )
                    launchApkInstaller(result.apkFile)
                }
                is ir.coffevista.vista_native.update.ApkDownloadResult.Failed -> {
                    updateBarState = updateBarState?.copy(
                        stage = ir.coffevista.vista_native.features.shell.ShellUpdateStage.Error,
                        errorMessage = result.message,
                    )
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        result.message,
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                }
                ir.coffevista.vista_native.update.ApkDownloadResult.Cancelled -> Unit
            }
        }
    }

    private fun launchApkInstaller(apkFile: java.io.File) {
        if (!vistaUpdateManager.canInstallPackages()) {
            pendingInstallAfterPermission = true
            android.widget.Toast.makeText(
                this@MainActivity,
                "لطفاً اجازه نصب برنامه از این منبع را فعال کنید",
                android.widget.Toast.LENGTH_LONG,
            ).show()
            vistaUpdateManager.openUnknownSourcesSettings()
            return
        }
        val launched = vistaUpdateManager.installApk(apkFile)
        if (!launched) {
            android.widget.Toast.makeText(
                this@MainActivity,
                "باز کردن نصب‌کننده اندروید با خطا مواجه شد",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun notificationDeepLink(data: Map<String, String>): String? {
        val type = (data["type"] ?: data["notification_type"]).orEmpty().lowercase()
        if (type in setOf("post_admin_edit", "moderation_notice", "appeal_resolved")) {
            data["post_id"]?.takeIf(String::isNotBlank)?.let { return "vista://appeal/$it" }
        }
        data["deeplink"]?.trim()?.takeIf { it.isNotBlank() && isAllowedNotificationDeepLink(it) }?.let { return it }
        return when {
            type in setOf("post", "like", "comment", "comment_reply", "mention", "suggest_post") ->
                data["post_id"]?.takeIf(String::isNotBlank)?.let { "vista://post/$it" }
            type in setOf("follow", "follow_request", "follow_request_accepted", "suggest_follow") ->
                (data["follower_id"] ?: data["user_id"] ?: data["sender_id"])
                    ?.takeIf(String::isNotBlank)
                    ?.let { "vista://profile/$it" }
            type == "story" ->
                data["user_id"]?.takeIf(String::isNotBlank)?.let { "vista://profile/$it" } ?: "vista://notifications"
            type in setOf("story_reply", "story_reaction", "admin_broadcast", "post_admin_removed", "appeal_approved", "appeal_rejected") ->
                "vista://notifications"
            type == "daily_suggestion_digest" ->
                "vista://notifications?tab=suggestions"
            type == "nearby_like" ->
                "vista://nearby/likes"
            type == "nearby_match" ->
                "vista://nearby/matches"
            else -> null
        }
    }

    private fun maybePromptNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        val isInstrumentation = runCatching {
            Class.forName("androidx.test.platform.app.InstrumentationRegistry")
        }.isSuccess
        if (isInstrumentation) return
        val prefs = getSharedPreferences("vista_notification_ui_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("post_notifications_prompted", false)) return
        prefs.edit().putBoolean("post_notifications_prompted", true).apply()
        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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

    private fun syncTierOverrides(remote: RemoteConfigSnapshot) {
        val mapped = buildMap {
            remote.tiers.forEach { (key, limits) ->
                val tier = when (key.lowercase()) {
                    "normal" -> UserTier.NORMAL
                    "blacktick", "black_tick" -> UserTier.BLACK_TICK
                    "goldtick", "gold_tick" -> UserTier.GOLD_TICK
                    "bluetick", "blue_tick" -> UserTier.BLUE_TICK
                    else -> return@forEach
                }
                put(
                    tier,
                    RemoteTierCapabilitiesOverride(
                        maxPostChars = limits.maxPostChars,
                        canEditPost = limits.canEditPost,
                        canEditComment = limits.canEditComment,
                        maxStoryHours = limits.maxStoryHours,
                        maxUploadMb = limits.maxUploadMb,
                        canPostMusic = limits.canPostMusic,
                        maxVideoDurationMs = limits.maxVideoSeconds?.let { it.toLong() * 1000L },
                        canHideEngagementCounts = limits.canHideEngagementCounts,
                        searchPriority = limits.searchPriority,
                        adsFree = limits.adsFree,
                        maxGalleryImages = limits.maxGalleryImages,
                    ),
                )
            }
        }
        UserTierPolicy.updateRemoteOverrides(mapped)
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
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
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
}
