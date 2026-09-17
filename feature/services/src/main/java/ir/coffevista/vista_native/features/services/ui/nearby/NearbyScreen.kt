package ir.coffevista.vista_native.features.services.ui.nearby

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.theme.vistaColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.services.data.nearby.NearbyCandidate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var reportingCandidate by remember { mutableStateOf<NearbyCandidate?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            checkAndAcquireLocation(context, viewModel)
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
            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            if (!isGpsEnabled) {
                viewModel.setLocationError("service_off")
            } else {
                checkAndAcquireLocation(context, viewModel)
            }
        }
    }

    LaunchedEffect(state.zoneTransition) {
        if (state.zoneTransition != null) {
            delay(3000)
            viewModel.clearZoneTransition()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "اطراف من",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = VistaBrandColors.Indigo,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    actions = {
                        // Shuffle / Random Online
                        IconButton(
                            onClick = {
                                viewModel.loadCards(reset = true, setRandomOnline = !state.isRandomOnline)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "تصادفی آنلاین",
                                tint = if (state.isRandomOnline) Color(0xFF10B981) else MaterialTheme.vistaColors.contentSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        // Likes & Matches Badge Button
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(onClick = onOpenLikes) {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = "لایک‌ها و مچ‌ها",
                                    tint = VistaBrandColors.Indigo,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            if (state.receivedLikesCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 4.dp, end = 4.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (state.receivedLikesCount > 99) "۹۹+" else state.receivedLikesCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = VistaFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                        ),
                                    )
                                }
                            }
                        }

                        // Preferences Button
                        IconButton(onClick = { viewModel.setPreferencesSheetVisible(true) }) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "تنظیمات کاوش",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when {
                    state.isDisabled -> {
                        DisabledDiscoveryView(
                            onEnable = { checkAndAcquireLocation(context, viewModel) }
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
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            },
                            onOpenAppSettings = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            onRetry = {
                                checkAndAcquireLocation(context, viewModel)
                            },
                        )
                    }
                    state.isLocating || (state.isLoading && state.cards.isEmpty()) -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 2.5.dp, color = VistaBrandColors.Indigo)
                        }
                    }
                    state.cards.isEmpty() -> {
                        EmptyDeckView(
                            onRefresh = { checkAndAcquireLocation(context, viewModel) }
                        )
                    }
                    else -> {
                        NearbyDeck(
                            cards = state.cards,
                            currentIndex = state.currentIndex,
                            onSwipeLeft = { viewModel.advance(forward = true) },
                            onSwipeRight = { viewModel.likeCurrent() },
                            onRewind = { viewModel.advance(forward = false) },
                            onLike = { viewModel.likeCurrent() },
                            onPass = { viewModel.passCurrent() },
                            onOpenProfile = { card -> onOpenProfile(card.userId, card.username) },
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
                            onOpenChat(matchId, otherUserId, username, avatarUrl)
                        },
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

@Composable
private fun NearbyDeck(
    cards: List<NearbyCandidate>,
    currentIndex: Int,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onRewind: () -> Unit,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onOpenProfile: (NearbyCandidate) -> Unit,
    onReport: (NearbyCandidate) -> Unit,
    zoneTransition: String?,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val safeIndex = currentIndex.coerceIn(0, (cards.size - 1).coerceAtLeast(0))
    val currentCard = cards.getOrNull(safeIndex)
    val nextCard = cards.getOrNull(safeIndex + 1)

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

            // Current Active Card with Drag
            if (currentCard != null) {
                val rotationAngle = (dragOffset.value.x / 380f).coerceIn(-15f, 15f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(dragOffset.value.x.roundToInt(), dragOffset.value.y.roundToInt()) }
                        .graphicsLayer { rotationZ = rotationAngle }
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
                                        if (totalX > 250f) {
                                            // Swiped Right -> Like
                                            dragOffset.animateTo(
                                                Offset(1200f, dragOffset.value.y),
                                                animationSpec = tween(220),
                                            )
                                            dragOffset.snapTo(Offset.Zero)
                                            onSwipeRight()
                                        } else if (totalX < -250f) {
                                            // Swiped Left -> Pass
                                            dragOffset.animateTo(
                                                Offset(-1200f, dragOffset.value.y),
                                                animationSpec = tween(220),
                                            )
                                            dragOffset.snapTo(Offset.Zero)
                                            onSwipeLeft()
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

        // Action Bar (Pass, Rewind, Info, Like)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
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

            // Rewind (Yellow)
            FloatingActionButton(
                onClick = onRewind,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color(0xFFF59E0B),
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(46.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Replay,
                    contentDescription = "کارت قبلی",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(22.dp),
                )
            }

            // Profile / Info (Blue)
            FloatingActionButton(
                onClick = { currentCard?.let { onOpenProfile(it) } },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = VistaBrandColors.Indigo,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(46.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "پروفایل",
                    tint = VistaBrandColors.Indigo,
                    modifier = Modifier.size(22.dp),
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
                colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Indigo),
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
    val msg = when (error) {
        "service_off" -> "سرویس موقعیت‌مکانی دستگاه خاموش است.\nبرای استفاده از «اطراف من» باید GPS فعال باشه."
        "permission_forever" -> "دسترسی مکان مسدود شده.\nبرای فعال‌سازی باید از تنظیمات دستگاه اجازه بدی."
        "permission" -> "برای پیدا کردن آدم‌های نزدیک، به دسترسی موقعیت مکانی نیاز داریم."
        else -> "خطا در دریافت موقعیت مکانی"
    }
    val actionLabel = when (error) {
        "service_off" -> "فعال‌سازی GPS"
        "permission_forever" -> "راهنمای تنظیمات"
        "permission" -> "فعال‌سازی"
        else -> "تلاش مجدد"
    }
    val icon = when (error) {
        "service_off" -> Icons.Rounded.LocationOff
        "permission_forever" -> Icons.Rounded.Close
        "permission" -> Icons.Rounded.LocationOff
        else -> Icons.Rounded.Replay
    }
    val onAction: () -> Unit = when (error) {
        "service_off" -> onOpenGpsSettings
        "permission_forever" -> onOpenAppSettings
        "permission" -> onRequestPermission
        else -> onRetry
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VistaBrandColors.Indigo.copy(alpha = 0.65f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = VistaFontFamily,
                    color = MaterialTheme.vistaColors.contentSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                ),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Indigo),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

private fun checkAndAcquireLocation(context: Context, viewModel: NearbyViewModel) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
        locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    if (!isGpsEnabled) {
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
        val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastNetwork = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val loc = lastGps ?: lastNetwork
        if (loc != null) {
            viewModel.onLocationAcquired(loc.latitude, loc.longitude)
        } else {
            locationManager?.requestSingleUpdate(
                LocationManager.NETWORK_PROVIDER,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        viewModel.onLocationAcquired(location.latitude, location.longitude)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        viewModel.setLocationError("service_off")
                    }
                },
                null
            )
        }
    } catch (_: SecurityException) {
        viewModel.setLocationError("permission")
    } catch (_: Exception) {
        viewModel.setLocationError("failed")
    }
}

@Composable
private fun EmptyDeckView(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.People,
                contentDescription = null,
                tint = VistaBrandColors.Indigo.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "فعلاً کاربری در این بازه پیدا نشد",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = VistaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "می‌تونی محدوده فیلترها را از تنظیمات افزایش بدی.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = VistaFontFamily,
                    color = MaterialTheme.vistaColors.contentSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRefresh,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VistaBrandColors.Indigo),
            ) {
                Text(
                    text = "جستجوی دوباره",
                    style = MaterialTheme.typography.labelLarge.copy(
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
