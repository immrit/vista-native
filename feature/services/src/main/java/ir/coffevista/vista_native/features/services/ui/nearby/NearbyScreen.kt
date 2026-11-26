package ir.coffevista.vista_native.features.services.ui.nearby

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import ir.coffevista.vista_native.core.designsystem.component.VistaViuStateIllustration
import ir.coffevista.vista_native.core.designsystem.component.VistaViuStateKind

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.ConnectionState
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.coffevista.vista_native.core.designsystem.component.VistaBadgedBox
import ir.coffevista.vista_native.core.designsystem.component.VistaCoinIcon
import ir.coffevista.vista_native.core.designsystem.component.VistaNotificationBadge
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.theme.vistaColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCandidate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

private const val NEARBY_BAZAAR_RSA_PUBLIC_KEY =
    "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwDFm5WUUaqhq2Ha3Al1b8OmGNnBCsPNNjbp04bDJUCFo/QTF6P9JPpqAwVeo0MqR84WEo3KKZBwIYA+aJZuVPYjZ5J6S9tJ1McWKY0v4+6s4PR07Z5N55TMCFz0ofi0GsnpQHVp94zLvhYBQqQatLBq0XsmOiPWTHt2UgP7d0ib2anR2LtRE/1J1WPe2/lji4u8b1+CLRgfT3hzdfqv2RkrpAn69GYqEfUuohQyQK8CAwEAAQ=="

