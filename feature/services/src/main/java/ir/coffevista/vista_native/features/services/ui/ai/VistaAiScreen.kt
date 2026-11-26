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
    var onPageFinished: (() -> Unit)? = null
    var onProgressChanged: ((Int) -> Unit)? = null
    var onLoadError: ((String) -> Unit)? = null
    var onRequestSsoReauth: (() -> Unit)? = null
    var onOpenInternalLink: ((String) -> Boolean)? = null
    var onOpenExternalBrowser: ((Uri) -> Unit)? = null
    var onLaunchFileChooser: ((ValueCallback<Array<Uri>>?, Intent?) -> Boolean)? = null
    var currentThemeName: String = "dark"
}

/**
 * Retains an authenticated WebView instance per signed-in userId so switching away from
 * and back to the Vista AI tab restores conversations and DOM state with zero reload delay.
 * Cleared strictly on logout or account switch.
 */
object VistaAiWebViewRegistry {
    private var retainedUserId: String? = null
    private var retainedRouteKey: String? = null
    private var retainedWebView: WebView? = null
    internal val callbacksHolder = VistaAiCallbacksHolder()

    internal fun getOrCreate(
        context: Context,
        userId: String,
        routeKey: String,
        factory: (Context) -> WebView,
    ): Pair<WebView, Boolean> {
        val existing = retainedWebView
        if (existing != null && retainedUserId == userId && retainedRouteKey == routeKey) {
            (existing.parent as? ViewGroup)?.removeView(existing)
            return existing to false
        }

        val switchingUser = retainedUserId != null && retainedUserId != userId
        clearAll(expireAiSessionCookies = switchingUser)

        val created = factory(context.applicationContext)
        retainedUserId = userId
        retainedRouteKey = routeKey
        retainedWebView = created
        return created to true
    }

    fun clearForUser(userId: String? = null) {
        if (userId == null || retainedUserId == userId) {
            clearAll(expireAiSessionCookies = true)
        }
    }

