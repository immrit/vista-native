package ir.coffevista.vista_native.features.services.ui.ai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ir.coffevista.vista_native.core.designsystem.navigation.LocalVistaInternalLinkNavigator
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.model.navigation.VistaGameUrlHelper
import ir.coffevista.vista_native.features.services.data.ServicesRepository
import java.net.URI
import java.net.URLEncoder

private const val VISTA_AI_ORIGIN = "https://ai.cafevista.ir"
private const val VISTA_AI_HOST = "ai.cafevista.ir"

internal class VistaAiCallbacksHolder {
    var onPageStarted: (() -> Unit)? = null
    var onPageFinished: ((WebView?, String?) -> Unit)? = null
    var onProgressChanged: ((Int) -> Unit)? = null
    var onLoadError: ((String) -> Unit)? = null
    var onRequestSsoReauth: (() -> Unit)? = null
    var onOpenInternalLink: ((String) -> Boolean)? = null
    var onOpenExternalBrowser: ((Uri) -> Unit)? = null
    var onLaunchFileChooser: ((ValueCallback<Array<Uri>>?, Intent?) -> Boolean)? = null
    var currentThemeName: String = "dark"
}

@Keep
private class VistaAiJsBridge(
    private val webViewProvider: () -> WebView,
    private val callbacksHolder: VistaAiCallbacksHolder,
) {
    @JavascriptInterface
    fun requestSsoReauth() {
        val wv = webViewProvider()
        wv.post {
            val host = runCatching { URI(wv.url.orEmpty()).host?.lowercase() }.getOrNull()
            if (host == VISTA_AI_HOST) {
                callbacksHolder.onRequestSsoReauth?.invoke()
            }
        }
    }

    @JavascriptInterface
    fun getTheme(): String = callbacksHolder.currentThemeName
}

/**
 * Coordinates Vista AI WebView cookie cleanup on sign-out/user switch while each screen
 * owns a clean Activity-bound WebView backed by Chromium disk cache, Service Worker, and IndexedDB.
 */
object VistaAiWebViewRegistry {
    @Volatile
    private var activeUserId: String? = null

    internal fun onActiveUser(userId: String) {
        val previous = activeUserId
        if (previous != null && previous != userId) {
            clearCookies()
        }
        activeUserId = userId
    }

    fun clearForUser(userId: String? = null) {
        if (userId == null || activeUserId == userId) {
            clearCookies()
            if (userId == null || activeUserId == userId) {
                activeUserId = null
            }
        }
    }