private tailrec fun Context.findNearbyComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findNearbyComponentActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    onBack: () -> Unit,
    onOpenLikes: () -> Unit,
    onOpenProfile: (userId: String, username: String) -> Unit,
    onOpenChat: (conversationId: String, otherUserId: String, username: String, avatarUrl: String) -> Unit,
    viewModel: NearbyViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val activeLocationListener = remember { mutableStateOf<LocationListener?>(null) }
    var reportingCandidate by remember { mutableStateOf<NearbyCandidate?>(null) }

    val payment = remember(context) {
        Payment(
            context = context.applicationContext,
            config = PaymentConfiguration(
                localSecurityCheck = SecurityCheck.Enable(rsaPublicKey = NEARBY_BAZAAR_RSA_PUBLIC_KEY),
            ),
        )
    }
    var bazaarConnection by remember { mutableStateOf<Connection?>(null) }

    DisposableEffect(payment) {
        val connection = runCatching {
            payment.connect {
                connectionSucceed {}
                connectionFailed {}
                disconnected {}
            }
        }.getOrNull()
        bazaarConnection = connection
        onDispose {
            runCatching { bazaarConnection?.disconnect() }
            bazaarConnection = null
        }
    }

    fun launchBazaarCoinPurchase(packageId: String) {
        val activity = context.findNearbyComponentActivity()
        if (activity == null) {
            viewModel.onBazaarCoinPurchaseFailed("امکان باز کردن درگاه پرداخت کافه‌بازار وجود ندارد.")
            return
        }
        viewModel.onBazaarCoinPurchaseStarted(packageId)
        val executePurchase = {
            runCatching {
                payment.purchaseProduct(
                    registry = activity.activityResultRegistry,
                    request = PurchaseRequest(
                        productId = packageId,
                        payload = "vista_coins",
                    ),
                ) {
                    purchaseSucceed { info ->
                        viewModel.verifyBazaarCoinPurchase(
                            productId = packageId,
                            purchaseToken = info.purchaseToken,
                            packageName = info.packageName.ifBlank { context.packageName },
                        )
                    }
                    purchaseCanceled { viewModel.onBazaarCoinPurchaseCanceled() }
                    purchaseFailed { throwable -> viewModel.onBazaarCoinPurchaseFailed(throwable.message) }
                    failedToBeginFlow { throwable ->
                        viewModel.onBazaarCoinPurchaseFailed(
                            throwable.message
                                ?: "برنامه کافه‌بازار روی دستگاه یافت نشد یا به‌روز نیست. لطفاً برنامه بازار را نصب یا به‌روزرسانی کنید.",
                        )
                    }
                }
            }.onFailure { throwable ->
                viewModel.onBazaarCoinPurchaseFailed(throwable.message)
            }
        }
        if (bazaarConnection?.getState() == ConnectionState.Connected) {
            executePurchase()
        } else {
            runCatching {
                bazaarConnection?.disconnect()
                bazaarConnection = payment.connect {
                    connectionSucceed { executePurchase() }
                    connectionFailed { throwable ->
                        viewModel.onBazaarCoinPurchaseFailed(
                            throwable.message
                                ?: "اتصال به درگاه کافه‌بازار برقرار نشد. از نصب و ورود به برنامه بازار اطمینان حاصل کنید.",
                        )
                    }
                    disconnected {}
                }
            }.onFailure { throwable ->
                viewModel.onBazaarCoinPurchaseFailed(throwable.message)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            checkAndAcquireLocation(context, viewModel, coroutineScope, activeLocationListener)
        } else {
            val activity = context as? Activity
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: true
            if (!showRationale) {
                viewModel.setLocationError("permission_forever")
            } else {
                viewModel.setLocationError("permission")
            }
        }
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            viewModel.setLocationError("permission")
        } else {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val isGpsEnabled = isAnyLocationProviderEnabled(locationManager)
            if (!isGpsEnabled) {
                viewModel.setLocationError("service_off")
            } else {
                checkAndAcquireLocation(context, viewModel, coroutineScope, activeLocationListener)
            }
        }
    }

    LaunchedEffect(state.zoneTransition) {
        if (state.zoneTransition != null) {
            delay(3000)
            viewModel.clearZoneTransition()
        }
    }

    state.pendingConsumeToken?.let { token ->
        LaunchedEffect(token) {
            runCatching {
                payment.consumeProduct(token) {
                    consumeSucceed { viewModel.onBazaarPurchaseConsumed() }
                    consumeFailed { viewModel.onBazaarPurchaseConsumed() }
                }
            }.onFailure { viewModel.onBazaarPurchaseConsumed() }
        }
    }

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        val message = state.errorMessage ?: state.infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        if (state.errorMessage != null) viewModel.clearErrorMessage()
        if (state.infoMessage != null) viewModel.clearInfoMessage()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshWallet()
                val err = viewModel.uiState.value.locationError
                if (err == "service_off" || err == "permission_forever" || err == "permission") {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val gpsOn = isAnyLocationProviderEnabled(lm)
                    if ((fine || coarse) && gpsOn) {
                        checkAndAcquireLocation(context, viewModel, coroutineScope, activeLocationListener)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            activeLocationListener.value?.let { listener ->
                runCatching { lm?.removeUpdates(listener) }
            }
            activeLocationListener.value = null
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "اطراف من",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = VistaFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        color = VistaBrandColors.ViuBlueDeep,
                                    ),
                                )
                                if (!state.locationCity.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = VistaBrandColors.ViuBlueDeep.copy(alpha = 0.10f),
                                    ) {
                                        Text(
                                            text = state.locationCity!!,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = VistaFontFamily,
                                                color = VistaBrandColors.ViuBlueDeep,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "بازگشت",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        },
                        actions = {
                            // Grid <-> Card Deck Toggle
                            IconButton(
                                onClick = {
                                    val nextMode = if (state.viewMode == NearbyViewMode.GRID) {
                                        NearbyViewMode.DECK
                                    } else {
                                        NearbyViewMode.GRID
                                    }
                                    viewModel.setViewMode(nextMode)
                                }
                            ) {
                                Icon(
                                    imageVector = if (state.viewMode == NearbyViewMode.GRID) {
                                        Icons.Rounded.ViewAgenda
                                    } else {
                                        Icons.Rounded.GridView
                                    },
                                    contentDescription = if (state.viewMode == NearbyViewMode.GRID) "نمای کارتی" else "نمای شبکه‌ای",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(21.dp),
                                )
                            }

                            // Likes, Chat Requests & Matches Badge Button
                            IconButton(onClick = onOpenLikes) {
                                VistaBadgedBox(
                                    badge = {
                                        if (state.totalInboxBadge > 0) {
                                            VistaNotificationBadge(
                                                count = state.totalInboxBadge,
                                                borderColor = MaterialTheme.colorScheme.surface,
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = "درخواست‌ها، لایک‌ها و مچ‌ها",
                                        tint = VistaBrandColors.ViuBlueDeep,
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                            }

                            // Preferences Button
                            IconButton(onClick = { viewModel.setPreferencesSheetVisible(true) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = "تنظیمات کاوش",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (!state.isDisabled && state.locationError == null) {
                    NearbyZoneFilterStrip(
                        selected = state.zoneFilter,
                        onSelect = { viewModel.selectZoneFilter(it) },
                        coins = state.wallet.coins,
                        requestsAvailable = state.wallet.totalAvailableRequests,
                        onOpenShop = { viewModel.openShop() },
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when {
                        state.isDisabled -> {
                            DisabledDiscoveryView(
                                onEnable = { checkAndAcquireLocation(context, viewModel, coroutineScope, activeLocationListener) }
                            )
                        }
                        state.locationError != null -> {
                            LocationErrorView(
                                error = state.locationError!!,
                                onRequestPermission = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        )
                                    )
                                },
                                onOpenGpsSettings = {
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    }.onFailure {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("امکان باز کردن تنظیمات مکان‌یاب وجود ندارد")
                                        }
                                    }
                                },
                                onOpenAppSettings = {
                                    runCatching {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }.onFailure {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("امکان باز کردن تنظیمات برنامه وجود ندارد")
                                        }
                                    }
                                },
                                onRetry = {
                                    checkAndAcquireLocation(context, viewModel, coroutineScope, activeLocationListener)
                                },
                            )
                        }
                        state.isLocating || (state.isLoading && state.cards.isEmpty()) -> {
                            NearbyRadarScanningView()
                        }
                        state.cards.isEmpty() -> {
                            NearbyEmptyRadarView(
                                onRefresh = { checkAndAcquireLocation(context, viewModel, coroutineScope, activeLocationListener) },
                                onOpenPreferences = { viewModel.setPreferencesSheetVisible(true) },
                            )
                        }
                        state.viewMode == NearbyViewMode.GRID -> {
                            NearbyGridView(
                                candidates = state.cards,
                                onCandidateClick = { card -> viewModel.openPreview(card) },
                                onLikeClick = { card -> viewModel.likeCandidate(card, action = "like") },
                                onChatRequestClick = { card -> viewModel.openChatRequestComposer(card) },
                                onLoadMore = { viewModel.loadCards(reset = false) },
                            )
                        }
                        else -> {
                            NearbyDeck(
                                cards = state.cards,
                                currentIndex = state.currentIndex,
                                onSwipeBrowse = { viewModel.advance(forward = true, wrap = true) },
                                onLike = { viewModel.likeCurrent() },
                                onPass = { viewModel.passCurrent() },
                                onChatRequest = { card -> viewModel.openChatRequestComposer(card) },
                                onOpenProfile = { card -> viewModel.openPreview(card) },
                                onReport = { card -> reportingCandidate = card },
                                zoneTransition = state.zoneTransition,
                            )
                        }
                    }

                    // Match Celebration Dialog
                    state.matchedResult?.let { matchResult ->
                        NearbyMatchDialog(
                            matchResult = matchResult,
                            candidate = state.matchedCandidate,
                            onDismiss = { viewModel.clearMatchDialog() },
                            onStartChat = { matchId, otherUserId, username, avatarUrl ->
                                viewModel.clearMatchDialog()
                                viewModel.openMatchChat(matchId, otherUserId) { conversationId ->
                                    onOpenChat(conversationId, otherUserId, username, avatarUrl)
                                }
                            },
                        )
                    }

                    // Profile Quick Preview Bottom Sheet
                    state.previewCandidate?.let { candidate ->
                        NearbyProfilePreviewSheet(
                            candidate = candidate,
                            superlikeCoinCost = state.wallet.superlikeCoinCost,
                            onDismiss = { viewModel.dismissPreview() },
                            onOpenFullProfile = {
                                viewModel.dismissPreview()
                                onOpenProfile(candidate.userId, candidate.username)
                            },
                            onSendChatRequest = {
                                viewModel.dismissPreview()
                                viewModel.openChatRequestComposer(candidate)
                            },
                            onLike = {
                                viewModel.dismissPreview()
                                viewModel.likeCandidate(candidate, action = "like")
                            },
                            onSuperlike = {
                                viewModel.dismissPreview()
                                viewModel.likeCandidate(candidate, action = "superlike")
                            },
                            onReport = {
                                viewModel.dismissPreview()
                                reportingCandidate = candidate
                            },
                        )
                    }

                    // Chat Request Composer Bottom Sheet
                    state.chatRequestTarget?.let { target ->
                        NearbyChatRequestDialog(
                            target = target,
                            wallet = state.wallet,
                            isSending = state.isSendingChatRequest,
                            onDismiss = { viewModel.dismissChatRequestComposer() },
                            onSend = { intro ->
                                viewModel.sendChatRequest(target.userId, intro)
                            },
                            onOpenShop = {
                                viewModel.dismissChatRequestComposer()
                                viewModel.openShop()
                            },
                        )
                    }

                    // Coins & Chat Request Shop Bottom Sheet
                    if (state.isShopVisible) {
                        NearbyShopSheet(
                            wallet = state.wallet,
                            packs = state.shopPacks,
                            coinPackages = state.coinPackages,
                            sampleRateLabel = state.sampleRateLabel,
                            isLoading = state.isShopLoading,
                            purchasingId = state.shopPurchasingId,
                            hasPendingVerification = state.pendingBazaarVerification != null,
                            shopMessage = state.shopMessage,
                            onDismiss = { viewModel.closeShop() },
                            onBuyRequestPack = { packId -> viewModel.buyChatRequestPack(packId) },
                            onBuyCoinPackage = { packageId -> launchBazaarCoinPurchase(packageId) },
                            onVerifyPendingPayment = { viewModel.retryPendingBazaarVerification() },
                            onClearMessage = { viewModel.clearShopMessage() },
                        )
                    }

                    // Preferences Modal Sheet
                    if (state.isPreferencesSheetVisible) {
                        NearbyPreferencesSheet(
                            preferences = state.preferences,
                            onDismiss = { viewModel.setPreferencesSheetVisible(false) },
                            onSave = { viewModel.updatePreferences(it) },
                            onToggleEnable = { enabled ->
                                if (!enabled) viewModel.disableDiscovery() else viewModel.initBootstrap()
                            },
                        )
                    }

                    // Report Modal Sheet
                    reportingCandidate?.let { candidate ->
                        ReportModalSheet(
                            candidate = candidate,
                            onDismiss = { reportingCandidate = null },
                            onReport = { reason ->
                                viewModel.reportUser(candidate.userId, reason)
                                reportingCandidate = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyWalletHeaderChip(
    coins: Long,
    requestsAvailable: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = VistaBrandColors.ViuBlueDeep.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, VistaBrandColors.ViuBlueDeep.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VistaCoinIcon(size = 16.dp)
            Text(
                text = formatFarsiCount(coins),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
            Text(
                text = "|",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                fontSize = 11.sp,
            )
            Icon(
                imageVector = Icons.Rounded.ChatBubbleOutline,
                contentDescription = "درخواست چت",
                tint = VistaBrandColors.ViuBlueDeep,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = formatFarsiCount(requestsAvailable),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = VistaBrandColors.ViuBlueDeep,
                ),
            )
        }
    }
}

@Composable
private fun NearbyZoneFilterStrip(
    selected: NearbyZoneFilter,
    onSelect: (NearbyZoneFilter) -> Unit,
    coins: Long,
    requestsAvailable: Int,
    onOpenShop: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            NearbyWalletHeaderChip(
                coins = coins,
                requestsAvailable = requestsAvailable,
                onClick = onOpenShop,
            )
        }
        items(NearbyZoneFilter.entries) { zone ->
            val isSelected = zone == selected
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) {
                    VistaBrandColors.ViuBlueDeep
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                },
                border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelect(zone) },
            ) {
                Text(
                    text = zone.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

private fun formatFarsiCount(number: Number): String {
    val digits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val raw = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(number.toLong())
    return buildString {
        for (ch in raw) {
            if (ch in '0'..'9') append(digits[ch - '0']) else append(ch)
        }
    }
}

@Composable
private fun NearbyDeck(
    cards: List<NearbyCandidate>,
    currentIndex: Int,
    onSwipeBrowse: () -> Unit,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onChatRequest: (NearbyCandidate) -> Unit,
    onOpenProfile: (NearbyCandidate) -> Unit,
    onReport: (NearbyCandidate) -> Unit,
    zoneTransition: String?,
    modifier: Modifier = Modifier,
) {
    val safeIndex = currentIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0))
    val currentCard = cards.getOrNull(safeIndex)
    val nextCard = if (cards.size > 1) cards.getOrNull((safeIndex + 1) % cards.size) else null

    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // Next Backing Card
            if (nextCard != null) {
                val dragProgress = (dragOffset.value.x.div(160f).coerceIn(-1f, 1f)).let { kotlin.math.abs(it) }
                val scale = 0.92f + dragProgress * 0.08f
                NearbyCard(
                    candidate = nextCard,
                    onTap = { onOpenProfile(nextCard) },
                    onReport = { onReport(nextCard) },
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale),
                )
            }

            // Current Active Card with physical-axis Drag (RTL-safe via graphicsLayer translationX)
            if (currentCard != null) {
                val rotationAngle = (dragOffset.value.x / 380f).coerceIn(-15f, 15f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = dragOffset.value.x
                            translationY = dragOffset.value.y
                            rotationZ = rotationAngle
                        }
                        .pointerInput(safeIndex) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        dragOffset.snapTo(dragOffset.value + dragAmount)
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        val totalX = dragOffset.value.x
                                        if (totalX > 220f) {
                                            // Pulled physically Right -> animate Right and browse next card (no like/pass action)
                                            dragOffset.animateTo(
                                                Offset(1200f, dragOffset.value.y),
                                                animationSpec = tween(220),
                                            )
                                            dragOffset.snapTo(Offset.Zero)
                                            onSwipeBrowse()
                                        } else if (totalX < -220f) {
                                            // Pulled physically Left -> animate Left and browse next card (no like/pass action)
                                            dragOffset.animateTo(
                                                Offset(-1200f, dragOffset.value.y),
                                                animationSpec = tween(220),
                                            )
                                            dragOffset.snapTo(Offset.Zero)
                                            onSwipeBrowse()
                                        } else {
                                            dragOffset.animateTo(Offset.Zero, animationSpec = tween(200))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        dragOffset.animateTo(Offset.Zero, animationSpec = tween(200))
                                    }
                                }
                            )
                        }
                ) {
                    NearbyCard(
                        candidate = currentCard,
                        onTap = { onOpenProfile(currentCard) },
                        onReport = { onReport(currentCard) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Floating Zone Transition Banner
            if (zoneTransition != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = zoneTransition,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = VistaFontFamily,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Clean Action Bar (Pass, Chat Request, Like)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 44.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Pass (Red)
            FloatingActionButton(
                onClick = onPass,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color(0xFFEF4444),
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "رد کردن",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp),
                )
            }

            // Direct Chat Request (Purple)
            FloatingActionButton(
                onClick = { currentCard?.let { onChatRequest(it) } },
                shape = CircleShape,
                containerColor = VistaBrandColors.ViuBlueDeep,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Forum,
                    contentDescription = "درخواست چت",
                    tint = Color.White,
                    modifier = Modifier.size(23.dp),
                )
            }

            // Like (Green)
            FloatingActionButton(
                onClick = onLike,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color(0xFF10B981),
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "پسندیدن",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun DisabledDiscoveryView(onEnable: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.LocationOff,
                contentDescription = null,
                tint = MaterialTheme.vistaColors.contentSecondary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "حالت کاوش غیرفعال است",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "برای دیدن افراد نزدیک و نمایش پروفایلت، کاوش را فعال کن.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = VistaFontFamily,
                    color = MaterialTheme.vistaColors.contentSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onEnable,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.ViuBlueDeep),
            ) {
                Text(
                    text = "فعال‌سازی «اطراف من»",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun LocationErrorView(
    error: String,
    onRequestPermission: () -> Unit,
    onOpenGpsSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    val title = when (error) {
        "service_off" -> "مکان‌یاب گوشی خاموشه!"
        "permission_forever" -> "اجازه دسترسی مکان نیازه"
        "permission" -> "بذار بدونیم کجایی!"
        else -> "نتونستیم موقعیتت رو پیدا کنیم"
    }
    val subtitle = when (error) {
        "service_off" -> "برای پیدا کردن هم‌شهری‌ها و افراد آنلاینِ اطراف، لطفاً GPS دستگاه رو روشن کن تا بقیه هم بتونن تو رو ببینن."
        "permission_forever" -> "دسترسی مکان مسدود شده؛ از تنظیمات برنامه دسترسی موقعیت رو فعال کن تا آدم‌های واقعی اطرافت رو نشونت بدیم."
        "permission" -> "بذار بدونیم کجایی تا آدم‌های واقعی و آنلاین اطرافت رو همین الان بهت نشون بدیم و بقیه هم بتونن پیدات کنن و باهات گپ بزنن ✨"
        else -> "ارتباط با سرویس مکان‌یاب برقرار نشد؛ لطفاً دوباره امتحان کن."
    }
    val actionLabel = when (error) {
        "service_off" -> "روشن کردن GPS"
        "permission_forever" -> "باز کردن تنظیمات دسترسی"
        "permission" -> "باشه، موقعیتم رو پیدا کن"
        else -> "تلاش مجدد"
    }
    val onAction: () -> Unit = when (error) {
        "service_off" -> onOpenGpsSettings
        "permission_forever" -> onOpenAppSettings
        "permission" -> onRequestPermission
        else -> onRetry
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Subtle radar backdrop behind the floating prompt window
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.25f },
        ) {
            NearbyRadarScanningView()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, VistaBrandColors.ViuBlueDeep.copy(alpha = 0.22f)),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    VistaBrandColors.ViuBlueDeep.copy(alpha = 0.18f),
                                    Color(0xFF10B981).copy(alpha = 0.18f),
                                )
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (error == "service_off") Icons.Rounded.LocationOff else Icons.Rounded.MyLocation,
                        contentDescription = null,
                        tint = VistaBrandColors.ViuBlueDeep,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    ),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VistaFontFamily,
                        color = MaterialTheme.vistaColors.contentSecondary,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    ),
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VistaBrandColors.ViuBlueDeep.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, VistaBrandColors.ViuBlueDeep.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "موقعیت دقیق تو هرگز نمایش داده نمی‌شه؛ فقط شهر و فاصله حدودی!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VistaFontFamily,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                ),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = VistaBrandColors.ViuBlueDeep,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "آشنایی سریع با هم‌شهری‌ها و کاربران آنلاین واقعی اطرافت",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = VistaFontFamily,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.ViuBlueDeep),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = VistaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color.White,
                        ),
                    )
                }
            }
        }
        }
    }
}

