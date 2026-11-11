package ir.coffevista.vista_native.features.services.ui.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import ir.coffevista.vista_native.core.designsystem.navigation.LocalVistaInternalLinkNavigator
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.core.model.navigation.VistaGameUrlHelper
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import org.json.JSONObject

data class DuelShareBridgeRequest(
    val requestId: String,
    val matchId: String,
    val duelUrl: String,
    val inviterName: String? = null,
    val inviterAvatar: String? = null,
    val entryFee: Long? = null,
    val targetUserId: String? = null,
    val conversationId: String? = null,
)

data class DuelShareBridgeResult(
    val requestId: String,
    val status: String,
    val conversationId: String? = null,
    val recipientName: String? = null,
    val message: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppWebScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
    restrictHost: String? = null,
    allowedPathPrefix: String? = null,
    miniAppMode: Boolean = false,
    appBarColor: Color? = null,
    appBarForegroundColor: Color? = null,
    useBackButton: Boolean = false,
    onShareDuelToChat: ((DuelShareBridgeRequest, (DuelShareBridgeResult) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val hostContext = LocalContext.current
    val internalNavigator = LocalVistaInternalLinkNavigator.current
    val view = LocalView.current
    val density = LocalDensity.current
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnShareDuel by rememberUpdatedState(onShareDuelToChat)
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val navBarBottomPx = WindowInsets.navigationBars.getBottom(density)
    val statusBarTopDp = with(density) { statusBarTopPx.toDp() }
    val navBarBottomDp = with(density) { navBarBottomPx.toDp() }
    val webTopSafeInsetDp = if (miniAppMode) (statusBarTopDp.value + MINI_APP_CHROME_HEIGHT_DP).roundToInt() else 0
    val webBottomSafeInsetDp = if (miniAppMode) navBarBottomDp.value.roundToInt() else 0

    val bridgeTopInsetDp = remember { AtomicInteger(webTopSafeInsetDp) }
    val bridgeBottomInsetDp = remember { AtomicInteger(webBottomSafeInsetDp) }
    SideEffect {
        bridgeTopInsetDp.set(webTopSafeInsetDp)
        bridgeBottomInsetDp.set(webBottomSafeInsetDp)
    }

    val appUsesLightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
    if (!view.isInEditMode) {
        DisposableEffect(view, miniAppMode, appUsesLightBars) {
            val window = (view.context as? Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            if (miniAppMode && controller != null) {
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
            onDispose {
                if (miniAppMode && controller != null) {
                    controller.isAppearanceLightStatusBars = appUsesLightBars
                    controller.isAppearanceLightNavigationBars = appUsesLightBars
                }
            }
        }
    }

    LaunchedEffect(webViewInstance, miniAppMode, webTopSafeInsetDp, webBottomSafeInsetDp) {
        if (miniAppMode) {
            injectGameSafeAreaCssVars(
                webView = webViewInstance,
                topDp = webTopSafeInsetDp,
                bottomDp = webBottomSafeInsetDp,
            )
        }
    }

    val barBg = appBarColor ?: MaterialTheme.colorScheme.background
    val fgColor = appBarForegroundColor ?: MaterialTheme.colorScheme.onBackground

    fun reloadWebPage() {
        loadError = null
        isLoading = true
        val webView = webViewInstance
        if (webView == null) return
        val current = webView.url
        if (current.isNullOrBlank() || current == "about:blank") {
            webView.loadUrl(url)
        } else {
            webView.reload()
        }
    }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onBack()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val webSurface: @Composable (Modifier) -> Unit = { contentModifier ->
            Box(
                modifier = contentModifier.imePadding(),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            if (miniAppMode) {
                                setBackgroundColor(android.graphics.Color.parseColor("#F5F1FE"))
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.setSupportMultipleWindows(false)
                            settings.javaScriptCanOpenWindowsAutomatically = false

                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            if (allowedPathPrefix == "/game") {
                                val bridge = object {
                                    @JavascriptInterface
                                    fun shareDuelToChat(payloadJson: String?) {
                                        post {
                                            if (!isTrustedGameOrigin(this@apply.url)) return@post
                                            val req = parseDuelShareBridgeRequest(payloadJson)
                                            if (req == null) {
                                                dispatchBridgeResult(
                                                    webView = this@apply,
                                                    result = DuelShareBridgeResult(
                                                        requestId = "",
                                                        status = "error",
                                                        message = "اطلاعات دوئل نامعتبر است",
                                                    ),
                                                )
                                                return@post
                                            }
                                            val handler = currentOnShareDuel
                                            if (handler == null) {
                                                dispatchBridgeResult(
                                                    webView = this@apply,
                                                    result = DuelShareBridgeResult(
                                                        requestId = req.requestId,
                                                        status = "unsupported",
                                                    ),
                                                )
                                                return@post
                                            }
                                            handler(req) { result ->
                                                post {
                                                    dispatchBridgeResult(this@apply, result)
                                                }
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun shareExternal(payloadJson: String?) {
                                        post {
                                            if (!isTrustedGameOrigin(this@apply.url)) return@post
                                            val parsed = runCatching { JSONObject(payloadJson.orEmpty()) }.getOrNull()
                                                ?: return@post
                                            val shareUrl = parsed.optString("url").trim()
                                            val shareText = parsed.optString("text").trim()
                                            val shareTitle = parsed.optString("title").trim().ifBlank { "ویستا کوییز" }
                                            if (shareUrl.isBlank() && shareText.isBlank()) return@post
                                            val combined = listOf(shareText, shareUrl).filter { it.isNotBlank() }.joinToString("\n")
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                                                putExtra(Intent.EXTRA_TEXT, combined)
                                            }
                                            runCatching {
                                                hostContext.startActivity(
                                                    Intent.createChooser(sendIntent, "ارسال دعوت دوئل")
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                                )
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun openConversation(conversationId: String?) {
                                        post {
                                            if (!isTrustedGameOrigin(this@apply.url)) return@post
                                            val cleanId = conversationId?.trim()?.takeIf { it.isNotEmpty() } ?: return@post
                                            internalNavigator?.openUrl("vista://chat/$cleanId")
                                        }
                                    }

                                    @JavascriptInterface
                                    fun closeGame() {
                                        post {
                                            if (!isTrustedGameOrigin(this@apply.url)) return@post
                                            currentOnBack()
                                        }
                                    }

                                    @JavascriptInterface
                                    fun getSafeAreaTopDp(): Int = bridgeTopInsetDp.get()

                                    @JavascriptInterface
                                    fun getSafeAreaBottomDp(): Int = bridgeBottomInsetDp.get()
                                }
                                addJavascriptInterface(bridge, "VistaGameBridge")
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadingProgress = newProgress / 100f
                                    isLoading = newProgress < 100
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    loadError = null
                                    if (miniAppMode) {
                                        injectGameSafeAreaCssVars(
                                            webView = view,
                                            topDp = bridgeTopInsetDp.get(),
                                            bottomDp = bridgeBottomInsetDp.get(),
                                        )
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    if (miniAppMode) {
                                        injectGameSafeAreaCssVars(
                                            webView = view,
                                            topDp = bridgeTopInsetDp.get(),
                                            bottomDp = bridgeBottomInsetDp.get(),
                                        )
                                    }
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    if (miniAppMode) {
                                        injectGameSafeAreaCssVars(
                                            webView = view,
                                            topDp = bridgeTopInsetDp.get(),
                                            bottomDp = bridgeBottomInsetDp.get(),
                                        )
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val requestedUrl = request?.url?.toString()
                                    if (!requestedUrl.isNullOrBlank()) {
                                        val isVistaCustomScheme = requestedUrl.startsWith("vista://", ignoreCase = true)
                                        val isNonGameVistaLink =
                                            !isNavigationAllowed(
                                                url = requestedUrl,
                                                restrictHost = restrictHost,
                                                allowedPathPrefix = allowedPathPrefix,
                                            ) &&
                                                VistaGameUrlHelper.isVistaWebHost(
                                                    runCatching { java.net.URI(requestedUrl).host }.getOrNull(),
                                                )
                                        if ((isVistaCustomScheme || isNonGameVistaLink) &&
                                            internalNavigator?.openUrl(requestedUrl) == true
                                        ) {
                                            return true
                                        }
                                    }
                                    return !isNavigationAllowed(
                                        url = requestedUrl,
                                        restrictHost = restrictHost,
                                        allowedPathPrefix = allowedPathPrefix,
                                    )
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?,
                                ) {
                                    handler?.cancel()
                                    isLoading = false
                                    loadError = "اتصال امن به این سرویس برقرار نشد. لطفاً بعداً دوباره تلاش کنید."
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?,
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        loadError = "این سرویس در حال حاضر در دسترس نیست."
                                    }
                                }
                            }

                            webViewInstance = this
                            val startLoad = { loadUrl(url) }
                            if (restrictHost == null) {
                                startLoad()
                            } else {
                                // A scoped SSO surface must not inherit a prior broad web session.
                                CookieManager.getInstance().removeAllCookies {
                                    CookieManager.getInstance().flush()
                                    startLoad()
                                }
                            }
                        }
                    },
                    update = { view ->
                        webViewInstance = view
                    },
                )

                if (loadError != null) {
                    val errorBgModifier = if (miniAppMode) {
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFF5F1FE),
                                        Color(0xFFFBF4FA),
                                        Color(0xFFF1F8FE),
                                    ),
                                ),
                            )
                            .padding(
                                top = statusBarTopDp + MINI_APP_CHROME_HEIGHT_DP.dp,
                                bottom = navBarBottomDp,
                            )
                    } else {
                        Modifier
                            .fillMaxSize()
                            .background(barBg)
                    }
                    val errorTextColor = if (miniAppMode) Color(0xFF4C1D95) else fgColor
                    Box(
                        modifier = errorBgModifier,
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = loadError.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = VistaFontFamily,
                                    color = errorTextColor,
                                ),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { reloadWebPage() }) {
                                Text(
                                    text = "تلاش مجدد",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = VistaFontFamily,
                                        color = if (miniAppMode) Color(0xFF7C3AED) else VistaBrandColors.Indigo,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (miniAppMode) {
            Box(modifier = modifier.fillMaxSize()) {
                webSurface(Modifier.fillMaxSize())

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.8f),
                        trackColor = Color.Transparent,
                    )
                }

                FloatingMiniAppTopBar(
                    canGoBack = { webViewInstance?.canGoBack() == true },
                    onClose = onBack,
                    onReload = { reloadWebPage() },
                    onGoBack = { webViewInstance?.goBack() },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        } else {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = barBg,
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                if (title.isNotBlank()) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = VistaFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = fgColor,
                                        ),
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = {
                                        if (webViewInstance?.canGoBack() == true) {
                                            webViewInstance?.goBack()
                                        } else {
                                            onBack()
                                        }
                                    },
                                ) {
                                    val imageVector = if (useBackButton) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Close
                                    Icon(
                                        imageVector = imageVector,
                                        contentDescription = "بازگشت",
                                        tint = fgColor,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = barBg,
                            ),
                        )
                        if (isLoading) {
                            LinearProgressIndicator(
                                progress = { loadingProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = if (appBarColor != null) Color.White.copy(alpha = 0.5f) else VistaBrandColors.Indigo,
                                trackColor = Color.Transparent,
                            )
                        }
                    }
                },
            ) { innerPadding ->
                webSurface(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

private const val MINI_APP_CHROME_HEIGHT_DP = 44f

@Composable
private fun FloatingMiniAppTopBar(
    canGoBack: () -> Boolean,
    onClose: () -> Unit,
    onReload: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val pillBackground = Color(0xFF1A1D26).copy(alpha = 0.56f)
    val pillBorder = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.22f))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onClose,
            shape = RoundedCornerShape(16.dp),
            color = pillBackground,
            border = pillBorder,
            shadowElevation = 2.dp,
            modifier = Modifier.height(32.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "بستن",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "بستن",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = VistaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color.White,
                    ),
                )
            }
        }

        Box {
            Surface(
                onClick = { menuExpanded = true },
                shape = CircleShape,
                color = pillBackground,
                border = pillBorder,
                shadowElevation = 2.dp,
                modifier = Modifier.size(width = 36.dp, height = 32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "گزینه‌ها",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "بارگذاری مجدد",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = VistaFontFamily,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onReload()
                    },
                )
                if (canGoBack()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "صفحه قبل",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = VistaFontFamily,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onGoBack()
                        },
                    )
                }
            }
        }
    }
}

