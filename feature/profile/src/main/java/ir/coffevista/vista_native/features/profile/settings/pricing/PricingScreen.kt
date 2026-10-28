package ir.coffevista.vista_native.features.profile.settings.pricing

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.ConnectionState
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.request.PurchaseRequest
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSection

private const val BAZAAR_RSA_PUBLIC_KEY =
    "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwDFm5WUUaqhq2Ha3Al1b8OmGNnBCsPNNjbp04bDJUCFo/QTF6P9JPpqAwVeo0MqR84WEo3KKZBwIYA+aJZuVPYjZ5J6S9tJ1McWKY0v4+6s4PR07Z5N55TMCFz0ofi0GsnpQHVp94zLvhYBQqQatLBq0XsmOiPWTHt2UgP7d0ib2anR2LtRE/1J1WPe2/lji4u8b1+CLRgfT3hzdfqv2RkrpAn69GYqEfUuohQyQK8CAwEAAQ=="

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

data class PricingPlanUiModel(
    val id: String,
    val title: String,
    val price: String,
    val description: String,
    val badge: String? = null,
)

data class PremiumFeatureUiModel(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    viewModel: PricingViewModel = hiltViewModel(),
) {
    val goldGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFFA000)),
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val plans = uiState.plans
    val context = LocalContext.current

    val features = listOf(
        PremiumFeatureUiModel(
            Icons.Outlined.Verified,
            "نشان تأیید طلایی",
            "تیک طلایی کنار نام شما در پروفایل، پست‌ها، کامنت‌ها و چت نمایش داده می‌شود.",
        ),
        PremiumFeatureUiModel(
            Icons.Outlined.Timelapse,
            "استوری ۴۸ ساعته",
            "استوری‌های شما تا ۴۸ ساعت در فید باقی می‌مانند (کاربران عادی: ۲۴ ساعت).",
        ),
        PremiumFeatureUiModel(
            Icons.Outlined.EditNote,
            "ویرایش پست پس از انتشار",
            "کپشن و رسانه پست‌های خود را هر زمان ویرایش کنید.",
        ),
        PremiumFeatureUiModel(
            Icons.Outlined.TextSnippet,
            "پست‌های طولانی‌تر",
            "نوشتن متن تا ۱٬۰۰۰ کاراکتر (کاربران عادی: ۵۰۰ کاراکتر).",
        ),
        PremiumFeatureUiModel(
            Icons.Outlined.UploadFile,
            "ارسال فایل تا ۱۰۰ مگابایت",
            "می‌توانید تصویر، کلیپ، موزیک، PDF و فایل‌ها را تا ۱۰۰ مگابایت ارسال کنید (کاربران عادی: ۱۵ مگابایت).",
        ),
        PremiumFeatureUiModel(
            Icons.Outlined.Collections,
            "ویدیو تا ۲ دقیقه",
            "آپلود و برش ویدیو تا ۲ دقیقه (کاربران عادی: ۱ دقیقه).",
        ),
        PremiumFeatureUiModel(
            Icons.Outlined.MusicNote,
            "برش موزیک تا ۶۰ ثانیه",
            "در پست‌های خود تا ۶۰ ثانیه موزیک انتخاب کنید؛ با گزینه‌های ۱۵، ۳۰ یا ۶۰ ثانیه (کاربران عادی: فقط ۱۵ ثانیه).",
        ),
        PremiumFeatureUiModel(
            Icons.Outlined.Collections,
            "آلبوم چندعکسی تا ۱۰ عکس",
            "در هر پست تا ۱۰ عکس به‌صورت اسلایدی منتشر کنید (کاربران عادی: ۳ عکس).",
        ),
        PremiumFeatureUiModel(
            Icons.Outlined.VisibilityOff,
            "کنترل آمار لایک و کامنت",
            "برای هر پست نمایش تعداد لایک و کامنت را روشن یا خاموش کنید.",
        ),
        PremiumFeatureUiModel(
            Icons.Outlined.TrendingUp,
            "اولویت در جستجو",
            "پروفایل شما در نتایج جستجو بالاتر دیده می‌شود.",
        ),
        PremiumFeatureUiModel(
            Icons.Outlined.Block,
            "بدون تبلیغات",
            "تجربه‌ای تمیزتر بدون نمایش تبلیغ در بخش‌های اصلی برنامه.",
        ),
    )

    var selectedPlanIndex by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var showPaymentConfirmation by remember { mutableStateOf(false) }
    val selectedPlan = plans.getOrNull(selectedPlanIndex.coerceAtLeast(0)) ?: plans.firstOrNull()
    val snackbarHostState = remember { SnackbarHostState() }
    val payment = remember(context) {
        Payment(
            context = context.applicationContext,
            config = PaymentConfiguration(
                localSecurityCheck = SecurityCheck.Enable(rsaPublicKey = BAZAAR_RSA_PUBLIC_KEY),
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

    fun launchBazaarProduct(productId: String, isSubscription: Boolean = false) {
        val activity = context.findComponentActivity()
        if (activity == null) {
            viewModel.onBazaarPurchaseFailed("امکان باز کردن درگاه پرداخت کافه‌بازار وجود ندارد.")
            return
        }
        viewModel.onBazaarPurchaseStarted()
        val executePurchase = {
            runCatching {
                if (isSubscription) {
                    payment.subscribeProduct(
                        registry = activity.activityResultRegistry,
                        request = PurchaseRequest(
                            productId = productId,
                            payload = viewModel.resolveBazaarPayload(),
                        ),
                    ) {
                        purchaseSucceed { info ->
                            viewModel.verifyBazaarPurchase(
                                productId = productId,
                                purchaseToken = info.purchaseToken,
                                packageName = info.packageName.ifBlank { context.packageName },
                            )
                        }
                        purchaseCanceled { viewModel.onBazaarPurchaseCanceled() }
                        purchaseFailed { throwable -> viewModel.onBazaarPurchaseFailed(throwable.message) }
                        failedToBeginFlow { throwable ->
                            viewModel.onBazaarPurchaseFailed(
                                throwable.message ?: "برنامه کافه‌بازار روی دستگاه یافت نشد یا به‌روز نیست. لطفاً برنامه بازار را نصب یا به‌روزرسانی کنید.",
                            )
                        }
                    }
                } else {
                    payment.purchaseProduct(
                        registry = activity.activityResultRegistry,
                        request = PurchaseRequest(
                            productId = productId,
                            payload = viewModel.resolveBazaarPayload(),
                        ),
                    ) {
                        purchaseSucceed { info ->
                            viewModel.verifyBazaarPurchase(
                                productId = productId,
                                purchaseToken = info.purchaseToken,
                                packageName = info.packageName.ifBlank { context.packageName },
                            )
                        }
                        purchaseCanceled { viewModel.onBazaarPurchaseCanceled() }
                        purchaseFailed { throwable -> viewModel.onBazaarPurchaseFailed(throwable.message) }
                        failedToBeginFlow { throwable ->
                            viewModel.onBazaarPurchaseFailed(
                                throwable.message ?: "برنامه کافه‌بازار روی دستگاه یافت نشد یا به‌روز نیست. لطفاً برنامه بازار را نصب یا به‌روزرسانی کنید.",
                            )
                        }
                    }
                }
            }.onFailure { throwable ->
                viewModel.onBazaarPurchaseFailed(throwable.message)
            }
        }
        if (bazaarConnection?.getState() == ConnectionState.Connected) {
            executePurchase()
        } else {
            runCatching {
                bazaarConnection = payment.connect {
                    connectionSucceed { executePurchase() }
                    connectionFailed { throwable ->
                        viewModel.onBazaarPurchaseFailed(
                            throwable.message ?: "اتصال به درگاه کافه‌بازار برقرار نشد. از نصب و ورود به برنامه بازار اطمینان حاصل کنید.",
                        )
                    }
                    disconnected {}
                }
            }.onFailure { throwable ->
                viewModel.onBazaarPurchaseFailed(throwable.message)
            }
        }
    }

    fun launchBazaarSubscription(planId: String) = launchBazaarProduct(planId, isSubscription = true)

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) { snackbarHostState.showSnackbar(message) }
    }
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) { snackbarHostState.showSnackbar(message) }
    }
    uiState.paymentUrl?.let { url ->
        LaunchedEffect(url) {
            val launched = runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }.isSuccess
            viewModel.paymentGatewayLaunched()
            if (!launched) snackbarHostState.showSnackbar("امکان باز کردن درگاه پرداخت وجود ندارد")
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "ویستا پریمیوم",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            ) {
                val tabs = listOf("اشتراک ویژه", "بسته‌های سکه", "نشان‌های تأیید")
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Color(0xFFFFA000),
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                    ),
                                )
                            },
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // Top Hero Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(goldGradient)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF5D4037),
                                        modifier = Modifier.size(44.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "ویستا پریمیوم",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF3E2723),
                                        fontSize = 22.sp,
                                    ),
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "تجربه‌ای فراتر از استاندارد با امکانات ویژه",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF4E342E),
                                        fontSize = 13.sp,
                                    ),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        // Plan Cards Selection
                        VistaSettingsSection(title = "انتخاب پلن اشتراک")

                        if (uiState.isLoading) {
                            Text(
                                text = "در حال دریافت کاتالوگ اشتراک…",
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else if (uiState.errorMessage != null) {
                            Text(
                                text = uiState.errorMessage!!,
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        if (plans.isNotEmpty()) Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            plans.forEachIndexed { index, plan ->
                                val isSelected = selectedPlanIndex == index
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .scale(if (isSelected) pulseScale else 1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(16.dp),
                                        )
                                        .clickable { selectedPlanIndex = index },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surface,
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        if (plan.badge != null) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFFB300),
                                                modifier = Modifier.padding(bottom = 6.dp),
                                            ) {
                                                Text(
                                                    text = plan.badge,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF3E2723),
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }

                                        Text(
                                            text = plan.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isSelected) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface,
                                            ),
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = plan.price,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            ),
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Feature List
                        VistaSettingsSection(title = "ویژگی‌های حساب پریمیوم")

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                features.forEachIndexed { index, feature ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFFFF8E1)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = feature.icon,
                                                contentDescription = null,
                                                tint = Color(0xFFFFA000),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = feature.title,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                ),
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = feature.subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                ),
                                            )
                                        }
                                    }
                                    if (index < features.size - 1) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Purchase CTA Button
                        Button(
                            onClick = {
                                when {
                                    uiState.pendingBazaarVerification != null -> viewModel.retryBazaarVerification()
                                    uiState.pendingTrackId != null -> viewModel.verifyPayment()
                                    else -> showPaymentConfirmation = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFA000),
                                contentColor = Color(0xFF3E2723),
                            ),
                            enabled = !uiState.isLoading && selectedPlan != null && !uiState.isRequestingPayment && !uiState.isVerifyingPayment,
                        ) {
                            if (uiState.isRequestingPayment || uiState.isVerifyingPayment) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF3E2723),
                                    strokeWidth = 2.dp,
                                )
                            } else Text(
                                text = when {
                                    uiState.pendingBazaarVerification != null -> "تایید مجدد خرید بازار"
                                    uiState.pendingTrackId != null -> "تایید پرداخت"
                                    else -> selectedPlan?.let { "خرید اشتراک ${it.title} (${it.price})" }
                                        ?: "کاتالوگ اشتراک در دسترس نیست"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                            )
                        }
                    }

                    1 -> {
                        // Coin Packages Tab
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF7E57C2), Color(0xFF5E35B1)),
                                    ),
                                )
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Verified,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "بسته‌های سکه ویستا",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                    ),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "برای حمایت از سازندگان، هدایای ویژه و قابلیت‌های اختصاصی",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.5.sp,
                                    ),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        VistaSettingsSection(title = "بسته‌های قابل خرید")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            uiState.coinPackages.forEach { pkg ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFD54F).copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "🪙",
                                                fontSize = 24.sp,
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "%,d سکه".format(pkg.coins),
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                    ),
                                                )
                                                if (pkg.bonus > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                                    ) {
                                                        Text(
                                                            text = "+%,d هدیه".format(pkg.bonus),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = Color(0xFF2E7D32),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp,
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "%,d تومان".format(pkg.priceToman),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                            )
                                        }
                                        Button(
                                            onClick = { launchBazaarProduct(pkg.id, isSubscription = false) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF7E57C2),
                                            ),
                                            enabled = !uiState.isRequestingPayment,
                                        ) {
                                            Text(
                                                text = "خرید",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Badge Store Tab
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF263238), Color(0xFF37474F)),
                                    ),
                                )
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "فروشگاه نشان‌های ویژه",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                    ),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "اعتبار رسمی، جایگاه نخبگان و تمایز در سراسر برنامه",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.5.sp,
                                    ),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        VistaSettingsSection(title = "نشان‌های موجود")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            uiState.badges.forEach { badge ->
                                val isGold = badge.badgeType == "gold"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (isGold) Color(0xFFFFB300) else Color(0xFF90A4AE),
                                            shape = RoundedCornerShape(16.dp),
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isGold) Color(0xFFFFFDE7) else Color(0xFFECEFF1),
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Verified,
                                                contentDescription = null,
                                                tint = if (isGold) Color(0xFFFFA000) else Color(0xFF263238),
                                                modifier = Modifier.size(28.dp),
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = badge.title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = if (isGold) Color(0xFF5D4037) else Color(0xFF263238),
                                                ),
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = badge.priceFormatted,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isGold) Color(0xFFE65100) else Color(0xFF37474F),
                                                ),
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = badge.description,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF455A64),
                                                lineHeight = 20.sp,
                                            ),
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Button(
                                            onClick = { viewModel.requestPayment(badge.id) },
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isGold) Color(0xFFFFA000) else Color(0xFF263238),
                                                contentColor = Color.White,
                                            ),
                                            enabled = !uiState.isRequestingPayment,
                                        ) {
                                            Text(
                                                text = "خرید و فعال‌سازی نشان",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        if (showPaymentConfirmation && selectedPlan != null) {
            AlertDialog(
                onDismissRequest = { showPaymentConfirmation = false },
                title = { Text("خرید از طریق کافه‌بازار") },
                text = { Text("برای فعال‌سازی اشتراک ${selectedPlan.title} (${selectedPlan.price}) به درگاه پرداخت درون‌برنامه‌ای کافه‌بازار منتقل می‌شوید.") },
                confirmButton = {
                    TextButton(onClick = {
                        showPaymentConfirmation = false
                        launchBazaarSubscription(selectedPlan.id)
                    }) { Text("پرداخت با بازار") }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentConfirmation = false }) { Text("انصراف") }
                },
            )
        }
    }
}