private fun isAnyLocationProviderEnabled(locationManager: LocationManager?): Boolean {
    if (locationManager == null) return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        if (runCatching { locationManager.isLocationEnabled }.getOrDefault(false)) return true
    }
    val fused = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching { locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER) }.getOrDefault(false)
    } else {
        false
    }
    val gps = runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
    val network = runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
    return fused || gps || network
}

private suspend fun resolveCityAndProvinceOffMain(
    context: Context,
    lat: Double,
    lng: Double,
): Pair<String?, String?> = withContext(Dispatchers.IO) {
    val appCtx = context.applicationContext
    withTimeoutOrNull(3_500L) {
        fun extract(locale: java.util.Locale): Pair<String?, String?>? {
            return runCatching {
                @Suppress("DEPRECATION")
                val addresses = android.location.Geocoder(appCtx, locale).getFromLocation(lat, lng, 3)
                val addr = addresses?.firstOrNull { !it.locality.isNullOrBlank() || !it.subAdminArea.isNullOrBlank() }
                    ?: addresses?.firstOrNull()
                    ?: return@runCatching null
                val rawCity = addr.locality?.takeIf { it.isNotBlank() }
                    ?: addr.subAdminArea?.takeIf { it.isNotBlank() }
                val cleanCity = rawCity?.trim()
                    ?.removePrefix("شهرستان ")
                    ?.removePrefix("شهر ")
                    ?.trim()
                val cleanProvince = addr.adminArea?.trim()?.removePrefix("استان ")?.trim()
                if (cleanCity.isNullOrBlank() && cleanProvince.isNullOrBlank()) null else cleanCity to cleanProvince
            }.getOrNull()
        }

        val fa = extract(java.util.Locale.forLanguageTag("fa-IR"))
        if (fa != null && !fa.first.isNullOrBlank()) fa
        else extract(java.util.Locale.ENGLISH) ?: (fa ?: (null to null))
    } ?: (null to null)
}