    private fun clearCookies() {
        runCatching {
            val cm = CookieManager.getInstance()
            cm.setCookie(VISTA_AI_ORIGIN, "vista_ai_access_token=; Max-Age=0; Path=/")
            cm.setCookie(VISTA_AI_ORIGIN, "vista_ai_user_meta=; Max-Age=0; Path=/")
            cm.flush()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VistaAiScreen(
    userId: String,
    repository: ServicesRepository,
    routeOverride: String? = null,
    onOpenWebFallback: (url: String, title: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val internalNavigator = LocalVistaInternalLinkNavigator.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    val themeName = if (isDarkTheme) "dark" else "light"
    val surfaceBgColor = MaterialTheme.colorScheme.background

    val resolvedTarget = remember(routeOverride) {
        resolveVistaAiTarget(routeOverride)
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var needsInitialLoad by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var isBootstrappingSso by remember { mutableStateOf(false) }
    var isPageLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reauthNonce by remember { mutableIntStateOf(0) }
    var lastReauthAtMs by remember { mutableStateOf(0L) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var webViewInitRetryKey by remember { mutableIntStateOf(0) }
    val callbacksHolder = remember { VistaAiCallbacksHolder() }

    LaunchedEffect(userId) {
        VistaAiWebViewRegistry.onActiveUser(userId)
    }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = pendingFileCallback
        pendingFileCallback = null
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        callback?.onReceiveValue(uris)
    }

    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val navBottomPx = WindowInsets.navigationBars.getBottom(density)
    val isImeVisible = imeBottomPx > navBottomPx
    val bottomIslandClearance = if (isImeVisible) 0.dp else 74.dp

    fun triggerSilentSsoReauth() {
        val now = System.currentTimeMillis()
        if (now - lastReauthAtMs > 3_500L) {
            lastReauthAtMs = now
            reauthNonce += 1
        }
    }

    SideEffect {
        callbacksHolder.apply {
            currentThemeName = themeName
            onPageStarted = {
                isPageLoading = true
                loadError = null
            }
            onPageFinished = { view, loadedUrl ->
                isPageLoading = false
                loadingProgress = 1f
                canGoBack = runCatching { view?.canGoBack() == true }.getOrDefault(false)
                val parsed = loadedUrl?.let { runCatching { URI(it) }.getOrNull() }
                val path = parsed?.path.orEmpty()
                if (parsed?.host?.equals(VISTA_AI_HOST, ignoreCase = true) == true &&
                    isManualAiAuthPath(path)
                ) {
                    // Never leave user on manual login inside native app; re-exchange SSO automatically.
                    triggerSilentSsoReauth()
                } else {
                    runCatching {
                        view?.evaluateJavascript(
                            "window.__vistaAiSyncTheme && window.__vistaAiSyncTheme('$themeName')",
                            null,
                        )
                    }
                }
            }
            onProgressChanged = { progress ->
                loadingProgress = (progress / 100f).coerceIn(0f, 1f)
                if (progress >= 95) {
                    isPageLoading = false
                }
            }
            onLoadError = { message ->
                isPageLoading = false
                loadError = message
            }
            onRequestSsoReauth = { triggerSilentSsoReauth() }
            onOpenInternalLink = { url ->
                val result = internalNavigator?.openUrl(url) == true
                if (!result && url.startsWith("https://", ignoreCase = true)) {
                    onOpenWebFallback(url, "ویستا")
                    true
                } else {
                    result
                }
            }
            onOpenExternalBrowser = { uri ->
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
            onLaunchFileChooser = { callback, intent ->
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = callback
                if (intent != null) {
                    runCatching {
                        fileChooserLauncher.launch(intent)
                        true
                    }.getOrElse {
                        pendingFileCallback = null
                        callback?.onReceiveValue(null)
                        false
                    }
                } else {
                    pendingFileCallback = null
                    callback?.onReceiveValue(null)
                    false
                }
            }
        }
    }

    LaunchedEffect(themeName, webViewInstance) {
        webViewInstance?.let { wv ->
            runCatching {
                wv.setBackgroundColor(surfaceBgColor.toArgb())
                wv.evaluateJavascript(
                    "window.__vistaAiSyncTheme && window.__vistaAiSyncTheme('$themeName')",
                    null,
                )
            }
        }
    }

    LaunchedEffect(userId, resolvedTarget.cacheKey, needsInitialLoad, reauthNonce, webViewInstance) {
        val wv = webViewInstance ?: return@LaunchedEffect
        val shouldExchangeSso = (needsInitialLoad || reauthNonce > 0) && resolvedTarget.useSso
        if (!shouldExchangeSso) {
            if (needsInitialLoad) {
                needsInitialLoad = false
                isPageLoading = true
                runCatching { wv.loadUrl(resolvedTarget.directUrl) }
            }
            return@LaunchedEffect
        }

        isBootstrappingSso = true
        loadError = null
        runCatching {
            val ticket = repository.createVistaSsoTicket(clientId = "vista-ai")
            val encodedTicket = URLEncoder.encode(ticket, "UTF-8")
            val nextSuffix = resolvedTarget.nextPath?.let { path ->
                "&next=${URLEncoder.encode(path, "UTF-8")}"
            }.orEmpty()
            "$VISTA_AI_ORIGIN/auth/callback?ticket=$encodedTicket&embedded=1&theme=$themeName$nextSuffix"
        }.onSuccess { callbackUrl ->
            needsInitialLoad = false
            isBootstrappingSso = false
            isPageLoading = true
            runCatching { wv.loadUrl(callbackUrl) }
        }.onFailure { err ->
            isBootstrappingSso = false
            isPageLoading = false
            loadError = err.message?.takeIf { it.isNotBlank() }
                ?: "اتصال یکپارچه با هوش مصنوعی ویستا برقرار نشد."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
            callbacksHolder.apply {
                onPageStarted = null
                onPageFinished = null
                onProgressChanged = null
                onLoadError = null
                onRequestSsoReauth = null
                onOpenInternalLink = null
                onOpenExternalBrowser = null
                onLaunchFileChooser = null
            }
            webViewInstance?.let { wv ->
                runCatching {
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.clearHistory()
                    wv.removeJavascriptInterface("VistaAiBridge")
                    wv.destroy()
                }
            }
            webViewInstance = null
        }
    }

    BackHandler(enabled = canGoBack) {
        runCatching {
            if (webViewInstance?.canGoBack() == true) {
                webViewInstance?.goBack()
            } else {
                canGoBack = false
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(surfaceBgColor)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = bottomIslandClearance)
                .imePadding(),
        ) {
            androidx.compose.runtime.key(webViewInitRetryKey, resolvedTarget.cacheKey) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        runCatching {
                            val webView = buildConfiguredVistaAiWebView(
                                context = ctx,
                                bgColorArgb = surfaceBgColor.toArgb(),
                                callbacksHolder = callbacksHolder,
                            )
                            webView.post {
                                webViewInstance = webView
                                needsInitialLoad = true
                            }
                            webView
                        }.getOrElse { err ->
                            loadError = err.message?.takeIf { it.isNotBlank() }
                                ?: "مرورگر داخلی دستگاه آماده نشد. لطفاً Android System WebView را بررسی کنید."
                            android.widget.FrameLayout(ctx)
                        }
                    },
                    update = { view ->
                        if (view is WebView) {
                            runCatching { view.setBackgroundColor(surfaceBgColor.toArgb()) }
                        }
                    },
                )
            }

            if (isBootstrappingSso || (isPageLoading && loadingProgress < 0.95f)) {
                LinearProgressIndicator(
                    progress = { if (isBootstrappingSso) 0.25f else loadingProgress.coerceAtLeast(0.08f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                )
            }

            if (isBootstrappingSso && webViewInstance?.url.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(surfaceBgColor.copy(alpha = 0.92f))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "در حال ورود خودکار به ویستا AI...",
                        fontFamily = VistaFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            if (loadError != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(surfaceBgColor)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "بارگذاری هوش مصنوعی ویستا با خطا مواجه شد",
                        fontFamily = VistaFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = loadError.orEmpty(),
                        fontFamily = VistaFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            loadError = null
                            if (webViewInstance == null) {
                                webViewInitRetryKey += 1
                            } else if (resolvedTarget.useSso) {
                                reauthNonce += 1
                            } else {
                                webViewInstance?.reload()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = "تلاش مجدد",
                            fontFamily = VistaFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

private fun isManualAiAuthPath(path: String): Boolean {
    val clean = path.trim().lowercase().trimEnd('/')
    return clean == "/auth" || (clean.startsWith("/auth/") && !clean.startsWith("/auth/callback"))
}

private data class ResolvedVistaAiTarget(
    val useSso: Boolean,
    val directUrl: String,
    val nextPath: String?,
    val cacheKey: String,
)

private fun resolveVistaAiTarget(routeOverride: String?): ResolvedVistaAiTarget {
    val raw = routeOverride?.trim().orEmpty()
    if (raw.isBlank() || raw.equals("vista://ai", ignoreCase = true)) {
        return ResolvedVistaAiTarget(
            useSso = true,
            directUrl = VISTA_AI_ORIGIN,
            nextPath = null,
            cacheKey = "ai-default",
        )
    }

    val parsed = runCatching { URI(raw) }.getOrNull()
    val host = parsed?.host?.lowercase().orEmpty()
    if (host == VISTA_AI_HOST || host.isBlank()) {
        val rawPath = parsed?.rawPath?.takeIf { it.isNotBlank() && it != "/" }
        val query = parsed?.rawQuery?.takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()
        val nextPath = rawPath?.let { "$it$query" }
        return ResolvedVistaAiTarget(
            useSso = true,
            directUrl = VISTA_AI_ORIGIN,
            nextPath = nextPath,
            cacheKey = "ai:${nextPath ?: "/"}",
        )
    }

    return ResolvedVistaAiTarget(
        useSso = false,
        directUrl = raw,
        nextPath = null,
        cacheKey = "ext:$raw",
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun buildConfiguredVistaAiWebView(
    context: Context,
    bgColorArgb: Int,
    callbacksHolder: VistaAiCallbacksHolder,
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        setBackgroundColor(bgColorArgb)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
        }

        val webView = this
        runCatching {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, false)
            }
        }

        addJavascriptInterface(
            VistaAiJsBridge(
                webViewProvider = { webView },
                callbacksHolder = callbacksHolder,
            ),
            "VistaAiBridge",
        )

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                callbacksHolder.onProgressChanged?.invoke(newProgress)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?,
            ): Boolean {
                val intent = runCatching { fileChooserParams?.createIntent() }.getOrNull()
                return callbacksHolder.onLaunchFileChooser?.invoke(
                    filePathCallback,
                    intent,
                ) ?: false
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                callbacksHolder.onPageStarted?.invoke()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                callbacksHolder.onPageFinished?.invoke(view, url)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val targetUrl = request?.url?.toString()?.trim().orEmpty()
                if (targetUrl.isBlank()) return true

                if (targetUrl.startsWith("vista://", ignoreCase = true)) {
                    return callbacksHolder.onOpenInternalLink?.invoke(targetUrl) == true
                }

                val parsed = runCatching { URI(targetUrl) }.getOrNull() ?: return true
                val scheme = parsed.scheme?.lowercase().orEmpty()
                if (scheme != "https" && scheme != "http") {
                    callbacksHolder.onOpenExternalBrowser?.invoke(Uri.parse(targetUrl))
                    return true
                }

                val host = parsed.host?.lowercase().orEmpty()
                val path = parsed.path.orEmpty()
                if (host == VISTA_AI_HOST) {
                    if (isManualAiAuthPath(path)) {
                        callbacksHolder.onRequestSsoReauth?.invoke()
                        return true
                    }
                    return false
                }
                if (host == "sso.cafevista.ir") {
                    return false
                }

                if (VistaGameUrlHelper.isVistaWebHost(host)) {
                    return callbacksHolder.onOpenInternalLink?.invoke(targetUrl) == true
                }

                callbacksHolder.onOpenExternalBrowser?.invoke(Uri.parse(targetUrl))
                return true
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
                handler?.cancel()
                callbacksHolder.onLoadError?.invoke(
                    "اتصال امن به هوش مصنوعی ویستا برقرار نشد.",
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                if (request?.isForMainFrame == true) {
                    callbacksHolder.onLoadError?.invoke(
                        "عدم دسترسی به سرویس هوش مصنوعی ویستا. لطفاً اتصال اینترنت را بررسی کنید.",
                    )
                }
            }
        }
    }
}