    private fun clearAll(expireAiSessionCookies: Boolean) {
        retainedWebView?.let { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
            runCatching {
                if (expireAiSessionCookies) {
                    webView.evaluateJavascript(
                        "try{localStorage.clear();sessionStorage.clear();}catch(e){}",
                        null,
                    )
                }
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.removeJavascriptInterface("VistaAiBridge")
                webView.destroy()
            }
        }
        retainedWebView = null
        retainedUserId = null
        retainedRouteKey = null

        if (expireAiSessionCookies) {
            runCatching {
                val cm = CookieManager.getInstance()
                cm.setCookie(VISTA_AI_ORIGIN, "vista_ai_access_token=; Max-Age=0; Path=/")
                cm.setCookie(VISTA_AI_ORIGIN, "vista_ai_user_meta=; Max-Age=0; Path=/")
                cm.flush()
            }
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
    var isBootstrappingSso by remember { mutableStateOf(false) }
    var isPageLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reauthNonce by remember { mutableIntStateOf(0) }
    var lastReauthAtMs by remember { mutableStateOf(0L) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

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
    // Clear floating VistaBottomIsland (height 58dp + 10dp bottom margin) when keyboard is hidden
    val bottomIslandClearance = if (isImeVisible) 0.dp else 74.dp

    SideEffect {
        VistaAiWebViewRegistry.callbacksHolder.apply {
            currentThemeName = themeName
            onPageStarted = {
                isPageLoading = true
                loadError = null
            }
            onPageFinished = {
                isPageLoading = false
                loadingProgress = 1f
                webViewInstance?.evaluateJavascript(
                    "window.__vistaAiSyncTheme && window.__vistaAiSyncTheme('$themeName')",
                    null,
                )
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
            onRequestSsoReauth = {
                val now = System.currentTimeMillis()
                if (now - lastReauthAtMs > 5_000L) {
                    lastReauthAtMs = now
                    reauthNonce += 1
                }
            }
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
            wv.setBackgroundColor(surfaceBgColor.toArgb())
            wv.evaluateJavascript(
                "window.__vistaAiSyncTheme && window.__vistaAiSyncTheme('$themeName')",
                null,
            )
        }
    }

    LaunchedEffect(userId, resolvedTarget.cacheKey, needsInitialLoad, reauthNonce) {
        val wv = webViewInstance ?: return@LaunchedEffect
        val shouldExchangeSso = (needsInitialLoad || reauthNonce > 0) && resolvedTarget.useSso
        if (!shouldExchangeSso) {
            if (needsInitialLoad) {
                needsInitialLoad = false
                isPageLoading = true
                wv.loadUrl(resolvedTarget.directUrl)
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
            wv.loadUrl(callbackUrl)
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
            // Detach view from Compose AndroidView without destroying cached WebView
            (webViewInstance?.parent as? ViewGroup)?.removeView(webViewInstance)
        }
    }

    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
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
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val (webView, createdNew) = VistaAiWebViewRegistry.getOrCreate(
                        context = ctx,
                        userId = userId,
                        routeKey = resolvedTarget.cacheKey,
                    ) { appCtx ->
                        buildConfiguredVistaAiWebView(
                            appContext = appCtx,
                            bgColorArgb = surfaceBgColor.toArgb(),
                        )
                    }
                    webViewInstance = webView
                    if (createdNew || webView.url.isNullOrBlank() || webView.url == "about:blank") {
                        needsInitialLoad = true
                    }
                    webView
                },
                update = { view ->
                    webViewInstance = view
                    view.setBackgroundColor(surfaceBgColor.toArgb())
                },
            )

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
                        text = "در حال ورود یکپارچه به ویستا AI...",
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
                            if (resolvedTarget.useSso) {
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
    appContext: Context,
    bgColorArgb: Int,
): WebView {
    return WebView(appContext).apply {
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
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, false)
        }

        addJavascriptInterface(
            object {
                @JavascriptInterface
                fun requestSsoReauth() {
                    post {
                        val host = runCatching { URI(this@apply.url.orEmpty()).host?.lowercase() }.getOrNull()
                        if (host == VISTA_AI_HOST) {
                            VistaAiWebViewRegistry.callbacksHolder.onRequestSsoReauth?.invoke()
                        }
                    }
                }

                @JavascriptInterface
                fun getTheme(): String {
                    return VistaAiWebViewRegistry.callbacksHolder.currentThemeName
                }
            },
            "VistaAiBridge",
        )

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                VistaAiWebViewRegistry.callbacksHolder.onProgressChanged?.invoke(newProgress)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?,
            ): Boolean {
                val intent = runCatching { fileChooserParams?.createIntent() }.getOrNull()
                return VistaAiWebViewRegistry.callbacksHolder.onLaunchFileChooser?.invoke(
                    filePathCallback,
                    intent,
                ) ?: false
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                VistaAiWebViewRegistry.callbacksHolder.onPageStarted?.invoke()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                VistaAiWebViewRegistry.callbacksHolder.onPageFinished?.invoke()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val targetUrl = request?.url?.toString()?.trim().orEmpty()
                if (targetUrl.isBlank()) return true

                if (targetUrl.startsWith("vista://", ignoreCase = true)) {
                    return VistaAiWebViewRegistry.callbacksHolder.onOpenInternalLink?.invoke(targetUrl) == true
                }

                val parsed = runCatching { URI(targetUrl) }.getOrNull() ?: return true
                val scheme = parsed.scheme?.lowercase().orEmpty()
                if (scheme != "https" && scheme != "http") {
                    VistaAiWebViewRegistry.callbacksHolder.onOpenExternalBrowser?.invoke(Uri.parse(targetUrl))
                    return true
                }

                val host = parsed.host?.lowercase().orEmpty()
                if (host == VISTA_AI_HOST || host == "sso.cafevista.ir") {
                    return false
                }

                if (VistaGameUrlHelper.isVistaWebHost(host)) {
                    return VistaAiWebViewRegistry.callbacksHolder.onOpenInternalLink?.invoke(targetUrl) == true
                }

                VistaAiWebViewRegistry.callbacksHolder.onOpenExternalBrowser?.invoke(Uri.parse(targetUrl))
                return true
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
                handler?.cancel()
                VistaAiWebViewRegistry.callbacksHolder.onLoadError?.invoke(
                    "اتصال امن به هوش مصنوعی ویستا برقرار نشد.",
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                if (request?.isForMainFrame == true) {
                    VistaAiWebViewRegistry.callbacksHolder.onLoadError?.invoke(
                        "عدم دسترسی به سرویس هوش مصنوعی ویستا. لطفاً اتصال اینترنت را بررسی کنید.",
                    )
                }
            }
        }
    }
}