private fun checkAndAcquireLocation(
    context: Context,
    viewModel: NearbyViewModel,
    scope: CoroutineScope,
    activeListenerHolder: androidx.compose.runtime.MutableState<LocationListener?>,
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (!isAnyLocationProviderEnabled(locationManager)) {
        viewModel.setLocationError("service_off")
        return
    }
    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted && !coarseGranted) {
        viewModel.setLocationError("permission")
        return
    }
    try {
        val prewarmed = viewModel.locationSnapshotCache?.latest(15 * 60_000L)
        if (prewarmed != null) {
            if (!prewarmed.city.isNullOrBlank()) {
                viewModel.onLocationAcquired(
                    lat = prewarmed.latitude,
                    lng = prewarmed.longitude,
                    cityName = prewarmed.city,
                    provinceName = prewarmed.province,
                )
            } else {
                scope.launch {
                    val (city, province) = resolveCityAndProvinceOffMain(
                        context,
                        prewarmed.latitude,
                        prewarmed.longitude,
                    )
                    viewModel.onLocationAcquired(prewarmed.latitude, prewarmed.longitude, city, province)
                }
            }
            return
        }

        val candidates = listOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching { locationManager?.getLastKnownLocation(LocationManager.FUSED_PROVIDER) }.getOrNull()
            } else null,
            if (fineGranted) runCatching { locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull() else null,
            runCatching { locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull(),
            if (fineGranted) runCatching { locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) }.getOrNull() else null,
        )
        val freshest = candidates.maxByOrNull { it.time }
        val isRecent = freshest != null && (System.currentTimeMillis() - freshest.time) <= 15 * 60 * 1000L

        if (freshest != null && isRecent) {
            scope.launch {
                val (city, province) = resolveCityAndProvinceOffMain(context, freshest.latitude, freshest.longitude)
                viewModel.onLocationAcquired(freshest.latitude, freshest.longitude, city, province)
            }
        } else {
            if (freshest != null) {
                scope.launch {
                    val (city, province) = resolveCityAndProvinceOffMain(context, freshest.latitude, freshest.longitude)
                    viewModel.onLocationAcquired(freshest.latitude, freshest.longitude, city, province)
                }
            }
            val activeProvider = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    runCatching { locationManager?.isProviderEnabled(LocationManager.FUSED_PROVIDER) == true }.getOrDefault(false) ->
                    LocationManager.FUSED_PROVIDER
                runCatching {
                    locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
                }.getOrDefault(false) ->
                    LocationManager.NETWORK_PROVIDER
                fineGranted && runCatching {
                    locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
                }.getOrDefault(false) ->
                    LocationManager.GPS_PROVIDER
                else -> null
            }
            if (activeProvider != null && locationManager != null) {
                activeListenerHolder.value?.let { oldListener ->
                    runCatching { locationManager.removeUpdates(oldListener) }
                }
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        activeListenerHolder.value = null
                        runCatching { locationManager.removeUpdates(this) }
                        scope.launch {
                            val (city, province) = resolveCityAndProvinceOffMain(context, location.latitude, location.longitude)
                            viewModel.onLocationAcquired(location.latitude, location.longitude, city, province)
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        viewModel.setLocationError("service_off")
                    }
                }
                activeListenerHolder.value = listener
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(
                    activeProvider,
                    listener,
                    Looper.getMainLooper(),
                )
            } else if (freshest == null) {
                viewModel.setLocationError("failed")
            }
        }
    } catch (_: SecurityException) {
        viewModel.setLocationError("permission")
    } catch (_: Exception) {
        viewModel.setLocationError("failed")
    }
}