private fun injectGameSafeAreaCssVars(
    webView: WebView?,
    topDp: Int,
    bottomDp: Int,
) {
    if (webView == null) return
    val js = """
        (function() {
          var root = document.documentElement;
          if (!root) return;
          root.style.setProperty('--vista-game-safe-top', '${topDp}px');
          root.style.setProperty('--vista-game-safe-bottom', '${bottomDp}px');
          window.dispatchEvent(new CustomEvent('vista-game-insets-changed', { detail: { top: $topDp, bottom: $bottomDp } }));
        })();
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}

internal fun shouldUseMiniAppMode(url: String?): Boolean {
    val path = runCatching { java.net.URI(url.orEmpty()).path }.getOrNull().orEmpty()
    return path == "/game" || path.startsWith("/game/")
}

internal fun isNavigationAllowed(
    url: String?,
    restrictHost: String?,
    allowedPathPrefix: String?,
): Boolean {
    if (restrictHost == null) return true
    val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false

    val scheme = uri.scheme.orEmpty().lowercase()
    if (scheme != "http" && scheme != "https") {
        return scheme == "about" || scheme == "data"
    }

    val host = uri.host.orEmpty().lowercase()
    val target = restrictHost.lowercase()
    val isAllowedHost = host == target ||
        host.endsWith(".$target") ||
        (target.contains("cafevista.ir") && (host == "coffevista.ir" || host.endsWith(".coffevista.ir"))) ||
        (target.contains("coffevista.ir") && (host == "cafevista.ir" || host.endsWith(".cafevista.ir")))

    if (!isAllowedHost) return false

    val prefix = allowedPathPrefix?.takeIf { it.isNotBlank() } ?: return true
    val path = uri.path.orEmpty().ifBlank { "/" }
    return path == prefix || path.startsWith("$prefix/")
}

internal fun isTrustedGameOrigin(currentUrl: String?): Boolean {
    val uri = runCatching { java.net.URI(currentUrl.orEmpty()) }.getOrNull() ?: return false
    if (uri.scheme?.lowercase() != "https") return false
    val host = uri.host?.lowercase() ?: return false
    return host == "cafevista.ir" ||
        host == "www.cafevista.ir" ||
        host == "coffevista.ir" ||
        host == "www.coffevista.ir"
}

internal fun parseDuelShareBridgeRequest(payloadJson: String?): DuelShareBridgeRequest? {
    val obj = runCatching { JSONObject(payloadJson.orEmpty()) }.getOrNull() ?: return null
    val matchId = obj.optString("matchId").trim()
    if (!VistaGameUrlHelper.isValidUuid(matchId)) return null
    val requestId = obj.optString("requestId").trim()
    val duelUrl = obj.optString("duelUrl").trim().ifBlank { VistaGameUrlHelper.buildDuelWebUrl(matchId) }
    val inviterName = obj.optString("inviterName").trim().takeIf { it.isNotEmpty() }
    val inviterAvatar = obj.optString("inviterAvatar").trim().takeIf { it.isNotEmpty() }
    val entryFee = if (obj.has("entryFee") && !obj.isNull("entryFee")) {
        obj.optLong("entryFee", -1L).takeIf { it >= 0L }
    } else null
    val targetUserId = obj.optString("targetUserId").trim().takeIf { it.isNotEmpty() }
    val conversationId = obj.optString("conversationId").trim().takeIf { it.isNotEmpty() }

    return DuelShareBridgeRequest(
        requestId = requestId,
        matchId = matchId.lowercase(),
        duelUrl = duelUrl,
        inviterName = inviterName,
        inviterAvatar = inviterAvatar,
        entryFee = entryFee,
        targetUserId = targetUserId,
        conversationId = conversationId,
    )
}

private fun dispatchBridgeResult(webView: WebView, result: DuelShareBridgeResult) {
    val json = JSONObject().apply {
        put("requestId", result.requestId)
        put("status", result.status)
        result.conversationId?.let { put("conversationId", it) }
        result.recipientName?.let { put("recipientName", it) }
        result.message?.let { put("message", it) }
    }.toString()
    val quoted = JSONObject.quote(json)
    webView.evaluateJavascript("window.__vistaNativeBridgeReceive && window.__vistaNativeBridgeReceive($quoted)", null)
}