@Composable
private fun NearbyRadarScanningView(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radar_sweep",
    )
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_scale",
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f

            drawCircle(
                color = VistaBrandColors.ViuBlueDeep.copy(alpha = 0.12f),
                radius = maxRadius,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawCircle(
                color = VistaBrandColors.ViuBlueDeep.copy(alpha = 0.18f),
                radius = maxRadius * 0.68f,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawCircle(
                color = VistaBrandColors.ViuBlueDeep.copy(alpha = 0.25f),
                radius = maxRadius * 0.38f,
                style = Stroke(width = 1.5.dp.toPx()),
            )

            drawCircle(
                color = VistaBrandColors.ViuBlueDeep.copy(alpha = waveAlpha * 0.45f),
                radius = maxRadius * waveScale,
                style = Stroke(width = 2.dp.toPx()),
            )

            drawLine(
                color = VistaBrandColors.ViuBlueDeep.copy(alpha = 0.15f),
                start = Offset(centerOffset.x, 0f),
                end = Offset(centerOffset.x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = VistaBrandColors.ViuBlueDeep.copy(alpha = 0.15f),
                start = Offset(0f, centerOffset.y),
                end = Offset(size.width, centerOffset.y),
                strokeWidth = 1.dp.toPx(),
            )

            rotate(sweepAngle, pivot = centerOffset) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            VistaBrandColors.ViuBlueDeep.copy(alpha = 0.35f),
                        ),
                        center = centerOffset,
                    ),
                    startAngle = 0f,
                    sweepAngle = 60f,
                    useCenter = true,
                )
            }
        }

        Box(
            modifier = Modifier
                .shadow(12.dp, CircleShape)
                .size(64.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(VistaBrandColors.ViuBlueDeep, VistaBrandColors.ViuBlue),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "در حال جستجوی کاربران نزدیک...",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "رادار ویستا فعال است و موقعیت شما در حال اسکن محدوده می‌باشد.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = VistaFontFamily,
                    color = MaterialTheme.vistaColors.contentSecondary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun NearbyEmptyRadarView(
    onRefresh: () -> Unit,
    onOpenPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VistaViuStateIllustration(
            kind = VistaViuStateKind.SearchEmpty,
            size = 152.dp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "کاربری در این محدوده پیدا نشد",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = VistaFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
            ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "افراد جدیدی به زودی به اطراف شما اضافه می‌شوند.\nمی‌توانید شعاع کاوش یا فیلترهای سن را افزایش دهید.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = VistaFontFamily,
                color = MaterialTheme.vistaColors.contentSecondary,
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            ),
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onOpenPreferences,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(imageVector = Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تنظیمات شعاع",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Button(
                onClick = onRefresh,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.ViuBlueDeep),
            ) {
                Icon(imageVector = Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "اسکن مجدد",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportModalSheet(
    candidate: NearbyCandidate,
    onDismiss: () -> Unit,
    onReport: (String) -> Unit,
) {
    val reasons = listOf(
        "پروفایل جعلی یا فیک",
        "محتوای نامناسب",
        "مزاحمت و توهین",
        "تبلیغات و اسپم",
        "سایر موارد",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = "علت گزارش کاربر",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            reasons.forEach { reason ->
                TextButton(
                    onClick = { onReport(reason) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = VistaFontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
